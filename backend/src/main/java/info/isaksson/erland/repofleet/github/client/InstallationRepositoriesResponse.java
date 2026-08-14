package info.isaksson.erland.repofleet.github.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InstallationRepositoriesResponse(
    @JsonProperty("total_count") int totalCount,
    List<GitHubRepositoryResponse> repositories
) {
    public InstallationRepositoriesResponse {
        repositories = repositories == null ? List.of() : List.copyOf(repositories);
    }
}
