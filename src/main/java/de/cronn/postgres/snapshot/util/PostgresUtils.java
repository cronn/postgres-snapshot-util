package de.cronn.postgres.snapshot.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.ExecInContainerPattern;
import org.testcontainers.containers.GenericContainer;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ContainerNetwork;

final class PostgresUtils {

	private static final Logger log = LoggerFactory.getLogger(PostgresUtils.class);

	private static final Pattern POSTGRES_VERSION_PATTERN = Pattern.compile("postgres \\(PostgreSQL\\) (\\d+\\.\\d+) \\(.+\\)");
	private static final String JDBC_URL_PREFIX = "jdbc:";

	static GenericContainer<?> createPostgresContainer(String postgresVersion) {
		GenericContainer<?> container = new GenericContainer<>("postgres:" + postgresVersion);

		if (SystemUtils.IS_OS_LINUX) {
			container.withExtraHost(PostgresConstants.DOCKER_HOST_INTERNAL, PostgresConstants.HOST_GATEWAY);
		}

		return container;
	}

	static ConnectionInformation parseConnectionInformation(String jdbcUrl, String username, String password) {
		URI databaseUri = toUri(jdbcUrl);
		String host = prepareHostname(databaseUri.getHost());
		ContainerAndNetwork containerAndNetwork = findDockerContainer(host);

		if (containerAndNetwork != null) {
			String postgresVersion = findPostgresVersionOfRunningContainer(containerAndNetwork.inspectResponse());
			String databaseUriPath = databaseUri.getPath();
			if (!databaseUriPath.startsWith("/")) {
				throw new IllegalArgumentException("Unexpected path: " + databaseUriPath);
			}
			String databaseName = databaseUriPath.substring(1);
			int port = databaseUri.getPort();
			String dockerNetworkId = containerAndNetwork.network().getNetworkID();
			return new ConnectionInformation(postgresVersion, host, dockerNetworkId, port, databaseName, username, password);
		} else {
			try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
				DatabaseMetaData metaData = connection.getMetaData();
				String postgresVersion = "%d.%d".formatted(metaData.getDatabaseMajorVersion(), metaData.getDatabaseMinorVersion());
				String databaseName = connection.getCatalog();
				int port = databaseUri.getPort();
				String resolvedHost = resolveHostIfNecessary(host);
				return new ConnectionInformation(postgresVersion, resolvedHost, null, port, databaseName, username, password);
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private static String resolveHostIfNecessary(String host) {
		if (isDockerHostInternal(host)) {
			return host;
		} else {
			try {
				return InetAddress.getByName(host).getHostAddress();
			} catch (UnknownHostException e) {
				log.warn("Failed to resolve '{}'", host, e);
				return host;
			}
		}
	}

	private static String findPostgresVersionOfRunningContainer(InspectContainerResponse containerInfo) {
		DockerClient client = DockerClientFactory.instance().client();
		try {
			ExecResult execResult = ExecInContainerPattern.execInContainer(client, containerInfo, "postgres", "--version");

			int exitCode = execResult.getExitCode();
			if (!execResult.getStderr().isBlank()) {
				log.error("Failed to obtain PostgreSQL version: {}", execResult.getStderr().trim());
			}
			if (exitCode != 0) {
				throw new RuntimeException("Failed to obtain PostgreSQL version. Command exited with code " + exitCode);
			}
			String postgresVersionOutput = execResult.getStdout().trim();
			Matcher matcher = POSTGRES_VERSION_PATTERN.matcher(postgresVersionOutput);
			if (!matcher.matches()) {
				throw new RuntimeException("Failed to parse PostgreSQL version: " + postgresVersionOutput);
			}

			return matcher.group(1);
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private record ContainerAndNetwork(InspectContainerResponse inspectResponse, ContainerNetwork network) {
	}

	static ContainerAndNetwork findDockerContainer(String host) {
		if (isDockerHostInternal(host)) {
			return null;
		}

		DockerClient client = DockerClientFactory.instance().client();

		return client.listContainersCmd().exec().stream()
			.map(container -> {
				InspectContainerResponse inspectContainerResponse = client.inspectContainerCmd(container.getId()).exec();
				for (ContainerNetwork network : inspectContainerResponse.getNetworkSettings().getNetworks().values()) {
					if (network.getAliases() != null && network.getAliases().contains(host)) {
						log.debug("Found Docker container {} with network {}",
							String.join(", ", container.getNames()), network.getNetworkID());
						return new ContainerAndNetwork(inspectContainerResponse, network);
					}
				}
				return null;
			})
			.filter(Objects::nonNull)
			.findFirst()
			.orElse(null);
	}

	private static URI toUri(String jdbcUrl) {
		if (!jdbcUrl.startsWith(JDBC_URL_PREFIX)) {
			throw new IllegalArgumentException("Unexpected jdbcUrl: " + jdbcUrl);
		}
		try {
			return new URI(jdbcUrl.substring(JDBC_URL_PREFIX.length()));
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	}

	private static String prepareHostname(String host) {
		if (isLocalhost(host)) {
			return PostgresConstants.DOCKER_HOST_INTERNAL;
		} else {
			return host;
		}
	}

	static boolean isLocalhost(String host) {
		if (host.equals("localhost")) {
			return true;
		}
		InetAddress inetAddress;
		try {
			inetAddress = InetAddress.getByName(host);
		} catch (UnknownHostException e) {
			log.trace("Failed to resolve host '{}'", host, e);
			return false;
		}
		try {
			return InetAddress.getByName("localhost").equals(inetAddress);
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
	}

	static String deriveNetworkMode(ConnectionInformation connectionInformation) {
		if (isDockerHostInternal(connectionInformation.host())) {
			return null;
		} else if (connectionInformation.dockerNetworkId() != null) {
			log.debug("Using Docker network '{}'", connectionInformation.dockerNetworkId());
			return connectionInformation.dockerNetworkId();
		} else {
			log.debug("Using network mode 'host'");
			return "host";
		}
	}

	private static boolean isDockerHostInternal(String host) {
		return host.equals(PostgresConstants.DOCKER_HOST_INTERNAL);
	}
}
