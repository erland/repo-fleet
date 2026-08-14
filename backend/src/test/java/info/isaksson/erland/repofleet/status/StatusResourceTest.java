package info.isaksson.erland.repofleet.status;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class StatusResourceTest {

    @Test
    void returnsApplicationStatus() {
        given()
            .when().get("/api/status")
            .then()
            .statusCode(200)
            .body("service", is("repo-fleet-backend"))
            .body("status", is("UP"));
    }
}
