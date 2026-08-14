package info.isaksson.erland.repofleet.github.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubRepositoryResponse(
    long id,
    String name,
    @JsonProperty("full_name") String fullName,
    @JsonProperty("html_url") String htmlUrl,
    GitHubRepositoryOwnerResponse owner,
    String visibility,
    @JsonProperty("private") boolean privateRepository,
    boolean archived,
    boolean fork,
    @JsonProperty("default_branch") String defaultBranch,
    @JsonProperty("pushed_at") Instant pushedAt,
    @JsonProperty("updated_at") Instant updatedAt
) {
}
