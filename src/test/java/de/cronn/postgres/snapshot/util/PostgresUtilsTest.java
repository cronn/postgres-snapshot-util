package de.cronn.postgres.snapshot.util;

import static org.assertj.core.api.Assertions.*;

import java.net.InetAddress;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PostgresUtilsTest {

	@Test
	void testParseConnectionInformation_illegalJdbcUrl() {
		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> PostgresUtils.parseConnectionInformation("jdbc:illegal url", "user", "password"))
			.withCauseExactlyInstanceOf(URISyntaxException.class)
			.withMessage("java.net.URISyntaxException: Illegal character in path at index 7: illegal url");
	}

	@Test
	void testParseConnectionInformation_unknownHost() {
		assertThatExceptionOfType(RuntimeException.class)
			.isThrownBy(() -> PostgresUtils.parseConnectionInformation("jdbc:postgresql://unknown-host/test", "user", "password"))
			.withMessage("org.postgresql.util.PSQLException: The connection attempt failed.");
	}

	@Test
	void testParseConnectionInformation_failsToConnect() {
		assertThatExceptionOfType(RuntimeException.class)
			.isThrownBy(() -> PostgresUtils.parseConnectionInformation("jdbc:postgresql://10.0.0.1/test?connectTimeout=1", "user", "password"))
			.withMessage("org.postgresql.util.PSQLException: The connection attempt failed.");
	}

	@Test
	void testParseConnectionInformation_driverFailsToParseJdbcUrl() {
		assertThatExceptionOfType(RuntimeException.class)
			.isThrownBy(() -> PostgresUtils.parseConnectionInformation("jdbc:postgresql://localhost", "user", "password"))
			.withMessage("org.postgresql.util.PSQLException: Unable to parse URL jdbc:postgresql://localhost");
	}

	@Test
	void testParseConnectionInformation_failToConnectToPostgresDatabase() {
		assertThatExceptionOfType(RuntimeException.class)
			.isThrownBy(() -> PostgresUtils.parseConnectionInformation("jdbc:postgresql://localhost/test", "user", "password"))
			.withMessage("org.postgresql.util.PSQLException: Connection to localhost:5432 refused." +
						 " Check that the hostname and port are correct and that the postmaster is accepting TCP/IP connections.");
	}

	@ParameterizedTest
	@ValueSource(strings = { "localhost", "127.0.0.1" })
	void testIsLocalHost_positive(String host) {
		assertThat(PostgresUtils.isLocalhost(host)).isTrue();
	}

	@Test
	void testIsLocalHost_negative() throws Exception {
		assertThat(PostgresUtils.isLocalhost("some.host.local")).isFalse();
		assertThat(PostgresUtils.isLocalhost("1.2.3.4")).isFalse();
		InetAddress localHost = InetAddress.getLocalHost();
		assertThat(PostgresUtils.isLocalhost(localHost.getHostName())).isFalse();
	}
}
