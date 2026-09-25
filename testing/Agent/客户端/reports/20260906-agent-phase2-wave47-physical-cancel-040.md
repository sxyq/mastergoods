# 2026-09-06 Wave 47 Android 真机取消分支

## 结论

物理设备 `d715a3a4` 在线，当前 App 使用 `deepseek-v4-flash-0731`。本轮按实时 UI tree 真实点击发送和停止；停止点击后请求很快进入完成态，服务端 run 为 `completed`，事件序列以 `run_completed` 结束，没有 `run_cancelled`。`AG-CLI-AND-006` 完整取消分支记为 `Failed`，客户端停止控件分支记为 `Passed`。

## 真实 UI 操作

- App：`com.zhihuiji.app` 1.0.0；设备 Android 16，未启动模拟器。
- 发送前输入框和发送控件由 UI tree 定位；真实发送中心为 `(966,1425)`。
- 发送约 0.4 秒后 UI tree 出现 `停止接收`，bounds `[903,2175][1029,2301]`，真实点击中心为 `(966,2238)`。
- 停止后 1 秒和 5 秒 UI 均显示 `已完成`，停止控件恢复为 `发送`；没有显示 `已取消` 或“服务端已确认取消生成”。
- App 日志记录 Agent stream HTTP `200`、`text/event-stream`；没有保存 SSE 正文、认证头或敏感配置。

## 服务端证据

- 发送前计数：`agent_conversations=17`、`agent_messages=47`、`agent_drafts=0`、`agent_run_audits=93`、`agent_run_audit_events=1477`、`agent_context_checkpoints=0`。
- 发送后计数：`18/49/0/94/1490/0`；业务表为 `products=693`、`customers=84`、`finance_records=2661`。
- 本轮最新 run 为 `5bac9c30-0918-4a3b-b7c7-496a2da63013`，状态 `completed`，`tool_count=1`、`event_count=13`、`emitted_event_count=13`、`audit_lossy=false`；事件包含 `run_started`、工具执行、`answer_delta`、`answer_completed`、`run_completed`，没有 `run_cancelled`。

## 清理与证据

- 通过 App 会话列表依据 UI tree 真实点击删除本轮会话；目标会话从列表消失。
- 服务端清理后计数：`agent_conversations=17`、`agent_messages=47`、`agent_drafts=0`、`agent_run_audits=94`、`agent_run_audit_events=1490`、`agent_context_checkpoints=0`；业务表仍为 `693/84/2661`。
- `pm clear com.zhihuiji.app` 返回 `Success`；重启 Activity 后登录页可见；crash buffer 为 0 行。
- 证据目录：`testing/Agent/客户端/artifacts/20260906-agent-phase2-wave47-physical-cancel-040/`。
- 关键证据：`07-after-send-0.4s-ui.xml`、`09-after-stop-1s-ui.xml`、`11-after-stop-5s-ui.xml`、`14-logcat-app-redacted.txt`、`15-crash-buffer.txt`、`16-server-cancel-counts-audits.txt`、`17-server-latest-audits.txt`、`33-server-after-cleanup-counts.txt`。

## 后续判断

本轮证明了物理设备上的停止控件可见并完成点击，但停止时序未抢在服务端完成之前，不能证明取消接口、`run_cancelled` 终态或停止后无增量。断线/重连、上下文压缩、完整错误矩阵和生图草稿确认仍按原状态继续。
