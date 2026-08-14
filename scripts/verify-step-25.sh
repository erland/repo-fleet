#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

python3 scripts/validate-release-step-25.py

rm -rf release-dist
RELEASE_VERSION=1.2.3 \
RELEASE_TAG=v1.2.3 \
RELEASE_SHA=0123456789abcdef0123456789abcdef01234567 \
REPOSITORY_OWNER=erland \
python3 scripts/package-release.py --output-dir release-dist

test -f release-dist/repo-fleet-v1.2.3-deployment.zip
unzip -l release-dist/repo-fleet-v1.2.3-deployment.zip | grep -q 'repo-fleet/docker-compose.yml'
unzip -l release-dist/repo-fleet-v1.2.3-deployment.zip | grep -q 'repo-fleet/.env.example'
unzip -l release-dist/repo-fleet-v1.2.3-deployment.zip | grep -q 'repo-fleet/DEPLOYMENT.md'
unzip -l release-dist/repo-fleet-v1.2.3-deployment.zip | grep -q 'repo-fleet/RELEASE-MANIFEST.txt'

if unzip -p release-dist/repo-fleet-v1.2.3-deployment.zip repo-fleet/docker-compose.yml | grep -q 'build:'; then
  echo "Release Compose file must not contain build sections." >&2
  exit 1
fi

unzip -p release-dist/repo-fleet-v1.2.3-deployment.zip repo-fleet/docker-compose.yml \
  | grep -q 'ghcr.io/erland/repo-fleet-frontend:1.2.3'
unzip -p release-dist/repo-fleet-v1.2.3-deployment.zip repo-fleet/docker-compose.yml \
  | grep -q 'ghcr.io/erland/repo-fleet-backend:1.2.3'

rm -rf release-dist
echo "Step 25 release packaging verification passed."
