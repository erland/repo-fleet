package info.isaksson.erland.repofleet.repository.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import info.isaksson.erland.repofleet.github.api.GitHubApiCallExecutor;
import info.isaksson.erland.repofleet.github.auth.GitHubInstallationToken;
import info.isaksson.erland.repofleet.github.auth.GitHubInstallationTokenService;
import info.isaksson.erland.repofleet.github.client.GitHubRepositoryMetadataClient;
import info.isaksson.erland.repofleet.github.client.GitHubTopicsResponse;
import info.isaksson.erland.repofleet.repository.api.ActivityStatus;
import info.isaksson.erland.repofleet.repository.api.AnalysisState;
import info.isaksson.erland.repofleet.repository.api.GitHubActionsStatus;
import info.isaksson.erland.repofleet.repository.api.LicensePresence;
import info.isaksson.erland.repofleet.repository.api.LicenseStatus;
import info.isaksson.erland.repofleet.repository.api.ReleaseStatus;
import info.isaksson.erland.repofleet.repository.api.RepositoryRefreshStatus;
import info.isaksson.erland.repofleet.repository.api.RepositorySummary;
import info.isaksson.erland.repofleet.repository.api.RepositoryVisibility;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GitHubTopicsEnrichmentComponentTest {

    private GitHubRepositoryMetadataClient client;
    private GitHubTopicsEnrichmentComponent component;

    @BeforeEach
    void setUp() {
        GitHubInstallationTokenService tokenService = mock(GitHubInstallationTokenService.class);
        when(tokenService.getToken())
                .thenReturn(new GitHubInstallationToken("token", Instant.parse("2026-09-20T10:00:00Z")));
        client = mock(GitHubRepositoryMetadataClient.class);
        component = new GitHubTopicsEnrichmentComponent(
                client,
                new GitHubApiCallExecutor(tokenService));
    }

    @Test
    void sortsTopicsFromGitHub() {
        when(client.getTopics(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new GitHubTopicsResponse(List.of("quarkus", "architecture")));

        RepositoryMetadataResult<List<String>> result =
                component.enrich(repository(List.of(), AnalysisState.NOT_ANALYZED), false, true);

        assertTrue(result.complete());
        assertFalse(result.degraded());
        assertEquals(List.of("architecture", "quarkus"), result.value());
    }

    @Test
    void retainsCompleteCachedTopicsWhenRefreshFails() {
        when(client.getTopics(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new WebApplicationException(Response.status(503).build()));

        RepositoryMetadataResult<List<String>> result =
                component.enrich(repository(List.of("cached-topic"), AnalysisState.COMPLETE), true, true);

        assertTrue(result.complete());
        assertTrue(result.degraded());
        assertEquals(List.of("cached-topic"), result.value());
        assertTrue(result.error().startsWith("topics:"));
    }

    @Test
    void marksRepositoryUnavailableOnNotFound() {
        when(client.getTopics(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new WebApplicationException(Response.status(404).build()));

        RepositoryMetadataResult<List<String>> result =
                component.enrich(repository(List.of("cached-topic"), AnalysisState.COMPLETE), true, true);

        assertTrue(result.unavailable());
        assertFalse(result.complete());
    }

    private RepositorySummary repository(List<String> topics, AnalysisState state) {
        return new RepositorySummary(
                1L,
                "erland",
                "repo-fleet",
                "erland/repo-fleet",
                "https://github.com/erland/repo-fleet",
                RepositoryVisibility.PRIVATE,
                false,
                false,
                "main",
                topics,
                List.of(),
                null,
                new LicenseStatus(AnalysisState.NOT_ANALYZED, LicensePresence.UNKNOWN, null, null, null),
                new GitHubActionsStatus(AnalysisState.NOT_ANALYZED, null, null),
                new ReleaseStatus(AnalysisState.NOT_ANALYZED, null, null, null, null, null),
                new ActivityStatus(null, null),
                new RepositoryRefreshStatus(state, null));
    }
}
