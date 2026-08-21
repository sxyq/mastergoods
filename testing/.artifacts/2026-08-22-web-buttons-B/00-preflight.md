# Web buttons Agent B preflight

- Date: 2026-08-22, Asia/Shanghai
- Scope: Web page, button, interaction-entry review only. No business source, configuration, database, deployment object, `data/server-backups/`, or `data/server-exports/` was opened or changed.
- Git branch: `codex/publish-local-updates`, ahead of `origin/codex/publish-local-updates` by 5 commits.
- Initial worktree status showed pre-existing backend/config/test changes and untracked `data/server-backups/` and `data/server-exports/`; they were preserved and were not staged. A later status check did not list the two data directories; no action was taken on them.
- Initial diff summary: 30 tracked files changed, 502 insertions, 81 deletions. This is pre-existing work and is outside this commit.

## Runtime

| Check | Result |
|---|---|
| `node --version` in default shell | command not found |
| `npm --version` in default shell | command not found |
| `command -v npx` | no result; unavailable |
| Bundled Node | `/Users/sunyiyang/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin/node`, v24.19.0 |
| Vite | 4.5.14, direct invocation through existing symlinked `node_modules` |
| vue-tsc | 5.8.3 executable present; no project compile was claimed |
| Web package | `zhihuiji-web-admin` 0.1.0, lockfileVersion 3 |
| Web node_modules | symlink to `tmp/build/web/node_modules`; `.bin` contains Vite, vue-tsc, TypeScript and related executables |

Web dependencies recorded from `Code/frontend/web/package.json` and the lockfile: `@vitejs/plugin-vue ^4.6.2`, `vite ^4.5.14`, `vue ^3.5.34`, `vue-router ^4.6.4`, `@types/node ^24.12.3`, `@vue/tsconfig ^0.9.1`, `typescript ~5.8.3`, `vue-tsc ^3.2.8`.

## Backend targets

- Web development default from `Code/frontend/web/src/shared/api/config.ts:1`: `http://127.0.0.1:18080`.
- `127.0.0.1:18080/v2/agent/workbench`: connection refused; no local Spring target was available.
- `127.0.0.1:8080/v2/agent/workbench`: HTTP 401, `text/html`, Cheroot Digest realm; treated as an unrelated protected service.
- Historical endpoint probe `https://sxyq27.online/zhj-api/v2/agent/workbench`: HTTP 410. No login request or credential was sent.
- No authenticated Web account/store context was available. No owner, token, cookie, or password was recorded.

## Write boundary

This evidence directory and the companion files under `testing/web/功能测试/` are the only intended outputs of this phase. No source path under `Code/frontend/web/src` was modified.
