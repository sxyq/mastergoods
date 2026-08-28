# Remote environment probe

- `test_id`: `AG-S-REMOTE-ENV-001`
- `category_id`: `AG-S-ENV-REMOTE`
- `wave_id`: `Wave 2`
- `result`: `Blocked`
- `environment`: user-provided HTTPS development endpoint; TLS handshake and HTTP response completed; no authentication header, Cookie, token, password, or Provider credential supplied.
- `source_commit`: `a65f9475` evidence commit; application source is the code at its parent `93d08542`.
- `android`: `emulator-5554`, package `com.zhihuiji.app`, foreground `MainActivity`; UI tree shows the login form, not an authenticated Agent conversation.
- `local_service`: the lazy-init local `18080` process was stopped after its anonymous probe; see `testing/Agent/功能/reports/20260829-agent-live-wave0-start-failed.md` and the lazy-init logs.
- `account_store`: `anonymous / redacted`; no development account password was entered or extracted.

The target was probed only over its user-supplied HTTPS development host. The host name and full URL are intentionally omitted from stored evidence.
