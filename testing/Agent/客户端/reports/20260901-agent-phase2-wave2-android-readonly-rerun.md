# 阶段二 Wave 2 Android 只读重测

## 结论

| 范围 | 结果 |
|---|---|
| Android 商品目录查询重测 | `Passed` |
| Android 近 30 天现金流查询重测 | `Passed` |
| Android 当前门店查询重测 | `Passed` |
| 本轮清理与业务数据无写入 | `Passed` |
| 8220 运行模型与目标模型一致性 | `Blocked` |
| 创建类工具、取消/断线、恢复、压缩、生图、性能 | `Blocked` |
| iOS | `Deferred` |

本轮从已登录的真实 Android App 工作台开始，每条用例新建独立对话，按 UI 树定位输入框和发送按钮，输入问题并点击发送。三条请求均到达 8220 公网 API，HTTP 为 200、响应类型为 `text/event-stream`，工具选择和完成事件与 App 展示一致；查询事务异常没有再次出现。

## 环境

| 字段 | 脱敏事实 |
|---|---|
| 目标服务 | `8220 / 8.220.206.9 / https://zhj-api.sxyq27.online/` |
| API 镜像 | `sxyq27-zhj-api:20260901T175500-agent-product-query-1429d657` |
| 源码提交 | `1429d657` |
| 数据库 | PostgreSQL 15，Flyway V42 |
| Provider 运行时 | `gpt-5.6-luna / chat_completions` |
| 目标模型 | `glm-5.3-flash`，本轮未切换 |
| Android | `emulator-5554`，Android API 34，`com.zhihuiji.app` `1.0.0` |
| 账号范围 | 8220 已存在启用账号；owner/store 标签已脱敏 |

## 实际点击步骤

1. 从 Android Agent 工作台点击“新建对话”，确认输入框为空。
2. 点击输入框，在 UI 树确认键盘展开后的发送节点中心为 `(634,550)`。
3. 分别输入 `Show the current product list`、`Summarize cashflow for the last 30 days`、`Show the current store information`，每条只点击一次发送。
4. 等待 App 结果页出现完成的执行步骤，再保存 UI 树摘要、截图、HTTP 日志摘要和 crash buffer。
5. 通过 8220 PostgreSQL 只读取该 run 的状态、工具名、事件序号和聚合计数，不读取认证材料或完整模型载荷。
6. 打开 App 会话列表，按标题和时间识别本轮 3 条新会话，逐条点击“删除会话”；确认 UI 只剩历史会话。
7. 执行 `pm clear com.zhihuiji.app`，重启 App，确认回到登录页；服务器最终计数再次读取。

## 用例结果

| test_id | run_id | conversation_id | 工具 | 事件序列 | App 观察 | 结果 |
|---|---|---:|---|---|---|---|
| `AG-CLI-AND-P2-RO-001-RERUN-001` | `0fedfbad-8a32-415a-a1f0-f05539c4fae9` | `140` | `product_catalog_lookup` | `run_started → plan_delta → tool_started → tool_completed → answer_delta ×24 → answer_completed → run_completed` | 显示商品列表，结果区可滚动，并提供复制、重新生成、数据来源入口 | `Passed` |
| `AG-CLI-AND-P2-RO-002-RERUN-001` | `afd437b2-f06f-454b-a6c8-b30d2d3a5ee0` | `141` | `cashflow_summary_lookup` | `run_started → plan_delta → tool_started → tool_completed → answer_delta ×6 → answer_completed → run_completed` | 显示收入、支出、净现金流均为 `¥0.00`，记录数 `0`，并显示期间无流水提示 | `Passed` |
| `AG-CLI-AND-P2-RO-003-RERUN-001` | `721a6483-067f-400a-953c-769f047daf69` | `142` | `store_info_lookup` | `run_started → plan_delta → tool_started → tool_completed → answer_delta ×4 → answer_completed → run_completed` | 显示当前门店为正常状态、成员数 `1`，来源标签与工具一致 | `Passed` |

每个 run 均为 `status=completed`、`tool_count=1`、`error_code=null`、`lossy=false`，事件序号连续且无重复。App crash buffer 三条均为 0 行。

## 数据与清理

执行前后的正式业务表计数保持不变：`products=693`、`finance_records=2661`、`stores=2`、`store_memberships=2`。每条测试只增加一个会话、两条消息、一个 run audit 和对应事件；本轮 3 条会话通过 App 会话列表删除后，`agent_conversations` 回到 `4`、`agent_messages` 回到 `14`。审计表保留 10 条 run 和 133 条事件，符合审计保留边界；没有创建草稿、任务或正式业务对象。

## 证据

三条用例的标准 `00–10` 证据目录：

- `testing/Agent/客户端/artifacts/20260901-agent-phase2-wave2-rerun-AG-CLI-AND-P2-RO-001-RERUN-001/`
- `testing/Agent/客户端/artifacts/20260901-agent-phase2-wave2-rerun-AG-CLI-AND-P2-RO-002-RERUN-001/`
- `testing/Agent/客户端/artifacts/20260901-agent-phase2-wave2-rerun-AG-CLI-AND-P2-RO-003-RERUN-001/`

旧首轮 `AG-CLI-AND-P2-RO-001` 至 `003` 的 `Failed` 记录和证据保持不变。本轮结果只证明当前部署镜像和当前运行模型下的 Android 只读链路；目标 `glm-5.3-flash`、创建类工具、故障恢复、上下文压缩、性能和 iOS 仍未形成通过证据。
