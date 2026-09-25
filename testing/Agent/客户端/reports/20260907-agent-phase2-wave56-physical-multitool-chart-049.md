# 2026-09-07 Wave 56 Android 真机多工具与有数据图表复测

## 结论

`AG-CLI-AND-003` 的有数据图表分支本轮失败。物理 Android 设备通过真实 UI 登录、进入助手、新建对话并发送两次图表请求。第一次请求取得 13 个销售数据点后，服务端以 `llm_answer_unavailable / model_empty_or_ungrounded` 结束；第二次重试完整执行了 `sales_trend_lookup` 和 `payment_lookup`，但后续仍以同一错误结束。两次均没有 `result_visualization` 事件或图表 UI，因此不能把工具查询成功当作图表通过。

本轮运行模型为 `deepseek-v4-flash-0731/chat_completions`，Base URL 为 `https://oneapi.sxyq27.online/v1`。未启动模拟器，未用接口替代 Android UI；未写入 API Key、密码、Token、Cookie、Authorization 或完整认证载荷。旧的 `gpt-5.6-luna` 历史记录保持原样。

## 环境与真实操作

| 项目 | 事实 |
|---|---|
| 设备 | 物理 USB ADB `d715a3a4`；Android 16；`com.zhihuiji.app` 1.0.0 |
| 服务端 | 8.220.206.9；容器镜像沿用 `sxyq27-zhj-api:20260907T0042-agent-message-text`；Flyway 44 |
| 登录 | 从现有启用账号进入登录页，通过 UI tree bounds 真实填写并点击 `(540,1492)`；登录和首页请求成功 |
| 助手入口 | 依据首页 UI tree 真实点击助手中心 `(940,2240)`，再点击新建对话中心 `(976,183)` |
| 第一次输入 | `Show real sales and payment trends for the last 365 days and render a chart`；发送 bounds `[903,2122][1029,2248]`，真实点击 `(966,2185)` |
| 第二次输入 | `Analyze sales trends and payment records for the last 365 days and show a chart`；同样依据 UI tree 真实点击 `(966,2185)` |

## 第一次请求

第一次 run 为 `89820ce9-9ac0-42cc-a60e-b8ddb1042f0b`，conversation 为 `219`。App 在 1 秒 UI 中显示本次查询包含 13 个数据点；3 秒时销售趋势工具返回销售额 `¥929414.93`、订单 3307。之后服务端返回错误终态，10 秒和 25 秒 UI 均显示 `暂时无法完成这次请求，请稍后重试。`。

服务端事件顺序为：

```text
run_started -> plan_delta -> tool_started -> tool_completed -> error -> run_failed
```

该 run 的审计状态为 `failed`，错误为 `LLM_ANSWER_UNAVAILABLE`，工具数为 1、事件数为 4；没有 `payment_lookup`、`result_visualization` 或完成态回答。证据见 `45-server-after-chart.txt`、`30-after-chart-send-1s-ui.xml`、`33-after-chart-send-3s-ui.xml`、`36-after-chart-send-10s-ui.xml`、`39-after-chart-send-25s-ui.xml` 和 `43-chart-logcat-safe.txt`。

## 第二次请求与重试

第二次 run 为 `c20b518c-94c1-41d6-9dc6-01afcd6a2c7c`，conversation 为 `220`。3 秒 UI 已显示两个工具步骤：

- `sales_trend_lookup`：销售额 `¥929414.93`、订单 3307、13 个返回数据点。
- `payment_lookup`：10 条记录、收款 `¥5992.78`、付款 `¥0.00`。

10 秒和 25 秒 UI 仍显示 `暂时无法完成这次请求，请稍后重试。`。服务端事件顺序为：

```text
run_started -> plan_delta -> tool_started -> tool_completed -> tool_started -> tool_completed -> error -> run_failed
```

该 run 的审计状态为 `failed`，错误为 `LLM_ANSWER_UNAVAILABLE`，工具数为 2、事件数为 6；没有 `result_visualization`，也没有图表结果或正式回答。证据见 `69-server-after-chart-retry.txt`、`54-after-chart-retry-1s-ui.xml`、`57-after-chart-retry-3s-ui.xml`、`60-after-chart-retry-10s-ui.xml`、`63-after-chart-retry-25s-ui.xml` 和 `67-chart-retry-logcat-safe.txt`。

## 数据与清理

Wave 55 App 清理后的已知计数为 `19/65/0/114/1686/1`，顺序为会话/消息/草稿/审计/审计事件/上下文记录。本轮第一次请求后为 `20/67/0/115/1692/1`，第二次请求后为 `21/69/0/116/1700/1`。App 内删除两个本轮会话后为 `19/65/0/116/1700/1`。

正式业务表在本轮前后均为 `products=693`、`customers=84`、`finance_records=2661`；草稿始终为 0。通过会话列表 UI tree 真实删除 conversation `219`、`220` 后，服务端残留为：

```text
conversation_219=0  messages_219=0  audits_219=0  events_219=0
conversation_220=0  messages_220=0  audits_220=0  events_220=0
```

随后 `pm clear com.zhihuiji.app` 返回 `Success`；重新启动后登录页可见，crash buffer 为 0 行。清理证据为 `70-session-list-before-cleanup-ui.xml`、`73-session-list-open-ui.xml`、`76-delete-first-dialog-ui.xml`、`79-after-delete-chart-sessions-ui.xml`、`83-server-after-chart-cleanup.txt`、`84-pm-clear.txt`、`86-final-login-ui.xml` 和 `88-final-crash-buffer.txt`。

## 状态与后续

| 用例 | 本轮结论 |
|---|---|
| `AG-CLI-AND-003` 多工具真实查询 | `Passed` 子步骤；第二次请求真实执行两个查询工具并返回非零事实 |
| `AG-CLI-AND-003` 有数据图表 | `Failed`；两次均未取得 `result_visualization` 或图表 UI，第二次仍为 LLM 空回答错误 |
| `AG-CLI-AND-004` | 商品草稿创建 `Failed`；三类确认 `Blocked` |
| `AG-CLI-AND-006` | 完整取消和断线重连 `Failed` |
| `AG-CLI-AND-007` | Wave 55 已通过，本轮未改变结果 |
| `AG-CLI-AND-008` | 首次请求和重试已有 `Passed`；完整错误矩阵 `Blocked` |
| `AG-CLI-AND-010` | 生图草稿流程 `Failed`，接口此前返回 HTTP 422 |

本轮只新增测试证据与报告，未修改产品源码、线上服务、数据库、账号密码或 API Key。
