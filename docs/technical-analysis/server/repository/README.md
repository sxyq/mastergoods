# Server repository 模块分析

- 对应源码目录：`src/main/java/com/zhihuiji/backend/infrastructure/repository`
- 当前仓储数：30
- 覆盖：用户、会话、商品、客户、供应商、销售、采购、付款、库存调整、财务、同步、AI 任务与通知

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
| 首版 JPA Repository 集合 | 新版已做 | 旧版本地库没有这一层 | 支撑当前 `/v1` 服务层查询 | 当前 22 个 Repository 已存在 | 能支撑现阶段联调 |
| owner-aware 查询方法 | 新版已做 | 旧版无统一 owner | 所有核心单据与主数据查询默认带 owner 条件 | 商品、客户、供应商、销售、采购、付款、财务、库存、同步、AI 仓储已新增 owner-aware 方法 | admin/global 统计仍需继续收口 |
| `/v2` 单据域复用 owner-aware Repository | 新版已做 | 旧版无 `/v2` | `/v2` 首批路由只依赖 owner-aware 查询 | 已通过销售/采购/付款三域的 facade service 复用 | 不再 fallback 到全局查询 |
| `FinanceRecordRepository.search` 关键字查询 | 新版已做 | 首版关键字条件存在字段漂移风险 | 关键字搜索保持 owner 过滤且字段命名与实体一致 | 当前按 `recordNo/category/notes` 搜索，并默认带 `ownerUserId` | 已修正历史 `remark` 命名偏差 |
| 商品与伙伴扩域仓储 | 新版已做 | 旧版这些主数据表域更厚 | 第二阶段先补分类、单位、分组、联系人仓储 | 已新增 `ProductCategoryRepository`、`ProductUnitRepository`、`PartnerGroupRepository`、`PartnerContactRepository` | 为 `/v2` 商品和伙伴域提供 owner-aware 基础 |
| 商品价格与供应关系仓储 | 新版已做 | 旧版商品域在价格和供应关系上更厚 | 第三阶段先补价格层级与商品-供应商关系仓储 | 已新增 `ProductPriceLevelRepository`、`ProductSupplierRelationRepository`，并补 `ProductCategoryRepository`、`ProductUnitRepository`、`SupplierRepository` 的 owner-aware 批量查询入口 | 为 `/v2/products` 扩域读写打底 |
| 财务与库存底座扩域仓储 | 新版已做 | 旧版表域更厚 | 第四阶段补账户、转账、单据资金关联、找零、库存账本、快照、月统计仓储 | 已新增 `AccountRepository`、`AccountTransferRepository`、`BillFundLinkRepository`、`CashChangeRecordRepository`、`InventoryLedgerRepository`、`InventorySnapshotRepository`、`InventoryMonthlyStatsRepository` | 为 `/v2` 财务和库存域提供 owner-aware 基础 |
| 单据增强第五阶段仓储 | 新版已做 | 旧版有退货/收货态表 | 第五阶段补销售退货与采购收货仓储 | 已新增 `SalesReturnRepository`、`SalesReturnItemRepository`、`PurchaseReceiptRepository`、`PurchaseReceiptItemRepository`，并保持 owner-aware 查询 | 为 `/v2/sales-returns` 与 `/v2/purchase-receipts` 提供基础 |
| `BillFundLinkRepository` 幂等查重入口 | 新版已做 | 首版只有普通列表/按单据查询 | 为 `pay_order` 支付态去重和回滚提供精确定位 | 已新增 `findFirstByOwnerUserIdAndBillTypeAndBillIdAndLinkType(...)` | 本轮用于防止重复 `PAID` 重复扣款/重复建 link |
| B06 同步与导入仓储 | 新版已做 | 旧版无 owner 私有 sync/import 仓储模型 | 为 `/v2/sync` 与 import job 提供 owner-aware 查询基础 | `SyncCursorRepository` 已按 `ownerUserId + clientId` 工作，`ImportJobRepository` 已落地，相关 item/master 仓储已补批量与 owner 查询方法 | 首轮 upload/pull 依赖这批方法；cursor 内容现允许稳定 token 语义，不再局限纯时间戳 |
| B07 Agent 会话级联删除仓储 | 待验证 | 旧版无会话级联删除方法 | 为 `DELETE /v2/agent/conversations/{id}` 提供 owner-aware 级联删除基础 | `AgentMessageRepository` 已新增 `deleteAllByOwnerUserIdAndConversationId()`，`AgentDraftRepository` 已新增 `deleteAllByOwnerUserIdAndConversationId()` | 配合 V14 迁移的 `ON DELETE CASCADE` 双重保障：应用层先显式删除消息/草稿，数据库层外键级联作为兜底 |
| 聚合查询下推数据库 | 需重构 | 旧版统计逻辑更偏本地 | 新版报表、搜索、筛选更多走 DB 层 | 当前仍有不少服务层聚合空间 | 后续按领域逐步下推 |
| 账户、库存快照、媒体等新仓储 | 旧版存在新版未做 | 旧版表域更厚 | 新版仓储数量最终应超过旧版核心域 | 账户、转账、单据资金关联、找零、库存账本、快照、月统计已补；媒体等仍待补 | 等实体层扩域 |
