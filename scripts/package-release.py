from __future__ import annotations

import argparse
import os
from pathlib import Path
import shutil
import zipfile

def require(name: str) -> str:
    value = os.environ.get(name, "").strip()
    if not value:
        raise SystemExit(f"Missing required environment variable: {name}")
    return value

def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output-dir", default="release-dist")
    args = parser.parse_args()

    root = Path(__file__).resolve().parents[1]
    output_dir = root / args.output_dir
    if output_dir.exists():
        shutil.rmtree(output_dir)
    output_dir.mkdir(parents=True)

    version = require("RELEASE_VERSION")
    tag = require("RELEASE_TAG")
    sha = require("RELEASE_SHA")
    owner = require("REPOSITORY_OWNER")

    expected_tag = f"v{version}"
    if tag != expected_tag:
        raise SystemExit(f"Release tag/version mismatch: {tag} != {expected_tag}")

    deploy = output_dir / "repo-fleet"
    deploy.mkdir()

    compose_source = (root / "docker-compose.yml").read_text(encoding="utf-8")
    compose_release = compose_source.replace(
        "${REPOFLEET_BACKEND_IMAGE:-repo-fleet-backend:local}",
        f"${{REPOFLEET_BACKEND_IMAGE:-ghcr.io/{owner}/repo-fleet-backend:{version}}}",
    ).replace(
        "${REPOFLEET_FRONTEND_IMAGE:-repo-fleet-frontend:local}",
        f"${{REPOFLEET_FRONTEND_IMAGE:-ghcr.io/{owner}/repo-fleet-frontend:{version}}}",
    )

    # Deployment archives consume immutable published images and should not build source.
    lines = compose_release.splitlines()
    filtered: list[str] = []
    skipping_build = False
    build_indent = None
    for line in lines:
        if line.startswith("    build:"):
            skipping_build = True
            build_indent = len(line) - len(line.lstrip())
            continue
        if skipping_build:
            indent = len(line) - len(line.lstrip())
            if line.strip() and indent <= (build_indent or 0):
                skipping_build = False
            else:
                continue
        filtered.append(line)
    (deploy / "docker-compose.yml").write_text("\n".join(filtered) + "\n", encoding="utf-8")

    shutil.copy2(root / ".env.example", deploy / ".env.example")
    shutil.copy2(root / "docs" / "docker-compose-runtime.md", deploy / "DEPLOYMENT.md")

    manifest = f"""RepoFleet release manifest

Release tag: {tag}
Version: {version}
Source commit: {sha}

Frontend image:
  ghcr.io/{owner}/repo-fleet-frontend:{version}
Backend image:
  ghcr.io/{owner}/repo-fleet-backend:{version}

Immutable source trace tags:
  ghcr.io/{owner}/repo-fleet-frontend:sha-{sha[:12]}
  ghcr.io/{owner}/repo-fleet-backend:sha-{sha[:12]}
"""
    (deploy / "RELEASE-MANIFEST.txt").write_text(manifest, encoding="utf-8")

    notes = f"""# RepoFleet {tag}

This release was built from Git tag `{tag}` and source commit `{sha}`.

Published container images:

- `ghcr.io/{owner}/repo-fleet-frontend:{version}`
- `ghcr.io/{owner}/repo-fleet-backend:{version}`

The attached deployment ZIP contains Docker Compose, `.env.example`, deployment documentation and a traceability manifest. No GitHub App credentials or private keys are included.
"""
    (output_dir / "RELEASE-NOTES.md").write_text(notes, encoding="utf-8")

    archive = output_dir / f"repo-fleet-{tag}-deployment.zip"
    with zipfile.ZipFile(archive, "w", zipfile.ZIP_DEFLATED) as zf:
        for path in sorted(deploy.rglob("*")):
            if path.is_file():
                zf.write(path, path.relative_to(output_dir))

    print(f"Created {archive}")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
