package info.isaksson.erland.repofleet.repository.refresh;

public enum RepositoryRefreshJobState {
    PENDING,
    RUNNING,
    RETRY,
    SUCCEEDED,
    FAILED
}
