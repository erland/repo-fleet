from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
compose = (root / "docker-compose.yml").read_text(encoding="utf-8")
env = (root / ".env.example").read_text(encoding="utf-8")
workflow = (root / ".github" / "workflows" / "ci.yml").read_text(encoding="utf-8")

checks = {
    "frontend service": re.search(r"(?m)^  frontend:$", compose) is not None,
    "backend service": re.search(r"(?m)^  backend:$", compose) is not None,
    "no postgres service": re.search(r"(?im)^  (postgres|database|db):$", compose) is None,
    "backend health dependency": "condition: service_healthy" in compose,
    "internal backend URL": "BACKEND_URL: http://backend:8080" in compose,
    "frontend host port configurable": "${REPOFLEET_FRONTEND_PORT:-8080}:8080" in compose,
    "backend host port configurable": "${REPOFLEET_BACKEND_PORT:-8081}:8080" in compose,
    "github app id runtime configuration": "REPOFLEET_GITHUB_APP_ID:" in compose,
    "github installation runtime configuration": "REPOFLEET_GITHUB_INSTALLATION_ID:" in compose,
    "private key not baked": "BEGIN PRIVATE KEY" not in compose,
    "env frontend port": "REPOFLEET_FRONTEND_PORT=8080" in env,
    "env backend port": "REPOFLEET_BACKEND_PORT=8081" in env,
    "CI compose verification": "bash scripts/verify-step-23.sh" in workflow,
}

failed = [name for name, ok in checks.items() if not ok]
if failed:
    raise SystemExit("Step 23 Compose validation failed: " + ", ".join(failed))

print("Step 23 Docker Compose static validation passed.")
