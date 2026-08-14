package info.isaksson.erland.repofleet.github.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import info.isaksson.erland.repofleet.github.auth.GitHubInstallationToken;
import info.isaksson.erland.repofleet.github.auth.GitHubInstallationTokenService;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class GitHubApiCallExecutorTest {

    @Test
    void refreshesInstallationTokenOnceAfterUnauthorizedResponse() {
        GitHubInstallationTokenService tokens = mock(GitHubInstallationTokenService.class);
        when(tokens.getToken())
                .thenReturn(token("old-token"))
                .thenReturn(token("new-token"));
        GitHubApiCallExecutor executor = new GitHubApiCallExecutor(tokens);
        AtomicInteger calls = new AtomicInteger();

        String result = executor.execute("test request", authorization -> {
            if (calls.getAndIncrement() == 0) {
                throw http(401);
            }
            return authorization;
        });

        assertEquals("Bearer new-token", result);
        assertEquals(2, calls.get());
        verify(tokens, times(1)).invalidate();
    }

    @Test
    void retriesTransientServerFailuresWithABoundedAttemptCount() {
        GitHubInstallationTokenService tokens = mock(GitHubInstallationTokenService.class);
        when(tokens.getToken()).thenReturn(token("token"));
        GitHubApiCallExecutor executor = new GitHubApiCallExecutor(tokens);
        AtomicInteger calls = new AtomicInteger();

        String result = executor.execute("metadata", authorization -> {
            if (calls.incrementAndGet() < 3) {
                throw http(503);
            }
            return "ok";
        });

        assertEquals("ok", result);
        assertEquals(3, calls.get());
    }


    @Test
    void retriesNetworkProcessingFailures() {
        GitHubInstallationTokenService tokens = mock(GitHubInstallationTokenService.class);
        when(tokens.getToken()).thenReturn(token("token"));
        GitHubApiCallExecutor executor = new GitHubApiCallExecutor(tokens);
        AtomicInteger calls = new AtomicInteger();

        String result = executor.execute("network request", authorization -> {
            if (calls.incrementAndGet() < 2) {
                throw new ProcessingException("connection reset");
            }
            return "ok";
        });

        assertEquals("ok", result);
        assertEquals(2, calls.get());
    }

    @Test
    void classifiesRateLimitAndDoesNotLeakAuthorizationValue() {
        GitHubInstallationTokenService tokens = mock(GitHubInstallationTokenService.class);
        when(tokens.getToken()).thenReturn(token("very-secret-token"));
        GitHubApiCallExecutor executor = new GitHubApiCallExecutor(tokens);
        AtomicInteger calls = new AtomicInteger();

        GitHubApiException failure = assertThrows(
                GitHubApiException.class,
                () -> executor.execute("topics", authorization -> {
                    calls.incrementAndGet();
                    throw new WebApplicationException(
                            Response.status(403)
                                    .header("X-RateLimit-Remaining", "0")
                                    .build());
                }));

        assertEquals(GitHubApiFailureKind.RATE_LIMIT, failure.kind());
        assertEquals(3, calls.get());
        assertFalse(failure.getMessage().contains("very-secret-token"));
    }

    @Test
    void doesNotRetryNotFoundRepositoryResources() {
        GitHubInstallationTokenService tokens = mock(GitHubInstallationTokenService.class);
        when(tokens.getToken()).thenReturn(token("token"));
        GitHubApiCallExecutor executor = new GitHubApiCallExecutor(tokens);
        AtomicInteger calls = new AtomicInteger();

        GitHubApiException failure = assertThrows(
                GitHubApiException.class,
                () -> executor.execute("repository metadata", authorization -> {
                    calls.incrementAndGet();
                    throw http(404);
                }));

        assertEquals(GitHubApiFailureKind.NOT_FOUND, failure.kind());
        assertEquals(1, calls.get());
    }

    @Test
    void doesNotRetryOrdinaryAuthorizationFailures() {
        GitHubInstallationTokenService tokens = mock(GitHubInstallationTokenService.class);
        when(tokens.getToken()).thenReturn(token("token"));
        GitHubApiCallExecutor executor = new GitHubApiCallExecutor(tokens);
        AtomicInteger calls = new AtomicInteger();

        GitHubApiException failure = assertThrows(
                GitHubApiException.class,
                () -> executor.execute("repository metadata", authorization -> {
                    calls.incrementAndGet();
                    throw http(403);
                }));

        assertEquals(GitHubApiFailureKind.AUTHORIZATION, failure.kind());
        assertEquals(1, calls.get());
    }

    private GitHubInstallationToken token(String value) {
        return new GitHubInstallationToken(value, Instant.parse("2026-08-14T18:00:00Z"));
    }

    private WebApplicationException http(int status) {
        return new WebApplicationException(Response.status(status).build());
    }
}
