# 2026-09-06 Wave 50 Android 真机断线重连

## 结论

物理设备 `d715a3a4` 通过 Android UI 发起长查询。关闭 Wi-Fi 后，App 显示 `重连中... (2/3)`，并记录 3 次 `UnknownHostException`；恢复 Wi-Fi 后重新建立 SSE，最终显示 10 条销售数据和回答，未重复展示第二条用户消息。重连 UI 子项为 `Passed`。服务端形成两个独立且均为 `completed` 的 run，因此 `AG-CLI-AND-006` 完整断线重连仍为 `Failed`，需要确认重连是否允许重新执行。

## 真实 UI 与服务端证据

- 设备为物理 Android `d715a3a4`，未启动模拟器；发送、断开 Wi-Fi、恢复 Wi-Fi 和结果观察均通过 ADB/UI 操作完成。
- 断线期间 UI tree 显示 `重连中... (2/3)`；恢复后 UI 显示完整回答和 10 条销售数据，第二条用户消息未重复出现。
- 服务端 run `0af76c9b-0438-4014-8777-948550e8502e` 为 `completed`、9 个事件；run `49eb470a-b71f-4919-9da6-1cf60beda09c` 为 `completed`、20 个事件。两者均已在服务端摘要中核对。
- App 日志为 3 次 `UnknownHostException` 重试；没有 crash。运行配置为 `https://oneapi.sxyq27.online/v1`、`deepseek-v4-flash-0731/chat_completions`，敏感认证材料未写入证据。

## 数据与清理

发送后计数为 `agent_conversations=18`、`agent_messages=51`、`agent_drafts=0`、`agent_run_audits=98`、`agent_run_audit_events=1552`、`agent_context_checkpoints=0`；清理后为 `17/47/0/98/1552/0`。`products=693`、`customers=84`、`finance_records=2661` 全程未变化。App 会话已通过真实 UI 删除，`pm clear` 成功，最终登录页可见，crash buffer 为 0 行。

证据目录：`testing/Agent/客户端/artifacts/20260906-agent-phase2-wave50-physical-reconnect-043/`。关键证据为 `07-after-send-0.5s-ui.xml`、`13-immediate-disconnect-ui.xml`、`15-disconnect-2s-ui.xml`、`17-after-reconnect-10s-ui.xml`、`19-after-reconnect-25s-ui.xml`、`22-logcat-safe.txt`、`23-server-counts.txt`、`24-server-latest-events.txt` 和 `32-server-final-counts.txt`。

