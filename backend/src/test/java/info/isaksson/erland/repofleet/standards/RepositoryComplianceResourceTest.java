package info.isaksson.erland.repofleet.standards;

import static io.restassured.RestAssured.given;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class RepositoryComplianceResourceTest {

    @Test
    void returnsNotFoundForUnknownRepository() {
        given()
                .when().get("/api/standards/repositories/999999/compliance")
                .then()
                .statusCode(404);
    }
}
