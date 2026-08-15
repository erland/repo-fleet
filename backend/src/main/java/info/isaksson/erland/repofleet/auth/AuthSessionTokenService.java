package info.isaksson.erland.repofleet.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

@ApplicationScoped
public class AuthSessionTokenService {
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final AuthConfig config;
    private final ObjectMapper objectMapper;

    @Inject
    public AuthSessionTokenService(AuthConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
    }

    public String create(AuthenticatedUser user) {
        validateSecret();
        long expiresAt = Instant.now().plusSeconds(config.sessionHours() * 3600).getEpochSecond();
        SessionPayload payload = new SessionPayload(user.login(), user.name(), user.avatarUrl(), expiresAt);
        try {
            String encodedPayload = ENCODER.encodeToString(objectMapper.writeValueAsBytes(payload));
            String signature = ENCODER.encodeToString(sign(encodedPayload.getBytes(StandardCharsets.UTF_8)));
            return encodedPayload + "." + signature;
        } catch (Exception ex) {
            throw new AuthException("Could not create authentication session.", ex);
        }
    }

    public Optional<AuthenticatedUser> parse(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        validateSecret();
        String[] parts = token.split("\\.", 2);
        if (parts.length != 2) return Optional.empty();
        try {
            byte[] expected = sign(parts[0].getBytes(StandardCharsets.UTF_8));
            byte[] actual = DECODER.decode(parts[1]);
            if (!MessageDigest.isEqual(expected, actual)) return Optional.empty();

            SessionPayload payload = objectMapper.readValue(DECODER.decode(parts[0]), SessionPayload.class);
            if (payload.expiresAtEpochSecond() <= Instant.now().getEpochSecond()) return Optional.empty();
            if (payload.login() == null || payload.login().isBlank()) return Optional.empty();
            return Optional.of(new AuthenticatedUser(payload.login(), payload.name(), payload.avatarUrl()));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private byte[] sign(byte[] payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return mac.doFinal(payload);
    }

    private String secret() {
        return config.sessionSecret().orElseThrow(() -> new AuthException(
            "REPOFLEET_AUTH_SESSION_SECRET is required when GitHub authentication is enabled."
        ));
    }

    private void validateSecret() {
        if (!config.enabled()) return;
        if (secret().length() < 32) {
            throw new AuthException("REPOFLEET_AUTH_SESSION_SECRET must contain at least 32 characters.");
        }
    }

    public record SessionPayload(String login, String name, String avatarUrl, long expiresAtEpochSecond) {
    }
}
