# 2026-09-03 Android Wave 18：create_customer 草稿确认与修复复测

## 结论

本 Wave 包含一次完整的客户创建草稿确认和一次修复后的登录复测。首轮真实输入、SSE、覆盖式确认和服务端正式写入均完成，但确认后对话卡片仍显示旧的 `active / 运行待确认 / 尚未执行业务写入` 状态，客户端整体结果为 `Failed`。新 APK 已构建、安装并完成真实登录点击；复测因 `zhj-api.sxyq27.online:443` 当前拒绝连接，未能重新进入 Agent，修复后的卡片状态保持 `Blocked`，没有把它写成通过。2026-09-03 14:33 的入口复核仍确认 443 无监听，本轮没有重复登录或进入 Agent。

| 用例 | 范围 | 结果 |
|---|---|---|
| `AG-CLI-AND-P2-DRAFT-CONFIRM-CUSTOMER-001` | 真实生成 `create_customer` 草稿并点击“允许一次” | `Failed` |
| `AG-CLI-AND-P2-DRAFT-CONFIRM-CUSTOMER-UI-FIX-RERUN-001` | 新 APK 登录并复测确认后卡片状态 | `Blocked` |
| `AG-CLI-AND-P2-DRAFT-CONFIRM-CUSTOMER-CLEANUP-001` | 测试客户、草稿、会话和 App 本地状态清理 | `Passed` |

## 环境与实时前置

- 设备：`emulator-5554`，Android API 34。
- App：`com.zhihuiji.app`，versionName `1.0.0`，启动 Activity 为 `com.zhihuiji.app/.MainActivity`。
- 目标服务：8220，`8.220.206.9`；PostgreSQL V42；当前 API 镜像为 `sxyq27-zhj-api:20260902T024500-agent-cancel-9536df5b`。
- 运行时模型：`gpt-5.6-luna/chat_completions`；目标 `glm-5.3-flash` 仍未成为实际运行模型，相关目标保持 `Blocked`。
- 首轮执行时公网入口可返回认证响应；本次修复复测的实时检查在 2026-09-03 13:44–13:47 显示 8220 的 Nginx、Docker、API 容器和 PostgreSQL 均在运行，但 8220 没有 443 监听，`https://zhj-api.sxyq27.online/` 为 HTTP `000`/连接拒绝。容器本机 `127.0.0.1:18080` 仍返回认证保护响应，HTTP 80 返回默认 Nginx 页面；旧入口 `https://sxyq27.online/zhj-api/` 返回 410。

证据：

- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave18-android-create-customer-confirm-001/109-server-before-send-live.txt`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave18-android-create-customer-confirm-001/110-public-healthz-headers-redacted.txt`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave18-android-create-customer-confirm-001/276-public-api-entry-probe.txt`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave18-android-create-customer-confirm-001/277-8220-live-entry-state.txt`

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

首轮确认前客户数为 `83`，确认后为 `84`；通过 App 删除测试客户后恢复为 `83`。最终数据库计数为：

`users=3 | stores=2 | store_memberships=2 | products=693 | customers=83 | finance_records=2661 | agent_conversations=10 | agent_messages=25 | agent_drafts=0 | agent_run_audits=50 | agent_run_audit_events=573`

目标手机号、draft `15`、conversation `176` 和对应消息均为 `0`。修复复测的登录失败没有建立会话，数据库计数保持 `3|2|2|693|83|2661|10|25|0|50|573`。

测试结束时执行 `pm clear com.zhihuiji.app`，随后重启 Activity。最终 UI tree 回到空白登录页，crash buffer 为 `0` 行。新增证据文件中的账号和密码显示均已脱敏；重点复核旧 logcat 和认证响应头文件，命中内容只包含 `[REDACTED]` 占位符，未发现真实认证值。

## 未执行项与风险

- 新 APK 修复后的 Agent 确认卡片复测等待公网 HTTPS 入口恢复；当前不能确认修复后的 UI 结果。
- 确认成功后的重复确认、并发确认、图片生成、取消/断线、上下文压缩、性能和 iOS 本轮未执行。
- 未修改 8220 Nginx、容器、数据库或账号配置；本轮只安装 Android APK、执行 App 点击、读取服务状态并清理模拟器。
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
