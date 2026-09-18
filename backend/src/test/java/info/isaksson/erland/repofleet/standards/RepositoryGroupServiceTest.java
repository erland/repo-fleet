package info.isaksson.erland.repofleet.standards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import info.isaksson.erland.repofleet.repository.api.ActivityStatus;
import info.isaksson.erland.repofleet.repository.api.AnalysisState;
import info.isaksson.erland.repofleet.repository.api.GitHubActionsStatus;
import info.isaksson.erland.repofleet.repository.api.LicensePresence;
import info.isaksson.erland.repofleet.repository.api.LicenseStatus;
import info.isaksson.erland.repofleet.repository.api.ReleaseStatus;
import info.isaksson.erland.repofleet.repository.api.RepositoryRefreshStatus;
import info.isaksson.erland.repofleet.repository.api.RepositorySummary;
import info.isaksson.erland.repofleet.repository.api.RepositoryVisibility;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class RepositoryGroupServiceTest {

    @Inject
    RepositoryGroupService groups;

    @BeforeEach
    @Transactional
    void clearDatabase() {
        RepositoryGroup.deleteAll();
    }

    @Test
    void storesUpdatesAndResolvesMembership() {
        Instant created = Instant.parse("2026-09-18T09:00:00Z");
        Instant updated = Instant.parse("2026-09-18T10:00:00Z");

        groups.save(
                "services",
                "Services",
                "Service repositories",
                new RepositoryGroupSelector(
                        "svc-",
                        "erland",
                        RepositoryVisibility.PRIVATE,
                        false,
                        false,
                        List.of("architecture"),
                        List.of("Java")),
                true,
                created);

        groups.save(
                "services",
                "Backend services",
                "Updated",
                new RepositoryGroupSelector(
                        "svc-",
                        "erland",
                        RepositoryVisibility.PRIVATE,
                        false,
                        false,
                        List.of("architecture"),
                        List.of("Java")),
                true,
                updated);

        var all = groups.list();
        assertEquals(1, all.size());
        assertEquals("Backend services", all.getFirst().name());
        assertEquals(created, all.getFirst().createdAt());
        assertEquals(updated, all.getFirst().updatedAt());

        var matches = groups.matchingGroups(repository());
        assertEquals(1, matches.size());
        assertEquals("services", matches.getFirst().groupKey());
    }

    @Test
    void disabledGroupsDoNotResolveAsMembership() {
        groups.save(
                "disabled",
                "Disabled",
                null,
                new RepositoryGroupSelector(null, null, null, null, null, List.of(), List.of()),
                false,
                Instant.parse("2026-09-18T09:00:00Z"));

        assertTrue(groups.matchingGroups(repository()).isEmpty());
    }

    private RepositorySummary repository() {
        return new RepositorySummary(
                1L,
                "erland",
                "svc-orders",
                "erland/svc-orders",
                "https://github.com/erland/svc-orders",
                RepositoryVisibility.PRIVATE,
                false,
                false,
                "main",
                List.of("architecture"),
                List.of("Java"),
                "Java",
                new LicenseStatus(AnalysisState.COMPLETE, LicensePresence.PRESENT, true, "mit", "MIT"),
                new GitHubActionsStatus(AnalysisState.COMPLETE, true, 1),
                new ReleaseStatus(AnalysisState.COMPLETE, true, "v1", "v1", null, false),
                new ActivityStatus(null, null),
                new RepositoryRefreshStatus(AnalysisState.COMPLETE, "complete"));
    }
}
