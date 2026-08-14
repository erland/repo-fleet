#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "== Frontend tests and build =="
cd "$ROOT_DIR/frontend"
npm install
npm test
npm run build

echo "== Backend tests =="
cd "$ROOT_DIR/backend"
mvn test

echo "Step 1 verification passed."
