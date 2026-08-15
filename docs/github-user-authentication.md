# GitHub user authentication

RepoFleet can protect its UI/API with the same GitHub App that is already used for installation-based repository access. User login and repository access are deliberately separate concerns.

## Flow

1. Browser requests `/api/auth/login`.
2. Backend generates OAuth `state` and a PKCE verifier/challenge.
3. Browser is redirected to GitHub App user authorization.
4. GitHub redirects to `/api/auth/github/callback`.
5. Backend validates `state`, exchanges the code using the App Client ID/Client Secret and PKCE verifier, then calls GitHub `/user`.
6. The GitHub login must be present in `REPOFLEET_AUTH_ALLOWED_USERS`.
7. RepoFleet discards the GitHub user access token and creates its own signed HttpOnly session cookie.

Repository inventory calls continue to use the GitHub App **installation access token**, not the signed-in user's access token.

## Runtime settings

- `REPOFLEET_AUTH_ENABLED=true`
- `REPOFLEET_AUTH_CLIENT_ID` – GitHub App Client ID (not App ID)
- `REPOFLEET_AUTH_CLIENT_SECRET` – GitHub App Client Secret
- `REPOFLEET_AUTH_SESSION_SECRET` – random secret, at least 32 characters
- `REPOFLEET_AUTH_CALLBACK_URL=https://repo-fleet.isaksson.info/api/auth/github/callback`
- `REPOFLEET_AUTH_ALLOWED_USERS` – comma-separated GitHub logins, case-insensitive
- `REPOFLEET_AUTH_SESSION_HOURS=12`
- `REPOFLEET_AUTH_COOKIE_SECURE=true`

Generate a session secret with, for example:

```bash
openssl rand -base64 48
```

The callback URL must also be registered under **GitHub App → General → Identifying and authorizing users → Callback URL**.

Normal development/CI leaves authentication disabled. Production server Compose enables it by default and requires the credentials/allowlist.
