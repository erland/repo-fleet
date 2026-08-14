from pathlib import Path

root = Path(__file__).resolve().parents[1]
workflow = (root / ".github" / "workflows" / "release.yml").read_text(encoding="utf-8")
packager = (root / "scripts" / "package-release.py").read_text(encoding="utf-8")

checks = {
    "semver-shaped tag trigger": "- 'v*.*.*'" in workflow,
    "tag validation": "^v([0-9]+)\\.([0-9]+)\\.([0-9]+)$" in workflow,
    "GHCR registry": "ghcr.io" in workflow,
    "packages write only publish job": "packages: write" in workflow,
    "release contents write": "contents: write" in workflow,
    "frontend image": "repo-fleet-frontend" in workflow,
    "backend image": "repo-fleet-backend" in workflow,
    "version image tag": "${{ needs.validate.outputs.version }}" in workflow,
    "sha image tag": "${{ needs.validate.outputs.sha_tag }}" in workflow,
    "moving latest alias": ":latest" in workflow,
    "tests before publish": "mvn --batch-mode --no-transfer-progress verify" in workflow and "npm test" in workflow,
    "deployment packager": "scripts/package-release.py" in workflow,
    "GitHub release": "gh release create" in workflow,
    "no private key packaged": "PRIVATE_KEY" not in packager and "BEGIN PRIVATE KEY" not in packager,
    "release compose removes builds": "skipping_build" in packager,
}

failed = [name for name, ok in checks.items() if not ok]
if failed:
    raise SystemExit("Step 25 release validation failed: " + ", ".join(failed))
print("Step 25 release workflow static validation passed.")
