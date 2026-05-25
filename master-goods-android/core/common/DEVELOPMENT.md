# core/common 模块开发说明

- 当前状态：脚手架已创建，工具类未开始。
- 实际源码目录：`core/common/src/main/java/com/zhihuiji/core/common`
- 目标：放通用格式化、状态文本、结果封装、错误消息转换。

## 需要创建的类

- `MoneyFormatter`
- `TimeFormatter`
- `StatusLabels`
- `UiMessage`
- `ResultExt`

## 需要实现的关键函数

- `MoneyFormatter.format(amount: BigDecimal?): String`
- `TimeFormatter.formatDateTime(epochMillis: Long?): String`
- `TimeFormatter.formatDate(epochMillis: Long?): String`
- `StatusLabels.saleOrderStatus(code: Int): String`
- `StatusLabels.purchaseOrderStatus(code: Int): String`
- `StatusLabels.payOrderStatus(code: Int): String`
- `StatusLabels.financeType(code: Int): String`
- `StatusLabels.supplierStatus(code: Int): String`
- `UiMessage.fromThrowable(throwable: Throwable): String`
- `ResultExt.requireData()`
  - 对 `ApiResponse<T>` 统一取值并抛业务异常。

## UI 设计规范支撑

- `StatusLabels` 的文案必须和 `StatusPill` 状态保持一致。
- 金额格式化必须支持正负色彩语义：收入/增长为绿色，欠款/支出为红色，主合计为蓝色。
- 时间格式要服务列表页的紧凑展示，优先输出 `yyyy-MM-dd` 和 `HH:mm` 两类短格式。

## 验收标准

- 业务模块不再各自写金额、时间、状态码转换逻辑。
