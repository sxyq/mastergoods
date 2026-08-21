package com.zhihuiji.backend.infrastructure.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class RepositoryOwnerScopeAuditTest {
    private static final Path REPOSITORY_DIR = Path.of("src/main/java/com/zhihuiji/backend/infrastructure/repository");
    private static final Set<String> GLOBAL_SYSTEM_REPOSITORIES = Set.of(
        "SessionRepository.java",
        "UserRepository.java"
    );

    @Test
    void businessRepositoriesExposeOwnerScopedQueriesOrDocumentedSystemExceptions() throws IOException {
        List<Path> repositories;
        try (var stream = Files.list(REPOSITORY_DIR)) {
            repositories = stream
                .filter(path -> path.getFileName().toString().endsWith("Repository.java"))
                .sorted()
                .collect(Collectors.toList());
        }

        assertFalse(repositories.isEmpty(), "repository scan should not be empty");

        for (Path repository : repositories) {
            String fileName = repository.getFileName().toString();
            String source = Files.readString(repository);
            if ("ImportJobRepository.java".equals(fileName)) {
                assertTrue(
                    source.toLowerCase().contains("system-level worker"),
                    () -> fileName + " must keep the documented system-level worker exception comment"
                );
                continue;
            }
            if (GLOBAL_SYSTEM_REPOSITORIES.contains(fileName)) {
                continue;
            }
            assertTrue(
                source.contains("ownerUserId") || source.contains("owner_user_id"),
                () -> fileName + " should include owner-scoped query constraints"
            );
        }
    }
}
