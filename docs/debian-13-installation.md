# RepoFleet – komplett installation på Debian 13

Den här manualen beskriver en ny produktionsinstallation av RepoFleet på **Debian 13 (Trixie)** med:

- installation under `/opt/repo-fleet`,
- Docker Engine + Docker Compose,
- GitHub App för read-only åtkomst till repositories,
- GitHub Container Registry (GHCR) för versionerade images,
- Nginx som reverse proxy på servern,
- HTTPS för `repo-fleet.isaksson.info` med Let's Encrypt/Certbot,
- Nginx Basic Auth framför tjänsten,
- en dedikerad SSH-användare för GitHub Actions-deploy,
- manuellt startad GitHub Action för deployment av en vald release.

> **Viktigt om åtkomstskydd:** RepoFleet Phase 1 har ingen egen användarinloggning. Eftersom tjänsten kan visa namn och metadata från privata repositories bör den inte exponeras anonymt på Internet. Den här manualen använder därför Nginx Basic Auth framför hela tjänsten.

---

## 1. Förutsättningar

Du behöver:

- en Debian 13-server med publik IPv4-adress,
- root/sudo-access till servern,
- DNS-kontroll över `isaksson.info`,
- GitHub-repositoryt `erland/repo-fleet`,
- rätt att skapa en GitHub App,
- rätt att administrera GitHub Actions Environments/secrets i repositoryt,
- minst en officiell RepoFleet-release, t.ex. `v1.0.0`, när första deploymenten ska göras.

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
  apache2-utils \
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
```

4. RepoFleet Phase 1 använder inga webhooks. Stäng därför av webhook-funktionen/`Active` om GitHub-formuläret tillåter det.
5. Under **Repository permissions**, använd read-only:

```text
Metadata: Read-only
Contents: Read-only
Actions: Read-only
```

6. Ge inga write-permissions i Phase 1.
7. Välj om appen bara får installeras på ditt konto eller enligt den installationsmodell du önskar.
8. Skapa appen.

Notera **App ID** från appens inställningssida.

GitHub-installationens repository-listning kan användas med ett installation access token, och den övergripande read-only-modellen håller GitHub som source of truth.

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

Skapa `/opt/repo-fleet/.env`:

```bash
sudo -u repofleet-deploy tee /opt/repo-fleet/.env >/dev/null <<'EOF_ENV'
COMPOSE_PROJECT_NAME=repo-fleet
REPOFLEET_GITHUB_APP_ID=<GITHUB_APP_ID>
REPOFLEET_GITHUB_INSTALLATION_ID=<GITHUB_INSTALLATION_ID>
REPOFLEET_GITHUB_TOKEN_REFRESH_MARGIN_SECONDS=300
GITHUB_API_URL=https://api.github.com
JAVA_OPTS=-XX:MaxRAMPercentage=75.0
REPOFLEET_FRONTEND_PORT=8082
EOF_ENV

sudo chmod 0600 /opt/repo-fleet/.env
```

Sätt in riktiga värden för App ID och Installation ID.

Image-referenser läggs **inte** permanent i `.env`; deploy-scriptet skriver en separat `.images.env` med exakt release-version.

`REPOFLEET_FRONTEND_PORT=8082` är host-porten som Nginx använder. Containerporten är fortfarande `8080`. Porten är konfigurerbar för att RepoFleet ska kunna samexistera med andra tjänster på samma server.

---

# Del E – Nginx och HTTPS

## 12. Förbered HTTP för Let's Encrypt

Skapa webroot för ACME challenge:

```bash
sudo install -d -m 0755 /var/www/letsencrypt
```

Skapa bootstrap-vhost:

```bash
sudo tee /etc/nginx/sites-available/repo-fleet.isaksson.info >/dev/null <<'EOF_NGINX'
server {
    listen 80;
    listen [::]:80;
    server_name repo-fleet.isaksson.info;

    location ^~ /.well-known/acme-challenge/ {
        root /var/www/letsencrypt;
        default_type text/plain;
        auth_basic off;
    }

    location / {
        default_type text/plain;
        return 200 "RepoFleet HTTPS bootstrap\n";
    }
}
EOF_NGINX
```

Aktivera siten:

```bash
sudo ln -sfn \
  /etc/nginx/sites-available/repo-fleet.isaksson.info \
  /etc/nginx/sites-enabled/repo-fleet.isaksson.info

sudo nginx -t
sudo systemctl reload nginx
```

Verifiera utifrån:

```bash
curl -v http://repo-fleet.isaksson.info/
```

Du ska få `RepoFleet HTTPS bootstrap`.

Om det inte fungerar: kontrollera DNS, serverns/provider-brandvägg och att inkommande **TCP 80** är tillåten.

---

## 13. Installera Certbot

Installera Certbot i den rekommenderade separata Python-miljön:

```bash
sudo python3 -m venv /opt/certbot
sudo /opt/certbot/bin/pip install --upgrade pip
sudo /opt/certbot/bin/pip install certbot certbot-nginx
sudo ln -sfn /opt/certbot/bin/certbot /usr/local/bin/certbot
```

---

## 14. Beställ certifikat

Byt `<EMAIL_ADDRESS>` till din e-postadress för Let's Encrypt-meddelanden:

```bash
sudo certbot certonly \
  --webroot \
  --webroot-path /var/www/letsencrypt \
  --domain repo-fleet.isaksson.info \
  --email <EMAIL_ADDRESS> \
  --agree-tos \
  --no-eff-email
```

Certifikatet ska nu finnas under:

```text
/etc/letsencrypt/live/repo-fleet.isaksson.info/
```

---

## 15. Skapa Basic Auth-användare

Exempel med användarnamnet `erland`:

```bash
sudo htpasswd -c /etc/nginx/.htpasswd-repo-fleet <REPOFLEET_USER>
```

Ange ett starkt, unikt lösenord.

För ytterligare användare används `htpasswd` utan `-c`:

```bash
sudo htpasswd /etc/nginx/.htpasswd-repo-fleet <ANVÄNDARE>
```

---

## 16. Aktivera slutlig HTTPS reverse proxy

Ersätt Nginx-konfigurationen:

```bash
sudo tee /etc/nginx/sites-available/repo-fleet.isaksson.info >/dev/null <<'EOF_NGINX'
server {
    listen 80;
    listen [::]:80;
    server_name repo-fleet.isaksson.info;

    location ^~ /.well-known/acme-challenge/ {
        root /var/www/letsencrypt;
        default_type text/plain;
        auth_basic off;
    }

    location / {
        return 301 https://$host$request_uri;
    }
}

server {
    listen 443 ssl;
    listen [::]:443 ssl;
    server_name repo-fleet.isaksson.info;

    ssl_certificate /etc/letsencrypt/live/repo-fleet.isaksson.info/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/repo-fleet.isaksson.info/privkey.pem;

    auth_basic "RepoFleet";
    auth_basic_user_file /etc/nginx/.htpasswd-repo-fleet;

    add_header Strict-Transport-Security "max-age=31536000" always;
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

sudo nginx -t
sudo systemctl reload nginx
```

Innan första application deployment kommer HTTPS-sidan normalt svara `502 Bad Gateway` efter Basic Auth. Det är förväntat tills frontend-containern kör på `127.0.0.1:8082`.

---

## 17. Automatisk certifikatförnyelse

Skapa reload-hook:

```bash
sudo install -d -m 0755 /etc/letsencrypt/renewal-hooks/deploy
sudo tee /etc/letsencrypt/renewal-hooks/deploy/reload-nginx >/dev/null <<'EOF_HOOK'
#!/bin/sh
systemctl reload nginx
EOF_HOOK
sudo chmod 0755 /etc/letsencrypt/renewal-hooks/deploy/reload-nginx
```

Skapa systemd service:

```bash
sudo tee /etc/systemd/system/certbot-renew.service >/dev/null <<'EOF_SERVICE'
[Unit]
Description=Renew Let's Encrypt certificates with Certbot

[Service]
Type=oneshot
ExecStart=/usr/local/bin/certbot renew -q
EOF_SERVICE
```

Skapa timer som provar två gånger per dygn med slumpmässig fördröjning:

```bash
sudo tee /etc/systemd/system/certbot-renew.timer >/dev/null <<'EOF_TIMER'
[Unit]
Description=Twice-daily Certbot renewal check

[Timer]
OnCalendar=*-*-* 00,12:00:00
RandomizedDelaySec=3600
Persistent=true

[Install]
WantedBy=timers.target
EOF_TIMER
```

Aktivera:

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now certbot-renew.timer
systemctl list-timers certbot-renew.timer
```

Testa renewal:

```bash
sudo certbot renew --dry-run
```

Uppdatera Certbot-miljön regelbundet, exempelvis månadsvis i samband med serverunderhåll:

```bash
sudo /opt/certbot/bin/pip install --upgrade certbot certbot-nginx
```

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
127.0.0.1:8082
```

Backend publiceras inte till hostens nätverk alls. Docker-portarna behöver därför inte öppnas externt.

Efter deployment kan du verifiera:

```bash
sudo ss -ltnp | grep -E ':(80|443|8082)\b'
```

Port `8082` ska visas på loopback (`127.0.0.1`), inte på `0.0.0.0`.

---

# Del G – GitHub Release/GHCR

## 19. Skapa minst en officiell release

RepoFleet använder release-taggar:

```text
vMAJOR.MINOR.PATCH
```

Exempel:

```bash
git tag v1.0.0
git push origin v1.0.0
```

Release-workflowet ska slutföras och skapa:

```text
ghcr.io/erland/repo-fleet-frontend:1.0.0
ghcr.io/erland/repo-fleet-backend:1.0.0
```

samt GitHub Release `v1.0.0`.

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
4. Ange en existerande officiell release, exempelvis:

```text
v1.0.0
```

5. Starta workflowet.

Workflowet gör följande:

1. validerar att input följer `vMAJOR.MINOR.PATCH`,
2. verifierar att GitHub Release faktiskt finns,
3. kopierar `docker-compose.server.yml` och `deploy.sh` till `/opt/repo-fleet`,
4. loggar in servern temporärt mot GHCR med workflowets kortlivade `GITHUB_TOKEN`,
5. pullar exakt vald frontend/backend-version,
6. startar Compose med health checks,
7. försöker återgå till föregående image-version om ny version inte blir healthy,
8. verifierar att publika HTTPS-endpointen svarar med giltig TLS,
9. loggar ut servern från GHCR.

Servern behöver därmed ingen permanent GitHub Packages-token.

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

Exempel:

```text
REPOFLEET_FRONTEND_IMAGE=ghcr.io/erland/repo-fleet-frontend:1.0.0
REPOFLEET_BACKEND_IMAGE=ghcr.io/erland/repo-fleet-backend:1.0.0
```

Kontrollera frontend lokalt:

```bash
curl -I http://127.0.0.1:8082/
```

Kontrollera HTTPS:

```bash
curl -I https://repo-fleet.isaksson.info/
```

Utan Basic Auth credentials ska HTTPS-endpointen normalt svara:

```text
HTTP/1.1 401 Unauthorized
```

Det visar att TLS och Basic Auth fungerar.

Testa sedan i webbläsare:

```text
https://repo-fleet.isaksson.info
```

Logga in med Basic Auth-användaren från steg 15.

---

## 25. Verifiera GitHub-anslutningen i RepoFleet

Efter Basic Auth-inloggning kan du kontrollera via UI eller endpoint:

```text
https://repo-fleet.isaksson.info/api/github/connection
```

Förväntat är:

```text
CONNECTED
```

Starta därefter en inventory refresh i UI:t och verifiera att förväntade repositories dyker upp.

Om status är `NOT_CONFIGURED`, kontrollera `/opt/repo-fleet/.env`.

Om status är `ERROR`, kontrollera:

- App ID,
- Installation ID,
- private key-filen,
- GitHub App-installationen,
- read-only permissions,
- serverns klocka,
- containerloggar.

Containerloggar:

```bash
sudo -u repofleet-deploy -H bash -lc '
  cd /opt/repo-fleet
  docker compose \
    --env-file .env \
    --env-file .images.env \
    -f docker-compose.server.yml \
    logs --tail=200 backend
'
```

---

# Del J – framtida deployment och rollback

## 26. Deploya en ny version

När en ny release, exempelvis `v1.1.0`, har skapats och release-workflowet är grönt:

1. **Actions → Deploy production**
2. **Run workflow**
3. `release_tag`:

```text
v1.1.0
```

4. Kör.

Ingen serverinloggning eller manuell ändring av `.env` behövs för normal versionsuppgradering.

---

## 27. Rollback

En deployment som inte blir healthy försöker automatiskt starta föregående image-konfiguration igen.

För en explicit rollback kör du bara deployment-workflowet manuellt med föregående officiella release, exempelvis:

```text
v1.0.0
```

Det är en av anledningarna till att deployment alltid använder immutable release-taggar istället för enbart `latest`.

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
systemctl status certbot-renew.timer
```

---

## 29. Backup

Phase 1 har ingen databas. Det viktigaste server-specifika innehållet att säkerhetskopiera är därför:

```text
/opt/repo-fleet/.env
/opt/repo-fleet/secrets/github-app.pem
/etc/nginx/sites-available/repo-fleet.isaksson.info
/etc/nginx/.htpasswd-repo-fleet
/etc/letsencrypt/
```

Saved views ligger i respektive webbläsares `localStorage` och ingår inte i serverbackup.

Deployment-filerna och images kan återskapas från GitHub/GHCR.

---

## 30. Uppdatering av Debian/Docker/Certbot

Planera normalt underhåll:

```bash
sudo apt update
sudo apt full-upgrade
```

Docker uppgraderas via det officiella Docker apt-repositoryt.

Certbot-miljön uppdateras separat:

```bash
sudo /opt/certbot/bin/pip install --upgrade certbot certbot-nginx
```

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
- [ ] `/opt/repo-fleet/.env` innehåller rätt App ID och Installation ID.
- [ ] Nginx svarar för `repo-fleet.isaksson.info`.
- [ ] Let's Encrypt-certifikatet är installerat.
- [ ] Certbot renewal timer och dry-run fungerar.
- [ ] Basic Auth är aktiverad.
- [ ] Serverbrandväggen tillåter 80/443 och nödvändig SSH-port.
- [ ] GitHub Environment `production` finns.
- [ ] Deploy-secrets och verifierad `DEPLOY_KNOWN_HOSTS` finns.
- [ ] GHCR packages kan läsas av repositoryts Actions-workflow.
- [ ] En officiell `vMAJOR.MINOR.PATCH` release finns.
- [ ] `Deploy production` har körts för vald release.
- [ ] Frontend och backend är `healthy`.
- [ ] HTTPS svarar och kräver Basic Auth.
- [ ] `/api/github/connection` visar `CONNECTED`.
- [ ] Inventory refresh visar förväntade repositories.
