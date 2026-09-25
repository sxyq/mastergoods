# 2026-09-05 Wave 35 DeepSeek 物理 Android 错误与重试

## 结论

`AG-CLI-AND-008` 的真实“重新生成”动作通过；完整错误矩阵仍为 `Blocked`。本轮没有把 HTTP 200、模型配置或一次成功回答扩展为完整用例通过。

## 环境与边界

- 设备：物理 `d715a3a4`（型号 `25010PN30C`），未启动模拟器。
- App：`com.zhihuiji.app` `1.0.0`，安装当前 Debug APK 后执行。
- 模型运行配置：`https://oneapi.sxyq27.online/v1`、`deepseek-v4-flash-0731`、`chat_completions`。
- App 实际 Agent 请求：`POST https://zhj-api.sxyq27.online/v2/agent/chat/stream`，日志已脱敏；API Key、密码、Authorization 值、Cookie 和完整认证载荷未保存。

## 真实 UI 流程

1. 通过物理设备 UI tree 定位输入框，输入长请求并真实点击发送；App 日志记录 stream HTTP `200`、`text/event-stream`。
2. 首次请求最终出现完整回答；完成态 UI tree 中“重新生成”节点 bounds 为 `[364,1802][646,1907]`，依据 bounds 计算中心 `(505,1854)` 并真实点击。
3. 重试再次发起同一 Agent stream，App 日志记录 HTTP `200`、`text/event-stream`，最终完成回答。
4. UI、脱敏 logcat、数据库和审计证据均保存在 `testing/Agent/客户端/artifacts/20260905-agent-phase2-wave35-physical-error-retry-023/`。

## 服务端审计

| 运行 | 结果 | 工具与事件 | 审计完整性 |
|---|---|---|---|
| `8b2e6034-bc60-4db7-b1f2-1f33fa2e8156` | `completed` | 6 个工具，110 个事件；`run_started → plan_delta → tool_started/tool_completed ×6 → answer_delta → answer_completed → run_completed` | `audit_lossy=false` |
| `ef130a2e-51b6-492f-b022-6cb476b83cb3` | `completed` | 6 个工具，111 个事件；重试运行使用 `non_stream_retry` | `audit_lossy=false` |

两次运行均由真实 Android UI 产生。完整错误矩阵所需的 401/403/409/422/429/5xx 样本本轮未建立，故错误矩阵和完整用例保持 `Blocked`；重试动作单独记为 `Passed`。

## 计数与清理

- 发送前基线：`agent_conversations=14`、`agent_messages=35`、`agent_drafts=0`、`agent_run_audits=79`、`agent_run_audit_events=995`、`agent_context_checkpoints=0`；`products=693`、`customers=84`、`finance_records=2661`。
- 两次真实请求完成后：`agent_conversations=15`、`agent_messages=39`、`agent_drafts=0`、`agent_run_audits=81`、`agent_run_audit_events=1216`；业务表未变化。
- 通过会话列表真实删除 22:56 的 Wave 35 会话后，会话和消息回到 `14/35`；审计记录按设计保留为 `81/1216`，草稿仍为 `0`。
- `pm clear com.zhihuiji.app` 返回 `Success`；重启后登录页可见；crash buffer 为 0 行。2026-09-06 00:04 的设备和 8220 只读复核仍显示 App 前台活动、PostgreSQL healthy、`agent_conversations=14`、`agent_messages=35`、`agent_drafts=0`、`agent_run_audits=81`、`agent_run_audit_events=1216`、`agent_context_checkpoints=0`。最终实时计数和审计见 `27-final-live-recount-and-audit.txt`，UI 清理见 `28-ui-cleanup-final.txt`，当前复核见 `29-current-recheck-0004.txt`。

## 状态

| 子项 | 状态 |
|---|---|
| 真实长请求与完整回答 | `Passed` |
| 真实点击“重新生成”并再次完成 | `Passed` |
| 完整错误矩阵 | `Blocked` |
| 业务数据、草稿和设备清理 | `Passed` |
