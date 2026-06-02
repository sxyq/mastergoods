# feature/customers 模块开发说明

- 当前状态：列表页+编辑页+详情页已完成，业务链路已走通。
- 实际源码目录：`feature/customers/src/main/java/com/zhihuiji/feature/customers`
- 目标：实现客户列表和客户编辑。

## 需要创建的类

- `CustomerListScreen`
- `CustomerEditorScreen`
- `CustomerViewModel`

## 需要实现的关键函数

- `CustomerViewModel.loadCustomers(keyword: String = "")`
- `CustomerViewModel.loadCustomer(id: Long)`
- `CustomerViewModel.saveCustomer()`
- `CustomerViewModel.deleteCustomer(id: Long)`
- `CustomerViewModel.validateForm()`

## 验收标准

- 客户搜索、新建、编辑、删除闭环完整。

## UI 设计规范

- 对照设计图 `04.png` 的客户列表页和客户详情页实现（来源见 `docs/design-mockups`）。
- 列表页顶部大标题“智慧记”，下方使用“客户/联系人”Tab、搜索框、状态筛选和筛选图标。
- 客户卡片显示名称、编码、联系人、脱敏手机号、应收余额和状态，欠款金额使用红色。
- 详情页顶部主卡展示客户图标、名称、编码、状态、联系人和地址。
- 详情汇总用三列或三卡展示应收余额、信用额度、可用额度。
- 底部固定操作为“联系客户”“新增销售单”“收款”，主操作按钮为蓝色。
