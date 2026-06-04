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

## UI 联动约束

- 本模块虽然不直接负责页面绘制，但其输出的数据结构、状态枚举、错误语义和交互支撑能力必须服务于统一的 Android UI 基线。
- 后续新增业务不能倒逼页面切换成另一套视觉风格；应优先通过补充 `core/designsystem` 通用组件或扩展既有页面母版来承接。
- 需要映射到 UI 的状态、金额、风险、同步结果等，应继续服从统一的颜色语义、状态标签和信息层级。
- Android 视觉真源固定为 `docs/design-mockups/01.png ~ 08.png` 与 `master-goods-android/UI-DESIGN-SPEC.md`。
