# iOS 功能测试执行手册

## Objective

验证 iOS 页面、导航、表单、Agent 会话和后端 API 联动的完整行为。

## Scope

- auth、dashboard、inventory、products
- purchases、sales、finance、reports、settings
- Agent 会话、消息、历史、取消、草稿确认和错误状态

## Per-Scenario Steps

1. 确认 Xcode、模拟器/设备、账号/store、权限和后端环境。
2. 进入目标页面，记录初始状态和请求。
3. 执行一个导航、按钮、表单或 Agent 操作。
4. 核对 API 方法、路径、请求字段、响应模型、错误码和状态转换。
5. 验证成功、空态、非法输入、无权限、网络失败、取消和重试分支。
6. 对创建类 Agent 操作验证“草稿 -> 覆盖式确认 -> 正式写入”；拒绝确认时正式业务表不得变化。
7. 保存证据并清理测试数据。

## Boundary Conditions

- 缺失、空白、超长和非法字段。
- 无 token、过期会话、owner/store 不匹配和跨租户标识。
- 空列表、重复提交、断线、重复终止事件和恢复历史。

## Acceptance

页面状态、请求、模型解析和服务端结果符合预期；无权限和非法写入被拒绝。没有运行 Xcode、模拟器或设备前，不记录为通过。
