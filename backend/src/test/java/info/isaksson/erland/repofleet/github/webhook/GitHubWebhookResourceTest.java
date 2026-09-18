package info.isaksson.erland.repofleet.github.webhook;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class GitHubWebhookResourceTest {

    @ConfigProperty(name = "repofleet.github.webhook-secret")
    String secret;

    @BeforeEach
    @Transactional
    void clearDeliveries() {
        GitHubWebhookDelivery.deleteAll();
    }

    @Test
    void acceptsSignedDeliveryExactlyOnce() throws Exception {
        String payload = "{\"action\":\"edited\"}";
        String signature = sign(payload);

        given()
                .header("X-Hub-Signature-256", signature)
                .header("X-GitHub-Delivery", "delivery-1")
                .header("X-GitHub-Event", "repository")
                .contentType("application/json")
                .body(payload)
                .when().post("/api/github/webhook")
                .then()
                .statusCode(200)
                .body("duplicate", equalTo(false))
                .body("eventSupport", equalTo("SUPPORTED"));

        given()
                .header("X-Hub-Signature-256", signature)
                .header("X-GitHub-Delivery", "delivery-1")
                .header("X-GitHub-Event", "repository")
                .contentType("application/json")
                .body(payload)
                .when().post("/api/github/webhook")
                .then()
                .statusCode(200)
                .body("duplicate", equalTo(true));

        assertEquals(1L, GitHubWebhookDelivery.count());
    }

    @Test
    void rejectsInvalidSignature() {
        given()
                .header("X-Hub-Signature-256", "sha256=deadbeef")
                .header("X-GitHub-Delivery", "delivery-2")
                .header("X-GitHub-Event", "repository")
                .contentType("application/json")
                .body("{}")
                .when().post("/api/github/webhook")
                .then()
                .statusCode(401);
    }

    @Test
    void recordsUnsupportedEventType() throws Exception {
        String payload = "{}";
        given()
                .header("X-Hub-Signature-256", sign(payload))
                .header("X-GitHub-Delivery", "delivery-3")
                .header("X-GitHub-Event", "issues")
                .contentType("application/json")
                .body(payload)
                .when().post("/api/github/webhook")
                .then()
                .statusCode(200)
                .body("eventSupport", equalTo("UNSUPPORTED"));
    }

    private String sign(String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(
                mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }
}
