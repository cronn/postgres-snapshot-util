package de.cronn.postgres.snapshot.util;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import de.cronn.assertions.validationfile.FileExtension;
import de.cronn.assertions.validationfile.FileExtensions;
import de.cronn.assertions.validationfile.ValidationFileAssertions;
import de.cronn.testutils.ThreadLeakCheck;

@ThreadLeakCheck.AllowedThreads(
	prefixes = {
		"ducttape-",
		"testcontainers-pull-watchdog-",
	},
	names = {
		"process reaper",
		"Attach Listener",
		"testcontainers-ryuk",
		"PostgreSQL-JDBC-Cleaner",
		"JNA Cleaner"
	}
)
@ExtendWith({
	ThreadLeakCheck.class,
	SoftAssertionsExtension.class
})
abstract class BaseTest {

	private static final DockerImageName POSTGRES_DOCKER_IMAGE = DockerImageName.parse("postgres:16.1");

	private static final String DATABASE_NAME = "test-db";
	protected static final String USERNAME = "test-user";
	protected static final String PASSWORD = "test-password";

	static final PostgreSQLContainer<?> postgresContainer = createPostgresContainer();
	static String jdbcUrl;

	@InjectSoftAssertions
	private SoftAssertions softly;

	private ValidationFileAssertions validationFileAssertions;

	protected static PostgreSQLContainer<?> createPostgresContainer() {
		return createPostgresContainer(POSTGRES_DOCKER_IMAGE);
	}

	protected static PostgreSQLContainer<?> createPostgresContainer(DockerImageName postgresDockerImage) {
		return new PostgreSQLContainer<>(postgresDockerImage)
			.withDatabaseName(DATABASE_NAME)
			.withUsername(USERNAME)
			.withPassword(PASSWORD)
			.withTmpFs(Map.of("/var/lib/postgresql/data", "rw"));
	}

	protected static void createSomeTableAndInsertData() {
		Properties connectionProperties = new Properties();
		connectionProperties.put("user", USERNAME);
		connectionProperties.put("password", PASSWORD);

		try (Connection connection = DriverManager.getConnection(jdbcUrl, connectionProperties)) {
			try (PreparedStatement createTableStatement = connection.prepareStatement("""
				CREATE TABLE employees (
				    id INT PRIMARY KEY,
				    first_name VARCHAR(50),
				    last_name VARCHAR(50),
				    email VARCHAR(100),
				    hire_date DATE,
				    salary DECIMAL(10, 2)
				)""")) {
				createTableStatement.execute();
			}
			try (PreparedStatement insertStatement = connection.prepareStatement("""
				INSERT INTO employees (id, first_name, last_name, email, hire_date, salary) VALUES
				 (1, 'John', 'Doe', 'john.doe@example.com', '2022-01-15', 60000.00),
				 (2, 'Jane', 'Smith', 'jane.smith@example.com', '2023-03-22', 75000.00),
				 (3, 'Emily', 'Johnson', 'emily.johnson@example.com', '2021-05-10', 50000.00),
				 (4, 'Michael', 'Brown', 'michael.brown@example.com', '2020-08-30', 80000.00),
				 (5, 'Sarah', 'Davis', 'sarah.davis@example.com', '2024-06-01', 70000.00);""")) {
				insertStatement.execute();
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@BeforeEach
	void prepareValidationFileAssertions(TestInfo testInfo) {
		validationFileAssertions = new SoftValidationFileAssertions(testInfo);
	}

	protected void compareActualWithValidationFile(String actual) throws Exception {
		compareActualWithValidationFile(actual, null);
	}

	protected void compareActualWithValidationFile(String actual, String suffix) throws Exception {
		compareActualWithValidationFile(actual, suffix, FileExtensions.TXT);
	}

	protected void compareActualWithValidationFile(String actual, String suffix, FileExtension fileExtension) {
		validationFileAssertions.assertWithFileWithSuffix(actual, suffix, fileExtension);
	}

	protected String getTestName() {
		return validationFileAssertions.getTestName();
	}

	private class SoftValidationFileAssertions implements ValidationFileAssertions {

		private final TestInfo testInfo;

		public SoftValidationFileAssertions(TestInfo testInfo) {
			this.testInfo = testInfo;
		}

		@Override
		public String getTestName() {
			List<String> classes = classHierarchy(getTestClass());
			return String.join("/", classes) + "/" + getTestMethod().getName();
		}

		private static List<String> classHierarchy(Class<?> aClass) {
			List<String> classHierarchy = new ArrayList<>();
			classHierarchy.add(aClass.getSimpleName());
			Class<?> enclosingClass = aClass.getEnclosingClass();
			while (enclosingClass != null) {
				classHierarchy.add(enclosingClass.getSimpleName());
				enclosingClass = enclosingClass.getEnclosingClass();
			}
			Collections.reverse(classHierarchy);
			return classHierarchy;
		}

		private Method getTestMethod() {
			return testInfo.getTestMethod().orElseThrow();
		}

		private Class<?> getTestClass() {
			return testInfo.getTestClass().orElseThrow();
		}

		@Override
		public FailedAssertionHandler failedAssertionHandler() {
			return callable -> softly.check(callable::call);
		}

	}
}
