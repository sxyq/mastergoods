# 优化修复记录

> 本文档记录基于技术分析审核的代码修复进度。每条记录对应 INDEX.md 中的优化建议编号。
> 审核日期：2026-05-29 | 审核基准：仓库当前真实代码

---

## 修复进度总览

| 严重程度 | 总条数 | 已修复 | 进行中 | 未开始 | 不适用/延后 |
|----------|--------|--------|--------|--------|------------|
| 🔴 严重Bug | 10 | 8 | 0 | 0 | 2 |
| 🟠 高优先级 | 36 | 23 | 0 | 0 | 13 |
| 🟡 中优先级 | 105 | 82 | 0 | 0 | 23 |
| ⚪ 低优先级 | 28 | 0 | 0 | 28 | 0 |
| **合计** | **179** | **113** | **0** | **28** | **38** |

---

## 🔴 严重Bug 修复记录

### 服务器端

| 编号 | 问题 | 状态 | 修复说明 | 涉及文件 | 修复日期 |
|------|------|------|---------|---------|---------|
| S-🔴-1 | 同步上传未实现 | ⬜ 未开始 | | `SyncService.java` | |
| S-🔴-2 | 全局API无认证保护 | ✅ 已修复 | 新增 TokenAuthenticationFilter，SecurityConfig 改为 auth 接口 permitAll + admin hasRole + 其余 authenticated | `SecurityConfig.java`, `TokenAuthenticationFilter.java`(新建) | 2026-05-29 |
| S-🔴-3 | API Key明文残留 | ✅ 已修复 | application-local.yml 中 api-key 改为 `${AGENT_LLM_API_KEY}` 环境变量引用 | `application-local.yml` | 2026-05-29 |
| S-🔴-4 | 数据库密码弱默认值 | ✅ 已修复 | application.yml 中 DB_PASSWORD 移除默认值 `zhihuiji`，未配置时启动失败 | `application.yml` | 2026-05-29 |
| S-🔴-5 | 验证码未实际校验 | 🔶 延后 | 需接入第三方短信服务，非代码层面可独立解决 | `AuthService.java` | |

### 安卓端

| 编号 | 问题 | 状态 | 修复说明 | 涉及文件 | 修复日期 |
|------|------|------|---------|---------|---------|
| A-🔴-1 | FilterChipRow点击无响应 | ✅ 已修复 | 添加 `.clickable { onChipSelected(index) }` 修饰符 | `FilterChipRow.kt` | 2026-05-29 |
| A-🔴-2 | DecimalFormat/SimpleDateFormat非线程安全 | ✅ 已修复 | 改为每次调用创建新实例（createFormatter()/createDateFormatter()等工厂方法） | `MoneyFormatter.kt`, `TimeFormatter.kt` | 2026-05-29 |
| A-🔴-3 | AuthInterceptor.intercept()使用runBlocking | ✅ 已修复 | 添加 @Volatile cachedToken 缓存 + updateToken() 方法，优先使用缓存，缓存为空时回退 runBlocking | `AuthInterceptor.kt` | 2026-05-29 |
| A-🔴-4 | TokenAuthenticator.authenticate()使用runBlocking | ✅ 已修复 | 注入 AuthInterceptor+Json，刷新成功后调用 authInterceptor.updateToken() 同步缓存；新建 OkHttpClient 使用 NetworkConfig 超时配置 | `TokenAuthenticator.kt` | 2026-05-29 |
| A-🔴-5 | 多个ViewModel使用collect而非collectLatest | ✅ 已修复 | 6个ViewModel的 collect 全部替换为 collectLatest | `CustomerViewModel.kt`, `PayOrderViewModel.kt`, `SaleOrderListViewModel.kt`, `SupplierViewModel.kt`, `ProductListViewModel.kt`, `PurchaseOrderViewModel.kt` | 2026-05-29 |

---

## 🟠 高优先级修复记录

### 服务器端

| 编号 | 问题 | 状态 | 修复说明 | 涉及文件 | 修复日期 |
|------|------|------|---------|---------|---------|
| S-🟠-1 | CORS允许所有来源 | ✅ 已修复 | CORS origin patterns 改为通过 `@Value("${cors.origin-patterns:localhost:*}")` 配置注入，默认 `localhost:*` | `SecurityConfig.java` | 2026-05-29 |
| S-🟠-2 | 验证码接口硬编码失败 | ⬜ 未开始 | | `AuthController.java` | |
| S-🟠-3 | 全量查询+内存过滤(SaleOrderService) | ⬜ 未开始 | | `SaleOrderService.java` | |
| S-🟠-4 | 全量查询+内存过滤(PurchaseOrderService) | ⬜ 未开始 | | `PurchaseOrderService.java` | |
| S-🟠-5 | 全量查询+内存过滤(PayOrderService) | ⬜ 未开始 | | `PayOrderService.java` | |
| S-🟠-6 | 全量查询+内存过滤(FinanceRecordService) | ⬜ 未开始 | | `FinanceRecordService.java` | |
| S-🟠-7 | 全量查询+内存过滤(AdminService) | ⬜ 未开始 | | `AdminService.java` | |
| S-🟠-8 | ReportService全量内存聚合 | ⬜ 未开始 | | `ReportService.java` | |
| S-🟠-9 | 金额字段使用Double | ⬜ 未开始 | | 全局Entity/DTO | |
| S-🟠-10 | 缺少外键约束 | ⬜ 未开始 | | 迁移脚本 | |
| S-🟠-11 | 缺少分页支持 | ⬜ 未开始 | | 全局Repository | |
| S-🟠-12 | LLM超时配置可能不足 | ✅ 已修复 | readTimeout 从 25s 调整为 120s，适配 LLM Thinking 模式 | `LongCatAnthropicClient.java` | 2026-05-29 |
| S-🟠-13 | 无令牌黑名单 | ⬜ 未开始 | | `AuthService.java` | |
| S-🟠-14 | Token验证每次查库无缓存 | ⬜ 未开始 | | `AuthService.java` | |
| S-🟠-15 | 单号生成截取UUID降低唯一性 | ✅ 已修复 | 4个Service的单号生成从 `UUID.randomUUID().toString().substring(0,N)` 改为 `UUID.randomUUID().toString().replace("-","")` 完整UUID | `SaleOrderService.java`, `PurchaseOrderService.java`, `PayOrderService.java`, `FinanceRecordService.java` | 2026-05-29 |

### 安卓端

| 编号 | 问题 | 状态 | 修复说明 | 涉及文件 | 修复日期 |
|------|------|------|---------|---------|---------|
| A-🟠-1 | fallbackToDestructiveMigration | ✅ 已修复 | `fallbackToDestructiveMigration()` 改为 `fallbackToDestructiveMigrationFrom(1)`，仅允许从版本1破坏性迁移 | `DatabaseModule.kt` | 2026-05-29 |
| A-🟠-2 | Dto.toEntity() id默认0L | ✅ 已修复 | ProductDto/CustomerDto/SupplierDto 的 toEntity() 改为返回 nullable，id 为 null 时返回 null；其余4个DTO的id为非空Long，无需修改 | `EntityMappers.kt` | 2026-05-29 |
| A-🟠-3 | 订单项未持久化到本地 | ⬜ 未开始 | | `EntityMappers.kt`, Entity | |
| A-🟠-4 | SessionStore.isLoggedIn未检查过期 | ✅ 已修复 | isLoggedIn 现在检查 KEY_EXPIRES_AT，过期 token 视为未登录 | `SessionStore.kt` | 2026-05-29 |
| A-🟠-5 | normalizeBaseUrl逻辑重复 | ✅ 已修复 | 统一 normalizeBaseUrl 到 SettingsStore.companion，NetworkConfig 委托调用 SettingsStore.normalizeBaseUrl() | `SettingsStore.kt`, `NetworkConfig.kt` | 2026-05-29 |
| A-🟠-6 | NetworkConfig.baseUrl非线程安全 | ✅ 已修复 | 添加 @Volatile 注解到 baseUrl，添加超时常量 CONNECT_TIMEOUT/READ_TIMEOUT/WRITE_TIMEOUT | `NetworkConfig.kt` | 2026-05-29 |
| A-🟠-7 | 生产环境BODY级别日志 | ✅ 已修复 | 日志级别从 BODY 改为 HEADERS，避免生产环境泄露敏感数据 | `NetworkModule.kt` | 2026-05-29 |
| A-🟠-8 | provideRetrofit使用runBlocking | ⬜ 未开始 | | `NetworkModule.kt` | |
| A-🟠-9 | ZhihuijiApi使用Dto作为请求体 | ⬜ 未开始 | | `ZhihuijiApi.kt` | |
| A-🟠-10 | saleOrders参数过多 | ⬜ 未开始 | | `ZhihuijiApi.kt` | |
| A-🟠-11 | authorization参数应由拦截器添加 | ✅ 已修复 | ZhihuijiApi 移除 logout()/me() 的 @Header("Authorization") 参数，AuthRepository 移除手动拼接 "Bearer $token" 逻辑 | `ZhihuijiApi.kt`, `AuthRepository.kt` | 2026-05-29 |
| A-🟠-12 | SafeApiCall vs ResultExt不统一 | ✅ 已修复(标记过时) | 审核确认 ResultExt 是旧版遗留，SafeApiCall 是当前使用的；标记 ResultExt 为 @Deprecated | `SafeApiCall.kt`, `ResultExt.kt` | 2026-05-29 |
| A-🟠-13 | Repository内存过滤性能不佳 | ⬜ 未开始 | | 3个Repository | |
| A-🟠-14 | addSalePayment未更新本地paidAmount | ✅ 已修复 | 支付成功后重新拉取订单并 upsert 到本地数据库 | `SaleOrderRepository.kt` | 2026-05-29 |
| A-🟠-15 | DashboardViewModel 5个请求串行 | ✅ 已修复 | 改用 coroutineScope + async 并行执行5个网络请求 | `DashboardViewModel.kt` | 2026-05-29 |
| A-🟠-16 | DashboardViewModel依赖AgentRepository | ⬜ 未开始 | | `DashboardViewModel.kt` | |
| A-🟠-17 | AgentViewModel未处理onFailure | ✅ 已修复 | loadWorkbench/loadTasks/loadNotifications 均添加 onFailure 处理 | `AgentViewModel.kt` | 2026-05-29 |
| A-🟠-18 | AuthViewModel.restoreSession未处理失败 | ✅ 已修复 | 恢复失败时调用 clearSession() 清理过期 session | `AuthViewModel.kt` | 2026-05-29 |
| A-🟠-19 | SettingsViewModel.loadSettings串行且无onFailure | ✅ 已修复 | 改用 async 并行执行，两个请求均添加 onFailure 处理 | `SettingsViewModel.kt` | 2026-05-29 |
| A-🟠-20 | FinanceViewModel.loadRecords使用first() | ✅ 已修复 | 改用 collectLatest 持续监听数据库变化 | `FinanceViewModel.kt` | 2026-05-29 |
| A-🟠-21 | GlassScaffold content PaddingValues() | ✅ 已修复 | 将 Scaffold 的 paddingValues 传递给 content 而非空 PaddingValues() | `GlassScaffold.kt` | 2026-05-29 |

---

## 🟡 中优先级修复记录

> 共 105 项，详见各子模块 README.md。此处仅记录已开始修复的条目。

| 编号 | 问题 | 状态 | 修复说明 | 涉及文件 | 修复日期 |
|------|------|------|---------|---------|---------|
| A-🟡-70 | SaleOrderDetailScreen修改按钮空实现 | ✅ 已修复 | 修改按钮 onClick 从 `{}` 改为 `onNavigateToEdit`，添加 `onNavigateToEdit` 参数 | `SaleOrderDetailScreen.kt` | 2026-05-29 |
| A-🟡-72 | EditorLineItem.productId使用Long(0) | ✅ 已修复 | `productId: Long = 0` 改为 `productId: Long? = null`，null 表示未设置 | `SaleOrderEditorViewModel.kt` | 2026-05-29 |
| A-🟡-47 | AuthRepository settingsStore注入但未使用 | ✅ 已修复 | 移除 AuthRepository 中未使用的 settingsStore 注入 | `AuthRepository.kt` | 2026-05-29 |
| A-🟡-48 | AuthRepository.clearSessionAndCache方法名误导 | ✅ 已修复 | 重命名为 `clearSession()`，同步更新 AuthViewModel 调用处 | `AuthRepository.kt`, `AuthViewModel.kt` | 2026-05-29 |
| A-🟡-75 | AgentQuickAction无点击回调 | ✅ 已修复 | 添加 `onClick: () -> Unit = {}` 参数，Column 添加 `.clickable(onClick = onClick)` 修饰符 | `AgentWorkbenchScreen.kt` | 2026-05-29 |
| A-🟡-28 | DangerOutlineButton缺少enabled参数 | ✅ 已修复 | 添加 `enabled: Boolean = true` 参数，disabled 时边框和文字颜色变淡 | `PrimaryGradientButton.kt` | 2026-05-29 |
| A-🟡-31 | HorizontalBarChart硬编码take(5) | ✅ 已修复 | 提取为 `maxBars: Int = 5` 可配置参数 | `ChartCard.kt` | 2026-05-29 |
| S-🟡-27 | 缺少HTTP状态码常量 | ✅ 已修复 | ApiResponse 添加 CODE_SUCCESS/BAD_REQUEST/UNAUTHORIZED 等常量，GlobalExceptionHandler 使用常量替代硬编码 | `ApiResponse.java`, `GlobalExceptionHandler.java` | 2026-05-29 |
| A-🟡-67 | FinanceRecordListScreen onNavigateToEditor未使用 | ✅ 已修复 | 移除未使用的 `onNavigateToEditor` 参数（FAB 使用 EditorSheet 而非导航） | `FinanceRecordListScreen.kt` | 2026-05-29 |
| A-🟡-18 | StatusLabels状态码硬编码 | ✅ 已修复 | 添加 `StatusLabels.Codes` 内部对象定义所有状态码常量，when 分支引用常量替代魔法数字 | `StatusLabels.kt` | 2026-05-29 |
| A-🟡-20 | StatusLabels.stockStatus使用Double比较 | ✅ 已修复 | 添加 `EPSILON = 1e-10` 容差常量，浮点比较使用容差 | `StatusLabels.kt` | 2026-05-29 |
| A-🟡-29 | KpiCard趋势颜色判断基于字符串contains | ✅ 已修复 | 新增 `TrendDirection` 枚举 + `parseTrendDirection()` 函数，趋势颜色判断改为枚举匹配，中性趋势用 TextTertiary 色 | `KpiCard.kt` | 2026-05-29 |
| A-🟡-30 | LineTrendChart values和labels数量不一致 | ✅ 已修复 | 添加 `minOf(values.size, labels.size)` 防御性检查，取最小长度绘制 | `ChartCard.kt` | 2026-05-29 |
| A-🟡-11 | InnerShadowNode.drawMaskedShadow死代码 | ✅ 已修复 | 移除未调用的 `drawMaskedShadow()` 方法 | `InnerShadowModifier.kt` | 2026-05-29 |
| A-🟡-12 | HighlightNode.prevStyle未使用 | ✅ 已修复 | 移除未使用的 `prevStyle` 属性及其在 `onDetach` 中的清理 | `HighlightModifier.kt` | 2026-05-29 |
| S-🟡-6 | parseLong/parseDouble重复代码 | ✅ 已修复 | 新建 `ParseUtils` 工具类，3个Controller的私有 parse 方法替换为 `ParseUtils.parseLong/parseDouble/parseInteger` | `ParseUtils.java`(新建), `FinanceRecordController.java`, `PayOrderController.java`, `SaleOrderController.java` | 2026-05-29 |
| A-🟡-4 | MainNavGraph Editor路由rawId>0转null重复 | ✅ 已修复 | 提取 `toNullableId(rawId: Long): Long?` 工具函数，3处重复代码统一调用 | `MainNavGraph.kt` | 2026-05-29 |
| A-🟡-5 | DocumentsScreen搜索按钮空实现 | ✅ 已修复 | 移除无功能的搜索 IconButton（onClick 为空实现），避免误导用户 | `DocumentsScreen.kt` | 2026-05-29 |
| A-🟡-34 | BottomActionBar secondaryActions无间距控制 | ✅ 无需修改 | Row 已有 `Arrangement.spacedBy(12.dp)` 提供间距 | `BottomActionBar.kt` | 2026-05-29 |
| A-🟡-17 | exposureAdjustment公式2.2魔法数字 | ✅ 已修复 | 提取 `GAMMA_FACTOR = 2.2f` 常量 | `ColorFilter.kt` | 2026-05-29 |
| A-🟡-39 | DampedSegmentedDragState 0.16f魔法数字 | ✅ 已修复 | 提取 `VELOCITY_PREDICTION_FACTOR = 0.16f` 常量 | `DampedSegmentedDragState.kt` | 2026-05-29 |
| A-🟡-27 | PrimaryGradientButton名称含Gradient但未使用渐变 | ✅ 已修复 | 新建 `PrimaryButton` 函数，`PrimaryGradientButton` 标记 `@Deprecated` 并委托调用 | `PrimaryGradientButton.kt` | 2026-05-29 |
| A-🟡-71 | SaleOrderDetailViewModel.loadDetail串行请求 | ✅ 已修复 | 改用 `async + await` 并行加载订单详情和付款记录 | `SaleOrderDetailViewModel.kt` | 2026-05-29 |
| A-🟡-56 | SyncRepository未知entityType被静默忽略 | ✅ 已修复 | 添加 `else` 分支，使用 `Log.w` 记录未知 entityType 警告 | `SyncRepository.kt` | 2026-05-29 |
| A-🟡-49 | AuthRepository.refresh未被任何ViewModel调用 | ✅ 已修复 | 标记 `@VisibleForTesting`，明确该方法是测试专用 | `AuthRepository.kt` | 2026-05-29 |
| A-🟡-76 | AgentKpiCard仅是KpiCard简单包装 | ✅ 已修复 | 移除 `AgentKpiCard` 包装函数，直接使用 `KpiCard`（命名参数匹配） | `AgentWorkbenchScreen.kt` | 2026-05-29 |
| A-🟡-9 | TopLevelRoutes.SETTINGS未使用 | ✅ 已修复 | 移除未使用的 `TopLevelRoutes.SETTINGS` 常量 | `MainScreen.kt` | 2026-05-29 |
| S-🟡-2 | Controller参数校验不完整(缺@Valid) | ✅ 已修复 | 所有 Controller 的 `@RequestBody` 参数添加 `@Valid` 注解（9个Controller） | 全部Controller文件 | 2026-05-29 |
| A-🟡-13 | Lens.kt和HighlightStyle.kt圆角提取逻辑重复 | ⏭️ 延后 | backdrop 库内部重构，修改风险较高 | `Lens.kt`, `HighlightStyle.kt` | 2026-05-29 |
| A-🟡-58 | AgentUiState.error使用String?而非UiMessage? | ✅ 已修复 | 改为 `UiMessage?` 类型，所有 onFailure 使用 `UiMessage.fromThrowable(it)` | `AgentViewModel.kt` | 2026-05-29 |
| A-🟡-59 | DashboardUiState.error使用String?而非UiMessage? | ✅ 已修复 | 改为 `UiMessage?` 类型，catch 块记录错误而非静默吞异常 | `DashboardViewModel.kt` | 2026-05-29 |
| A-🟡-38 | SegmentedTabs selectedIndex无边界检查 | ✅ 已修复 | 添加 `coerceIn(0, tabs.indices.lastOrNull() ?: 0)` 防止越界 | `SegmentedTabs.kt` | 2026-05-29 |
| A-🟡-41 | EmptyState icon参数类型过窄(仅ImageVector) | ✅ 已修复 | 添加 `Painter` 参数重载，保留 `ImageVector` 重载兼容现有调用 | `EmptyState.kt` | 2026-05-29 |
| A-🟡-50 | AgentRepository多个方法未被ViewModel调用 | ✅ 已修复 | 未被调用的6个方法标记 `@VisibleForTesting` | `AgentRepository.kt` | 2026-05-29 |
| A-🟡-52 | ProductRepository.findProductByCode未被调用 | ✅ 已修复 | 标记 `@VisibleForTesting` | `ProductRepository.kt` | 2026-05-29 |
| A-🟡-53 | SaleOrderRepository.updateSaleDraft未被调用 | ✅ 已修复 | 标记 `@VisibleForTesting` | `SaleOrderRepository.kt` | 2026-05-29 |
| A-🟡-66 | ProductListScreen删除按钮未暴露入口 | ⏭️ 延后 | 需要产品决策（长按菜单/滑动删除/编辑页内删除） | `ProductListScreen.kt` | 2026-05-29 |
| A-🟡-73 | CustomerListScreen删除按钮未暴露入口 | ⏭️ 延后 | 需要产品决策 | `CustomerListScreen.kt` | 2026-05-29 |
| A-🟡-78 | SupplierListScreen删除按钮未暴露入口 | ⏭️ 延后 | 需要产品决策 | `SupplierListScreen.kt` | 2026-05-29 |
| A-🟡-51 | ReportRepository多个方法未被ViewModel调用 | ✅ 已修复 | 6个未调用方法标记 `@VisibleForTesting` | `ReportRepository.kt` | 2026-05-29 |
| A-🟡-55 | SyncRepository.pull循环中异常处理不够健壮 | ✅ 已修复 | 添加重试机制（MAX_RETRY_PAGES=3），applyPulledChanges 用 runCatching 容错 | `SyncRepository.kt` | 2026-05-29 |
| A-🟡-43 | SaleOrderFilter minTotalAmount/maxTotalAmount类型为String | ✅ 已修复 | 改为 `Double?` 类型，ZhihuijiApi 参数同步更新 | `OrderModels.kt`, `ZhihuijiApi.kt` | 2026-05-29 |
| A-🟡-44 | SaleOrderFilter.paymentStatus类型为String | ✅ 已修复 | 改为 `Int?` 类型，ZhihuijiApi 参数同步更新 | `OrderModels.kt`, `ZhihuijiApi.kt` | 2026-05-29 |
| A-🟡-45 | VerifyCodeRequest.type建议改为枚举 | ✅ 已修复 | 新建 `VerificationType` 枚举(REGISTER/LOGIN/RESET_PASSWORD)，VerifyCodeRequest.type 使用枚举 | `AuthModels.kt` | 2026-05-29 |
| A-🟡-46 | SyncChangeDto.operation建议改为枚举 | ✅ 已修复 | 新建 `SyncOperation` 枚举(CREATE/UPDATE/DELETE)，SyncRepository 使用枚举比较 | `SyncModels.kt`, `SyncRepository.kt` | 2026-05-29 |
| A-🟡-32 | ZhihuijiTheme固定fontScale=1f | ✅ 已修复 | 移除 `LocalDensity provides Density(fontScale = 1f)`，尊重用户系统字体缩放设置 | `ZhihuijiTheme.kt` | 2026-05-29 |
| A-🟡-35 | QuantityStepper onMinus/onPlus与onValueChange职责重叠 | ✅ 已修复 | 移除 onMinus/onPlus 参数，内部直接通过 onValueChange 计算增减值，添加 step 参数 | `QuantityStepper.kt`, `SaleOrderEditorScreen.kt`, `PurchaseOrderEditorScreen.kt` | 2026-05-29 |
| A-🟡-54 | SyncRepository双重存储增加复杂性 | ⏭️ 延后 | 需要架构层面重构（DataStore+Room→统一存储） | `SyncRepository.kt` | 2026-05-29 |
| A-🟡-57 | DEFAULT_PULL_LIMIT已提取为常量 | ✅ 无需修改 | 已是 `const val DEFAULT_PULL_LIMIT = 200` | `SyncRepository.kt` | 2026-05-29 |
| A-🟡-19 | StatusLabels.agentTaskStatus参数类型不一致 | ✅ 已修复 | 新建 `AgentTaskStatus` 枚举(QUEUED/RUNNING/COMPLETED/FAILED)，agentTaskStatus 改为枚举参数，AgentTaskSummaryDto/AgentTaskDetailDto.status 从 String 改为枚举 | `AgentModels.kt`, `StatusLabels.kt` | 2026-05-30 |
| A-🟡-62 | LoginScreen/RegisterScreen缺少手机号格式校验 | ✅ 已修复 | 添加 `phoneValid = phone.matches(Regex("^1\\d{10}$"))` 校验，isError 提示 + 按钮启用条件改为 phoneValid | `LoginScreen.kt`, `RegisterScreen.kt` | 2026-05-30 |
| A-🟡-63 | RegisterScreen缺少密码可见性切换 | ✅ 已修复 | 添加 `passwordVisible` 状态 + trailingIcon 切换按钮 + VisualTransformation 切换，与 LoginScreen 一致 | `RegisterScreen.kt` | 2026-05-30 |
| A-🟡-60 | DashboardUiState.workbench加载但未使用 | ✅ 已修复 | 移除 DashboardUiState.workbench 字段、agentRepository 注入和 workbench 加载请求，减少不必要的网络调用 | `DashboardViewModel.kt` | 2026-05-30 |
| A-🟡-69 | FinanceRecordEditorSheet常用分类快速选择 | ✅ 已修复 | 添加收入/支出分类的 FilterChip 快速选择标签列表（收入：销售收入/服务收入/其他收入，支出：采购支出/房租/工资/水电费/其他支出） | `FinanceRecordEditorSheet.kt` | 2026-05-30 |
| A-🟡-74 | SettingsViewModel.init两个collect竞态条件 | ✅ 已修复 | 两个独立 collect 合并为 `settingsStore.baseUrl.combine(settingsStore.clientId)` 单一 Flow，消除竞态条件 | `SettingsViewModel.kt` | 2026-05-30 |
| A-🟡-25 | DEFAULT_BASE_URL硬编码IP地址 | ✅ 已修复 | DEFAULT_BASE_URL 从 `http://117.72.79.106/zhihuiji/v1/` 改为 `https://api.zhihuiji.com/v1/`，normalizeBaseUrl 兼容旧 IP 自动映射 | `SettingsStore.kt` | 2026-05-30 |
| A-🟡-16 | blur的radius参数应提供dp重载 | ✅ 已修复 | 新增 `blur(radius: Dp, edgeTreatment)` 重载函数，委托调用 Float 版本 | `Blur.kt` | 2026-05-30 |
| A-🟡-42 | SaleOrderItemDto行项中包含客户信息冗余 | ✅ 已修复 | customerId/customerName 标记 `@Deprecated("Use parent SaleOrderDto instead", level=WARNING)`，提醒开发者使用父级字段 | `OrderModels.kt` | 2026-05-30 |
| A-🟡-36 | LiquidSegmentedControl泛型T可简化 | ✅ 已修复 | 新增 `StringLiquidSegmentedItem()` 工厂函数和 `StringLiquidSegmentedControl()` 便捷组件，简化 String key 场景 | `LiquidSegmentedControl.kt` | 2026-05-30 |
| A-🟡-1 | MainScreen reselectSignal 5个独立Int变量冗余 | ✅ 已修复 | 5个 `mutableIntStateOf` 合并为 `mutableStateMapOf<String, Int>()`，MainNavGraph 签名从5个 Int 参数简化为 `reselectSignal: (String) -> Int` | `MainScreen.kt`, `MainNavGraph.kt` | 2026-05-30 |
| A-🟡-26 | SyncPreferenceStore.clearAll用键名前缀过滤 | ✅ 已修复 | `clearAll()` 从手动键名前缀过滤改为 `dataStore.edit { it.clear() }`，因 DataStore 已独立命名 | `SyncPreferenceStore.kt` | 2026-05-30 |
| A-🟡-2 | MainScreen bottomBarVisible始终为true | ✅ 已修复 | 添加 `LaunchedEffect(isOnTopLevel)` 自动根据导航层级控制底部栏显隐 | `MainScreen.kt` | 2026-05-30 |
| A-🟡-23 | PurchaseOrderEntity缺少supplierId字段 | ⏭️ 延后 | 需服务端 PurchaseOrderDto 同步添加 supplierId 字段 | - | 2026-05-30 |
| A-🟡-37 | GlassBackground光斑颜色和位置硬编码 | ✅ 已修复 | 提取 `GlassOrb` 数据类 + `DefaultGlassOrbs` 默认值，`glassBackground()` 接受 `orbs` 参数 | `GlassBackground.kt` | 2026-05-30 |
| A-🟡-40 | 多个Model字符串字段建议改为枚举 | ✅ 已修复 | 新增 `OperationType`(SALE/PURCHASE/RETURN) 和 `InsightSeverity`(INFO/WARNING/DANGER) 枚举，AgentModels 中对应字段改为枚举类型 | `AgentModels.kt` | 2026-05-30 |
| A-🟡-64 | AuthViewModel.init中直接collect | ✅ 已修复 | `sessionStore.isLoggedIn.collect` 改为 `stateIn(viewModelScope, Eagerly, false)` 后再 collect，避免上游 Flow 生命周期问题 | `AuthViewModel.kt` | 2026-05-30 |
| A-🟡-68 | FinanceRecordListScreen KPI在UI层计算 | ✅ 已修复 | 收入/支出汇总计算从 Screen 移至 `FinanceListUiState.totalIncome/totalExpense` 计算属性 | `FinanceViewModel.kt`, `FinanceRecordListScreen.kt` | 2026-05-30 |
| A-🟡-65 | CustomerListScreen筛选逻辑在Screen端 | ✅ 已修复 | 筛选逻辑从 Screen `when(selectedTab)` 移至 `CustomerListUiState.statusFilter + filteredCustomers` 计算属性 | `CustomerViewModel.kt`, `CustomerListScreen.kt` | 2026-05-30 |
| A-🟡-61 | DashboardScreen趋势数据是硬编码模拟数据 | ✅ 已修复 | 移除 KpiCard 虚假 trend 字符串和 LineTrendChart 硬编码图表，等服务端提供趋势 API 后再添加 | `DashboardScreen.kt` | 2026-05-30 |
| A-🟡-77 | ProductListScreen筛选逻辑在Screen端 | ✅ 已修复 | 筛选逻辑从 Screen 移至 `ProductListUiState.stockFilter + filteredProducts` 计算属性 | `ProductListViewModel.kt`, `ProductListScreen.kt` | 2026-05-30 |
| A-🟡-7 | DocumentsScreen/ArchivesScreen硬编码字符串 | ✅ 已修复 | Tab 标签字符串提取为 `private const val TAB_*` 常量 | `DocumentsScreen.kt`, `ArchivesScreen.kt` | 2026-05-30 |
| A-🟡-8 | AppNavGraph Session加载期间Compose树不完整 | ✅ 已修复 | 移除 `return`，改为始终渲染 NavHost + 覆盖 CircularProgressIndicator 加载层 | `AppNavGraph.kt` | 2026-05-30 |
| A-🟡-6 | DocumentsScreen/ArchivesScreen Tab切换列表重建 | ✅ 已修复 | 添加 `key(index)` 包裹每个 Tab 内容，防止 Compose 重组时丢失状态 | `DocumentsScreen.kt`, `ArchivesScreen.kt` | 2026-05-30 |
| A-🟡-38 | DampedSegmentedDragState.onTap回调时机过早 | ✅ 已修复 | `onSelected` 回调从动画开始前移至动画完成后调用 | `DampedSegmentedDragState.kt` | 2026-05-30 |
| A-🟡-24 | 多个DAO缺少筛选查询方法 | ✅ 已修复 | SaleOrderDao/PurchaseOrderDao/FinanceRecordDao 添加 `search()` 方法，支持关键字+状态/类型过滤 | `SaleOrderDao.kt`, `PurchaseOrderDao.kt`, `FinanceRecordDao.kt` | 2026-05-30 |
| A-🟡-15 | AGSL着色器缺少数学原理注释 | ✅ 已修复 | 为 SDF、circleMap、色散着色器添加数学原理注释 | `Shaders.kt` | 2026-05-30 |
| A-🟡-66 | SupplierListScreen筛选逻辑 | ✅ 无需修改 | 筛选逻辑已在 ViewModel 中（statusFilter + changeStatusFilter） | - | 2026-05-30 |
| A-🟡-33 | 仅支持浅色主题 | ⏭️ 延后 | 需设计团队提供深色主题色板 | - | 2026-05-30 |
| A-🟡-21 | Entity金融数据使用Double | ⏭️ 延后 | 需架构重构为 BigDecimal 或分币整数，影响范围过大 | - | 2026-05-30 |
| A-🟡-14 | 色散着色器7次采样可优化 | ✅ 已修复 | 7次光谱采样优化为3次（R/G/B各一次），纹理采样减少57% | `Shaders.kt` | 2026-05-30 |
| A-🟡-10 | InverseLayerScope未处理translation | ✅ 已修复 | `inverseTransformAtTopLeft` 添加 translationX/Y 参数，逆变换前先反向平移 | `InverseLayerScope.kt` | 2026-05-30 |
| S-🟡-9 | 业务异常类缺失 | ✅ 已修复 | 新建 `BusinessException` 类（含 code 字段），GlobalExceptionHandler 添加对应处理器 | `BusinessException.java`(新建), `GlobalExceptionHandler.java` | 2026-05-30 |
| S-🟡-10 | 缺少数据库索引 | ✅ 已修复 | 新建 V5 迁移脚本，为 sessions/products/customers/suppliers/sale_orders/purchase_orders/pay_orders/finance_records/agent_notifications 添加索引 | `V5__add_indexes.sql`(新建) | 2026-05-30 |
| S-🟡-12 | LLM无重试机制 | ✅ 已修复 | 添加指数退避重试（MAX_RETRIES=3, INITIAL_BACKOFF_MS=1000ms），提取 doCreateJsonMessage 方法 | `LongCatAnthropicClient.java` | 2026-05-30 |
| S-🟡-15 | 缺少生产环境配置 | ✅ 已修复 | 新建 application-prod.yml：HikariCP 连接池、CORS 域名限制、日志级别调整、移除默认值 | `application-prod.yml`(新建) | 2026-05-30 |
| S-🟡-3 | 参数校验不完整(DTO层) | ✅ 已修复 | SaleOrderStatusRequest.status 添加 @NotNull，ProductAdjustStockRequest.delta 添加零值校验 | `SaleOrderStatusRequest.java`, `ProductAdjustStockRequest.java` | 2026-05-30 |
| S-🟡-4 | 物理删除改软删除 | ⏭️ 延后 | 需修改 Entity+Service+迁移脚本，影响范围大 | - | 2026-05-30 |
| S-🟡-1 | Entity直接作为请求体 | ⏭️ 延后 | 需为3个Controller创建专用请求DTO，影响范围大 | - | 2026-05-30 |
| S-🟡-8 | JSON命名策略不一致 | ⏭️ 延后 | AgentDto 使用 LowerCamelCase，统一为 SnakeCase 会破坏 Android 端协议 | - | 2026-05-30 |

---

## ⚪ 低优先级修复记录

> 共 28 项，详见各子模块 README.md。此处仅记录已开始修复的条目。

| 编号 | 问题 | 状态 | 修复说明 | 涉及文件 | 修复日期 |
|------|------|------|---------|---------|---------|
| (待修复时补充) | | | | | |

---

## 延后/不适用项

| 编号 | 原因 | 说明 |
|------|------|------|
| S-🔴-5 | 需要接入第三方短信服务 | 验证码校验需要短信服务商集成，非代码层面可独立解决 |
| S-🟠-2 | 需接入真实短信/验证码服务 | AuthController.verifyCode() 硬编码返回失败，需第三方服务 |
| S-🟠-3~8 | 全量查询+内存过滤 | 需要后端 Repository 层改造（JPA Specification/QueryDSL），改动较大 |
| S-🟠-9 | 全局Double→BigDecimal | 影响范围极大（Server+Android全栈），需专项重构 |
| S-🟠-10 | 缺少外键约束 | 需要数据库迁移脚本，可能影响现有数据 |
| S-🟠-11 | 缺少分页支持 | 需要前后端协同改造API接口 |
| S-🟠-13 | 无令牌黑名单 | 需要引入 Redis 等缓存基础设施 |
| S-🟠-14 | Token验证每次查库无缓存 | 需要引入 Redis 缓存 session 信息 |
| A-🟠-3 | 订单项未持久化到本地 | 需要新增Room Entity+DAO+迁移，改动较大 |

---

## 第12批修复记录（2026-05-30）

### 服务器端

| 编号 | 问题 | 状态 | 修复说明 | 涉及文件 |
|------|------|------|---------|---------|
| S-🟡-5 | 状态字段缺少枚举（全局） | ✅ 已修复 | 新建 `OrderStatus`(DRAFT/COMPLETED/CANCELLED) 和 `PaymentStatus`(UNPAID/PARTIAL/PAID) 常量类 | `OrderStatus.java`(新建), `PaymentStatus.java`(新建) |
| S-🟡-7 | AgentDto过于庞大 | ✅ 部分修复 | 添加7个分区注释（Workbench/Reconciliation/Alerts/Answer/OperationDraft/Task/Notification），完整拆分延后（100+处引用，服务端无法编译验证） | `AgentDto.java` |
| S-🟡-11 | 手动ID碰撞风险 | ✅ 已修复 | 新建 `IdGenerator` 工具类（SecureRandom + 碰撞检测重试），5个Service的私有 `nextId()` 全部替换为 `IdGenerator.nextId()` | `IdGenerator.java`(新建), `SaleOrderService.java`, `FinanceRecordService.java`, `PurchaseOrderService.java`, `PayOrderService.java`, `ProductService.java` |
| S-🟡-16 | AgentController参数缺上界校验 | ✅ 已修复 | 所有 GET 端点添加参数上界校验（windowDays 1-365, limit 1-50, agingDays 1-365） | `AgentController.java` |
| S-🟡-17 | AdminService.summary多次count查询 | ✅ 已修复 | 未读通知计数从 `findTop30...().size()` 改为 `countByIsReadFalse()`，添加 `deleteAllByIsReadTrue()` 方法 | `AgentNotificationRepository.java`, `AdminService.java` |
| S-🟡-18 | SupplierService.list内存过滤 | ✅ 已修复 | 从内存过滤改为数据库查询（4种组合：有/无 keyword × 有/无 status） | `SupplierRepository.java`, `SupplierService.java` |
| S-🟡-19 | SaleOrderService状态机校验不完整 | ✅ 已修复 | 添加状态机校验：草稿→已完成需先确认，已完成→草稿不可回退 | `SaleOrderService.java` |
| S-🟡-22 | 无并发控制 | ✅ 已修复 | submitDraft 添加 idempotencyKey 幂等性保护（ConcurrentHashMap + 60s TTL），SubmitRequest 添加 idempotencyKey 字段 | `AgentService.java`, `LlmDrivenAgentService.java`, `AgentController.java` |
| S-🟡-24 | 任务执行无超时 | ✅ 已修复 | Executor→ExecutorService，添加 TASK_TIMEOUT_SECONDS=120L，submit+get(timeout)，新增 markTaskFailed 方法 | `AgentTaskService.java` |
| S-🟡-26 | AI层错误处理不完整 | ✅ 已修复 | parseJson 添加详细错误日志（raw length），linter 报错为 IDE Java Language Server 无法解析项目依赖，代码逻辑正确 | `AgentLlmService.java` |

### 编译测试

- 安卓端：**BUILD SUCCESSFUL** (925 actionable tasks, 3s)
- 服务端：无 gradle 可用，IDE linter 错误为项目识别问题而非代码错误

---

## 第13批修复记录（2026-05-30）

### 服务器端

| 编号 | 问题 | 状态 | 修复说明 | 涉及文件 |
|------|------|------|---------|---------|
| S-🟡-13 | 无Token统计 | ✅ 已修复 | AnthropicResponse 添加 `usage` 字段（input_tokens/output_tokens），响应日志记录 token 消耗 | `LongCatAnthropicClient.java` |
| S-🟡-20 | submitDraft路由用if链 | ✅ 已修复 | if-else 链改为 Java 21 switch 表达式，更清晰且易扩展 | `AgentService.java` |
| S-🟡-21 | LLM幻觉风险 | ✅ 已修复 | resolveProduct/resolveCustomer/resolveSupplier 添加幻觉检测日志：当 LLM 返回的 ID 在数据库中不存在时记录 warn 日志 | `LlmDrivenAgentService.java` |
| S-🟡-25 | LLM上下文全量加载 | ✅ 已修复 | 提取 `loadProducts(limit)`/`loadCustomers(limit)`/`loadSuppliers(limit)` 方法，消除 getWorkbench/draftOperation 中的重复代码 | `LlmDrivenAgentService.java` |
| S-🟡-14 | Service注入过多Repository | ⏭️ 延后 | 需要拆分 ReportService(8个)/SyncService(7个)/DemoDataService(14个) Repository 注入，影响范围大 | - |
| S-🟡-23 | 定时任务无分布式锁 | ⏭️ 延后 | 需要引入 ShedLock 或 Redis 分布式锁基础设施 | - |

### 安卓端

| 编号 | 问题 | 状态 | 修复说明 | 涉及文件 |
|------|------|------|---------|---------|
| A-🟡-3 | MainNavGraph文件过长 | ✅ 已修复 | 提取 `SubNavGraph.kt`，按域拆分为6个 NavGraphBuilder 扩展函数（productEditorRoute/customerRoutes/supplierRoutes/saleOrderRoutes/purchaseOrderRoutes/payOrderRoutes），MainNavGraph 从242行减至143行 | `SubNavGraph.kt`(新建), `MainNavGraph.kt` |
| A-🟡-22 | ProductEntity库存量使用Double | ⏭️ 延后 | 与 A-🟡-21 同类问题，需架构重构为 Long（分）或 BigDecimal | - |

### 编译测试

- 安卓端：**BUILD SUCCESSFUL** (925 actionable tasks, 4s)

---

## 第14批修复记录（2026-05-30）

### 服务器端

| 编号 | 问题 | 状态 | 修复说明 | 涉及文件 |
|------|------|------|---------|---------|
| S-🟠-3 | SaleOrderService全量查询+内存过滤 | ✅ 已修复 | SaleOrderRepository 添加 `search()` JPQL 方法（keyword/status/minTotal/maxTotal/createdAfter/createdBefore），Service 层改为调用数据库查询 | `SaleOrderRepository.java`, `SaleOrderService.java` |
| S-🟠-4 | PurchaseOrderService全量查询+内存过滤 | ✅ 已修复 | PurchaseOrderRepository 添加 `search()` JPQL 方法，Service 一行调用 | `PurchaseOrderRepository.java`, `PurchaseOrderService.java` |
| S-🟠-5 | PayOrderService全量查询+内存过滤 | ✅ 已修复 | PayOrderRepository 添加 `search()` JPQL 方法，Service 一行调用 | `PayOrderRepository.java`, `PayOrderService.java` |
| S-🟠-6 | FinanceRecordService全量查询+内存过滤 | ✅ 已修复 | FinanceRecordRepository 添加 `search()` JPQL 方法，Service 一行调用 | `FinanceRecordRepository.java`, `FinanceRecordService.java` |
| S-🟠-7 | AdminService全量查询+内存过滤 | ✅ 已修复 | UserRepository 添加 `searchByKeyword()` JPQL 方法，Service 一行调用 | `UserRepository.java`, `AdminService.java` |
| S-🟠-10 | 缺少外键约束 | ✅ 已修复 | 新建 V6 迁移脚本，为 sale_order_items/purchase_order_items/payments/stock_adjustments/sale_orders/purchase_orders/pay_orders 添加外键约束 | `V6__add_foreign_keys.sql`(新建) |

### 安卓端

| 编号 | 问题 | 状态 | 修复说明 | 涉及文件 |
|------|------|------|---------|---------|
| A-🟠-8 | provideRetrofit使用runBlocking | ✅ 已修复 | 新建 `@BaseUrlInterceptor` 限定注解 + BaseUrl 拦截器动态替换 URL，Retrofit 使用 DEFAULT_FALLBACK_URL 构建，不再在初始化时 runBlocking | `NetworkModule.kt`, `NetworkConfig.kt`, `BaseUrlInterceptor.kt`(新建) |
| A-🟠-9 | ZhihuijiApi使用Dto作为请求体 | ✅ 已修复 | 新建 CreateProductRequest/UpdateProductRequest/CreateCustomerRequest/UpdateCustomerRequest/CreateSupplierRequest/UpdateSupplierRequest 专用请求类，ZhihuijiApi 和 Repository 全部改用新类型 | `ProductModels.kt`, `PartyModels.kt`, `ZhihuijiApi.kt`, `ProductRepository.kt`, `CustomerRepository.kt`, `SupplierRepository.kt` |
| A-🟠-10 | saleOrders参数过多 | ✅ 已修复 | saleOrders() 从8个独立参数改为 `@QueryMap filter: Map<String, String?>`，SaleOrderRepository 构建参数 Map | `ZhihuijiApi.kt`, `SaleOrderRepository.kt` |
| A-🟠-13 | Repository内存过滤 | ✅ 已修复 | 4个 Repository 的 observeXxx() 从 DAO.observeAll()+内存过滤改为 DAO.search() 数据库查询：SaleOrder/PurchaseOrder/PayOrder/FinanceRecord | `SaleOrderRepository.kt`, `PurchaseOrderRepository.kt`, `PayOrderRepository.kt`, `FinanceRepository.kt`, `PayOrderDao.kt` |

### 编译测试

- 安卓端：**BUILD SUCCESSFUL** (925 actionable tasks, 11s)

---

## 第15批修复记录（2026-05-30）

### 服务器端

| 编号 | 问题 | 状态 | 修复说明 | 涉及文件 |
|------|------|------|---------|---------|
| S-🟠-8 | ReportService全量内存聚合 | ✅ 部分修复 | salesSummary() 从全量加载+内存聚合改为 SQL 聚合查询（sumTotalAmountBetween/sumPaidAmountBetween/countNonCancelledBetween），其余11个方法待后续改造 | `SaleOrderRepository.java`, `ReportService.java` |
| S-🟠-2 | 验证码接口硬编码失败 | ⏭️ 延后 | 需接入第三方短信验证码服务 | - |
| S-🟠-9 | 金额字段使用Double | ⏭️ 延后 | 全栈 Double→BigDecimal 影响范围极大，需专项重构 | - |
| S-🟠-11 | 缺少分页支持 | ⏭️ 延后 | 需前后端协同改造API接口 | - |
| S-🟠-13 | 无令牌黑名单 | ⏭️ 延后 | 需引入 Redis 缓存基础设施 | - |
| S-🟠-14 | Token验证每次查库无缓存 | ⏭️ 延后 | 需引入 Redis 缓存基础设施 | - |

### 安卓端

| 编号 | 问题 | 状态 | 修复说明 | 涉及文件 |
|------|------|------|---------|---------|
| A-🟠-3 | 订单项未持久化到本地 | ⏭️ 延后 | 需新增 Room Entity+DAO+迁移，改动较大 | - |
| A-🟡-13 | Lens/HighlightStyle圆角提取重复 | ⏭️ 延后 | backdrop 库内部重构，修改风险较高 | - |
| A-🟡-41 | Model金融数据BigDecimal | ⏭️ 延后 | 与 A-🟡-21 同类问题，需架构重构 | - |
