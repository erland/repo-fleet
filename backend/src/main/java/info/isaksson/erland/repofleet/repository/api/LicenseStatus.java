package info.isaksson.erland.repofleet.repository.api;

public record LicenseStatus(
    AnalysisState analysisState,
    LicensePresence presence,
    Boolean recognized,
    String key,
    String name
) {
}
