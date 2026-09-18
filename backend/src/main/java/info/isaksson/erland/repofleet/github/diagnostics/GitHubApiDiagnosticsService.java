package info.isaksson.erland.repofleet.github.diagnostics;

import info.isaksson.erland.repofleet.github.conditional.GitHubConditionalResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import java.time.Instant;

@ApplicationScoped
public class GitHubApiDiagnosticsService {

    @Transactional
    public void recordConditional(
            GitHubConditionalResult.Status status,
            Response response,
            Instant now) {
        GitHubApiDiagnostics diagnostics = current();
        switch (status) {
            case MODIFIED -> diagnostics.conditionalModifiedCount++;
            case NOT_MODIFIED -> diagnostics.conditionalNotModifiedCount++;
            case CACHED_FRESH -> diagnostics.conditionalCachedFreshCount++;
        }
        if (response != null) {
            String remaining = response.getHeaderString("X-RateLimit-Remaining");
            String reset = response.getHeaderString("X-RateLimit-Reset");
            if (remaining != null) {
                try {
                    diagnostics.rateLimitRemaining = Integer.valueOf(remaining);
                } catch (NumberFormatException ignored) {
                    // Keep the last known valid value.
                }
            }
            if (reset != null) {
                try {
                    diagnostics.rateLimitResetAt = Instant.ofEpochSecond(Long.parseLong(reset));
                } catch (NumberFormatException ignored) {
                    // Keep the last known valid value.
                }
            }
        }
        diagnostics.updatedAt = now;
    }

    @Transactional
    public GitHubApiDiagnostics snapshot() {
        return current();
    }

    private GitHubApiDiagnostics current() {
        GitHubApiDiagnostics diagnostics = GitHubApiDiagnostics.findById(1L);
        if (diagnostics == null) {
            diagnostics = new GitHubApiDiagnostics();
            diagnostics.id = 1L;
            diagnostics.updatedAt = Instant.now();
            diagnostics.persist();
        }
        return diagnostics;
    }
}
