package info.isaksson.erland.repofleet.standards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class RepositoryStandardRuleServiceTest {

    @Inject
    RepositoryStandardRuleService rules;

    @BeforeEach
    @Transactional
    void clearDatabase() {
        RepositoryStandardRule.deleteAll();
    }

    @Test
    void storesRetrievesAndUpdatesRuleByStableKey() {
        Instant createdAt = Instant.parse("2026-09-18T09:00:00Z");
        Instant updatedAt = Instant.parse("2026-09-18T10:00:00Z");

        rules.save(
                "license-required",
                RepositoryRuleType.LICENSE_REQUIRED,
                "LICENSE required",
                "Repositories should contain a license.",
                RepositoryRuleSeverity.REQUIRED,
                true,
                Map.of(),
                RepositoryRuleScope.ALL_REPOSITORIES,
                createdAt);

        rules.save(
                "license-required",
                RepositoryRuleType.LICENSE_REQUIRED,
                "License file required",
                "Repositories must contain a license file.",
                RepositoryRuleSeverity.REQUIRED,
                true,
                Map.of("allowCustom", true),
                RepositoryRuleScope.ALL_REPOSITORIES,
                updatedAt);

        var all = rules.list();

        assertEquals(1, all.size());
        var rule = all.getFirst();
        assertEquals("license-required", rule.ruleKey());
        assertEquals(RepositoryRuleType.LICENSE_REQUIRED, rule.ruleType());
        assertEquals("License file required", rule.name());
        assertEquals(RepositoryRuleSeverity.REQUIRED, rule.severity());
        assertTrue(rule.enabled());
        assertEquals(true, rule.parameters().get("allowCustom"));
        assertEquals(RepositoryRuleScope.ALL_REPOSITORIES, rule.scope());
        assertEquals(createdAt, rule.createdAt());
        assertEquals(updatedAt, rule.updatedAt());
    }

    @Test
    void supportsParameterizedRuleTypes() {
        rules.save(
                "inactive-after-days",
                RepositoryRuleType.MAXIMUM_INACTIVITY_AGE,
                "Maximum inactivity",
                null,
                RepositoryRuleSeverity.RECOMMENDED,
                false,
                Map.of("days", 180),
                RepositoryRuleScope.ALL_REPOSITORIES,
                Instant.parse("2026-09-18T09:00:00Z"));

        var rule = rules.list().getFirst();

        assertEquals(180, ((Number) rule.parameters().get("days")).intValue());
        assertEquals(RepositoryRuleSeverity.RECOMMENDED, rule.severity());
        assertTrue(!rule.enabled());
    }
}
