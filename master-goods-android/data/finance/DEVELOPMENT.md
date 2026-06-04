# data/finance 模块开发说明

- 当前状态：`FinanceRepository` 已实现 `/v1` 轻量流水；`FinanceV2Repository` 已实现 `/v2` 账户、转账、单据资金关联首轮承接。
- 实际源码目录：`data/finance/src/main/java/com/zhihuiji/data/finance`
- 目标：封装资金流水查询与新增。

## 需要创建的类

- `FinanceRepository`
- `FinanceV2Repository`

## 需要实现的关键函数

- `observeFinanceRecords(filter: FinanceFilter): Flow<List<FinanceRecordDto>>`
- `refreshFinanceRecords(filter: FinanceFilter)`
- `createFinanceRecord(request: CreateFinanceRecordRequest): FinanceRecordDto`
- `/v2`：
  - `listAccounts()/getAccount()/createAccount()/updateAccount()/deleteAccount()`
  - `listTransfers()/getTransfer()/createTransfer()`
  - `listBillFundLinks()/createBillFundLink()/deleteBillFundLink()`

## 验收标准

- 能支持收入/支出筛选、日期筛选和手工记账。

## UI 设计规范支撑

- 资金流水列表需要提供收入、支出、净流入、期末余额四个摘要值。
- 流水项需要提供类型、关联方、时间、支付方式和正负金额色彩语义。

## UI 联动约束

- 本模块虽然不直接负责页面绘制，但其输出的数据结构、状态枚举、错误语义和交互支撑能力必须服务于统一的 Android UI 基线。
- 后续新增业务不能倒逼页面切换成另一套视觉风格；应优先通过补充 `core/designsystem` 通用组件或扩展既有页面母版来承接。
- 需要映射到 UI 的状态、金额、风险、同步结果等，应继续服从统一的颜色语义、状态标签和信息层级。
- Android 视觉真源固定为 `docs/design-mockups/01.png ~ 08.png` 与 `master-goods-android/UI-DESIGN-SPEC.md`。
