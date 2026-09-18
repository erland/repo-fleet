package info.isaksson.erland.repofleet.standards;

import static org.junit.jupiter.api.Assertions.assertEquals;

import info.isaksson.erland.repofleet.repository.api.AnalysisState;
import info.isaksson.erland.repofleet.repository.api.GitHubActionsStatus;
import info.isaksson.erland.repofleet.repository.api.LicensePresence;
import info.isaksson.erland.repofleet.repository.api.LicenseStatus;
import info.isaksson.erland.repofleet.repository.api.ReleaseStatus;
import info.isaksson.erland.repofleet.repository.api.RepositoryRefreshStatus;
import info.isaksson.erland.repofleet.repository.api.RepositorySummary;
import info.isaksson.erland.repofleet.repository.api.RepositoryVisibility;
import info.isaksson.erland.repofleet.repository.api.ActivityStatus;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryEnrichmentSnapshotRepository;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryEnrichmentSnapshotService;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryIdentityRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class RepositoryComplianceSummaryServiceTest {

    @Inject
    RepositoryComplianceSummaryService summaries;

    @Inject
    RepositoryIdentityRepository identities;

    @Inject
    RepositoryEnrichmentSnapshotRepository snapshots;

    @Inject
    RepositoryEnrichmentSnapshotService snapshotService;

    @Inject
    RepositoryStandardRuleService rules;

    @Inject
    RepositoryGroupService groups;

    @BeforeEach
    @Transactional
    void clearDatabase() {
        RepositoryComplianceResult.deleteAll();
        RepositoryRuleGroupAssignment.deleteAll();
        RepositoryGroup.deleteAll();
        RepositoryStandardRule.deleteAll();
        snapshots.deleteAll();
        identities.deleteAll();
    }

    @Test
    @Transactional
    void summarizesPortfolioSeverityGroupsRepositoriesAndRules() {
        Instant now = Instant.parse("2026-09-18T11:00:00Z");
        persistRepository(repository(1L, "svc-one", List.of("architecture")), now);
        persistRepository(repository(2L, "lib-two", List.of("library")), now);

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
        rules.save(
                "release-recommended",
                RepositoryRuleType.PUBLISHED_RELEASE_REQUIRED,
                "Release recommended",
                null,
                RepositoryRuleSeverity.RECOMMENDED,
                true,
                Map.of(),
                RepositoryRuleScope.ALL_REPOSITORIES,
                now);

        groups.save(
                "services",
                "Services",
                null,
                new RepositoryGroupSelector(
                        "svc-",
                        null,
                        null,
                        null,
                        null,
                        List.of(),
                        List.of()),
                true,
                now);

        persistResult(1L, "license-required", RepositoryRuleEvaluationResult.FAIL, now);
        persistResult(1L, "release-recommended", RepositoryRuleEvaluationResult.PASS, now);
        persistResult(2L, "license-required", RepositoryRuleEvaluationResult.PASS, now);
        persistResult(2L, "release-recommended", RepositoryRuleEvaluationResult.UNKNOWN, now);

        CompliancePortfolioSummary summary = summaries.summarize();

        assertEquals(2, summary.repositoryCount());
        assertEquals(4, summary.evaluatedRuleCount());
        assertEquals(1L, summary.resultCounts().get(RepositoryRuleEvaluationResult.FAIL));
        assertEquals(
                1L,
                summary.severityResultCounts()
                        .get(RepositoryRuleSeverity.REQUIRED)
                        .get(RepositoryRuleEvaluationResult.FAIL));

        assertEquals(1, summary.repositoriesWithMostRequiredFailures().size());
        assertEquals(1L, summary.repositoriesWithMostRequiredFailures().getFirst().githubRepositoryId());
        assertEquals(1L, summary.repositoriesWithMostRequiredFailures().getFirst().requiredFailureCount());

        assertEquals(2, summary.rules().size());
        ComplianceRuleSummary licenseRule = summary.rules().stream()
                .filter(rule -> rule.ruleKey().equals("license-required"))
                .findFirst()
                .orElseThrow();
        assertEquals(1L, licenseRule.resultCounts().get(RepositoryRuleEvaluationResult.FAIL));
        assertEquals(1L, licenseRule.resultCounts().get(RepositoryRuleEvaluationResult.PASS));

        assertEquals(1, summary.groups().size());
        ComplianceGroupSummary services = summary.groups().getFirst();
        assertEquals("services", services.groupKey());
        assertEquals(1L, services.repositoryCount());
        assertEquals(1L, services.resultCounts().get(RepositoryRuleEvaluationResult.FAIL));
        assertEquals(1L, services.resultCounts().get(RepositoryRuleEvaluationResult.PASS));
    }

    private void persistRepository(
            RepositorySummary repository,
            Instant now) {
        identities.insert(
                repository.id(),
                repository.owner(),
                repository.name(),
                repository.fullName(),
                repository.visibility(),
                repository.archived(),
                repository.fork(),
                repository.defaultBranch(),
                now,
                now,
                now);
        snapshotService.saveSnapshot(repository, now, null);
    }

    private void persistResult(
            long repositoryId,
            String ruleKey,
            RepositoryRuleEvaluationResult result,
            Instant now) {
        RepositoryComplianceResult entity = new RepositoryComplianceResult();
        entity.githubRepositoryId = repositoryId;
        entity.ruleKey = ruleKey;
        entity.result = result;
        entity.reason = result.name();
        entity.evaluatedAt = now;
        entity.sourceUpdatedAt = now;
        entity.ruleUpdatedAt = now;
        entity.persist();
    }

    private RepositorySummary repository(
            long id,
            String name,
            List<String> topics) {
        Instant now = Instant.parse("2026-09-18T11:00:00Z");
        return new RepositorySummary(
                id,
                "erland",
                name,
                "erland/" + name,
                "https://github.com/erland/" + name,
                RepositoryVisibility.PRIVATE,
                false,
                false,
                "main",
                topics,
                List.of("Java"),
                "Java",
                new LicenseStatus(
                        AnalysisState.COMPLETE,
                        LicensePresence.PRESENT,
                        true,
                        "mit",
                        "MIT"),
                new GitHubActionsStatus(AnalysisState.COMPLETE, true, 1),
                new ReleaseStatus(AnalysisState.COMPLETE, true, "v1", "v1", now, false),
                new ActivityStatus(now, now),
                new RepositoryRefreshStatus(AnalysisState.COMPLETE, "complete"));
    }
}
