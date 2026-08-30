#!/usr/bin/env bash
set -euo pipefail

agent_base_url="${AGENT_PROBE_BASE_URL:-http://127.0.0.1:18080}"
agent_probe_routes=(
  "GET /v2/agent/conversations"
  "GET /v2/agent/conversations/1"
  "POST /v2/agent/conversations"
  "PUT /v2/agent/conversations/1"
  "DELETE /v2/agent/conversations/1"
  "GET /v2/agent/conversations/1/messages"
  "GET /v2/agent/conversations/1/run-traces"
  "POST /v2/agent/conversations/1/messages"
  "GET /v2/agent/drafts"
  "POST /v2/agent/drafts"
  "GET /v2/agent/drafts/pending"
  "POST /v2/agent/drafts/1/confirm"
  "POST /v2/agent/drafts/1/cancel"
  "PUT /v2/agent/drafts/1"
  "DELETE /v2/agent/drafts/1"
  "GET /v2/agent/workbench"
  "GET /v2/agent/tasks"
  "GET /v2/agent/notifications"
  "POST /v2/agent/notifications/1/read"
  "POST /v2/agent/chat"
  "POST /v2/agent/images/generate"
  "POST /v2/agent/chat/stream"
  "POST /v2/agent/runs/anonymous-probe/cancel"
  "GET /v2/agent/runs/anonymous-probe/audit"
)

printf 'method\tpath\thttp_status\tcontent_type\n'
for agent_probe_route in "${agent_probe_routes[@]}"; do
  agent_probe_method="${agent_probe_route%% *}"
  agent_probe_path="${agent_probe_route#* }"
  agent_probe_body="$(mktemp)"
  trap 'rm -f "$agent_probe_body"' EXIT
  if [[ "$agent_probe_method" == "POST" || "$agent_probe_method" == "PUT" ]]; then
    agent_probe_meta="$(curl -sS --max-time 3 -X "$agent_probe_method" -H 'Content-Type: application/json' --data '{}' -o "$agent_probe_body" -w $'%{http_code}\t%{content_type}' "$agent_base_url$agent_probe_path")"
  else
    agent_probe_meta="$(curl -sS --max-time 3 -X "$agent_probe_method" -o "$agent_probe_body" -w $'%{http_code}\t%{content_type}' "$agent_base_url$agent_probe_path")"
  fi
  printf '%s\t%s\t%s\n' "$agent_probe_method" "$agent_probe_path" "$agent_probe_meta"
done
