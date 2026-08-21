#!/usr/bin/env bash
set -euo pipefail

REMOTE_DIR="${1:?remote evidence directory is required}"
DURATION_SECONDS="${2:?duration in seconds is required}"
BASE_URL='http://127.0.0.1:18080'
DB_CONTAINER='zhihuiji154-postgres'
DB_USER='zhihuiji'
DB_NAME='zhihuiji'
OWNER_ID=7
conversation_id=''
AUTH_HEADER=''

mkdir -p "$REMOTE_DIR"

count_json() {
  docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -At -c "select json_build_object('users',(select count(*) from users),'agent_conversations',(select count(*) from agent_conversations),'agent_messages',(select count(*) from agent_messages),'agent_run_audits',(select count(*) from agent_run_audits),'agent_run_audit_events',(select count(*) from agent_run_audit_events),'agent_drafts',(select count(*) from agent_drafts),'products',(select count(*) from products),'customers',(select count(*) from customers),'suppliers',(select count(*) from suppliers),'sale_orders',(select count(*) from sale_orders),'purchase_orders',(select count(*) from purchase_orders),'finance_records',(select count(*) from finance_records),'inventory_snapshots',(select count(*) from inventory_snapshots),'inventory_ledger',(select count(*) from inventory_ledger),'accounts',(select count(*) from accounts),'payments',(select count(*) from payments));"
}

cleanup() {
  if [[ -n "$conversation_id" && -n "$AUTH_HEADER" ]]; then
    curl -sS --connect-timeout 10 --max-time 30 \
      -o "$REMOTE_DIR/conversation-delete.json" \
      -w '%{http_code}\n' \
      -H "$AUTH_HEADER" \
      -X DELETE "$BASE_URL/v2/agent/conversations/$conversation_id" \
      > "$REMOTE_DIR/conversation-delete.status" || true
  fi
  count_json > "$REMOTE_DIR/db-counts-post.json" 2> "$REMOTE_DIR/db-counts-post.error" || true
}
trap cleanup EXIT

count_json > "$REMOTE_DIR/db-counts-pre.json"
TOKEN="$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -At -c "select token from sessions where user_id = $OWNER_ID and is_active = true and expires_at > (extract(epoch from now()) * 1000)::bigint order by id desc limit 1;" | head -n 1)"
if [[ -z "$TOKEN" ]]; then
  printf '%s\n' 'token_missing' > "$REMOTE_DIR/error.txt"
  exit 2
fi

AUTH_HEADER="Authorization: Bearer $TOKEN"
conversation_body='{"title":"Agent 15 minute read-only soak","status":"active"}'
conversation_status="$(curl -sS --connect-timeout 10 --max-time 30 \
  -o "$REMOTE_DIR/conversation-create.json" \
  -w '%{http_code}' \
  -H "$AUTH_HEADER" \
  -H 'Content-Type: application/json' \
  -d "$conversation_body" \
  "$BASE_URL/v2/agent/conversations" || true)"
conversation_id="$(python3 - "$REMOTE_DIR/conversation-create.json" <<'PY'
import json
import sys

try:
    with open(sys.argv[1], encoding='utf-8') as handle:
        body = json.load(handle)
    value = body.get('data', {}).get('id')
    print(value if value is not None else '')
except (OSError, ValueError, TypeError, AttributeError):
    print('')
PY
)"
printf '%s\n' "$conversation_status" > "$REMOTE_DIR/conversation-create.status"
printf '%s\n' "$conversation_id" > "$REMOTE_DIR/conversation-id.txt"
if [[ -z "$conversation_id" ]]; then
  printf '%s\n' 'conversation_missing' > "$REMOTE_DIR/error.txt"
  exit 3
fi

: > "$REMOTE_DIR/results.jsonl"
printf 'index\thttp_status\tduration_ms\ttool_count\tanswer_present\trun_id\tprompt\n' > "$REMOTE_DIR/results.tsv"
start_epoch="$(date +%s)"
index=0
prompts=(
  '我这家店现在有几个商品？'
  '库存里哪些商品需要补货？'
  '最近的销售情况帮我看一下。'
  '客户欠款现在有多少？'
  '最近收付款记录帮我理一下。'
  '这个月经营情况给我一个汇总。'
)

while (( $(date +%s) - start_epoch < DURATION_SECONDS )); do
  index=$((index + 1))
  prompt="${prompts[$(( (index - 1) % ${#prompts[@]} ))]}"
  request_body="$(PROMPT="$prompt" CONVERSATION_ID="$conversation_id" python3 - <<'PY'
import json
import os

print(json.dumps({
    'conversation_id': int(os.environ['CONVERSATION_ID']),
    'message': os.environ['PROMPT'],
    'stream': False,
}, ensure_ascii=False))
PY
)"
  output="$REMOTE_DIR/response-$(printf '%03d' "$index").json"
  started="$(date +%s%3N)"
  status="$(curl -sS --connect-timeout 10 --max-time 110 \
    -o "$output" \
    -w '%{http_code}' \
    -H "$AUTH_HEADER" \
    -H 'Content-Type: application/json' \
    -d "$request_body" \
    "$BASE_URL/v2/agent/chat" || true)"
  finished="$(date +%s%3N)"
  duration=$((finished - started))
  [[ "$status" =~ ^[0-9]+$ ]] || status=0
  metrics="$(python3 - "$output" <<'PY'
import json
import sys

try:
    with open(sys.argv[1], encoding='utf-8') as handle:
        body = json.load(handle)
    data = body.get('data') or {}
    run_id = data.get('run_id') or ''
    tool_calls = data.get('tool_calls') or []
    answer = data.get('answer') or ''
    print(f'{run_id or "-"}|{len(tool_calls) if isinstance(tool_calls, list) else 0}|{str(bool(answer)).lower()}')
except (OSError, ValueError, TypeError, AttributeError):
    print('-|0|false')
PY
)"
  IFS='|' read -r run_id tool_count answer_present <<< "$metrics"
  INDEX="$index" PROMPT="$prompt" STATUS="$status" DURATION_MS="$duration" \
    TOOL_COUNT="$tool_count" ANSWER_PRESENT="$answer_present" RUN_ID="$run_id" \
    python3 - <<'PY' >> "$REMOTE_DIR/results.jsonl"
import json
import os

print(json.dumps({
    'index': int(os.environ['INDEX']),
    'prompt': os.environ['PROMPT'],
    'http_status': int(os.environ['STATUS']),
    'duration_ms': int(os.environ['DURATION_MS']),
    'tool_count': int(os.environ['TOOL_COUNT']),
    'answer_present': os.environ['ANSWER_PRESENT'] == 'true',
    'run_id': os.environ['RUN_ID'],
}, ensure_ascii=False))
PY
  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
    "$index" "$status" "$duration" "$tool_count" "$answer_present" "$run_id" "$prompt" \
    >> "$REMOTE_DIR/results.tsv"
  printf 'SOAK_SAMPLE\t%s\t%s\t%s\t%s\n' "$index" "$status" "$duration" "$tool_count"
done

printf '%s\n' "$(date -u +%FT%TZ)" > "$REMOTE_DIR/completed-at.txt"
printf 'SOAK_DONE\t%s\n' "$index"
