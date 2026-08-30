# Android Agent Luna live test report

- wave_id: `20260830-luna-android-wave-01`
- requested model: `gpt-5.6-luna / max`; runtime未暴露可独立核对的模型标识
- source commit: `c4ae9b86dc8582e48613b43d6318c8ef3e02204e`
- device: `Zhihuiji_API34`, `emulator-5554`, Android 14/API 34, 720x1280
- app: `com.zhihuiji.app` debug `1.0.0 (1)`; current-worktree build and install passed
- App service URL: `https://zhj-api.sxyq27.online/`; host anonymous reachability probe returned HTTP 403

## Result

| scope | Passed | Failed | Blocked | Deferred |
|---|---:|---:|---:|---:|
| AVD boot and stable access | 1 | 0 | 0 | 0 |
| APK build/install/launch | 3 | 0 | 0 | 0 |
| App login and owner/store session | 0 | 0 | 1 | 0 |
| AG-CLI-AND-001..010 | 0 | 0 | 1 | 9 |
| Agent tools: 46 READ_ONLY + 15 CREATE_ONLY | 0 | 0 | 0 | 61 |

## Real requests

| request type | count | note |
|---|---:|---|
| host anonymous service probe | 1 | HTTP 403; response body not retained |
| App login | 0 | fixture did not enter the App |
| Agent | 0 | no authenticated run |
| tool | 0 | no tool event |
| image Provider | 0 | image flow not reached |

## Client flows

| test_id | flow | result | reason |
|---|---|---|---|
| `AG-CLI-AND-001` | 登录与会话列表 | `Blocked` | 已批准开发 fixture 未通过安全输入通道提供；未从日志、数据库、进程、网络或历史命令读取认证信息 |
| `AG-CLI-AND-002` | 单只读工具流式 | `Deferred` | 认证及 owner/store 会话未建立 |
| `AG-CLI-AND-003` | 多工具、Loop 与图表 | `Deferred` | 认证及 Agent run 前置未建立 |
| `AG-CLI-AND-004` | 草稿确认与拒绝 | `Deferred` | 认证及 Agent run 前置未建立 |
| `AG-CLI-AND-005` | 历史恢复 | `Deferred` | 没有可恢复的本批 Agent run |
| `AG-CLI-AND-006` | 取消、断线与重连 | `Deferred` | 没有可取消或重连的本批 Agent run |
| `AG-CLI-AND-007` | 上下文压缩 | `Deferred` | 没有达到上下文压缩条件的本批 Agent run |
| `AG-CLI-AND-008` | 错误与重试 | `Deferred` | 未建立认证会话，未进入 Agent 错误矩阵 |
| `AG-CLI-AND-009` | 前后台切换 | `Deferred` | 没有运行中的本批 Agent run |
| `AG-CLI-AND-010` | 生图草稿确认 | `Deferred` | 认证、Agent run 与 Provider 前置未建立 |

## Evidence

- per-flow directories: `testing/Agent/客户端/artifacts/20260830-luna-android-wave-01-AG-CLI-AND-001/` through `...-010/`
- flow ledger: `testing/Agent/客户端/reports/20260830-luna-android-wave-01-flow-status.csv`
- 61-tool ledger: `testing/Agent/客户端/reports/20260830-luna-android-wave-01-tool-status.csv`
- environment: `testing/Agent/客户端/reports/environment-20260830-luna-android-wave-01.json`
- run summary: `testing/Agent/客户端/reports/run-summary-20260830-luna-android-wave-01.json`
- redacted app logcat: `testing/Agent/客户端/logs/20260830-luna-android-wave-01-app-redacted.log`

The approved development fixture was not exposed through a safe input channel in this runtime. No credential, Cookie, Token, Authorization value, password, private key, API key, or complete authentication payload was read or retained. Historical 20260829 and Terra outcomes remain separate evidence.
