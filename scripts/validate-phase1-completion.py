from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]

required_docs = [
    "README.md",
    "docs/functional-specification.md",
    "docs/development-plan-phase-1.md",
    "docs/implementation-status.md",
    "docs/local-development.md",
    "docs/github-app-setup.md",
    "docs/github-user-authentication.md",
    "docs/configuration.md",
    "docs/docker-images.md",
    "docs/docker-compose-runtime.md",
    "docs/ci-quality-gate.md",
    "docs/release-publishing.md",
    "docs/phase-1-acceptance-validation.md",
    "docs/phase-1-completion-review.md",
    "docs/debian-13-installation.md",
]

errors: list[str] = []
for name in required_docs:
    if not (root / name).is_file():
        errors.append(f"missing required documentation: {name}")

readme = (root / "README.md").read_text(encoding="utf-8")
for needle in (
    "docs/local-development.md",
    "docs/github-app-setup.md",
    "docs/github-user-authentication.md",
    "docs/configuration.md",
    "docs/docker-compose-runtime.md",
    "docs/release-publishing.md",
    "docs/phase-1-completion-review.md",
):
    if needle not in readme:
        errors.append(f"README documentation index/reference missing: {needle}")

setup = (root / "docs/github-app-setup.md").read_text(encoding="utf-8")
for permission in ("Metadata: Read-only", "Contents: Read-only", "Actions: Read-only"):
    if permission not in setup:
        errors.append(f"GitHub App setup missing permission: {permission}")
if "Do not grant write permissions for Phase 1" not in setup:
    errors.append("GitHub App setup does not preserve the Phase 1 read-only boundary")

configuration = (root / "docs/configuration.md").read_text(encoding="utf-8")
for variable in (
    "REPOFLEET_GITHUB_APP_ID",
    "REPOFLEET_GITHUB_INSTALLATION_ID",
    "REPOFLEET_GITHUB_PRIVATE_KEY_PATH",
    "REPOFLEET_GITHUB_PRIVATE_KEY",
    "REPOFLEET_GITHUB_TOKEN_REFRESH_MARGIN_SECONDS",
    "GITHUB_API_URL",
    "BACKEND_URL",
    "REPOFLEET_FRONTEND_PORT",
    "REPOFLEET_BACKEND_PORT",
):
    if variable not in configuration:
        errors.append(f"configuration reference missing variable: {variable}")

completion = (root / "docs/phase-1-completion-review.md").read_text(encoding="utf-8")
for heading in (
    "## Completed Phase 1 requirements",
    "## Final comparison with the functional specification",
    "## Known limitations",
    "## Candidate improvements for Phase 2",
    "## Technical debt before write functionality",
):
    if heading not in completion:
        errors.append(f"completion review missing section: {heading}")

for deviation in (
    "Multiple-topic",
    "License recognized/type filters",
    "Arbitrary activity cutoff date",
    "Grouped summary breakdowns",
    "Dedicated repository-detail backend endpoint",
):
    if deviation not in completion:
        errors.append(f"completion review missing documented deviation: {deviation}")

status = (root / "docs/implementation-status.md").read_text(encoding="utf-8")
for step in range(1, 26):
    match = re.search(rf"(?m)^\|\s*{step}\s*\|.*?\|\s*(DONE|IN PROGRESS|NOT STARTED|BLOCKED|DEFERRED)\s*\|", status)
    if not match or match.group(1) != "DONE":
        errors.append(f"Step {step} is not recorded DONE before Phase 1 completion review")
match26 = re.search(r"(?m)^\|\s*26\s*\|.*?\|\s*(DONE|IN PROGRESS|NOT STARTED|BLOCKED|DEFERRED)\s*\|", status)
if not match26 or match26.group(1) != "DONE":
    errors.append("Step 26 must be DONE after Phase 1 completion verification")

release = (root / ".github/workflows/release.yml").read_text(encoding="utf-8")
if "v*.*.*" not in release or "ghcr.io" not in release:
    errors.append("release/versioning workflow is not documented by an implemented tag/GHCR pipeline")

compose = (root / "docker-compose.yml").read_text(encoding="utf-8")
if "frontend:" not in compose or "backend:" not in compose:
    errors.append("Docker Compose runtime does not contain frontend and backend")
if re.search(r"(?im)^  (postgres|postgresql|database|db):\s*$", compose):
    errors.append("Phase 1 unexpectedly contains a database service")

# Validate that markdown references to docs/scripts that look like project paths exist.
for doc in [root / "README.md", *sorted((root / "docs").glob("*.md"))]:
    text = doc.read_text(encoding="utf-8")
    for match in re.finditer(r"`((?:docs|scripts)/[^`\s]+)`", text):
        relative = match.group(1).rstrip(".,;:")
        if "*" in relative or "<" in relative or ">" in relative:
            continue
        if not (root / relative).exists():
            errors.append(f"{doc.relative_to(root)} references missing path: {relative}")

if errors:
    print("PHASE 1 COMPLETION VALIDATION FAILED")
    for error in errors:
        print(f"- {error}")
    raise SystemExit(1)

print("RepoFleet Phase 1 completion documentation validation passed.")
