#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

python3 "$ROOT/scripts/validate-quality-gate.py"
python3 "$ROOT/scripts/validate-dockerfiles-step-22.py"
python3 "$ROOT/scripts/validate-compose-step-23.py"

cd "$ROOT/frontend"
npm install --no-audit --no-fund
npm run typecheck
npm test
npm run build:bundle

cd "$ROOT/backend"
mvn --batch-mode --no-transfer-progress verify

cd "$ROOT"
bash scripts/verify-step-23.sh

echo "Step 24 CI Quality Gate verification passed."
