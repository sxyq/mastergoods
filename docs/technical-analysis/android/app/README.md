# App 模块技术分析

## 文件清单
- ZhihuijiApp.kt
- MainActivity.kt
- navigation/MainScreen.kt
- navigation/MainNavGraph.kt
- navigation/DocumentsScreen.kt
- navigation/AppNavGraph.kt
- navigation/ArchivesScreen.kt

---

## ZhihuijiApp.kt

### ZhihuijiApp
- 父类：`Application`
- 注解：`@HiltAndroidApp`
- 职责：应用程序入口类，作为 Hilt 依赖注入的 Application 级容器。Hilt 通过此注解自动生成 Application 级别的 Dagger 组件，为整个应用提供依赖注入能力。
- 设计模式：依赖注入（Hilt/Dagger）

#### 建议
- 当前类体为空，仅作为 Hilt 注入锚点，这是标准做法，无需额外修改。
- 若未来需要全局初始化（如 Timber 日志、WorkManager、LeakCanary 等），可在此覆写 `onCreate()`。
- 建议避免在此类中放置过多业务逻辑，保持其作为 DI 容器的单一职责。

---

## MainActivity.kt

### MainActivity
- 父类：`ComponentActivity`
- 注解：`@AndroidEntryPoint`
- 职责：应用主 Activity，作为所有 Compose UI 的宿主。通过 Hilt 注入，设置边到边显示和 Compose 内容。
- 设计模式：单 Activity 架构

#### onCreate(savedInstanceState: Bundle?)
- 参数：
  - `savedInstanceState: Bundle` — Activity 状态恢复数据
- 返回值：无
- 实现逻辑：
  1. 调用 `super.onCreate()` 完成父类初始化
  2. 调用 `enableEdgeToEdge()` 启用边到边显示模式
  3. 调用 `setContent` 设置 Compose 内容树：`ZhihuijiTheme { AppNavGraph() }`
- 调用关系：由 Android 框架调用；内部调用 `AppNavGraph()` 启动导航图
- 建议：
  - 当前实现简洁合理，符合单 Activity + Compose Navigation 的最佳实践。
  - 如果未来需要处理深层链接（Deep Link），可在此处配置 `NavDeepLink`。
  - 如果需要处理 `onNewIntent`（如通知点击），可考虑添加。

---

## navigation/MainScreen.kt

### TopLevelRoutes
- 类型：`object`（单例）
- 职责：定义顶层导航路由常量
- 设计模式：常量集中管理

#### HOME: String
- 作用域：`TopLevelRoutes` 伴生对象
- 初始值：`"main_home"`
- 使用场景：首页路由标识

#### DOCUMENTS: String
- 作用域：`TopLevelRoutes` 伴生对象
- 初始值：`"main_documents"`
- 使用场景：单据页路由标识

#### ARCHIVES: String
- 作用域：`TopLevelRoutes` 伴生对象
- 初始值：`"main_archives"`
- 使用场景：档案页路由标识

#### REPORTS: String
- 作用域：`TopLevelRoutes` 伴生对象
- 初始值：`"main_reports"`
- 使用场景：报表页路由标识

#### AGENT: String
- 作用域：`TopLevelRoutes` 伴生对象
- 初始值：`"main_agent"`
- 使用场景：助手页路由标识

#### SETTINGS: String
- 作用域：`TopLevelRoutes` 伴生对象
- 初始值：`"main_settings"`
- 使用场景：设置页路由标识（当前未在 MainScreen 中使用，在 AppNavGraph 中使用）

- 建议：路由常量集中管理是好的实践。建议将 `SETTINGS` 移至 `MainRoutes`（AppNavGraph 中已有定义），避免跨文件重复定义。

### bottomBarDestinations: List\<BottomBarDestination\>
- 类型：顶层属性 `List<BottomBarDestination>`
- 初始值：包含5个目的地的列表（首页、单据、档案、报表、助手）
- 使用场景：定义底部导航栏的所有目的地，包含路由、标签、未选中图标和选中图标
- 建议：
  - 当前硬编码了5个目的地，若未来需要动态配置（如权限控制隐藏某些 Tab），可改为由 ViewModel 提供。
  - 图标使用了 `Icons.Outlined` 和 `Icons.Filled` 的切换，这是 Material 3 推荐做法。

### MainScreen(onNavigateToSettings)
- 类型：`@Composable` 函数
- 职责：主屏幕容器，包含底部导航栏和 NavHost，管理 Tab 切换和重选信号

#### 参数
- `onNavigateToSettings: () -> Unit` — 导航到设置页的回调

#### 内部变量
- `navController: NavHostController` — 导航控制器
- `navBackStackEntry` — 当前导航栈条目（通过 `currentBackStackEntryAsState()` 观察）
- `currentRoute: String?` — 当前路由路径
- `bottomBarVisible: Boolean` — 底部导航栏可见性状态，初始值 `true`
- `homeReselectSignal: Int` — 首页重选信号，初始值 `0`
- `documentsReselectSignal: Int` — 单据页重选信号，初始值 `0`
- `archivesReselectSignal: Int` — 档案页重选信号，初始值 `0`
- `reportsReselectSignal: Int` — 报表页重选信号，初始值 `0`
- `agentReselectSignal: Int` — 助手页重选信号，初始值 `0`
- `selectedRoute: String` — 当前选中的底部导航路由
- `showBottomBar: Boolean` — 是否显示底部导航栏

#### 实现逻辑
1. 创建 NavController 并观察当前路由
2. 根据 `currentRoute` 计算当前选中的 Tab 和是否显示底部栏
3. 在 `GlassScaffold` 中组合底部导航和 NavHost
4. 处理导航逻辑：同 Tab 重选时递增 reselectSignal 并显示底部栏；不同 Tab 切换时执行导航并恢复状态
5. 将所有 reselectSignal 传递给 `MainNavGraph`

#### 调用关系
- 被 `AppNavGraph` 调用
- 内部调用 `GlassScaffold`、`MainNavGraph`

#### 建议
- **reselectSignal 机制**：使用 Int 递增作为重选信号是可行的，但5个独立的 signal 变量导致代码冗余。建议使用 `Map<String, Int>` 或数据类来统一管理，减少样板代码。
- **bottomBarVisible 状态**：当前在重选和切换 Tab 时都设为 `true`，但从未设为 `false`，这个状态变量似乎未完整使用。如果某些子页面需要隐藏底部栏，应在导航时根据路由更新此值。
- **导航策略**：`popUpTo` + `saveState` + `restoreState` 的组合是标准的多 Tab 导航模式，实现正确。

---

## navigation/MainNavGraph.kt

### SubRoutes
- 类型：`object`（单例）
- 职责：定义子页面导航路由常量
- 设计模式：常量集中管理

#### PRODUCT_EDITOR: String
- 初始值：`"product_editor"`

#### CUSTOMER_EDITOR: String
- 初始值：`"customer_editor"`

#### CUSTOMER_DETAIL: String
- 初始值：`"customer_detail"`

#### SUPPLIER_EDITOR: String
- 初始值：`"supplier_editor"`

#### SUPPLIER_DETAIL: String
- 初始值：`"supplier_detail"`

#### SALE_ORDER_EDITOR: String
- 初始值：`"sale_order_editor"`

#### SALE_ORDER_DETAIL: String
- 初始值：`"sale_order_detail"`

#### PURCHASE_ORDER_EDITOR: String
- 初始值：`"purchase_order_editor"`

#### PURCHASE_ORDER_DETAIL: String
- 初始值：`"purchase_order_detail"`

#### PAY_ORDER_EDITOR: String
- 初始值：`"pay_order_editor"`

#### PAY_ORDER_DETAIL: String
- 初始值：`"pay_order_detail"`

- 建议：路由常量集中管理是好的实践。当前使用简单字符串路由，若项目规模增长，可考虑使用类型安全导航（Type-Safe Navigation，Compose Navigation 2.8+）。

### MainNavGraph(navController, modifier, onNavigateToSettings, ...)
- 类型：`@Composable` 函数
- 职责：定义主导航图，包含所有顶层和子页面路由

#### 参数
- `navController: NavHostController` — 导航控制器
- `modifier: Modifier = Modifier` — 修饰符
- `onNavigateToSettings: () -> Unit` — 导航到设置页的回调
- `homeReselectSignal: Int = 0` — 首页重选信号
- `documentsReselectSignal: Int = 0` — 单据页重选信号
- `archivesReselectSignal: Int = 0` — 档案页重选信号
- `reportsReselectSignal: Int = 0` — 报表页重选信号
- `agentReselectSignal: Int = 0` — 助手页重选信号

#### 内部函数
##### navigateBack()
- 返回值：无
- 实现逻辑：调用 `navController.popBackStack()`
- 使用场景：作为所有子页面的返回回调

#### 实现逻辑
1. 使用 `NavHost` 定义导航图，起始目的地为 `TopLevelRoutes.HOME`
2. 注册5个顶层路由（HOME、DOCUMENTS、ARCHIVES、REPORTS、AGENT）
3. 注册10个子页面路由（编辑器和详情页）
4. 每个路由通过 `composable` 注册，解析参数并创建对应的 Screen 组件
5. 子页面路由使用 `launchSingleTop = true` 避免重复入栈

#### 调用关系
- 被 `MainScreen` 调用
- 内部调用各 feature 模块的 Screen 组件

#### 建议
- **文件长度**：此文件约244行，包含15个路由定义。随着业务增长，建议按业务域拆分为多个 NavGraph 扩展函数（如 `NavGraphBuilder.documentsGraph()`），使用 Navigation 的 `navigation` 嵌套图功能。
- **参数解析模式重复**：Editor 路由的 `rawId > 0` 转 `null` 的模式重复出现（ProductEditor、CustomerEditor、SupplierEditor），建议抽取为通用工具函数。
- **DashboardScreen 的导航回调**：`onNavigateToSales`、`onNavigateToProducts`、`onNavigateToCustomers` 使用了带 `initialTab` 参数的路由跳转，这是跨 Tab 导航的正确实现。
- **ReportScreen 和 AgentWorkbenchScreen**：`onNavigateBack = {}` 传入空实现，因为它们是顶层 Tab 页面不需要返回。`showTopBar = false` 表示由外层 MainScreen 统一管理顶栏。

---

## navigation/DocumentsScreen.kt

### DocumentsScreen(initialTab, reselectSignal, ...)
- 类型：`@Composable` 函数
- 职责：单据页面容器，包含4个 Tab（销售单、采购单、付款单、资金流水），管理 Tab 切换和重选行为

#### 参数
- `initialTab: Int = 0` — 初始选中的 Tab 索引
- `reselectSignal: Int = 0` — 重选信号
- `onNavigateToSaleOrderEditor: () -> Unit = {}` — 导航到销售单编辑器
- `onNavigateToSaleOrderDetail: (Long) -> Unit = {}` — 导航到销售单详情（传入订单ID）
- `onNavigateToPurchaseOrderEditor: () -> Unit = {}` — 导航到采购单编辑器
- `onNavigateToPurchaseOrderDetail: (Long) -> Unit = {}` — 导航到采购单详情
- `onNavigateToPayOrderEditor: () -> Unit = {}` — 导航到付款单编辑器
- `onNavigateToPayOrderDetail: (Long) -> Unit = {}` — 导航到付款单详情

#### 内部变量
- `selectedTab: Int` — 当前选中的 Tab 索引，使用 `rememberSaveable` 保存
- `saleScrollToTopSignal: Int` — 销售单滚动到顶部信号
- `purchaseScrollToTopSignal: Int` — 采购单滚动到顶部信号
- `payScrollToTopSignal: Int` — 付款单滚动到顶部信号
- `financeScrollToTopSignal: Int` — 资金流水滚动到顶部信号
- `tabs: List<String>` — Tab 标签列表 `["销售单", "采购单", "付款单", "资金流水"]`

#### 实现逻辑
1. 使用 `rememberSaveable` 保存 Tab 选择状态
2. `LaunchedEffect(reselectSignal)` 监听重选信号：当重选时，如果不在第一个 Tab 则切回第一个 Tab 并滚动到顶部；如果在第一个 Tab 则直接滚动到顶部
3. 顶部栏包含标题、当前 Tab 名称、搜索按钮和新增按钮
4. 新增按钮在"资金流水"Tab 下禁用（`enabled = selectedTab != 3`）
5. 使用 `SegmentedTabs` 组件展示 Tab 切换
6. 根据 `selectedTab` 显示对应的列表 Screen

#### 调用关系
- 被 `MainNavGraph` 调用
- 内部调用 `SaleOrderListScreen`、`PurchaseOrderListScreen`、`PayOrderListScreen`、`FinanceRecordListScreen`

#### 建议
- **重选逻辑**：`LaunchedEffect` 中的 `when` 分支 1、2、3 的逻辑完全相同（切回 Tab 0 并滚动），可简化为 `else -> { selectedTab = 0; saleScrollToTopSignal++ }`。
- **搜索按钮**：`IconButton(onClick = {})` 是空实现，建议添加搜索功能或移除按钮避免用户困惑。
- **Tab 切换时列表重建**：`when(selectedTab)` 会导致非当前 Tab 的 Screen 被移除，每次切换都会重新创建。如果需要保持各 Tab 的滚动位置和状态，可考虑使用 `HorizontalPager` 或将所有 Screen 都放在布局中通过可见性控制。
- **硬编码字符串**：Tab 标签 "销售单" 等硬编码在代码中，建议提取为字符串资源以支持国际化。

---

## navigation/AppNavGraph.kt

### AuthRoutes
- 类型：`object`（单例）
- 职责：定义认证相关路由常量

#### LOGIN: String
- 初始值：`"login"`

#### REGISTER: String
- 初始值：`"register"`

### MainRoutes
- 类型：`object`（单例）
- 职责：定义主流程路由常量

#### MAIN: String
- 初始值：`"main"`

#### SETTINGS: String
- 初始值：`"settings"`

- 建议：`MainRoutes.SETTINGS` 与 `TopLevelRoutes.SETTINGS` 存在语义重叠，建议统一使用一个路由常量。

### AppNavGraph()
- 类型：`@Composable` 函数
- 职责：应用顶层导航图，管理认证流程和主流程的切换

#### 内部变量
- `navController: NavHostController` — 导航控制器
- `authViewModel: AuthViewModel` — 认证 ViewModel（通过 `hiltViewModel()` 获取）
- `uiState` — 认证 UI 状态（通过 `collectAsState()` 观察）

#### 实现逻辑
1. 创建 NavController 和 AuthViewModel
2. 如果 `uiState.isSessionReady` 为 `false`，显示加载画面（"智慧记"文字 + glassBackground），并提前返回
3. 根据 `uiState.isLoggedIn` 决定起始目的地：已登录 -> `MainRoutes.MAIN`，未登录 -> `AuthRoutes.LOGIN`
4. 定义4个路由：LOGIN、REGISTER、MAIN、SETTINGS
5. 登录/注册成功后导航到 MAIN 并清除认证页面栈
6. 登出时调用 `authViewModel.logout()` 并导航回 LOGIN，清除整个导航栈

#### 调用关系
- 被 `MainActivity.setContent` 调用
- 内部调用 `LoginScreen`、`RegisterScreen`、`MainScreen`、`SettingsScreen`

#### 建议
- **Session 加载状态**：`if (!uiState.isSessionReady) return` 的处理方式会导致加载期间 Compose 树不完整。建议使用 `AnimatedVisibility` 或 `Crossfade` 来平滑过渡加载状态。
- **AuthViewModel 作用域**：`hiltViewModel()` 在 AppNavGraph 级别创建，其生命周期与 NavHost 绑定。这意味着登录后 AuthViewModel 仍然存活，这是正确的（需要保持登录状态），但需注意其内存占用。
- **登出导航**：`popUpTo(0) { inclusive = true }` 清除整个栈是正确的做法，确保登出后无法返回。
- **起始目的地动态决定**：`startDestination` 在 `isSessionReady` 变为 true 后才计算，这确保了正确的路由选择。但需注意 NavHost 不支持动态更改 `startDestination`，当前实现依赖条件返回来规避此问题。

---

## navigation/ArchivesScreen.kt

### ArchivesScreen(initialTab, reselectSignal, ...)
- 类型：`@Composable` 函数
- 职责：档案页面容器，包含3个 Tab（商品、客户、供应商），管理 Tab 切换和重选行为

#### 参数
- `initialTab: Int = 0` — 初始选中的 Tab 索引
- `reselectSignal: Int = 0` — 重选信号
- `onNavigateToProductEditor: (Long?) -> Unit = {}` — 导航到商品编辑器（null 表示新建，非 null 表示编辑）
- `onNavigateToCustomerEditor: (Long?) -> Unit = {}` — 导航到客户编辑器
- `onNavigateToCustomerDetail: (Long) -> Unit = {}` — 导航到客户详情
- `onNavigateToSupplierEditor: (Long?) -> Unit = {}` — 导航到供应商编辑器
- `onNavigateToSupplierDetail: (Long) -> Unit = {}` — 导航到供应商详情

#### 内部变量
- `selectedTab: Int` — 当前选中的 Tab 索引，使用 `rememberSaveable` 保存
- `productScrollToTopSignal: Int` — 商品滚动到顶部信号
- `customerScrollToTopSignal: Int` — 客户滚动到顶部信号
- `supplierScrollToTopSignal: Int` — 供应商滚动到顶部信号
- `tabs: List<String>` — Tab 标签列表 `["商品", "客户", "供应商"]`

#### 实现逻辑
1. 使用 `rememberSaveable` 保存 Tab 选择状态
2. `LaunchedEffect(reselectSignal)` 监听重选信号：当重选时，如果不在第一个 Tab 则切回第一个 Tab 并滚动到顶部
3. 顶部栏包含标题、当前 Tab 名称、筛选按钮和新增按钮
4. 使用 `SegmentedTabs` 组件展示 Tab 切换
5. 根据 `selectedTab` 显示对应的列表 Screen

#### 调用关系
- 被 `MainNavGraph` 调用
- 内部调用 `ProductListScreen`、`CustomerListScreen`、`SupplierListScreen`

#### 建议
- **筛选按钮**：`IconButton(onClick = {})` 是空实现，建议添加筛选功能或移除按钮。
- **重选逻辑**：`when` 分支 `1, 2 ->` 合并处理是好的简化，比 DocumentsScreen 更简洁。
- **Tab 切换时列表重建**：与 DocumentsScreen 相同的问题，非当前 Tab 的 Screen 会被移除。
- **新增按钮图标**：使用 `Icons.Default.AddCircleOutline`，而 DocumentsScreen 使用 `Icons.Default.Add`，建议统一图标风格。
- **硬编码字符串**：同 DocumentsScreen，建议提取为字符串资源。
