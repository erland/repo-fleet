package info.isaksson.erland.repofleet.github.conditional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import info.isaksson.erland.repofleet.github.api.GitHubApiCallExecutor;
import jakarta.ws.rs.core.Response;
import java.time.Duration;
import java.time.Instant;
import info.isaksson.erland.repofleet.repository.refresh.RepositoryRefreshPolicy;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class GitHubConditionalRequestExecutorTest {

    @Test
    void freshCategoryCacheSkipsGitHubRequestEntirely() {
        GitHubApiCallExecutor apiCalls = mock(GitHubApiCallExecutor.class);
        GitHubConditionalRequestStateService states = mock(GitHubConditionalRequestStateService.class);
        GitHubConditionalRequestState existing = new GitHubConditionalRequestState();
        existing.etag = "\"etag-1\"";
        existing.lastSuccessfulFetchAt = Instant.parse("2026-09-18T08:50:00Z");
        when(states.find(1234L, "topics")).thenReturn(java.util.Optional.of(existing));

        RepositoryRefreshPolicy policy = new RepositoryRefreshPolicy(
                Duration.ofMinutes(15),
                Duration.ofMinutes(60),
                Duration.ofHours(24));
        GitHubConditionalRequestExecutor executor =
                new GitHubConditionalRequestExecutor(apiCalls, states, policy);

        var result = executor.execute(
                1234L,
                "topics",
                "topics",
                Instant.parse("2026-09-18T09:00:00Z"),
                (authorization, etag) -> Response.ok("should-not-run").build(),
                response -> response.readEntity(String.class),
                () -> "cached-value");

        assertEquals(GitHubConditionalResult.Status.CACHED_FRESH, result.status());
        assertEquals("cached-value", result.value());
        verify(apiCalls, never()).execute(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void sendsPersistedEtagAndStoresNewEtagAfterModifiedResponse() {
        GitHubApiCallExecutor apiCalls = mock(GitHubApiCallExecutor.class);
        GitHubConditionalRequestStateService states = mock(GitHubConditionalRequestStateService.class);
        GitHubConditionalRequestState existing = new GitHubConditionalRequestState();
        existing.etag = "\"old\"";
        when(states.find(1234L, "topics")).thenReturn(java.util.Optional.of(existing));

        when(apiCalls.execute(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    java.util.function.Function<String, Response> request = invocation.getArgument(1);
                    return request.apply("Bearer token");
                });

        GitHubConditionalRequestExecutor executor =
                new GitHubConditionalRequestExecutor(apiCalls, states);
        AtomicReference<String> sentEtag = new AtomicReference<>();
        Instant fetchedAt = Instant.parse("2026-09-18T09:00:00Z");

        var result = executor.execute(
                1234L,
                "topics",
                "topics",
                fetchedAt,
                (authorization, etag) -> {
                    sentEtag.set(etag);
                    return Response.ok("fresh").header("ETag", "\"new\"").build();
                },
                response -> response.readEntity(String.class),
                () -> "cached");

        assertEquals("\"old\"", sentEtag.get());
        assertEquals(GitHubConditionalResult.Status.MODIFIED, result.status());
        assertEquals("fresh", result.value());
        assertEquals("\"new\"", result.etag());
        verify(states).recordModified(1234L, "topics", "\"new\"", fetchedAt);
    }

    @Test
    void treatsNotModifiedAsSuccessAndReturnsCachedValue() {
        GitHubApiCallExecutor apiCalls = mock(GitHubApiCallExecutor.class);
        GitHubConditionalRequestStateService states = mock(GitHubConditionalRequestStateService.class);
        GitHubConditionalRequestState existing = new GitHubConditionalRequestState();
        existing.etag = "\"etag-1\"";
        when(states.find(1234L, "languages")).thenReturn(java.util.Optional.of(existing));
        when(states.recordNotModified(
                1234L,
                "languages",
                Instant.parse("2026-09-18T09:00:00Z")))
                .thenReturn(existing);

        when(apiCalls.execute(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    java.util.function.Function<String, Response> request = invocation.getArgument(1);
                    return request.apply("Bearer token");
                });

        GitHubConditionalRequestExecutor executor =
                new GitHubConditionalRequestExecutor(apiCalls, states);

        var result = executor.execute(
                1234L,
                "languages",
                "languages",
                Instant.parse("2026-09-18T09:00:00Z"),
                (authorization, etag) -> Response.status(304).build(),
                response -> "unused",
                () -> "cached-value");

        assertTrue(result.notModified());
        assertEquals("cached-value", result.value());
        assertEquals("\"etag-1\"", result.etag());
    }
}
