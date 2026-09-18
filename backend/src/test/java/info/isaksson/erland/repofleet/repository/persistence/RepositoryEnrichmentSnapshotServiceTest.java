package info.isaksson.erland.repofleet.repository.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
class RepositoryEnrichmentSnapshotServiceTest {

    @Inject
    RepositoryIdentityRepository identityRepository;

    @Inject
    RepositoryEnrichmentSnapshotRepository snapshotRepository;

    @Inject
    RepositoryEnrichmentSnapshotService snapshotService;

    @Inject
    CachedRepositoryInventoryService cachedInventory;

    @BeforeEach
    @Transactional
    void clearDatabase() {
        snapshotRepository.deleteAll();
        identityRepository.deleteAll();
    }

    @Test
    @Transactional
    void failedProgressiveRefreshPreservesPreviouslySuccessfulMetadata() {
        Instant seenAt = Instant.parse("2026-09-18T08:00:00Z");
        identityRepository.insert(
                1234L,
                "erland",
                "repo-fleet",
                "erland/repo-fleet",
                RepositoryVisibility.PUBLIC,
                false,
                false,
                "main",
                null,
                null,
                seenAt);

        RepositorySummary successful = new RepositorySummary(
                1234L,
                "erland",
                "repo-fleet",
                "erland/repo-fleet",
                "https://github.com/erland/repo-fleet",
                RepositoryVisibility.PUBLIC,
                false,
                false,
                "main",
                List.of("architecture"),
                List.of("Java"),
                "Java",
                new LicenseStatus(
                        AnalysisState.COMPLETE,
                        LicensePresence.PRESENT,
                        true,
                        "mit",
                        "MIT License"),
                new GitHubActionsStatus(AnalysisState.COMPLETE, true, 2),
                new ReleaseStatus(
                        AnalysisState.COMPLETE,
                        true,
                        "v1.0.0",
                        "v1.0.0",
                        Instant.parse("2026-09-17T12:00:00Z"),
                        false),
                new ActivityStatus(null, null),
                new RepositoryRefreshStatus(AnalysisState.COMPLETE, "complete"));

        Instant successfulAt = Instant.parse("2026-09-18T08:05:00Z");
        snapshotService.persistProgressiveResult(successful, successfulAt);

        RepositorySummary failed = new RepositorySummary(
                1234L,
                "erland",
                "repo-fleet",
                "erland/repo-fleet",
                "https://github.com/erland/repo-fleet",
                RepositoryVisibility.PUBLIC,
                false,
                false,
                "main",
                List.of(),
                List.of(),
                null,
                new LicenseStatus(
                        AnalysisState.NOT_ANALYZED,
                        LicensePresence.UNKNOWN,
                        null,
                        null,
                        null),
                new GitHubActionsStatus(AnalysisState.NOT_ANALYZED, null, null),
                new ReleaseStatus(AnalysisState.NOT_ANALYZED, null, null, null, null, null),
                new ActivityStatus(null, null),
                new RepositoryRefreshStatus(
                        AnalysisState.FAILED,
                        "Repository enrichment failed: GitHub unavailable"));

        snapshotService.persistProgressiveResult(
                failed,
                Instant.parse("2026-09-18T09:00:00Z"));

        RepositorySummary reconstructed = cachedInventory.loadActiveRepositories().getFirst();
        RepositoryEnrichmentSnapshot stored =
                snapshotRepository.findByGitHubRepositoryId(1234L).orElseThrow();

        assertEquals(List.of("architecture"), reconstructed.topics());
        assertEquals(List.of("Java"), reconstructed.languages());
        assertEquals("Java", reconstructed.primaryLanguage());
        assertEquals("mit", reconstructed.license().key());
        assertEquals("v1.0.0", reconstructed.release().latestReleaseTag());
        assertEquals(AnalysisState.FAILED, reconstructed.refreshStatus().state());
        assertEquals(successfulAt, stored.lastSuccessfulRefreshAt);
        assertEquals(
                "Repository enrichment failed: GitHub unavailable",
                stored.lastRelevantError);
    }

    @Test
    @Transactional
    void persistsAndReconstructsCompleteRepositorySummary() {
        Instant seenAt = Instant.parse("2026-09-18T08:00:00Z");
        identityRepository.insert(
                1234L,
                "erland",
                "repo-fleet",
                "erland/repo-fleet",
                RepositoryVisibility.PUBLIC,
                false,
                false,
                "main",
                Instant.parse("2026-09-18T07:45:00Z"),
                Instant.parse("2026-09-18T07:40:00Z"),
                seenAt);

        RepositorySummary summary = new RepositorySummary(
                1234L,
                "erland",
                "repo-fleet",
                "erland/repo-fleet",
                "https://github.com/erland/repo-fleet",
                RepositoryVisibility.PUBLIC,
                false,
                false,
                "main",
                List.of("architecture", "github"),
                List.of("Java", "TypeScript"),
                "Java",
                new LicenseStatus(
                        AnalysisState.COMPLETE,
                        LicensePresence.PRESENT,
                        true,
                        "mit",
                        "MIT License"),
                new GitHubActionsStatus(
                        AnalysisState.COMPLETE,
                        true,
                        4),
                new ReleaseStatus(
                        AnalysisState.COMPLETE,
                        true,
                        "v1.2.3",
                        "v1.2.3",
                        Instant.parse("2026-09-17T12:00:00Z"),
                        false),
                new ActivityStatus(
                        Instant.parse("2026-09-18T07:40:00Z"),
                        Instant.parse("2026-09-18T07:45:00Z")),
                new RepositoryRefreshStatus(
                        AnalysisState.COMPLETE,
                        "Enrichment complete."));

        Instant lastSuccess = Instant.parse("2026-09-18T08:05:00Z");
        snapshotService.saveSnapshot(summary, lastSuccess, "previous transient error");

        RepositorySummary reconstructed = cachedInventory.loadActiveRepositories().getFirst();
        RepositoryEnrichmentSnapshot stored =
                snapshotRepository.findByGitHubRepositoryId(1234L).orElseThrow();

        assertEquals(summary.id(), reconstructed.id());
        assertEquals(summary.fullName(), reconstructed.fullName());
        assertEquals(summary.topics(), reconstructed.topics());
        assertEquals(summary.languages(), reconstructed.languages());
        assertEquals(summary.primaryLanguage(), reconstructed.primaryLanguage());
        assertEquals(summary.license(), reconstructed.license());
        assertEquals(summary.githubActions(), reconstructed.githubActions());
        assertEquals(summary.release(), reconstructed.release());
        assertEquals(summary.activity(), reconstructed.activity());
        assertEquals(summary.refreshStatus(), reconstructed.refreshStatus());

        assertEquals(lastSuccess, stored.lastSuccessfulRefreshAt);
        assertEquals("previous transient error", stored.lastRelevantError);
        assertNotNull(stored.updatedAt);
        assertTrue(stored.topicsJson.contains("architecture"));
    }
}
