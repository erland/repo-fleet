#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "== Backend tests/build =="
(cd "$ROOT_DIR/backend" && mvn verify)

echo "== Frontend tests/build =="
(cd "$ROOT_DIR/frontend" && npm test && npm run build)

echo "Step 5 verification passed."
