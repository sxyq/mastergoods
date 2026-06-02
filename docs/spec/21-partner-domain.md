# 21 往来单位域

## 需求表

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| customers | 需重构 | 客户画像更厚 | 客户主档 | 已补 `group_id/contact_name/contact_phone` 扩域位 | `/v2/customers` 待补接口和服务 |
| suppliers | 需重构 | 供应商画像更厚 | 供应商主档 | 已补 `group_id/contact_name/contact_phone` 扩域位 | `/v2/suppliers` 待补接口和服务 |
| partner_groups | 新版已做 | 旧版有分组 | 分组体系 | 已新增迁移与实体 | `/v2/customer-groups`、`/v2/supplier-groups` 待补 |
| partner_contacts | 新版已做 | 旧版有联系人 | 联系人结构化 | 已新增迁移与实体 | `/v2/customer-contacts`、`/v2/supplier-contacts` 待补 |
| partner_tags | 旧版存在新版未做 | 旧版有标签/分类暗示 | 标签体系 | 未做 | 需要补 |

## 第二阶段当前落点

- `customers` 新增：
  - `group_id`
  - `contact_name`
  - `contact_phone`
- `suppliers` 新增：
  - `group_id`
  - `contact_name`
  - `contact_phone`
- `partner_groups.partner_type` 固定限定为：
  - `customer`
  - `supplier`
- 当前阶段仍不进入：
  - partner tags
  - 价格等级/折扣策略
  - 更厚的运营标签体系
