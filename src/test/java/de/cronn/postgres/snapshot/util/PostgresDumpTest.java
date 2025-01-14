package de.cronn.postgres.snapshot.util;

import static org.assertj.core.api.Assertions.*;

import java.io.OutputStream;
import java.io.Writer;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.utility.DockerImageName;

import de.cronn.assertions.validationfile.normalization.SimpleRegexReplacement;

class PostgresDumpTest extends BaseTest {

	@Nested
	class EmptyDatabase {

		@BeforeAll
		static void startPostgresContainer() {
			postgresContainer.start();
			jdbcUrl = postgresContainer.getJdbcUrl();
		}

		@AfterAll
		static void stopPostgresContainer() {
			postgresContainer.stop();
		}

		@Test
		void testDumpToString() {
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
		void testSchemaOnly() {
			String dump = PostgresDump.dumpToString(jdbcUrl, USERNAME, PASSWORD, PostgresDumpOption.SCHEMA_ONLY);
			compareActualWithValidationFile(dump);
		}
	}

	@Nested
	class DatabaseWithData {

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
		void testDump() {
			String dump = PostgresDump.dumpToString(jdbcUrl, USERNAME, PASSWORD);
			compareActualWithValidationFile(dump);
		}

		@Test
		void testDumpVerbose() {
			String dump = PostgresDump.dumpToString(jdbcUrl, USERNAME, PASSWORD, PostgresDumpOption.VERBOSE);
			compareActualWithValidationFile(dump,
				new SimpleRegexReplacement("(Started|Completed) on \\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}", "$1 on [MASKED_TIMESTAMP]"));
		}

		@Test
		void testDumpAsTarFile(@TempDir Path tempDir) throws Exception {
			Path dumpFile = tempDir.resolve("dump.tar");
			try (OutputStream outputStream = Files.newOutputStream(dumpFile)) {
				PostgresDump.dump(outputStream, jdbcUrl, USERNAME, PASSWORD, PostgresDumpFormat.TAR);
			}
			assertThat(dumpFile).hasSize(10240);
		}

		@Test
		void testSchemaOnly() {
			String dump = PostgresDump.dumpToString(jdbcUrl, USERNAME, PASSWORD, PostgresDumpOption.SCHEMA_ONLY);
			compareActualWithValidationFile(dump);
		}

		@Test
		void testDataOnly() {
			String dump = PostgresDump.dumpToString(jdbcUrl, USERNAME, PASSWORD, PostgresDumpOption.DATA_ONLY);
			compareActualWithValidationFile(dump);
		}

		@Test
		void testCleanAndCreate() {
			String dump = PostgresDump.dumpToString(jdbcUrl, USERNAME, PASSWORD,
				PostgresDumpOption.CLEAN,
				PostgresDumpOption.CREATE,
				PostgresDumpOption.SCHEMA_ONLY);
			compareActualWithValidationFile(dump);
		}

		@Test
		void testCleanAndCreateIfExists() {
			String dump = PostgresDump.dumpToString(jdbcUrl, USERNAME, PASSWORD,
				PostgresDumpOption.CLEAN,
				PostgresDumpOption.IF_EXISTS,
				PostgresDumpOption.CREATE,
				PostgresDumpOption.SCHEMA_ONLY);
			compareActualWithValidationFile(dump);
		}

		@Test
		void testDumpWithoutPrivileges() {
			String dump = PostgresDump.dumpToString(jdbcUrl, USERNAME, PASSWORD, PostgresDumpOption.NO_PRIVILEGES);
			compareActualWithValidationFile(dump);
		}

		@Test
		void testDumpSpecificSchemaOnly() {
			String dump = PostgresDump.dumpToString(jdbcUrl, USERNAME, PASSWORD,
				List.of(Schema.include("other_schema")),
				PostgresDumpOption.NO_OWNER, PostgresDumpOption.INSERTS);
			compareActualWithValidationFile(dump);
		}

		@Test
		void testDumpWithoutSpecificSchema() {
			String dump = PostgresDump.dumpToString(jdbcUrl, USERNAME, PASSWORD,
				List.of(Schema.exclude("other_schema")),
				PostgresDumpOption.NO_OWNER, PostgresDumpOption.INSERTS);
			compareActualWithValidationFile(dump);
		}

		@Test
		void testDumpWithIllegalParameters() {
			assertThatExceptionOfType(ContainerLaunchException.class)
				.isThrownBy(() ->
					PostgresDump.dumpToString(jdbcUrl, USERNAME, PASSWORD,
						PostgresDumpOption.SCHEMA_ONLY, PostgresDumpOption.DATA_ONLY))
				.withMessageStartingWith("Container startup failed for image postgres:");
		}

		@Test
		void testDumpExcludingTableData() {
			String dump = PostgresDump.dumpToString(jdbcUrl, USERNAME, PASSWORD,
				List.of(), List.of("emplo*"));
			compareActualWithValidationFile(dump);
		}

		@Test
		void testDumpToFileExcludingTableData(@TempDir Path tempDir) throws Exception {
			Path dumpFile = tempDir.resolve("dump.sql");
			PostgresDump.dumpToFile(dumpFile, jdbcUrl, USERNAME, PASSWORD, PostgresDumpFormat.PLAIN_TEXT,
				List.of(), List.of("other_schema.persons"), PostgresDumpOption.INSERTS);
			String fileContent = Files.readString(dumpFile, PostgresDump.ENCODING);
			compareActualWithValidationFile(fileContent);
		}

		@Test
		void testConnectViaHostName() throws Exception {
			String jdbcUrl = postgresContainer.getJdbcUrl();
			assertThat(jdbcUrl).contains("://localhost:");
			String replacedJdbcUrl = jdbcUrl.replaceFirst("localhost:", InetAddress.getLocalHost().getHostName() + ":");
			String schema = PostgresDump.dumpToString(replacedJdbcUrl, USERNAME, PASSWORD);
			compareActualWithValidationFile(schema);
		}
	}

	@Test
	void testOtherPostgresVersion() {
		try (PostgreSQLContainer<?> otherPostgresContainer = createPostgresContainer(DockerImageName.parse("postgres:14.12"))) {
			otherPostgresContainer.start();
			String jdbcUrl = otherPostgresContainer.getJdbcUrl();

			String schema = PostgresDump.dumpToString(jdbcUrl, USERNAME, PASSWORD, PostgresDumpOption.SCHEMA_ONLY);
			compareActualWithValidationFile(schema);
		}
	}

	@Test
	void testConnectViaDockerContainerIpAddress() {
		String networkAlias = "postgres-db";
		try (Network network = Network.newNetwork();
			 PostgreSQLContainer<?> postgresInNetworkContainer = createPostgresContainer(DockerImageName.parse("postgres:17.0"))
				 .withNetwork(network)
				 .withNetworkAliases(networkAlias)) {
			postgresInNetworkContainer.start();

			String ipAddress = resolveHostname(network, networkAlias);

			String jdbcUrl = "jdbc:postgresql://%s/test-db".formatted(ipAddress);
			String schema = PostgresDump.dumpToString(jdbcUrl, USERNAME, PASSWORD);
			compareActualWithValidationFile(schema);
		}
	}

	@ParameterizedTest
	@CsvSource({
		"vanilla-17, postgres:17.0",
		"postgis-17, postgis/postgis:17-3.4",
	})
	void testConnectViaDockerNetworkAlias(String testName, String fullImageName) {
		String networkAlias = "postgres-db";
		try (Network network = Network.newNetwork();
			 PostgreSQLContainer<?> postgresInNetworkContainer = createPostgresContainer(DockerImageName.parse(fullImageName)
				 .asCompatibleSubstituteFor("postgres"))
				 .withNetwork(network)
				 .withNetworkAliases(networkAlias)) {
			postgresInNetworkContainer.start();

			String jdbcUrl = "jdbc:postgresql://%s/test-db".formatted(networkAlias);
			String schema = PostgresDump.dumpToString(jdbcUrl, USERNAME, PASSWORD);
			compareActualWithValidationFile(schema, testName);
		}
	}

	private String resolveHostname(Network network, String hostname) {
		try (GenericContainer<?> dnsContainer = new GenericContainer<>("alpine:3.20.3")
			.withNetwork(network)
			.withCommand("getent hosts " + hostname)
			.withStartupCheckStrategy(new OneShotStartupCheckStrategy())) {
			dnsContainer.start();
			String logs = dnsContainer.getLogs();
			Matcher matcher = Pattern.compile("(\\d+\\.\\d+\\.\\d+\\.\\d+)\\s+" + hostname + "\\s+" + hostname + "\\n").matcher(logs);
			assertThat(matcher.matches()).describedAs("Failed to parse '%s'", logs).isTrue();
			return matcher.group(1);
		}
	}
}
