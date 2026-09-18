package info.isaksson.erland.repofleet.standards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
class RepositoryComplianceExceptionServiceTest {

    @Inject
    RepositoryComplianceExceptionService exceptions;

    @Inject
    RepositoryStandardRuleService rules;

    @Inject
    RepositoryIdentityRepository identities;

    @BeforeEach
    @Transactional
    void clearDatabase() {
        RepositoryComplianceException.deleteAll();
        RepositoryComplianceResult.deleteAll();
        RepositoryRuleGroupAssignment.deleteAll();
        RepositoryGroup.deleteAll();
        RepositoryStandardRule.deleteAll();
        identities.deleteAll();
    }

    @Test
    @Transactional
    void persistsActiveAndExpiredAcceptedDeviations() {
        Instant now = Instant.now();
        identities.insert(
                1001L,
                "erland",
                "repo",
                "erland/repo",
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

        var active = exceptions.save(
                1001L,
                "license-required",
                "Accepted while legacy license review is pending.",
                Instant.parse("2099-01-01T00:00:00Z"),
                now);

        assertEquals(RepositoryComplianceExceptionState.ACTIVE, active.state());

        var expired = exceptions.save(
                1001L,
                "license-required",
                "Expired deviation.",
                Instant.parse("2000-01-01T00:00:00Z"),
                now.plusSeconds(1));

        assertEquals(RepositoryComplianceExceptionState.EXPIRED, expired.state());
    }

    @Test
    @Transactional
    void expiresAndRemovesAcceptedDeviation() {
        Instant now = Instant.now();
        identities.insert(
                2002L,
                "erland",
                "repo-two",
                "erland/repo-two",
                RepositoryVisibility.PRIVATE,
                false,
                false,
                "main",
                now,
                now,
                now);

        rules.save(
                "actions-required",
                RepositoryRuleType.ACTIONS_WORKFLOW_REQUIRED,
                "Actions required",
                null,
                RepositoryRuleSeverity.REQUIRED,
                true,
                Map.of(),
                RepositoryRuleScope.ALL_REPOSITORIES,
                now);

        exceptions.save(
                2002L,
                "actions-required",
                "Temporary accepted deviation.",
                null,
                now);

        assertTrue(exceptions.hasActiveException(2002L, "actions-required"));
        assertTrue(exceptions.expire(2002L, "actions-required", now));
        assertFalse(exceptions.hasActiveException(2002L, "actions-required"));
        assertTrue(exceptions.remove(2002L, "actions-required"));
        assertTrue(exceptions.listForRepository(2002L).isEmpty());
    }
}
