#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
EVIDENCE_ROOT="${EVIDENCE_ROOT:?EVIDENCE_ROOT is required}"
DURATION_SECONDS="${DURATION_SECONDS:-900}"
SSH_KEY="${SSH_KEY:-/Users/sunyiyang/Downloads/234.pem}"
JUMP_HOST="${JUMP_HOST:-ubuntu@124.222.153.108}"
TARGET_HOST="${TARGET_HOST:-154.217.241.207}"
PAYLOAD_PATH="${ROOT_DIR}/testing/scripts/run_server_agent_soak_remote_payload.sh"

if [[ -z "${SSH_PASSWORD:-}" ]]; then
  SSH_PASSWORD="$(sed -n 's/^  - password: //p' \
    /Users/sunyiyang/.codex/skills/sxyq27-server-maintenance/references/local-credentials.md \
    | sed -E 's/^`//; s/`$//' | tr -d '\r\n' | head -n 1)"
fi
if [[ -z "$SSH_PASSWORD" ]]; then
  printf '%s\n' 'SSH_PASSWORD is required' >&2
  exit 2
fi
if [[ ! -f "$PAYLOAD_PATH" ]]; then
  printf 'payload_missing=%s\n' "$PAYLOAD_PATH" >&2
  exit 2
fi

mkdir -p "$EVIDENCE_ROOT"
REMOTE_DIR="/tmp/mg-agent-soak-$(date -u +%Y%m%dT%H%M%SZ)-$$"
RAW_OUTPUT="${EVIDENCE_ROOT}/soak-transport.log"
export EVIDENCE_ROOT SSH_PASSWORD REMOTE_DIR DURATION_SECONDS SSH_KEY JUMP_HOST TARGET_HOST PAYLOAD_PATH

expect <<'EXPECT' > "$RAW_OUTPUT"
set timeout 1800
log_user 0
set key $env(SSH_KEY)
set jump $env(JUMP_HOST)
set target $env(TARGET_HOST)
set remote_dir $env(REMOTE_DIR)
set duration $env(DURATION_SECONDS)
set payload $env(PAYLOAD_PATH)
set proxy "ProxyCommand=ssh -i $key -o BatchMode=yes -o ConnectTimeout=10 -o StrictHostKeyChecking=accept-new -W %h:%p $jump"
set command [format {ssh -T -o StrictHostKeyChecking=accept-new -o PreferredAuthentications=password -o PubkeyAuthentication=no -o '%s' root@%s bash -s -- '%s' '%s' < '%s'} $proxy $target $remote_dir $duration $payload]
spawn sh -c $command
expect {
    -re {[Pp]assword:} {
        send -- "$env(SSH_PASSWORD)\r"
        log_user 1
        exp_continue
    }
    -re {Permission denied} { exit 13 }
    eof { }
    timeout { exit 124 }
}

spawn scp -r -o StrictHostKeyChecking=accept-new -o $proxy root@$target:$remote_dir "$env(EVIDENCE_ROOT)/"
expect {
    -re {[Pp]assword:} { send -- "$env(SSH_PASSWORD)\r"; exp_continue }
    -re {Permission denied} { exit 14 }
    eof { }
    timeout { exit 124 }
}

spawn ssh -T -o StrictHostKeyChecking=accept-new -o PreferredAuthentications=password -o PubkeyAuthentication=no -o $proxy root@$target "rm -rf '$remote_dir'"
expect {
    -re {[Pp]assword:} { send -- "$env(SSH_PASSWORD)\r"; exp_continue }
    eof { }
    timeout { exit 124 }
}
EXPECT

printf 'captured_at=%s\n' "$(date -u +%FT%TZ)" > "${EVIDENCE_ROOT}/soak-run-meta.txt"
printf 'duration_seconds=%s\n' "$DURATION_SECONDS" >> "${EVIDENCE_ROOT}/soak-run-meta.txt"
printf 'remote_host=%s\n' "$TARGET_HOST" >> "${EVIDENCE_ROOT}/soak-run-meta.txt"
printf 'database=zhihuiji\nowner_user_id=7\nmode=non_stream_read_only\n' >> "${EVIDENCE_ROOT}/soak-run-meta.txt"
