# 2026-09-04 Android Wave 19：create_customer 草稿确认、审计事件与清理

## 结论

本报告只记录证据目录 `testing/Agent/客户端/artifacts/20260904-agent-phase2-wave19-android-confirm-audit-009/` 中已完成的 Android 真实操作。Wave 19 的客户草稿确认、确认后的审计事件和测试对象清理均为 `Passed`。

| 用例 | 范围 | 状态 |
|---|---|---|
| `AG-CLI-AND-P2-DRAFT-CONFIRM-CUSTOMER-REAL-RERUN-009` | Android 真实输入、发送、草稿确认和客户写入 | `Passed` |
| `AG-CLI-AND-P2-DRAFT-CONFIRM-CUSTOMER-AUDIT-009` | draft、run audit 和 audit event 对齐 | `Passed` |
| `AG-CLI-AND-P2-DRAFT-CONFIRM-CUSTOMER-CLEANUP-009` | 会话、草稿、客户和 App 本地状态清理 | `Passed` |

`agent_run_audits.status=confirmation_pending` 是生成草稿的原始 run 终态；确认操作随后追加了同一 run 的 `draft_confirmed` 审计事件。因此本轮按“原始生成终态 + 后续确认事件”记录，未把 `confirmation_pending` 单独解释为确认失败。

## 环境与输入

- Android 设备：`emulator-5554`；App：`com.zhihuiji.app`，`versionName=1.0.0`；启动 Activity 为 `com.zhihuiji.app/.MainActivity`。
- 公网健康检查返回 HTTP `200`，响应体为 `ok`。
- App 中最终 `EditText` 的脱敏输入为：`新建客户名称测试客户手机[REDACTED_TEST_PHONE]先生成草稿不要直接保存`。

证据：`01-activity.txt`、`35-final-input-ui.xml`、`37-public-healthz-body.txt`、`37-public-healthz-headers.txt`、`72-app-version.txt`、`73-final-activity.txt`。

## 真实点击、HTTP 与 SSE

1. 发送前 UI tree 显示输入框和 `发送` 控件。依据 `35-final-input-ui.xml` 点击发送父节点 `[586,502][682,598]`，中心 `(634,550)`；点击记录见 `38-send-tap-start.txt` 和 `39-send-tap-end.txt`。
2. App 实际发起 `POST /v2/agent/chat/stream`，请求接受 `text/event-stream`；响应为 HTTP `200`、`Content-Type: text/event-stream`。对应脱敏 App 日志见 `54-after-app-clear-logcat-safe.txt`。
3. 服务端生成 run `539a6a1a-a6bd-44c9-a9cb-ec7fd54fef45`、conversation `182`、draft `21`，工具为 `create_customer`。确认前摘要显示 draft `21` 为 `active`，全局计数为 `3|2|2|693|83|2661|11|27|1|59|664`。
4. `40-after-send-2s-ui.xml` 显示覆盖式“操作确认”弹窗，包含“新建客户：测试客户”“草稿状态：待确认”和“允许一次”。依据该 UI tree 点击“允许一次”父节点 `[392,803][592,899]`，中心 `(492,851)`；点击记录见 `43-confirm-tap-start.txt` 和 `44-confirm-tap-end.txt`。
5. App 实际发起 `POST /v2/agent/drafts/21/confirm`，响应为 HTTP `200`、`Content-Type: application/json`。确认后的 UI tree 显示“草稿已确认，业务数据已写入”和 `状态：confirmed`，见 `45-after-confirm-4s-ui.xml` 及截图。

服务端保存的生成阶段事件顺序为：

`run_started → plan_delta → tool_started → tool_completed → draft_created → answer_delta → result_block → answer_delta ×4 → answer_completed → run_completed`

其中 `run_completed` 的 `terminal_status` 为 `CONFIRMATION_PENDING`。确认接口完成后，同一 run 追加 `draft_confirmed` 事件（seq `14`）。

## Draft、客户与 audit

确认后服务端摘要 `49-server-after-confirm.txt` 记录：

- draft `21`：`create_customer`，状态 `confirmed`，业务引用 `create_customer:92`。
- 客户 `92`：名称“测试客户”，目标客户记录数为 `1`。
- audit 对应 run `539a6a1a-a6bd-44c9-a9cb-ec7fd54fef45`、conversation `182`，状态仍为 `confirmation_pending`；同一审计事件序列包含 `draft_confirmed`，事件序号为 `14`。
- 确认后全局计数为 `3|2|2|693|84|2661|11|27|1|59|665`。相对于确认前，正式客户增加一条，审计事件增加一条；未观察到重复客户。

因此：

- `AG-CLI-AND-P2-DRAFT-CONFIRM-CUSTOMER-REAL-RERUN-009`：`Passed`。真实 Android 点击、SSE、确认 HTTP 结果、draft 状态、客户 `92` 和确认后 UI 均有证据。
- `AG-CLI-AND-P2-DRAFT-CONFIRM-CUSTOMER-AUDIT-009`：`Passed`。原始 run 终态保留 `confirmation_pending`，确认行为由追加的 `draft_confirmed` 事件表达，且业务引用与 draft 一致。

## 清理与最终状态

清理按证据顺序执行：

- 点击“清空对话”父节点 `[592,136][688,232]`，中心 `(640,184)`。此后 UI 回到空对话页；`53-server-after-app-clear.txt` 仍显示目标 draft、conversation 和两条消息各存在，随后继续走会话列表删除。
- 点击会话列表并核对列表首项后，依据 `59-session-list-ui.xml` 点击目标会话删除父节点 `[580,316][676,412]`，中心 `(628,364)`。App 日志记录会话删除 HTTP `200`；`63-server-after-app-delete.txt` 显示目标 draft、conversation 和 messages 均为 `0`。
- 通过精确服务端清理删除客户 `92`，`66-server-customer-delete-status.txt` 记录 HTTP `200`；`67-server-final-before-app-reset.txt` 显示目标客户和手机号记录均为 `0`。
- 最终服务端计数为 `users=3`、`stores=2`、`store_memberships=2`、`products=693`、`customers=83`、`finance_records=2661`、`agent_conversations=10`、`agent_messages=25`、`agent_drafts=0`、`agent_run_audits=59`、`agent_run_audit_events=665`。审计记录保留，目标草稿、会话、消息、客户和手机号记录均无残留。
- 最终目标核对还记录 `run_audit_event=1`；该记录属于审计保留，不是目标草稿、会话、消息、客户或手机号残留。
- `pm clear com.zhihuiji.app` 返回 `Success`；重启 `MainActivity` 后最终 UI 为登录页，crash buffer 为空。证据为 `68-pm-clear.txt`、`69-activity-restart.txt`、`70-final-login-ui.xml`、`71-final-crash-buffer.txt`。

`AG-CLI-AND-P2-DRAFT-CONFIRM-CUSTOMER-CLEANUP-009`：`Passed`。

## 证据索引

- 输入与发送：`35-final-input-ui.xml`、`35-final-input.png`、`38-send-tap-start.txt`、`39-send-tap-end.txt`、`40-after-send-2s-ui.xml`、`40-after-send-2s.png`。
- 确认与 UI：`42-server-before-confirm.txt`、`43-confirm-tap-start.txt`、`44-confirm-tap-end.txt`、`45-after-confirm-4s-ui.xml`、`45-after-confirm-4s.png`、`54-after-app-clear-logcat-safe.txt`。
- 服务端 draft、客户和 audit：`36-server-before-send.txt`、`48-server-after-confirm.txt`、`49-server-after-confirm.txt`。
- 会话与最终清理：`50-clear-conversation-tap-start.txt`、`52-clear-conversation-confirm-ui.xml`、`53-server-after-app-clear.txt`、`57-session-list-tap-start.txt`、`59-session-list-ui.xml`、`60-delete-conversation-tap-start.txt`、`62-delete-confirm-ui.xml`、`63-server-after-app-delete.txt`、`64-after-conversation-delete-logcat-safe.txt`、`66-server-customer-delete-status.txt`、`67-server-final-before-app-reset.txt`。
- App 重置：`68-pm-clear.txt`、`69-activity-restart.txt`、`70-final-login-ui.xml`、`71-final-crash-buffer.txt`。
