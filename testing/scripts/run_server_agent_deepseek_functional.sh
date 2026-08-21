#!/usr/bin/env bash
set -u

EVIDENCE_ROOT="${EVIDENCE_ROOT:?EVIDENCE_ROOT is required}"
BASE_URL="${BASE_URL:-http://127.0.0.1:28081}"
TOKEN_FILE="${TOKEN_FILE:?TOKEN_FILE is required}"

TOKEN="$(tr -d '\r\n' < "${TOKEN_FILE}")"
AUTH_HEADER="Authorization: Bearer ${TOKEN}"
CONTENT_TYPE='Content-Type: application/json'
STATUS_FILE="${EVIDENCE_ROOT}/deepseek-functional-status.tsv"

status() {
  printf '%s\t%s\t%s\n' "$1" "$2" "$3" >> "${STATUS_FILE}"
}

request() {
  local name="$1"
  local method="$2"
  local path="$3"
  local body="$4"
  local output="${EVIDENCE_ROOT}/${name}.json"
  local code
  code="$(curl -sS --connect-timeout 10 --max-time 90 \
    -o "${output}" -w '%{http_code}' \
    -X "${method}" \
    -H "${AUTH_HEADER}" -H "${CONTENT_TYPE}" \
    -d "${body}" "${BASE_URL}${path}" || true)"
  status "${name}" "${code}" "${path}"
}

mkdir -p "${EVIDENCE_ROOT}"
: > "${STATUS_FILE}"

request deepseek_multiturn POST /v2/agent/chat \
  '{"message":"How many products are in the current account? Include total stock and low-stock count, using only current account data.","stream":false}'
request deepseek_chart POST /v2/agent/chat \
  '{"message":"Show the sales trend for the current account for the last 30 days. Return a chart only if real sales data exists; otherwise state that no usable data exists.","stream":false}'

request draft_cancel_create POST /v2/agent/drafts \
  '{"draft_type":"media_upload","title":"DeepSeek cancel fixture","content_json":"{}","status":"active"}'
cancel_id="$(sed -n 's/.*\"id\":\([0-9][0-9]*\).*/\1/p' "${EVIDENCE_ROOT}/draft_cancel_create.json" | head -1)"
if [[ -n "${cancel_id}" ]]; then
  request draft_cancel POST "/v2/agent/drafts/${cancel_id}/cancel" '{}'
  request draft_cancel_delete DELETE "/v2/agent/drafts/${cancel_id}" '{}'
fi

request draft_confirm_create POST /v2/agent/drafts \
  '{"draft_type":"media_upload","title":"DeepSeek confirm fixture","content_json":"{}","status":"active"}'
confirm_id="$(sed -n 's/.*\"id\":\([0-9][0-9]*\).*/\1/p' "${EVIDENCE_ROOT}/draft_confirm_create.json" | head -1)"
if [[ -n "${confirm_id}" ]]; then
  request draft_confirm POST "/v2/agent/drafts/${confirm_id}/confirm" '{}'
  request draft_confirm_delete DELETE "/v2/agent/drafts/${confirm_id}" '{}'
fi

stream_file="${EVIDENCE_ROOT}/cancel.sse"
curl -sS --no-buffer --connect-timeout 10 --max-time 90 \
  -o "${stream_file}" -w '%{http_code}' \
  -H "${AUTH_HEADER}" -H "${CONTENT_TYPE}" \
  -d '{"message":"List every product in the current account and explain each field.","stream":true}' \
  "${BASE_URL}/v2/agent/chat/stream" \
  > "${EVIDENCE_ROOT}/cancel.http-status" \
  2> "${EVIDENCE_ROOT}/cancel.curl.log" &
stream_pid=$!
run_id=''
for _ in $(seq 1 100); do
  if [[ -s "${stream_file}" ]]; then
    run_id="$(sed -n 's/.*\"event_type\":\"run_started\"[^}]*\"run_id\":\"\([^\"]*\)\".*/\1/p' "${stream_file}" | head -1)"
    if [[ -z "${run_id}" ]]; then
      run_id="$(sed -n 's/.*\"run_id\":\"\([^\"]*\)\".*/\1/p' "${stream_file}" | head -1)"
    fi
    [[ -n "${run_id}" ]] && break
  fi
  sleep 0.1
done
printf '%s\n' "${run_id}" > "${EVIDENCE_ROOT}/cancel-run-id.txt"
if [[ -n "${run_id}" ]]; then
  request cancel POST "/v2/agent/runs/${run_id}/cancel" '{}'
fi
wait "${stream_pid}" || true
if [[ -n "${run_id}" ]]; then
  request cancel_audit GET "/v2/agent/runs/${run_id}/audit" '{}'
fi

{
  printf 'captured_at=%s\n' "$(date -u +%FT%TZ)"
  printf 'model=deepseek-v4-flash\n'
  printf 'base_url=https://tokenrhythm.studio/v1\n'
  printf 'wire_api=chat_completions\n'
  printf 'cancel_run_id=%s\n' "${run_id}"
  printf 'cancel_stream_http='
  cat "${EVIDENCE_ROOT}/cancel.http-status" 2>/dev/null || true
  printf '\n'
} > "${EVIDENCE_ROOT}/deepseek-functional-summary.txt"

rm -f "${TOKEN_FILE}"
