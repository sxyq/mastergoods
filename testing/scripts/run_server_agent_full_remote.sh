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
if [[ -z "$SSH_PASSWORD" ]]; then
  printf '%s\n' 'SSH_PASSWORD is required' >&2
  exit 2
fi

RUN_ID="${RUN_ID:-agent-full-current-$(date +%Y%m%dT%H%M%S%z)}"
REMOTE_DIR="${REMOTE_DIR:-/tmp/${RUN_ID}}"
LOCAL_SCRIPT="${ROOT_DIR}/testing/scripts/run_server_agent_all_tools.py"
JAR_PATH="${JAR_PATH:-${ROOT_DIR}/tmp/build/gradle-output/backend/libs/zhihuiji-backend-0.1.0.jar}"
if [[ -z "${TEST_SOURCE:-}" ]]; then
    if [[ -f "$JAR_PATH" ]]; then
        TEST_SOURCE="local current JAR sha256 $(shasum -a 256 "$JAR_PATH" | awk '{print $1}')"
    else
        TEST_SOURCE="local current source; JAR not found at ${JAR_PATH}"
    fi
fi
mkdir -p "$EVIDENCE_ROOT"
export ROOT_DIR SSH_KEY JUMP_HOST TARGET_HOST SSH_PASSWORD RUN_ID REMOTE_DIR EVIDENCE_ROOT TEST_SOURCE

expect <<'EXPECT'
set timeout 3600
log_user 0
set key $env(SSH_KEY)
set jump $env(JUMP_HOST)
set target $env(TARGET_HOST)
set remote_dir $env(REMOTE_DIR)
set local_script [file join $env(ROOT_DIR) testing scripts run_server_agent_all_tools.py]
set evidence_root $env(EVIDENCE_ROOT)
set run_id $env(RUN_ID)
if {[info exists env(CASE_FILTER)]} {
    set case_filter $env(CASE_FILTER)
} else {
    set case_filter ""
}
set test_source $env(TEST_SOURCE)
set proxy "ProxyCommand=ssh -i $key -o BatchMode=yes -o ConnectTimeout=10 -o StrictHostKeyChecking=accept-new -W %h:%p $jump"

proc run_password_command {command} {
    global env
    spawn sh -c $command
    expect {
        -re {[Pp]assword:} {
            send -- "$env(SSH_PASSWORD)\r"
            exp_continue
        }
        -re {Permission denied} { exit 13 }
        eof { }
        timeout { exit 124 }
    }
}

run_password_command [format {ssh -T -o StrictHostKeyChecking=accept-new -o PreferredAuthentications=password -o PubkeyAuthentication=no -o "%s" root@%s "rm -rf '%s'; mkdir -p '%s'"} $proxy $target $remote_dir $remote_dir]

spawn scp -q -o StrictHostKeyChecking=accept-new -o "$proxy" $local_script root@$target:$remote_dir/run_server_agent_all_tools.py
expect {
    -re {[Pp]assword:} { send -- "$env(SSH_PASSWORD)\r"; exp_continue }
    -re {Permission denied} { exit 13 }
    eof { }
    timeout { exit 124 }
}

set remote_command [format {set +e; : > '%s/provider.log'; (docker logs -f --since 1s zhihuiji154-backend > '%s/provider.log' 2>&1) & log_pid=\$!; export BASE_URL='http://127.0.0.1:18080'; export EVIDENCE_ROOT='%s/evidence'; export PROVIDER_EVIDENCE_PATH='%s/provider.log'; export DB_CONTAINER='zhihuiji154-postgres'; export DB_USER='zhihuiji'; export DB_NAME='zhihuiji'; export TEST_HOST='154.217.241.207'; export TEST_RUNTIME='154 production backend'; export TEST_DATABASE='zhihuiji'; export TEST_SOURCE='%s'; export TEST_ACCOUNT_SOURCE='owner_user_id=7 active session from 154 database'; export TEST_OWNER_USER_ID='7'; export CASE_FILTER='%s'; export RUN_ID='%s'; export AGENT_LLM_MODEL='deepseek-v4-flash'; export AGENT_LLM_BASE_URL='https://tokenrhythm.studio/v1'; export AGENT_LLM_WIRE_API='chat_completions'; export AGENT_TOOL_CHOICE='auto'; python3 '%s/run_server_agent_all_tools.py' > '%s/runner.log' 2>&1; rc=\$?; kill \$log_pid 2>/dev/null || true; echo RUNNER_EXIT_CODE=\$rc; exit 0} $remote_dir $remote_dir $remote_dir $remote_dir $test_source $case_filter $run_id $remote_dir $remote_dir]

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

printf 'run_id=%s\n' "$RUN_ID"
printf 'evidence_root=%s\n' "$EVIDENCE_ROOT"
printf 'host=%s\nowner_user_id=7\nmodel=deepseek-v4-flash\nwire_api=chat_completions\ntool_choice=auto\n' "$TARGET_HOST" > "$EVIDENCE_ROOT/run-meta.txt"
