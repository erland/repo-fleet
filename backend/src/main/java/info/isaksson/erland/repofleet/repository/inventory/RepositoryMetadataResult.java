package info.isaksson.erland.repofleet.repository.inventory;

record RepositoryMetadataResult<T>(
        T value,
        boolean complete,
        boolean degraded,
        boolean unavailable,
        String error) {

    static <T> RepositoryMetadataResult<T> success(T value) {
        return new RepositoryMetadataResult<>(value, true, false, false, null);
    }

    static <T> RepositoryMetadataResult<T> degraded(T value, boolean complete, String error) {
        return new RepositoryMetadataResult<>(value, complete, true, false, error);
    }

    static <T> RepositoryMetadataResult<T> unavailable(T value, String error) {
        return new RepositoryMetadataResult<>(value, false, true, true, error);
    }
}
