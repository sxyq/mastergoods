# Android Data 层 - Auth 子模块详尽代码分析

> 自动生成于 2026-05-28，覆盖 auth 子模块全部 1 个 Kotlin 源文件

---

## 1. AuthRepository

- **文件路径**: `data/auth/src/main/java/com/zhihuiji/data/auth/AuthRepository.kt`
- **父类/接口**: 无
- **注解**: `@Singleton`
- **职责**: 用户认证（登录、注册、刷新令牌、登出）和会话管理
- **设计模式**: Repository 模式 + 单例模式（通过 Hilt `@Singleton`）

### 类属性

##### api: ZhihuijiApi
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：调用认证相关 API
- 建议：无

##### sessionStore: SessionStore
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：管理本地会话（token 存储、读取、清除）
- 建议：无

##### settingsStore: SettingsStore
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：获取 clientId
- 建议：当前未在 AuthRepository 中直接使用 settingsStore，但构造函数中注入了。建议移除未使用的依赖

##### isLoggedIn: Flow<Boolean>
- 作用域：类公开（val 属性）
- 初始值：`sessionStore.isLoggedIn`
- 使用场景：观察用户登录状态
- 调用关系：委托给 `sessionStore.isLoggedIn`
- 建议：无

### 函数/方法

##### login(phone: String, password: String): Result<AuthResult>
- 参数：`phone: String` - 手机号；`password: String` - 密码
- 返回值：`Result<AuthResult>` - 认证结果
- 实现逻辑：调用 API 登录，成功后保存会话信息（token、refreshToken、userId、expiresIn）
- 调用关系：调用了 `safeApiCall()`、`api.login()`、`sessionStore.saveSession()`，被 `AuthViewModel.login()` 调用
- 建议：无

##### register(phone: String, password: String, verifyCode: String): Result<AuthResult>
- 参数：`phone: String` - 手机号；`password: String` - 密码；`verifyCode: String` - 验证码
- 返回值：`Result<AuthResult>` - 认证结果
- 实现逻辑：调用 API 注册，成功后保存会话信息
- 调用关系：调用了 `safeApiCall()`、`api.register()`、`sessionStore.saveSession()`，被 `AuthViewModel.register()` 调用
- 建议：无

##### refresh(refreshToken: String): Result<AuthResult>
- 参数：`refreshToken: String` - 刷新令牌
- 返回值：`Result<AuthResult>` - 新的认证结果
- 实现逻辑：调用 API 刷新令牌，成功后保存新会话信息
- 调用关系：调用了 `safeApiCall()`、`api.refresh()`、`sessionStore.saveSession()`
- 建议：当前未被任何 ViewModel 调用。建议在 token 过期时自动调用此方法实现无感刷新

##### logout()
- 参数：无
- 返回值：无
- 实现逻辑：尝试调用 API 登出（忽略异常），最终在 finally 块中清除本地会话，确保即使 API 调用失败也能清除本地状态
- 调用关系：调用了 `sessionStore.requireAccessToken()`、`api.logout()`、`sessionStore.clearSession()`，被 `SettingsViewModel.logout()`、`AuthViewModel.logout()` 调用
- 建议：无

##### fetchCurrentUser(): Result<UserProfile>
- 参数：无
- 返回值：`Result<UserProfile>` - 当前用户信息
- 实现逻辑：先获取 access token，再调用 API 获取用户信息。token 获取失败时直接返回 failure
- 调用关系：调用了 `sessionStore.requireAccessToken()`、`safeApiCall()`、`api.me()`，被 `SettingsViewModel.loadSettings()`、`AuthViewModel.loadMe()` 调用
- 建议：无

##### restoreSessionIfNeeded(): Boolean
- 参数：无
- 返回值：`Boolean` - 是否成功恢复会话
- 实现逻辑：尝试获取 token 并调用 `fetchCurrentUser()` 验证，成功返回 true，任何异常返回 false
- 调用关系：调用了 `sessionStore.requireAccessToken()`、`fetchCurrentUser()`，被 `AuthViewModel.restoreSession()` 调用
- 建议：无

##### clearSessionAndCache()
- 参数：无
- 返回值：无
- 实现逻辑：清除本地会话
- 调用关系：调用了 `sessionStore.clearSession()`
- 建议：当前未被任何 ViewModel 调用，方法名暗示应同时清除缓存，但实际只清除了会话。建议补充缓存清除逻辑或重命名方法
