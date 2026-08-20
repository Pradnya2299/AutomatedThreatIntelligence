#!/usr/bin/env bash
# Load KEY=value lines from a .env file without executing comments or free-form notes.
# Lines that are not valid assignments (for example "NVD API KEY=...") are skipped.
load_dotenv() {
  local env_file="${1:-}"
  [[ -f "${env_file}" ]] || return 0
  local line key value
  while IFS= read -r line || [[ -n "${line}" ]]; do
    line="${line%$'\r'}"
    [[ -z "${line}" || "${line}" =~ ^[[:space:]]*# ]] && continue
    if [[ "${line}" =~ ^[A-Za-z_][A-Za-z0-9_]*= ]]; then
      key="${line%%=*}"
      value="${line#*=}"
      export "${key}=${value}"
    fi
  done < "${env_file}"
}
