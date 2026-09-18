package info.isaksson.erland.repofleet.github.webhook;

import info.isaksson.erland.repofleet.github.config.GitHubAppConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@ApplicationScoped
public class GitHubWebhookSignatureVerifier {
    private final GitHubAppConfig config;

    @Inject
    public GitHubWebhookSignatureVerifier(GitHubAppConfig config) {
        this.config = config;
    }

    public boolean verify(String payload, String signatureHeader) {
        String secret = config.webhookSecret().filter(value -> !value.isBlank()).orElse(null);
        if (secret == null || signatureHeader == null || !signatureHeader.startsWith("sha256=")) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = "sha256=" + HexFormat.of().formatHex(
                    mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.US_ASCII),
                    signatureHeader.getBytes(StandardCharsets.US_ASCII));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not verify GitHub webhook signature", exception);
        }
    }
}
