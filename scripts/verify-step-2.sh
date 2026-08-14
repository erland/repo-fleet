#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

printf 'Verifying RepoFleet Step 2...\n'
(
  cd "$ROOT_DIR/backend"
  mvn verify
)
printf 'Step 2 verification passed.\n'
