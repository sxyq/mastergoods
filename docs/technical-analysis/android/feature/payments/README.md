# Payments 模块详尽代码分析

> 自动生成于 2026-05-28，覆盖 payments 目录下全部 6 个 Kotlin 源文件

---

## 1. PayOrderDetailUiState

- **文件路径**: `feature/payments/src/main/java/com/zhihuiji/feature/payments/PayOrderDetailViewModel.kt`
- **父类/接口**: 无（data class）
- **注解**: 无
- **职责**: 付款单详情页面的 UI 状态数据容器
- **设计模式**: MVI 状态模式

### 变量/属性

##### order: PayOrderDto? = null
- 作用域：类公开
- 初始值：null
- 使用场景：当前查看的付款单数据
- 建议：无

##### isLoading: Boolean = false
- 作用域：类公开
- 初始值：false
- 使用场景：是否正在加载
- 建议：无

##### statusUpdateSuccess: Boolean = false
- 作用域：类公开
- 初始值：false
- 使用场景：状态更新是否成功
- 建议：无

##### error: UiMessage? = null
- 作用域：类公开
- 初始值：null
- 使用场景：错误信息
- 建议：无

---

## 2. PayOrderDetailViewModel

- **文件路径**: `feature/payments/src/main/java/com/zhihuiji/feature/payments/PayOrderDetailViewModel.kt`
- **父类/接口**: `ViewModel`
- **注解**: `@HiltViewModel`
- **职责**: 管理付款单详情页面的状态和业务逻辑
- **设计模式**: MVVM 模式

### 类属性

##### payOrderRepository: PayOrderRepository
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：调用付款单数据操作
- 建议：无

##### _uiState: MutableStateFlow<PayOrderDetailUiState>
- 作用域：类私有
- 初始值：`MutableStateFlow(PayOrderDetailUiState())`
- 使用场景：内部可变的 UI 状态流
- 建议：无

##### uiState: StateFlow<PayOrderDetailUiState>
- 作用域：类公开
- 初始值：`_uiState.asStateFlow()`
- 使用场景：对外暴露只读的 UI 状态流
- 建议：无

### 函数/方法

##### loadDetail(id: Long)
- 参数：`id: Long` - 付款单 ID
- 返回值：无
- 实现逻辑：设置 isLoading 为 true，调用 `payOrderRepository.getPayOrder(id)`，成功时更新 order
- 调用关系：调用了 `payOrderRepository.getPayOrder()`
- 建议：无

##### updateStatus(status: Int)
- 参数：`status: Int` - 目标状态
- 返回值：无
- 实现逻辑：获取当前订单 ID，调用 `payOrderRepository.updatePayOrderStatus()`，成功时设置 statusUpdateSuccess 并重新加载详情
- 调用关系：调用了 `payOrderRepository.updatePayOrderStatus()`、`loadDetail()`
- 建议：无

##### cancelOrder()
- 参数：无
- 返回值：无
- 实现逻辑：委托给 `updateStatus(2)`
- 调用关系：调用了 `updateStatus()`
- 建议：无

##### completeOrder()
- 参数：无
- 返回值：无
- 实现逻辑：委托给 `updateStatus(1)`
- 调用关系：调用了 `updateStatus()`
- 建议：无

##### clearError()
- 参数：无
- 返回值：无
- 实现逻辑：将 error 设置为 null
- 调用关系：无
- 建议：无

---

## 3. PayOrderDetailScreen

- **文件路径**: `feature/payments/src/main/java/com/zhihuiji/feature/payments/PayOrderDetailScreen.kt`
- **父类/接口**: 无（Composable 函数）
- **注解**: `@Composable`
- **职责**: 付款单详情页面的 UI 渲染
- **设计模式**: 声明式 UI

### 函数参数

##### orderId: Long
- 作用：要查看的付款单 ID
- 建议：无

##### onNavigateBack: () -> Unit
- 作用：返回上一页的回调
- 建议：无

##### viewModel: PayOrderDetailViewModel = hiltViewModel()
- 作用：注入的 ViewModel 实例
- 建议：无

### 关键逻辑

##### LaunchedEffect(orderId)
- 当 orderId 变化时调用 `viewModel.loadDetail(orderId)`
- 建议：无

##### 底部操作栏
- 当订单状态为 0（待付款）时显示"确认付款"和"取消"按钮
- 建议：无

---

## 4. PayOrderEditorUiState

- **文件路径**: `feature/payments/src/main/java/com/zhihuiji/feature/payments/PayOrderEditorViewModel.kt`
- **父类/接口**: 无（data class）
- **注解**: 无
- **职责**: 付款单编辑页面的 UI 状态数据容器
- **设计模式**: MVI 状态模式

### 变量/属性

##### supplierId: Long? = null
- 作用域：类公开
- 初始值：null
- 使用场景：选中的供应商 ID
- 建议：无

##### supplierName: String? = null
- 作用域：类公开
- 初始值：null
- 使用场景：选中的供应商名称
- 建议：无

##### amount: Double = 0.0
- 作用域：类公开
- 初始值：0.0
- 使用场景：付款金额
- 建议：无

##### method: Int = 1
- 作用域：类公开
- 初始值：1（现金）
- 使用场景：付款方式
- 建议：无

##### referenceNo: String = ""
- 作用域：类公开
- 初始值：空字符串
- 使用场景：参考号
- 建议：无

##### notes: String = ""
- 作用域：类公开
- 初始值：空字符串
- 使用场景：备注
- 建议：无

##### supplierSearchResults: List<SupplierDto> = emptyList()
- 作用域：类公开
- 初始值：空列表
- 使用场景：供应商搜索结果
- 建议：无

##### isSaving: Boolean = false
- 作用域：类公开
- 初始值：false
- 使用场景：是否正在保存
- 建议：无

##### saveSuccess: Boolean = false
- 作用域：类公开
- 初始值：false
- 使用场景：保存是否成功
- 建议：无

##### error: UiMessage? = null
- 作用域：类公开
- 初始值：null
- 使用场景：错误信息
- 建议：无

---

## 5. PayOrderEditorViewModel

- **文件路径**: `feature/payments/src/main/java/com/zhihuiji/feature/payments/PayOrderEditorViewModel.kt`
- **父类/接口**: `ViewModel`
- **注解**: `@HiltViewModel`
- **职责**: 管理付款单创建页面的状态和业务逻辑
- **设计模式**: MVVM 模式

### 类属性

##### payOrderRepository: PayOrderRepository
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：调用付款单数据操作
- 建议：无

##### supplierRepository: SupplierRepository
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：搜索和选择供应商
- 建议：无

##### _uiState: MutableStateFlow<PayOrderEditorUiState>
- 作用域：类私有
- 初始值：`MutableStateFlow(PayOrderEditorUiState())`
- 使用场景：内部可变的 UI 状态流
- 建议：无

##### uiState: StateFlow<PayOrderEditorUiState>
- 作用域：类公开
- 初始值：`_uiState.asStateFlow()`
- 使用场景：对外暴露只读的 UI 状态流
- 建议：无

### 函数/方法

##### init 块
- 参数：无
- 返回值：无
- 实现逻辑：预加载供应商列表
- 调用关系：调用了 `supplierRepository.refreshSuppliers()`
- 建议：无

##### selectSupplier(id: Long, name: String)
- 参数：`id: Long` - 供应商 ID；`name: String` - 供应商名称
- 返回值：无
- 实现逻辑：更新 supplierId 和 supplierName
- 调用关系：无
- 建议：无

##### searchSuppliers(keyword: String)
- 参数：`keyword: String` - 搜索关键词
- 返回值：无
- 实现逻辑：刷新远程数据，获取本地数据流第一批结果
- 调用关系：调用了 `supplierRepository.refreshSuppliers()`、`supplierRepository.observeSuppliers()`
- 建议：无

##### updateAmount(amount: Double)
- 参数：`amount: Double` - 付款金额
- 返回值：无
- 实现逻辑：更新 amount
- 调用关系：无
- 建议：无

##### updateMethod(method: Int)
- 参数：`method: Int` - 付款方式
- 返回值：无
- 实现逻辑：更新 method
- 调用关系：无
- 建议：无

##### updateReferenceNo(ref: String)
- 参数：`ref: String` - 参考号
- 返回值：无
- 实现逻辑：更新 referenceNo
- 调用关系：无
- 建议：无

##### updateNotes(notes: String)
- 参数：`notes: String` - 备注
- 返回值：无
- 实现逻辑：更新 notes
- 调用关系：无
- 建议：无

##### submitOrder()
- 参数：无
- 返回值：无
- 实现逻辑：校验供应商已选择且金额 > 0，构建 `CreatePayOrderRequest`，调用 `payOrderRepository.createPayOrder()`
- 调用关系：调用了 `payOrderRepository.createPayOrder()`
- 建议：无

##### clearError()
- 参数：无
- 返回值：无
- 实现逻辑：将 error 设置为 null
- 调用关系：无
- 建议：无

---

## 6. PayOrderEditorScreen

- **文件路径**: `feature/payments/src/main/java/com/zhihuiji/feature/payments/PayOrderEditorScreen.kt`
- **父类/接口**: 无（Composable 函数）
- **注解**: `@Composable`、`@OptIn(ExperimentalMaterial3Api::class)`
- **职责**: 新建付款单页面的 UI 渲染
- **设计模式**: 声明式 UI

### 函数参数

##### onNavigateBack: () -> Unit
- 作用：返回上一页的回调
- 建议：无

##### viewModel: PayOrderEditorViewModel = hiltViewModel()
- 作用：注入的 ViewModel 实例
- 建议：无

### 局部变量

##### showSupplierPicker: Boolean
- 作用域：函数局部
- 初始值：false
- 使用场景：控制供应商选择弹窗的显示/隐藏
- 建议：无

##### supplierSearchQuery: String
- 作用域：函数局部
- 初始值：空字符串
- 使用场景：供应商搜索输入文本
- 建议：无

##### amountText: String
- 作用域：函数局部
- 初始值：空字符串
- 使用场景：金额输入文本
- 建议：无

### 关键逻辑

##### LaunchedEffect(uiState.saveSuccess)
- 当 saveSuccess 为 true 时调用 `onNavigateBack()`
- 建议：无

##### 供应商选择弹窗
- 使用 `ModalBottomSheet` 展示供应商搜索列表
- 建议：无

---

## 7. PayOrderListUiState

- **文件路径**: `feature/payments/src/main/java/com/zhihuiji/feature/payments/PayOrderViewModel.kt`
- **父类/接口**: 无（data class）
- **注解**: 无
- **职责**: 付款单列表页面的 UI 状态数据容器
- **设计模式**: MVI 状态模式

### 变量/属性

##### orders: List<PayOrderDto> = emptyList()
- 作用域：类公开
- 初始值：空列表
- 使用场景：当前显示的付款单列表
- 建议：无

##### filter: PayOrderFilter = PayOrderFilter()
- 作用域：类公开
- 初始值：默认 PayOrderFilter
- 使用场景：当前筛选条件
- 建议：无

##### isLoading: Boolean = false
- 作用域：类公开
- 初始值：false
- 使用场景：是否正在加载
- 建议：无

---

## 8. PayOrderViewModel

- **文件路径**: `feature/payments/src/main/java/com/zhihuiji/feature/payments/PayOrderViewModel.kt`
- **父类/接口**: `ViewModel`
- **注解**: `@HiltViewModel`
- **职责**: 管理付款单列表页面的状态和业务逻辑
- **设计模式**: MVVM 模式

### 类属性

##### payOrderRepository: PayOrderRepository
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：调用付款单数据操作
- 建议：无

##### _uiState: MutableStateFlow<PayOrderListUiState>
- 作用域：类私有
- 初始值：`MutableStateFlow(PayOrderListUiState())`
- 使用场景：内部可变的 UI 状态流
- 建议：无

##### uiState: StateFlow<PayOrderListUiState>
- 作用域：类公开
- 初始值：`_uiState.asStateFlow()`
- 使用场景：对外暴露只读的 UI 状态流
- 建议：无

### 函数/方法

##### init 块
- 参数：无
- 返回值：无
- 实现逻辑：初始化时调用 `loadOrders()`
- 调用关系：调用了 `loadOrders()`
- 建议：无

##### loadOrders(filter: PayOrderFilter)
- 参数：`filter: PayOrderFilter` - 筛选条件（默认取当前状态值）
- 返回值：无
- 实现逻辑：设置加载状态，刷新远程数据，观察本地数据流更新列表
- 调用关系：调用了 `payOrderRepository.refreshPayOrders()`、`payOrderRepository.observePayOrders()`
- 建议：`collect` 是终端操作符，多次调用可能导致状态覆盖，建议使用 `collectLatest` 替代

---

## 9. PayOrderListScreen

- **文件路径**: `feature/payments/src/main/java/com/zhihuiji/feature/payments/PayOrderListScreen.kt`
- **父类/接口**: 无（Composable 函数）
- **注解**: `@Composable`
- **职责**: 付款单列表页面的 UI 渲染
- **设计模式**: 声明式 UI

### 函数参数

##### onNavigateBack: () -> Unit
- 作用：返回上一页的回调
- 建议：无

##### showTopBar: Boolean = true
- 作用：是否显示顶部导航栏
- 建议：无

##### scrollToTopSignal: Int = 0
- 作用：滚动到顶部的信号值
- 建议：无

##### onNavigateToEditor: () -> Unit = {}
- 作用：导航到新建付款单页面的回调
- 建议：无

##### onNavigateToDetail: (Long) -> Unit = {}
- 作用：导航到付款单详情页面的回调
- 建议：无

##### viewModel: PayOrderViewModel = hiltViewModel()
- 作用：注入的 ViewModel 实例
- 建议：无

### 局部变量

##### uiState: PayOrderListUiState
- 作用域：函数局部
- 初始值：通过 `viewModel.uiState.collectAsState()` 获取
- 使用场景：订阅 UI 状态
- 建议：无

##### listState: LazyListState
- 作用域：函数局部
- 初始值：`rememberLazyListState()`
- 使用场景：控制列表滚动
- 建议：无
