package info.isaksson.erland.repofleet.github.conditional;

public record GitHubConditionalResult<T>(
        Status status,
        T value,
        String etag) {

    public enum Status {
        MODIFIED,
        NOT_MODIFIED,
        CACHED_FRESH
    }

    public static <T> GitHubConditionalResult<T> modified(T value, String etag) {
        return new GitHubConditionalResult<>(Status.MODIFIED, value, etag);
    }

    public static <T> GitHubConditionalResult<T> notModified(T cachedValue, String etag) {
        return new GitHubConditionalResult<>(Status.NOT_MODIFIED, cachedValue, etag);
    }

    public static <T> GitHubConditionalResult<T> cachedFresh(T cachedValue, String etag) {
        return new GitHubConditionalResult<>(Status.CACHED_FRESH, cachedValue, etag);
    }

    public boolean notModified() {
        return status == Status.NOT_MODIFIED;
    }

    public boolean reusedCached() {
        return status == Status.NOT_MODIFIED || status == Status.CACHED_FRESH;
    }
}
