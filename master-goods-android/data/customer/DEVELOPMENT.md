# data/customer 模块开发说明

- 当前状态：`CustomerRepository` 已实现 `/v1` 兼容链路；`CustomerV2Repository` 已实现 `/v2` 客户、分组、联系人首轮承接。
- 实际源码目录：`data/customer/src/main/java/com/zhihuiji/data/customer`
- 目标：封装客户列表、详情、增删改。

## 需要创建的类

- `CustomerRepository`
- `CustomerV2Repository`

## 需要实现的关键函数

- `observeCustomers(keyword: String): Flow<List<CustomerDto>>`
- `refreshCustomers(keyword: String?)`
- `getCustomer(id: Long): CustomerDto`
- `createCustomer(draft: CustomerDto): CustomerDto`
- `updateCustomer(id: Long, draft: CustomerDto): CustomerDto`
- `deleteCustomer(id: Long)`
- `/v2`：
  - `listCustomers(keyword, status, groupId)`
  - `listGroups()/createGroup()/updateGroup()/deleteGroup()`
  - `listContacts(customerId)/createContact()/updateContact()/deleteContact()`

## 验收标准

- 搜索和编辑客户时能直接复用统一仓储接口。

## UI 设计规范支撑

- 客户列表需要提供联系人、脱敏手机号、应收余额和状态。
- 客户详情需要提供额度、可用额度、交易汇总和往来记录入口。

## UI 联动约束

- 本模块虽然不直接负责页面绘制，但其输出的数据结构、状态枚举、错误语义和交互支撑能力必须服务于统一的 Android UI 基线。
- 后续新增业务不能倒逼页面切换成另一套视觉风格；应优先通过补充 `core/designsystem` 通用组件或扩展既有页面母版来承接。
- 需要映射到 UI 的状态、金额、风险、同步结果等，应继续服从统一的颜色语义、状态标签和信息层级。
- Android 视觉真源固定为 `docs/design-mockups/01.png ~ 08.png` 与 `master-goods-android/UI-DESIGN-SPEC.md`。
