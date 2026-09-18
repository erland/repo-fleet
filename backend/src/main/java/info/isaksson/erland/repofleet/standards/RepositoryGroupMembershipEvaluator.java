package info.isaksson.erland.repofleet.standards;

import info.isaksson.erland.repofleet.repository.api.RepositorySummary;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Locale;

@ApplicationScoped
public class RepositoryGroupMembershipEvaluator {

    public boolean matches(RepositoryGroupSelector selector, RepositorySummary repository) {
        if (selector == null) {
            return true;
        }
        if (selector.namePrefix() != null
                && !repository.name().toLowerCase(Locale.ROOT)
                        .startsWith(selector.namePrefix().toLowerCase(Locale.ROOT))) {
            return false;
        }
        if (selector.owner() != null
                && !selector.owner().equalsIgnoreCase(repository.owner())) {
            return false;
        }
        if (selector.visibility() != null
                && selector.visibility() != repository.visibility()) {
            return false;
        }
        if (selector.archived() != null
                && selector.archived() != repository.archived()) {
            return false;
        }
        if (selector.fork() != null
                && selector.fork() != repository.fork()) {
            return false;
        }
        if (!containsAllIgnoreCase(repository.topics(), selector.topics())) {
            return false;
        }
        return containsAllIgnoreCase(repository.languages(), selector.languages());
    }

    private boolean containsAllIgnoreCase(
            java.util.List<String> actual,
            java.util.List<String> required) {
        if (required == null || required.isEmpty()) {
            return true;
        }
        java.util.Set<String> normalized = actual.stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet());
        return required.stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .allMatch(normalized::contains);
    }
}
