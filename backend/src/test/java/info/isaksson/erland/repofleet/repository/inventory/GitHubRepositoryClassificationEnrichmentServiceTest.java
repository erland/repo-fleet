package info.isaksson.erland.repofleet.repository.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import info.isaksson.erland.repofleet.github.auth.GitHubInstallationToken;
import info.isaksson.erland.repofleet.github.auth.GitHubInstallationTokenService;
import info.isaksson.erland.repofleet.github.client.GitHubContentItemResponse;
import info.isaksson.erland.repofleet.github.client.GitHubLicenseInfoResponse;
import info.isaksson.erland.repofleet.github.client.GitHubLicenseResponse;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GitHubRepositoryClassificationEnrichmentServiceTest {

    private GitHubInstallationTokenService tokenService;
    private GitHubRepositoryMetadataClient client;
    private GitHubRepositoryClassificationEnrichmentService service;

    @BeforeEach
    void setUp() {
        tokenService = mock(GitHubInstallationTokenService.class);
        client = mock(GitHubRepositoryMetadataClient.class);
        when(tokenService.getToken())
                .thenReturn(new GitHubInstallationToken("token", Instant.parse("2026-08-14T10:00:00Z")));
        when(client.getTopics(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new GitHubTopicsResponse(List.of()));
        when(client.getLanguages(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Map.of());
        when(client.getRootContents(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(List.of());

        service = new GitHubRepositoryClassificationEnrichmentService(tokenService, client);
    }

    @Test
    void enrichesTopicsLanguagesAndPrimaryLanguage() {
        when(client.getTopics(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new GitHubTopicsResponse(List.of("quarkus", "analytics")));

        Map<String, Long> languages = new LinkedHashMap<>();
        languages.put("TypeScript", 1_000L);
        languages.put("Java", 5_000L);
        when(client.getLanguages(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(languages);

        RepositorySummary enriched = service.enrich(repository());

        assertEquals(List.of("analytics", "quarkus"), enriched.topics());
        assertEquals(List.of("Java", "TypeScript"), enriched.languages());
        assertEquals("Java", enriched.primaryLanguage());
        assertEquals(AnalysisState.PARTIAL, enriched.refreshStatus().state());
    }

    @Test
    void supportsRepositoriesWithoutTopicsOrLanguages() {
        when(client.getTopics(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new GitHubTopicsResponse(List.of()));
        when(client.getLanguages(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Map.of());

        RepositorySummary enriched = service.enrich(repository());

        assertEquals(List.of(), enriched.topics());
        assertEquals(List.of(), enriched.languages());
        assertNull(enriched.primaryLanguage());
        assertEquals(AnalysisState.PARTIAL, enriched.refreshStatus().state());
    }

    @Test
    void keepsSuccessfulTopicsWhenLanguageLookupFails() {
        when(client.getTopics(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new GitHubTopicsResponse(List.of("java")));
        when(client.getLanguages(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("languages unavailable"));

        RepositorySummary enriched = service.enrich(repository());

        assertEquals(List.of("java"), enriched.topics());
        assertEquals(List.of(), enriched.languages());
        assertNull(enriched.primaryLanguage());
        assertEquals(AnalysisState.PARTIAL, enriched.refreshStatus().state());
    }

    @Test
    void marksRepositoryPartialWhenTopicsAndLanguagesFailButLicenseAnalysisSucceeds() {
        when(client.getTopics(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("topics unavailable"));
        when(client.getLanguages(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("languages unavailable"));

        RepositorySummary enriched = service.enrich(repository());

        assertEquals(AnalysisState.PARTIAL, enriched.refreshStatus().state());
        assertEquals(List.of(), enriched.topics());
        assertEquals(List.of(), enriched.languages());
        assertEquals(AnalysisState.COMPLETE, enriched.license().analysisState());
    }


    @Test
    void marksLicenseMissingWhenNoLicenseFileExists() {
        RepositorySummary enriched = service.enrich(repository());

        assertEquals(LicensePresence.MISSING, enriched.license().presence());
        assertEquals(Boolean.FALSE, enriched.license().recognized());
        assertEquals(AnalysisState.COMPLETE, enriched.license().analysisState());
    }

    @Test
    void identifiesRecognizedLicense() {
        when(client.getRootContents(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(List.of(new GitHubContentItemResponse("LICENSE", "LICENSE", "file")));
        when(client.getLicense(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new GitHubLicenseResponse(
                        "LICENSE",
                        "LICENSE",
                        new GitHubLicenseInfoResponse("mit", "MIT License", "MIT")));

        RepositorySummary enriched = service.enrich(repository());

        assertEquals(LicensePresence.PRESENT, enriched.license().presence());
        assertEquals(Boolean.TRUE, enriched.license().recognized());
        assertEquals("mit", enriched.license().key());
        assertEquals("MIT License", enriched.license().name());
        assertEquals(AnalysisState.COMPLETE, enriched.license().analysisState());
    }

    @Test
    void identifiesCustomLicenseWhenFileExistsButGitHubDoesNotRecognizeIt() {
        when(client.getRootContents(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(List.of(new GitHubContentItemResponse("LICENSE.md", "LICENSE.md", "file")));
        when(client.getLicense(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new WebApplicationException(Response.status(404).build()));

        RepositorySummary enriched = service.enrich(repository());

        assertEquals(LicensePresence.PRESENT, enriched.license().presence());
        assertEquals(Boolean.FALSE, enriched.license().recognized());
        assertEquals("Custom or unrecognized license", enriched.license().name());
        assertEquals(AnalysisState.COMPLETE, enriched.license().analysisState());
    }

    @Test
    void keepsLicenseUnknownWhenLicenseAnalysisFails() {
        when(client.getRootContents(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("contents unavailable"));

        RepositorySummary enriched = service.enrich(repository());

        assertEquals(LicensePresence.UNKNOWN, enriched.license().presence());
        assertEquals(AnalysisState.NOT_ANALYZED, enriched.license().analysisState());
        assertEquals(AnalysisState.PARTIAL, enriched.refreshStatus().state());
    }


    @Test
    void marksRepositoryFailedWhenAllStep8EnrichmentLookupsFail() {
        when(client.getTopics(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("topics unavailable"));
        when(client.getLanguages(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("languages unavailable"));
        when(client.getRootContents(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("contents unavailable"));

        RepositorySummary enriched = service.enrich(repository());

        assertEquals(AnalysisState.FAILED, enriched.refreshStatus().state());
        assertEquals(AnalysisState.NOT_ANALYZED, enriched.license().analysisState());
    }

    private RepositorySummary repository() {
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
                List.of(),
                null,
                new LicenseStatus(AnalysisState.NOT_ANALYZED, LicensePresence.UNKNOWN, null, null, null),
                new GitHubActionsStatus(AnalysisState.NOT_ANALYZED, null, null),
                new ReleaseStatus(AnalysisState.NOT_ANALYZED, null, null, null, null, null),
                new ActivityStatus(null, null),
                new RepositoryRefreshStatus(AnalysisState.NOT_ANALYZED, "pending"));
    }
}
