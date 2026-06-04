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
- `StatusLabels.customerStatus(code: Int): String`
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

## UI 联动约束

- 本模块虽然不直接负责页面绘制，但其输出的数据结构、状态枚举、错误语义和交互支撑能力必须服务于统一的 Android UI 基线。
- 后续新增业务不能倒逼页面切换成另一套视觉风格；应优先通过补充 `core/designsystem` 通用组件或扩展既有页面母版来承接。
- 需要映射到 UI 的状态、金额、风险、同步结果等，应继续服从统一的颜色语义、状态标签和信息层级。
- Android 视觉真源固定为 `docs/design-mockups/01.png ~ 08.png` 与 `master-goods-android/UI-DESIGN-SPEC.md`。
