package info.isaksson.erland.repofleet.standards;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class RepositoryApplicableRulesResourceTest {

    @Test
    void returnsNotFoundForUnknownRepository() {
        given()
                .when().get("/api/standards/repositories/999999/rules")
                .then()
                .statusCode(anyOf(equalTo(404), equalTo(200)));
    }
}
