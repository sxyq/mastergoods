# UI 设计规范（Stitch 版摘要）

> 本文件不再以 `docs/design-mockups/01.png ~ 08.png` 作为唯一视觉真源。
> 当前 Android UI 的正式重构基线以：
>
> - [42-android-liquid-glass-ui-refactor-plan.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/spec/42-android-liquid-glass-ui-refactor-plan.md)
> - [stitch manifest.tsv](/Users/sunyiyang/Desktop/Project/master-goods/stitch_exports/visual-design_system_framework_14840154594131085259/manifest.tsv)
>
> 为准。

## 1. 当前视觉定位

- 风格关键词：极光玻璃、经营工具、高密度信息、轻盈但专业
- 页面气质：不是营销页，不使用大面积插画或夸张阴影
- 交互基调：快速录入、快速扫读、快速跳转、底部拇指优先

## 2. 全局视觉摘要

- 背景：
  - 顶部浅蓝极光渐变 `#E1EFFF`
  - 底部过渡到 `#FFFFFF`
  - 基础背景色 `#F7F9FE`
- 主色：
  - 主品牌蓝 `#005BBF`
  - 亮蓝强调 `#1A73E8`
- 语义色：
  - 成功 `#34A853`
  - 警告 `#FB8C00`
  - 错误 `#EA4335`
- 主文字：
  - 一级 `#181C20`
  - 二级 `#414754`
  - 数据二级 `#6B7280`

## 3. 版式摘要

- 页面左右边距：16dp
- 区块垂直间距：12dp
- 行内间距：8dp
- 卡片内边距：16dp
- 主卡片圆角：16dp
- 输入框 / 主按钮圆角：12dp
- chip / pill：全圆角

## 4. 字体摘要

- 标题字体：`Plus Jakarta Sans`
- 正文字体：`Inter`
- 页面主标题：24sp / 32sp / 700
- 二级标题：20sp / 28sp / 600
- 正文：14sp / 20sp / 400
- 大金额：22sp / 28sp / 700

## 5. 组件摘要

- 全局壳层：`GlassScaffold`
- 顶栏：`GlassTopBar`
- 玻璃卡：`LiquidGlassSurface` `LiquidGlassCard`
- 指标卡：`KpiCard`
- 图表卡：`ChartCard`
- 搜索筛选：`SearchFilterBar` `SegmentedTabs` `FilterChipRow`
- 列表行：`BusinessListItem`
- 底部操作：`BottomActionBar`
- 状态标签：`StatusPill`

## 6. 页面母版摘要

- 列表页：顶栏 + 搜索/筛选 + 状态切换 + 高密度玻璃列表
- 详情页：主信息卡 + 汇总卡 + 明细卡 + 底部固定操作
- 编辑页：分组表单卡 + 金额汇总 + 固定提交栏
- 报表页：时间切换 + KPI + 图表 + 排行榜
- AI 页：身份区 + 摘要 KPI + 洞察/过程 + 输入区
- 设置页：头像卡 + 设置项卡组 + 底部危险操作

## 7. 实施说明

- 所有细节、页面映射、颜色令牌、重构顺序、验收标准，统一看：
  - [42-android-liquid-glass-ui-refactor-plan.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/spec/42-android-liquid-glass-ui-refactor-plan.md)
- 历史 `docs/design-mockups/` 仅作旧参考，不再作为新 UI 验收的唯一标准。
