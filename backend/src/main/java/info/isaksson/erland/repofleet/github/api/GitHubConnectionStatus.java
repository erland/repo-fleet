package info.isaksson.erland.repofleet.github.api;

import java.time.Instant;

public record GitHubConnectionStatus(
    GitHubConnectionState state,
    String appSlug,
    Long installationId,
    Instant installationTokenExpiresAt,
    String message
) {
}
