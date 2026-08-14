package info.isaksson.erland.repofleet.repository.api;

import java.util.List;

public record RepositorySummary(
    long id,
    String owner,
    String name,
    String fullName,
    String url,
    RepositoryVisibility visibility,
    boolean archived,
    boolean fork,
    String defaultBranch,
    List<String> topics,
    List<String> languages,
    String primaryLanguage,
    LicenseStatus license,
    GitHubActionsStatus githubActions,
    ReleaseStatus release,
    ActivityStatus activity,
    RepositoryRefreshStatus refreshStatus
) {
    public RepositorySummary {
        topics = topics == null ? List.of() : List.copyOf(topics);
        languages = languages == null ? List.of() : List.copyOf(languages);
    }
}
