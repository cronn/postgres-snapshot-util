package de.cronn.postgres.snapshot.util;

import static org.assertj.core.api.Assertions.*;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.PostgreSQLContainer;

class PostgresRestoreTest extends BaseTest {

	@BeforeAll
	static void startPostgresContainer() {
		postgresContainer.start();
		jdbcUrl = postgresContainer.getJdbcUrl();
	}

	@AfterAll
	static void stopPostgresContainer() {
		postgresContainer.stop();
	}

	@BeforeAll
	static void createTablesAndInsertData() {
		createSomeTableAndInsertData();
	}

	@Test
	void testDumpAndRestore(@TempDir Path tempDir) {
		String dumpPrimaryPostgres = PostgresDump.dumpToString(jdbcUrl, USERNAME, PASSWORD);

		Path dumpFile = tempDir.resolve("dump.tar");
		PostgresDump.dumpToFile(dumpFile, jdbcUrl, USERNAME, PASSWORD, PostgresDumpFormat.CUSTOM);

		try (PostgreSQLContainer<?> otherPostgres = createPostgresContainer()) {
			otherPostgres.start();
			String otherPostgresJdbcUrl = otherPostgres.getJdbcUrl();

			String dumpOtherPostgresBeforeRestore = PostgresDump.dumpToString(otherPostgresJdbcUrl, USERNAME, PASSWORD);
			assertThat(dumpOtherPostgresBeforeRestore).isNotEqualTo(dumpPrimaryPostgres);

			PostgresRestore.restoreFromFile(dumpFile, otherPostgresJdbcUrl, USERNAME, PASSWORD,
				PostgresRestoreOption.SINGLE_TRANSACTION,
				PostgresRestoreOption.EXIT_ON_ERROR);

			String dumpOtherPostgres = PostgresDump.dumpToString(otherPostgresJdbcUrl, USERNAME, PASSWORD);
			assertThat(dumpOtherPostgres).isEqualTo(dumpPrimaryPostgres);
		}
	}

	@Test
	void testRestoreOnlySpecificSchema(@TempDir Path tempDir) {
		Path dumpFile = tempDir.resolve("dump.tar");
		PostgresDump.dumpToFile(dumpFile, jdbcUrl, USERNAME, PASSWORD, PostgresDumpFormat.TAR);

		try (PostgreSQLContainer<?> otherPostgres = createPostgresContainer()) {
			otherPostgres.start();
			String otherPostgresJdbcUrl = otherPostgres.getJdbcUrl();

			PostgresRestore.restoreFromFile(dumpFile, otherPostgresJdbcUrl, USERNAME, PASSWORD,
				List.of(Schema.exclude("public")),
				PostgresRestoreOption.SCHEMA_ONLY,
				PostgresRestoreOption.SINGLE_TRANSACTION,
				PostgresRestoreOption.EXIT_ON_ERROR);

			String dumpAfterRestorePostgres = PostgresDump.dumpToString(otherPostgresJdbcUrl, USERNAME, PASSWORD);
			compareActualWithValidationFile(dumpAfterRestorePostgres);
		}
	}

	@Test
	void testRestoreFromFileThatDoesNotExist() {
		try (PostgreSQLContainer<?> otherPostgres = createPostgresContainer()) {
			otherPostgres.start();
			String otherPostgresJdbcUrl = otherPostgres.getJdbcUrl();

			assertThatExceptionOfType(ContainerLaunchException.class)
				.isThrownBy(() -> PostgresRestore.restoreFromFile(Path.of("does-not-exist"),
					otherPostgresJdbcUrl, USERNAME, PASSWORD))
				.withMessageStartingWith("Container startup failed for image postgres:");
		}
	}
}
