# 23 采购域

## 需求表

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| purchase_orders | 需重构 | 旧版采购过程字段更细 | 采购单据主表 | 已有基础结构 | 需增强 |
| purchase_order_items | 需重构 | 旧版采购明细更完整 | 采购明细表 | 已有基础结构 | 需增强 |
| purchase_orders owner 归属 | 新版已做 | 旧版无统一 owner | 采购单主表与明细表全部 owner 隔离 | 已补 `owner_user_id`，并通过 service/repository 默认过滤 | 历史数据已回填 |
| /v2/purchase-orders | 新版已做 | 旧版无 `/v2` 契约层 | 单据域首批新版接口 | 已落地列表、详情、创建 | 仍未拆订单态/入库态 |
| purchase_receipts | 新版已做 | 旧版有订单/入库流转 | 采购入库态 | 已落地 `purchase_receipts` + `purchase_receipt_items`，`/v2/purchase-receipts` 列表/详情/创建 | 本轮补强：`supplierId` 必须 owner 内存在，`purchaseOrderId` 与供应商一致性已校验，并改为优先落 trusted supplier 信息；对应 service test 已实跑通过 |
| pay_orders | 需重构 | 旧版应付/付款联动更细 | 采购付款表 | 已有基础结构 | B05 已增强账户关联；本轮进一步补了 `PAID` 状态幂等保护，重复置为 `PAID` 不再重复扣余额/重复写 `bill_fund_link`，回退非 `PAID` 时优先按已存在 link 回滚账户 |
