package info.isaksson.erland.repofleet.standards;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryIdentity;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryIdentityRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class ComplianceRuleDetailService {

    private final RepositoryIdentityRepository identities;
    private final ObjectMapper objectMapper;
    private final RepositoryComplianceExceptionService exceptions;

    @Inject
    public ComplianceRuleDetailService(
            RepositoryIdentityRepository identities,
            ObjectMapper objectMapper,
            RepositoryComplianceExceptionService exceptions) {
        this.identities = identities;
        this.objectMapper = objectMapper;
        this.exceptions = exceptions;
    }

    @Transactional
    public ComplianceRuleDetail detail(String ruleKey) {
        RepositoryStandardRule rule = RepositoryStandardRule.find("ruleKey", ruleKey)
                .firstResultOptional()
                .map(RepositoryStandardRule.class::cast)
                .orElse(null);
        if (rule == null) {
            return null;
        }

        List<String> groups = RepositoryRuleGroupAssignment.list(
                        "ruleKey = ?1 order by groupKey",
                        ruleKey)
                .stream()
                .map(entity -> ((RepositoryRuleGroupAssignment) entity).groupKey)
                .toList();

        List<RepositoryComplianceResult> results = RepositoryComplianceResult.list(
                        "ruleKey",
                        ruleKey)
                .stream()
                .map(RepositoryComplianceResult.class::cast)
                .toList();

        Map<Long, RepositoryIdentity> repositories = identities.list("active", true).stream()
                .collect(Collectors.toMap(
                        identity -> identity.githubRepositoryId,
                        Function.identity()));

        Map<RepositoryRuleEvaluationResult, Long> counts =
                new EnumMap<>(RepositoryRuleEvaluationResult.class);
        for (RepositoryRuleEvaluationResult result : RepositoryRuleEvaluationResult.values()) {
            counts.put(result, 0L);
        }

        for (RepositoryComplianceResult result : results) {
            if (!repositories.containsKey(result.githubRepositoryId)) {
                continue;
            }
            counts.compute(result.result, (key, value) -> value == null ? 1L : value + 1L);
        }

        Map<Long, RepositoryComplianceExceptionDefinition> activeExceptions =
                exceptions.listForRule(ruleKey).stream()
                        .filter(exception -> exception.state() == RepositoryComplianceExceptionState.ACTIVE)
                        .collect(Collectors.toMap(
                                RepositoryComplianceExceptionDefinition::githubRepositoryId,
                                exception -> exception));

        List<ComplianceRuleAffectedRepository> affectedRepositories = results.stream()
                .filter(result -> repositories.containsKey(result.githubRepositoryId))
                .filter(result -> result.result == RepositoryRuleEvaluationResult.FAIL
                        || result.result == RepositoryRuleEvaluationResult.UNKNOWN)
                .map(result -> {
                    RepositoryIdentity repository = repositories.get(result.githubRepositoryId);
                    RepositoryComplianceExceptionDefinition exception =
                            activeExceptions.get(result.githubRepositoryId);
                    return new ComplianceRuleAffectedRepository(
                            result.githubRepositoryId,
                            repository.fullName,
                            result.result,
                            result.reason,
                            result.observedValue,
                            exception != null,
                            exception == null ? null : exception.reason(),
                            exception == null ? null : exception.expiresAt());
                })
                .sorted(java.util.Comparator
                        .comparing(ComplianceRuleAffectedRepository::result)
                        .thenComparing(ComplianceRuleAffectedRepository::fullName))
                .toList();

        return new ComplianceRuleDetail(
                rule.ruleKey,
                rule.name,
                rule.description,
                rule.ruleType,
                rule.severity,
                rule.scope,
                readParameters(rule.parametersJson),
                groups,
                counts,
                affectedRepositories);
    }

    private Map<String, Object> readParameters(String json) {
        try {
            return objectMapper.readValue(
                    json == null || json.isBlank() ? "{}" : json,
                    new TypeReference<Map<String, Object>>() {});
        } catch (Exception exception) {
            throw new IllegalStateException("Could not deserialize rule parameters", exception);
        }
    }
}
