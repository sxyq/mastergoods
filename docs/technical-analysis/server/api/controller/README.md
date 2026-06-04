# Server api/controller 模块分析

- 对应源码目录：`src/main/java/com/zhihuiji/backend/api/controller`
- 当前控制器：
  - `/v1`: `Admin / Agent / AgentTask / Auth / Customer / FinanceRecord / PayOrder / Product / PurchaseOrder / Report / SaleOrder / Supplier / Sync`
  - `/v2`: `V2SaleOrder / V2SalesReturn / V2PurchaseOrder / V2PurchaseReceipt / V2PayOrder / V2Product / V2ProductCategory / V2ProductUnit / V2ProductPriceLevel / V2ProductSupplierRelation / V2Customer / V2Supplier / V2CustomerGroup / V2SupplierGroup / V2CustomerContact / V2SupplierContact / V2Account / V2AccountTransfer / V2BillFundLink / V2Inventory / V2Sync / V2ImportJob`

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
| `/v1` 业务控制器集合 | 新版已做 | 旧版无当前服务端控制器层 | 保持现有兼容接口 | 13 个控制器已存在 | 继续服务当前安卓版本 |
| `/v2` 分域控制器 | 新版已做 | 旧版无 `/v2` | 新增 products/partners/sales/purchase/finance/inventory/media/agent/sync 等新版路由 | 已建立 `/v2` 控制器目录，并落地 sale/purchase/pay/product/partner 首批路由与商品第三阶段扩域路由，以及财务/库存第四阶段扩域路由 | 媒体等后续继续补齐 |
| Entity 直接作为请求体 | 新版需要去掉 | 首版为提速做过简化 | 创建/更新改成专用 request DTO | 当前部分控制器仍保留此模式 | 属于后端首批重构项 |
| owner 归属过滤 | 新版已做 | 旧版无统一 owner | 所有列表/详情/统计按当前 owner 过滤 | 当前单据相关 controller 已完全下沉到 owner-aware service | 非单据域仍需继续清理 |
| 商品第三阶段控制器 | 新版已做 | 旧版无价格层级与供应关系路由 | 暴露价格层级 CRUD、供应关系 CRUD 与扩域商品读写 | `V2ProductPriceLevelController`、`V2ProductSupplierRelationController` 已落地，`V2ProductController` 已升级返回多价格和供应关系 | `/v1/products` 保持冻结 |
| 财务与库存第四阶段控制器 | 新版已做 | 旧版无 `/v2` 财务/库存路由 | 暴露账户、转账、单据资金关联、库存读写 | `V2AccountController`、`V2AccountTransferController`、`V2BillFundLinkController`、`V2InventoryController` 已落地 | `/v1` 财务/库存保持冻结 |
| 单据增强第五阶段控制器 | 新版已做 | 旧版无独立退货/收货和草稿确认路由 | 暴露销售退货、采购收货、草稿确认与付款增强入口 | `V2SalesReturnController`、`V2PurchaseReceiptController` 已落地，`V2SaleOrderController` 已补 confirm，`V2PayOrderController` 已支持 `account_id` 账户关联 | 本轮同步收口：退货/收货入参约束已收紧，付款 `PAID` 状态改为幂等语义 |
| inventory by-source 路由 | 新版已做 | 文档曾先于代码声明“按来源查” | 显式暴露库存账本按来源查询入口 | `GET /v2/inventory/ledger/by-source?source_type=&source_id=` 已补到 `V2InventoryController` | 解决 B04 文档与真实 API 不一致问题 |
| B06 同步与导入控制器 | 新版已做 | 旧版没有 owner 私有 sync/import 新合同 | 暴露 `/v2/sync/*` 与 `/v2/import-jobs/*` | `V2SyncController`、`V2ImportJobController` 已落地 | `/v1/sync` 保留兼容职责；`/v2/sync` cursor 字段现按 opaque token 返回，且服务端游标推进以 `POST /v2/sync/cursor/ack` 为准；`/v2/import-jobs/{id}/retry` 仅允许失败/已取消任务，`/cancel` 仅允许未完成任务 |
| B07 Agent 会话更新与删除控制器 | 待验证 | 旧版无会话更新/删除路由 | 暴露会话状态更新与级联删除入口 | `V2AgentController` 已新增 `PUT /v2/agent/conversations/{id}` 与 `DELETE /v2/agent/conversations/{id}` | PUT 用于更新会话状态（active/closed/archived），DELETE 级联删除会话下所有消息与草稿；closed/archived 状态会话拒绝新消息 |
