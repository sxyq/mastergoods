# Reports 模块详尽代码分析

> 自动生成于 2026-05-28，覆盖 reports 目录下全部 2 个 Kotlin 源文件

---

## 1. ReportUiState

- **文件路径**: `feature/reports/src/main/java/com/zhihuiji/feature/reports/ReportViewModel.kt`
- **父类/接口**: 无（data class）
- **注解**: 无
- **职责**: 报表页面的 UI 状态数据容器
- **设计模式**: MVI 状态模式

### 变量/属性

##### isLoading: Boolean = false
- 作用域：类公开
- 初始值：false
- 使用场景：是否正在加载数据
- 建议：无

##### salesSummary: SalesSummaryReportDto? = null
- 作用域：类公开
- 初始值：null
- 使用场景：销售汇总数据
- 建议：无

##### profitSummary: ProfitSummaryReportDto? = null
- 作用域：类公开
- 初始值：null
- 使用场景：利润汇总数据
- 建议：无

##### reconciliation: ReconciliationSummaryReportDto? = null
- 作用域：类公开
- 初始值：null
- 使用场景：往来对账汇总数据
- 建议：无

##### lowStockProducts: List<LowStockProductReportDto> = emptyList()
- 作用域：类公开
- 初始值：空列表
- 使用场景：低库存商品列表
- 建议：无

##### topProducts: List<TopSellingProductReportDto> = emptyList()
- 作用域：类公开
- 初始值：空列表
- 使用场景：热销商品排行列表
- 建议：无

##### topReceivables: List<CustomerReceivableReportDto> = emptyList()
- 作用域：类公开
- 初始值：空列表
- 使用场景：应收客户排行列表
- 建议：无

---

## 2. ReportViewModel

- **文件路径**: `feature/reports/src/main/java/com/zhihuiji/feature/reports/ReportViewModel.kt`
- **父类/接口**: `ViewModel`
- **注解**: `@HiltViewModel`
- **职责**: 管理报表页面的状态和业务逻辑
- **设计模式**: MVVM 模式

### 类属性

##### reportRepository: ReportRepository
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：调用报表数据操作
- 建议：无

##### _uiState: MutableStateFlow<ReportUiState>
- 作用域：类私有
- 初始值：`MutableStateFlow(ReportUiState())`
- 使用场景：内部可变的 UI 状态流
- 建议：无

##### uiState: StateFlow<ReportUiState>
- 作用域：类公开
- 初始值：`_uiState.asStateFlow()`
- 使用场景：对外暴露只读的 UI 状态流
- 建议：无

### 函数/方法

##### init 块
- 参数：无
- 返回值：无
- 实现逻辑：初始化时调用 `loadReports()`
- 调用关系：调用了 `loadReports()`
- 建议：无

##### loadReports()
- 参数：无
- 返回值：无
- 实现逻辑：计算近30天时间范围，依次请求6个报表接口（销售汇总、利润汇总、往来对账、低库存、热销商品、应收排行），最后设置 isLoading 为 false
- 调用关系：调用了 `reportRepository.salesSummary()`、`reportRepository.profitSummary()`、`reportRepository.reconciliationSummary()`、`reportRepository.lowStockProducts()`、`reportRepository.topProducts()`、`reportRepository.topReceivableCustomers()`
- 建议：1) 6个网络请求串行执行，建议使用 `async/awaitAll` 并行化以提升加载速度；2) 未处理任何 onFailure 情况；3) 时间范围硬编码为近30天，但 UI 有"今日/近7天/近30天/本月"选项，选中不同选项并未影响数据加载，建议将 selectedPeriod 与 loadReports 关联

---

## 3. ReportScreen

- **文件路径**: `feature/reports/src/main/java/com/zhihuiji/feature/reports/ReportScreen.kt`
- **父类/接口**: 无（Composable 函数）
- **注解**: `@Composable`
- **职责**: 报表页面的 UI 渲染
- **设计模式**: 声明式 UI

### 函数参数

##### onNavigateBack: () -> Unit
- 作用：返回上一页的回调
- 建议：无

##### showTopBar: Boolean = true
- 作用：是否显示顶部导航栏
- 建议：无

##### reselectSignal: Int = 0
- 作用：底部导航重新选择信号
- 建议：无

##### viewModel: ReportViewModel = hiltViewModel()
- 作用：注入的 ViewModel 实例
- 建议：无

### 局部变量

##### uiState: ReportUiState
- 作用域：函数局部
- 初始值：通过 `viewModel.uiState.collectAsState()` 获取
- 使用场景：订阅 UI 状态
- 建议：无

##### lifecycleOwner: LifecycleOwner
- 作用域：函数局部
- 初始值：`LocalLifecycleOwner.current`
- 使用场景：监听生命周期事件以自动刷新数据
- 建议：无

##### scrollState: ScrollState
- 作用域：函数局部
- 初始值：`rememberScrollState()`
- 使用场景：控制页面滚动
- 建议：无

##### selectedPeriod: Int
- 作用域：函数局部
- 初始值：0（今日）
- 使用场景：当前选中的时间周期（今日/近7天/近30天/本月）
- 建议：当前 selectedPeriod 仅影响 UI 显示但不影响数据加载，建议将此状态传递给 ViewModel 以加载对应时间范围的数据

##### salesAmount: Double
- 作用域：函数局部
- 初始值：从 salesSummary 获取
- 使用场景：销售额
- 建议：无

##### paidAmount: Double
- 作用域：函数局部
- 初始值：从 salesSummary 获取
- 使用场景：已收金额
- 建议：无

##### unpaidAmount: Double
- 作用域：函数局部
- 初始值：从 salesSummary 获取
- 使用场景：待收金额
- 建议：无

##### profitAmount: Double
- 作用域：函数局部
- 初始值：从 profitSummary 获取
- 使用场景：利润金额
- 建议：无

##### payableAmount: Double
- 作用域：函数局部
- 初始值：从 reconciliation 获取
- 使用场景：应付金额
- 建议：无

##### receivableAmount: Double
- 作用域：函数局部
- 初始值：从 reconciliation 获取
- 使用场景：应收金额
- 建议：无

##### trendValues: List<Double>
- 作用域：函数局部
- 初始值：基于 salesAmount 的模拟趋势数据
- 使用场景：销售趋势图表数据
- 建议：与 Dashboard 类似，当前是硬编码模拟数据，建议从后端获取真实数据

##### barItems: List<Pair<String, Double>>
- 作用域：函数局部
- 初始值：从 topProducts 映射或默认占位数据
- 使用场景：热销商品排行图表数据
- 建议：无

##### receivableItems: List<Pair<String, Double>>
- 作用域：函数局部
- 初始值：从 topReceivables 映射或默认占位数据
- 使用场景：应收客户排行图表数据
- 建议：无

### 关键逻辑

##### DisposableEffect(lifecycleOwner)
- 监听 `ON_RESUME` 生命周期事件自动刷新数据
- 建议：无
