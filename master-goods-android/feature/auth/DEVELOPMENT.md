# feature/auth 模块开发说明

- 当前状态：脚手架已创建，页面未开始。
- 实际源码目录：`feature/auth/src/main/java/com/zhihuiji/feature/auth`
- 目标：实现登录、注册、自动恢复会话。

## 需要创建的类

- `LoginScreen`
- `RegisterScreen`
- `AuthViewModel`
- `AuthUiState`

## 需要实现的关键函数

- `AuthViewModel.restoreSession()`
- `AuthViewModel.login(phone: String, password: String)`
- `AuthViewModel.register(phone: String, password: String, inviteCode: String)`
- `AuthViewModel.logout()`
- `AuthViewModel.loadMe()`
- `AuthViewModel.clearError()`

## 页面内容

- 登录：手机号、密码、登录按钮、服务器入口。
- 注册：手机号、密码、邀请码、注册按钮。

## UI 设计规范

- 对照设计图 `02.png` 的登录页和注册页实现。
- 登录页顶部居中放蓝色 App 图标、标题“智慧记”和副标题“让生意更轻松”。
- 表单放在居中的毛玻璃卡片内，登录方式使用顶部双 Tab。
- 输入框使用线性图标：手机号、锁、眼睛；主按钮为蓝色渐变。
- 第三方登录入口用圆形图标按钮，底部文字链接使用主蓝色。
- 注册页使用返回按钮、大标题、说明文案、毛玻璃表单卡和底部登录跳转。

## 验收标准

- 登录成功跳首页。
- 登录失败能展示后端返回的业务错误。
