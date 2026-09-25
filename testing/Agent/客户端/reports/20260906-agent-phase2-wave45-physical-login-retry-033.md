# 2026-09-06 Wave 45 Android 真机登录前置重试

## 结论

本轮在物理 Android 设备 `d715a3a4` 上重新安装并启动当前 App，按实时 UI tree 完成登录字段输入和登录按钮真实点击。`POST /v1/auth/login` 返回 HTTP `422`，App 仍停留登录页，未建立会话，未进入 Agent UI，因此没有继续执行阶段二未收敛用例。

## 环境与证据

- 设备：物理 Android `d715a3a4`；未启动模拟器。
- App：`com.zhihuiji.app` `1.0.0`。
- 运行配置：`https://oneapi.sxyq27.online/v1`、`deepseek-v4-flash-0731`、`chat_completions`。
- 证据目录：`testing/Agent/客户端/artifacts/20260906-agent-phase2-wave45-physical-login-retry-033/`。
- API Key、密码、Authorization、Cookie 和完整认证载荷未写入证据。

## 真实 UI 结果

| 动作 | 实际结果 | 状态 |
|---|---|---|
| 登录页启动 | `am start` 成功；登录页 UI tree 可读 | `Passed` |
| 登录字段输入 | 依据 UI tree 的手机号 bounds `[116,1009][964,1177]` 和密码 bounds `[116,1214][964,1382]` 完成真实输入；证据中的字段值已脱敏 | `Passed` |
| 登录按钮真实点击 | 依据 UI tree 的按钮 bounds `[116,1429][964,1555]` 点击中心 `(540,1492)`；`/v1/auth/login` 返回 HTTP `422` | `Blocked` |
| 登录后页面 | 仍为登录页，未建立会话，未进入 Agent UI | `Blocked` |
| App 稳定性 | crash buffer 为空 | `Passed` |

## 服务端与数据证据

- App 日志只保留脱敏 HTTP 元信息和状态码，没有保存请求体、响应体、密码或认证材料。
- 本轮没有创建会话、Agent run、消息、草稿、SSE 事件或业务写入。
- 8220 服务器的数据库与审计读取条件仍不可用，因此本轮没有取得数据库前后计数；不能用公开健康接口代替数据库证据。
- `https://zhj-api.sxyq27.online/healthz` 可作为服务存活参考，但 HTTP `200` 不代表登录或 Agent 链路通过。

## 状态与解除条件

登录前置为 `Blocked`；`AG-CLI-AND-003/004/006/007/008/010` 本轮均未开始执行，保持各自既有状态。解除条件是取得可用的现有测试账号登录条件，并保持设备以物理 ADB serial 在线；登录成功后再按测试计划逐项执行 UI tree、截图、脱敏日志、SSE/HTTP 摘要、服务端审计和数据库计数采集。

本轮未修改产品源码、API Key、线上配置或账号密码。
