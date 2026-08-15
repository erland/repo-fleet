from pathlib import Path

root = Path(__file__).resolve().parents[1]
workflow = (root / ".github/workflows/release-candidate.yml").read_text(encoding="utf-8")
deploy_workflow = (root / ".github/workflows/deploy.yml").read_text(encoding="utf-8")
deploy_script = (root / "deploy/deploy.sh").read_text(encoding="utf-8")

checks = {
    "manual RC workflow": "workflow_dispatch:" in workflow,
    "RC strict version syntax": "-rc\\.[0-9]+" in workflow,
    "default branch only": "Release candidates must be published from the default branch" in workflow,
    "source policy validation": "validate-quality-gate.py" in workflow,
    "deployment validation": "validate-deployment-step.py" in workflow,
    "frontend tests": "npm test" in workflow and "npm run typecheck" in workflow,
    "backend verify": "mvn --batch-mode --no-transfer-progress verify" in workflow,
    "GHCR packages write": "packages: write" in workflow,
    "immutable RC tag": "${{ needs.validate.outputs.version }}" in workflow,
    "source SHA tag": "${{ needs.validate.outputs.sha_tag }}" in workflow,
    "moving RC alias": "${{ matrix.image }}:rc" in workflow,
    "does not create GitHub Release": "gh release create" not in workflow,
    "deploy accepts RC": "(-rc\\.[0-9]+)?" in deploy_workflow,
    "server script accepts RC": "(-rc\\.[0-9]+)?" in deploy_script,
}

failed = [name for name, ok in checks.items() if not ok]
if failed:
    raise SystemExit("Release candidate validation failed: " + ", ".join(failed))
print("RepoFleet release candidate workflow validation passed.")
