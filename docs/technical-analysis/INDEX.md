# 智慧记 (Master-Goods) 优化点总索引

> 本文档记录项目全部分析中发现的值得优化的点，共 **184 项**，按严重程度分级。
> 每项标注所属模块、涉及代码元素、问题描述与建议修改方式。详细分析见各子目录 [README.md](#目录结构速览)。
> ⚠️ 本文档已经过代码审核校验，修正了原始分析中的错误条目（标注 ❌ 已移除）和不准确描述（标注 ✏️ 已修正）。

---

## 统计总览

| 严重程度 | 安卓端 | 服务器端 | 合计 |
|----------|--------|----------|------|
| 🔴 严重Bug | 5 | 5 | **10** |
| 🟠 高优先级 | 21 | 15 | **36** |
| 🟡 中优先级 | 78 | 27 | **105** |
| ⚪ 低优先级 | 10 | 18 | **28** |
| ❌ 审核移除 | 0 | 5 | **5** |
| **合计** | **114** | **70** | **184** |

---

## 跨模块系统性风险

以下问题横跨多个模块，需统一规划修复：

| # | 系统性风险 | 波及范围 | 严重度 |
|---|-----------|----------|--------|
| S1 | 金额/数量字段使用 Double，浮点精度丢失 | server/entity, server/dto, android/core/model, android/core/database, android/core/common | 🔴 |
| S2 | 敏感信息硬编码（DB密码弱默认值/API Key残留明文） | server/config, server/resources | 🔴 |
| S3 | 全量查询+内存过滤（服务端5个Service + 安卓3个Repository） | server/service, android/data | 🟠 |
| S4 | 状态/类型字段缺少枚举（服务端18处 + 安卓端StatusLabels/Model） | server/entity, android/core/common, android/core/model | 🟡 |
| S5 | collect应替换为collectLatest（6个ViewModel） | android/feature | 🔴 |
| S6 | 网络请求串行应并行化（Dashboard/Settings/SaleOrderDetail ViewModel） | android/feature | 🟠 |
| S7 | 筛选逻辑应在ViewModel/DAO层而非Screen层 | android/feature | 🟡 |
| S8 | Controller参数校验不完整（8个Controller缺@Valid） | server/controller | 🟡 |
| S9 | 缺少数据库索引和外键约束 | server/resources, server/entity | 🟡 |
| S10 | 死代码/未调用方法（Agent/Report/SaleOrder Repository） | android/data | 🟡 |
| S11 | TokenService文档与代码严重不符（影响4条建议准确性） | server/security | 🔴 |
| S12 | 方法名定位系统性偏差（10+条建议方法名与实际代码不一致） | 全局 | 🟡 |

---

## 🔴 严重Bug（10项）

### 服务器端（5项）

| # | 问题 | 模块 | 涉及代码 | 建议 |
|---|------|------|----------|------|
| S-🔴-1 | 同步上传未实现 | server/service | `SyncService.upload()` | upload() 仅保存同步游标，未将客户端上传的变更应用到数据库，需实现 change 列表中每条变更的实体解析与数据库写入逻辑 |
| S-🔴-2 | 全局API无认证保护 | server/config | `SecurityConfig` → `anyRequest().permitAll()` | 当前所有 API 均无需认证（不仅是 /admin 或 /sync），应按路径配置认证+角色授权 |
| S-🔴-3 | API Key明文残留 | server/resources | `application-local.yml` → `agent.llm.api-key` | 主配置已用 `${AGENT_LLM_API_KEY:}` 外部化，但 local profile 残留明文，应改为环境变量引用 |
| S-🔴-4 | 数据库密码弱默认值 | server/resources | `application.yml` → `${DB_PASSWORD:zhihuiji}` | 使用了环境变量但提供弱默认值 `zhihuiji`，应移除默认值使未配置时启动失败 |
| S-🔴-5 | 验证码未实际校验 | server/service | `AuthService.register(phone, password, verifyCode)` | verifyCode 被复用为邀请码校验（非短信验证码），AuthController.verifyCode() 直接返回失败；需接入真实短信验证码服务 |

> ❌ **已移除 S-🔴-1(旧)** "取消订单未恢复库存" — 代码审核确认 `SaleOrderService.cancel()` 已正确恢复库存、扣减客户余额、生成退款记录
> ❌ **已移除 S-🔴-3(旧)** "JWT密钥硬编码" — 代码审核确认 TokenService 不使用 JWT，仅生成 UUID token，无 secretKey 字段
> ❌ **已移除 S-🔴-7(旧)** "单号生成非线程安全" — 方法不在 AuthService 中；UUID 本身线程安全，但 substring(0,4~6) 截取降低唯一性，风险不足以列为 🔴
> ✏️ **S-🔴-2(旧)→S-🔴-1** "同步上传未实现" 修正描述：upload() 并非完全未实现，而是仅保存游标未将变更应用到数据库
> ✏️ **S-🔴-4(旧)→S-🔴-3** "API Key明文存储" 修正：主配置已外部化，仅 local profile 残留
> ✏️ **S-🔴-5(旧)→S-🔴-4** "数据库密码明文硬编码" 修正：使用了环境变量但提供弱默认值
> ✏️ **S-🔴-6(旧)→S-🔴-5** "验证码未实际校验" 修正：verifyCode 被复用为邀请码
> ✏️ **S-🟠-2(旧)→S-🔴-2** 严重度上调：不是仅 Admin/Sync 无认证，而是全局 `anyRequest().permitAll()`

### 安卓端（5项）

| # | 问题 | 模块 | 涉及代码 | 建议 |
|---|------|------|----------|------|
| A-🔴-1 | FilterChipRow 点击无响应 | android/core/designsystem | `FilterChipRow` Composable | 为每个 Chip 添加 `clickable` 修饰符，点击时调用 `onChipSelected(index)` |
| A-🔴-2 | DecimalFormat/SimpleDateFormat 非线程安全 | android/core/common | `MoneyFormatter`, `TimeFormatter` | 每次格式化创建新实例，或使用 `ThreadLocal`，或改用 `DateTimeFormatter` |
| A-🔴-3 | AuthInterceptor.intercept() 使用 runBlocking 阻塞线程 | android/core/network | `AuthInterceptor.intercept()` | 使用同步方式获取 token，或使用 CacheInterceptor 模式 |
| A-🔴-4 | TokenAuthenticator.authenticate() 使用 runBlocking | android/core/network | `TokenAuthenticator.authenticate()` | 将 token 刷新逻辑改为同步调用，或使用专用线程池 |
| A-🔴-5 | 多个 ViewModel 的 loadXxx() 使用 collect 而非 collectLatest | android/feature (6个模块) | `CustomerViewModel`, `PayOrderViewModel`, `SaleOrderListViewModel`, `SupplierViewModel`, `ProductListViewModel`, `PurchaseOrderViewModel` | 将 `collect` 替换为 `collectLatest`，确保只处理最新数据流发射 |

---

## 🟠 高优先级（39项）

### 服务器端（15项）

| # | 问题 | 模块 | 涉及代码 | 建议 |
|---|------|------|----------|------|
| S-🟠-1 | CORS允许所有来源 | server/config | `SecurityConfig.corsConfigurationSource()` → `allowedOriginPatterns("*")` | 限制为指定前端域名 |
| S-🟠-2 | 验证码接口硬编码失败 | server/controller | `AuthController.verifyCode()` | 接入真实短信/验证码服务 |
| S-🟠-3 | 全量查询+内存过滤（SaleOrderService） | server/service | `SaleOrderService.list()` | 在 Repository 层实现多条件数据库查询，使用 JPA Specification 或 QueryDSL |
| S-🟠-4 | 全量查询+内存过滤（PurchaseOrderService） | server/service | `PurchaseOrderService.list()` | 在 Repository 层增加按 status/keyword 的数据库条件查询 |
| S-🟠-5 | 全量查询+内存过滤（PayOrderService） | server/service | `PayOrderService.list()` | 同上 |
| S-🟠-6 | 全量查询+内存过滤（FinanceRecordService） | server/service | `FinanceRecordService.list()` | 同上 |
| S-🟠-7 | 全量查询+内存过滤（AdminService） | server/service | `AdminService.listUsers()` | 在 UserRepository 增加模糊搜索方法 |
| S-🟠-8 | ReportService全量内存聚合 | server/service | 所有12个报表方法 | 12个方法均涉及内存聚合，部分有时间范围预过滤；应使用 SQL 聚合查询下推到数据库层 |
| S-🟠-9 | 金额字段使用 Double（全局） | server/entity, server/dto | 所有金额/数量字段 | 统一改用 `BigDecimal` |
| S-🟠-10 | 缺少外键约束（全局） | server/entity, server/resources | 所有关联字段 | 在迁移脚本中添加外键约束 |
| S-🟠-11 | 缺少分页支持（全局） | server/repository | 所有 Repository 查询方法 | 引入 `Pageable` 参数，返回 `Page<T>` |
| S-🟠-12 | LLM超时配置可能不足 | server/ai | `LongCatAnthropicClient` → `SimpleClientHttpRequestFactory` connectTimeout=10s, readTimeout=25s | 已配置超时但 readTimeout=25s 对 LLM Thinking 模式可能不足，建议读取超时调至 120s |
| S-🟠-13 | 无令牌黑名单 | server/security, server/service | `AuthService.logout()` | logout 仅设 session.isActive=false，无黑名单机制；登出后 token 在 session 过期前仍可被使用 |
| S-🟠-14 | Token验证每次查库无缓存 | server/security, server/service | `AuthService.me()` | 每次验证 token 查 DB 2次（session+user），应引入 Redis 缓存 session 信息 |
| S-🟠-15 | 单号生成截取UUID降低唯一性 | server/service | `SaleOrderService:58`, `PurchaseOrderService:42`, `PayOrderService:154` | UUID 截取前4-6位作为后缀，高并发下有碰撞风险；建议改用完整 UUID 或雪花算法 |

> ❌ **已移除 S-🟠-2(旧)+S-🟠-3(旧)** "管理接口/同步接口无认证" — 根因是全局 `anyRequest().permitAll()`，已合并为 S-🔴-2
> ❌ **已移除 S-🟠-11(旧)** "循环依赖风险" — 代码审核确认 AgentTaskService 单向依赖 LlmDrivenAgentService，不存在循环依赖
> ❌ **已移除 S-🟠-17(旧)** "刷新令牌未轮换" — 代码审核确认 `AuthService.refresh()` 已实现轮换（旧 session isActive=false，新 session 颁发新 token）
> ✏️ **S-🟠-1** 位置修正：`allowedOriginPatterns("*")` 在 `corsConfigurationSource()` 而非 `securityFilterChain()`
> ✏️ **S-🟠-5~9(旧)→S-🟠-3~7** 方法名修正：实际方法名是 `list()` / `listUsers()` 而非 `search()` / `searchUsers()`
> ✏️ **S-🟠-15(旧)→S-🟠-12** 技术细节修正：使用 `SimpleClientHttpRequestFactory`（非 OkHttpClient），已配置超时但可能不足
> ✏️ **S-🟠-16(旧)→S-🟠-13** 修正：`TokenService.validateToken()` 方法不存在，token 验证通过 `AuthService.me()` 查库实现
> ✏️ **S-🟠-18(旧)→S-🟠-14** 修正：token 验证非每次请求自动触发，而是手动调用 `me()` 时查库
> ✏️ **S-🔴-7(旧)→S-🟠-15** 严重度下调：UUID 本身线程安全，截取后缀降低唯一性但风险不足以列为 🔴

### 安卓端（21项）

| # | 问题 | 模块 | 涉及代码 | 建议 |
|---|------|------|----------|------|
| A-🟠-1 | fallbackToDestructiveMigration 生产环境数据丢失 | android/core/database | `DatabaseModule` → ZhihuijiDatabase 构建 | 移除 fallbackToDestructiveMigration()，编写正式 Migration 对象 |
| A-🟠-2 | Dto.toEntity() id为null默认0L导致主键冲突 | android/core/database | `EntityMappers` 各 toEntity() | 使用 autoGenerate = true 并将默认值设为 0 |
| A-🟠-3 | 订单项未持久化到本地数据库 | android/core/database | `SaleOrderEntity`, `PurchaseOrderEntity` 的 items 字段 | 新增 SaleOrderItemEntity / PurchaseOrderItemEntity 表及对应 DAO |
| A-🟠-4 | SessionStore.isLoggedIn 未考虑 token 过期 | android/core/datastore | `SessionStore.isLoggedIn` | 加入 token 过期时间检查 |
| A-🟠-5 | normalizeBaseUrl 逻辑重复 | android/core/datastore, android/core/network | `SettingsStore.normalizeBaseUrl()`, `NetworkConfig.normalizeBaseUrl()` | 提取为公共工具函数放在 core/common |
| A-🟠-6 | NetworkConfig.baseUrl 可变全局状态非线程安全 | android/core/network | `NetworkConfig.baseUrl`（var 全局变量） | 使用 `AtomicReference<String>` 或 `MutableStateFlow<String>` |
| A-🟠-7 | provideLoggingInterceptor 生产环境 BODY 级别日志 | android/core/network | `NetworkModule.provideLoggingInterceptor()` | 根据 BuildConfig 区分环境，生产用 NONE 或 BASIC |
| A-🟠-8 | provideRetrofit 使用 runBlocking | android/core/network | `NetworkModule.provideRetrofit()` | baseUrl 通过拦截器动态替换，缓存 Retrofit 实例 |
| A-🟠-9 | ZhihuijiApi 创建接口使用 Dto 作为请求体 | android/core/network | `ZhihuijiApi.createProduct()` 等 | 为每个创建操作定义专用 CreateXxxRequest 数据类 |
| A-🟠-10 | ZhihuijiApi.saleOrders 参数过多 | android/core/network | `ZhihuijiApi.saleOrders()` | 封装为 @QueryMap 或 SaleOrderFilter 对象 |
| A-🟠-11 | ZhihuijiApi authorization 参数应由拦截器添加 | android/core/network | `ZhihuijiApi.logout()`, `ZhihuijiApi.me()` | 移除 authorization 参数，统一由 AuthInterceptor 添加 |
| A-🟠-12 | SafeApiCall vs ResultExt 错误处理策略不统一 | android/core/network, android/core/common | `SafeApiCall`, `ResultExt` | 统一为一套错误处理机制 |
| A-🟠-13 | Repository 内存过滤大数据量性能不佳 | android/data/order, android/data/finance | `SaleOrderRepository`, `PurchaseOrderRepository`, `PayOrderRepository`, `FinanceRepository` | 筛选条件下推到 DAO 层 SQL WHERE 子句 |
| A-🟠-14 | SaleOrderRepository.addSalePayment 收款后未更新本地 paidAmount | android/data/order | `SaleOrderRepository.addSalePayment()` | 收款成功后立即更新本地 SaleOrderEntity 的 paidAmount |
| A-🟠-15 | DashboardViewModel 5个网络请求串行执行 | android/feature/dashboard | `DashboardViewModel.loadDashboard()` | 使用 coroutineScope + async 并行发起请求 |
| A-🟠-16 | DashboardViewModel 依赖 AgentRepository 不合理 | android/feature/dashboard | `DashboardViewModel` 对 `AgentRepository` 的依赖 | 通过 DashboardRepository 统一封装，或使用 UseCase 模式解耦 |
| A-🟠-17 | AgentViewModel 未处理 onFailure | android/feature/agent | `AgentViewModel.loadWorkbench/loadTasks/loadNotifications()` | 添加 onFailure 处理，更新 error 状态字段 |
| A-🟠-18 | AuthViewModel.restoreSession 未处理恢复失败 | android/feature/auth | `AuthViewModel.restoreSession()` | 恢复失败时清除本地会话并导航到登录页 |
| A-🟠-19 | SettingsViewModel.loadSettings 串行且未处理 onFailure | android/feature/settings | `SettingsViewModel.loadSettings()` | 使用 async 并行请求，添加 onFailure 错误处理 |
| A-🟠-20 | FinanceViewModel.loadRecords 使用 first() 只获取初始快照 | android/feature/finance | `FinanceViewModel.loadRecords()` | 使用 collectLatest 持续订阅数据流 |
| A-🟠-21 | GlassScaffold content 传入 PaddingValues() 导致内容被遮挡 | android/core/designsystem | `GlassScaffold` 的 content 参数 | 将 Scaffold 提供的 PaddingValues 传递给 content lambda |

---

## 🟡 中优先级（106项）

### 服务器端（27项）

| # | 问题 | 模块 | 涉及代码 | 建议 |
|---|------|------|----------|------|
| S-🟡-1 | Entity直接作为请求体 | server/controller | `CustomerController.create()/update()`, `SupplierController.create()/update()`, `ProductController.create()/update()` | 创建专用请求 DTO（共3个Controller 6个方法受影响） |
| S-🟡-2 | 参数校验不完整（Controller层） | server/controller | 多个 Controller @RequestBody 缺 @Valid | 根本问题是缺 @Valid 触发校验，且大部分内部 record 无校验注解 |
| S-🟡-3 | 参数校验不完整（DTO层） | server/dto | `ProductAdjustStockRequest`, `SaleOrderStatusRequest` | 补充校验注解 |
| S-🟡-4 | 物理删除 | server/controller + service | `CustomerService.delete()`, `SupplierService.delete()`, `ProductService.delete()` | 改为软删除（增加 deletedAt 字段），删除前检查关联数据 |
| S-🟡-5 | 状态字段缺少枚举（全局） | server/entity, server/dto | 所有 status/type/method 字段（18处） | 改用 Java 枚举类型 + @Enumerated |
| S-🟡-6 | parseLong/parseDouble 重复代码 | server/controller | `FinanceRecordController`, `PayOrderController`, `SaleOrderController` | 抽取到 api/common/ParseUtils 工具类 |
| S-🟡-7 | AgentDto过于庞大 | server/dto | `AgentDto.java`（21个 record） | 按职责拆分为独立文件 |
| S-🟡-8 | JSON命名策略不一致 | server/dto | `AgentDto`(驼峰) vs `ReportDto`等(下划线) | 统一为一种策略 |
| S-🟡-9 | 业务异常类缺失 | server/common | `GlobalExceptionHandler.handleBusiness()` → IllegalArgumentException | 定义 BusinessException 类（51处 throw new IllegalArgumentException） |
| S-🟡-10 | 缺少数据库索引 | server/resources | `sessions.user_id`, `products.name`, `agent_notifications.task_id` | 创建新 Flyway 迁移脚本添加索引 |
| S-🟡-11 | 手动ID碰撞风险 | server/entity, server/resources | 5个Service的 `nextId()` 使用 UUID random bits 覆盖8张表 | 改用完整 UUID 或雪花算法，增加碰撞检测/重试 |
| S-🟡-12 | LLM无重试机制 | server/ai | `LongCatAnthropicClient.createJsonMessage()` | 增加指数退避重试（最大3次） |
| S-🟡-13 | 无Token统计 | server/ai | `LongCatAnthropicClient.createJsonMessage()` | 解析响应 usage 字段，记录到日志或数据库 |
| S-🟡-14 | Service注入过多Repository | server/service | `ReportService`(8个), `SyncService`(7个), `DemoDataService`(14个) | 拆分 Service |
| S-🟡-15 | 缺少生产环境配置 | server/resources | 无 application-prod.yml | 创建 application-prod.yml |
| S-🟡-16 | AgentController参数缺少上界校验 | server/controller | `workbench()`, `reconciliationFollowup()` 等 | Controller 缺 @Max/@Min，但 Service 屓有 `normalizePositive()` 兜底 |
| S-🟡-17 | AdminService.summary()多次count查询 | server/service | `AdminService.summary()` | 编写原生 SQL 一次查询所有计数 |
| S-🟡-18 | SupplierService.list()内存过滤 | server/service | `SupplierService.list()` | keyword 非空时已用 DB 查询，但 status 过滤始终在内存中 |
| S-🟡-19 | SaleOrderService状态机校验不完整 | server/service | `SaleOrderService.updateStatus()` | 实现完整状态机，定义合法状态转换矩阵 |
| S-🟡-20 | submitDraft路由用if链 | server/service | `AgentService.submitDraft()` | 改用策略模式 |
| S-🟡-21 | LLM幻觉风险 | server/service | `LlmDrivenAgentService.draftOperation()` | 幻觉风险在 draftOperation()（调用 LLM 解析指令），非 submitDraft() |
| S-🟡-22 | 无并发控制 | server/service | `AgentService.submitDraft()` | submitDraft() 无防重复提交，但下游 SaleOrderService.create() 有 findByIdForUpdate 保护 |
| S-🟡-23 | 定时任务无分布式锁 | server/service, server/config | `AgentTaskService.runScheduledAnomalyWatch()` | 使用 ShedLock 或 Redis 分布式锁 |
| S-🟡-24 | 任务执行无超时 | server/service | `AgentTaskService.executeTask()` | 设置超时时间，超时后标记 failed |
| S-🟡-25 | LLM上下文全量加载 | server/service | `LlmDrivenAgentService.getWorkbench()/draftOperation()` | 无独立 buildBusinessContext() 方法，全量加载逻辑内联在两个方法中（findAll()+limit） |
| S-🟡-26 | AI层错误处理不完整 | server/ai | `LongCatAnthropicClient.createJsonMessage()` | 定义专用异常类，区分不同错误类型 |
| S-🟡-27 | 缺少HTTP状态码常量 | server/common | `ApiResponse` | 定义 CODE_SUCCESS = 0 等常量 |

> ❌ **已移除 S-🟡-19(旧)** "SaleOrderService创建未更新客户余额" — 代码审核确认 `SaleOrderService.create()` 第110-118行已更新客户余额
> ✏️ **S-🟡-14** Repository 数量修正：ReportService 8个(非11)、SyncService 7个(非12)、DemoDataService 14个(非12)
> ✏️ **S-🟡-17** 方法名修正：`summary()` 而非 `getSummary()`
> ✏️ **S-🟡-18** 方法名修正：`list()` 而非 `search()`；补充 keyword 非空时已用 DB 查询
> ✏️ **S-🟡-21(旧)→S-🟡-20** 类名+方法名修正：`AgentService.submitDraft()` 而非 `LlmDrivenAgentService.operationSubmit()`
> ✏️ **S-🟡-22(旧)→S-🟡-21** 方法定位修正：幻觉风险在 `draftOperation()` 而非 `submitDraft()`
> ✏️ **S-🟡-23(旧)→S-🟡-22** 补充：下游 `SaleOrderService.create()` 有 `findByIdForUpdate` 保护
> ✏️ **S-🟡-24(旧)→S-🟡-23** 方法名修正：`runScheduledAnomalyWatch()` 而非 `runScheduledTasks()`
> ✏️ **S-🟡-26(旧)→S-🟡-25** 类名+方法名修正：无独立 `buildBusinessContext()`，逻辑在 `LlmDrivenAgentService`
> ✏️ **S-🟡-27(旧)→S-🟡-26** 方法名修正：`createJsonMessage()` 而非 `chat()`

### 安卓端（78项）

| # | 问题 | 模块 | 涉及代码 | 建议 |
|---|------|------|----------|------|
| A-🟡-1 | MainScreen reselectSignal 5个独立Int变量冗余 | android/app | `MainScreen` → reselectSignal0~4 | 使用 Map<Int, Int> 统一管理 |
| A-🟡-2 | MainScreen bottomBarVisible 始终为true | android/app | `MainScreen` → bottomBarVisible | 在详情页/编辑页设为 false |
| A-🟡-3 | MainNavGraph 文件过长 | android/app | `MainNavGraph` | 按业务域拆分为多个 NavGraph 扩展函数 |
| A-🟡-4 | MainNavGraph Editor 路由 rawId>0 转 null 重复 | android/app | `MainNavGraph` 多个 Editor 路由 | 抽取为工具函数 toNullableId() |
| A-🟡-5 | DocumentsScreen 搜索按钮空实现 | android/app | `DocumentsScreen` 搜索按钮 | 实现搜索功能或移除按钮 |
| A-🟡-6 | DocumentsScreen/ArchivesScreen Tab切换列表重建 | android/app | `DocumentsScreen`, `ArchivesScreen` | 使用 HorizontalPager 保持各 Tab 页面状态 |
| A-🟡-7 | DocumentsScreen/ArchivesScreen 硬编码字符串 | android/app | `DocumentsScreen`, `ArchivesScreen` | 提取为 strings.xml 资源文件 |
| A-🟡-8 | AppNavGraph Session加载期间 Compose 树不完整 | android/app | `AppNavGraph` | 使用 AnimatedVisibility 或 Crossfade 平滑过渡 |
| A-🟡-9 | TopLevelRoutes.SETTINGS 与 MainRoutes.SETTINGS 语义重叠 | android/app | `TopLevelRoutes`, `MainRoutes` | 统一为一个路由定义 |
| A-🟡-10 | InverseLayerScope 未处理 translation 和 3D 旋转 | android/backdrop | `InverseLayerScope` | 增加 translationX/Y 逆变换支持 |
| A-🟡-11 | InnerShadowNode.drawMaskedShadow 死代码 | android/backdrop | `InnerShadowNode.drawMaskedShadow()` | 移除该方法 |
| A-🟡-12 | HighlightNode.prevStyle 未使用 | android/backdrop | `HighlightNode.prevStyle` | 移除该属性 |
| A-🟡-13 | Lens.kt 和 HighlightStyle.kt 圆角提取逻辑重复 | android/backdrop | `Lens.kt`, `HighlightStyle.kt` | 提取为公共 CornerSize 解析工具函数 |
| A-🟡-14 | 色散着色器7次采样可优化 | android/backdrop | 色散着色器 | 减少到3次采样（RGB各一次） |
| A-🟡-15 | AGSL着色器缺少数学原理注释 | android/backdrop | 各 AGSL 着色器代码 | 添加数学原理注释 |
| A-🟡-16 | blur的radius参数应提供dp重载 | android/backdrop | `blur()` 扩展函数 | 提供 Dp 类型的重载函数 |
| A-🟡-17 | exposureAdjustment公式命名混淆 | android/backdrop | `exposureAdjustment` | 将 2.2 提取为命名常量 GAMMA_FACTOR |
| A-🟡-18 | StatusLabels 状态码硬编码 | android/core/common | `StatusLabels` 各状态判断 | 定义 StatusConstants 对象，引用常量 |
| A-🟡-19 | StatusLabels.agentTaskStatus 参数类型不一致 | android/core/common | `StatusLabels.agentTaskStatus()` | 统一为 Int 类型参数 |
| A-🟡-20 | StatusLabels.stockStatus 使用 Double 比较库存量 | android/core/common | `StatusLabels.stockStatus()` | 使用容差比较或改用整数/BigDecimal |
| A-🟡-21 | 多个Entity金融数据使用Double存在精度风险 | android/core/database | `SaleOrderEntity`, `PurchaseOrderEntity`, `FinanceRecordEntity` 等 | 使用 Long（存储分）或 BigDecimal + TypeConverter |
| A-🟡-22 | ProductEntity库存量使用Double | android/core/database | `ProductEntity.stockQuantity` | 改用 Int 或 Long |
| A-🟡-23 | PurchaseOrderEntity缺少supplierId字段 | android/core/database | `PurchaseOrderEntity` | 添加 supplierId: Long 字段 |
| A-🟡-24 | 多个DAO缺少筛选查询方法 | android/core/database | `FinanceRecordDao`, `PayOrderDao`, `SaleOrderDao`, `PurchaseOrderDao` | 为各 DAO 添加带筛选参数的 @Query 方法 |
| A-🟡-25 | DEFAULT_BASE_URL硬编码IP地址 | android/core/datastore | `DEFAULT_BASE_URL` | 使用域名替代 IP，或从 BuildConfig 注入 |
| A-🟡-26 | SyncPreferenceStore.clearAll使用键名前缀过滤 | android/core/datastore | `SyncPreferenceStore.clearAll()` | 使用独立 DataStore 实例，清除时直接 edit { clear() } |
| A-🟡-27 | PrimaryGradientButton名称含Gradient但未使用渐变 | android/core/designsystem | `PrimaryGradientButton` | 重命名为 PrimaryButton 或添加渐变效果 |
| A-🟡-28 | DangerOutlineButton缺少enabled参数 | android/core/designsystem | `DangerOutlineButton` | 添加 enabled: Boolean = true 参数 |
| A-🟡-29 | KpiCard趋势颜色判断基于字符串contains | android/core/designsystem | `KpiCard` 趋势颜色逻辑 | 使用数值类型判断或定义枚举 TrendDirection |
| A-🟡-30 | LineTrendChart values和labels数量不一致时未防御 | android/core/designsystem | `LineTrendChart` | 添加防御性检查，取最小长度绘制 |
| A-🟡-31 | HorizontalBarChart硬编码take(5) | android/core/designsystem | `HorizontalBarChart` | 提取为可配置参数 maxBars: Int = 5 |
| A-🟡-32 | ZhihuijiTheme固定fontScale=1f忽略系统字体缩放 | android/core/designsystem | `ZhihuijiTheme` | 移除 fontScale = 1f 或提供配置项 |
| A-🟡-33 | 仅支持浅色主题 | android/core/designsystem | `ZhihuijiTheme` | 添加深色主题 Color 方案 |
| A-🟡-34 | BottomActionBar secondaryActions无间距控制 | android/core/designsystem | `BottomActionBar` | 添加 horizontalArrangement = Arrangement.spacedBy(dp) |
| A-🟡-35 | QuantityStepper onMinus/onPlus与onValueChange职责重叠 | android/core/designsystem | `QuantityStepper` | 统一为 onValueChange: (Double) -> Unit |
| A-🟡-36 | LiquidSegmentedControl泛型T可简化 | android/core/designsystem | `LiquidSegmentedControl<T>` | 提供非泛型的 StringSegmentedControl 便捷函数 |
| A-🟡-37 | GlassBackground光斑颜色和位置硬编码 | android/core/designsystem | `GlassBackground` | 将光斑参数提取为可配置属性 |
| A-🟡-38 | DampedSegmentedDragState.onTap回调时机过早 | android/core/designsystem | `DampedSegmentedDragState.onTap()` | 在动画完成后再触发 onSelected 回调 |
| A-🟡-39 | DampedSegmentedDragState.onDragEnd经验值未提取 | android/core/designsystem | `DampedSegmentedDragState.onDragEnd()` → 0.16f | 提取为可配置参数或命名常量 |
| A-🟡-40 | 多个Model字符串字段建议改为枚举 | android/core/model | `tone`, `severity`, `status`, `actionType`, `operationType` | 定义对应枚举类 + @SerialName 注解 |
| A-🟡-41 | 多个Model金融数据建议使用BigDecimal | android/core/model | 各 DTO 金额字段 | 改用 BigDecimal + 自定义序列化器 |
| A-🟡-42 | SaleOrderItemDto行项中包含客户信息冗余 | android/core/model | `SaleOrderItemDto` 客户信息字段 | 移除行项中的客户信息 |
| A-🟡-43 | SaleOrderFilter minTotalAmount/maxTotalAmount类型为String | android/core/model | `SaleOrderFilter` | 改为 Double? 或 BigDecimal? |
| A-🟡-44 | SaleOrderFilter.paymentStatus类型为String | android/core/model | `SaleOrderFilter.paymentStatus` | 改为 Int? 类型 |
| A-🟡-45 | VerifyCodeRequest.type建议改为枚举 | android/core/model | `VerifyCodeRequest.type` | 定义 VerificationType 枚举 |
| A-🟡-46 | SyncChangeDto.operation建议改为枚举 | android/core/model | `SyncChangeDto.operation` | 定义 SyncOperation 枚举（CREATE, UPDATE, DELETE） |
| A-🟡-47 | AuthRepository settingsStore注入但未使用 | android/data/auth | `AuthRepository.settingsStore` | 移除注入或实现相关功能 |
| A-🟡-48 | AuthRepository.clearSessionAndCache方法名误导 | android/data/auth | `AuthRepository.clearSessionAndCache()` | 重命名为 clearSession() 或实际清除缓存 |
| A-🟡-49 | AuthRepository.refresh未被任何ViewModel调用 | android/data/auth | `AuthRepository.refresh()` | 确认是否需要，如需要则在 ViewModel 中集成 |
| A-🟡-50 | AgentRepository多个方法未被ViewModel调用 | android/data/agent | `generateOperationDraft()` 等6个方法 | 确认是否为未来功能预留，如不需要则移除 |
| A-🟡-51 | ReportRepository多个方法未被ViewModel调用 | android/data/report | `refundRecords()` 等6个方法 | 确认是否为未来功能预留，如不需要则移除 |
| A-🟡-52 | ProductRepository.findProductByCode无本地缓存且未被调用 | android/data/product | `ProductRepository.findProductByCode()` | 移除或实现本地缓存并集成到 ViewModel |
| A-🟡-53 | SaleOrderRepository.updateSaleDraft未被ViewModel调用 | android/data/order | `SaleOrderRepository.updateSaleDraft()` | 确认是否需要，如不需要则移除 |
| A-🟡-54 | SyncRepository双重存储增加复杂性 | android/data/sync | `SyncRepository` | 统一为数据库存储，移除偏好存储方式 |
| A-🟡-55 | SyncRepository.pull循环中异常处理不够健壮 | android/data/sync | `SyncRepository.pull()` | 增加容错机制，记录失败页面，继续同步后续页面 |
| A-🟡-56 | SyncRepository.applyPulledChanges未知entityType被静默忽略 | android/data/sync | `SyncRepository.applyPulledChanges()` | 记录警告日志或抛出异常 |
| A-🟡-57 | SyncRepository.DEFAULT_PULL_LIMIT建议提取到配置 | android/data/sync | `SyncRepository.DEFAULT_PULL_LIMIT` | 提取到配置文件或 BuildConfig |
| A-🟡-58 | AgentUiState.error使用String?而非UiMessage? | android/feature/agent | `AgentUiState.error` | 统一为 UiMessage? 类型 |
| A-🟡-59 | DashboardUiState.error使用String?而非UiMessage? | android/feature/dashboard | `DashboardUiState.error` | 统一为 UiMessage? 类型 |
| A-🟡-60 | DashboardUiState.workbench加载但未使用 | android/feature/dashboard | `DashboardUiState.workbench` | 在 Screen 中使用该数据或移除加载逻辑 |
| A-🟡-61 | DashboardScreen趋势数据是硬编码模拟数据 | android/feature/dashboard | `DashboardScreen` 趋势图表数据 | 从 ViewModel 获取真实趋势数据 |
| A-🟡-62 | LoginScreen/RegisterScreen缺少手机号格式校验 | android/feature/auth | `LoginScreen`, `RegisterScreen` | 添加手机号格式校验逻辑 |
| A-🟡-63 | RegisterScreen缺少密码可见性切换 | android/feature/auth | `RegisterScreen` | 添加 VisualTransformation 切换按钮 |
| A-🟡-64 | AuthViewModel.init中直接collect | android/feature/auth | `AuthViewModel.init` | 使用 stateIn 操作符替代直接 collect |
| A-🟡-65 | CustomerListScreen筛选逻辑在Screen端 | android/feature/customers | `CustomerListScreen` 筛选逻辑 | 将筛选逻辑下沉到 ViewModel |
| A-🟡-66 | CustomerViewModel.deleteCustomer Screen未暴露删除入口 | android/feature/customers | `CustomerViewModel.deleteCustomer()` | 在 UI 中添加删除按钮 |
| A-🟡-67 | FinanceRecordListScreen onNavigateToEditor参数未使用 | android/feature/finance | `FinanceRecordListScreen.onNavigateToEditor` | 实现导航逻辑或移除参数 |
| A-🟡-68 | FinanceRecordListScreen KPI在UI层计算 | android/feature/finance | `FinanceRecordListScreen` KPI 计算逻辑 | 将 KPI 计算下沉到 ViewModel 或 Repository 层 |
| A-🟡-69 | FinanceRecordEditorSheet建议提供常用分类快速选择 | android/feature/finance | `FinanceRecordEditorSheet` | 添加常用分类的快速选择标签列表 |
| A-🟡-70 | SaleOrderDetailScreen修改按钮空实现 | android/feature/sales | `SaleOrderDetailScreen` 修改按钮 | 实现导航到编辑页面的逻辑或移除按钮 |
| A-🟡-71 | SaleOrderDetailViewModel.loadDetail两个请求串行 | android/feature/sales | `SaleOrderDetailViewModel.loadDetail()` | 使用 async 并行请求 |
| A-🟡-72 | EditorLineItem.productId使用Long(0)表示未设置 | android/feature/sales | `EditorLineItem.productId` | 改为 Long? 类型，null 表示未设置 |
| A-🟡-73 | SupplierViewModel.deleteSupplier Screen未暴露删除入口 | android/feature/suppliers | `SupplierViewModel.deleteSupplier()` | 在 UI 中添加删除按钮 |
| A-🟡-74 | SettingsViewModel.init两个collect存在竞态条件 | android/feature/settings | `SettingsViewModel.init` | 使用 combine 操作符合并两个 Flow |
| A-🟡-75 | AgentQuickAction无点击回调 | android/feature/agent | `AgentQuickAction` | 添加 onClick 回调参数 |
| A-🟡-76 | AgentKpiCard仅是KpiCard的简单包装 | android/feature/agent | `AgentKpiCard` | 直接使用 KpiCard 或添加业务特定逻辑 |
| A-🟡-77 | ProductListScreen筛选逻辑在Screen端 | android/feature/products | `ProductListScreen` 筛选逻辑 | 将筛选逻辑下沉到 ViewModel |
| A-🟡-78 | ProductListViewModel.deleteProduct Screen未暴露删除入口 | android/feature/products | `ProductListViewModel.deleteProduct()` | 在 UI 中添加删除按钮 |

---

## ⚪ 低优先级（30项）

### 服务器端（18项）

| # | 问题 | 模块 | 涉及代码 | 建议 |
|---|------|------|----------|------|
| S-⚪-1 | ApiResponse.timestamp可选 | server/common | `ApiResponse.timestamp` | 考虑移除或改为请求ID |
| S-⚪-2 | ApiResponse.failure方法泛型歧义 | server/common | `ApiResponse.failure()` | 显式声明泛型类型 |
| S-⚪-3 | 错误信息国际化 | server/common | `GlobalExceptionHandler` | Handler 方法本身使用英文，中文来自业务层 IllegalArgumentException 透传；应统一使用 MessageSource 实现国际化 |
| S-⚪-4 | 验证错误仅返回第一个 | server/common | `GlobalExceptionHandler.handleValidation()` | 汇总所有字段错误返回 |
| S-⚪-5 | ConstraintViolation未返回字段信息 | server/common | `GlobalExceptionHandler.handleConstraint()` | 提取并返回违反约束的字段名和错误信息 |
| S-⚪-6 | 缺少请求追踪traceId | server/common | `GlobalExceptionHandler` | 在异常响应中加入 traceId |
| S-⚪-7 | 兜底异常日志未脱敏 | server/common | `GlobalExceptionHandler.handleUnknown()` | 客户端响应已脱敏（返回 "Internal server error"），但服务端日志 `log.error("Unhandled backend exception", ex)` 记录了完整异常栈 |
| S-⚪-8 | 时间戳使用Long缺少时区 | server/entity | 所有 createdAt/updatedAt 字段 | 迁移到 Instant 或 OffsetDateTime |
| S-⚪-9 | 缺少审计字段基类 | server/entity | 所有实体类 | 审计字段不统一（部分只有 createdAt 没有 updatedAt），不能简单抽取单一基类；建议分两层：CreatableEntity 和 AuditableEntity |
| S-⚪-10 | LocalDemoDataInitializer初始化判断过于简单 | server/config | `LocalDemoDataInitializer.run()` → `DemoDataService.seed()` | 实际检查了 `userRepository.count() > 0 && productRepository.count() > 0`（非仅检查用户），但判断仍不够健壮 |
| S-⚪-11 | 线程池大小硬编码 | server/config | `AgentTaskConfig.agentTaskExecutor()` → corePoolSize=2, maxPoolSize=4 | 提取到配置文件 |
| S-⚪-12 | SSE超时硬编码 | server/service | `AgentTaskService` → `SSE_TIMEOUT_MS = 30L * 60L * 1000L` | 提取到配置文件 |
| S-⚪-13 | PDF导出极简 | server/controller + service | `SaleOrderService.buildSimplePdf()` | 改用专业PDF库（iText/OpenPDF） |
| S-⚪-14 | Token无签名保护 | server/security | `TokenService.issueToken()` → UUID | TokenService 不使用 JWT，仅生成 UUID token 依赖数据库验证；可考虑引入 JWT 减少数据库查询，或增加 token 过期清理机制 |
| S-⚪-15 | AI层仅支持单一模型 | server/ai | `LongCatAnthropicClient` → `AgentLlmProperties.model` | 模型名可配置但仅支持单一模型，无法按任务类型切换 |
| S-⚪-16 | AI层API版本较旧 | server/ai | `AgentLlmProperties` → 默认 `2023-06-01` | 已可通过环境变量 `${AGENT_LLM_ANTHROPIC_VERSION}` 配置，但默认值较旧 |
| S-⚪-17 | AI层无速率限制 | server/ai | `LongCatAnthropicClient.createJsonMessage()` | 实现令牌桶或滑动窗口限流 |
| S-⚪-18 | SaleOrderController.updateDraft路径歧义 | server/controller | `updateDraft()` → 两个路径映射 | 仅保留 /{id}/draft 路径 |

> ❌ **已移除 S-⚪-14(旧)** "升级JWT签名算法" — TokenService 不使用 JWT（仅生成 UUID），HS256→RS256 的建议不适用
> ✏️ **S-⚪-3** 描述修正：Handler 方法本身用英文（非中文），中文来自业务层 IllegalArgumentException 透传
> ✏️ **S-⚪-7** 描述修正：客户端响应已脱敏，问题在服务端日志记录完整异常栈
> ✏️ **S-⚪-9** 描述修正：审计字段不统一，不能简单抽取单一基类
> ✏️ **S-⚪-10** 描述修正：实际检查了 user+product 两个表
> ✏️ **S-⚪-11** 方法名修正：`agentTaskExecutor()` 而非 `taskScheduler()`
> ✏️ **S-⚪-12** 位置修正：超时常量在 `AgentTaskService` 而非 `AgentTaskController`
> ✏️ **S-⚪-14(旧)→S-⚪-14** 重写：改为"Token无签名保护"（基于 UUID+数据库 Session 的实际架构）
> ✏️ **S-⚪-15** 描述修正：模型名可配置但仅支持单一模型，非硬编码
> ✏️ **S-⚪-16** 描述修正：API 版本已可通过环境变量配置
> ✏️ **S-⚪-17** 方法名修正：`createJsonMessage()` 而非 `chat()`

### 安卓端（10项）

| # | 问题 | 模块 | 涉及代码 | 建议 |
|---|------|------|----------|------|
| A-⚪-1 | ArchivesScreen新增按钮图标与DocumentsScreen不统一 | android/app | `ArchivesScreen`, `DocumentsScreen` | 统一使用相同图标风格 |
| A-⚪-2 | DocumentsScreen重选逻辑when分支可简化 | android/app | `DocumentsScreen` when 分支 | 使用范围判断（in 1..3） |
| A-⚪-3 | MainNavGraph建议使用类型安全导航 | android/app | `MainNavGraph` | 迁移到 TypeSafeNavigation API |
| A-⚪-4 | ZhihuijiDatabase exportSchema=false | android/core/database | `ZhihuijiDatabase` | 设置 exportSchema = true 并配置 schemaLocation |
| A-⚪-5 | SupplierDao.search搜索模式较完善其他DAO可参考 | android/core/database | `SupplierDao.search()` | 参考实现增强其他 DAO 搜索能力 |
| A-⚪-6 | BackdropEffectScope可变属性需文档说明 | android/backdrop | `BackdropEffectScope` → padding, renderEffect | 添加 KDoc 文档 |
| A-⚪-7 | InnerShadow blendMode阈值切换可能导致动画突变 | android/backdrop | `InnerShadow` blendMode 切换 | 使用平滑过渡或 lerp 插值替代 |
| A-⚪-8 | CombinedBackdrops vararg版本热路径开销 | android/backdrop | `CombinedBackdrops` vararg 版本 | 提供固定参数数量重载版本 |
| A-⚪-9 | ShapeProvider shapeBlock每次调用innerShape都执行 | android/backdrop | `ShapeProvider.shapeBlock()` | 添加记忆化缓存 |
| A-⚪-10 | GlassTopBar使用ExperimentalMaterial3Api | android/core/designsystem | `GlassTopBar` | 关注 Material3 API 稳定进展 |

---

## 按模块速查

点击模块名跳转到详细分析文档：

### 安卓端

| 模块 | 优化点数 | 🔴 | 🟠 | 🟡 | ⚪ | 详细文档 |
|------|---------|----|----|----|----|---------|
| app | 10 | 0 | 0 | 9 | 1 | [README.md](android/app/README.md) |
| backdrop | 10 | 0 | 0 | 8 | 2 | [README.md](android/backdrop/README.md) |
| core/common | 3 | 1 | 0 | 2 | 0 | [README.md](android/core/common/README.md) |
| core/database | 6 | 0 | 3 | 2 | 1 | [README.md](android/core/database/README.md) |
| core/datastore | 3 | 0 | 1 | 2 | 0 | [README.md](android/core/datastore/README.md) |
| core/designsystem | 13 | 1 | 1 | 10 | 1 | [README.md](android/core/designsystem/README.md) |
| core/model | 7 | 0 | 0 | 7 | 0 | [README.md](android/core/model/README.md) |
| core/network | 8 | 2 | 7 | 0 | 0 | [README.md](android/core/network/README.md) |
| data/agent | 1 | 0 | 0 | 1 | 0 | [README.md](android/data/agent/README.md) |
| data/auth | 3 | 0 | 0 | 3 | 0 | [README.md](android/data/auth/README.md) |
| data/customer | 0 | 0 | 0 | 0 | 0 | [README.md](android/data/customer/README.md) |
| data/finance | 1 | 0 | 1 | 0 | 0 | [README.md](android/data/finance/README.md) |
| data/order | 2 | 0 | 2 | 0 | 0 | [README.md](android/data/order/README.md) |
| data/product | 1 | 0 | 0 | 1 | 0 | [README.md](android/data/product/README.md) |
| data/report | 1 | 0 | 0 | 1 | 0 | [README.md](android/data/report/README.md) |
| data/supplier | 0 | 0 | 0 | 0 | 0 | [README.md](android/data/supplier/README.md) |
| data/sync | 4 | 0 | 0 | 4 | 0 | [README.md](android/data/sync/README.md) |
| feature/agent | 3 | 0 | 1 | 2 | 0 | [README.md](android/feature/agent/README.md) |
| feature/auth | 4 | 0 | 1 | 3 | 0 | [README.md](android/feature/auth/README.md) |
| feature/customers | 2 | 0 | 0 | 2 | 0 | [README.md](android/feature/customers/README.md) |
| feature/dashboard | 5 | 0 | 2 | 3 | 0 | [README.md](android/feature/dashboard/README.md) |
| feature/finance | 4 | 0 | 1 | 3 | 0 | [README.md](android/feature/finance/README.md) |
| feature/payments | 1 | 0 | 0 | 0 | 0 | [README.md](android/feature/payments/README.md) |
| feature/products | 2 | 0 | 0 | 2 | 0 | [README.md](android/feature/products/README.md) |
| feature/purchases | 1 | 0 | 0 | 0 | 0 | [README.md](android/feature/purchases/README.md) |
| feature/reports | 0 | 0 | 0 | 0 | 0 | [README.md](android/feature/reports/README.md) |
| feature/sales | 3 | 0 | 0 | 3 | 0 | [README.md](android/feature/sales/README.md) |
| feature/settings | 2 | 0 | 1 | 1 | 0 | [README.md](android/feature/settings/README.md) |
| feature/suppliers | 1 | 0 | 0 | 1 | 0 | [README.md](android/feature/suppliers/README.md) |

### 服务器端

| 模块 | 优化点数 | 🔴 | 🟠 | 🟡 | ⚪ | 详细文档 |
|------|---------|----|----|----|----|---------|
| api/controller | 6 | 1 | 1 | 4 | 1 | [README.md](server/api/controller/README.md) |
| api/dto | 5 | 0 | 1 | 3 | 1 | [README.md](server/api/dto/README.md) |
| api/common | 7 | 0 | 0 | 2 | 5 | [README.md](server/api/common/README.md) |
| service | 8 | 2 | 4 | 2 | 0 | [README.md](server/service/README.md) |
| entity | 7 | 0 | 2 | 2 | 3 | [README.md](server/entity/README.md) |
| repository | 5 | 0 | 1 | 2 | 2 | [README.md](server/repository/README.md) |
| infrastructure/config | 8 | 0 | 3 | 2 | 3 | [README.md](server/infrastructure/config/README.md) |
| infrastructure/security | 4 | 1 | 2 | 0 | 1 | [README.md](server/infrastructure/security/README.md) |
| infrastructure/ai | 7 | 0 | 1 | 3 | 3 | [README.md](server/infrastructure/ai/README.md) |
| resources | 6 | 2 | 2 | 1 | 1 | [README.md](server/resources/README.md) |

---

## 目录结构速览

```
docs/technical-analysis/
├── INDEX.md                          ← 本文件
├── android/
│   ├── app/README.md
│   ├── backdrop/README.md
│   ├── core/
│   │   ├── common/README.md
│   │   ├── database/README.md
│   │   ├── datastore/README.md
│   │   ├── designsystem/README.md
│   │   ├── model/README.md
│   │   └── network/README.md
│   ├── data/
│   │   ├── agent/README.md
│   │   ├── auth/README.md
│   │   ├── customer/README.md
│   │   ├── finance/README.md
│   │   ├── order/README.md
│   │   ├── product/README.md
│   │   ├── report/README.md
│   │   ├── supplier/README.md
│   │   └── sync/README.md
│   └── feature/
│       ├── agent/README.md
│       ├── auth/README.md
│       ├── customers/README.md
│       ├── dashboard/README.md
│       ├── finance/README.md
│       ├── payments/README.md
│       ├── products/README.md
│       ├── purchases/README.md
│       ├── reports/README.md
│       ├── sales/README.md
│       ├── settings/README.md
│       └── suppliers/README.md
└── server/
    ├── api/
    │   ├── common/README.md
    │   ├── controller/README.md
    │   └── dto/README.md
    ├── entity/README.md
    ├── infrastructure/
    │   ├── ai/README.md
    │   ├── config/README.md
    │   └── security/README.md
    ├── repository/README.md
    ├── resources/README.md
    └── service/README.md
```
