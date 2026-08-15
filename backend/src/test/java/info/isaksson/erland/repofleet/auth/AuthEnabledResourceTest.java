package info.isaksson.erland.repofleet.auth;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
@TestProfile(AuthEnabledResourceTest.EnabledAuthProfile.class)
class AuthEnabledResourceTest {

    @Test
    void protectsRepositoryApiWithoutSession() {
        given()
            .when().get("/api/repositories")
            .then()
            .statusCode(401)
            .body("error", is("authentication_required"));
    }

    @Test
    void leavesAuthenticationSessionEndpointPublic() {
        given()
            .when().get("/api/auth/session")
            .then()
            .statusCode(200)
            .body("authEnabled", is(true))
            .body("authenticated", is(false));
    }

    public static class EnabledAuthProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                "repofleet.auth.enabled", "true",
                "repofleet.auth.client-id", "test-client-id",
                "repofleet.auth.client-secret", "test-client-secret",
                "repofleet.auth.session-secret", "01234567890123456789012345678901-test-secret",
                "repofleet.auth.callback-url", "https://example.invalid/api/auth/github/callback",
                "repofleet.auth.allowed-users", "erland",
                "repofleet.auth.cookie-secure", "false"
            );
        }
    }
}
