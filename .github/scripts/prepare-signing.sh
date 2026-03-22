#!/usr/bin/env bash
set -euo pipefail

workspace_dir="${1:-$PWD}"
output_path="${2:-$workspace_dir/signing.properties}"
keystore_path="${SIGNING_KEYSTORE_PATH:-$workspace_dir/release.keystore}"
keytool_bin="${KEYTOOL_BIN:-keytool}"
signing_required="${SIGNING_REQUIRED:-false}"

set_output() {
  local key="$1"
  local value="$2"

  echo "${key}=${value}"
  if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
    echo "${key}=${value}" >>"$GITHUB_OUTPUT"
  fi
}

warn() {
  echo "warning: $1" >&2
}

cleanup_signing_properties() {
  rm -f "$output_path"
}

require_release_signing() {
  warn "Release signing is required but unavailable: $1"
  set_output "using_release_signing" "false"
  cleanup_signing_properties
  return 1
}

skip_release_signing() {
  warn "Skipping release signing, falling back to debug signing: $1"
  set_output "using_release_signing" "false"
  cleanup_signing_properties
}

validate_release_signing() {
  "$keytool_bin" \
    -list \
    -keystore "$keystore_path" \
    -storepass "$SIGNING_STORE_PASSWORD" \
    -alias "$SIGNING_KEY_ALIAS" \
    -keypass "$SIGNING_KEY_PASSWORD" \
    >/dev/null 2>&1
}

if [[ ! -f "$keystore_path" ]]; then
  if [[ "$signing_required" == "true" ]]; then
    require_release_signing "keystore not found at $keystore_path"
  else
    skip_release_signing "keystore not found at $keystore_path"
  fi
  exit $?
fi

missing_env_vars=()
for key in SIGNING_STORE_PASSWORD SIGNING_KEY_ALIAS SIGNING_KEY_PASSWORD; do
  if [[ -z "${!key:-}" ]]; then
    missing_env_vars+=("$key")
  fi
done

if (( ${#missing_env_vars[@]} > 0 )); then
  reason="missing required environment variables: ${missing_env_vars[*]}"
  if [[ "$signing_required" == "true" ]]; then
    require_release_signing "$reason"
  else
    skip_release_signing "$reason"
  fi
  exit $?
fi

if ! validate_release_signing; then
  reason="keystore credentials validation failed for $keystore_path"
  if [[ "$signing_required" == "true" ]]; then
    require_release_signing "$reason"
  else
    skip_release_signing "$reason"
  fi
  exit $?
fi

cat >"$output_path" <<EOF
keystore.password=$SIGNING_STORE_PASSWORD
key.alias=$SIGNING_KEY_ALIAS
key.password=$SIGNING_KEY_PASSWORD
EOF

set_output "using_release_signing" "true"
