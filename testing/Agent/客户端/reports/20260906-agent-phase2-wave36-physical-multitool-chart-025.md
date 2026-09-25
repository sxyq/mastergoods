# 2026-09-06 Wave 36 Android 真机多工具与图表复测

## 范围

- 设备：`d715a3a4` / `25010PN30C`，USB 物理设备；未启动模拟器。
- APK：`com.zhihuiji.app` `1.0.0`，当前构建产物 `tmp/build/gradle-output/android/app/outputs/apk/debug/app-debug.apk`。
- 运行配置：`https://oneapi.sxyq27.online/v1`、`deepseek-v4-flash-0731`、`chat_completions`。API Key、密码、Authorization 和 Cookie 未写入证据。
- 提示词通过 App 输入框填写，并依据实时 UI tree 的 bounds 点击发送；未使用接口调用替代 UI 操作。

## 结果

| 子项 | 事实 | 状态 |
|---|---|---|
| 多工具链 | App 请求 `POST /v2/agent/chat/stream` 返回 HTTP `200`、`text/event-stream`；run `eda5dabb-2a81-4814-bc5b-c34f025e4407` 为 `completed`，工具顺序为 `sales_trend_lookup → payment_lookup`，30 个连续事件，`audit_lossy=false` | `Passed` |
| 空数据展示 | App 显示近 30 天销售额、回款和订单数均为 0，并明确说明无法生成有意义的趋势图；同时显示 10 条付款记录和累计收款金额 `¥5,992.78`。未发现独立 `result_visualization` 节点或虚假趋势线 | `Passed` |
| 有数据图表 | 本轮查询区间为零销售数据，未产生 `result_visualization`；本轮没有证明有数据时的图表渲染 | `Blocked` |
| 清理 | 依据会话列表 UI tree 真实点击本轮首项“删除会话”；目标会话从列表消失。服务端会话、消息和草稿计数回到发送前水平；crash buffer 为 0 行 | `Passed` |

## 数据与证据

- 发送前计数：`users=3`、`stores=2`、`store_memberships=2`、`products=693`、`customers=84`、`finance_records=2661`、`agent_conversations=15`、`agent_messages=37`、`agent_drafts=0`、`agent_run_audits=82`、`agent_run_audit_events=1256`、`agent_context_checkpoints=0`。
- 本轮最新 audit：`tool_count=2`、`event_count=30`、`emitted_event_count=30`、`status=completed`、`mode=tool_query_llm_native`、`llm_status=native_continuation_completed`。
- 清理后计数：`agent_conversations=15`、`agent_messages=37`、`agent_drafts=0`、`agent_run_audits=83`、`agent_run_audit_events=1286`；`products`、`customers`、`finance_records` 均未变化。
- 关键证据目录：`testing/Agent/客户端/artifacts/20260906-agent-phase2-wave36-physical-multitool-chart-025/`。
- 关键文件：`08-input-final-ui.xml`、`09-send-tap.txt`、`10-after-send-2s-ui.xml`、`12-after-send-25s-ui.xml`、`13-logcat-safe.txt`、`15-server-after-audit.txt`、`16-run-218-events.txt`、`19-session-list-ui.xml`、`20-session-delete-tap.txt`、`23-server-after-cleanup-counts.txt`、`24-crash-buffer.txt`。

## 结论

`AG-CLI-AND-003` 本轮完成了真实多工具调用和零值空状态验证；有数据图表分支仍未收敛，综合状态保持 `Blocked`。本轮未修改产品源码、服务端、数据库结构、账号、密码或 API Key。
