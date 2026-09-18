package info.isaksson.erland.repofleet.repository.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import info.isaksson.erland.repofleet.repository.api.AnalysisState;
import info.isaksson.erland.repofleet.repository.api.RepositoryVisibility;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class CachedRepositoryInventoryServiceTest {

    @Inject
    RepositoryIdentityRepository repository;

    @Inject
    CachedRepositoryInventoryService cachedInventory;

    @BeforeEach
    @Transactional
    void clearDatabase() {
        repository.deleteAll();
    }

    @Test
    @Transactional
    void loadsOnlyActiveRepositoriesFromPostgres() {
        repository.insert(
                2L,
                "erland",
                "two",
                "erland/two",
                RepositoryVisibility.PRIVATE,
                false,
                false,
                "main",
                Instant.parse("2026-09-17T12:05:00Z"),
                Instant.parse("2026-09-17T12:00:00Z"),
                Instant.parse("2026-09-18T08:00:00Z"));
        repository.insert(
                1L,
                "erland",
                "one",
                "erland/one",
                RepositoryVisibility.PUBLIC,
                false,
                false,
                "trunk",
                null,
                null,
                Instant.parse("2026-09-18T08:00:00Z"));
        repository.update(
                2L,
                "erland",
                "two",
                "erland/two",
                RepositoryVisibility.PRIVATE,
                false,
                false,
                "main",
                Instant.parse("2026-09-17T12:05:00Z"),
                Instant.parse("2026-09-17T12:00:00Z"),
                Instant.parse("2026-09-18T09:00:00Z"),
                false);

        var result = cachedInventory.loadActiveRepositories();

        assertEquals(1, result.size());
        assertEquals(1L, result.getFirst().id());
        assertEquals("erland/one", result.getFirst().fullName());
        assertEquals("https://github.com/erland/one", result.getFirst().url());
        assertEquals("trunk", result.getFirst().defaultBranch());
        assertEquals(AnalysisState.NOT_ANALYZED, result.getFirst().refreshStatus().state());
        assertTrue(result.getFirst().topics().isEmpty());
        assertTrue(result.getFirst().languages().isEmpty());
    }

    @Test
    @Transactional
    void returnsRepositoriesSortedByFullName() {
        repository.insert(
                20L,
                "erland",
                "zeta",
                "erland/zeta",
                RepositoryVisibility.PUBLIC,
                false,
                false,
                "main",
                null,
                null,
                Instant.parse("2026-09-18T08:00:00Z"));
        repository.insert(
                10L,
                "erland",
                "alpha",
                "erland/alpha",
                RepositoryVisibility.PUBLIC,
                false,
                false,
                "main",
                null,
                null,
                Instant.parse("2026-09-18T08:00:00Z"));

        List<String> names = cachedInventory.loadActiveRepositories().stream()
                .map(repository -> repository.fullName())
                .toList();

        assertEquals(List.of("erland/alpha", "erland/zeta"), names);
    }
}
