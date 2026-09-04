# 2026-09-04 Android Wave 23：取消与停止接收真实点击复测

## 结论

本报告记录 `AG-CLI-AND-P2-CANCEL-RERUN-013` 的独立 Android 真实点击复测，不修改 Wave 21、Wave 22 或第一次取消失败尝试的历史记录。

| 用例 | 范围 | 状态 |
|---|---|---|
| `AG-CLI-AND-P2-CANCEL-UI-013` | Android 真实发送、停止接收、取消态展示 | `Passed` |
| `AG-CLI-AND-P2-CANCEL-RERUN-013` | 服务端取消、审计事件、无增量和业务表核对 | `Blocked` |
| `AG-CLI-AND-P2-CANCEL-CLEANUP-013` | App 清空对话、删除会话、本地重置 | `Blocked` |

客户端确实完成了两次真实点击：发送按钮中心 `(634,550)`，停止按钮由即时 UI tree 计算为 `(634,1140)`。App 进入“正在处理当前问题”，停止后显示“已取消”和“运行取消”，最终没有显示回答正文。App 日志还记录了 `POST /v2/agent/runs/<run_id>/cancel` 返回 HTTP `200`。

完整用例暂不能判定为 `Passed`。OkHttp 日志只保留了请求/响应头，没有保留 SSE 事件正文，因此无法从客户端日志证明停止后没有新的 `answer_delta`；本轮目标主机 `root@8.220.206.9` 的公钥认证失败，且当前环境没有可用的 root SSH 密码，无法读取本 run 的服务端 audit、事件序列和最终 PostgreSQL 计数。停止后的 UI 没有出现“服务端已确认取消生成”，所以 HTTP `200` 不扩展为服务端取消成功。

## 环境与基线

- 设备：`emulator-5554`，Android API 34。
- App：`com.zhihuiji.app`，`versionName=1.0.0`，`versionCode=1`，Activity 为 `com.zhihuiji.app/.MainActivity`。
- API：`https://zhj-api.sxyq27.online`；本轮 `/healthz` 返回 HTTP `200`，响应体为 `ok`。
- API 容器：`sxyq27-zhj-api:20260904T1316-customer-sync-version-48532083`。
- 本轮账号通过 App 正常登录流程进入工作台；报告和证据不保存账号、密码、Token、Cookie、Authorization 或完整认证载荷。
- 发送前数据库基线来自 `42-db-before-agent.txt`：`products=693`、`customers=84`、`finance_records=2661`、`agent_conversations=9`、`agent_messages=23`、`agent_drafts=0`、`agent_run_audits=56`、`agent_run_audit_events=659`。基线中的历史 `confirmation_pending` 运行不作为本轮活动 run。

## 真实点击与客户端状态

1. App 已在独立新对话中装载完整英文长提示词，内容要求生成长篇库存管理教程并禁止工具调用；提示词在 UI tree 中可见后才执行发送。
2. 发送按钮父节点 bounds 为 `[586,502][682,598]`，由 UI tree 计算中心 `(634,550)`；执行 `adb shell input tap 634 550`。
3. 发送后的即时 UI tree 出现“正在处理当前问题”和“处理中”，停止控件 content-desc 为“停止接收”，其 bounds 为 `[610,1116][658,1164]`；可点击父节点 bounds 为 `[586,1092][682,1188]`，中心 `(634,1140)`。
4. 执行 `adb shell input tap 634 1140` 后，即时和等待后的 UI tree 均显示“已取消”“运行取消”“已停止本机接收，正在请求服务端取消”。停止后没有新的回答正文出现在 UI tree 中。

客户端日志中的网络摘要为：

- `POST /v2/agent/chat/stream`：HTTP `200`，`Content-Type: text/event-stream`。
- `POST /v2/agent/runs/ac3933f8-cb55-40c8-b1b7-af2b58c6d5e7/cancel`：HTTP `200`。
- App 清理时 `DELETE /v2/agent/conversations/186`：HTTP `200`。

`ac3933f8-cb55-40c8-b1b7-af2b58c6d5e7` 是从 App 的取消请求路径取得的本轮 run 标识；`186` 是 App 删除本轮会话时实际调用的会话标识。服务端 audit 的 `status`、`event_type`、`event_count`、`audit_lossy` 和消息行未通过数据库或认证 API 核验。

## 清理与最终设备状态

- 依据最终 UI tree 计算“清空对话”父节点中心 `(640,184)` 并真实点击；对话视图回到空输入框。
- 依据会话列表 UI tree 定位本轮第一项“Write a long tutorial on”，其“删除会话” bounds 为 `[610,343][646,379]`，中心 `(628,361)`；真实点击后该会话从列表消失。
- App 删除会话请求 HTTP `200`，但服务端数据库残留没有独立核对，因此服务端清理归入 `Blocked`。
- `pm clear com.zhihuiji.app` 返回 `Success`；重启 Activity 后登录页显示“手机号”“密码”“登录并进入首页”，crash buffer 为 `0` 行。
- 3 个可能含登录诊断数据的 `/tmp/zhj-wave23-login-*` 文件已从 `/tmp` 精确移动到 macOS 废纸篓，源路径均已确认不存在；没有删除其他临时文件或用户数据。

## 证据索引

证据目录：`testing/Agent/客户端/artifacts/20260904-agent-phase2-wave23-AG-CLI-AND-P2-CANCEL-RERUN-013/`。

- 发送前：`90-current-before-send-ui.xml`、`91-current-before-send.png`。
- 发送和停止：`93-send-tap-start.txt`、`94-after-send-immediate-ui.xml`、`95-stop-coordinate-from-ui.txt`、`96-stop-tap-start.txt`、`97-after-stop-immediate-ui.xml`、`98-after-stop-immediate.png`。
- 终态和日志：`101-after-stop-final-ui.xml`、`102-after-stop-final.png`、`103-after-stop-final-logcat-filtered-redacted.txt`、`104-after-stop-final-capture-time.txt`。
- App 清理：`105-clear-coordinate-from-ui.txt`、`107-after-clear-ui.xml`、`109-after-clear-logcat-filtered-redacted.txt`、`111-session-list-coordinate-from-ui.txt`、`113-session-list-ui.xml`、`114-session-list.png`、`116-delete-coordinate-from-ui.txt`、`120-after-delete-tap-logcat-filtered-redacted.txt`。
- 最终状态：`124-pm-clear.txt`、`125-app-restart.txt`、`126-final-login-ui.xml`、`127-final-login.png`、`128-final-crash-buffer.txt`、`129-final-package-state.txt`。
- 前置与限制：`00-environment.txt`、`00-healthz.txt`、`00-healthz-body.txt`、`10-server-preflight.txt`、`42-db-before-agent.txt`；匿名 audit/会话探针只保留命令返回的 `401`，含 `Set-Cookie` 的响应文件已清理。

## 限制与解除条件

1. 需要恢复对目标 `8.220.206.9` 的已授权 root SSH 登录，再查询本 run 的 `agent_run_audits` 和 `agent_run_audit_events`，确认服务端终态是否为 `cancelled`、事件序列是否包含 `run_cancelled`，并核对消息和业务表计数。
2. 需要保留一份不含认证字段的 SSE 事件摘要，或通过服务端审计事件核对停止后的最后事件，才能判定“停止后无 `answer_delta`”。
3. 目标模型 `glm-5.3-flash` 仍未成为实际运行模型；本轮没有执行工具、生图、上下文压缩、性能或 iOS 流程。iOS 继续为 `Deferred`。
