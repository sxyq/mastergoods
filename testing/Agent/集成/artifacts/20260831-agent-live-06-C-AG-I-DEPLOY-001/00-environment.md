test_id: AG-I-DEPLOY-001
category_id: I
wave_id: 20260831-agent-live-06-C-W0

Environment:

- source commit: `1e79277630fe298d40c946d703f49faa667348a0` (祖先包含 A `ef6b1317`、B `77b19a2b`、D `1e792776`)
- evidence HEAD at capture: `ac0e3b0b688c0732c8dc7e364fb4214eff46a01f`
- build source: clean `git archive` of D in `/tmp`; current worktree modifications were not included
- backend artifact: `zhihuiji-backend-0.1.0.jar`, SHA-256 `d67844d8b1c2b73bf8f91ce8e12e09d40cdef97343550a03931396454805a8bb`
- server: `8.220.206.9`, `/opt/sxyq27/master-goods`, PostgreSQL 15.19, Redis 7
- server release: `/opt/sxyq27/master-goods/releases/20260831T094606-C-abd-owner-web-agent/`
- image: `sxyq27-zhj-api:20260831T094606-C-abd-owner-web-agent`, running, restart count 0
- Flyway latest successful version: `41`
- effective non-secret Agent config: enabled `true`, model `glm-5.3-flash`, context maximum `272000`, wire API `chat_completions`; API key key exists, value not read
- public API root: HTTP `401` without saving response headers/body
- account/store label: `account=unbound; owner=2 scope; store=redacted`; no login or Agent request was attempted
- cleanup: none; existing owner=2 business fixtures were preserved
- admin scope: `Code/frontend/admin-web` was not uploaded or built for this deployment

This artifact validates the backend deployment boundary only. It does not claim a real authenticated Agent run, tool call, SSE stream, or formal answer.
