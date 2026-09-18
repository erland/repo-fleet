package info.isaksson.erland.repofleet.standards;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class RepositoryComplianceSummaryResourceTest {

    @Test
    void exposesComplianceSummaryShape() {
        given()
                .when().get("/api/compliance/summary")
                .then()
                .statusCode(200)
                .body("repositoryCount", greaterThanOrEqualTo(0))
                .body("evaluatedRuleCount", greaterThanOrEqualTo(0));
    }
}
