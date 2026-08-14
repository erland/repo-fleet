package info.isaksson.erland.repofleet.github.auth;

import java.time.Instant;

public record GitHubInstallationToken(String value, Instant expiresAt) {
}
