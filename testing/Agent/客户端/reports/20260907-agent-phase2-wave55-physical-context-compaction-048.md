# 2026-09-07 Wave 55 Android 真机上下文压缩闭环

## 结论

`AG-CLI-AND-007` 本轮通过。物理设备 `d715a3a4` 使用当前 Android APK，通过 UI tree 定位并真实点击发送；同一轮取得了客户端压缩提示、后续回答、服务端 `context_compacted` 事件、检查点和审计结果。新 APK 只展示固定的压缩说明，没有把完整摘要 JSON 展开到对话界面。

本轮运行模型为 `deepseek-v4-flash-0731/chat_completions`，Base URL 为 `https://oneapi.sxyq27.online/v1`。未写入 API Key、密码、Token、Cookie、Authorization 或完整认证载荷。旧的 `gpt-5.6-luna` 历史记录保持原样。

## 环境与真实操作

| 项目 | 事实 |
|---|---|
| 设备 | 物理 USB ADB `d715a3a4`；Android 16；`com.zhihuiji.app` 1.0.0 |
| 模拟器 | 未启动 |
| 会话 | `conversation_id=218`；沿用当前测试会话的已完成历史 |
| 输入 | 通过 Android 输入框输入非敏感业务运行说明长文本；没有使用接口替代 UI |
| 发送 | 依据 `40-round7-input-ui.xml` 的发送 bounds `[903,1362][1029,1488]`，真实点击 `(966,1425)`；记录见 `43-round7-send-tap.txt` |
| HTTP/SSE | App 日志记录 `POST /v2/agent/chat/stream`、HTTP `200`、`text/event-stream` |

## 客户端证据

发送后 2 秒、10 秒和 25 秒的 UI tree 均显示：`上下文已压缩（2 条），已按上下文预算生成摘要`。10 秒和 25 秒的 UI 同时显示 `已完成`、后续回答和 `重新生成` 操作；界面没有再展开完整 `summary_preview` JSON。

| 采集点 | 证据 |
|---|---|
| 发送前 UI tree、截图 | `40-round7-input-ui.xml`、`41-round7-input.png`、`42-round7-input-summary.txt` |
| 发送后 1 秒 | `44-round7-after-send-1s-ui.xml`、`45-round7-after-send-1s.png`、`46-round7-after-send-1s-summary.txt` |
| 发送后 2 秒 | `47-round7-after-send-2s-ui.xml`、`48-round7-after-send-2s.png`、`49-round7-after-send-2s-summary.txt` |
| 发送后 10 秒 | `50-round7-after-send-10s-ui.xml`、`51-round7-after-send-10s.png`、`52-round7-after-send-10s-summary.txt` |
| 发送后 25 秒 | `53-round7-after-send-25s-ui.xml`、`54-round7-after-send-25s.png`、`55-round7-after-send-25s-summary.txt` |
| App 日志与崩溃 | `56-round7-app-logcat-safe.txt`、`57-round7-crash-buffer.txt` |

## 服务端证据

新 run 为 `6a903ac0-4a6c-4cf2-b333-5bc10a0de02f`，状态为 `completed`，审计字段 `audit_lossy=false`。事件顺序为：

```text
run_started -> context_compacted -> answer_delta -> answer_delta -> answer_delta -> answer_completed -> run_completed
```

检查点从发送前的 3 条增至 4 条；新增检查点属于会话 218，运行模型字段为 `deepseek-v4-flash-0731`。服务端事件、检查点、消息和计数见 `58-server-after-round7.txt`。

| 计数时点 | 会话/消息/草稿/审计/审计事件/检查点 | 业务表 |
|---|---|---|
| 发送前 | `20/77/0/113/1679/3` | `products=693`、`customers=84`、`finance_records=2661` |
| 发送后 | `20/79/0/114/1686/4` | `products=693`、`customers=84`、`finance_records=2661` |
| App 内删除测试会话后 | `19/65/0/114/1686/1` | 本轮未产生业务表写入 |

## 清理与终态

已打开 App 会话列表，依据实时 UI tree 对测试会话执行真实删除。服务端复核显示 `conversation_218=0`、`messages_218=0`、`audits_218=0`、`events_218=0`，见 `68-server-after-ui-delete.txt`。随后执行 `pm clear com.zhihuiji.app`，返回 `Success`；重新启动后登录页可见，crash buffer 为 0 行。

清理证据：`59-session-list-open-ui.xml`、`60-session-list-open.png`、`61-session-list-open-summary.txt`、`62-delete-dialog-ui.xml`、`63-delete-dialog.png`、`64-delete-dialog-summary.txt`、`65-after-delete-ui.xml`、`66-after-delete.png`、`67-after-delete-summary.txt`、`69-pm-clear.txt`、`71-final-login-ui.xml`、`72-final-login.png`、`73-final-crash-buffer.txt`、`74-final-login-summary.txt`。

## 阶段二状态

| 用例 | 当前结论 |
|---|---|
| `AG-CLI-AND-003` | 多工具与空数据分支已有 `Passed`；有数据图表分支仍为 `Failed`，未因本轮压缩结果上调 |
| `AG-CLI-AND-004` | 商品草稿创建 `Failed`；确认成功、重复确认、并发确认 `Blocked` |
| `AG-CLI-AND-006` | 完整取消和断线重连仍为 `Failed`，客户端局部状态已有证据 |
| `AG-CLI-AND-007` | 本轮完整压缩闭环 `Passed` |
| `AG-CLI-AND-008` | 首次请求和重试已有 `Passed`；完整错误矩阵仍为 `Blocked` |
| `AG-CLI-AND-010` | 生图接口 HTTP `422`，生图草稿流程仍为 `Failed` |

本轮只更新测试报告、测试计划、总入口、执行记录和台账；未修改产品源码、线上服务、数据库、账号密码或 API Key。
