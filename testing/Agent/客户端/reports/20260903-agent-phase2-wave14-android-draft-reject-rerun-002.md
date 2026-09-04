# 2026-09-03 Android Wave 14: create_product 草稿拒绝复测

## 结论

| Test ID | 范围 | 结果 |
|---|---|---|
| `AG-CLI-AND-P2-DRAFT-REJECT-001-RERUN-002` | Android App 真实输入、发送、草稿弹窗、拒绝、服务端审计、数据库核对和清理 | `Passed` |
| 同一 Wave 的首次无分隔输入尝试 | App 真实发送；模型把商品编码提交为空，服务端拒绝工具参数 | `Failed` |
| 运行模型前置 | 当前运行时为 `gpt-5.6-luna/chat_completions`，目标为 `glm-5.3-flash` | `Blocked` |

核心用例以第二次、带明确编码标签和空格的输入作为验收样本。首次尝试保留为独立失败证据，不覆盖第二次已完成的草稿拒绝结果。

## 环境

- 设备：`emulator-5554`，Android API 34。
- App：`com.zhihuiji.app` 1.0.0，Activity `com.zhihuiji.app/.MainActivity`。
- 服务：`https://zhj-api.sxyq27.online/`；8220 SSH 只读核查显示 Nginx、Docker、API、PostgreSQL、Redis 和转发服务均 active/running，PostgreSQL healthy，systemd failed units 为 0。
- API 容器：`sxyq27-zhj-api:20260902T024500-agent-cancel-9536df5b`。
- 认证会话、owner/store 标签均只按脱敏方式记录。

## 真实 UI 流程

1. 在 App 新建对话，使用 Gboard 分段输入最终提示词：`新增一个商品 商品编码是 EVALONLY20260903D 名称是阶段2 中文点击商品先生成草稿不要直接保存`。输入前后的完整文本由 UI tree 校验；其中 `2` 是因拼音候选选择稳定性采用的数字写法。
2. 从发送前 UI tree 取得可点击父节点 `[586,502][682,598]`，点击中心 `(634,550)` 一次。
3. App 进入处理中，收到 `/v2/agent/chat/stream` HTTP 200 `text/event-stream`；随后显示覆盖式“操作确认”、商品名称和“草稿状态：待确认”。
4. 重新 dump UI tree，取得“拒绝”可点击父节点 `[260,803][376,899]`，点击中心 `(318,851)` 一次。
5. App 会话详情显示商品编码 `EVALONLY20260903D`、草稿状态 `cancelled`、`运行取消` 和“草稿已取消，未执行任何业务写入”。
6. 进入草稿列表，确认 `create_product` 为“已取消（未执行）”；通过 App 删除草稿，再通过会话列表删除两条本轮测试会话。
7. `pm clear com.zhihuiji.app` 返回 `Success`；重启首帧采集需要等待一次，最终 UI tree 为登录页，清理后 crash buffer 为 0 行。

## 服务端事实

### 首次失败尝试

- Run：`45ef8240-b523-4891-995a-fd879adc777b`；conversation：`168`。
- 事件：`run_started → plan_delta → tool_started → tool_failed → answer_delta → answer_completed → run_exhausted`，序号 `1..8` 连续。
- 服务端已选择 `create_product`，但工具参数 `code` 为空，返回 `TOOL_ARGUMENTS_INVALID`；终态为 `exhausted`，未生成草稿。
- App 显示“本次运行已达轮次预算上限……未写入任何正式业务数据”。

### 最终通过尝试

- Run：`5f72e106-5fce-4318-a112-058bf044b982`；conversation：`169`；draft：`12`。
- 工具参数摘要包含 `code=EVALONLY20260903D`、`name=阶段2 中文点击商品`；工具结果生成商品草稿。
- 事件序列：`run_started → plan_delta → tool_started → tool_completed → draft_created → answer_delta → result_block → answer_delta ×9 → answer_completed → run_completed`，序号 `1..17` 连续。
- 审计记录：`tool_count=1`、`event_count=17`、`emitted_event_count=17`、`audit_lossy=false`；草稿生成阶段终态为 `confirmation_pending`，拒绝后草稿记录为 `cancelled`。

## 数据与清理

| 时点 | conversations | messages | run_audits | audit_events | drafts | products | finance_records |
|---|---:|---:|---:|---:|---:|---:|---:|
| 发送前 | 11 | 27 | 40 | 482 | 0 | 693 | 2661 |
| 拒绝后、清理前 | 12 | 29 | 41 | 499 | 1 | 693 | 2661 |
| App 清理后 | 10 | 25 | 41 | 499 | 0 | 693 | 2661 |

正式商品数从发送前到清理后保持 `693`，没有观察到本轮正式商品写入；审计和事件记录按服务端保留策略继续存在。草稿和两条测试会话已由 App 清理。

## 证据与限制

- 证据目录：`testing/Agent/客户端/artifacts/20260903-agent-phase2-wave14-AG-CLI-AND-P2-DRAFT-REJECT-RERUN-002/`。
- 关键证据：`93-corrected-send-before.xml`、`99-corrected-after-send-2s.xml`、`101–108-corrected-poll-*.xml`、`122-reject-before.xml`、`126-after-reject-2s.xml`、`130-database-after-reject.txt`、`131-latest-audits-after-reject.txt`、`132-latest-drafts-after-reject.txt`、`133-current-run-event-index.txt`、`134-current-run-events.txt`、`156-database-after-cleanup.txt`、`167-after-cleanup-login-summary.txt`。
- App 日志只保留脱敏的请求方法、路径、HTTP 状态和媒体类型；未保存 SSE 正文，服务端持久化 audit event 用于序列核对。
- `ui_pick.py` 在本机 Python 运行时因不支持 `str | None` 未能执行；拒绝坐标直接依据同一份 UI tree summary 的可点击父节点 bounds 计算，并已完成真实点击。
- 本轮未执行确认写入、重复/并发确认、目标模型切换、生图、性能或 iOS；这些项目不因本轮 `Passed` 改变状态。
