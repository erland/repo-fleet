package info.isaksson.erland.repofleet.repository.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasKey;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class OpenApiContractValidationTest {

    @Test
    void exposesRepresentativeRepositoryContracts() {
        given()
                .queryParam("format", "json")
                .when().get("/q/openapi")
                .then()
                .statusCode(200)
                .body("components.schemas.RepositorySummary.properties", hasKey("refreshStatus"))
                .body("components.schemas.RepositorySummary.properties", hasKey("topics"))
                .body("components.schemas.RepositoryRefreshStatus.properties", hasKey("state"))
                .body("components.schemas.RepositoryRefreshStatus.properties", hasKey("freshness"))
                .body("components.schemas.RepositoryRefreshStatus.properties", hasKey("latestOutcome"))
                .body(
                        "components.schemas.RepositoryRefreshOutcome.enum",
                        hasItems("SUCCESS", "DEGRADED", "FAILED"))
                .body(
                        "components.schemas.AnalysisState.enum",
                        hasItems("NOT_ANALYZED", "COMPLETE", "PARTIAL", "FAILED"));
    }

    @Test
    void exposesInventoryCountersButNotRunningAsSerializedProperty() {
        given()
                .queryParam("format", "json")
                .when().get("/q/openapi")
                .then()
                .statusCode(200)
                .body("components.schemas.InventoryStatus.properties", hasKey("reusedCount"))
                .body("components.schemas.InventoryStatus.properties", hasKey("newCount"))
                .body("components.schemas.InventoryStatus.properties", hasKey("changedCount"))
                .body("components.schemas.InventoryStatus.properties", hasKey("scheduledCount"))
                .body("components.schemas.InventoryStatus.properties", not(hasKey("running")));
    }
}
