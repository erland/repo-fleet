package info.isaksson.erland.repofleet.github.conditional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import info.isaksson.erland.repofleet.github.api.GitHubApiCallExecutor;
import info.isaksson.erland.repofleet.repository.refresh.RepositoryRefreshPolicy;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class Phase2ConditionalCacheAcceptanceTest {

    @Test
    void freshConditionalCacheAvoidsGitHubApiCallWithoutSecrets() {
        GitHubApiCallExecutor apiCalls = mock(GitHubApiCallExecutor.class);
        GitHubConditionalRequestStateService states = mock(GitHubConditionalRequestStateService.class);

        Instant now = Instant.parse("2026-09-18T17:00:00Z");
        GitHubConditionalRequestState state = new GitHubConditionalRequestState();
        state.githubRepositoryId = 8201L;
        state.resourceCategory = "topics";
        state.etag = ""acceptance-etag"";
        state.lastSuccessfulFetchAt = now.minusSeconds(60);
        state.updatedAt = state.lastSuccessfulFetchAt;

        when(states.find(8201L, "topics")).thenReturn(java.util.Optional.of(state));

        RepositoryRefreshPolicy policy = new RepositoryRefreshPolicy(
                Duration.ofMinutes(15),
                Duration.ofMinutes(60),
                Duration.ofHours(24));
        GitHubConditionalRequestExecutor executor =
                new GitHubConditionalRequestExecutor(apiCalls, states, policy);

        var result = executor.execute(
                8201L,
                "topics",
                "acceptance topics",
                now,
                (authorization, etag) -> {
                    throw new AssertionError("GitHub request must not run for fresh cache");
                },
                response -> "network-value",
                () -> "persisted-cache-value");

        assertEquals(GitHubConditionalResult.Status.CACHED_FRESH, result.status());
        assertEquals("persisted-cache-value", result.value());
        assertEquals(""acceptance-etag"", result.etag());
        verify(apiCalls, never()).execute(anyString(), any());
    }
}
