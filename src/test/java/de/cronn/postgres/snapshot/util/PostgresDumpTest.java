package de.cronn.postgres.snapshot.util;

import static org.assertj.core.api.Assertions.*;

import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

class PostgresDumpTest extends BaseTest {

	@Nested
	class EmptyDatabase {

		@BeforeAll
		static void startPostgresContainer() {
			postgresContainer.start();
			jdbcUrl = getJdbcUrl(postgresContainer);
		}

		@AfterAll
		static void stopPostgresContainer() {
			postgresContainer.stop();
		}

		@Test
		void testDumpToString() throws Exception {
			String dump = PostgresDump.dumpToString(jdbcUrl, USERNAME, PASSWORD);
			compareActualWithValidationFile(dump);
		}

		@Test
		void testDumpToFile(@TempDir Path tempDir) throws Exception {
			Path dumpFile = tempDir.resolve("dump.txt");
			try (Writer writer = Files.newBufferedWriter(dumpFile, StandardCharsets.UTF_8)) {
				PostgresDump.dump(writer, jdbcUrl, USERNAME, PASSWORD);
			}
			String dump = Files.readString(dumpFile, StandardCharsets.UTF_8);
			compareActualWithValidationFile(dump);
		}

		@Test
		void testSchemaOnly() throws Exception {
			String dump = PostgresDump.dumpToString(jdbcUrl, USERNAME, PASSWORD, PostgresDumpOption.SCHEMA_ONLY);
			compareActualWithValidationFile(dump);
		}
	}

	@Nested
	class DatabaseWithData {

		@BeforeAll
		static void startPostgresContainer() {
			postgresContainer.start();
			jdbcUrl = getJdbcUrl(postgresContainer);
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
		void testDump() throws Exception {
			String dump = PostgresDump.dumpToString(jdbcUrl, USERNAME, PASSWORD);
			compareActualWithValidationFile(dump);
		}

		@Test
		void testDumpAsTarFile(@TempDir Path tempDir) throws Exception {
			Path dumpFile = tempDir.resolve("dump.tar");
			try (OutputStream outputStream = Files.newOutputStream(dumpFile)) {
				PostgresDump.dump(outputStream, jdbcUrl, USERNAME, PASSWORD, PostgresDumpFormat.TAR);
			}
			assertThat(dumpFile).hasSize(7680);
		}

		@Test
		void testSchemaOnly() throws Exception {
			String dump = PostgresDump.dumpToString(jdbcUrl, USERNAME, PASSWORD, PostgresDumpOption.SCHEMA_ONLY);
			compareActualWithValidationFile(dump);
		}

		@Test
		void testDataOnly() throws Exception {
			String dump = PostgresDump.dumpToString(jdbcUrl, USERNAME, PASSWORD, PostgresDumpOption.DATA_ONLY);
			compareActualWithValidationFile(dump);
		}

		@Test
		void testCleanAndCreate() throws Exception {
			String dump = PostgresDump.dumpToString(jdbcUrl, USERNAME, PASSWORD,
				PostgresDumpOption.CLEAN,
				PostgresDumpOption.CREATE,
				PostgresDumpOption.SCHEMA_ONLY);
			compareActualWithValidationFile(dump);
		}

		@Test
		void testCleanAndCreateIfExists() throws Exception {
			String dump = PostgresDump.dumpToString(jdbcUrl, USERNAME, PASSWORD,
				PostgresDumpOption.CLEAN,
				PostgresDumpOption.IF_EXISTS,
				PostgresDumpOption.CREATE,
				PostgresDumpOption.SCHEMA_ONLY);
			compareActualWithValidationFile(dump);
		}
	}

	@Test
	void testOtherPostgresVersion() throws Exception {
		try (PostgreSQLContainer<?> otherPostgresContainer = createPostgresContainer(DockerImageName.parse("postgres:14.12"))) {
			otherPostgresContainer.start();
			String jdbcUrl = getJdbcUrl(otherPostgresContainer);

			String schema = PostgresDump.dumpToString(jdbcUrl, USERNAME, PASSWORD, PostgresDumpOption.SCHEMA_ONLY);
			compareActualWithValidationFile(schema);
		}
	}
}
