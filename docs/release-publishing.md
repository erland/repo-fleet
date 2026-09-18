# Release Publishing

RepoFleet has two publish paths:

- **Release candidates** such as `1.1.0-rc.3` are published manually from the default branch through GitHub Actions without creating a GitHub Release.
- **Official releases** such as `1.1.0` are created manually in the GitHub Releases UI. Publishing the release triggers GitHub Actions, which validates the released source, builds/pushes the frontend and backend images, and attaches the deployment package to the release.

Both paths publish immutable versioned frontend/backend images to GHCR. Production deployment always selects an exact version; the moving `rc` and `latest` aliases are convenience pointers only and are never used as deployment identifiers.

## Release candidates

Run **Actions → Publish release candidate → Run workflow** from the default branch and enter, for example:

```text
1.1.0-rc.3
```

`.github/workflows/release-candidate.yml` reruns repository/deployment validation, frontend typecheck/tests/build and backend Maven verification before publishing:

```text
ghcr.io/<owner>/repo-fleet-frontend:1.1.0-rc.3
ghcr.io/<owner>/repo-fleet-backend:1.1.0-rc.3
```

It also publishes the source trace tag `sha-<12-char-commit>` and updates the convenience alias `rc`. It does **not** create a GitHub Release.

## Official releases

Create official RepoFleet releases from the GitHub UI:

1. Open **Releases**.
2. Choose **Draft a new release**.
3. Choose an existing tag or create a new tag in the form:
   ```text
   vMAJOR.MINOR.PATCH
   ```
4. Make sure the tag targets the current/default branch commit that you intend to release.
5. Enter the release title and notes.
6. Click **Publish release**.

Example tag:

```text
v2.0.0
```

The **published GitHub Release** is the trigger for `.github/workflows/release.yml`. Creating a tag by itself does not publish the official RepoFleet release.

Draft releases do not trigger the workflow. The workflow runs when the release is published.

## GitHub Actions release flow

### 1. Validate released source

The workflow:

- checks out the tag associated with the published GitHub Release,
- validates that the tag is exactly `vMAJOR.MINOR.PATCH`,
- verifies that the released commit is reachable from the repository default branch,
- reruns repository policy and Docker/Compose static validation,
- runs frontend dependency install, typecheck, tests and production bundle,
- runs backend Maven `verify`.

A release tag that points to a commit outside the default branch fails before any image is published.

### 2. Publish GHCR images

For a release `v1.2.3`, the workflow builds and publishes:

```text
ghcr.io/<owner>/repo-fleet-frontend:1.2.3
ghcr.io/<owner>/repo-fleet-backend:1.2.3
```

It also publishes:

```text
ghcr.io/<owner>/repo-fleet-frontend:sha-<12-char-commit>
ghcr.io/<owner>/repo-fleet-backend:sha-<12-char-commit>
ghcr.io/<owner>/repo-fleet-frontend:latest
ghcr.io/<owner>/repo-fleet-backend:latest
```

The immutable version tag should be used for production deployments.

### 3. Attach deployment package

`scripts/package-release.py` creates:

```text
repo-fleet-v1.2.3-deployment.zip
```

The workflow uploads that archive to the **already published GitHub Release**. It does not create, rename or replace the release itself, so the release title and release notes remain under manual control.

## Deploying candidates and official versions

**Actions → Deploy production** accepts either:

```text
1.1.0-rc.3
1.1.0
```

For an RC, the deploy workflow uses the exact immutable GHCR tags. For an official version such as `1.1.0`, it verifies that the matching GitHub Release `v1.1.0` exists before deploying.

## Permissions and secrets

The workflow uses job-scoped least privilege:

- validation: read-only repository access,
- image publishing: `contents: read`, `packages: write`,
- deployment-package upload: `contents: write`.

The built-in `GITHUB_TOKEN` authenticates GHCR and the release-asset upload.

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
