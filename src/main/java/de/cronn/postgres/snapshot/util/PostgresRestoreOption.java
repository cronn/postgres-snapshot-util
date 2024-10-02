package de.cronn.postgres.snapshot.util;

public enum PostgresRestoreOption {
	/**
	 * clean (drop) database objects before recreating
	 */
	CLEAN("--clean"),
	/**
	 * use IF EXISTS when dropping objects
	 */
	IF_EXISTS("--if-exists"),
	/**
	 * create the target database
	 */
	CREATE("--create"),
	/**
	 * exit on error, default is to continue
	 */
	EXIT_ON_ERROR("--exit-on-error"),
	/**
	 * restore as a single transaction
	 */
	SINGLE_TRANSACTION("--single-transaction"),
	/**
	 * restore only the data, no schema
	 */
	DATA_ONLY("--data-only"),
	/**
	 * restore only the schema, no data
	 */
	SCHEMA_ONLY("--schema-only"),
	/**
	 * skip restoration of object ownership
	 */
	NO_OWNER("--no-owner"),

	/**
	 * Specifies verbose mode.
	 * This will cause pg_restore to output detailed object comments and start/stop times to the output file,
	 * and progress messages to standard error. Repeating the option causes additional debug-level messages to appear on standard error.
	 */
	VERBOSE("--verbose"),
	;

	private final String commandArgument;

	PostgresRestoreOption(String commandArgument) {
		this.commandArgument = commandArgument;
	}

	public String getCommandArgument() {
		return commandArgument;
	}
}
