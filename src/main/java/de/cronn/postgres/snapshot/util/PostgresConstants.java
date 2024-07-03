package de.cronn.postgres.snapshot.util;

import org.testcontainers.utility.DockerImageName;

final class PostgresConstants {
	static final DockerImageName POSTGRES_DOCKER_IMAGE = DockerImageName.parse("postgres:16.1");
	static final String DOCKER_HOST_INTERNAL = "host.docker.internal";
	static final String HOST_GATEWAY = "host-gateway";
	static final String PG_PASSWORD_ENVIRONMENT_VARIABLE = "PGPASSWORD";
}
