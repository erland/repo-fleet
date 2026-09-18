package info.isaksson.erland.repofleet.standards;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class RepositoryComplianceDetailResourceTest {

    @Test
    void returnsNotFoundForUnknownRepository() {
        given()
                .when().get("/api/compliance/repositories/999999")
                .then()
                .statusCode(404);
    }

    @Test
    void returnsEmptyDetailForKnownRepositoryWithoutEvaluations() {
        var identity = new info.isaksson.erland.repofleet.repository.persistence.RepositoryIdentity();
        identity.githubRepositoryId = 424242L;
        identity.ownerLogin = "erland";
        identity.name = "known";
        identity.fullName = "erland/known";
        identity.visibility = info.isaksson.erland.repofleet.repository.api.RepositoryVisibility.PRIVATE;
        identity.archived = false;
        identity.fork = false;
        identity.defaultBranch = "main";
        identity.firstSeenAt = java.time.Instant.parse("2026-09-18T12:00:00Z");
        identity.lastSeenAt = identity.firstSeenAt;
        identity.active = true;

        io.quarkus.hibernate.orm.panache.Panache.getEntityManager().getTransaction().begin();
        identity.persist();
        io.quarkus.hibernate.orm.panache.Panache.getEntityManager().getTransaction().commit();

        given()
                .when().get("/api/compliance/repositories/424242")
                .then()
                .statusCode(200)
                .body("size()", equalTo(0));
    }
}
