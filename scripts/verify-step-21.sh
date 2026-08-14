#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

cd "$ROOT/frontend"
npm test
npm run build

cd "$ROOT/backend"
mvn --batch-mode --no-transfer-progress verify

echo "Step 21 Phase 1 end-to-end validation passed."
