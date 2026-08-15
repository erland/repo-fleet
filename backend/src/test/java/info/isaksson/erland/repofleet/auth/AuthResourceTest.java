package info.isaksson.erland.repofleet.auth;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;

@QuarkusTest
class AuthResourceTest {
    @Test
    void reportsAuthenticationDisabledByDefault() {
        given()
            .when().get("/api/auth/session")
            .then()
            .statusCode(200)
            .body("authEnabled", is(false))
            .body("authenticated", is(false))
            .body("user", nullValue());
    }
}
