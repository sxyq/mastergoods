# 后端与安卓端连通性测试计划

## 摘要

对 `master-goods` 后端 Spring Boot API 与 `master-goods-android` 客户端进行全面的接口连通性测试。测试范围覆盖 API 端点映射一致性、DTO 字段兼容性、HTTP 方法匹配、以及 Repository 层调用链完整性。目标是发现所有可能导致运行时 404/400/500 错误的接口不匹配问题。

---

## 当前状态分析

### 1. API 端点映射总览

| 模块 | 后端 Controller | Android API (ZhihuijiV2Api) | 状态 |
|------|----------------|----------------------------|------|
| 商品 | V2ProductController | `productsV2`, `productV2`, `createProductV2`, `updateProductV2`, `deleteProductV2`, `lowStockProductsV2` | 完整 |
| 商品分类 | - | `productCategoriesV2` 等 | 需确认后端 Controller |
| 商品单位 | - | `productUnitsV2` 等 | 需确认后端 Controller |
| 客户 | V2CustomerController | `customersV2`, `customerV2`, `createCustomerV2`, `updateCustomerV2`, `deleteCustomerV2` | 完整 |
| 客户分组 | - | `customerGroupsV2` 等 | 需确认后端 Controller |
| 供应商 | V2SupplierController | `suppliersV2`, `supplierV2`, `createSupplierV2`, `updateSupplierV2`, `deleteSupplierV2` | 完整 |
| 销售订单 | V2SaleOrderController | `saleOrdersV2`, `saleOrderV2`, `createSaleOrderV2`, `updateSaleOrderDraftV2`, `confirmSaleOrderV2`, `addSaleOrderPaymentV2`, `saleOrderPaymentsV2`, `updateSaleOrderStatusV2`, `cancelSaleOrderV2` | 完整 |
| 采购订单 | V2PurchaseOrderController | `purchaseOrdersV2`, `purchaseOrderV2`, `createPurchaseOrderV2`, `updatePurchaseOrderV2`, `deletePurchaseOrderV2` | **后端缺少 PUT/DELETE** |
| 付款单 | V2PayOrderController | `payOrdersV2`, `payOrderV2`, `createPayOrderV2`, `updatePayOrderStatusV2` | 完整 |
| 采购收货 | V2PurchaseReceiptController | `purchaseReceiptsV2`, `purchaseReceiptV2`, `createPurchaseReceiptV2`, `updatePurchaseReceiptDraftV2`, `confirmPurchaseReceiptV2`, `cancelPurchaseReceiptV2` | 完整 |
| 销售退货 | V2SalesReturnController | `salesReturnsV2`, `salesReturnV2`, `createSalesReturnV2`, `updateSalesReturnDraftV2`, `confirmSalesReturnV2`, `addSalesReturnRefundV2`, `cancelSalesReturnV2` | 完整 |
| 库存 | V2InventoryController | `inventoryLedgerV2`, `inventoryLedgerBySourceV2`, `createInventoryLedgerEntryV2`, `inventorySnapshotsV2`, `createInventorySnapshotV2`, `inventoryMonthlyStatsV2` | 完整 |
| 资金账户 | V2AccountController | `accountsV2`, `accountV2`, `createAccountV2`, `updateAccountV2`, `deleteAccountV2` | 完整 |
| 资金转账 | V2AccountTransferController | `accountTransfersV2`, `accountTransferV2`, `createAccountTransferV2` | 完整 |
| 单据资金关联 | V2BillFundLinkController | `billFundLinksV2`, `createBillFundLinkV2`, `deleteBillFundLinkV2` | 完整 |
| AI 助手 | V2AgentController | `agentConversationsV2`, `agentConversationV2`, `createAgentConversationV2`, `updateAgentConversationV2`, `deleteAgentConversationV2`, `agentMessagesV2`, `createAgentMessageV2`, `agentDraftsV2`, `createAgentDraftV2`, `updateAgentDraftV2`, `deleteAgentDraftV2` | 完整 |
| 媒体 | V2MediaController | `mediaAssetsV2`, `mediaAssetV2`, `createMediaAssetV2`, `deleteMediaAssetV2`, `mediaBindingsV2`, `createMediaBindingV2`, `deleteMediaBindingV2` | 完整 |
| 同步 | V2SyncController | `syncHealthV2`, `syncCursorV2`, `acknowledgeSyncCursorV2`, `uploadSyncChangesV2`, `pullSyncChangesV2` | 完整 |
| 导入任务 | V2ImportJobController | `importJobsV2`, `importJobV2`, `createImportJobV2`, `retryImportJobV2`, `cancelImportJobV2` | 完整 |

### 2. 已发现的不匹配问题

#### 问题 1：采购订单后端缺少 PUT / DELETE 端点
- **后端**: [V2PurchaseOrderController.java](file:///Users/sunyiyang/Desktop/Project/master-goods/src/main/java/com/zhihuiji/backend/api/controller/v2/V2PurchaseOrderController.java) 只有 `POST`, `GET /{id}`, `GET` (list)
- **Android**: [ZhihuijiV2Api.kt L330-334](file:///Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/core/network/src/main/java/com/zhihuiji/core/network/ZhihuijiV2Api.kt#L330-L334) 定义了 `updatePurchaseOrderV2` (`PUT /v2/purchase-orders/{id}`) 和 `deletePurchaseOrderV2` (`DELETE /v2/purchase-orders/{id}`)
- **影响**: 调用更新/删除采购订单会返回 404
- **修复**: 后端需添加 `@PutMapping("/{id}")` 和 `@DeleteMapping("/{id}")`

#### 问题 2：销售订单草稿更新路径冗余
- **后端**: [V2SaleOrderController.java L67](file:///Users/sunyiyang/Desktop/Project/master-goods/src/main/java/com/zhihuiji/backend/api/controller/v2/V2SaleOrderController.java#L67) `@PutMapping({"/{id}", "/{id}/draft"})` 同时匹配两个路径
- **Android**: [ZhihuijiV2Api.kt L270-274](file:///Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/core/network/src/main/java/com/zhihuiji/core/network/ZhihuijiV2Api.kt#L270-L274) 分别定义了 `updateSaleOrderDraftV2` (`PUT /{id}`) 和 `updateSaleOrderDraftAliasV2` (`PUT /{id}/draft`)
- **影响**: 功能正常但 Android 端有冗余方法，可合并

#### 问题 3：V1 API 与 V2 API 混用
- **Android**: [ZhihuijiApi.kt](file:///Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/core/network/src/main/java/com/zhihuiji/core/network/ZhihuijiApi.kt) 仍保留大量 V1 端点（`products`, `customers`, `saleOrders` 等）
- **问题**: 部分 Screen 可能仍在调用 V1 API，而 V1 后端 Controller 可能已废弃或字段不一致
- **需检查**: 哪些 Screen/Repository 仍在使用 `ZhihuijiApi` 而非 `ZhihuijiV2Api`

#### 问题 4：DTO 字段类型潜在不匹配
- **SaleOrder**: 后端 `StatusRequest` 是 `record StatusRequest(Integer status)`，Android 使用 `com.zhihuiji.core.model.StatusRequest` —— 需确认字段名一致
- **PurchaseOrder**: 后端 `CreateRequest` 包含 `supplierId`, `supplierName`, `items`, `notes`, `status`；Android `CreatePurchaseOrderV2Request` 字段一致
- **Product**: 需对比 [V2ProductDtos.java](file:///Users/sunyiyang/Desktop/Project/master-goods/src/main/java/com/zhihuiji/backend/api/dto/v2/product/V2ProductDtos.java) 与 Android `ProductV2Dto`

#### 问题 5：分页参数
- **后端**: 多个 list 接口支持 `page` 和 `size` 参数（如 [V2ProductController.java L34-35](file:///Users/sunyiyang/Desktop/Project/master-goods/src/main/java/com/zhihuiji/backend/api/controller/v2/V2ProductController.java#L34-L35)）
- **Android**: [ZhihuijiV2Api.kt L84-89](file:///Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/core/network/src/main/java/com/zhihuiji/core/network/ZhihuijiV2Api.kt#L84-L89) `productsV2` 未传 `page`/`size`
- **影响**: 默认返回全量数据，大数据量时性能差

---

## 测试方案

### 阶段一：静态代码对比测试（无需运行后端）

#### 1.1 生成完整 API 映射表
- 遍历所有后端 V2 Controller，提取 `@XxxMapping` 注解的路径、方法、参数名
- 遍历 Android `ZhihuijiV2Api`，提取所有 Retrofit 注解
- 生成 CSV/表格对比，标记缺失项

#### 1.2 DTO 字段对比
- 对核心实体（Product, Customer, Supplier, SaleOrder, PurchaseOrder, PayOrder）逐字段对比
- 检查 `@SerialName` / `@JsonNaming` 是否匹配后端 snake_case
- 检查可空类型（`?`）是否一致

#### 1.3 Repository 调用链检查
- 检查所有 `*Repository.kt` 文件，确认是否调用正确的 API 接口
- 标记仍在使用 V1 API (`ZhihuijiApi`) 的 Repository

### 阶段二：动态连通性测试（需启动后端）

#### 2.1 创建 API 测试用例
- 使用 Android 单元测试 + MockWebServer，验证每个 API 接口的请求格式
- 或使用后端集成测试，验证每个 Android 调用链

#### 2.2 端到端测试路径
1. **登录** → 获取 Token
2. **商品 CRUD** → 创建 → 查询列表 → 查询详情 → 更新 → 删除
3. **客户 CRUD** → 同上
4. **销售开单** → 创建草稿 → 确认 → 查询 → 添加收款 → 取消
5. **采购开单** → 创建 → 查询（更新/删除需后端修复后测试）
6. **库存查询** → 流水 / 快照 / 月统计

### 阶段三：修复验证

- 修复后端缺失的 PUT/DELETE 端点
- 修复 Android 端 V1/V2 混用问题
- 重新运行测试验证

---

## 实施顺序

1. **阶段一** — 静态对比，生成问题清单
2. **阶段二** — 动态测试，验证问题清单
3. **阶段三** — 修复问题，回归测试

---

## 验证步骤

1. 确保 `./gradlew :app:assembleDebug` 编译成功
2. 确保后端 `./mvnw test` 通过
3. 运行 Android 单元测试覆盖所有 Repository 方法
4. 运行端到端测试覆盖核心业务流程

---

## 假设与决策

- **假设**: 后端 V1 Controller 已废弃，Android 应全面迁移到 V2
- **决策**: 优先修复后端缺失端点（采购订单 PUT/DELETE），而非修改 Android 调用
- **决策**: 分页参数作为增强功能，不阻塞核心连通性测试
