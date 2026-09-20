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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GitHubLanguagesEnrichmentComponentTest {

    private GitHubRepositoryMetadataClient client;
    private GitHubLanguagesEnrichmentComponent component;

    @BeforeEach
    void setUp() {
        GitHubInstallationTokenService tokenService = mock(GitHubInstallationTokenService.class);
        when(tokenService.getToken())
                .thenReturn(new GitHubInstallationToken("token", Instant.parse("2026-09-20T10:00:00Z")));
        client = mock(GitHubRepositoryMetadataClient.class);
        component = new GitHubLanguagesEnrichmentComponent(
                client,
                new GitHubApiCallExecutor(tokenService));
    }

    @Test
    void sortsLanguagesAndSelectsLargestAsPrimary() {
        Map<String, Long> languages = new LinkedHashMap<>();
        languages.put("TypeScript", 1_000L);
        languages.put("Java", 5_000L);
        when(client.getLanguages(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(languages);

        RepositoryMetadataResult<RepositoryLanguageMetadata> result =
                component.enrich(repository(List.of(), null, AnalysisState.NOT_ANALYZED), false, true);

        assertTrue(result.complete());
        assertFalse(result.degraded());
        assertEquals(List.of("Java", "TypeScript"), result.value().languages());
        assertEquals("Java", result.value().primaryLanguage());
    }

    @Test
    void retainsCompleteCachedLanguagesWhenRefreshFails() {
        when(client.getLanguages(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new WebApplicationException(Response.status(503).build()));

        RepositoryMetadataResult<RepositoryLanguageMetadata> result =
                component.enrich(repository(List.of("Java"), "Java", AnalysisState.COMPLETE), true, true);

        assertTrue(result.complete());
        assertTrue(result.degraded());
        assertEquals(List.of("Java"), result.value().languages());
        assertEquals("Java", result.value().primaryLanguage());
        assertTrue(result.error().startsWith("languages:"));
    }

    private RepositorySummary repository(
            List<String> languages,
            String primaryLanguage,
            AnalysisState state) {
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
                List.of(),
                languages,
                primaryLanguage,
                new LicenseStatus(AnalysisState.NOT_ANALYZED, LicensePresence.UNKNOWN, null, null, null),
                new GitHubActionsStatus(AnalysisState.NOT_ANALYZED, null, null),
                new ReleaseStatus(AnalysisState.NOT_ANALYZED, null, null, null, null, null),
                new ActivityStatus(null, null),
                new RepositoryRefreshStatus(state, null));
    }
}
