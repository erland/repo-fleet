package info.isaksson.erland.repofleet.repository.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import info.isaksson.erland.repofleet.github.auth.GitHubInstallationToken;
import info.isaksson.erland.repofleet.github.auth.GitHubInstallationTokenService;
import info.isaksson.erland.repofleet.github.client.GitHubAppClient;
import info.isaksson.erland.repofleet.github.client.GitHubContentItemResponse;
import info.isaksson.erland.repofleet.github.client.GitHubLicenseInfoResponse;
import info.isaksson.erland.repofleet.github.client.GitHubLicenseResponse;
import info.isaksson.erland.repofleet.github.client.GitHubReleaseResponse;
import info.isaksson.erland.repofleet.github.client.GitHubRepositoryMetadataClient;
import info.isaksson.erland.repofleet.github.client.GitHubRepositoryOwnerResponse;
import info.isaksson.erland.repofleet.github.client.GitHubRepositoryResponse;
import info.isaksson.erland.repofleet.github.client.GitHubTopicsResponse;
import info.isaksson.erland.repofleet.github.client.GitHubWorkflowsResponse;
import info.isaksson.erland.repofleet.github.client.InstallationRepositoriesResponse;
import info.isaksson.erland.repofleet.repository.api.AnalysisState;
import info.isaksson.erland.repofleet.repository.api.LicensePresence;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class Phase1GitHubFixtureAcceptanceTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-08-14T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void refreshesDeterministicGitHubFixtureIntoCompletePhase1Inventory() {
        GitHubInstallationTokenService tokens = mock(GitHubInstallationTokenService.class);
        GitHubAppClient discoveryClient = mock(GitHubAppClient.class);
        GitHubRepositoryMetadataClient metadataClient = mock(GitHubRepositoryMetadataClient.class);

        when(tokens.getToken()).thenReturn(new GitHubInstallationToken(
                "fixture-token",
                Instant.parse("2026-08-14T13:00:00Z")));

        when(discoveryClient.listInstallationRepositories(
                anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(new InstallationRepositoriesResponse(
                        2,
                        List.of(
                                repository(101L, "roman-alpha"),
                                repository(102L, "roman-beta"))));

        when(metadataClient.getTopics(eq("erland"), eq("roman-alpha"), anyString(), anyString(), anyString()))
                .thenReturn(new GitHubTopicsResponse(List.of("novel", "publishing")));
        when(metadataClient.getTopics(eq("erland"), eq("roman-beta"), anyString(), anyString(), anyString()))
                .thenReturn(new GitHubTopicsResponse(List.of("novel")));

        when(metadataClient.getLanguages(eq("erland"), eq("roman-alpha"), anyString(), anyString(), anyString()))
                .thenReturn(Map.of("Python", 5000L, "Markdown", 500L));
        when(metadataClient.getLanguages(eq("erland"), eq("roman-beta"), anyString(), anyString(), anyString()))
                .thenReturn(Map.of("Java", 7000L, "Markdown", 300L));

        when(metadataClient.getRootContents(eq("erland"), eq("roman-alpha"), anyString(), anyString(), anyString()))
                .thenReturn(List.of(new GitHubContentItemResponse("LICENSE", "LICENSE", "file")));
        when(metadataClient.getRootContents(eq("erland"), eq("roman-beta"), anyString(), anyString(), anyString()))
                .thenReturn(List.of());

        when(metadataClient.getLicense(eq("erland"), eq("roman-alpha"), anyString(), anyString(), anyString()))
                .thenReturn(new GitHubLicenseResponse(
                        "LICENSE",
                        "LICENSE",
                        new GitHubLicenseInfoResponse("mit", "MIT License", "MIT")));

        when(metadataClient.getWorkflows(eq("erland"), eq("roman-alpha"), anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(new GitHubWorkflowsResponse(2));
        when(metadataClient.getWorkflows(eq("erland"), eq("roman-beta"), anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(new GitHubWorkflowsResponse(0));

        when(metadataClient.getReleases(eq("erland"), eq("roman-alpha"), anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(List.of(new GitHubReleaseResponse(
                        1L,
                        "v1.0.0",
                        "v1.0.0",
                        false,
                        false,
                        Instant.parse("2026-08-10T10:00:00Z"),
                        Instant.parse("2026-08-10T09:00:00Z"))));
        when(metadataClient.getReleases(eq("erland"), eq("roman-beta"), anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(List.of());

        var discovery = new GitHubRepositoryInventoryService(tokens, discoveryClient);
        var enrichment = new GitHubRepositoryClassificationEnrichmentService(tokens, metadataClient);
        var inventory = new InMemoryRepositoryInventoryService(discovery, enrichment, CLOCK);

        var status = inventory.refresh();
        var repositories = inventory.listRepositories();

        assertEquals(InventoryRefreshState.COMPLETED, status.state());
        assertEquals(2, status.totalCount());
        assertEquals(2, status.processedCount());
        assertEquals(2, status.successfulCount());
        assertEquals(0, status.errorCount());
        assertEquals(2, repositories.size());

        var alpha = repositories.stream()
                .filter(repository -> repository.name().equals("roman-alpha"))
                .findFirst()
                .orElseThrow();
        assertEquals(List.of("novel", "publishing"), alpha.topics());
        assertEquals("Python", alpha.primaryLanguage());
        assertEquals(LicensePresence.PRESENT, alpha.license().presence());
        assertEquals(Boolean.TRUE, alpha.githubActions().workflowsPresent());
        assertEquals(Boolean.TRUE, alpha.release().releasePresent());
        assertEquals(AnalysisState.COMPLETE, alpha.refreshStatus().state());

        var beta = repositories.stream()
                .filter(repository -> repository.name().equals("roman-beta"))
                .findFirst()
                .orElseThrow();
        assertEquals("Java", beta.primaryLanguage());
        assertTrue(beta.languages().contains("Java"));
        assertEquals(LicensePresence.MISSING, beta.license().presence());
        assertEquals(Boolean.FALSE, beta.githubActions().workflowsPresent());
        assertEquals(Boolean.FALSE, beta.release().releasePresent());
        assertEquals(AnalysisState.COMPLETE, beta.refreshStatus().state());
    }

    private GitHubRepositoryResponse repository(long id, String name) {
        return new GitHubRepositoryResponse(
                id,
                name,
                "erland/" + name,
                "https://github.com/erland/" + name,
                new GitHubRepositoryOwnerResponse(1097841L, "erland"),
                "private",
                true,
                false,
                false,
                "main",
                Instant.parse("2026-08-13T12:00:00Z"),
                Instant.parse("2026-08-13T12:05:00Z"));
    }
}
