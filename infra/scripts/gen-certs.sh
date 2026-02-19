#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# gen-certs.sh — Export the TLS certificate from the Docker volume and
#                (optionally) add it to the OS trust store.
#
# The certificate is generated AUTOMATICALLY by the `certs` service when
# running `docker compose up -d`.
# This script is only needed if you want to avoid browser TLS warnings
# by trusting the self-signed certificate at the OS level.
#
# Usage:
#   ./scripts/gen-certs.sh            # export cert to nginx/certs/ + print instructions
#   ./scripts/gen-certs.sh --trust    # export + add to OS trust store (requires sudo)
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CERT_DIR="$SCRIPT_DIR/../nginx/certs"
TRUST="${1:-}"

mkdir -p "$CERT_DIR"

# ── Extract cert from the running nginx container (mounts certs_data volume) ──
echo "📦  Extracting certificate from Docker volume..."

if ! docker ps --format '{{.Names}}' | grep -q '^infra_nginx$'; then
  echo "❌  infra_nginx is not running. Start the stack first:"
  echo "    cd infra && docker compose up -d"
  exit 1
fi

docker cp infra_nginx:/etc/nginx/certs/localhost.crt "$CERT_DIR/localhost.crt"
docker cp infra_nginx:/etc/nginx/certs/localhost.key "$CERT_DIR/localhost.key"
chmod 600 "$CERT_DIR/localhost.key"

echo "✅  Certificate exported to:"
echo "    $CERT_DIR/localhost.crt"
echo ""

# ── Trust in OS trust store ──────────────────────────────────────────────────
if [[ "$TRUST" == "--trust" ]]; then
  OS="$(uname -s)"
  echo "🔐  Adding certificate to OS trust store..."
  case "$OS" in
    Darwin)
      sudo security add-trusted-cert -d -r trustRoot \
        -k /Library/Keychains/System.keychain \
        "$CERT_DIR/localhost.crt"
      echo "✅  Trusted on macOS. Restart your browser."
      ;;
    Linux)
      sudo cp "$CERT_DIR/localhost.crt" /usr/local/share/ca-certificates/nomecliente-dev.crt
      sudo update-ca-certificates
      echo "✅  Trusted on Linux. Restart your browser."
      ;;
    *)
      echo "⚠️   Unsupported OS: $OS — manually import $CERT_DIR/localhost.crt."
      ;;
  esac
else
  echo "ℹ️   To trust the certificate and remove browser warnings, run:"
  echo "    ./scripts/gen-certs.sh --trust"
fi

