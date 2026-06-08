# data/supplier 模块开发说明

- 当前状态：`SupplierRepository` 已实现 `/v1` 兼容链路；`SupplierV2Repository` 已实现 `/v2` 供应商、分组、联系人首轮承接。
- 实际源码目录：`data/supplier/src/main/java/com/zhihuiji/data/supplier`
- 目标：封装供应商列表、状态过滤、增删改。

## 需要创建的类

- `SupplierRepository`
- `SupplierV2Repository`

## 需要实现的关键函数

- `observeSuppliers(keyword: String, status: Int?): Flow<List<SupplierDto>>`
- `refreshSuppliers(keyword: String?, status: Int?)`
- `getSupplier(id: Long): SupplierDto`
- `createSupplier(draft: SupplierDto): SupplierDto`
- `updateSupplier(id: Long, draft: SupplierDto): SupplierDto`
- `deleteSupplier(id: Long)`
- `/v2`：
  - `listSuppliers(keyword, status, groupId)`
  - `listGroups()/createGroup()/updateGroup()/deleteGroup()`
  - `listContacts(supplierId)/createContact()/updateContact()/deleteContact()`

## 验收标准

- 支持“全部/启用/停用”状态切换和关键字搜索。

## UI 设计规范支撑

- 供应商列表需要提供联系人、脱敏手机号、应付余额和状态。
- 供应商详情需要提供信用额度、可用额度、采购汇总和往来记录入口。

## UI 联动约束

- 本模块虽然不直接负责页面绘制，但其输出的数据结构、状态枚举、错误语义和交互支撑能力必须服务于统一的 Android UI 基线。
- 后续新增业务不能倒逼页面切换成另一套视觉风格；应优先通过补充 `core/designsystem` 通用组件或扩展既有页面母版来承接。
- 需要映射到 UI 的状态、金额、风险、同步结果等，应继续服从统一的颜色语义、状态标签和信息层级。
- Android 当前视觉真源以 Stitch 导出、`docs/spec/42-android-liquid-glass-ui-refactor-plan.md` 与 `master-goods-android/UI-DESIGN-SPEC.md` 为准；`docs/design-mockups/` 仅作历史参考。
