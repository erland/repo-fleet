package info.isaksson.erland.repofleet.repository.api;

public record RepositoryRefreshStatus(
    AnalysisState state,
    String message,
    CacheFreshness freshness
) {
    public RepositoryRefreshStatus(AnalysisState state, String message) {
        this(state, message, null);
    }
}
