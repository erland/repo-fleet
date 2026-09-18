package info.isaksson.erland.repofleet.standards;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import info.isaksson.erland.repofleet.repository.api.RepositoryVisibility;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryIdentity;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.transaction.Transactional;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class RepositoryComplianceDetailResourceTest {

    @BeforeEach
    @Transactional
    void clearDatabase() {
        RepositoryComplianceResult.deleteAll();
        RepositoryRuleGroupAssignment.deleteAll();
        RepositoryGroup.deleteAll();
        RepositoryStandardRule.deleteAll();
        RepositoryIdentity.deleteAll();
    }

    @Test
    void returnsNotFoundForUnknownRepository() {
        given()
                .when().get("/api/compliance/repositories/999999")
                .then()
                .statusCode(404);
    }

    @Test
    void returnsEmptyDetailForKnownRepositoryWithoutEvaluations() {
        persistKnownRepository();

        given()
                .when().get("/api/compliance/repositories/424242")
                .then()
                .statusCode(200)
                .body("size()", equalTo(0));
    }

    @Transactional
    void persistKnownRepository() {
        RepositoryIdentity identity = new RepositoryIdentity();
        identity.githubRepositoryId = 424242L;
        identity.ownerLogin = "erland";
        identity.name = "known";
        identity.fullName = "erland/known";
        identity.visibility = RepositoryVisibility.PRIVATE;
        identity.archived = false;
        identity.fork = false;
        identity.defaultBranch = "main";
        identity.firstSeenAt = Instant.parse("2026-09-18T12:00:00Z");
        identity.lastSeenAt = identity.firstSeenAt;
        identity.active = true;
        identity.persist();
    }
}
