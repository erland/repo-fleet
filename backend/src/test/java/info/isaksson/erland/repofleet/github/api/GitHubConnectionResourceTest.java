package info.isaksson.erland.repofleet.github.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
class GitHubConnectionResourceTest {

    @Test
    void reportsNotConfiguredWithoutLeakingCredentialsWhenSecretsAreAbsent() {
        given()
            .when().get("/api/github/connection")
            .then()
            .statusCode(200)
            .body("state", equalTo("NOT_CONFIGURED"))
            .body("appSlug", nullValue())
            .body("installationTokenExpiresAt", nullValue());
    }
}
