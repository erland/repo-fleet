package info.isaksson.erland.repofleet.github.api;

public class GitHubApiException extends RuntimeException {

    private final GitHubApiFailureKind kind;
    private final Integer httpStatus;

    public GitHubApiException(GitHubApiFailureKind kind, Integer httpStatus, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
        this.httpStatus = httpStatus;
    }

    public GitHubApiFailureKind kind() {
        return kind;
    }

    public Integer httpStatus() {
        return httpStatus;
    }
}
