# Sales 模块详尽代码分析

> 自动生成于 2026-05-28，覆盖 sales 目录下全部 7 个 Kotlin 源文件

---

## 1. EditorLineItem

- **文件路径**: `feature/sales/src/main/java/com/zhihuiji/feature/sales/SaleOrderEditorViewModel.kt`
- **父类/接口**: 无（data class）
- **注解**: 无
- **职责**: 销售单编辑页面中单行商品的临时数据模型
- **设计模式**: 值对象

### 变量/属性

##### lineId: String = UUID.randomUUID().toString()
- 作用域：类公开
- 初始值：随机 UUID
- 使用场景：行项目唯一标识（用于前端操作）
- 建议：无

##### productId: Long = 0
- 作用域：类公开
- 初始值：0
- 使用场景：关联的商品 ID
- 建议：与 PurchaseLineItem 不同，此处为非空 Long，0 表示未设置，建议改为 `Long?` 以明确语义

##### productCode: String = ""
- 作用域：类公开
- 初始值：空字符串
- 使用场景：商品编码
- 建议：无

##### productName: String = ""
- 作用域：类公开
- 初始值：空字符串
- 使用场景：商品名称
- 建议：无

##### quantity: Double = 1.0
- 作用域：类公开
- 初始值：1.0
- 使用场景：销售数量
- 建议：无

##### unitPrice: Double = 0.0
- 作用域：类公开
- 初始值：0.0
- 使用场景：销售单价
- 建议：无

##### amount: Double = 0.0
- 作用域：类公开
- 初始值：0.0
- 使用场景：行金额（unitPrice * quantity）
- 建议：无

---

## 2. SaleOrderEditorUiState

- **文件路径**: `feature/sales/src/main/java/com/zhihuiji/feature/sales/SaleOrderEditorViewModel.kt`
- **父类/接口**: 无（data class）
- **注解**: 无
- **职责**: 销售单编辑页面的 UI 状态数据容器
- **设计模式**: MVI 状态模式

### 变量/属性

##### customerId: Long? = null
- 作用域：类公开
- 初始值：null
- 使用场景：选中的客户 ID
- 建议：无

##### customerName: String? = null
- 作用域：类公开
- 初始值：null
- 使用场景：选中的客户名称
- 建议：无

##### lines: List<EditorLineItem> = emptyList()
- 作用域：类公开
- 初始值：空列表
- 使用场景：销售单行项目列表
- 建议：无

##### discountAmount: Double = 0.0
- 作用域：类公开
- 初始值：0.0
- 使用场景：整单折扣金额
- 建议：无

##### notes: String = ""
- 作用域：类公开
- 初始值：空字符串
- 使用场景：备注
- 建议：无

##### totalAmount: Double = 0.0
- 作用域：类公开
- 初始值：0.0
- 使用场景：合计金额（商品合计 - 折扣）
- 建议：无

##### productSearchResults: List<ProductDto> = emptyList()
- 作用域：类公开
- 初始值：空列表
- 使用场景：商品搜索结果
- 建议：无

##### customerSearchResults: List<CustomerDto> = emptyList()
- 作用域：类公开
- 初始值：空列表
- 使用场景：客户搜索结果
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

## 3. SaleOrderEditorViewModel

- **文件路径**: `feature/sales/src/main/java/com/zhihuiji/feature/sales/SaleOrderEditorViewModel.kt`
- **父类/接口**: `ViewModel`
- **注解**: `@HiltViewModel`
- **职责**: 管理销售单创建页面的状态和业务逻辑
- **设计模式**: MVVM 模式

### 类属性

##### saleOrderRepository: SaleOrderRepository
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：调用销售单数据操作
- 建议：无

##### productRepository: ProductRepository
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：搜索商品
- 建议：无

##### customerRepository: CustomerRepository
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：搜索客户
- 建议：无

##### _uiState: MutableStateFlow<SaleOrderEditorUiState>
- 作用域：类私有
- 初始值：`MutableStateFlow(SaleOrderEditorUiState())`
- 使用场景：内部可变的 UI 状态流
- 建议：无

##### uiState: StateFlow<SaleOrderEditorUiState>
- 作用域：类公开
- 初始值：`_uiState.asStateFlow()`
- 使用场景：对外暴露只读的 UI 状态流
- 建议：无

### 函数/方法

##### init 块
- 参数：无
- 返回值：无
- 实现逻辑：预加载客户和商品列表
- 调用关系：调用了 `customerRepository.refreshCustomers()`、`productRepository.refreshProducts()`
- 建议：无

##### selectCustomer(id: Long, name: String)
- 参数：`id: Long` - 客户 ID；`name: String` - 客户名称
- 返回值：无
- 实现逻辑：更新 customerId 和 customerName
- 调用关系：无
- 建议：无

##### searchCustomers(keyword: String)
- 参数：`keyword: String` - 搜索关键词
- 返回值：无
- 实现逻辑：刷新远程数据，获取本地数据流第一批结果
- 调用关系：调用了 `customerRepository.refreshCustomers()`、`customerRepository.observeCustomers()`
- 建议：无

##### searchProducts(keyword: String)
- 参数：`keyword: String` - 搜索关键词
- 返回值：无
- 实现逻辑：刷新远程数据，获取本地数据流第一批结果
- 调用关系：调用了 `productRepository.refreshProducts()`、`productRepository.observeProducts()`
- 建议：无

##### addItem(product: ProductDto)
- 参数：`product: ProductDto` - 要添加的商品
- 返回值：无
- 实现逻辑：如果商品已存在则增加数量，否则新增一行（数量1，单价为 salePrice）
- 调用关系：无
- 建议：无

##### removeItem(lineId: String)
- 参数：`lineId: String` - 行项目 ID
- 返回值：无
- 实现逻辑：从列表中移除指定行项目，重新计算合计
- 调用关系：无
- 建议：无

##### changeQuantity(lineId: String, quantity: Double)
- 参数：`lineId: String` - 行项目 ID；`quantity: Double` - 新数量
- 返回值：无
- 实现逻辑：更新指定行项目的数量和金额，重新计算合计
- 调用关系：无
- 建议：无

##### updateNotes(notes: String)
- 参数：`notes: String` - 备注
- 返回值：无
- 实现逻辑：更新 notes
- 调用关系：无
- 建议：无

##### updateDiscount(discount: Double)
- 参数：`discount: Double` - 折扣金额
- 返回值：无
- 实现逻辑：更新 discountAmount 并重新计算合计
- 调用关系：无
- 建议：无

##### calcTotal(lines: List<EditorLineItem>): Double
- 参数：`lines: List<EditorLineItem>` - 行项目列表
- 返回值：`Double` - 合计金额
- 实现逻辑：所有行项目金额之和减去折扣金额
- 调用关系：被 `addItem()`、`removeItem()`、`changeQuantity()`、`updateDiscount()` 调用
- 建议：无

##### submitOrder()
- 参数：无
- 返回值：无
- 实现逻辑：校验至少有一个商品，构建 `CreateSaleOrderRequest`，调用 `saleOrderRepository.createSaleOrder()`
- 调用关系：调用了 `saleOrderRepository.createSaleOrder()`
- 建议：无

##### clearError()
- 参数：无
- 返回值：无
- 实现逻辑：将 error 设置为 null
- 调用关系：无
- 建议：无

---

## 4. SaleOrderEditorScreen

- **文件路径**: `feature/sales/src/main/java/com/zhihuiji/feature/sales/SaleOrderEditorScreen.kt`
- **父类/接口**: 无（Composable 函数）
- **注解**: `@Composable`、`@OptIn(ExperimentalMaterial3Api::class)`
- **职责**: 销售开单页面的 UI 渲染
- **设计模式**: 声明式 UI

### 函数参数

##### onNavigateBack: () -> Unit
- 作用：返回上一页的回调
- 建议：无

##### viewModel: SaleOrderEditorViewModel = hiltViewModel()
- 作用：注入的 ViewModel 实例
- 建议：无

### 局部变量

##### showProductPicker: Boolean
- 作用域：函数局部
- 初始值：false
- 使用场景：控制商品选择弹窗的显示/隐藏
- 建议：无

##### showCustomerPicker: Boolean
- 作用域：函数局部
- 初始值：false
- 使用场景：控制客户选择弹窗的显示/隐藏
- 建议：无

##### productSearchQuery: String
- 作用域：函数局部
- 初始值：空字符串
- 使用场景：商品搜索输入文本
- 建议：无

##### customerSearchQuery: String
- 作用域：函数局部
- 初始值：空字符串
- 使用场景：客户搜索输入文本
- 建议：无

### 关键逻辑

##### LaunchedEffect(uiState.saveSuccess)
- 当 saveSuccess 为 true 时调用 `onNavigateBack()`
- 建议：无

##### 折扣功能
- 支持整单折扣金额输入，合计 = 商品合计 - 折扣
- 建议：无

---

## 5. SaleOrderDetailUiState

- **文件路径**: `feature/sales/src/main/java/com/zhihuiji/feature/sales/SaleOrderDetailViewModel.kt`
- **父类/接口**: 无（data class）
- **注解**: 无
- **职责**: 销售单详情页面的 UI 状态数据容器
- **设计模式**: MVI 状态模式

### 变量/属性

##### order: SaleOrderDto? = null
- 作用域：类公开
- 初始值：null
- 使用场景：当前查看的销售单数据
- 建议：无

##### payments: List<PaymentDto> = emptyList()
- 作用域：类公开
- 初始值：空列表
- 使用场景：该销售单的收款记录列表
- 建议：无

##### isLoading: Boolean = false
- 作用域：类公开
- 初始值：false
- 使用场景：是否正在加载
- 建议：无

##### paymentSuccess: Boolean = false
- 作用域：类公开
- 初始值：false
- 使用场景：收款操作是否成功
- 建议：无

##### cancelSuccess: Boolean = false
- 作用域：类公开
- 初始值：false
- 使用场景：作废操作是否成功
- 建议：无

##### error: UiMessage? = null
- 作用域：类公开
- 初始值：null
- 使用场景：错误信息
- 建议：无

---

## 6. SaleOrderDetailViewModel

- **文件路径**: `feature/sales/src/main/java/com/zhihuiji/feature/sales/SaleOrderDetailViewModel.kt`
- **父类/接口**: `ViewModel`
- **注解**: `@HiltViewModel`
- **职责**: 管理销售单详情页面的状态和业务逻辑
- **设计模式**: MVVM 模式

### 类属性

##### saleOrderRepository: SaleOrderRepository
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：调用销售单数据操作
- 建议：无

##### _uiState: MutableStateFlow<SaleOrderDetailUiState>
- 作用域：类私有
- 初始值：`MutableStateFlow(SaleOrderDetailUiState())`
- 使用场景：内部可变的 UI 状态流
- 建议：无

##### uiState: StateFlow<SaleOrderDetailUiState>
- 作用域：类公开
- 初始值：`_uiState.asStateFlow()`
- 使用场景：对外暴露只读的 UI 状态流
- 建议：无

### 函数/方法

##### loadDetail(id: Long)
- 参数：`id: Long` - 销售单 ID
- 返回值：无
- 实现逻辑：设置 isLoading 为 true，获取销售单详情和收款记录
- 调用关系：调用了 `saleOrderRepository.getSaleOrder()`、`saleOrderRepository.listSalePayments()`
- 建议：两个请求串行执行，建议并行化

##### addPayment(amount: Double, method: Int, referenceNo: String?)
- 参数：`amount: Double` - 收款金额；`method: Int` - 收款方式；`referenceNo: String?` - 参考号
- 返回值：无
- 实现逻辑：调用 `saleOrderRepository.addSalePayment()`，成功时设置 paymentSuccess 并重新加载详情
- 调用关系：调用了 `saleOrderRepository.addSalePayment()`、`loadDetail()`
- 建议：无

##### cancelOrder()
- 参数：无
- 返回值：无
- 实现逻辑：调用 `saleOrderRepository.cancelSaleOrder()`，成功时设置 cancelSuccess 并重新加载详情
- 调用关系：调用了 `saleOrderRepository.cancelSaleOrder()`、`loadDetail()`
- 建议：无

##### completeOrder()
- 参数：无
- 返回值：无
- 实现逻辑：调用 `saleOrderRepository.updateSaleStatus(orderId, 1)`，成功时重新加载详情
- 调用关系：调用了 `saleOrderRepository.updateSaleStatus()`、`loadDetail()`
- 建议：无

##### clearError()
- 参数：无
- 返回值：无
- 实现逻辑：将 error 设置为 null
- 调用关系：无
- 建议：无

---

## 7. SaleOrderDetailScreen

- **文件路径**: `feature/sales/src/main/java/com/zhihuiji/feature/sales/SaleOrderDetailScreen.kt`
- **父类/接口**: 无（Composable 函数）
- **注解**: `@Composable`
- **职责**: 销售单详情页面的 UI 渲染
- **设计模式**: 声明式 UI

### 函数参数

##### orderId: Long
- 作用：要查看的销售单 ID
- 建议：无

##### onNavigateBack: () -> Unit
- 作用：返回上一页的回调
- 建议：无

##### viewModel: SaleOrderDetailViewModel = hiltViewModel()
- 作用：注入的 ViewModel 实例
- 建议：无

### 局部变量

##### showPaymentSheet: Boolean
- 作用域：函数局部
- 初始值：false
- 使用场景：控制收款弹窗的显示/隐藏
- 建议：无

### 关键逻辑

##### LaunchedEffect(orderId)
- 当 orderId 变化时调用 `viewModel.loadDetail(orderId)`
- 建议：无

##### 底部操作栏
- 待收款时显示"收款"按钮，已收齐时显示"完成订单"按钮；未作废时显示"作废"和"修改"按钮
- 建议：修改按钮当前 onClick 为空实现，建议补充导航到编辑页的逻辑

---

## 8. AmountColumn（私有 Composable）

- **文件路径**: `feature/sales/src/main/java/com/zhihuiji/feature/sales/SaleOrderDetailScreen.kt`
- **父类/接口**: 无（私有 Composable 函数）
- **注解**: `@Composable`、`private`
- **职责**: 渲染销售单详情中的金额列
- **设计模式**: 声明式 UI

### 函数参数

##### label: String
- 作用：金额标签
- 建议：无

##### value: Double
- 作用：金额值
- 建议：无

##### color: Color
- 作用：金额文字颜色
- 建议：无

### 实现逻辑
- 垂直排列标签和格式化后的金额
- 建议：无

---

## 9. SaleOrderListUiState

- **文件路径**: `feature/sales/src/main/java/com/zhihuiji/feature/sales/SaleOrderListViewModel.kt`
- **父类/接口**: 无（data class）
- **注解**: 无
- **职责**: 销售单列表页面的 UI 状态数据容器
- **设计模式**: MVI 状态模式

### 变量/属性

##### orders: List<SaleOrderDto> = emptyList()
- 作用域：类公开
- 初始值：空列表
- 使用场景：当前显示的销售单列表
- 建议：无

##### filter: SaleOrderFilter = SaleOrderFilter()
- 作用域：类公开
- 初始值：默认 SaleOrderFilter
- 使用场景：当前筛选条件
- 建议：无

##### isLoading: Boolean = false
- 作用域：类公开
- 初始值：false
- 使用场景：是否正在加载
- 建议：无

---

## 10. SaleOrderListViewModel

- **文件路径**: `feature/sales/src/main/java/com/zhihuiji/feature/sales/SaleOrderListViewModel.kt`
- **父类/接口**: `ViewModel`
- **注解**: `@HiltViewModel`
- **职责**: 管理销售单列表页面的状态和业务逻辑
- **设计模式**: MVVM 模式

### 类属性

##### saleOrderRepository: SaleOrderRepository
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：调用销售单数据操作
- 建议：无

##### _uiState: MutableStateFlow<SaleOrderListUiState>
- 作用域：类私有
- 初始值：`MutableStateFlow(SaleOrderListUiState())`
- 使用场景：内部可变的 UI 状态流
- 建议：无

##### uiState: StateFlow<SaleOrderListUiState>
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

##### loadOrders(filter: SaleOrderFilter)
- 参数：`filter: SaleOrderFilter` - 筛选条件（默认取当前状态值）
- 返回值：无
- 实现逻辑：设置加载状态，刷新远程数据，观察本地数据流更新列表
- 调用关系：调用了 `saleOrderRepository.refreshSaleOrders()`、`saleOrderRepository.observeSaleOrders()`
- 建议：`collect` 是终端操作符，建议使用 `collectLatest` 替代

##### updateFilter(keyword: String?, status: Int?)
- 参数：`keyword: String?` - 搜索关键词；`status: Int?` - 状态筛选
- 返回值：无
- 实现逻辑：更新 filter 并重新加载
- 调用关系：调用了 `loadOrders()`
- 建议：无

---

## 11. SaleOrderListScreen

- **文件路径**: `feature/sales/src/main/java/com/zhihuiji/feature/sales/SaleOrderListScreen.kt`
- **父类/接口**: 无（Composable 函数）
- **注解**: `@Composable`
- **职责**: 销售单列表页面的 UI 渲染
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
- 作用：导航到新建销售单页面的回调
- 建议：无

##### onNavigateToDetail: (Long) -> Unit = {}
- 作用：导航到销售单详情页面的回调
- 建议：无

##### viewModel: SaleOrderListViewModel = hiltViewModel()
- 作用：注入的 ViewModel 实例
- 建议：无

### 局部变量

##### uiState: SaleOrderListUiState
- 作用域：函数局部
- 初始值：通过 `viewModel.uiState.collectAsState()` 获取
- 使用场景：订阅 UI 状态
- 建议：无

##### listState: LazyListState
- 作用域：函数局部
- 初始值：`rememberLazyListState()`
- 使用场景：控制列表滚动
- 建议：无

##### statusTabs: List<String>
- 作用域：函数局部
- 初始值：`listOf("全部", "待审核", "待发货", "待收款", "已完成", "已作废")`
- 使用场景：状态筛选 Tab 列表
- 建议：无

---

## 12. SalePaymentSheet

- **文件路径**: `feature/sales/src/main/java/com/zhihuiji/feature/sales/SalePaymentSheet.kt`
- **父类/接口**: 无（Composable 函数）
- **注解**: `@Composable`、`@OptIn(ExperimentalMaterial3Api::class)`
- **职责**: 销售单收款底部弹窗的 UI 渲染
- **设计模式**: 声明式 UI

### 函数参数

##### maxAmount: Double
- 作用：最大可收金额（待收金额）
- 建议：无

##### onConfirm: (amount: Double, method: Int, referenceNo: String?) -> Unit
- 作用：确认收款的回调
- 建议：无

##### onDismiss: () -> Unit
- 作用：关闭弹窗的回调
- 建议：无

### 局部变量

##### amountText: String
- 作用域：函数局部
- 初始值：`maxAmount.toString()`（默认填满待收金额）
- 使用场景：收款金额输入文本
- 建议：无

##### selectedMethod: Int
- 作用域：函数局部
- 初始值：1（现金）
- 使用场景：当前选中的收款方式
- 建议：无

##### referenceNo: String
- 作用域：函数局部
- 初始值：空字符串
- 使用场景：参考号输入
- 建议：无

### 关键逻辑

##### 确认收款按钮
- 校验金额 > 0 后调用 onConfirm
- 建议：无
