# Android data/customer 模块分析

- 对应源码目录：`master-goods-android/data/customer`
- 关键源码：`CustomerRepository.kt`

## 模块定位

`data/customer` 当前负责基础客户档案。  
新版里，它会逐步并入更完整的 partner 领域能力，支持：

- 客户基础档案
- 联系人
- 分组
- 标签
- 价格等级与折扣策略
- owner 私有客户视图

## 状态图例

- `新版已做`
- `新版待做`
- `旧版存在新版未做`
- `新版需要去掉`
- `需重构`
- `待验证`

## 状态表

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| 客户列表/详情/编辑仓储 | 新版已做 | 旧版客户模型更厚 | 支撑当前客户业务链 | `CustomerRepository.kt` 已实现 | 已联通列表/编辑/详情页 |
| 客户分组/联系人/价格等级/标签 | 旧版存在新版未做 | 旧版 `companies` 有更多画像字段 | 新版补齐更强客户运营能力 | 当前仓储只覆盖基础客户字段 | 后端扩域后跟进 |
| owner 过滤与 `/v2` DTO | 需重构 | 旧版无统一 owner | 所有客户查询按 owner 和 `/v2` 契约执行 | 后端已具备 `/v2/customers`、`/v2/customer-groups`、`/v2/customer-contacts`，当前安卓仍主要消费 `/v1` | 下一步转 `core/model/v2/partner` |
