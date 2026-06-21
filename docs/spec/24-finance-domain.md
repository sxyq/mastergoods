# 24 财务域

## 需求表

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| finance_records | 需重构 | 旧版 funds 更完整 | 基础流水 | 已有基础结构，且 owner-aware 搜索已落地 | 当前关键字搜索字段为 `recordNo/category/notes`，仍需扩展到账户体系 |
| pay_orders owner 归属 | 新版已做 | 旧版无统一 owner | 付款单按 owner 隔离 | 已补 `owner_user_id`，并通过 service/repository 默认过滤 | B05 已增强账户关联；本轮补强 `create(status=PAID)` 与 `updateStatus(..., PAID)` 的幂等保护，避免重复扣账户余额或重复写 `bill_fund_link` |
| /v2/pay-orders | 新版已做 | 旧版无 `/v2` 契约层 | 单据域首批新版接口 | 已落地列表、详情、创建、状态更新 | 当前仍是轻量付款模型 |
| accounts | 新版已做 | 旧版有账户表 accts | 账户主数据 | `/v2/accounts` 已落地：CRUD + owner 过滤 + 编码唯一；创建/更新请求已拆分，更新不再提交 `balance` | 证据：`V2AccountController.java`，`V2FinanceDtos.java`，V10迁移 |
| account_transfers | 新版已做 | 旧版支持账户间流转 | 转账表 | `/v2/account-transfers` 已落地：创建+列表+详情，自动更新余额，转账单号 owner 内唯一，冲突重试后失败会返回业务异常 | 证据：`V2AccountTransferController.java`，`V2AccountTransferService.java`，V10迁移 |
| bill_fund_links | 新版已做 | 旧版支持单据资金联动 | 关联表 | `/v2/bill-fund-links` 已落地：创建+按单据查+按账户查+删除 | 本轮补了 `BillFundLinkRepository.findFirstByOwnerUserIdAndBillTypeAndBillIdAndLinkType(...)`，供 `pay_order` 支付态幂等去重与回滚使用 |
| cash_change_records | 新版已做 | 旧版有找零/零钱语义 | 找零记录 | `/v2/cash-change-records` 已落地列表、详情、创建、删除；创建时校验应收/实收并回写关联账户余额，删除时回滚账户侧影响 | 当前为首版闭环，仍未补 `bill_fund_links` 自动联动与端侧接入 |
