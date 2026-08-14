from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]

errors: list[str] = []

# Project Java code must retain the agreed package namespace.
java_root = root / "backend" / "src"
for path in java_root.rglob("*.java"):
    text = path.read_text(encoding="utf-8")
    package = re.search(r"(?m)^package\s+([A-Za-z0-9_.]+);", text)
    if package and not package.group(1).startswith("info.isaksson.erland.repofleet"):
        errors.append(f"{path.relative_to(root)} uses unexpected package {package.group(1)}")

# Environment examples must remain examples, not committed credentials/private keys.
env_example = (root / ".env.example").read_text(encoding="utf-8")
if "BEGIN PRIVATE KEY" in env_example or "BEGIN RSA PRIVATE KEY" in env_example:
    errors.append(".env.example contains private-key material")

for secret_name in (
    "REPOFLEET_GITHUB_APP_ID",
    "REPOFLEET_GITHUB_INSTALLATION_ID",
    "REPOFLEET_GITHUB_PRIVATE_KEY",
):
    match = re.search(rf"(?m)^{re.escape(secret_name)}=(.+)$", env_example)
    if match and match.group(1).strip():
        errors.append(f".env.example contains a non-empty {secret_name}")

# Generated build output and local dependencies must remain ignored.
gitignore = (root / ".gitignore").read_text(encoding="utf-8")
for required in ("frontend/node_modules/", "frontend/dist/", "backend/target/", ".env", "*.pem", "*.key"):
    if required not in gitignore:
        errors.append(f".gitignore is missing required entry: {required}")

# Phase 1 must remain DB-free.
compose = (root / "docker-compose.yml").read_text(encoding="utf-8")
if re.search(r"(?im)^  (postgres|postgresql|database|db):\s*$", compose):
    errors.append("Phase 1 docker-compose.yml unexpectedly defines a database service")

# Acceptance mapping must continue to cover all 13 Phase 1 criteria.
acceptance = (root / "docs" / "phase-1-acceptance-validation.md").read_text(encoding="utf-8")
for criterion in range(1, 14):
    if f"| {criterion} |" not in acceptance:
        errors.append(f"Phase 1 acceptance validation is missing criterion {criterion}")

# Basic source hygiene on project-owned text files. Avoid generated/vendor paths.
suffixes = {".java", ".ts", ".tsx", ".css", ".yml", ".yaml", ".py", ".sh", ".json", ".properties"}
ignored_parts = {".git", "node_modules", "target", "dist"}
for path in root.rglob("*"):
    if not path.is_file() or path.suffix not in suffixes or any(part in ignored_parts for part in path.parts):
        continue
    lines = path.read_text(encoding="utf-8").splitlines()
    for number, line in enumerate(lines, 1):
        if line.rstrip() != line:
            errors.append(f"{path.relative_to(root)}:{number} has trailing whitespace")
            if len(errors) >= 25:
                break
    if len(errors) >= 25:
        break

if errors:
    print("QUALITY GATE POLICY FAILED")
    for error in errors:
        print(f"- {error}")
    raise SystemExit(1)

print("RepoFleet quality-gate policy validation passed.")
