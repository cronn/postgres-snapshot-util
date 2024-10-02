package de.cronn.postgres.snapshot.util;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.lang3.SystemUtils;
import org.testcontainers.containers.GenericContainer;

final class PostgresUtils {
	static GenericContainer<?> createPostgresContainer(String postgresVersion) {
		GenericContainer<?> container = new GenericContainer<>("postgres:" + postgresVersion);

		if (SystemUtils.IS_OS_LINUX) {
			container.withExtraHost(PostgresConstants.DOCKER_HOST_INTERNAL, PostgresConstants.HOST_GATEWAY);
		}

		return container;
	}

	static ConnectionInformation parseConnectionInformation(String jdbcUrl, String username, String password) {
		URI databaseUri = toUri(jdbcUrl);

		int port = databaseUri.getPort();
		String host = resolveHost(databaseUri.getHost());

		Properties connectionProperties = new Properties();
		connectionProperties.put("user", username);
		connectionProperties.put("password", password);

		try (Connection connection = DriverManager.getConnection(jdbcUrl, connectionProperties)) {
			DatabaseMetaData metaData = connection.getMetaData();
			String postgresVersion = "%d.%d".formatted(metaData.getDatabaseMajorVersion(), metaData.getDatabaseMinorVersion());
			String databaseName = connection.getCatalog();
			return new ConnectionInformation(postgresVersion, host, port, databaseName, username, password);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	private static URI toUri(String jdbcUrl) {
		try {
			return new URI(jdbcUrl.replaceFirst("^jdbc:", ""));
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	}

	private static String resolveHost(String host) {
		try {
			InetAddress inetAddress = InetAddress.getByName(host);
			if (isLocalhost(inetAddress)) {
				return PostgresConstants.DOCKER_HOST_INTERNAL;
			} else {
				return inetAddress.getHostAddress();
			}
		} catch (UnknownHostException e) {
			throw new IllegalArgumentException("Failed to resolve host", e);
		}
	}

	private static boolean isLocalhost(InetAddress inetAddress) throws UnknownHostException {
		return InetAddress.getByName("localhost").equals(inetAddress);
	}

	static String deriveNetworkMode(ConnectionInformation connectionInformation) {
		return connectionInformation.host().equals(PostgresConstants.DOCKER_HOST_INTERNAL) ? null : "host";
	}
}
