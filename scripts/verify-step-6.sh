#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

cd "$ROOT/backend"
mvn --batch-mode --no-transfer-progress verify

echo "Step 6 backend verification passed."
