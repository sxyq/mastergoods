# 2026-09-06 Wave 44 Android 真机登录前置复测

## 结论

设备恢复后重新按 Android UI 流程验证登录。当前登录页保留第二个已有测试账号，依据当次 UI tree 的登录按钮 bounds `[116,1429][964,1555]` 计算并点击中心 `(540,1492)`；请求 `/v1/auth/login` 返回 HTTP `422`，App 仍在登录页，未进入 Agent 页面。

## 环境与证据

- 设备：物理 Android `d715a3a4`，未启动模拟器。
- App：`com.zhihuiji.app` `1.0.0`。
- 运行配置：`https://oneapi.sxyq27.online/v1`、`deepseek-v4-flash-0731`、`chat_completions`。
- 证据目录：`testing/Agent/客户端/artifacts/20260906-agent-phase2-wave44-physical-login-422-032/`。
- API Key、密码、Authorization、Cookie 和完整认证载荷未写入证据。

## 结果

| 项目 | 实际结果 | 状态 |
|---|---|---|
| 登录按钮真实点击 | 从 UI tree 取得 bounds 并点击 `(540,1492)`；HTTP `422`；最终 UI 仍为登录页 | `Blocked` |
| Agent 用例进入 | 未建立会话，未进入 Agent UI，未发送 Agent 请求 | `Blocked` |
| App 稳定性 | crash buffer 为空 | `Passed` |
| 线上服务只读状态 | `https://zhj-api.sxyq27.online/healthz` 已在本轮前置核对为 HTTP `200` / `ok`；8220 SSH 仍未提供可用 shell | `Blocked` |

本轮没有会话、run、消息、草稿、SSE 或业务写入证据，未重置账号密码，未修改线上配置。此前 Wave 43 的两个账号均为 HTTP `422`，本轮结果与其一致。

解除条件：取得可用的现有测试账号登录条件，并保持设备以物理 ADB serial 在线；登录成功后再按 `AG-CLI-AND-003/004/006/007/008/010` 逐项执行真实 UI 测试。
