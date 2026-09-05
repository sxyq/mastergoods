# 2026-09-05 Android Wave 24：多工具与图表真实点击复测

## 结论

| 用例 | 范围 | 状态 |
|---|---|---|
| `AG-CLI-AND-P2-MULTITOOL-CHART-014` | 物理 Android 设备真实输入、发送、错误态与服务端前后核对 | `Blocked` |

物理设备 `d715a3a4` 曾在线并完成真实输入和发送点击，但当时设备上的 APK 是 8 月 1 日安装的旧包。App 实际请求了已退役的 `https://sxyq27.online/zhj-api/v2/agent/chat/stream`，服务端返回 HTTP `410`，因此没有进入多工具、`result_visualization`、图表或正式回答阶段，不能判定为 `Passed`。

当前源码构建产物已生成，但安装前物理设备断开，未安装新包、未继续点击，也未启动模拟器。用例完整重测需在设备重新连接后从新 APK 开始。

## 环境与前置

- 设备：物理 Android `d715a3a4`，USB，`25010PN30C/ishtar`；当前复核结束时 `adb devices -l` 为空。
- App：`com.zhihuiji.app`，`versionName=1.0.0`，`versionCode=1`；旧包最后安装时间为 `2026-08-01 02:45:57`。
- 服务器：8220 `8.220.206.9`，容器 `sxyq27-zhj-api:20260904T2003-agent-cancel-audit-391e3e3e`，Flyway 日志确认 43 个迁移已验证且数据库已是最新。
- 运行模型：`deepseek-v4-flash-0731`；Wire API：`chat_completions`；Base URL：`https://oneapi.sxyq27.online/v1`。API Key 未写入报告、日志或证据。
- 发送前服务端计数：`users=3|stores=2|store_memberships=2|products=693|customers=84|finance_records=2661|agent_conversations=11|agent_messages=27|agent_drafts=0|agent_run_audits=63|agent_run_audit_events=705`。

## 真实点击结果

1. 设备在线时，从 Agent 对话页 UI tree 定位 `EditText` 和发送控件；输入框最终 bounds 为 `[355,1299][887,1551]`，发送控件 bounds 为 `[935,1394][998,1457]`。
2. 真实点击发送中心 `(966,1425)` 后，UI 显示用户输入、`AI` 和 `SSE 连接失败: 410`；没有工具卡、图表、`result_block` 或正式回答。
3. 脱敏 App 日志记录实际请求：`POST https://sxyq27.online/zhj-api/v2/agent/chat/stream`，响应 `410`、`content-type: text/html`。该请求没有到达当前要求的 `https://zhj-api.sxyq27.online/`。
4. 服务端数据库前后核对未见本轮新的 run：复核时计数仍为 `11|27|0|63|705`，最新审计列表中没有本轮 09:06 请求对应的 run。

## 构建与安装边界

- `Code/frontend/android` 执行 `./gradlew :app:assembleDebug --no-daemon --console=plain`：`BUILD SUCCESSFUL`，648 个任务均已完成或为最新。
- 实际 APK 路径：`tmp/build/gradle-output/android/app/outputs/apk/debug/app-debug.apk`。
- APK SHA-256：`fbbfae46dc978a75d8b357cda9aff244455cd7e9f6f9a68244adf25f338b0639`。
- 首次安装命令使用了不存在的默认输出路径，未产生安装；改用实际产物安装时设备已断开，`adb` 返回 `device 'd715a3a4' not found`。设备包未被替换。

## 证据

证据目录：`testing/Agent/客户端/artifacts/20260905-agent-phase2-wave24-AG-CLI-AND-P2-MULTITOOL-CHART-014/`。

- 设备与 UI：`01-device-start.txt`、`02-before-input-ui.xml`、`03-prompt-entered-ui.xml`、`04-prompt-english-ui.xml`、`06-after-send-ui.xml`、`09-after-error-ui.xml` 及对应截图。
- App 日志：`07-logcat-filtered-redacted.txt`、`11-app-logcat-redacted.txt`、`08-crash-buffer.txt`。
- 服务端：`00-server-before.txt`。
- 构建与安装：`12-apk-sha256.txt`、`13-apk-install.txt`、`14-device-package.txt`。

未保留本轮原始未脱敏 logcat；其中曾出现认证头的文件已删除，只保留脱敏日志。没有输出或写入 API Key。

## 解除条件

设备重新连接后，安装并核对当前 APK，再从 UI tree 真实输入“销售趋势和现金流，用图表展示”等多工具提示；采集同一 run 的脱敏 SSE/HTTP 摘要、工具顺序、`result_block`、审计和数据库 before/after，完成后再判定 `Passed`、`Failed` 或 `Blocked`。
