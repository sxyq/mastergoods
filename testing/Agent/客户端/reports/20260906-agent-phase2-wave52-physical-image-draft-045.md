# 2026-09-06 Wave 52 Android 真机生图草稿确认

## 结论

物理设备 `d715a3a4` 保持在线，通过 Android UI 登录后的助手新建对话进入“生图”面板。依据 UI tree 真实点击“开始生成”后，App 请求 `/v2/agent/images/generate`，服务端返回 HTTP `422`。没有生成图片、`image_generate` 事件、`draft_card`、确认或拒绝按钮，也没有正式业务写入。因此 `AG-CLI-AND-010` 本轮为 `Failed`；确认和拒绝分支未到达，无法判定为通过。

## 真实 UI 与服务端证据

- 未启动模拟器；登录、助手、生图入口、文生图面板、描述输入和“开始生成”均通过物理 ADB/UI tree 完成。
- 生图面板 UI tree 显示文生图输入框 bounds `[95,1258][984,1615]`，按钮 bounds `[722,1653][984,1779]`；收起键盘后真实点击 `(853,1716)`。
- App 日志记录 `POST https://zhj-api.sxyq27.online/v2/agent/images/generate`，响应 HTTP `422`、`application/json`；响应内容未保存，认证材料已脱敏。
- 生成面板在错误后仍可见，没有图片或草稿确认 UI；App crash buffer 为 0 行。

## 数据与清理

最终计数为 `products=693`、`customers=84`、`finance_records=2661`、`agent_conversations=18`、`agent_messages=53`、`agent_drafts=0`、`agent_run_audits=101`、`agent_run_audit_events=1587`、`agent_context_checkpoints=0`，与生图请求前一致。`pm clear com.zhihuiji.app` 返回 `Success`，重启后最终 UI 为登录页，crash buffer 为 0 行。

证据目录：`testing/Agent/客户端/artifacts/20260906-agent-phase2-wave52-physical-image-draft-045/`。关键文件为 `07-image-tap-3s-ui.xml`、`09-image-prompt-entered-ui.xml`、`13-image-keyboard-closed-ui.xml`、`14-after-generate-8s-ui.xml`、`16-logcat-safe.txt`、`17-crash-buffer.txt`、`18-server-final-counts.txt`、`20-pm-clear.txt` 和 `21-final-login-ui.xml`。

