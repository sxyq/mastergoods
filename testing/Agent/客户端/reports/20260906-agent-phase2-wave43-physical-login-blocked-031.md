# 2026-09-06 Wave 43 Android 真机登录前置复测

## 结论

本轮未进入 Agent 页面。物理设备 `d715a3a4` 上按实时 UI tree 完成两次账号登录输入和登录按钮点击，两个已有测试账号均返回 HTTP `422`，App 保持在登录页。健康接口仍返回 HTTP `200`，因此当前证据只支持账号登录前置未收敛，不能据此判断 Agent 服务故障。

## 环境

- 设备：物理 Android `d715a3a4`；未启动模拟器。
- App：`com.zhihuiji.app` `1.0.0`。
- 运行配置：`https://oneapi.sxyq27.online/v1`、`deepseek-v4-flash-0731`、`chat_completions`。
- API Key、密码、Authorization、Cookie 和完整认证载荷未写入证据。

## 真实 UI 结果

| 动作 | 实际结果 | 状态 |
|---|---|---|
| 第一个已有测试账号登录 | 从登录页 UI tree 取得手机号、密码和按钮 bounds；真实点击登录后 `/v1/auth/login` 返回 HTTP `422`；仍在登录页 | `Blocked` |
| 第二个已有测试账号登录 | 清空手机号后重新输入；从同一轮 UI tree 取得登录按钮 bounds；真实点击后 `/v1/auth/login` 返回 HTTP `422`；仍在登录页 | `Blocked` |
| App 稳定性 | 两次操作未出现 App crash；crash buffer 为空 | `Passed` |
| 设备连接 | 两次点击完成后设备从 ADB 消失，随后 `adb devices -l` 为空 | `Blocked` |

## 服务端只读核对

- `https://zhj-api.sxyq27.online/healthz` 返回 `ok` / HTTP `200`。
- `https://oneapi.sxyq27.online/healthz` 返回 New API 页面 / HTTP `200`，这不是 Agent 完整链路结果。
- 直接连接 `8.220.206.9:22` 在 SSH banner 阶段被服务端关闭；未取得 PostgreSQL 计数、run audit 或事件记录。

## 证据与限制

证据目录为 `testing/Agent/客户端/artifacts/20260906-agent-phase2-wave43-physical-login-blocked-031/`，包含脱敏登录 UI tree、截图、logcat 和 crash buffer。当前没有会话、Agent run、SSE 事件、草稿或业务写入证据；`AG-CLI-AND-003/004/006/007/008/010` 均未开始本轮执行，保持原有各自状态。

解除条件：设备重新以非 `emulator-*` serial 出现，且账号负责人提供可用的现有登录条件或完成线上账号恢复；随后重新通过 UI 登录，再逐用例执行真实点击、UI tree、截图、脱敏日志、SSE/HTTP 摘要和服务端计数采集。

本轮未修改产品源码、API Key、线上配置或账号密码。
