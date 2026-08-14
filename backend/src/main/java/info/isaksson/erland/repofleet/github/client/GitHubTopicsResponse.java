package info.isaksson.erland.repofleet.github.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubTopicsResponse(List<String> names) {
    public GitHubTopicsResponse {
        names = names == null ? List.of() : List.copyOf(names);
    }
}
