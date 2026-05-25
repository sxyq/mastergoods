# data/finance 模块开发说明

- 当前状态：FinanceRepository 已实现，采用在线优先 + Room 列表缓存。
- 实际源码目录：`data/finance/src/main/java/com/zhihuiji/data/finance`
- 目标：封装资金流水查询与新增。

## 需要创建的类

- `FinanceRepository`

## 需要实现的关键函数

- `observeFinanceRecords(filter: FinanceFilter): Flow<List<FinanceRecordDto>>`
- `refreshFinanceRecords(filter: FinanceFilter)`
- `createFinanceRecord(request: CreateFinanceRecordRequest): FinanceRecordDto`

## 验收标准

- 能支持收入/支出筛选、日期筛选和手工记账。

## UI 设计规范支撑

- 资金流水列表需要提供收入、支出、净流入、期末余额四个摘要值。
- 流水项需要提供类型、关联方、时间、支付方式和正负金额色彩语义。
