package info.isaksson.erland.repofleet.auth;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.quarkus.runtime.annotations.StaticInitSafe;

import java.util.Optional;

@StaticInitSafe
@ConfigMapping(prefix = "repofleet.auth")
public interface AuthConfig {
    @WithDefault("false")
    boolean enabled();

    Optional<String> clientId();
    Optional<String> clientSecret();
    Optional<String> sessionSecret();
    Optional<String> callbackUrl();

    @WithDefault("")
    String allowedUsers();

    @WithDefault("12")
    long sessionHours();

    @WithDefault("true")
    boolean cookieSecure();

    @WithDefault("https://github.com")
    String githubWebBaseUrl();

    @WithDefault("https://api.github.com")
    String githubApiBaseUrl();
}
