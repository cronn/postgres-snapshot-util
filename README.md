[![CI](https://github.com/cronn/postgres-snapshot-util/workflows/CI/badge.svg)](https://github.com/cronn/postgres-snapshot-util/actions)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.cronn/postgres-snapshot-util/badge.svg)](http://maven-badges.herokuapp.com/maven-central/de.cronn/postgres-snapshot-util)
[![Apache 2.0](https://img.shields.io/github/license/cronn/postgres-snapshot-util.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![codecov](https://codecov.io/gh/cronn/postgres-snapshot-util/branch/main/graph/badge.svg?token=KD1WJK5ZFK)](https://codecov.io/gh/cronn/postgres-snapshot-util)
[![Valid Gradle Wrapper](https://github.com/cronn/postgres-snapshot-util/workflows/Validate%20Gradle%20Wrapper/badge.svg)](https://github.com/cronn/postgres-snapshot-util/actions/workflows/gradle-wrapper-validation.yml)

# Snapshot Utilities for PostgreSQL #

This library provides Java wrappers to run `pg_dump` and `pg_restore` on platforms that do not have this binary installed.
We build on top of [Testcontainers](testcontainers) to spin-up a temporary Docker container that executes the command.

## Usage ##

Add the following Maven dependency to your project:

```xml
<dependency>
    <groupId>de.cronn</groupId>
    <artifactId>postgres-snapshot-util</artifactId>
    <version>1.0</version>
</dependency>
```

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

## Use Cases ##

### Integration / Regression Test ###

`PostgresDump` was designed to be used in a JUnit (regression) tests to dump and compare the actual database schema
of an application in cases where the schema is managed by a library/framework such as Liquibase.
We recommend to use our [validation-file-assertions] library to write such a test.

Full example:

```java
@SpringBootTest
@Testcontainers
class SchemaTest implements JUnit5ValidationFileAssertions {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.1");

    @Test
    void schemaExport() {
        String schema = PostgresDump.dumpToString(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword(),
                                                  PostgresDumpOption.SCHEMA_ONLY);
        assertWithFile(schema);
    }
}
```

## Requirements ##

- Java 17+

[testcontainers]: https://testcontainers.com/
[validation-file-assertions]: https://github.com/cronn/validation-file-assertions
