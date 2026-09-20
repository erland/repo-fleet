package info.isaksson.erland.repofleet.repository.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import info.isaksson.erland.repofleet.github.api.GitHubApiCallExecutor;
import info.isaksson.erland.repofleet.github.auth.GitHubInstallationToken;
import info.isaksson.erland.repofleet.github.auth.GitHubInstallationTokenService;
import info.isaksson.erland.repofleet.github.client.GitHubRepositoryMetadataClient;
import info.isaksson.erland.repofleet.github.client.GitHubWorkflowsResponse;
import info.isaksson.erland.repofleet.repository.api.ActivityStatus;
import info.isaksson.erland.repofleet.repository.api.AnalysisState;
import info.isaksson.erland.repofleet.repository.api.GitHubActionsStatus;
import info.isaksson.erland.repofleet.repository.api.LicensePresence;
import info.isaksson.erland.repofleet.repository.api.LicenseStatus;
import info.isaksson.erland.repofleet.repository.api.ReleaseStatus;
import info.isaksson.erland.repofleet.repository.api.RepositoryRefreshStatus;
import info.isaksson.erland.repofleet.repository.api.RepositorySummary;
import info.isaksson.erland.repofleet.repository.api.RepositoryVisibility;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GitHubActionsEnrichmentComponentTest {

    private GitHubRepositoryMetadataClient client;
    private GitHubActionsEnrichmentComponent component;

    @BeforeEach
    void setUp() {
        GitHubInstallationTokenService tokenService = mock(GitHubInstallationTokenService.class);
        when(tokenService.getToken()).thenReturn(
                new GitHubInstallationToken("token", Instant.parse("2026-09-20T10:00:00Z")));
        client = mock(GitHubRepositoryMetadataClient.class);
        component = new GitHubActionsEnrichmentComponent(client, new GitHubApiCallExecutor(tokenService));
    }

    @Test
    void reportsWorkflowPresenceAndCount() {
        when(client.getWorkflows(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(new GitHubWorkflowsResponse(3));

        RepositoryMetadataResult<GitHubActionsStatus> result =
                component.enrich(repository(actionsUnknown()), false);

        assertTrue(result.complete());
        assertFalse(result.degraded());
        assertEquals(Boolean.TRUE, result.value().workflowsPresent());
        assertEquals(3, result.value().workflowCount());
    }

    @Test
    void reportsNoWorkflowsWithoutCallingThatUnknown() {
        when(client.getWorkflows(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(new GitHubWorkflowsResponse(0));

        RepositoryMetadataResult<GitHubActionsStatus> result =
                component.enrich(repository(actionsUnknown()), false);

        assertTrue(result.complete());
        assertEquals(Boolean.FALSE, result.value().workflowsPresent());
        assertEquals(0, result.value().workflowCount());
    }

    @Test
    void retainsCompleteCachedActionsOnTransientFailure() {
        GitHubActionsStatus cached = new GitHubActionsStatus(AnalysisState.COMPLETE, true, 2);
        when(client.getWorkflows(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenThrow(new IllegalStateException("actions unavailable"));

        RepositoryMetadataResult<GitHubActionsStatus> result =
                component.enrich(repository(cached), true);

        assertTrue(result.complete());
        assertTrue(result.degraded());
        assertEquals(cached, result.value());
    }

    @Test
    void failedActionsAnalysisDoesNotClaimWorkflowsAreMissing() {
        when(client.getWorkflows(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenThrow(new IllegalStateException("actions unavailable"));

        RepositoryMetadataResult<GitHubActionsStatus> result =
                component.enrich(repository(actionsUnknown()), false);

        assertFalse(result.complete());
        assertTrue(result.degraded());
        assertEquals(AnalysisState.FAILED, result.value().analysisState());
        assertNull(result.value().workflowsPresent());
        assertNull(result.value().workflowCount());
    }

    private GitHubActionsStatus actionsUnknown() {
        return new GitHubActionsStatus(AnalysisState.NOT_ANALYZED, null, null);
    }

    private RepositorySummary repository(GitHubActionsStatus actions) {
        return new RepositorySummary(
                1L, "erland", "repo-fleet", "erland/repo-fleet",
                "https://github.com/erland/repo-fleet", RepositoryVisibility.PRIVATE,
                false, false, "main", List.of(), List.of(), null,
                new LicenseStatus(AnalysisState.NOT_ANALYZED, LicensePresence.UNKNOWN, null, null, null),
                actions,
                new ReleaseStatus(AnalysisState.NOT_ANALYZED, null, null, null, null, null),
                new ActivityStatus(null, null),
                new RepositoryRefreshStatus(AnalysisState.NOT_ANALYZED, null));
    }
}
