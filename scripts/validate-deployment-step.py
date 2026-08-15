from pathlib import Path

root = Path(__file__).resolve().parents[1]
workflow = (root / ".github/workflows/deploy.yml").read_text(encoding="utf-8")
compose = (root / "deploy/docker-compose.server.yml").read_text(encoding="utf-8")
deploy = (root / "deploy/deploy.sh").read_text(encoding="utf-8")
manual = (root / "docs/debian-13-installation.md").read_text(encoding="utf-8")

checks = {
    "manual workflow only": "workflow_dispatch:" in workflow and "push:" not in workflow,
    "production environment": "name: production" in workflow,
    "deploy accepts official and RC versions": "(-rc\\.[0-9]+)?" in workflow,
    "official deploy still requires GitHub Release": "gh release view" in workflow and "v${DEPLOY_VERSION}" in workflow,
    "RC deploy bypasses formal release requirement": "if [[ \"$DEPLOY_VERSION\" != *-rc.* ]]" in workflow,
    "deploy requires default branch": "Production deploys must be dispatched from the default branch" in workflow,
    "least privilege packages read": "packages: read" in workflow,
    "known hosts required": "DEPLOY_KNOWN_HOSTS" in workflow,
    "no disabled host key check": "StrictHostKeyChecking=no" not in workflow,
    "exact immutable image tags": "repo-fleet-frontend:${VERSION}" in deploy and "repo-fleet-backend:${VERSION}" in deploy,
    "deploy script accepts RC": "(-rc\\.[0-9]+)?" in deploy,
    "RC publisher exists": (root / ".github/workflows/release-candidate.yml").is_file(),
    "rollback path": ".images.env.previous" in deploy,
    "compose frontend configurable loopback only": '127.0.0.1:${REPOFLEET_FRONTEND_PORT:-8082}:8080' in compose,
    "default frontend host port documented": "REPOFLEET_FRONTEND_PORT=8082" in (root / "deploy/.env.server.example").read_text(encoding="utf-8"),
    "nginx uses default frontend host port": "proxy_pass http://127.0.0.1:8082;" in (root / "deploy/nginx/repo-fleet.conf").read_text(encoding="utf-8"),
    "backend not published": "ports:" not in compose.split("frontend:")[0],
    "github pem mounted": "./secrets/github-app.pem:/run/secrets/github-app.pem:ro" in compose,
    "https hostname documented": "repo-fleet.isaksson.info" in manual,
    "GitHub auth documented": "Sign in with GitHub" in manual and "REPOFLEET_AUTH_ALLOWED_USERS" in manual,
    "certbot nginx command documented": "certbot --nginx -d repo-fleet.isaksson.info" in manual,
    "no nginx basic auth": "auth_basic_user_file" not in (root / "deploy/nginx/repo-fleet.conf").read_text(encoding="utf-8"),
    "auth callback runtime": "REPOFLEET_AUTH_CALLBACK_URL" in compose,
    "auth client secret runtime": "REPOFLEET_AUTH_CLIENT_SECRET" in compose,
    "auth allowlist runtime": "REPOFLEET_AUTH_ALLOWED_USERS" in compose,
    "certbot documented": "certbot" in manual.lower(),
}

failed = [name for name, ok in checks.items() if not ok]
if failed:
    raise SystemExit("Deployment validation failed: " + ", ".join(failed))
print("RepoFleet Debian deployment validation passed.")
