#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
script_path="$repo_root/.github/scripts/prepare-signing.sh"

assert_contains() {
  local haystack="$1"
  local needle="$2"

  if [[ "$haystack" != *"$needle"* ]]; then
    echo "expected output to contain: $needle" >&2
    echo "$haystack" >&2
    return 1
  fi
}

assert_file_contains() {
  local path="$1"
  local needle="$2"

  if ! grep -Fq "$needle" "$path"; then
    echo "expected $path to contain: $needle" >&2
    cat "$path" >&2
    return 1
  fi
}

run_fallback_case() {
  local temp_dir
  temp_dir="$(mktemp -d)"
  trap 'rm -rf "$temp_dir"' RETURN

  touch "$temp_dir/release.keystore"
  cat >"$temp_dir/fake-keytool" <<'EOF'
#!/usr/bin/env bash
exit 1
EOF
  chmod +x "$temp_dir/fake-keytool"

  local output
  output="$(
    SIGNING_STORE_PASSWORD=wrong-password \
    SIGNING_KEY_ALIAS=release \
    SIGNING_KEY_PASSWORD=wrong-password \
    KEYTOOL_BIN="$temp_dir/fake-keytool" \
    bash "$script_path" "$temp_dir" "$temp_dir/signing.properties" 2>&1
  )"

  assert_contains "$output" "using_release_signing=false"
  assert_contains "$output" "Skipping release signing"

  if [[ -f "$temp_dir/signing.properties" ]]; then
    echo "signing.properties should not be created when validation fails" >&2
    return 1
  fi
}

run_release_signing_case() {
  local temp_dir
  temp_dir="$(mktemp -d)"
  trap 'rm -rf "$temp_dir"' RETURN

  touch "$temp_dir/release.keystore"
  cat >"$temp_dir/fake-keytool" <<'EOF'
#!/usr/bin/env bash
exit 0
EOF
  chmod +x "$temp_dir/fake-keytool"

  local output
  output="$(
    SIGNING_STORE_PASSWORD=correct-store \
    SIGNING_KEY_ALIAS=release \
    SIGNING_KEY_PASSWORD=correct-key \
    KEYTOOL_BIN="$temp_dir/fake-keytool" \
    bash "$script_path" "$temp_dir" "$temp_dir/signing.properties" 2>&1
  )"

  assert_contains "$output" "using_release_signing=true"

  if [[ ! -f "$temp_dir/signing.properties" ]]; then
    echo "signing.properties should be created when validation succeeds" >&2
    return 1
  fi

  assert_file_contains "$temp_dir/signing.properties" "keystore.password=correct-store"
  assert_file_contains "$temp_dir/signing.properties" "key.alias=release"
  assert_file_contains "$temp_dir/signing.properties" "key.password=correct-key"
}

run_fallback_case
run_release_signing_case
