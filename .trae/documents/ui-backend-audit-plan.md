# 安卓端 UI 与后端接口/数据库字段一致性审查计划

## 摘要

本计划旨在系统审查 Android 端 UI 实现与设计稿的一致性，以及后端 API 接口和数据库字段在安卓端的覆盖情况。审查将产出一份详细的差距报告，列出所有不一致项、缺失字段和未使用接口。

## 当前状态分析

### 1. 设计图 vs UI 实现

**设计图（8张 PNG）覆盖页面：**
- 01.png: 首页经营看板（含销售趋势图、待处理提醒、低库存预警）
- 02.png: 登录页、注册页、首页、设置页
- 03.png: 商品列表、商品详情、编辑商品、库存调整
- 04.png: 客户列表、客户详情、供应商列表、供应商详情
- 05.png: 销售单列表、销售开单、销售单详情、收款
- 06.png: 采购单列表、采购开单、采购单详情、付款单列表
- 07.png: 资金流水、报表总览、库存流水报表、对账汇总
- 08.png: AI工作台、AI问答、操作草稿、任务与通知

**当前 Android 已实现页面（12个 Screen）：**
| 页面 | 文件 | 状态 |
|------|------|------|
| 登录 | LoginScreen.kt | 基础实现 |
| 注册 | RegisterScreen.kt | 基础实现 |
| 首页 | DashboardScreen.kt | 基础实现（缺销售趋势图、待处理提醒） |
| 单据-销售单 | SaleOrderListScreen.kt | 基础列表 |
| 单据-采购单 | PurchaseOrderListScreen.kt | 基础列表 |
| 单据-付款单 | PayOrderListScreen.kt | 基础列表 |
| 单据-资金流水 | FinanceRecordListScreen.kt | 基础列表 |
| 档案-商品 | ProductListScreen.kt | 基础列表 |
| 档案-客户 | CustomerListScreen.kt | 基础列表 |
| 档案-供应商 | SupplierListScreen.kt | 基础列表 |
| 报表 | ReportScreen.kt | 基础实现（缺图表） |
| AI助手 | AgentWorkbenchScreen.kt | 基础实现 |
| 设置 | SettingsScreen.kt | 基础实现 |

**关键差距：**
- 设计图中有约 32 个页面，当前实现仅 13 个
- 大量详情页、编辑页、开单页完全缺失
- 首页缺少销售趋势折线图、待处理提醒列表、低库存商品列表
- 报表页缺少图表（ChartCard 组件未实现）
- AI助手页缺少 KPI 行展示（代码中 KpiRow 组件已写但未被调用）

### 2. 后端 API vs Android 端模型

**后端 V2 API 已落地接口组：**
| 领域 | 接口状态 | Android 端状态 |
|------|----------|----------------|
| auth | 新版待做 | AuthRepository 存在，但无 V2 接口定义 |
| products | 新版已做 | ZhihuijiV2Api 已定义，但 ViewModel 未调用 |
| partners (customers/suppliers) | 新版已做 | ZhihuijiV2Api 已定义，但 ViewModel 未调用 |
| sales (sale-orders/sales-returns) | 新版已做 | ZhihuijiV2Api 已定义，但 ViewModel 未调用 |
| purchase (purchase-orders/purchase-receipts) | 新版已做 | ZhihuijiV2Api 已定义，但 ViewModel 未调用 |
| pay-orders | 新版已做 | ZhihuijiV2Api 已定义，但 ViewModel 未调用 |
| finance (accounts/transfers/bill-fund-links) | 新版已做 | ZhihuijiV2Api 已定义，但 ViewModel 未调用 |
| inventory (ledger/snapshots/monthly-stats) | 新版已做 | ZhihuijiV2Api 已定义，但 ViewModel 未调用 |
| media | 待验证 | ZhihuijiV2Api 已定义，未使用 |
| agent | 待验证 | ZhihuijiV2Api 已定义，未使用 |
| sync | 待验证 | ZhihuijiV2Api 已定义，未使用 |
| import-jobs | 新版已做 | ZhihuijiV2Api 已定义，未使用 |

**核心问题：所有 ViewModel 目前都使用 mock 数据，未调用任何真实 API。**

### 3. 数据库字段 vs Android 端模型

**后端 Entity 字段（以 ProductEntity 为例）：**
- id, code, name, category_id, category_name, unit_id, unit_name, sale_price, purchase_price, stock, safe_stock, status, created_at, updated_at
- 扩展字段：price_levels, default_supplier, supplier_relations

**Android 端 ProductV2Dto：**
- 完全映射了后端所有字段，包括扩展字段

**后端 SaleOrderEntity 字段：**
- id, order_no, customer_id, customer_name, items, subtotal_amount, discount_amount, total_amount, paid_amount, notes, status, created_at, updated_at

**Android 端 SaleOrderV2Dto：**
- 完全映射了后端所有字段

**整体评估：Android 端 V2 数据模型与后端 DTO 字段基本一一对应，模型层没有问题。**

## 审查发现的问题清单

### A. UI 与设计图不一致（高优先级）

| # | 问题 | 设计图 | 当前实现 | 影响 |
|---|------|--------|----------|------|
| A1 | 首页缺少销售趋势折线图 | 01.png 有折线图 | DashboardScreen 无图表 | 高 |
| A2 | 首页缺少待处理提醒列表 | 01.png 有待处理提醒 | DashboardScreen 无此区域 | 高 |
| A3 | 首页缺少低库存商品列表 | 01.png 有低库存预警 | DashboardScreen 仅显示数量 | 高 |
| A4 | AI助手页 KPI 行未展示 | 08.png 有 KPI 卡片 | AgentWorkbenchScreen 未调用 KpiRow | 中 |
| A5 | 报表页缺少图表 | 07.png 有折线图/饼图 | ReportScreen 无图表 | 高 |
| A6 | 报表页缺少库存流水/对账汇总 | 07.png 有多个报表页 | 仅一个 ReportScreen | 高 |
| A7 | 所有列表页缺少新增按钮 | 设计图有右下角 FAB | 当前实现无 FAB | 中 |
| A8 | 登录页缺少 Logo 和品牌展示 | 02.png 有 Logo | LoginScreen 纯表单 | 低 |
| A9 | 设置页缺少多个设置项 | 02.png 有账号安全/通知/语言等 | SettingsScreen 仅服务器+同步 | 中 |
| A10 | 商品列表缺少分类筛选 Chip | 03.png 有分类 Chip | ProductListScreen 有状态 Chip 但无分类 | 中 |

### B. 后端接口未在 Android 端使用（高优先级）

| # | 接口/功能 | 后端状态 | Android 端状态 | 差距 |
|---|-----------|----------|----------------|------|
| B1 | /v2/products | 已做 | API 定义存在，ViewModel 未调用 | 需接入 |
| B2 | /v2/customers | 已做 | API 定义存在，ViewModel 未调用 | 需接入 |
| B3 | /v2/suppliers | 已做 | API 定义存在，ViewModel 未调用 | 需接入 |
| B4 | /v2/sale-orders | 已做 | API 定义存在，ViewModel 未调用 | 需接入 |
| B5 | /v2/purchase-orders | 已做 | API 定义存在，ViewModel 未调用 | 需接入 |
| B6 | /v2/pay-orders | 已做 | API 定义存在，ViewModel 未调用 | 需接入 |
| B7 | /v2/accounts | 已做 | API 定义存在，ViewModel 未调用 | 需接入 |
| B8 | /v2/inventory/ledger | 已做 | API 定义存在，ViewModel 未调用 | 需接入 |
| B9 | /v2/agent/conversations | 待验证 | API 定义存在，ViewModel 未调用 | 需接入 |
| B10 | /v2/sync/* | 待验证 | API 定义存在，ViewModel 未调用 | 需接入 |
| B11 | /v2/media/assets | 待验证 | API 定义存在，ViewModel 未调用 | 需接入 |

### C. 数据库字段未在 Android 端使用（中优先级）

| # | 字段/实体 | 后端存在 | Android 端模型 | UI 展示 |
|---|-----------|----------|----------------|---------|
| C1 | Product.price_levels | 是 | ProductV2Dto 有 | 未在 UI 展示 |
| C2 | Product.default_supplier | 是 | ProductV2Dto 有 | 未在 UI 展示 |
| C3 | Product.supplier_relations | 是 | ProductV2Dto 有 | 未在 UI 展示 |
| C4 | Customer.level | 是 | CustomerV2Dto 有 | 未在 UI 展示 |
| C5 | Customer.balance | 是 | CustomerV2Dto 有 | 未在 UI 展示 |
| C6 | Customer.address | 是 | CustomerV2Dto 有 | 未在 UI 展示 |
| C7 | Customer.group_id/group_name | 是 | CustomerV2Dto 有 | 未在 UI 展示 |
| C8 | Supplier.balance | 是 | SupplierV2Dto 有 | 未在 UI 展示 |
| C9 | SaleOrder.subtotal_amount | 是 | SaleOrderV2Dto 有 | 未在 UI 展示 |
| C10 | SaleOrder.discount_amount | 是 | SaleOrderV2Dto 有 | 未在 UI 展示 |
| C11 | SaleOrder.paid_amount | 是 | SaleOrderV2Dto 有 | 未在 UI 展示 |
| C12 | Account.type/isDefault/sortOrder | 是 | AccountV2Dto 有 | 未在 UI 展示 |
| C13 | InventoryLedgerEntry.quantity_before/after | 是 | InventoryLedgerEntryV2Dto 有 | 未在 UI 展示 |
| C14 | BillFundLink.link_type | 是 | BillFundLinkV2Dto 有 | 未在 UI 展示 |

### D. 缺失页面（高优先级）

| # | 页面 | 设计图 | 说明 |
|---|------|--------|------|
| D1 | 商品详情页 | 03.png | 展示商品完整信息 |
| D2 | 编辑商品页 | 03.png | 商品创建/编辑表单 |
| D3 | 库存调整页 | 03.png | 调整商品库存 |
| D4 | 客户详情页 | 04.png | 展示客户完整信息 |
| D5 | 供应商详情页 | 04.png | 展示供应商完整信息 |
| D6 | 销售开单页 | 05.png | 创建销售订单 |
| D7 | 销售单详情页 | 05.png | 展示销售订单详情 |
| D8 | 收款页 | 05.png | 为销售单收款 |
| D9 | 采购开单页 | 06.png | 创建采购订单 |
| D10 | 采购单详情页 | 06.png | 展示采购订单详情 |
| D11 | 资金流水页 | 07.png | 财务流水列表 |
| D12 | AI问答页 | 08.png | 对话式 AI 问答 |
| D13 | 操作草稿页 | 08.png | 草稿列表 |
| D14 | 任务与通知页 | 08.png | 任务列表和通知 |

## 修复状态更新

### 已完成修复

| # | 问题 | 修复内容 | 状态 |
|---|------|----------|------|
| B1-B8 | 后端 V2 API 未在 Android 端使用 | 所有列表页 ViewModel 已接入真实 API | 已完成 |
| A1 | 首页缺少销售趋势折线图 | DashboardScreen 添加 7 天销售趋势柱状图 | 已完成 |
| A2 | 首页缺少待处理提醒列表 | DashboardScreen 添加待处理提醒卡片 | 已完成 |
| A3 | 首页缺少低库存商品列表 | DashboardScreen 添加低库存商品列表+进度条 | 已完成 |
| A4 | AI助手页 KPI 行未展示 | AgentWorkbenchScreen 添加 KpiRow 组件调用 | 已完成 |
| A5 | 报表页缺少图表 | ReportScreen 添加销售利润对比图+财务构成图 | 已完成 |
| A6 | 报表页缺少库存流水/对账汇总 | ReportScreen 已添加财务构成展示（完整报表页待后续拆分） | 部分完成 |
| A7 | 所有列表页缺少新增按钮 | Product/Sale/Customer/Supplier ListScreen 添加 FAB | 已完成 |
| A8 | 登录页缺少 Logo 和品牌展示 | LoginScreen 添加 Store 图标+品牌名称+副标题 | 已完成 |
| A9 | 设置页缺少多个设置项 | SettingsScreen 添加账号安全/通知设置/语言与地区 | 已完成 |
| A10 | 商品列表缺少分类筛选 Chip | ProductListScreen 添加分类筛选 Chip+ViewModel 支持 | 已完成 |
| C5 | Customer.balance 未在 UI 展示 | CustomerListScreen 已展示客户余额 | 已完成 |
| C8 | Supplier.balance 未在 UI 展示 | SupplierListScreen 已展示供应商应付余额 | 已完成 |

### 待修复（需新增页面）

| # | 页面 | 设计图 | 说明 |
|---|------|--------|------|
| D1 | 商品详情页 | 03.png | 展示商品完整信息 |
| D2 | 编辑商品页 | 03.png | 商品创建/编辑表单 |
| D3 | 库存调整页 | 03.png | 调整商品库存 |
| D4 | 客户详情页 | 04.png | 展示客户完整信息 |
| D5 | 供应商详情页 | 04.png | 展示供应商完整信息 |
| D6 | 销售开单页 | 05.png | 创建销售订单 |
| D7 | 销售单详情页 | 05.png | 展示销售订单详情 |
| D8 | 收款页 | 05.png | 为销售单收款 |
| D9 | 采购开单页 | 06.png | 创建采购订单 |
| D10 | 采购单详情页 | 06.png | 展示采购订单详情 |
| D11 | 资金流水页 | 07.png | 财务流水列表 |
| D12 | AI问答页 | 08.png | 对话式 AI 问答 |
| D13 | 操作草稿页 | 08.png | 草稿列表 |
| D14 | 任务与通知页 | 08.png | 任务列表和通知 |

## 验证步骤

1. 逐页对比设计图截图与当前实现
2. 检查每个 ViewModel 是否调用了 ZhihuijiV2Api 对应方法
3. 检查每个后端 DTO 字段是否在 Android 模型中有对应
4. 检查每个 Android 模型字段是否在 UI 中有展示
