package info.isaksson.erland.repofleet.standards;

import com.fasterxml.jackson.databind.ObjectMapper;
import info.isaksson.erland.repofleet.repository.api.RepositorySummary;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class RepositoryGroupService {

    private final ObjectMapper objectMapper;
    private final RepositoryGroupMembershipEvaluator evaluator;

    @Inject
    public RepositoryGroupService(
            ObjectMapper objectMapper,
            RepositoryGroupMembershipEvaluator evaluator) {
        this.objectMapper = objectMapper;
        this.evaluator = evaluator;
    }

    @Transactional
    public RepositoryGroupDefinition save(
            String groupKey,
            String name,
            String description,
            RepositoryGroupSelector selector,
            boolean enabled,
            Instant now) {
        if (groupKey == null || groupKey.isBlank()) {
            throw new IllegalArgumentException("groupKey must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }

        RepositoryGroup entity = RepositoryGroup.find("groupKey", groupKey)
                .firstResultOptional()
                .map(RepositoryGroup.class::cast)
                .orElseGet(RepositoryGroup::new);
        if (entity.id == null) {
            entity.groupKey = groupKey;
            entity.createdAt = now;
        }
        entity.name = name;
        entity.description = description;
        entity.selectorJson = writeSelector(selector);
        entity.enabled = enabled;
        entity.updatedAt = now;
        if (entity.id == null) {
            entity.persist();
        }
        return toDefinition(entity);
    }

    @Transactional
    public List<RepositoryGroupDefinition> list() {
        return RepositoryGroup.find("order by groupKey")
                .list()
                .stream()
                .map(entity -> toDefinition((RepositoryGroup) entity))
                .toList();
    }

    public List<RepositoryGroupDefinition> matchingGroups(RepositorySummary repository) {
        return list().stream()
                .filter(RepositoryGroupDefinition::enabled)
                .filter(group -> evaluator.matches(group.selector(), repository))
                .toList();
    }

    private RepositoryGroupDefinition toDefinition(RepositoryGroup entity) {
        return new RepositoryGroupDefinition(
                entity.groupKey,
                entity.name,
                entity.description,
                readSelector(entity.selectorJson),
                entity.enabled,
                entity.createdAt,
                entity.updatedAt);
    }

    private String writeSelector(RepositoryGroupSelector selector) {
        try {
            return objectMapper.writeValueAsString(
                    selector == null
                            ? new RepositoryGroupSelector(null, null, null, null, null, List.of(), List.of())
                            : selector);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Could not serialize repository group selector", exception);
        }
    }

    private RepositoryGroupSelector readSelector(String json) {
        try {
            return objectMapper.readValue(json, RepositoryGroupSelector.class);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not deserialize repository group selector", exception);
        }
    }
}
