# feature/payments 模块开发说明

- 当前状态：列表页+新建+详情+状态更新已完成，业务链路已走通。
- 实际源码目录：`feature/payments/src/main/java/com/zhihuiji/feature/payments`
- 目标：实现付款单列表、详情、创建和状态更新。

## 需要创建的类

- `PayOrderListScreen`
- `PayOrderEditorScreen`
- `PayOrderDetailScreen`
- `PayOrderViewModel`

## 需要实现的关键函数

- `PayOrderViewModel.loadOrders(filter: PayOrderFilter)`
- `PayOrderViewModel.loadDetail(id: Long)`
- `PayOrderViewModel.selectSupplier(supplierId: Long)`
- `PayOrderViewModel.submitOrder()`
- `PayOrderViewModel.updateStatus(id: Long, status: Int)`
- `PayOrderViewModel.validateForm()`

## 验收标准

- 付款单状态切换后，前端要能及时刷新余额相关展示。

## UI 设计规范

- 对照设计图 `06.png` 的付款单列表实现（来源见 `docs/design-mockups`）。
- 顶部标题为“付款单列表”，右侧更多图标；状态 Tab 包含全部、待付款、部分付款、已完成。
- 搜索框支持单号、供应商、备注，右侧保留筛选按钮。
- 付款单卡片展示付款单号、供应商、关联单号、应付金额、已付金额、日期和状态。
- 右下角使用蓝色浮动按钮“新建付款单”。
