# Tasks

- [ ] Task 1: 删除旧 UI 源码（保留模块骨架）
  - [ ] SubTask 1.1: 删除 `core/designsystem/src/main/java/com/zhihuiji/core/designsystem/` 下全部 Kotlin 文件
  - [ ] SubTask 1.2: 删除 `feature/auth/src/main/java/com/zhihuiji/feature/auth/` 下全部 Kotlin 文件
  - [ ] SubTask 1.3: 删除 `feature/dashboard/src/main/java/com/zhihuiji/feature/dashboard/` 下全部 Kotlin 文件
  - [ ] SubTask 1.4: 删除 `feature/products/src/main/java/com/zhihuiji/feature/products/` 下全部 Kotlin 文件
  - [ ] SubTask 1.5: 删除 `feature/customers/src/main/java/com/zhihuiji/feature/customers/` 下全部 Kotlin 文件
  - [ ] SubTask 1.6: 删除 `feature/suppliers/src/main/java/com/zhihuiji/feature/suppliers/` 下全部 Kotlin 文件
  - [ ] SubTask 1.7: 删除 `feature/sales/src/main/java/com/zhihuiji/feature/sales/` 下全部 Kotlin 文件
  - [ ] SubTask 1.8: 删除 `feature/purchases/src/main/java/com/zhihuiji/feature/purchases/` 下全部 Kotlin 文件
  - [ ] SubTask 1.9: 删除 `feature/payments/src/main/java/com/zhihuiji/feature/payments/` 下全部 Kotlin 文件
  - [ ] SubTask 1.10: 删除 `feature/finance/src/main/java/com/zhihuiji/feature/finance/` 下全部 Kotlin 文件
  - [ ] SubTask 1.11: 删除 `feature/reports/src/main/java/com/zhihuiji/feature/reports/` 下全部 Kotlin 文件
  - [ ] SubTask 1.12: 删除 `feature/agent/src/main/java/com/zhihuiji/feature/agent/` 下全部 Kotlin 文件
  - [ ] SubTask 1.13: 删除 `feature/settings/src/main/java/com/zhihuiji/feature/settings/` 下全部 Kotlin 文件
  - [ ] SubTask 1.14: 删除 `app/src/main/java/com/zhihuiji/app/navigation/` 下除 `AppNavGraph.kt` 外的全部 Kotlin 文件
  - [ ] SubTask 1.15: 验证删除后项目能编译（data/core 层不受影响）

- [ ] Task 2: 重建 `core/designsystem` Liquid Glass 设计系统
  - [ ] SubTask 2.1: 创建 `ZhihuijiColors.kt` — 颜色令牌（主色 `#1677FF`、成功 `#18B66A`、警告 `#FF9F1A`、危险 `#F04438`）
  - [ ] SubTask 2.2: 创建 `ZhihuijiTypography.kt` — 排版令牌（页面大标题 24sp、页面标题 18sp、卡片标题 14sp、正文 13sp、辅助 11sp、金额 22sp）
  - [ ] SubTask 2.3: 创建 `ZhihuijiShapes.kt` — 形状令牌（卡片 12-16dp、输入框 10-12dp、按钮 12dp）
  - [ ] SubTask 2.4: 创建 `ZhihuijiTheme.kt` — Material 3 主题入口
  - [ ] SubTask 2.5: 创建 `LiquidGlassCard.kt` — 液态玻璃卡片（blur + vibrancy + lens + highlight + innerShadow）
  - [ ] SubTask 2.6: 创建 `LiquidGlassSurface.kt` — 通用玻璃表面容器
  - [ ] SubTask 2.7: 创建 `GlassScaffold.kt` — 主壳容器（渐变背景 + 状态栏适配 + 底部导航占位）
  - [ ] SubTask 2.8: 创建 `FloatingLiquidBottomBar.kt` — 五栏底部导航
  - [ ] SubTask 2.9: 创建 `GlassTopBar.kt` — 顶部栏（大标题/居中标题/返回/搜索/筛选/更多）
  - [ ] SubTask 2.10: 创建 `KpiCard.kt` — KPI 指标卡片
  - [ ] SubTask 2.11: 创建 `StatusPill.kt` — 状态标签（正常/低库存/缺货/待收款/已完成/作废等）
  - [ ] SubTask 2.12: 创建 `SearchFilterBar.kt` — 搜索栏 + 筛选按钮
  - [ ] SubTask 2.13: 创建 `SegmentedTabs.kt` — 顶部分类切换
  - [ ] SubTask 2.14: 创建 `FilterChipRow.kt` — 横向胶囊筛选
  - [ ] SubTask 2.15: 创建 `PrimaryButton.kt` / `SecondaryOutlineButton.kt` / `DangerOutlineButton.kt` — 按钮体系
  - [ ] SubTask 2.16: 创建 `BottomActionBar.kt` — 底部固定操作区
  - [ ] SubTask 2.17: 创建 `EmptyState.kt` — 空状态组件
  - [ ] SubTask 2.18: 创建 `QuantityStepper.kt` — 数量步进器
  - [ ] SubTask 2.19: 创建 `ChartCard.kt` — 图表容器（折线图/饼图/柱状图）
  - [ ] SubTask 2.20: 创建 `BusinessListItem.kt` — 通用业务列表项

- [ ] Task 3: 重建 `app/navigation` 导航体系
  - [ ] SubTask 3.1: 创建 `MainScreen.kt` — 五栏主壳 + 底部导航
  - [ ] SubTask 3.2: 创建 `MainNavGraph.kt` — 主模块导航图
  - [ ] SubTask 3.3: 创建 `SubNavGraph.kt` — 子页面导航图
  - [ ] SubTask 3.4: 更新 `AppNavGraph.kt` — 根导航图整合

- [ ] Task 4: 重建 `feature/auth` 登录/注册
  - [ ] SubTask 4.1: 创建 `AuthViewModel.kt` — 认证状态机
  - [ ] SubTask 4.2: 创建 `LoginScreen.kt` — 登录页（Logo + 手机号/密码输入 + 记住密码 + 忘记密码 + 第三方登录）
  - [ ] SubTask 4.3: 创建 `RegisterScreen.kt` — 注册页

- [ ] Task 5: 重建 `feature/dashboard` 首页经营看板
  - [ ] SubTask 5.1: 创建 `DashboardViewModel.kt` — 经营数据聚合
  - [ ] SubTask 5.2: 创建 `DashboardScreen.kt` — 今日经营标题 + 日期 + 四个 KPI 卡 + 销售趋势图 + 待处理提醒 + 快捷操作

- [ ] Task 6: 重建 `feature/sales` 销售单据
  - [ ] SubTask 6.1: 创建 `SaleOrderListScreen.kt` + `SaleOrderListViewModel.kt`
  - [ ] SubTask 6.2: 创建 `SaleOrderDetailScreen.kt` + `SaleOrderDetailViewModel.kt`
  - [ ] SubTask 6.3: 创建 `SaleOrderEditorScreen.kt` + `SaleOrderEditorViewModel.kt`
  - [ ] SubTask 6.4: 创建 `SalePaymentSheet.kt` — 收款底部弹窗

- [ ] Task 7: 重建 `feature/purchases` 采购单据
  - [ ] SubTask 7.1: 创建 `PurchaseOrderListScreen.kt` + `PurchaseOrderViewModel.kt`
  - [ ] SubTask 7.2: 创建 `PurchaseOrderDetailScreen.kt` + `PurchaseOrderDetailViewModel.kt`
  - [ ] SubTask 7.3: 创建 `PurchaseOrderEditorScreen.kt` + `PurchaseOrderEditorViewModel.kt`

- [ ] Task 8: 重建 `feature/payments` 付款单
  - [ ] SubTask 8.1: 创建 `PayOrderListScreen.kt` + `PayOrderViewModel.kt`
  - [ ] SubTask 8.2: 创建 `PayOrderEditorScreen.kt` — 付款单创建/编辑

- [ ] Task 9: 重建 `feature/products` 商品档案
  - [ ] SubTask 9.1: 创建 `ProductListScreen.kt` + `ProductListViewModel.kt`
  - [ ] SubTask 9.2: 创建 `ProductDetailScreen.kt` — 商品详情
  - [ ] SubTask 9.3: 创建 `ProductEditorScreen.kt` + `ProductEditorViewModel.kt`
  - [ ] SubTask 9.4: 创建 `StockAdjustSheet.kt` — 库存调整底部弹窗

- [ ] Task 10: 重建 `feature/customers` 客户档案
  - [ ] SubTask 10.1: 创建 `CustomerListScreen.kt` + `CustomerViewModel.kt`
  - [ ] SubTask 10.2: 创建 `CustomerDetailScreen.kt` + `CustomerDetailViewModel.kt`
  - [ ] SubTask 10.3: 创建 `CustomerEditorScreen.kt` + `CustomerEditorViewModel.kt`

- [ ] Task 11: 重建 `feature/suppliers` 供应商档案
  - [ ] SubTask 11.1: 创建 `SupplierListScreen.kt` + `SupplierViewModel.kt`
  - [ ] SubTask 11.2: 创建 `SupplierDetailScreen.kt` + `SupplierDetailViewModel.kt`
  - [ ] SubTask 11.3: 创建 `SupplierEditorScreen.kt` + `SupplierEditorViewModel.kt`

- [ ] Task 12: 重建 `feature/finance` 财务
  - [ ] SubTask 12.1: 创建 `FinanceRecordListScreen.kt` + `FinanceViewModel.kt`
  - [ ] SubTask 12.2: 创建 `FinanceRecordEditorSheet.kt` — 资金记录编辑

- [ ] Task 13: 重建 `feature/reports` 报表
  - [ ] SubTask 13.1: 创建 `ReportScreen.kt` + `ReportViewModel.kt`
  - [ ] SubTask 13.2: 实现资金流水/报表总览/库存流水/对账汇总四个子页

- [ ] Task 14: 重建 `feature/agent` AI 助手
  - [ ] SubTask 14.1: 创建 `AgentWorkbenchScreen.kt` + `AgentViewModel.kt`
  - [ ] SubTask 14.2: 创建 `AgentChatScreen.kt` — AI 问答页
  - [ ] SubTask 14.3: 创建 `OperationDraftScreen.kt` — 操作草稿页
  - [ ] SubTask 14.4: 创建 `AgentTaskScreen.kt` — 任务与通知页

- [ ] Task 15: 重建 `feature/settings` 设置
  - [ ] SubTask 15.1: 创建 `SettingsScreen.kt` + `SettingsViewModel.kt`
  - [ ] SubTask 15.2: 实现账号安全、通知设置、服务器地址、数据同步、语言设置、清除缓存、关于、用户协议、隐私政策、退出登录

- [ ] Task 16: 编译验证与修复
  - [ ] SubTask 16.1: 运行 `./gradlew :app:assembleDebug` 检查编译错误
  - [ ] SubTask 16.2: 修复所有编译错误和 import 问题
  - [ ] SubTask 16.3: 运行 lint 检查

# Task Dependencies

- Task 2 (designsystem) must complete before Tasks 4-15
- Task 3 (navigation) must complete before Tasks 4-15
- Task 1 (delete old code) has no dependencies and can run first
- Tasks 4-15 can run in parallel after Task 2 and Task 3 complete
- Task 16 depends on all previous tasks
