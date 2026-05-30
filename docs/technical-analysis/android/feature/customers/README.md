# Customers 模块详尽代码分析

> 自动生成于 2026-05-28，覆盖 customers 目录下全部 6 个 Kotlin 源文件

---

## 1. CustomerDetailUiState

- **文件路径**: `feature/customers/src/main/java/com/zhihuiji/feature/customers/CustomerDetailViewModel.kt`
- **父类/接口**: 无（data class）
- **注解**: 无
- **职责**: 客户详情页面的 UI 状态数据容器
- **设计模式**: MVI 状态模式

### 变量/属性

##### customer: CustomerDto? = null
- 作用域：类公开
- 初始值：null
- 使用场景：当前查看的客户数据
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

## 2. CustomerDetailViewModel

- **文件路径**: `feature/customers/src/main/java/com/zhihuiji/feature/customers/CustomerDetailViewModel.kt`
- **父类/接口**: `ViewModel`
- **注解**: `@HiltViewModel`
- **职责**: 管理客户详情页面的状态和业务逻辑
- **设计模式**: MVVM 模式

### 类属性

##### customerRepository: CustomerRepository
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：调用客户数据操作
- 建议：无

##### _uiState: MutableStateFlow<CustomerDetailUiState>
- 作用域：类私有
- 初始值：`MutableStateFlow(CustomerDetailUiState())`
- 使用场景：内部可变的 UI 状态流
- 建议：无

##### uiState: StateFlow<CustomerDetailUiState>
- 作用域：类公开
- 初始值：`_uiState.asStateFlow()`
- 使用场景：对外暴露只读的 UI 状态流
- 建议：无

### 函数/方法

##### loadCustomer(id: Long)
- 参数：`id: Long` - 客户 ID
- 返回值：无
- 实现逻辑：设置 isLoading 为 true，调用 `customerRepository.getCustomer(id)`，成功时更新 customer，失败时更新 error
- 调用关系：调用了 `customerRepository.getCustomer()`
- 建议：无

---

## 3. CustomerDetailScreen

- **文件路径**: `feature/customers/src/main/java/com/zhihuiji/feature/customers/CustomerDetailScreen.kt`
- **父类/接口**: 无（Composable 函数）
- **注解**: `@Composable`
- **职责**: 客户详情页面的 UI 渲染
- **设计模式**: 声明式 UI

### 函数参数

##### customerId: Long
- 作用：要查看的客户 ID
- 建议：无

##### onNavigateBack: () -> Unit
- 作用：返回上一页的回调
- 建议：无

##### onNavigateToEditor: (Long) -> Unit = {}
- 作用：导航到编辑页面的回调
- 建议：无

##### viewModel: CustomerDetailViewModel = hiltViewModel()
- 作用：注入的 ViewModel 实例
- 建议：无

### 局部变量

##### uiState: CustomerDetailUiState
- 作用域：函数局部
- 初始值：通过 `viewModel.uiState.collectAsState()` 获取
- 使用场景：订阅 UI 状态
- 建议：无

### 关键逻辑

##### LaunchedEffect(customerId)
- 当 customerId 变化时调用 `viewModel.loadCustomer(customerId)`
- 建议：无

---

## 4. CustomerEditorUiState

- **文件路径**: `feature/customers/src/main/java/com/zhihuiji/feature/customers/CustomerEditorViewModel.kt`
- **父类/接口**: 无（data class）
- **注解**: 无
- **职责**: 客户编辑页面的 UI 状态数据容器
- **设计模式**: MVI 状态模式

### 变量/属性

##### draft: CustomerDto = CustomerDto()
- 作用域：类公开
- 初始值：空的 CustomerDto
- 使用场景：正在编辑的客户草稿数据
- 建议：无

##### existingId: Long? = null
- 作用域：类公开
- 初始值：null
- 使用场景：正在编辑的客户 ID，null 表示新建模式
- 建议：无

##### isLoading: Boolean = false
- 作用域：类公开
- 初始值：false
- 使用场景：是否正在加载客户数据
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

## 5. CustomerEditorViewModel

- **文件路径**: `feature/customers/src/main/java/com/zhihuiji/feature/customers/CustomerEditorViewModel.kt`
- **父类/接口**: `ViewModel`
- **注解**: `@HiltViewModel`
- **职责**: 管理客户编辑（新增/修改）页面的状态和业务逻辑
- **设计模式**: MVVM 模式

### 类属性

##### customerRepository: CustomerRepository
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：调用客户数据操作
- 建议：无

##### _uiState: MutableStateFlow<CustomerEditorUiState>
- 作用域：类私有
- 初始值：`MutableStateFlow(CustomerEditorUiState())`
- 使用场景：内部可变的 UI 状态流
- 建议：无

##### uiState: StateFlow<CustomerEditorUiState>
- 作用域：类公开
- 初始值：`_uiState.asStateFlow()`
- 使用场景：对外暴露只读的 UI 状态流
- 建议：无

### 函数/方法

##### loadCustomer(id: Long)
- 参数：`id: Long` - 客户 ID
- 返回值：无
- 实现逻辑：设置 isLoading 为 true，调用 `customerRepository.getCustomer(id)`，成功时更新 existingId 和 draft
- 调用关系：调用了 `customerRepository.getCustomer()`
- 建议：无

##### updateDraft(update: (CustomerDto) -> CustomerDto)
- 参数：`update: (CustomerDto) -> CustomerDto` - 草稿更新函数
- 返回值：无
- 实现逻辑：应用更新函数到当前 draft
- 调用关系：无
- 建议：无

##### saveCustomer()
- 参数：无
- 返回值：无
- 实现逻辑：校验 name 不为空，根据 existingId 是否为 null 决定调用 `updateCustomer` 或 `createCustomer`，成功时设置 saveSuccess
- 调用关系：调用了 `customerRepository.updateCustomer()` 或 `customerRepository.createCustomer()`
- 建议：无

##### clearError()
- 参数：无
- 返回值：无
- 实现逻辑：将 error 设置为 null
- 调用关系：无
- 建议：无

---

## 6. CustomerEditorScreen

- **文件路径**: `feature/customers/src/main/java/com/zhihuiji/feature/customers/CustomerEditorScreen.kt`
- **父类/接口**: 无（Composable 函数）
- **注解**: `@Composable`
- **职责**: 客户编辑页面的 UI 渲染
- **设计模式**: 声明式 UI

### 函数参数

##### customerId: Long?
- 作用：要编辑的客户 ID，null 表示新建
- 建议：无

##### onNavigateBack: () -> Unit
- 作用：返回上一页的回调
- 建议：无

##### viewModel: CustomerEditorViewModel = hiltViewModel()
- 作用：注入的 ViewModel 实例
- 建议：无

### 关键逻辑

##### LaunchedEffect(customerId)
- 当 customerId 存在且大于 0 时调用 `viewModel.loadCustomer(customerId)`
- 建议：无

##### LaunchedEffect(uiState.saveSuccess)
- 当 saveSuccess 为 true 时调用 `onNavigateBack()`
- 建议：无

---

## 7. CustomerListUiState

- **文件路径**: `feature/customers/src/main/java/com/zhihuiji/feature/customers/CustomerViewModel.kt`
- **父类/接口**: 无（data class）
- **注解**: 无
- **职责**: 客户列表页面的 UI 状态数据容器
- **设计模式**: MVI 状态模式

### 变量/属性

##### customers: List<CustomerDto> = emptyList()
- 作用域：类公开
- 初始值：空列表
- 使用场景：当前显示的客户列表数据
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

## 8. CustomerViewModel

- **文件路径**: `feature/customers/src/main/java/com/zhihuiji/feature/customers/CustomerViewModel.kt`
- **父类/接口**: `ViewModel`
- **注解**: `@HiltViewModel`
- **职责**: 管理客户列表页面的状态和业务逻辑
- **设计模式**: MVVM 模式

### 类属性

##### customerRepository: CustomerRepository
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：调用客户数据操作
- 建议：无

##### _uiState: MutableStateFlow<CustomerListUiState>
- 作用域：类私有
- 初始值：`MutableStateFlow(CustomerListUiState())`
- 使用场景：内部可变的 UI 状态流
- 建议：无

##### uiState: StateFlow<CustomerListUiState>
- 作用域：类公开
- 初始值：`_uiState.asStateFlow()`
- 使用场景：对外暴露只读的 UI 状态流
- 建议：无

### 函数/方法

##### init 块
- 参数：无
- 返回值：无
- 实现逻辑：初始化时调用 `loadCustomers()`
- 调用关系：调用了 `loadCustomers()`
- 建议：无

##### loadCustomers(keyword: String)
- 参数：`keyword: String` - 搜索关键词（默认取当前状态值）
- 返回值：无
- 实现逻辑：设置加载状态，刷新远程数据，观察本地数据流更新列表
- 调用关系：调用了 `customerRepository.refreshCustomers()`、`customerRepository.observeCustomers()`
- 建议：`collect` 是终端操作符，多次调用 loadCustomers 可能导致状态覆盖，建议使用 `collectLatest` 替代

##### deleteCustomer(id: Long)
- 参数：`id: Long` - 客户 ID
- 返回值：无
- 实现逻辑：删除客户后重新加载列表
- 调用关系：调用了 `customerRepository.deleteCustomer()`、`loadCustomers()`
- 建议：当前 Screen 中未暴露删除操作入口，此方法暂未被调用

---

## 9. CustomerListScreen

- **文件路径**: `feature/customers/src/main/java/com/zhihuiji/feature/customers/CustomerListScreen.kt`
- **父类/接口**: 无（Composable 函数）
- **注解**: `@Composable`
- **职责**: 客户列表页面的 UI 渲染
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
- 作用：导航到编辑页的回调
- 建议：无

##### onNavigateToDetail: (Long) -> Unit = {}
- 作用：导航到详情页的回调
- 建议：无

##### viewModel: CustomerViewModel = hiltViewModel()
- 作用：注入的 ViewModel 实例
- 建议：无

### 局部变量

##### uiState: CustomerListUiState
- 作用域：函数局部
- 初始值：通过 `viewModel.uiState.collectAsState()` 获取
- 使用场景：订阅 UI 状态
- 建议：无

##### selectedTab: Int
- 作用域：函数局部
- 初始值：0
- 使用场景：当前选中的筛选 Tab 索引（全部/正常/欠款/已停用）
- 建议：筛选逻辑在 Screen 端通过 `when` 表达式实现，而非通过 ViewModel，建议将筛选逻辑下沉到 ViewModel 以保持一致性

##### listState: LazyListState
- 作用域：函数局部
- 初始值：`rememberLazyListState()`
- 使用场景：控制列表滚动
- 建议：无
