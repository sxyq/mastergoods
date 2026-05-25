# data/auth 模块开发说明

- 当前状态：脚手架已创建，仓储未开始。
- 实际源码目录：`data/auth/src/main/java/com/zhihuiji/data/auth`
- 目标：封装认证接口和会话管理。

## 需要创建的类

- `AuthRepository`
- `SessionCoordinator`

## 需要实现的关键函数

- `login(phone: String, password: String): AuthResult`
- `register(phone: String, password: String, verifyCode: String): AuthResult`
- `refresh(refreshToken: String): AuthResult`
- `logout()`
- `fetchCurrentUser(): UserProfile`
- `restoreSessionIfNeeded(): Boolean`
- `clearSessionAndCache()`

## 规则说明

- `/v1/auth/register` 的 `verifyCode` 实际是邀请码。
- `/v1/auth/verify-code` 当前后端并不构成真实短信流程，第一版可以不接入主流程。

## UI 设计规范支撑

- 认证接口错误要保留后端 message，登录页和注册页直接展示短错误提示。
- `fetchCurrentUser()` 返回结果要支撑设置页的账号安全卡片。

## 验收标准

- 登录成功能写入本地会话。
- 退出登录能清理 DataStore 和 Room 缓存。
