package info.isaksson.erland.repofleet.auth;

import jakarta.inject.Inject;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {
    public static final String SESSION_COOKIE = "repofleet_session";

    private final AuthConfig config;
    private final GitHubUserAuthService authService;
    private final AuthSessionTokenService tokenService;

    @Inject
    public AuthResource(AuthConfig config, GitHubUserAuthService authService, AuthSessionTokenService tokenService) {
        this.config = config;
        this.authService = authService;
        this.tokenService = tokenService;
    }

    @GET
    @Path("/session")
    public AuthSessionResponse session(@CookieParam(SESSION_COOKIE) String token) {
        if (!config.enabled()) return new AuthSessionResponse(false, false, null);
        return tokenService.parse(token)
            .map(user -> new AuthSessionResponse(true, true, user))
            .orElseGet(() -> new AuthSessionResponse(true, false, null));
    }

    @GET
    @Path("/login")
    public Response login() {
        if (!config.enabled()) return Response.seeOther(URI.create("/")).build();
        try {
            return Response.seeOther(URI.create(authService.beginLogin())).build();
        } catch (AuthException ex) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity(new AuthErrorResponse(ex.getMessage()))
                .build();
        }
    }

    @GET
    @Path("/github/callback")
    public Response callback(
        @QueryParam("code") String code,
        @QueryParam("state") String state,
        @QueryParam("error") String error
    ) {
        if (!config.enabled()) return Response.seeOther(URI.create("/")).build();
        if (error != null && !error.isBlank()) {
            return Response.seeOther(URI.create("/?auth_error=github_denied")).build();
        }
        try {
            AuthenticatedUser user = authService.completeLogin(code, state);
            String session = tokenService.create(user);
            return Response.seeOther(URI.create("/"))
                .header("Set-Cookie", sessionCookie(session, config.sessionHours() * 3600))
                .build();
        } catch (AuthException ex) {
            String reason = ex.getMessage() != null && ex.getMessage().contains("not allowed")
                ? "not_allowed"
                : "login_failed";
            return Response.seeOther(URI.create("/?auth_error=" + reason)).build();
        }
    }

    @POST
    @Path("/logout")
    public Response logout() {
        return Response.noContent()
            .header("Set-Cookie", sessionCookie("", 0))
            .build();
    }

    private String sessionCookie(String value, long maxAgeSeconds) {
        StringBuilder cookie = new StringBuilder()
            .append(SESSION_COOKIE).append('=').append(value)
            .append("; Path=/; HttpOnly; SameSite=Lax; Max-Age=").append(maxAgeSeconds);
        if (config.cookieSecure()) cookie.append("; Secure");
        return cookie.toString();
    }

    public record AuthErrorResponse(String error) {
    }
}
