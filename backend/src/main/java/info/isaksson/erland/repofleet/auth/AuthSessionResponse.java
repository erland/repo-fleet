package info.isaksson.erland.repofleet.auth;

public record AuthSessionResponse(
    boolean authEnabled,
    boolean authenticated,
    AuthenticatedUser user
) {
}
