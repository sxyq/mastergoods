# app 模块开发说明

- 当前状态：脚手架已创建，业务代码未开始。
- 实际源码目录：`app/src/main/java/com/zhihuiji/app`
- 目标：承载应用入口、全局导航、登录态切换、底部导航和全局错误提示。

## 需要创建的类

- `ZhihuijiApp`
- `MainActivity`
- `AppNavGraph`
- `AppState`
- `BottomBarDestination`

## 需要实现的关键函数

- `ZhihuijiApp.onCreate()`
  - 初始化 Hilt、日志、全局配置。
- `MainActivity.onCreate()`
  - 挂载 Compose 根节点，注入 `AppNavGraph`。
- `AppState.shouldShowAuth()`
  - 根据本地 token 和当前用户信息判断进入登录流还是主流程。
- `AppNavGraph.buildGraph()`
  - 注册 `auth`、`dashboard`、`documents`、`master-data`、`reports`、`agent`、`settings` 路由。
- `AppNavGraph.navigateAfterLogin()`
  - 登录成功后进入首页并清空认证栈。
- `AppNavGraph.logoutAndReset()`
  - 清理导航栈，回到登录页。

## 页面职责

- `AuthNavGraph`
  - 登录页、注册页。
- `MainNavGraph`
  - 首页、单据、档案、报表、智能助手、设置。

## UI 设计规范

- 必须使用 `core/designsystem` 中的 `ZhihuijiTheme` 和 `GlassScaffold` 作为全局容器。
- 底部导航固定五项：首页、单据、档案、报表、助手，激活态为蓝色图标和文字。
- 顶栏动作要使用图标按钮承载：通知、搜索、筛选、扫码、更多、打印。
- 页面背景统一为浅蓝渐变，不能在单个 feature 中自行改成纯白或深色背景。

## 验收标准

- 冷启动能根据登录态进入正确首页。
- 任意业务页都能回到设置页并执行退出登录。
