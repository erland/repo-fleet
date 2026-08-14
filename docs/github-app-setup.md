# GitHub App Setup

RepoFleet Phase 1 uses a GitHub App installation for read-only repository inventory and analysis.

## Required repository permissions

Configure the GitHub App with read-only access for:

- **Metadata: Read-only** – repository metadata and topics.
- **Contents: Read-only** – root contents, LICENSE detection and release information.
- **Actions: Read-only** – workflow inventory.

Do not grant write permissions for Phase 1.

The app can be installed for all repositories or a selected repository subset. RepoFleet only sees repositories accessible to that installation.

## Create and install the app

1. Create a GitHub App under the GitHub account or organization that should own it.
2. Configure the repository permissions above.
3. No webhook is required for Phase 1; refreshes are explicit/in-memory.
4. Generate a private key for the app.
5. Install the app on the account/organization containing the repositories.
6. Choose either all repositories or the specific repositories RepoFleet should analyze.
7. Note:
   - the GitHub App ID,
   - the installation ID,
   - the downloaded private-key file.

## Configure RepoFleet

Required identity variables:

```text
REPOFLEET_GITHUB_APP_ID
REPOFLEET_GITHUB_INSTALLATION_ID
```

Then provide the private key through exactly one of these approaches:

- `REPOFLEET_GITHUB_PRIVATE_KEY_PATH` when the PEM file is actually mounted/readable inside the backend runtime.
- `REPOFLEET_GITHUB_PRIVATE_KEY` containing the PEM text.

For native or managed-container deployments, a read-only mounted file is preferred:

```bash
export REPOFLEET_GITHUB_APP_ID=123456
export REPOFLEET_GITHUB_INSTALLATION_ID=987654321
export REPOFLEET_GITHUB_PRIVATE_KEY_PATH=/run/secrets/github-app.pem
```

The stock Phase 1 `docker-compose.yml` does not automatically mount a host PEM file. For the simplest Compose setup, supply `REPOFLEET_GITHUB_PRIVATE_KEY` through the local `.env`/runtime environment, or extend the Compose deployment with an explicit read-only secret/bind mount before using `REPOFLEET_GITHUB_PRIVATE_KEY_PATH`.

Optional variables:

```text
REPOFLEET_GITHUB_TOKEN_REFRESH_MARGIN_SECONDS
GITHUB_API_URL
```

## Validate the connection

After the backend is running:

```text
GET /api/github/connection
```

Expected states:

- `CONNECTED` – app authentication and installation token acquisition succeeded.
- `NOT_CONFIGURED` – required configuration is missing.
- `ERROR` – configuration exists but the connection check failed.

The endpoint never returns the JWT, installation token or private key.

## Token lifecycle

RepoFleet signs a GitHub App JWT server-side, exchanges it for an installation access token and caches that token until it approaches expiry. A `401` during GitHub API access invalidates the cached installation token and triggers acquisition of a fresh token.

No browser-side GitHub credential is used.

## Security boundary

Phase 1 is read-only. Do not add repository write permissions merely to make a read endpoint work. If a required analysis endpoint cannot be accessed, RepoFleet should report an unknown/failed analysis rather than broaden permissions silently.

Write permissions should be reconsidered only when later maintenance phases introduce explicit repository changes.
