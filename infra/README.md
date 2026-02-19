# Local Development Infrastructure

Full-stack local environment for **nomecliente** backend development.

---

## Services

| Service | Image | HTTP | HTTPS |
|---|---|---|---|
| PostgreSQL 17 | `postgres:17` | `localhost:5432` | — |
| Keycloak | `keycloak:26.x` | `http://localhost:8180` | `https://localhost:8443` |
| WireMock | `wiremock/wiremock` | `http://localhost:9090` | `https://localhost:9091` |
| Mailpit SMTP | `axllent/mailpit` | `localhost:1025` | — |
| Mailpit Web UI | `axllent/mailpit` | `http://localhost:8025` | `https://localhost:8026` |
| nginx hub | `nginx:alpine` | `http://localhost:80` | — |

> **Note on Keycloak version**: the default in `.env` is `26.1.4` (last verified stable).
> Set `KEYCLOAK_VERSION=26.5.1` (or any tag from [quay.io/keycloak/keycloak](https://quay.io/repository/keycloak/keycloak)) to override.

---

## Quick start

```bash
# Start everything (first time and every time)
cd infra
docker compose up -d
```

On first run Docker Compose:
1. Pulls all images
2. Creates the `postgres_data` and `certs_data` named volumes
3. Runs the `certs` service: generates a self-signed TLS certificate (RSA 4096, 10 years) into the `certs_data` volume — **idempotent**, skips if already present
4. Initialises PostgreSQL with `template_db` and `keycloak_db`
5. Starts Keycloak and imports the `nomecliente` realm with test users
6. Starts nginx (waits for `certs` to complete), WireMock, Mailpit

```bash
# Stop
docker compose down

# Full reset — deletes all DB data and certs, starts completely fresh
docker compose down -v && docker compose up -d
```

---

## Folder structure

```
infra/
├── docker-compose.yml
├── .env                        ← defaults (safe to commit)
├── .gitignore
├── scripts/
│   ├── openssl.cnf             ← SAN config mounted read-only into the `certs` service
│   └── gen-certs.sh            ← optional: export cert from Docker for OS/browser trust
├── nginx/
│   ├── nginx.conf              ← global SSL settings (TLS 1.2/1.3)
│   ├── html/
│   │   └── index.html          ← service hub at http://localhost:80
│   ├── certs/                  ← gitignored — only populated by gen-certs.sh (optional)
│   └── conf.d/
│       ├── 00-hub.conf         ← port 80 static hub
│       ├── keycloak.conf       ← port 8443 → keycloak:8180
│       ├── wiremock.conf       ← port 9091 → wiremock:8080
│       └── mailpit.conf        ← port 8026 → mailpit:8025
├── postgres/
│   └── init/
│       └── 01-init-databases.sql  ← creates template_db + keycloak_db
├── keycloak/
│   └── realm-import.json       ← realm "nomecliente" with test users
└── wiremock/
    ├── mappings/               ← one JSON file per stubbed endpoint
    │   └── example-external-api.json
    └── __files/                ← static response body files
        └── example-response.json
```

---

## Trust the self-signed certificate (optional)

The certificate lives inside the `certs_data` Docker volume and is used automatically by nginx.
To avoid browser TLS warnings, use the provided script to export and trust it:

```bash
# Export cert from Docker volume + print OS trust instructions
./scripts/gen-certs.sh

# Export + automatically add to OS trust store (requires sudo)
./scripts/gen-certs.sh --trust
```

Manual commands if preferred:

```bash
# macOS
sudo security add-trusted-cert -d -r trustRoot \
  -k /Library/Keychains/System.keychain \
  nginx/certs/localhost.crt

# Linux (Debian/Ubuntu)
sudo cp nginx/certs/localhost.crt /usr/local/share/ca-certificates/nomecliente-dev.crt
sudo update-ca-certificates
```

---

## PostgreSQL

Two databases are created automatically on first start:

| Database | User | Password | Used by |
|---|---|---|---|
| `template_db` | `template_user` | `template_pass` | Spring Boot app |
| `keycloak_db` | `keycloak_user` | `keycloak_pass` | Keycloak |

The root superuser is `postgres / postgres` (set via `POSTGRES_ROOT_PASSWORD` in `.env`).

Spring Boot `application.yml` already points to `template_db` by default — no extra config needed.

---

## Keycloak

### Admin console

- HTTP:  `http://localhost:8180/admin` → `admin / admin`
- HTTPS: `https://localhost:8443/admin` → `admin / admin`

### Realm: `nomecliente`

Imported automatically on first start from `keycloak/realm-import.json`.

#### Test users

| Username | Password | Roles |
|---|---|---|
| `developer` | `dev123` | `user`, `admin` |
| `editor` | `editor123` | `user`, `editor` |
| `testuser` | `test123` | `user` |

#### Client: `nomecliente-client`

- Public client (no secret required)
- `directAccessGrantsEnabled: true` — supports Resource Owner Password flow for dev/test

#### Get a JWT token (curl)

```bash
curl -s -X POST \
  http://localhost:8180/realms/nomecliente/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=nomecliente-client" \
  -d "username=developer" \
  -d "password=dev123" \
  | jq .access_token
```

#### Spring Boot configuration (already set in `application.yml`)

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8180/realms/nomecliente
          jwk-set-uri: http://localhost:8180/realms/nomecliente/protocol/openid-connect/certs
```

> The issuer is `http://localhost:8180` (HTTP) even when you access Keycloak via HTTPS on 8443.
> Spring Boot connects **directly** to port 8180 for JWK validation — no TLS trust issues.

---

## WireMock

- Admin UI: `http://localhost:9090/__admin/ui`
- Base URL for mocked calls: `http://localhost:9090` (or `https://localhost:9091`)

### Add a mock

Create a JSON file in `wiremock/mappings/`:

```json
{
  "request": {
    "method": "POST",
    "url": "/external-service/v1/notify"
  },
  "response": {
    "status": 200,
    "body": "{\"status\": \"sent\"}",
    "headers": { "Content-Type": "application/json" }
  }
}
```

WireMock picks up new mapping files without restart (hot-reload via `__admin/mappings/reset`).

### Reference in `application.yml`

```yaml
external-service:
  base-url: ${EXTERNAL_SERVICE_BASE_URL:http://localhost:9090}
```

---

## Mailpit

All emails sent via SMTP to `localhost:1025` are captured and visible in the web UI.
**No email is actually delivered.**

- Web UI: `http://localhost:8025` (or `https://localhost:8026`)
- SMTP host: `localhost`, port: `1025`

### Spring Boot configuration

```yaml
spring:
  mail:
    host: ${MAIL_HOST:localhost}
    port: ${MAIL_PORT:1025}
    properties:
      mail.smtp.auth: false
      mail.smtp.starttls.enable: false
```

---

## Environment overrides

All variables are defined in `.env`. To override locally without committing:

```bash
cp .env .env.local
# edit .env.local
```

`.env.local` is gitignored. Docker Compose loads `.env` automatically; `.env.local` must be
passed explicitly if needed: `docker compose --env-file .env.local up -d`.

