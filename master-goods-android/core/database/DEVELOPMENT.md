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

## UI 联动约束

- 本模块虽然不直接负责页面绘制，但其输出的数据结构、状态枚举、错误语义和交互支撑能力必须服务于统一的 Android UI 基线。
- 后续新增业务不能倒逼页面切换成另一套视觉风格；应优先通过补充 `core/designsystem` 通用组件或扩展既有页面母版来承接。
- 需要映射到 UI 的状态、金额、风险、同步结果等，应继续服从统一的颜色语义、状态标签和信息层级。
- Android 视觉真源固定为 `docs/design-mockups/01.png ~ 08.png` 与 `master-goods-android/UI-DESIGN-SPEC.md`。
