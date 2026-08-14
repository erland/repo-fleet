package info.isaksson.erland.repofleet.github.auth;

import info.isaksson.erland.repofleet.github.client.GitHubAppClient;
import info.isaksson.erland.repofleet.github.config.GitHubAppConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.time.Clock;
import java.time.Instant;

@ApplicationScoped
public class GitHubInstallationTokenService {

    public static final String ACCEPT = "application/vnd.github+json";
    public static final String API_VERSION = "2026-03-10";

    private final GitHubAppConfig config;
    private final GitHubAppJwtService jwtService;
    private final GitHubAppClient client;
    private final Clock clock;
    private volatile GitHubInstallationToken cached;

    @Inject
    public GitHubInstallationTokenService(
        GitHubAppConfig config,
        GitHubAppJwtService jwtService,
        @RestClient GitHubAppClient client
    ) {
        this(config, jwtService, client, Clock.systemUTC());
    }

    GitHubInstallationTokenService(
        GitHubAppConfig config,
        GitHubAppJwtService jwtService,
        GitHubAppClient client,
        Clock clock
    ) {
        this.config = config;
        this.jwtService = jwtService;
        this.client = client;
        this.clock = clock;
    }

    public GitHubInstallationToken getToken() {
        GitHubInstallationToken current = cached;
        if (isReusable(current)) {
            return current;
        }
        synchronized (this) {
            current = cached;
            if (isReusable(current)) {
                return current;
            }
            long installationId = config.installationId()
                .orElseThrow(() -> new GitHubAppNotConfiguredException("GitHub App installation ID is not configured."));
            String jwt = jwtService.createJwt();
            var response = client.createInstallationToken(
                installationId,
                "Bearer " + jwt,
                ACCEPT,
                API_VERSION
            );
            if (response == null || response.token() == null || response.token().isBlank() || response.expiresAt() == null) {
                throw new IllegalStateException("GitHub returned an invalid installation-token response.");
            }
            cached = new GitHubInstallationToken(response.token(), response.expiresAt());
            return cached;
        }
    }

    public synchronized void invalidate() {
        cached = null;
    }

    private boolean isReusable(GitHubInstallationToken token) {
        if (token == null) return false;
        Instant refreshAt = token.expiresAt().minusSeconds(config.tokenRefreshMarginSeconds());
        return clock.instant().isBefore(refreshAt);
    }
}
