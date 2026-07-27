# core/designsystem 模块开发说明

- 当前状态：主题、玻璃卡片/顶栏/底栏、状态标签、搜索筛选、分段 Tab、KPI 卡、主按钮体系已落地；`FloatingGlassActionButton` 作为主列表统一的右下胶囊主操作。
- 实际源码目录：`core/designsystem/src/main/java/com/zhihuiji/core/designsystem`
- 目标：把设计图中的毛玻璃风格沉淀为 Compose 主题、组件和页面容器，所有 feature 页面必须复用这里的组件。

## 设计依据

- 全局规范见 [UI-DESIGN-SPEC.md](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/UI-DESIGN-SPEC.md)。
- 参考设计图见 Stitch 导出清单 `web/public/stitch_exports/`。

## 需要创建的主题类

- `ZhihuijiTheme`
- `ZhihuijiColors`
- `ZhihuijiTypography`
- `ZhihuijiShapes`
- `GlassBackground`

## 需要创建的基础组件

- `GlassScaffold`
- `GlassTopBar`
- `BottomNavigationBar`
- `GlassCard`
- `PrimaryButton`，`PrimaryGradientButton` 仅作为迁移期废弃兼容入口保留
- `SecondaryOutlineButton`
- `DangerOutlineButton`
- `StatusPill`
- `KpiCard`
- `SearchFilterBar`
- `SegmentedTabs`
- `FilterChipRow`
- `QuantityStepper`
- `BottomActionBar`
- `ChartCard`
- `EmptyState`
- `FloatingGlassActionButton`

## 需要实现的关键函数

- `ZhihuijiTheme(content)`
  - 提供全局颜色、字体、形状、状态栏和导航栏样式。
- `GlassScaffold(title, selectedDestination, actions, content)`
  - 提供渐变背景、顶部栏、底部导航、安全区和页面内容槽位。
- `GlassTopBar(title, navigationIcon, actions)`
  - 支持返回、搜索、筛选、扫码、打印、更多、通知。
- `GlassCard(content)`
  - 统一半透明白色、轻描边、弱阴影、圆角和内边距。
- `PrimaryButton(text, icon, enabled, onClick)`；新增 feature 不再直接调用 `PrimaryGradientButton`
  - 用于登录、保存、提交订单、确认收款、付款等主操作。
- `StatusPill(status, text)`
  - 根据状态映射背景色、文字色和图标。
- `KpiCard(title, value, subtitle, trend, icon, tone)`
  - 用于首页、报表、AI 工作台的指标卡。
- `SearchFilterBar(query, placeholder, showFilter, onQueryChange, onFilterClick)`
  - 用于商品、客户、供应商、单据和报表列表。
- `QuantityStepper(value, min, max, onMinus, onPlus, onValueChange)`
  - 用于销售和采购开单。
- `BottomActionBar(primaryAction, secondaryActions)`
  - 固定在底部，处理主次按钮布局。
- `FloatingGlassActionButton(text, icon, onClick)`
  - 用于档案/单据/财务主列表的右下胶囊主操作，替代各 feature 自己手写圆形 `FloatingActionButton`。

## 验收标准

- 不允许 feature 模块各自手写一套卡片、按钮、状态标签。
- 任意一个页面只要切换到本主题，就应呈现浅蓝背景和毛玻璃卡片。
- 设计系统组件需要覆盖当前 8 张设计图中出现的主要控件。

## UI 联动约束

- 本模块虽然不直接负责页面绘制，但其输出的数据结构、状态枚举、错误语义和交互支撑能力必须服务于统一的 Android UI 基线。
- 后续新增业务不能倒逼页面切换成另一套视觉风格；应优先通过补充 `core/designsystem` 通用组件或扩展既有页面母版来承接。
- 需要映射到 UI 的状态、金额、风险、同步结果等，应继续服从统一的颜色语义、状态标签和信息层级。
- Android 当前视觉真源以 Stitch 导出（`web/public/stitch_exports/`）、`docs/spec/42-android-liquid-glass-ui-refactor-plan.md` 与 `master-goods-android/UI-DESIGN-SPEC.md` 为准。
