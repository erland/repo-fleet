package info.isaksson.erland.repofleet.github.api;

import info.isaksson.erland.repofleet.github.auth.GitHubAppJwtService;
import info.isaksson.erland.repofleet.github.auth.GitHubAppNotConfiguredException;
import info.isaksson.erland.repofleet.github.auth.GitHubInstallationTokenService;
import info.isaksson.erland.repofleet.github.client.GitHubAppClient;
import info.isaksson.erland.repofleet.github.config.GitHubAppConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class GitHubConnectionService {

    private final GitHubAppConfig config;
    private final GitHubAppJwtService jwtService;
    private final GitHubInstallationTokenService installationTokenService;
    private final GitHubAppClient client;

    @Inject
    public GitHubConnectionService(
        GitHubAppConfig config,
        GitHubAppJwtService jwtService,
        GitHubInstallationTokenService installationTokenService,
        @RestClient GitHubAppClient client
    ) {
        this.config = config;
        this.jwtService = jwtService;
        this.installationTokenService = installationTokenService;
        this.client = client;
    }

    public GitHubConnectionStatus check() {
        try {
            String jwt = jwtService.createJwt();
            var app = client.getAuthenticatedApp(
                "Bearer " + jwt,
                GitHubInstallationTokenService.ACCEPT,
                GitHubInstallationTokenService.API_VERSION
            );
            var token = installationTokenService.getToken();
            return new GitHubConnectionStatus(
                GitHubConnectionState.CONNECTED,
                app == null ? null : app.slug(),
                config.installationId().orElse(null),
                token.expiresAt(),
                "GitHub App authentication and installation-token acquisition succeeded."
            );
        } catch (GitHubAppNotConfiguredException ex) {
            return new GitHubConnectionStatus(
                GitHubConnectionState.NOT_CONFIGURED,
                null,
                config.installationId().orElse(null),
                null,
                ex.getMessage()
            );
        } catch (RuntimeException ex) {
            return new GitHubConnectionStatus(
                GitHubConnectionState.ERROR,
                null,
                config.installationId().orElse(null),
                null,
                "GitHub connection check failed."
            );
        }
    }
}
