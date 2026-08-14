package info.isaksson.erland.repofleet.github.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubLicenseInfoResponse(
        String key,
        String name,
        @JsonProperty("spdx_id") String spdxId) {
}
