# Android core/designsystem 模块分析

- 对应源码目录：`master-goods-android/core/designsystem`
- 关键源码：
  - `ZhihuijiTheme.kt`
  - `ZhihuijiColors.kt`
  - `ZhihuijiTypography.kt`
  - `ZhihuijiShapes.kt`
  - `GlassScaffold.kt`
  - `FloatingLiquidBottomBar.kt`
  - `LiquidSegmentedControl.kt`
  - `LiquidGlassSurface.kt`
  - `BottomBarBehavior.kt`
  - `GlassCard.kt`
  - `KpiCard.kt`
  - `ChartCard.kt`
  - `PrimaryGradientButton.kt`

## 模块定位

`core/designsystem` 在新版里承接的是**通用交互容器与领域组件底座**。  
虽然它不替代逐页视觉稿，但当前文档必须明确统一 UI 基线如何落到可复用实现里：

- 设计系统的分层边界
- 哪些组件是稳定底座
- 哪些组件仍带首版业务痕迹
- `/v2` 扩域后需要新增哪些领域组件

## 状态图例

- `新版已做`
- `新版待做`
- `旧版存在新版未做`
- `新版需要去掉`
- `需重构`
- `待验证`

## 当前源码覆盖

### 设计令牌与主题层

| 文件 | 当前作用 | 状态 | 说明 |
|---|---|---|---|
| `ZhihuijiColors.kt` | 颜色令牌 | 新版已做 | 已有主色、语义色、文本、边框、背景等基础变量 |
| `ZhihuijiTypography.kt` | 排版令牌 | 需重构 | 已统一字号层级，但仍使用 `FontFamily.SansSerif`，后续可继续升级 |
| `ZhihuijiShapes.kt` | 形状令牌 | 新版已做 | 已有统一圆角体系 |
| `ZhihuijiTheme.kt` | Material 3 主题入口 | 新版已做 | 已形成统一主题承载层 |

### 通用容器与导航层

| 文件 | 当前作用 | 状态 | 说明 |
|---|---|---|---|
| `GlassScaffold.kt` | 主壳容器 | 新版已做 | 承接五栏主壳与内容区 |
| `FloatingLiquidBottomBar.kt` | 五栏底部导航 | 新版已做 | 当前主壳的核心导航容器 |
| `LiquidSegmentedControl.kt` | 通用切换组件底座 | 新版已做 | 已成为底栏与 tabs 的统一基础 |
| `BottomBarBehavior.kt` | 底栏隐藏/回顶/重选行为 | 新版已做 | 已有首版交互行为抽象 |
| `LiquidGlassSurface.kt` | 通用玻璃表面容器 | 新版已做 | 通用玻璃基础层 |

### 通用业务组件层

| 文件 | 当前作用 | 状态 | 说明 |
|---|---|---|---|
| `GlassCard.kt` / `KpiCard.kt` / `StatusPill.kt` | 卡片与状态组件 | 新版已做 | 可继续作为领域页基础积木 |
| `SearchFilterBar.kt` / `SegmentedTabs.kt` / `QuantityStepper.kt` | 交互型基础组件 | 新版已做 | 仍能继续复用 |
| `ChartCard.kt` | 图表容器与图表示例组件 | 需重构 | 当前既是通用容器，也夹带具体图表实现 |
| `PrimaryGradientButton.kt` | 老按钮兼容入口 | 新版需要去掉 | 已标记废弃，建议收口到 `PrimaryButton` |

## 状态表

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| 统一设计系统底座 | 新版已做 | 旧版 UI 不是当前 Compose 体系 | 所有页面通过统一设计系统承载 | 当前 20+ 设计系统文件已存在 | 是后续页面重构基础 |
| 视觉令牌层 | 新版已做 | 旧版无当前 token 体系 | 保持色彩/排版/形状统一 | 当前 `Colors/Typography/Shapes/Theme` 已存在 | 继续沿用 |
| 与设计稿像素级贴合 | 待验证 | 旧版视觉参考不同 | 在真机上继续压细节 | 当前效果已形成风格，但仍需核对截图 | 不属于本轮后端文档改造 |
| `/v2` 新领域组件 | 新版待做 | 旧版无新版页面组件 | 随账户、库存、财务扩域新增组件 | 当前组件集仍围绕首版业务 | 后续随 feature 扩展 |
| 兼容型旧按钮入口 | 需重构 | 首版迁移期常保留旧命名组件 | 收口到更统一的按钮体系 | `PrimaryGradientButton` 已废弃但仍保留；B10 已把 feature 层主按钮调用收口到 `PrimaryButton` | 后续可清理兼容入口本身 |
| 图表组件职责混合 | 需重构 | 首版为了快交付把图表容器和图表示例放在一起 | 容器、图表 primitive、领域图表分层 | 当前 `ChartCard.kt` 混合度偏高 | 报表扩域时会成为瓶颈 |

## 新版领域组件规划

| 领域 | 状态 | 未来组件方向 |
|---|---|---|
| 商品域 | 新版待做 | 多价格行、多单位换算、供应关系卡片、媒体宫格 |
| 档案域 | 新版待做 | 联系人列表、标签组、价格策略块 |
| 销售/采购域 | 新版待做 | 订单态时间轴、状态轨迹、资金联动块 |
| 财务域 | 新版待做 | 账户卡、转账条目、资金来源标签、找零记录块 |
| 库存域 | 新版待做 | 库存账本条目、库存快照卡、月统计图块 |
| 导入/同步域 | 新版待做 | 导入任务卡、同步状态卡、冲突提示块 |
| AI 域 | 新版待做 | owner 感知的洞察卡、消息卡、建议动作卡 |

## 当前结论

- `core/designsystem` 已经具备稳定底座。
- 下一阶段它最重要的工作不是换风格，而是把“通用容器”和“领域组件”边界切清楚。
- 这样等后端 `/v2` 和领域扩容开始落地时，安卓页面才能在不重写整套视觉基础层的前提下继续演进。

## UI 联动约束

- `core/designsystem` 是后续所有新增业务页的**唯一视觉实现底座**。
- 允许扩展的是：
  - 新的领域卡片块
  - 新的状态标签组合
  - 新的明细行组件
  - 新的报表图块
- 不允许漂移的是：
  - 全局背景和色系
  - 卡片形态
  - 顶栏/底栏结构
  - 主次按钮层级
  - 状态色语义
- 如果某个新业务页面“看起来放不进去”，优先补页面母版或补通用组件，而不是让 feature 自己发明第二套设计系统。
- Android 视觉真源固定为 `docs/design-mockups/01.png ~ 08.png` 与 `master-goods-android/UI-DESIGN-SPEC.md`。
