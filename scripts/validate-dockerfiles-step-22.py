from pathlib import Path

root = Path(__file__).resolve().parents[1]

backend = (root / "backend" / "Dockerfile").read_text(encoding="utf-8")
frontend = (root / "frontend" / "Dockerfile").read_text(encoding="utf-8")
nginx = (root / "frontend" / "nginx" / "default.conf.template").read_text(encoding="utf-8")
workflow = (root / ".github" / "workflows" / "ci.yml").read_text(encoding="utf-8")

checks = {
    "backend multi-stage": backend.count("FROM ") >= 2,
    "backend non-root": "USER repofleet" in backend,
    "backend health": "HEALTHCHECK" in backend and "/api/status" in backend,
    "backend runtime jar": "quarkus-run.jar" in backend,
    "frontend multi-stage": frontend.count("FROM ") >= 2,
    "frontend non-root": "USER nginx" in frontend,
    "frontend writable nginx runtime config": "/etc/nginx/conf.d" in frontend and "chown -R nginx:nginx" in frontend,
    "frontend writable nginx pid path": "/run" in frontend and "chown -R nginx:nginx" in frontend,
    "frontend health": "HEALTHCHECK" in frontend and "/healthz" in frontend,
    "frontend runtime backend config": "BACKEND_URL" in frontend and "${BACKEND_URL}" in nginx,
    "spa fallback": "try_files $uri $uri/ /index.html;" in nginx,
    "api proxy": "location /api/" in nginx and "proxy_pass ${BACKEND_URL};" in nginx,
    "CI invokes smoke test via bash": "run: bash scripts/verify-step-22.sh" in workflow,
}

failed = [name for name, ok in checks.items() if not ok]
if failed:
    raise SystemExit("Step 22 Docker validation failed: " + ", ".join(failed))

print("Step 22 Dockerfile static validation passed.")
