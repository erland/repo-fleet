package info.isaksson.erland.repofleet.repository.diagnostics;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import info.isaksson.erland.repofleet.github.diagnostics.GitHubApiDiagnostics;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryRefreshRun;
import info.isaksson.erland.repofleet.repository.refresh.RepositoryRefreshJob;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class RefreshDiagnosticsResourceTest {

    @BeforeEach
    @Transactional
    void clearDiagnostics() {
        RepositoryRefreshJob.deleteAll();
        RepositoryRefreshRun.deleteAll();
        GitHubApiDiagnostics.deleteAll();
    }

    @Test
    void exposesOperationalDiagnosticsWithoutServerLogs() {
        given()
                .when().get("/api/diagnostics/refresh")
                .then()
                .statusCode(200)
                .body("conditionalModifiedCount", equalTo(0))
                .body("conditionalNotModifiedCount", equalTo(0))
                .body("conditionalCachedFreshCount", equalTo(0))
                .body("webhookTriggeredRefreshCount", equalTo(0))
                .body("targetedFailedCount", equalTo(0))
                .body("recentRuns", notNullValue())
                .body("recentTargetedFailures", notNullValue());
    }
}
