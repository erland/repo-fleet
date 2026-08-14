#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

printf '\n==> Frontend tests\n'
cd "$ROOT_DIR/frontend"
npm test

printf '\n==> Frontend production build\n'
npm run build

printf '\nStep 3 verification passed.\n'
