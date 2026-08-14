#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "==> Verifying frontend remains healthy"
cd "$ROOT_DIR/frontend"
npm test
npm run build

echo "==> Verifying backend and GitHub App authentication tests"
cd "$ROOT_DIR/backend"
mvn verify

echo "Step 4 verification passed."
