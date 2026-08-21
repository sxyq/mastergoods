# Android API 第一阶段报告：Agent C

日期：2026-08-22

状态：首阶段已收束。源码、调用链和当前 8220 无认证路由探针已登记；所有新增接口 case 结果均为 `Blocked`。未开始 Agent 真实测试，等待 B 的历史交接单和主 Agent 明确指令。

## 范围与结果

- Android API 入口：`ZhihuijiV2Api.kt`（V2 163 个方法）和 `ZhihuijiApi.kt`（V1/兼容接口 64 个方法）。
- 方法计数：V2 为 GET 71、POST 44、PUT 30、DELETE 18；V1 为 GET 34、POST 18、PUT 9、DELETE 3。
- 逐方法 inventory 共 227 行；其中 105 个 GET 方法映射到 90 个去重路由，当前 8220 探针全部返回 HTTP 403；122 个非 GET 方法因没有认证材料且可能改变数据，未发送请求。
- 当前目标配置来自 Android `SettingsStore.DEFAULT_BASE_URL`：`https://zhj-api.sxyq27.online/`。目标记录为 `8.220.206.9`；本轮 curl 经过网络出口，响应 `remote_ip=198.18.0.183`，不能据此认定后端进程版本与本地源码一致。
- 响应头显示 `nginx/1.24.0 (Ubuntu)`。本机无 `adb`，无 8220/18080 监听服务；没有真机、模拟器或 APK 运行结论。
- 没有发送 Authorization、Cookie、refresh token、请求体、multipart 或写入方法；没有创建业务夹具。

## 调用链摘要

| 业务域 | Network | Repository | ViewModel / Screen | 后端入口 |
| --- | --- | --- | --- | --- |
| 登录、用户、门店 | `ZhihuijiApi` 的 V1 auth 与 `ZhihuijiV2Api` 的 stores | `AuthRepository` | `AuthViewModel` -> `LoginScreen` / `RegisterScreen`；`SettingsViewModel`、`StaffManagementViewModel` -> 设置页 | `V2AuthController`、`V2StoreController` |
| 商品、库存 | V2 products、categories、units、price levels、supplier relations、inventory | `ProductV2Repository`、`InventoryV2Repository` | 产品列表/详情/编辑、库存流水、库存快照及对应 ViewModel | `V2ProductController`、产品子 Controller、`V2InventoryController` |
| 客户、供应商 | V2 partners、groups、contacts | `CustomerV2Repository`、`SupplierV2Repository` | 客户和供应商列表、详情、编辑、联系人、对账页 | `V2CustomerController`、`V2SupplierController` 及 partner Controller |
| 销售、付款 | V2 sale-orders、sales-returns、pay-orders | `SaleOrderV2Repository`、`SalesReturnV2Repository`、`PayOrderV2Repository` | 销售列表/详情/编辑、付款、销售退货、付款列表 | `V2SaleOrderController`、`V2SalesReturnController`、`V2PayOrderController` |
| 采购 | V2 purchase-orders、purchase-receipts、purchase-returns | `PurchaseOrderV2Repository`、`PurchaseReceiptV2Repository`、`PurchaseReturnV2Repository` | 采购列表/详情/编辑、收货、采购退货 | 对应 V2 purchase Controller |
| 财务、报表 | V2 finance-records、accounts、transfers、bill-fund-links、reports | `FinanceRepository`、`FinanceV2Repository`、`AccountV2Repository`、`ReportRepository` | 财务、账户、资金转账、报表 ViewModel -> 对应 Screen | `V2FinanceRecordController`、`V2AccountController`、`V2ReportController` 等 |
| 同步、导入 | V2 sync、import-jobs | `SyncV2Repository`、`InventoryV2Repository`；`SyncWorker` 调度 | `DashboardViewModel`、`SettingsViewModel`、后台 `SyncWorker` | `V2SyncController`、`V2ImportJobController` |
| Agent、媒体 | V2 conversations/messages/drafts/workbench/tasks/notifications/chat/images/media；SSE 单独走 `AgentSseClient` | `AgentV2Repository`、`MediaV2Repository`、`AgentPendingMessageRepository`、本地 `AgentAuditRepository` | `AgentWorkbenchViewModel`、`AgentChatViewModel`、`DraftListViewModel`、`TaskNotificationViewModel` -> Agent screens | `V2AgentController`、`V2MediaController` |

逐接口的 method、path、`@Query`/`@Path`/`@Body`/`@Part`、Kotlin response type、Repository 引用、ViewModel/Screen 引用和后端 Controller 已写入 `20-android-endpoint-inventory.tsv`。其中 ViewModel/Screen 引用包含注入和类型引用，直接 API 调用位置以 `data_call_sites` 列为准。

## 契约核对摘要

1. **认证与刷新**：`AuthInterceptor` 对匿名 V1 auth 路径跳过 Bearer，其余请求从 `SessionStore` 添加 `Authorization: Bearer`。`TokenAuthenticator` 在 401 后调用 `v1/auth/refresh`，最多沿 prior response 重试一次；当前 `AuthRepository` 也使用 V1 auth。后端另有 `V2AuthController`，本轮未替换客户端认证版本。
2. **响应与空响应**：Android `ApiResponse<T>` 对应后端 `ApiResponse(code,message,data,timestamp)`。`safeApiCall` 只把 `code=0` 且 `data != null` 转成成功；`safeApiUnitCall` 允许 `code=0` 且 `data=null`，适合删除接口。删除类接口的空响应仅完成源码核对，未做认证 HTTP 验证。
3. **错误处理**：客户端为 401、403、404 和 5xx 提供固定提示；409、422 等状态没有专门分支，主要保留服务端 message。后端 `GlobalExceptionHandler` 覆盖 400、401、403、404、405、422、500、503 等映射。本轮只有无认证 403，其他状态均没有被声称为已验证。
4. **字段与类型**：Android ID 主要使用 `Long`，金额和数量使用 `Double`；模型对 snake_case 字段使用 `@SerialName`，例如 `conversation_id`、`operation_id`、`created_at`。后端 V2 DTO 使用 Jackson snake case 命名策略。由于全部请求停在鉴权层，未执行真实 JSON 解码成功路径。
5. **owner/store context**：Android 请求不在业务 body 中自行填充 owner；后端通过 Bearer 会话和 `@RequireStorePermission` / 当前 owner 服务取得 owner/store。没有有效 session 和 store，本轮无法验证成功响应、跨店拒绝、空列表或 owner 边界。
6. **分页**：后端多个列表接受 `page`/`size`，例如 products、customers、suppliers、orders、finance、Agent；相应 Android 方法有一部分没有暴露这两个参数。已确认的硬差异是 `inventoryLedgerV2`、`inventorySnapshotsV2`、`inventoryMonthlyStatsV2` 声明返回 `List<...>`，而 `V2InventoryController` 返回 `Page<...>` 并要求/支持分页参数。该差异记录为源码契约风险，未以 HTTP 结果定性。
7. **幂等与重复提交**：同步模型包含 `operation_id`，导入模型包含 `idempotency_key`；后端付款单创建 DTO 和服务使用 `idempotencyKey`。Android `CreatePayOrderV2Request` 当前未声明 `idempotency_key`，付款单重复提交能力存在客户端字段缺口候选。真实重复提交、事务结果和清理本轮均为 `Blocked`。
8. **SSE**：`ZhihuijiV2Api` 没有 Retrofit `chat/stream` 方法；`AgentV2Repository.chatStream` 编码 `AgentChatRequest(stream=true)`，由 `AgentSseClient` 手工 POST `/v2/agent/chat/stream`，解析 `data:` 行和终止事件，并在连接建立阶段重试。后端 `V2AgentController` 确有 `text/event-stream` 路由。本轮未发送 SSE 请求，事件顺序和客户端状态转换没有实测结论。

## HTTP 与台账

- HTTP 原始请求模板、路由 sweep、响应摘要和返回体文件：`testing/.artifacts/2026-08-22-android-api-C/http/`。
- Android API、Controller、调用点和 response type inventory：`testing/.artifacts/2026-08-22-android-api-C/20-android-endpoint-inventory.tsv`。
- 当前阶段逐接口台账：`testing/安卓/功能测试/live_execution_ledger.csv`，新增 `AND-API-C-001` 至 `AND-API-C-227`，全部为 `Blocked`。
- 每条新增记录均声明无凭据、无 owner/store context、无业务写入；历史 154 设备/API 结果不计入本轮。

## 未完成与边界

- 认证成功后的 2xx、空响应、401/403/404/409/422、分页实际 payload、重复提交/idempotency、owner/store 隔离、SSE 顺序和客户端状态转换均待有授权的当前 8220 session 后执行。
- Android 单元/契约/compile 检查未作为本轮 HTTP 通过依据；本报告不声称真机通过，也不声称当前服务与本地源码版本相同。
- 第二阶段必须先收到 B 历史交接单及主 Agent 明确指令；本轮没有读取或修改 `testing/Agent` 台账。

本轮未修改 `Code/frontend/android`、`Code/backend`、配置、迁移、部署对象及业务数据，未触碰 `data/server-backups/`、`data/server-exports/`。
