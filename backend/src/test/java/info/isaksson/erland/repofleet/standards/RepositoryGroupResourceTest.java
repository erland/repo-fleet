package info.isaksson.erland.repofleet.standards;

import static io.restassured.RestAssured.given;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class RepositoryGroupResourceTest {

    @Test
    void exposesRepositoryGroups() {
        given()
                .when().get("/api/standards/groups")
                .then()
                .statusCode(200);
    }
}
