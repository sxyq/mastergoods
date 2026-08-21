#!/usr/bin/env bash
set -euo pipefail

CODEX_CONFIG_PATH="${CODEX_CONFIG_PATH:-$HOME/.codex/config.toml}"
WIRE_API="${AGENT_LLM_WIRE_API_OVERRIDE:-chat_completions}"

if [[ ! -f "${CODEX_CONFIG_PATH}" ]]; then
  echo "Codex config not found: ${CODEX_CONFIG_PATH}" >&2
  exit 1
fi

provider_name="$(
  awk -F '"' '/^[[:space:]]*model_provider[[:space:]]*=/{print $2; exit}' "${CODEX_CONFIG_PATH}"
)"
if [[ -z "${provider_name}" ]]; then
  echo "Cannot resolve model_provider from ${CODEX_CONFIG_PATH}" >&2
  exit 1
fi

provider_block="$(
  awk -v provider="${provider_name}" '
    $0 == "[model_providers." provider "]" { in_block = 1; next }
    /^\[/ && in_block { exit }
    in_block { print }
  ' "${CODEX_CONFIG_PATH}"
)"
if [[ -z "${provider_block}" ]]; then
  echo "Cannot resolve [model_providers.${provider_name}] block from ${CODEX_CONFIG_PATH}" >&2
  exit 1
fi

extract_value() {
  local key="$1"
  printf '%s\n' "${provider_block}" | awk -F '"' -v key="${key}" '
    $0 ~ "^[[:space:]]*" key "[[:space:]]*=" { print $2; exit }
  '
}

extract_boolean() {
  local key="$1"
  printf '%s\n' "${provider_block}" | awk -v key="${key}" '
    $0 ~ "^[[:space:]]*" key "[[:space:]]*=" {
      value = $0
      sub(/^[^=]*=[[:space:]]*/, "", value)
      sub(/[[:space:]]*$/, "", value)
      print value
      exit
    }
  '
}

base_url="$(extract_value base_url)"
api_key="$(extract_value experimental_bearer_token)"
model="$(extract_value model)"
requires_openai_auth="$(extract_boolean requires_openai_auth)"

if [[ -z "${base_url}" || -z "${api_key}" || -z "${model}" ]]; then
  echo "Codex provider block is missing base_url, experimental_bearer_token, or model" >&2
  exit 1
fi

if [[ -z "${requires_openai_auth}" ]]; then
  requires_openai_auth="true"
fi

cat <<EOF
export AGENT_LLM_ENABLED=true
export AGENT_LLM_BASE_URL='${base_url}'
export AGENT_LLM_API_KEY='${api_key}'
export AGENT_LLM_MODEL='${model}'
export AGENT_LLM_WIRE_API='${WIRE_API}'
export AGENT_LLM_REQUIRES_OPENAI_AUTH='${requires_openai_auth}'
EOF
