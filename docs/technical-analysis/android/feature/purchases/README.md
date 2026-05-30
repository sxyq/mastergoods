# Purchases 模块详尽代码分析

> 自动生成于 2026-05-28，覆盖 purchases 目录下全部 6 个 Kotlin 源文件

---

## 1. PurchaseLineItem

- **文件路径**: `feature/purchases/src/main/java/com/zhihuiji/feature/purchases/PurchaseOrderEditorViewModel.kt`
- **父类/接口**: 无（data class）
- **注解**: 无
- **职责**: 采购单编辑页面中单行商品的临时数据模型
- **设计模式**: 值对象

### 变量/属性

##### lineId: String = UUID.randomUUID().toString()
- 作用域：类公开
- 初始值：随机 UUID
- 使用场景：行项目唯一标识（用于前端操作）
- 建议：无

##### productId: Long? = null
- 作用域：类公开
- 初始值：null
- 使用场景：关联的商品 ID
- 建议：无

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
- 使用场景：采购数量
- 建议：无

##### unitCost: Double = 0.0
- 作用域：类公开
- 初始值：0.0
- 使用场景：单价
- 建议：无

##### amount: Double = 0.0
- 作用域：类公开
- 初始值：0.0
- 使用场景：行金额（unitCost * quantity）
- 建议：无

---

## 2. PurchaseOrderEditorUiState

- **文件路径**: `feature/purchases/src/main/java/com/zhihuiji/feature/purchases/PurchaseOrderEditorViewModel.kt`
- **父类/接口**: 无（data class）
- **注解**: 无
- **职责**: 采购单编辑页面的 UI 状态数据容器
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

##### lines: List<PurchaseLineItem> = emptyList()
- 作用域：类公开
- 初始值：空列表
- 使用场景：采购单行项目列表
- 建议：无

##### notes: String = ""
- 作用域：类公开
- 初始值：空字符串
- 使用场景：备注
- 建议：无

##### totalAmount: Double = 0.0
- 作用域：类公开
- 初始值：0.0
- 使用场景：应付合计金额
- 建议：无

##### productSearchResults: List<ProductDto> = emptyList()
- 作用域：类公开
- 初始值：空列表
- 使用场景：商品搜索结果
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

## 3. PurchaseOrderEditorViewModel

- **文件路径**: `feature/purchases/src/main/java/com/zhihuiji/feature/purchases/PurchaseOrderEditorViewModel.kt`
- **父类/接口**: `ViewModel`
- **注解**: `@HiltViewModel`
- **职责**: 管理采购单创建页面的状态和业务逻辑
- **设计模式**: MVVM 模式

### 类属性

##### purchaseOrderRepository: PurchaseOrderRepository
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：调用采购单数据操作
- 建议：无

##### productRepository: ProductRepository
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：搜索商品
- 建议：无

##### supplierRepository: SupplierRepository
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：搜索供应商
- 建议：无

##### _uiState: MutableStateFlow<PurchaseOrderEditorUiState>
- 作用域：类私有
- 初始值：`MutableStateFlow(PurchaseOrderEditorUiState())`
- 使用场景：内部可变的 UI 状态流
- 建议：无

##### uiState: StateFlow<PurchaseOrderEditorUiState>
- 作用域：类公开
- 初始值：`_uiState.asStateFlow()`
- 使用场景：对外暴露只读的 UI 状态流
- 建议：无

### 函数/方法

##### init 块
- 参数：无
- 返回值：无
- 实现逻辑：预加载供应商和商品列表
- 调用关系：调用了 `supplierRepository.refreshSuppliers()`、`productRepository.refreshProducts()`
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

##### searchProducts(keyword: String)
- 参数：`keyword: String` - 搜索关键词
- 返回值：无
- 实现逻辑：刷新远程数据，获取本地数据流第一批结果
- 调用关系：调用了 `productRepository.refreshProducts()`、`productRepository.observeProducts()`
- 建议：无

##### addItem(product: ProductDto)
- 参数：`product: ProductDto` - 要添加的商品
- 返回值：无
- 实现逻辑：如果商品已存在则增加数量，否则新增一行（数量1，单价为 purchasePrice）
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

##### calcTotal(lines: List<PurchaseLineItem>): Double
- 参数：`lines: List<PurchaseLineItem>` - 行项目列表
- 返回值：`Double` - 合计金额
- 实现逻辑：对所有行项目的 amount 求和
- 调用关系：被 `addItem()`、`removeItem()`、`changeQuantity()` 调用
- 建议：无

##### submitOrder()
- 参数：无
- 返回值：无
- 实现逻辑：校验供应商已选择且至少有一个商品，构建 `CreatePurchaseOrderRequest`，调用 `purchaseOrderRepository.createPurchaseOrder()`
- 调用关系：调用了 `purchaseOrderRepository.createPurchaseOrder()`
- 建议：无

##### clearError()
- 参数：无
- 返回值：无
- 实现逻辑：将 error 设置为 null
- 调用关系：无
- 建议：无

---

## 4. PurchaseOrderEditorScreen

- **文件路径**: `feature/purchases/src/main/java/com/zhihuiji/feature/purchases/PurchaseOrderEditorScreen.kt`
- **父类/接口**: 无（Composable 函数）
- **注解**: `@Composable`、`@OptIn(ExperimentalMaterial3Api::class)`
- **职责**: 采购开单页面的 UI 渲染
- **设计模式**: 声明式 UI

### 函数参数

##### onNavigateBack: () -> Unit
- 作用：返回上一页的回调
- 建议：无

##### viewModel: PurchaseOrderEditorViewModel = hiltViewModel()
- 作用：注入的 ViewModel 实例
- 建议：无

### 局部变量

##### showProductPicker: Boolean
- 作用域：函数局部
- 初始值：false
- 使用场景：控制商品选择弹窗的显示/隐藏
- 建议：无

##### showSupplierPicker: Boolean
- 作用域：函数局部
- 初始值：false
- 使用场景：控制供应商选择弹窗的显示/隐藏
- 建议：无

##### productSearchQuery: String
- 作用域：函数局部
- 初始值：空字符串
- 使用场景：商品搜索输入文本
- 建议：无

##### supplierSearchQuery: String
- 作用域：函数局部
- 初始值：空字符串
- 使用场景：供应商搜索输入文本
- 建议：无

### 关键逻辑

##### LaunchedEffect(uiState.saveSuccess)
- 当 saveSuccess 为 true 时调用 `onNavigateBack()`
- 建议：无

##### 商品/供应商选择弹窗
- 使用 `ModalBottomSheet` 展示搜索列表
- 建议：无

---

## 5. PurchaseOrderDetailUiState

- **文件路径**: `feature/purchases/src/main/java/com/zhihuiji/feature/purchases/PurchaseOrderDetailViewModel.kt`
- **父类/接口**: 无（data class）
- **注解**: 无
- **职责**: 采购单详情页面的 UI 状态数据容器
- **设计模式**: MVI 状态模式

### 变量/属性

##### order: PurchaseOrderDto? = null
- 作用域：类公开
- 初始值：null
- 使用场景：当前查看的采购单数据
- 建议：无

##### isLoading: Boolean = false
- 作用域：类公开
- 初始值：false
- 使用场景：是否正在加载
- 建议：无

##### error: UiMessage? = null
- 作用域：类公开
- 初始值：null
- 使用场景：错误信息
- 建议：无

---

## 6. PurchaseOrderDetailViewModel

- **文件路径**: `feature/purchases/src/main/java/com/zhihuiji/feature/purchases/PurchaseOrderDetailViewModel.kt`
- **父类/接口**: `ViewModel`
- **注解**: `@HiltViewModel`
- **职责**: 管理采购单详情页面的状态和业务逻辑
- **设计模式**: MVVM 模式

### 类属性

##### purchaseOrderRepository: PurchaseOrderRepository
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：调用采购单数据操作
- 建议：无

##### _uiState: MutableStateFlow<PurchaseOrderDetailUiState>
- 作用域：类私有
- 初始值：`MutableStateFlow(PurchaseOrderDetailUiState())`
- 使用场景：内部可变的 UI 状态流
- 建议：无

##### uiState: StateFlow<PurchaseOrderDetailUiState>
- 作用域：类公开
- 初始值：`_uiState.asStateFlow()`
- 使用场景：对外暴露只读的 UI 状态流
- 建议：无

### 函数/方法

##### loadDetail(id: Long)
- 参数：`id: Long` - 采购单 ID
- 返回值：无
- 实现逻辑：设置 isLoading 为 true，调用 `purchaseOrderRepository.getPurchaseOrder(id)`，成功时更新 order
- 调用关系：调用了 `purchaseOrderRepository.getPurchaseOrder()`
- 建议：无

##### clearError()
- 参数：无
- 返回值：无
- 实现逻辑：将 error 设置为 null
- 调用关系：无
- 建议：无

---

## 7. PurchaseOrderDetailScreen

- **文件路径**: `feature/purchases/src/main/java/com/zhihuiji/feature/purchases/PurchaseOrderDetailScreen.kt`
- **父类/接口**: 无（Composable 函数）
- **注解**: `@Composable`
- **职责**: 采购单详情页面的 UI 渲染
- **设计模式**: 声明式 UI

### 函数参数

##### orderId: Long
- 作用：要查看的采购单 ID
- 建议：无

##### onNavigateBack: () -> Unit
- 作用：返回上一页的回调
- 建议：无

##### viewModel: PurchaseOrderDetailViewModel = hiltViewModel()
- 作用：注入的 ViewModel 实例
- 建议：无

### 关键逻辑

##### LaunchedEffect(orderId)
- 当 orderId 变化时调用 `viewModel.loadDetail(orderId)`
- 建议：无

---

## 8. PurchaseAmountColumn（私有 Composable）

- **文件路径**: `feature/purchases/src/main/java/com/zhihuiji/feature/purchases/PurchaseOrderDetailScreen.kt`
- **父类/接口**: 无（私有 Composable 函数）
- **注解**: `@Composable`、`private`
- **职责**: 渲染采购单详情中的金额列
- **设计模式**: 声明式 UI

### 函数参数

##### label: String
- 作用：金额标签
- 建议：无

##### value: Double
- 作用：金额值
- 建议：无

### 实现逻辑
- 垂直排列标签和格式化后的金额
- 建议：无

---

## 9. PurchaseOrderListUiState

- **文件路径**: `feature/purchases/src/main/java/com/zhihuiji/feature/purchases/PurchaseOrderViewModel.kt`
- **父类/接口**: 无（data class）
- **注解**: 无
- **职责**: 采购单列表页面的 UI 状态数据容器
- **设计模式**: MVI 状态模式

### 变量/属性

##### orders: List<PurchaseOrderDto> = emptyList()
- 作用域：类公开
- 初始值：空列表
- 使用场景：当前显示的采购单列表
- 建议：无

##### filter: PurchaseOrderFilter = PurchaseOrderFilter()
- 作用域：类公开
- 初始值：默认 PurchaseOrderFilter
- 使用场景：当前筛选条件
- 建议：无

##### isLoading: Boolean = false
- 作用域：类公开
- 初始值：false
- 使用场景：是否正在加载
- 建议：无

---

## 10. PurchaseOrderViewModel

- **文件路径**: `feature/purchases/src/main/java/com/zhihuiji/feature/purchases/PurchaseOrderViewModel.kt`
- **父类/接口**: `ViewModel`
- **注解**: `@HiltViewModel`
- **职责**: 管理采购单列表页面的状态和业务逻辑
- **设计模式**: MVVM 模式

### 类属性

##### purchaseOrderRepository: PurchaseOrderRepository
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：调用采购单数据操作
- 建议：无

##### _uiState: MutableStateFlow<PurchaseOrderListUiState>
- 作用域：类私有
- 初始值：`MutableStateFlow(PurchaseOrderListUiState())`
- 使用场景：内部可变的 UI 状态流
- 建议：无

##### uiState: StateFlow<PurchaseOrderListUiState>
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

##### loadOrders(filter: PurchaseOrderFilter)
- 参数：`filter: PurchaseOrderFilter` - 筛选条件（默认取当前状态值）
- 返回值：无
- 实现逻辑：设置加载状态，刷新远程数据，观察本地数据流更新列表
- 调用关系：调用了 `purchaseOrderRepository.refreshPurchaseOrders()`、`purchaseOrderRepository.observePurchaseOrders()`
- 建议：`collect` 是终端操作符，建议使用 `collectLatest` 替代

---

## 11. PurchaseOrderListScreen

- **文件路径**: `feature/purchases/src/main/java/com/zhihuiji/feature/purchases/PurchaseOrderListScreen.kt`
- **父类/接口**: 无（Composable 函数）
- **注解**: `@Composable`
- **职责**: 采购单列表页面的 UI 渲染
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
- 作用：导航到新建采购单页面的回调
- 建议：无

##### onNavigateToDetail: (Long) -> Unit = {}
- 作用：导航到采购单详情页面的回调
- 建议：无

##### viewModel: PurchaseOrderViewModel = hiltViewModel()
- 作用：注入的 ViewModel 实例
- 建议：无

### 局部变量

##### uiState: PurchaseOrderListUiState
- 作用域：函数局部
- 初始值：通过 `viewModel.uiState.collectAsState()` 获取
- 使用场景：订阅 UI 状态
- 建议：无

##### listState: LazyListState
- 作用域：函数局部
- 初始值：`rememberLazyListState()`
- 使用场景：控制列表滚动
- 建议：无
