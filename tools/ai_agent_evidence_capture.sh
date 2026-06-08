#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
EVIDENCE_ROOT="${EVIDENCE_ROOT:-${ROOT_DIR}/docs/acceptance-evidence/ai-agent}"
BASE_URL="${BASE_URL:-http://localhost:8080}"
MODE="${MODE:-stream}"
MESSAGE="${MESSAGE:-哪些商品库存不足，风险最高？}"
CONVERSATION_ID="${CONVERSATION_ID:-}"
TOKEN="${TOKEN:-}"
LOGIN_PHONE="${LOGIN_PHONE:-}"
LOGIN_PASSWORD="${LOGIN_PASSWORD:-}"
ACCOUNT_LABEL="${ACCOUNT_LABEL:-manual}"
BACKEND_PROFILE="${BACKEND_PROFILE:-unknown}"
LLM_STATUS_NOTE="${LLM_STATUS_NOTE:-unknown}"
TOKEN_SOURCE="provided"

usage() {
  cat <<'USAGE'
Usage:
  TOKEN="<bearer-token>" ./tools/ai_agent_evidence_capture.sh
  ./tools/ai_agent_evidence_capture.sh self-test

Environment:
  BASE_URL          Backend base URL. Default: http://localhost:8080
  TOKEN             Bearer token. Required unless ALLOW_NO_AUTH=1.
  LOGIN_PHONE       Optional phone for /v1/auth/login when TOKEN is empty.
  LOGIN_PASSWORD    Optional password for /v1/auth/login when TOKEN is empty.
  ALLOW_NO_AUTH     Set to 1 for local endpoints that do not require auth.
  MESSAGE           Real business question to ask.
  CONVERSATION_ID   Optional numeric conversation id.
  MODE              stream or chat. Default: stream.
  EVIDENCE_ROOT     Default: docs/acceptance-evidence/ai-agent
  ACCOUNT_LABEL     Redacted account label written to 00-env.md.
  BACKEND_PROFILE   Human note for active backend profile.
  LLM_STATUS_NOTE   Human note for LLM config state.

Output:
  docs/acceptance-evidence/ai-agent/{yyyyMMdd-HHmm}-{run_id}/

This script captures HTTP/SSE/audit evidence only. It never fabricates UI
screenshots; add adb screenshots and UI tree dumps separately before marking
the evidence package pass.
USAGE
}

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 127
  fi
}

timestamp_utc() {
  date -u +"%Y-%m-%dT%H:%M:%SZ"
}

safe_name() {
  printf '%s' "$1" | tr -c 'A-Za-z0-9._-' '-'
}

build_payload() {
  local stream="$1"
  if [[ -n "${CONVERSATION_ID}" ]]; then
    if [[ ! "${CONVERSATION_ID}" =~ ^[0-9]+$ ]]; then
      echo "CONVERSATION_ID must be numeric: ${CONVERSATION_ID}" >&2
      exit 2
    fi
    jq -n \
      --arg message "${MESSAGE}" \
      --argjson conversation_id "${CONVERSATION_ID}" \
      --argjson stream "${stream}" \
      '{conversation_id: $conversation_id, message: $message, stream: $stream}'
  else
    jq -n \
      --arg message "${MESSAGE}" \
      --argjson stream "${stream}" \
      '{message: $message, stream: $stream}'
  fi
}

redact_phone() {
  local phone="$1"
  if [[ "${#phone}" -le 4 ]]; then
    printf '***'
  else
    printf '***%s' "${phone: -4}"
  fi
}

resolve_token() {
  if [[ -n "${TOKEN}" ]]; then
    TOKEN_SOURCE="provided"
    return 0
  fi
  if [[ -n "${LOGIN_PHONE}" || -n "${LOGIN_PASSWORD}" ]]; then
    if [[ -z "${LOGIN_PHONE}" || -z "${LOGIN_PASSWORD}" ]]; then
      echo "LOGIN_PHONE and LOGIN_PASSWORD must be provided together." >&2
      exit 2
    fi
    local login_body login_response login_token
    login_body="$(jq -n --arg phone "${LOGIN_PHONE}" --arg password "${LOGIN_PASSWORD}" '{phone: $phone, password: $password}')"
    login_response="$(
      curl -sS \
        -H "Content-Type: application/json" \
        -H "Accept: application/json" \
        -d "${login_body}" \
        "${BASE_URL%/}/v1/auth/login"
    )"
    login_token="$(printf '%s' "${login_response}" | jq -er '.data.token // empty' 2>/dev/null || true)"
    if [[ -z "${login_token}" ]]; then
      echo "Login did not return data.token. Response was not saved to avoid leaking credentials." >&2
      exit 2
    fi
    TOKEN="${login_token}"
    TOKEN_SOURCE="login"
    return 0
  fi
  if [[ "${ALLOW_NO_AUTH:-0}" != "1" ]]; then
    echo "TOKEN is required unless LOGIN_PHONE/LOGIN_PASSWORD or ALLOW_NO_AUTH=1 is provided." >&2
    exit 2
  fi
  TOKEN_SOURCE="none"
}

extract_run_id_from_json() {
  local file="$1"
  jq -er '
    .data.run_id // .data.runId // .run_id // .runId // empty
  ' "${file}" 2>/dev/null | head -n 1 || true
}

extract_run_id_from_sse() {
  local file="$1"
  local line data candidate
  while IFS= read -r line; do
    [[ "${line}" == data:* ]] || continue
    data="${line#data:}"
    data="${data#"${data%%[![:space:]]*}"}"
    candidate="$(
      printf '%s' "${data}" |
        jq -er '.. | objects | (.run_id? // .runId? // empty)' 2>/dev/null |
        head -n 1 || true
    )"
    if [[ -n "${candidate}" ]]; then
      printf '%s\n' "${candidate}"
      return 0
    fi
  done < "${file}"
}

tool_results_filter() {
  jq '
    def payload: (.payload // {});
    def first_present(paths):
      . as $root | reduce paths[] as $p (null; if . == null then ($root | getpath($p)) else . end);
    {
      run_id: (.data.run_id // .data.runId // null),
      audit_id: (.data.audit_id // .data.auditId // null),
      trace_id: (.data.trace_id // .data.traceId // null),
      status: (.data.status // null),
      mode: (.data.mode // null),
      llm_status: (.data.llm_status // .data.llmStatus // null),
      tool_count: (.data.tool_count // .data.toolCount // null),
      tools: [
        .data.events[]? |
        select((.event_type // .eventType // "") | test("^tool_")) |
        {
          seq: .seq,
          event_id: (.event_id // .eventId),
          event_type: (.event_type // .eventType),
          created_at: (.created_at // .createdAt),
          tool_call_id: (payload | first_present([["tool_call_id"], ["toolCallId"], ["data", "tool_call_id"], ["data", "toolCallId"]])),
          tool_name: (payload | first_present([["tool_name"], ["toolName"], ["data", "tool_name"], ["data", "toolName"]])),
          status: (payload | first_present([["status"], ["data", "status"]])),
          input_summary: (payload | first_present([["input_summary"], ["inputSummary"], ["data", "input_summary"], ["data", "inputSummary"]])),
          query_window: (payload | first_present([["query_window"], ["queryWindow"], ["data", "query_window"], ["data", "queryWindow"]])),
          returned_count: (payload | first_present([["returned_count"], ["returnedCount"], ["data", "returned_count"], ["data", "returnedCount"]])),
          total_count: (payload | first_present([["total_count"], ["totalCount"], ["data", "total_count"], ["data", "totalCount"]])),
          limit: (payload | first_present([["limit"], ["data", "limit"]])),
          is_truncated: (payload | first_present([["is_truncated"], ["isTruncated"], ["data", "is_truncated"], ["data", "isTruncated"]])),
          next_cursor: (payload | first_present([["next_cursor"], ["nextCursor"], ["data", "next_cursor"], ["data", "nextCursor"]])),
          duration_ms: (payload | first_present([["duration_ms"], ["durationMs"], ["data", "duration_ms"], ["data", "durationMs"]])),
          error_code: (payload | first_present([["error_code"], ["errorCode"], ["data", "error_code"], ["data", "errorCode"]])),
          evidence: (payload | first_present([["evidence"], ["data", "evidence"]])),
          result_summary: (payload | first_present([["result_summary"], ["resultSummary"], ["data", "result_summary"], ["data", "resultSummary"]])),
          raw_payload: .payload
        }
      ]
    }
  '
}

run_self_test() {
  require_cmd jq
  local tmp_dir sse_file audit_file tool_file run_id
  tmp_dir="$(mktemp -d)"
  trap 'rm -rf "${tmp_dir}"' RETURN
  sse_file="${tmp_dir}/sample.sse"
  audit_file="${tmp_dir}/audit.json"
  tool_file="${tmp_dir}/tool-results.json"

  cat > "${sse_file}" <<'EOF'
event: run_started
data:{"event_type":"run_started","run_id":"run-self-test","seq":1}

event: tool_completed
data: {"event_type":"tool_completed","runId":"run-self-test","seq":2}
EOF

  run_id="$(extract_run_id_from_sse "${sse_file}")"
  if [[ "${run_id}" != "run-self-test" ]]; then
    echo "self-test failed: expected run-self-test from SSE, got ${run_id:-missing}" >&2
    exit 1
  fi

  cat > "${audit_file}" <<'EOF'
{
  "data": {
    "run_id": "run-self-test",
    "audit_id": "audit-self-test",
    "trace_id": "trace-self-test",
    "status": "completed",
    "mode": "tool_query_rule_summary",
    "llm_status": "disabled",
    "tool_count": 1,
    "events": [
      {
        "seq": 2,
        "event_id": "evt-tool-2",
        "event_type": "tool_completed",
        "created_at": 1710000000000,
        "payload": {
          "tool_name": "inventory_low_stock_lookup",
          "status": "completed",
          "input_summary": "库存风险",
          "query_window": {"scope": "current_owner"},
          "returned_count": 3,
          "total_count": 9,
          "limit": 3,
          "is_truncated": false,
          "duration_ms": 42,
          "evidence": [{"label": "低库存", "value": "3"}],
          "result_summary": "3 个商品低库存"
        }
      }
    ]
  }
}
EOF

  tool_results_filter < "${audit_file}" > "${tool_file}"
  jq -e '
    .run_id == "run-self-test"
    and .tools[0].tool_name == "inventory_low_stock_lookup"
    and .tools[0].returned_count == 3
    and .tools[0].total_count == 9
    and .tools[0].is_truncated == false
    and .tools[0].duration_ms == 42
    and .tools[0].evidence[0].label == "低库存"
  ' "${tool_file}" >/dev/null

  echo "ai_agent_evidence_capture self-test passed"
}

write_env_file() {
  local dir="$1"
  cat > "${dir}/00-env.md" <<EOF
# AI Agent Evidence Environment

- Captured at: $(timestamp_utc)
- Base URL: ${BASE_URL}
- Mode: ${MODE}
- Account label: ${ACCOUNT_LABEL}
- Backend profile: ${BACKEND_PROFILE}
- LLM config note: ${LLM_STATUS_NOTE}
- Token present: $(if [[ -n "${TOKEN}" ]]; then echo "yes (redacted)"; else echo "no"; fi)
- Token source: ${TOKEN_SOURCE}
- Login phone: $(if [[ "${TOKEN_SOURCE}" == "login" ]]; then redact_phone "${LOGIN_PHONE}"; else echo "not used"; fi)
- Conversation ID: ${CONVERSATION_ID:-new conversation}
- Question: ${MESSAGE}

## Important

This file intentionally does not store passwords, tokens, model keys, or raw
secrets. UI screenshots and UI tree dumps are not captured by this script and
must be added from a real device before the package can pass P0 review.
EOF
}

write_latency_file() {
  local dir="$1"
  local audit_file="${dir}/03-run-audit.json"
  local started completed duration tool_count event_count
  started=""
  completed=""
  duration=""
  tool_count=""
  event_count=""
  if [[ -s "${audit_file}" ]] && jq -e '.data' "${audit_file}" >/dev/null 2>&1; then
    started="$(jq -r '.data.started_at // ""' "${audit_file}")"
    completed="$(jq -r '.data.completed_at // ""' "${audit_file}")"
    tool_count="$(jq -r '.data.tool_count // ""' "${audit_file}")"
    event_count="$(jq -r '.data.event_count // ""' "${audit_file}")"
    if [[ "${started}" =~ ^[0-9]+$ && "${completed}" =~ ^[0-9]+$ && "${completed}" -ge "${started}" ]]; then
      duration="$((completed - started))"
    fi
  fi
  cat > "${dir}/11-latency.md" <<EOF
# AI Agent Latency

## curl timings

$(if [[ -f "${dir}/01-http-metrics.txt" ]]; then cat "${dir}/01-http-metrics.txt"; else echo "No HTTP metrics captured."; fi)

## audit timings

- started_at: ${started:-missing}
- completed_at: ${completed:-missing}
- audit_duration_ms: ${duration:-missing}
- tool_count: ${tool_count:-missing}
- event_count: ${event_count:-missing}

## UI timing

Android first-visible timing is not captured by this script. Add device-side
screen recording, logcat, or frame timing evidence before marking this package
pass.
EOF
}

write_conclusion_file() {
  local dir="$1"
  local run_id="$2"
  local audit_status="missing"
  local mode_status="missing"
  local llm_status="missing"
  if [[ -s "${dir}/03-run-audit.json" ]] && jq -e '.data' "${dir}/03-run-audit.json" >/dev/null 2>&1; then
    audit_status="$(jq -r '.data.status // "missing"' "${dir}/03-run-audit.json")"
    mode_status="$(jq -r '.data.mode // "missing"' "${dir}/03-run-audit.json")"
    llm_status="$(jq -r '.data.llm_status // "missing"' "${dir}/03-run-audit.json")"
  fi
  cat > "${dir}/12-conclusion.md" <<EOF
# AI Agent Evidence Conclusion

Status: partial

## Captured

- run_id: ${run_id:-missing}
- audit_status: ${audit_status}
- mode: ${mode_status}
- llm_status: ${llm_status}
- HTTP/SSE evidence: captured according to MODE=${MODE}
- Server run audit: $(if [[ -s "${dir}/03-run-audit.json" ]] && jq -e '.data.run_id or .data.runId' "${dir}/03-run-audit.json" >/dev/null 2>&1; then echo "captured"; else echo "missing or invalid"; fi)
- Forbidden scan: captured in 10-forbidden-scan.txt

## Still required before pass

- Add real Android screenshots for AI home, chat answer, expanded RunTrace, and result blocks.
- Add real UI tree dump from the same device/session.
- Manually explain every production-path forbidden-scan hit.
- Confirm answer numbers, rankings, risks, and charts map to tool evidence.
- Confirm mode, llm_status, delta_source, RunTrace UI, and audit records agree.

This script defaults to partial because interface evidence alone cannot prove
the full P0 Android UI and rendered Markdown/chart experience.
EOF
}

capture_forbidden_scan() {
  local dir="$1"
  {
    echo "# Forbidden item scan"
    echo
    echo "Raw production-path matches. Each hit must be reviewed; legitimate"
    echo "negative copy such as 'do not use simulated data' is allowed only when"
    echo "it does not generate mock data, fake streaming, or placeholder results."
    echo
    rg -n \
      "mock|sample|demo|fake|placeholder|模拟|演示|假数据|delay|timer|substring|chunkSize" \
      "${ROOT_DIR}/master-goods-android/feature/agent/src/main" \
      "${ROOT_DIR}/master-goods-android/data/agent/src/main" \
      "${ROOT_DIR}/master-goods-android/core/network/src/main" \
      "${ROOT_DIR}/master-goods-android/core/model/src/main/java/com/zhihuiji/core/model/v2/agent" \
      "${ROOT_DIR}/src/main/java/com/zhihuiji/backend/application/service/v2" \
      "${ROOT_DIR}/src/main/java/com/zhihuiji/backend/api/controller/v2" \
      "${ROOT_DIR}/src/main/java/com/zhihuiji/backend/api/dto/v2/agent" || true
  } > "${dir}/10-forbidden-scan.txt"
}

capture_audit_and_tools() {
  local dir="$1"
  local run_id="$2"
  shift 2
  local auth_args=()
  if [[ -n "${TOKEN}" ]]; then
    auth_args=(-H "Authorization: Bearer ${TOKEN}")
  fi
  if [[ -z "${run_id}" ]]; then
    echo '{"error":"missing run_id; cannot fetch /v2/agent/runs/{runId}/audit"}' > "${dir}/03-run-audit.json"
    echo '{"error":"missing run_id; cannot derive tool results"}' > "${dir}/04-tool-results.json"
    return 0
  fi

  local audit_http_status
  audit_http_status="$(
    curl -sS \
      -w 'http_code=%{http_code}\ntime_total=%{time_total}\n' \
      -D "${dir}/03-run-audit-headers.txt" \
      ${auth_args[@]+"${auth_args[@]}"} \
      -H "Accept: application/json" \
      "${BASE_URL%/}/v2/agent/runs/${run_id}/audit" \
      -o "${dir}/03-run-audit.json"
  )"
  printf '%s' "${audit_http_status}" > "${dir}/03-run-audit-metrics.txt"
  if ! grep -q '^http_code=2' "${dir}/03-run-audit-metrics.txt" || ! jq -e '.data.run_id or .data.runId' "${dir}/03-run-audit.json" >/dev/null 2>&1; then
    jq -n \
      --arg run_id "${run_id}" \
      --rawfile metrics "${dir}/03-run-audit-metrics.txt" \
      --rawfile headers "${dir}/03-run-audit-headers.txt" \
      --rawfile body "${dir}/03-run-audit.json" \
      '{error: "run audit fetch failed or returned invalid envelope", run_id: $run_id, metrics: $metrics, headers: $headers, body: $body}' \
      > "${dir}/03-run-audit-error.json"
    echo '{"error":"run audit missing or invalid; cannot derive tool results"}' > "${dir}/04-tool-results.json"
    return 0
  fi

  tool_results_filter < "${dir}/03-run-audit.json" > "${dir}/04-tool-results.json" 2>/dev/null || {
    echo '{"error":"run audit response was not parseable as expected"}' > "${dir}/04-tool-results.json"
  }
}

main() {
  if [[ "${1:-}" == "self-test" ]]; then
    run_self_test
    exit 0
  fi
  if [[ "${1:-}" == "-h" || "${1:-}" == "--help" || "${1:-}" == "help" ]]; then
    usage
    exit 0
  fi
  require_cmd jq
  require_cmd curl
  require_cmd rg

  if [[ "${MODE}" != "stream" && "${MODE}" != "chat" ]]; then
    echo "MODE must be stream or chat: ${MODE}" >&2
    exit 2
  fi
  resolve_token

  mkdir -p "${EVIDENCE_ROOT}"
  local stamp dir run_id final_dir payload stream_flag endpoint http_status
  stamp="$(date +"%Y%m%d-%H%M")"
  dir="${EVIDENCE_ROOT}/${stamp}-manual"
  mkdir -p "${dir}"
  write_env_file "${dir}"

  local auth_args=()
  if [[ -n "${TOKEN}" ]]; then
    auth_args=(-H "Authorization: Bearer ${TOKEN}")
  fi

  if [[ "${MODE}" == "stream" ]]; then
    stream_flag="true"
    endpoint="${BASE_URL%/}/v2/agent/chat/stream"
    payload="$(build_payload "${stream_flag}")"
    printf '%s\n' "${payload}" > "${dir}/00-request.json"
    http_status="$(
      curl -sS -N \
        ${auth_args[@]+"${auth_args[@]}"} \
        -H "Content-Type: application/json" \
        -H "Accept: text/event-stream" \
        -D "${dir}/01-http-headers.txt" \
        -w 'http_code=%{http_code}\ntime_namelookup=%{time_namelookup}\ntime_connect=%{time_connect}\ntime_starttransfer=%{time_starttransfer}\ntime_total=%{time_total}\n' \
        -o "${dir}/02-raw-sse.log" \
        -d "${payload}" \
        "${endpoint}"
    )"
    printf '%s' "${http_status}" > "${dir}/01-http-metrics.txt"
    jq -n \
      --arg endpoint "${endpoint}" \
      --arg mode "${MODE}" \
      --arg captured_at "$(timestamp_utc)" \
      --slurpfile request "${dir}/00-request.json" \
      --rawfile headers "${dir}/01-http-headers.txt" \
      --rawfile metrics "${dir}/01-http-metrics.txt" \
      '{captured_at: $captured_at, mode: $mode, endpoint: $endpoint, request: $request[0], headers: $headers, metrics: $metrics}' \
      > "${dir}/01-http-response.json"
    run_id="$(extract_run_id_from_sse "${dir}/02-raw-sse.log")"
  else
    stream_flag="false"
    endpoint="${BASE_URL%/}/v2/agent/chat"
    payload="$(build_payload "${stream_flag}")"
    printf '%s\n' "${payload}" > "${dir}/00-request.json"
    http_status="$(
      curl -sS \
        ${auth_args[@]+"${auth_args[@]}"} \
        -H "Content-Type: application/json" \
        -H "Accept: application/json" \
        -D "${dir}/01-http-headers.txt" \
        -w 'http_code=%{http_code}\ntime_namelookup=%{time_namelookup}\ntime_connect=%{time_connect}\ntime_starttransfer=%{time_starttransfer}\ntime_total=%{time_total}\n' \
        -o "${dir}/01-http-body.json" \
        -d "${payload}" \
        "${endpoint}"
    )"
    printf '%s' "${http_status}" > "${dir}/01-http-metrics.txt"
    jq -n \
      --arg endpoint "${endpoint}" \
      --arg mode "${MODE}" \
      --arg captured_at "$(timestamp_utc)" \
      --slurpfile request "${dir}/00-request.json" \
      --slurpfile body "${dir}/01-http-body.json" \
      --rawfile headers "${dir}/01-http-headers.txt" \
      --rawfile metrics "${dir}/01-http-metrics.txt" \
      '{captured_at: $captured_at, mode: $mode, endpoint: $endpoint, request: $request[0], headers: $headers, metrics: $metrics, body: $body[0]}' \
      > "${dir}/01-http-response.json"
    run_id="$(extract_run_id_from_json "${dir}/01-http-body.json")"
    printf 'MODE=chat; no SSE stream captured.\n' > "${dir}/02-raw-sse.log"
  fi

  if [[ -n "${run_id}" ]]; then
    final_dir="${EVIDENCE_ROOT}/${stamp}-$(safe_name "${run_id}")"
    if [[ "${final_dir}" != "${dir}" ]]; then
      mv "${dir}" "${final_dir}"
      dir="${final_dir}"
    fi
  fi

  capture_audit_and_tools "${dir}" "${run_id:-}"
  capture_forbidden_scan "${dir}"
  write_latency_file "${dir}"
  write_conclusion_file "${dir}" "${run_id:-}"

  echo "AI agent evidence package written to: ${dir}"
  if [[ -z "${run_id:-}" ]]; then
    echo "WARNING: run_id was not found; inspect 01-http-response.json and 02-raw-sse.log." >&2
  fi
}

main "$@"
