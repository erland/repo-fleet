package info.isaksson.erland.repofleet.github.api;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;

@ApplicationScoped
public class GitHubRateLimitWaitState {

    private volatile Instant pausedUntil;
    private volatile String reason;

    public synchronized void pauseUntil(Instant until, String reason) {
        if (until == null) return;
        if (pausedUntil == null || until.isAfter(pausedUntil)) {
            pausedUntil = until;
            this.reason = reason;
        }
    }

    public synchronized void clearIfElapsed(Instant now) {
        if (pausedUntil != null && !pausedUntil.isAfter(now)) {
            pausedUntil = null;
            reason = null;
        }
    }

    public Instant pausedUntil() {
        return pausedUntil;
    }

    public String reason() {
        return reason;
    }

    public boolean paused(Instant now) {
        Instant until = pausedUntil;
        return until != null && until.isAfter(now);
    }
}
