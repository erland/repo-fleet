package info.isaksson.erland.repofleet.repository.api;

public record RepositoryRefreshStatus(
    AnalysisState state,
    String message,
    CacheFreshness freshness,
    RepositoryRefreshOutcome latestOutcome
) {
    public RepositoryRefreshStatus(AnalysisState state, String message, CacheFreshness freshness) {
        this(state, message, freshness, null);
    }

    public RepositoryRefreshStatus(AnalysisState state, String message) {
        this(state, message, null, null);
    }
}
