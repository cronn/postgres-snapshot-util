package de.cronn.postgres.snapshot.util;

import static org.assertj.core.api.Assertions.*;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PostgresRestoreTest extends BaseTest {

    @BeforeAll
    static void startPostgresContainer() {
        postgresContainer.start();
        jdbcUrl = "jdbc:postgresql://localhost:%d/test-db".formatted(postgresContainer.getFirstMappedPort());
    }

    @AfterAll
    static void stopPostgresContainer() {
        postgresContainer.stop();
    }

    @BeforeAll
    static void createTablesAndInsertData() {
        createSomeTableAndInsertData();
    }

    @Test
    void testDumpAndRestore(@TempDir Path tempDir) {
        String dumpBefore = PostgresDump.dumpToString(jdbcUrl, USERNAME, PASSWORD);

        Path dumpFile = tempDir.resolve("dump.tar");
        PostgresDump.dumpToFile(dumpFile, jdbcUrl, USERNAME, PASSWORD, PostgresDumpFormat.CUSTOM);

        PostgresRestore.restoreFromFile(dumpFile, jdbcUrl, USERNAME, PASSWORD,
                PostgresRestoreOption.CLEAN,
                PostgresRestoreOption.SINGLE_TRANSACTION);

        String dumpAfter = PostgresDump.dumpToString(jdbcUrl, USERNAME, PASSWORD);
        assertThat(dumpAfter).isEqualTo(dumpBefore);
    }
}
