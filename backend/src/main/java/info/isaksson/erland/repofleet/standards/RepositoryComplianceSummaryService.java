package info.isaksson.erland.repofleet.standards;

import info.isaksson.erland.repofleet.repository.persistence.RepositoryEnrichmentSnapshotService;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryIdentity;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryIdentityRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class RepositoryComplianceSummaryService {

    private final RepositoryIdentityRepository identities;
    private final RepositoryEnrichmentSnapshotService snapshots;
    private final RepositoryGroupService groups;

    @Inject
    public RepositoryComplianceSummaryService(
            RepositoryIdentityRepository identities,
            RepositoryEnrichmentSnapshotService snapshots,
            RepositoryGroupService groups) {
        this.identities = identities;
        this.snapshots = snapshots;
        this.groups = groups;
    }

    @Transactional
    public CompliancePortfolioSummary summarize() {
        Map<Long, RepositoryIdentity> repositoriesById =
                identities.list("active", true).stream()
                        .collect(Collectors.toMap(identity -> identity.githubRepositoryId, Function.identity()));
        Set<Long> activeRepositoryIds = repositoriesById.keySet();

        List<RepositoryComplianceResult> results =
                RepositoryComplianceResult.listAll().stream()
                        .map(RepositoryComplianceResult.class::cast)
                        .filter(result -> activeRepositoryIds.contains(result.githubRepositoryId))
                        .toList();

        Map<String, RepositoryStandardRule> rulesByKey =
                RepositoryStandardRule.listAll().stream()
                        .map(RepositoryStandardRule.class::cast)
                        .collect(Collectors.toMap(rule -> rule.ruleKey, Function.identity()));

        Map<RepositoryRuleEvaluationResult, Long> resultCounts =
                emptyResultCounts();
        Map<RepositoryRuleSeverity, Map<RepositoryRuleEvaluationResult, Long>> severityResultCounts =
                emptySeverityResultCounts();

        for (RepositoryComplianceResult result : results) {
            increment(resultCounts, result.result);
            RepositoryStandardRule rule = rulesByKey.get(result.ruleKey);
            if (rule != null) {
                increment(severityResultCounts.get(rule.severity), result.result);
            }
        }

        List<ComplianceRepositoryFailureSummary> repositoryFailures =
                repositoriesById.values().stream()
                        .map(repository -> new ComplianceRepositoryFailureSummary(
                                repository.githubRepositoryId,
                                repository.fullName,
                                results.stream()
                                        .filter(result -> result.githubRepositoryId == repository.githubRepositoryId)
                                        .filter(result -> result.result == RepositoryRuleEvaluationResult.FAIL)
                                        .filter(result -> {
                                            RepositoryStandardRule rule = rulesByKey.get(result.ruleKey);
                                            return rule != null && rule.severity == RepositoryRuleSeverity.REQUIRED;
                                        })
                                        .count()))
                        .filter(summary -> summary.requiredFailureCount() > 0)
                        .sorted(java.util.Comparator
                                .comparingLong(ComplianceRepositoryFailureSummary::requiredFailureCount)
                                .reversed()
                                .thenComparing(ComplianceRepositoryFailureSummary::fullName))
                        .toList();

        List<ComplianceRuleSummary> ruleSummaries = rulesByKey.values().stream()
                .filter(rule -> rule.enabled)
                .sorted(java.util.Comparator.comparing(rule -> rule.ruleKey))
                .map(rule -> {
                    Map<RepositoryRuleEvaluationResult, Long> counts = emptyResultCounts();
                    results.stream()
                            .filter(result -> result.ruleKey.equals(rule.ruleKey))
                            .forEach(result -> increment(counts, result.result));
                    return new ComplianceRuleSummary(
                            rule.ruleKey,
                            rule.name,
                            rule.severity,
                            counts);
                })
                .toList();

        List<ComplianceGroupSummary> groupSummaries = new ArrayList<>();
        for (RepositoryGroupDefinition group : groups.list().stream()
                .filter(RepositoryGroupDefinition::enabled)
                .toList()) {
            Set<Long> memberIds = repositoriesById.values().stream()
                    .map(snapshots::reconstruct)
                    .filter(repository -> groups.matchingGroups(repository).stream()
                            .anyMatch(match -> match.groupKey().equals(group.groupKey())))
                    .map(repository -> repository.id())
                    .collect(Collectors.toSet());

            Map<RepositoryRuleEvaluationResult, Long> counts = emptyResultCounts();
            results.stream()
                    .filter(result -> memberIds.contains(result.githubRepositoryId))
                    .forEach(result -> increment(counts, result.result));

            groupSummaries.add(new ComplianceGroupSummary(
                    group.groupKey(),
                    group.name(),
                    memberIds.size(),
                    counts));
        }
        groupSummaries.sort(java.util.Comparator.comparing(ComplianceGroupSummary::groupKey));

        return new CompliancePortfolioSummary(
                repositoriesById.size(),
                results.size(),
                resultCounts,
                severityResultCounts,
                List.copyOf(groupSummaries),
                repositoryFailures,
                ruleSummaries);
    }

    private Map<RepositoryRuleEvaluationResult, Long> emptyResultCounts() {
        Map<RepositoryRuleEvaluationResult, Long> counts =
                new EnumMap<>(RepositoryRuleEvaluationResult.class);
        for (RepositoryRuleEvaluationResult result : RepositoryRuleEvaluationResult.values()) {
            counts.put(result, 0L);
        }
        return counts;
    }

    private Map<RepositoryRuleSeverity, Map<RepositoryRuleEvaluationResult, Long>>
            emptySeverityResultCounts() {
        Map<RepositoryRuleSeverity, Map<RepositoryRuleEvaluationResult, Long>> counts =
                new EnumMap<>(RepositoryRuleSeverity.class);
        for (RepositoryRuleSeverity severity : RepositoryRuleSeverity.values()) {
            counts.put(severity, emptyResultCounts());
        }
        return counts;
    }

    private void increment(
            Map<RepositoryRuleEvaluationResult, Long> counts,
            RepositoryRuleEvaluationResult result) {
        counts.compute(result, (key, value) -> value == null ? 1L : value + 1L);
    }
}
