#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# This script has been removed.
#
# The infrastructure now starts with a single command:
#
#   cd infra && docker compose up -d
#
# TLS certificates are generated automatically by the `certs` service
# defined in docker-compose.yml (idempotent — skips if already present).
#
# To stop:   docker compose down
# To reset:  docker compose down -v && docker compose up -d
# ─────────────────────────────────────────────────────────────────────────────
echo "ℹ️  start.sh has been removed. Use: docker compose up -d"
exit 0

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INFRA_DIR="$SCRIPT_DIR/.."

cd "$INFRA_DIR"

# ── Argument handling ─────────────────────────────────────────────────────────
ACTION="${1:-start}"

if [[ "$ACTION" == "--stop" ]]; then
  echo "🛑  Stopping infrastructure..."
  docker compose down
  echo "✅  Done."
  exit 0
fi

if [[ "$ACTION" == "--reset" ]]; then
  echo "⚠️   Resetting infrastructure (all data will be lost)..."
  read -r -p "    Are you sure? [y/N] " confirm
  [[ "$confirm" =~ ^[Yy]$ ]] || { echo "Aborted."; exit 1; }
  docker compose down -v --remove-orphans
  echo "✅  Volumes removed."
fi

# ── TLS certificates ──────────────────────────────────────────────────────────
if [[ ! -f nginx/certs/localhost.crt ]]; then
  echo "🔐  Self-signed certificate not found — generating..."
  bash scripts/gen-certs.sh
else
  echo "🔐  TLS certificate already present — skipping generation."
fi

# ── Start services ────────────────────────────────────────────────────────────
echo ""
echo "🚀  Starting infrastructure services..."
docker compose up -d

# ── Wait for Keycloak to be ready ─────────────────────────────────────────────
echo ""
echo "⏳  Waiting for Keycloak to be ready (may take ~30s on first start)..."
max_attempts=36
attempt=0
until curl -sf http://localhost:8180/realms/master > /dev/null 2>&1; do
  attempt=$((attempt + 1))
  if [[ $attempt -ge $max_attempts ]]; then
    echo "⚠️   Keycloak did not become ready in time. Check logs: docker compose logs keycloak"
    break
  fi
  printf '.'
  sleep 5
done
echo ""

# ── Summary ───────────────────────────────────────────────────────────────────
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  🛠️   Local Infrastructure — Running"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "  Service          HTTP                    HTTPS"
echo "  ─────────────────────────────────────────────────────────"
echo "  PostgreSQL 17    localhost:5432          —"
echo "  Keycloak         http://localhost:8180   https://localhost:8443"
echo "  WireMock         http://localhost:9090   https://localhost:9091"
echo "  Mailpit SMTP     localhost:1025          —"
echo "  Mailpit Web      http://localhost:8025   https://localhost:8026"
echo "  Hub (index)      http://localhost:80     —"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Keycloak admin → http://localhost:8180/admin  (admin / admin)"
echo "  Realm          → nomecliente"
echo ""
echo "  Test users:"
echo "    developer  / dev123     → roles: user, admin"
echo "    editor     / editor123  → roles: user, editor"
echo "    testuser   / test123    → roles: user"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "  Spring Boot env:"
echo "    SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/template_db"
echo "    SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=http://localhost:8180/realms/nomecliente"
echo ""
echo "  Get a JWT token:"
echo '    curl -s -X POST http://localhost:8180/realms/nomecliente/protocol/openid-connect/token \'
echo '      -d "grant_type=password&client_id=nomecliente-client" \'
echo '      -d "username=developer&password=dev123" | jq .access_token'
echo ""

