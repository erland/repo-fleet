package info.isaksson.erland.repofleet.github.api;

public enum GitHubApiFailureKind {
    NOT_FOUND,
    AUTHENTICATION,
    AUTHORIZATION,
    RATE_LIMIT,
    TRANSIENT,
    UNKNOWN
}
