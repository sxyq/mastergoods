# Agent Remote Wave 2 门禁证据

- target: `https://zhj-api.sxyq27.online/`
- probe: unauthenticated `GET`, no request body, no Authorization header, response body discarded
- `/v2/auth/users/me`: `403`
- `/v2/agent/chat`: `403`
- `/v2/agent/chat/stream`: `403`
- `AGENT_ACCESS_TOKEN`: absent (presence check only; value not read)
- `AGENT_BASE_URL`: absent (presence check only)
- authenticated Agent requests: `0`
- Agent POST requests: `0`
- SSE sessions: `0`
- Provider model requests: `0`
- database queries: `0`
- writes or cleanup mutations: `0`
- APP/adb validation: unavailable; no `adb` command

This file contains status-only evidence. No Token, Cookie, password, private key, or complete authentication payload was read, printed, or saved.
