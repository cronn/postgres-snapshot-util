package de.cronn.postgres.snapshot.util;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.lang3.SystemUtils;
import org.testcontainers.containers.GenericContainer;

final class PostgresUtils {
	static GenericContainer<?> createPostgresContainer() {
		GenericContainer<?> container = new GenericContainer<>(PostgresConstants.POSTGRES_DOCKER_IMAGE);

		if (SystemUtils.IS_OS_LINUX) {
			container.withExtraHost(PostgresConstants.DOCKER_HOST_INTERNAL, PostgresConstants.HOST_GATEWAY);
		}

		return container;
	}

	static ConnectionInformation parseConnectionInformation(String jdbcUrl, String username, String password) {
		URI databaseUri = toUri(jdbcUrl);

		int port = databaseUri.getPort();
		String databaseName = getDatabaseName(jdbcUrl, username, password);
		String host = resolveHost(databaseUri.getHost());

		return new ConnectionInformation(host, port, databaseName, username, password);
	}

	private static String getDatabaseName(String jdbcUrl, String username, String password) {
		Properties connectionProperties = new Properties();
		connectionProperties.put("user", username);
		connectionProperties.put("password", password);

		try (Connection connection = DriverManager.getConnection(jdbcUrl, connectionProperties)) {
			return connection.getCatalog();
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
}
