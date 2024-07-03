package de.cronn.postgres.snapshot.util;

record ConnectionInformation(String postgresVersion, String host, int port, String databaseName, String username,
							 String password) {
}
