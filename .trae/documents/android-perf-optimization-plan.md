# Android 性能优化计划

## 摘要

在不改变任何 UI 布局与实现的前提下，对 `master-goods-android` 项目进行系统性性能优化。优化范围覆盖 Compose UI 层、数据层/网络层、ViewModel 状态管理、以及构建配置四个维度。

---

## 当前状态分析

### 1. Compose UI 层 — 存在重组性能隐患

| 问题 | 位置 | 影响 |
|------|------|------|
| `LazyVerticalGrid` 内嵌在 `LazyColumn` 中，且未设置固定高度约束 | [DashboardScreen.kt KpiGridSection](file:///Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/feature/dashboard/src/main/java/com/zhihuiji/feature/dashboard/DashboardScreen.kt#L173-L189) | 每次滚动父 LazyColumn 时，KpiGrid 都会重新测量，触发不必要的重组 |
| `statusChips` 在 `ProductListScreenContent` 中每次重组都重新创建 | [ProductListScreen.kt L70-L75](file:///Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/feature/products/src/main/java/com/zhihuiji/feature/products/ProductListScreen.kt#L70-L75) | 每次状态变化都创建新的 List 对象，导致 FilterChipRow 不必要的重组 |
| `KpiGridSection` 中 `kpiItems` 每次重组都重新创建 | [DashboardScreen.kt L138-L171](file:///Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/feature/dashboard/src/main/java/com/zhihuiji/feature/dashboard/DashboardScreen.kt#L138-L171) | 每次父组件重组都创建新的 List 和 KpiItem 对象 |
| `SaleOrderEditContent` 中 `totalAmount` 在 UI State 中以 getter 计算 | [SaleOrderEditViewModel.kt L38-L43](file:///Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/feature/sales/src/main/java/com/zhihuiji/feature/sales/SaleOrderEditViewModel.kt#L38-L43) | 每次读取 uiState 都重新遍历 items 计算总金额 |
| `AgentWorkbenchScreen` 中 `LazyColumn` 与 `InputBar` 共享同一层级 | [AgentWorkbenchScreen.kt L67-L153](file:///Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/feature/agent/src/main/java/com/zhihuiji/feature/agent/AgentWorkbenchScreen.kt#L67-L153) | InputBar 的输入变化会导致整个 LazyColumn 重组 |

### 2. 数据层/网络层 — 缺乏缓存与批处理

| 问题 | 位置 | 影响 |
|------|------|------|
| `fetchWeekSalesTrend()` 串行发起 7 次网络请求 | [DashboardViewModel.kt L110-L142](file:///Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/feature/dashboard/src/main/java/com/zhihuiji/feature/dashboard/DashboardViewModel.kt#L110-L142) | 首页加载时阻塞 7 次 RTT，冷启动慢 |
| `SimpleDateFormat` 每次调用都创建新实例 | [DashboardViewModel.kt L112](file:///Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/feature/dashboard/src/main/java/com/zhihuiji/feature/dashboard/DashboardViewModel.kt#L112) | 内存分配开销大，且 SimpleDateFormat 非线程安全 |
| Repository 层无内存缓存，每次请求都走网络 | [SaleOrderV2Repository.kt](file:///Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/data/order/src/main/java/com/zhihuiji/data/order/SaleOrderV2Repository.kt) | 重复页面切换时重复请求相同数据 |
| OkHttp 未配置连接池/缓存 | [ZhihuijiApi.kt](file:///Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/core/network/src/main/java/com/zhihuiji/core/network/ZhihuijiApi.kt) | 每次请求新建 TCP 连接，无 HTTP 缓存 |

### 3. ViewModel 状态管理 — 存在冗余更新

| 问题 | 位置 | 影响 |
|------|------|------|
| `updateInput()` 每次按键都触发整个 UI State 的复制 | [AgentViewModel.kt L105-L107](file:///Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/feature/agent/src/main/java/com/zhihuiji/feature/agent/AgentViewModel.kt#L105-L107) | 输入框高频更新导致频繁重组 |
| `SaleOrderEditViewModel` 每个字段更新都复制整个 items 列表 | [SaleOrderEditViewModel.kt L114-L139](file:///Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/feature/sales/src/main/java/com/zhihuiji/feature/sales/SaleOrderEditViewModel.kt#L114-L139) | 编辑商品明细时 O(n) 列表复制开销 |
| `loadDashboard()` 中 `customersDeferred` 仅用于计算应收款总额 | [DashboardViewModel.kt L65](file:///Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/feature/dashboard/src/main/java/com/zhihuiji/feature/dashboard/DashboardViewModel.kt#L65) | 拉取全部客户列表（可能上千条）仅为了 sumOf balance |

### 4. 构建配置 — 缺少性能相关配置

| 问题 | 位置 | 影响 |
|------|------|------|
| 无 Baseline Profile | 全局 | 用户首次启动/升级后无 AOT 编译优势 |
| 无 ProfileInstaller | [libs.versions.toml](file:///Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/gradle/libs.versions.toml) | 无法通过 Play Store 分发 baseline profile |
| `compileSdk = 35` 但未启用 R8 全模式 | [app/build.gradle.kts](file:///Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/app/build.gradle.kts) | release 包体积未充分优化 |
| 未启用 Kotlin 编译器非确定性模式 | [app/build.gradle.kts](file:///Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/app/build.gradle.kts) | CI 构建缓存命中率低 |

---

## 优化方案

### 阶段一：Compose 重组优化（零 UI 变更）

#### 1.1 DashboardScreen — KpiGridSection 提取为独立 Composable + remember

**文件**: [DashboardScreen.kt](file:///Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/feature/dashboard/src/main/java/com/zhihuiji/feature/dashboard/DashboardScreen.kt)

**修改内容**:
- 将 `kpiItems` 的创建包裹在 `remember(uiState)` 中
- `KpiGridSection` 接收 `DashboardUiState` 而非展开字段，减少参数传递
- `LazyVerticalGrid` 的 `modifier.height(320.dp)` 保持不变

**预期收益**: 父 LazyColumn 滚动时 KpiGrid 不再重组

#### 1.2 ProductListScreen — statusChips 使用 remember

**文件**: [ProductListScreen.kt](file:///Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/feature/products/src/main/java/com/zhihuiji/feature/products/ProductListScreen.kt)

**修改内容**:
- `statusChips` 使用 `remember { listOf(...) }` 缓存
- 当筛选状态变化时通过 `derivedStateOf` 计算选中状态

**预期收益**: 搜索/滚动时 FilterChipRow 不再不必要重组

#### 1.3 SaleOrderEditScreen — totalAmount 使用 derivedStateOf

**文件**: [SaleOrderEditScreen.kt](file:///Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/feature/sales/src/main/java/com/zhihuiji/feature/sales/SaleOrderEditScreen.kt)

**修改内容**:
- 在 `SaleOrderEditContent` 中使用 `val totalAmount by remember(uiState.items, uiState.discountAmount) { derivedStateOf { ... } }`
- 移除 ViewModel 中 `totalAmount` 的 getter 计算属性

**预期收益**: 只有 items 或 discount 变化时才重新计算，其他字段变化不触发

#### 1.4 AgentWorkbenchScreen — InputBar 状态隔离

**文件**: [AgentWorkbenchScreen.kt](file:///Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/feature/agent/src/main/java/com/zhihuiji/feature/agent/AgentWorkbenchScreen.kt)

**修改内容**:
- 将 `inputText` 从 ViewModel State 中提取为局部 `remember { mutableStateOf("") }`
- 只有点击发送时才将文本传入 ViewModel
- `InputBar` 提取为顶层 Composable，接收 `String` 和 `onValueChange`

**预期收益**: 输入框打字不再触发整个屏幕重组

---

### 阶段二：数据层/网络层优化

#### 2.1 DashboardViewModel — 周销售趋势并行请求 + 缓存

**文件**: [DashboardViewModel.kt](file:///Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/feature/dashboard/src/main/java/com/zhihuiji/feature/dashboard/DashboardViewModel.kt)

**修改内容**:
- `fetchWeekSalesTrend()` 改为 `List<Deferred>` 并行发起 7 个请求
- 添加内存缓存 `weekTrendCache: Map<LocalDate, List<SalesTrendPoint>>`
- 同一天内重复进入 Dashboard 直接返回缓存

**预期收益**: 首页加载从 ~7 RTT 降至 ~1 RTT

#### 2.2 DashboardViewModel — SimpleDateFormat 静态化

**文件**: [DashboardViewModel.kt](file:///Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/feature/dashboard/src/main/java/com/zhihuiji/feature/dashboard/DashboardViewModel.kt)

**修改内容**:
- 将 `SimpleDateFormat` 提升为 `private val` 伴生对象成员
- 或使用 `java.time.format.DateTimeFormatter`（API 26+，minSdk=26 满足）

**预期收益**: 减少内存分配，DateTimeFormatter 线程安全

#### 2.3 OkHttp 配置连接池与缓存

**文件**: [core/network 模块 NetworkModule](file:///Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/core/network/src/main/java/com/zhihuiji/core/network)

**修改内容**:
- 配置 `ConnectionPool(maxIdleConnections = 5, keepAliveDuration = 5min)`
- 配置 `Cache(cacheDir, maxSize = 50MB)`
- 添加 `addNetworkInterceptor(HttpLoggingInterceptor)`（仅 debug）

**预期收益**: 复用 TCP 连接，减少握手延迟；离线时可用缓存数据

#### 2.4 Repository 层添加内存缓存

**文件**: 各 Repository 文件

**修改内容**:
- 使用 `MutableMap<String, CacheEntry<T>>` 作为简单内存缓存
- 列表数据缓存 TTL = 60s，详情数据 TTL = 300s
- 写操作后主动失效相关缓存

**预期收益**: 页面间切换不再重复请求

---

### 阶段三：ViewModel 状态优化

#### 3.1 AgentViewModel — inputText 移出 StateFlow

**文件**: [AgentViewModel.kt](file:///Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/feature/agent/src/main/java/com/zhihuiji/feature/agent/AgentViewModel.kt)

**修改内容**:
- 移除 `inputText` 字段
- 提供 `sendQuestion(question: String)` 方法替代 `updateInput + askQuestion`

**预期收益**: 输入框高频更新与 UI State 解耦

#### 3.2 SaleOrderEditViewModel — 使用 immutable list 优化

**文件**: [SaleOrderEditViewModel.kt](file:///Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/feature/sales/src/main/java/com/zhihuiji/feature/sales/SaleOrderEditViewModel.kt)

**修改内容**:
- 使用 `SnapshotStateList<EditItem>` 替代 `List<EditItem>` + 全量复制
- 或保持当前方式但使用 `PersistentList`（kotlinx.collections.immutable）

**预期收益**: 编辑明细时从 O(n) 降至 O(1)

#### 3.3 DashboardViewModel — 应收款独立接口

**文件**: [DashboardViewModel.kt](file:///Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/feature/dashboard/src/main/java/com/zhihuiji/feature/dashboard/DashboardViewModel.kt)

**修改内容**:
- 将 `customerRepository.listCustomers()` 替换为专用聚合接口（如 `/api/v2/dashboard/summary`）
- 或本地缓存客户数据，仅首次加载全量

**预期收益**: 避免拉取全量客户列表仅为了 sumOf

---

### 阶段四：构建优化

#### 4.1 添加 Baseline Profile

**文件**: 新增 `:baselineprofile` 模块

**修改内容**:
- 创建 `baselineprofile` 模块
- 添加 `BaselineProfileRule` 测试，覆盖核心用户路径：首页 → 商品列表 → 商品详情 → 销售开单
- app 模块添加 `implementation(libs.profileinstaller)`

**预期收益**: 启动时间减少 15-30%

#### 4.2 启用 Kotlin 编译器缓存

**文件**: [app/build.gradle.kts](file:///Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/app/build.gradle.kts)

**修改内容**:
- `kotlinOptions { freeCompilerArgs += "-Xassertions=jar-descriptor" }`（如适用）
- 在 `gradle.properties` 中确保 `kotlin.incremental=true`

**预期收益**: CI 增量构建提速

#### 4.3 启用 R8 全模式

**文件**: [app/build.gradle.kts](file:///Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/app/build.gradle.kts)

**修改内容**:
- `buildTypes.release { isMinifyEnabled = true; isShrinkResources = true }`（已启用，确认 ProGuard 规则充分）
- 补充 `-keepattributes LineNumberTable,SourceFile` 用于崩溃分析

**预期收益**: APK 体积优化（已部分实现）

---

## 实施顺序

1. **阶段一**（Compose 优化）— 风险最低，收益最直接
2. **阶段三**（ViewModel 优化）— 与阶段一配合效果最佳
3. **阶段二**（数据层优化）— 需要测试网络缓存行为
4. **阶段四**（构建优化）— 最后进行，需验证 Baseline Profile 生成

---

## 验证步骤

1. 使用 Compose Compiler Metrics 生成重组报告：
   ```bash
   ./gradlew :app:assembleDebug -Pandroidx.enableComposeCompilerMetrics=true
   ```
2. 使用 Android Studio Profiler 对比优化前后的 CPU/Memory 占用
3. 使用 Macrobenchmark 测量启动时间和帧率
4. 确保 `./gradlew :app:assembleRelease` 成功且 APK 体积未异常增长

---

## 假设与决策

- **假设**: 后端 API 响应时间平均 200ms，7 次串行请求 ≈ 1.4s
- **决策**: 优先并行化 Dashboard 周趋势请求，而非修改后端接口
- **决策**: `SnapshotStateList` 不引入，因为当前项目未依赖 compose-runtime 显式使用；改用 `derivedStateOf` 和 `remember` 即可满足大部分场景
- **决策**: Baseline Profile 使用 Macrobenchmark 1.2.x 生成，与当前 AGP 8.5.2 兼容
