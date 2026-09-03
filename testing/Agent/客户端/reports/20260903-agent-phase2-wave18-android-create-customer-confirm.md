# 2026-09-03 - 09-04 Android Wave 18：create_customer 草稿确认、幂等竞争与修复复测

## 结论

本 Wave 先完成了首轮 `create_customer` 确认和客户端修复，再在公网入口恢复后执行了 `007` 真实复测。首轮客户 `87` 的服务端写入成功，但客户端卡片仍保留旧状态，整体为 `Failed`；`007` 中首次确认、重复确认和并发确认的服务端业务结果均有真实证据，正式写入各只发生一次，清理也已完成。`007` 的客户端卡片同时出现 `状态：confirmed` 和旧文案“草稿已生成，等待用户确认后才会写入正式业务数据”，且 run audit 仍为 `confirmation_pending`，因此客户端/审计一致性仍为 `Failed`，不能把整个闭环写成 `Passed`。

| 用例 | 范围 | 结果 |
|---|---|---|
| `AG-CLI-AND-P2-DRAFT-CONFIRM-CUSTOMER-001` | 真实生成 `create_customer` 草稿并点击“允许一次” | `Failed` |
| `AG-CLI-AND-P2-DRAFT-CONFIRM-CUSTOMER-UI-FIX-RERUN-001` | 新 APK 登录并复测确认后卡片状态 | `Blocked` |
| `AG-CLI-AND-P2-DRAFT-CONFIRM-CUSTOMER-CLEANUP-001` | 测试客户、草稿、会话和 App 本地状态清理 | `Passed` |
| `AG-CLI-AND-P2-DRAFT-CONFIRM-CUSTOMER-REAL-RERUN-007` | 入口恢复后的真实首次确认闭环 | `Failed` |
| `AG-CLI-AND-P2-DRAFT-CONFIRM-CUSTOMER-REPEAT-007` | 同一草稿重复确认 | `Passed` |
| `AG-CLI-AND-P2-DRAFT-CONFIRM-CUSTOMER-CONCURRENT-007` | 同一草稿并发确认 | `Passed` |
| `AG-CLI-AND-P2-DRAFT-CONFIRM-CUSTOMER-AUDIT-007` | 确认后的 run audit 终态对齐 | `Failed` |
| `AG-CLI-AND-P2-DRAFT-CONFIRM-CUSTOMER-CLEANUP-007` | 测试客户、草稿、会话和 App 本地状态清理 | `Passed` |

## 环境与实时前置

- 设备：`emulator-5554`，Android API 34。
- App：`com.zhihuiji.app`，versionName `1.0.0`，启动 Activity 为 `com.zhihuiji.app/.MainActivity`。
- 目标服务：8220，`8.220.206.9`；PostgreSQL V42；当前 API 镜像为 `sxyq27-zhj-api:20260902T024500-agent-cancel-9536df5b`。
- 运行时模型：`gpt-5.6-luna/chat_completions`；目标 `glm-5.3-flash` 仍未成为实际运行模型，相关目标保持 `Blocked`。
- 首轮和修复阻塞复测期间公网入口曾不可用；`007` 于 2026-09-04 01:40–02:47 在入口恢复后完成真实 App 流程。最终公网 `/healthz` 返回 HTTP `200`，8220 API、PostgreSQL 和 Redis 运行状态与证据一致。

证据：

- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave18-android-create-customer-confirm-001/109-server-before-send-live.txt`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave18-android-create-customer-confirm-001/110-public-healthz-headers-redacted.txt`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave18-android-create-customer-confirm-001/276-public-api-entry-probe.txt`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave18-android-create-customer-confirm-001/277-8220-live-entry-state.txt`
- `testing/Agent/客户端/artifacts/20260904-agent-phase2-wave18-android-create-customer-confirm-007/128-public-healthz-final.txt`
- `testing/Agent/客户端/artifacts/20260904-agent-phase2-wave18-android-create-customer-confirm-007/127-server-runtime-final.txt`

### 2026-09-03 14:20–14:33 入口恢复复核：Blocked

本次只执行阶段二第 0 步前置检查，没有重新输入账号、点击登录、发送 Agent 请求或操作草稿确认：

- DNS `zhj-api.sxyq27.online` 仍解析到 `8.220.206.9`。
- 本机 HTTPS 探针仍为 HTTP `000`，连接 `8.220.206.9:443` 被拒绝；HTTP 80 返回默认 Nginx HTML 页面。
- 8220 远端 `nginx`、`docker`、`sxyq27-zhj-api` 和 PostgreSQL 均正常，PostgreSQL health 为 `healthy`；Nginx `sites-enabled` 仍只有 `default`，域名配置文件只声明 80，监听列表没有 443；容器本机 `127.0.0.1:18080` 的认证入口返回 HTTP `405 application/json`。
- `emulator-5554` 在线，新 APK `com.zhihuiji.app` 1.0.0 已安装，当前 UI tree 和截图为干净登录页，crash buffer 为 0 行。

因此本次入口恢复复核为 `Blocked`，没有新增会话、草稿、消息、审计或业务数据变化，也没有对线上 Nginx、容器、数据库和账号配置做修改。解除条件仍是恢复并验证 `https://zhj-api.sxyq27.online/` 的公网 HTTPS 443 反向代理。

新增证据：

- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave18-android-create-customer-confirm-001/286-public-api-entry-recheck-20260903T1432.txt`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave18-android-create-customer-confirm-001/287-8220-nginx-entry-recheck-20260903T1432.txt`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave18-android-create-customer-confirm-001/288-android-precondition-recheck-20260903T1432.txt`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave18-android-create-customer-confirm-001/289-login-page-ui-20260903T1432.xml`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave18-android-create-customer-confirm-001/290-login-page-ui-summary-20260903T1432.txt`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave18-android-create-customer-confirm-001/291-login-page-20260903T1432.png`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave18-android-create-customer-confirm-001/292-crash-buffer-20260903T1432.txt`

## 首轮完整确认：Failed

### 真实输入、SSE 和确认点击

App 中真实输入：

`新增客户名称测试客户手机号[REDACTED_TEST_PHONE]先生成草稿不要直接保存`

UI tree 确认输入框和发送控件后，App 真实调用 `POST /v2/agent/chat/stream`，返回 HTTP `200`、`text/event-stream`，服务端生成：

- run：`5eb6b1f7-c75b-47cb-84d3-0d0261916ae0`
- conversation：`176`
- draft：`15`
- 工具：`create_customer`

确认覆盖层显示后，按 UI tree 得到“允许一次”父节点中心 `(492,851)`，执行一次真实点击。App 随后调用 `/v2/agent/drafts/15/confirm`，返回 HTTP `200`；服务端新增客户 ID `87`，draft 状态为 `confirmed`，`business_reference=create_customer:87`。服务端审计和事件序列包含 `draft_created`、确认和 `run_completed`。

证据：

- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave18-android-create-customer-confirm-001/117-send-coordinate.txt`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave18-android-create-customer-confirm-001/131-server-before-confirm-evidence.txt`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave18-android-create-customer-confirm-001/132-confirm-coordinate.txt`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave18-android-create-customer-confirm-001/143-server-after-confirm-evidence.txt`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave18-android-create-customer-confirm-001/144-server-after-confirm-db.txt`

### 客户端结果边界

确认接口成功后，App 对话结果卡片仍保留旧的 active/待确认状态和“尚未执行业务写入”文案。服务端正式写入已发生，客户端显示与服务端状态不一致，因此整体用例为 `Failed`。该结果没有扩展为 Android 确认闭环通过。

## 修复与复测：Blocked

已完成的客户端修复：

- `Code/frontend/android/feature/agent/src/main/java/com/zhihuiji/feature/agent/conversation/AgentChatViewModel.kt`：确认成功后按 `draftId` 更新对应消息的 draft、终态和回答状态。
- `Code/frontend/android/feature/agent/src/main/java/com/zhihuiji/feature/agent/result/ResultBlockRenderer.kt`：移除固定的未写入提示，按状态展示草稿字段。
- `Code/frontend/android/feature/agent/src/test/java/com/zhihuiji/feature/agent/conversation/AgentChatViewModelAnswerMergeTest.kt`：增加匹配 draft 更新和不同 draft 不受影响的测试。

代码级验证和安装：

```text
./gradlew :feature:agent:testDebugUnitTest --console=plain       BUILD SUCCESSFUL
./gradlew :app:assembleDebug --console=plain                    BUILD SUCCESSFUL
adb -s emulator-5554 install -r app-debug.apk                    Success
```

APK 路径为 `tmp/build/gradle-output/android/app/outputs/apk/debug/app-debug.apk`，本次安装后的 SHA-256 记录在 `279-new-apk-build-install-meta.txt`。

复测按 UI tree 真实输入已有账号并点击登录中心 `(360,857)`。App 日志记录：

```text
POST https://zhj-api.sxyq27.online/v1/auth/login
HTTP FAILED: java.net.ConnectException: Failed to connect to zhj-api.sxyq27.online/8.220.206.9:443
```

因此没有建立 session，没有进入首页或 Agent，没有重新生成草稿，也没有重新验证确认后的卡片文案。该复测结果为 `Blocked`，阻塞解除条件是恢复 `zhj-api.sxyq27.online` 的公网 HTTPS 443 反向代理，并完成一次匿名认证边界复核。

证据：

- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave18-android-create-customer-confirm-001/272-login-failure-ui-redacted.xml`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave18-android-create-customer-confirm-001/273-login-failure-ui-summary.txt`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave18-android-create-customer-confirm-001/274-login-failure-app-logcat-safe.txt`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave18-android-create-customer-confirm-001/276-public-api-entry-probe.txt`

## 数据和清理

首轮 `001` 确认前客户数为 `83`，确认后为 `84`；通过 App 删除测试客户后恢复为 `83`。该计数只对应首轮客户 `87`，不覆盖后续 `007`。

`007` 首次确认前数据库计数为 `users=3 | stores=2 | store_memberships=2 | products=693 | customers=83 | finance_records=2661 | agent_conversations=10 | agent_messages=25 | agent_drafts=0 | agent_run_audits=50 | agent_run_audit_events=573`。首次确认后客户 `88` 写入，重复确认保持目标客户 1 条；并发确认后客户 `89` 写入，两个竞争确认请求均返回幂等成功结果且没有第二条同手机号客户。清理后数据库计数为：

`users=3 | stores=2 | store_memberships=2 | products=693 | customers=83 | finance_records=2661 | agent_conversations=10 | agent_messages=25 | agent_drafts=0 | agent_run_audits=53 | agent_run_audit_events=604`

首轮目标手机号、draft `15`、conversation `176` 和对应消息均为 `0`。`007` 的目标客户 `88/89`、draft `16/17/18`、conversation `177/178/179` 和对应消息清理后均为 `0`；`135-db-target-residuals-after-server-cleanup.txt` 为 `0|0|0|0`。App 删除客户只清理本地 Room/同步队列，没有观察到远端客户删除请求，因此随后对 8220 本机 API 精确删除客户 `88/89`，两个请求均为 HTTP `200`。

测试结束时执行 `pm clear com.zhihuiji.app`，随后重启 Activity。最终 UI tree 回到空白登录页，crash buffer 为 `0` 行。新增证据文件中的账号和密码显示均已脱敏；重点复核旧 logcat 和认证响应头文件，命中内容只包含 `[REDACTED]` 占位符，未发现真实认证值。

## 未执行项与风险

- `007` 的服务端首次确认、重复确认、并发确认和清理已完成；客户端卡片旧文案与确认状态并存，run audit 保持 `confirmation_pending`，客户端/审计一致性仍为 `Failed`。
- `image_generate`、取消/断线、上下文压缩、性能和 iOS 本轮未执行；iOS 按计划为 `Deferred`。
- App 客户删除路径没有调用远端删除接口，当前由测试侧精确调用 8220 本机 API 完成客户 `88/89` 清理；该客户端删除能力仍需单独修复和复测。
- 运行时模型仍为 `gpt-5.6-luna/chat_completions`，目标 `glm-5.3-flash` 未成为实际运行模型，目标项保持 `Blocked`。
- 本轮未修改 8220 Nginx、容器、数据库结构或账号配置；执行过测试对象的精确数据删除、Android APK 安装、App 点击和模拟器本地状态清理。
- `ui_pick.py` 在本机 Python 运行时因 `str | None` 语法不兼容未执行；实际坐标均由同一份 UI tree 的 bounds 计算，点击已执行并保留坐标证据。

## 2026-09-03 19:35–19:44：模拟器恢复与入口复核

本轮根据用户要求重新启动误关闭的模拟器，并从阶段二前置步骤继续核对：

- 同一 AVD `Zhihuiji_API34` 已以前台 qemu 会话重新启动，未使用 `-wipe-data`；`emulator-5554` 回到 `device`，`sys.boot_completed=1`，启动 Activity 可解析。
- 使用 UI/ADB 验收规范重新安装新 APK，`adb install -r` 返回 `Success`；包为 `com.zhihuiji.app` 1.0.0，APK SHA-256 仍为 `384d98cdbd08081328e88604a757fc1ba95599379a4143500567abd79ae3c78c`。
- 真实启动 App 后，UI tree 显示空白登录页，账号、密码和登录控件均存在；本轮没有在公网入口不可用时输入凭据或点击登录。
- `zhj-api.sxyq27.online` DNS 仍解析到 `8.220.206.9`；HTTPS 根路径和 `/healthz` 均为 HTTP `000`、443 连接拒绝；HTTP `/healthz` 为默认 Nginx 页面 HTTP `404`。
- 8220 直连只读核对显示 Nginx、Docker、`sxyq27-zhj-api` 和 PostgreSQL 正常，PostgreSQL health 为 `healthy`；远端监听仍只有 80 和本机 18080，没有 443。

因此 `AG-CLI-AND-P2-DRAFT-CONFIRM-CUSTOMER-UI-FIX-RERUN-002` 继续为 `Blocked`。本轮没有创建 session、run、conversation、draft、消息、审计或业务数据，也没有修改线上 Nginx、容器、数据库、账号配置或 Android 源码。模拟器保持运行，入口恢复后从当前 AVD 登录页重新执行 Wave 18。

本轮证据位于：

- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave18-android-create-customer-confirm-002/00-environment.txt`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave18-android-create-customer-confirm-002/01-apk-install.txt`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave18-android-create-customer-confirm-002/02-activity-start.txt`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave18-android-create-customer-confirm-002/04-login-ui.xml`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave18-android-create-customer-confirm-002/06-login-screen.png`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave18-android-create-customer-confirm-002/07-crash-buffer.txt`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave18-android-create-customer-confirm-002/08-public-entry-probe.txt`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave18-android-create-customer-confirm-002/09-server-entry-probe.txt`

## 2026-09-03 23:44–23:50：模拟器再次启动，公网入口复核仍阻塞

本轮根据用户要求重新启动误关闭的模拟器，并继续从阶段二 Wave 18 的入口前置开始核对：

- `Zhihuiji_API34` 以前台方式启动，未使用 `-wipe-data`；`emulator-5554` 在线，`sys.boot_completed=1`。
- 新 APK `com.zhihuiji.app` 1.0.0 重新安装返回 `Success`，SHA-256 为 `384d98cdbd08081328e88604a757fc1ba95599379a4143500567abd79ae3c78c`；清空 App 本地状态后启动 Activity 成功。
- UI tree 和截图显示空白登录页，账号、密码和登录控件存在；crash buffer 为 0 行。本轮按计划没有在公网入口失败时输入凭据或点击登录。
- `zhj-api.sxyq27.online` 当前 DNS 为 `198.18.0.92`；HTTPS 根路径和 `/healthz` 均在 TLS 阶段失败，HTTP `/healthz` 为默认 404。直接核对 8220 显示 Nginx、Docker、API 容器和 PostgreSQL 正常，但监听只有 `80` 和本机 `18080`，没有 `443`；124 的 `/zhj-api/` 返回 `410`。

因此 `AG-CLI-AND-P2-DRAFT-CONFIRM-CUSTOMER-UI-FIX-RERUN-003` 为 `Blocked`。本轮没有建立 session、run、conversation、draft、消息、审计或业务数据，也没有修改 Android 源码、线上 Nginx、容器、数据库或账号配置。模拟器保持运行，公网 HTTPS 恢复并通过匿名入口复核后，从当前登录页继续真实点击测试。

本轮证据位于：

- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave18-android-create-customer-confirm-003/00-environment.txt`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave18-android-create-customer-confirm-003/01-apk-install.txt`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave18-android-create-customer-confirm-003/02-activity-start.txt`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave18-android-create-customer-confirm-003/04-login-ui.xml`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave18-android-create-customer-confirm-003/06-login-screen.png`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave18-android-create-customer-confirm-003/07-crash-buffer.txt`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave18-android-create-customer-confirm-003/08-public-entry-probe.txt`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave18-android-create-customer-confirm-003/09-server-entry-probe.txt`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave18-android-create-customer-confirm-003/10-conclusion.md`

## 2026-09-04 00:06–00:08：阶段二入口再次复核，仍未达到登录前置

本轮继续从当前 AVD 登录页复核 Wave 18：

- `emulator-5554` 仍为 `device`，`sys.boot_completed=1`；修复 APK 已安装，Activity 可解析，crash buffer 为 0 行。
- 公网 DNS 将 `zhj-api.sxyq27.online` 解析为 `198.18.0.92`；域名 HTTPS 和强制直连 8220 的 HTTPS 均在 TLS 阶段失败，返回 HTTP `000`/`SSL_ERROR_SYSCALL`。8220 HTTP `/healthz` 是默认 404，124 的 `/zhj-api/` 返回 410。
- 8220 远端 SSH 核对显示 Nginx、Docker、`sxyq27-zhj-api`、PostgreSQL 和 Redis 正常，但只监听 `80` 与本机 `18080`，没有公网 `443`。

因此 `AG-CLI-AND-P2-DRAFT-CONFIRM-CUSTOMER-UI-FIX-RERUN-004` 仍为 `Blocked`。本轮没有输入凭据、点击登录或调用 Agent，也没有产生 session、run、conversation、draft、消息、审计或业务数据；没有修改源码、线上配置、数据库或账号。公网 HTTPS 443 恢复并通过匿名入口核对后，继续从当前登录页进行真实点击测试。

本轮证据位于：

- `testing/Agent/客户端/artifacts/20260904-agent-phase2-wave18-android-create-customer-confirm-004/00-environment.txt`
- `testing/Agent/客户端/artifacts/20260904-agent-phase2-wave18-android-create-customer-confirm-004/01-apk-install.txt`
- `testing/Agent/客户端/artifacts/20260904-agent-phase2-wave18-android-create-customer-confirm-004/02-activity-start.txt`
- `testing/Agent/客户端/artifacts/20260904-agent-phase2-wave18-android-create-customer-confirm-004/04-login-ui.xml`
- `testing/Agent/客户端/artifacts/20260904-agent-phase2-wave18-android-create-customer-confirm-004/06-login-screen.png`
- `testing/Agent/客户端/artifacts/20260904-agent-phase2-wave18-android-create-customer-confirm-004/07-crash-buffer.txt`
- `testing/Agent/客户端/artifacts/20260904-agent-phase2-wave18-android-create-customer-confirm-004/08-public-entry-probe.txt`
- `testing/Agent/客户端/artifacts/20260904-agent-phase2-wave18-android-create-customer-confirm-004/09-server-entry-probe.txt`
- `testing/Agent/客户端/artifacts/20260904-agent-phase2-wave18-android-create-customer-confirm-004/10-conclusion.md`

## 2026-09-04 00:20–00:30：当前源码回归与 APK 重建安装，公网联调仍阻塞

本轮在继续等待公网入口恢复期间，补做当前源码和安装包的一致性核验：

- 强制执行 `./gradlew :feature:agent:testDebugUnitTest --rerun-tasks --console=plain`，结果 `BUILD SUCCESSFUL`，115 个任务实际执行。
- 强制执行 `./gradlew :app:assembleDebug --rerun-tasks --console=plain`，结果 `BUILD SUCCESSFUL`，648 个任务实际执行。
- 新构建 APK 已安装到 `emulator-5554`，`adb install -r` 和 `pm clear` 均返回 `Success`；宿主构建包与从模拟器读取的已安装 APK SHA-256 均为 `f7b92adb686c54be0450038fb854375a256f8f48f34ef61e6b07c7f19c902495`。
- Activity 启动成功，UI tree 仍为干净登录页，`crash buffer=0`。
- 公网探针仍为 HTTPS `000`/`SSL_ERROR_SYSCALL`；8220 的 API、PostgreSQL、Redis 正常，但没有 `443` 监听；124 的 `/zhj-api/` 返回 `410`。

因此 `AG-CLI-AND-P2-DRAFT-CONFIRM-CUSTOMER-UI-FIX-RERUN-005` 的代码级检查为通过，端到端 App 流程仍为 `Blocked`。本轮没有输入凭据、点击登录或调用 Agent，没有产生业务数据，也没有修改 Android 源码、线上配置、数据库或账号。公网 HTTPS 443 恢复并通过匿名入口核对后，继续从当前登录页执行真实点击测试。

本轮证据位于：

- `testing/Agent/客户端/artifacts/20260904-agent-phase2-wave18-android-create-customer-confirm-005/00-environment.txt`
- `testing/Agent/客户端/artifacts/20260904-agent-phase2-wave18-android-create-customer-confirm-005/01-apk-install.txt`
- `testing/Agent/客户端/artifacts/20260904-agent-phase2-wave18-android-create-customer-confirm-005/02-activity-start.txt`
- `testing/Agent/客户端/artifacts/20260904-agent-phase2-wave18-android-create-customer-confirm-005/04-login-ui.xml`
- `testing/Agent/客户端/artifacts/20260904-agent-phase2-wave18-android-create-customer-confirm-005/06-login-screen.png`
- `testing/Agent/客户端/artifacts/20260904-agent-phase2-wave18-android-create-customer-confirm-005/07-crash-buffer.txt`
- `testing/Agent/客户端/artifacts/20260904-agent-phase2-wave18-android-create-customer-confirm-005/08-public-entry-probe.txt`
- `testing/Agent/客户端/artifacts/20260904-agent-phase2-wave18-android-create-customer-confirm-005/09-server-entry-probe.txt`
- `testing/Agent/客户端/artifacts/20260904-agent-phase2-wave18-android-create-customer-confirm-005/10-conclusion.md`
- `testing/Agent/客户端/artifacts/20260904-agent-phase2-wave18-android-create-customer-confirm-005/11-agent-unit-test.log`
- `testing/Agent/客户端/artifacts/20260904-agent-phase2-wave18-android-create-customer-confirm-005/12-assemble-debug.log`
## 2026-09-04 00:48：8220 公网入口根因定位

本轮继续对 Wave 18 的公网前置做只读定位：

- 8220 的 API 容器、PostgreSQL、Redis、Docker 和 Nginx 均正常；API 绑定 `127.0.0.1:18080`，本机 `/`、`/healthz` 和 Agent 路由均返回认证保护或方法响应。
- `/etc/nginx/sites-available/zhj-api.sxyq27.online.conf` 文件存在，但 `/etc/nginx/sites-enabled/` 只有 `default` 链接；该文件只声明 HTTP `80`，没有 `443 ssl` 配置。
- 证书目录中未找到 `zhj-api` 对应证书；Nginx `-t` 语法检查通过。公网域名和强制直连 8220 的 HTTPS 仍为 `HTTP 000`/`SSL_ERROR_SYSCALL`，124 的旧 `/zhj-api/` 入口为 `410`。

因此 `AG-CLI-AND-P2-DRAFT-CONFIRM-CUSTOMER-ENTRY-DIAG-006` 为 `Blocked`。启用 8220 的 zhj-api 站点、配置匹配证书并 reload Nginx 属于线上变更，本轮未执行；没有输入账号、点击登录或产生数据库变化。入口恢复后继续从当前 APK 登录页真实点击 Wave 18。

本轮证据位于：

- `testing/Agent/客户端/artifacts/20260904-agent-phase2-wave18-android-create-customer-confirm-006/00-environment.txt`
- `testing/Agent/客户端/artifacts/20260904-agent-phase2-wave18-android-create-customer-confirm-006/08-public-entry-probe.txt`
- `testing/Agent/客户端/artifacts/20260904-agent-phase2-wave18-android-create-customer-confirm-006/09-server-entry-probe.txt`
- `testing/Agent/客户端/artifacts/20260904-agent-phase2-wave18-android-create-customer-confirm-006/10-conclusion.md`

## 2026-09-04 01:40–02:47：入口恢复后的真实复测（007）

入口恢复后，继续使用 `emulator-5554` 上的 `com.zhihuiji.app` 1.0.0，从 App UI 真实输入、发送并点击覆盖式确认。发送和确认控件均根据同一时刻的 UI tree bounds 定位；“允许一次”中心坐标为 `(492,851)`。本轮没有用脚本请求替代 App 的首次发送或确认点击。

### 首次确认：服务端业务 Passed，完整客户端闭环 Failed

测试前数据库计数为：

`users=3 | stores=2 | store_memberships=2 | products=693 | customers=83 | finance_records=2661 | agent_conversations=10 | agent_messages=25 | agent_drafts=0 | agent_run_audits=50 | agent_run_audit_events=573`

App 真实输入客户创建提示并点击发送后，服务端生成 run `4b569aa2-40b6-4fb0-8b60-7f1f1d99524b`、conversation `177`、draft `16`，工具为 `create_customer`。发送前的 UI 处于处理中，确认覆盖层显示“操作确认”“新建客户：测试客户”“草稿状态：待确认”；点击“允许一次”后，App 的确认请求返回 HTTP `200`。服务端计数显示客户从 `83` 增加到 `84`，draft `16` 为 `confirmed`，业务引用为 `create_customer:88`。

服务端业务确认满足一次写入条件，但确认后的 App 会话详情同时显示：

- `状态：confirmed · 新建客户：测试客户`
- 旧文案：`草稿已生成，等待用户确认后才会写入正式业务数据。`
- 底部状态：`草稿已确认，业务数据已写入`

同一张卡片的状态和说明互相矛盾，因此 `AG-CLI-AND-P2-DRAFT-CONFIRM-CUSTOMER-REAL-RERUN-007` 整体为 `Failed`；服务端单次写入子项为 `Passed`，客户端展示子项为 `Failed`。

### 重复确认：Passed

对已确认的 draft `16` 发起重复确认请求，结果为 `code=0`、状态 `confirmed`，目标客户记录仍为 1 条，没有重复创建客户。证据为 `23-repeat-confirm-result.txt`。该结果满足重复确认的幂等验收，`AG-CLI-AND-P2-DRAFT-CONFIRM-CUSTOMER-REPEAT-007` 为 `Passed`。

### 并发确认：Passed

通过 App 生成并发测试草稿 `17`、`18`，随后对 draft `18` 执行 App 确认和竞争确认请求。App 请求和竞争请求均返回 HTTP `200`，竞争结果为 `0 / confirmed / 18`；服务端只新增客户 `89` 一条，draft `18` 为 `confirmed`、业务引用为 `create_customer:89`。draft `17` 保持 `active`，未被确认，随后作为测试对象删除。`AG-CLI-AND-P2-DRAFT-CONFIRM-CUSTOMER-CONCURRENT-007` 在“最多一次正式写入、竞争请求幂等返回”口径下为 `Passed`。

并发批次还记录到 conversation `178` 的独立 `STREAM_ERROR`，它属于并发草稿生成的旁支运行，不改变 draft `18` 确认只产生一条客户的结果；该旁支不扩展为整体成功。

### 审计对齐：Failed

确认前后，run `4b569aa2-40b6-4fb0-8b60-7f1f1d99524b` 对应的 `agent_run_audits.status` 仍为 `confirmation_pending`；并发确认后的相关 run audit 也保留 `confirmation_pending`。草稿实体已经保存 `confirmed`、确认人、确认时间和 `business_reference`，但确认接口没有同步写入 audit 的确认结果或确认事件。当前代码中 `AgentTerminalStatus.CONFIRMATION_PENDING` 是草稿生成阶段的合法运行终态，但 `AgentDraftConfirmService.confirmDraft()` 没有更新 `RunAuditService`，所以“确认后审计与业务结果完全对齐”记为 `Failed`，暂不擅自把 audit 状态改写成 `completed`。

### 清理：Passed，但暴露 App 远端删除缺口

App 端按 UI 操作删除 draft `16/17/18` 和 conversation `177/178/179`；草稿列表为空，会话列表移除目标项。客户页面的删除操作只清理本地 Room/同步队列，App logcat 没有出现远端客户删除请求。随后对 8220 本机 API 精确删除客户 `88/89`，两个请求均返回 HTTP `200`。

清理后数据库计数为：

`users=3 | stores=2 | store_memberships=2 | products=693 | customers=83 | finance_records=2661 | agent_conversations=10 | agent_messages=25 | agent_drafts=0 | agent_run_audits=53 | agent_run_audit_events=604`

目标残留核对为 `0|0|0|0`；模拟器执行 `pm clear com.zhihuiji.app` 返回 `Success`，重启后回到空白登录页，crash buffer 为空。因此 `AG-CLI-AND-P2-DRAFT-CONFIRM-CUSTOMER-CLEANUP-007` 为 `Passed`，但 App 客户远端删除能力仍需单独修复。

### 007 证据索引

- 真实输入、发送和确认：`testing/Agent/客户端/artifacts/20260904-agent-phase2-wave18-android-create-customer-confirm-007/02-before-send-ui.xml`、`06-send-tap-start.txt`、`08-after-send-2s-ui.xml`、`13-confirm-before-ui.xml`、`16-confirm-tap-start.txt`、`18-after-confirm-4s-ui.xml`
- 服务端 before/after、draft 和 audit：`01-server-before-send.txt`、`12-server-before-confirm.txt`、`20-server-after-confirm.txt`、`47-server-concurrent-draft-current.txt`、`56-server-after-concurrent-confirm.txt`
- 重复和并发确认：`23-repeat-confirm-result.txt`、`49-direct-concurrent-result.tsv`、`50-direct-concurrent-http-status.txt`、`55-concurrent-app-logcat-safe.txt`
- 清理和最终状态：`125-db-final-counts.txt`、`126-db-target-residuals.txt`、`129-app-delete-logcat-safe.txt`、`133-server-customer-delete-status.txt`、`134-db-final-counts-after-server-cleanup.txt`、`135-db-target-residuals-after-server-cleanup.txt`、`138-pm-clear-result.txt`、`141-final-login-ui.xml`、`143-final-crash-buffer.txt`
- 入口和运行时：`127-server-runtime-final.txt`、`128-public-healthz-final.txt`

本轮实际运行模型仍为 `gpt-5.6-luna/chat_completions`，目标 `glm-5.3-flash` 未成为实际模型，目标项保持 `Blocked`。生图、取消/断线、上下文压缩、性能和 iOS 仍未执行。
