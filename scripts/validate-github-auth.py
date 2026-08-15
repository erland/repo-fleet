from pathlib import Path

root = Path(__file__).resolve().parents[1]
backend = root / "backend/src/main/java/info/isaksson/erland/repofleet/auth"
app = (root / "frontend/src/App.tsx").read_text(encoding="utf-8")
api = (root / "frontend/src/api.ts").read_text(encoding="utf-8")
compose = (root / "deploy/docker-compose.server.yml").read_text(encoding="utf-8")
env = (root / "deploy/.env.server.example").read_text(encoding="utf-8")
nginx = (root / "deploy/nginx/repo-fleet.conf").read_text(encoding="utf-8")
manual = (root / "docs/debian-13-installation.md").read_text(encoding="utf-8")

required_backend = [
    "AuthConfig.java",
    "AuthResource.java",
    "AuthRequestFilter.java",
    "AuthSessionTokenService.java",
    "GitHubUserAuthService.java",
]

checks = {
    "backend auth files": all((backend / name).is_file() for name in required_backend),
    "OAuth state": "state" in (backend / "GitHubUserAuthService.java").read_text(encoding="utf-8"),
    "PKCE S256": "code_challenge_method=S256" in (backend / "GitHubUserAuthService.java").read_text(encoding="utf-8") and "code_verifier" in (backend / "GitHubUserAuthService.java").read_text(encoding="utf-8"),
    "GitHub token not stored as session": "repofleet_session" in (backend / "AuthResource.java").read_text(encoding="utf-8") and "accessToken" not in (backend / "AuthSessionTokenService.java").read_text(encoding="utf-8"),
    "signed session": "HmacSHA256" in (backend / "AuthSessionTokenService.java").read_text(encoding="utf-8"),
    "secure session cookie": "HttpOnly" in (backend / "AuthResource.java").read_text(encoding="utf-8") and "SameSite=Lax" in (backend / "AuthResource.java").read_text(encoding="utf-8"),
    "allowlist": "REPOFLEET_AUTH_ALLOWED_USERS" in (backend / "GitHubUserAuthService.java").read_text(encoding="utf-8"),
    "API protected": "ContainerRequestFilter" in (backend / "AuthRequestFilter.java").read_text(encoding="utf-8"),
    "frontend login": "Sign in with GitHub" in app and "/api/auth/login" in app,
    "frontend logout": "Sign out" in app and "logout()" in app,
    "frontend session API": "/api/auth/session" in api,
    "production auth enabled": "REPOFLEET_AUTH_ENABLED: ${REPOFLEET_AUTH_ENABLED:-true}" in compose,
    "production client secret": "REPOFLEET_AUTH_CLIENT_SECRET" in compose,
    "production session secret": "REPOFLEET_AUTH_SESSION_SECRET" in compose,
    "production allowlist": "REPOFLEET_AUTH_ALLOWED_USERS" in compose,
    "server env template": all(name in env for name in ["REPOFLEET_AUTH_CLIENT_ID", "REPOFLEET_AUTH_CLIENT_SECRET", "REPOFLEET_AUTH_SESSION_SECRET", "REPOFLEET_AUTH_ALLOWED_USERS"]),
    "nginx no basic auth": "auth_basic" not in nginx and ".htpasswd" not in nginx,
    "certbot nginx manual": "sudo certbot --nginx -d repo-fleet.isaksson.info" in manual,
    "configurable production port": "REPOFLEET_FRONTEND_PORT=8082" in env and "${REPOFLEET_FRONTEND_PORT:-8082}" in compose,
}

failed = [name for name, ok in checks.items() if not ok]
if failed:
    raise SystemExit("GitHub authentication validation failed: " + ", ".join(failed))
print("RepoFleet GitHub user authentication validation passed.")
