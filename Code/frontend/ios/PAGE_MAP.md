# 智慧记 iOS 页面与接口地图

这份文档用于说明当前 iOS 原生端的页面结构、主要职责、权限边界和对应的后端接口族，方便后续继续开发或做验收核对。

## 1. 总体分层

- App 入口：`Code/frontend/ios/ZhihuijiIOS/ZhihuijiIOSApp.swift`
- 路由与会话：`Code/frontend/ios/ZhihuijiIOS/App/AppRouter.swift`、`Code/frontend/ios/ZhihuijiIOS/App/AppSession.swift`、`Code/frontend/ios/ZhihuijiIOS/App/AppEnvironment.swift`
- API 层：`Code/frontend/ios/ZhihuijiIOS/Core/API/*`
- 设计系统：`Code/frontend/ios/ZhihuijiIOS/Core/Design/*`
- 业务页面：`Code/frontend/ios/ZhihuijiIOS/Features/*`

## 2. 页面地图

### 顶级导航

- iOS 底部导航严格对齐当前 Android 移动端：`首页 / 单据 / 档案 / 报表 / 助手`。
- 库存、资金、设置不是底部顶级 tab；库存和资金保留为首页快捷入口、单据中心联动入口或业务详情入口，设置保留为各顶级页右上角入口与首页快捷入口，避免变成 Web PC 管理台式信息架构。
- `单据` tab 在账号具备 `sales:view`、`purchase:view`、`finance:view`、`inventory:view` 任一权限时可见，确保仓库/库存角色也能进入库存盘点和台账。
- iOS `PermissionPolicy.rolePermissions` 已按后端 `StoreAccessPolicy` 的 `OWNER / MANAGER / SALES / PURCHASING / WAREHOUSE / FINANCE / ASSISTANT` 权限集合核对；角色显示名保持后端语义，`ASSISTANT` 展示为 `AI/只读助理`。

### 登录与会话

- `Features/Auth/LoginView.swift`
- `Features/Auth/RegisterView.swift`
  - 登录手机号和密码
  - 手机号注册与验证码流程
  - 后端地址切换
  - 登录后写入 token 并进入门店同步

### 首页

- `Features/Dashboard/DashboardView.swift`
  - 经营总览
  - 销售、采购、资金、库存和风险摘要
  - 快捷入口到销售、采购、库存、资金、AI 和设置

### 单据中心

- `Features/DocumentsHomeView.swift`
  - 单据总入口
  - 汇聚销售、采购、资金和库存的高频操作

### 销售

- `Features/Sales/SalesListView.swift`
- `Features/Sales/SalesDetailView.swift`
- `Features/Sales/SalesEditView.swift`
- `Features/Sales/SalesPaymentView.swift`
- `Features/Sales/SalesReturnView.swift`
  - 销售单列表、详情、新建/编辑、收款、退货

### 采购

- `Features/Purchases/PurchaseListView.swift`
- `Features/Purchases/PurchaseDetailView.swift`
- `Features/Purchases/PurchaseEditView.swift`
- `Features/Purchases/PurchaseReceiptView.swift`
- `Features/Purchases/PurchaseReturnView.swift`
  - 采购单列表、详情、新建/编辑、入库、退货

### 档案

- `Features/ArchivesHomeView.swift`
- `Features/Archives/CustomerListView.swift`
- `Features/Archives/CustomerDetailView.swift`
- `Features/Archives/SupplierListView.swift`
- `Features/Archives/SupplierDetailView.swift`
- `Features/Products/ProductListView.swift`
- `Features/Products/ProductDetailView.swift`
- `Features/Products/ProductEditView.swift`
  - 商品档案、客户档案、供应商档案
  - 商品编辑支持扫码、价格层级、供应关系和媒体绑定

### 库存

- `Features/Inventory/InventorySnapshotView.swift`
- `Features/Inventory/InventoryAdjustView.swift`
- `Features/Inventory/InventoryLedgerView.swift`
  - 盘点快照、低库存、月度统计、库存调整、库存流水

### 资金

- `Features/Finance/FinanceRecordView.swift`
- `Features/Finance/PayOrderDetailView.swift`
- `Features/Finance/DailyExpenseView.swift`
- `Features/Finance/SupplierStatementView.swift`
  - 资金流水、日常支出、付款单工作台、供应商对账、现金调整、付款单状态流转
  - `finance:view` 可查看资金流水和现金调整记录；`finance:write` 才能新增日常流水、新建现金调整、删除现金调整记录、创建付款单或切换付款单状态。

### 报表

- `Features/Reports/ReportsView.swift`
  - 销售趋势
  - 商品分析
  - 客户分析
  - 风险与履约
  - CSV 导出与打印

### AI

- `Features/Agent/AgentChatView.swift`
- `Features/Agent/AgentWorkbenchView.swift`
- `Features/Agent/AgentDraftsView.swift`
- `Features/Agent/AgentTasksView.swift`
  - 会话列表、聊天流、运行轨迹、审计抽屉
  - 支持流式回答、停止生成、工作台、草稿管理和任务查看

### 设置

- `Features/Settings/SettingsView.swift`
- `Features/Settings/RoleAccessView.swift`
- `Features/Settings/StaffManagementView.swift`
- `Features/Settings/SyncImportView.swift`
- `Features/Settings/MediaAssetsView.swift`
- `Features/Settings/PlanningOverviewView.swift`
  - 员工管理
  - 权限可见性
  - 同步与导入
  - 媒体对象与绑定
  - 规划概览与后续功能入口

## 3. 接口族映射

### 认证与门店

- `POST /v2/auth/register`
- `POST /v2/auth/login`
- `POST /v2/auth/refresh`
- `POST /v2/auth/logout`
- `POST /v2/auth/verify-code`
- `GET /v2/auth/users/me`
- `GET /v2/stores/current`
- `GET /v2/stores/current/members`
- `POST /v2/stores/current/members`
- `PUT /v2/stores/current/members/{userId}`

### 销售

- `GET /v2/sale-orders`
- `GET /v2/sale-orders/{id}`
- `POST /v2/sale-orders`
- `GET /v2/sale-orders/{id}/payments`
- `POST /v2/sale-orders/{id}/payments`
- `GET /v2/sales-returns`
- `GET /v2/sales-returns/{id}`
- `GET /v2/sales-returns/by-order/{orderId}`
- `POST /v2/sales-returns`
- `PUT /v2/sales-returns/{id}/draft`
- `PUT /v2/sales-returns/{id}/confirm`
- `POST /v2/sales-returns/{id}/refunds`
- `PUT /v2/sales-returns/{id}/cancel`

### 采购

- `GET /v2/purchase-orders`
- `GET /v2/purchase-orders/{id}`
- `POST /v2/purchase-orders`
- `GET /v2/purchase-receipts`
- `GET /v2/purchase-receipts/{id}`
- `GET /v2/purchase-receipts/by-order/{orderId}`
- `POST /v2/purchase-receipts`
- `PUT /v2/purchase-receipts/{id}/draft`
- `PUT /v2/purchase-receipts/{id}/confirm`
- `PUT /v2/purchase-receipts/{id}/cancel`
- `GET /v2/purchase-returns`
- `GET /v2/purchase-returns/{id}`
- `GET /v2/purchase-returns/by-order/{orderId}`
- `POST /v2/purchase-returns`
- `PUT /v2/purchase-returns/{id}/draft`
- `PUT /v2/purchase-returns/{id}/confirm`
- `POST /v2/purchase-returns/{id}/refunds`
- `PUT /v2/purchase-returns/{id}/cancel`

### 商品、库存、档案

- `GET /v2/products`
- `GET /v2/products/{id}`
- `POST /v2/products`
- `PUT /v2/products/{id}`
- `GET /v2/product-categories`
- `GET /v2/product-units`
- `GET /v2/product-price-levels`
- `GET /v2/customers`
- `GET /v2/customers/{id}`
- `GET /v2/customer-groups`
- `GET /v2/suppliers`
- `GET /v2/suppliers/{id}`
- `GET /v2/supplier-groups`
- `GET /v2/inventory/snapshots`
- `GET /v2/inventory/monthly-stats`
- `GET /v2/inventory/ledger`
- `POST /v2/inventory/ledger`
- `POST /v2/inventory/snapshots`

### 资金

- `GET /v2/finance-records`
- `POST /v2/finance-records`
- `GET /v2/cash-change-records`
- `POST /v2/cash-change-records`
- `DELETE /v2/cash-change-records/{id}`
- `GET /v2/reports/reconciliation-summary`
- `GET /v2/pay-orders`
- `GET /v2/pay-orders/{id}`
- `POST /v2/pay-orders`
- `PUT /v2/pay-orders/{id}/status`

### 报表

- `GET /v2/reports/sales-summary`
- `GET /v2/reports/sales-trend`
- `GET /v2/reports/profit-summary`
- `GET /v2/reports/cashflow-summary`
- `GET /v2/reports/reconciliation-summary`
- `GET /v2/reports/top-products`
- `GET /v2/reports/profit-by-products`
- `GET /v2/reports/profit-by-customers`
- `GET /v2/reports/customer-sales`
- `GET /v2/reports/top-receivable-customers`
- `GET /v2/reports/refund-records`
- `GET /v2/reports/stock-out-records`
- `GET /v2/reports/inventory-flow`
- `GET /v2/reports/low-stock-products`

### AI

- `GET /v2/agent/workbench`
- `GET /v2/agent/conversations`
- `GET /v2/agent/conversations/{id}`
- `POST /v2/agent/conversations`
- `PUT /v2/agent/conversations/{id}`
- `DELETE /v2/agent/conversations/{id}`
- `GET /v2/agent/conversations/{id}/messages`
- `POST /v2/agent/conversations/{id}/messages`
- `GET /v2/agent/drafts`
- `POST /v2/agent/drafts`
- `PUT /v2/agent/drafts/{id}`
- `DELETE /v2/agent/drafts/{id}`
- `POST /v2/agent/chat`
- `POST /v2/agent/chat/stream`
- `POST /v2/agent/runs/{runId}/cancel`
- `GET /v2/agent/runs/{runId}/audit`
- `GET /v2/agent/tasks`
- `GET /v2/agent/notifications`
- `POST /v2/agent/notifications/{id}/read`

### 同步与媒体

- `GET /v2/sync/health`
- `GET /v2/sync/cursor`
- `GET /v2/sync/cursor/{clientId}`
- `POST /v2/sync/cursor/ack`
- `POST /v2/sync/pull`
- `POST /v2/sync/pull-apply`
- `POST /v2/sync/pull-apply-ack`
- `POST /v2/sync/upload`
- `POST /v2/sync/changes`
- `POST /v2/import-jobs`
- `GET /v2/import-jobs`
- `GET /v2/import-jobs/{id}`
- `POST /v2/import-jobs/{id}/retry`
- `POST /v2/import-jobs/{id}/cancel`
- `GET /v2/media/assets`
- `POST /v2/media/assets`
- `POST /v2/media/assets/upload`
- `DELETE /v2/media/assets/{id}`
- `GET /v2/media/bindings`
- `POST /v2/media/bindings`
- `DELETE /v2/media/bindings/{id}`

## 4. 当前验证状态

- 代码级验证：`xcrun swiftc -typecheck` 对 `Code/frontend/ios/ZhihuijiIOS` 全量 App 源码通过
- 临时模块验证：`swiftc -emit-module -enable-testing -module-name ZhihuijiIOS` 通过
- 工程引用：`Code/frontend/ios/ZhihuijiIOS.xcodeproj/project.pbxproj` 与现有 Swift 文件已对齐
- 工程文件验证：`plutil -lint Code/frontend/ios/ZhihuijiIOS.xcodeproj/project.pbxproj` 通过
- 收口验证：`InlinePermissionAuditTests` 已覆盖 `Code/frontend/ios/ZhihuijiIOS` 源码，当前不存在 `session.hasPermission(...)` 形式的 inline 权限判断
- 环境限制：当前机器缺少完整 Xcode / `xcodebuild` / `iphonesimulator` SDK，且 `XCTest` 模块不可用，因此还不能给出真正的测试目标或模拟器运行证据

## 5. 后续建议

- 如果继续做功能，优先收口：
  - 细化表单校验
  - 补充少量页面空态与错误态
  - 在完整 Xcode 环境中补真实运行验收
