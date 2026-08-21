#!/usr/bin/env bash
set -euo pipefail

# Read-only Wave 0 capture for the server-side Agent. It never calls chat,
# stream, create, update, confirm, cancel, or delete endpoints.

BASE_URL="${BASE_URL:-https://zhj-api.sxyq27.online}"
LOGIN_PHONE="${LOGIN_PHONE:-}"
LOGIN_PASSWORD="${LOGIN_PASSWORD:-}"
EVIDENCE_ROOT="${EVIDENCE_ROOT:-testing/.artifacts/2026-08-18-8220-current-baseline/wave0-readonly}"

if [[ -z "${LOGIN_PHONE}" || -z "${LOGIN_PASSWORD}" ]]; then
  echo "LOGIN_PHONE and LOGIN_PASSWORD are required through the environment." >&2
  exit 2
fi

command -v curl >/dev/null || { echo "curl is required" >&2; exit 127; }
command -v jq >/dev/null || { echo "jq is required" >&2; exit 127; }

stamp="$(date -u +%Y%m%dT%H%M%SZ)"
run_id="wave0-readonly-8220-${stamp}"
out="${EVIDENCE_ROOT}/${run_id}"
mkdir -p "${out}"
status_file="${out}/http-status.tsv"
: > "${status_file}"

login_body="$(jq -n --arg phone "${LOGIN_PHONE}" --arg password "${LOGIN_PASSWORD}" '{phone:$phone,password:$password}')"
login_tmp="$(mktemp)"
trap 'rm -f "${login_tmp}"' EXIT
login_status="$(curl -sS --retry 2 --connect-timeout 10 --max-time 30 \
  -o "${login_tmp}" -w "%{http_code}" \
  -H "Content-Type: application/json" \
  -d "${login_body}" "${BASE_URL%/}/v1/auth/login")"
printf 'login\t%s\t/v1/auth/login\n' "${login_status}" >> "${status_file}"

token="$(jq -er '.data.token // empty' "${login_tmp}" 2>/dev/null || true)"
if [[ -z "${token}" ]]; then
  jq -n \
    --arg captured_at "$(date -u +%FT%TZ)" \
    --arg endpoint "${BASE_URL%/}/v1/auth/login" \
    --arg status "${login_status}" \
    '{captured_at:$captured_at,endpoint:$endpoint,http_status:($status|tonumber),token_present:false}' \
    > "${out}/auth-login-meta.json"
  echo "Login did not return a token; response body was not persisted." >&2
  exit 2
fi
jq -n \
  --arg captured_at "$(date -u +%FT%TZ)" \
  --arg endpoint "${BASE_URL%/}/v1/auth/login" \
  --arg status "${login_status}" \
  '{captured_at:$captured_at,endpoint:$endpoint,http_status:($status|tonumber),token_present:true}' \
  > "${out}/auth-login-meta.json"
rm -f "${login_tmp}"
trap - EXIT

request() {
  local name="$1"
  local path="$2"
  local body_file="${out}/${name}.json"
  local status
  status="$(curl -sS --retry 2 --connect-timeout 10 --max-time 45 \
    -o "${body_file}" -w "%{http_code}" \
    -H "Authorization: Bearer ${token}" \
    -H "Accept: application/json" \
    "${BASE_URL%/}${path}" || true)"
  printf '%s\t%s\t%s\n' "${name}" "${status}" "${path}" >> "${status_file}"
}

request auth_me /v2/auth/users/me
request store_current /v2/stores/current
request store_members /v2/stores/current/members
request agent_conversations '/v2/agent/conversations?page=1&limit=10'
request agent_workbench /v2/agent/workbench
request agent_drafts '/v2/agent/drafts?page=1&limit=20'
request agent_pending_drafts /v2/agent/drafts/pending
request agent_tasks /v2/agent/tasks
request agent_notifications '/v2/agent/notifications?unread_only=false'

conversation_id="$(jq -er '.data[0].id // .data.items[0].id // empty' "${out}/agent_conversations.json" 2>/dev/null || true)"
if [[ -n "${conversation_id}" && "${conversation_id}" =~ ^[0-9]+$ ]]; then
  request "agent_messages_${conversation_id}" "/v2/agent/conversations/${conversation_id}/messages?page=1&limit=50"
fi

python3 - "${out}" <<'PY'
import json
import sys
from pathlib import Path

out = Path(sys.argv[1])
status = []
for line in (out / "http-status.tsv").read_text(encoding="utf-8").splitlines():
    name, code, path = line.split("\t", 2)
    status.append({"name": name, "http_status": int(code), "path": path})

def body(name):
    path = out / f"{name}.json"
    if not path.exists():
        return None
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError:
        return {"raw_file": str(path)}

def case_result(name):
    item = next((row for row in status if row["name"] == name), None)
    if item is None:
        return "Blocked"
    if item["http_status"] == 200:
        return "Passed"
    if item["http_status"] in (401, 403):
        return "Blocked"
    return "Failed"

checks = [
    ("AG-SERVER-W0-RO-001", "auth/session", "auth_me", "读取真实账户身份"),
    ("AG-SERVER-W0-RO-002", "store-context", "store_current", "读取真实门店上下文"),
    ("AG-SERVER-W0-RO-003", "store-members", "store_members", "读取真实成员边界"),
    ("AG-SERVER-W0-RO-004", "agent-history", "agent_conversations", "读取真实会话历史"),
    ("AG-SERVER-W0-RO-005", "agent-workbench", "agent_workbench", "读取真实 Agent 首页聚合"),
    ("AG-SERVER-W0-RO-006", "agent-drafts", "agent_drafts", "读取真实草稿"),
    ("AG-SERVER-W0-RO-007", "agent-pending-drafts", "agent_pending_drafts", "读取真实待确认草稿"),
    ("AG-SERVER-W0-RO-008", "agent-tasks", "agent_tasks", "读取真实 Agent 任务"),
    ("AG-SERVER-W0-RO-009", "agent-notifications", "agent_notifications", "读取真实 Agent 通知"),
]
cases = []
for test_id, category_id, name, expected in checks:
    row = next((item for item in status if item["name"] == name), None)
    cases.append({
        "test_id": test_id,
        "category_id": category_id,
        "wave_id": "Wave 0",
        "prompt": None,
        "request": {"path": row["path"] if row else None},
        "response": body(name),
        "raw_sse_events": [],
        "model_calls": [],
        "tool_trace": [],
        "oracle_snapshot": {},
        "expected": expected,
        "actual": {"http_status": row["http_status"] if row else None},
        "final_answer": {},
        "result_blocks": [],
        "performance": {},
        "identifiers": {},
        "cleanup": {"type": "read_only", "created_entities": []},
        "artifacts": [f"{out.name}/{name}.json", f"{out.name}/http-status.tsv"],
        "result": case_result(name),
        "trace_completeness": "Not applicable; read-only Wave 0",
    })

summary = {
    "passed": sum(item["result"] == "Passed" for item in cases),
    "failed": sum(item["result"] == "Failed" for item in cases),
    "blocked": sum(item["result"] == "Blocked" for item in cases),
    "total": len(cases),
}
payload = {
    "schema_version": "server-agent-eval.v1",
    "run_id": out.name,
    "test_stage": "wave0_readonly_8220_current",
    "source_snapshot_id": "see current-8220-baseline.md",
    "server": {
        "api_base": "https://zhj-api.sxyq27.online/",
        "host": "8.220.206.9",
        "image_status": "sxyq27-zhj-api:20260818",
    },
    "provider": {
        "base_url": "https://oneapi.sxyq27.online/v1",
        "model": "gpt-5.6-luna",
        "wire_api": "chat_completions",
        "api_key_present": "runtime_secret_only",
        "api_key": "[REDACTED]",
    },
    "account_context": {
        "source": "auth_me/store_current response",
        "owner_user_id": "[see local response]",
        "store_id": "[see local response]",
    },
    "database_baseline": {"source": "API read-only only; no direct DB query in this run"},
    "cases": cases,
    "summary": summary,
    "release_gate": {
        "result": "Blocked",
        "reasons": [
            "8220 has no reusable production session/store/Agent history",
            "this run intentionally excluded model chat and writes",
        ],
    },
    "http_status": status,
}
(out / "agent-evaluation.json").write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(json.dumps({"run_id": out.name, "summary": summary}, ensure_ascii=False))
PY

printf 'evidence_dir=%s\n' "${out}"
cat "${status_file}"
