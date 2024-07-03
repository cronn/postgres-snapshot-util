package de.cronn.postgres.snapshot.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.utility.ThrowingFunction;

public final class PostgresDump {

	private static final Logger log = LoggerFactory.getLogger(PostgresDump.class);

	private static final String CONTAINER_DUMP_FILE = "/tmp/pg_dump.data";
	private static final Charset ENCODING = StandardCharsets.UTF_8;

	private PostgresDump() {
	}

	public static String dumpToString(String jdbcUrl, String username, String password, PostgresDumpOption... options) {
		try (StringWriter writer = new StringWriter()) {
			dump(writer, jdbcUrl, username, password, options);
			return writer.toString();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void dumpToFile(Path path, String jdbcUrl, String username, String password,
								  PostgresDumpFormat format, PostgresDumpOption... options) {
		try (OutputStream outputStream = Files.newOutputStream(path)) {
			dump(outputStream, jdbcUrl, username, password, format, options);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void dump(Writer writer, String jdbcUrl, String username, String password,
							PostgresDumpOption... options) {
		dump(jdbcUrl, username, password, PostgresDumpFormat.PLAIN_TEXT, inputStream -> {
			try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream, ENCODING)) {
				return inputStreamReader.transferTo(writer);
			}
		}, options);
	}

	public static void dump(OutputStream outputStream, String jdbcUrl, String username, String password,
							PostgresDumpFormat format, PostgresDumpOption... options) {
		dump(jdbcUrl, username, password, format, inputStream -> inputStream.transferTo(outputStream), options);
	}

	public static void dump(String jdbcUrl, String username, String password, PostgresDumpFormat format,
							ThrowingFunction<InputStream, Long> inputStreamProcessor, PostgresDumpOption... options) {
		try (GenericContainer<?> container =
				 createPgDumpInContainer(jdbcUrl, username, password, format, options)) {
			container.start();

			container.copyFileFromContainer(CONTAINER_DUMP_FILE, inputStreamProcessor);
		}
	}

	private static GenericContainer<?> createPgDumpInContainer(
		String jdbcUrl, String username, String password, PostgresDumpFormat format, PostgresDumpOption... options) {
		ConnectionInformation connectionInformation = PostgresUtils.parseConnectionInformation(jdbcUrl, username, password);
		String[] command = createPgDumpCommand(connectionInformation, format, options);

		log.debug("Executing {}", String.join(" ", command));

		return PostgresUtils.createPostgresContainer()
			.withEnv(PostgresConstants.PG_PASSWORD_ENVIRONMENT_VARIABLE, connectionInformation.password())
			.withStartupCheckStrategy(new OneShotStartupCheckStrategy())
			.withCommand(command);
	}

	private static String[] createPgDumpCommand(ConnectionInformation connectionInformation,
												PostgresDumpFormat format, PostgresDumpOption... options) {
		List<String> commandArgs = new ArrayList<>(List.of(
			"pg_dump",
			"--host=" + connectionInformation.host(),
			"--username=" + connectionInformation.username(),
			"--dbname=" + connectionInformation.databaseName(),
			"--format=" + format.getCommandArgument(),
			"--file=" + CONTAINER_DUMP_FILE,
			"--encoding=" + ENCODING.name()));

		if (connectionInformation.port() > 0) {
			commandArgs.add(2, "--port=" + connectionInformation.port());
		}

		commandArgs.addAll(Arrays.stream(options).map(PostgresDumpOption::getCommandArgument).toList());

		return commandArgs.toArray(String[]::new);
	}

}
