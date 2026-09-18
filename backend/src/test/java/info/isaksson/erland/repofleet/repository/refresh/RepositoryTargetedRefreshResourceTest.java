package info.isaksson.erland.repofleet.repository.refresh;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import info.isaksson.erland.repofleet.repository.api.RepositoryVisibility;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryIdentityRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class RepositoryTargetedRefreshResourceTest {

    @Inject
    RepositoryIdentityRepository identities;

    @BeforeEach
    @Transactional
    void setup() {
        RepositoryRefreshJob.deleteAll();
        identities.deleteAll();

        Instant now = Instant.parse("2026-09-18T15:00:00Z");
        identities.insert(
                601L,
                "erland",
                "targeted",
                "erland/targeted",
                RepositoryVisibility.PRIVATE,
                false,
                false,
                "main",
                now,
                now,
                now);
    }

    @Test
    void queuesOneActiveManualJobPerRepository() {
        long firstId = given()
                .when().post("/api/repositories/601/targeted-refresh")
                .then()
                .statusCode(202)
                .body("githubRepositoryId", equalTo(601))
                .body("triggerType", equalTo("MANUAL_SINGLE_REPOSITORY"))
                .body("state", equalTo("PENDING"))
                .extract().jsonPath().getLong("id");

        given()
                .when().post("/api/repositories/601/targeted-refresh")
                .then()
                .statusCode(202)
                .body("id", equalTo((int) firstId));
    }

    @Test
    void returnsNotFoundForUnknownRepository() {
        given()
                .when().post("/api/repositories/999999/targeted-refresh")
                .then()
                .statusCode(404);
    }
}
