# 阶段二 Wave 26：物理 Android 登录前置复测

## 结论

`AG-CLI-AND-P2-LOGIN-PREFLIGHT-017` 为 `Blocked`。本轮使用物理设备 `d715a3a4` 和当前 Debug APK，通过 UI tree bounds 真实点击登录；App 请求 `/v1/auth/login` 返回 HTTP `422`，没有建立会话。因此 003、004、006、007、008、010 没有开始，不能写成 Agent 用例通过。

## 运行条件

- 设备：`d715a3a4`，型号 `25010PN30C`；未使用模拟器。
- App：`com.zhihuiji.app`，版本 `1.0.0`。
- APK：`tmp/build/gradle-output/android/app/outputs/apk/debug/app-debug.apk`，SHA-256 `fbbfae46dc978a75d8b357cda9aff244455cd7e9f6f9a68244adf25f338b0639`。
- Provider：`https://oneapi.sxyq27.online/v1`、`deepseek-v4-flash-0731`、`chat_completions`；API Key 未记录。

## 真实操作与结果

1. 安装当前 APK，清理本地 App 数据并启动 `MainActivity`。
2. 从实时 UI tree 读取手机号、密码和登录按钮 bounds。
3. 通过 ADB `input tap` 和 `input text` 填写已知演示账号，按实时 bounds 点击“登录并进入首页”。
4. App 实际请求 `https://zhj-api.sxyq27.online/v1/auth/login`，返回 HTTP `422`；UI 仍显示登录页，未取得 session。

本轮没有重置账号密码、修改 API Key、调用 Agent 接口替代 UI，也没有生成会话、run、消息、草稿或业务对象。crash buffer 未发现 `com.zhihuiji.app` 崩溃。

## 服务端只读复核

2026-09-05 14:02 直连 `8.220.206.9` 的 SSH 检查成功。`sxyq27-zhj-api`、PostgreSQL（healthy）和 Redis 容器均在线；API 容器当前非敏感运行配置为 `https://oneapi.sxyq27.online/v1`、`deepseek-v4-flash-0731`、`chat_completions`。数据库账号状态查询未取得结果，不能据此推断账号可用，也没有执行任何数据库或配置修改。详见脱敏记录：`客户端/artifacts/20260905-agent-phase2-wave26-physical-login-017/05-server-readonly-observation.txt`。

## 证据

- 前置 UI tree 与截图：`客户端/artifacts/20260905-agent-phase26-physical-preflight-017/`
- 脱敏登录 UI tree、HTTP/logcat 摘要和环境结论：`客户端/artifacts/20260905-agent-phase26-physical-login-017/`

## 解除条件

需要准确且已授权的现有测试账号登录条件，或账号负责人完成服务端账号恢复后通知复测。凭据不得写入证据；登录成功后再按 003、004、006、007、008、010 顺序执行真实 UI 测试。
