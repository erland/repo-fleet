package info.isaksson.erland.repofleet.github.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.isaksson.erland.repofleet.github.config.GitHubAppConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Signature;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@ApplicationScoped
public class GitHubAppJwtService {

    private static final Base64.Encoder BASE64_URL = Base64.getUrlEncoder().withoutPadding();
    private final GitHubAppConfig config;
    private final GitHubPrivateKeyLoader privateKeyLoader;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Inject
    public GitHubAppJwtService(GitHubAppConfig config, GitHubPrivateKeyLoader privateKeyLoader, ObjectMapper objectMapper) {
        this(config, privateKeyLoader, objectMapper, Clock.systemUTC());
    }

    GitHubAppJwtService(GitHubAppConfig config, GitHubPrivateKeyLoader privateKeyLoader, ObjectMapper objectMapper, Clock clock) {
        this.config = config;
        this.privateKeyLoader = privateKeyLoader;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public String createJwt() {
        String appId = config.appId().filter(value -> !value.isBlank())
            .orElseThrow(() -> new GitHubAppNotConfiguredException("GitHub App ID is not configured."));
        PrivateKey privateKey = privateKeyLoader.load(config.privateKey().orElse(null), config.privateKeyPath().orElse(null));
        Instant now = clock.instant();

        Map<String, Object> header = Map.of("alg", "RS256", "typ", "JWT");
        Map<String, Object> payload = Map.of(
            "iat", now.minusSeconds(60).getEpochSecond(),
            "exp", now.plusSeconds(9 * 60).getEpochSecond(),
            "iss", appId
        );

        try {
            String encodedHeader = encodeJson(header);
            String encodedPayload = encodeJson(payload);
            String unsigned = encodedHeader + "." + encodedPayload;
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(unsigned.getBytes(StandardCharsets.US_ASCII));
            return unsigned + "." + BASE64_URL.encodeToString(signature.sign());
        } catch (GeneralSecurityException | JsonProcessingException ex) {
            throw new IllegalStateException("Could not generate GitHub App JWT.", ex);
        }
    }

    private String encodeJson(Object value) throws JsonProcessingException {
        return BASE64_URL.encodeToString(objectMapper.writeValueAsBytes(value));
    }
}
