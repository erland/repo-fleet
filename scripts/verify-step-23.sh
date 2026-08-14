#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

PROJECT="${COMPOSE_PROJECT_NAME:-repo-fleet-step23-test}"
FRONTEND_PORT="${REPOFLEET_FRONTEND_PORT:-18080}"
BACKEND_PORT="${REPOFLEET_BACKEND_PORT:-18081}"

export COMPOSE_PROJECT_NAME="$PROJECT"
export REPOFLEET_FRONTEND_PORT="$FRONTEND_PORT"
export REPOFLEET_BACKEND_PORT="$BACKEND_PORT"

cleanup() {
  docker compose down --volumes --remove-orphans >/dev/null 2>&1 || true
}
trap cleanup EXIT

docker compose config --quiet
docker compose up --build --detach --wait --wait-timeout 180

curl --fail --silent "http://127.0.0.1:${FRONTEND_PORT}/" | grep -q '<div id="root">'
curl --fail --silent "http://127.0.0.1:${FRONTEND_PORT}/api/status" | grep -q '"status":"UP"'
curl --fail --silent "http://127.0.0.1:${BACKEND_PORT}/api/status" | grep -q '"status":"UP"'

FRONTEND_HEALTH="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "${PROJECT}-frontend-1")"
BACKEND_HEALTH="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "${PROJECT}-backend-1")"

test "$FRONTEND_HEALTH" = "healthy"
test "$BACKEND_HEALTH" = "healthy"

echo "Step 23 Docker Compose runtime verification passed."
