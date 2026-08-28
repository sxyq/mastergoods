# Remote Agent environment probe report

- `test_id`: `AG-S-REMOTE-ENV-001`
- `category_id`: `AG-S-ENV-REMOTE`
- `wave_id`: `Wave 2`
- `result`: `Blocked`
- Probe type: independent local HTTP client for the user-provided remote HTTPS development endpoint; it is not Android UI evidence.

## Confirmed

- TLS/HTTP access completed without supplying credentials.
- Anonymous `GET` health/root and Agent route probes returned `403`.
- Empty JSON `POST` probes to `/v1/auth/login` and `/v2/auth/login` returned `400`, with no session retained.
- Anonymous `POST` probes to chat, stream, image REST, and cancel returned `403`.
- Android emulator `emulator-5554` is online, but the app is on its login screen and filtered logcat contained no `run_id`.

## Not confirmed

- Authorized development environment status.
- Agent tool selection/execution, `image_generate`, SSE terminal event, run/audit linkage, owner/store scope, or database before/after.
- Any Provider behavior. No Provider call was made.

Detailed redacted evidence is in `testing/Agent/安全/artifacts/20260829-agent-remote-env-probe-01/`.
