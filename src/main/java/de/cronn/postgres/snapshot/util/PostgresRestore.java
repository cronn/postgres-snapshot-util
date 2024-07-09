package de.cronn.postgres.snapshot.util;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.utility.MountableFile;

public final class PostgresRestore {

	private static final Logger log = LoggerFactory.getLogger(PostgresRestore.class);

	private static final String RESTORE_FILE = "/tmp/pg_restore.data";

	private PostgresRestore() {
	}

	public static void restoreFromFile(Path fileToRestore, String jdbcUrl, String username, String password,
									   PostgresRestoreOption... options) {
		restoreFromFile(fileToRestore, jdbcUrl, username, password, List.of(), options);
	}

	public static void restoreFromFile(Path fileToRestore, String jdbcUrl, String username, String password,
									   List<Schema> schemas, PostgresRestoreOption... options) {
		try (GenericContainer<?> container = createPgRestoreInContainer(jdbcUrl, username, password, schemas, options)
			.withCopyFileToContainer(MountableFile.forHostPath(fileToRestore), RESTORE_FILE)) {
			container.start();
		}
	}

	private static GenericContainer<?> createPgRestoreInContainer(
		String jdbcUrl, String username, String password, List<Schema> schemas, PostgresRestoreOption... options) {
		ConnectionInformation connectionInformation = PostgresUtils.parseConnectionInformation(jdbcUrl, username, password);
		String[] command = createPgRestoreCommand(connectionInformation, schemas, options);

		log.debug("Executing {}", String.join(" ", command));

		return PostgresUtils.createPostgresContainer(connectionInformation.postgresVersion())
			.withEnv(PostgresConstants.PG_PASSWORD_ENVIRONMENT_VARIABLE, connectionInformation.password())
			.withStartupCheckStrategy(new OneShotStartupCheckStrategy())
			.withCommand(command);
	}

	private static String[] createPgRestoreCommand(ConnectionInformation connectionInformation, List<Schema> schemas,
												   PostgresRestoreOption... options) {
		List<String> commandArgs = new ArrayList<>(List.of(
			"pg_restore",
			"--host=" + connectionInformation.host(),
			"--username=" + connectionInformation.username(),
			"--dbname=" + connectionInformation.databaseName()));

		if (connectionInformation.port() > 0) {
			commandArgs.add(2, "--port=" + connectionInformation.port());
		}

		for (Schema schema : schemas) {
			commandArgs.addAll(schema.getCommandArguments());
		}

		for (PostgresRestoreOption option : options) {
			commandArgs.add(option.getCommandArgument());
		}

		commandArgs.add(RESTORE_FILE);

		return commandArgs.toArray(String[]::new);
	}

}
