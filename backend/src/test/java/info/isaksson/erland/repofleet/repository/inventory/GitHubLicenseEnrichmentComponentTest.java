package info.isaksson.erland.repofleet.repository.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import info.isaksson.erland.repofleet.github.api.GitHubApiCallExecutor;
import info.isaksson.erland.repofleet.github.auth.GitHubInstallationToken;
import info.isaksson.erland.repofleet.github.auth.GitHubInstallationTokenService;
import info.isaksson.erland.repofleet.github.client.GitHubContentItemResponse;
import info.isaksson.erland.repofleet.github.client.GitHubLicenseInfoResponse;
import info.isaksson.erland.repofleet.github.client.GitHubLicenseResponse;
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
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GitHubLicenseEnrichmentComponentTest {

    private GitHubRepositoryMetadataClient client;
    private GitHubLicenseEnrichmentComponent component;

    @BeforeEach
    void setUp() {
        GitHubInstallationTokenService tokenService = mock(GitHubInstallationTokenService.class);
        when(tokenService.getToken())
                .thenReturn(new GitHubInstallationToken(
                        "token",
                        Instant.parse("2026-09-20T10:00:00Z")));
        client = mock(GitHubRepositoryMetadataClient.class);
        component = new GitHubLicenseEnrichmentComponent(
                client,
                new GitHubApiCallExecutor(tokenService));
    }

    @Test
    void marksLicenseMissingWhenRootContainsNoLicenseFile() {
        when(client.getRootContents(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(List.of());

        RepositoryMetadataResult<LicenseStatus> result =
                component.enrich(repository(unknownLicense(), AnalysisState.NOT_ANALYZED), false);

        assertTrue(result.complete());
        assertFalse(result.degraded());
        assertEquals(LicensePresence.MISSING, result.value().presence());
        assertEquals(Boolean.FALSE, result.value().recognized());
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

        RepositoryMetadataResult<LicenseStatus> result =
                component.enrich(repository(unknownLicense(), AnalysisState.NOT_ANALYZED), false);

        assertTrue(result.complete());
        assertEquals(LicensePresence.PRESENT, result.value().presence());
        assertEquals(Boolean.TRUE, result.value().recognized());
        assertEquals("mit", result.value().key());
        assertEquals("MIT License", result.value().name());
    }

    @Test
    void treatsExistingUnrecognizedLicenseFileAsCustomLicense() {
        when(client.getRootContents(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(List.of(new GitHubContentItemResponse("LICENSE.md", "LICENSE.md", "file")));
        when(client.getLicense(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new WebApplicationException(Response.status(404).build()));

        RepositoryMetadataResult<LicenseStatus> result =
                component.enrich(repository(unknownLicense(), AnalysisState.NOT_ANALYZED), false);

        assertTrue(result.complete());
        assertFalse(result.degraded());
        assertEquals(LicensePresence.PRESENT, result.value().presence());
        assertEquals(Boolean.FALSE, result.value().recognized());
        assertEquals("Custom or unrecognized license", result.value().name());
    }

    @Test
    void retainsCompleteCachedLicenseWhenRefreshFails() {
        LicenseStatus cached = new LicenseStatus(
                AnalysisState.COMPLETE,
                LicensePresence.PRESENT,
                true,
                "mit",
                "MIT License");
        when(client.getRootContents(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new WebApplicationException(Response.status(503).build()));

        RepositoryMetadataResult<LicenseStatus> result =
                component.enrich(repository(cached, AnalysisState.COMPLETE), true);

        assertTrue(result.complete());
        assertTrue(result.degraded());
        assertEquals(cached, result.value());
        assertTrue(result.error().startsWith("license:"));
    }

    @Test
    void leavesLicenseUnknownWhenRefreshFailsWithoutUsableCache() {
        LicenseStatus unknown = unknownLicense();
        when(client.getRootContents(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("contents unavailable"));

        RepositoryMetadataResult<LicenseStatus> result =
                component.enrich(repository(unknown, AnalysisState.NOT_ANALYZED), false);

        assertFalse(result.complete());
        assertTrue(result.degraded());
        assertEquals(LicensePresence.UNKNOWN, result.value().presence());
        assertEquals(AnalysisState.NOT_ANALYZED, result.value().analysisState());
        assertNull(result.value().recognized());
    }

    private LicenseStatus unknownLicense() {
        return new LicenseStatus(
                AnalysisState.NOT_ANALYZED,
                LicensePresence.UNKNOWN,
                null,
                null,
                null);
    }

    private RepositorySummary repository(LicenseStatus license, AnalysisState state) {
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
                license,
                new GitHubActionsStatus(AnalysisState.NOT_ANALYZED, null, null),
                new ReleaseStatus(AnalysisState.NOT_ANALYZED, null, null, null, null, null),
                new ActivityStatus(null, null),
                new RepositoryRefreshStatus(state, null));
    }
}
