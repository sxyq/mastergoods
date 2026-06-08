# Android Liquid Glass UI 重构计划与验收记录

> 状态：核心母版已接入，Stitch 32 屏仍需逐屏补齐与设备截图复核
> 设计真源更新时间：2026-06-06
> 适用范围：`master-goods-android` 全部本地 UI
> 目标：基于 Stitch 新设计与 `android-liquid-glass` 技能，把当前 Android 端统一收口到“浅蓝极光 + 设备安全玻璃层 + 高密度经营工具”视觉体系

## 0. 当前实施状态

本轮已经完成 Android 端核心 UI 母版收口，但不能判定为 32 张 Stitch 稿一对一全部完成：

- 视觉真源已从旧 `docs/design-mockups/01.png ~ 08.png` 切换为 Stitch 导出目录与本文件。
- `core/designsystem` 已沉淀新的液态玻璃组件体系：`LiquidGlassSurface`、`LiquidGlassCard`、`GlassScaffold`、`GlassTopBar`、`GlassTextField`、`PrimaryButton`、`SecondaryOutlineButton`、`DangerOutlineButton`、`FloatingGlassActionButton`、`BusinessListItem`、`BottomActionBar`、`KpiCard`、`ChartCard`、`SearchFilterBar`、`SegmentedTabs`、`FilterChipRow`、`StatusPill`。
- 主壳、认证、首页、单据、档案、报表、设置、商品、客户、供应商、销售、采购、付款、财务、AI 工作台、AI 对话、草稿、任务通知页面均已接入新的玻璃视觉组件。
- 业务页裸 `OutlinedTextField` 已收敛到 `GlassTextField`；当前仅 `GlassTextField` 自身和认证页内部封装 `AuthOutlinedField` 直接调用 Material `OutlinedTextField`。
- 列表页新增入口已接入已有创建路由：商品、客户、供应商、销售单、采购单。
- AI 图表块已从静态占位改为基于返回数据的轻量摘要条；后续如接入正式图表库，只能替换渲染器，不得伪造结果数据。
- AI 任务/通知已经要求走真实后端接口 `/v2/agent/tasks`、`/v2/agent/notifications` 与 `/v2/agent/notifications/{id}/read`；没有数据时只能显示真实空态或错误态。
- Stitch 扩展业务稿仍有 UI 缺口：采购退货、供应商对账、供应商往来详情当前不是独立 Android route/screen；其中部分只有 backend 或 Android data/network 合同层。
- `LiquidGlassSurface` 当前采用设备安全的玻璃视觉降级层；真实 backdrop renderer 曾在已验证 Android 16 设备首帧测量时崩溃，因此后续恢复真实 blur/lens 必须先完成设备回归。

## 1. 本计划的唯一视觉真源

### 1.1 Stitch 项目

- Project: `Visual Design System Framework`
- Project ID: `14840154594131085259`
- 本地导出根目录：
  - `/Users/sunyiyang/Desktop/Project/master-goods/stitch_exports/visual-design_system_framework_14840154594131085259`
- 屏幕清单：
  - `/Users/sunyiyang/Desktop/Project/master-goods/stitch_exports/visual-design_system_framework_14840154594131085259/manifest.tsv`

### 1.2 当前必须优先对照的 Stitch 设计稿

- 壳层与首页
  - `11484f8c3688487085ddd485eaca5daa` 首页 - 经营总览 (底部优化版)
  - `a194c0a633854700b6603964da79caca` 首页 - 经营总览 (亮色极光玻璃版)
  - `851bba4950bc4f1385ade2cce0668d2f` 单据中心 (底部优化版)
  - `faf71221e71e4b43a37192508eecfe0d` 档案管理 - 商品列表展示 (极光玻璃版)
- 业务页面
  - `71bd8f9c60234565aadd7fd505d9bc16` 商品编辑 (极光玻璃版)
  - `13620e8ea5ec47a08a93ee4ec4c6c331` 库存调整 (极光玻璃版)
  - `670a28679b57420d8c75e670f964b58c` 供应商档案 (极光玻璃版)
  - `d750377ad8d04dbfb8e502c0092798fc` 客户档案 (极光玻璃版)
  - `5aafbb8f938e42fe9021390d45c30d42` 销售单详情 (极光玻璃版)
  - `624955bd91ed45fa9d4093c2fe7259fa` 付款单详情 (极光玻璃版)
  - `018b7e292a0c488fb689a5d279dafb6b` 经营报表 - 多维数据可视化版
- AI 页面
  - `790e9c9b67f74e29a312d5f9f333873c` AI 智能助手 - 思考查询中 (极光玻璃版)
  - `4664bc10c2db4ff7beecb0cb710f5c51` AI 智能助手 - 深度思考与优化布局版
  - `bb4cfaeb86aa4862ba26a7eca264b4e6` AI 智能助手 (亮色极光玻璃版)
- 财务与对账扩展页面
  - `b70cadecc87e49d583075e0b7a71b38b` 资金流水详情 (亮色玻璃版)
  - `6565096a23a94309b0d1e37126ed35b1` 资金流水详情 (极光玻璃版)
  - `20cc45c3ab5940ff81061dc21851f0c0` 供应商对账 (亮色玻璃版)
  - `151d6ada46844fc385e54f5f4e597104` 供应商对账 (极光玻璃版)
  - `124304d91cd44e088d3227e90f35d1ae` 商品库存流水 (亮色玻璃版)
  - `d97470c943fa4c79980bd71c9e412727` 商品库存流水 (极光玻璃版)

### 1.3 旧 8 张设计图的处理原则

- `docs/design-mockups/` 仅保留为历史参考。
- 后续 UI 实施、验收、截图对照、组件抽象、颜色/间距/圆角判断，一律以 Stitch 导出和本计划为准。
- 任何仍写着“01.png ~ 08.png 是统一视觉真源”的文档，都按历史遗留理解，不再作为新开发依据。

## 2. 与 android-liquid-glass 技能的对齐结论

### 2.1 已满足的前置条件

- `master-goods-android` 所有模块 `minSdk = 26`，满足 liquid glass 技能要求。
- `master-goods-android/settings.gradle.kts` 已包含 `:backdrop`。
- `master-goods-android/core/designsystem/build.gradle.kts` 已 `implementation(project(":backdrop"))`。

### 2.2 本轮实施前的差距与当前收口

- `core/designsystem/LiquidGlassSurface.kt`
  - 改造前：只是 `Color.White.copy(alpha = 0.35f)` 的半透明背景模拟。
  - 当前：已沉淀为 `LiquidGlassSurface` / `LiquidGlassCard` 统一玻璃容器；真实 backdrop renderer 暂按设备安全策略降级。
- `core/designsystem/GlassScaffold.kt`
  - 改造前：只提供静态渐变背景。
  - 当前：已承载统一极光背景、顶栏、底部操作栏、主壳底栏和内容层分区。
- `core/designsystem/ZhihuijiColors.kt`
  - 改造前：仍是旧蓝色体系 `#1677FF`。
  - 当前：已切换到 Stitch 浅蓝极光、主品牌蓝、语义色与玻璃层 token。
- `core/designsystem/ZhihuijiTypography.kt`
  - 改造前：字号层级可用，但缺少 Stitch 规定的 `Plus Jakarta Sans + Inter` 组合。
  - 当前：已补齐标题、正文、数据数字等稳定 token。
- `app/navigation/MainScreen.kt`
  - 改造前：底栏仍是 Material `NavigationBar`。
  - 当前：已替换为浮动玻璃底栏，选中态为浅蓝高亮胶囊。

### 2.3 结论

- 本项目不需要先解决 SDK 或依赖问题。
- 本轮已经把“玻璃风格模拟版”收口为 `core/designsystem` 的统一玻璃组件能力，并由已存在的 feature 页面复用。

## 3. 视觉系统精确规范

## 3.1 全局背景

- 页面主背景：
  - 顶部冷白蓝：`#E1EFFF`
  - 底部近白：`#FFFFFF`
  - 实际基础背景色：`#F7F9FE`
- 背景层必须保持以下感受：
  - 上方更亮，形成空气感
  - 下方更白，避免厚重感
  - 不允许回退到纯灰白 `#F5F7FA` 的平面背景

## 3.2 颜色令牌

### 核心品牌色

- 主品牌蓝：`#005BBF`
- 亮主色：`#1A73E8`
- 主色浅底：`#D8E2FF`
- 状态浅蓝底：`#E3F2FD`

### 文字

- 一级正文：`#181C20`
- 二级正文：`#414754`
- 数据一级文字：`#1F2937`
- 数据二级文字：`#6B7280`

### 语义色

- 成功：`#34A853`
- 警告：`#FB8C00`
- 错误强调：`#EA4335`
- 错误主色：`#BA1A1A`

### 玻璃层与边框

- 玻璃卡底色：白色，透明度 `0.70 ~ 0.85`
- 玻璃高亮边框：白色，透明度 `0.50`
- 常规描边：`#C1C6D6`
- 次级容器底：`#F1F4F9`
- 列表分隔弱底：`#ECEEF3`

## 3.3 排版令牌

- 标题字体：`Plus Jakarta Sans`
- 正文字体：`Inter`

### 必须落地的字号层级

- `headline-lg`
  - 24sp / 32sp / 700
- `headline-md`
  - 20sp / 28sp / 600
- `body-lg`
  - 16sp / 24sp / 500
- `body-md`
  - 14sp / 20sp / 400
- `label-sm`
  - 12sp / 16sp / 600 / `0.5sp` tracking
- `data-numeric-lg`
  - 22sp / 28sp / 700
- `data-numeric-sm`
  - 13sp / 18sp / 600

### 字体使用规则

- 顶部页面标题、大金额、KPI 数字、模块标题：优先 `Plus Jakarta Sans`
- 表单正文、列表二级信息、状态描述、输入内容：优先 `Inter`
- 金额超过 7 位时，从 `24sp` 自动降到 `20sp`

## 3.4 圆角、间距、阴影、模糊

- 页面外边距：16dp
- 区块垂直间距：12dp
- 行内元素间距：8dp
- 卡片内边距：16dp
- 大区块内边距：20dp

- 输入框 / 按钮圆角：12dp
- 主卡片圆角：16dp
- 小胶囊 / 筛选块圆角：8dp
- 标签 / chip：全圆角

- 标准玻璃模糊：20dp
- 轻玻璃模糊：16dp
- 强玻璃模糊：24dp

- 弹层阴影：
  - `0 8dp 24dp rgba(0, 0, 0, 0.08)`
- 卡片不使用厚重黑阴影
- 卡片边缘要有顶侧高光与轻微内阴影，形成玻璃厚度

## 4. 本地 UI 覆盖清单

## 4.1 壳层与导航 UI

| 本地文件 | 角色 | 目标 |
|---|---|---|
| `app/navigation/AppNavGraph.kt` | 登录态 / 主壳路由切换 | 改成统一极光启动底色与更轻的过渡态 |
| `app/navigation/MainScreen.kt` | 五栏底部导航壳 | 升级为浮动玻璃底栏 |
| `app/navigation/DocumentsScreen.kt` | 单据中心壳层 | 对齐 `单据中心 (底部优化版)` |
| `app/navigation/ArchivesScreen.kt` | 档案中心壳层 | 对齐 `档案管理 - 商品列表展示` 的壳层语义 |
| `app/navigation/MainNavGraph.kt` | 全部 detail route 宿主 | 保持路由不变，页面壳层统一重构 |

## 4.2 Feature 级主 Screen 文件

| 模块 | 本地文件 |
|---|---|
| auth | `LoginScreen.kt` `RegisterScreen.kt` |
| dashboard | `DashboardScreen.kt` |
| documents/sales | `SaleOrderListScreen.kt` `SaleOrderDetailScreen.kt` `SaleOrderEditScreen.kt` `PaymentScreen.kt` |
| purchases | `PurchaseOrderListScreen.kt` `PurchaseOrderDetailScreen.kt` `PurchaseOrderEditScreen.kt` `PurchaseReceiptScreen.kt` |
| payments | `PayOrderListScreen.kt` `PayOrderDetailScreen.kt` |
| finance | `FinanceRecordListScreen.kt` `FinanceRecordDetailScreen.kt` |
| products | `ProductListScreen.kt` `ProductDetailScreen.kt` `ProductEditScreen.kt` `StockAdjustScreen.kt` `InventoryLedgerScreen.kt` `InventorySnapshotScreen.kt` |
| customers | `CustomerListScreen.kt` `CustomerDetailScreen.kt` `CustomerEditScreen.kt` |
| suppliers | `SupplierListScreen.kt` `SupplierDetailScreen.kt` `SupplierEditScreen.kt` |
| reports | `ReportScreen.kt` |
| settings | `SettingsScreen.kt` |
| agent | `AgentWorkbenchScreen.kt` `AgentChatScreen.kt` `DraftListScreen.kt` `TaskNotificationScreen.kt` |

## 4.3 设计系统基座

必须重构或补齐的 `core/designsystem` 文件：

- `ZhihuijiColors.kt`
- `ZhihuijiTypography.kt`
- `ZhihuijiShapes.kt`
- `ZhihuijiTheme.kt`
- `LiquidGlassSurface.kt`
- `LiquidGlassCard.kt`
- `GlassScaffold.kt`
- `GlassTopBar.kt`
- `SegmentedTabs.kt`
- `SearchFilterBar.kt`
- `FilterChipRow.kt`
- `StatusPill.kt`
- `KpiCard.kt`
- `ChartCard.kt`
- `BottomActionBar.kt`
- `BusinessListItem.kt`
- `PrimaryButton.kt`
- `SecondaryOutlineButton.kt`
- `DangerOutlineButton.kt`

## 4.4 导航路由与页面映射总表

### 认证与主壳路由

| 路由 | 本地入口 | 页面职责 | 目标视觉 |
|---|---|---|---|
| `login` | `AppNavGraph.kt -> LoginScreen.kt` | 登录 | 极光背景 + 居中玻璃登录卡 |
| `register` | `AppNavGraph.kt -> RegisterScreen.kt` | 注册 | 极光背景 + 居中玻璃注册卡 |
| `main` | `AppNavGraph.kt -> MainScreen.kt` | 五栏主壳 | 浮动玻璃底栏 |
| `settings` | `AppNavGraph.kt -> SettingsScreen.kt` | 设置页 | `系统设置 (极光玻璃版)` |

### 顶级 tab 路由

| 路由 | 本地入口 | 页面职责 | 目标设计 |
|---|---|---|---|
| `home` | `MainNavGraph.kt -> DashboardScreen.kt` | 首页经营总览 | `首页 - 经营总览` 两版 |
| `documents` | `MainNavGraph.kt -> DocumentsScreen.kt` | 单据中心壳 | `单据中心 (底部优化版)` |
| `archives` | `MainNavGraph.kt -> ArchivesScreen.kt` | 档案中心壳 | `档案管理 - 商品列表展示` 壳层化 |
| `reports` | `MainNavGraph.kt -> ReportScreen.kt` | 经营报表 | `经营报表 - 多维数据可视化版` |
| `agent` | `MainNavGraph.kt -> AgentWorkbenchScreen.kt` | AI 工作台 | `AI 智能助手` 系列三稿 |

### 明细与编辑路由

| 路由 | 本地入口 | 目标设计 |
|---|---|---|
| `product_detail/{productId}` | `ProductDetailScreen.kt` | 商品详情派生稿 |
| `product_edit/{productId}` / `product_create` | `ProductEditScreen.kt` | `商品编辑 (极光玻璃版)` |
| `stock_adjust/{productId}` | `StockAdjustScreen.kt` | `库存调整 (极光玻璃版)` |
| `inventory_ledger/{productId}` | `InventoryLedgerScreen.kt` | `商品库存流水 (亮色/极光玻璃版)` |
| `inventory_snapshot` | `InventorySnapshotScreen.kt` | `库存盘点 (亮色玻璃版)` |
| `customer_detail/{customerId}` | `CustomerDetailScreen.kt` | `客户档案 (极光玻璃版)` |
| `customer_edit/{customerId}` / `customer_create` | `CustomerEditScreen.kt` | 客户档案编辑派生稿 |
| `supplier_detail/{supplierId}` | `SupplierDetailScreen.kt` | `供应商档案 (极光玻璃版)` |
| `supplier_edit/{supplierId}` / `supplier_create` | `SupplierEditScreen.kt` | 供应商档案编辑派生稿 |
| `sale_order_detail/{orderId}` | `SaleOrderDetailScreen.kt` | `销售单详情 (亮色/极光玻璃版)` |
| `sale_order_edit/{orderId}` / `sale_order_create` | `SaleOrderEditScreen.kt` | 销售开单派生稿 |
| `payment/{orderId}` | `PaymentScreen.kt` | `销售收款 (极光玻璃版)` |
| `sales_returns` | `SalesReturnScreen.kt` | `销售退货 (极光玻璃版)` |
| `purchase_order_detail/{orderId}` | `PurchaseOrderDetailScreen.kt` | 采购详情派生稿 |
| `purchase_order_edit/{orderId}` / `purchase_order_create` | `PurchaseOrderEditScreen.kt` | `采购开单 - 底部视觉优化版` |
| `purchase_receipts` | `PurchaseReceiptScreen.kt` | `采购入库 (亮色玻璃版)` |
| `pay_order_detail/{orderId}` | `PayOrderDetailScreen.kt` | `付款单详情 (极光玻璃版)` |
| `finance_record_detail/{recordId}` | `FinanceRecordDetailScreen.kt` | `资金流水详情 (亮色/极光玻璃版)` |
| `daily_expense` | `DailyExpenseScreen.kt` | `日常支出 / 费用支出 (亮色玻璃版)` |
| `draft_list` | `DraftListScreen.kt` | AI 草稿中心派生稿 |
| `task_notification` | `TaskNotificationScreen.kt` | AI 任务/通知中心派生稿 |
| `agent_chat?...` | `AgentChatScreen.kt` | `AI 智能助手 - 思考查询中 / 深度思考` |

## 4.4.1 Stitch 32 屏覆盖审计表

状态定义：

- `已接入`：已有明确 Android route/screen，且已接入本轮玻璃组件。
- `派生接入`：没有同名独立稿，但由现有 screen 承担同业务语义。
- `合同层已有，UI 缺口`：backend 或 Android network/data 层有接口/仓储，但 feature route/screen 缺失。
- `UI 缺口`：当前未发现独立 Android route/screen；是否已有后端合同需继续专项核对。
- `待设备复核`：尚未完成当前 APK 的真机/模拟器截图比对。

| 序号 | Stitch 标题 | Android 覆盖状态 | 当前入口或缺口 | 验收状态 |
|---|---|---|---|---|
| 01 | 系统设置 (极光玻璃版) | 已接入 | `settings -> SettingsScreen.kt` | 待设备复核 |
| 02 | 供应商档案 (极光玻璃版) | 已接入 | `archives` 供应商 tab、`SupplierList/Detail/EditScreen.kt` | 待设备复核 |
| 03 | 客户档案 (极光玻璃版) | 已接入 | `archives` 客户 tab、`CustomerList/Detail/EditScreen.kt` | 待设备复核 |
| 04 | 销售退货 (极光玻璃版) | 已接入 | `sales_returns -> SalesReturnScreen.kt`，由 `SalesReturnV2Repository.listSalesReturns()/confirm()` 读取并确认真实销售退货单；退款登记因后端仅返回 `refund_amount` 暂保持禁用，不伪造退款流水 | 已设备复核 |
| 05 | 付款单详情 (极光玻璃版) | 已接入 | `pay_order_detail/{orderId} -> PayOrderDetailScreen.kt` | 待设备复核 |
| 06 | AI 智能助手 - 思考查询中 (极光玻璃版) | 已接入 | `agent_chat?... -> AgentChatScreen.kt` | 待设备复核 |
| 07 | 档案管理 - 商品列表展示 (极光玻璃版) | 已接入 | `archives` 商品 tab、`ProductListScreen.kt` | 待设备复核 |
| 08 | 经营报表 - 多维数据可视化版 | 已接入 | `reports -> ReportScreen.kt` | 待设备复核 |
| 09 | AI 智能助手 - 深度思考与优化布局版 | 已接入 | `AgentChatScreen.kt` + `RunTracePanel.kt` + `ResultBlockRenderer.kt` | 待设备复核 |
| 10 | 单据中心 (底部优化版) | 已接入 | `documents -> DocumentsScreen.kt` | 待设备复核 |
| 11 | 商品编辑 (极光玻璃版) | 已接入 | `product_edit/{productId}` / `product_create -> ProductEditScreen.kt` | 待设备复核 |
| 12 | 库存调整 (极光玻璃版) | 已接入 | `stock_adjust/{productId} -> StockAdjustScreen.kt` | 待设备复核 |
| 13 | 销售收款 (极光玻璃版) | 已接入 | `payment/{orderId} -> PaymentScreen.kt` | 待设备复核 |
| 14 | 采购开单 - 底部视觉优化版 | 已接入 | `purchase_order_edit/{orderId}` / `purchase_order_create -> PurchaseOrderEditScreen.kt` | 待设备复核 |
| 15 | 首页 - 经营总览 (底部优化版) | 已接入 | `home -> DashboardScreen.kt` | 待设备复核 |
| 16 | 资金流水详情 (亮色玻璃版) | 已接入 | `finance_record_detail/{recordId} -> FinanceRecordDetailScreen.kt` | 待设备复核 |
| 17 | 供应商对账 (亮色玻璃版) | UI 缺口 | `SupplierDetailScreen.kt` 可承接入口，但缺少独立对账 screen/route | 未完成 |
| 18 | 采购入库 (亮色玻璃版) | 已接入 | `purchase_receipts -> PurchaseReceiptScreen.kt`，由 `PurchaseReceiptV2Repository.listPurchaseReceipts()/confirm()` 读取并确认真实入库单；部分收货与仓库字段仍需后端补合同 | 已设备复核 |
| 19 | 商品库存流水 (亮色玻璃版) | 已接入 | `inventory_ledger/{productId} -> InventoryLedgerScreen.kt`，由 `SyncV2Repository.listInventoryLedger()` 读取真实库存流水 | 待设备复核 |
| 20 | 销售单详情 (亮色玻璃版) | 已接入 | `sale_order_detail/{orderId} -> SaleOrderDetailScreen.kt` | 待设备复核 |
| 21 | 销售单详情 (极光玻璃版) | 已接入 | `sale_order_detail/{orderId} -> SaleOrderDetailScreen.kt` | 待设备复核 |
| 22 | 资金流水详情 (极光玻璃版) | 已接入 | `finance_record_detail/{recordId} -> FinanceRecordDetailScreen.kt` | 待设备复核 |
| 23 | 供应商对账 (极光玻璃版) | UI 缺口 | 缺少独立对账 screen/route | 未完成 |
| 24 | 商品库存流水 (极光玻璃版) | 已接入 | `inventory_ledger/{productId} -> InventoryLedgerScreen.kt`，从商品详情进入 | 待设备复核 |
| 25 | 采购退货 (亮色玻璃版) | UI 缺口 | 当前未发现采购退货 feature screen/route | 未完成 |
| 26 | 供应商往来详情 (亮色玻璃版) | UI 缺口 | `SupplierDetailScreen.kt` 只有供应商详情语义；缺少往来详情 screen/route | 未完成 |
| 27 | 采购退货 (动态交互版) | UI 缺口 | 当前未发现采购退货动态交互 screen/route | 未完成 |
| 28 | 经营报表 (亮色极光玻璃版) | 已接入 | `reports -> ReportScreen.kt` | 待设备复核 |
| 29 | 日常支出 (亮色玻璃版) | 已接入 | `documents -> 资金流水 -> 记录支出 -> daily_expense`，由 `DailyExpenseScreen.kt` + `DailyExpenseViewModel.kt` 调用 `FinanceRepository.createFinanceRecord(CreateFinanceRecordRequest(type=FINANCE_EXPENSE))` 写入真实 `/v1/finance-records`；当前后端合同不含账户余额扣减、附件或自定义发生日期字段，页面显式不伪造这些数据 | 已设备复核 |
| 30 | 首页 - 经营总览 (亮色极光玻璃版) | 已接入 | `home -> DashboardScreen.kt` | 待设备复核 |
| 31 | 库存盘点 (亮色玻璃版) | 已接入 | `inventory_snapshot -> InventorySnapshotScreen.kt`，由 `SyncV2Repository.listInventorySnapshots()/createInventorySnapshot()` 读取/生成真实库存快照；实盘差异草稿仍需后端补合同 | 待设备复核 |
| 32 | AI 智能助手 (亮色极光玻璃版) | 已接入 | `agent -> AgentWorkbenchScreen.kt` | 待设备复核 |

当前覆盖结论：

- Stitch 32 屏中，已有明确 UI route/screen 的为 25 屏。
- 合同层已有但 feature UI 缺失的为 0 屏。
- 其余扩展经营页仍需补独立 UI 并继续核对合同层：供应商对账、采购退货、供应商往来详情。
- 上述统计只代表代码路径覆盖，不代表像素级验收；所有 `已接入` 页面仍需设备截图对照 Stitch 图。

## 4.5 本地所有 UI 文件的逐文件改造责任

### app/navigation

| 文件 | 必改点 |
|---|---|
| `AppNavGraph.kt` | 启动 loading 背景、认证切换过渡、登录成功无白屏闪动 |
| `MainScreen.kt` | Material `NavigationBar` 替换为浮动玻璃底栏；选中态改为蓝色高亮胶囊 |
| `DocumentsScreen.kt` | 顶部标题、分段、内容留白、子页面壳层统一 |
| `ArchivesScreen.kt` | 顶部标题、三段切换、搜索与新增入口上收 |
| `MainNavGraph.kt` | 保持 route 不变，但确保 detail 页全部接入统一 top bar / bottom action 样式 |

### feature/auth

| 文件 | 必改点 |
|---|---|
| `LoginScreen.kt` | 玻璃登录卡、输入框 token、主按钮、错误态、底部次级链接 |
| `RegisterScreen.kt` | 与登录页同母版，补齐确认密码/验证码层级 |

### feature/dashboard

| 文件 | 必改点 |
|---|---|
| `DashboardScreen.kt` | 2x2 KPI 卡重新排版、趋势图卡、待办卡、快捷操作卡、风险提醒卡、顶部品牌区 |

### feature/reports

| 文件 | 必改点 |
|---|---|
| `ReportScreen.kt` | 时间粒度 segmented tab、KPI 卡、折线图卡、环图卡、Top 排行卡整体统一 |

### feature/settings

| 文件 | 必改点 |
|---|---|
| `SettingsScreen.kt` | 头像资料卡、设置项卡组、同步卡、服务器设置行内编辑化、退出按钮降噪 |

### feature/products

| 文件 | 必改点 |
|---|---|
| `ProductListScreen.kt` | 搜索 + 加号、商品高密度卡片、库存状态 pill、价格/库存右对齐 |
| `ProductDetailScreen.kt` | 主信息卡、价格卡、库存卡、最近流水卡、底部固定操作 |
| `ProductEditScreen.kt` | 基本信息/价格信息/库存与供应信息分组、图片上传卡、底部保存栏 |
| `StockAdjustScreen.kt` | 当前库存/调整数量/调整后库存的强视觉关系、危险态提示 |
| `InventoryLedgerScreen.kt` | 商品库存流水时间线、入出库方向标识、变更前后库存、来源单据与时间信息 |
| `InventorySnapshotScreen.kt` | 库存盘点汇总、真实商品快照列表、底部完成盘点动作；草稿保存待后端合同 |

### feature/customers

| 文件 | 必改点 |
|---|---|
| `CustomerListScreen.kt` | 客户搜索、应收金额强调、风险客户高亮 |
| `CustomerDetailScreen.kt` | 主信息卡、应收摘要、最近订单、联系信息、底部操作 |
| `CustomerEditScreen.kt` | 基本资料、联系资料、业务属性字段分组 |

### feature/suppliers

| 文件 | 必改点 |
|---|---|
| `SupplierListScreen.kt` | 搜索、应付金额右侧强调、标签与次级说明重排 |
| `SupplierDetailScreen.kt` | 应付摘要、联系资料、最近采购、对账入口卡 |
| `SupplierEditScreen.kt` | 基本资料、联系人、结算与备注信息分组 |

### feature/sales

| 文件 | 必改点 |
|---|---|
| `SaleOrderListScreen.kt` | 顶部搜索与状态 tab、订单卡信息密度、金额/状态层级 |
| `SaleOrderDetailScreen.kt` | 顶部单号与状态、商品明细、财务摘要、时间线、底部主操作 |
| `SaleOrderEditScreen.kt` | 客户卡、商品明细卡、金额汇总卡、备注卡、底部提交栏 |
| `PaymentScreen.kt` | 收款金额主焦点、支付方式与备注信息、底部确认收款栏 |

### feature/purchases

| 文件 | 必改点 |
|---|---|
| `PurchaseOrderListScreen.kt` | 采购列表卡、状态切换、供应商与金额层级 |
| `PurchaseOrderDetailScreen.kt` | 顶部单号状态卡、商品明细、应付摘要、收货进度、底部操作 |
| `PurchaseOrderEditScreen.kt` | 供应商卡、商品明细卡、金额汇总卡、备注卡、底部提交栏 |
| `PurchaseReceiptScreen.kt` | 采购入库单读取、明细展示、确认入库底部操作；部分收货与仓库字段待后端合同 |

### feature/payments

| 文件 | 必改点 |
|---|---|
| `PayOrderListScreen.kt` | 付款单列表卡、金额和日期层级、状态标签 |
| `PayOrderDetailScreen.kt` | 金额大卡、基本信息卡、备注卡、双底部按钮 |

### feature/finance

| 文件 | 必改点 |
|---|---|
| `FinanceRecordListScreen.kt` | 时间筛选、账户/类型筛选、收支颜色、资金流水卡语义化 |
| `FinanceRecordDetailScreen.kt` | 金额大卡、基础信息卡、备注卡、底部返回操作 |

### feature/agent

| 文件 | 必改点 |
|---|---|
| `AgentWorkbenchScreen.kt` | 身份区、经营摘要、快捷问题、风险提醒、最近会话、待确认草稿 |
| `AgentChatScreen.kt` | 用户气泡、AI 玻璃回复卡、过程轨迹、工具结果卡、输入栏 |
| `DraftListScreen.kt` | 草稿分类 tab、草稿卡字段、确认与归档操作层级 |
| `TaskNotificationScreen.kt` | 任务/通知双 tab、进度卡、已读/未读视觉与时间信息 |

### 共享渲染器与 AI 子组件

| 文件 | 必改点 |
|---|---|
| `ResultBlockRenderer.kt` | 数据卡、榜单卡、建议卡、风险卡统一玻璃视觉 |
| `RunTracePanel.kt` | 过程轨迹折叠、状态标记、耗时/命中数展示 |

## 4.6 实施前的颜色替换矩阵

| 当前旧色 | 新目标色 | 用途 |
|---|---|---|
| `#1677FF` | `#005BBF` | 主品牌蓝、按钮、选中态 |
| `#E6F4FF` | `#D8E2FF` | 主色浅底、激活区背景 |
| `#F5F7FA` | `#F7F9FE` | 页面基础背景 |
| `#EEEEEE` | `#C1C6D6` | 常规边框 / 分隔线 |
| `#666666` | `#414754` | 次文字 |
| `#999999` | `#6B7280` | 弱文字 / 数据次级 |

## 4.7 实施前的液态玻璃效果参数表

| 组件类型 | blur | surface alpha | 圆角 | highlight | inner shadow |
|---|---|---|---|---|---|
| 浮动底栏 | 24dp | 0.16 | 24dp | 1.0 | 6dp |
| 常规信息卡 | 20dp | 0.15 | 16dp | 0.85 | 4dp |
| 强调金额卡 | 20dp | 0.18 | 16dp | 1.0 | 6dp |
| 顶部分段控件 | 16dp | 0.14 | 12dp | 0.8 | 4dp |
| 输入栏 / 搜索栏 | 18dp | 0.12 | 12dp | 0.75 | 3dp |
| 模态 / 底部操作浮层 | 24dp | 0.20 | 20dp | 1.0 | 8dp |

## 5. 设计系统重构任务

## 5.1 Token 层

- `ZhihuijiColors.kt`
  - 用 Stitch 颜色全部替换旧的 `#1677FF` 系
  - 增加 `GlassBorder`, `GlassSurfaceLow`, `GlassSurfaceHigh`, `PageGradientTop`, `PageGradientBottom`, `StatusBlueLight`
- `ZhihuijiTypography.kt`
  - 建立 `Plus Jakarta Sans + Inter` 双字体体系
  - 大标题、金额、图表数字从默认 `MaterialTheme.typography` 中剥离成稳定 token
- `ZhihuijiShapes.kt`
  - 统一收口到 `8 / 12 / 16 / full`
  - 不再使用 24dp 作为常规大圆角

## 5.2 Glass 基础容器

- `LiquidGlassSurface.kt`
  - 当前采用设备安全玻璃层，避免真实 backdrop renderer 在已验证 Android 16 设备首帧测量时崩溃
  - 后续恢复 backdrop blur + vibrancy + lens 前，必须先做独立设备回归
  - 默认视觉预算建议：
    - blur: `20dp`
    - lens refraction: `16dp ~ 24dp`
    - surface alpha: `0.15 ~ 0.20`
    - white highlight: `alpha 0.8 ~ 1.0`
    - inner shadow: `4dp ~ 8dp`
- `LiquidGlassCard.kt`
  - 承担业务卡片统一容器
  - 提供普通、强强调、可点击、危险提示 4 个 variant
- `GlassScaffold.kt`
  - 支持：
    - 渐变背景
    - 状态栏留白
    - 内容滚动区
    - 悬浮底部导航
    - 固定底部主操作区
- `GlassTopBar.kt`
  - 统一支持：
    - 返回
    - 中心标题
    - 左对齐大标题
    - 右侧图标组
    - 下挂搜索 / tab / 过滤区

## 5.3 交互组件

- `SegmentedTabs.kt`
  - 改成更轻的玻璃底托 + 实心选中胶囊
- `SearchFilterBar.kt`
  - 搜索框高度固定 44dp
  - 左侧搜索 icon 灰蓝色
  - 右侧筛选按钮与加号按钮采用深蓝实底
- `StatusPill.kt`
  - 收口为 6 类语义：蓝 / 绿 / 橙 / 红 / 灰 / 白描边
- `BottomActionBar.kt`
  - 所有编辑页、详情页、付款页共用
  - 支持一主一辅、双主操作、单主操作三种模式

## 6. 按页面分组的详细改造计划

## 6.1 认证层：`LoginScreen` `RegisterScreen`

### 目标

- 虽然 Stitch 当前没有单独给登录页，但认证页必须服从同一套极光玻璃系统。

### 布局

- 背景：全屏浅蓝极光渐变
- 中部：单张玻璃登录卡
- 顶部：品牌名 + 简短副标题
- 表单区：手机号、密码、验证码/确认密码
- 底部：主按钮 + 次级跳转文字

### 颜色

- 卡片：白色 `0.78` 透明度
- 输入框：白底，`#C1C6D6` 边框
- 聚焦态：`#1A73E8`
- 主按钮：`#005BBF`

### 交互

- 错误提示只出现在输入框下方，不铺满整页
- 登录成功后过渡到主壳，不出现突兀纯白闪屏

## 6.2 主壳层：`MainScreen` `DocumentsScreen` `ArchivesScreen`

### 目标

- 主壳统一变成“底部浮动玻璃导航 + 顶部轻标题 + 中央内容卡片流”。

### `MainScreen`

- 底栏不再使用标准 `NavigationBar`
- 改为悬浮玻璃底托：
  - 左右边距 16dp
  - 距底 10dp ~ 14dp
  - 高度约 64dp
  - 白色高透底 + 20dp blur
- 选中态：
  - 图标与文字深蓝
  - 指示器为更亮的浅蓝玻璃胶囊

### `DocumentsScreen`

- 顶栏标题固定“单据中心”
- 顶部 tab 胶囊对齐 Stitch `单据中心 (底部优化版)`
- 各子页共享上方 8dp 间距与 16dp 左右边距，不要再出现子列表和壳层留白风格不一致

### `ArchivesScreen`

- 顶栏标题固定“档案管理”
- 顶部三段 tab：商品 / 客户 / 供应商
- 壳层搜索区优先沉淀到顶层，而不是每个列表自己再套一层重复搜索

## 6.3 首页：`DashboardScreen`

### 设计映射

- 主参考：`首页 - 经营总览 (底部优化版)` 与 `首页 - 经营总览 (亮色极光玻璃版)`

### 布局

- 顶栏：左上品牌“智慧记”，右上通知与设置
- 首屏 KPI：2 x 2 玻璃指标卡
- 第二屏：销售趋势图
- 第三屏：待办快捷入口
- 第四屏：风险提醒 / 低库存

### 颜色与组件

- KPI 卡不是纯白卡，而是“浅色语义底 + 玻璃叠层”
- 今日销售额：蓝
- 待收款：橙
- 低库存：红
- 订单完成：绿
- 趋势图蓝线：`#1A73E8`
- 图表底区：`#E3F2FD`

### 当前代码必须调整

- `LazyVerticalGrid` 的卡片高度要更接近 Stitch，避免现在 320dp 网格块太呆板
- 快捷操作区要更接近横向业务快捷入口，而不是平均分布的简单 icon row
- 当前 `DashboardScreen.kt` 已有 KPI、趋势、提醒、低库存、快捷操作五段结构，重构时不改信息骨架，只重排视觉层级，避免把现有经营信息重写成只有漂亮卡片的空首页。
- 顶部品牌区升级成两行头部：
  - 第一行：品牌名、日期或门店概览、通知与设置。
  - 第二行：一句经营摘要，例如“3 个库存预警待处理”。
- KPI 区从固定 320dp 网格改成两列自适应卡：
  - 单卡最小高度 132dp。
  - 卡内固定为 `标题 -> 主数字 -> 趋势/说明`。
  - 主数字角落允许放半透明语义图标，不能只剩文字。
- 待处理提醒区拆成两层：
  - 上方 1 张总提醒卡，概括今日风险数量。
  - 下方 2~4 张轻量业务入口卡，承接待收款、低库存、草稿和 AI 事项。
- 快捷操作改成横向业务胶囊组：
  - 每个胶囊宽 88dp ~ 104dp。
  - 上方 20dp 语义图标，下方 12sp 标签。
  - 点击反馈使用浅蓝玻璃高亮，不以 Material 波纹为主视觉。

## 6.4 报表：`ReportScreen`

### 设计映射

- 主参考：`经营报表 - 多维数据可视化版`
- 次参考：`经营报表 (亮色极光玻璃版)`

### 布局

- 顶部时间范围 segmented tab
- 4 个 KPI 卡
- 折线趋势图
- 环形占比图
- Top 商品排行榜

### 颜色

- 销售额：蓝
- 利润：绿
- 应收：橙
- 应付：红
- 图表卡统一使用浅蓝玻璃容器，不允许出现大面积灰底

### 图表要求

- 折线图卡高度比当前更高，避免信息过密
- 图例、X 轴标签、百分比标签全部降噪
- 环形图色带采用蓝 / 深蓝 / 浅蓝 / 灰蓝，不要混入过饱和杂色
- 当前 `ReportScreen.kt` 已是 `KPI -> 趋势图 -> 分类占比 -> Top 排行` 的顺序，重构只增强层级，不改成瀑布式杂乱拼图。
- 顶部时间范围分段明确为 `今日 / 本周 / 本月 / 自定义`。
- KPI 四卡统一结构：
  - 左上角指标名。
  - 中部大数字。
  - 底部趋势箭头与环比说明。
- 趋势图卡建议高度 280dp ~ 320dp：
  - 上方标题行。
  - 中部图表主体。
  - 下方保留 2~4 个关键图例。
- Top 商品排行榜改成榜单卡：
  - 左侧排名徽标。
  - 中间商品名 + 分类/数量。
  - 右侧金额或占比右对齐。

## 6.5 设置：`SettingsScreen`

### 设计映射

- 主参考：`系统设置 (极光玻璃版)`

### 布局

- 顶部品牌栏
- 第一张卡：头像、昵称、角色说明
- 第二张卡组：个人资料 / 账号安全 / 同步设置 / 导入导出 / 关于我们
- 底部单独退出按钮

### 颜色

- 行项图标：蓝灰体系
- 行项卡：白玻璃，高透轻描边
- 退出按钮：白底红字，不要整块深红

### 当前代码必须调整

- `OutlinedTextField` 不能继续裸用 Material 默认视觉
- 服务器设置应改成单独的玻璃信息块 + 行内编辑样式
- `SettingsScreen.kt` 现状已经是 `账号 -> 服务器 -> 设置项 -> 同步 -> 退出` 的顺序，这个信息顺序保留，但每一段都要从“单张列表卡”升级到“分组卡 + 行项”。
- 账号区拆为：
  - 左侧头像徽标。
  - 中间昵称、账号身份、最近登录状态。
  - 右侧轻量“编辑资料”入口。
- 服务器设置默认展示为只读地址行，点击“编辑”后展开单行玻璃输入框与“保存/取消”。
- 同步卡强化状态结构：
  - 左侧图标。
  - 中间两行文案：当前状态 / 最近同步时间。
  - 右侧按钮或加载态。
- 危险操作区只保留退出登录，不在这页混入删除账号等重操作。

## 6.6 AI 模块：`AgentWorkbenchScreen` `AgentChatScreen` `DraftListScreen` `TaskNotificationScreen`

### 设计映射

- `AgentWorkbenchScreen`：`AI 智能助手 (亮色极光玻璃版)`
- `AgentChatScreen`：`AI 智能助手 - 思考查询中` 与 `深度思考与优化布局版`
- `DraftListScreen` `TaskNotificationScreen`：没有一一对应 Stitch 单屏，风格从上述 AI 主屏派生

### `AgentWorkbenchScreen`

- 顶部：AI 身份区 + 问候语
- 中段：经营摘要 KPI
- 下段：风险洞察 + 快捷问题 + 最近会话 + 待确认草稿
- 快捷问题必须用小胶囊，不用大按钮阵列
- 当前 `AgentWorkbenchScreen.kt` 已经有这五段主结构，重构目标是收敛样式而不是再加新块：
  - `GreetingHeader` 改成 56dp AI 头像玻璃圆章 + 双行问候。
  - `KpiGrid` 每张卡必须出现“指标名 / 数值 / 来源模块”。
  - `QuickQuestionsSection` 使用自动换行胶囊，不用通栏按钮。
  - `RiskAlertsSection` 用红橙语义条，不要每项都像普通列表。
  - `RecentConversationsSection` 和 `PendingDraftsSection` 要一眼区分“历史”和“待处理”。

### `AgentChatScreen`

- 用户消息：右侧蓝色实底气泡
- AI 回答：左侧玻璃卡片
- 过程轨迹：可折叠时间线 / 状态块
- 数据结论：使用 KPI 子卡和榜单卡，不要只有纯文本
- 底部输入区：悬浮玻璃条，发送按钮为深蓝圆按钮
- 当前 `AgentChatScreen.kt` 已包含消息、run trace、压缩提示、草稿确认四层信息；重构要把这四层视觉明确区分：
  - 用户气泡：深蓝实底 `#005BBF`，白字，最大宽度 78%。
  - AI 气泡：白玻璃卡，深色文字，最大宽度 86%。
  - Run trace：嵌入 AI 气泡下方的折叠玻璃子卡。
  - Context compacted banner：顶部悬浮细条，不长期占据大高度。
- 输入区固定高度建议 60dp：
  - 左侧多行输入。
  - 中间停止/压缩等辅助状态位。
  - 右侧 40dp 圆形发送按钮。
- AI 结果中如果有列表、洞察、建议操作，不再渲染成整段正文，而要分别落到数据卡、榜单卡、建议动作卡、风险卡。

### `DraftListScreen`

- 顶栏下方放分类 segmented tabs
- 草稿卡要有：
  - 草稿类型
  - 编号
  - 往来方
  - 金额
  - 创建时间
  - 状态标签
- 当前 `DraftListScreen.kt` 现状缺少编号、往来方、金额三项主字段，计划中必须补到卡片主视区：
  - 第一行：类型标签 + 状态标签。
  - 第二行：草稿标题。
  - 第三行：单号/往来方/金额三列信息。
  - 第四行：创建时间 + 操作按钮。
- 删除与提交按钮改成“弱危险 + 主操作”组合，不继续使用两个同权小图标。

### `TaskNotificationScreen`

- 顶部双 tab：任务 / 通知
- 任务卡：进度、状态、耗时、来源模块
- 通知卡：图标、标题、摘要、时间、已读状态
- 当前 `TaskNotificationScreen.kt` 已有双 tab 与进度条，但卡片信息仍偏薄：
  - 任务卡需要补“来源模块”“最近更新时间”“执行耗时/进度说明”。
  - 通知卡需要补未读点、摘要截断策略、点击后状态切换反馈。
- 已读通知卡透明度应略降，但不能灰到不可读。

## 6.7 商品域：`ProductListScreen` `ProductDetailScreen` `ProductEditScreen` `StockAdjustScreen` `InventoryLedgerScreen` `InventorySnapshotScreen`

### 设计映射

- 商品列表：`档案管理 - 商品列表展示`
- 商品编辑：`商品编辑 (极光玻璃版)`
- 库存调整：`库存调整 (极光玻璃版)`
- 商品详情：从 `商品编辑 + 商品库存流水` 组合派生
- 商品库存流水：`商品库存流水 (亮色玻璃版)`、`商品库存流水 (极光玻璃版)`
- 库存盘点：`库存盘点 (亮色玻璃版)`

### `ProductListScreen`

- 顶部搜索框 + 加号按钮
- 卡片结构：
  - 左：圆形或圆角商品图 / 首字图标
  - 中：商品名称、分类、条码/规格
  - 右：库存、价格、状态
- 状态标签悬在右上，优先用蓝 / 橙 / 红
- 当前 `ProductListScreen.kt` 已有 `SearchFilterBar + 分类 Chip + 状态 Chip + 列表 + FAB` 五段结构。重构时：
  - 搜索与加号入口上收为同一行。
  - 分类 Chip 和状态 Chip 分成上下两行。
  - 列表卡单行高度控制在 92dp ~ 104dp。
  - 右侧优先展示 `库存数量 + 售价`，状态 pill 悬于卡片右上角。

### `ProductDetailScreen`

- 顶部主信息卡：名称、分类、条码、库存状态
- 中段：价格、单位、供应商、最近变更
- 下段：库存流水预览 / 最近交易
- 右下不再单独悬浮多个按钮，改为底部固定“编辑 / 调整库存”
- 当前 `ProductDetailScreen.kt` 已收口为 `GlassScaffold + GlassTopBar + 主信息/价格/库存玻璃卡 + BottomActionBar`。
- 底部操作栏已固定在安全区上方，采用 `次按钮=编辑资料`、`主按钮=调整库存`；最近库存流水和最近交易仍属于后端/数据增强项。
- 商品库存流水入口已从商品详情接入 `inventory_ledger/{productId}`，由 `InventoryLedgerScreen.kt` 展示真实库存变动时间线。
- 库存盘点已从单据中心顶栏“盘点”进入 `inventory_snapshot`，由 `InventorySnapshotScreen.kt` 使用真实商品列表和 `/v2/inventory/snapshots` 生成/展示当天库存快照。Stitch 中的“保存草稿”和手工实盘差异输入需要新增后端盘点草稿/实盘数量合同后再放开。

### `ProductEditScreen`

- 表单按卡片分组：
  - 基本信息
  - 价格信息
  - 库存与供应信息
  - 备注 / 图片
- 主按钮固定底部
- 当前 `ProductEditScreen.kt` 已拆成基本信息、价格信息、库存预警玻璃表单卡，输入框统一使用 `GlassTextField`。
- 保存入口已统一收口到底部 `BottomActionBar`；图片上传与更多供应字段保留为后续业务字段增强。

### `StockAdjustScreen`

- 主体是单张聚焦表单卡
- 数量输入区必须突出：
  - 当前库存
  - 调整数量
  - 调整后库存
- 危险方向（减少库存）用橙红强调，不是纯文字
- 当前 `StockAdjustScreen.kt` 只有商品卡、数量框、原因框、确认按钮，视觉重构时要把计算关系直接展示出来：
  - 上方信息卡：商品名 + 当前库存。
  - 中间核心卡：调整数量大输入。
  - 下方结果条：调整后库存实时预览。
  - 最下方原因备注卡。

## 6.8 客户域：`CustomerListScreen` `CustomerDetailScreen` `CustomerEditScreen`

### 设计映射

- 主参考：`客户档案 (极光玻璃版)`

### `CustomerListScreen`

- 与供应商列表共用同一母版
- 卡片右侧重点展示应收金额
- 风险客户用红色金额 + 小状态标签
- 当前 `CustomerListScreen.kt` 已有搜索、状态分段和列表，重构重点是：
  - 列表卡右侧金额独立成两行：`应收` 标签 + 金额。
  - 欠款客户的金额和 pill 同时转红橙语义。
  - 不再用普通文本硬塞状态，统一改为 `StatusPill`。

### `CustomerDetailScreen`

- 顶部主卡：
  - 客户名
  - 联系电话
  - 联系地址 / 标签
  - 应收总额
- 中段：
  - 最近订单
  - 欠款信息
  - 备注
- 当前 `CustomerDetailScreen.kt` 已收口为 `GlassScaffold + GlassTopBar + 基本/联系/财务/备注玻璃卡 + BottomActionBar`，底部突出应收余额与编辑入口。
- 最近订单、最近下单时间、订单数等仍属于后端数据增强项，后续接真实数据后补入风险摘要卡。

### `CustomerEditScreen`

- 表单保持 2~3 个逻辑分组，不要一整页无层级文本框
- 风险字段如信用额度、欠款提醒阈值用浅橙提示
- 当前 `CustomerEditScreen.kt` 与商品编辑一样还是直排输入框，必须至少拆成基本资料、联系方式、业务备注/风险字段三组。

## 6.9 供应商域：`SupplierListScreen` `SupplierDetailScreen` `SupplierEditScreen`

### 设计映射

- 主参考：`供应商档案 (极光玻璃版)`
- 明细扩展：`供应商对账`、`供应商往来详情`

### 目标

- 列表与客户共用同一母版
- 详情页重点突出：
  - 应付金额
  - 最近采购
  - 对账状态
  - 联系方式
- 当前 `SupplierListScreen.kt` 与客户列表形态接近，重构时直接沿用同母版，只替换成“应付”语义和供应商图标。
- 列表右侧金额结构改为 `应付金额标签 + 金额 + 状态 pill`。

### `SupplierDetailScreen`

- 底部主操作优先考虑“编辑 / 查看对账 / 发起采购”
- 当前 `SupplierDetailScreen.kt` 已收口为 `GlassScaffold + GlassTopBar + 基本/联系/财务/备注玻璃卡 + BottomActionBar`，底部突出应付余额与编辑入口。
- 对账状态卡、最近采购记录预览和发起采购入口属于后续数据/流程增强项，不再作为本轮 UI 母版缺口。

## 6.10 销售域：`SaleOrderListScreen` `SaleOrderDetailScreen` `SaleOrderEditScreen` `PaymentScreen`

### 设计映射

- 列表壳层：`单据中心`
- 详情：`销售单详情 (亮色/极光玻璃版)`
- 收款：`销售收款 (极光玻璃版)`

### `SaleOrderListScreen`

- 顶部搜索 + segmented tabs：全部 / 草稿 / 已完成 / 已取消
- 列表卡突出：
  - 单号
  - 客户
  - 时间
  - 实收 / 应收
  - 状态
- 当前 `SaleOrderListScreen.kt` 已有搜索、tab、列表、FAB，重构重点是订单卡：
  - 第一行：单号 + 状态 pill。
  - 第二行：客户名 + 日期。
  - 第三行：应收/已收/总额三列。

### `SaleOrderDetailScreen`

- 顶部单号与状态卡
- 商品明细卡
- 财务摘要卡
- 时间线卡
- 底部操作：“再次销售”或“编辑 / 收款”
- 当前 `SaleOrderDetailScreen.kt` 已替换为 `GlassScaffold + GlassTopBar + 订单摘要玻璃卡 + 商品明细玻璃卡 + BottomActionBar`，底部主操作为收款，草稿态提供编辑单据次操作。
- 金额摘要已在订单摘要与底部待收金额中呈现；更完整时间线仍属于后续数据增强项。

### `SaleOrderEditScreen`

- 分组：
  - 客户信息
  - 商品明细
  - 金额汇总
  - 备注
- 商品行要是高密度业务卡，而不是松散表单项
- 当前 `SaleOrderEditScreen.kt` 与采购编辑高度相似，计划中要求两者共用编辑母版。
- “添加商品”按钮从顶栏图标转为商品卡区内的浅蓝次按钮。

### `PaymentScreen`

- 强调收款金额
- 下方列出支付方式、收款备注、关联订单
- 底部主按钮固定，次按钮弱化
- 当前 `PaymentScreen.kt` 已改成订单摘要卡、金额输入玻璃卡、支付方式胶囊卡、备注输入卡、底部固定确认收款栏。
- 待收金额和订单总额必须同时出现，且待收金额是视觉主焦点。

## 6.11 采购域：`PurchaseOrderListScreen` `PurchaseOrderDetailScreen` `PurchaseOrderEditScreen` `PurchaseReceiptScreen`

### 设计映射

- 编辑主参考：`采购开单 - 底部视觉优化版`
- 扩展参考：`采购入库` `采购退货`

### 目标

- 采购编辑页结构与销售编辑页同母版，但色彩更偏蓝灰，不要和销售收款蓝色强调冲突
- 详情页要兼容“待入库 / 部分入库 / 已完成 / 退货”状态
- 当前三页代码与销售域几乎同构，计划要求直接复用销售域母版，只做文案和语义色差异化。
- 采购状态新增一档“部分入库”后，语义色用橙色，不与取消态红色混淆。
- 采购入库已从单据中心顶栏“入库”进入 `purchase_receipts`，由 `PurchaseReceiptScreen.kt` 通过 `/v2/purchase-receipts` 展示真实入库单并调用确认入库；当前后端合同未提供仓库字段或部分收货数量编辑，因此页面只展示合同可证字段并禁用“部分收货”。

## 6.12 付款单域：`PayOrderListScreen` `PayOrderDetailScreen`

### 设计映射

- 详情主参考：`付款单详情 (极光玻璃版)`

### `PayOrderListScreen`

- 放在单据中心分页内
- 行项突出供应商、金额、日期、状态
- 当前 `PayOrderListScreen.kt` 已使用标准 `StatusPill`，列表次级信息已补付款方式与关联单据位。

### `PayOrderDetailScreen`

- 顶部金额数字必须是整页视觉焦点
- 基本信息与备注分成两张独立玻璃卡
- 底部操作“打印单据 / 分享”用双按钮布局
- 当前 `PayOrderDetailScreen.kt` 已拆成金额主卡、基本信息卡、备注卡，并接入底部“打印单据 / 分享”双按钮操作栏。

## 6.13 财务域：`FinanceRecordListScreen`

### 设计映射

- `资金流水详情`、`日常支出`、`供应商对账`

### 目标

- 当前财务列表不能只像普通 list screen
- 必须具备：
  - 时间维度筛选
  - 账户 / 类型筛选
  - 收入 / 支出颜色区分
  - 支出卡片和转账卡片不同图标语义
- 当前 `FinanceRecordListScreen.kt` 已具备搜索、时间维度 segmented tabs、类型 segmented tabs、账户 filter chips 和资金流水列表。
- 行项金额已按收入/支出使用语义色；左侧类型图标可在后续业务图标库增强时补齐。

## 7. 组件替换与代码落点

## 7.1 允许直接复用

- `GlassScaffold`
- `GlassTopBar`
- `KpiCard`
- `SegmentedTabs`
- `StatusPill`
- `BottomActionBar`

但前提是先完成 token 和 liquid glass 升级。

## 7.2 必须先改后用

- `LiquidGlassSurface`
- `LiquidGlassCard`
- `SearchFilterBar`
- `ChartCard`
- `BusinessListItem`
- `PrimaryButton`

## 7.3 页面里必须逐步移除的直接 Material 默认视觉

- 直接裸用的 `NavigationBar`
- 直接裸用的 `OutlinedTextField`
- feature 内自己拼的白底卡片
- 不带统一 token 的临时颜色 `Color(...)`

## 8. 实施顺序

### Phase 0：设计基线冻结

- 冻结本计划与 Stitch 导出目录
- 所有 UI 任务先标注使用哪张 Stitch screen

### Phase 1：设计系统基座

- 改 `ZhihuijiColors`
- 改 `ZhihuijiTypography`
- 改 `LiquidGlassSurface`
- 改 `GlassScaffold`
- 改底部导航与顶栏

### Phase 2：主壳与首页

- `MainScreen`
- `DocumentsScreen`
- `ArchivesScreen`
- `DashboardScreen`
- `SettingsScreen`
- 交付要求：
  - 底部导航、顶部标题、首页 KPI 和快捷入口一起验收。
  - 先统一壳层，再允许子页逐个替换。

### Phase 3：高频业务页

- `ProductList/Detail/Edit/StockAdjust`
- `SaleOrderList/Detail/Edit/Payment`
- `PurchaseOrderList/Detail/Edit`
- `PayOrderList/Detail`
- 交付要求：
  - 先打通列表页母版、详情页母版、编辑页母版、底部操作栏四套共性。
  - 再落业务差异，避免每页单独发明样式。

### Phase 4：档案与报表

- `Customer*`
- `Supplier*`
- `ReportScreen`
- `FinanceRecordListScreen`
- 交付要求：
  - 与前一阶段复用率达到 70% 以上。
  - 新增视觉只能来自领域组件，不再新增新的页面骨架。

### Phase 5：AI

- `AgentWorkbenchScreen`
- `AgentChatScreen`
- `DraftListScreen`
- `TaskNotificationScreen`
- 交付要求：
  - AI 区必须验证玻璃消息卡、run trace、建议卡、草稿卡四种子母版。
  - AI 结果块优先抽到共享渲染器，避免在 `feature/agent` 内重复拼装。

## 8.1 页面母版收口规则

- 列表页母版
  - 固定结构：`TopBar -> Search/Filter -> Segmented/Chip -> List -> Floating Action`
  - 适用：商品、客户、供应商、销售单、采购单、付款单、财务流水
- 详情页母版
  - 固定结构：`TopBar -> 主摘要卡 -> 明细/信息卡组 -> BottomActionBar`
  - 适用：商品详情、客户详情、供应商详情、销售单详情、采购单详情、付款单详情
- 编辑页母版
  - 固定结构：`TopBar -> 多张分组表单卡 -> 汇总卡(可选) -> BottomActionBar`
  - 适用：商品、客户、供应商、销售单、采购单、收款、库存调整
- 报表页母版
  - 固定结构：`TopBar -> Time Tabs -> KPI Grid -> Chart Cards -> Ranking`
- AI 页母版
  - 固定结构：`TopBar -> 身份/摘要 -> 对话或洞察主区 -> 辅助状态卡 -> 底部输入/操作区`

## 8.2 现状与目标差距总表

| 页面类型 | 当前现状 | 目标状态 | 本轮动作 |
|---|---|---|---|
| 认证页 | 纯表单居中，裸 Material 输入框/按钮 | 单卡玻璃认证母版 | 改卡片、输入框、按钮、错误态 |
| 主壳页 | 已有背景与底栏，但底栏仍偏 Material | 浮动玻璃导航壳 | 改底栏、标题栏、留白 |
| 列表页 | 搜索/分段/列表已具备，但卡片信息薄 | 高密度业务卡 + 统一筛选母版 | 改列表卡、筛选层级、FAB |
| 详情页 | 多数仍是普通 Card 堆叠 | 主摘要卡 + 信息卡组 + 底部操作 | 改卡组结构、补底部操作栏 |
| 编辑页 | 连续 `OutlinedTextField` 为主 | 分组表单卡 + 固定提交栏 | 拆卡、收口按钮 |
| 报表页 | 已有 KPI/图表，但视觉层级不足 | 统一图表卡与报表母版 | 改 KPI 卡、图表卡、榜单卡 |
| AI 页 | 结构较完整，但子块风格不统一 | 玻璃 AI 母版 + 结果块体系 | 改消息卡、轨迹卡、草稿卡、任务卡 |

## 9. 验收标准

- 所有页面截图一眼可识别为同一产品，不再出现“壳层像新稿，子页像旧 Material”的割裂。
- 所有页面背景统一为浅蓝极光，而不是局部灰白。
- 所有主卡片都有玻璃层级、白色高光边、轻内阴影和一致圆角。
- 所有大金额、KPI、主标题遵守新的字体和层级。
- 所有编辑页都有固定底部主操作区。
- 所有列表页都有统一搜索 / tab / 筛选体系。
- AI 页必须具备“过程、数据、回答、输入”四层结构，而不是单纯聊天框。

## 10. 本轮文档收口要求

- `docs/spec/42-android-liquid-glass-ui-refactor-plan.md`
  - 作为本轮新的 UI 重构总计划
- `master-goods-android/UI-DESIGN-SPEC.md`
  - 降为“兼容入口 + 摘要规范”
- `docs/design-mockups/README.md`
  - 降为历史参考说明，不再写“统一视觉真源”
- `docs/.DS_Store`
  - 删除
- `docs/technical-analysis/.DS_Store`
  - 删除
- 清理原则：
  - 已失去唯一信息价值、只重复历史参考指向的说明文档可以删除。
  - 仍承载代码定位、历史审计、验收记录、字段定义的文档不删除。

## 11. 本轮实施证据

### 11.1 代码覆盖证据

- 全局组件调用覆盖：
  - `GlassScaffold` / `GlassTopBar` / `LiquidGlassCard` / `GlassTextField` / `BottomActionBar` / `FloatingGlassActionButton` / `PrimaryButton` / `BusinessListItem` / `SearchFilterBar` / `SegmentedTabs` 在 `app`、`feature`、`core/designsystem` 中合计出现 298 处。
- 裸 Material 输入框收口：
  - `rg "OutlinedTextField\\(" master-goods-android/app/src/main/java master-goods-android/feature master-goods-android/core/designsystem/src/main/java`
  - 当前仅剩：
    - `core/designsystem/GlassTextField.kt`
    - `feature/auth/LoginScreen.kt` 中的 `AuthOutlinedField` 封装
- 导航闭环：
  - `DocumentsScreen` 已透传销售单/采购单创建入口，并接入销售退货、采购入库与库存盘点入口。
  - `ArchivesScreen` 已透传商品/客户/供应商创建入口。
  - `MainNavGraph` 已接入上述 create route。

### 11.2 编译验证

已通过以下验证命令：

```bash
JAVA_HOME=/Users/sunyiyang/.local/jdks/temurin-21/Contents/Home ./master-goods-android/gradlew -p /Users/sunyiyang/Desktop/Project/master-goods/master-goods-android :app:compileDebugKotlin --console=plain -Dorg.gradle.java.home=/Users/sunyiyang/.local/jdks/temurin-21/Contents/Home
```

结果：`BUILD SUCCESSFUL`。

### 11.3 采购入库端到端复核

2026-06-06 已完成采购入库真实链路复核：

- 后端修复并部署到 `117.72.79.106`：`/opt/zhihuiji117/app.jar` 新包 SHA-256 为 `6ec43822c086c3fe2333534880b4bb0fca501fee79e4492aeb08f01773bae135`，旧包已备份到 `/opt/zhihuiji117/backups/runtime-20260606-224900/app.jar`，旧镜像已保留 `zhihuiji117-backend-local:backup-20260606-224900`。
- 线上接口复核：登录 `POST /zhihuiji/v1/auth/login` 返回 200；`GET /zhihuiji/v2/purchase-receipts`、`GET /zhihuiji/v2/sales-returns`、以及两者带 `keyword=__unlikely__` 的查询均返回 200 + `code:0`，不再触发 PostgreSQL `LOWER(:keyword)` 空参数 500。
- Android 设备复核：设备 `d715a3a4` 从 `单据中心 -> 入库` 进入 `purchase_receipts`，页面显示真实后端空态 `暂无采购入库单 / 当前账号还没有后端返回的采购入库记录。`，不再显示 `采购入库单加载失败`。
- 证据文件：`/tmp/master-goods-purchase-receipts-after-backend-fix.png`、`/tmp/master-goods-purchase-receipts-after-backend-fix-ui.xml`。

### 11.4 销售退货端到端复核

2026-06-06 已完成销售退货真实链路复核：

- Android 入口复核：设备 `d715a3a4` 从 `单据中心 -> 退货` 进入 `sales_returns`，UI 树出现 `销售退货`、`读取当前账号真实销售退货单`。
- Android 数据态复核：当前账号后端返回空列表时，页面显示真实空态 `暂无销售退货单 / 当前账号还没有后端返回的销售退货记录。`，未显示 `销售退货单加载失败`。
- 证据文件：`/tmp/master-goods-sales-returns.png`、`/tmp/master-goods-sales-returns-ui.xml`。

### 11.5 日常支出端到端复核

2026-06-06 已完成日常支出 route 与表单设备复核：

- Android 入口复核：设备 `d715a3a4` 从 `单据中心 -> 资金流水 -> 记录支出` 进入 `daily_expense`，UI 树出现 `费用支出`、`写入当前账号真实资金流水`、`支出金额 (¥)`、`房租/水电/工资/办公/营销/物流/餐饮/其他`、`付款账户`、`发生日期`、`上传照片`、`记录支出`。
- Android 合同边界复核：页面文案明确 `保存后会直接写入 /v1/finance-records，不生成任何示例流水。`；`发生日期` 由后端写入当前时间；`上传照片` 因后端资金流水合同暂未提供附件字段而不提交照片占位数据。
- 写入路径复核：代码路径固定提交 `CreateFinanceRecordRequest(type=FINANCE_EXPENSE)` 到 `FinanceRepository.createFinanceRecord()`；本次设备验收没有点击 `记录支出`，避免向当前真实账号写入测试支出流水。
- 证据文件：`/tmp/master-goods-finance-list-with-expense-ui.xml`、`/tmp/master-goods-daily-expense.png`、`/tmp/master-goods-daily-expense-ui.xml`、`/tmp/master-goods-daily-expense-mid.png`、`/tmp/master-goods-daily-expense-mid-ui.xml`、`/tmp/master-goods-daily-expense-lower.png`、`/tmp/master-goods-daily-expense-lower-ui.xml`。

### 11.6 后续复核建议

- 设备或模拟器逐屏截图复核：重点看浮动底栏、详情页底部操作、编辑页键盘弹出、AI 输入栏。
- 按 `4.4.1 Stitch 32 屏覆盖审计表` 补齐缺失 route/screen，尤其是采购退货、供应商对账、供应商往来详情。
- AI 结果图表块接入正式图表库时，只替换 `ResultBlockRenderer` 的可视化层，不允许回退到 mock/sample/demo 数据。
