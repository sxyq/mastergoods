# Android 整体开发计划

这个文档负责串联所有模块开发说明，并记录每个模块当前完成状态。

状态约定：
- `Scaffold Ready`：目录和开发说明已建立。
- `In Progress`：已开始编码但未完成联调。
- `Done`：已完成并通过联调。

## 模块状态总表

| 模块 | 作用 | 依赖 | 当前状态 |
| --- | --- | --- | --- |
| `app` | 应用入口与导航 | 全部模块 | Done |
| `core/common` | 格式化与通用工具 | `core/model` | Done |
| `core/designsystem` | 毛玻璃 UI 主题与组件 | `core/common` | Done |
| `core/model` | DTO 与领域模型 | 无 | Done |
| `core/network` | Retrofit、OkHttp、鉴权 | `core/model`, `core/datastore` | Done |
| `core/database` | Room 缓存 | `core/model` | Done |
| `core/datastore` | 会话与设置持久化 | 无 | Done |
| `data/auth` | 登录注册与会话管理 | `core/network`, `core/datastore` | Done |
| `data/product` | 商品仓储 | `core/network`, `core/database` | Done |
| `data/customer` | 客户仓储 | `core/network`, `core/database` | Done |
| `data/supplier` | 供应商仓储 | `core/network`, `core/database` | Done |
| `data/order` | 销售/采购/付款仓储 | `core/network`, `core/database` | Done |
| `data/finance` | 资金流水仓储 | `core/network`, `core/database` | Done |
| `data/report` | 报表仓储 | `core/network` | Done |
| `data/agent` | AI 能力仓储 | `core/network`, `core/database` | Done |
| `data/sync` | 同步与游标 | `core/network`, `core/database`, `core/datastore` | In Progress |
| `feature/auth` | 登录注册界面 | `data/auth`, `core:datastore` | Done |
| `feature/dashboard` | 首页经营看板 | `data/report`, `data/agent` | Done |
| `feature/products` | 商品页面 | `data/product` | Done |
| `feature/customers` | 客户页面 | `data/customer` | Done |
| `feature/suppliers` | 供应商页面 | `data/supplier` | Done |
| `feature/sales` | 销售单页面 | `data/order`, `data/product`, `data/customer` | Done |
| `feature/purchases` | 采购单页面 | `data/order`, `data/product`, `data:supplier` | Done |
| `feature/payments` | 付款单页面 | `data/order`, `data:supplier` | Done |
| `feature/finance` | 资金流水页面 | `data:finance` | Done |
| `feature/reports` | 报表页面 | `data/report` | Done |
| `feature/agent` | AI 助手页面 | `data/agent` | Done |
| `feature/settings` | 设置与同步页面 | `data/auth`, `data/sync`, `core:datastore` | Done |

## 推荐开发顺序

1. `core/model`、`core/common`、`core/designsystem`、`core/datastore`、`core/network`
2. `data/auth`、`feature/auth`、`app`
3. `data/product`、`data/customer`、`data/supplier`
4. `feature/products`、`feature/customers`、`feature/suppliers`
5. `data/order`、`feature/sales`
6. `feature/purchases`、`feature/payments`
7. `data/finance`、`feature/finance`
8. `data/report`、`feature/dashboard`、`feature/reports`
9. `data/agent`、`feature/agent`
10. `core/database`、`data/sync`、`feature/settings`

## 子说明阅读顺序

- 先读入口和基础层：
  - [app/DEVELOPMENT.md](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/app/DEVELOPMENT.md)
  - [UI-DESIGN-SPEC.md](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/UI-DESIGN-SPEC.md)
  - [core/designsystem/DEVELOPMENT.md](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/core/designsystem/DEVELOPMENT.md)
  - [core/model/DEVELOPMENT.md](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/core/model/DEVELOPMENT.md)
  - [core/network/DEVELOPMENT.md](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/core/network/DEVELOPMENT.md)
  - [core/datastore/DEVELOPMENT.md](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/core/datastore/DEVELOPMENT.md)
- 再读仓储层：
  - [data/auth/DEVELOPMENT.md](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/data/auth/DEVELOPMENT.md)
  - [data/product/DEVELOPMENT.md](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/data/product/DEVELOPMENT.md)
  - [data/customer/DEVELOPMENT.md](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/data/customer/DEVELOPMENT.md)
  - [data/supplier/DEVELOPMENT.md](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/data/supplier/DEVELOPMENT.md)
  - [data/order/DEVELOPMENT.md](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/data/order/DEVELOPMENT.md)
  - [data/finance/DEVELOPMENT.md](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/data/finance/DEVELOPMENT.md)
  - [data/report/DEVELOPMENT.md](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/data/report/DEVELOPMENT.md)
  - [data/agent/DEVELOPMENT.md](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/data/agent/DEVELOPMENT.md)
  - [data/sync/DEVELOPMENT.md](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/data/sync/DEVELOPMENT.md)
- 最后读页面层：
  - [feature/auth/DEVELOPMENT.md](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/feature/auth/DEVELOPMENT.md)
  - [feature/dashboard/DEVELOPMENT.md](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/feature/dashboard/DEVELOPMENT.md)
  - [feature/products/DEVELOPMENT.md](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/feature/products/DEVELOPMENT.md)
  - [feature/customers/DEVELOPMENT.md](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/feature/customers/DEVELOPMENT.md)
  - [feature/suppliers/DEVELOPMENT.md](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/feature/suppliers/DEVELOPMENT.md)
  - [feature/sales/DEVELOPMENT.md](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/feature/sales/DEVELOPMENT.md)
  - [feature/purchases/DEVELOPMENT.md](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/feature/purchases/DEVELOPMENT.md)
  - [feature/payments/DEVELOPMENT.md](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/feature/payments/DEVELOPMENT.md)
  - [feature/finance/DEVELOPMENT.md](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/feature/finance/DEVELOPMENT.md)
  - [feature/reports/DEVELOPMENT.md](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/feature/reports/DEVELOPMENT.md)
  - [feature/agent/DEVELOPMENT.md](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/feature/agent/DEVELOPMENT.md)
  - [feature/settings/DEVELOPMENT.md](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/feature/settings/DEVELOPMENT.md)

## 开发执行记录

### 2026-05-25 第六阶段：按设计稿重构 UI

##### 文件 1：app/src/main/AndroidManifest.xml
- 所属模块：app
- 本次修改内容：MainActivity 锁定 portrait，避免真机横屏导致界面完全偏离手机设计稿。
- 当前状态：In Progress
- 下一步：重构全局设计系统与销售链路页面。

##### 文件 2：core/designsystem/src/main/java/com/zhihuiji/core/designsystem/ZhihuijiColors.kt
- 所属模块：core/designsystem
- 本次修改内容：调整主色、背景渐变、卡片透明白、边框和选中态颜色，贴近设计稿的浅蓝毛玻璃视觉。
- 当前状态：In Progress
- 下一步：继续统一组件尺寸、圆角和密度。

##### 文件 3：core/designsystem/src/main/java/com/zhihuiji/core/designsystem/GlassBackground.kt
- 所属模块：core/designsystem
- 本次修改内容：背景从双色渐变改为三段浅蓝到白色渐变，减少当前页面的平铺感。
- 当前状态：In Progress
- 下一步：继续压缩卡片与导航组件视觉密度。

##### 文件 4：core/designsystem/src/main/java/com/zhihuiji/core/designsystem/GlassCard.kt
- 所属模块：core/designsystem
- 本次修改内容：卡片圆角、边框、阴影调整为更接近设计稿的轻量卡片。
- 当前状态：In Progress
- 下一步：继续重构顶部栏、搜索栏、分段标签和底部导航。

##### 文件 5：core/designsystem/src/main/java/com/zhihuiji/core/designsystem/GlassTopBar.kt
- 所属模块：core/designsystem
- 本次修改内容：顶部栏高度压到 56dp，标题字号改为紧凑业务 App 风格。
- 当前状态：In Progress
- 下一步：重构单据容器顶部栏。

##### 文件 6：core/designsystem/src/main/java/com/zhihuiji/core/designsystem/SearchFilterBar.kt
- 所属模块：core/designsystem
- 本次修改内容：搜索框改为浅白半透明容器和 9dp 圆角，接近设计稿列表页搜索栏。
- 当前状态：In Progress
- 下一步：重构销售列表。

##### 文件 7：core/designsystem/src/main/java/com/zhihuiji/core/designsystem/SegmentedTabs.kt
- 所属模块：core/designsystem
- 本次修改内容：分段标签改为浅蓝选中态、白色未选中态，并补上点击行为。
- 当前状态：In Progress
- 下一步：应用到单据页和销售状态筛选。

##### 文件 8：core/designsystem/src/main/java/com/zhihuiji/core/designsystem/PrimaryGradientButton.kt
- 所属模块：core/designsystem
- 本次修改内容：主按钮和描边按钮高度、圆角收敛到设计稿的底部操作按钮尺寸。
- 当前状态：In Progress
- 下一步：重构销售开单和详情底部操作条。

##### 文件 9：core/designsystem/src/main/java/com/zhihuiji/core/designsystem/GlassScaffold.kt
- 所属模块：core/designsystem
- 本次修改内容：底部导航改为更轻的半透明白底和浅蓝选中态。
- 当前状态：In Progress
- 下一步：真机验证底部五栏观感。

##### 文件 10：core/designsystem/src/main/java/com/zhihuiji/core/designsystem/QuantityStepper.kt
- 所属模块：core/designsystem
- 本次修改内容：数量步进器按钮尺寸和圆角收敛，便于销售开单商品行按设计稿排布。
- 当前状态：In Progress
- 下一步：重排销售开单商品明细。

##### 文件 11：app/src/main/java/com/zhihuiji/app/navigation/DocumentsScreen.kt
- 所属模块：app
- 本次修改内容：单据容器顶部改为“智慧记 + 当前单据类型 + 搜索/新增”结构，新增按钮按当前子页跳转销售、采购、付款创建页。
- 当前状态：In Progress
- 下一步：去掉嵌入销售列表的悬浮新增按钮，并按设计稿重排列表卡片。

##### 文件 12：feature/sales/src/main/java/com/zhihuiji/feature/sales/SaleOrderListScreen.kt
- 所属模块：feature/sales
- 本次修改内容：销售列表重排为设计稿式搜索、横向状态标签、紧凑订单卡片和底部统计；嵌入主壳时隐藏悬浮新增按钮，改由单据顶部新增入口承载。
- 当前状态：In Progress
- 下一步：重构销售开单、详情和收款页面。

##### 文件 13：feature/sales/src/main/java/com/zhihuiji/feature/sales/SaleOrderEditorScreen.kt
- 所属模块：feature/sales
- 本次修改内容：销售开单页改为设计稿式信息分组、商品明细行、数量步进器、金额汇总和底部双按钮操作条。
- 当前状态：In Progress
- 下一步：重构销售详情与收款页面。

##### 文件 14：feature/sales/src/main/java/com/zhihuiji/feature/sales/SaleOrderDetailScreen.kt
- 所属模块：feature/sales
- 本次修改内容：销售详情页重排为订单信息、三栏金额汇总、商品明细、收款记录和底部“作废/修改/收款”结构；未收款单据优先展示收款主操作。
- 当前状态：In Progress
- 下一步：重构收款弹窗。

##### 文件 15：feature/sales/src/main/java/com/zhihuiji/feature/sales/SalePaymentSheet.kt
- 所属模块：feature/sales
- 本次修改内容：收款弹窗改为设计稿式“单据信息 + 本次收款 + 方式/参考号 + 确认收款”层级。
- 当前状态：In Progress
- 下一步：编译并真机安装截图验收。

##### 文件 16：app/src/main/java/com/zhihuiji/app/navigation/ArchivesScreen.kt
- 所属模块：app
- 本次修改内容：档案容器顶部改为“智慧记 + 当前档案类型 + 筛选/新增”结构，新增按钮按商品、客户、供应商子页进入对应创建页。
- 当前状态：In Progress
- 下一步：重新编译并等待真机 ADB 恢复后安装截图验收。

##### 验证记录
- 执行命令：`./gradlew :app:assembleDebug`
- 结果：`BUILD SUCCESSFUL`
- 真机状态：构建后执行 `:app:installDebug` 时手机从 ADB 设备列表掉线，当前 `adb devices -l` 为空；安装截图验收等待设备重新连接后继续。

##### 文件 17：feature/products/src/main/java/com/zhihuiji/feature/products/ProductListScreen.kt
- 所属模块：feature/products
- 本次修改内容：商品列表改为设计稿式库存页结构，增加库存状态标签筛选、紧凑商品卡、库存/安全库存/售价/状态排布；嵌入档案主壳时隐藏重复悬浮新增按钮。
- 当前状态：In Progress
- 下一步：重构客户和供应商列表。

##### 文件 18：feature/customers/src/main/java/com/zhihuiji/feature/customers/CustomerListScreen.kt
- 所属模块：feature/customers
- 本次修改内容：客户列表改为设计稿式档案页结构，增加“全部/正常/欠款/已停用”标签筛选，重排客户名称、编号、联系方式、应收余额和状态；嵌入档案主壳时隐藏重复悬浮新增按钮。
- 当前状态：In Progress
- 下一步：重构供应商列表。

##### 文件 19：feature/suppliers/src/main/java/com/zhihuiji/feature/suppliers/SupplierListScreen.kt
- 所属模块：feature/suppliers
- 本次修改内容：供应商列表改为设计稿式档案页结构，重排供应商名称、编号、联系方式、应付余额和状态；嵌入档案主壳时隐藏重复悬浮新增按钮。
- 当前状态：In Progress
- 下一步：编译验证。

##### 验证记录 2
- 执行命令：`./gradlew :app:assembleDebug`
- 结果：`BUILD SUCCESSFUL`
- 真机状态：ADB server 已重启，但 `adb devices -l` 仍为空；安装截图验收等待手机重新出现在 USB 调试设备列表后继续。

### 2026-05-24 第一阶段：从 0 到 1 完成全部模块初始实现

#### Gradle 工程初始化
- 更新文件：`settings.gradle.kts`, `build.gradle.kts`(root), `gradle.properties`, `gradle/wrapper/gradle-wrapper.properties`, `gradle/libs.versions.toml`, `gradlew`, `gradlew.bat`, `local.properties`
- 所属模块：根工程
- 完成内容：28 个模块注册、版本目录（AGP 8.5.2, Kotlin 2.0.21, Compose BOM 2024.12.01, Hilt 2.53.1, Retrofit 2.11.0 等）
- 当前状态：Done

#### app 模块
- 更新文件：`app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/values/themes.xml`, `app/src/main/res/values/strings.xml`, `app/proguard-rules.pro`, `app/src/main/java/com/zhihuiji/app/ZhihuijiApp.kt`, `app/src/main/java/com/zhihuiji/app/MainActivity.kt`, `app/src/main/java/com/zhihuiji/app/navigation/AppNavGraph.kt`
- 所属模块：app
- 完成内容：Application 入口、MainActivity、导航图（13 条路由：login/register/dashboard/products/customers/suppliers/sales/purchases/payments/finance/reports/agent/settings）
- 当前状态：Done

#### 第二阶段：五栏底部导航主壳重构（2026-05-24）

##### 文件 1：app/src/main/java/com/zhihuiji/app/navigation/AppNavGraph.kt
- 所属模块：app
- 本次修改内容：重构为认证流（AuthRoutes: login/register）+ 主流程（MainRoutes: main/settings）双层结构。登录成功进入 MainScreen，退出登录清空栈回到登录页
- 当前状态：Done
- 下一步：无

##### 文件 2：app/src/main/java/com/zhihuiji/app/navigation/MainScreen.kt
- 所属模块：app（新建）
- 本次修改内容：创建五栏底部导航主壳。定义 TopLevelRoutes（HOME/DOCUMENTS/ARCHIVES/REPORTS/AGENT）和 bottomBarDestinations（首页/单据/档案/报表/助手），使用 GlassScaffold 统一承载，底部导航切换保留 saveState/restoreState
- 当前状态：Done
- 下一步：无

##### 文件 3：app/src/main/java/com/zhihuiji/app/navigation/MainNavGraph.kt
- 所属模块：app（新建）
- 本次修改内容：主流程内部导航图。5 个 composable 目标：HOME→DashboardScreen, DOCUMENTS→DocumentsScreen, ARCHIVES→ArchivesScreen, REPORTS→ReportScreen, AGENT→AgentWorkbenchScreen。所有子页面传入 showTopBar=false 避免重复标题栏
- 当前状态：Done
- 下一步：无

##### 文件 4：app/src/main/java/com/zhihuiji/app/navigation/DocumentsScreen.kt
- 所属模块：app（新建）
- 本次修改内容：单据Tab容器页面。包含 GlassTopBar(title="单据") + SegmentedTabs(销售单/采购单/付款单/资金流水) + 子页面切换。子页面传入 showTopBar=false
- 当前状态：Done
- 下一步：无

##### 文件 5：app/src/main/java/com/zhihuiji/app/navigation/ArchivesScreen.kt
- 所属模块：app（新建）
- 本次修改内容：档案Tab容器页面。包含 GlassTopBar(title="档案") + SegmentedTabs(商品/客户/供应商) + 子页面切换。子页面传入 showTopBar=false
- 当前状态：Done
- 下一步：无

##### 文件 6：feature/dashboard/src/main/java/com/zhihuiji/feature/dashboard/DashboardScreen.kt
- 所属模块：feature/dashboard
- 本次修改内容：添加 showTopBar: Boolean = true 参数。当 showTopBar=false 时不应用 glassBackground()。在主壳中传入 showTopBar=false
- 当前状态：Done
- 下一步：无

##### 文件 7：feature/sales/src/main/java/com/zhihuiji/feature/sales/SaleOrderListScreen.kt
- 所属模块：feature/sales
- 本次修改内容：添加 showTopBar: Boolean = true 参数。当 showTopBar=false 时不渲染 GlassTopBar 和 glassBackground()。在 DocumentsScreen 中传入 showTopBar=false
- 当前状态：Done
- 下一步：无

##### 文件 8：feature/purchases/src/main/java/com/zhihuiji/feature/purchases/PurchaseOrderListScreen.kt
- 所属模块：feature/purchases
- 本次修改内容：同文件7，添加 showTopBar 参数
- 当前状态：Done
- 下一步：无

##### 文件 9：feature/payments/src/main/java/com/zhihuiji/feature/payments/PayOrderListScreen.kt
- 所属模块：feature/payments
- 本次修改内容：同文件7，添加 showTopBar 参数
- 当前状态：Done
- 下一步：无

##### 文件 10：feature/finance/src/main/java/com/zhihuiji/feature/finance/FinanceRecordListScreen.kt
- 所属模块：feature/finance
- 本次修改内容：同文件7，添加 showTopBar 参数
- 当前状态：Done
- 下一步：无

##### 文件 11：feature/products/src/main/java/com/zhihuiji/feature/products/ProductListScreen.kt
- 所属模块：feature/products
- 本次修改内容：同文件7，添加 showTopBar 参数
- 当前状态：Done
- 下一步：无

##### 文件 12：feature/customers/src/main/java/com/zhihuiji/feature/customers/CustomerListScreen.kt
- 所属模块：feature/customers
- 本次修改内容：同文件7，添加 showTopBar 参数
- 当前状态：Done
- 下一步：无

##### 文件 13：feature/suppliers/src/main/java/com/zhihuiji/feature/suppliers/SupplierListScreen.kt
- 所属模块：feature/suppliers
- 本次修改内容：同文件7，添加 showTopBar 参数
- 当前状态：Done
- 下一步：无

##### 文件 14：feature/reports/src/main/java/com/zhihuiji/feature/reports/ReportScreen.kt
- 所属模块：feature/reports
- 本次修改内容：添加 showTopBar 参数。在主壳中传入 showTopBar=false
- 当前状态：Done
- 下一步：无

##### 文件 15：feature/agent/src/main/java/com/zhihuiji/feature/agent/AgentWorkbenchScreen.kt
- 所属模块：feature/agent
- 本次修改内容：添加 showTopBar 参数。在主壳中传入 showTopBar=false
- 当前状态：Done
- 下一步：无

#### 第三阶段：验收修复（2026-05-24）

##### 文件 1：feature/settings/src/main/java/com/zhihuiji/feature/settings/SettingsScreen.kt
- 所属模块：feature/settings
- 本次修改内容：添加 `onLogout: () -> Unit = {}` 参数；添加 `LaunchedEffect(uiState.isLoggedOut)` 监听，当 ViewModel 的 `isLoggedOut` 变为 true 时自动调用 `onLogout()` 回调触发导航
- 当前状态：Done
- 下一步：无

##### 文件 2：app/src/main/java/com/zhihuiji/app/navigation/AppNavGraph.kt
- 所属模块：app
- 本次修改内容：SettingsScreen 的 composable 传入 `onLogout` 回调，回调内调用 `authViewModel.logout()` 清除会话后 `navController.navigate(AuthRoutes.LOGIN) { popUpTo(0) { inclusive = true } }` 清空全栈回到登录页；移除 MainScreen 上多余的 `onLogout` 参数
- 当前状态：Done
- 下一步：无

##### 文件 3：app/src/main/java/com/zhihuiji/app/navigation/MainScreen.kt
- 所属模块：app
- 本次修改内容：移除 `onLogout: () -> Unit` 参数（退出登录不再经此层传递，改为从 SettingsScreen 直接触发 AppNavGraph 层导航）
- 当前状态：Done
- 下一步：无

##### 文件 4：app/src/main/java/com/zhihuiji/app/navigation/MainNavGraph.kt
- 所属模块：app
- 本次修改内容：移除 `onLogout: () -> Unit` 参数（同上，退出登录不再经此层传递）
- 当前状态：Done
- 下一步：无

##### 文件 5：app/src/main/java/com/zhihuiji/app/navigation/ArchivesScreen.kt
- 所属模块：app
- 本次修改内容：添加 `initialTab: Int = 0` 参数，替代硬编码的 `mutableIntStateOf(0)`，使首页快捷入口可指定初始子页（0=商品, 1=客户, 2=供应商）
- 当前状态：Done
- 下一步：无

##### 文件 6：app/src/main/java/com/zhihuiji/app/navigation/DocumentsScreen.kt
- 所属模块：app
- 本次修改内容：添加 `initialTab: Int = 0` 参数，替代硬编码的 `mutableIntStateOf(0)`，使首页快捷入口可指定初始子页（0=销售单, 1=采购单, 2=付款单, 3=资金流水）
- 当前状态：Done
- 下一步：无

##### 文件 7：app/src/main/java/com/zhihuiji/app/navigation/MainNavGraph.kt
- 所属模块：app
- 本次修改内容：Archives/Documents 路由改为带可选参数 `?initialTab={initialTab}`；DashboardScreen 的 onNavigateToProducts 传入 initialTab=0（商品），onNavigateToCustomers 传入 initialTab=1（客户），onNavigateToSales 传入 initialTab=0（销售单）；移除 onLogout 参数
- 当前状态：Done
- 下一步：无

##### 文件 8：app/src/main/java/com/zhihuiji/app/navigation/MainScreen.kt
- 所属模块：app
- 本次修改内容：底部导航选中状态匹配逻辑从 `it.route == currentRoute` 改为 `currentRoute?.startsWith(dest.route)`，兼容带可选参数的路由；移除 onLogout 参数
- 当前状态：Done
- 下一步：无
- 更新文件：`MoneyFormatter.kt`, `TimeFormatter.kt`, `StatusLabels.kt`, `UiMessage.kt`, `ResultExt.kt`
- 所属模块：core/common
- 完成内容：金额格式化（¥符号、千分位）、时间格式化、状态码中文映射、UI 消息模型、ApiResponse 扩展函数
- 当前状态：Done

#### core/model
- 更新文件：`ApiResponse.kt`, `AuthModels.kt`, `ProductModels.kt`, `PartyModels.kt`, `OrderModels.kt`, `FinanceModels.kt`, `ReportModels.kt`, `AgentModels.kt`, `SyncModels.kt`, `StatusConstants.kt`
- 所属模块：core/model
- 完成内容：全部 DTO（snake_case 字段使用 @SerialName）、Request/Response/Filter 模型、Agent 模型（lowerCamelCase）、状态常量
- 当前状态：Done

#### core/datastore
- 更新文件：`SessionStore.kt`, `SettingsStore.kt`, `SyncPreferenceStore.kt`, `DataStoreModule.kt`
- 所属模块：core/datastore
- 完成内容：三个独立 DataStore（session/settings/sync）、@Named 限定符注入、token/refreshToken/userId 管理、baseUrl/clientId 配置、同步游标持久化
- 当前状态：Done

#### core/network
- 更新文件：`NetworkConfig.kt`, `AuthInterceptor.kt`, `TokenAuthenticator.kt`, `ZhihuijiApi.kt`, `NetworkModule.kt`, `SafeApiCall.kt`
- 所属模块：core/network
- 完成内容：完整 Retrofit API 接口定义（覆盖 android-api-contract.md 所有接口）、Bearer Token 拦截器、Token 自动刷新、Hilt 网络模块、安全 API 调用封装
- 当前状态：Done

#### core/designsystem
- 更新文件：`ZhihuijiColors.kt`, `ZhihuijiTypography.kt`, `ZhihuijiShapes.kt`, `ZhihuijiTheme.kt`, `GlassBackground.kt`, `GlassCard.kt`, `GlassScaffold.kt`, `GlassTopBar.kt`, `PrimaryGradientButton.kt`, `StatusPill.kt`, `KpiCard.kt`, `SearchFilterBar.kt`, `SegmentedTabs.kt`, `FilterChipRow.kt`, `QuantityStepper.kt`, `BottomActionBar.kt`, `EmptyState.kt`, `ChartCard.kt`
- 所属模块：core/designsystem
- 完成内容：浅蓝渐变背景、毛玻璃卡片、蓝色主按钮、状态标签、KPI 卡片、搜索筛选栏、分段选项卡、筛选芯片、数量步进器、底部操作栏、空状态、图表卡片
- 当前状态：Done

#### core/database
- 更新文件：`ZhihuijiDatabase.kt`, `DatabaseModule.kt`, 9 个 Entity（Product/Customer/Supplier/SaleOrder/PurchaseOrder/PayOrder/FinanceRecord/AgentNotification/SyncCursor）, 9 个 Dao
- 所属模块：core/database
- 完成内容：Room 数据库定义、9 个缓存实体、9 个 DAO 接口（observeAll/search/findById/upsert/upsertAll/deleteById/clear）、Hilt 数据库模块
- 当前状态：Done

#### data 层（9 个模块）
- 更新文件：`AuthRepository.kt`, `ProductRepository.kt`, `CustomerRepository.kt`, `SupplierRepository.kt`, `SaleOrderRepository.kt`, `PurchaseOrderRepository.kt`, `PayOrderRepository.kt`, `FinanceRepository.kt`, `ReportRepository.kt`, `AgentRepository.kt`, `SyncRepository.kt`
- 所属模块：data/auth, data/product, data/customer, data/supplier, data/order, data/finance, data/report, data/agent, data/sync
- 完成内容：全部 Repository 实现，在线优先策略，MutableStateFlow 缓存 + API 刷新，safeApiCall 统一错误处理
- 当前状态：Done

#### feature 层（12 个模块）
- 更新文件：每个模块包含 ViewModel + Screen（共 24 个文件）
- 所属模块：feature/auth, feature/dashboard, feature/products, feature/customers, feature/suppliers, feature/sales, feature/purchases, feature/payments, feature/finance, feature/reports, feature/agent, feature/settings
- 完成内容：
  - auth: LoginScreen + RegisterScreen + AuthViewModel
  - dashboard: DashboardScreen（KPI 卡片 + 低库存预警 + 应收排行 + 快捷入口）
  - products: ProductListScreen（搜索 + 库存状态标签 + FAB）
  - customers: CustomerListScreen（搜索 + 余额 + 状态标签）
  - suppliers: SupplierListScreen（状态筛选 Tab + 搜索）
  - sales: SaleOrderListScreen（状态 Tab + 搜索 + 金额 + 状态标签）
  - purchases: PurchaseOrderListScreen（状态 Tab + 列表）
  - payments: PayOrderListScreen（状态 Tab + 列表）
  - finance: FinanceRecordListScreen（收入/支出 Tab + 收支汇总 KPI + 金额颜色语义）
  - reports: ReportScreen（销售/利润/应收/应付 KPI + 热销商品排行）
  - agent: AgentWorkbenchScreen（AI 助手对话 + KPI + 快捷操作）
  - settings: SettingsScreen（账号信息 + 服务器设置 + 同步状态 + 退出登录）
- 当前状态：Done（列表页完成，编辑/详情页待后续迭代）

#### 第四阶段：档案 + 销售单核心业务闭环（2026-05-24）

##### 文件 1：feature/products/src/main/java/com/zhihuiji/feature/products/ProductEditorViewModel.kt
- 所属模块：feature/products（新建）
- 本次修改内容：商品编辑 ViewModel。管理 ProductDraft 状态，支持 loadProduct（编辑模式）、updateDraft、saveProduct（新增/编辑统一入口）、adjustStock（库存调整调用 productRepository.adjustStock）、clearError
- 当前状态：Done
- 下一步：无

##### 文件 2：feature/products/src/main/java/com/zhihuiji/feature/products/ProductEditorScreen.kt
- 所属模块：feature/products（新建）
- 本次修改内容：商品编辑页面。三段 GlassCard（基本信息/价格设置/库存设置），BottomActionBar（保存/取消），编辑模式下显示"库存调整"按钮。接收 productId: Long? 参数，非空则加载已有商品。修复编码字段 onValueChange bug（it.copy(code = it.code) → it.copy(code = v)）
- 当前状态：Done
- 下一步：无

##### 文件 3：feature/products/src/main/java/com/zhihuiji/feature/products/StockAdjustSheet.kt
- 所属模块：feature/products（新建）
- 本次修改内容：库存调整 ModalBottomSheet。SegmentedTabs（入库/出库/盘盈/盘亏），数量输入，原因输入，根据调整类型计算正负 delta
- 当前状态：Done
- 下一步：无

##### 文件 4：feature/customers/src/main/java/com/zhihuiji/feature/customers/CustomerEditorViewModel.kt
- 所属模块：feature/customers（新建）
- 本次修改内容：客户编辑 ViewModel。管理 CustomerDto 作为 draft，支持 loadCustomer、updateDraft、saveCustomer（新增/编辑）、clearError
- 当前状态：Done
- 下一步：无

##### 文件 5：feature/customers/src/main/java/com/zhihuiji/feature/customers/CustomerEditorScreen.kt
- 所属模块：feature/customers（新建）
- 本次修改内容：客户编辑页面。GlassCard 表单（名称/手机号/地址/备注），BottomActionBar（保存/取消），接收 customerId: Long?
- 当前状态：Done
- 下一步：无

##### 文件 6：feature/customers/src/main/java/com/zhihuiji/feature/customers/CustomerDetailViewModel.kt
- 所属模块：feature/customers（新建）
- 本次修改内容：客户详情 ViewModel。通过 customerRepository.getCustomer(id) 加载详情，状态包含 customer/isLoading/error
- 当前状态：Done
- 下一步：无

##### 文件 7：feature/customers/src/main/java/com/zhihuiji/feature/customers/CustomerDetailScreen.kt
- 所属模块：feature/customers（新建）
- 本次修改内容：客户详情页面。信息卡片（名称/手机/地址/状态）、KPI 卡片（应收余额/等级）、备注卡片、BottomActionBar（编辑按钮→导航到编辑器）。修复 notes 跨模块 smart cast 问题（使用局部变量）
- 当前状态：Done
- 下一步：无

##### 文件 8：feature/suppliers/src/main/java/com/zhihuiji/feature/suppliers/SupplierEditorViewModel.kt
- 所属模块：feature/suppliers（新建）
- 本次修改内容：供应商编辑 ViewModel。同 CustomerEditorViewModel 模式，管理 SupplierDto draft
- 当前状态：Done
- 下一步：无

##### 文件 9：feature/suppliers/src/main/java/com/zhihuiji/feature/suppliers/SupplierEditorScreen.kt
- 所属模块：feature/suppliers（新建）
- 本次修改内容：供应商编辑页面。同 CustomerEditorScreen 模式
- 当前状态：Done
- 下一步：无

##### 文件 10：feature/suppliers/src/main/java/com/zhihuiji/feature/suppliers/SupplierDetailViewModel.kt
- 所属模块：feature/suppliers（新建）
- 本次修改内容：供应商详情 ViewModel。通过 supplierRepository.getSupplier(id) 加载详情
- 当前状态：Done
- 下一步：无

##### 文件 11：feature/suppliers/src/main/java/com/zhihuiji/feature/suppliers/SupplierDetailScreen.kt
- 所属模块：feature/suppliers（新建）
- 本次修改内容：供应商详情页面。信息卡片、KPI 卡片（应付余额/状态）、备注卡片、编辑按钮。修复 notes 跨模块 smart cast 问题
- 当前状态：Done
- 下一步：无

##### 文件 12：feature/sales/src/main/java/com/zhihuiji/feature/sales/SaleOrderEditorViewModel.kt
- 所属模块：feature/sales（新建）
- 本次修改内容：销售开单 ViewModel。EditorLineItem 数据类，支持 selectCustomer、searchCustomers/searchProducts（使用 first() 避免 Flow 无限 collect）、addItem/removeItem/changeQuantity、updateNotes/updateDiscount、submitOrder（构建 CreateSaleOrderRequest 调用 saleOrderRepository.createSaleOrder）
- 当前状态：Done
- 下一步：无

##### 文件 13：feature/sales/src/main/java/com/zhihuiji/feature/sales/SaleOrderEditorScreen.kt
- 所属模块：feature/sales（新建）
- 本次修改内容：销售开单页面。客户选择（ModalBottomSheet + 搜索）、商品明细（添加/删除/数量显示）、备注、合计金额、提交订单。添加 @OptIn(ExperimentalMaterial3Api::class) 注解
- 当前状态：Done
- 下一步：无

##### 文件 14：feature/sales/src/main/java/com/zhihuiji/feature/sales/SaleOrderDetailViewModel.kt
- 所属模块：feature/sales（新建）
- 本次修改内容：销售单详情 ViewModel。loadDetail（加载订单+收款记录）、addPayment（调用 saleOrderRepository.addSalePayment）、cancelOrder、completeOrder、clearError
- 当前状态：Done
- 下一步：无

##### 文件 15：feature/sales/src/main/java/com/zhihuiji/feature/sales/SaleOrderDetailScreen.kt
- 所属模块：feature/sales（新建）
- 本次修改内容：销售单详情页面。订单信息卡片（单号+状态+客户+时间）、KPI 卡片（订单金额/已收/待收）、商品明细、收款记录、BottomActionBar（完成订单/收款/作废）。修复 notes 跨模块 smart cast 问题
- 当前状态：Done
- 下一步：无

##### 文件 16：feature/sales/src/main/java/com/zhihuiji/feature/sales/SalePaymentSheet.kt
- 所属模块：feature/sales（新建）
- 本次修改内容：收款 ModalBottomSheet。金额输入、收款方式 FilterChips（现金/微信/支付宝/银行卡）、参考号输入、确认收款按钮
- 当前状态：Done
- 下一步：无

##### 文件 17：feature/products/src/main/java/com/zhihuiji/feature/products/ProductListScreen.kt
- 所属模块：feature/products
- 本次修改内容：添加 `onNavigateToEditor: (Long?) -> Unit = {}` 导航回调。FAB 点击传入 null（新增），列表项点击传入 product.id（编辑）
- 当前状态：Done
- 下一步：无

##### 文件 18：feature/customers/src/main/java/com/zhihuiji/feature/customers/CustomerListScreen.kt
- 所属模块：feature/customers
- 本次修改内容：添加 `onNavigateToEditor: (Long?) -> Unit = {}` 和 `onNavigateToDetail: (Long) -> Unit = {}` 导航回调。FAB→新增编辑器，列表项→详情页
- 当前状态：Done
- 下一步：无

##### 文件 19：feature/suppliers/src/main/java/com/zhihuiji/feature/suppliers/SupplierListScreen.kt
- 所属模块：feature/suppliers
- 本次修改内容：同 CustomerListScreen，添加 onNavigateToEditor 和 onNavigateToDetail 导航回调
- 当前状态：Done
- 下一步：无

##### 文件 20：feature/sales/src/main/java/com/zhihuiji/feature/sales/SaleOrderListScreen.kt
- 所属模块：feature/sales
- 本次修改内容：添加 `onNavigateToEditor: () -> Unit = {}` 和 `onNavigateToDetail: (Long) -> Unit = {}` 导航回调。FAB→开单页，列表项→详情页
- 当前状态：Done
- 下一步：无

##### 文件 21：app/src/main/java/com/zhihuiji/app/navigation/ArchivesScreen.kt
- 所属模块：app
- 本次修改内容：添加 5 个导航回调参数（onNavigateToProductEditor/onNavigateToCustomerEditor/onNavigateToCustomerDetail/onNavigateToSupplierEditor/onNavigateToSupplierDetail），传递给对应列表页
- 当前状态：Done
- 下一步：无

##### 文件 22：app/src/main/java/com/zhihuiji/app/navigation/DocumentsScreen.kt
- 所属模块：app
- 本次修改内容：添加 2 个导航回调参数（onNavigateToSaleOrderEditor/onNavigateToSaleOrderDetail），传递给 SaleOrderListScreen
- 当前状态：Done
- 下一步：无

##### 文件 23：app/src/main/java/com/zhihuiji/app/navigation/MainNavGraph.kt
- 所属模块：app
- 本次修改内容：新增 SubRoutes 对象定义 7 条子路由（product_editor/customer_editor/customer_detail/supplier_editor/supplier_detail/sale_order_editor/sale_order_detail）；新增 7 个 composable 目标处理编辑器/详情页导航；ArchivesScreen/DocumentsScreen 传入导航回调使用 navController.navigate；编辑器路由使用可选参数（?productId={productId}），详情路由使用必选参数（/{customerId}）；navigateBack() 封装 navController.popBackStack()
- 当前状态：Done
- 下一步：无

#### 第五阶段：采购单/付款单/资金流水业务闭环 + 文档状态修正（2026-05-24）

##### 文件 0：DEVELOPMENT-PLAN.md（状态修正）
- 所属模块：根工程
- 本次修改内容：将 feature/purchases、feature/payments、feature/finance 从 Done 改为 In Progress；将 data/sync 从 Done 改为 In Progress
- 当前状态：Done
- 下一步：无

##### 文件 1-10：各子模块 DEVELOPMENT.md（状态同步）
- 所属模块：feature/products, feature/customers, feature/suppliers, feature/sales, feature/purchases, feature/payments, feature/finance, data/order, data/finance, data/sync
- 本次修改内容：将所有子模块 DEVELOPMENT.md 的"当前状态"行更新为真实进度（products/customers/suppliers/sales 标为已完成，purchases/payments/finance 标为列表页已完成，data/order/data/finance 标为已实现，data/sync 标为开发中）
- 当前状态：Done
- 下一步：无

##### 文件 11：feature/purchases/src/main/java/com/zhihuiji/feature/purchases/PurchaseOrderEditorViewModel.kt
- 所属模块：feature/purchases（新建）
- 本次修改内容：采购开单 ViewModel。PurchaseLineItem 数据类，支持 selectSupplier、searchSuppliers/searchProducts（使用 first()）、addItem/removeItem/changeQuantity、updateNotes、submitOrder（构建 CreatePurchaseOrderRequest 调用 purchaseOrderRepository.createPurchaseOrder）
- 当前状态：Done
- 下一步：无

##### 文件 12：feature/purchases/src/main/java/com/zhihuiji/feature/purchases/PurchaseOrderEditorScreen.kt
- 所属模块：feature/purchases（新建）
- 本次修改内容：采购开单页面。供应商选择（ModalBottomSheet + 搜索）、商品明细（添加/删除/数量显示）、备注、应付合计金额、提交采购单
- 当前状态：Done
- 下一步：无

##### 文件 13：feature/purchases/src/main/java/com/zhihuiji/feature/purchases/PurchaseOrderDetailViewModel.kt
- 所属模块：feature/purchases（新建）
- 本次修改内容：采购单详情 ViewModel。loadDetail 通过 purchaseOrderRepository.getPurchaseOrder(id) 加载
- 当前状态：Done
- 下一步：无

##### 文件 14：feature/purchases/src/main/java/com/zhihuiji/feature/purchases/PurchaseOrderDetailScreen.kt
- 所属模块：feature/purchases（新建）
- 本次修改内容：采购单详情页面。订单信息卡片（单号+状态+供应商+时间）、KPI 卡片（应付金额）、商品明细、备注卡片、返回按钮
- 当前状态：Done
- 下一步：无

##### 文件 15：feature/payments/src/main/java/com/zhihuiji/feature/payments/PayOrderEditorViewModel.kt
- 所属模块：feature/payments（新建）
- 本次修改内容：付款单新建 ViewModel。支持 selectSupplier、searchSuppliers、updateAmount/updateMethod/updateReferenceNo/updateNotes、submitOrder（构建 CreatePayOrderRequest 调用 payOrderRepository.createPayOrder）
- 当前状态：Done
- 下一步：无

##### 文件 16：feature/payments/src/main/java/com/zhihuiji/feature/payments/PayOrderEditorScreen.kt
- 所属模块：feature/payments（新建）
- 本次修改内容：付款单新建页面。供应商选择（ModalBottomSheet + 搜索）、付款金额输入、付款方式 FilterChips（现金/微信/支付宝/银行卡）、参考号、备注、创建付款单
- 当前状态：Done
- 下一步：无

##### 文件 17：feature/payments/src/main/java/com/zhihuiji/feature/payments/PayOrderDetailViewModel.kt
- 所属模块：feature/payments（新建）
- 本次修改内容：付款单详情 ViewModel。loadDetail、updateStatus（调用 payOrderRepository.updatePayOrderStatus）、cancelOrder、completeOrder
- 当前状态：Done
- 下一步：无

##### 文件 18：feature/payments/src/main/java/com/zhihuiji/feature/payments/PayOrderDetailScreen.kt
- 所属模块：feature/payments（新建）
- 本次修改内容：付款单详情页面。订单信息卡片（单号+状态+供应商+付款方式+时间）、KPI 卡片（付款金额）、参考号卡片、备注卡片、BottomActionBar（确认付款/取消）
- 当前状态：Done
- 下一步：无

##### 文件 19：feature/finance/src/main/java/com/zhihuiji/feature/finance/FinanceRecordEditorSheet.kt
- 所属模块：feature/finance（新建）
- 本次修改内容：新增流水 ModalBottomSheet。收入/支出类型选择、分类输入、金额输入、付款方式 FilterChips、备注输入、确认新增按钮
- 当前状态：Done
- 下一步：无

##### 文件 20：feature/finance/src/main/java/com/zhihuiji/feature/finance/FinanceViewModel.kt
- 所属模块：feature/finance
- 本次修改内容：扩展 FinanceListUiState 添加 createSuccess/error 字段；loadRecords 改用 first() 避免 Flow 无限 collect；新增 createRecord 方法（构建 CreateFinanceRecordRequest 调用 financeRepository.createFinanceRecord）；新增 clearCreateSuccess/clearError
- 当前状态：Done
- 下一步：无

##### 文件 21：feature/finance/src/main/java/com/zhihuiji/feature/finance/FinanceRecordListScreen.kt
- 所属模块：feature/finance
- 本次修改内容：添加 FAB（+按钮）触发 FinanceRecordEditorSheet；LaunchedEffect 监听 createSuccess 自动关闭 Sheet；使用 Box 布局支持 FAB 浮动
- 当前状态：Done
- 下一步：无

##### 文件 22：feature/purchases/src/main/java/com/zhihuiji/feature/purchases/PurchaseOrderListScreen.kt
- 所属模块：feature/purchases
- 本次修改内容：添加 onNavigateToEditor 和 onNavigateToDetail 导航回调。FAB→开单页，列表项→详情页
- 当前状态：Done
- 下一步：无

##### 文件 23：feature/payments/src/main/java/com/zhihuiji/feature/payments/PayOrderListScreen.kt
- 所属模块：feature/payments
- 本次修改内容：同 PurchaseOrderListScreen，添加 onNavigateToEditor 和 onNavigateToDetail 导航回调
- 当前状态：Done
- 下一步：无

##### 文件 24：app/src/main/java/com/zhihuiji/app/navigation/DocumentsScreen.kt
- 所属模块：app
- 本次修改内容：添加 4 个导航回调参数（onNavigateToPurchaseOrderEditor/onNavigateToPurchaseOrderDetail/onNavigateToPayOrderEditor/onNavigateToPayOrderDetail），传递给 PurchaseOrderListScreen 和 PayOrderListScreen
- 当前状态：Done
- 下一步：无

##### 文件 25：app/src/main/java/com/zhihuiji/app/navigation/MainNavGraph.kt
- 所属模块：app
- 本次修改内容：SubRoutes 新增 4 条子路由（purchase_order_editor/purchase_order_detail/pay_order_editor/pay_order_detail）；新增 4 个 composable 目标处理采购单/付款单的编辑器和详情页导航；DocumentsScreen 传入采购单/付款单的导航回调
- 当前状态：Done
- 下一步：无

- 第一阶段：`./gradlew assembleDebug` → **BUILD SUCCESSFUL**（2026-05-24）
- 第二阶段：`./gradlew assembleDebug` → **BUILD SUCCESSFUL**（2026-05-24，五栏导航主壳重构后）
- 第三阶段：`./gradlew assembleDebug` → **BUILD SUCCESSFUL**（2026-05-24，退出登录链路+快捷入口落点修复后）
- 第四阶段：`./gradlew assembleDebug` → **BUILD SUCCESSFUL**（2026-05-24，档案+销售单核心业务闭环完成后）
- 第五阶段：`./gradlew assembleDebug` → **BUILD SUCCESSFUL**（2026-05-24，采购单/付款单/资金流水业务闭环完成后）
- 第六阶段：`./gradlew assembleDebug` → **BUILD SUCCESSFUL**（2026-05-24，Room 列表缓存接线 + 手动同步落库修复后）

## 第七阶段：远程联调修复

##### 文件 1：core/network/src/main/java/com/zhihuiji/core/network/AuthInterceptor.kt
- 所属模块：core/network
- 本次修改内容：修复未登录态联调崩溃。对 `auth/login`、`auth/register`、`auth/refresh`、`auth/verify-code` 路径跳过鉴权头；其余请求改为“有 token 则附加，无 token 则不强制抛错”
- 当前状态：Done
- 下一步：继续模拟器登录联调，验证远程 117 后端全链路

##### 文件 2：app/src/main/AndroidManifest.xml
- 所属模块：app
- 本次修改内容：为远程联调地址 `http://117.72.79.106/zhihuiji/v1/` 打开 `usesCleartextTraffic`
- 当前状态：Done
- 下一步：重新安装 APK 并执行 `test-android-apps` 流程验证登录、首页与核心页面

##### 文件 3：core/model/src/main/java/com/zhihuiji/core/model/AuthModels.kt
- 所属模块：core/model
- 本次修改内容：修复认证模型与后端 `SNAKE_CASE` 响应/请求不一致问题；`AuthResult`、`RegisterRequest`、`RefreshRequest`、`VerifyCodeResponse` 改为使用 `snake_case` 字段映射
- 当前状态：Done
- 下一步：重新安装 APK，验证登录进入主流程并继续测试首页与列表联调

##### 文件 4：core/network/src/main/java/com/zhihuiji/core/network/NetworkModule.kt
- 所属模块：core/network
- 本次修改内容：为远程联调请求开启 `encodeDefaults = true`，保证默认值字段（如 `status`、`stock`）在创建类请求中也会发送到后端
- 当前状态：Done
- 下一步：重新验证商品新增等依赖默认字段的创建链路

##### 文件 5：core/model/src/main/java/com/zhihuiji/core/model/ProductModels.kt
- 所属模块：core/model
- 本次修改内容：修复商品 DTO 与后端 `snake_case` 契约不一致；`sale_price/purchase_price/safe_stock/sync_*` 改为蛇形映射，并在 `ProductDraft.toDto()` 中显式补上 `stock`
- 当前状态：Done
- 下一步：重装包后继续跑商品新增和后续业务联调

##### 文件 6：core/model/src/main/java/com/zhihuiji/core/model/PartyModels.kt
- 所属模块：core/model
- 本次修改内容：修复客户/供应商 DTO 的 `sync_*`、`created_at`、`updated_at` 蛇形映射，保证远程联调时列表与详情字段回填正确
- 当前状态：Done
- 下一步：继续验证客户、供应商和依赖它们的业务单据

##### 文件 7：core/model/src/main/java/com/zhihuiji/core/model/FinanceModels.kt
- 所属模块：core/model
- 本次修改内容：修复资金流水 DTO/请求中的 `record_no`、`partner_name`、`created_at`、`updated_at` 蛇形映射
- 当前状态：Done
- 下一步：继续验证资金流水新增与列表回显

##### 文件 8：core/model/src/main/java/com/zhihuiji/core/model/OrderModels.kt
- 所属模块：core/model
- 本次修改内容：修复销售单/采购单/付款单 DTO 与请求模型的蛇形字段映射，包括 `*_id`、`*_name`、`order_no`、`unit_price`、`unit_cost`、`discount_amount`、`reference_no`、`created_at`、`updated_at`
- 当前状态：Done
- 下一步：继续验证销售、采购、付款业务提交流程

##### 文件 9：feature/reports/src/main/java/com/zhihuiji/feature/reports/ReportScreen.kt
- 所属模块：feature/reports
- 本次修改内容：为报表页增加基于 `Lifecycle.Event.ON_RESUME` 的主动刷新，解决远程联调中创建销售单后重新进入报表页仍显示旧 `0.00` 的问题
- 当前状态：Done
- 下一步：重新安装 APK 并验证报表 KPI 会随远程后端最新销售数据刷新

##### 文件 10：core/model/src/main/java/com/zhihuiji/core/model/ReportModels.kt
- 所属模块：core/model
- 本次修改内容：修复报表 DTO 与后端 `snake_case` 契约不一致；`sales/profit/reconciliation/top-products/low-stock/top-receivable` 等字段改为蛇形映射，解决报表接口返回成功但 UI 仍显示默认 `0.00` 的问题
- 当前状态：Done
- 下一步：重新安装 APK，复测报表页 KPI 与后端汇总值是否一致

## 第八阶段：按设计稿统一 UI 视觉重构

##### 文件 1：app/src/main/AndroidManifest.xml
- 所属模块：app
- 本次修改内容：将 MainActivity 锁定为 portrait，避免 Android 设备/模拟器横屏导致界面与设计稿比例完全不一致
- 当前状态：Done
- 下一步：真机在线后复测竖屏显示

##### 文件 2：core/designsystem/src/main/java/com/zhihuiji/core/designsystem/ZhihuijiColors.kt
- 所属模块：core/designsystem
- 本次修改内容：重定义浅蓝业务色板，补充 BackgroundGradientMid、PressedBlue、CardBackground、CardBorder 等设计稿所需颜色
- 当前状态：Done
- 下一步：根据真机截图继续微调透明度和边框色

##### 文件 3：core/designsystem/src/main/java/com/zhihuiji/core/designsystem/GlassBackground.kt
- 所属模块：core/designsystem
- 本次修改内容：背景改为浅蓝到白色的三段式竖向渐变，贴近设计稿顶部蓝雾感
- 当前状态：Done
- 下一步：真机截图后检查顶部蓝色浓度

##### 文件 4：core/designsystem/src/main/java/com/zhihuiji/core/designsystem/GlassCard.kt
- 所属模块：core/designsystem
- 本次修改内容：统一毛玻璃卡片为 10dp 圆角、浅蓝边框、低阴影、白色半透明底
- 当前状态：Done
- 下一步：继续用于所有业务页面

##### 文件 5：core/designsystem/src/main/java/com/zhihuiji/core/designsystem/GlassTopBar.kt
- 所属模块：core/designsystem
- 本次修改内容：顶部栏高度收敛到 56dp，标题样式改为更接近移动端业务 App 的紧凑标题
- 当前状态：Done
- 下一步：真机验证状态栏和标题区间距

##### 文件 6：core/designsystem/src/main/java/com/zhihuiji/core/designsystem/GlassScaffold.kt
- 所属模块：core/designsystem
- 本次修改内容：五栏底部导航改为白色半透明容器、浅蓝选中指示、紧凑高度，贴近设计稿底栏
- 当前状态：Done
- 下一步：真机验证底部安全区

##### 文件 7：core/designsystem/src/main/java/com/zhihuiji/core/designsystem/SearchFilterBar.kt
- 所属模块：core/designsystem
- 本次修改内容：搜索与筛选栏改为白色半透明输入框、9dp 圆角、浅蓝边框
- 当前状态：Done
- 下一步：列表页继续复用

##### 文件 8：core/designsystem/src/main/java/com/zhihuiji/core/designsystem/SegmentedTabs.kt
- 所属模块：core/designsystem
- 本次修改内容：分段 Tab 改为横向紧凑胶囊，选中态使用浅蓝底和主蓝文字
- 当前状态：Done
- 下一步：如设计稿需要下划线态，可在后续继续调整

##### 文件 9：core/designsystem/src/main/java/com/zhihuiji/core/designsystem/PrimaryGradientButton.kt
- 所属模块：core/designsystem
- 本次修改内容：主按钮高度、圆角、渐变和二级按钮样式统一为设计稿底部操作按钮风格
- 当前状态：Done
- 下一步：所有提交/确认类动作继续使用

##### 文件 10：core/designsystem/src/main/java/com/zhihuiji/core/designsystem/QuantityStepper.kt
- 所属模块：core/designsystem
- 本次修改内容：数量步进器按钮缩小到紧凑业务表格尺寸，适配开单商品明细行
- 当前状态：Done
- 下一步：销售/采购开单继续复用

##### 文件 11：app/src/main/java/com/zhihuiji/app/navigation/DocumentsScreen.kt
- 所属模块：app
- 本次修改内容：单据主 Tab 顶部改为“智慧记 + 当前单据类型 + 搜索/新增动作”，新增按钮按当前子 Tab 跳转销售/采购/付款创建页
- 当前状态：Done
- 下一步：资金流水继续使用页内新增 FAB

##### 文件 12：app/src/main/java/com/zhihuiji/app/navigation/ArchivesScreen.kt
- 所属模块：app
- 本次修改内容：档案主 Tab 顶部改为“智慧记 + 当前档案类型 + 筛选/新增动作”，新增按钮按当前子 Tab 跳转商品/客户/供应商创建页
- 当前状态：Done
- 下一步：后续补真实筛选弹窗

##### 文件 13：feature/sales/src/main/java/com/zhihuiji/feature/sales/SaleOrderListScreen.kt
- 所属模块：feature/sales
- 本次修改内容：销售单列表重构为搜索栏、状态分段、筛选行、紧凑订单卡、底部统计，嵌入主壳时隐藏重复 FAB
- 当前状态：Done
- 下一步：真机截图后微调卡片行高

##### 文件 14：feature/sales/src/main/java/com/zhihuiji/feature/sales/SaleOrderEditorScreen.kt
- 所属模块：feature/sales
- 本次修改内容：销售开单页面重构为客户/仓库/商品明细/合计金额分组卡片，使用底部“保存草稿 + 提交订单”
- 当前状态：Done
- 下一步：补充商品图片/规格后可继续增强明细展示

##### 文件 15：feature/sales/src/main/java/com/zhihuiji/feature/sales/SaleOrderDetailScreen.kt
- 所属模块：feature/sales
- 本次修改内容：销售单详情重构为单号状态头、金额三栏、商品明细、收款记录与底部操作栏
- 当前状态：Done
- 下一步：真机复测收款入口

##### 文件 16：feature/sales/src/main/java/com/zhihuiji/feature/sales/SalePaymentSheet.kt
- 所属模块：feature/sales
- 本次修改内容：收款弹窗改为“单据信息 + 本次收款”分组卡片，按钮和输入框统一设计系统
- 当前状态：Done
- 下一步：真机验证键盘弹出时底部按钮位置

##### 文件 17：feature/products/src/main/java/com/zhihuiji/feature/products/ProductListScreen.kt
- 所属模块：feature/products
- 本次修改内容：商品列表改为搜索、库存状态分段、紧凑商品卡片，嵌入档案主壳时隐藏重复 FAB
- 当前状态：Done
- 下一步：后续可接商品缩略图

##### 文件 18：feature/customers/src/main/java/com/zhihuiji/feature/customers/CustomerListScreen.kt
- 所属模块：feature/customers
- 本次修改内容：客户列表改为搜索、状态分段、客户卡片、欠款状态胶囊，嵌入主壳时隐藏重复 FAB
- 当前状态：Done
- 下一步：后续可接客户等级筛选

##### 文件 19：feature/suppliers/src/main/java/com/zhihuiji/feature/suppliers/SupplierListScreen.kt
- 所属模块：feature/suppliers
- 本次修改内容：供应商列表改为搜索、紧凑卡片、应付金额和状态胶囊，嵌入主壳时隐藏重复 FAB
- 当前状态：Done
- 下一步：后续可接供应商分类筛选

##### 文件 20：feature/purchases/src/main/java/com/zhihuiji/feature/purchases/PurchaseOrderListScreen.kt
- 所属模块：feature/purchases
- 本次修改内容：采购单列表改为搜索栏、紧凑订单卡、状态胶囊和底部安全留白，嵌入主壳时隐藏重复 FAB
- 当前状态：Done
- 下一步：真机验证单据 Tab 内滚动体验

##### 文件 21：feature/payments/src/main/java/com/zhihuiji/feature/payments/PayOrderListScreen.kt
- 所属模块：feature/payments
- 本次修改内容：付款单列表改为搜索栏、紧凑付款单卡、状态胶囊和底部安全留白，嵌入主壳时隐藏重复 FAB
- 当前状态：Done
- 下一步：真机验证付款单详情入口

##### 文件 22：feature/finance/src/main/java/com/zhihuiji/feature/finance/FinanceRecordListScreen.kt
- 所属模块：feature/finance
- 本次修改内容：资金流水列表改为紧凑流水卡，收入/支出金额使用不同色彩表达，并保留新增流水 FAB
- 当前状态：Done
- 下一步：真机验证新增流水弹窗

##### 文件 23：feature/purchases/src/main/java/com/zhihuiji/feature/purchases/PurchaseOrderEditorScreen.kt
- 所属模块：feature/purchases
- 本次修改内容：采购开单页面重构为供应商/商品明细/合计分组卡片，商品行加入紧凑数量步进器和删除操作
- 当前状态：Done
- 下一步：后续补仓库选择

##### 文件 24：feature/purchases/src/main/java/com/zhihuiji/feature/purchases/PurchaseOrderDetailScreen.kt
- 所属模块：feature/purchases
- 本次修改内容：采购单详情重构为单号状态头、金额三栏、商品明细和备注卡片
- 当前状态：Done
- 下一步：后续补入库/付款联动入口

##### 文件 25：feature/payments/src/main/java/com/zhihuiji/feature/payments/PayOrderDetailScreen.kt
- 所属模块：feature/payments
- 本次修改内容：付款单详情改为紧凑单据头、付款金额卡、参考号/备注卡片和固定底部操作
- 当前状态：Done
- 下一步：真机验证确认付款按钮

##### 文件 26：feature/finance/src/main/java/com/zhihuiji/feature/finance/FinanceRecordEditorSheet.kt
- 所属模块：feature/finance
- 本次修改内容：新增流水弹窗改为流水信息/结算方式双分组卡片，输入框和筛选芯片统一设计系统
- 当前状态：Done
- 下一步：真机验证弹窗高度和键盘避让

##### 文件 27：feature/dashboard/src/main/java/com/zhihuiji/feature/dashboard/DashboardScreen.kt
- 所属模块：feature/dashboard
- 本次修改内容：首页改为紧凑顶部品牌区、KPI 卡片、快捷开单卡片、预警/应收排行卡片，并预留底部导航安全距离
- 当前状态：Done
- 下一步：真机验证首页首屏信息密度

##### 文件 28：feature/reports/src/main/java/com/zhihuiji/feature/reports/ReportScreen.kt
- 所属模块：feature/reports
- 本次修改内容：报表页加入时间分段 Tab，KPI 和热销商品列表改为紧凑卡片展示
- 当前状态：Done
- 下一步：后续接真实时间筛选参数

##### 文件 29：feature/agent/src/main/java/com/zhihuiji/feature/agent/AgentWorkbenchScreen.kt
- 所属模块：feature/agent
- 本次修改内容：AI 助手页改为横向助手身份卡、KPI 卡、问题输入卡和快捷问题按钮，整体缩小留白
- 当前状态：Done
- 下一步：真机验证输入框与发送按钮宽度

##### 文件 30：feature/settings/src/main/java/com/zhihuiji/feature/settings/SettingsScreen.kt
- 所属模块：feature/settings
- 本次修改内容：设置页改为 12dp 页面边距和紧凑分组卡片，移除冗长说明文字，保留账号/服务器/同步/退出
- 当前状态：Done
- 下一步：后续补服务器连接测试按钮

##### 文件 31：feature/auth/src/main/java/com/zhihuiji/feature/auth/LoginScreen.kt
- 所属模块：feature/auth
- 本次修改内容：登录页收紧卡片内外边距，统一输入框和按钮节奏，避免居中表单在手机上过松
- 当前状态：Done
- 下一步：真机验证软键盘弹出效果

##### 文件 32：feature/auth/src/main/java/com/zhihuiji/feature/auth/RegisterScreen.kt
- 所属模块：feature/auth
- 本次修改内容：注册页收紧顶部/表单间距，表单字段统一使用 12dp 间隔和毛玻璃卡片
- 当前状态：Done
- 下一步：真机验证返回按钮和状态栏间距

##### 文件 33：feature/products/src/main/java/com/zhihuiji/feature/products/ProductEditorScreen.kt
- 所属模块：feature/products
- 本次修改内容：商品编辑页外边距从 16dp 收敛为 12dp/8dp，分组卡片间距统一为 10dp
- 当前状态：Done
- 下一步：继续按真机截图微调输入框高度

##### 文件 34：feature/customers/src/main/java/com/zhihuiji/feature/customers/CustomerEditorScreen.kt
- 所属模块：feature/customers
- 本次修改内容：客户编辑页外边距和卡片间距统一到新 UI 规范
- 当前状态：Done
- 下一步：无

##### 文件 35：feature/suppliers/src/main/java/com/zhihuiji/feature/suppliers/SupplierEditorScreen.kt
- 所属模块：feature/suppliers
- 本次修改内容：供应商编辑页外边距和卡片间距统一到新 UI 规范
- 当前状态：Done
- 下一步：无

##### 文件 36：feature/customers/src/main/java/com/zhihuiji/feature/customers/CustomerDetailScreen.kt
- 所属模块：feature/customers
- 本次修改内容：客户详情页外边距和信息卡片间距统一到新 UI 规范
- 当前状态：Done
- 下一步：无

##### 文件 37：feature/suppliers/src/main/java/com/zhihuiji/feature/suppliers/SupplierDetailScreen.kt
- 所属模块：feature/suppliers
- 本次修改内容：供应商详情页外边距和信息卡片间距统一到新 UI 规范
- 当前状态：Done
- 下一步：无

##### 验证记录
- `./gradlew :app:assembleDebug` → **BUILD SUCCESSFUL**（2026-05-25，第八阶段 UI 重构后）
- `adb devices -l` → 真机 `d715a3a4` 在线，已可安装与截图验收
- 真机截图目录：`/Users/sunyiyang/Desktop/Project/master-goods/android-test-screenshots/round11-final-ui-pass`

### 第九阶段：设计稿逐页对照调试记录（2026-05-25）

#### 本阶段目标
- 以 `/Users/sunyiyang/Desktop/Project/master-goods/image doc/01.png`、`03.png`、`06.png`、`08.png` 为主参考，逐页核对首页、单据、档案、报表、AI 工作台。
- 不做安卓单机离线版，本阶段只做在线优先 UI 对齐和真机截图验证。

#### 逐文件记录

##### 文件 38：core/designsystem/src/main/java/com/zhihuiji/core/designsystem/ChartCard.kt
- 所属模块：core/designsystem
- 本次修改内容：补齐设计稿级报表图表组件，包括 `LineTrendChart`、`RingMetricChart`、`HorizontalBarChart` 和图例行；用于首页销售趋势、报表趋势、收款结构、往来余额和排行条。
- 当前状态：Done
- 下一步：后续按真实数据维度补坐标轴数值和 tooltip。

##### 文件 39：feature/reports/src/main/java/com/zhihuiji/feature/reports/ReportScreen.kt
- 所属模块：feature/reports
- 本次修改内容：报表页由纯 KPI/列表升级为设计稿式报表中心，包含时间分段、KPI、销售趋势折线图、双环形统计图、热销商品排行和应收客户排行。
- 当前状态：Done
- 下一步：继续接入更丰富的后端报表数据，使排行和图表不依赖少量演示数据。

##### 文件 40：feature/dashboard/src/main/java/com/zhihuiji/feature/dashboard/DashboardScreen.kt
- 所属模块：feature/dashboard
- 本次修改内容：首页 KPI 改为设计稿式“左侧文字 + 右侧淡色圆形图标”，顶部操作从刷新/齿轮调整为通知/扫码视觉；补齐销售趋势、待处理提醒和快捷开单区域。
- 当前状态：Done
- 下一步：如需保留设置入口，需要在扫码/通知入口之外增加设计稿兼容的隐藏或二级入口。

##### 文件 41：core/designsystem/src/main/java/com/zhihuiji/core/designsystem/KpiCard.kt
- 所属模块：core/designsystem
- 本次修改内容：重构通用 KPI 卡片布局，统一为设计稿使用的右侧圆形图标、左侧标题/数值/趋势结构，影响首页、报表和 AI 工作台。
- 当前状态：Done
- 下一步：后续按页面需要支持小尺寸和横向紧凑变体。

##### 文件 42：core/designsystem/src/main/java/com/zhihuiji/core/designsystem/ZhihuijiTheme.kt
- 所属模块：core/designsystem
- 本次修改内容：固定 Compose 字体缩放为 `fontScale = 1f`，避免真机系统字号导致页面密度偏离设计稿。
- 当前状态：Done
- 下一步：如需无障碍大字体，需要单独做响应式布局策略。

##### 文件 43：core/designsystem/src/main/java/com/zhihuiji/core/designsystem/ZhihuijiTypography.kt
- 所属模块：core/designsystem
- 本次修改内容：整体收敛字号层级，让列表、KPI、标签和图表更接近设计稿的高密度移动端样式。
- 当前状态：Done
- 下一步：继续按截图微调 `titleMedium` 和 `bodyMedium` 的权重。

##### 文件 44：core/designsystem/src/main/java/com/zhihuiji/core/designsystem/SearchFilterBar.kt
- 所属模块：core/designsystem
- 本次修改内容：搜索框由默认 `OutlinedTextField` 改为紧凑自绘搜索框，高度、圆角和边框贴近设计图中的浅色毛玻璃搜索条。
- 当前状态：Done
- 下一步：支持左侧搜索图标和右侧筛选图标的更多布局组合。

##### 文件 45：core/designsystem/src/main/java/com/zhihuiji/core/designsystem/SegmentedTabs.kt
- 所属模块：core/designsystem
- 本次修改内容：分段 Tab 收紧外边距、芯片高度和水平间距，使单据/档案筛选条接近设计稿。
- 当前状态：Done
- 下一步：根据设计稿补选中态底部线或更轻的浅蓝背景变体。

##### 文件 46：feature/products/src/main/java/com/zhihuiji/feature/products/ProductListScreen.kt
- 所属模块：feature/products
- 本次修改内容：商品列表行加入左侧商品缩略图占位、右侧状态胶囊和更紧凑的价格/库存信息，对齐商品列表设计稿。
- 当前状态：Done
- 下一步：接入真实商品图片或本地分类图标资源。

##### 文件 47：feature/sales/src/main/java/com/zhihuiji/feature/sales/SaleOrderListScreen.kt
- 所属模块：feature/sales
- 本次修改内容：销售单列表收紧卡片内边距和列表间距，靠近设计稿的单据卡密度。
- 当前状态：Done
- 下一步：如数据充足，继续验证多条列表下的底部合计栏位置。

##### 文件 48：feature/purchases/src/main/java/com/zhihuiji/feature/purchases/PurchaseOrderListScreen.kt
- 所属模块：feature/purchases
- 本次修改内容：采购单列表收紧卡片内边距和列表间距，维持与销售单列表一致的单据风格。
- 当前状态：Done
- 下一步：补充更多采购状态数据后继续对照设计稿 06。

##### 文件 49：feature/payments/src/main/java/com/zhihuiji/feature/payments/PayOrderListScreen.kt
- 所属模块：feature/payments
- 本次修改内容：付款单列表收紧卡片内边距和列表间距，保持单据域视觉一致。
- 当前状态：Done
- 下一步：后续继续微调状态胶囊颜色。

##### 文件 50：feature/agent/src/main/java/com/zhihuiji/feature/agent/AgentWorkbenchScreen.kt
- 所属模块：feature/agent
- 本次修改内容：AI 工作台按设计稿 08 重构为品牌助手卡、四 KPI 卡、经营洞察、快捷操作、2x2 问题胶囊和输入发送区。
- 当前状态：Done
- 下一步：继续补 AI 问答页、操作草稿页、任务通知页三个二级界面。

##### 文件 51：feature/auth/src/main/java/com/zhihuiji/feature/auth/AuthViewModel.kt
- 所属模块：feature/auth
- 本次修改内容：新增 `isSessionReady`，防止会话恢复前先渲染登录页再跳主流程，解决首页截图透出登录表单的问题。
- 当前状态：Done
- 下一步：后续可增加启动页超时兜底和错误状态。

##### 文件 52：app/src/main/java/com/zhihuiji/app/navigation/AppNavGraph.kt
- 所属模块：app
- 本次修改内容：在会话初始化完成前显示轻量品牌启动屏，避免认证流与主流程产生视觉层叠。
- 当前状态：Done
- 下一步：后续可以替换为设计稿级启动页。

##### 文件 53：core/designsystem/src/main/java/com/zhihuiji/core/designsystem/GlassScaffold.kt
- 所属模块：core/designsystem
- 本次修改内容：新增 `showBottomBar` 参数，允许编辑页/详情页隐藏底部五栏导航，避免二级页面与设计稿不一致。
- 当前状态：Done
- 下一步：后续可为二级页增加统一沉浸式顶部栏参数。

##### 文件 54：app/src/main/java/com/zhihuiji/app/navigation/MainScreen.kt
- 所属模块：app
- 本次修改内容：根据当前路由判断是否属于五个一级 Tab，只在首页/单据/档案/报表/助手显示底栏；销售开单、新增商品等二级路由隐藏底栏。
- 当前状态：Done
- 下一步：继续核对所有详情页是否都符合无底栏结构。

#### 真机验证记录
- 构建：`./gradlew :app:assembleDebug --console=plain` → **BUILD SUCCESSFUL**
- 设备：`d715a3a4`，真机 ADB 在线
- 安装：`adb -s d715a3a4 install -r app-debug.apk` → Success
- 截图目录：`/Users/sunyiyang/Desktop/Project/master-goods/android-test-screenshots/round11-final-ui-pass`
- 已确认页面：`01-home.png`、`05-agent.png`
- 补充页面截图目录：`/Users/sunyiyang/Desktop/Project/master-goods/android-test-screenshots/round10-auth-gate`
- 已确认页面：`01-home-after-auth-gate.png`、`02-documents.png`、`05-agent.png`
- 二级页截图目录：`/Users/sunyiyang/Desktop/Project/master-goods/android-test-screenshots/round13-subroute-no-bottom`
- 已确认页面：`01-sale-editor-no-bottom.png`、`02-product-editor-no-bottom.png`

#### 本阶段结论
- 首页：已接近设计稿 01 的 KPI、趋势图、提醒卡和底部导航结构；真实业务数值与设计稿样例不同属于数据差异。
- 单据：销售单列表已接入紧凑卡片和筛选结构；多条数据密度仍需后端/演示数据补足后继续核对。
- 档案：商品列表已接近设计稿 03 的缩略图 + 库存 + 状态胶囊结构。
- 报表：已补齐统计图，不再是纯列表/KPI 页面。
- 助手：已接近设计稿 08 的 AI 工作台首页，但问答详情、操作草稿、任务通知仍是后续页面。

### 第十阶段：字体与显示效果优化记录（2026-05-25）

#### 本阶段目标
- 优化全局字体显示、行高、底部导航选中态、卡片毛玻璃质感、按钮边框与状态标签，使页面整体更接近设计稿的轻量高密度风格。

#### 逐文件记录

##### 文件 55：core/designsystem/src/main/java/com/zhihuiji/core/designsystem/ZhihuijiTypography.kt
- 所属模块：core/designsystem
- 本次修改内容：统一使用 `FontFamily.SansSerif`，补充 `LineHeightStyle` 让中文行高居中裁切更稳定；微调 `display/headline/title/body/label` 字号和字重，减少真机上文字过粗、过挤的问题。
- 当前状态：Done
- 下一步：后续可根据品牌需要接入自定义字体资源。

##### 文件 56：core/designsystem/src/main/java/com/zhihuiji/core/designsystem/ZhihuijiColors.kt
- 所属模块：core/designsystem
- 本次修改内容：优化文字灰阶、边框色、卡片透明度和背景渐变，使毛玻璃层次更柔和，降低黑字和蓝底的生硬感。
- 当前状态：Done
- 下一步：继续按截图观察低亮度屏幕下的对比度。

##### 文件 57：core/designsystem/src/main/java/com/zhihuiji/core/designsystem/GlassCard.kt
- 所属模块：core/designsystem
- 本次修改内容：卡片圆角从 `10dp` 调整为 `12dp`，边框从 `0.7dp` 调整为 `0.6dp`，阴影提升到 `1.5dp`，让卡片更接近设计稿的轻阴影毛玻璃效果。
- 当前状态：Done
- 下一步：后续可增加可配置紧凑/突出两种卡片强度。

##### 文件 58：core/designsystem/src/main/java/com/zhihuiji/core/designsystem/PrimaryGradientButton.kt
- 所属模块：core/designsystem
- 本次修改内容：主按钮/次按钮/危险按钮高度统一收敛到 `44dp`，圆角调整为 `10dp`；次按钮和危险按钮改用轻量 `BorderStroke`，移除默认系统重边框显示。
- 当前状态：Done
- 下一步：后续将主按钮改为真正渐变绘制，而不是纯主色填充。

##### 文件 59：core/designsystem/src/main/java/com/zhihuiji/core/designsystem/GlassScaffold.kt
- 所属模块：core/designsystem
- 本次修改内容：底部导航图标固定为 `22dp`，文字统一为小号标签；取消 Material3 默认的大面积选中胶囊，改为设计稿更接近的蓝色图标+文字选中态。
- 当前状态：Done
- 下一步：后续可自绘底栏，进一步压缩高度并加入顶部细分割线。

##### 文件 60：core/designsystem/src/main/java/com/zhihuiji/core/designsystem/StatusPill.kt
- 所属模块：core/designsystem
- 本次修改内容：状态胶囊垂直内边距从 `3dp` 收敛为 `2dp`，让列表状态更轻、更贴近设计稿。
- 当前状态：Done
- 下一步：后续可按业务状态补更精确的色板。

#### 真机验证记录
- 构建：`./gradlew :app:assembleDebug --console=plain` → **BUILD SUCCESSFUL**
- 设备：`d715a3a4`，真机 ADB 在线
- 截图目录：`/Users/sunyiyang/Desktop/Project/master-goods/android-test-screenshots/round14-font-display`
- 已确认页面：`01-home.png`、`02-documents.png`、`03-archives.png`、`04-reports.png`、`05-agent.png`

#### 本阶段结论
- 字体：中文显示更稳定，行高更贴近设计稿，不再明显受系统字体缩放影响。
- 底栏：去掉默认 Material3 选中大胶囊后，视觉更接近设计稿中的简洁底部导航。
- 卡片：圆角、边框、阴影和透明度更柔和，毛玻璃观感更明显。
- 按钮/胶囊：整体边框更轻，和设计稿中的浅蓝边框体系更一致。

## 当前整体结论

- 已完成：全部 28 个模块首版实现 + 五栏底部导航主壳 + 全部核心前台业务闭环，Gradle 构建通过
- 五栏导航组织：
  - 首页 → DashboardScreen（KPI 卡片 + 快捷入口）
  - 单据 → DocumentsScreen（SegmentedTabs: 销售单/采购单/付款单/资金流水）
  - 档案 → ArchivesScreen（SegmentedTabs: 商品/客户/供应商）
  - 报表 → ReportScreen（销售/利润/应收/应付 KPI）
  - 助手 → AgentWorkbenchScreen（AI 对话 + KPI）
  - 设置 → 从首页入口进入，不占底部导航主栏位
- 认证流：登录/注册 → 主流程，退出登录清空栈回登录页
- 已走通的业务链路：
  - 商品：列表 → 新增/编辑 → 库存调整
  - 客户：列表 → 新增/编辑 → 详情
  - 供应商：列表 → 新增/编辑 → 详情
  - 销售单：列表 → 开单 → 详情 → 收款 → 取消/完成
  - 采购单：列表 → 开单（选供应商+添加商品+修改数量+计算应付+提交）→ 详情
  - 付款单：列表 → 新建（选供应商+输入金额+选择方式+提交）→ 详情 → 确认付款/取消
  - 资金流水：列表 → 新增流水（收入/支出+分类+金额+方式+提交）
- 数据层现状：
  - `data/product`、`data/customer`、`data/supplier`、`data/order`、`data/finance` 已切换为“Room 观察 + 网络刷新落库”
  - `data/sync` 已支持手动同步、服务端变更应用、本地游标持久化
- 待完善：
  - Room 与 API 的更完整双向同步策略（当前以在线优先和列表缓存为主）
  - `data/sync` 的后台同步调度、离线回写与冲突处理
  - WorkManager 定时同步
  - SSE 通知推送
  - UI 已按设计稿主风格完成统一重构，仍需真机截图后做像素级微调
  - 单元测试

## 开发执行建议

- 第一阶段只做在线优先，不默认开启离线编辑。
- 第二阶段再补 Room 缓存、手动同步、SSE 通知。
- 后端真实接口应优先以 `docs/android-api-contract.md` 和 Controller 代码为准。
- UI 实现必须先完成 `core/designsystem`，再进入各 feature 页面，避免页面风格漂移。
