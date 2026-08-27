# Web 功能测试执行手册

## Objective

验证 Web 管理端页面、按钮、表单、Agent 交互和后端契约的完整行为。

## Scope

- auth、dashboard、archives、agent、documents
- finance、inventory、planning、reports、settings

## Per-Scenario Steps

1. 确认环境、账号/store、权限和页面前置状态。
2. 进入目标路由，记录初始状态和请求。
3. 执行单个按钮、菜单、表单或 Agent 操作。
4. 验证 API 方法、路径、请求字段、响应 envelope 和错误码。
5. 分别验证成功、空态、参数错误、无权限、请求失败、取消和重试分支。
6. 对 Agent 验证 SSE 事件顺序、工具展示、正式回答、审计和草稿状态。
7. 保存证据并清理测试数据。

## Boundary Conditions

- 缺失、空白、超长和非法字段。
- 无 token、过期会话、无权限和跨 owner/store 标识。
- 空列表、最后一页、重复提交、断线、AbortError 和局部请求失败。
- 大于 `2^53` 的实体 ID。

## Acceptance

页面状态、按钮状态、请求和响应都符合预期；失败分支可见且可恢复；服务端拒绝越权和非法写入。浏览器点击、真实登录和真实 SSE 未执行前不得记录为通过。
