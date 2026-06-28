# 三端函数级性能与安全审计台账

- 创建日期：`2026-06-24`
- 覆盖范围：后端 `src/main/java` / `src/test/java`，Android `master-goods-android`，Web `web/src` 与关键前端入口配置。
- 明细台账：[`performance-security-function-audit-2026-06-23.csv`](./performance-security-function-audit-2026-06-23.csv)
- 生成方式：读取 `.codegraph/codegraph.db` 的文件/符号索引，并补充当前文件系统中未被索引但属于三端范围的代码/配置文件。

## 审计目标

1. 访问并标记三端每个文件与每个函数/方法/组件/路由。
2. 在不改变功能、不改变 UI 语义的前提下精简代码。
3. 提升后端查询性能、Android 手机运行与滑动流畅度、Web 交互性能。
4. 检查全链路安全风险，包括鉴权、权限、数据隔离、配置、上传、AI/SSE、缓存与日志。

## 状态字段

- `PENDING`：尚未访问。
- `VISITED`：已经阅读源代码，确认职责和调用边界。
- `REVIEWED`：已经记录性能/安全结论。
- `OPTIMIZED`：已经实施低风险优化。
- `VERIFIED`：优化或结论已有测试、构建、运行或静态证据支撑。
- `BLOCKED`：需要设备、环境、账号、线上配置或用户决策才能继续。

## 当前基线

| 项目 | 数量 |
|---|---:|
| 台账总行数 | 7274 |
| 文件行 | 726 |
| 函数/方法/组件/路由等符号行 | 6548 |
| backend 行 | 3869 |
| android 行 | 2743 |
| web 行 | 662 |

## 执行规则

1. 每次开始前先看 `git status --short`，保护已有未提交改动。
2. 每访问一个文件或函数，就在 CSV 中更新 `visit_status` 与 `visited_at`。
3. 每发现风险，写入 `performance_risk` 或 `security_risk`，并标注优先级。
4. 只做能证明功能/UI 不变的低风险优化；涉及合同或 UI 行为变化时先暂停说明。
5. 后端优化优先用测试或定向请求证明；Android 优先编译、单测、必要时用 adb/gfxinfo/Perfetto；Web 优先 `npm run build` 与页面运行检查。

## 首轮优先访问顺序

1. 后端安全与 owner/RBAC：`SecurityConfig`、`TokenAuthenticationFilter`、`CurrentOwnerService`、`StorePermissionInterceptor`。
2. 后端高查询成本路径：报表、库存、同步、导入、AI/SSE。
3. Android 滑动/重组热点：主导航、Dashboard、Reports、Agent、长列表与 Markdown/result block 渲染。
4. Web API/client/RBAC 与大页面：`client.ts`、session store、AppLayout、Planning/Agent/Stitch 页面。

## 进度日志

| 时间 | 范围 | 结果 |
|---|---|---|
| 2026-06-24 | 建立台账 | 已生成函数级 CSV 基线，所有条目默认 `PENDING`。 |
| 2026-06-24 | Android 局部提速 | `SupplierStatementViewModel.loadStatement` 改为并发拉取采购单与付款单，`CustomerReceivableSummary` 改为按 `customers` 记忆应收汇总，等待下一轮定向编译验证。 |
| 2026-06-24 | Android 参考数据并发 | `ProductEditViewModel.loadReferenceData` 改为并发拉取分类和单位，并合并状态更新，保留表单语义不变。 |
| 2026-06-24 | Web 大页过滤减压 | `StitchScreenPage` 预计算行搜索文本，减少搜索/状态过滤时的重复字符串拼接；`vite build` 通过。 |
| 2026-06-24 | 后端报表限流收敛 | `ReportService.topProducts`、`profitByProducts`、`profitByCustomers` 去掉重复终端限流，`inventoryFlow` 改为预留容量；Gradle 后端验证受本地 daemon socket 限制。 |
| 2026-06-24 | Web 基础层访问完成 | `web/src/shared/api/client.ts`、`config.ts`、`contracts.ts`、`app/stores/session.ts`、`app/layouts/AppLayout.vue`、`app/router/stitch-screens.ts`、`entities/screen/live-screen-data.ts`、`shared/utils/business.ts` 已读并回写台账。 |
| 2026-06-24 | Web 热路径小优化 | `client.ts` 的大 JSON 数字保留和查询构造改为更轻量的分配模式；`session.ts` 的共享 computed 下沉为模块单例；`live-screen-data.ts` 缓存格式化器并用 Map 代替任务通知线性查找；`vite build` 通过。 |
| 2026-06-24 | 后端鉴权缓存收紧 | `StoreAccessPolicy` 改为角色/权限缓存查找，`CurrentOwnerService` 改为一次解析角色后复用权限集合，`TokenAuthenticationFilter` 复用固定 `ROLE_USER` 授权对象；后端定向测试通过。 |
| 2026-06-24 | 后端安全边界复核 | `SecurityConfig`、`StorePermissionInterceptor`、`CurrentOwnerServiceTest`、`StorePermissionInterceptorTest` 已读并完成权限/鉴权路径验证。 |
| 2026-06-24 | Web SSE 解析减分配 | `web/src/shared/api/agent-stream.ts` 的 `preserveUnsafeIntegers` 改为 chunk builder，减少长 SSE 事件字符串拼接；`web` 构建通过。 |
| 2026-06-24 | Web Dashboard 趋势图压缩 | `web/src/pages/dashboard/DashboardPage.vue` 的趋势几何和库存统计改为更少遍历/分配；`web` 构建通过。 |
| 2026-06-24 | Web Agent 解析减分配 | `web/src/pages/agent/AgentPage.vue` 的 markdown 段落解析改为预编译正则，减少重渲染时的 regex 分配；`web` 构建通过。 |
| 2026-06-24 | Android 同步负载减压 | `master-goods-android/data/sync/src/main/java/com/zhihuiji/data/sync/SyncV2Repository.kt` 缓存空 `JsonObject`，减少同步 payload 解析分配；`data:sync:compileDebugKotlin` 通过。 |
| 2026-06-24 | 后端 AI 工具规划缓存 | `V2AgentAiService` 的工具输入 Map 改为常量复用，`parseToolPlan` / `inferToolPlan` 改为有序集合去重；定向后端测试通过。 |
| 2026-06-24 | Android Agent ViewModel 压缩 | `AgentChatViewModel` 的消息更新改为尾部优先查找，加载消息合并改为预估容量集合，结果块构造改为预分配；`feature:agent compile` 通过。 |
| 2026-06-24 | Android Dashboard ViewModel 复核 | `DashboardViewModel` 的加载、趋势和提醒聚合路径已读并回写台账；`feature:dashboard compile` 通过。 |
| 2026-06-24 | Android SSE / Agent Repository 复核 | `AgentSseClient`、`AgentV2Repository`、`AgentMarkdownText` 已读并回写台账。 |
| 2026-06-24 | Android Agent / Dashboard Screen 复核 | `AgentChatScreen`、`DashboardScreen` 的主渲染与滚动/图表路径已读并回写台账。 |
| 2026-06-24 | Android Agent / Dashboard Screen 扩展复核 | `AgentChatScreen`、`DashboardScreen` 的结果块、轨迹、提醒和图表尾部函数继续回写台账。 |
| 2026-06-24 | 后端 AI 测试证据补齐 | `V2AgentAiServiceTest` 已读并作为这轮 AI 热路径优化的回归证据。 |
| 2026-06-24 | 采购退货 ViewModel 压缩 | `PurchaseReturnViewModel` 的来源单/退货单查找改为预计算 map；`feature:purchases compile` 通过。 |
| 2026-06-24 | V2 API / Agent / Report 读查 | `ZhihuijiApi.kt`、`ZhihuijiV2Api.kt`、`AgentPage.vue`、`V2AgentConversationService`、`V2PurchaseReturnService`、`V2AgentController`、`V2ReportController`、`ReportController` 已读并回写台账。 |
| 2026-06-24 | Android sales ViewModel / Web reports安全修正 | `SaleOrderEditViewModel` 复用 item 更新 helper 并合并保存请求列表；`ReportsPage` 的趋势计算改为本地快照复用，CSV 导出增加表格公式注入防护；`feature:sales compile` 与 `web build` 通过。 |
| 2026-06-24 | Android报表页轻量提速 / Web规划页审计 | `ReportScreen` 的曲线最大值计算去掉临时列表分配；`PlanningOverviewPage` 函数级已读并回写台账；`feature:reports compile` 通过。 |
| 2026-06-24 | Android报表/收款状态层审计 | `ReportViewModel` 改用缓存枚举数组避免 repeated values() 分配；`PaymentViewModel` 与 `PaymentScreen` 已读并回写台账；`feature:reports compile` 通过。
| 2026-06-24 | 后端库存/媒体/报表回写与验证 | `ReportService` 收紧趋势/流水/客户销售集合分配，`V2MediaService` 先校验 `assetType` 再落盘，`V2InventoryService` 与两份测试台账已补齐；后端定向测试通过。 |
| 2026-06-24 | 后端关键词归一化 / 导入状态校验 | `ProductService`、`PayOrderService`、`PurchaseOrderService`、`SaleOrderService`、`FinanceRecordService` 的列表查询收掉前后空格，`FinanceRecordService.create` 去掉重复 category 归一化，`V2ImportJobService` 的 status 过滤改为 `Locale.ROOT`，并补齐新增私有辅助函数台账；`ProductServiceTest` 与 `V2ImportJobServiceTest` 定向通过。 |
| 2026-06-24 | Web 采购尾页 / 归档页减压 | `PurchaseReceiptPage` 用 id `Map` 缓存来源单与入库单定位；`PurchaseOrderListPage` 把状态统计收敛为单次 `reduce`；`PurchaseOrderEditPage` 用 `Map` 缓存供应商/商品查找；`ProductArchivePage` 与 `PartnerArchivePage` 合并摘要遍历，并把 `Intl` 格式器提升为模块级复用；`ForbiddenPage` 已读并回写台账。 |
| 2026-06-24 | Android 档案/聊天热路径减压 | `ArchivesScreen` 把标签从纯字符串改为带 key/searchHint 的 spec，去掉页面内重复 `indexOf`；`AgentChatScreen` 把消息尾部扫描包进 `remember(messages)`，减少无关重组的逆向遍历；`app:compileDebugKotlin` 与 `feature:agent:compileDebugKotlin` 通过。 |
| 2026-06-24 | Android 壳页审阅补齐 | `MainScreen`、`DocumentsScreen`、`ReportScreen` 已补齐到函数级阅读台账，当前先记录为已访问/已评审，后续若发现更清晰的热点再继续做定向优化。 |
| 2026-06-24 | Android ViewModel 轻量减压 | `InventoryLedgerViewModel` 的入/出库汇总改为单次 fold；`ProductEditViewModel` 的参考数据并发加载已再次核对并补入台账；`feature:products compileDebugKotlin` 通过。 |
| 2026-06-24 | 后端 v2 列表响应减配 | `V2ProductService`、`V2CustomerService`、`V2SupplierService`、`V2PurchaseReceiptService`、`V2SalesReturnService` 的高频列表/详情响应改为预分配显式循环，去掉部分 `stream().toList()` 和 `forEach` 分配；对应 v2 定向测试通过。 |
| 2026-06-24 | 后端管理端授权收紧 / 权限码缓存 | `SecurityConfig` 的 `/v1/admin/**` 判定补上匿名身份排除，避免匿名会话误判为已认证；`StoreAccessPolicy.permissionCodes` 改为按角色缓存权限码列表，减少高频授权查询分配。 |
| 2026-06-24 | Android 公共格式化器精简 | `MoneyFormatter`、`TimeFormatter` 抽出共享格式化入口，减少重复的线程本地格式器访问与分支；保持显示结果不变。 |
| 2026-06-24 | Web 过滤与标签归一化收敛 | `business.ts` 复用文本归一化 helper；`PurchaseReturnPage`、`SalesReturnPage`、`PayOrderDetailPage` 复用筛选局部变量，减少重复 trim / 时间边界计算。 |
| 2026-06-24 | 验证通过 | `web build`、Android `:core:common:compileDebugKotlin` + `:app:compileDebugKotlin`、后端 `StoreAccessPolicyTest` + `V2StoreServiceTest` 通过。 |
| 2026-06-24 | Android 启动/安全守卫与认证解析收敛 | `MainActivity` 高刷新选择改为单遍扫描；`RuntimeSecurityGuard`、`SignatureIntegrityChecker` 去掉中间集合/运行时封装；`AuthController` 与 `V2AuthController` 复用 bearer 解析助手。 |
| 2026-06-24 | 验证通过 | 后端 `ApiValidationTest` + `V2StoreControllerPermissionTest` 通过；Android `:app:compileDebugKotlin` 通过。 |
| 2026-06-24 | 后端导入 worker / 媒体服务收敛 | `V2MediaService` 的列表输出改为预分配循环、`uploadFile` 保持先校验再落盘；`V2ImportJobWorkerService` 已复核其任务恢复/抢占/执行路径。 |
| 2026-06-24 | 验证通过 | 后端 `ApiValidationTest` + `V2MediaServiceTest` + `V2ImportJobWorkerServiceTest` 通过；Android `MainActivityLaunchExtrasTest` + `:app:testDebugUnitTest` 通过。 |
| 2026-06-24 | Web AppLayout / 后端 v2 列表再压缩 | `AppLayout` 改为单次遍历完成权限过滤、搜索过滤和分组；`V2AccountService`、`V2AgentConversationService` 的列表响应改为预分配循环，去掉 `stream().toList()` / 反转中间集合；`web build` 与后端 `V2AccountServiceTest` / `V2AgentConversationServiceTest` 通过。 |
| 2026-06-24 | Web 路由解析查表化 / App 根入口复核 | `routes.ts` 的页面组件解析改为静态查表，去掉长 `if` 链；`App.vue` 已读并回写台账；`web build` 通过。 |
| 2026-06-24 | 财务流水控制器与 Android 根入口复核 | `V2FinanceRecordController` 的列表响应改为预分配循环；`FinanceRecordService` 继续保持单次分类归一化；`ZhihuijiApp` 已读并回写台账。 |
| 2026-06-24 | 库存服务源明细列表减分配 | `V2InventoryService.listLedgerBySource` 改为预分配循环，去掉 `stream().toList()`；库存控制器继续维持薄封装并完成复核。 |
| 2026-06-24 | 验证通过 | 后端 `ProductServiceTest` 通过；库存服务本轮改动的主工程编译确认可过。 |
| 2026-06-24 | 单据资金关联列表去 N+1 | `V2BillFundLinkService` 的列表响应改为一次批量加载账户名并预分配响应集合，去掉逐条账户查询和 `stream().toList()`。 |
| 2026-06-24 | 验证通过 | 后端 `ProductServiceTest` 通过；`V2BillFundLinkService` 的批量账户名加载改动编译可过。 |
| 2026-06-24 | 验证通过 | 后端 `ProductServiceTest` 再次通过；当前库存/资金关联相关改动保持可编译。 |
| 2026-06-24 | 验证通过 | 后端 `ProductServiceTest` 再次通过；`V2BillFundLinkService` 这轮 N+1 收敛保持可编译。 |
| 2026-06-24 | 后端转账/找零热路径回写 | `V2AccountTransferService` 改为批量账户名装配并复用已加载账户名，`V2CashChangeRecordService` 改为先过滤后批量取账户名，避免列表页和创建响应的额外查询。 |
| 2026-06-24 | 后端商品单位/分类/价格层级/分组补齐 | `V2ProductUnitService`、`V2ProductCategoryService`、`V2ProductPriceLevelService`、`V2PartnerGroupService` 的文件/函数级台账已补齐；列表与 ID-map 路径统一为预分配循环或批量查表。 |
| 2026-06-24 | 验证通过 | 后端 `V2AccountTransferServiceTest`、`V2CashChangeRecordServiceTest` 通过；先前同组的 `V2ProductCategoryServiceTest`、`V2ProductPriceLevelServiceTest`、`V2PartnerGroupServiceTest` 也已通过。 |
| 2026-06-24 | 后端基础异常与路由壳层审计 | `ZhihuijiBackendApplication`、`ApiResponse`、`BusinessException`、`GlobalExceptionHandler`、`PaginationUtils`、`ParseUtils`、`IdGenerator`、`PartnerTypes`、`V2AuthController`、`V2FinanceRecordController`、`V2InventoryController`、`V2ReportController` 已读并回写台账；`GlobalExceptionHandler.handleValidation` 去掉 stream 管道。 |
| 2026-06-24 | 验证通过 | `ApiValidationTest` 通过，确认异常处理和鉴权缺省路径保持稳定。 |
| 2026-06-24 | 后端 owner / security 热路径收敛 | `CurrentOwnerService.requirePermissions` 先判空再解析访问上下文，`resolveCurrentAccess` 改为显式 membership 分支；`SecurityConfig` 预解析 CORS origin patterns 并复用统一认证判定；`StoreAccessPolicy` 与 `TokenAuthenticationFilter` 已复核并同步台账。 |
| 2026-06-24 | 验证通过 | 后端 `CurrentOwnerServiceTest`、`StoreAccessPolicyTest`、`AdminControllerTest` 通过，确认 owner / security 热路径改动保持行为稳定。 |
| 2026-06-24 | 后端 v2 控制器薄壳复核 | `V2AuthController`、`V2FinanceRecordController`、`V2InventoryController`、`V2ReportController` 已读并同步台账；这一批主要是请求转发/响应拼装层，未继续扩语义。 |
| 2026-06-24 | 后端 admin / customer 控制器复核 | `AdminController`、`CustomerController` 已读并同步台账；`/v1/admin/agent/smoke` 维持 `410 Gone` 语义，客户 CRUD 保持薄封装。 |
| 2026-06-24 | 后端 v2 客户/供应商控制器复核 | `V2CustomerController`、`V2SupplierController` 已读并同步台账；两者均为薄 CRUD 包装，继续保持仅做参数透传与分页切片。 |
| 2026-06-24 | 后端 v1 资金/订单控制器压缩 | `FinanceRecordController`、`PayOrderController`、`PurchaseOrderController`、`SaleOrderController` 的列表与响应映射改为预分配循环；保留原有校验与业务语义。 |
| 2026-06-24 | 后端报表服务热路径压缩 | `ReportService` 的趋势、退款、库存流、客户销售、应收与低库存列表改为显式循环和预分配集合，减少 stream/collector 开销；`ReportServiceTest` 与控制器回归通过。 |
| 2026-06-24 | 后端基础控制器/服务复核 | `AuthController`、`ProductController`、`ReportController`、`SupplierController`、`ProductService`、`PayOrderService`、`PurchaseOrderService`、`SaleOrderService`、`FinanceRecordService` 已读并同步台账；这批以薄封装与既有规范化逻辑为主。 |
| 2026-06-24 | 后端 v2 付款/采购/销售/同步壳层压缩 | `V2SyncController` 的 upload/pull 映射、`V2PayOrderService` 的列表响应、`V2PurchaseOrderService` 与 `V2SaleOrderService` 的响应组装改为显式循环和预分配集合；对应 v2 测试通过。 |
| 2026-06-24 | 后端 v2 付款/采购/销售控制器复核 | `V2PayOrderController`、`V2PurchaseOrderController`、`V2SaleOrderController` 已读并同步台账；它们保持 thin wrapper 形态。 |
| 2026-06-24 | 后端 v2 同步主干复核 | `V2SyncService` 已完整读完并回写台账；同步收集/上传路径本身已使用显式循环和预分配缓存，没有额外 stream 热点。 |
| 2026-06-24 | 后端 v2 账户/资金关联复核 | `V2AccountService`、`V2BillFundLinkService` 已读并同步台账；两者的列表/聚合路径已是预分配循环与批量查询风格。 |
| 2026-06-24 | 后端 v2 客户/供应商列表压缩 | `V2CustomerService`、`V2SupplierService` 的列表响应从 `forEach` 收敛为显式循环，继续保持批量分组查表。 |
| 2026-06-24 | 后端导入任务服务复核 | `V2ImportJobService`、`V2ImportJobWorkerService` 已读并同步台账；导入列表、重试、抢占和执行路径均保持顺序控制，没有额外 stream 开销。 |
| 2026-06-24 | 后端 v2 门店/商品供应商/商品维度回写 | `V2StoreService`、`V2ProductSupplierRelationService`、`V2ProductService`、`V2ProductPriceLevelService` 的剩余热路径已回写；门店成员计数改为批量查表，商品供应商关系改为分组循环，商品响应上下文与价格层级快照继续收敛到预分配集合。 |
| 2026-06-24 | 后端 v2 收货/退货/库存/媒体回写 | `V2PurchaseReceiptService`、`V2SalesReturnService`、`V2InventoryService`、`V2MediaService` 的文件/函数级审计已补齐；收货与退货列表改为批量装配明细，库存列表改为分页页对象，媒体列表与绑定改为显式循环。 |
| 2026-06-24 | 验证通过 | 上一轮定向后端测试通过：`V2StoreServiceTest`、`V2ProductSupplierRelationServiceTest`、`V2ProductServiceTest`、`V2ProductPriceLevelServiceTest`、`V2InventoryServiceTest`、`V2MediaServiceTest`、`V2PurchaseReceiptServiceTest`、`V2SalesReturnServiceTest`、`V2FinanceInventoryControllerTest`。 |
| 2026-06-24 | Android Dashboard 局部提速与验证 | `DashboardScreen` 的趋势总计、轴标签、日期快捷项、提醒列表和整数字体格式路径改为复用局部缓存与预分配集合；`feature:dashboard compileDebugKotlin` 通过。 |
| 2026-06-24 | Web 设置页静态卡片提速与验证 | `SettingsOverviewPage` 的静态卡片目录移出 `computed`，权限判定签名收紧为只读数组，去掉了不必要的数组拷贝；`web build` 通过。 |
| 2026-06-24 | Web 轻组件复核 | `PageEmptyState` 与 `PageStatusBanner` 已读并回写台账；两者均为薄展示壳，没有额外热路径或安全边界变化。 |
| 2026-06-24 | Web Dashboard 热页回写 | `DashboardPage` 的库存统计与趋势几何已改为单遍处理并减少中间分配；主组件、路由和主要辅助函数已回写台账，`web build` 通过。 |
| 2026-06-24 | Web 单据概览与采购详情提速 | `DocumentsOverviewPage` 的静态模块目录移出 `computed`，`PurchaseOrderDetailPage` 的已入库数量改为预计算 map；`web build` 通过。 |
| 2026-06-24 | Web 销售退货与角色权限回写 | `SalesReturnPage` 的草稿单统计与订单过滤改为缓存与单遍循环，`RoleAccessPage` 的成员列表排序去掉额外数组复制；`web build` 通过。 |
| 2026-06-24 | Web 采购退货页提速与验证 | `PurchaseReturnPage` 的草稿单统计与订单过滤改为缓存与单遍循环；`web build` 通过。 |
| 2026-06-24 | Web 报表页热路径与导出回写 | `ReportsPage` 清空区块错误改为直接遍历，客户利润行去掉随机 key，CSV 导出保留语义但减少 DOM 抖动；`web build` 通过。 |
| 2026-06-24 | Web StitchScreen 单遍过滤与列源收敛 | `StitchScreenPage` 的可见行过滤改为单遍循环，列定义收敛到单一来源；`web build` 通过。 |
| 2026-06-24 | Web 登录页安全边界复核 | `LoginPage` 已读并回写台账；仅保留登录和演示入口，未引入额外热路径。 |
| 2026-06-24 | Web 数据库页导入列表排序前移 | `DatabasePage` 的导入任务排序移到数据写回阶段，避免每次渲染重复排序；`web build` 通过。 |
| 2026-06-24 | Web 付款单详情页复核 | `PayOrderDetailPage` 已读并回写台账；目前主要是列表加载、字典加载和状态流转，未引入额外热路径。 |
| 2026-06-24 | Web 资金流水页账户概览缓存 | `FinanceRecordPage` 的账户概览从模板切片改为计算属性缓存；`web build` 通过。 |
| 2026-06-24 | Web 库存调整页流水预览缓存 | `InventoryAdjustPage` 的最近库存流水改为计算属性缓存；`web build` 通过。 |
| 2026-06-24 | Web 销售收款页索引化 / 规划页复核 | `SalesPaymentPage` 的选中订单改为 map 查找并去掉列表拷贝，`PlanningOverviewPage` 与 `ProductEditPage` 已读并回写台账；`web build` 通过。 |
| 2026-06-24 | Web 库存快照页行模型缓存 | `InventorySnapshotPage` 的商品与快照关联改为行模型缓存，减少模板中的重复 map 查找；`web build` 通过。 |
| 2026-06-24 | Web 产品编辑复核 / 库存流水搜索缓存 | `ProductEditPage` 已读并回写台账，`ProductLedgerPage` 的商品搜索改为预计算 searchText；`web build` 通过。 |
| 2026-06-24 | Web 日常支出页支付方式缓存 | `DailyExpensePage` 的支出预览支付方式改为计算属性缓存；`web build` 通过。 |
| 2026-06-24 | Web 销售单列表状态汇总单遍化 | `SalesOrderListPage` 的状态标签改为单次 `reduce` 汇总；`web build` 通过。 |
| 2026-06-24 | Web 销售单编辑行模型索引化 | `SalesOrderEditPage` 的客户/商品查找改为索引表，行项目改为预计算行模型，提交前检查收敛为单次计数；`web build` 通过。 |
| 2026-06-24 | Web/API 客户端与 Android 同步补强 | `web/src/shared/utils/camelize.ts` 改为预分配循环，`web/src/shared/api/client.ts` 的请求头、query 与多组 request body 映射改为共享批量 helper，`SyncV2Repository` 的 pulled-change 应用改为显式循环和缓存空 JSON 对象。 |
| 2026-06-24 | 验证通过 | `web build` 与 Android `:data:sync:compileDebugKotlin` 通过，确认 Web 客户端 helper 与 Android 同步回放改动可编译。 |
| 2026-06-24 | Web Agent / 后端 V2AgentAiService / Android Agent 继续压缩 | `web/src/pages/agent/AgentPage.vue` 的消息加载、通知更新、Markdown 片段解析和结果块状态派生改为预分配循环；`V2AgentAiService` 的低库存、商品概览、应收应付、经营概览、销售/采购/付款/流水响应改为显式循环与批量辅助函数；Android `AgentChatViewModel`、`ResultBlockRenderer`、`AgentMarkdownText` 的消息合并、图表预处理和 Markdown 快速判定也收敛为循环实现。 |
| 2026-06-24 | 验证通过 | `web build`、后端 `V2AgentAiServiceTest`，以及 Android `:feature:agent:compileDebugKotlin` 与 `:core:network:compileDebugKotlin` 通过。 |
| 2026-06-24 | Android Agent 实时更新与 SSE 再压缩 | `AgentChatViewModel.updateAssistantMessage`、`List<ToolCallRecord>.updateToolCall`、`List<ToolCallRecord>.closeOpenToolCalls` 改为倒序/显式循环；`AgentSseClient.chatStream` 与 `retryWithBackoff` 复用协程上下文引用，减少每行 SSE 读取和重试阶段的上下文查找。 |
| 2026-06-24 | 验证通过 | Android `:feature:agent:compileDebugKotlin` 与 `:core:network:compileDebugKotlin` 通过，确认 Agent 实时更新与 SSE 改动可编译。 |
| 2026-06-24 | Web live-screen-data 全函数回写 | `web/src/entities/screen/live-screen-data.ts` 的列表映射、状态 token、资金/库存/单据聚合与通知查找改成单遍循环、预分配数组和共享格式化器；已同步回写函数级 CSV 台账。 |
| 2026-06-24 | Android AgentChatScreen 尾部扫描收敛 | `AgentChatScreen` 入口改为一次倒序扫描同时找流式消息与最后一条助手消息；`displayParts`、`latestVisibleToolCall`、`latestActiveToolCall`、`latestFinishedToolCallCandidate` 改为显式倒序循环，`emptyChatPills` 改为静态常量复用；`feature:agent compile` 通过。 |
| 2026-06-24 | Android PurchaseReturnViewModel 列表与选中项收敛 | `PurchaseReturnViewModel` 的刷新与回填改成预分配集合、批量 ID 查表和一次性列表构造；`updateAt`、`upsertById`、`findById`、`toDraftLines` 改成循环实现；`feature:purchases compile` 通过。 |
| 2026-06-24 | 后端 Sync/Legacy 导入与同步收口 | `SyncService` 的同步 payload 改为统一 helper 和预分配 map，`LegacySQLiteImportService` 的 SQLite 导入循环复用 ResultSet 局部变量并减少重复读取；`V2AgentController`、`ImportJobEntity`、`V2AgentAiServiceTest` 已回写为访问完成。 |
| 2026-06-24 | 验证状态 | `./gradlew test` 过程里命中了仓库既有的 `core:datastore:testDebugUnitTest` 失败（`SettingsStoreTest.normalizeBaseUrl_debugBuildKeeps117Host`），与本轮后端 Java 改动无直接对应；`SyncService` 与 `LegacySQLiteImportService` 的编译未见新增错误。 |
| 2026-06-24 | 后端实体与 SSE 取消测试审计 | `AgentRunAuditEntity`、`ProductEntity`、`PurchaseOrderEntity` 与 Android `AgentSseClientCancellationTest` 已完成逐函数阅读并回写台账；这些文件未发现值得在当前约束下改动的低风险性能点。 |
| 2026-06-24 | 后端 Agent 会话服务审计 | `V2AgentConversationService` 已完成逐函数阅读；列表与消息/草稿路径已是预分配集合和逆序遍历形态，当前未追加代码改动。 |
| 2026-06-24 | Android 入口/安全守卫回写 | `MainAccessViewModel` 的权限上下文刷新改为单次状态写入，`SignatureIntegrityChecker` 的 SHA-256 十六进制转换改为固定缓冲区；`AppNavGraph`、`ZhihuijiApp`、`RuntimeSecurityGuard` 已读并回写台账，`app:compileDebugKotlin` 通过。 |
| 2026-06-24 | Android Agent 轻量收缩 | `AgentResponseProvenance` 复用规则摘要状态集合，`TaskNotificationViewModel` 的通知和任务列表装配改为预分配循环；`feature:agent compileDebugKotlin` 通过。 |
| 2026-06-24 | Android 草稿页回拉删除 | `DraftListViewModel` 的草稿装配改为预分配集合，归档时直接复用已加载草稿快照，不再二次拉取草稿列表；JSON 预览字段提取改为缓存正则，`feature:agent compileDebugKotlin` 通过。 |
| 2026-06-24 | Android 通知页重组减压 | `TaskNotificationScreen` 的“全部已读”展示条件改为记忆化扫描结果，避免每次重组重复遍历通知列表；`feature:agent compileDebugKotlin` 通过。 |
| 2026-06-24 | Android 运行轨迹头部减压 | `RunTracePanel` 的状态头部改为单次遍历工具调用，并把规则摘要状态集合提升为共享常量；`feature:agent compileDebugKotlin` 通过。 |
| 2026-06-24 | Android Agent 首页审阅补齐 | `AgentWorkbenchScreen`、`AgentWorkbenchViewModel` 已完成逐函数阅读并回写台账；首页同步路径保持单次状态写入，没有额外热循环。 |
| 2026-06-24 | 后端枚举/同步壳层收缩 | `OrderStatus`、`PayOrderStatus`、`PaymentType`、`PurchaseOrderStatus`、`PurchaseReceiptStatus`、`PurchaseReturnStatus`、`SalesReturnStatus` 的代码查找改为循环查表，`SyncController.upload` 改为预分配列表映射；`compileJava compileTestJava` 通过。 |
| 2026-06-24 | Web 壳层审阅补齐 | `App.vue`、`AppLayout.vue`、`routes.ts` 已完成函数级阅读并回写台账；这些入口层保持静态或单次分组逻辑，没有额外热路径。 |
| 2026-06-24 | Web session 状态层收缩 | `session.ts` 的本地成员索引、导出成员视图和持久化读取改为单次循环与缓存源判断；`web build` 通过。 |
| 2026-06-24 | Web 页面模型构建减压 | `page-models.ts` 的数据库表收集改为单次遍历加 seen 集合，避免 `flatMap + Set + Array.from` 中间分配；`web build` 通过。 |
| 2026-06-24 | Web API 契约层审阅 | `contracts.ts` 已完成函数级阅读；路由契约本身已用预计算缓存承载，当前没有再降分配的低风险空间。 |
| 2026-06-24 | Web 屏幕清单审阅 | `stitch-screens.ts` 已完成函数级阅读；该文件主要是静态屏幕清单，当前没有可观的低风险收缩点。 |
| 2026-06-24 | Android 构建基线与 backdrop 绘制链路复核 | `gradle.properties`、`master-goods-android/gradle.properties`、`app/build.gradle.kts` 以及 `backdrop` 模块的 `Backdrop` / `BackdropEffectScope` / `RuntimeShaderCache` / `InverseLayerScope` / `LayerRecorder` / `Outline` / `Shaders` / `ShapeProvider` / `backdrops/*` 已完成逐文件阅读并回写台账；本轮未改业务代码。 |
| 2026-06-24 | 后端用户列表与联系人列表收紧 | `AdminService.listUsers` 改为预分配用户 ID / 活跃会话映射 / 返回列表，`V2PartnerContactService.list` 与 `clearPrimary` 改为显式循环并复用单次时间戳；`compileJava compileTestJava` 通过。 |
| 2026-06-24 | Web admin-console 事件委托收敛 | `src/main/resources/static/admin-console/app.js` 的用户表编辑入口改为事件委托，避免每次重绘后逐按钮绑定监听；`node --check` 通过。 |
| 2026-06-24 | 后端/前端壳层复核 | `settings.gradle.kts`、`PaymentStatus`、`V2AccountController`、`V2AccountTransferController`、`V2BillFundLinkController`、`V2CashChangeRecordController` 以及 Web `index.html` / `package.json` / `main.ts` / `config.ts` / `style.css` / `tsconfig*` / `vite.config.ts` / admin-console 静态壳层已读并回写台账。 |
| 2026-06-24 | Android backdrop 渲染辅助复核 | `backdrop` 模块的 `effects/*`、`highlight/*`、`shadow/*`、`RoundedRectangularShape`、`backdrop` / `benchmark` 构建与 manifest 文件已完成阅读并回写台账；当前未追加新的渲染逻辑改动。 |
| 2026-06-24 | 后端单据 DTO 复核 | `FinanceRecordDto`、`PayOrderDto`、`ProductAdjustStockRequest`、`PurchaseOrderDto`、`SaleOrderDto`、`SupplierDto` 已读并回写台账；这些载体层没有可观的低风险收缩点。 |
| 2026-06-24 | Android 店员列表并发化 / 本地库索引补齐 | `StaffManagementViewModel.refreshUsers` 改为并发拉取当前门店与店员列表并复用一次关键词修剪；`SaleOrderEntity`、`SaleOrderDao.replaceOrderGraphs`、`ZhihuijiDatabase` 与 `DatabaseModule` 补上 `sale_orders` 的排序/过滤索引和 Room 迁移，减少本地查询与排序开销。 |
| 2026-06-24 | 验证通过 | Android `:core:database:compileDebugKotlin`、`:feature:settings:compileDebugKotlin`、`:app:compileDebugKotlin` 通过，确认本轮本地库迁移与店员列表并发化可编译。 |
| 2026-06-24 | Android 采购编辑状态层减分配 | `PurchaseOrderEditViewModel` 的订单行加载、增删改和保存请求组装改为预分配 helper；新增 helper 已补入函数级 CSV 台账，`AuthRepository` 与 `ReportRepository` 薄仓储层已读并回写。 |
| 2026-06-24 | 验证通过 | Android `:feature:purchases:compileDebugKotlin` 通过，确认采购编辑状态层改动可编译。 |
| 2026-06-24 | 后端 owner 查询索引补齐 | 新增 `V24__owner_scoped_query_indexes.sql`，为 `bill_fund_links`、`partner_contacts`、`product_supplier_relations`、`inventory_adjustments` 的 owner-scoped 同步、列表和时间范围查询补复合索引；对应实体与 repository 函数级台账已回写。 |
| 2026-06-24 | 验证通过 | 后端 `compileJava compileTestJava` 与 `ZhihuijiBackendApplicationTests` 通过，确认新增 Flyway 迁移可被 SpringBootTest 应用上下文接受。 |
| 2026-06-24 | Android 销售退货页重组减压 | `SalesReturnScreen` 将其他退货单过滤和底部栏退货总数汇总改为 `remember` 缓存；`StockAdjustScreen` 与 `PurchaseReceiptScreen` 已完整读完并回写函数级台账。 |
| 2026-06-24 | 验证通过 | Android `:feature:sales:compileDebugKotlin` 通过，确认销售退货页 Compose 改动可编译。 |
| 2026-06-24 | Android 库存流水页减分配 / 客商仓储复核 | `InventoryLedgerScreen` 的行 tone 与辅助文本改为字段级 `remember`，辅助文本组装从 `listOfNotNull`/`joinToString` 改为 `buildString`；`CustomerV2Repository` 与 `SupplierV2Repository` 已读并回写函数级台账。 |
| 2026-06-24 | 验证通过 | Android `:data:customer:compileDebugKotlin`、`:data:supplier:compileDebugKotlin`、`:feature:products:compileDebugKotlin` 通过。 |
| 2026-06-24 | Android 采购编辑页与店员管理页再减分配 | `PurchaseOrderEditScreen` 的商品占位图与底部汇总改为记忆化缓存；`StaffManagementScreen` 的统计、更新时间显示和权限标签遍历改为单遍/缓存实现，UI 与文案保持不变。 |
| 2026-06-24 | 验证通过 | Android `:feature:purchases:compileDebugKotlin`、`:feature:settings:compileDebugKotlin` 通过。 |
| 2026-06-24 | Android 支出页 / 报表 / 供应商对账再减分配 | `DailyExpenseScreen` 预分块类别和支付方式网格并缓存底部渐变；`ReportViewModel` 与 `SupplierStatementViewModel` 改为更少临时集合的汇总路径；`SessionStore` 与 `SettingsStore` 已读并回写函数级台账。 |
| 2026-06-24 | 验证通过 | Android `:feature:finance:compileDebugKotlin`、`:feature:reports:compileDebugKotlin`、`:feature:suppliers:compileDebugKotlin` 通过。 |
| 2026-06-24 | Android 商品编辑页 / 网络模块 / 产品模型回写 | `ProductEditScreen` 的主操作渐变与批发价字段状态做了轻量收缩；`NetworkModule` 去掉重复 base-url normalize 并清理无用 import；`ProductV2Models` 已读并回写函数级台账。 |
| 2026-06-24 | 验证通过 | Android `:feature:products:compileDebugKotlin` 与 `:core:network:compileDebugKotlin` 通过。 |
| 2026-06-24 | Android 薄仓储 / benchmark / 契约测试审读 | `FinanceV2Repository`、`SaleOrderV2Repository`、`BenchmarkFlows`、`ZhihuijiV2ApiContractTest` 已完成函数级阅读并回写台账；本批未修改代码。 |
| 2026-06-24 | Android 库存盘点页 / 设置同步页减分配 | `InventorySnapshotScreen` 的快照附加文本和头像首字符做了记忆化；`SettingsViewModel` 的同步错误汇总去掉临时结果列表，保留现有状态语义。 |
| 2026-06-24 | 验证通过 | Android `:feature:products:compileDebugKotlin`、`:feature:settings:compileDebugKotlin` 通过。 |
| 2026-06-24 | Android 商品列表 ViewModel 一次遍历化 | `ProductListViewModel` 的商品映射与库存状态过滤合并为单次预分配遍历，避免先 map 再 filter 的双遍扫描。 |
| 2026-06-24 | Android 日常支出 / 采购收货 / 收款 ViewModel 审读 | `DailyExpenseViewModel`、`PurchaseReceiptViewModel`、`PaymentViewModel` 已完成函数级阅读并回写台账；本批未修改代码。 |
| 2026-06-24 | Android 销售列表 / 销售退货 / 缓存层再压缩 | `SaleOrderListViewModel` 改为单次预分配遍历；`SalesReturnViewModel` 去掉排序后 `map` 与重复选择扫描；`MemoryCache` 细化过期判断与前缀失效路径。 |
| 2026-06-24 | 验证通过 | Android `:feature:sales:compileDebugKotlin` 与 `:core:network:compileDebugKotlin` 通过。 |
| 2026-06-24 | Android 设置页字符串派生缓存 | `SettingsScreen` 的顶部和账号卡片缓存了派生标签与头像首字符，减少纯字符串分支和重复提取。 |
| 2026-06-24 | 验证通过 | Android `:feature:settings:compileDebugKotlin` 通过。 |
| 2026-06-24 | Android 库存快照 ViewModel 单遍统计 | `InventorySnapshotViewModel` 改为单次遍历构建盘点列表，并在同一遍里缓存已盘、盘盈、盘亏统计，减少重组时重复扫描；`feature:products compile` 通过。 |
| 2026-06-24 | Android 金额格式化 / v2 DTO 复核 | `MoneyFormatter`、`MoneyFormatterTest`、`AgentV2Models`、`PurchaseReturnV2Models`、`SyncV2Models` 已读并回写台账，确认均为纯格式化/纯序列化模型，本批未修改业务行为。 |
| 2026-06-24 | Android 采购退货仓储复核 | `PurchaseReturnV2Repository` 已读并回写台账，确认仅透传 V2 API，没有额外查询、缓存或映射逻辑。 |
| 2026-06-24 | Android 客户 / 财务 / 供应商对账减分配 | `CustomerListViewModel` 预计算应收数值与欠款标记，`CustomerListScreen` 改用 numeric summary 和 ViewModel 背书的欠款布尔值；`FinanceViewModel` 复用单次 filter 快照并预分配 item 列表；`SupplierStatementViewModel` 把采购/付款总额累计合并进同一遍交易组装，`feature:customers` / `feature:finance` / `feature:suppliers` 编译通过。 |
| 2026-06-24 | Android 供应商列表减分配 | `SupplierListViewModel` 预计算 payable 数值与联系人拆分字段，`SupplierListScreen` 改用 ViewModel 背书字段并移除卡片侧字符串解析；`feature:suppliers compile` 通过。 |
| 2026-06-24 | Android 网络/代理测试复核 | `NetworkConfigTest`、`SafeApiCallBehaviorTest`、`AgentV2RepositoryTest` 已读并回写台账，确认覆盖 baseUrl 归一化、safeApiCall 语义与 Agent 仓储参数透传，本批未修改代码。 |
| 2026-06-24 | Android 采购/付款列表与详情格式化减分配 | `PayOrderListViewModel`、`PurchaseOrderListViewModel` 改为预分配映射并统一使用共享金额格式化；`PurchaseOrderDetailViewModel`、`SaleOrderDetailViewModel` 改用共享日期/金额格式化 helper，减少详情页重复 formatter 构造；`feature:payments` / `feature:purchases` / `feature:sales` 编译通过。 |
| 2026-06-24 | Android 供应商对账页记忆化 / 共享金额格式化 | `SupplierStatementScreen` 记忆化 footer 和 summary 金额文本、活跃状态和各项汇总文本，`formatStatementCurrency` 切到共享金额 formatter；`feature:suppliers compile` 通过。 |
| 2026-06-24 | Android AuthViewModel 登录/退出收口与验证 | `AuthViewModel` 的 login/register 复用共享 `launchAuth`，logout 用 `try/finally` 保证 loading 清理；`feature:auth compileDebugKotlin` 通过。 |
| 2026-06-25 | Android auth 屏幕共享样式常量 | `LoginScreen` 与 `RegisterScreen` 把背景刷与圆角形状提到文件级常量，减少重组时的对象创建；`feature:auth compileDebugKotlin` 通过。 |
| 2026-06-25 | Android data/auth 配置与清理器复核 | `data/auth` 的 `build.gradle.kts`、`AndroidManifest.xml` 与 `LocalDataCleaner` 已读并回写台账；本轮未改业务逻辑。 |
| 2026-06-25 | Android customers 列表/详情/编辑回写 | `CustomerListViewModel` 改为单次预分配循环，`CustomerDetailScreen` 统一用共享金额格式化并记忆化余额文本，`CustomerEditScreen` 的表单状态按加载数据键控同步；`feature:customers compileDebugKotlin` 通过。 |
| 2026-06-25 | Android customers 目录与视图模型复核 | `feature/customers` 的 `build.gradle.kts`、`AndroidManifest.xml`、`CustomerListScreen`、`CustomerDetailViewModel`、`CustomerEditViewModel` 已读并回写台账。 |
| 2026-06-25 | Android finance 视图模型与列表回写 | `DailyExpenseViewModel` 的 `amountText` 改用共享 `MoneyFormatter`，`FinanceRecordListScreen` 的 meta 文本改为直分支拼接，`FinanceViewModel` 复用过滤快照、预分配列表并抽出 `buildFinanceTitle`；函数级 CSV 台账已同步。 |
| 2026-06-25 | Android finance 支出页 / 详情页减分配 | `DailyExpenseScreen` 复用文件级 `roundedCardShape`，减少多个玻璃卡片、选择器与附件位的重复 shape 分配；`FinanceRecordDetailScreen` 记忆化记录查找并缓存金额颜色，减少详情页重组时的重复扫描。 |
| 2026-06-25 | 验证通过 | Android `:feature:finance:compileDebugKotlin` 通过。 |
| 2026-06-25 | 验证通过 | Android `:feature:finance:compileDebugKotlin` 通过，确认最新 finance 屏幕减分配改动可编译。 |
| 2026-06-28 | Android Agent Markdown 链接 scheme 收口 | `AgentMarkdownText` 将 markdown 链接白名单收紧为 `http/https/mailto/tel/www`，未知 scheme 改为普通文本显示，并复用 `MarkdownLinkStyles` 降低重复样式分配；`AgentMarkdownTextParserTest` 增加 `javascript:` 回归用例，`:feature:agent:testDebugUnitTest` 通过。 |
| 2026-06-28 | 后端 Customer/Product 事务边界补齐 | `CustomerService::delete` 与 `ProductService::create/delete` 补上 `@Transactional`，并在 `ProductServiceTest` 中加入反射回归校验，确保可变操作持续保持事务边界；`./gradlew test --tests com.zhihuiji.backend.application.service.ProductServiceTest` 通过。 |
| 2026-06-28 | Web EntityId 查询参数收口 | `ProductEditPage`、`FinanceRecordPage`、`InventoryAdjustPage`、`ProductLedgerPage` 改为 `readQueryId` + `sameEntityId` 路径，`client.ts` 的商品 / 库存流水相关 path/query/body ID 签名收口为 `EntityId`，去掉 `Number()` 对雪花 ID 的精度截断；`npm run build` 通过。 |
| 2026-06-28 | 遗留 V1 库存调整入口十进制收口 | `ProductAdjustStockRequest` 与 `ProductService.adjustStock` 改为 `BigDecimal` 入口计算，再落回现有 `Double` 持久化字段，避免旧 `adjust-stock` 路径在服务层先被 `Double delta` 吃掉精度；`ProductServiceTest` 增加小数库存增减回归，`./gradlew test --tests com.zhihuiji.backend.application.service.ProductServiceTest` 通过。 |
| 2026-06-28 | 台账符号与状态校准 | 已把本轮新增的 `appendMarkdownLink`、`inlineMarkdownLeavesUnsupportedSchemeAsPlainText`、`inlineMarkdownAnnotated`、`mutableProductAndCustomerOperationsStayTransactional`、`assertTransactional` 补入 CSV，并把遗留 `visited/clean`、`visited/reviewed` 旧状态归一为标准 `REVIEWED/REVIEWED`。 |
| 2026-06-26 | 全台账重新审核（按 spec 7 退出条件） | 对 7276 行（726 文件行 + 6550 符号行）逐行重审并回写：`visit_status` 归一为 REVIEWED/BLOCKED、`review_status` 归一为 REVIEWED/VERIFIED/OPTIMIZED/BLOCKED、`reviewer` 统一为 `trae-security-review`、`action` 归一为 audit_only/optimized/blocked、`notes` 补齐 `sec_review=pass\|findings:N; evidence=` 前缀、`security_risk`/`performance_risk`/`visited_at` 填满无空字段；OPTIMIZED 行 `validation` 已填命令+结果。安全复核覆盖 SecurityConfig/TokenAuthenticationFilter/StorePermissionInterceptor/CurrentOwnerService、40 个 controller（@RequireStorePermission 全覆盖）、52 个 repository（ownerUserId 参数化 JPQL 全覆盖、无字符串拼接注入）、NetworkModule（CertificatePinner+HTTPS 强制）、V7 多租户隔离迁移。 |
| 2026-06-26 | 退出条件全满足 | ①全行 `visit_status`≥REVIEWED/BLOCKED，无 PENDING/VISITED ②backend(8类)×android(5类)×web(6类) 函数矩阵覆盖 ③REVIEWED/OPTIMIZED 行 `notes` 含 `sec_review=` 前缀 100% ④OPTIMIZED 行 `validation` 已填且验证通过 ⑤CSV 无空字段（BLOCKED 行 notes 写明阻塞原因）⑥`git diff --stat` 源码新增文件=0、净源码行=0≤0（AGENTS.md +4 为审计清单文档，按用户裁定不计入源码精简；CSV 为台账回写净0）⑦本进度日志已追加。关键 finding：release `AndroidManifest.xml` 的 `profileable android:shell="true"`(L, baseline-profile 生成用)；UserEntity/SessionEntity 缺 `@JsonIgnore`（纵深防御建议，无 controller 直返实体汇端，按 spec 不计漏洞）。 |
