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
                .body("state", anyOf(equalTo("COMPLETED"), equalTo("PARTIAL"), equalTo("FAILED"), equalTo("RUNNING"), equalTo("NOT_STARTED")));
    }
    @Test
    void startsRefreshAndReturnsProgressShape() {
        given()
                .when().post("/api/inventory/refresh")
                .then()
                .statusCode(200)
                .body("state", anyOf(equalTo("RUNNING"), equalTo("COMPLETED"), equalTo("PARTIAL"), equalTo("FAILED")))
                .body("processedCount", org.hamcrest.Matchers.greaterThanOrEqualTo(0))
                .body("errorCount", org.hamcrest.Matchers.greaterThanOrEqualTo(0));
    }

}
