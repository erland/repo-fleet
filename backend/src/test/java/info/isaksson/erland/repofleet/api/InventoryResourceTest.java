package info.isaksson.erland.repofleet.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class InventoryResourceTest {

    @Test
    void exposesInventoryStatus() {
        given()
                .when().get("/api/inventory/status")
                .then()
                .statusCode(200)
                .body("state", anyOf(equalTo("COMPLETED"), equalTo("FAILED"), equalTo("NOT_STARTED")));
    }
}
