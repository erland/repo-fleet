#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BACKEND_IMAGE="${BACKEND_IMAGE:-repofleet-backend:step22}"
FRONTEND_IMAGE="${FRONTEND_IMAGE:-repofleet-frontend:step22}"
NETWORK="${NETWORK:-repofleet-step22-test}"
BACKEND_CONTAINER="${BACKEND_CONTAINER:-repofleet-step22-backend}"
FRONTEND_CONTAINER="${FRONTEND_CONTAINER:-repofleet-step22-frontend}"

cleanup() {
  docker rm -f "$FRONTEND_CONTAINER" "$BACKEND_CONTAINER" >/dev/null 2>&1 || true
  docker network rm "$NETWORK" >/dev/null 2>&1 || true
}
trap cleanup EXIT

docker build -t "$BACKEND_IMAGE" "$ROOT/backend"
docker build -t "$FRONTEND_IMAGE" "$ROOT/frontend"

docker network create "$NETWORK" >/dev/null

docker run -d \
  --name "$BACKEND_CONTAINER" \
  --network "$NETWORK" \
  --network-alias backend \
  "$BACKEND_IMAGE" >/dev/null

docker run -d \
  --name "$FRONTEND_CONTAINER" \
  --network "$NETWORK" \
  -e BACKEND_URL="http://backend:8080" \
  -p 127.0.0.1::8080 \
  "$FRONTEND_IMAGE" >/dev/null

wait_healthy() {
  local container="$1"
  local attempts=30
  for _ in $(seq 1 "$attempts"); do
    local status
    status="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$container")"
    if [[ "$status" == "healthy" ]]; then
      return 0
    fi
    if [[ "$status" == "unhealthy" ]]; then
      docker logs "$container"
      return 1
    fi
    sleep 1
  done

  docker logs "$container"
  echo "Timed out waiting for $container health check." >&2
  return 1
}

wait_healthy "$BACKEND_CONTAINER"
wait_healthy "$FRONTEND_CONTAINER"

HOST_PORT="$(docker port "$FRONTEND_CONTAINER" 8080/tcp | awk -F: 'NR==1 {print $NF}')"

curl --fail --silent "http://127.0.0.1:${HOST_PORT}/" | grep -q '<div id="root">'
curl --fail --silent "http://127.0.0.1:${HOST_PORT}/api/status" | grep -q '"status":"UP"'

echo "Step 22 Docker image and container verification passed."
