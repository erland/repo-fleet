package info.isaksson.erland.repofleet.standards;

import info.isaksson.erland.repofleet.repository.api.RepositoryVisibility;
import java.util.List;

public record RepositoryGroupSelector(
        String namePrefix,
        String owner,
        RepositoryVisibility visibility,
        Boolean archived,
        Boolean fork,
        List<String> topics,
        List<String> languages) {

    public RepositoryGroupSelector {
        topics = topics == null ? List.of() : List.copyOf(topics);
        languages = languages == null ? List.of() : List.copyOf(languages);
    }
}
