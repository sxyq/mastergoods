# app 模块开发说明

- 当前状态：应用入口、主壳导航、档案/单据容器页和子路由首轮已落地；本轮已把 `DocumentsScreen` / `ArchivesScreen` 统一到 `GlassTopBar` 母版、移除壳层伪动作图标，并接入商品 `列表 -> 详情 -> 编辑` 导航链；底栏重复点击当前主导航时也会保留当前子 Tab，仅触发所在列表回到顶部。设置页现已新增“店员与权限”子路由，进入真实店员账号管理专页。
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
  - 注册 `auth`、`dashboard`、`documents`、`master-data`、`reports`、`agent`、`settings` 路由，并承接设置页下的店员管理子路由。
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
- 档案/单据容器页嵌入子列表时，壳层顶栏统一复用 `GlassTopBar`，主操作留给子列表右下胶囊按钮，不再由壳层顶部塞一个“伪新增”图标代替。

## 验收标准

- 冷启动能根据登录态进入正确首页。
- 任意业务页都能回到设置页并执行退出登录。

## UI 联动约束

- 本模块虽然不直接负责页面绘制，但其输出的数据结构、状态枚举、错误语义和交互支撑能力必须服务于统一的 Android UI 基线。
- 后续新增业务不能倒逼页面切换成另一套视觉风格；应优先通过补充 `core/designsystem` 通用组件或扩展既有页面母版来承接。
- 需要映射到 UI 的状态、金额、风险、同步结果等，应继续服从统一的颜色语义、状态标签和信息层级。
- Android 当前视觉真源以 Stitch 导出、`docs/spec/42-android-liquid-glass-ui-refactor-plan.md` 与 `master-goods-android/UI-DESIGN-SPEC.md` 为准；`docs/design-mockups/` 仅作历史参考。
- 当前仍未完成的项要如实保留：Agent workbench/chat 已接真实 V2/SSE，tasks/notifications 当前只承接列表、入口与已读状态；草稿仅归档，不执行业务写入；服务端 run cancel 已有接口和后端单测，但仍缺 Android 真机点击停止后的 HTTP/SSE 抓包、审计接口对账和 UI 反馈截图；dashboard/reports 设计稿级贴合和真机截图验收都不能仅凭本地编译成功升级状态。
