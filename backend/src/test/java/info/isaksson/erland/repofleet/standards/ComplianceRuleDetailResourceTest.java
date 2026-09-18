package info.isaksson.erland.repofleet.standards;

import static io.restassured.RestAssured.given;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ComplianceRuleDetailResourceTest {

    @Test
    void returnsNotFoundForUnknownRule() {
        given()
                .when().get("/api/compliance/rules/does-not-exist")
                .then()
                .statusCode(404);
    }
}
