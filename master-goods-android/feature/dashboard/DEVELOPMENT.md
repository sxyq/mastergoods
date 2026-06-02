# feature/dashboard 模块开发说明

- 当前状态：脚手架已创建，页面未开始。
- 实际源码目录：`feature/dashboard/src/main/java/com/zhihuiji/feature/dashboard`
- 目标：实现首页经营看板。

## 需要创建的类

- `DashboardScreen`
- `DashboardViewModel`
- `DashboardUiState`

## 需要实现的关键函数

- `DashboardViewModel.loadDashboard()`
- `DashboardViewModel.refresh()`
- `DashboardViewModel.loadQuickReports(windowDays: Int)`
- `DashboardViewModel.openLowStock()`
- `DashboardViewModel.openReceivables()`
- `DashboardViewModel.openAgentWorkbench()`

## 页面内容

- 销售汇总卡片
- 利润汇总卡片
- 低库存商品
- 应收排行
- Agent 摘要入口

## UI 设计规范

- 对照设计图 `01.png` 和 `02.png` 的首页经营看板实现（来源见 `docs/design-mockups`）。
- 顶部使用大标题“智慧记”，右侧放通知和扫码图标，通知角标为红色圆点。
- KPI 使用 2 列网格玻璃卡片，包含图标、主数字、较昨日增长或说明。
- 销售趋势图放入独立卡片，时间 Tab 使用蓝色激活胶囊。
- 待处理提醒使用列表卡片，左侧彩色图标，右侧红橙角标和箭头。

## 验收标准

- 首屏能在一次刷新中聚合报表和提醒数据。
