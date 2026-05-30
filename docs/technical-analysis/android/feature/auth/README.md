# Auth 模块详尽代码分析

> 自动生成于 2026-05-28，覆盖 auth 目录下全部 3 个 Kotlin 源文件

---

## 1. AuthUiState

- **文件路径**: `feature/auth/src/main/java/com/zhihuiji/feature/auth/AuthViewModel.kt`
- **父类/接口**: 无（data class）
- **注解**: 无
- **职责**: 认证页面的 UI 状态数据容器
- **设计模式**: MVI 状态模式

### 变量/属性

##### isLoading: Boolean = false
- 作用域：类公开
- 初始值：false
- 使用场景：是否正在执行登录/注册操作
- 建议：无

##### isSessionReady: Boolean = false
- 作用域：类公开
- 初始值：false
- 使用场景：Session 状态是否已从本地存储中读取完毕
- 建议：无

##### isLoggedIn: Boolean = false
- 作用域：类公开
- 初始值：false
- 使用场景：用户是否已登录
- 建议：无

##### userProfile: UserProfile? = null
- 作用域：类公开
- 初始值：null
- 使用场景：当前登录用户的信息
- 建议：无

##### error: UiMessage? = null
- 作用域：类公开
- 初始值：null
- 使用场景：错误信息
- 建议：无

---

## 2. AuthViewModel

- **文件路径**: `feature/auth/src/main/java/com/zhihuiji/feature/auth/AuthViewModel.kt`
- **父类/接口**: `ViewModel`
- **注解**: `@HiltViewModel`
- **职责**: 管理用户认证（登录、注册、登出）的状态和业务逻辑
- **设计模式**: MVVM 模式

### 类属性

##### authRepository: AuthRepository
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：调用认证数据操作
- 建议：无

##### sessionStore: SessionStore
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：读取本地 Session 状态
- 建议：无

##### _uiState: MutableStateFlow<AuthUiState>
- 作用域：类私有
- 初始值：`MutableStateFlow(AuthUiState())`
- 使用场景：内部可变的 UI 状态流
- 建议：无

##### uiState: StateFlow<AuthUiState>
- 作用域：类公开
- 初始值：`_uiState.asStateFlow()`
- 使用场景：对外暴露只读的 UI 状态流
- 建议：无

### 函数/方法

##### init 块
- 参数：无
- 返回值：无
- 实现逻辑：在 `viewModelScope` 中收集 `sessionStore.isLoggedIn` 流，更新 `isSessionReady` 和 `isLoggedIn`
- 调用关系：调用了 `sessionStore.isLoggedIn.collect`
- 建议：使用 `collect` 会持续挂起，若 SessionStore 流永不完成则此协程永不结束，这是预期行为。但建议添加 `stateIn` 转换以避免在 init 中直接 collect

##### login(phone: String, password: String)
- 参数：`phone: String` - 手机号；`password: String` - 密码
- 返回值：无
- 实现逻辑：设置 isLoading 和清除 error，调用 `authRepository.login()`，成功时设置 isLoggedIn 为 true，失败时设置 error
- 调用关系：调用了 `authRepository.login()`
- 建议：无

##### register(phone: String, password: String, inviteCode: String)
- 参数：`phone: String` - 手机号；`password: String` - 密码；`inviteCode: String` - 邀请码
- 返回值：无
- 实现逻辑：设置 isLoading 和清除 error，调用 `authRepository.register()`，成功时设置 isLoggedIn 为 true，失败时设置 error
- 调用关系：调用了 `authRepository.register()`
- 建议：无

##### logout()
- 参数：无
- 返回值：无
- 实现逻辑：调用 `authRepository.logout()`，重置 UI 状态为初始值
- 调用关系：调用了 `authRepository.logout()`
- 建议：无

##### loadMe()
- 参数：无
- 返回值：无
- 实现逻辑：调用 `authRepository.fetchCurrentUser()`，成功时更新 userProfile
- 调用关系：调用了 `authRepository.fetchCurrentUser()`
- 建议：未处理 onFailure 情况；当前未被 Auth 模块的 Screen 直接调用，可能由 Settings 模块间接使用

##### clearError()
- 参数：无
- 返回值：无
- 实现逻辑：将 error 设置为 null
- 调用关系：无
- 建议：无

##### restoreSession()
- 参数：无
- 返回值：无
- 实现逻辑：调用 `authRepository.restoreSessionIfNeeded()`，更新 isLoggedIn
- 调用关系：调用了 `authRepository.restoreSessionIfNeeded()`
- 建议：未处理恢复失败的情况，建议添加错误处理

---

## 3. LoginScreen

- **文件路径**: `feature/auth/src/main/java/com/zhihuiji/feature/auth/LoginScreen.kt`
- **父类/接口**: 无（Composable 函数）
- **注解**: `@Composable`
- **职责**: 登录页面的 UI 渲染
- **设计模式**: 声明式 UI

### 函数参数

##### onLoginSuccess: () -> Unit
- 作用：登录成功后的导航回调
- 建议：无

##### onNavigateToRegister: () -> Unit
- 作用：导航到注册页面的回调
- 建议：无

##### viewModel: AuthViewModel = hiltViewModel()
- 作用：注入的 ViewModel 实例
- 建议：无

### 局部变量

##### uiState: AuthUiState
- 作用域：函数局部
- 初始值：通过 `viewModel.uiState.collectAsState()` 获取
- 使用场景：订阅 UI 状态
- 建议：无

##### phone: String
- 作用域：函数局部
- 初始值：空字符串
- 使用场景：用户输入的手机号
- 建议：建议添加手机号格式校验（如11位数字）

##### password: String
- 作用域：函数局部
- 初始值：空字符串
- 使用场景：用户输入的密码
- 建议：无

##### passwordVisible: Boolean
- 作用域：函数局部
- 初始值：false
- 使用场景：控制密码可见性
- 建议：无

### 关键逻辑

##### LaunchedEffect(uiState.isLoggedIn)
- 当 `isLoggedIn` 变为 true 时触发 `onLoginSuccess()` 回调
- 建议：无

---

## 4. RegisterScreen

- **文件路径**: `feature/auth/src/main/java/com/zhihuiji/feature/auth/RegisterScreen.kt`
- **父类/接口**: 无（Composable 函数）
- **注解**: `@Composable`
- **职责**: 注册页面的 UI 渲染
- **设计模式**: 声明式 UI

### 函数参数

##### onRegisterSuccess: () -> Unit
- 作用：注册成功后的导航回调
- 建议：无

##### onNavigateBack: () -> Unit
- 作用：返回上一页的回调
- 建议：无

##### viewModel: AuthViewModel = hiltViewModel()
- 作用：注入的 ViewModel 实例
- 建议：无

### 局部变量

##### uiState: AuthUiState
- 作用域：函数局部
- 初始值：通过 `viewModel.uiState.collectAsState()` 获取
- 使用场景：订阅 UI 状态
- 建议：无

##### phone: String
- 作用域：函数局部
- 初始值：空字符串
- 使用场景：用户输入的手机号
- 建议：建议添加手机号格式校验

##### password: String
- 作用域：函数局部
- 初始值：空字符串
- 使用场景：用户输入的密码
- 建议：注册页面没有密码可见性切换（与登录页面不一致），建议添加

##### inviteCode: String
- 作用域：函数局部
- 初始值：空字符串
- 使用场景：用户输入的邀请码
- 建议：无

### 关键逻辑

##### LaunchedEffect(uiState.isLoggedIn)
- 当 `isLoggedIn` 变为 true 时触发 `onRegisterSuccess()` 回调
- 建议：无
