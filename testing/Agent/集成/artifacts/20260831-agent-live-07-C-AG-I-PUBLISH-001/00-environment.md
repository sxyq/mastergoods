test_id: AG-I-PUBLISH-001
category_id: I
wave_id: 20260831-agent-live-07-C-W0

Release environment:

- direct_connection: `true`; target host: `8.220.206.9`; jump host/ProxyCommand: `false`
- source reference: `HEAD=636be234` plus current dirty worktree; the dirty worktree was intentionally used for this release
- application source scope: `Code/backend`; `Code/frontend/web` built separately; `Code/frontend/android` and iOS are client-only
- current backend worktree change summary: 10 application files changed, 24 insertions, 190 deletions; exact redacted list in `11-source-files-redacted.txt` and summary in `12-git-diff-summary.md`
- backend build: `./Code/backend/gradlew -p Code/backend bootJar --no-daemon --console=plain`, successful
- backend JAR SHA-256: `06a77955eedb385cab31656bcc0c324de86bf3eede938725ada4e769616c8e9d`
- release: `/opt/sxyq27/master-goods/releases/20260831T112015-C-worktree-owner-agent/`
- image: `sxyq27-zhj-api:20260831T112015-C-worktree-owner-agent`, digest `sha256:7050b6ae8776db0ddfc9300de56a2c8243aae9f99029c381ac811968d5b8effc`
- container: `sxyq27-zhj-api` running, restart count `0`, port `127.0.0.1:18080`
- protected configuration observed by key/existence only: `AGENT_LLM_API_KEY=present`, `AGENT_LLM_BASE_URL=present`; effective model `glm-5.3-flash`, context maximum `272000`, wire API `chat_completions`, enabled `true`
- database: PostgreSQL 15.19, Flyway latest successful version `41`; Redis 7 remained running
- public API root anonymous status: HTTP `401`; headers/body were not saved
- rollback: prior image `sxyq27-zhj-api:20260831T094606-C-abd-owner-web-agent` remains present
- database mutation: none; existing business fixtures and protected configuration values were not read or changed
- management scope: `Code/frontend/admin-web`, management services and `newapi-usage-collector.service` excluded

This case covers current-dirty-worktree backend release verification only. No login, Agent prompt, HTTP Agent request, SSE stream, tool call, or client UI functional test was performed.
