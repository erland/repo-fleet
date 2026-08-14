package info.isaksson.erland.repofleet.github.api;

import info.isaksson.erland.repofleet.github.auth.GitHubInstallationTokenService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.function.Function;

@ApplicationScoped
public class GitHubApiCallExecutor {

    static final int MAX_ATTEMPTS = 3;

    private final GitHubInstallationTokenService tokenService;

    @Inject
    public GitHubApiCallExecutor(GitHubInstallationTokenService tokenService) {
        this.tokenService = tokenService;
    }

    public <T> T execute(String operation, Function<String, T> request) {
        WebApplicationException lastHttpFailure = null;
        boolean tokenInvalidated = false;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            String authorization = "Bearer " + tokenService.getToken().value();
            try {
                return request.apply(authorization);
            } catch (WebApplicationException exception) {
                lastHttpFailure = exception;
                Response response = exception.getResponse();
                int status = response == null ? 0 : response.getStatus();

                if (status == 401 && !tokenInvalidated) {
                    tokenService.invalidate();
                    tokenInvalidated = true;
                    continue;
                }

                GitHubApiFailureKind kind = classify(response);
                if (isRetryable(kind) && attempt < MAX_ATTEMPTS) {
                    continue;
                }

                throw safeException(operation, kind, status, exception);
            } catch (ProcessingException exception) {
                if (attempt < MAX_ATTEMPTS) {
                    continue;
                }
                throw safeException(operation, GitHubApiFailureKind.TRANSIENT, 0, exception);
            }
        }

        throw safeException(
                operation,
                GitHubApiFailureKind.TRANSIENT,
                lastHttpFailure == null || lastHttpFailure.getResponse() == null
                        ? 0
                        : lastHttpFailure.getResponse().getStatus(),
                lastHttpFailure);
    }

    static GitHubApiFailureKind classify(Response response) {
        if (response == null) return GitHubApiFailureKind.UNKNOWN;

        int status = response.getStatus();
        if (status == 404) return GitHubApiFailureKind.NOT_FOUND;
        if (status == 401) return GitHubApiFailureKind.AUTHENTICATION;
        if (status == 429) return GitHubApiFailureKind.RATE_LIMIT;
        if (status == 403) {
            String remaining = response.getHeaderString("X-RateLimit-Remaining");
            return "0".equals(remaining)
                    ? GitHubApiFailureKind.RATE_LIMIT
                    : GitHubApiFailureKind.AUTHORIZATION;
        }
        if (status == 408 || status == 502 || status == 503 || status == 504 || status >= 500) {
            return GitHubApiFailureKind.TRANSIENT;
        }
        return GitHubApiFailureKind.UNKNOWN;
    }

    private boolean isRetryable(GitHubApiFailureKind kind) {
        return kind == GitHubApiFailureKind.RATE_LIMIT || kind == GitHubApiFailureKind.TRANSIENT;
    }

    private GitHubApiException safeException(
            String operation,
            GitHubApiFailureKind kind,
            int status,
            Throwable cause) {
        String detail = switch (kind) {
            case NOT_FOUND -> "repository or resource is unavailable";
            case AUTHENTICATION -> "GitHub authentication failed after token refresh";
            case AUTHORIZATION -> "GitHub App is not authorized for this resource";
            case RATE_LIMIT -> "GitHub API rate limit was reached";
            case TRANSIENT -> "GitHub API remained temporarily unavailable after retries";
            case UNKNOWN -> status > 0 ? "GitHub API returned HTTP " + status : "GitHub API request failed";
        };
        return new GitHubApiException(
                kind,
                status > 0 ? status : null,
                operation + ": " + detail,
                cause);
    }
}
