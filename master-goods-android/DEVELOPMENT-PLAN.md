# Android 整体开发计划

这个文档负责串联所有模块开发说明，并记录每个模块当前完成状态。

状态约定：
- `Scaffold Ready`：目录和开发说明已建立。
- `In Progress`：已开始编码但未完成联调。
- `Done`：已完成并通过联调。

## UI 统一基线

- Android 视觉真源固定为 `/Users/sunyiyang/Desktop/Project/master-goods/docs/design-mockups/01.png ~ 08.png`。
- 页面母版与组件组合以 [UI-DESIGN-SPEC.md](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/UI-DESIGN-SPEC.md) 为准。
- 可复用实现以 [core/designsystem/DEVELOPMENT.md](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/core/designsystem/DEVELOPMENT.md) 为准。
- 后续新增业务必须复用既有列表页、详情页、编辑页、报表页、AI 页、设置页母版；如需新组件，先沉淀到 `core/designsystem`。
- 当前是“设计基线文档统一”，不是 B10 UI 实装完成；真机截图、逐页核对、细节微调仍属于后续验收。

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
| `feature/dashboard` | 首页经营看板 | `data/order`, `data/finance`, `data/product` | Done |
| `feature/products` | 商品页面 | `data/product` | Done |
| `feature/customers` | 客户页面 | `data/customer` | Done |
| `feature/suppliers` | 供应商页面 | `data/supplier` | Done |
| `feature/sales` | 销售单页面 | `data/order`, `data/product`, `data/customer` | Done |
| `feature/purchases` | 采购单页面 | `data/order`, `data/product`, `data:supplier` | Done |
| `feature/payments` | 付款单页面 | `data/order`, `data:supplier` | Done |
| `feature/finance` | 资金流水页面 | `data:finance` | Done |
| `feature/reports` | 报表页面 | `data/order`, `data/finance`, `data/product`, `data/sync` | Done |
| `feature/agent` | AI 助手页面 | `data/agent` | In Progress |
| `feature/settings` | 设置与同步页面 | `data/auth`, `data/sync`, `core:datastore` | In Progress |

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

### 2026-06-04 B10/B11：dashboard / reports 收口与文档同步

##### 文件 1：feature/dashboard/src/main/java/com/zhihuiji/feature/dashboard/DashboardScreen.kt
- 所属模块：feature/dashboard
- 本次修改内容：继续按 `UI-DESIGN-SPEC.md` 收紧首页经营看板的诚实态文案；把顶部设置入口图标改成与实际跳转一致的设置语义，并把总览说明、搜索提示、底部摘要统一为“当前仅汇总已接入数据”的表述。
- 当前状态：Done
- 下一步：真机继续核对首页首屏信息密度、留白和状态层级；如后端补齐首页聚合接口，再把趋势/通知等占位能力升级为真实联动。

##### 文件 2：feature/reports/src/main/java/com/zhihuiji/feature/reports/ReportScreen.kt
- 所属模块：feature/reports
- 本次修改内容：继续收紧报表页的时间标签语义和诚实态说明，明确区分“销售/应收会随时间标签刷新”与“账户余额/库存成本/占位图仍是当前快照”，避免把静态占位误写成完整动态报表。
- 当前状态：Done
- 下一步：真机继续核对 `07.png` 对应的 KPI、图表比例和空态层级；如后端补齐趋势序列，再替换当前占位图说明。

##### 文件 3：feature/dashboard/DEVELOPMENT.md
- 所属模块：feature/dashboard
- 本次修改内容：将模块说明从计划态函数清单改为源码真实状态，修正依赖、已实现结构、待验证边界与下一步，避免文档继续引用不存在的入口函数。
- 当前状态：Done
- 下一步：无

##### 文件 4：feature/reports/DEVELOPMENT.md
- 所属模块：feature/reports
- 本次修改内容：将模块说明同步为当前源码真实状态，移除已不存在的多 Tab 计划类与旧函数名，补入真实依赖、时间标签边界和下一步联调方向。
- 当前状态：Done
- 下一步：无

##### 文件 5：DEVELOPMENT-PLAN.md
- 所属模块：总体计划
- 本次修改内容：修正 `feature/dashboard` / `feature/reports` 在模块状态总表中的真实依赖，并补记本轮 dashboard / reports 收口与文档同步记录。
- 当前状态：Done
- 下一步：无

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

### 2026-05-26 第七阶段：参考 BiliPai 重构底部切换条

##### 文件 1：core/designsystem/src/main/java/com/zhihuiji/core/designsystem/LiquidSegmentedControl.kt
- 所属模块：core/designsystem（新建）
- 本次修改内容：新增统一的液态切换组件，抽象为通用 `LiquidSegmentedControl`；现已接入阻尼拖拽、速度感知释放、按压高光/折射驱动和底栏项强调度插值。
- 当前状态：In Progress
- 下一步：让主壳层接住二次点击策略，并让页面滚动状态驱动底栏显隐。

##### 文件 2：core/designsystem/src/main/java/com/zhihuiji/core/designsystem/BottomBarBehavior.kt
- 所属模块：core/designsystem（新建）
- 本次修改内容：新增底栏行为通道，提供全局底栏显示控制与“滚动隐藏 / 回顶”辅助效果，给后续主壳和列表页复用。
- 当前状态：In Progress
- 下一步：主壳注入底栏可见性控制，列表页接入滚动隐藏。

##### 文件 3：core/designsystem/src/main/java/com/zhihuiji/core/designsystem/DampedSegmentedDragState.kt
- 所属模块：core/designsystem（新建）
- 本次修改内容：新增分段切换拖拽状态，提供阻尼拖拽、速度感知释放目标、按压脉冲和弹性吸附；并修正状态同步时机，避免在组合阶段直接触发动画协程。
- 当前状态：In Progress
- 下一步：接入 `LiquidSegmentedControl`，替换当前单纯 `animateDpAsState` 指示器位移。

##### 文件 4：core/designsystem/src/main/java/com/zhihuiji/core/designsystem/FloatingLiquidBottomBar.kt
- 所属模块：core/designsystem（新建）
- 本次修改内容：保留底栏适配入口，但其内部不再维护独立 UI 逻辑，改为直接委托给统一的 `LiquidSegmentedControl`，确保底栏与页面切换条是同一种组件体系。
- 当前状态：In Progress
- 下一步：编译验证统一组件方案，并视情况进一步删薄适配层。

##### 文件 5：core/designsystem/src/main/java/com/zhihuiji/core/designsystem/GlassScaffold.kt
- 所属模块：core/designsystem
- 本次修改内容：移除 Material `NavigationBar` / `NavigationBarItem` 组合，改为统一调用 `FloatingLiquidBottomBar`；现已注入全局底栏可见性通道，并加上显示/隐藏过渡动画，给滚动隐藏提供主壳承载。
- 当前状态：In Progress
- 下一步：主壳继续接入二次点击重选策略。

##### 文件 6：core/designsystem/src/main/java/com/zhihuiji/core/designsystem/SegmentedTabs.kt
- 所属模块：core/designsystem
- 本次修改内容：不再单独维护一套标签切换 UI，改为直接委托到统一的 `LiquidSegmentedControl`，让页面内切换条与底栏共享同一套视觉和动画底层。
- 当前状态：In Progress
- 下一步：继续让底栏适配层也改为委托到统一组件。

##### 文件 7：core/designsystem/src/main/java/com/zhihuiji/core/designsystem/LiquidGlassSurface.kt
- 所属模块：core/designsystem
- 本次修改内容：为玻璃容器新增 `lensProgress` 和高光强度控制，准备给底栏选中胶囊接入按压折射与高光增强效果。
- 当前状态：In Progress
- 下一步：让底栏选中指示器在按压/拖拽时驱动这些参数。

##### 文件 31：feature/agent/src/main/java/com/zhihuiji/feature/agent/OperationDraftScreen.kt
- 所属模块：feature/agent
- 本次修改内容：继续按 `08.png` 打磨“操作草稿”页，新增“草稿列表”标题区，把真实草稿与占位草稿都改成更接近业务卡片的字段布局，补上草稿编号、往来方、商品数、金额、创建时间，并让收款草稿主按钮更接近“新建草稿”的设计语义。
- 当前状态：In Progress
- 下一步：继续压近任务与通知页的筛选维度、状态层级和时间信息表现。

##### 文件 32：feature/agent/src/main/java/com/zhihuiji/feature/agent/AgentTaskScreen.kt
- 所属模块：feature/agent
- 本次修改内容：继续按 `08.png` 打磨“任务与通知”页，把任务筛选补成“全部/排队/进行中/已完成/失败”五档；同时增强任务卡的进度文案、任务类型、耗时/等待状态展示，并为通知卡补上送达状态说明，让页面更接近设计稿中的状态中心层级。
- 当前状态：In Progress
- 下一步：继续收紧 AI 问答页的结构化答案层级和推荐问题形态。

##### 文件 34：feature/agent/src/main/java/com/zhihuiji/feature/agent/AgentTaskScreen.kt
- 所属模块：feature/agent
- 本次修改内容：修复 `completedAt` 在跨模块 DTO 上触发的 smart cast 编译问题，改为先落到局部不可变变量后再计算耗时，确保任务页新增的“耗时”展示可稳定编译。
- 当前状态：In Progress
- 下一步：重新执行 `assembleDebug` 验证本轮 agent 页面细化是否全部通过。

##### 文件 35：core/datastore/src/main/java/com/zhihuiji/core/datastore/SessionStore.kt
- 所属模块：core/datastore
- 本次修改内容：修复 APK 打包阶段暴露出的 Kotlin 编译问题，删除重复的 `Flow` 导入，避免 `Conflicting import` 导致 `core:datastore` 编译失败。
- 当前状态：In Progress
- 下一步：重新执行 `./gradlew :app:assembleDebug` 输出可安装 APK。

##### 文件 36：app/build.gradle.kts
- 所属模块：app
- 本次修改内容：开始做 Android 加固，调整构建策略为 `debug` 不混淆、`release` 开启 `minify` 与 `shrinkResources`，让正式构建不再把完整类名、方法名和冗余资源直接暴露给逆向分析。
- 当前状态：In Progress
- 下一步：继续收紧 Manifest、备份策略与网络安全配置。

##### 文件 37：app/src/main/AndroidManifest.xml
- 所属模块：app
- 本次修改内容：关闭 `allowBackup`，接入 `dataExtractionRules` 与 `fullBackupContent`，同时把网络策略切换到显式 `networkSecurityConfig`，收掉当前“全局允许备份 / 全局允许明文流量”的高暴露配置。
- 当前状态：In Progress
- 下一步：补齐 debug/release 两套网络安全 XML 与备份规则文件。

##### 文件 38：app/src/debug/res/xml/network_security_config.xml
- 所属模块：app
- 本次修改内容：新增 debug 专用网络安全配置，默认拒绝明文流量，只对白名单开发地址（`10.0.2.2`、`localhost`、`127.0.0.1`、`117.72.79.106`）允许 HTTP，避免调试版也对任意明文目标放开。
- 当前状态：In Progress
- 下一步：新增 release 版本的严格 HTTPS 配置。

##### 文件 39：app/src/release/res/xml/network_security_config.xml
- 所属模块：app
- 本次修改内容：新增 release 专用网络安全配置，正式版默认仅允许受信任 HTTPS 通信，从配置层切断任意明文抓包与中间人降级空间。
- 当前状态：In Progress
- 下一步：补齐备份与数据提取规则。

##### 文件 40：app/src/main/res/xml/backup_rules.xml
- 所属模块：app
- 本次修改内容：新增传统备份规则，直接排除根路径，避免应用文件通过系统备份通道被整体带出。
- 当前状态：In Progress
- 下一步：补齐 Android 12+ 数据提取规则。

##### 文件 41：app/src/main/res/xml/data_extraction_rules.xml
- 所属模块：app
- 本次修改内容：新增 Android 12+ 数据提取规则，禁止云备份与设备迁移把应用目录整体导出，补齐新系统路径下的数据保护。
- 当前状态：In Progress
- 下一步：收紧网络日志与 release 环境的 HTTPS 约束。

##### 文件 42：core/network/build.gradle.kts
- 所属模块：core/network
- 本次修改内容：为网络模块开启 `BuildConfig` 并按 `debug/release` 注入安全开关，给后续的“仅调试版输出网络日志、仅调试版允许非 HTTPS 基础地址”提供变体级控制。
- 当前状态：In Progress
- 下一步：在 `NetworkModule` 中消费这些开关。

##### 文件 43：core/network/src/main/java/com/zhihuiji/core/network/NetworkModule.kt
- 所属模块：core/network
- 本次修改内容：网络层开始消费安全开关，关闭 release 版的 OkHttp 日志输出，并在基础地址拦截器中强制正式构建只接受 HTTPS 基础地址，收掉请求头泄漏和 HTTP 降级面。
- 当前状态：In Progress
- 下一步：补充 UI 层的截图保护与 R8 混淆规则。

##### 文件 44：app/src/main/java/com/zhihuiji/app/MainActivity.kt
- 所属模块：app
- 本次修改内容：为正式构建启用 `FLAG_SECURE`，减少敏感页面被系统截图、录屏、最近任务缩略图直接带出的风险；调试版保持不受影响，便于开发验收。
- 当前状态：In Progress
- 下一步：补齐 ProGuard / R8 规则，确保 release 混淆可稳定落地。

##### 文件 45：app/proguard-rules.pro
- 所属模块：app
- 本次修改内容：补充 release 混淆规则，重点保留 kotlinx.serialization、Hilt/Dagger、Retrofit 接口与核心模型，确保正式版在开启 R8 后既具备混淆收益，又不因反射/生成代码被裁剪而失稳。
- 当前状态：In Progress
- 下一步：补一份逆向审计与加固记录文档，并执行 debug/release 双构建验证。

##### 文件 46：docs/android-security-hardening-audit.md
- 所属模块：docs
- 本次修改内容：新增“Android 逆向审计与加固记录”，记录本轮从 APK/Manifest/网络/备份面观察到的暴露点，以及已经落地的 build、Manifest、network、FLAG_SECURE 等加固措施，便于后续复查与继续补强。
- 当前状态：In Progress
- 下一步：执行 debug / release 构建验证，确认本轮加固没有破坏工程产物。

##### 文件 33：feature/agent/src/main/java/com/zhihuiji/feature/agent/AgentChatScreen.kt
- 所属模块：feature/agent
- 本次修改内容：继续按 `08.png` 打磨“AI问答”页，把推荐问题从按钮改成更轻的胶囊问题项；同时给助手回复卡补上“经营分析结果”头部标识，并增强 TOP3 商品区标题信息层级，使整页更接近专业分析问答界面的视觉结构。
- 当前状态：In Progress
- 下一步：编译验证并视报错继续做最小修正。

##### 文件 8：app/src/main/java/com/zhihuiji/app/navigation/MainScreen.kt
- 所属模块：app
- 本次修改内容：主壳接入底栏可见性状态和二次点击策略；点击当前 tab 时不再重复导航，而是派发对应的 reselect signal，并主动恢复底栏显示。
- 当前状态：In Progress
- 下一步：把这些 reselect signal 继续传给 `MainNavGraph` 与各主页面。

##### 文件 9：app/src/main/java/com/zhihuiji/app/navigation/MainNavGraph.kt
- 所属模块：app
- 本次修改内容：主导航图新增 reselect signal 透传，把首页/单据/档案/报表/助手的重选事件继续分发到对应页面，实现“主壳认知、页面响应”的结构。
- 当前状态：In Progress
- 下一步：各页面消费 signal，执行回顶或重置子 tab。

##### 文件 10：app/src/main/java/com/zhihuiji/app/navigation/DocumentsScreen.kt
- 所属模块：app
- 本次修改内容：单据容器接入 reselect signal；重选“单据”时优先回到销售单子页，并向当前列表派发回顶信号。
- 当前状态：In Progress
- 下一步：档案容器和首页/报表/助手页面继续接入同类策略。

##### 文件 11：app/src/main/java/com/zhihuiji/app/navigation/ArchivesScreen.kt
- 所属模块：app
- 本次修改内容：档案容器接入 reselect signal；重选“档案”时优先回到商品子页，并向对应列表派发回顶信号。
- 当前状态：In Progress
- 下一步：首页/报表/助手页面继续接入同类策略。

##### 文件 12：feature/dashboard/src/main/java/com/zhihuiji/feature/dashboard/DashboardScreen.kt
- 所属模块：feature/dashboard
- 本次修改内容：首页接入底栏滚动隐藏和重选回顶效果，滚动经营概览时可驱动底栏显隐，重选“首页”时回到顶部。
- 当前状态：In Progress
- 下一步：报表和助手页接入同样的行为。

##### 文件 13：feature/reports/src/main/java/com/zhihuiji/feature/reports/ReportScreen.kt
- 所属模块：feature/reports
- 本次修改内容：报表页接入底栏滚动隐藏和重选回顶效果，保证图表长页在向下浏览时可隐藏底栏，重选“报表”时快速回顶。
- 当前状态：In Progress
- 下一步：助手页接入同样的行为。

##### 文件 14：feature/agent/src/main/java/com/zhihuiji/feature/agent/AgentWorkbenchScreen.kt
- 所属模块：feature/agent
- 本次修改内容：AI 助手页接入底栏滚动隐藏和重选回顶效果，长内容对话/卡片页向下浏览时可收起底栏，重选“助手”时回顶。
- 当前状态：In Progress
- 下一步：单据与档案列表页接入相同行为。

##### 文件 15：feature/sales/src/main/java/com/zhihuiji/feature/sales/SaleOrderListScreen.kt
- 所属模块：feature/sales
- 本次修改内容：销售单列表接入 `LazyListState`，实现底栏滚动隐藏和重选“单据”后的回顶行为。
- 当前状态：In Progress
- 下一步：继续把采购/付款/资金流水与档案列表页接齐。

##### 文件 16：feature/purchases/src/main/java/com/zhihuiji/feature/purchases/PurchaseOrderListScreen.kt
- 所属模块：feature/purchases
- 本次修改内容：采购单列表接入 `LazyListState`，实现底栏滚动隐藏和重选“单据”后的回顶行为。
- 当前状态：In Progress
- 下一步：继续付款单与资金流水列表。

##### 文件 17：feature/payments/src/main/java/com/zhihuiji/feature/payments/PayOrderListScreen.kt
- 所属模块：feature/payments
- 本次修改内容：付款单列表接入 `LazyListState`，实现底栏滚动隐藏和重选“单据”后的回顶行为。
- 当前状态：In Progress
- 下一步：继续资金流水列表与档案列表。

##### 文件 18：feature/finance/src/main/java/com/zhihuiji/feature/finance/FinanceRecordListScreen.kt
- 所属模块：feature/finance
- 本次修改内容：资金流水列表接入 `LazyListState`，实现底栏滚动隐藏和重选“单据”后的回顶行为。
- 当前状态：In Progress
- 下一步：继续档案侧商品/客户/供应商列表。

##### 文件 19：feature/products/src/main/java/com/zhihuiji/feature/products/ProductListScreen.kt
- 所属模块：feature/products
- 本次修改内容：商品列表接入 `LazyListState`，实现底栏滚动隐藏和重选“档案”后的回顶行为。
- 当前状态：In Progress
- 下一步：继续客户和供应商列表。

##### 文件 20：feature/customers/src/main/java/com/zhihuiji/feature/customers/CustomerListScreen.kt
- 所属模块：feature/customers
- 本次修改内容：客户列表接入 `LazyListState`，实现底栏滚动隐藏和重选“档案”后的回顶行为。
- 当前状态：In Progress
- 下一步：继续供应商列表。

##### 文件 21：feature/suppliers/src/main/java/com/zhihuiji/feature/suppliers/SupplierListScreen.kt
- 所属模块：feature/suppliers
- 本次修改内容：供应商列表接入 `LazyListState`，实现底栏滚动隐藏和重选“档案”后的回顶行为。
- 当前状态：In Progress
- 下一步：整体编译验证，并检查顶层入口页面与适配层是否有签名未同步。

##### 文件 22：feature/agent/src/main/java/com/zhihuiji/feature/agent/AgentViewModel.kt
- 所属模块：feature/agent
- 本次修改内容：将 agent 状态中心从单页工作台扩展为多页共享状态，新增聊天消息、操作草稿、提交结果、任务详情、通知更新等状态与加载方法，为 AI 问答 / 操作草稿 / 任务通知页面拆分做准备。
- 当前状态：In Progress
- 下一步：新增独立的 `AgentChatScreen`、`OperationDraftScreen`、`AgentTaskScreen`、`NotificationScreen` 并接入这些状态。

##### 文件 23：feature/agent/src/main/java/com/zhihuiji/feature/agent/AgentChatScreen.kt
- 所属模块：feature/agent（新建）
- 本次修改内容：新增独立 AI 问答页，拆出聊天气泡、助手结构化回答卡片、建议问题区和底部输入栏，视觉结构开始向设计稿 `08.png` 的第二屏靠拢。
- 当前状态：In Progress
- 下一步：把工作台页入口和导航接到该页面，并继续新增操作草稿页。

##### 文件 24：feature/agent/src/main/java/com/zhihuiji/feature/agent/OperationDraftScreen.kt
- 所属模块：feature/agent（新建）
- 本次修改内容：新增操作草稿页，包含分类 Tab、仅看我创建、指令生成草稿、草稿卡片、警告与提交结果区，初步贴近设计稿 `08.png` 的第三屏结构。
- 当前状态：In Progress
- 下一步：补任务与通知页，并把工作台快捷入口接到问答 / 草稿 / 任务通知页面。

##### 文件 25：feature/agent/src/main/java/com/zhihuiji/feature/agent/AgentTaskScreen.kt
- 所属模块：feature/agent（新建）
- 本次修改内容：新增任务与通知中心页，并补 `NotificationScreen` 包装入口；实现“任务 / 通知”双 Tab、状态筛选、任务进度卡、通知已读操作，并继续增强状态 Chip、图标分层、进度描述与通知语义色，进一步贴近设计稿 `08.png` 的第四屏结构；同时补齐 `background` 导入，修复本轮视觉增强引入的编译错误。
- 当前状态：In Progress
- 下一步：修改工作台页与主导航，把问答 / 草稿 / 任务通知真正接入助手入口流转。

##### 文件 26：feature/agent/src/main/java/com/zhihuiji/feature/agent/AgentWorkbenchScreen.kt
- 所属模块：feature/agent
- 本次修改内容：将助手首页重构为更接近设计稿 `08.png` 的工作台首屏，补齐右上角入口、四宫格 KPI、经营洞察、快捷操作和推荐问题，并增加跳转到问答 / 草稿 / 任务通知的回调入口。
- 当前状态：In Progress
- 下一步：在主导航中新增 agent 子路由，把这些回调真正接入页面流转。

##### 文件 27：app/src/main/java/com/zhihuiji/app/navigation/MainNavGraph.kt
- 所属模块：app
- 本次修改内容：新增 agent 子路由，接入 `AgentChatScreen`、`OperationDraftScreen`、`AgentTaskScreen`、`NotificationScreen`，并将工作台页的问答 / 草稿 / 任务通知入口真正挂到导航流转上；同时对聊天初始问题参数增加 `Uri.encode`，避免中文问句造成路由解析问题。
- 当前状态：In Progress
- 下一步：修正路由参数细节后执行编译验证，确保四页都能正常进入。

##### 文件 28：feature/agent/src/main/java/com/zhihuiji/feature/agent/AgentChatScreen.kt
- 所属模块：feature/agent
- 本次修改内容：为 `FlowRow` 增加 `ExperimentalLayoutApi` 显式 `OptIn`，修复 AI 问答页因实验布局 API 导致的 Kotlin 编译失败；同时将发送图标切换到 `Icons.AutoMirrored.Filled.Send`，清理 Compose deprecation 警告，并继续补强“销售概览 / 指标卡 / 趋势图 / Top3 商品”结构，让问答页更贴近 `08.png` 第二屏的分析回答样式。
- 当前状态：In Progress
- 下一步：继续修复工作台页与草稿页相同的实验布局 API 编译问题。

##### 文件 29：feature/agent/src/main/java/com/zhihuiji/feature/agent/AgentWorkbenchScreen.kt
- 所属模块：feature/agent
- 本次修改内容：为工作台页增加 `ExperimentalLayoutApi` 显式 `OptIn`，修复四宫格 `FlowRow` 造成的 Kotlin 编译失败；同时将 `ReceiptLong` 切换为 `AutoMirrored` 版本，清理 Compose deprecation 警告，并继续强化顶部入口、快捷操作、推荐问题与 KPI 卡的视觉层级，使首页更贴近 `08.png` 第一屏。
- 当前状态：In Progress
- 下一步：继续修复操作草稿页的实验布局 API 编译问题，并重新执行构建验证。

##### 文件 30：feature/agent/src/main/java/com/zhihuiji/feature/agent/OperationDraftScreen.kt
- 所属模块：feature/agent
- 本次修改内容：为操作草稿页及其私有 `DraftCard` 增加 `ExperimentalLayoutApi` 显式 `OptIn`，修复警告标签区 `FlowRow` 造成的 Kotlin 编译失败，并继续增强草稿卡片的类型图标、编号、副标题与操作层级，让页面更贴近 `08.png` 第三屏；同时补齐 `background / Box / size / RoundedCornerShape` 等 Compose 导入，修复本轮视觉增强引入的编译错误。
- 当前状态：In Progress
- 下一步：重新执行构建验证，并继续修复 agent 路由或 UI 细节问题。

##### 文件 31：feature/agent/DEVELOPMENT.md
- 所属模块：feature/agent
- 本次修改内容：将模块说明从“脚手架已创建，页面未开始”更新为真实状态，明确 AI 工作台 / 问答 / 草稿 / 任务通知首版已完成，同时补充当前剩余差距与下一步完善方向。
- 当前状态：In Progress
- 下一步：继续通过真机截图核对 `08.png`，逐项微调视觉和交互细节。

##### 验证记录
- 执行命令：`./gradlew :app:assembleDebug`
- 结果：`BUILD SUCCESSFUL`
- 当前结论：自定义浮动底栏已接入主壳，项目可正常编译，下一步适合上真机看选中胶囊宽度、上下留白和图标字重是否还需要继续贴近参考项目。

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
- 以 `/Users/sunyiyang/Desktop/Project/master-goods/docs/design-mockups/01.png`、`03.png`、`06.png`、`08.png` 为主参考，逐页核对首页、单据、档案、报表、AI 工作台。
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

### 第十一阶段：逆向审计与加固记录（2026-05-31）

#### 本阶段目标
- 参考逆向分析常见暴露面，针对当前 Android 包的可读性、网络明文、日志泄露、备份泄露和界面截屏风险做第一轮工程级加固，并保持可构建状态。

#### 逐文件记录

##### 文件 61：app/build.gradle.kts
- 所属模块：app
- 本次修改内容：在 `buildFeatures` 中显式开启 `buildConfig = true`，确保 `MainActivity` 中基于 `BuildConfig.DEBUG` 的发布版截屏保护逻辑可以稳定编译。
- 当前状态：Done
- 下一步：继续执行 `assembleDebug` 和 `assembleRelease`，确认本阶段所有加固改动都通过构建验证。

##### 文件 62：core/datastore/src/main/java/com/zhihuiji/core/datastore/SecureSessionCipher.kt
- 所属模块：core/datastore
- 本次修改内容：新增基于 Android Keystore 的 AES/GCM 会话加密器，用于把 access token 和 refresh token 在落盘前加密，并兼容已存储密文的透明解密。
- 当前状态：Done
- 下一步：把 `SessionStore` 接到该加密器，并对历史明文 token 做自动迁移。

##### 文件 63：core/datastore/src/main/java/com/zhihuiji/core/datastore/SessionStore.kt
- 所属模块：core/datastore
- 本次修改内容：会话读写改为“落盘前加密、读取时解密”，并在初始化监听中自动把历史明文 token 迁移成 Keystore 密文。
- 当前状态：Done
- 下一步：补一层运行时高风险环境检测，降低调试注入与 Frida 直接附加的暴露面。

##### 文件 64：app/src/main/java/com/zhihuiji/app/security/RuntimeSecurityGuard.kt
- 所属模块：app
- 本次修改内容：新增发布版运行时高风险检测，覆盖调试器附加、Frida 默认端口探测与进程 maps 中的 Frida/Gum 痕迹，同时预留 root 检测能力用于后续分级策略。
- 当前状态：Done
- 下一步：把高风险运行时检测接入 `MainActivity`，仅在非 debug 构建下拦截明显的注入/调试场景。

##### 文件 65：app/src/main/java/com/zhihuiji/app/MainActivity.kt
- 所属模块：app
- 本次修改内容：在发布版启动路径中接入 `RuntimeSecurityGuard.isHighRiskRuntime()`，与 `FLAG_SECURE` 组合生效；检测到调试器/Frida 高风险环境时直接终止界面初始化。
- 当前状态：Done
- 下一步：更新逆向加固审计文档，并重新执行 debug/release 构建验证。

##### 文件 66：docs/android-security-hardening-audit.md
- 所属模块：docs
- 本次修改内容：把第二轮逆向加固结果补入审计文档，新增 Keystore 会话加密、历史明文迁移、Frida/Debugger 运行时拦截与后续证书绑定/完整性建议。
- 当前状态：Done
- 下一步：重新执行 `assembleDebug` 与 `assembleRelease`，确认第二轮加固后的完整工程仍可交付。

##### 文件 67：core/datastore/build.gradle.kts
- 所属模块：core/datastore
- 本次修改内容：为 `core:datastore` 开启 `BuildConfig` 生成，并新增 `BASE_URL_EDITABLE` 构建开关，使 debug 可切环境、release 收紧为不可编辑。
- 当前状态：Done
- 下一步：把 `SettingsStore` 和设置页接到该构建开关，封住 release 任意改服务器地址的入口。

##### 文件 68：core/datastore/src/main/java/com/zhihuiji/core/datastore/SettingsStore.kt
- 所属模块：core/datastore
- 本次修改内容：新增 release 主机白名单与 `isTrustedReleaseBaseUrl()` 校验；保存和读取基础地址时都会按构建类型做净化，release 下只接受受控 HTTPS 主机，其他地址一律回退到默认正式地址。
- 当前状态：Done
- 下一步：把网络拦截器和刷新 token 链路一并切到同一套 release 白名单规则，避免旁路请求绕过。

##### 文件 69：core/network/src/main/java/com/zhihuiji/core/network/NetworkModule.kt
- 所属模块：core/network
- 本次修改内容：基础地址拦截器新增 release 主机白名单校验；正式构建除了必须 HTTPS 之外，还必须命中受控生产主机，否则直接拒绝请求。
- 当前状态：Done
- 下一步：同步修补 `TokenAuthenticator` 的刷新 token 链路，避免它绕过主拦截器单独访问未受控主机。

##### 文件 70：core/network/src/main/java/com/zhihuiji/core/network/TokenAuthenticator.kt
- 所属模块：core/network
- 本次修改内容：刷新 token 前增加 release 主机白名单校验，防止认证器单独 new 出的 `OkHttpClient` 绕过基础地址拦截器，向未受控主机发起刷新请求。
- 当前状态：Done
- 下一步：把设置页改成“debug 可编辑 / release 只读展示”，从 UI 层彻底收掉生产环境可改服务器地址入口。

##### 文件 71：feature/settings/src/main/java/com/zhihuiji/feature/settings/SettingsViewModel.kt
- 所属模块：feature/settings
- 本次修改内容：把“服务器地址是否可编辑”纳入 `SettingsUiState`，初始化时读取构建级开关；同时在保存逻辑中增加 release 拦截，避免 UI 之外的普通调用误改正式环境地址。
- 当前状态：Done
- 下一步：更新设置页显示逻辑，release 下改成只读展示和安全说明。

##### 文件 72：feature/settings/src/main/java/com/zhihuiji/feature/settings/SettingsScreen.kt
- 所属模块：feature/settings
- 本次修改内容：设置页改为“debug 可编辑 / release 只读展示”；正式版仅显示当前受控服务器地址和安全说明，不再暴露手工输入与保存按钮。
- 当前状态：Done
- 下一步：把 root 检测并入高风险运行时判定，并重新执行 debug/release 构建验证。

##### 文件 73：app/src/main/java/com/zhihuiji/app/security/RuntimeSecurityGuard.kt
- 所属模块：app
- 本次修改内容：将 `isRooted()` 正式并入 `isHighRiskRuntime()`，让发布版不仅拦调试器/Frida，也拦截已知 root 高风险运行环境。
- 当前状态：Done
- 下一步：更新逆向加固审计文档，然后重新跑完整构建验证。

##### 文件 74：docs/android-security-hardening-audit.md
- 所属模块：docs
- 本次修改内容：补记第三轮逆向加固结果，新增 release 服务器地址入口收口、正式主机白名单、刷新 token 旁路收紧与 root 纳入阻断的说明。
- 当前状态：Done
- 下一步：执行 `assembleDebug` 与 `assembleRelease`，确认第三轮加固没有破坏联调和发版链路。

##### 文件 75：core/datastore/src/main/java/com/zhihuiji/core/datastore/SettingsStore.kt
- 所属模块：core/datastore
- 本次修改内容：将 release 主机白名单校验从 `okhttp` URL 扩展改为 `java.net.URI` 标准库解析，避免为 `core:datastore` 引入额外网络依赖并修复构建错误。
- 当前状态：Done
- 下一步：重新执行 debug/release 构建，确认第三轮加固链路全部恢复通过。

##### 文件 76：app/build.gradle.kts
- 所属模块：app
- 本次修改内容：新增构建期 `APP_SIGNING_SHA256` 常量；debug 使用本机 debug keystore 摘要，release 允许通过 `ZHIHUIJI_RELEASE_SIGNING_SHA256` 注入正式签名摘要，用于运行时签名完整性校验。
- 当前状态：Done
- 下一步：新增签名校验器并接入主入口，让发布版能识别重打包或非预期签名。

##### 文件 77：app/src/main/java/com/zhihuiji/app/security/SignatureIntegrityChecker.kt
- 所属模块：app
- 本次修改内容：新增 APK 签名完整性校验器，兼容 Android P 及以下签名 API，按运行时安装包签名计算 SHA-256 并与构建期白名单摘要比对。
- 当前状态：Done
- 下一步：把签名校验器接到发布版启动链路，与现有高风险运行时拦截组合生效。

##### 文件 78：app/src/main/java/com/zhihuiji/app/MainActivity.kt
- 所属模块：app
- 本次修改内容：在发布版启动链路中新增签名完整性校验，先校验 APK 签名是否命中构建期白名单，再继续执行 root/Frida/debugger 风险拦截。
- 当前状态：Done
- 下一步：为证书绑定补可安全启用的构建入口，并重新执行 debug/release 构建验证。

##### 文件 79：core/network/build.gradle.kts
- 所属模块：core/network
- 本次修改内容：新增证书绑定相关构建常量；通过 `ZHIHUIJI_PINNED_HOST` 与 `ZHIHUIJI_CERT_PINS` 注入正式域名和公钥 pin，debug 默认关闭，release 仅在真实 pin 提供时启用。
- 当前状态：Done
- 下一步：在 `NetworkModule` 中接入 `CertificatePinner`，让证书绑定在提供真实 pin 后可直接生效。

##### 文件 80：core/network/src/main/java/com/zhihuiji/core/network/NetworkModule.kt
- 所属模块：core/network
- 本次修改内容：新增 `CertificatePinner` 接入点；当 release 构建注入真实 pin 时自动启用公钥绑定，未提供 pin 时保持关闭，避免在证书链不明的环境里误锁发布包。
- 当前状态：Done
- 下一步：更新逆向加固审计文档，并重新执行 debug/release 构建验证。

##### 文件 81：docs/android-security-hardening-audit.md
- 所属模块：docs
- 本次修改内容：补记第四轮逆向加固结果，新增 APK 签名完整性校验、证书绑定的构建入口，以及当前由于正式证书链不可用而未直接硬启 pinning 的说明。
- 当前状态：Done
- 下一步：执行 debug/release 构建验证，确认签名校验和 pinning 入口没有破坏现有发布链路。

##### 文件 82：docs/android-security-hardening-audit.md
- 所属模块：docs
- 本次修改内容：新增“本轮加固总览”段落，把当前已经落地的服务器地址收口、Keystore 会话加密、运行时拦截、签名完整性校验、主机白名单与证书绑定入口集中整理到一个汇总视图中。
- 当前状态：Done
- 下一步：后续若继续做数据库加密或真实证书 pin 注入，可继续在该文档中追加统一汇总。

##### 文件 83：tools/migrate_kingdee_zhihuiji.py
- 所属模块：tools
- 本次修改内容：新增旧版“智慧记进销存”到当前 Android Room 库的迁移脚本；支持读取 `9ffd...db` 主业务库、重建 `zhihuiji.db` 目标结构、迁移商品/客户/供应商/销售单/销售明细/采购单/付款单/资金流水，并可选直接通过 rooted ADB 推送到 `com.zhihuiji.app` 沙箱。
- 当前状态：Done
- 下一步：补充迁移说明文档，随后用真机实际生成并部署迁移后的数据库，验证导入结果。

##### 文件 84：docs/android-kingdee-data-migration.md
- 所属模块：docs
- 本次修改内容：新增旧版“智慧记进销存”本地数据迁移说明，整理来源 APK、来源主业务库、当前支持的表映射、命令行用法、rooted ADB 部署逻辑、金额/状态映射规则与已知风险。
- 当前状态：Done
- 下一步：执行迁移脚本，核对生成库的表结构与数据量，再把结果实际推送到真机并验证。

##### 文件 85：tools/migrate_kingdee_zhihuiji.py
- 所属模块：tools
- 本次修改内容：修复设备部署分支；优先走 `run-as com.zhihuiji.app` 将迁移库复制到 app 自己的 `databases/zhihuiji.db`，只有在 `run-as` 不可用时才回退到 rooted `su + dd` 路径，避免之前直接 root 复制命中 SELinux/目标文件名限制。
- 当前状态：Done
- 下一步：重新执行 `--deploy` 自验证，并在真机上读取目标库或启动 app，确认迁移结果已经实际生效。

##### 文件 86：docs/android-kingdee-data-migration.md
- 所属模块：docs
- 本次修改内容：补记 2026-06-01 真机实测结果，记录 serial `50f87ee9`、各目标表实际迁移数量、Room identity hash 校验、设备回读 `SHA-256` 一致性，以及 `MainActivity` 无 Room/SQLite/FATAL 异常的启动验证结果。
- 当前状态：Done
- 下一步：如需把旧数据长期纳入正式业务链路，下一阶段应考虑做“本地导入后上送后端”或专门的导入 UI，而不是只停留在离线本地库替换。

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
  - UI 已具备设计稿主风格基础，但 B10 前仍需按 `docs/design-mockups/01.png ~ 08.png` 与 `UI-DESIGN-SPEC.md` 逐页核对；新增业务必须复用统一页面母版和 `core/designsystem`
  - 单元测试

## 开发执行建议

- 第一阶段只做在线优先，不默认开启离线编辑。
- 第二阶段再补 Room 缓存、手动同步、SSE 通知。
- 后端真实接口应优先以 `docs/android-api-contract.md` 和 Controller 代码为准。
- UI 实现必须先完成 `core/designsystem`，再进入各 feature 页面，避免页面风格漂移。
