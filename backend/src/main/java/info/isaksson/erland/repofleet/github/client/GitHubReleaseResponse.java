package info.isaksson.erland.repofleet.github.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubReleaseResponse(
        long id,
        String name,
        @JsonProperty("tag_name") String tagName,
        boolean draft,
        boolean prerelease,
        @JsonProperty("published_at") Instant publishedAt,
        @JsonProperty("created_at") Instant createdAt) {
}
