#!/usr/bin/env bash
set -u

BASE_URL="${REMOTE_BASE_URL:-http://127.0.0.1:18080}"
DB_CONTAINER="${DB_CONTAINER:-zhihuiji154-postgres}"
DB_USER="${DB_USER:-zhihuiji}"
DB_NAME="${DB_NAME:-zhihuiji}"
OWNER_ID="${OWNER_ID:-7}"
OTHER_OWNER_ID="${OTHER_OWNER_ID:-8}"
RUN_ID="${RUN_ID:-sync-scope-$(date -u +%Y%m%dT%H%M%SZ)}"
CLIENT_ID="sync-evidence-${RUN_ID}"

psql_query() {
    docker exec "${DB_CONTAINER}" psql -U "${DB_USER}" -d "${DB_NAME}" -At -c "$1"
}

b64() {
    printf '%s' "$1" | base64 | tr -d '\n\r'
}

emit_meta() {
    local counts scopes
    counts="$(psql_query "select json_build_object(
        'users',(select count(*) from users),
        'sessions',(select count(*) from sessions),
        'stores',(select count(*) from stores),
        'store_memberships',(select count(*) from store_memberships),
        'products',(select count(*) from products),
        'customers',(select count(*) from customers),
        'suppliers',(select count(*) from suppliers),
        'sale_orders',(select count(*) from sale_orders),
        'purchase_orders',(select count(*) from purchase_orders),
        'finance_records',(select count(*) from finance_records),
        'inventory_snapshots',(select count(*) from inventory_snapshots),
        'inventory_ledger',(select count(*) from inventory_ledger),
        'accounts',(select count(*) from accounts),
        'payments',(select count(*) from payments),
        'agent_conversations',(select count(*) from agent_conversations),
        'agent_messages',(select count(*) from agent_messages),
        'agent_run_audits',(select count(*) from agent_run_audits),
        'agent_run_audit_events',(select count(*) from agent_run_audit_events),
        'agent_drafts',(select count(*) from agent_drafts),
        'sync_operation_log',(select count(*) from sync_operation_log),
        'sync_change_log',(select count(*) from sync_change_log),
        'sync_tombstones',(select count(*) from sync_tombstones)
    );")"
    scopes="$(psql_query "select coalesce(json_agg(row_to_json(scope_row)), '[]'::json) from (
        select s.owner_user_id, s.id as store_id, count(sm.id) as member_count,
               count(*) filter (where sm.status = 1) as enabled_member_count
        from stores s left join store_memberships sm on sm.store_id = s.id
        group by s.owner_user_id, s.id order by s.owner_user_id, s.id
    ) scope_row;")"
    printf 'META\t%s\t%s\n' "$(b64 "${counts}")" "$(b64 "${scopes}")"
}

token_for() {
    psql_query "select token from sessions where user_id = $1 and is_active = true and expires_at > (extract(epoch from now()) * 1000)::bigint order by id desc limit 1;" | head -n 1
}

request() {
    local label="$1" token="$2" method="$3" path="$4" payload="${5:-}"
    local body_file status request_b64 response_b64
    body_file="$(mktemp)"
    request_b64="$(b64 "${payload}")"
    if [[ -n "${payload}" ]]; then
        status="$(curl -sS --connect-timeout 10 --max-time 90 -o "${body_file}" -w '%{http_code}' \
            -X "${method}" -H "Authorization: Bearer ${token}" -H 'Content-Type: application/json' \
            -d "${payload}" "${BASE_URL}${path}" || true)"
    else
        status="$(curl -sS --connect-timeout 10 --max-time 90 -o "${body_file}" -w '%{http_code}' \
            -X "${method}" -H "Authorization: Bearer ${token}" "${BASE_URL}${path}" || true)"
    fi
    response_b64="$(base64 < "${body_file}" | tr -d '\n\r')"
    printf 'REQUEST\t%s\t%s\t%s\t%s\t%s\t%s\n' \
        "${label}" "${method}" "${path}" "${status}" "${request_b64}" "${response_b64}"
    rm -f "${body_file}"
}

TOKEN_OWNER="$(token_for "${OWNER_ID}")"
TOKEN_OTHER="$(token_for "${OTHER_OWNER_ID}")"
if [[ -z "${TOKEN_OWNER}" || -z "${TOKEN_OTHER}" ]]; then
    printf 'ERROR\t%s\n' "$(b64 'active token missing for owner 7 or owner 8')"
    exit 2
fi

emit_meta
request "owner_me" "${TOKEN_OWNER}" GET /v2/auth/users/me
request "owner_store_context" "${TOKEN_OWNER}" GET /v2/stores/current
request "owner_store_members" "${TOKEN_OWNER}" GET /v2/stores/current/members
request "owner_sync_health" "${TOKEN_OWNER}" GET /v2/sync/health
request "owner_cursor_initial" "${TOKEN_OWNER}" GET "/v2/sync/cursor/${CLIENT_ID}"
request "owner_pull_initial" "${TOKEN_OWNER}" POST /v2/sync/pull "{\"client_id\":\"${CLIENT_ID}\",\"since_cursor\":\"seq:0\",\"limit\":5}"
request "invalid_token_me" "invalid-token-for-evidence" GET /v2/auth/users/me

request "other_owner_me" "${TOKEN_OTHER}" GET /v2/auth/users/me
request "other_owner_store_context" "${TOKEN_OTHER}" GET /v2/stores/current
request "other_owner_sync_health" "${TOKEN_OTHER}" GET /v2/sync/health
AGENT_TITLE="sync-scope-agent-${RUN_ID}"
request "other_owner_agent_create_conversation" "${TOKEN_OTHER}" POST /v2/agent/conversations "{\"title\":\"${AGENT_TITLE}\",\"status\":\"active\"}"
OTHER_AGENT_CONVERSATION_ID="$(psql_query "select id from agent_conversations where owner_user_id = ${OTHER_OWNER_ID} and title = '${AGENT_TITLE}' order by id desc limit 1;" | head -n 1)"
if [[ -n "${OTHER_AGENT_CONVERSATION_ID}" ]]; then
    request "other_owner_agent_empty_scope" "${TOKEN_OTHER}" POST /v2/agent/chat "{\"conversation_id\":${OTHER_AGENT_CONVERSATION_ID},\"message\":\"我这家店现在有几个商品？\",\"stream\":false}"
    request "other_owner_agent_delete_conversation" "${TOKEN_OTHER}" DELETE "/v2/agent/conversations/${OTHER_AGENT_CONVERSATION_ID}"
else
    printf 'ERROR\t%s\n' "$(b64 'agent conversation id missing for owner 8')"
fi

PRODUCT_ID="$(psql_query "select id from products where owner_user_id = ${OWNER_ID} order by id limit 1;" | head -n 1)"
if [[ -n "${PRODUCT_ID}" ]]; then
    request "other_owner_product_list" "${TOKEN_OTHER}" GET /v2/products
    request "other_owner_cannot_read_owner_product" "${TOKEN_OTHER}" GET "/v2/products/${PRODUCT_ID}"
fi

TMP_ID="$(date +%s%3N)"
NOW_MS="$(date +%s%3N)"
TMP_CODE="SYNC-EVIDENCE-${RUN_ID}"
TMP_PAYLOAD="{\"id\":${TMP_ID},\"code\":\"${TMP_CODE}\",\"name\":\"同步证据临时商品\",\"category\":\"默认分类\",\"unit\":\"件\",\"sale_price\":1.23,\"purchase_price\":0.5,\"stock\":0,\"safe_stock\":0,\"status\":1,\"sync_status\":0,\"sync_version\":0,\"created_at\":${NOW_MS},\"updated_at\":${NOW_MS}}"
TMP_PAYLOAD_ESCAPED="$(printf '%s' "${TMP_PAYLOAD}" | sed 's/\\/\\\\/g; s/"/\\"/g')"
CREATE_OP="${RUN_ID}-create"
DELETE_OP="${RUN_ID}-delete"
CONFLICT_OP="${RUN_ID}-conflict"
UPLOAD_CREATE="{\"client_id\":\"${CLIENT_ID}\",\"changes\":[{\"operation_id\":\"${CREATE_OP}\",\"entity_type\":\"product\",\"entity_id\":\"${TMP_ID}\",\"operation\":\"upsert\",\"payload\":\"${TMP_PAYLOAD_ESCAPED}\",\"updated_at\":${NOW_MS},\"base_version\":0}],\"last_sync_cursor\":\"seq:0\"}"
request "sync_upload_apply" "${TOKEN_OWNER}" POST /v2/sync/upload "${UPLOAD_CREATE}"
request "sync_upload_duplicate" "${TOKEN_OWNER}" POST /v2/sync/upload "${UPLOAD_CREATE}"

SERVER_PRODUCT_ID="$(psql_query "select id from products where owner_user_id = ${OWNER_ID} and code = '${TMP_CODE}' order by id desc limit 1;" | head -n 1)"
if [[ -z "${SERVER_PRODUCT_ID}" ]]; then
    SERVER_PRODUCT_ID=0
fi
printf 'INFO\tserver_product_id\t%s\n' "${SERVER_PRODUCT_ID}"
request "other_owner_cannot_read_temp_product" "${TOKEN_OTHER}" GET "/v2/products/${SERVER_PRODUCT_ID}"

CONFLICT_PAYLOAD="{\"id\":${SERVER_PRODUCT_ID},\"code\":\"${TMP_CODE}\",\"name\":\"同步证据冲突版本\",\"category\":\"默认分类\",\"unit\":\"件\",\"sale_price\":2.34,\"purchase_price\":0.5,\"stock\":0,\"safe_stock\":0,\"status\":1,\"sync_status\":0,\"sync_version\":0,\"created_at\":${NOW_MS},\"updated_at\":${NOW_MS}}"
CONFLICT_ESCAPED="$(printf '%s' "${CONFLICT_PAYLOAD}" | sed 's/\\/\\\\/g; s/"/\\"/g')"
UPLOAD_CONFLICT="{\"client_id\":\"${CLIENT_ID}\",\"changes\":[{\"operation_id\":\"${CONFLICT_OP}\",\"entity_type\":\"product\",\"entity_id\":\"${SERVER_PRODUCT_ID}\",\"operation\":\"upsert\",\"payload\":\"${CONFLICT_ESCAPED}\",\"updated_at\":${NOW_MS},\"base_version\":0}],\"last_sync_cursor\":\"seq:0\"}"
request "sync_upload_version_conflict" "${TOKEN_OWNER}" POST /v2/sync/upload "${UPLOAD_CONFLICT}"

DELETE_PAYLOAD_ESCAPED="{\\\"id\\\":${SERVER_PRODUCT_ID}}"
DELETE_NOW="$(date +%s%3N)"
UPLOAD_DELETE="{\"client_id\":\"${CLIENT_ID}\",\"changes\":[{\"operation_id\":\"${DELETE_OP}\",\"entity_type\":\"product\",\"entity_id\":\"${SERVER_PRODUCT_ID}\",\"operation\":\"delete\",\"payload\":\"${DELETE_PAYLOAD_ESCAPED}\",\"updated_at\":${DELETE_NOW},\"base_version\":1}],\"last_sync_cursor\":\"seq:0\"}"
request "sync_upload_delete_tombstone" "${TOKEN_OWNER}" POST /v2/sync/upload "${UPLOAD_DELETE}"
request "other_owner_pull_isolated" "${TOKEN_OTHER}" POST /v2/sync/pull "{\"client_id\":\"${CLIENT_ID}-other\",\"since_cursor\":\"seq:0\",\"limit\":500}"
request "owner_pull_temp_history" "${TOKEN_OWNER}" POST /v2/sync/pull "{\"client_id\":\"${CLIENT_ID}\",\"since_cursor\":\"seq:0\",\"limit\":500}"
request "owner_cursor_ack" "${TOKEN_OWNER}" POST /v2/sync/cursor/ack "{\"client_id\":\"${CLIENT_ID}\",\"cursor\":\"seq:0\"}"

cleanup_status="$(psql_query "delete from sync_change_log where operation_id in ('${CREATE_OP}','${DELETE_OP}','${CONFLICT_OP}'); delete from sync_operation_log where operation_id in ('${CREATE_OP}','${DELETE_OP}','${CONFLICT_OP}'); delete from sync_tombstones where owner_user_id = ${OWNER_ID} and entity_type = 'product' and entity_id in ('${TMP_ID}','${SERVER_PRODUCT_ID}'); delete from products where owner_user_id = ${OWNER_ID} and (id = ${SERVER_PRODUCT_ID} or code = '${TMP_CODE}'); delete from sync_cursors where owner_user_id = ${OWNER_ID} and client_id = '${CLIENT_ID}'; select json_build_object('temp_product_count', (select count(*) from products where owner_user_id = ${OWNER_ID} and (id = ${SERVER_PRODUCT_ID} or code = '${TMP_CODE}')), 'operation_log_count', (select count(*) from sync_operation_log where owner_user_id = ${OWNER_ID} and operation_id in ('${CREATE_OP}','${DELETE_OP}','${CONFLICT_OP}')), 'change_log_count', (select count(*) from sync_change_log where owner_user_id = ${OWNER_ID} and operation_id in ('${CREATE_OP}','${DELETE_OP}','${CONFLICT_OP}')), 'tombstone_count', (select count(*) from sync_tombstones where owner_user_id = ${OWNER_ID} and entity_type = 'product' and entity_id in ('${TMP_ID}','${SERVER_PRODUCT_ID}')), 'cursor_count', (select count(*) from sync_cursors where owner_user_id = ${OWNER_ID} and client_id = '${CLIENT_ID}'));" | tail -n 1)"
printf 'CLEANUP\t%s\n' "$(b64 "${cleanup_status}")"
emit_meta
