#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
STATE_DIR="${ROOT}/.dev-backend"
PACKAGE="${ROOT}/.dev-packages/GoreeCloud-Gallery-0.8.11-dev-vc2000883.apk"
CERT="${STATE_DIR}/tls-cert.pem"
KEY="${STATE_DIR}/tls-key.pem"
TOKEN_FILE="${STATE_DIR}/bearer-token"
ENV_FILE="${STATE_DIR}/runtime.env"
RELEASES_FILE="${STATE_DIR}/releases.json"

mkdir -p "${STATE_DIR}"
chmod 700 "${STATE_DIR}"

if [[ ! -f "${PACKAGE}" ]]; then
  echo "Missing Development artifact: ${PACKAGE}" >&2
  echo "Place the governed GoreeCloud Gallery 0.8.11-dev APK in .dev-packages first." >&2
  exit 2
fi

command -v openssl >/dev/null
command -v python3 >/dev/null

if [[ ! -s "${TOKEN_FILE}" ]]; then
  python3 - <<'PY' > "${TOKEN_FILE}"
import secrets
print(secrets.token_urlsafe(48))
PY
  chmod 600 "${TOKEN_FILE}"
fi

if [[ ! -s "${CERT}" || ! -s "${KEY}" ]]; then
  openssl req -x509 -newkey rsa:3072 -sha256 -nodes     -keyout "${KEY}"     -out "${CERT}"     -days 30     -subj "/CN=127.0.0.1/O=GoreeCloud/OU=App Store Development"     -addext "subjectAltName=IP:127.0.0.1,DNS:localhost" >/dev/null 2>&1
  chmod 600 "${KEY}"
fi

cp "${ROOT}/development/backend/releases.example.json" "${RELEASES_FILE}"
chmod 600 "${RELEASES_FILE}"

SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [[ -z "${SDK_ROOT}" ]]; then
  for candidate in "${HOME}/Android/Sdk" "${HOME}/GoreeCloud-work/.android-sdk"; do
    if [[ -d "${candidate}/build-tools" ]]; then
      SDK_ROOT="${candidate}"
      break
    fi
  done
fi
if [[ -z "${SDK_ROOT}" || ! -d "${SDK_ROOT}/build-tools" ]]; then
  echo "Android SDK build-tools were not found." >&2
  exit 2
fi

AAPT="$(find "${SDK_ROOT}/build-tools" -type f -name aapt -print | sort -V | tail -n 1)"
APKSIGNER="$(find "${SDK_ROOT}/build-tools" -type f -name apksigner -print | sort -V | tail -n 1)"
[[ -x "${AAPT}" && -x "${APKSIGNER}" ]]

WARDVEIL_REPO="${GOREECLOUD_WARDVEIL_REPO:-}"
if [[ -z "${WARDVEIL_REPO}" ]]; then
  for candidate in "${ROOT}/../wardveil" "${HOME}/GoreeCloud/Repositories/wardveil" "${HOME}/GoreeCloud-work/wardveil"; do
    if [[ -f "${candidate}/reference/wardveil_clamav.py" ]]; then
      WARDVEIL_REPO="${candidate}"
      break
    fi
  done
fi
if [[ -z "${WARDVEIL_REPO}" || ! -f "${WARDVEIL_REPO}/reference/wardveil_clamav.py" ]]; then
  echo "Set GOREECLOUD_WARDVEIL_REPO to a current GoreeCloud/wardveil checkout." >&2
  exit 2
fi

TLS_PIN="$(openssl x509 -in "${CERT}" -outform DER | sha256sum | awk '{print $1}')"
TOKEN="$(<"${TOKEN_FILE}")"

{
  printf 'export GOREECLOUD_APP_STORE_TOKEN=%q\n' "${TOKEN}"
  printf 'export GOREECLOUD_APP_STORE_HOST=%q\n' "127.0.0.1"
  printf 'export GOREECLOUD_APP_STORE_PORT=%q\n' "8443"
  printf 'export GOREECLOUD_APP_STORE_RELEASES=%q\n' "${RELEASES_FILE}"
  printf 'export GOREECLOUD_APP_STORE_TLS_CERT=%q\n' "${CERT}"
  printf 'export GOREECLOUD_APP_STORE_TLS_KEY=%q\n' "${KEY}"
  printf 'export GOREECLOUD_WARDVEIL_REPO=%q\n' "${WARDVEIL_REPO}"
  printf 'export GOREECLOUD_AAPT=%q\n' "${AAPT}"
  printf 'export GOREECLOUD_APKSIGNER=%q\n' "${APKSIGNER}"
  printf 'export WARDVEIL_CLAMAV_TCP_HOST=%q\n' "${WARDVEIL_CLAMAV_TCP_HOST:-127.0.0.1}"
  printf 'export WARDVEIL_CLAMAV_PORT=%q\n' "${WARDVEIL_CLAMAV_PORT:-3310}"
  printf 'export GOREECLOUD_DEV_DELIVERY_BASE_URL=%q\n' "https://127.0.0.1:8443"
  printf 'export GOREECLOUD_DEV_DELIVERY_TOKEN=%q\n' "${TOKEN}"
  printf 'export GOREECLOUD_DEV_DELIVERY_TLS_CERT_SHA256=%q\n' "${TLS_PIN}"
} > "${ENV_FILE}"
chmod 600 "${ENV_FILE}"

echo "Development package backend bootstrap complete."
echo "Runtime state: ${STATE_DIR}"
echo "TLS certificate SHA-256: ${TLS_PIN}"
echo "Production Acceptance: false"
