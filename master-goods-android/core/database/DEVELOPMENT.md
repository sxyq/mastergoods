# core/database 模块开发说明

- 当前状态：脚手架已创建，数据库层未开始。
- 实际源码目录：`core/database/src/main/java/com/zhihuiji/core/database`
- 目标：为在线优先和同步缓存准备 Room 表、DAO 与数据库入口。

## 需要创建的类

- `ZhihuijiDatabase`
- `ProductEntity` / `ProductDao`
- `CustomerEntity` / `CustomerDao`
- `SupplierEntity` / `SupplierDao`
- `SaleOrderEntity` / `SaleOrderDao`
- `PurchaseOrderEntity` / `PurchaseOrderDao`
- `PayOrderEntity` / `PayOrderDao`
- `FinanceRecordEntity` / `FinanceRecordDao`
- `AgentNotificationEntity` / `AgentNotificationDao`
- `SyncCursorEntity` / `SyncCursorDao`

## 需要实现的关键函数

- DAO 基础方法：`observeAll()`、`findById()`、`upsert()`、`upsertAll()`、`deleteById()`、`clear()`
- 复杂查询：
  - `ProductDao.search(keyword)`
  - `CustomerDao.search(keyword)`
  - `SupplierDao.search(keyword, status)`
  - `SaleOrderDao.query(filter)`
  - `FinanceRecordDao.query(filter)`
- `ZhihuijiDatabase.withTransaction { ... }`
  - 给同步批量写入和退出登录清缓存使用。

## 验收标准

- 开启 Room 后不需要改 ViewModel 公开接口，只替换 Repository 内部实现。

## UI 设计规范支撑

- 本地缓存查询要优先满足列表页快速渲染，避免进入页面后长时间空白。
- 缓存实体要保留状态、金额、更新时间等字段，供列表卡片、状态标签和报表摘要直接展示。
