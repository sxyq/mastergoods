# data/customer 模块开发说明

- 当前状态：CustomerRepository 已实现，采用在线优先 + Room 本地缓存观察。
- 实际源码目录：`data/customer/src/main/java/com/zhihuiji/data/customer`
- 目标：封装客户列表、详情、增删改。

## 需要创建的类

- `CustomerRepository`

## 需要实现的关键函数

- `observeCustomers(keyword: String): Flow<List<CustomerDto>>`
- `refreshCustomers(keyword: String?)`
- `getCustomer(id: Long): CustomerDto`
- `createCustomer(draft: CustomerDto): CustomerDto`
- `updateCustomer(id: Long, draft: CustomerDto): CustomerDto`
- `deleteCustomer(id: Long)`

## 验收标准

- 搜索和编辑客户时能直接复用统一仓储接口。

## UI 设计规范支撑

- 客户列表需要提供联系人、脱敏手机号、应收余额和状态。
- 客户详情需要提供额度、可用额度、交易汇总和往来记录入口。
