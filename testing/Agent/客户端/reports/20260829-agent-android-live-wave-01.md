# Android Agent Live Wave 01

- `wave_id`: `20260829-agent-android-live-wave-01`
- `category_id`: `CLI`
- `device`: `emulator-5554`, AVD `Zhihuiji_API34`, Android 14/API 34, 720x1280
- `app`: `com.zhihuiji.app` debug `1.0.0 (1)`, APK installed and launched
- `base_url`: `https://zhj-api.sxyq27.online/`
- `account_store_label`: `授权开发 fixture（脱敏） / 未建立当前门店会话`
- `backend_observation`: local `18080` had no listener; `127.0.0.1:8080` was excluded as Python WebDAV; anonymous remote probes returned 403; App normal login `POST /v1/auth/login` returned 422 twice.

## Wave result

| scope | Passed | Failed | Blocked | Deferred |
|---|---:|---:|---:|---:|
| device/build/install/launch | 4 | 0 | 0 | 0 |
| authenticated login and owner/store | 0 | 0 | 1 | 0 |
| Android client flows `AG-CLI-AND-001..010` | 0 | 0 | 10 | 0 |
| Agent tools | 0 | 0 | 61 | 0 |
| real Provider calls | 0 | 0 | 1 | 0 |

The authorized development fixture was entered only through the normal login UI. No credential, Cookie, Token, Authorization value, or complete authentication payload was saved. The App remained on the login page and displayed `请求参数未通过校验`; no Agent prompt was sent.

## Client flow status

| test_id | flow | result | evidence / reason |
|---|---|---|---|
| `AG-CLI-AND-001` | 登录与会话列表 | Blocked | login endpoint 422; no owner/store session |
| `AG-CLI-AND-002` | 单只读工具流式 | Blocked | authentication precondition |
| `AG-CLI-AND-003` | 多工具与图表 | Blocked | authentication precondition |
| `AG-CLI-AND-004` | 草稿确认/拒绝 | Blocked | authentication precondition |
| `AG-CLI-AND-005` | 历史恢复 | Blocked | authentication precondition |
| `AG-CLI-AND-006` | 取消与断线 | Blocked | no Agent run to cancel/reconnect |
| `AG-CLI-AND-007` | 压缩事件展示 | Blocked | no Agent run/context checkpoint |
| `AG-CLI-AND-008` | 错误与重试 | Blocked | only login error observed; Agent error matrix not reached |
| `AG-CLI-AND-009` | 后台/前台切换 | Blocked | no authenticated run |
| `AG-CLI-AND-010` | 生图草稿确认 | Blocked | no authenticated Agent run; Provider not called |

## Tool status

The complete 61-tool status is recorded in `20260829-agent-android-live-wave-01-tool-status.csv`. All 46 `READ_ONLY` tools and all 15 `CREATE_ONLY` tools are `Blocked` because the normal login did not establish a session. `image_generate` is included as `AG-F-DRAFT-CO-015`; it was not invoked and no Provider request was made.

No SSE event, tool name/order, formal answer, result block, draft transition, audit, database before/after, cancellation, retry, compaction, background/foreground recovery, or business write is claimed for this wave. The retry gate is an available authenticated development backend/session; after that gate, the same AVD and installed debug APK are ready.

Evidence:

- Wave 0 baseline: `testing/Agent/客户端/reports/20260829-agent-android-wave0-baseline.md`
- Login artifact: `testing/Agent/客户端/artifacts/20260829-agent-android-wave0-AG-CLI-AND-001/`
- Tool ledger: `testing/Agent/客户端/reports/20260829-agent-android-live-wave-01-tool-status.csv`
- crash-only log: `testing/Agent/客户端/logs/20260829-agent-android-wave0-crash.log`
