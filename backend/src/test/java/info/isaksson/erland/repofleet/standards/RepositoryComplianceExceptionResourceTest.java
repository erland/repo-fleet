package info.isaksson.erland.repofleet.standards;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import info.isaksson.erland.repofleet.repository.api.RepositoryVisibility;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryIdentityRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class RepositoryComplianceExceptionResourceTest {

    @Inject
    RepositoryIdentityRepository identities;

    @Inject
    RepositoryStandardRuleService rules;

    @BeforeEach
    @Transactional
    void setup() {
        RepositoryComplianceException.deleteAll();
        RepositoryComplianceResult.deleteAll();
        RepositoryRuleGroupAssignment.deleteAll();
        RepositoryGroup.deleteAll();
        RepositoryStandardRule.deleteAll();
        identities.deleteAll();

        Instant now = Instant.parse("2026-09-18T14:00:00Z");
        identities.insert(
                3003L,
                "erland",
                "repo-three",
                "erland/repo-three",
                RepositoryVisibility.PRIVATE,
                false,
                false,
                "main",
                now,
                now,
                now);

        rules.save(
                "license-required",
                RepositoryRuleType.LICENSE_REQUIRED,
                "License required",
                null,
                RepositoryRuleSeverity.REQUIRED,
                true,
                Map.of(),
                RepositoryRuleScope.ALL_REPOSITORIES,
                now);
    }

    @Test
    void createEditExpireAndRemoveExceptionOverHttp() {
        given()
                .contentType("application/json")
                .body("""
                        {
                          "reason": "Temporary deviation",
                          "expiresAt": "2099-01-01T00:00:00Z"
                        }
                        """)
                .when().post("/api/compliance/repositories/3003/exceptions/license-required")
                .then()
                .statusCode(200)
                .body("reason", equalTo("Temporary deviation"))
                .body("state", equalTo("ACTIVE"));

        given()
                .contentType("application/json")
                .body("""
                        {
                          "reason": "Updated deviation",
                          "expiresAt": null
                        }
                        """)
                .when().post("/api/compliance/repositories/3003/exceptions/license-required")
                .then()
                .statusCode(200)
                .body("reason", equalTo("Updated deviation"))
                .body("state", equalTo("ACTIVE"));

        given()
                .when().post("/api/compliance/repositories/3003/exceptions/license-required/expire")
                .then()
                .statusCode(204);

        given()
                .when().delete("/api/compliance/repositories/3003/exceptions/license-required")
                .then()
                .statusCode(204);

        given()
                .when().get("/api/compliance/repositories/3003/exceptions")
                .then()
                .statusCode(200)
                .body("size()", equalTo(0));
    }
}
