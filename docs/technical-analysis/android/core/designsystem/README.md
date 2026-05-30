# designsystem 技术分析

## 文件清单
- BottomActionBar.kt
- BottomBarBehavior.kt
- ChartCard.kt
- DampedSegmentedDragState.kt
- EmptyState.kt
- FilterChipRow.kt
- FloatingLiquidBottomBar.kt
- GlassBackground.kt
- GlassCard.kt
- GlassScaffold.kt
- GlassTopBar.kt
- KpiCard.kt
- LiquidGlassSurface.kt
- LiquidSegmentedControl.kt
- PrimaryGradientButton.kt
- QuantityStepper.kt
- SearchFilterBar.kt
- SegmentedTabs.kt
- StatusPill.kt
- ZhihuijiColors.kt
- ZhihuijiShapes.kt
- ZhihuijiTheme.kt
- ZhihuijiTypography.kt

---

## BottomActionBar.kt

### BottomActionBar — @Composable
- 职责：底部操作栏组件，包含主操作按钮和可选的次要操作按钮 / 设计模式：组合组件

#### BottomActionBar(primaryAction, modifier, secondaryActions)
- 参数：`primaryAction: @Composable () -> Unit` — 主操作按钮内容；`modifier: Modifier = Modifier` — 修饰符；`secondaryActions: List<@Composable () -> Unit> = emptyList()` — 次要操作按钮列表
- 返回值：无（Composable 函数）
- 实现逻辑：使用 LiquidGlassSurface 作为背景容器，内部 Column + Row 布局，次要操作在左，主操作在右（weight(1f) + Arrangement.End）
- 调用关系：被详情页底部操作栏使用
- 建议：secondaryActions 之间没有间距控制，建议添加 spacedBy

---

## BottomBarBehavior.kt

### LocalSetBottomBarVisible — CompositionLocal
- 类型：`compositionLocalOf<(Boolean) -> Unit>` / 职责：提供底部栏可见性控制回调
- 初始值：error("No bottom bar visibility controller provided")
- 建议：无

### LocalBottomBarVisible — CompositionLocal
- 类型：`compositionLocalOf { true }` / 职责：提供底部栏当前可见状态
- 初始值：true
- 建议：无

### BottomBarScrollVisibilityEffect(LazyListState) — @Composable
- 职责：根据 LazyList 滚动方向自动显示/隐藏底部栏

#### BottomBarScrollVisibilityEffect(listState, hideThresholdPx)
- 参数：`listState: LazyListState` — 列表滚动状态；`hideThresholdPx: Int = 8` — 触发隐藏的滚动阈值（像素）
- 返回值：无（Composable 函数）
- 实现逻辑：通过 LaunchedEffect + snapshotFlow 监听 firstVisibleItemIndex 和 firstVisibleItemScrollOffset 变化，向上滚动隐藏，向下滚动显示，回到顶部显示
- 调用关系：被列表页面使用
- 建议：无

### BottomBarScrollVisibilityEffect(ScrollState) — @Composable
- 职责：根据普通 ScrollState 滚动方向自动显示/隐藏底部栏

#### BottomBarScrollVisibilityEffect(scrollState, hideThresholdPx)
- 参数：`scrollState: ScrollState` — 滚动状态；`hideThresholdPx: Int = 8` — 触发隐藏的滚动阈值
- 返回值：无（Composable 函数）
- 实现逻辑：通过 LaunchedEffect + snapshotFlow 监听 scrollState.value 变化，逻辑与 LazyList 版本类似
- 调用关系：被非 Lazy 列表页面使用
- 建议：无

### BottomBarScrollToTopEffect(LazyListState) — @Composable
- 职责：响应信号将 LazyList 滚动到顶部

#### BottomBarScrollToTopEffect(signal, listState)
- 参数：`signal: Int` — 触发信号（大于 0 时执行）；`listState: LazyListState` — 列表状态
- 返回值：无（Composable 函数）
- 实现逻辑：LaunchedEffect 监听 signal 变化，signal > 0 时调用 animateScrollToItem(0)
- 调用关系：被底部栏 Tab 点击回到顶部时使用
- 建议：无

### BottomBarScrollToTopEffect(ScrollState) — @Composable
- 职责：响应信号将 ScrollState 滚动到顶部

#### BottomBarScrollToTopEffect(signal, scrollState)
- 参数：`signal: Int` — 触发信号；`scrollState: ScrollState` — 滚动状态
- 返回值：无（Composable 函数）
- 实现逻辑：LaunchedEffect 监听 signal 变化，signal > 0 时调用 animateScrollTo(0)
- 调用关系：被非 Lazy 列表页面使用
- 建议：无

---

## ChartCard.kt

### ChartCard — @Composable
- 职责：图表卡片容器，带标题和内容区域

#### ChartCard(title, modifier, content)
- 参数：`title: String` — 图表标题；`modifier: Modifier = Modifier`；`content: @Composable () -> Unit` — 图表内容
- 返回值：无（Composable 函数）
- 实现逻辑：使用 GlassCard 包裹，Column 布局，顶部标题 + 8dp 间距 + 内容
- 调用关系：被报表页面图表展示使用
- 建议：无

### LineTrendChart — @Composable
- 职责：折线趋势图组件

#### LineTrendChart(values, labels, modifier)
- 参数：`values: List<Double>` — 数据值列表；`labels: List<String>` — X 轴标签列表；`modifier: Modifier = Modifier`
- 返回值：无（Composable 函数）
- 实现逻辑：Canvas 绘制：4 条水平网格线 → 计算数据点坐标 → 绘制面积填充（Primary 10% 透明度）→ 绘制连线（3dp 圆角）→ 绘制数据点（白底+Primary 圆点）→ 底部标签 Row
- 调用关系：被报表页面趋势图展示使用
- 建议：values 和 labels 数量不一致时未做防御处理，建议添加校验

### RingMetricChart — @Composable
- 职责：环形指标图组件

#### RingMetricChart(primaryValue, secondaryValue, centerText, primaryLabel, secondaryLabel, modifier)
- 参数：`primaryValue: Double` — 主值；`secondaryValue: Double` — 次值；`centerText: String` — 中心文本；`primaryLabel: String` — 主图例标签；`secondaryLabel: String` — 次图例标签；`modifier: Modifier = Modifier`
- 返回值：无（Composable 函数）
- 实现逻辑：Row 布局，左侧 72dp Box 内 Canvas 绘制三段弧（背景、Primary、Warning），中心显示文本；右侧 Column 显示图例
- 调用关系：被报表页面环形图展示使用
- 建议：secondaryValue 弧线与 primaryValue 弧线之间有 8f 间隔，但未处理两值之和为 0 的情况

### HorizontalBarChart — @Composable
- 职责：水平条形图组件

#### HorizontalBarChart(items, modifier)
- 参数：`items: List<Pair<String, Double>>` — 标签-值对列表；`modifier: Modifier = Modifier`
- 返回值：无（Composable 函数）
- 实现逻辑：取前 5 项，每项绘制标签行 + 进度条，第一项使用 Primary 全色，其余 62% 透明度
- 调用关系：被报表页面条形图展示使用
- 建议：硬编码 take(5)，建议作为参数暴露

### LegendRow — @Composable (private)
- 职责：图例行组件

#### LegendRow(color, label)
- 参数：`color: Color` — 图例颜色；`label: String` — 图例文本
- 返回值：无（Composable 函数）
- 实现逻辑：9dp 圆形色块 + 8dp 间距 + 文本
- 调用关系：被 RingMetricChart 内部调用
- 建议：无

### formatChartAmount() — Double 扩展函数 (private)
- 职责：格式化图表金额

#### Double.formatChartAmount(): String
- 参数：无（接收者为 Double）
- 返回值：`String` — 格式化后的金额字符串
- 实现逻辑：>= 10000 显示 "万"，>= 1000 显示 "k"，否则保留两位小数
- 调用关系：被 HorizontalBarChart 内部调用
- 建议：无

---

## DampedSegmentedDragState.kt

### DampedSegmentedDragState
- class / 注解：@Stable / 职责：管理分段控件的阻尼拖拽状态，包括指示器位置动画、按压脉冲动画、拖拽惯性计算 / 设计模式：状态管理模式

#### DampedSegmentedDragState(initialIndex, itemCount, coroutineScope)
- 参数：`initialIndex: Int` — 初始选中索引；`itemCount: Int` — 项目数量；`coroutineScope: CoroutineScope` — 协程作用域
- 返回值：无（internal 构造函数）
- 实现逻辑：初始化 Animatable 和状态变量
- 调用关系：由 rememberDampedSegmentedDragState 创建
- 建议：无

#### indicator: Animatable\<Float\>
- 作用域：private / 初始值：Animatable(initialIndex.toFloat()) / 使用场景：指示器位置动画
- 建议：无

#### pressPulse: Animatable\<Float\>
- 作用域：private / 初始值：Animatable(0f) / 使用场景：按压脉冲动画
- 建议：无

#### indicatorJob: Job?
- 作用域：private / 使用场景：管理指示器动画协程
- 建议：无

#### pulseJob: Job?
- 作用域：private / 使用场景：管理脉冲动画协程
- 建议：无

#### selectedIndex: Int
- 作用域：public (private set) / 使用场景：当前选中索引
- 建议：无

#### isDragging: Boolean
- 作用域：public (private set) / 使用场景：是否正在拖拽
- 建议：无

#### dragVelocityItemsPerSecond: Float
- 作用域：public (private set) / 使用场景：拖拽速度（项/秒）
- 建议：无

#### value: Float
- 作用域：public val / 使用场景：当前指示器浮点位置，用于动画
- 建议：无

#### pressProgress: Float
- 作用域：public val / 使用场景：当前按压脉冲进度 0~1
- 建议：无

#### syncSelection(index: Int)
- 参数：`index: Int` — 目标选中索引
- 返回值：无
- 实现逻辑：若 index 未变且未拖拽则跳过；更新 selectedIndex；若未拖拽则用弹簧动画将 indicator 移动到目标位置
- 调用关系：被 LiquidSegmentedControl 的 LaunchedEffect 调用
- 建议：无

#### setPressed(pressed: Boolean)
- 参数：`pressed: Boolean` — 是否按下
- 返回值：无
- 实现逻辑：用弹簧动画将 pressPulse 动画到 1f（按下/拖拽中）或 0f（释放）
- 调用关系：被手势处理调用
- 建议：无

#### onTap(targetIndex: Int, onSelected: (Int) -> Unit)
- 参数：`targetIndex: Int` — 点击目标索引；`onSelected: (Int) -> Unit` — 选中回调
- 返回值：无
- 实现逻辑：更新 selectedIndex → 先脉冲到 1f → 指示器弹簧动画到目标 → 脉冲回到 0f → 回调 onSelected
- 调用关系：被 BottomBar 模式点击事件调用
- 建议：onSelected 在动画完成前就回调，可能导致 UI 状态不一致

#### onDrag(deltaX: Float, itemWidthPx: Float)
- 参数：`deltaX: Float` — 水平拖拽增量（像素）；`itemWidthPx: Float` — 单项宽度（像素）
- 返回值：无
- 实现逻辑：计算 overscroll 阻尼系数（边界 0.28，正常 0.96），将像素增量转换为项数增量，snapTo 更新指示器位置，允许 ±0.35 项的越界
- 调用关系：被手势拖拽事件调用
- 建议：无

#### onDragEnd(velocityPxPerSecond: Float, itemWidthPx: Float, onSelected: (Int) -> Unit)
- 参数：`velocityPxPerSecond: Float` — 拖拽释放速度；`itemWidthPx: Float` — 单项宽度；`onSelected: (Int) -> Unit` — 选中回调
- 返回值：无
- 实现逻辑：基于速度计算目标索引（0.16s 投影），限制最大步进 1 项，弹簧动画到目标位置，脉冲回到 0f
- 调用关系：被手势拖拽结束事件调用
- 建议：0.16f 是经验值，可考虑提取为可配置参数

### rememberDampedSegmentedDragState — @Composable
- 职责：创建并记住 DampedSegmentedDragState 实例

#### rememberDampedSegmentedDragState(selectedIndex, itemCount)
- 参数：`selectedIndex: Int` — 当前选中索引；`itemCount: Int` — 项目数量
- 返回值：`DampedSegmentedDragState`
- 实现逻辑：remember + rememberCoroutineScope 创建状态实例，key 为 itemCount 和 coroutineScope
- 调用关系：被 LiquidSegmentedControl 调用
- 建议：selectedIndex 变化不会重建状态，这是正确的（通过 syncSelection 同步）

---

## EmptyState.kt

### EmptyState — @Composable
- 职责：空状态占位组件

#### EmptyState(icon, title, modifier, subtitle)
- 参数：`icon: ImageVector` — 空状态图标；`title: String` — 主标题；`modifier: Modifier = Modifier`；`subtitle: String? = null` — 副标题
- 返回值：无（Composable 函数）
- 实现逻辑：Column 居中布局，48dp 图标 + 8dp 间距 + 标题 + 可选副标题
- 调用关系：被列表为空时展示
- 建议：无

---

## FilterChipRow.kt

### FilterChipRow — @Composable
- 职责：水平可滚动的筛选标签行

#### FilterChipRow(chips, selectedIndex, onChipSelected, modifier)
- 参数：`chips: List<String>` — 标签文本列表；`selectedIndex: Int` — 当前选中索引；`onChipSelected: (Int) -> Unit` — 选中回调；`modifier: Modifier = Modifier`
- 返回值：无（Composable 函数）
- 实现逻辑：水平可滚动 Row，每个 chip 根据选中状态显示不同背景色和文字颜色，点击触发回调
- 调用关系：被筛选功能页面使用
- 建议：点击 chip 未调用 onChipSelected，存在 Bug — 缺少 `clickable` 修饰符和点击回调

---

## FloatingLiquidBottomBar.kt

### FloatingLiquidBottomBar — @Composable
- 职责：浮动液态玻璃底部导航栏

#### FloatingLiquidBottomBar(selectedDestination, destinations, onNavigate, modifier)
- 参数：`selectedDestination: String` — 当前选中路由；`destinations: List<BottomBarDestination>` — 导航目标列表；`onNavigate: (String) -> Unit` — 导航回调；`modifier: Modifier = Modifier`
- 返回值：无（Composable 函数）
- 实现逻辑：若 destinations 为空则返回；委托给 LiquidSegmentedControl（BottomBar 样式）
- 调用关系：被 GlassScaffold 底部栏使用
- 建议：无

---

## GlassBackground.kt

### glassBackground() — Modifier 扩展函数
- 职责：为组件添加玻璃态背景效果

#### Modifier.glassBackground(): Modifier
- 参数：无（接收者为 Modifier）
- 返回值：`Modifier` — 添加了渐变背景和装饰性圆形光斑的 Modifier
- 实现逻辑：先添加三色垂直渐变背景，再 drawBehind 绘制三个半透明圆形光斑（蓝色系），模拟玻璃质感
- 调用关系：被 GlassScaffold 的内容区域使用
- 建议：光斑颜色和位置硬编码，建议抽取为主题变量以支持主题切换

---

## GlassCard.kt

### GlassCard（无点击） — @Composable
- 职责：玻璃态卡片容器（纯展示）

#### GlassCard(modifier, content)
- 参数：`modifier: Modifier = Modifier`；`content: @Composable ColumnScope.() -> Unit` — 卡片内容
- 返回值：无（Composable 函数）
- 实现逻辑：使用 LiquidGlassSurface（cornerRadius=22dp, blurRadius=20dp, surfaceAlpha=0.14f），内部 Column fillMaxWidth
- 调用关系：被各页面卡片展示使用
- 建议：无

### GlassCard（可点击） — @Composable
- 职责：可点击的玻璃态卡片容器

#### GlassCard(modifier, onClick, content)
- 参数：`modifier: Modifier = Modifier`；`onClick: () -> Unit` — 点击回调；`content: @Composable ColumnScope.() -> Unit`
- 返回值：无（Composable 函数）
- 实现逻辑：与无点击版本类似，额外传入 onClick 给 LiquidGlassSurface
- 调用关系：被可交互的卡片使用
- 建议：无

---

## GlassScaffold.kt

### BottomBarDestination
- data class / 职责：底部导航栏目标定义

#### BottomBarDestination(route, label, icon, selectedIcon)
- 参数：`route: String` — 路由路径；`label: String` — 显示标签；`icon: ImageVector` — 未选中图标；`selectedIcon: ImageVector` — 选中图标
- 建议：无

### GlassScaffold — @Composable
- 职责：应用主脚手架，包含顶部区域、底部导航栏、内容区域 / 设计模式：Scaffold 模式

#### GlassScaffold(title, selectedDestination, destinations, onNavigate, showBottomBar, isBottomBarVisible, setBottomBarVisible, topBarActions, content)
- 参数：`title: String = ""` — 标题；`selectedDestination: String` — 当前选中路由；`destinations: List<BottomBarDestination>` — 导航目标列表；`onNavigate: (String) -> Unit` — 导航回调；`showBottomBar: Boolean = true` — 是否显示底部栏；`isBottomBarVisible: Boolean = true` — 底部栏当前可见性；`setBottomBarVisible: (Boolean) -> Unit = {}` — 底部栏可见性控制回调；`topBarActions: @Composable () -> Unit = {}` — 顶部栏操作区；`content: @Composable (PaddingValues) -> Unit` — 页面内容
- 返回值：无（Composable 函数）
- 实现逻辑：CompositionLocalProvider 注入底部栏可见性状态 → Scaffold（containerColor 为渐变终止色）→ 底部栏 AnimatedVisibility + FloatingLiquidBottomBar → 内容区域 Box 添加 glassBackground + padding
- 调用关系：被各主页面使用
- 建议：content 传入 PaddingValues() 而非 Scaffold 的 paddingValues，可能导致内容被底部栏遮挡

---

## GlassTopBar.kt

### GlassTopBar — @Composable
- 职责：玻璃态顶部栏组件

#### GlassTopBar(title, modifier, navigationIcon, onNavigationClick, actions)
- 参数：`title: String` — 标题；`modifier: Modifier = Modifier`；`navigationIcon: ImageVector? = null` — 导航图标；`onNavigationClick: (() -> Unit)? = null` — 导航点击回调；`actions: @Composable () -> Unit = {}` — 操作区
- 返回值：无（Composable 函数）
- 实现逻辑：LiquidGlassSurface 包裹 TopAppBar，透明背景色，56dp 高度
- 调用关系：被详情页顶部栏使用
- 建议：使用 ExperimentalMaterial3Api，需关注 API 稳定性

---

## KpiCard.kt

### KpiTone — 枚举
- 职责：KPI 卡片色调类型

#### PRIMARY
- 使用场景：主色调（蓝色）
- 建议：无

#### SUCCESS
- 使用场景：成功色调（绿色）
- 建议：无

#### WARNING
- 使用场景：警告色调（橙色）
- 建议：无

#### DANGER
- 使用场景：危险色调（红色）
- 建议：无

### KpiCard — @Composable
- 职责：KPI 指标卡片组件

#### KpiCard(title, value, modifier, subtitle, trend, icon, tone)
- 参数：`title: String` — 指标标题；`value: String` — 指标值；`modifier: Modifier = Modifier`；`subtitle: String? = null` — 副标题；`trend: String? = null` — 趋势文本；`icon: ImageVector? = null` — 图标；`tone: KpiTone = KpiTone.PRIMARY` — 色调
- 返回值：无（Composable 函数）
- 实现逻辑：GlassCard 内 Row 布局，左侧 Column 显示标题+值+副标题+趋势，右侧可选图标（圆形背景+图标），色调决定值和图标颜色，趋势含"+"或"↑"为绿色否则红色
- 调用关系：被首页/报表页 KPI 展示使用
- 建议：趋势颜色判断基于字符串 contains，不够健壮，建议使用枚举或布尔标志

---

## LiquidGlassSurface.kt

### LiquidGlassSurface — @Composable
- 职责：液态玻璃效果表面组件，是整个设计系统的核心基础组件 / 设计模式：基础组件

#### LiquidGlassSurface(modifier, cornerRadius, blurRadius, surfaceAlpha, lensProgress, highlightAlpha, contentPadding, onClick, content)
- 参数：`modifier: Modifier = Modifier`；`cornerRadius: Dp = 22.dp` — 圆角半径；`blurRadius: Dp = 20.dp` — 模糊半径；`surfaceAlpha: Float = 0.16f` — 表面透明度；`lensProgress: Float = 0f` — 透镜效果进度（0~1）；`highlightAlpha: Float = 0.92f` — 高光透明度；`contentPadding: PaddingValues = PaddingValues(0.dp)` — 内容内边距；`onClick: (() -> Unit)? = null` — 点击回调；`content: @Composable BoxScope.() -> Unit` — 内容
- 返回值：无（Composable 函数）
- 实现逻辑：使用 kyant/backdrop 库的 rememberLayerBackdrop + drawBackdrop 实现：blur 模糊 + vibrancy 增强 + 可选 lens 折射效果 + Highlight 高光 + Shadow 外阴影 + InnerShadow 内阴影 + drawRect 白色半透明表面
- 调用关系：被 GlassCard、GlassTopBar、BottomActionBar、SearchFilterBar、LiquidSegmentedControl 等组件使用
- 建议：这是核心组件，性能敏感区域，建议对 blur 和 lens 效果做性能测试

---

## LiquidSegmentedControl.kt

### LiquidSegmentedItem\<T\>
- data class / 职责：分段控件项数据

#### LiquidSegmentedItem(key, label, icon, selectedIcon)
- 参数：`key: T` — 唯一标识；`label: String` — 显示标签；`icon: ImageVector? = null` — 未选中图标；`selectedIcon: ImageVector? = null` — 选中图标
- 建议：无

### LiquidSegmentedStyle — 枚举
- 职责：分段控件样式

#### TextOnly
- 使用场景：纯文本标签样式（小尺寸）

#### BottomBar
- 使用场景：底部导航栏样式（大尺寸，支持拖拽）

### LiquidSegmentedControl — @Composable
- 职责：液态分段控件，支持拖拽和点击切换 / 设计模式：组合组件

#### LiquidSegmentedControl(items, selectedKey, onItemSelected, modifier, style, height, cornerRadius, indicatorCornerRadius, contentPadding, surfaceAlpha, indicatorSurfaceAlpha)
- 参数：`items: List<LiquidSegmentedItem<T>>` — 项目列表；`selectedKey: T` — 选中项 key；`onItemSelected: (T) -> Unit` — 选中回调；`modifier: Modifier = Modifier`；`style: LiquidSegmentedStyle = LiquidSegmentedStyle.TextOnly` — 样式；`height: Dp` — 高度（BottomBar 58dp, TextOnly 36dp）；`cornerRadius: Dp` — 外框圆角；`indicatorCornerRadius: Dp` — 指示器圆角；`contentPadding: PaddingValues` — 内容内边距；`surfaceAlpha: Float` — 表面透明度；`indicatorSurfaceAlpha: Float` — 指示器透明度
- 返回值：无（Composable 函数）
- 实现逻辑：外层 LiquidGlassSurface → BoxWithConstraints 计算项宽 → 指示器 LiquidGlassSurface（带 lens 效果）→ Row 放置各 ItemCell → BottomBar 样式添加 pointerInput 手势处理（拖拽+惯性）
- 调用关系：被 FloatingLiquidBottomBar 和 SegmentedTabs 使用
- 建议：泛型 T 在 BottomBar 样式中实际为 String（route），可简化

### LiquidSegmentedItemCell — @Composable (private)
- 职责：分段控件单个单元格

#### LiquidSegmentedItemCell(modifier, item, selected, emphasis, style, onClick)
- 参数：`modifier: Modifier`；`item: LiquidSegmentedItem<T>` — 数据项；`selected: Boolean` — 是否选中；`emphasis: Float` — 强调度 0~1；`style: LiquidSegmentedStyle` — 样式；`onClick: () -> Unit` — 点击回调
- 返回值：无（Composable 函数）
- 实现逻辑：根据 style 区分 BottomBar（Column 布局：图标+标签）和 TextOnly（Box 布局：Row 图标+标签），emphasis 控制图标缩放和文字透明度动画
- 调用关系：被 LiquidSegmentedControl 内部调用
- 建议：无

---

## PrimaryGradientButton.kt

### PrimaryGradientButton — @Composable
- 职责：主操作按钮组件

#### PrimaryGradientButton(text, onClick, modifier, icon, enabled)
- 参数：`text: String` — 按钮文本；`onClick: () -> Unit` — 点击回调；`modifier: Modifier = Modifier`；`icon: ImageVector? = null` — 可选图标；`enabled: Boolean = true` — 是否启用
- 返回值：无（Composable 函数）
- 实现逻辑：Material3 Button，Primary 色，44dp 高，10dp 圆角，禁用时 50% 透明度
- 调用关系：被表单提交等主操作场景使用
- 建议：名称含 "Gradient" 但实际未使用渐变效果，建议更名或添加渐变

### SecondaryOutlineButton — @Composable
- 职责：次要描边按钮组件

#### SecondaryOutlineButton(text, onClick, modifier, enabled)
- 参数：`text: String`；`onClick: () -> Unit`；`modifier: Modifier = Modifier`；`enabled: Boolean = true`
- 返回值：无（Composable 函数）
- 实现逻辑：Material3 outlinedButton，透明背景，BorderLight 描边 0.8dp
- 调用关系：被次要操作场景使用
- 建议：无

### DangerOutlineButton — @Composable
- 职责：危险操作描边按钮组件

#### DangerOutlineButton(text, onClick, modifier)
- 参数：`text: String`；`onClick: () -> Unit`；`modifier: Modifier = Modifier`
- 返回值：无（Composable 函数）
- 实现逻辑：Material3 outlinedButton，Danger 色描边和文字
- 调用关系：被删除等危险操作场景使用
- 建议：缺少 enabled 参数，与 SecondaryOutlineButton API 不一致

---

## QuantityStepper.kt

### QuantityStepper — @Composable
- 职责：数量步进器组件

#### QuantityStepper(value, onValueChange, onMinus, onPlus, minusIcon, plusIcon, modifier, min, max)
- 参数：`value: Double` — 当前值；`onValueChange: (Double) -> Unit` — 值变化回调；`onMinus: () -> Unit` — 减少按钮回调；`onPlus: () -> Unit` — 增加按钮回调；`minusIcon: ImageVector` — 减少图标；`plusIcon: ImageVector` — 增加图标；`modifier: Modifier = Modifier`；`min: Double = 0.0` — 最小值；`max: Double = Double.MAX_VALUE` — 最大值
- 返回值：无（Composable 函数）
- 实现逻辑：Row 布局，左侧 FilledIconButton（SurfaceVariant 色）减少 → 中间 OutlinedTextField 输入 → 右侧 FilledIconButton（Primary 色）增加，输入值 coerceIn(min, max)
- 调用关系：被订单编辑页面数量调整使用
- 建议：onMinus 和 onPlus 回调与 onValueChange 存在职责重叠，建议统一为一种回调机制

---

## SearchFilterBar.kt

### SearchFilterBar — @Composable
- 职责：搜索筛选栏组件

#### SearchFilterBar(query, onQueryChange, placeholder, modifier, showFilter, onFilterClick, filterIcon)
- 参数：`query: String` — 当前搜索文本；`onQueryChange: (String) -> Unit` — 搜索文本变化回调；`placeholder: String = "搜索"` — 占位文本；`modifier: Modifier = Modifier`；`showFilter: Boolean = true` — 是否显示筛选按钮；`onFilterClick: (() -> Unit)? = null` — 筛选点击回调；`filterIcon: ImageVector? = null` — 筛选图标
- 返回值：无（Composable 函数）
- 实现逻辑：LiquidGlassSurface 包裹 Row，左侧 BasicTextField（带 placeholder），右侧可选筛选 IconButton
- 调用关系：被列表页面搜索栏使用
- 建议：无

---

## SegmentedTabs.kt

### SegmentedTabs — @Composable
- 职责：分段标签页组件（纯文本样式）

#### SegmentedTabs(tabs, selectedIndex, onTabSelected, modifier)
- 参数：`tabs: List<String>` — 标签文本列表；`selectedIndex: Int` — 当前选中索引；`onTabSelected: (Int) -> Unit` — 选中回调；`modifier: Modifier = Modifier`
- 返回值：无（Composable 函数）
- 实现逻辑：委托给 LiquidSegmentedControl（TextOnly 样式），key 为索引
- 调用关系：被页面内标签切换使用
- 建议：无

---

## StatusPill.kt

### PillTone — 枚举
- 职责：状态药丸色调类型

#### SUCCESS
- 使用场景：成功/已完成状态

#### WARNING
- 使用场景：警告/待处理状态

#### DANGER
- 使用场景：危险/异常状态

#### INFO
- 使用场景：信息/中性状态

#### NEUTRAL
- 使用场景：默认/中性状态

### StatusPill — @Composable
- 职责：状态药丸标签组件

#### StatusPill(text, tone, modifier)
- 参数：`text: String` — 状态文本；`tone: PillTone` — 色调；`modifier: Modifier = Modifier`
- 返回值：无（Composable 函数）
- 实现逻辑：根据 tone 选择背景色（14% 透明度）和文字色，999dp 圆角药丸形状
- 调用关系：被列表项状态标签使用
- 建议：无

---

## ZhihuijiColors.kt

### ZhihuijiColors
- object / 职责：定义应用全局颜色常量 / 设计模式：颜色主题常量

#### Primary: Color
- 作用域：public val / 初始值：Color(0xFF1677FF) / 使用场景：主色调蓝色
- 建议：无

#### PrimaryGradientStart: Color
- 作用域：public val / 初始值：Color(0xFF2D8BFF) / 使用场景：主色渐变起始
- 建议：无

#### PrimaryGradientEnd: Color
- 作用域：public val / 初始值：Color(0xFF0874F9) / 使用场景：主色渐变终止
- 建议：无

#### Success: Color
- 作用域：public val / 初始值：Color(0xFF18B66A) / 使用场景：成功/正向
- 建议：无

#### Warning: Color
- 作用域：public val / 初始值：Color(0xFFFF9F1A) / 使用场景：警告/待处理
- 建议：无

#### Danger: Color
- 作用域：public val / 初始值：Color(0xFFF04438) / 使用场景：危险/错误
- 建议：无

#### InfoBlue: Color
- 作用域：public val / 初始值：Color(0xFF3B82F6) / 使用场景：信息蓝色
- 建议：无

#### TextPrimary: Color
- 作用域：public val / 初始值：Color(0xFF0F172A) / 使用场景：主文本色
- 建议：无

#### TextSecondary: Color
- 作用域：public val / 初始值：Color(0xFF667085) / 使用场景：次要文本色
- 建议：无

#### TextTertiary: Color
- 作用域：public val / 初始值：Color(0xFF98A2B3) / 使用场景：第三级文本色
- 建议：无

#### BorderLight: Color
- 作用域：public val / 初始值：Color(0xFFDCEBFA) / 使用场景：浅色边框
- 建议：无

#### CardBackground: Color
- 作用域：public val / 初始值：Color(0xF7FFFFFF) / 使用场景：卡片背景
- 建议：无

#### CardBorder: Color
- 作用域：public val / 初始值：Color(0xFFDDEBFA) / 使用场景：卡片边框
- 建议：无

#### BackgroundGradientStart: Color
- 作用域：public val / 初始值：Color(0xFFE8F7FF) / 使用场景：页面背景渐变起始
- 建议：无

#### BackgroundGradientMid: Color
- 作用域：public val / 初始值：Color(0xFFF4FBFF) / 使用场景：页面背景渐变中间
- 建议：无

#### BackgroundGradientEnd: Color
- 作用域：public val / 初始值：Color(0xFFFFFFFF) / 使用场景：页面背景渐变终止
- 建议：无

#### White: Color
- 作用域：public val / 初始值：Color(0xFFFFFFFF) / 使用场景：白色
- 建议：无

#### SurfaceVariant: Color
- 作用域：public val / 初始值：Color(0xFFF3F8FE) / 使用场景：变体表面色
- 建议：无

#### PressedBlue: Color
- 作用域：public val / 初始值：Color(0xFFE8F2FF) / 使用场景：按压态蓝色背景
- 建议：无

---

## ZhihuijiShapes.kt

### ZhihuijiShapes: Shapes
- 作用域：顶层 val / 初始值：Shapes(extraSmall=6dp, small=8dp, medium=12dp, large=16dp, extraLarge=20dp) / 使用场景：Material3 形状主题
- 建议：无

---

## ZhihuijiTheme.kt

### LightColorScheme: ColorScheme
- 作用域：private val / 初始值：lightColorScheme(...) / 使用场景：浅色主题配色方案
- 建议：仅支持浅色主题，缺少深色主题支持

### LocalExtendedColors: CompositionLocal\<ZhihuijiColors\>
- 作用域：顶层 val / 初始值：staticCompositionLocalOf { ZhihuijiColors } / 使用场景：提供扩展颜色访问
- 建议：无

### ZhihuijiTheme — @Composable
- 职责：应用主题入口 Composable

#### ZhihuijiTheme(content)
- 参数：`content: @Composable () -> Unit` — 主题包裹的内容
- 返回值：无（Composable 函数）
- 实现逻辑：CompositionLocalProvider 注入 LocalExtendedColors 和固定 fontScale=1f 的 Density → MaterialTheme 设置 colorScheme/typography/shapes
- 调用关系：被 Application/Activity 根 Composable 调用
- 建议：固定 fontScale=1f 会忽略用户系统字体缩放偏好，可能影响无障碍体验

### ExtendedTheme
- object / 职责：提供扩展主题颜色访问入口

#### colors: ZhihuijiColors
- 作用域：@Composable getter / 使用场景：在 Composable 中获取扩展颜色
- 建议：无

---

## ZhihuijiTypography.kt

### baseLineHeightStyle: LineHeightStyle
- 作用域：private val / 初始值：LineHeightStyle(alignment=Center, trim=Both) / 使用场景：所有文本样式的行高样式基准
- 建议：无

### zhihuijiTextStyle(fontSize, lineHeight, fontWeight): TextStyle
- 参数：`fontSize: TextUnit` — 字号；`lineHeight: TextUnit` — 行高；`fontWeight: FontWeight` — 字重
- 返回值：`TextStyle` — 统一的文本样式
- 实现逻辑：创建 SansSerif 字体族的 TextStyle，应用 baseLineHeightStyle
- 调用关系：被 ZhihuijiTypography 各样式定义调用
- 建议：无

### ZhihuijiTypography: Typography
- 作用域：顶层 val / 初始值：Typography(...) / 使用场景：全局排版主题
- 建议：定义了完整的 Material3 Typography 层级，字号从 10sp（labelSmall）到 23sp（displayLarge），覆盖应用所有文本场景
