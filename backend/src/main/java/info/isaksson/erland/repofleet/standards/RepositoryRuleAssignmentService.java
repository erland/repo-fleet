package info.isaksson.erland.repofleet.standards;

import info.isaksson.erland.repofleet.repository.api.RepositorySummary;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class RepositoryRuleAssignmentService {

    private final RepositoryStandardRuleService rules;
    private final RepositoryGroupService groups;

    @Inject
    public RepositoryRuleAssignmentService(
            RepositoryStandardRuleService rules,
            RepositoryGroupService groups) {
        this.rules = rules;
        this.groups = groups;
    }

    @Transactional
    public void assign(String ruleKey, String groupKey, Instant now) {
        RepositoryStandardRule rule = RepositoryStandardRule.find("ruleKey", ruleKey)
                .firstResultOptional()
                .map(RepositoryStandardRule.class::cast)
                .orElseThrow(() -> new IllegalArgumentException("Unknown rule: " + ruleKey));
        RepositoryGroup group = RepositoryGroup.find("groupKey", groupKey)
                .firstResultOptional()
                .map(RepositoryGroup.class::cast)
                .orElseThrow(() -> new IllegalArgumentException("Unknown group: " + groupKey));

        boolean exists = RepositoryRuleGroupAssignment.count(
                "ruleKey = ?1 and groupKey = ?2",
                rule.ruleKey,
                group.groupKey) > 0;
        if (!exists) {
            RepositoryRuleGroupAssignment assignment = new RepositoryRuleGroupAssignment();
            assignment.ruleKey = rule.ruleKey;
            assignment.groupKey = group.groupKey;
            assignment.createdAt = now;
            assignment.persist();
        }
    }

    @Transactional
    public List<ApplicableRepositoryRule> applicableRules(RepositorySummary repository) {
        List<RepositoryStandardRuleDefinition> enabledRules = rules.list().stream()
                .filter(RepositoryStandardRuleDefinition::enabled)
                .toList();

        Map<String, ApplicableRepositoryRule> applicable = new LinkedHashMap<>();

        for (RepositoryStandardRuleDefinition rule : enabledRules) {
            if (rule.scope() == RepositoryRuleScope.ALL_REPOSITORIES) {
                applicable.put(
                        rule.ruleKey(),
                        new ApplicableRepositoryRule(
                                rule,
                                ApplicableRepositoryRule.ApplicationReason.ALL_REPOSITORIES,
                                List.of()));
            }
        }

        List<String> matchingGroupKeys = groups.matchingGroups(repository).stream()
                .map(RepositoryGroupDefinition::groupKey)
                .sorted()
                .toList();

        if (!matchingGroupKeys.isEmpty()) {
            List<RepositoryRuleGroupAssignment> assignments =
                    RepositoryRuleGroupAssignment.list("groupKey in ?1", matchingGroupKeys);
            Map<String, List<String>> groupsByRule = assignments.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            assignment -> assignment.ruleKey,
                            java.util.TreeMap::new,
                            java.util.stream.Collectors.mapping(
                                    assignment -> assignment.groupKey,
                                    java.util.stream.Collectors.toCollection(java.util.TreeSet::new))));

            Map<String, RepositoryStandardRuleDefinition> ruleByKey = enabledRules.stream()
                    .collect(java.util.stream.Collectors.toMap(
                            RepositoryStandardRuleDefinition::ruleKey,
                            rule -> rule));

            for (Map.Entry<String, List<String>> entry : groupsByRule.entrySet()) {
                if (applicable.containsKey(entry.getKey())) {
                    continue;
                }
                RepositoryStandardRuleDefinition rule = ruleByKey.get(entry.getKey());
                if (rule != null) {
                    applicable.put(
                            rule.ruleKey(),
                            new ApplicableRepositoryRule(
                                    rule,
                                    ApplicableRepositoryRule.ApplicationReason.MATCHING_GROUP,
                                    entry.getValue()));
                }
            }
        }

        return applicable.values().stream()
                .sorted(java.util.Comparator.comparing(item -> item.rule().ruleKey()))
                .toList();
    }
}
