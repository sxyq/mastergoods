# Suppliers 模块详尽代码分析

> 自动生成于 2026-05-28，覆盖 suppliers 目录下全部 6 个 Kotlin 源文件

---

## 1. SupplierDetailUiState

- **文件路径**: `feature/suppliers/src/main/java/com/zhihuiji/feature/suppliers/SupplierDetailViewModel.kt`
- **父类/接口**: 无（data class）
- **注解**: 无
- **职责**: 供应商详情页面的 UI 状态数据容器
- **设计模式**: MVI 状态模式

### 变量/属性

##### supplier: SupplierDto? = null
- 作用域：类公开
- 初始值：null
- 使用场景：当前查看的供应商数据
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

## 2. SupplierDetailViewModel

- **文件路径**: `feature/suppliers/src/main/java/com/zhihuiji/feature/suppliers/SupplierDetailViewModel.kt`
- **父类/接口**: `ViewModel`
- **注解**: `@HiltViewModel`
- **职责**: 管理供应商详情页面的状态和业务逻辑
- **设计模式**: MVVM 模式

### 类属性

##### supplierRepository: SupplierRepository
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：调用供应商数据操作
- 建议：无

##### _uiState: MutableStateFlow<SupplierDetailUiState>
- 作用域：类私有
- 初始值：`MutableStateFlow(SupplierDetailUiState())`
- 使用场景：内部可变的 UI 状态流
- 建议：无

##### uiState: StateFlow<SupplierDetailUiState>
- 作用域：类公开
- 初始值：`_uiState.asStateFlow()`
- 使用场景：对外暴露只读的 UI 状态流
- 建议：无

### 函数/方法

##### loadSupplier(id: Long)
- 参数：`id: Long` - 供应商 ID
- 返回值：无
- 实现逻辑：设置 isLoading 为 true，调用 `supplierRepository.getSupplier(id)`，成功时更新 supplier，失败时更新 error
- 调用关系：调用了 `supplierRepository.getSupplier()`
- 建议：无

---

## 3. SupplierDetailScreen

- **文件路径**: `feature/suppliers/src/main/java/com/zhihuiji/feature/suppliers/SupplierDetailScreen.kt`
- **父类/接口**: 无（Composable 函数）
- **注解**: `@Composable`
- **职责**: 供应商详情页面的 UI 渲染
- **设计模式**: 声明式 UI

### 函数参数

##### supplierId: Long
- 作用：要查看的供应商 ID
- 建议：无

##### onNavigateBack: () -> Unit
- 作用：返回上一页的回调
- 建议：无

##### onNavigateToEditor: (Long) -> Unit = {}
- 作用：导航到编辑页面的回调
- 建议：无

##### viewModel: SupplierDetailViewModel = hiltViewModel()
- 作用：注入的 ViewModel 实例
- 建议：无

### 关键逻辑

##### LaunchedEffect(supplierId)
- 当 supplierId 变化时调用 `viewModel.loadSupplier(supplierId)`
- 建议：无

##### 底部操作栏
- 显示"编辑"按钮，点击后导航到编辑页面
- 建议：无

---

## 4. SupplierEditorUiState

- **文件路径**: `feature/suppliers/src/main/java/com/zhihuiji/feature/suppliers/SupplierEditorViewModel.kt`
- **父类/接口**: 无（data class）
- **注解**: 无
- **职责**: 供应商编辑页面的 UI 状态数据容器
- **设计模式**: MVI 状态模式

### 变量/属性

##### draft: SupplierDto = SupplierDto()
- 作用域：类公开
- 初始值：空的 SupplierDto
- 使用场景：正在编辑的供应商草稿数据
- 建议：无

##### existingId: Long? = null
- 作用域：类公开
- 初始值：null
- 使用场景：正在编辑的供应商 ID，null 表示新建模式
- 建议：无

##### isLoading: Boolean = false
- 作用域：类公开
- 初始值：false
- 使用场景：是否正在加载供应商数据
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

## 5. SupplierEditorViewModel

- **文件路径**: `feature/suppliers/src/main/java/com/zhihuiji/feature/suppliers/SupplierEditorViewModel.kt`
- **父类/接口**: `ViewModel`
- **注解**: `@HiltViewModel`
- **职责**: 管理供应商编辑（新增/修改）页面的状态和业务逻辑
- **设计模式**: MVVM 模式

### 类属性

##### supplierRepository: SupplierRepository
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：调用供应商数据操作
- 建议：无

##### _uiState: MutableStateFlow<SupplierEditorUiState>
- 作用域：类私有
- 初始值：`MutableStateFlow(SupplierEditorUiState())`
- 使用场景：内部可变的 UI 状态流
- 建议：无

##### uiState: StateFlow<SupplierEditorUiState>
- 作用域：类公开
- 初始值：`_uiState.asStateFlow()`
- 使用场景：对外暴露只读的 UI 状态流
- 建议：无

### 函数/方法

##### loadSupplier(id: Long)
- 参数：`id: Long` - 供应商 ID
- 返回值：无
- 实现逻辑：设置 isLoading 为 true，调用 `supplierRepository.getSupplier(id)`，成功时更新 existingId 和 draft
- 调用关系：调用了 `supplierRepository.getSupplier()`
- 建议：无

##### updateDraft(update: (SupplierDto) -> SupplierDto)
- 参数：`update: (SupplierDto) -> SupplierDto` - 草稿更新函数
- 返回值：无
- 实现逻辑：应用更新函数到当前 draft
- 调用关系：无
- 建议：无

##### saveSupplier()
- 参数：无
- 返回值：无
- 实现逻辑：校验 name 不为空，根据 existingId 是否为 null 决定调用 `updateSupplier` 或 `createSupplier`
- 调用关系：调用了 `supplierRepository.updateSupplier()` 或 `supplierRepository.createSupplier()`
- 建议：无

##### clearError()
- 参数：无
- 返回值：无
- 实现逻辑：将 error 设置为 null
- 调用关系：无
- 建议：无

---

## 6. SupplierEditorScreen

- **文件路径**: `feature/suppliers/src/main/java/com/zhihuiji/feature/suppliers/SupplierEditorScreen.kt`
- **父类/接口**: 无（Composable 函数）
- **注解**: `@Composable`
- **职责**: 供应商编辑页面的 UI 渲染
- **设计模式**: 声明式 UI

### 函数参数

##### supplierId: Long?
- 作用：要编辑的供应商 ID，null 表示新建
- 建议：无

##### onNavigateBack: () -> Unit
- 作用：返回上一页的回调
- 建议：无

##### viewModel: SupplierEditorViewModel = hiltViewModel()
- 作用：注入的 ViewModel 实例
- 建议：无

### 关键逻辑

##### LaunchedEffect(supplierId)
- 当 supplierId 存在且大于 0 时调用 `viewModel.loadSupplier(supplierId)`
- 建议：无

##### LaunchedEffect(uiState.saveSuccess)
- 当 saveSuccess 为 true 时调用 `onNavigateBack()`
- 建议：无

---

## 7. SupplierListUiState

- **文件路径**: `feature/suppliers/src/main/java/com/zhihuiji/feature/suppliers/SupplierViewModel.kt`
- **父类/接口**: 无（data class）
- **注解**: 无
- **职责**: 供应商列表页面的 UI 状态数据容器
- **设计模式**: MVI 状态模式

### 变量/属性

##### suppliers: List<SupplierDto> = emptyList()
- 作用域：类公开
- 初始值：空列表
- 使用场景：当前显示的供应商列表数据
- 建议：无

##### keyword: String = ""
- 作用域：类公开
- 初始值：空字符串
- 使用场景：当前搜索关键词
- 建议：无

##### statusFilter: Int? = null
- 作用域：类公开
- 初始值：null
- 使用场景：当前状态筛选条件，null 表示全部
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

## 8. SupplierViewModel

- **文件路径**: `feature/suppliers/src/main/java/com/zhihuiji/feature/suppliers/SupplierViewModel.kt`
- **父类/接口**: `ViewModel`
- **注解**: `@HiltViewModel`
- **职责**: 管理供应商列表页面的状态和业务逻辑
- **设计模式**: MVVM 模式

### 类属性

##### supplierRepository: SupplierRepository
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：调用供应商数据操作
- 建议：无

##### _uiState: MutableStateFlow<SupplierListUiState>
- 作用域：类私有
- 初始值：`MutableStateFlow(SupplierListUiState())`
- 使用场景：内部可变的 UI 状态流
- 建议：无

##### uiState: StateFlow<SupplierListUiState>
- 作用域：类公开
- 初始值：`_uiState.asStateFlow()`
- 使用场景：对外暴露只读的 UI 状态流
- 建议：无

### 函数/方法

##### init 块
- 参数：无
- 返回值：无
- 实现逻辑：初始化时调用 `loadSuppliers()`
- 调用关系：调用了 `loadSuppliers()`
- 建议：无

##### loadSuppliers(keyword: String, status: Int?)
- 参数：`keyword: String` - 搜索关键词（默认取当前状态值）；`status: Int?` - 状态筛选（默认取当前状态值）
- 返回值：无
- 实现逻辑：设置加载状态，刷新远程数据，观察本地数据流更新列表
- 调用关系：调用了 `supplierRepository.refreshSuppliers()`、`supplierRepository.observeSuppliers()`，被 init 和 `changeStatusFilter()` 调用
- 建议：`collect` 是终端操作符，在 viewModelScope 中使用 collect 会挂起协程直到流完成。如果多次调用 loadSuppliers，前一个 collect 仍在运行，可能导致状态覆盖。建议使用 `collectLatest` 替代

##### changeStatusFilter(status: Int?)
- 参数：`status: Int?` - 新的状态筛选值
- 返回值：无
- 实现逻辑：委托给 `loadSuppliers(status = status)`
- 调用关系：调用了 `loadSuppliers()`
- 建议：无

##### deleteSupplier(id: Long)
- 参数：`id: Long` - 供应商 ID
- 返回值：无
- 实现逻辑：删除供应商后重新加载列表
- 调用关系：调用了 `supplierRepository.deleteSupplier()`、`loadSuppliers()`
- 建议：当前 Screen 中未暴露删除操作入口，此方法暂未被调用

---

## 9. SupplierListScreen

- **文件路径**: `feature/suppliers/src/main/java/com/zhihuiji/feature/suppliers/SupplierListScreen.kt`
- **父类/接口**: 无（Composable 函数）
- **注解**: `@Composable`
- **职责**: 供应商列表页面的 UI 渲染
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
- 作用：导航到编辑页的回调，参数为供应商 ID（null 表示新建）
- 建议：无

##### onNavigateToDetail: (Long) -> Unit = {}
- 作用：导航到详情页的回调
- 建议：无

##### viewModel: SupplierViewModel = hiltViewModel()
- 作用：注入的 ViewModel 实例
- 建议：无

### 局部变量

##### uiState: SupplierListUiState
- 作用域：函数局部
- 初始值：通过 `viewModel.uiState.collectAsState()` 获取
- 使用场景：订阅 UI 状态
- 建议：无

##### listState: LazyListState
- 作用域：函数局部
- 初始值：`rememberLazyListState()`
- 使用场景：控制列表滚动
- 建议：无
