# Release Publishing

Official RepoFleet releases use Git tags matching:

```text
vMAJOR.MINOR.PATCH
```

Example:

```bash
git tag v1.0.0
git push origin v1.0.0
```

The tag is the release version source of truth. No matching version must be manually copied into the frontend package or backend Maven artifact version.

## GitHub Actions release flow

`.github/workflows/release.yml` performs three stages.

### 1. Validate tagged source

The workflow validates the semver tag and reruns the source quality checks:

- repository policy,
- Docker/Compose static validation,
- frontend dependency install, typecheck, tests and production bundle,
- backend Maven `verify`.

No live GitHub App credentials are needed.

### 2. Publish GHCR images

The frontend and backend Dockerfiles are rebuilt from the tagged source and pushed to GitHub Container Registry.

For `v1.2.3`, the images are:

```text
ghcr.io/<owner>/repo-fleet-frontend:1.2.3
ghcr.io/<owner>/repo-fleet-backend:1.2.3
```

Traceability aliases are also published:

```text
ghcr.io/<owner>/repo-fleet-frontend:sha-<12-char-commit>
ghcr.io/<owner>/repo-fleet-backend:sha-<12-char-commit>
```

`latest` is updated for each official `vMAJOR.MINOR.PATCH` release as a convenience alias. Deployments that need reproducibility should use the immutable version tag.

Each image receives OCI labels linking version, source commit, repository and Git ref.

### 3. Package the deployment release

`scripts/package-release.py` creates:

```text
repo-fleet-v1.2.3-deployment.zip
```

The archive contains:

- `repo-fleet/docker-compose.yml`,
- `repo-fleet/.env.example`,
- `repo-fleet/DEPLOYMENT.md`,
- `repo-fleet/RELEASE-MANIFEST.txt`.

The release-specific Compose file references the published versioned GHCR images and omits `build:` sections, so a deployment host only needs Docker/Compose.

The manifest links the source tag, exact commit and image tags.

The workflow creates the GitHub Release if necessary, or uploads/replaces the deployment ZIP if the release already exists.

## Permissions and secrets

The workflow uses job-scoped least privilege:

- validation: read-only repository access,
- image publishing: `contents: read`, `packages: write`,
- GitHub Release packaging: `contents: write`.

The built-in `GITHUB_TOKEN` authenticates GHCR and GitHub Release operations.

No GitHub App private key or runtime credential is passed to Docker builds or packaged into release artifacts.

## Local packaging check

A release archive can be tested without publishing by supplying synthetic release metadata:

```bash
RELEASE_VERSION=1.2.3 \
RELEASE_TAG=v1.2.3 \
RELEASE_SHA=0123456789abcdef0123456789abcdef01234567 \
REPOSITORY_OWNER=erland \
python3 scripts/package-release.py --output-dir release-dist
```

Then inspect `release-dist/repo-fleet-v1.2.3-deployment.zip`.
