package info.isaksson.erland.repofleet.standards;

import info.isaksson.erland.repofleet.repository.persistence.RepositoryEnrichmentSnapshotService;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryIdentity;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryIdentityRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.EnumMap;
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
    private final RepositoryComplianceExceptionService exceptions;
    private final RepositoryComplianceResultService complianceResults;
    private final RepositoryStandardRuleService standardRules;

    @Inject
    public RepositoryComplianceSummaryService(
            RepositoryIdentityRepository identities,
            RepositoryEnrichmentSnapshotService snapshots,
            RepositoryGroupService groups,
            RepositoryComplianceExceptionService exceptions,
            RepositoryComplianceResultService complianceResults,
            RepositoryStandardRuleService standardRules) {
        this.identities = identities;
        this.snapshots = snapshots;
        this.groups = groups;
        this.exceptions = exceptions;
        this.complianceResults = complianceResults;
        this.standardRules = standardRules;
    }

    @Transactional
    public CompliancePortfolioSummary summarize() {
        Map<Long, RepositoryIdentity> repositoriesById =
                identities.list("active", true).stream()
                        .collect(Collectors.toMap(identity -> identity.githubRepositoryId, Function.identity()));
        Set<Long> activeRepositoryIds = repositoriesById.keySet();

        List<StoredRepositoryComplianceResult> results =
                complianceResults.listAll().stream()
                        .filter(result -> activeRepositoryIds.contains(result.githubRepositoryId()))
                        .toList();

        Set<String> activeExceptionKeys = exceptions.activeKeys();
        java.util.function.Predicate<StoredRepositoryComplianceResult> acceptedDeviation =
                result -> result.evaluation().result() == RepositoryRuleEvaluationResult.FAIL
                        && activeExceptionKeys.contains(exceptions.key(
                                result.githubRepositoryId(),
                                result.evaluation().ruleKey()));
        List<StoredRepositoryComplianceResult> actionableResults = results.stream()
                .filter(result -> !acceptedDeviation.test(result))
                .toList();
        Map<Long, List<StoredRepositoryComplianceResult>> actionableResultsByRepositoryId =
                actionableResults.stream()
                        .collect(Collectors.groupingBy(result -> result.githubRepositoryId()));
        Map<String, List<StoredRepositoryComplianceResult>> actionableResultsByRuleKey =
                actionableResults.stream()
                        .collect(Collectors.groupingBy(result -> result.evaluation().ruleKey()));
        Map<String, List<StoredRepositoryComplianceResult>> resultsByRuleKey =
                results.stream()
                        .collect(Collectors.groupingBy(result -> result.evaluation().ruleKey()));
        long acceptedDeviationCount = results.stream()
                .filter(acceptedDeviation)
                .count();

        Map<String, RepositoryStandardRuleDefinition> rulesByKey =
                standardRules.list().stream()
                        .collect(Collectors.toMap(
                                RepositoryStandardRuleDefinition::ruleKey,
                                Function.identity()));

        Map<RepositoryRuleEvaluationResult, Long> resultCounts =
                emptyResultCounts();
        Map<RepositoryRuleSeverity, Map<RepositoryRuleEvaluationResult, Long>> severityResultCounts =
                emptySeverityResultCounts();

        for (StoredRepositoryComplianceResult result : actionableResults) {
            increment(resultCounts, result.evaluation().result());
            RepositoryStandardRuleDefinition rule = rulesByKey.get(result.evaluation().ruleKey());
            if (rule != null) {
                increment(severityResultCounts.get(rule.severity()), result.evaluation().result());
            }
        }

        List<ComplianceRepositoryFailureSummary> repositoryFailures =
                repositoriesById.values().stream()
                        .map(repository -> new ComplianceRepositoryFailureSummary(
                                repository.githubRepositoryId,
                                repository.fullName,
                                actionableResultsByRepositoryId
                                        .getOrDefault(repository.githubRepositoryId, List.of())
                                        .stream()
                                        .filter(result -> result.evaluation().result() == RepositoryRuleEvaluationResult.FAIL)
                                        .filter(result -> {
                                            RepositoryStandardRuleDefinition rule = rulesByKey.get(result.evaluation().ruleKey());
                                            return rule != null && rule.severity() == RepositoryRuleSeverity.REQUIRED;
                                        })
                                        .count()))
                        .filter(summary -> summary.requiredFailureCount() > 0)
                        .sorted(java.util.Comparator
                                .comparingLong(ComplianceRepositoryFailureSummary::requiredFailureCount)
                                .reversed()
                                .thenComparing(ComplianceRepositoryFailureSummary::fullName))
                        .toList();

        List<ComplianceRuleSummary> ruleSummaries = rulesByKey.values().stream()
                .filter(rule -> rule.enabled())
                .sorted(java.util.Comparator.comparing(rule -> rule.ruleKey()))
                .map(rule -> {
                    Map<RepositoryRuleEvaluationResult, Long> counts = emptyResultCounts();
                    actionableResultsByRuleKey
                            .getOrDefault(rule.ruleKey(), List.of())
                            .forEach(result -> increment(counts, result.evaluation().result()));
                    long ruleAcceptedDeviations = resultsByRuleKey
                            .getOrDefault(rule.ruleKey(), List.of())
                            .stream()
                            .filter(acceptedDeviation)
                            .count();
                    return new ComplianceRuleSummary(
                            rule.ruleKey(),
                            rule.name(),
                            rule.severity(),
                            ruleAcceptedDeviations,
                            counts);
                })
                .toList();

        Map<Long, info.isaksson.erland.repofleet.repository.api.RepositorySummary> reconstructedRepositoriesById =
                repositoriesById.values().stream()
                        .map(snapshots::reconstruct)
                        .collect(Collectors.toMap(
                                repository -> repository.id(),
                                Function.identity()));

        List<ComplianceGroupSummary> groupSummaries = new ArrayList<>();
        for (RepositoryGroupDefinition group : groups.list().stream()
                .filter(RepositoryGroupDefinition::enabled)
                .toList()) {
            Set<Long> memberIds = reconstructedRepositoriesById.values().stream()
                    .filter(repository -> groups.matchingGroups(repository).stream()
                            .anyMatch(match -> match.groupKey().equals(group.groupKey())))
                    .map(repository -> repository.id())
                    .collect(Collectors.toSet());

            Map<RepositoryRuleEvaluationResult, Long> counts = emptyResultCounts();
            actionableResults.stream()
                    .filter(result -> memberIds.contains(result.githubRepositoryId()))
                    .forEach(result -> increment(counts, result.evaluation().result()));
            long groupAcceptedDeviations = results.stream()
                    .filter(result -> memberIds.contains(result.githubRepositoryId()))
                    .filter(acceptedDeviation)
                    .count();

            groupSummaries.add(new ComplianceGroupSummary(
                    group.groupKey(),
                    group.name(),
                    memberIds.size(),
                    groupAcceptedDeviations,
                    counts));
        }
        groupSummaries.sort(java.util.Comparator.comparing(ComplianceGroupSummary::groupKey));

        return new CompliancePortfolioSummary(
                repositoriesById.size(),
                results.size(),
                acceptedDeviationCount,
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
