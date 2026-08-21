#!/usr/bin/env bash
set -u

EVIDENCE_ROOT="${EVIDENCE_ROOT:?EVIDENCE_ROOT is required}"
BASE_URL="${BASE_URL:-http://127.0.0.1:28081}"
TOKEN_FILE="${TOKEN_FILE:?TOKEN_FILE is required}"
SAMPLE_COUNT="${SAMPLE_COUNT:-30}"

TOKEN="$(tr -d '\r\n' < "${TOKEN_FILE}")"
AUTH_HEADER="Authorization: Bearer ${TOKEN}"
CONTENT_TYPE='Content-Type: application/json'
STATUS_FILE="${EVIDENCE_ROOT}/deepseek-performance.tsv"

mkdir -p "${EVIDENCE_ROOT}"
: > "${STATUS_FILE}"

conversation_body='{"title":"DeepSeek performance fixture","status":"active"}'
conversation_json="${EVIDENCE_ROOT}/performance-conversation-create.json"
conversation_code="$(curl -sS --connect-timeout 10 --max-time 30 \
  -o "${conversation_json}" -w '%{http_code}' \
  -H "${AUTH_HEADER}" -H "${CONTENT_TYPE}" \
  -d "${conversation_body}" "${BASE_URL}/v2/agent/conversations" || true)"
conversation_id="$(sed -n 's/.*\"id\":\([0-9][0-9]*\).*/\1/p' "${conversation_json}" | head -1)"
printf 'conversation_create\t%s\t%s\n' "${conversation_code}" "${conversation_id}" >> "${STATUS_FILE}"

if [[ -z "${conversation_id}" ]]; then
  printf 'conversation_id_missing\n' > "${EVIDENCE_ROOT}/performance-error.txt"
  rm -f "${TOKEN_FILE}"
  exit 1
fi

printf 'index\thttp_status\tduration_ms\ttool_count\tllm_status\tanswer_present\trun_id\n' >> "${STATUS_FILE}"
for i in $(seq 1 "${SAMPLE_COUNT}"); do
  case $((i % 3)) in
    1) message='How many products are in the current account?' ;;
    2) message='How many low-stock products are in the current account?' ;;
    0) message='Summarize the current account product count and total stock.' ;;
  esac
  request_body="{\"conversation_id\":${conversation_id},\"message\":\"${message}\",\"stream\":false}"
  output="${EVIDENCE_ROOT}/performance-$(printf '%02d' "${i}").json"
  started="$(date +%s%3N)"
  code="$(curl -sS --connect-timeout 10 --max-time 90 \
    -o "${output}" -w '%{http_code}' \
    -H "${AUTH_HEADER}" -H "${CONTENT_TYPE}" \
    -d "${request_body}" "${BASE_URL}/v2/agent/chat" || true)"
  finished="$(date +%s%3N)"
  duration_ms=$((finished - started))
  run_id="$(sed -n 's/.*\"run_id\":\"\([^\"]*\)\".*/\1/p' "${output}" | head -1)"
  llm_status="$(sed -n 's/.*\"llm_status\":\"\([^\"]*\)\".*/\1/p' "${output}" | head -1)"
  tool_count="$(grep -o '"tool_name"' "${output}" 2>/dev/null | wc -l | tr -d ' ')"
  answer_present=false
  grep -q '"answer":"[^"].*"' "${output}" 2>/dev/null && answer_present=true
  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
    "${i}" "${code}" "${duration_ms}" "${tool_count}" \
    "${llm_status}" "${answer_present}" "${run_id}" >> "${STATUS_FILE}"
done

curl -sS --connect-timeout 10 --max-time 30 \
  -o "${EVIDENCE_ROOT}/performance-conversation-delete.json" \
  -w '%{http_code}\n' -H "${AUTH_HEADER}" \
  -X DELETE "${BASE_URL}/v2/agent/conversations/${conversation_id}" \
  > "${EVIDENCE_ROOT}/performance-conversation-delete.status" || true

{
  printf 'captured_at=%s\n' "$(date -u +%FT%TZ)"
  printf 'sample_count=%s\n' "${SAMPLE_COUNT}"
  printf 'model=deepseek-v4-flash\n'
  printf 'base_url=https://tokenrhythm.studio/v1\n'
  printf 'wire_api=chat_completions\n'
  printf 'conversation_id=%s\n' "${conversation_id}"
  printf 'conversation_create_http=%s\n' "${conversation_code}"
  printf 'conversation_delete_http='
  cat "${EVIDENCE_ROOT}/performance-conversation-delete.status"
} > "${EVIDENCE_ROOT}/deepseek-performance-summary.txt"

rm -f "${TOKEN_FILE}"
