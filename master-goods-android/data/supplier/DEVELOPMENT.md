# data/supplier 模块开发说明

- 当前状态：SupplierRepository 已实现，采用在线优先 + Room 本地缓存观察。
- 实际源码目录：`data/supplier/src/main/java/com/zhihuiji/data/supplier`
- 目标：封装供应商列表、状态过滤、增删改。

## 需要创建的类

- `SupplierRepository`

## 需要实现的关键函数

- `observeSuppliers(keyword: String, status: Int?): Flow<List<SupplierDto>>`
- `refreshSuppliers(keyword: String?, status: Int?)`
- `getSupplier(id: Long): SupplierDto`
- `createSupplier(draft: SupplierDto): SupplierDto`
- `updateSupplier(id: Long, draft: SupplierDto): SupplierDto`
- `deleteSupplier(id: Long)`

## 验收标准

- 支持“全部/启用/停用”状态切换和关键字搜索。

## UI 设计规范支撑

- 供应商列表需要提供联系人、脱敏手机号、应付余额和状态。
- 供应商详情需要提供信用额度、可用额度、采购汇总和往来记录入口。
