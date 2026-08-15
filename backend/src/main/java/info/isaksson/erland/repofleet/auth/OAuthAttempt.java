package info.isaksson.erland.repofleet.auth;

import java.time.Instant;

record OAuthAttempt(String codeVerifier, Instant expiresAt) {
}
