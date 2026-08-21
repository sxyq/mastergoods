#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
EVIDENCE_ROOT="${EVIDENCE_ROOT:?EVIDENCE_ROOT is required}"
SSH_KEY="${SSH_KEY:-/Users/sunyiyang/Downloads/234.pem}"
JUMP_HOST="${JUMP_HOST:-ubuntu@124.222.153.108}"
TARGET_HOST="${TARGET_HOST:-154.217.241.207}"
if [[ -z "${SSH_PASSWORD:-}" ]]; then
  SSH_PASSWORD="$(awk '/^  - password: / { line=$0; sub(/^  - password: /,"",line); gsub(sprintf("%c",96),"",line); print line; exit }' \
    /Users/sunyiyang/.codex/skills/sxyq27-server-maintenance/references/local-credentials.md)"
fi
if [[ -z "${SSH_PASSWORD}" ]]; then
  printf '%s\n' 'SSH_PASSWORD is required' >&2
  exit 2
fi

RUN_ID="${RUN_ID:-agent-stream-concurrency-$(date +%Y%m%dT%H%M%S%z)}"
REMOTE_DIR="${REMOTE_DIR:-/tmp/${RUN_ID}}"
LOCAL_SCRIPT="${ROOT_DIR}/testing/scripts/run_server_agent_stream_concurrency.py"
CONCURRENCY="${CONCURRENCY:-10}"
mkdir -p "${EVIDENCE_ROOT}"
export EVIDENCE_ROOT SSH_KEY JUMP_HOST TARGET_HOST SSH_PASSWORD RUN_ID REMOTE_DIR LOCAL_SCRIPT CONCURRENCY

expect <<'EXPECT'
set timeout 3600
log_user 0
set key $env(SSH_KEY)
set jump $env(JUMP_HOST)
set target $env(TARGET_HOST)
set remote_dir $env(REMOTE_DIR)
set local_script $env(LOCAL_SCRIPT)
set evidence_root $env(EVIDENCE_ROOT)
set concurrency $env(CONCURRENCY)
set proxy "ProxyCommand=ssh -i $key -o BatchMode=yes -o ConnectTimeout=10 -o StrictHostKeyChecking=accept-new -W %h:%p $jump"

proc run_password_command {command} {
    global env
    spawn sh -c $command
    expect {
        -re {[Pp]assword:} { send -- "$env(SSH_PASSWORD)\r"; exp_continue }
        -re {Permission denied} { exit 13 }
        eof { }
        timeout { exit 124 }
    }
}

run_password_command [format {ssh -T -o StrictHostKeyChecking=accept-new -o PreferredAuthentications=password -o PubkeyAuthentication=no -o "%s" root@%s "rm -rf '%s'; mkdir -p '%s'"} $proxy $target $remote_dir $remote_dir]

spawn scp -q -o StrictHostKeyChecking=accept-new -o "$proxy" $local_script root@$target:$remote_dir/run_server_agent_stream_concurrency.py
expect {
    -re {[Pp]assword:} { send -- "$env(SSH_PASSWORD)\r"; exp_continue }
    -re {Permission denied} { exit 13 }
    eof { }
    timeout { exit 124 }
}

set remote_command [format {set +e; export BASE_URL='http://127.0.0.1:18080'; export OUTPUT_ROOT='%s/evidence'; export TOKEN=\$(docker exec zhihuiji154-postgres psql -U zhihuiji -d zhihuiji -At -c 'select token from sessions where user_id = 7 and is_active = true and expires_at > (extract(epoch from now()) * 1000)::bigint order by id desc limit 1;' | head -n 1); if [ -z "\$TOKEN" ]; then echo TOKEN_NOT_FOUND > '%s/runner.log'; rc=2; else python3 '%s/run_server_agent_stream_concurrency.py' --concurrency '%s' > '%s/runner.log' 2>&1; rc=\$?; fi; echo RUNNER_EXIT_CODE=\$rc >> '%s/runner.log'; exit 0} $remote_dir $remote_dir $remote_dir $concurrency $remote_dir $remote_dir]
spawn sh -c [format {ssh -T -o StrictHostKeyChecking=accept-new -o PreferredAuthentications=password -o PubkeyAuthentication=no -o "%s" root@%s "%s"} $proxy $target $remote_command]
expect {
    -re {[Pp]assword:} { send -- "$env(SSH_PASSWORD)\r"; exp_continue }
    -re {Permission denied} { exit 13 }
    eof { }
    timeout { exit 124 }
}

spawn scp -q -r -o StrictHostKeyChecking=accept-new -o "$proxy" root@$target:$remote_dir "$evidence_root"
expect {
    -re {[Pp]assword:} { send -- "$env(SSH_PASSWORD)\r"; exp_continue }
    -re {Permission denied} { exit 13 }
    eof { }
    timeout { exit 124 }
}

run_password_command [format {ssh -T -o StrictHostKeyChecking=accept-new -o PreferredAuthentications=password -o PubkeyAuthentication=no -o "%s" root@%s "rm -rf '%s'"} $proxy $target $remote_dir]
EXPECT

printf 'run_id=%s\n' "${RUN_ID}"
printf 'evidence_root=%s\n' "${EVIDENCE_ROOT}"
printf 'host=%s\nconcurrency=%s\nmodel=deepseek-v4-flash\nwire_api=chat_completions\ntool_choice=auto\n' "${TARGET_HOST}" "${CONCURRENCY}" > "${EVIDENCE_ROOT}/concurrency-run-meta.txt"
