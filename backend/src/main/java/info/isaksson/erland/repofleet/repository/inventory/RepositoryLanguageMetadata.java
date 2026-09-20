package info.isaksson.erland.repofleet.repository.inventory;

import java.util.List;

record RepositoryLanguageMetadata(List<String> languages, String primaryLanguage) {
    RepositoryLanguageMetadata {
        languages = languages == null ? List.of() : List.copyOf(languages);
    }
}
