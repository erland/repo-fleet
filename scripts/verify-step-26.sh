#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

python3 "$ROOT/scripts/validate-quality-gate.py"
python3 "$ROOT/scripts/validate-dockerfiles-step-22.py"
python3 "$ROOT/scripts/validate-compose-step-23.py"
python3 "$ROOT/scripts/validate-release-step-25.py"
python3 "$ROOT/scripts/validate-phase1-completion.py"

echo "Step 26 Phase 1 completion review validation passed."
