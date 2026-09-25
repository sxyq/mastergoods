# 2026-09-05 Wave 33-34 物理 Android / DeepSeek

## 环境

- 设备：物理 `d715a3a4`（`25010PN30C`），未启动模拟器。
- App：`com.zhihuiji.app` `1.0.0`；重新安装当前 Debug APK 后执行。
- 运行配置：`https://oneapi.sxyq27.online/v1`、`deepseek-v4-flash-0731`、`chat_completions`。
- API Key、密码、Authorization、Cookie 和完整认证载荷未写入证据。

## AG-CLI-AND-006 取消、断线与重连

- 真实 UI 输入长请求并点击发送；UI tree 出现 `停止接收`，依据 `[935,2207][998,2270]` 点击停止。
- App 先展示处理中，最终显示“暂时无法完成这次请求，请稍后重试”；HTTP stream 为 `200 text/event-stream`，服务端最新对应失败审计没有 `run_cancelled` 事件，故完整取消链路未通过。
- 线上审计与事件总量从 `76/958` 增至 `79/995`，清理后仍保留审计；业务表未变化。
- 状态：`Failed`（服务端终态/停止后增量未满足验收）；断线重连分支：`Blocked`（本轮未能建立可核对的断线恢复样本）。

证据目录：`artifacts/20260905-agent-phase2-wave33-physical-cancel-021/`。

## AG-CLI-AND-010 生图草稿确认

- 真实点击生图面板、文生图、输入描述和“开始生成”；直接生图端点返回 HTTP `422`。
- 真实 Agent 输入生图草稿请求并点击发送，HTTP `200 text/event-stream`，但服务端审计对应普通完成回答、`tool_count=0`，没有 `image_generate`、`draft_created` 或 Provider 结果。
- 线上数据库 `agent_drafts` 保持 `0`，正式业务表无变化；确认动作未到达草稿确认弹窗。
- 状态：`Failed`；失败后的 UI 重试/确认成功分支未到达，不能标记通过。

证据目录：`artifacts/20260905-agent-phase2-wave34-image-022/`。

## 清理

通过 App 会话列表删除本轮 21:34、21:39、21:53 测试会话，随后执行 `pm clear com.zhihuiji.app` 并重启，登录页可见，crash buffer 为空。最终线上计数：`agent_conversations=14`、`agent_messages=35`、`agent_drafts=0`、`agent_run_audits=79`、`agent_run_audit_events=995`、`products=693`、`customers=84`、`finance_records=2661`。

