package info.isaksson.erland.repofleet.standards;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class RepositoryStandardRuleService {

    private final ObjectMapper objectMapper;

    @Inject
    public RepositoryStandardRuleService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Transactional
    public RepositoryStandardRuleDefinition save(
            String ruleKey,
            RepositoryRuleType ruleType,
            String name,
            String description,
            RepositoryRuleSeverity severity,
            boolean enabled,
            Map<String, Object> parameters,
            RepositoryRuleScope scope,
            Instant now) {
        validate(ruleKey, ruleType, name, severity, scope);

        RepositoryStandardRule entity = RepositoryStandardRule
                .find("ruleKey", ruleKey)
                .firstResultOptional()
                .map(RepositoryStandardRule.class::cast)
                .orElseGet(RepositoryStandardRule::new);

        if (entity.id == null) {
            entity.ruleKey = ruleKey;
            entity.createdAt = now;
        }
        entity.ruleType = ruleType;
        entity.name = name;
        entity.description = description;
        entity.severity = severity;
        entity.enabled = enabled;
        entity.parametersJson = writeParameters(parameters);
        entity.scope = scope;
        entity.updatedAt = now;

        if (entity.id == null) {
            entity.persist();
        }
        return toDefinition(entity);
    }

    @Transactional
    public List<RepositoryStandardRuleDefinition> list() {
        return RepositoryStandardRule.find("order by ruleKey")
                .list()
                .stream()
                .map(entity -> toDefinition((RepositoryStandardRule) entity))
                .toList();
    }

    private RepositoryStandardRuleDefinition toDefinition(RepositoryStandardRule entity) {
        return new RepositoryStandardRuleDefinition(
                entity.ruleKey,
                entity.ruleType,
                entity.name,
                entity.description,
                entity.severity,
                entity.enabled,
                readParameters(entity.parametersJson),
                entity.scope,
                entity.createdAt,
                entity.updatedAt);
    }

    private void validate(
            String ruleKey,
            RepositoryRuleType ruleType,
            String name,
            RepositoryRuleSeverity severity,
            RepositoryRuleScope scope) {
        if (ruleKey == null || ruleKey.isBlank()) {
            throw new IllegalArgumentException("ruleKey must not be blank");
        }
        if (ruleType == null) {
            throw new IllegalArgumentException("ruleType must not be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (severity == null) {
            throw new IllegalArgumentException("severity must not be null");
        }
        if (scope == null) {
            throw new IllegalArgumentException("scope must not be null");
        }
    }

    private String writeParameters(Map<String, Object> parameters) {
        try {
            return objectMapper.writeValueAsString(parameters == null ? Map.of() : parameters);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Could not serialize rule parameters", exception);
        }
    }

    private Map<String, Object> readParameters(String parametersJson) {
        try {
            return objectMapper.readValue(
                    parametersJson == null || parametersJson.isBlank() ? "{}" : parametersJson,
                    new TypeReference<Map<String, Object>>() {});
        } catch (Exception exception) {
            throw new IllegalStateException("Could not deserialize rule parameters", exception);
        }
    }
}
