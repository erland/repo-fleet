from pathlib import Path

root = Path(__file__).resolve().parents[1]
service = (root / "backend/src/main/java/info/isaksson/erland/repofleet/repository/inventory/InMemoryRepositoryInventoryService.java").read_text(encoding="utf-8")
frontend = (root / "frontend/src/App.tsx").read_text(encoding="utf-8")

checks = {
    "startup refresh is asynchronous": "void initialize() {\n        startRefresh();" in service,
    "discovered snapshot is published before enrichment": "repositories = List.copyOf(working);" in service,
    "each enriched repository republishes the snapshot": "working.set(index, enriched);\n                repositories = List.copyOf(working);" in service,
    "existing repository data is retained during refresh": "previousById.getOrDefault(repository.id(), repository)" in service,
    "frontend reloads repositories during refresh polling": "await loadRepositories(false)\n      if (nextStatus.state === 'RUNNING') return" in frontend,
}

failed = [name for name, ok in checks.items() if not ok]
if failed:
    raise SystemExit("Progressive inventory validation failed: " + ", ".join(failed))
print("RepoFleet progressive inventory validation passed.")
