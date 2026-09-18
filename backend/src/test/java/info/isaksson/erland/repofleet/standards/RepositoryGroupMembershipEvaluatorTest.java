package info.isaksson.erland.repofleet.standards;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
import java.util.List;
import org.junit.jupiter.api.Test;

class RepositoryGroupMembershipEvaluatorTest {

    private final RepositoryGroupMembershipEvaluator evaluator =
            new RepositoryGroupMembershipEvaluator();

    @Test
    void matchesAllConfiguredSelectorsDeterministically() {
        RepositoryGroupSelector selector = new RepositoryGroupSelector(
                "svc-",
                "Erland",
                RepositoryVisibility.PRIVATE,
                false,
                false,
                List.of("Architecture", "backend"),
                List.of("java"));

        assertTrue(evaluator.matches(selector, repository()));
    }

    @Test
    void rejectsRepositoryWhenAnySelectorDoesNotMatch() {
        RepositoryGroupSelector selector = new RepositoryGroupSelector(
                "svc-",
                "erland",
                RepositoryVisibility.PRIVATE,
                false,
                true,
                List.of("architecture"),
                List.of("Java"));

        assertFalse(evaluator.matches(selector, repository()));
    }

    @Test
    void emptySelectorMatchesRepository() {
        RepositoryGroupSelector selector = new RepositoryGroupSelector(
                null, null, null, null, null, List.of(), List.of());

        assertTrue(evaluator.matches(selector, repository()));
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
                List.of("architecture", "Backend"),
                List.of("Java", "TypeScript"),
                "Java",
                new LicenseStatus(AnalysisState.COMPLETE, LicensePresence.PRESENT, true, "mit", "MIT"),
                new GitHubActionsStatus(AnalysisState.COMPLETE, true, 1),
                new ReleaseStatus(AnalysisState.COMPLETE, true, "v1", "v1", null, false),
                new ActivityStatus(null, null),
                new RepositoryRefreshStatus(AnalysisState.COMPLETE, "complete"));
    }
}
