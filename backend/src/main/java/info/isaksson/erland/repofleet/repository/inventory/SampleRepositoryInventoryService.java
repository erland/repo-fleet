package info.isaksson.erland.repofleet.repository.inventory;

import info.isaksson.erland.repofleet.repository.api.ActivityStatus;
import info.isaksson.erland.repofleet.repository.api.AnalysisState;
import info.isaksson.erland.repofleet.repository.api.GitHubActionsStatus;
import info.isaksson.erland.repofleet.repository.api.LicensePresence;
import info.isaksson.erland.repofleet.repository.api.LicenseStatus;
import info.isaksson.erland.repofleet.repository.api.ReleaseStatus;
import info.isaksson.erland.repofleet.repository.api.RepositoryRefreshStatus;
import info.isaksson.erland.repofleet.repository.api.RepositorySummary;
import info.isaksson.erland.repofleet.repository.api.RepositoryVisibility;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class SampleRepositoryInventoryService implements RepositoryInventoryService {

    private static final List<RepositorySummary> SAMPLE_REPOSITORIES = List.of(
        new RepositorySummary(
            1001L,
            "erland",
            "roman-nollpunkten",
            "erland/roman-nollpunkten",
            "https://github.com/erland/roman-nollpunkten",
            RepositoryVisibility.PRIVATE,
            false,
            false,
            "main",
            List.of("novel", "publishing"),
            List.of("Python", "Markdown"),
            "Python",
            new LicenseStatus(AnalysisState.COMPLETE, LicensePresence.PRESENT, true, "mit", "MIT License"),
            new GitHubActionsStatus(AnalysisState.COMPLETE, true, 3),
            new ReleaseStatus(
                AnalysisState.COMPLETE,
                true,
                "v1.2.0",
                "v1.2.0",
                Instant.parse("2026-08-10T17:30:00Z"),
                false
            ),
            new ActivityStatus(
                Instant.parse("2026-08-12T14:15:00Z"),
                Instant.parse("2026-08-12T14:16:30Z")
            ),
            new RepositoryRefreshStatus(AnalysisState.COMPLETE, null)
        ),
        new RepositorySummary(
            1002L,
            "erland",
            "board-game-naturreservatet",
            "erland/board-game-naturreservatet",
            "https://github.com/erland/board-game-naturreservatet",
            RepositoryVisibility.PRIVATE,
            false,
            false,
            "main",
            List.of("board-game"),
            List.of("Python", "SVG"),
            "Python",
            new LicenseStatus(AnalysisState.COMPLETE, LicensePresence.MISSING, false, null, null),
            new GitHubActionsStatus(AnalysisState.COMPLETE, false, 0),
            new ReleaseStatus(AnalysisState.COMPLETE, false, null, null, null, null),
            new ActivityStatus(
                Instant.parse("2026-07-20T09:00:00Z"),
                Instant.parse("2026-07-20T09:05:00Z")
            ),
            new RepositoryRefreshStatus(AnalysisState.COMPLETE, null)
        ),
        new RepositorySummary(
            1003L,
            "erland",
            "legacy-java-tool",
            "erland/legacy-java-tool",
            "https://github.com/erland/legacy-java-tool",
            RepositoryVisibility.PUBLIC,
            true,
            false,
            "master",
            List.of("java", "archive"),
            List.of("Java"),
            "Java",
            new LicenseStatus(AnalysisState.FAILED, LicensePresence.UNKNOWN, null, null, null),
            new GitHubActionsStatus(AnalysisState.PARTIAL, null, null),
            new ReleaseStatus(AnalysisState.NOT_ANALYZED, null, null, null, null, null),
            new ActivityStatus(
                Instant.parse("2021-03-11T08:00:00Z"),
                Instant.parse("2021-03-11T08:05:00Z")
            ),
            new RepositoryRefreshStatus(
                AnalysisState.PARTIAL,
                "Some maintenance metadata could not be analyzed."
            )
        )
    );

    @Override
    public List<RepositorySummary> listRepositories() {
        return SAMPLE_REPOSITORIES;
    }
}
