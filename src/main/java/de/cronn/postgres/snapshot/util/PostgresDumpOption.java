package de.cronn.postgres.snapshot.util;

public enum PostgresDumpOption {
	/**
	 * clean (drop) database objects before recreating
	 */
	CLEAN("--clean"),
	/**
	 * use IF EXISTS when dropping objects
	 */
	IF_EXISTS("--if-exists"),
	/**
	 * include commands to create database in dump
	 */
	CREATE("--create"),
	/**
	 * dump only the schema, no data
	 */
	SCHEMA_ONLY("--schema-only"),
	/**
	 * do not dump comments
	 */
	NO_COMMENTS("--no-comments"),
	/**
	 * dump data as INSERT commands, rather than COPY
	 */
	INSERTS("--inserts"),
	/**
	 * dump only the data, not the schema
	 */
	DATA_ONLY("--data-only"),
	/**
	 * include large objects in dump
	 */
	LARGE_OBJECTS("--large-objects"),
	/**
	 * exclude large objects in dump
	 */
	NO_LARGE_OBJECTS("--no-large-objects"),
	/**
	 * do not dump privileges (grant/revoke)
	 */
	NO_PRIVILEGES("--no-privileges");

	private final String commandArgument;

	PostgresDumpOption(String commandArgument) {
		this.commandArgument = commandArgument;
	}

	public String getCommandArgument() {
		return commandArgument;
	}
}
