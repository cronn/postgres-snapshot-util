package de.cronn.postgres.snapshot.util;

import static org.assertj.core.api.Assertions.*;

import java.net.URISyntaxException;
import java.net.UnknownHostException;

import org.junit.jupiter.api.Test;

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
		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> PostgresUtils.parseConnectionInformation("jdbc:postgresql://unknown-host", "user", "password"))
			.withCauseExactlyInstanceOf(UnknownHostException.class)
			.withMessage("Failed to resolve host");
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
}
