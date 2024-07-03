package de.cronn.postgres.snapshot.util;

public enum PostgresDumpFormat {
	CUSTOM("c"),
	DIRECTORY("d"),
	TAR("t"),
	PLAIN_TEXT("p");

	private final String commandArgument;

	PostgresDumpFormat(String commandArgument) {
		this.commandArgument = commandArgument;
	}

	public String getCommandArgument() {
		return commandArgument;
	}
}
