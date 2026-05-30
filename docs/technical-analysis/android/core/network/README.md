# network 技术分析

## 文件清单
- AuthInterceptor.kt
- NetworkConfig.kt
- NetworkModule.kt
- SafeApiCall.kt
- TokenAuthenticator.kt
- ZhihuijiApi.kt

---

## AuthInterceptor.kt

### AuthInterceptor
- class / 注解：@Singleton / 父类：okhttp3.Interceptor / 职责：OkHttp 拦截器，为非匿名请求自动添加 Authorization 请求头 / 设计模式：拦截器模式

#### AuthInterceptor(sessionStore: SessionStore)
- 参数：`sessionStore: SessionStore` — 会话存储，@Inject 自动注入
- 返回值：无（构造函数）
- 实现逻辑：保存 SessionStore 引用
- 调用关系：由 Hilt 注入到 OkHttpClient
- 建议：无

#### intercept(chain: Interceptor.Chain): Response
- 参数：`chain: Interceptor.Chain` — 拦截器链
- 返回值：`Response` — HTTP 响应
- 实现逻辑：1. 判断请求路径是否为匿名请求（登录/注册/刷新/验证码）；2. 若是匿名请求直接放行；3. 否则通过 runBlocking 从 SessionStore 获取 token；4. 若 token 非空则添加 "Authorization: Bearer {token}" 请求头；5. 继续请求链
- 调用关系：由 OkHttp 框架在每个请求前自动调用
- 建议：使用 runBlocking 阻塞线程获取 token，在 OkHttp 线程中可能导致死锁或性能问题，建议改用同步方式读取缓存值

#### isAnonymousRequest(path: String): Boolean
- 参数：`path: String` — 请求路径
- 返回值：`Boolean` — 是否为匿名请求
- 实现逻辑：判断路径是否以 "/auth/login"、"/auth/register"、"/auth/refresh"、"/auth/verify-code" 结尾
- 调用关系：被 intercept 内部调用
- 建议：使用 endsWith 匹配可能不够精确（如带查询参数时），建议使用 contains 或 URL 解析

---

## NetworkConfig.kt

### NetworkConfig
- object / 职责：网络配置常量和工具方法 / 设计模式：配置常量类

#### SERVER_117_BASE_URL: String
- 作用域：const / 初始值："http://117.72.79.106/zhihuiji/v1/" / 使用场景：默认服务器基础 URL
- 建议：硬编码 IP 地址，建议抽取到 BuildConfig 或远程配置

#### SERVER_124_HOST: String
- 作用域：private const / 初始值："124.222.153.108" / 使用场景：被屏蔽的旧服务器主机地址
- 建议：同上

#### baseUrl: String
- 作用域：var / 初始值：SERVER_117_BASE_URL / 使用场景：当前生效的基础 URL，可被 NetworkModule 动态更新
- 建议：可变全局状态，非线程安全，建议改为通过 DI 提供的只读配置

#### CONNECT_TIMEOUT: Long
- 作用域：const / 初始值：30L / 使用场景：连接超时（秒）
- 建议：无

#### READ_TIMEOUT: Long
- 作用域：const / 初始值：30L / 使用场景：读取超时（秒）
- 建议：无

#### WRITE_TIMEOUT: Long
- 作用域：const / 初始值：30L / 使用场景：写入超时（秒）
- 建议：无

#### normalizeBaseUrl(raw: String): String
- 参数：`raw: String` — 原始 URL 字符串
- 返回值：`String` — 规范化后的 URL
- 实现逻辑：trim 后若为空返回默认 URL；若包含旧服务器地址返回默认 URL；确保以 "/" 结尾
- 调用关系：被 NetworkModule.provideRetrofit 调用
- 建议：与 SettingsStore.normalizeBaseUrl 逻辑完全重复，建议统一到一处

---

## NetworkModule.kt

### NetworkModule
- object / 注解：@Module, @InstallIn(SingletonComponent::class) / 职责：通过 Hilt DI 提供网络相关实例（Json、OkHttpClient、Retrofit、Api） / 设计模式：依赖注入模块

#### provideJson(): Json
- 返回值：`Json` — kotlinx.serialization.json.Json 实例
- 实现逻辑：配置 Json — ignoreUnknownKeys=true（忽略未知字段）、coerceInputValues=true（强制默认值）、isLenient=true（宽松解析）、encodeDefaults=true（编码默认值）
- 调用关系：被 provideRetrofit 使用
- 建议：无

#### provideLoggingInterceptor(): HttpLoggingInterceptor
- 返回值：`HttpLoggingInterceptor` — 日志拦截器
- 实现逻辑：设置 Level.BODY（打印完整请求/响应体）
- 调用关系：被 provideOkHttpClient 使用
- 建议：生产环境应关闭或降级为 Level.NONE/HEADERS，避免敏感数据泄露

#### provideOkHttpClient(authInterceptor, tokenAuthenticator, loggingInterceptor): OkHttpClient
- 参数：`authInterceptor: AuthInterceptor` — 认证拦截器；`tokenAuthenticator: TokenAuthenticator` — Token 认证器；`loggingInterceptor: HttpLoggingInterceptor` — 日志拦截器
- 返回值：`OkHttpClient`
- 实现逻辑：添加 authInterceptor → loggingInterceptor → tokenAuthenticator，设置 30 秒超时
- 调用关系：被 provideRetrofit 使用
- 建议：拦截器顺序为 auth → logging，日志会记录带 Authorization 头的请求，生产环境需注意安全

#### provideRetrofit(client: OkHttpClient, json: Json, settingsStore: SettingsStore): Retrofit
- 参数：`client: OkHttpClient`；`json: Json`；`settingsStore: SettingsStore`
- 返回值：`Retrofit`
- 实现逻辑：通过 runBlocking 从 SettingsStore 读取 baseUrl → normalizeBaseUrl → 更新 NetworkConfig.baseUrl → 构建 Retrofit 实例
- 调用关系：被 provideZhihuijiApi 使用
- 建议：1. runBlocking 阻塞调用可能影响启动性能；2. Retrofit 为 Singleton，baseUrl 在首次创建后不可更改，切换服务器需重启应用

#### provideZhihuijiApi(retrofit: Retrofit): ZhihuijiApi
- 参数：`retrofit: Retrofit`
- 返回值：`ZhihuijiApi` — API 接口实例
- 实现逻辑：retrofit.create(ZhihuijiApi::class.java)
- 调用关系：被 Hilt 注入到各 Repository
- 建议：无

---

## SafeApiCall.kt

### NetworkException
- class / 父类：Exception / 职责：网络层异常，封装错误码和消息 / 设计模式：自定义异常

#### NetworkException(code: Int, message: String)
- 参数：`code: Int` — 错误码；`message: String` — 错误消息
- 返回值：无（构造函数）
- 实现逻辑：传递给父类 Exception
- 调用关系：由 safeApiCall 在错误时创建
- 建议：与 common 模块的 BusinessException 职责重叠，建议统一或明确分层

### safeApiCall — 顶层挂起函数
- 职责：安全执行 API 调用，统一处理成功/业务错误/网络错误/未知错误 / 设计模式：结果包装模式

#### safeApiCall(block: suspend () -> ApiResponse\<T\>): Result\<T\>
- 参数：`block: suspend () -> ApiResponse<T>` — API 调用 lambda
- 返回值：`Result<T>` — 成功包含 data，失败包含 NetworkException
- 实现逻辑：1. 执行 block 获取 ApiResponse；2. 若 code==0 且 data!=null 返回 Result.success；3. 否则返回 Result.failure(NetworkException)；4. 捕获 HttpException 转为 NetworkException；5. 捕获 IOException 返回 "网络连接失败" 错误；6. 捕获其他 Exception 返回通用错误
- 调用关系：被 Repository 层调用
- 建议：与 ResultExt.getOrThrow() 功能类似但异常类型不同，建议统一错误处理策略

---

## TokenAuthenticator.kt

### TokenAuthenticator
- class / 注解：@Singleton / 父类：okhttp3.Authenticator / 职责：当服务器返回 401 时自动刷新 Token 并重试请求 / 设计模式：认证器模式

#### TokenAuthenticator(sessionStore: SessionStore)
- 参数：`sessionStore: SessionStore` — 会话存储，@Inject 自动注入
- 返回值：无（构造函数）
- 实现逻辑：保存 SessionStore 引用
- 调用关系：由 Hilt 注入到 OkHttpClient
- 建议：无

#### authenticate(route: Route?, response: Response): Request?
- 参数：`route: Route?` — 路由信息；`response: Response` — 401 响应
- 返回值：`Request?` — 刷新成功返回带新 Token 的请求，失败返回 null
- 实现逻辑：1. 检查重试次数（>=2 返回 null）；2. runBlocking 获取 refreshToken；3. 创建新 OkHttpClient 发起刷新请求（避免递归调用 authenticator）；4. 解析刷新响应；5. 成功则保存新会话并返回带新 Token 的请求；6. 失败返回 null
- 调用关系：由 OkHttp 框架在收到 401 时自动调用
- 建议：1. 使用 runBlocking 可能阻塞 OkHttp 线程；2. 创建新的 OkHttpClient 实例绕过 authenticator 是正确做法；3. 刷新请求未添加 Content-Type 头，建议补充

#### responseCount(response: Response): Int
- 参数：`response: Response` — 当前响应
- 返回值：`Int` — 重试次数（含当前响应）
- 实现逻辑：遍历 priorResponse 链计算总响应次数
- 调用关系：被 authenticate 内部调用
- 建议：无

---

## ZhihuijiApi.kt

### ZhihuijiApi
- interface / 注解：Retrofit API 接口 / 职责：定义所有 HTTP API 端点 / 设计模式：API 接口定义模式

#### register(body: RegisterRequest): ApiResponse\<AuthResult\>
- HTTP：POST auth/register
- 参数：`body: RegisterRequest` — 注册请求体
- 返回值：`ApiResponse<AuthResult>`
- 建议：无

#### login(body: LoginRequest): ApiResponse\<AuthResult\>
- HTTP：POST auth/login
- 参数：`body: LoginRequest` — 登录请求体
- 返回值：`ApiResponse<AuthResult>`
- 建议：无

#### refresh(body: RefreshRequest): ApiResponse\<AuthResult\>
- HTTP：POST auth/refresh
- 参数：`body: RefreshRequest` — 刷新请求体
- 返回值：`ApiResponse<AuthResult>`
- 建议：无

#### logout(authorization: String?): ApiResponse\<Unit\>
- HTTP：POST auth/logout
- 参数：`authorization: String?` — Authorization 头值
- 返回值：`ApiResponse<Unit>`
- 建议：authorization 参数由调用方手动传入，与 AuthInterceptor 自动注入不一致，建议统一

#### verifyCode(body: VerifyCodeRequest): ApiResponse\<VerifyCodeResponse\>
- HTTP：POST auth/verify-code
- 参数：`body: VerifyCodeRequest` — 验证码请求体
- 返回值：`ApiResponse<VerifyCodeResponse>`
- 建议：无

#### me(authorization: String): ApiResponse\<UserProfile\>
- HTTP：GET auth/users/me
- 参数：`authorization: String` — Authorization 头值
- 返回值：`ApiResponse<UserProfile>`
- 建议：同 logout，authorization 应由拦截器自动添加

#### products(keyword: String?): ApiResponse\<List\<ProductDto\>\>
- HTTP：GET products
- 参数：`keyword: String? = null` — 搜索关键词
- 返回值：`ApiResponse<List<ProductDto>>`
- 建议：无

#### product(id: Long): ApiResponse\<ProductDto\>
- HTTP：GET products/{id}
- 参数：`id: Long` — 商品 ID
- 返回值：`ApiResponse<ProductDto>`
- 建议：无

#### productByCode(code: String): ApiResponse\<ProductDto?\>
- HTTP：GET products/by-code
- 参数：`code: String` — 商品编码
- 返回值：`ApiResponse<ProductDto?>` — 可能为空
- 建议：无

#### createProduct(body: ProductDto): ApiResponse\<ProductDto\>
- HTTP：POST products
- 参数：`body: ProductDto` — 商品数据
- 返回值：`ApiResponse<ProductDto>`
- 建议：使用 ProductDto 作为请求体包含过多字段，建议使用专用的 CreateRequest

#### updateProduct(id: Long, body: ProductDto): ApiResponse\<ProductDto\>
- HTTP：PUT products/{id}
- 参数：`id: Long`；`body: ProductDto`
- 返回值：`ApiResponse<ProductDto>`
- 建议：同上

#### adjustStock(id: Long, body: ProductAdjustStockRequest): ApiResponse\<ProductDto\>
- HTTP：POST products/{id}/adjust-stock
- 参数：`id: Long`；`body: ProductAdjustStockRequest`
- 返回值：`ApiResponse<ProductDto>`
- 建议：无

#### deleteProduct(id: Long): ApiResponse\<Unit\>
- HTTP：DELETE products/{id}
- 参数：`id: Long`
- 返回值：`ApiResponse<Unit>`
- 建议：无

#### customers(keyword: String?): ApiResponse\<List\<CustomerDto\>\>
- HTTP：GET customers
- 参数：`keyword: String? = null`
- 返回值：`ApiResponse<List<CustomerDto>>`
- 建议：无

#### customer(id: Long): ApiResponse\<CustomerDto\>
- HTTP：GET customers/{id}
- 参数：`id: Long`
- 返回值：`ApiResponse<CustomerDto>`
- 建议：无

#### createCustomer(body: CustomerDto): ApiResponse\<CustomerDto\>
- HTTP：POST customers
- 参数：`body: CustomerDto`
- 返回值：`ApiResponse<CustomerDto>`
- 建议：同 createProduct，建议使用专用 CreateRequest

#### updateCustomer(id: Long, body: CustomerDto): ApiResponse\<CustomerDto\>
- HTTP：PUT customers/{id}
- 参数：`id: Long`；`body: CustomerDto`
- 返回值：`ApiResponse<CustomerDto>`
- 建议：同上

#### deleteCustomer(id: Long): ApiResponse\<Unit\>
- HTTP：DELETE customers/{id}
- 参数：`id: Long`
- 返回值：`ApiResponse<Unit>`
- 建议：无

#### suppliers(keyword: String?, status: Int?): ApiResponse\<List\<SupplierDto\>\>
- HTTP：GET suppliers
- 参数：`keyword: String? = null`；`status: Int? = null`
- 返回值：`ApiResponse<List<SupplierDto>>`
- 建议：无

#### supplier(id: Long): ApiResponse\<SupplierDto\>
- HTTP：GET suppliers/{id}
- 参数：`id: Long`
- 返回值：`ApiResponse<SupplierDto>`
- 建议：无

#### createSupplier(body: SupplierDto): ApiResponse\<SupplierDto\>
- HTTP：POST suppliers
- 参数：`body: SupplierDto`
- 返回值：`ApiResponse<SupplierDto>`
- 建议：同上

#### updateSupplier(id: Long, body: SupplierDto): ApiResponse\<SupplierDto\>
- HTTP：PUT suppliers/{id}
- 参数：`id: Long`；`body: SupplierDto`
- 返回值：`ApiResponse<SupplierDto>`
- 建议：同上

#### deleteSupplier(id: Long): ApiResponse\<Unit\>
- HTTP：DELETE suppliers/{id}
- 参数：`id: Long`
- 返回值：`ApiResponse<Unit>`
- 建议：无

#### saleOrders(keyword, status, minTotalAmount, maxTotalAmount, createdAfter, createdBefore, productKeyword, paymentStatus): ApiResponse\<List\<SaleOrderDto\>\>
- HTTP：GET sale-orders
- 参数：8 个可选查询参数
- 返回值：`ApiResponse<List<SaleOrderDto>>`
- 建议：参数过多，建议封装为 QueryMap 或 Filter 对象

#### saleOrder(id: Long): ApiResponse\<SaleOrderDto\>
- HTTP：GET sale-orders/{id}
- 参数：`id: Long`
- 返回值：`ApiResponse<SaleOrderDto>`
- 建议：无

#### createSaleOrder(body: CreateSaleOrderRequest): ApiResponse\<SaleOrderDto\>
- HTTP：POST sale-orders
- 参数：`body: CreateSaleOrderRequest`
- 返回值：`ApiResponse<SaleOrderDto>`
- 建议：无

#### updateSaleDraft(id: Long, body: UpdateSaleDraftRequest): ApiResponse\<SaleOrderDto\>
- HTTP：PUT sale-orders/{id}/draft
- 参数：`id: Long`；`body: UpdateSaleDraftRequest`
- 返回值：`ApiResponse<SaleOrderDto>`
- 建议：无

#### addSalePayment(id: Long, body: PaymentRequest): ApiResponse\<PaymentDto\>
- HTTP：POST sale-orders/{id}/payments
- 参数：`id: Long`；`body: PaymentRequest`
- 返回值：`ApiResponse<PaymentDto>`
- 建议：无

#### salePayments(id: Long): ApiResponse\<List\<PaymentDto\>\>
- HTTP：GET sale-orders/{id}/payments
- 参数：`id: Long`
- 返回值：`ApiResponse<List<PaymentDto>>`
- 建议：无

#### updateSaleStatus(id: Long, body: StatusRequest): ApiResponse\<Unit\>
- HTTP：PUT sale-orders/{id}/status
- 参数：`id: Long`；`body: StatusRequest`
- 返回值：`ApiResponse<Unit>`
- 建议：无

#### cancelSaleOrder(id: Long): ApiResponse\<SaleOrderDto\>
- HTTP：PUT sale-orders/{id}/cancel
- 参数：`id: Long`
- 返回值：`ApiResponse<SaleOrderDto>`
- 建议：无

#### purchaseOrders(keyword: String?, status: Int?): ApiResponse\<List\<PurchaseOrderDto\>\>
- HTTP：GET purchase-orders
- 参数：`keyword: String? = null`；`status: Int? = null`
- 返回值：`ApiResponse<List<PurchaseOrderDto>>`
- 建议：无

#### purchaseOrder(id: Long): ApiResponse\<PurchaseOrderDto\>
- HTTP：GET purchase-orders/{id}
- 参数：`id: Long`
- 返回值：`ApiResponse<PurchaseOrderDto>`
- 建议：无

#### createPurchaseOrder(body: CreatePurchaseOrderRequest): ApiResponse\<PurchaseOrderDto\>
- HTTP：POST purchase-orders
- 参数：`body: CreatePurchaseOrderRequest`
- 返回值：`ApiResponse<PurchaseOrderDto>`
- 建议：无

#### payOrders(keyword, status, createdAfter, createdBefore): ApiResponse\<List\<PayOrderDto\>\>
- HTTP：GET pay-orders
- 参数：4 个可选查询参数
- 返回值：`ApiResponse<List<PayOrderDto>>`
- 建议：无

#### payOrder(id: Long): ApiResponse\<PayOrderDto\>
- HTTP：GET pay-orders/{id}
- 参数：`id: Long`
- 返回值：`ApiResponse<PayOrderDto>`
- 建议：无

#### createPayOrder(body: CreatePayOrderRequest): ApiResponse\<PayOrderDto\>
- HTTP：POST pay-orders
- 参数：`body: CreatePayOrderRequest`
- 返回值：`ApiResponse<PayOrderDto>`
- 建议：无

#### updatePayOrderStatus(id: Long, body: StatusRequest): ApiResponse\<PayOrderDto\>
- HTTP：PUT pay-orders/{id}/status
- 参数：`id: Long`；`body: StatusRequest`
- 返回值：`ApiResponse<PayOrderDto>`
- 建议：无

#### financeRecords(keyword, type, createdAfter, createdBefore): ApiResponse\<List\<FinanceRecordDto\>\>
- HTTP：GET finance-records
- 参数：4 个可选查询参数
- 返回值：`ApiResponse<List<FinanceRecordDto>>`
- 建议：无

#### createFinanceRecord(body: CreateFinanceRecordRequest): ApiResponse\<FinanceRecordDto\>
- HTTP：POST finance-records
- 参数：`body: CreateFinanceRecordRequest`
- 返回值：`ApiResponse<FinanceRecordDto>`
- 建议：无

#### salesSummary(startAt: Long, endAt: Long): ApiResponse\<SalesSummaryReportDto\>
- HTTP：GET reports/sales-summary
- 参数：`startAt: Long`；`endAt: Long`
- 返回值：`ApiResponse<SalesSummaryReportDto>`
- 建议：无

#### profitSummary(startAt: Long, endAt: Long): ApiResponse\<ProfitSummaryReportDto\>
- HTTP：GET reports/profit-summary
- 参数：`startAt: Long`；`endAt: Long`
- 返回值：`ApiResponse<ProfitSummaryReportDto>`
- 建议：无

#### refundRecords(startAt: Long, endAt: Long, limit: Int): ApiResponse\<List\<RefundRecordReportDto\>\>
- HTTP：GET reports/refund-records
- 参数：`startAt: Long`；`endAt: Long`；`limit: Int = 10`
- 返回值：`ApiResponse<List<RefundRecordReportDto>>`
- 建议：无

#### stockOutRecords(startAt: Long, endAt: Long, limit: Int): ApiResponse\<List\<StockOutRecordReportDto\>\>
- HTTP：GET reports/stock-out-records
- 参数：`startAt: Long`；`endAt: Long`；`limit: Int = 10`
- 返回值：`ApiResponse<List<StockOutRecordReportDto>>`
- 建议：无

#### topProducts(startAt: Long, endAt: Long, limit: Int): ApiResponse\<List\<TopSellingProductReportDto\>\>
- HTTP：GET reports/top-products
- 参数：`startAt: Long`；`endAt: Long`；`limit: Int = 10`
- 返回值：`ApiResponse<List<TopSellingProductReportDto>>`
- 建议：无

#### profitByProducts(startAt: Long, endAt: Long, limit: Int): ApiResponse\<List\<ProfitByProductReportDto\>\>
- HTTP：GET reports/profit-by-products
- 参数：`startAt: Long`；`endAt: Long`；`limit: Int = 10`
- 返回值：`ApiResponse<List<ProfitByProductReportDto>>`
- 建议：无

#### profitByCustomers(startAt: Long, endAt: Long, limit: Int): ApiResponse\<List\<ProfitByCustomerReportDto\>\>
- HTTP：GET reports/profit-by-customers
- 参数：`startAt: Long`；`endAt: Long`；`limit: Int = 10`
- 返回值：`ApiResponse<List<ProfitByCustomerReportDto>>`
- 建议：无

#### inventoryFlow(startAt: Long, endAt: Long, limit: Int): ApiResponse\<List\<InventoryFlowRecordDto\>\>
- HTTP：GET reports/inventory-flow
- 参数：`startAt: Long`；`endAt: Long`；`limit: Int = 10`
- 返回值：`ApiResponse<List<InventoryFlowRecordDto>>`
- 建议：无

#### customerSales(startAt: Long, endAt: Long, limit: Int): ApiResponse\<List\<CustomerSalesReportDto\>\>
- HTTP：GET reports/customer-sales
- 参数：`startAt: Long`；`endAt: Long`；`limit: Int = 10`
- 返回值：`ApiResponse<List<CustomerSalesReportDto>>`
- 建议：无

#### topReceivableCustomers(limit: Int): ApiResponse\<List\<CustomerReceivableReportDto\>\>
- HTTP：GET reports/top-receivable-customers
- 参数：`limit: Int = 10`
- 返回值：`ApiResponse<List<CustomerReceivableReportDto>>`
- 建议：无

#### lowStockProducts(limit: Int): ApiResponse\<List\<LowStockProductReportDto\>\>
- HTTP：GET reports/low-stock-products
- 参数：`limit: Int = 10`
- 返回值：`ApiResponse<List<LowStockProductReportDto>>`
- 建议：无

#### reconciliationSummary(startAt: Long, endAt: Long): ApiResponse\<ReconciliationSummaryReportDto\>
- HTTP：GET reports/reconciliation-summary
- 参数：`startAt: Long`；`endAt: Long`
- 返回值：`ApiResponse<ReconciliationSummaryReportDto>`
- 建议：无

#### syncHealth(): ApiResponse\<SyncHealthResult\>
- HTTP：GET sync/health
- 返回值：`ApiResponse<SyncHealthResult>`
- 建议：无

#### pull(body: PullRequest): ApiResponse\<PullResult\>
- HTTP：POST sync/pull
- 参数：`body: PullRequest`
- 返回值：`ApiResponse<PullResult>`
- 建议：无

#### upload(body: UploadRequest): ApiResponse\<UploadResult\>
- HTTP：POST sync/upload
- 参数：`body: UploadRequest`
- 返回值：`ApiResponse<UploadResult>`
- 建议：无

#### agentWorkbench(windowDays: Int, limit: Int, agingDays: Int): ApiResponse\<AgentWorkbenchDto\>
- HTTP：GET agent/workbench
- 参数：`windowDays: Int = 7`；`limit: Int = 6`；`agingDays: Int = 15`
- 返回值：`ApiResponse<AgentWorkbenchDto>`
- 建议：无

#### agentQuery(body: AgentQueryRequest): ApiResponse\<AgentAnswerDto\>
- HTTP：POST agent/query
- 参数：`body: AgentQueryRequest`
- 返回值：`ApiResponse<AgentAnswerDto>`
- 建议：无

#### operationDraft(body: OperationDraftRequest): ApiResponse\<OperationDraftDto\>
- HTTP：POST agent/operation-draft
- 参数：`body: OperationDraftRequest`
- 返回值：`ApiResponse<OperationDraftDto>`
- 建议：无

#### operationSubmit(body: OperationSubmitRequest): ApiResponse\<OperationSubmitResultDto\>
- HTTP：POST agent/operation-submit
- 参数：`body: OperationSubmitRequest`
- 返回值：`ApiResponse<OperationSubmitResultDto>`
- 建议：无

#### createAgentTask(body: CreateAgentTaskRequest): ApiResponse\<AgentTaskSummaryDto\>
- HTTP：POST agent/tasks
- 参数：`body: CreateAgentTaskRequest`
- 返回值：`ApiResponse<AgentTaskSummaryDto>`
- 建议：无

#### agentTasks(): ApiResponse\<List\<AgentTaskSummaryDto\>\>
- HTTP：GET agent/tasks
- 返回值：`ApiResponse<List<AgentTaskSummaryDto>>`
- 建议：无

#### agentTask(taskId: Long): ApiResponse\<AgentTaskDetailDto\>
- HTTP：GET agent/tasks/{taskId}
- 参数：`taskId: Long`
- 返回值：`ApiResponse<AgentTaskDetailDto>`
- 建议：无

#### notifications(unreadOnly: Boolean, undeliveredOnly: Boolean): ApiResponse\<List\<AgentNotificationDto\>\>
- HTTP：GET agent/notifications
- 参数：`unreadOnly: Boolean = false`；`undeliveredOnly: Boolean = false`
- 返回值：`ApiResponse<List<AgentNotificationDto>>`
- 建议：无

#### markNotificationRead(notificationId: Long): ApiResponse\<AgentNotificationDto\>
- HTTP：POST agent/notifications/{notificationId}/read
- 参数：`notificationId: Long`
- 返回值：`ApiResponse<AgentNotificationDto>`
- 建议：无

#### markNotificationDelivered(notificationId: Long): ApiResponse\<AgentNotificationDto\>
- HTTP：POST agent/notifications/{notificationId}/delivered
- 参数：`notificationId: Long`
- 返回值：`ApiResponse<AgentNotificationDto>`
- 建议：无
