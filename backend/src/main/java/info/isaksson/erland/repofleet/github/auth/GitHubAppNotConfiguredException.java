package info.isaksson.erland.repofleet.github.auth;

public class GitHubAppNotConfiguredException extends RuntimeException {
    public GitHubAppNotConfiguredException(String message) {
        super(message);
    }
}
