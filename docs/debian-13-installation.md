# RepoFleet – komplett installation på Debian 13

Den här manualen beskriver en ny produktionsinstallation av RepoFleet på **Debian 13 (Trixie)** med:

- installation under `/opt/repo-fleet`,
- Docker Engine + Docker Compose,
- GitHub App för read-only åtkomst till repositories,
- GitHub Container Registry (GHCR) för versionerade images,
- Nginx som reverse proxy på servern,
- HTTPS för `repo-fleet.isaksson.info` med Let's Encrypt/Certbot,
- GitHub-inloggning via RepoFleet GitHub App framför tjänstens data/API,
- en dedikerad SSH-användare för GitHub Actions-deploy,
- manuellt startad GitHub Action för deployment av en vald officiell version eller release candidate.

> **Viktigt om åtkomstskydd:** RepoFleet använder GitHub-inloggning med en explicit allowlist. GitHub App-installationen styr vilka repositories tjänsten får analysera, medan den inloggade GitHub-identiteten styr vem som får använda webbgränssnittet. Nginx Basic Auth används inte.

---

## 1. Förutsättningar

Du behöver:

- en Debian 13-server med publik IPv4-adress,
- root/sudo-access till servern,
- DNS-kontroll över `isaksson.info`,
- GitHub-repositoryt `erland/repo-fleet`,
- rätt att skapa en GitHub App,
- rätt att administrera GitHub Actions Environments/secrets i repositoryt,
- minst en publicerad RepoFleet-imageversion i GHCR när första deploymenten ska göras; det kan vara en release candidate som `1.0.1-rc.1` eller en officiell version som `1.0.0`.

Om servern har fungerande publik IPv6 kan även en AAAA-post användas. Skapa inte AAAA-post om IPv6-routing eller brandvägg inte fungerar korrekt.

---

# Del A – DNS och grundserver

## 2. Skapa DNS-post

Hos DNS-leverantören för `isaksson.info`, skapa:

```text
Typ:   A
Namn:  repo-fleet
Värde: <SERVERNS_PUBLIKA_IPV4>
TTL:   exempelvis 300 eller leverantörens standard
```

Om servern har fungerande IPv6:

```text
Typ:   AAAA
Namn:  repo-fleet
Värde: <SERVERNS_PUBLIKA_IPV6>
```

Verifiera från en annan dator:

```bash
dig +short repo-fleet.isaksson.info A
dig +short repo-fleet.isaksson.info AAAA
```

A-posten ska peka på rätt server innan certifikatet beställs.

---

## 3. Uppdatera Debian 13

Logga in på servern med din vanliga administrativa användare:

```bash
ssh <ADMIN_USER>@repo-fleet.isaksson.info
```

Uppdatera systemet:

```bash
sudo apt update
sudo apt full-upgrade -y
```

Om uppdateringen installerade ny kernel är det lämpligt att starta om innan du fortsätter:

```bash
sudo reboot
```

Logga därefter in igen.

Kontrollera tidssynkronisering, eftersom både TLS och GitHub App-JWT är beroende av korrekt klocka:

```bash
timedatectl status
```

`System clock synchronized` bör vara `yes`.

---

## 4. Installera baspaket

```bash
sudo apt install -y \
  ca-certificates \
  curl \
  gnupg \
  openssh-server \
  nginx \
  python3 \
  python3-dev \
  python3-venv \
  libaugeas-dev \
  gcc
```

Aktivera SSH och Nginx:

```bash
sudo systemctl enable --now ssh
sudo systemctl enable --now nginx
```

---

# Del B – Docker

## 5. Installera Docker Engine från Dockers officiella Debian-repository

Ta bort eventuella konfliktande paket om de finns:

```bash
sudo apt remove -y docker.io docker-compose docker-doc docker-buildx podman-docker containerd runc || true
```

Lägg till Dockers officiella nyckel:

```bash
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/debian/gpg \
  -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc
```

Lägg till Docker-repositoryt:

```bash
sudo tee /etc/apt/sources.list.d/docker.sources >/dev/null <<EOF_DOCKER
Types: deb
URIs: https://download.docker.com/linux/debian
Suites: $(. /etc/os-release && echo "$VERSION_CODENAME")
Components: stable
Architectures: $(dpkg --print-architecture)
Signed-By: /etc/apt/keyrings/docker.asc
EOF_DOCKER
```

Installera Docker Engine och Compose-plugin:

```bash
sudo apt update
sudo apt install -y \
  docker-ce \
  docker-ce-cli \
  containerd.io \
  docker-buildx-plugin \
  docker-compose-plugin
```

Aktivera Docker:

```bash
sudo systemctl enable --now docker
```

Verifiera:

```bash
sudo docker run --rm hello-world
docker compose version
```

Debian 13/Trixie stöds av Dockers officiella Debian-installation.

---

# Del C – användare och katalogstruktur under /opt

## 6. Skapa deployment-användare

Skapa en separat användare för RepoFleet-deployments:

```bash
sudo useradd \
  --system \
  --create-home \
  --home-dir /opt/repo-fleet \
  --shell /bin/bash \
  repofleet-deploy
```

Lägg användaren i Docker-gruppen:

```bash
sudo usermod -aG docker repofleet-deploy
```

> Medlemskap i `docker`-gruppen ger i praktiken mycket höga/root-liknande rättigheter genom Docker. Använd därför en dedikerad deployment-nyckel och dela inte användaren med andra tjänster.

Skapa kataloger:

```bash
sudo install -d -m 0750 \
  -o repofleet-deploy -g repofleet-deploy \
  /opt/repo-fleet

sudo install -d -m 0700 \
  -o repofleet-deploy -g repofleet-deploy \
  /opt/repo-fleet/secrets

sudo install -d -m 0700 \
  -o repofleet-deploy -g repofleet-deploy \
  /opt/repo-fleet/.ssh
```

---

## 7. Skapa separat SSH-nyckel för GitHub Actions

Gör detta på en betrodd administratörsdator, **inte i GitHub Actions**:

```bash
ssh-keygen \
  -t ed25519 \
  -f ~/.ssh/repo-fleet-github-deploy \
  -C "repo-fleet GitHub Actions production deploy"
```

Använd gärna en separat nyckel som bara används för denna deployment.

Visa public key:

```bash
cat ~/.ssh/repo-fleet-github-deploy.pub
```

På servern, lägg in public key:

```bash
sudo tee /opt/repo-fleet/.ssh/authorized_keys >/dev/null <<'EOF_KEY'
restrict ssh-ed25519 <PUBLIC_KEY_DATA> repo-fleet GitHub Actions production deploy
EOF_KEY

sudo chown repofleet-deploy:repofleet-deploy \
  /opt/repo-fleet/.ssh/authorized_keys
sudo chmod 0600 /opt/repo-fleet/.ssh/authorized_keys
```

Testa från administratörsdatorn:

```bash
ssh -i ~/.ssh/repo-fleet-github-deploy \
  repofleet-deploy@repo-fleet.isaksson.info \
  'id && docker version --format "{{.Server.Version}}"'
```

---

# Del D – GitHub App

## 8. Skapa GitHub App

På GitHub:

1. Öppna **Settings** för det konto/den organisation som ska äga appen.
2. Gå till **Developer settings → GitHub Apps → New GitHub App**.
3. Ange exempelvis:

```text
GitHub App name: RepoFleet
Homepage URL:    https://repo-fleet.isaksson.info
Callback URL:    https://repo-fleet.isaksson.info/api/auth/github/callback
```

4. RepoFleet använder inga webhooks i den här fasen. Stäng av webhook-funktionen/`Active` om GitHub-formuläret tillåter det.
5. Under **Repository permissions**, använd read-only:

```text
Metadata: Read-only
Contents: Read-only
Actions: Read-only
```

6. Ge inga write-permissions enbart för inloggningen.
7. Välj om appen bara får installeras på ditt konto eller enligt den installationsmodell du önskar.
8. Skapa appen.

Notera följande tre separata värden från appens inställningssida:

- **App ID** – används för installation/autentisering mot repositories.
- **Client ID** – används för användarinloggning och är inte samma sak som App ID.
- **Client Secret** – generera ett nytt under appens inställningar och behandla det som en hemlighet.

Callback URL kan också läggas till i efterhand under **Identifying and authorizing users**. Den måste vara exakt:

```text
https://repo-fleet.isaksson.info/api/auth/github/callback
```

RepoFleet använder GitHub Apps web application flow med `state` och PKCE för användarinloggning. Repositoryinventeringen fortsätter oberoende av detta att använda installation access token.

---

## 9. Generera GitHub App private key

På GitHub App-inställningssidan:

1. Gå till **Private keys**.
2. Klicka **Generate a private key**.
3. En `.pem`-fil laddas ned.

Förvara den som en hemlighet.

Kopiera filen till servern, tillfälligt exempel:

```bash
scp <NEDLADDAD_APP_PRIVATE_KEY>.pem \
  <ADMIN_USER>@repo-fleet.isaksson.info:/tmp/repo-fleet-github-app.pem
```

På servern:

```bash
sudo install \
  -o repofleet-deploy \
  -g repofleet-deploy \
  -m 0644 \
  /tmp/repo-fleet-github-app.pem \
  /opt/repo-fleet/secrets/github-app.pem

sudo rm -f /tmp/repo-fleet-github-app.pem
```

Filen har `0644` för att den non-root backend-process som körs i containern ska kunna läsa bind-mounten. Katalogen `/opt/repo-fleet/secrets` är samtidigt `0700`, vilket hindrar andra vanliga serveranvändare från att traversera katalogen.

---

## 10. Installera GitHub App på repositories

På GitHub App-sidan:

1. Klicka **Install App**.
2. Installera den på rätt konto/organisation.
3. Välj:
   - **All repositories**, eller
   - endast de repositories som RepoFleet ska analysera.
4. Slutför installationen.

Notera **Installation ID**. Det syns bland annat i installationssidans URL, typ:

```text
.../settings/installations/12345678
```

Här är `12345678` installation ID.

---

## 11. Skapa serverns RepoFleet-konfiguration

Skapa först en slumpmässig sessionshemlighet. Den används av RepoFleet för att signera den egna HttpOnly-sessionen efter GitHub-login:

```bash
openssl rand -base64 48
```

Skapa därefter `/opt/repo-fleet/.env`:

```bash
sudo -u repofleet-deploy tee /opt/repo-fleet/.env >/dev/null <<'EOF_ENV'
COMPOSE_PROJECT_NAME=repo-fleet
REPOFLEET_FRONTEND_PORT=8082

REPOFLEET_GITHUB_APP_ID=<GITHUB_APP_ID>
REPOFLEET_GITHUB_INSTALLATION_ID=<GITHUB_INSTALLATION_ID>
REPOFLEET_GITHUB_TOKEN_REFRESH_MARGIN_SECONDS=300
GITHUB_API_URL=https://api.github.com

REPOFLEET_AUTH_ENABLED=true
REPOFLEET_AUTH_CLIENT_ID=<GITHUB_APP_CLIENT_ID>
REPOFLEET_AUTH_CLIENT_SECRET=<GITHUB_APP_CLIENT_SECRET>
REPOFLEET_AUTH_SESSION_SECRET=<SLUMPMÄSSIG_SESSION_SECRET>
REPOFLEET_AUTH_CALLBACK_URL=https://repo-fleet.isaksson.info/api/auth/github/callback
REPOFLEET_AUTH_ALLOWED_USERS=<GITHUB_LOGIN>
REPOFLEET_AUTH_SESSION_HOURS=12
REPOFLEET_AUTH_COOKIE_SECURE=true

JAVA_OPTS=-XX:MaxRAMPercentage=75.0
EOF_ENV

sudo chmod 0600 /opt/repo-fleet/.env
```

`REPOFLEET_AUTH_ALLOWED_USERS` kan innehålla flera GitHub-login separerade med komma, exempelvis:

```text
REPOFLEET_AUTH_ALLOWED_USERS=user-one,user-two
```

Sätt in riktiga värden men checka **aldrig** in serverns `.env`. `deploy/.env.server.example` i repositoryt är endast en mall utan hemligheter.

### Om porten

`REPOFLEET_FRONTEND_PORT` är **host-porten** där RepoFleet frontend publiceras på loopback. Standard/rekommendation i denna installation är `8082` eftersom andra tjänster på samma server redan kan använda exempelvis `8080`.

```text
Nginx → 127.0.0.1:${REPOFLEET_FRONTEND_PORT} → frontend-container:8080 → backend-container:8080
```

Du kan välja en annan ledig port genom att ändra `REPOFLEET_FRONTEND_PORT` i `/opt/repo-fleet/.env`. Nginx läser inte `.env`, så `proxy_pass` i Nginx-konfigurationen måste använda **samma host-port**.

Image-referenser läggs inte permanent i `.env`; deploy-scriptet skriver en separat `.images.env` med exakt deployad version.

---

# Del E – Nginx och HTTPS

## 12. Skapa Nginx reverse proxy över HTTP

Innan Certbot körs behöver Nginx ha en fungerande HTTP-vhost för domänen. Exemplet nedan utgår från `REPOFLEET_FRONTEND_PORT=8082` i `/opt/repo-fleet/.env`.

> Om du valt en annan port i `.env`, byt `8082` i `proxy_pass` till samma värde.

```bash
sudo tee /etc/nginx/sites-available/repo-fleet.isaksson.info >/dev/null <<'EOF_NGINX'
server {
    listen 80;
    listen [::]:80;
    server_name repo-fleet.isaksson.info;

    add_header X-Content-Type-Options "nosniff" always;
    add_header Referrer-Policy "same-origin" always;

    location / {
        proxy_pass http://127.0.0.1:8082;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 5s;
        proxy_read_timeout 180s;
    }
}
EOF_NGINX

sudo ln -sfn \
  /etc/nginx/sites-available/repo-fleet.isaksson.info \
  /etc/nginx/sites-enabled/repo-fleet.isaksson.info

sudo nginx -t
sudo systemctl reload nginx
```

Det är normalt att du får `502 Bad Gateway` innan RepoFleet-containern har deployats. Det viktiga inför Certbot är att Nginx-konfigurationen är giltig och att DNS/port 80 når rätt server.

---

## 13. Installera eller verifiera Certbot med Nginx-plugin

Om servern redan använder Certbot/Nginx på samma sätt som andra tjänster behöver du **inte** installera en separat RepoFleet-instans av Certbot. Verifiera först:

```bash
certbot --version
```

Om Certbot inte finns kan du använda samma installationsmetod som för övriga Nginx-siter på servern. RepoFleet kräver att Nginx-pluginen finns så `certbot --nginx` kan läsa och uppdatera vhosten.

Den tidigare RepoFleet-guidens `certonly --webroot`-modell används inte längre.

---

## 14. Beställ och installera HTTPS-certifikatet med Nginx-plugin

Kontrollera först:

```bash
sudo nginx -t
```

Kör sedan samma Certbot-modell som för `zip-github`:

```bash
sudo certbot --nginx -d repo-fleet.isaksson.info
```

Följ Certbots frågor. När Certbot erbjuder redirect från HTTP till HTTPS, välj redirect om din installerade version frågar efter detta.

Nginx-pluginen hämtar certifikatet **och ändrar Nginx-konfigurationen** med bland annat `listen 443 ssl`, certifikatvägar och HTTPS/redirect. Redigera därför inte in en separat manuell TLS-server från den gamla guiden efteråt.

Verifiera:

```bash
sudo nginx -t
sudo systemctl reload nginx
curl -I https://repo-fleet.isaksson.info/
```

Innan första application deployment kan HTTPS fortfarande ge `502 Bad Gateway`, eftersom Nginx då ännu inte har något på `127.0.0.1:8082`.

### Flera HTTPS-siter på samma Nginx

Om servern redan har andra Certbot-hanterade siter, använd konsekventa listen-rader. Ett tidigare problem med `duplicate listen options for [::]:443` kan uppstå om en site explicit har `ipv6only=on` och en annan delar samma `[::]:443`-listener.

Standardisera i så fall exempelvis till:

```nginx
listen 443 ssl;
listen [::]:443 ssl;
```

på de berörda HTTPS-vhostarna och kör alltid `sudo nginx -t` före reload.

---

## 15. Verifiera automatisk certifikatförnyelse

Certbot-installationer har normalt redan cron eller systemd-timer för `certbot renew`. Skapa inte en andra RepoFleet-specifik renewal-timer innan du kontrollerat vad servern redan har.

Kontrollera exempelvis:

```bash
systemctl list-timers | grep -i certbot || true
sudo grep -R "certbot renew" /etc/cron* 2>/dev/null || true
```

Testa sedan hela renewal-flödet:

```bash
sudo certbot renew --dry-run
```

Eftersom certifikatet skapades med Nginx-pluginen återanvänder `certbot renew` samma plugin vid förnyelse.

---

## 16. GitHub-login ersätter Basic Authentication

Skapa **ingen** `/etc/nginx/.htpasswd-repo-fleet` och lägg inte `auth_basic` i vhosten.

Efter deployment laddas RepoFleet-gränssnittet normalt via HTTPS. En besökare utan giltig RepoFleet-session får en **Sign in with GitHub**-vy. Efter GitHub-login verifierar backend att login finns i `REPOFLEET_AUTH_ALLOWED_USERS` och skapar därefter en Secure/HttpOnly-session-cookie.

Nginx ansvarar alltså för TLS/reverse proxy; RepoFleet ansvarar för användarautentisering.

---

## 17. Kontrollera slutlig Nginx-konfiguration

Efter `certbot --nginx` bör konfigurationen funktionellt motsvara:

```text
Internet :443
   ↓
Nginx / Certbot TLS
   ↓
127.0.0.1:<REPOFLEET_FRONTEND_PORT>
   ↓
RepoFleet frontend
   ↓ /api
RepoFleet backend
   ↓
GitHub user login/session + installation API
```

Certbots exakta genererade TLS-rader kan skilja sig mellan versioner. Behåll de rader som markeras `managed by Certbot` så länge `sudo nginx -t` är lyckad.

Kontrollera särskilt att `proxy_pass` fortfarande pekar på samma port som `REPOFLEET_FRONTEND_PORT` i `/opt/repo-fleet/.env`.

---

# Del F – brandvägg och extern exponering

## 18. Öppna bara nödvändiga portar

Server/provider-brandväggen ska åtminstone tillåta de portar du faktiskt använder. För RepoFleet:

```text
TCP 22   SSH, eller din alternativa SSH-port
TCP 80   HTTP, används för redirect och Let's Encrypt challenge
TCP 443  HTTPS
```

Eftersom du redan kör andra tjänster på servern ska du **inte ersätta befintliga brandväggsregler blint**. Lägg till RepoFleet-behoven i din nuvarande policy.

RepoFleets Docker Compose för serverdrift publicerar endast frontend på:

```text
127.0.0.1:${REPOFLEET_FRONTEND_PORT}
```

Backend publiceras inte till hostens nätverk alls. Docker-portarna behöver därför inte öppnas externt.

Efter deployment kan du verifiera:

```bash
sudo ss -ltnp | grep -E ':(80|443|8082)\b'
```

Port `8082` ska visas på loopback (`127.0.0.1`), inte på `0.0.0.0`.

---

# Del G – GitHub Release/GHCR

## 19. Publicera en deploybar version

För den första driftsättningen kan du välja antingen en release candidate eller en officiell release.

### Alternativ A – release candidate utan formell release

Gå till **Actions → Publish release candidate → Run workflow** på `main` och ange exempelvis:

```text
1.0.1-rc.1
```

Workflowet validerar/testar källan och publicerar därefter:

```text
ghcr.io/erland/repo-fleet-frontend:1.0.1-rc.1
ghcr.io/erland/repo-fleet-backend:1.0.1-rc.1
```

Ingen Git-tag eller GitHub Release skapas för RC-versionen.

### Alternativ B – officiell release

RepoFleet använder officiella release-taggar `vMAJOR.MINOR.PATCH`, exempelvis:

```bash
git tag v1.0.0
git push origin v1.0.0
```

Release-workflowet publicerar då `:1.0.0`-images och skapar/uppdaterar GitHub Release `v1.0.0`.

På GitHub Packages-inställningarna för båda packages, kontrollera att repositoryt `erland/repo-fleet` har Actions-åtkomst till dem. När packages är kopplade till repositoryt kan workflowets `GITHUB_TOKEN` användas för `packages: read` under deployment.

---

# Del H – GitHub Environment och deploy-secrets

## 20. Verifiera serverns SSH host key

På servern:

```bash
sudo ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub
```

Notera fingerprint.

På en betrodd administratörsdator:

```bash
ssh-keyscan -t ed25519 -H repo-fleet.isaksson.info \
  > repo-fleet.known_hosts

ssh-keygen -lf repo-fleet.known_hosts
```

Kontrollera att fingerprint matchar serverns lokalt lästa fingerprint innan filen används i GitHub.

Visa filinnehållet:

```bash
cat repo-fleet.known_hosts
```

---

## 21. Skapa GitHub Environment `production`

I `erland/repo-fleet`:

1. Gå till **Settings → Environments**.
2. Skapa environment:

```text
production
```

3. Om du vill ha extra säkerhet kan du konfigurera required reviewers så att varje production deploy måste godkännas.

Lägg följande **Environment secrets**:

### `DEPLOY_HOST`

```text
repo-fleet.isaksson.info
```

### `DEPLOY_USER`

```text
repofleet-deploy
```

### `DEPLOY_SSH_PRIVATE_KEY`

Innehållet från den privata deploy-nyckeln:

```bash
cat ~/.ssh/repo-fleet-github-deploy
```

Lägg hela private key-innehållet som secret.

### `DEPLOY_KNOWN_HOSTS`

Innehållet från den verifierade:

```bash
cat repo-fleet.known_hosts
```

Lägg detta som secret.

Skapa dessutom environment **variable**:

```text
DEPLOY_PORT=22
```

Om SSH kör på annan port, använd den istället.

Deploy-workflowet använder inte `StrictHostKeyChecking=no`; serverns host key måste matcha `DEPLOY_KNOWN_HOSTS`.

---

# Del I – första deploymenten

## 22. Lägg deploy-workflowet på default branch

Filen ska finnas i repositoryt som:

```text
.github/workflows/deploy.yml
```

Ett manuellt GitHub workflow (`workflow_dispatch`) måste finnas på default branch för att visas/köras från Actions-gränssnittet.

När den här projektversionen är mergad till `main` ska workflowet **Deploy production** dyka upp under Actions.

---

## 23. Kör första deploymenten

På GitHub:

1. Gå till **Actions**.
2. Välj **Deploy production**.
3. Klicka **Run workflow**.
4. Ange en redan publicerad immutable version, exempelvis en RC:

```text
1.0.1-rc.1
```

eller en officiell version:

```text
1.0.0
```

5. Starta workflowet.

Workflowet:

1. validerar versionsformatet,
2. kräver GitHub Release endast för officiella versioner (RC kräver ingen formell release),
3. kopierar `docker-compose.server.yml` och `deploy.sh` till `/opt/repo-fleet`,
4. loggar in servern temporärt mot GHCR med workflowets kortlivade `GITHUB_TOKEN`,
5. pullar exakt vald frontend/backend-version,
6. startar Compose med health checks,
7. försöker återgå till föregående image-version om den nya versionen inte blir healthy,
8. verifierar HTTPS och RepoFleets auth-session-endpoint,
9. loggar ut servern från GHCR.

Servern behöver inget Git-repository checkout och ingen permanent GitHub Packages-token för denna deploymodell. Driftfilerna kopieras från workflowets checkout och applikationen körs från versionerade GHCR-images.

---

## 24. Verifiera första deploymenten på servern

På servern:

```bash
sudo -u repofleet-deploy -H bash -lc '
  cd /opt/repo-fleet
  docker compose \
    --env-file .env \
    --env-file .images.env \
    -f docker-compose.server.yml \
    ps
'
```

Både `frontend` och `backend` ska vara `healthy`.

Kontrollera image-versioner:

```bash
sudo -u repofleet-deploy cat /opt/repo-fleet/.images.env
```

Kontrollera den host-port som är satt i `.env`:

```bash
grep '^REPOFLEET_FRONTEND_PORT=' /opt/repo-fleet/.env
sudo ss -ltnp | grep ':8082'
```

Om du valt annan port än `8082`, använd den i kontrollen. Porten ska vara bunden till `127.0.0.1`, inte `0.0.0.0`.

Kontrollera frontend lokalt med samma port:

```bash
curl -I http://127.0.0.1:8082/
```

Kontrollera publik HTTPS:

```bash
curl -I https://repo-fleet.isaksson.info/
```

Den statiska frontend-sidan ska kunna svara `200 OK` även när du ännu inte är inloggad; känsliga API-endpoints skyddas i backend. Kontrollera auth-status:

```bash
curl -sS https://repo-fleet.isaksson.info/api/auth/session
```

Före login ska svaret motsvara:

```json
{"authEnabled":true,"authenticated":false,"user":null}
```

Öppna därefter tjänsten i webbläsaren och fortsätt med GitHub-login i steg 25.

---

## 25. Verifiera GitHub-inloggning och GitHub App-anslutning

Öppna:

```text
https://repo-fleet.isaksson.info/
```

Du ska först se **Sign in with GitHub**. Klicka på knappen och godkänn GitHub App-användarauktoriseringen.

Efter callback ska du komma tillbaka till RepoFleet som inloggad användare. Headern visar GitHub-login och en **Sign out**-knapp.

Verifiera session via webbläsaren eller, efter login, genom UI:t. Om en annan GitHub-användare som inte finns i `REPOFLEET_AUTH_ALLOWED_USERS` försöker logga in ska RepoFleet neka åtkomst.

Repositoryinventeringen använder fortfarande GitHub App-installationen. Kontrollera därför också att GitHub-anslutningen fungerar och att repositories kan refreshas. Om login fungerar men inventory inte gör det är det normalt en separat App ID/Installation ID/private-key-fråga.

Vid felsökning:

```bash
sudo -u repofleet-deploy -H bash -lc '
  cd /opt/repo-fleet
  docker compose --env-file .env --env-file .images.env \
    -f docker-compose.server.yml logs --tail=200 backend
'
```

Vanliga auth-konfigurationsfel är fel **Client ID**, fel **Client Secret**, callback URL som inte exakt matchar GitHub App-inställningen, för kort sessionshemlighet eller att login saknas i allowlist.

---

# Del J – framtida deployment och rollback

## 26. Deploya en ny version

När en ny RC eller officiell version har publicerats och dess workflow är grönt:

1. **Actions → Deploy production**
2. **Run workflow**
3. `version`, exempelvis:

```text
1.1.0-rc.3
```

eller:

```text
1.1.0
```

4. Kör.

Ingen serverinloggning eller manuell ändring av `.env` behövs för normal versionsuppgradering.

---

## 27. Rollback

En deployment som inte blir healthy försöker automatiskt starta föregående image-konfiguration igen.

För en explicit rollback kör du deployment-workflowet manuellt med en tidigare fortfarande publicerad immutable version, exempelvis:

```text
1.0.0
```

eller en tidigare RC om du uttryckligen vill återgå till den.

Det är en av anledningarna till att deployment alltid använder immutable versions-taggar istället för de rörliga aliasen `rc` eller `latest`.

---

# Del K – drift och felsökning

## 28. Vanliga driftkommandon

### Status

```bash
sudo -u repofleet-deploy -H bash -lc '
  cd /opt/repo-fleet
  docker compose --env-file .env --env-file .images.env \
    -f docker-compose.server.yml ps
'
```

### Loggar

```bash
sudo -u repofleet-deploy -H bash -lc '
  cd /opt/repo-fleet
  docker compose --env-file .env --env-file .images.env \
    -f docker-compose.server.yml logs --tail=200
'
```

### Följ loggar

```bash
sudo -u repofleet-deploy -H bash -lc '
  cd /opt/repo-fleet
  docker compose --env-file .env --env-file .images.env \
    -f docker-compose.server.yml logs -f
'
```

### Starta om

```bash
sudo -u repofleet-deploy -H bash -lc '
  cd /opt/repo-fleet
  docker compose --env-file .env --env-file .images.env \
    -f docker-compose.server.yml restart
'
```

### Nginx

```bash
sudo nginx -t
sudo systemctl status nginx
sudo journalctl -u nginx --since today
```

### Docker

```bash
sudo systemctl status docker
sudo journalctl -u docker --since today
```

### Certifikat

```bash
sudo certbot certificates
systemctl list-timers | grep -i certbot || true
```

---

## 29. Backup

Phase 1 har ingen databas. Det viktigaste server-specifika innehållet att säkerhetskopiera är därför (observera att `.env` nu även innehåller GitHub OAuth Client Secret och RepoFleets sessionshemlighet):

```text
/opt/repo-fleet/.env
/opt/repo-fleet/secrets/github-app.pem
/etc/nginx/sites-available/repo-fleet.isaksson.info
/etc/letsencrypt/
```

Saved views ligger i respektive webbläsares `localStorage` och ingår inte i serverbackup.

Deployment-filerna och images kan återskapas från GitHub/GHCR.

---

## 30. Uppdatering av Debian, Docker och Certbot

Planera normalt underhåll:

```bash
sudo apt update
sudo apt full-upgrade
```

Docker uppgraderas via det officiella Docker apt-repositoryt.

Uppdatera Certbot enligt den installationsmetod som används gemensamt på servern. Kontrollera därefter att renewal fortfarande fungerar.

Efter större Docker/Nginx-uppdateringar, kontrollera:

```bash
sudo nginx -t
sudo certbot renew --dry-run
```

och kör vid behov en RepoFleet deployment av aktuell release igen.

---

# Slutlig checklista

- [ ] DNS A-post pekar på servern.
- [ ] Eventuell AAAA-post används bara om IPv6 fungerar.
- [ ] Debian 13 är uppdaterad och klockan synkad.
- [ ] Docker Engine och Compose-plugin är installerade.
- [ ] `/opt/repo-fleet` ägs av `repofleet-deploy`.
- [ ] Dedikerad deploy-SSH-nyckel fungerar.
- [ ] GitHub App är skapad med read-only Metadata/Contents/Actions.
- [ ] GitHub App är installerad på rätt repositories.
- [ ] GitHub App private key finns i `/opt/repo-fleet/secrets/github-app.pem`.
- [ ] `/opt/repo-fleet/.env` innehåller rätt App ID, Installation ID, auth Client ID/Secret, session secret, allowlist och vald frontend-port.
- [ ] Nginx svarar för `repo-fleet.isaksson.info`.
- [ ] Let's Encrypt-certifikatet är installerat.
- [ ] Certbots befintliga automatiska renewal-schemaläggning är verifierad och `renew --dry-run` fungerar.
- [ ] GitHub App Callback URL, Client ID och Client Secret är konfigurerade för RepoFleet-login.
- [ ] Serverbrandväggen tillåter 80/443 och nödvändig SSH-port.
- [ ] GitHub Environment `production` finns.
- [ ] Deploy-secrets och verifierad `DEPLOY_KNOWN_HOSTS` finns.
- [ ] GHCR packages kan läsas av repositoryts Actions-workflow.
- [ ] Minst en deploybar immutable version finns i GHCR (`MAJOR.MINOR.PATCH-rc.N` eller `MAJOR.MINOR.PATCH`).
- [ ] `Deploy production` har körts för vald version.
- [ ] Frontend och backend är `healthy`.
- [ ] HTTPS visar RepoFleets GitHub-login och en allowlistad GitHub-användare kan logga in.
- [ ] Efter GitHub-login visar repositoryinventeringen förväntad anslutning/data och refresh fungerar.
- [ ] Inventory refresh visar förväntade repositories.
