# 24 财务域

## 需求表

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| finance_records | 需重构 | 旧版 funds 更完整 | 基础流水 | 已有基础结构，且 owner-aware 搜索已落地 | 当前关键字搜索字段为 `recordNo/category/notes`，仍需扩展到账户体系 |
| pay_orders owner 归属 | 新版已做 | 旧版无统一 owner | 付款单按 owner 隔离 | 已补 `owner_user_id`，并通过 service/repository 默认过滤 | 当前仍未进入账户体系 |
| /v2/pay-orders | 新版已做 | 旧版无 `/v2` 契约层 | 单据域首批新版接口 | 已落地列表、详情、创建、状态更新 | 当前仍是轻量付款模型 |
| accounts | 旧版存在新版未做 | 旧版有账户表 accts | 账户主数据 | 未做 | 需要补 |
| account_transfers | 旧版存在新版未做 | 旧版支持账户间流转 | 转账表 | 未做 | 需要补 |
| bill_fund_links | 旧版存在新版未做 | 旧版支持单据资金联动 | 关联表 | 未做 | 需要补 |
| cash_change_records | 旧版存在新版未做 | 旧版有找零/零钱语义 | 找零记录 | 未做 | 需要补 |
