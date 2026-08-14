package info.isaksson.erland.repofleet.repository.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
class RepositoryResourceTest {

    @Test
    void returnsDeterministicSampleInventory() {
        given()
            .when().get("/api/repositories")
            .then()
            .statusCode(200)
            .body("$", hasSize(3))
            .body("name", hasItems("roman-nollpunkten", "board-game-naturreservatet", "legacy-java-tool"))
            .body("[0].fullName", equalTo("erland/roman-nollpunkten"))
            .body("[0].license.presence", equalTo("PRESENT"))
            .body("[0].githubActions.workflowCount", equalTo(3))
            .body("[0].release.latestReleaseTag", equalTo("v1.2.0"));
    }

    @Test
    void exposesPartialAnalysisWithoutReportingMissingData() {
        given()
            .when().get("/api/repositories")
            .then()
            .statusCode(200)
            .body("[2].refreshStatus.state", equalTo("PARTIAL"))
            .body("[2].license.analysisState", equalTo("FAILED"))
            .body("[2].license.presence", equalTo("UNKNOWN"))
            .body("[2].githubActions.analysisState", equalTo("PARTIAL"))
            .body("[2].release.analysisState", equalTo("NOT_ANALYZED"));
    }
}
