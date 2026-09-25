# REWRITE-CLEAN-SLATE-001A — Clean-Slate Inventory

日期：2026-09-25
基线：archive/pre-domain-rewrite = pre-domain-rewrite-2026-09-25 = 19283a4d
分支：rewrite/business-v3（自 19283a4d 创建）
性质：**清单文档，本轮不删除任何源码。** 删除与骨架整理由 REWRITE-CLEAN-SLATE-001B 执行。

---

## 0. 判定原则

1. 基础能力能复用就复用（KEEP / KEEP_BUT_SIMPLIFY）。
2. 业务领域重新设计 → 旧领域模型一律 REBUILD，不因"写得不错"保留。
3. UI 业务页面重新设计 → feature/pages 层业务绑定一律 REBUILD。
4. Agent 只保留基础设施，业务 Tool 跟随新领域重写。
5. 旧代码已封存于 `archive/pre-domain-rewrite`（19283a4d），随时可查历史。

---

## 1. BACKEND 分类

### BACKEND_KEEP（原样保留）

| 位置 | 内容 |
|---|---|
| 工程壳 | `Code/backend` build.gradle / settings / gradle wrapper / application.yml 骨架 |
| `api/common` | ApiResponse、BusinessException、GlobalExceptionHandler、IdGenerator、PaginationUtils、ParseUtils（6 个纯基础设施类） |
| `infrastructure/security` | TokenService、TokenAuthenticationFilter、RequireStorePermission、StorePermissionInterceptor、admin/ |
| `infrastructure/config` | HttpClientConfig、SecurityConfig、WebMvcConfig、AgentLlmProperties、AgentMemoryProperties、AgentImageProperties |
| `infrastructure/ai` | LongCatAnthropicClient（LLM client） |
| `application/service` | CurrentOwnerService（多租户 owner 上下文）、SessionAccessService |
| Flyway 框架 | migration 机制本身（V1–V44 作为已应用历史**不修改、不删除**） |

### BACKEND_KEEP_BUT_SIMPLIFY

| 位置 | 理由与简化方向 |
|---|---|
| `application/service/v2/agent/component` | AgentRunState、AgentIterationPolicy、AgentTerminalStatus、AgentTypes、AnswerSynthesizer、RunAuditService、SafetyGuard、SafetyDecision、SseStreamEmitter、ToolInvocationIdentity、ToolRegistry、ToolExecutor、ToolContext、ToolResult、ToolSupport、ToolArgumentsValidator、AgentEntityReferenceValidator、ToolPlanner — run lifecycle / Tool framework 通用，但类型签名引用旧业务 DTO，接新领域时需适配 |
| `application/service/v2/agent/context` | ContextBuilder、ContextCompactionService、ContextWindowResolver、TokenEstimator — context 基础设施，Builder 里的业务取数逻辑换新模型 |
| `application/service/v2/agent/memory` | AgentMemoryService — memory 基础设施 |
| `application/service/v2/agent/search` | WebSearchProvider / Request / Result / UrlSafety / DisabledWebSearchProvider — 通用 |
| V2 认证/媒体 | V2AuthController + 认证服务、V2MediaController + MediaService + MediaAsset/MediaBinding 实体与 repository、V2AgentController + AgentConversationService（会话 CRUD 部分） |
| **认证（001B 修正一）** | **AuthService 归类 KEEP_BUT_SIMPLIFY，不删除。** `V2AuthController` 直接依赖 `AuthService`；连同 TokenService、TokenAuthenticationFilter、CurrentOwnerService、SessionAccessService、认证相关 User/Session 基础设施一并保留。001B 只保证认证骨架存在且可编译，不重写认证产品需求。旧邀请码/注册逻辑后续重新设计。 |
| Sync（001B 修正三，**改判 REBUILD BUSINESS**） | 旧 `SyncService` 直接依赖 CustomerEntity / SupplierEntity / ProductEntity / SaleOrderEntity / PurchaseOrderEntity / PayOrderEntity 及其 Repository，**001B 已删除该实现**，不按 KEEP 原文件处理。**保留的同步基础设施**：SyncChangeLog、SyncCursor、SyncOperationLog、SyncTombstone 及 SyncCursorId / SyncOperationLogId / SyncTombstoneId 实体与 4 个 Repository（001B 未改动）；`V2SyncService` 收敛为通用协议面（health/cursor/ack/upload/pull + 墓碑/变更日志通用写入），实体类型分发清空，业务实体上传返回 `unsupported_entity_type` |
| admin 域 | `api/controller/admin/*`（9 个）、`application/service/admin/*`（14 个）、Admin*Entity（6 个）、AdminAudit/Export/Retention 机制 — 平台管理层与业务领域弱耦合；其中 Export/Retention 的具体表引用需要跟随新模型调整 |
| V2 import | ImportJobEntity / ImportJobRepository / V2ImportJobService / V2ImportJobWorkerService — 导入作业框架通用，字段映射重写 |
| 基础实体 | UserEntity、SessionEntity、StoreEntity、StoreMembershipEntity、CreditTransactionEntity、UserCreditEntity、PosterGenerationEntity、AgentConversationEntity、AgentMessageEntity、AgentContextCheckpointEntity、AgentMemoryEntity、AgentDraftEntity、AgentTaskEntity、AgentNotificationEntity、AgentRunAuditEntity、AgentRunAuditEventEntity — 会话/店铺/积分/媒体/Agent 运行时框架实体 |
| Demo | LocalDemoDataInitializer 的机制骨架（演示数据种子跟随新模型重写内容） |

### BACKEND_REBUILD（删除并按新领域重写）

| 位置 | 内容 |
|---|---|
| `api/common` 业务枚举 | OrderStatus、PayOrderStatus、PaymentStatus、PurchaseOrderStatus、PurchaseReceiptStatus、PurchaseReturnStatus、SalesReturnStatus、PartnerTypes |
| v1 Controller | AuthController、CustomerController、FinanceRecordController、PayOrderController、ProductController、PurchaseOrderController、SaleOrderController、SupplierController、SyncController、ReportController、AdminController + `api/dto` v1 部分 |
| v2 业务 Controller | V2AccountController、V2AccountTransferController、V2BillFundLinkController、V2CashChangeRecordController、V2CustomerContactController、V2CustomerController、V2CustomerGroupController、V2FinanceRecordController、V2InventoryController、V2PayOrderController、V2PosterController（业务部分）、V2PurchaseOrderController、V2PurchaseReceiptController、V2PurchaseReturnController、V2SaleOrderController、V2SalesReturnController、V2StoreController、V2SupplierContactController、V2SupplierController、V2SupplierGroupController、V2ReportController + `product/` 子包 |
| 业务 Service | AdminService(v1)、AuthService(v1)、ProductService、CustomerService、SupplierService、SaleOrderService、PurchaseOrderService、PayOrderService、FinanceRecordService、ReportService、DemoDataService、LegacySQLiteImportService（重写为按新模型的导入）、application/service/v2 业务 Service（V2AccountService、V2AccountTransferService、V2BillFundLinkService、V2CashChangeRecordService、V2CustomerService、V2PartnerContactService、V2PartnerGroupService、V2PayOrderService、V2PurchaseOrderService、V2PurchaseReceiptService、V2PurchaseReturnService、V2SalesReturnService、V2InventoryService、V2ReportService、CreditService、PosterService、AgentImageService 的业务部分） |
| 业务 Entity + Repository | AccountEntity、AccountTransferEntity、BillFundLinkEntity、CashChangeRecordEntity、CustomerEntity、FinanceRecordEntity、InventoryAdjustmentEntity、InventoryLedgerEntity、InventoryMonthlyStatsEntity、InventorySnapshotEntity、PartnerContactEntity、PartnerGroupEntity、PayOrderEntity、PaymentEntity、PosterGenerationEntity（业务字段部分）、Product 族（product/ 子包）、PurchaseOrderEntity 族、PurchaseReceiptEntity 族、PurchaseReturnEntity 族、SaleOrderEntity 族、SalesReturnEntity 族、SupplierEntity 及对应全部 Repository |
| 新迁移 | 新领域使用全新 baseline 迁移（不复用 V1–V44 的业务表定义；历史迁移保持已应用状态不动） |

### AGENT_INFRASTRUCTURE vs AGENT_BUSINESS_TOOLS

- **AGENT_INFRASTRUCTURE（评估复用，见 KEEP_BUT_SIMPLIFY）**：LLM client（LongCatAnthropicClient）、SSE（SseStreamEmitter）、run state（AgentRunState/AgentTerminalStatus/AgentIterationPolicy）、context（ContextBuilder/ContextCompactionService/ContextWindowResolver/TokenEstimator）、memory（AgentMemoryService）、Tool execution framework（ToolRegistry/ToolExecutor/ToolPlanner/ToolArgumentsValidator/ToolContext/ToolResult）、audit（RunAuditService）、search（WebSearchProvider 族）。
- **AGENT_BUSINESS_TOOLS**：`tool/readonly/` 46 个 + `tool/write/` 15 个，共约 61 个。

  > **001B 修正二：原「61 个全部跟随新领域重写」分类过粗，不再按 blanket 删除执行。** 拆分为三档：

  **保留的通用能力（001B 实际保留 8 个）**

  | Tool | 保留理由 |
  |---|---|
  | `WebSearchTool` | 通用 AI 能力 |
  | `ResultVisualizationTool` | 通用 AI 能力 |
  | `ImportJobLookupTool` | 导入作业框架查询 |
  | `ImageGenerateTool` | 通用生成能力 |
  | `MediaUploadTool` | 媒体基础设施 |
  | `DataExportTool` | **能力壳重建**：移除 6 个业务 Repository 依赖，保留 schema/审计/结果块框架，字段清单与量预估留空待新领域接入 |
  | `SyncStatusLookupTool` | 仅查询同步 infrastructure（`V2SyncService.health`） |
  | `StoreInfoLookupTool` | 仅读 StoreEntity / StoreMembership（店铺为保留实体） |

  **评估后删除**：`GeneratePosterPromptTool` —— 依赖 `ProductRepository` 且 `dependsOn() = product_catalog_lookup`，与已删商品域不可分离，判定属旧商品/营销领域，整体删除并在此记录。

  **删除的旧业务 Tool（53 个）**：SaleOrder*、SalesReturn*、SalesOverview*、SalesTrend*、SalesFullChain*、PurchaseOrder*、PurchaseReceipt*、PurchaseReturn*、PurchaseTracking*、PayOrder*、Payment*、FinanceRecord*、Cashflow*、ReceivablePayable*、Inventory* 5、SmartRestock、Customer* 3、Supplier* 3、Partner* 2、Product* 4、Account* 3、AnomalyAlert、CrossAnalysis、CashChange、ReportQuery，以及 `tool/write/` 下全部 Create*（CreateSaleOrder / CreatePurchaseOrder / CreatePurchaseReceipt / CreatePurchaseReturn / CreateSalesReturn / CreatePayOrder / CreateFinanceRecord / CreateInventory* / CreateCustomer / CreateSupplier / CreateAccountTransfer 等）。

  **Tool framework 全量保留**：AgentTool、ToolRegistry、ToolExecutor、ToolPlanner、ToolContext、ToolResult、ToolSupport、ToolArgumentsValidator、AgentEntityReferenceValidator。核心原则：**framework 留、通用 AI 能力留、旧业务领域 Tool 删**。

---

## 2. ANDROID 分类

### ANDROID_KEEP（原样保留）

| 模块 | 内容 |
|---|---|
| `:app` | 应用壳（MainActivity、导航骨架壳、DI 装配骨架） |
| `:backdrop` | com.kyant.backdrop 液态玻璃渲染库（Blur/Shadow/Highlight/RuntimeShader），第三方只读 |
| `:benchmark` | Macrobenchmark 基建 |
| `core/designsystem`（25 文件） | ZhihuijiTheme/Colors/Shapes/Typography、LiquidGlassCard、LiquidGlassSurface、GlassScaffold、GlassTextField、GlassTopBar、FloatingGlassActionButton、PrimaryButton、SecondaryOutlineButton、DangerOutlineButton、BottomActionBar、SegmentedTabs、FilterChipRow、SearchFilterBar、StatusPill、KpiCard、ChartCard、DocumentListCard、DocumentChromeSpacing、BusinessListItem、QuantityStepper、EmptyState |
| `core/common` | MoneyFormatter、TimeFormatter、UiMessage、FileReadGuards（+ 各自测试） |
| `core/network` | Retrofit/OkHttp/SSE 基础 |
| `core/datastore` | 会话/偏好持久化基础 |
| `data/auth`、`data/sync` 机制 | 登录与同步管道基础设施 |

### ANDROID_KEEP_BUT_SIMPLIFY

| 位置 | 理由 |
|---|---|
| `core/common/StatusLabels.kt` | 14 组状态→标签 lookup 全部绑定旧状态码；机制（mapOf lookup 模式）保留，表内容跟随新领域重写 |
| `core/model` | DTO 基础类型（snake_case 序列化配置、ID Long 规则）保留；业务 DTO 模型全部重写 |
| `data/agent` | SSE 流客户端/会话管道基础设施保留；draft/task 业务载荷模型重写 |
| `feature/agent` | ResultBlockRenderer、AgentMarkdownText、AgentResponseProvenance、timeline key 机制、ConversationListPanel 渲染层保留（24 文件中约 1/3）；AgentChatScreen/DraftListScreen/TaskNotificationScreen 的业务区块与 ViewModel 跟随新 Tool 协议适配 |
| `feature/auth`、`feature/settings` | 登录/设置流程大体通用；StaffManagement 跟随新组织模型微调 |
| `core/database` | Room 基建保留；EntityMappers 与业务实体全部重写 |

### ANDROID_REBUILD（删除并重写）

| 模块 | 内容 |
|---|---|
| `:feature:dashboard` | 仪表盘 |
| `:feature:products` | 商品业务流 |
| `:feature:customers` | 客户业务流 |
| `:feature:suppliers` | 供应商业务流 |
| `:feature:sales` | 销售单/销售退货 UI |
| `:feature:purchases` | 采购单/收货/退货 UI |
| `:feature:payments` | 收付款 UI |
| `:feature:finance` | 财务记录 UI |
| `:feature:reports` | 报表 UI |
| `:data:product` `:data:customer` `:data:supplier` `:data:order` `:data:finance` `:data:report` | 旧领域 repository 全部 |
| 导航 | `:app` 内旧业务 navigation 图随新 IA 重写 |

---

## 3. WEB 分类

### KEEP
- `app/` 壳：router 基础（routes 机制）、providers、layouts 基础、`app/stores/session.ts`（登录态/权限）
- `shared/api/client.ts`、`shared/api/config.ts`（基础 API client）
- `shared/api/agent-stream.ts`（SSE 基础）
- `shared/utils/camelize.ts`、`shared/utils/id.ts`（BigInt-safe ID）
- `shared/ui/`：PageEmptyState.vue、PageStatusBanner.vue

### KEEP_BUT_SIMPLIFY
- `shared/utils/business.ts`：格式化（formatCurrency/formatNumber/formatPercent/formatDate/formatDateTime）、`readQueryId`/`sameEntityId` 保留；全部 sale*/purchase*/finance* 状态标签函数跟随新领域重写
- `shared/api/contracts.ts`：类型契约骨架机制保留，业务类型全部重写

### REBUILD
- `pages/`：agent(业务部分)、archives、auth(重设计)、dashboard、documents、finance、inventory、planning、reports、settings(业务部分)、StitchScreenPage
- `features/`：agent(业务部分)、archives、auth、dashboard、documents、reports、settings
- `entities/`：agent、auth(静态角色配置可参考后重写)、finance、import-job、inventory、media、order、partner、product、report、screen、sync

---

## 4. TESTS 分类

- **KEEP_INFRA_TEST**：`api/common` 相关测试、TokenEstimatorTest、agent component 非业务测试（Tool framework/context/search）、core/common（MoneyFormatter/TimeFormatter/FileReadGuards/UiMessage）、core/network、ResultBlockRendererContractTest 等渲染契约测试、benchmark 基建。
- **REWRITE_BUSINESS_TEST**：全部领域服务/repository/controller 测试、migration SQL 测试（跟随新模型写新测试）、feature 屏测试、entities/page 测试、Agent Tool 业务测试。
- **ARCHIVE_ONLY**：已封存的 wave 报告、性能报告、审阅台账（testing/、docs/）——只作历史，不再维护。
- 原则：不为保住旧测试而限制新模型设计。

---

## 5. LEGACY DATA KEEP LOCAL（不删除、不进 Git）

- `data/database/migration_source_zhihuiji/`（data.db 500K、demo.db 500K、9ffd7446…ef.db 14M）
- `data/database/migration_output/`（zhihuiji.db 1.9M、zhihuiji.device.db 1.9M）
- 保持被 `/data/database/` ignore；后续 Legacy DB → Export → Transform → New Import 直接使用这些样本。sha256 已记录于 2026-09-25 archive 报告。

---

## 6. REWRITE SECURITY REQUIREMENT（记录，不阻塞本轮）

- 仓库为 public；README 历史固定邀请码 `021218` 公开可见。
- AuthService 在 `auth.invite-code` 非空时接受配置值；application-prod.yml 用 `AUTH_INVITE_CODE` 环境变量、无硬编码 secret——现状合规。
- **要求**：重新部署前必须确认并更换 `AUTH_INVITE_CODE`（若现网仍为 `021218`）；新系统邀请码/注册机制设计时禁止复用任何曾在公开仓库出现过的历史值；README 中该历史说明在新文档中移除。

---

## 7. DELETE PLAN（001B 已执行，实际结果见 §10）

预计删除目录树（001A 编制时的计划；001B 按本清单 + 主控三条修正执行，差异见 §10）：

```
Code/backend/src/main/java/com/zhihuiji/backend/
├── api/controller/                    # v1 全部 11 个 + admin/ 9 个保留项重审后按上表处理
├── api/controller/v2/                 # 业务 controller 21 个（保留 V2Auth/Media/Agent）
├── api/dto/                           # 业务 DTO 包
├── api/common/                        # 仅 8 个业务枚举 + PartnerTypes
├── application/service/               # 业务 Service（保留 CurrentOwner/SessionAccess）
├── application/service/v2/            # 业务 Service（保留 agent infra / media / auth）
├── application/service/v2/agent/tool/ # readonly 46 + write 15 全部
├── domain/entity/                     # 业务实体（保留基础设施实体）
└── infrastructure/repository/         # 业务 repository（保留基础设施 repository）

Code/frontend/android/
├── feature/dashboard|products|customers|suppliers|sales|purchases|payments|finance|reports/
├── data/product|customer|supplier|order|finance|report/
└── core/database 内业务实体与 EntityMappers

Code/frontend/web/src/
├── pages/{archives,dashboard,documents,finance,inventory,planning,reports} + agent/settings 业务部分
├── features/{archives,dashboard,documents,reports,auth} + agent/settings 业务部分
└── entities/*（重写）
```

删除后需同步清理：settings.gradle.kts 模块引用、:app 导航图、Backend 未使用 import、web router 引用。

## 8. POST-CLEAN TARGET TREE

```
Code/
├── backend/
│   └── src/main/java/com/zhihuiji/backend/
│       ├── api/common/            # ApiResponse/BusinessException/GlobalExceptionHandler/IdGenerator/PaginationUtils/ParseUtils
│       ├── api/controller/v2/     # 新领域 controller（重写）
│       ├── application/service/   # CurrentOwner/SessionAccess + 新领域 service
│       ├── application/service/v2/agent/{component,context,memory,search,tool}/  # Agent infra + 新业务 Tool
│       ├── domain/entity/         # 基础设施实体 + 新领域实体
│       └── infrastructure/{ai,config,security,storage,repository}/
│
├── frontend/
│   ├── android/
│   │   ├── app/                   # 新 IA 导航
│   │   ├── backdrop/
│   │   ├── benchmark/
│   │   ├── core/{common,designsystem,network,datastore,model,database}/
│   │   ├── data/{auth,sync,agent} + 新领域 data 模块
│   │   └── feature/{auth,settings,agent} + 新业务 feature 模块
│   └── web/
│       └── src/{app,shared,entities,pages,features}/   # 壳保留，业务重写
│
docs/
testing/
deploy/
data/database/                    # 本地 legacy 样本，永不进 Git
```

## 9. READY_FOR_DELETE

**YES** — 各模块边界清晰（Android 以 Gradle 模块为界、Web 以目录为界、Backend 以包为界），业务与基础设施无不可分离的混杂；唯一需要 001B 执行时现场确认的混合点（已在上表标注，非 blocker）：
1. `feature/agent`、`data/agent` 内渲染基建与业务载荷同模块 — 按文件级清单拆分；
2. admin 域 Export/Retention 引用旧表 — 保留机制、改表引用；
3. Sync/Import 机制与载荷同文件 — 保留机制、重写载荷。

---

## 10. 001B 实际删除结果（执行记录）

执行分支：`rewrite/business-v3`；起点 HEAD `c19986aa`；`archive/pre-domain-rewrite` 与 tag `pre-domain-rewrite-2026-09-25` **未改动**（仍为 `19283a4d`）。
迁移 **V1–V44 全部未动**（44 个文件，0 改动）；`data/database/**`、`docs/`、`testing/`、`deploy/` 未删。

### 10.1 对本清单的三处修正（以主控提示为准，覆盖上文对应表述）

| # | 修正 | 001B 实际处理 |
|---|---|---|
| 修正一 | AuthService 不直接删除 | `V2AuthController → AuthService` 依赖成立；AuthService、TokenService、TokenAuthenticationFilter、CurrentOwnerService、SessionAccessService、User/Session 基础设施全部保留，仅保证认证骨架可编译 |
| 修正二 | 不删除全部 61 个 Agent Tool | 按三档拆分：**保留 8**（WebSearch / ResultVisualization / ImportJobLookup / ImageGenerate / MediaUpload / DataExport(能力壳) / SyncStatusLookup / StoreInfoLookup），**删除 53**（旧业务领域 Tool），`GeneratePosterPromptTool` 因依赖 `ProductRepository` 判定删除并记录 |
| 修正三 | SyncService 非 KEEP 原文件 | 旧 v1 `SyncService`（576 行，直依赖 7 个业务实体）**已删除**；`V2SyncService` 2494 → 877 行收敛为通用协议面；Sync 四件套实体 + 4 个 Repository 原样保留 |

### 10.2 三端删除量

| 端 | 前 | 后 | 说明 |
|---|---:|---:|---|
| Backend main java | 421 | **196** | 删 v1 全部 11 个 controller、v2 业务 controller 26、业务 DTO 27、业务枚举 9、业务 Service 36、业务 Entity 31、业务 Repository 31、readonly Tool 40、write Tool 13 |
| Backend test java | 143 | **55** | 删业务 controller/service/repository/tool/migration-SQL 测试 86 + `ToolPlannerTest`×2（55 个旧业务路由用例，清场后无通过用例，整体删除） |
| Android kt | 345 | **190** | 删 15 个模块共 100 kt + core/model、core/database 业务实体与 EntityMappers 等 |
| Android modules | 30 | **15** | settings.gradle.kts include 30 → 15 |
| Web ts+vue | 51 | **18** | 删 34 个旧业务页面/实体，新增 1 个 rewrite shell |
| Backend Agent Tool | 61 | **8** | readonly 46→6、write 15→2 |
| Migration | 44 | **44** | 零改动 |

### 10.3 保留骨架

- **Backend**：`api/common` 6 个通用类 + admin conflict；`api/controller/{admin,v2}` 9+5；`application/service/{admin,store,v2,v2/agent}`；agent component/context/memory/search/tool framework；`domain/entity` 32 个基础设施实体（含 Sync 四件套）；`infrastructure/{security,config,ai,storage,repository}`；认证 AuthService + SecurityConfig。
- **Android**：`app`（收敛为 rewrite shell，底栏「首页/助手」）、`backdrop`、`benchmark`、`core/{common,designsystem,network,datastore,model,database}`、`data/{auth,agent,sync}`、`feature/{auth,settings,agent}`；Room migration 7 个 + DatabaseModule 内联 3 个保留。
- **Web**：`app/{layouts,router,stores}`、`entities/auth`、`shared/{api,ui,utils}`、`pages/{auth,Forbidden,RewriteShell}`。

### 10.4 构建与测试验证

| 端 | 命令 | 结果 |
|---|---|---|
| Backend | `./gradlew clean compileJava compileTestJava test` | **BUILD SUCCESSFUL**；55 suites / **359 tests / 0 failures / 0 errors** |
| Android | `./gradlew :app:compileDebugKotlin` + 10 个保留模块单测 | **BUILD SUCCESSFUL**；**294 tests / 0 failures / 0 errors** |
| Web | `npm run build`（`vue-tsc -b && vite build`） | **成功**，退出码 0 |

### 10.5 旧业务源码残留搜索结果

对 `Code/**` 全部 `.java/.kt/.ts/.vue/.kts` 扫描 `SaleOrder / PurchaseReceipt / SalesReturn / PurchaseReturn / PayOrder / FinanceRecord / AccountTransfer / BillFundLink / CashChange / InventoryLedger / InventoryAdjustment / SmartRestock / create_sale_order / create_purchase_order` 等符号（排除 migration 与生成物）：**0 命中**。

仍存在但**不属旧业务源码**的项：
1. `ToolPlanner.isAmbiguousWriteRequest` 用中文词「商品/客户/供应商」做写请求歧义判断 —— 通用自然语言启发式，不引用任何领域类；`AgentPromptCatalog.targetWriteTool` 只会返回保留的 `image_generate` / `media_upload_tool`，`targetReadTool` 已中和返回 null。
2. `PosterGenerationEntity.product_id` 列 —— 该实体在保留清单内，migration 不可改。
3. `ToolExecutorAdminConfigContractTest` / `AdminAgentRuntimeConfigServiceTest` / `LongCatAnthropicClientTest` / `ToolRegistryTest` / `ContextBuilderTest` / Android 序列化测试中的旧工具名字符串 —— 测试主体是 admin 配置、LLM client、registry、context、序列化机制，工具名仅为随行样本；001B 已把 `create_sale_order` / `create_purchase_order` 改为中性样本名。
4. Android Room migration（`Migration9To10.kt` 含 `receivableCustomerCount` 列）与 Backend V1–V44 —— 迁移历史，按第 4 节规定不动。
5. `LegacyBusinessTableCleaner` 内 19 个业务表名字符串 —— 注销/访问撤销清数据的既有安全能力，改用裸 SQL 延续，未恢复实体。

### 10.6 待主控决策（001B 未处理）

1. **Room 版本仍为 11、实体 27 → 8**：老设备打开旧库时遗留业务表成为 schema 外孤儿表（Room 不校验）。是否随新领域升版本并 DROP 旧表。
2. **迁移链保留 vs 清除**：若确定重写后强制清库重导，可整体删除 Android 10 个迁移并重置版本 —— 涉及现网老设备策略。
3. **Backend `static/admin-console` 已删除**：它只调用随清单删除的 `/v1/admin/*`，保留即死 UI；如需恢复要改接 `/v2/admin/*`。
4. **三处「旧请求按失败处理」**：`AgentDraftConfirmService` 非 image_generate/media_upload 草稿抛"不支持的草稿类型"；ImportJob worker 对任何 sourceType 标记失败；`V2SyncService` 上传对任何实体类型返回 `unsupported_entity_type`。均为清场后的预期行为，已加占位注释。
5. **Web `style.css`（3468 行）与 `public/stitch_exports/`** 仍含旧页面样式/静态导出，不影响构建，留待新 IA 时清理。

