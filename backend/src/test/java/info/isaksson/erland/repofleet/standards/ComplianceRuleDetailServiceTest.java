package info.isaksson.erland.repofleet.standards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
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
class ComplianceRuleDetailServiceTest {

    @Inject
    ComplianceRuleDetailService details;

    @Inject
    RepositoryStandardRuleService rules;

    @Inject
    RepositoryIdentityRepository identities;

    @Inject
    ObjectMapper objectMapper;

    @BeforeEach
    @Transactional
    void clearDatabase() {
        RepositoryComplianceResult.deleteAll();
        RepositoryRuleGroupAssignment.deleteAll();
        RepositoryGroup.deleteAll();
        RepositoryStandardRule.deleteAll();
        identities.deleteAll();
    }

    @Test
    @Transactional
    void returnsRuleScopeCountsAndAffectedRepositories() {
        Instant now = Instant.parse("2026-09-18T13:00:00Z");

        identities.insert(
                1L, "erland", "repo-one", "erland/repo-one",
                RepositoryVisibility.PRIVATE, false, false, "main",
                now, now, now);
        identities.insert(
                2L, "erland", "repo-two", "erland/repo-two",
                RepositoryVisibility.PRIVATE, false, false, "main",
                now, now, now);

        rules.save(
                "license-required",
                RepositoryRuleType.LICENSE_REQUIRED,
                "License required",
                "Repositories must have a license.",
                RepositoryRuleSeverity.REQUIRED,
                true,
                Map.of("allowCustom", true),
                RepositoryRuleScope.ALL_REPOSITORIES,
                now);

        RepositoryComplianceResult failed = new RepositoryComplianceResult();
        failed.githubRepositoryId = 1L;
        failed.ruleKey = "license-required";
        failed.result = RepositoryRuleEvaluationResult.FAIL;
        failed.reason = "Missing license";
        failed.observedValue = "MISSING";
        failed.evaluatedAt = now;
        failed.sourceUpdatedAt = now;
        failed.ruleUpdatedAt = now;
        failed.persist();

        RepositoryComplianceResult unknown = new RepositoryComplianceResult();
        unknown.githubRepositoryId = 2L;
        unknown.ruleKey = "license-required";
        unknown.result = RepositoryRuleEvaluationResult.UNKNOWN;
        unknown.reason = "License analysis incomplete";
        unknown.evaluatedAt = now;
        unknown.sourceUpdatedAt = now;
        unknown.ruleUpdatedAt = now;
        unknown.persist();

        ComplianceRuleDetail detail = details.detail("license-required");

        assertEquals("license-required", detail.ruleKey());
        assertEquals(RepositoryRuleScope.ALL_REPOSITORIES, detail.scope());
        assertEquals(1L, detail.resultCounts().get(RepositoryRuleEvaluationResult.FAIL));
        assertEquals(1L, detail.resultCounts().get(RepositoryRuleEvaluationResult.UNKNOWN));
        assertEquals(2, detail.affectedRepositories().size());
        assertTrue(detail.affectedRepositories().stream()
                .anyMatch(repository -> repository.fullName().equals("erland/repo-one")
                        && repository.reason().equals("Missing license")));
    }
}
