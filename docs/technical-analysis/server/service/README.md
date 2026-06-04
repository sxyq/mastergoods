# Server service 模块分析

- 对应源码目录：`src/main/java/com/zhihuiji/backend/application/service`
- 当前服务数：20+
- 覆盖：认证、商品、客户、供应商、销售、采购、付款、财务、报表、同步、AI、管理端、演示数据，以及 `application/service/v2` 下的单据、同步、media/agent 首轮 facade

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
| 首版业务 Service 集合 | 新版已做 | 旧版没有当前服务端服务层 | 支撑当前 `/v1` 业务闭环 | 17 个 Service 已存在 | 当前安卓依赖这层 |
| owner-aware 事务边界 | 新版已做 | 旧版无统一 owner | 所有单据主链路都按 owner 处理 | 商品、客户、供应商、销售、采购、付款、财务、同步、报表、AI 已接入 `CurrentOwnerService` | admin/global 口径继续清理 |
| 会话访问缓存与黑名单 | 新版已做 | 旧版认证链每次直查 session 表，也没有失效 token 的短期黑名单 | 统一处理 access/refresh token 的缓存、过期判断与失效回收 | 已新增 `SessionAccessService`，并接入 `AuthService` 与 `TokenAuthenticationFilter` | 当前是单实例内存级策略，分布式部署后仍需扩展 |
| `/v2` 单据域 facade service | 新版已做 | 旧版无 `/v2` service 分层 | 新版控制器通过独立 facade 访问领域服务 | 已新增 `application/service/v2` 下销售/采购/付款 facade | 避免 `/v1` DTO 与 `/v2` DTO 混用 |
| `/v2` 商品与伙伴域服务 | 新版已做 | 旧版无 `/v2` 商品/伙伴服务层 | 第二阶段先建立商品、分类、单位、客户、供应商、分组、联系人服务 | 已新增 `V2ProductService`、`V2ProductCategoryService`、`V2ProductUnitService`、`V2CustomerService`、`V2SupplierService`、`V2PartnerGroupService`、`V2PartnerContactService` | `/v1` 基础 service 保持冻结兼容职责 |
| 商品第三阶段扩域服务 | 新版已做 | 旧版无价格层级与供应关系 service 分层 | 为 `/v2/products` 提供多价格与供应关系主逻辑 | 已新增 `V2ProductPriceLevelService`、`V2ProductSupplierRelationService`，并升级 `V2ProductService` 读写扩域字段 | 当前已补 service 单测 |
| 财务与库存底座扩域服务 | 新版已做 | 旧版无 `/v2` 财务/库存服务层 | 第四阶段建立账户、转账、单据资金关联、库存服务 | 已新增 `V2AccountService`、`V2AccountTransferService`、`V2BillFundLinkService`、`V2InventoryService`，并补了首轮 service 单测 | `/v1` 基础 service 保持冻结兼容职责 |
| 单据增强第五阶段服务 | 新版已做 | 旧版没有独立退货/收货与草稿确认服务分层 | 为销售退货、采购收货、草稿确认、付款账户联动提供 `/v2` facade 与事务边界 | 已新增 `V2SalesReturnService`、`V2PurchaseReceiptService`，并增强 `V2SaleOrderService`、`V2PayOrderService` | 本轮补强：`V2PayOrderService` 增加 `PAID` 幂等保护；`V2SalesReturnService` 禁止 0/负数量并校验可信 customer；`V2PurchaseReceiptService` 校验可信 supplier；对应 service test 已实跑通过 |
| B06 同步与导入服务 | 待验证 | 旧版没有 owner 私有 `/v2/sync` 与 server import job 服务 | 建立 owner 私有同步服务与导入任务编排服务 | 已新增 `V2SyncService`、`V2ImportJobService` | 本轮复核已确认 `/v2/sync` 已覆盖 `pay_order/bill_fund_link/sales_return/purchase_receipt/inventory_*` 的 pull，且 upload 仍有 pull-only 对象；`/v1/SyncService` 兼容链已补 `pay_order.account_id`；`V2SyncService` 已把 cursor 从纯时间戳升级为稳定 token，并收口为 `pull` 不自动推进、`ack` 才推进；`V2ImportJobService` 当前只允许 `failed/cancelled -> retry`、`pending/running -> cancel` |
| B07 媒体与 AI 扩域服务 | 待验证 | 旧版没有媒体附件与会话消息 `/v2` facade | 建立 owner 私有 media/agent 首轮服务与事务边界 | 已新增 `V2MediaService`、`V2AgentConversationService` | 当前已补 service 定向回归：验证 media 绑定去重、asset 删除级联、agent 会话摘要刷新与草稿 owner 引用；真实上传链与工作台联调仍待后续完成 |
| B07 Agent 会话更新与删除 | 待验证 | 旧版无会话更新/删除服务 | 为会话状态更新与级联删除提供事务边界 | `V2AgentConversationService` 已新增 `updateConversation()` 与 `deleteConversation()` | `updateConversation()` 强制 status 枚举约束 `[active, closed, archived]`，closed/archived 会话拒绝新消息；`deleteConversation()` 先显式删除消息/草稿再删除会话（应用层级联），V14 迁移的 `ON DELETE CASCADE` 作为兜底；草稿状态约束为 `[active, archived]`；B07 测试覆盖：`V2AgentConversationServiceTest` 3→12 测试，`V2AgentMediaControllerTest` 4→13 测试，`V2MediaServiceTest` 3→9 测试 |
| 联系人主摘要镜像 | 新版已做 | 旧版客户/供应商联系人摘要未沉淀到新版主档字段 | 联系人主联系人变化后自动刷新 `customers` / `suppliers` 摘要字段 | `V2PartnerContactService` 已在 create/update/delete/sync 路径同步 `contactName/contactPhone` | 已补 service 测试覆盖主联系人刷新行为 |
| 同步上传只覆盖部分实体类型 | 需重构 | 首版同步范围较窄 | 新版同步应覆盖扩域后的核心实体 | `/v2/sync` 首轮已覆盖核心实体；`/v1/sync` 仍保留首版窄合同 | 安卓迁移完成后再评估收口 |
| 销售订单态/采购订单态/库存统计等服务 | 旧版存在新版未做 | 旧版能力域更厚 | 新版服务层要超过旧版 | 账户/转账/单据资金关联/库存已补；销售订单态/采购订单态仍待补 | 会新增多个领域服务 |
