# 2026-09-04 Android Wave 21：create_customer 草稿确认、审计与清理

## 结论

本报告只记录证据目录 `testing/Agent/客户端/artifacts/20260904-agent-phase2-wave21-android-confirm-audit-011/` 中的真实 Android 操作和服务端结果。

| 用例 | 范围 | 状态 |
|---|---|---|
| `AG-CLI-AND-P2-DRAFT-CONFIRM-CUSTOMER-REAL-RERUN-011` | Android 真实输入、发送、草稿确认和客户写入 | `Passed` |
| `AG-CLI-AND-P2-DRAFT-CONFIRM-CUSTOMER-AUDIT-011` | 草稿、run audit 和确认事件关联 | `Passed` |
| `AG-CLI-AND-P2-DRAFT-CONFIRM-CUSTOMER-CLEANUP-011` | 会话、草稿、客户和 App 状态清理 | `Passed` |
| `AG-CLI-AND-P2-CUSTOMER-DELETE-SYNC-011` | App 客户删除、同步上传和远端结果 | `Failed` |

确认链路已通过真实点击验证。App 客户删除已产生本地同步操作并收到同步接口 HTTP `200`，但由于版本冲突，远端客户没有被同步删除；测试使用当前账号的精确服务端删除接口完成环境清理，这一补救不改变客户端用例的 `Failed` 状态。

## 环境与输入

- 设备：`emulator-5554`，Android API 34，已启动。
- App：`com.zhihuiji.app`，`versionName=1.0.0`，Activity 为 `com.zhihuiji.app/.MainActivity`。
- 服务端：8220 节点公网 HTTPS API；健康检查 HTTP `200`；API、PostgreSQL 和 Redis 容器均在运行。
- App 中的最终输入为：`新建客户名称测试客户手机[REDACTED_TEST_PHONE]先生成草稿不要直接保存`。
- 实际运行模型为 `gpt-5.6-luna/chat_completions`，目标模型 `glm-5.3-flash` 未成为本次运行模型，目标模型状态为 `Blocked`。

环境和版本证据：`69-app-version-device.txt`、`70-server-health-final.txt`。输入和 UI 证据：`09-final-input-ui.xml`、`05-after-login.png`、`13-after-send.png`、`14-after-send-10s.png`、`14-after-send-10s-ui.xml`、`19-after-confirm.png`、`19-after-confirm-ui.xml`。

## 真实点击与 Agent 调用

1. 发送前依据 UI tree `09-final-input-ui.xml` 定位发送控件父节点 `[586,1052][682,1148]`，中心坐标为 `(634,1100)`；执行记录见 `11-send-tap-start.txt` 和 `12-send-tap-end.txt`。
2. App 发起 `POST /v2/agent/chat/stream`，服务端返回 HTTP `200` 和 `text/event-stream`。本轮生成 run `d990139c-8828-41c9-8f08-b5d57bee2176`、conversation `184`、draft `23`，工具为 `create_customer`。
3. `14-after-send-10s-ui.xml` 显示覆盖式“操作确认”弹窗和“允许一次”。依据节点 `[392,803][592,899]` 计算中心坐标 `(492,851)`，执行记录见 `17-confirm-tap-start.txt` 和 `18-confirm-tap-end.txt`。
4. App 发起 `POST /v2/agent/drafts/23/confirm`，返回 HTTP `200`。`19-after-confirm-ui.xml` 显示 `状态：confirmed` 和“草稿已确认，业务数据已写入”。

服务端生成阶段事件按以下顺序保存：

`run_started → plan_delta → tool_started → tool_completed → draft_created → answer_delta → result_block → answer_delta ×5 → answer_completed → run_completed`

确认完成后同一 run 追加 `draft_confirmed` 事件。`20-server-after-confirm.txt` 中的事件序号从 `1` 连续到 `15`，`audit_lossy=false`；`run_completed` 的原始终态为 `confirmation_pending`，后续确认动作由事件序号 `15` 表达。

## Draft、客户与 audit

发送前计数见 `10-server-before-send.txt`：

`users=3`、`stores=2`、`store_memberships=2`、`products=693`、`customers=84`、`finance_records=2661`、`agent_conversations=10`、`agent_messages=25`、`agent_drafts=0`、`agent_run_audits=60`、`agent_run_audit_events=680`。

确认前 `16-server-before-confirm.txt` 显示 draft `23` 为 `active`，并记录 `conversation=184`；确认后 `20-server-after-confirm.txt` 显示：

- draft `23`：`create_customer`，状态 `confirmed`，业务引用 `create_customer:94`。
- 客户 `94`：目标记录数为 `1`，目标手机号记录数为 `1`。
- run audit：状态仍为 `confirmation_pending`，事件序列连续到 `15`，包含 `draft_confirmed`，`audit_lossy=false`。
- 确认后计数为 `customers=85`、`agent_conversations=11`、`agent_messages=27`、`agent_drafts=1`、`agent_run_audits=61`、`agent_run_audit_events=695`；相对确认前新增一条客户和一条确认审计事件，未观察到重复客户。

因此 `REAL-RERUN-011` 和 `AUDIT-011` 均为 `Passed`。`confirmation_pending` 在本轮作为原始草稿生成 run 的终态保留，不把它解释为确认失败。

## 清理与客户端同步缺口

清理过程中的真实点击记录为：

- 依据 `19-after-confirm-ui.xml` 点击“清空对话”中心 `(640,184)`，见 `23-clear-conversation-tap-start.txt`；随后打开会话列表。
- 依据 `30-session-list-ui.xml` 点击目标会话删除中心 `(628,364)`，见 `31-delete-conversation-tap-start.txt`；`35-server-after-conversation-delete.txt` 显示目标 draft、conversation 和 messages 均为 `0`。
- 进入档案管理的客户列表，依据 `43-customers-list-ui.xml` 点击本轮新增客户中心 `(360,812)`，再依据 `46-customer-detail-ui.xml` 点击删除中心 `(640,200)`，见 `44-target-customer-tap-start.txt` 和 `48-customer-delete-tap-start.txt`。

客户删除分支的服务端和 App 证据显示：

- App 发起 `/v2/sync/upload`，返回 HTTP `200`；随后 cursor、pull 请求均返回 HTTP `200`，WorkManager 记录 `Worker result SUCCESS`，见 `54-after-customer-delete-sync-logcat-safe.txt` 和 `59-customer-delete-and-sync-logcat-safe.txt`。
- 本地 Room outbox 中的 customer `94` delete 操作状态为 `blocked`，错误为 `sync version conflict: expected 0, current 1`，见 `58-local-outbox-customer94.txt`。
- 同步复核显示远端客户行仍存在且没有 tombstone，见 `55-server-sync-delete-recheck.txt`；因此 `AG-CLI-AND-P2-CUSTOMER-DELETE-SYNC-011` 为 `Failed`。
- 测试侧使用当前账号的精确服务端删除接口完成清理，HTTP `200`，见 `56-server-exact-customer-cleanup-status.txt`。最终目标客户、手机号、操作日志、变更日志和 tombstone 均为 `0`，同名历史客户保留 `1` 条，避免按名称误删，见 `57-server-final-after-exact-customer-cleanup.txt`。

清理后的服务端计数为：

`users=3`、`stores=2`、`store_memberships=2`、`products=693`、`customers=84`、`finance_records=2661`、`agent_conversations=10`、`agent_messages=25`、`agent_drafts=0`、`agent_run_audits=61`、`agent_run_audit_events=695`。

`AG-CLI-AND-P2-DRAFT-CONFIRM-CUSTOMER-CLEANUP-011` 为 `Passed`：测试对象已经精确清理，审计记录按保留策略继续存在。`pm clear com.zhihuiji.app` 返回 `Success`，重启 MainActivity 后回到登录页，crash buffer 为 `0` 行，证据为 `61-pm-clear.txt`、`62-activity-restart.txt`、`65-final-login-recheck-ui.xml` 和 `68-final-crash-buffer-recheck.txt`。

## 验证边界

- 本轮保存了 UI tree、真实 tap 记录、App 脱敏 logcat、服务端 before/after 摘要和最终清理核对；截图仅保留在本地证据目录，未加入 Git。
- 目标模型、iOS、生图、取消/断线、上下文压缩和性能分支没有在本轮形成新的真实证据；iOS 继续为 `Deferred`，目标模型继续为 `Blocked`。
- 本报告不记录账号、密码、token、Cookie、Authorization 载荷或 API key。
