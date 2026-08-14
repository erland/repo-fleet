package info.isaksson.erland.repofleet.github.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import info.isaksson.erland.repofleet.github.client.GitHubAppClient;
import info.isaksson.erland.repofleet.github.client.GitHubAppResponse;
import info.isaksson.erland.repofleet.github.client.InstallationRepositoriesResponse;
import info.isaksson.erland.repofleet.github.client.InstallationTokenResponse;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static info.isaksson.erland.repofleet.github.auth.GitHubAppJwtServiceTest.config;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GitHubInstallationTokenServiceTest {

    @Test
    void reusesTokenUntilRefreshMarginThenReacquires() {
        var config = config("123", 456L, "unused", null, 300);
        var clock = new MutableClock(Instant.parse("2026-08-14T06:00:00Z"));
        var client = new FakeClient(clock);
        var jwtService = fixedJwtService(config, clock);
        var service = new GitHubInstallationTokenService(config, jwtService, client, clock);

        var first = service.getToken();
        var reused = service.getToken();
        assertEquals(first.value(), reused.value());
        assertEquals(1, client.createCalls);

        clock.instant = Instant.parse("2026-08-14T06:56:00Z");
        var refreshed = service.getToken();
        assertNotEquals(first.value(), refreshed.value());
        assertEquals(2, client.createCalls);
    }

    @Test
    void requiresInstallationId() {
        var config = config("123", null, "unused", null, 300);
        var clock = new MutableClock(Instant.parse("2026-08-14T06:00:00Z"));
        var service = new GitHubInstallationTokenService(config, fixedJwtService(config, clock), new FakeClient(clock), clock);

        assertThrows(GitHubAppNotConfiguredException.class, service::getToken);
    }

    @Test
    void propagatesGitHubFailureWithoutCachingToken() {
        var config = config("123", 456L, "unused", null, 300);
        var clock = new MutableClock(Instant.parse("2026-08-14T06:00:00Z"));
        GitHubAppClient failing = new GitHubAppClient() {
            public GitHubAppResponse getAuthenticatedApp(String a, String accept, String version) { throw new UnsupportedOperationException(); }

            public InstallationRepositoriesResponse listInstallationRepositories(
                    String a, String accept, String version, int perPage, int page) {
                throw new UnsupportedOperationException();
            }
            public InstallationTokenResponse createInstallationToken(long id, String a, String accept, String version) {
                throw new IllegalStateException("simulated GitHub failure");
            }
        };
        var service = new GitHubInstallationTokenService(config, fixedJwtService(config, clock), failing, clock);

        assertThrows(IllegalStateException.class, service::getToken);
        assertThrows(IllegalStateException.class, service::getToken);
    }

    private GitHubAppJwtService fixedJwtService(info.isaksson.erland.repofleet.github.config.GitHubAppConfig config, Clock clock) {
        return new GitHubAppJwtService(config, new GitHubPrivateKeyLoader(), new ObjectMapper(), clock) {
            @Override public String createJwt() { return "test-jwt"; }
        };
    }

    private static class FakeClient implements GitHubAppClient {
        private final Clock clock;
        int createCalls;
        FakeClient(Clock clock) { this.clock = clock; }
        public GitHubAppResponse getAuthenticatedApp(String a, String accept, String version) {
            return new GitHubAppResponse(1L, "repo-fleet", "RepoFleet");
        }
        public InstallationRepositoriesResponse listInstallationRepositories(
                String a, String accept, String version, int perPage, int page) {
            throw new UnsupportedOperationException();
        }
        public InstallationTokenResponse createInstallationToken(long id, String a, String accept, String version) {
            createCalls++;
            return new InstallationTokenResponse("token-" + createCalls, clock.instant().plusSeconds(3600));
        }
    }

    private static class MutableClock extends Clock {
        Instant instant;
        MutableClock(Instant instant) { this.instant = instant; }
        public ZoneId getZone() { return ZoneId.of("UTC"); }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return instant; }
    }
}
