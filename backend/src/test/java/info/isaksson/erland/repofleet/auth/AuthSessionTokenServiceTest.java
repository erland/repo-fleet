package info.isaksson.erland.repofleet.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthSessionTokenServiceTest {
    private static final String SECRET = "01234567890123456789012345678901-test-secret";

    @Test
    void roundTripsSignedSessionAndRejectsTampering() {
        AuthSessionTokenService service = new AuthSessionTokenService(config(), new ObjectMapper());
        String token = service.create(new AuthenticatedUser("erland", "Erland", "https://example.invalid/avatar"));

        assertEquals("erland", service.parse(token).orElseThrow().login());
        assertTrue(service.parse(token + "x").isEmpty());
    }

    private static AuthConfig config() {
        return new AuthConfig() {
            public boolean enabled() { return true; }
            public Optional<String> clientId() { return Optional.of("client"); }
            public Optional<String> clientSecret() { return Optional.of("secret"); }
            public Optional<String> sessionSecret() { return Optional.of(SECRET); }
            public Optional<String> callbackUrl() { return Optional.of("https://example.invalid/api/auth/github/callback"); }
            public String allowedUsers() { return "erland"; }
            public long sessionHours() { return 12; }
            public boolean cookieSecure() { return true; }
            public String githubWebBaseUrl() { return "https://github.com"; }
            public String githubApiBaseUrl() { return "https://api.github.com"; }
        };
    }
}
