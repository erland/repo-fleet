package info.isaksson.erland.repofleet.repository.api;

import java.time.Instant;

public record ReleaseStatus(
    AnalysisState analysisState,
    Boolean releasePresent,
    String latestReleaseName,
    String latestReleaseTag,
    Instant latestReleaseDate,
    Boolean latestReleasePrerelease
) {
}
