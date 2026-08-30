test_id: AG-I-012
category_id: AG-I-012
wave_id: 20260831-agent-live-05-C-W0
environment: remote 8220 / 8.220.206.9 / production compose
account_store_label: account=unbound; store=unbound
precondition: D context-state caller patch is not yet available; deployment is intentionally paused.
redacted_input: read-only infrastructure probes only; no Agent prompt and no credential input.
operation: SSH with the existing local key as root; inspect the fixed application directory, compose metadata, container status, safe image fields, provider-key presence, public status, PostgreSQL readiness, migration marker, and selected table counts.
expected_tools_order: none; no Agent tool planner or executor was invoked.
loop_compaction: not exercised; no model request was sent.
http_sse: public GET requests only; Agent REST/SSE was not called.
formal_answer: not applicable; no Agent answer was generated.
db_before: captured in 06-database-before.json; read-only counts only.
db_after: no mutation; no cleanup or write operation was executed.
boundary: remote source directory has no .git metadata; current container image is the pre-D image; runtime.env was not read.
acceptance: containers and PostgreSQL are reachable; migration marker is 41; agent tables are present and all selected counts are 0; deployment source gate remains unresolved.
evidence: 00-environment.md, 02-http-response.json, 06-database-before.json, 10-conclusion.md.
cleanup: none; no test data was created by this wave.
result: Blocked

Observed remote state:

- SSH authentication succeeded with the existing local RSA key; no key content was printed.
- Docker, Nginx, `sxyq27-zhj-api`, `sxyq27-zhj-postgres`, and `sxyq27-zhj-redis` were active. PostgreSQL was healthy and accepting connections.
- Compose root: `/opt/sxyq27/master-goods/compose.yml`; backend container image: `sxyq27-zhj-api:20260830T183500-admin-usage-f8361754`; image digest was captured in the operator command output but is intentionally not duplicated here.
- The remote directory exists but has no `.git` metadata, so no remote source commit can be verified. The active image is therefore not attributable to A/B/D from the remote checkout.
- Safe container environment inspection found `SPRING_PROFILES_ACTIVE=prod` and presence of protected configuration keys. `AGENT_MODEL` and `AGENT_CONTEXT_MAXIMUM_WINDOW` were absent from the container environment. No secret values were read or stored.
- PostgreSQL database `zhj` accepted a local read-only connection as role `zhj`; the latest Flyway marker was version `41` with success `true`. Agent and media table counts are recorded in `06-database-before.json`.
- Public TLS requests to `/` and `/actuator/health` returned HTTP 401 with JSON content type. No response body was saved.

This record does not authorize deployment, cleanup, migration, model calls, or test-data creation. Deployment remains gated on D and must use the eventual A/B/D merge source.
