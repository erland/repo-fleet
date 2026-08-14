# CI Quality Gate

Step 24 turns the earlier incremental CI bootstrap into the Phase 1 quality gate.

## Events and duplicate avoidance

CI runs for:

- pull requests,
- pushes to `main`.

Feature-branch pushes are not independently built by this workflow when a pull request is the intended validation path. The merge/push to `main` is validated once after integration.

Concurrency cancellation removes superseded runs for the same pull request or ref.

## Jobs

### Repository policy

Fast, dependency-free checks run first:

- RepoFleet Java package namespace,
- `.env.example` contains no populated GitHub credentials/private key material,
- required generated/secret files remain ignored,
- Phase 1 Compose remains database-free,
- all 13 Phase 1 acceptance criteria remain mapped,
- basic trailing-whitespace source hygiene,
- Dockerfile static validation,
- Compose static validation.

### Frontend

The frontend job performs distinct stages:

1. dependency installation,
2. TypeScript type checking,
3. Vitest tests (including the deterministic Phase 1 acceptance suite),
4. production Vite bundling.

Type checking and bundling are separated in CI so the production bundle step does not repeat `tsc`.

### Backend

Maven `verify` compiles the Quarkus application and runs the backend unit/component/acceptance tests.

No live GitHub integration is required.

### Production containers

This job starts only after source/policy jobs succeed.

It uses the Step 23 Docker Compose smoke suite, which builds both production images once and validates:

- backend container startup/health,
- frontend container startup/health,
- complete Compose startup ordering,
- frontend HTML,
- frontend-to-backend `/api/status` proxying,
- direct backend status.

The previous Step 22 standalone Docker smoke is retained for targeted local troubleshooting, but CI no longer executes both Step 22 and Step 23 builds. This avoids building the same production images twice.

### Quality Gate

The final `Quality Gate` job always evaluates the results of all four preceding jobs and fails unless every required job succeeded.

This gives branch protection a single stable check name to require:

```text
Quality Gate
```

## Local verification

Source checks:

```bash
python3 scripts/validate-quality-gate.py

cd frontend
npm install
npm run typecheck
npm test
npm run build:bundle

cd ../backend
mvn --batch-mode --no-transfer-progress verify
```

Production packaging:

```bash
cd ..
bash scripts/verify-step-23.sh
```

Live GitHub credentials are not necessary for the CI quality gate; deterministic fixtures cover Phase 1 integration behavior.
