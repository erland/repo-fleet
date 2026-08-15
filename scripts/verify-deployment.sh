#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
python3 "$ROOT/scripts/validate-deployment-step.py"
python3 "$ROOT/scripts/validate-release-candidate.py"
docker compose --env-file "$ROOT/.env.example" \
  --env-file <(printf '%s\n' \
    'REPOFLEET_FRONTEND_IMAGE=ghcr.io/example/repo-fleet-frontend:1.2.3' \
    'REPOFLEET_BACKEND_IMAGE=ghcr.io/example/repo-fleet-backend:1.2.3') \
  -f "$ROOT/deploy/docker-compose.server.yml" config --quiet || true
