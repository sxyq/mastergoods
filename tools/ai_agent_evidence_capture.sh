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
  ./tools/ai_agent_evidence_capture.sh refresh-existing <evidence-dir>

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
the evidence package pass. It also captures `/v2/agent/workbench` when auth is
available, or records an honest skipped/failed workbench result when it is not.
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

extract_sse_events_filter() {
  jq -R '
    select(startswith("data:")) |
    ltrimstr("data:") |
    fromjson? |
    {
      seq: (.seq // null),
      event_id: (.event_id // .eventId // null),
      event_type: (.event_type // .eventType // null),
      run_id: (.run_id // .runId // null),
      tool_name: (.tool_name // .toolName // null)
    }
  '
}

write_reconciliation_file() {
  local dir="$1"
  local sse_events_file="${dir}/.sse-events.jsonl"
  local audit_events_file="${dir}/.audit-events.jsonl"
  local sse_events_json="${dir}/.sse-events.json"
  local audit_events_json="${dir}/.audit-events.json"
  local reconciliation_json="${dir}/.reconciliation.json"
  local output_file="${dir}/13-sse-audit-ui-reconciliation.md"

  if [[ ! -s "${dir}/02-raw-sse.log" || ! -s "${dir}/03-run-audit.json" ]] || ! jq -e '.data.events' "${dir}/03-run-audit.json" >/dev/null 2>&1; then
    cat > "${output_file}" <<'EOF'
# SSE / Audit / UI Reconciliation

Status: partial

Raw SSE or audit events are missing, so this package cannot prove event-level
SSE/audit alignment. Add a valid `02-raw-sse.log` and `03-run-audit.json`.
EOF
    return 0
  fi

  extract_sse_events_filter < "${dir}/02-raw-sse.log" > "${sse_events_file}" || true
  jq -c '
    .data.events[]? |
    {
      seq: (.seq // null),
      event_id: (.event_id // .eventId // null),
      event_type: (.event_type // .eventType // null),
      run_id: (.payload.run_id // .payload.runId // .data.run_id // .data.runId // null),
      tool_name: (.payload.tool_name // .payload.toolName // null)
    }
  ' "${dir}/03-run-audit.json" > "${audit_events_file}"
  jq -s '.' "${sse_events_file}" > "${sse_events_json}"
  jq -s '.' "${audit_events_file}" > "${audit_events_json}"

  jq -n \
    --slurpfile sseEvents "${sse_events_json}" \
    --slurpfile auditEvents "${audit_events_json}" '
    def key($event): (($event.seq // -1) | tostring);
    ($sseEvents[0] // []) as $sseEvents |
    ($auditEvents[0] // []) as $auditEvents |
    ($auditEvents | map({key: key(.), value: .}) | from_entries) as $auditBySeq |
    [
      $sseEvents[] |
      . as $sse |
      ($auditBySeq[key($sse)] // {}) as $audit |
      {
        seq: $sse.seq,
        event_id: $sse.event_id,
        raw_sse_event_type: $sse.event_type,
        audit_event_type: ($audit.event_type // null),
        android_runtrace_row:
          (if ($sse.event_type // "") | startswith("tool_") then
            "RunTrace tool card" + (if $sse.tool_name then ": " + $sse.tool_name else "" end)
          elif ($sse.event_type // "") == "answer_completed" then
            "Chat answer / completion state"
          elif ($sse.event_type // "") == "run_started" or ($sse.event_type // "") == "run_completed" or ($sse.event_type // "") == "run_cancelled" or ($sse.event_type // "") == "run_failed" then
            "Run lifecycle row"
          else
            "RunTrace process row"
          end),
        conclusion:
          (if ($audit | length) == 0 then "fail"
          elif $sse.event_id != ($audit.event_id // null) then "fail"
          elif $sse.event_type != ($audit.event_type // null) then "fail"
          else "pass"
          end)
      }
    ]
  ' > "${reconciliation_json}"

  {
    echo "# SSE / Audit / UI Reconciliation"
    echo
    echo "| seq | event_id | raw SSE event_type | audit event_type | Android RunTrace row | conclusion |"
    echo "|---|---|---|---|---|---|"
    jq -r '
      .[] |
      "| \(.seq // "missing") | `\(.event_id // "missing")` | `\(.raw_sse_event_type // "missing")` | `\(.audit_event_type // "missing")` | \(.android_runtrace_row) | \(.conclusion) |"
    ' "${reconciliation_json}"
    if jq -e 'any(.conclusion == "fail")' "${reconciliation_json}" >/dev/null; then
      echo
      echo "Status: fail"
      echo
      echo "At least one SSE event did not match the persisted audit event with the same seq."
    else
      echo
      echo "Status: pass-for-interface"
      echo
      echo "SSE and server audit events match by seq, event_id, and event_type. Android UI evidence is still required before full P0 pass."
    fi
  } > "${output_file}"

  rm -f "${sse_events_file}" "${audit_events_file}" "${sse_events_json}" "${audit_events_json}" "${reconciliation_json}"
}

write_run_summary_file() {
  local dir="$1"
  local output_file="${dir}/14-agent-run-summary.json"
  local tool_results_json="${dir}/04-tool-results.json"
  local generated_tool_results_json=""
  if [[ ! -s "${dir}/03-run-audit.json" ]] || ! jq -e '.data' "${dir}/03-run-audit.json" >/dev/null 2>&1; then
    echo '{"error":"run audit missing or invalid; cannot write run summary"}' > "${output_file}"
    return 0
  fi
  if [[ ! -s "${tool_results_json}" ]] || ! jq -e '.tools' "${tool_results_json}" >/dev/null 2>&1; then
    generated_tool_results_json="${dir}/.tool-results-for-summary.json"
    tool_results_filter < "${dir}/03-run-audit.json" > "${generated_tool_results_json}"
    tool_results_json="${generated_tool_results_json}"
  fi

  jq -n \
    --slurpfile audit "${dir}/03-run-audit.json" \
    --slurpfile toolResults "${tool_results_json}" '
    ($audit[0] // {}) as $auditDoc |
    ($auditDoc.data // {}) as $data |
    ($toolResults[0] // {}) as $toolDoc |
    {
      run_id: ($data.run_id // $data.runId // null),
      status: ($data.status // null),
      mode: ($data.mode // null),
      llm_status: ($data.llm_status // $data.llmStatus // null),
      plan_source: ($data.plan_source // $data.planSource // null),
      tool_count: ($data.tool_count // $data.toolCount // null),
      event_count: ($data.event_count // $data.eventCount // null),
      performance: {
        started_at: ($data.started_at // $data.startedAt // null),
        completed_at: ($data.completed_at // $data.completedAt // null),
        duration_ms:
          (if (($data.started_at // $data.startedAt // null) | type) == "number"
              and (($data.completed_at // $data.completedAt // null) | type) == "number"
           then (($data.completed_at // $data.completedAt) - ($data.started_at // $data.startedAt))
           else null end)
      },
      tools: [
        ($toolDoc.tools // [])[] |
        select((.event_type // .eventType // "") == "tool_completed") |
        {
          tool_name,
          returned_count,
          limit,
          is_truncated,
          duration_ms,
          result_summary,
          evidence
        }
      ],
      events: [
        ($data.events // [])[] |
        {
          seq: (.seq // null),
          event_id: (.event_id // .eventId // null),
          event_type: (.event_type // .eventType // null)
        }
      ]
    }
  ' > "${output_file}"
  if [[ -n "${generated_tool_results_json}" ]]; then
    rm -f "${generated_tool_results_json}"
  fi
}

capture_workbench_response() {
  local dir="$1"
  local output_file="${dir}/16-workbench-response.json"
  local endpoint="${BASE_URL%/}/v2/agent/workbench"
  local body_file="${dir}/.workbench-body.json"
  local headers_file="${dir}/.workbench-headers.txt"
  local metrics_file="${dir}/.workbench-metrics.txt"
  local error_file="${dir}/.workbench-curl-error.txt"
  local auth_args=()
  local curl_output curl_status

  if [[ -z "${TOKEN}" && "${ALLOW_NO_AUTH:-0}" != "1" ]]; then
    if [[ -s "${output_file}" ]]; then
      return 0
    fi
    jq -n \
      --arg endpoint "${endpoint}" \
      --arg captured_at "$(timestamp_utc)" \
      '{captured_at: $captured_at, endpoint: $endpoint, status: "skipped", reason: "TOKEN missing and ALLOW_NO_AUTH is not 1; cannot fetch owner-scoped workbench response"}' \
      > "${output_file}"
    return 0
  fi

  if [[ -n "${TOKEN}" ]]; then
    auth_args=(-H "Authorization: Bearer ${TOKEN}")
  fi

  set +e
  curl_output="$(
    curl -sS \
      -w 'http_code=%{http_code}\ntime_namelookup=%{time_namelookup}\ntime_connect=%{time_connect}\ntime_starttransfer=%{time_starttransfer}\ntime_total=%{time_total}\n' \
      -D "${headers_file}" \
      ${auth_args[@]+"${auth_args[@]}"} \
      -H "Accept: application/json" \
      "${endpoint}" \
      -o "${body_file}" \
      2> "${error_file}"
  )"
  curl_status=$?
  set -e
  printf '%s' "${curl_output}" > "${metrics_file}"

  if [[ "${curl_status}" -ne 0 ]]; then
    jq -n \
      --arg endpoint "${endpoint}" \
      --arg captured_at "$(timestamp_utc)" \
      --arg curl_status "${curl_status}" \
      --rawfile stderr "${error_file}" \
      '{captured_at: $captured_at, endpoint: $endpoint, status: "failed", curl_status: ($curl_status | tonumber), stderr: $stderr}' \
      > "${output_file}"
    rm -f "${body_file}" "${headers_file}" "${metrics_file}" "${error_file}"
    return 0
  fi

  if jq -e . "${body_file}" >/dev/null 2>&1; then
    jq -n \
      --arg endpoint "${endpoint}" \
      --arg captured_at "$(timestamp_utc)" \
      --rawfile headers "${headers_file}" \
      --rawfile metrics "${metrics_file}" \
      --slurpfile body "${body_file}" \
      '{captured_at: $captured_at, endpoint: $endpoint, status: "captured", headers: $headers, metrics: $metrics, body: $body[0]}' \
      > "${output_file}"
  else
    jq -n \
      --arg endpoint "${endpoint}" \
      --arg captured_at "$(timestamp_utc)" \
      --rawfile headers "${headers_file}" \
      --rawfile metrics "${metrics_file}" \
      --rawfile body "${body_file}" \
      '{captured_at: $captured_at, endpoint: $endpoint, status: "invalid_json", headers: $headers, metrics: $metrics, raw_body: $body}' \
      > "${output_file}"
  fi
  rm -f "${body_file}" "${headers_file}" "${metrics_file}" "${error_file}"
}

write_workbench_cleanliness_file() {
  local dir="$1"
  local response_file="${dir}/16-workbench-response.json"
  local output_file="${dir}/17-workbench-cleanliness.md"
  local summary_file="${dir}/.workbench-cleanliness.json"

  if [[ ! -s "${response_file}" ]]; then
    cat > "${output_file}" <<'EOF'
# Workbench Cleanliness Review

Status: partial

`16-workbench-response.json` is missing. Capture `/v2/agent/workbench`
before claiming the AI home entry is clean.
EOF
    return 0
  fi

  jq '
    def data: (.body.data // .body // {});
    def arr(v): if v == null then [] elif (v | type) == "array" then v else [v] end;
    def text(v): if v == null then "" else (v | tostring) end;
    def bad_question:
      test("今日|销售额|报表|KPI|图表|看板|摘要|排行|经营概览|默认统计");
    (.status // "unknown") as $status |
    data as $data |
    (arr($data.kpi_cards // $data.kpiCards)) as $kpi |
    (arr($data.risk_alerts // $data.riskAlerts)) as $risks |
    (text($data.today_summary // $data.todaySummary)) as $today |
    (arr($data.quick_questions // $data.quickQuestions)) as $questions |
    ($questions | map(select((text(.) | bad_question)))) as $badQuestions |
    {
      response_status: $status,
      kpi_cards_count: (if $status == "captured" then ($kpi | length) else null end),
      risk_alerts_count: (if $status == "captured" then ($risks | length) else null end),
      today_summary_present: (if $status == "captured" then ($today | length > 0) else null end),
      quick_questions_count: (if $status == "captured" then ($questions | length) else null end),
      dashboard_like_quick_questions: (if $status == "captured" then $badQuestions else null end),
      verdict:
        (if (.status // "") != "captured" then "partial"
         elif ($kpi | length) == 0
          and ($risks | length) == 0
          and (($today | length) == 0)
          and (($badQuestions | length) == 0)
         then "pass-for-interface"
         else "fail"
         end)
    }
  ' "${response_file}" > "${summary_file}"

  {
    echo "# Workbench Cleanliness Review"
    echo
    echo "| Check | Result |"
    echo "|---|---|"
    jq -r '
      "| response status | `\(.response_status)` |",
      "| kpi_cards count | `\(.kpi_cards_count // "n/a")` |",
      "| risk_alerts count | `\(.risk_alerts_count // "n/a")` |",
      "| today_summary present | `\(.today_summary_present // "n/a")` |",
      "| quick_questions count | `\(.quick_questions_count // "n/a")` |",
      "| dashboard-like quick questions | `\(if .dashboard_like_quick_questions == null then "n/a" else ((.dashboard_like_quick_questions // []) | join(" ; ")) end)` |"
    ' "${summary_file}"
    echo
    jq -r '"Status: \(.verdict)"' "${summary_file}"
    echo
    echo "Only a captured response with Status: pass-for-interface proves the backend workbench response shape. Android first-screen screenshot and UI tree are still required before full P0 pass."
  } > "${output_file}"

  rm -f "${summary_file}"
}

refresh_existing_evidence() {
  local dir="$1"
  local run_id
  if [[ -z "${dir}" || ! -d "${dir}" ]]; then
    echo "refresh-existing requires an existing evidence directory." >&2
    exit 2
  fi
  require_cmd jq
  require_cmd rg
  run_id="$(extract_run_id_from_json "${dir}/03-run-audit.json")"
  if [[ -z "${run_id}" && -s "${dir}/02-raw-sse.log" ]]; then
    run_id="$(extract_run_id_from_sse "${dir}/02-raw-sse.log")"
  fi
  if [[ -s "${dir}/03-run-audit.json" ]] && jq -e '.data' "${dir}/03-run-audit.json" >/dev/null 2>&1; then
    tool_results_filter < "${dir}/03-run-audit.json" > "${dir}/04-tool-results.json"
  fi
  capture_workbench_response "${dir}"
  write_reconciliation_file "${dir}"
  write_run_summary_file "${dir}"
  write_workbench_cleanliness_file "${dir}"
  capture_forbidden_scan "${dir}"
  write_forbidden_scan_review "${dir}"
  write_latency_file "${dir}"
  write_conclusion_file "${dir}" "${run_id:-}"
  echo "AI agent existing evidence package refreshed: ${dir}"
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
data:{"event_type":"run_started","run_id":"run-self-test","seq":1,"event_id":"evt-run-1"}

event: tool_completed
data: {"event_type":"tool_completed","runId":"run-self-test","seq":2,"event_id":"evt-tool-2","tool_name":"inventory_low_stock_lookup"}
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
    "event_count": 2,
    "events": [
      {
        "seq": 1,
        "event_id": "evt-run-1",
        "event_type": "run_started",
        "payload": {
          "run_id": "run-self-test"
        }
      },
      {
        "seq": 2,
        "event_id": null,
        "event_type": "tool_completed",
        "created_at": 1710000000000,
        "payload": {
          "event_id": "evt-tool-2",
          "run_id": "run-self-test",
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
  jq '
    .data.events[0].event_id = "evt-run-1"
    | .data.events[0].payload.event_id = "evt-run-1"
    | .data.events[1].event_id = "evt-tool-2"
  ' "${audit_file}" > "${tmp_dir}/audit.fixed.json"
  mv "${tmp_dir}/audit.fixed.json" "${audit_file}"

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

  cp "${sse_file}" "${tmp_dir}/02-raw-sse.log"
  cp "${audit_file}" "${tmp_dir}/03-run-audit.json"
  write_reconciliation_file "${tmp_dir}"
  write_run_summary_file "${tmp_dir}"
  jq -e '.event_count == 2 and .tools[0].is_truncated == false' "${tmp_dir}/14-agent-run-summary.json" >/dev/null
  grep -q 'Status: pass-for-interface' "${tmp_dir}/13-sse-audit-ui-reconciliation.md"

  cat > "${tmp_dir}/16-workbench-response.json" <<'EOF'
{
  "status": "captured",
  "body": {
    "code": 0,
    "message": "success",
    "data": {
      "greeting": "你好，我是智慧记 AI 助手",
      "kpi_cards": [],
      "quick_questions": ["查看 AI 助手可以查询哪些真实数据"],
      "recent_conversations": [],
      "pending_drafts": [],
      "risk_alerts": [],
      "today_summary": null
    }
  }
}
EOF
  write_workbench_cleanliness_file "${tmp_dir}"
  grep -q 'Status: pass-for-interface' "${tmp_dir}/17-workbench-cleanliness.md"

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
  local workbench_status="missing"
  if [[ -s "${dir}/03-run-audit.json" ]] && jq -e '.data' "${dir}/03-run-audit.json" >/dev/null 2>&1; then
    audit_status="$(jq -r '.data.status // "missing"' "${dir}/03-run-audit.json")"
    mode_status="$(jq -r '.data.mode // "missing"' "${dir}/03-run-audit.json")"
    llm_status="$(jq -r '.data.llm_status // "missing"' "${dir}/03-run-audit.json")"
  fi
  if [[ -s "${dir}/16-workbench-response.json" ]]; then
    workbench_status="$(jq -r '.status // "missing"' "${dir}/16-workbench-response.json" 2>/dev/null || echo "invalid")"
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
- Workbench response: ${workbench_status}

## Still required before pass

- Add real Android screenshots for AI home, chat answer, expanded RunTrace, and result blocks.
- Add real UI tree dump from the same device/session.
- Capture \`/v2/agent/workbench\` with a valid owner token; current status is ${workbench_status}.
- Forbidden scan review draft is in 15-forbidden-scan-review.md; resolve any \`needs evidence\` row before pass.
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
      "${ROOT_DIR}/src/main/java/com/zhihuiji/backend/api/dto/v2/agent" | sort || true
  } > "${dir}/10-forbidden-scan.txt"
}

review_forbidden_hit() {
  local path="$1"
  local content="$2"
  local verdict="needs evidence"
  local reason="新增命中未被脚本规则识别，需要人工确认是否进入生产回答、任务、通知、草稿或流式体验。"

  case "${path}:${content}" in
    *AgentChatViewModel.kt*delay\(48\)*)
      verdict="pass"
      reason="UI 合帧节流，只在收到服务端 answer_delta 后合并刷新；不会拆分完整 answer，也不会生成本地假 token。"
      ;;
    *AgentChatViewModel.kt*"import kotlinx.coroutines.delay"*)
      verdict="pass"
      reason="仅为同文件 answer_delta 合帧节流提供 coroutine delay import；是否安全由 delay(48) 调用点约束。"
      ;;
    *AgentMarkdownText.kt*substring*)
      verdict="pass"
      reason="Markdown parser 的边界解析、链接、强调和行内代码切片；不参与回答拆字、假流式或补造业务数据。"
      ;;
    *ResultBlockRenderer.kt*"模拟标签"*)
      verdict="pass"
      reason="负向防护文案：当图表缺少真实标签时停止绘制，明确避免生成模拟标签。"
      ;;
    *AgentChatScreen.kt*placeholder*)
      verdict="pass"
      reason="输入框 placeholder 文案，提示用户输入经营问题；不生成 placeholder 数据或默认报表结果。"
      ;;
    *V2AgentConversationService.java*substring*)
      verdict="pass"
      reason="会话标题/摘要长度裁剪；不改变工具查询结果，不生成回答内容或流式事件。"
      ;;
    *V2AgentAiService.java*extractJsonObject*|*V2AgentAiService.java*"rawText.substring"*)
      verdict="pass"
      reason="从模型规划文本中提取 JSON 对象边界；不拆分最终答案，也不补造工具结果。"
      ;;
    *V2AgentAiService.java*"不要用模拟数据替代"*|*V2AgentAiService.java*"没有使用模拟数据替代"*|*V2AgentAiService.java*"未使用模拟数据替代"*)
      verdict="pass"
      reason="负向安全/诚实文案，要求工具失败时不得用模拟数据替代真实查询。"
      ;;
    *V2AgentAiService.java*substring*)
      verdict="pass"
      reason="日志、错误摘要、图表标签、conversation title 或 UI 摘要的长度裁剪；不用于本地打字机、规则摘要分块或 fake model_stream。"
      ;;
  esac

  printf '%s\t%s\n' "${verdict}" "${reason}"
}

write_forbidden_scan_review() {
  local dir="$1"
  local scan_file="${dir}/10-forbidden-scan.txt"
  local review_file="${dir}/15-forbidden-scan-review.md"
  local line path rest line_no content verdict reason index

  {
    echo "# Forbidden Scan Review"
    echo
    echo "This review explains each production-path keyword hit from"
    echo "\`10-forbidden-scan.txt\`. A \`pass\` verdict means the hit was checked"
    echo "against source context and does not create mock data, fake streaming,"
    echo "placeholder results, or simulated agent behavior. Unknown future hits"
    echo "must remain \`needs evidence\` until reviewed."
    echo
    echo "| # | Location | Verdict | Reason |"
    echo "|---|---|---|---|"

    index=0
    while IFS= read -r line; do
      [[ "${line}" == /* ]] || continue
      path="${line%%:*}"
      rest="${line#*:}"
      line_no="${rest%%:*}"
      content="${rest#*:}"
      IFS=$'\t' read -r verdict reason < <(review_forbidden_hit "${path}" "${content}")
      index=$((index + 1))
      printf '| %s | `%s:%s` | `%s` | %s |\n' \
        "${index}" \
        "${path#${ROOT_DIR}/}" \
        "${line_no}" \
        "${verdict}" \
        "${reason}"
    done < "${scan_file}"

    if [[ "${index}" -eq 0 ]]; then
      echo "| - | none | \`pass\` | No production-path forbidden keyword hits were found. |"
    fi
  } > "${review_file}"
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
  if [[ "${1:-}" == "refresh-existing" ]]; then
    refresh_existing_evidence "${2:-}"
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
  capture_workbench_response "${dir}"
  write_reconciliation_file "${dir}"
  write_run_summary_file "${dir}"
  write_workbench_cleanliness_file "${dir}"
  capture_forbidden_scan "${dir}"
  write_forbidden_scan_review "${dir}"
  write_latency_file "${dir}"
  write_conclusion_file "${dir}" "${run_id:-}"

  echo "AI agent evidence package written to: ${dir}"
  if [[ -z "${run_id:-}" ]]; then
    echo "WARNING: run_id was not found; inspect 01-http-response.json and 02-raw-sse.log." >&2
  fi
}

main "$@"
