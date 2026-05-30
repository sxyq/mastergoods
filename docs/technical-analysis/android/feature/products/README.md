# Products 模块详尽代码分析

> 自动生成于 2026-05-28，覆盖 products 目录下全部 5 个 Kotlin 源文件

---

## 1. ProductEditorUiState

- **文件路径**: `feature/products/src/main/java/com/zhihuiji/feature/products/ProductEditorViewModel.kt`
- **父类/接口**: 无（data class）
- **注解**: 无
- **职责**: 商品编辑页面的 UI 状态数据容器
- **设计模式**: MVI 状态模式

### 变量/属性

##### draft: ProductDraft = ProductDraft()
- 作用域：类公开
- 初始值：空的 ProductDraft
- 使用场景：正在编辑的商品草稿数据
- 建议：无

##### existingId: Long? = null
- 作用域：类公开
- 初始值：null
- 使用场景：正在编辑的商品 ID，null 表示新建模式
- 建议：无

##### isLoading: Boolean = false
- 作用域：类公开
- 初始值：false
- 使用场景：是否正在加载商品数据
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

## 2. ProductEditorViewModel

- **文件路径**: `feature/products/src/main/java/com/zhihuiji/feature/products/ProductEditorViewModel.kt`
- **父类/接口**: `ViewModel`
- **注解**: `@HiltViewModel`
- **职责**: 管理商品编辑（新增/修改）页面的状态和业务逻辑
- **设计模式**: MVVM 模式

### 类属性

##### productRepository: ProductRepository
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：调用商品数据操作
- 建议：无

##### _uiState: MutableStateFlow<ProductEditorUiState>
- 作用域：类私有
- 初始值：`MutableStateFlow(ProductEditorUiState())`
- 使用场景：内部可变的 UI 状态流
- 建议：无

##### uiState: StateFlow<ProductEditorUiState>
- 作用域：类公开
- 初始值：`_uiState.asStateFlow()`
- 使用场景：对外暴露只读的 UI 状态流
- 建议：无

### 函数/方法

##### loadProduct(id: Long)
- 参数：`id: Long` - 商品 ID
- 返回值：无
- 实现逻辑：设置 isLoading 为 true，调用 `productRepository.getProduct(id)`，成功时将 DTO 转换为 ProductDraft 并更新 existingId
- 调用关系：调用了 `productRepository.getProduct()`
- 建议：无

##### updateDraft(update: (ProductDraft) -> ProductDraft)
- 参数：`update: (ProductDraft) -> ProductDraft` - 草稿更新函数
- 返回值：无
- 实现逻辑：应用更新函数到当前 draft
- 调用关系：无
- 建议：无

##### saveProduct()
- 参数：无
- 返回值：无
- 实现逻辑：校验 name 不为空，根据 existingId 是否为 null 决定调用 `updateProduct` 或 `createProduct`
- 调用关系：调用了 `productRepository.updateProduct()` 或 `productRepository.createProduct()`
- 建议：无

##### adjustStock(delta: Double, reason: String?)
- 参数：`delta: Double` - 库存调整数量（正数入库，负数出库）；`reason: String?` - 调整原因
- 返回值：无
- 实现逻辑：校验 existingId 不为 null，调用 `productRepository.adjustStock()`，成功后重新加载商品数据
- 调用关系：调用了 `productRepository.adjustStock()`、`loadProduct()`
- 建议：无

##### clearError()
- 参数：无
- 返回值：无
- 实现逻辑：将 error 设置为 null
- 调用关系：无
- 建议：无

---

## 3. ProductEditorScreen

- **文件路径**: `feature/products/src/main/java/com/zhihuiji/feature/products/ProductEditorScreen.kt`
- **父类/接口**: 无（Composable 函数）
- **注解**: `@Composable`
- **职责**: 商品编辑页面的 UI 渲染
- **设计模式**: 声明式 UI

### 函数参数

##### productId: Long?
- 作用：要编辑的商品 ID，null 表示新建
- 建议：无

##### onNavigateBack: () -> Unit
- 作用：返回上一页的回调
- 建议：无

##### onStockAdjust: () -> Unit = {}
- 作用：打开库存调整弹窗的回调
- 建议：无

##### viewModel: ProductEditorViewModel = hiltViewModel()
- 作用：注入的 ViewModel 实例
- 建议：无

### 关键逻辑

##### LaunchedEffect(productId)
- 当 productId 存在且大于 0 时调用 `viewModel.loadProduct(productId)`
- 建议：无

##### LaunchedEffect(uiState.saveSuccess)
- 当 saveSuccess 为 true 时调用 `onNavigateBack()`
- 建议：无

##### 库存调整按钮
- 仅在编辑模式（existingId != null）下显示
- 建议：无

---

## 4. ProductListUiState

- **文件路径**: `feature/products/src/main/java/com/zhihuiji/feature/products/ProductListViewModel.kt`
- **父类/接口**: 无（data class）
- **注解**: 无
- **职责**: 商品列表页面的 UI 状态数据容器
- **设计模式**: MVI 状态模式

### 变量/属性

##### products: List<ProductDto> = emptyList()
- 作用域：类公开
- 初始值：空列表
- 使用场景：当前显示的商品列表
- 建议：无

##### keyword: String = ""
- 作用域：类公开
- 初始值：空字符串
- 使用场景：当前搜索关键词
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

## 5. ProductListViewModel

- **文件路径**: `feature/products/src/main/java/com/zhihuiji/feature/products/ProductListViewModel.kt`
- **父类/接口**: `ViewModel`
- **注解**: `@HiltViewModel`
- **职责**: 管理商品列表页面的状态和业务逻辑
- **设计模式**: MVVM 模式

### 类属性

##### productRepository: ProductRepository
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：调用商品数据操作
- 建议：无

##### _uiState: MutableStateFlow<ProductListUiState>
- 作用域：类私有
- 初始值：`MutableStateFlow(ProductListUiState())`
- 使用场景：内部可变的 UI 状态流
- 建议：无

##### uiState: StateFlow<ProductListUiState>
- 作用域：类公开
- 初始值：`_uiState.asStateFlow()`
- 使用场景：对外暴露只读的 UI 状态流
- 建议：无

### 函数/方法

##### init 块
- 参数：无
- 返回值：无
- 实现逻辑：初始化时调用 `loadProducts()`
- 调用关系：调用了 `loadProducts()`
- 建议：无

##### loadProducts(keyword: String)
- 参数：`keyword: String` - 搜索关键词（默认取当前状态值）
- 返回值：无
- 实现逻辑：设置加载状态，刷新远程数据，观察本地数据流更新列表
- 调用关系：调用了 `productRepository.refreshProducts()`、`productRepository.observeProducts()`
- 建议：`collect` 是终端操作符，建议使用 `collectLatest` 替代

##### deleteProduct(id: Long)
- 参数：`id: Long` - 商品 ID
- 返回值：无
- 实现逻辑：删除商品后重新加载列表
- 调用关系：调用了 `productRepository.deleteProduct()`、`loadProducts()`
- 建议：当前 Screen 中未暴露删除操作入口，此方法暂未被调用

---

## 6. ProductListScreen

- **文件路径**: `feature/products/src/main/java/com/zhihuiji/feature/products/ProductListScreen.kt`
- **父类/接口**: 无（Composable 函数）
- **注解**: `@Composable`
- **职责**: 商品列表页面的 UI 渲染
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

##### onNavigateToEditor: (Long?) -> Unit = {}
- 作用：导航到编辑页的回调，参数为商品 ID（null 表示新建）
- 建议：无

##### viewModel: ProductListViewModel = hiltViewModel()
- 作用：注入的 ViewModel 实例
- 建议：无

### 局部变量

##### uiState: ProductListUiState
- 作用域：函数局部
- 初始值：通过 `viewModel.uiState.collectAsState()` 获取
- 使用场景：订阅 UI 状态
- 建议：无

##### selectedTab: Int
- 作用域：函数局部
- 初始值：0
- 使用场景：当前选中的筛选 Tab 索引（全部/低库存/正常/缺货）
- 建议：与 customers 模块类似，筛选逻辑在 Screen 端实现，建议下沉到 ViewModel

##### listState: LazyListState
- 作用域：函数局部
- 初始值：`rememberLazyListState()`
- 使用场景：控制列表滚动
- 建议：无

---

## 7. StockAdjustSheet

- **文件路径**: `feature/products/src/main/java/com/zhihuiji/feature/products/StockAdjustSheet.kt`
- **父类/接口**: 无（Composable 函数）
- **注解**: `@Composable`、`@OptIn(ExperimentalMaterial3Api::class)`
- **职责**: 库存调整底部弹窗的 UI 渲染
- **设计模式**: 声明式 UI

### 函数参数

##### productName: String
- 作用：商品名称（显示在弹窗标题中）
- 建议：无

##### currentStock: Double
- 作用：当前库存数量
- 建议：无

##### onConfirm: (delta: Double, reason: String) -> Unit
- 作用：确认调整的回调，传递调整数量（带正负号）和原因
- 建议：无

##### onDismiss: () -> Unit
- 作用：关闭弹窗的回调
- 建议：无

### 局部变量

##### deltaText: String
- 作用域：函数局部
- 初始值：空字符串
- 使用场景：调整数量输入文本
- 建议：无

##### reason: String
- 作用域：函数局部
- 初始值：空字符串
- 使用场景：调整原因输入
- 建议：无

##### adjustType: Int
- 作用域：函数局部
- 初始值：0（入库）
- 使用场景：调整类型选择（入库/出库/盘盈/盘亏）
- 建议：无

### 关键逻辑

##### 确认调整按钮
- 根据调整类型计算带符号的 delta（入库/盘盈为正，出库/盘亏为负），校验数量 > 0 后调用 onConfirm
- 建议：无
