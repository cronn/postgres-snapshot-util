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
	 * skip restoration of object ownership
	 */
	NO_OWNER("--no-owner"),
	;

	private final String commandArgument;

	PostgresRestoreOption(String commandArgument) {
		this.commandArgument = commandArgument;
	}

	public String getCommandArgument() {
		return commandArgument;
	}
}
