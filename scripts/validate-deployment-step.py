from pathlib import Path

root = Path(__file__).resolve().parents[1]
workflow = (root / ".github/workflows/deploy.yml").read_text(encoding="utf-8")
compose = (root / "deploy/docker-compose.server.yml").read_text(encoding="utf-8")
deploy = (root / "deploy/deploy.sh").read_text(encoding="utf-8")
manual = (root / "docs/debian-13-installation.md").read_text(encoding="utf-8")

checks = {
    "manual workflow only": "workflow_dispatch:" in workflow and "push:" not in workflow,
    "production environment": "name: production" in workflow,
    "release validation": "gh release view" in workflow,
    "least privilege packages read": "packages: read" in workflow,
    "known hosts required": "DEPLOY_KNOWN_HOSTS" in workflow,
    "no disabled host key check": "StrictHostKeyChecking=no" not in workflow,
    "exact release image tags": "repo-fleet-frontend:${VERSION}" in deploy and "repo-fleet-backend:${VERSION}" in deploy,
    "rollback path": ".images.env.previous" in deploy,
    "compose frontend configurable loopback only": '127.0.0.1:${REPOFLEET_FRONTEND_PORT:-8082}:8080' in compose,
    "default frontend host port documented": "REPOFLEET_FRONTEND_PORT=8082" in (root / "deploy/.env.server.example").read_text(encoding="utf-8"),
    "nginx uses default frontend host port": "proxy_pass http://127.0.0.1:8082;" in (root / "deploy/nginx/repo-fleet.conf").read_text(encoding="utf-8"),
    "backend not published": "ports:" not in compose.split("frontend:")[0],
    "github pem mounted": "./secrets/github-app.pem:/run/secrets/github-app.pem:ro" in compose,
    "https hostname documented": "repo-fleet.isaksson.info" in manual,
    "basic auth documented": "Basic Auth" in manual,
    "certbot documented": "certbot" in manual.lower(),
}

failed = [name for name, ok in checks.items() if not ok]
if failed:
    raise SystemExit("Deployment validation failed: " + ", ".join(failed))
print("RepoFleet Debian deployment validation passed.")
