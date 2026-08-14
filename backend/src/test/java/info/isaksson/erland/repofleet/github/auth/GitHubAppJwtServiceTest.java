package info.isaksson.erland.repofleet.github.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import info.isaksson.erland.repofleet.github.config.GitHubAppConfig;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitHubAppJwtServiceTest {

    @Test
    void createsRs256JwtWithExpectedClaimsAndValidSignature() throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        var keyPair = generator.generateKeyPair();
        String pem = "-----BEGIN PRIVATE KEY-----\n"
            + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(keyPair.getPrivate().getEncoded())
            + "\n-----END PRIVATE KEY-----";
        var config = config("12345", 99L, pem, null, 300);
        var clock = Clock.fixed(Instant.parse("2026-08-14T06:00:00Z"), ZoneOffset.UTC);
        var mapper = new ObjectMapper();
        var service = new GitHubAppJwtService(config, new GitHubPrivateKeyLoader(), mapper, clock);

        String jwt = service.createJwt();
        String[] parts = jwt.split("\\.");
        assertEquals(3, parts.length);

        var payload = mapper.readTree(Base64.getUrlDecoder().decode(parts[1]));
        assertEquals("12345", payload.get("iss").asText());
        assertEquals(Instant.parse("2026-08-14T05:59:00Z").getEpochSecond(), payload.get("iat").asLong());
        assertEquals(Instant.parse("2026-08-14T06:09:00Z").getEpochSecond(), payload.get("exp").asLong());

        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(keyPair.getPublic());
        verifier.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
        assertTrue(verifier.verify(Base64.getUrlDecoder().decode(parts[2])));
    }

    @Test
    void acceptsEscapedNewlinesInInlinePrivateKey() throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        var keyPair = generator.generateKeyPair();
        String pem = "-----BEGIN PRIVATE KEY-----\n"
            + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(keyPair.getPrivate().getEncoded())
            + "\n-----END PRIVATE KEY-----";
        var config = config("12345", 99L, pem.replace("\n", "\\n"), null, 300);
        var service = new GitHubAppJwtService(
            config,
            new GitHubPrivateKeyLoader(),
            new ObjectMapper(),
            Clock.fixed(Instant.parse("2026-08-14T06:00:00Z"), ZoneOffset.UTC)
        );

        assertEquals(3, service.createJwt().split("\\.").length);
    }

    static GitHubAppConfig config(String appId, Long installationId, String privateKey, String privateKeyPath, long margin) {
        return new GitHubAppConfig() {
            public Optional<String> appId() { return Optional.ofNullable(appId); }
            public Optional<Long> installationId() { return Optional.ofNullable(installationId); }
            public Optional<String> privateKey() { return Optional.ofNullable(privateKey); }
            public Optional<String> privateKeyPath() { return Optional.ofNullable(privateKeyPath); }
            public long tokenRefreshMarginSeconds() { return margin; }
        };
    }
}
