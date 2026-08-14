package info.isaksson.erland.repofleet.github.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.Optional;

@ConfigMapping(prefix = "repofleet.github")
public interface GitHubAppConfig {
    Optional<String> appId();
    Optional<Long> installationId();
    Optional<String> privateKey();
    Optional<String> privateKeyPath();

    @WithDefault("300")
    long tokenRefreshMarginSeconds();
}
