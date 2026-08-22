package com.zhihuiji.backend.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LegacySQLiteImportServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void acceptsReadableFileInsideConfiguredImportRoot() throws Exception {
        Path importRoot = Files.createDirectory(tempDir.resolve("imports"));
        Path database = Files.writeString(importRoot.resolve("legacy.db"), "sqlite");

        assertEquals(database.toRealPath(), LegacySQLiteImportService.resolveImportPath(importRoot, database.toString()));
    }

    @Test
    void rejectsReadableFileOutsideConfiguredImportRoot() throws Exception {
        Path importRoot = Files.createDirectory(tempDir.resolve("imports"));
        Path outside = Files.writeString(tempDir.resolve("outside.db"), "sqlite");

        assertThrows(
            IllegalArgumentException.class,
            () -> LegacySQLiteImportService.resolveImportPath(importRoot, outside.toString())
        );
    }

    @Test
    void rejectsSymlinkThatEscapesConfiguredImportRoot() throws Exception {
        Path importRoot = Files.createDirectory(tempDir.resolve("imports"));
        Path outside = Files.writeString(tempDir.resolve("outside.db"), "sqlite");
        Path link = Files.createSymbolicLink(importRoot.resolve("linked.db"), outside);

        assertThrows(
            IllegalArgumentException.class,
            () -> LegacySQLiteImportService.resolveImportPath(importRoot, link.toString())
        );
    }
}
