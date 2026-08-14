package info.isaksson.erland.repofleet.repository.inventory;

import info.isaksson.erland.repofleet.github.auth.GitHubInstallationToken;
import info.isaksson.erland.repofleet.github.auth.GitHubInstallationTokenService;
import info.isaksson.erland.repofleet.github.client.GitHubAppClient;
import info.isaksson.erland.repofleet.github.client.GitHubRepositoryOwnerResponse;
import info.isaksson.erland.repofleet.github.client.GitHubRepositoryResponse;
import info.isaksson.erland.repofleet.github.client.InstallationRepositoriesResponse;
import info.isaksson.erland.repofleet.repository.api.AnalysisState;
import info.isaksson.erland.repofleet.repository.api.RepositoryVisibility;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GitHubRepositoryInventoryServiceTest {

    private final GitHubInstallationTokenService tokenService = mock(GitHubInstallationTokenService.class);
    private final GitHubAppClient client = mock(GitHubAppClient.class);

    @Test
    void mapsRepositoryAndLeavesEnrichmentNotAnalyzed() {
        when(tokenService.getToken()).thenReturn(new GitHubInstallationToken("token", Instant.now().plusSeconds(3600)));
        when(client.listInstallationRepositories(anyString(), anyString(), anyString(), anyInt(), anyInt()))
            .thenReturn(new InstallationRepositoriesResponse(1, List.of(repository(1, "demo", "private"))));

        var service = new GitHubRepositoryInventoryService(tokenService, client);
        var result = service.listRepositories();

        assertEquals(1, result.size());
        var repo = result.getFirst();
        assertEquals("erland/demo", repo.fullName());
        assertEquals(RepositoryVisibility.PRIVATE, repo.visibility());
        assertTrue(repo.topics().isEmpty());
        assertTrue(repo.languages().isEmpty());
        assertNull(repo.primaryLanguage());
        assertEquals(AnalysisState.NOT_ANALYZED, repo.license().analysisState());
        assertEquals(AnalysisState.NOT_ANALYZED, repo.githubActions().analysisState());
        assertEquals(AnalysisState.NOT_ANALYZED, repo.release().analysisState());
    }

    @Test
    void followsPaginationUntilTotalCountIsReached() {
        when(tokenService.getToken()).thenReturn(new GitHubInstallationToken("token", Instant.now().plusSeconds(3600)));
        List<GitHubRepositoryResponse> firstPage = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            firstPage.add(repository(i + 1, "repo-%03d".formatted(i + 1), "public"));
        }
        when(client.listInstallationRepositories(anyString(), anyString(), anyString(), anyInt(), org.mockito.ArgumentMatchers.eq(1)))
            .thenReturn(new InstallationRepositoriesResponse(101, firstPage));
        when(client.listInstallationRepositories(anyString(), anyString(), anyString(), anyInt(), org.mockito.ArgumentMatchers.eq(2)))
            .thenReturn(new InstallationRepositoriesResponse(101, List.of(repository(101, "repo-101", "public"))));

        var service = new GitHubRepositoryInventoryService(tokenService, client);
        var result = service.listRepositories();

        assertEquals(101, result.size());
    }

    @Test
    void handlesEmptyInstallation() {
        when(tokenService.getToken()).thenReturn(new GitHubInstallationToken("token", Instant.now().plusSeconds(3600)));
        when(client.listInstallationRepositories(anyString(), anyString(), anyString(), anyInt(), anyInt()))
            .thenReturn(new InstallationRepositoriesResponse(0, List.of()));

        assertTrue(new GitHubRepositoryInventoryService(tokenService, client).listRepositories().isEmpty());
    }

    @Test
    void rejectsNullGitHubResponse() {
        when(tokenService.getToken()).thenReturn(new GitHubInstallationToken("token", Instant.now().plusSeconds(3600)));
        when(client.listInstallationRepositories(anyString(), anyString(), anyString(), anyInt(), anyInt())).thenReturn(null);

        assertThrows(IllegalStateException.class,
            () -> new GitHubRepositoryInventoryService(tokenService, client).listRepositories());
    }

    private GitHubRepositoryResponse repository(long id, String name, String visibility) {
        return new GitHubRepositoryResponse(
            id,
            name,
            "erland/" + name,
            "https://github.com/erland/" + name,
            new GitHubRepositoryOwnerResponse(1097841L, "erland"),
            visibility,
            "private".equals(visibility),
            false,
            false,
            "main",
            Instant.parse("2026-08-13T12:00:00Z"),
            Instant.parse("2026-08-13T12:05:00Z")
        );
    }
}
