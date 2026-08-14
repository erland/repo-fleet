package info.isaksson.erland.repofleet.repository.api;

import info.isaksson.erland.repofleet.repository.inventory.RepositoryInventoryService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;

@QuarkusTest
class RepositoryResourceTest {

    @InjectMock
    RepositoryInventoryService inventoryService;

    @BeforeEach
    void setUp() {
        when(inventoryService.listRepositories()).thenReturn(List.of(
            new RepositorySummary(
                1L,
                "erland",
                "repo-fleet",
                "erland/repo-fleet",
                "https://github.com/erland/repo-fleet",
                RepositoryVisibility.PRIVATE,
                false,
                false,
                "main",
                List.of(),
                List.of(),
                null,
                new LicenseStatus(AnalysisState.NOT_ANALYZED, LicensePresence.UNKNOWN, null, null, null),
                new GitHubActionsStatus(AnalysisState.NOT_ANALYZED, null, null),
                new ReleaseStatus(AnalysisState.NOT_ANALYZED, null, null, null, null, null),
                new ActivityStatus(Instant.parse("2026-08-13T12:00:00Z"), Instant.parse("2026-08-13T12:05:00Z")),
                new RepositoryRefreshStatus(AnalysisState.NOT_ANALYZED, "Repository discovered; maintenance enrichment pending.")
            )
        ));
    }

    @Test
    void returnsRepositoryInventory() {
        given()
            .when().get("/api/repositories")
            .then()
            .statusCode(200)
            .body("$", hasSize(1))
            .body("[0].fullName", equalTo("erland/repo-fleet"))
            .body("[0].license.presence", equalTo("UNKNOWN"))
            .body("[0].githubActions.analysisState", equalTo("NOT_ANALYZED"))
            .body("[0].release.analysisState", equalTo("NOT_ANALYZED"));
    }
}
