package info.isaksson.erland.repofleet.github.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubRepositoryOwnerResponse(
    long id,
    String login
) {
}
