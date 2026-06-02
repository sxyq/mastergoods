# Server repository 模块分析

- 对应源码目录：`src/main/java/com/zhihuiji/backend/infrastructure/repository`
- 当前仓储数：22
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
| 聚合查询下推数据库 | 需重构 | 旧版统计逻辑更偏本地 | 新版报表、搜索、筛选更多走 DB 层 | 当前仍有不少服务层聚合空间 | 后续按领域逐步下推 |
| 账户、库存快照、媒体等新仓储 | 旧版存在新版未做 | 旧版表域更厚 | 新版仓储数量最终应超过旧版核心域 | 当前尚未覆盖这些新表 | 等实体层扩域 |
