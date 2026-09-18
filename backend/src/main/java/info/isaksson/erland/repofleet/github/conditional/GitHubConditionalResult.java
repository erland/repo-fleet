package info.isaksson.erland.repofleet.github.conditional;

public record GitHubConditionalResult<T>(
        Status status,
        T value,
        String etag) {

    public enum Status {
        MODIFIED,
        NOT_MODIFIED
    }

    public static <T> GitHubConditionalResult<T> modified(T value, String etag) {
        return new GitHubConditionalResult<>(Status.MODIFIED, value, etag);
    }

    public static <T> GitHubConditionalResult<T> notModified(T cachedValue, String etag) {
        return new GitHubConditionalResult<>(Status.NOT_MODIFIED, cachedValue, etag);
    }

    public boolean notModified() {
        return status == Status.NOT_MODIFIED;
    }
}
