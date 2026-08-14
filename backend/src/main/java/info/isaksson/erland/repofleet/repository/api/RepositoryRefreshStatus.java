package info.isaksson.erland.repofleet.repository.api;

public record RepositoryRefreshStatus(
    AnalysisState state,
    String message
) {
}
