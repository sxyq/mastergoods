# 25 库存域

## 需求表

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| inventory_adjustments | 需重构 | 旧版有库存流水/快照/月结 | 库存调整 | 基础调整 | 需要扩展 |
| inventory_ledger | 新版已做 | 旧版库存流转更完整 | 库存流水表 | `/v2/inventory/ledger` 已落地：创建+按商品查+按时间查，且本轮已补齐真实的 `GET /v2/inventory/ledger/by-source?source_type=&source_id=` 路由；自动更新商品库存 | 不再只在文档层宣称“按来源查” |
| inventory_snapshots | 新版已做 | 旧版支持快照 | 库存快照表 | `/v2/inventory/snapshots` 已落地：创建+按日期查+按范围查，幂等（同 owner+商品+日期覆盖），并补数据库级唯一约束 | 证据：`V2InventoryController.java`，`V2InventoryService.java`，V10迁移 |
| inventory_monthly_stats | 新版已做 | 旧版支持月统计 | 月统计表 | `/v2/inventory/monthly-stats` 已落地：按年月查询，并补 owner+商品+年月数据库级唯一约束 | 证据：`V2InventoryController.java`，V10迁移 |
