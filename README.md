[![CI](https://github.com/cronn/postgres-snapshot-util/workflows/CI/badge.svg)](https://github.com/cronn/postgres-snapshot-util/actions)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.cronn/postgres-snapshot-util/badge.svg)](http://maven-badges.herokuapp.com/maven-central/de.cronn/postgres-snapshot-util)
[![Apache 2.0](https://img.shields.io/github/license/cronn/postgres-snapshot-util.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![codecov](https://codecov.io/gh/cronn/postgres-snapshot-util/branch/main/graph/badge.svg?token=KD1WJK5ZFK)](https://codecov.io/gh/cronn/postgres-snapshot-util)
[![Valid Gradle Wrapper](https://github.com/cronn/postgres-snapshot-util/workflows/Validate%20Gradle%20Wrapper/badge.svg)](https://github.com/cronn/postgres-snapshot-util/actions/workflows/gradle-wrapper-validation.yml)

# Snapshot Utilities for PostgreSQL #

This library provides Java wrappers to run `pg_dump` and `pg_restore` on platforms that do not have this binary installed.
We build on top of [Testcontainers](testcontainers) to spin-up a temporary Docker container that executes the command.

## Usage ##

### Simple Schema Dump

```java
String jdbcUrl = "jdbc:postgresql://localhost/my-db";
String schemaDump = PostgresDump.dumpToString(jdbcUrl, "user", "password",
                                              PostgresDumpOption.NO_COMMENTS,
                                              PostgresDumpOption.SCHEMA_ONLY);
```

### Dump and Restore

```java
Path dumpFile = Path.of("/path/to/dump.tar");
String jdbcUrl = "jdbc:postgresql://localhost/my-db";
PostgresDump.dumpToFile(dumpFile, jdbcUrl, "user", "pass", PostgresDumpFormat.TAR);

PostgresRestore.restoreFromFile(dumpFile, jdbcUrl, "user", "pass",
                                PostgresRestoreOption.CLEAN,
                                PostgresRestoreOption.EXIT_ON_ERROR,
                                PostgresRestoreOption.SINGLE_TRANSACTION);
```

## Requirements ##

- Java 17+

[testcontainers]: https://testcontainers.com/
