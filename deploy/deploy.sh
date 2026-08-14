#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 2 ]]; then
  echo "Usage: $0 <version-without-v> <github-owner>" >&2
  exit 2
fi

VERSION="$1"
OWNER="${2,,}"

if [[ ! "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "Invalid release version: $VERSION" >&2
  exit 2
fi

cd "$(dirname "$0")"

if [[ ! -f .env ]]; then
  echo "Missing $(pwd)/.env" >&2
  exit 1
fi
if [[ ! -r secrets/github-app.pem ]]; then
  echo "Missing or unreadable $(pwd)/secrets/github-app.pem" >&2
  exit 1
fi

NEXT="$(mktemp .images.env.next.XXXXXX)"
trap 'rm -f "$NEXT"' EXIT
chmod 600 "$NEXT"
cat > "$NEXT" <<ENV
REPOFLEET_FRONTEND_IMAGE=ghcr.io/${OWNER}/repo-fleet-frontend:${VERSION}
REPOFLEET_BACKEND_IMAGE=ghcr.io/${OWNER}/repo-fleet-backend:${VERSION}
ENV

compose_next=(docker compose --env-file .env --env-file "$NEXT" -f docker-compose.server.yml)

echo "Pulling RepoFleet ${VERSION} images..."
"${compose_next[@]}" pull

if [[ -f .images.env ]]; then
  cp -f .images.env .images.env.previous
  chmod 600 .images.env.previous
fi
mv -f "$NEXT" .images.env
trap - EXIT
chmod 600 .images.env

compose=(docker compose --env-file .env --env-file .images.env -f docker-compose.server.yml)

echo "Starting RepoFleet ${VERSION}..."
if "${compose[@]}" up --detach --remove-orphans --wait --wait-timeout 180; then
  echo "RepoFleet ${VERSION} is healthy."
  "${compose[@]}" ps
  exit 0
fi

echo "Deployment failed health checks." >&2
if [[ -f .images.env.previous ]]; then
  echo "Attempting rollback to previous images..." >&2
  cp -f .images.env.previous .images.env
  rollback=(docker compose --env-file .env --env-file .images.env -f docker-compose.server.yml)
  "${rollback[@]}" pull || true
  "${rollback[@]}" up --detach --remove-orphans --wait --wait-timeout 180 || true
  "${rollback[@]}" ps || true
fi
exit 1
