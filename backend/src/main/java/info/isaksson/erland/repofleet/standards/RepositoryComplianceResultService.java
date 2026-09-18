package info.isaksson.erland.repofleet.standards;

import info.isaksson.erland.repofleet.repository.api.RepositorySummary;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryEnrichmentSnapshotRepository;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryIdentityRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class RepositoryComplianceResultService {

    private final RepositoryRuleAssignmentService assignments;
    private final RepositoryRuleEvaluator evaluator;
    private final RepositoryEnrichmentSnapshotRepository snapshots;
    private final RepositoryIdentityRepository identities;
    private final Clock clock;

    @Inject
    public RepositoryComplianceResultService(
            RepositoryRuleAssignmentService assignments,
            RepositoryRuleEvaluator evaluator,
            RepositoryEnrichmentSnapshotRepository snapshots,
            RepositoryIdentityRepository identities) {
        this(assignments, evaluator, snapshots, identities, Clock.systemUTC());
    }

    RepositoryComplianceResultService(
            RepositoryRuleAssignmentService assignments,
            RepositoryRuleEvaluator evaluator,
            RepositoryEnrichmentSnapshotRepository snapshots,
            RepositoryIdentityRepository identities,
            Clock clock) {
        this.assignments = assignments;
        this.evaluator = evaluator;
        this.snapshots = snapshots;
        this.identities = identities;
        this.clock = clock;
    }

    @Transactional
    public List<StoredRepositoryComplianceResult> evaluateAndPersist(RepositorySummary repository) {
        Instant sourceUpdatedAt = snapshots.findByGitHubRepositoryId(repository.id())
                .map(snapshot -> snapshot.updatedAt)
                .orElseGet(() -> identities.findByGitHubRepositoryId(repository.id())
                        .map(identity -> identity.lastSeenAt)
                        .orElse(null));

        List<ApplicableRepositoryRule> applicableRules = assignments.applicableRules(repository);
        java.util.Set<String> applicableRuleKeys = applicableRules.stream()
                .map(item -> item.rule().ruleKey())
                .collect(java.util.stream.Collectors.toSet());

        if (applicableRuleKeys.isEmpty()) {
            RepositoryComplianceResult.delete("githubRepositoryId", repository.id());
        } else {
            RepositoryComplianceResult.delete(
                    "githubRepositoryId = ?1 and ruleKey not in ?2",
                    repository.id(),
                    applicableRuleKeys);
        }

        List<StoredRepositoryComplianceResult> results = new ArrayList<>();
        for (ApplicableRepositoryRule applicable : applicableRules) {
            RepositoryStandardRuleDefinition rule = applicable.rule();
            RepositoryComplianceResult existing = RepositoryComplianceResult.find(
                            "githubRepositoryId = ?1 and ruleKey = ?2",
                            repository.id(),
                            rule.ruleKey())
                    .firstResultOptional()
                    .map(RepositoryComplianceResult.class::cast)
                    .orElse(null);

            if (existing != null
                    && Objects.equals(existing.sourceUpdatedAt, sourceUpdatedAt)
                    && Objects.equals(existing.ruleUpdatedAt, rule.updatedAt())) {
                results.add(toStored(existing, rule));
                continue;
            }

            Instant evaluatedAt = clock.instant();
            RepositoryRuleEvaluation evaluation =
                    evaluator.evaluate(repository, rule, evaluatedAt);

            RepositoryComplianceResult entity =
                    existing == null ? new RepositoryComplianceResult() : existing;
            entity.githubRepositoryId = repository.id();
            entity.ruleKey = rule.ruleKey();
            entity.result = evaluation.result();
            entity.reason = evaluation.reason();
            entity.observedValue = evaluation.observedValue();
            entity.evaluatedAt = evaluatedAt;
            entity.sourceUpdatedAt = sourceUpdatedAt;
            entity.ruleUpdatedAt = rule.updatedAt();
            if (entity.id == null) {
                entity.persist();
            }

            results.add(new StoredRepositoryComplianceResult(
                    repository.id(),
                    evaluation,
                    evaluatedAt,
                    sourceUpdatedAt,
                    rule.updatedAt()));
        }

        return results.stream()
                .sorted(java.util.Comparator.comparing(item -> item.evaluation().ruleKey()))
                .toList();
    }

    @Transactional
    public List<StoredRepositoryComplianceResult> listForRepository(long githubRepositoryId) {
        return RepositoryComplianceResult.list(
                        "githubRepositoryId = ?1 order by ruleKey",
                        githubRepositoryId)
                .stream()
                .map(entity -> {
                    RepositoryComplianceResult result = (RepositoryComplianceResult) entity;
                    RepositoryStandardRule rule = RepositoryStandardRule.find(
                                    "ruleKey",
                                    result.ruleKey)
                            .firstResultOptional()
                            .map(RepositoryStandardRule.class::cast)
                            .orElse(null);
                    if (rule == null) {
                        throw new IllegalStateException("Stored compliance result references missing rule: " + result.ruleKey);
                    }
                    return toStored(result, toDefinition(rule));
                })
                .toList();
    }

    private StoredRepositoryComplianceResult toStored(
            RepositoryComplianceResult entity,
            RepositoryStandardRuleDefinition rule) {
        return new StoredRepositoryComplianceResult(
                entity.githubRepositoryId,
                new RepositoryRuleEvaluation(
                        entity.ruleKey,
                        rule.ruleType(),
                        rule.severity(),
                        entity.result,
                        entity.reason,
                        entity.observedValue),
                entity.evaluatedAt,
                entity.sourceUpdatedAt,
                entity.ruleUpdatedAt);
    }

    private RepositoryStandardRuleDefinition toDefinition(RepositoryStandardRule entity) {
        return new RepositoryStandardRuleDefinition(
                entity.ruleKey,
                entity.ruleType,
                entity.name,
                entity.description,
                entity.severity,
                entity.enabled,
                java.util.Map.of(),
                entity.scope,
                entity.createdAt,
                entity.updatedAt);
    }
}
