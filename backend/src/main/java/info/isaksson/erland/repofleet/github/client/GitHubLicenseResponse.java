package info.isaksson.erland.repofleet.github.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubLicenseResponse(
        String name,
        String path,
        GitHubLicenseInfoResponse license) {
}
