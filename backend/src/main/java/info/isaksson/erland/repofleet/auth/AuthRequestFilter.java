package info.isaksson.erland.repofleet.auth;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthRequestFilter implements ContainerRequestFilter {
    private final AuthConfig config;
    private final AuthSessionTokenService tokenService;

    @Inject
    public AuthRequestFilter(AuthConfig config, AuthSessionTokenService tokenService) {
        this.config = config;
        this.tokenService = tokenService;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (!config.enabled()) return;

        String path = requestContext.getUriInfo().getPath();
        if (path.equals("api/status") || path.startsWith("api/auth/")) return;

        var cookie = requestContext.getCookies().get(AuthResource.SESSION_COOKIE);
        if (cookie != null && tokenService.parse(cookie.getValue()).isPresent()) return;

        requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .entity(Map.of("error", "authentication_required"))
            .build());
    }
}
