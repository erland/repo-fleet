package info.isaksson.erland.repofleet.repository.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import info.isaksson.erland.repofleet.github.api.GitHubApiCallExecutor;
import info.isaksson.erland.repofleet.github.auth.GitHubInstallationToken;
import info.isaksson.erland.repofleet.github.auth.GitHubInstallationTokenService;
import info.isaksson.erland.repofleet.github.client.GitHubRepositoryMetadataClient;
import info.isaksson.erland.repofleet.github.client.GitHubWorkflowsResponse;
import info.isaksson.erland.repofleet.github.conditional.GitHubConditionalRequestExecutor;
import info.isaksson.erland.repofleet.github.conditional.GitHubConditionalResult;
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

class GitHubRepositoryConditionalEnrichmentServiceTest {

    private GitHubInstallationTokenService tokenService;
    private GitHubRepositoryMetadataClient client;
    private GitHubConditionalRequestExecutor conditionalRequests;

    @BeforeEach
    void setUp() {
        tokenService = mock(GitHubInstallationTokenService.class);
        client = mock(GitHubRepositoryMetadataClient.class);
        conditionalRequests = mock(GitHubConditionalRequestExecutor.class);

        when(tokenService.getToken())
                .thenReturn(new GitHubInstallationToken(
                        "token",
                        Instant.parse("2026-09-18T12:00:00Z")));
        when(client.getRootContents(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(List.of());
        when(client.getWorkflows(
                anyString(), anyString(), anyString(), anyString(), anyString(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(new GitHubWorkflowsResponse(0));
        when(client.getReleases(
                anyString(), anyString(), anyString(), anyString(), anyString(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(List.of());
    }

    @Test
    void notModifiedReusesCachedTopicsAndLanguages() {
        RepositorySummary cached = cachedRepository();

        when(conditionalRequests.execute(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq("topics"),
                anyString(),
                any(),
                any(),
                any(),
                any()))
                .thenReturn(GitHubConditionalResult.notModified(
                        List.of("cached-topic"),
                        "\"topics-etag\""));

        when(conditionalRequests.execute(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq("languages"),
                anyString(),
                any(),
                any(),
                any(),
                any()))
                .thenReturn(GitHubConditionalResult.notModified(
                        new RepositoryLanguageMetadata(
                                List.of("Java", "TypeScript"),
                                "Java"),
                        "\"languages-etag\""));

        var service = serviceWithConditionalRequests();

        RepositorySummary enriched = service.enrich(cached);

        assertEquals(List.of("cached-topic"), enriched.topics());
        assertEquals(List.of("Java", "TypeScript"), enriched.languages());
        assertEquals("Java", enriched.primaryLanguage());
        assertEquals(AnalysisState.COMPLETE, enriched.refreshStatus().state());
    }

    @Test
    void transientConditionalFailurePreservesCachedTopicsAndLanguages() {
        RepositorySummary cached = cachedRepository();

        when(conditionalRequests.execute(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq("topics"),
                anyString(),
                any(),
                any(),
                any(),
                any()))
                .thenThrow(new IllegalStateException("topics temporarily unavailable"));

        when(conditionalRequests.execute(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq("languages"),
                anyString(),
                any(),
                any(),
                any(),
                any()))
                .thenThrow(new IllegalStateException("languages temporarily unavailable"));

        var service = serviceWithConditionalRequests();

        RepositorySummary enriched = service.enrich(cached);

        assertEquals(List.of("cached-topic"), enriched.topics());
        assertEquals(List.of("Java", "TypeScript"), enriched.languages());
        assertEquals("Java", enriched.primaryLanguage());
        assertEquals(AnalysisState.COMPLETE, enriched.refreshStatus().state());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void notModifiedReusesCachedLicenseActionsAndRelease() {
        RepositorySummary cached = cachedRepository();

        when(conditionalRequests.execute(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq("root-contents"),
                anyString(),
                any(),
                any(),
                any(),
                any()))
                .thenReturn((GitHubConditionalResult) GitHubConditionalResult.notModified(
                        null,
                        "\"contents-etag\""));

        when(conditionalRequests.execute(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq("workflows"),
                anyString(),
                any(),
                any(),
                any(),
                any()))
                .thenReturn((GitHubConditionalResult) GitHubConditionalResult.notModified(
                        null,
                        "\"workflows-etag\""));

        when(conditionalRequests.execute(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq("releases"),
                anyString(),
                any(),
                any(),
                any(),
                any()))
                .thenReturn((GitHubConditionalResult) GitHubConditionalResult.notModified(
                        null,
                        "\"releases-etag\""));

        var service = serviceWithConditionalRequests();

        RepositorySummary enriched = service.enrich(cached);

        assertEquals(cached.license(), enriched.license());
        assertEquals(cached.githubActions(), enriched.githubActions());
        assertEquals(cached.release(), enriched.release());
        assertEquals(AnalysisState.COMPLETE, enriched.refreshStatus().state());
    }

    @Test
    void transientFailuresPreserveCachedLicenseActionsAndRelease() {
        RepositorySummary cached = cachedRepository();

        when(conditionalRequests.execute(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq("root-contents"),
                anyString(),
                any(),
                any(),
                any(),
                any()))
                .thenThrow(new IllegalStateException("contents temporarily unavailable"));

        when(conditionalRequests.execute(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq("workflows"),
                anyString(),
                any(),
                any(),
                any(),
                any()))
                .thenThrow(new IllegalStateException("workflows temporarily unavailable"));

        when(conditionalRequests.execute(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq("releases"),
                anyString(),
                any(),
                any(),
                any(),
                any()))
                .thenThrow(new IllegalStateException("releases temporarily unavailable"));

        var service = serviceWithConditionalRequests();

        RepositorySummary enriched = service.enrich(cached);

        assertEquals(cached.license(), enriched.license());
        assertEquals(cached.githubActions(), enriched.githubActions());
        assertEquals(cached.release(), enriched.release());
        assertEquals(AnalysisState.COMPLETE, enriched.refreshStatus().state());
    }

    private GitHubRepositoryClassificationEnrichmentService serviceWithConditionalRequests() {
        GitHubApiCallExecutor apiCalls = new GitHubApiCallExecutor(tokenService);
        return new GitHubRepositoryClassificationEnrichmentService(
                new GitHubTopicsEnrichmentComponent(client, apiCalls, conditionalRequests),
                new GitHubLanguagesEnrichmentComponent(client, apiCalls, conditionalRequests),
                new GitHubLicenseEnrichmentComponent(client, apiCalls, conditionalRequests),
                new GitHubActionsEnrichmentComponent(client, apiCalls, conditionalRequests),
                new GitHubReleaseEnrichmentComponent(client, apiCalls, conditionalRequests));
    }

    private RepositorySummary cachedRepository() {
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
                List.of("cached-topic"),
                List.of("Java", "TypeScript"),
                "Java",
                new LicenseStatus(
                        AnalysisState.COMPLETE,
                        LicensePresence.MISSING,
                        false,
                        null,
                        null),
                new GitHubActionsStatus(
                        AnalysisState.COMPLETE,
                        false,
                        0),
                new ReleaseStatus(
                        AnalysisState.COMPLETE,
                        false,
                        null,
                        null,
                        null,
                        null),
                new ActivityStatus(null, null),
                new RepositoryRefreshStatus(
                        AnalysisState.COMPLETE,
                        "Repository enrichment complete."));
    }
}
