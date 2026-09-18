package info.isaksson.erland.repofleet.standards;

import static io.restassured.RestAssured.given;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class RepositoryStandardRuleResourceTest {

    @Test
    void exposesRepositoryStandards() {
        given()
                .when().get("/api/standards/rules")
                .then()
                .statusCode(200);
    }
}
