package info.isaksson.erland.repofleet.repository.diagnostics;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/diagnostics/refresh")
@Produces(MediaType.APPLICATION_JSON)
public class RefreshDiagnosticsResource {

    private final RefreshDiagnosticsService diagnostics;

    @Inject
    public RefreshDiagnosticsResource(RefreshDiagnosticsService diagnostics) {
        this.diagnostics = diagnostics;
    }

    @GET
    public RefreshDiagnosticsSnapshot diagnostics() {
        return diagnostics.snapshot();
    }
}
