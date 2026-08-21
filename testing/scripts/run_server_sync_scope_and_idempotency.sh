#!/usr/bin/env bash
set -u

: "${EVIDENCE_ROOT:?EVIDENCE_ROOT is required}"
: "${SSH_PASSWORD:?SSH_PASSWORD is required}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
export ROOT_DIR
mkdir -p "${EVIDENCE_ROOT}"
export REMOTE_BASE_URL="${REMOTE_BASE_URL:-http://127.0.0.1:18080}"
export DB_CONTAINER="${DB_CONTAINER:-zhihuiji154-postgres}"
export DB_USER="${DB_USER:-zhihuiji}"
export DB_NAME="${DB_NAME:-zhihuiji}"
export OWNER_ID="${OWNER_ID:-7}"
export OTHER_OWNER_ID="${OTHER_OWNER_ID:-8}"
export RUN_ID="${RUN_ID:-sync-scope-$(date -u +%Y%m%dT%H%M%SZ)}"
export SSH_KEY="${SSH_KEY:-/Users/sunyiyang/Downloads/234.pem}"
export JUMP_HOST="${JUMP_HOST:-ubuntu@124.222.153.108}"
export TARGET_HOST="${TARGET_HOST:-154.217.241.207}"

RAW_EVIDENCE="${EVIDENCE_ROOT}/remote-sync-scope.raw.tsv"
JSON_EVIDENCE="${EVIDENCE_ROOT}/remote-sync-scope.jsonl"

expect <<'EXPECT' > "${RAW_EVIDENCE}"
set timeout 300
log_user 0
set key $env(SSH_KEY)
set jump $env(JUMP_HOST)
set target $env(TARGET_HOST)
set script_path [file join $env(ROOT_DIR) testing scripts run_server_sync_scope_remote.sh]
set proxy "ProxyCommand=ssh -i $key -o BatchMode=yes -o ConnectTimeout=10 -o StrictHostKeyChecking=accept-new -W %h:%p $jump"
set command [format {ssh -o StrictHostKeyChecking=accept-new -o PreferredAuthentications=password -o PubkeyAuthentication=no -o '%s' root@%s bash -s < '%s'} $proxy $target $script_path]
spawn sh -c $command
expect {
    -re {[Pp]assword:} {
        send -- $env(SSH_PASSWORD)
        send -- "\r"
        log_user 1
        exp_continue
    }
    eof {}
    timeout {exit 124}
}
EXPECT

RUN_ID="${RUN_ID}" python3 - "${RAW_EVIDENCE}" "${JSON_EVIDENCE}" <<'PY'
import base64
import json
import os
import sys
from pathlib import Path

raw_path = Path(sys.argv[1])
json_path = Path(sys.argv[2])
run_id = os.environ.get("RUN_ID", "unknown")

def decode(value):
    if not value:
        return ""
    return base64.b64decode(value).decode("utf-8", errors="replace")

def parse_json(value):
    try:
        return json.loads(value)
    except (TypeError, json.JSONDecodeError):
        return value

with json_path.open("w", encoding="utf-8") as output:
    for line in raw_path.read_text(encoding="utf-8", errors="replace").splitlines():
        fields = line.split("\t")
        if not fields or not fields[0]:
            continue
        if fields[0] == "META" and len(fields) == 3:
            output.write(json.dumps({
                "record_type": "meta",
                "run_id": run_id,
                "host": "154.217.241.207",
                "database": "zhihuiji",
                "owner_user_id": 7,
                "counts": parse_json(decode(fields[1])),
                "store_scopes": parse_json(decode(fields[2])),
            }, ensure_ascii=False) + "\n")
        elif fields[0] == "REQUEST" and len(fields) == 7:
            output.write(json.dumps({
                "record_type": "request",
                "case": fields[1],
                "method": fields[2],
                "path": fields[3],
                "http_status": int(fields[4]) if fields[4].isdigit() else 0,
                "request": parse_json(decode(fields[5])),
                "response": parse_json(decode(fields[6])),
            }, ensure_ascii=False) + "\n")
        elif fields[0] == "CLEANUP" and len(fields) == 2:
            output.write(json.dumps({
                "record_type": "cleanup",
                "run_id": run_id,
                "cleanup": parse_json(decode(fields[1])),
            }, ensure_ascii=False) + "\n")
        elif fields[0] == "INFO" and len(fields) == 3:
            output.write(json.dumps({
                "record_type": "info",
                "key": fields[1],
                "value": fields[2],
            }, ensure_ascii=False) + "\n")
        elif fields[0] == "ERROR" and len(fields) == 2:
            output.write(json.dumps({
                "record_type": "error",
                "reason": decode(fields[1]),
            }, ensure_ascii=False) + "\n")
        else:
            output.write(json.dumps({
                "record_type": "transport_output",
                "raw": line,
            }, ensure_ascii=False) + "\n")
PY

printf '%s\n' "run_id=${RUN_ID}" > "${EVIDENCE_ROOT}/run-meta.txt"
printf '%s\n' "remote_host=${TARGET_HOST}" >> "${EVIDENCE_ROOT}/run-meta.txt"
printf '%s\n' "remote_base_url=${REMOTE_BASE_URL}" >> "${EVIDENCE_ROOT}/run-meta.txt"
printf '%s\n' "script=testing/scripts/run_server_sync_scope_remote.sh" >> "${EVIDENCE_ROOT}/run-meta.txt"
