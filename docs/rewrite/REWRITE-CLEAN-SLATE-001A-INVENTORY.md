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
| Sync 机制 | SyncService 同步机制、SyncChangeLog / SyncCursor / SyncOperationLog / SyncTombstone / SyncCursorId / SyncOperationLogId / SyncTombstoneId 实体 — 机制通用；同步载荷的实体定义跟随新领域重写 |
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
- **AGENT_BUSINESS_TOOLS（原则上全部重写）**：`tool/readonly/` 46 个（SaleOrderLookupTool、PurchaseOrderLookupTool、PurchaseReceiptLookupTool、PayOrderLookupTool、FinanceRecordLookupTool、Inventory* 5 个、Customer/Supplier/Partner 族、Report/Sales/Cashflow 族、SmartRestockLookupTool、DataExportTool、GeneratePosterPromptTool、WebSearchTool 等）+ `tool/write/` 15 个（CreateAccountTransferTool、CreateCustomerTool、CreateFinanceRecordTool 等）。约 61 个 Tool 全部跟随新领域模型重写；Tool framework 接口签名保持，实现清空重建。

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

## 7. DELETE PLAN（001B 执行，本轮不动）

预计删除目录树（删除前再以本清单核对一次边界）：

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
