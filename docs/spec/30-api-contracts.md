# 30 API 契约

## 分层

| 层 | 状态 | 说明 |
|---|---|---|
| /v1 | 新版已做 | 兼容层，保留现有行为 |
| /v2 | 新版已做 | 新版正式契约层已建立首批单据域入口，其他领域继续补齐 |

## 新版接口组

| 接口组 | 状态 | 备注 |
|---|---|---|
| auth | 新版待做 | /v2/auth/* |
| products | 新版已做 | `/v2/products/*`、`/v2/product-categories/*`、`/v2/product-units/*`、`/v2/product-price-levels/*`、`/v2/product-supplier-relations/*` 已落地第二、三阶段首批接口 |
| partners | 新版已做 | `/v2/customers/*`、`/v2/suppliers/*`、`/v2/customer-groups/*`、`/v2/supplier-groups/*`、`/v2/customer-contacts/*`、`/v2/supplier-contacts/*` 已落地 |
| sales | 新版已做 | `/v2/sale-orders/*` 首批已落地，`/v2/sales-returns/*` 第五阶段已落地，草稿增强已落地 |
| purchase | 新版已做 | `/v2/purchase-orders/*` 首批已落地，`/v2/purchase-receipts/*` 第五阶段已落地 |
| pay-orders | 新版已做 | `/v2/pay-orders/*` 首批已落地，B05 已增强账户关联（`account_id` + 余额扣减 + `bill_fund_link`），本轮补齐 `PAID` 幂等保护 |
| finance | 新版已做 | `/v2/accounts/*`、`/v2/account-transfers/*`、`/v2/bill-fund-links/*` 已落地，账户 create/update 请求已拆分，cash_change_records 待补；Android `@Query` 参数名已验证与后端 `@RequestParam` 一致（camelCase，后端未显式写 name，依赖 `-parameters` 编译保留） |
| inventory | 新版已做 | `/v2/inventory/ledger`、`/v2/inventory/ledger/by-source`（显式 `source_type`/`source_id`）、`/v2/inventory/snapshots`、`/v2/inventory/monthly-stats` 已落地；Android `@Query` 参数名已验证与后端 `@RequestParam` 一致（`/ledger` 和 `/snapshots` 为 camelCase，`/by-source` 为 snake_case） |
| media | 待验证 | `/v2/media/assets/*`、`/v2/media/bindings/*` 已落地首轮合同，仍待真实上传链与客户端联调；V14 迁移已补 `ON DELETE CASCADE`，会话删除时关联绑定自动级联 |
| agent | 待验证 | `/v2/agent/conversations/*` 已补 `PUT /v2/agent/conversations/{id}`（更新标题/状态）与 `DELETE /v2/agent/conversations/{id}`（级联删除消息与草稿）；`/v2/agent/conversations/{conversationId}/messages`、`/v2/agent/drafts/*` 已落地首轮合同；会话状态枚举约束为 `[active, closed, archived]`，草稿状态枚举约束为 `[active, archived]`；`closed/archived` 会话拒绝新消息写入；推荐结果缓存等后续继续扩展 |
| sync | 待验证 | `/v2/sync/*`、`/v2/import-jobs/*` 已落地首轮合同，仍待客户端联调 |

## 第一阶段已落地

| 对象 | 状态 | 说明 |
|---|---|---|
| `/v2/sale-orders` | 新版已做 | 已提供列表、详情、创建、草稿更新、收款、状态更新、取消 |
| `/v2/purchase-orders` | 新版已做 | 已提供列表、详情、创建 |
| `/v2/pay-orders` | 新版已做 | 已提供列表、详情、创建、状态更新 |
| `/v2` DTO 命名空间 | 新版已做 | 已新增 `api/dto/v2/sales|purchase|pay` |
| `/v2` 领域 facade service | 新版已做 | 已新增 `application/service/v2/*`，底层复用 owner-aware 领域服务 |

## 第二阶段已落地

| 对象 | 状态 | 说明 |
|---|---|---|
| `/v2/products` | 新版已做 | 已提供列表、详情、创建、更新、删除、低库存 |
| `/v2/product-categories` | 新版已做 | 已提供列表、创建、更新、删除 |
| `/v2/product-units` | 新版已做 | 已提供列表、创建、更新、删除 |
| `/v2/customers` | 新版已做 | 已提供列表、详情、创建、更新、删除 |
| `/v2/suppliers` | 新版已做 | 已提供列表、详情、创建、更新、删除 |
| `/v2/customer-groups` / `/v2/supplier-groups` | 新版已做 | 已提供分组列表、创建、更新、删除 |
| `/v2/customer-contacts` / `/v2/supplier-contacts` | 新版已做 | 已提供联系人列表、创建、更新、删除 |
| `/v2` DTO 命名空间扩展 | 新版已做 | 已新增 `api/dto/v2/product`、`api/dto/v2/partner` |

## 第三阶段已落地

| 对象 | 状态 | 说明 |
|---|---|---|
| `/v2/product-price-levels` | 新版已做 | 已提供列表、创建、更新、删除 |
| `/v2/product-supplier-relations` | 新版已做 | 已提供按商品列表、创建、更新、删除 |
| `/v2/products` 扩域读模型 | 新版已做 | 商品详情/列表已可返回 `price_levels`、`default_supplier`、`supplier_relations` |
| `/v2/products` 扩域写模型 | 新版已做 | 商品创建/更新已支持写入 `price_levels`、`supplier_relations` |
| `/v1/products` 冻结兼容 | 新版已做 | `/v1` 不新增多价格、供应关系字段，继续服务当前安卓兼容层 |

## 第四阶段已落地

| 对象 | 状态 | 说明 |
|---|---|---|
| `/v2/accounts` | 新版已做 | 已提供列表、详情、创建、更新、删除；owner 过滤 + 编码唯一；创建/更新请求已拆分 |
| `/v2/account-transfers` | 新版已做 | 已提供列表、详情、创建；自动更新转出/转入账户余额，转账单号冲突有重试与业务异常兜底 |
| `/v2/bill-fund-links` | 新版已做 | 已提供按单据查、按账户查、创建、删除 |
| `/v2/inventory/ledger` | 新版已做 | 已提供按商品查、按时间查、创建；自动更新商品库存 |
| `/v2/inventory/snapshots` | 新版已做 | 已提供按日期查、按范围查、创建；幂等覆盖，并补数据库级唯一约束 |
| `/v2/inventory/monthly-stats` | 新版已做 | 已提供按年月查询，并补数据库级唯一约束 |
| `/v2` DTO 命名空间扩展 | 新版已做 | 已新增 `api/dto/v2/finance`、`api/dto/v2/inventory` |

## 第五阶段已落地

| 对象 | 状态 | 说明 |
|---|---|---|
| `/v2/sales-returns` | 新版已做 | 已提供列表、详情、创建；独立退货单 `sales_returns` + `sales_return_items`，创建时显式拒绝 0/负数量，并校验 owner 内 customer 与 original order 一致性 |
| `/v2/sale-orders` 草稿增强 | 新版已做 | `OrderStatus` 新增 CONFIRMED(3)，草稿更新支持明细编辑，新增 confirm 端点 |
| `/v2/purchase-receipts` | 新版已做 | 已提供列表、详情、创建；采购收货 `purchase_receipts` + `purchase_receipt_items`，创建时校验 owner 内 supplier，且与 `purchaseOrderId` 保持一致 |
| `/v2/pay-orders` 账户关联增强 | 新版已做 | `PayOrderEntity` 新增 `account_id`，付款时扣减账户余额并创建 `bill_fund_link`；本轮已补 `create(status=PAID)` 与重复 `PAID` 的幂等保护 |
| `/v2` DTO 命名空间扩展 | 新版已做 | 已在 `api/dto/v2/sales` 新增退货 DTO，在 `api/dto/v2/purchase` 新增收货 DTO |

## 第六阶段已落地（首轮）

| 对象 | 状态 | 说明 |
|---|---|---|
| `/v2/sync/health` | 新版已做 | 返回 owner-scoped 同步状态与支持的实体类型清单 |
| `/v2/sync/cursor/{clientId}` | 新版已做 | 可读取 owner+client 维度同步基线；返回值现为 opaque cursor token |
| `/v2/sync/cursor/ack` | 新版已做 | 可显式确认并推进 owner+client 游标；兼容旧数值 cursor 输入；这是服务端持久推进的唯一入口 |
| `/v2/sync/pull` | 新版已做 | 已覆盖商品/伙伴/单据/财务/库存核心实体的首轮 pull 语义；`next_cursor` 已升级为稳定分页 token，避免同时间戳跨页漏数；`pull` 本身不自动提交 cursor |
| `/v2/sync/upload` | 待验证 | 已支持可写核心实体的首轮 upload，仍待客户端顺序与冲突联调；`inventory_ledger/snapshot/monthly_stats` 当前不在 uploadable 清单内 |
| `/v2/import-jobs` | 新版已做 | 已提供列表、详情、创建 |
| `/v2/import-jobs/{id}/retry` | 新版已做 | 已支持失败/取消任务重试，并记录重试次数与 replay_cursor |
| `/v2/import-jobs/{id}/cancel` | 新版已做 | 已支持取消未完成导入任务；已失败/已取消/已成功任务不能再次 cancel |

## 第七阶段已落地（首轮）

| 对象 | 状态 | 说明 |
|---|---|---|
| `/v2/media/assets` | 待验证 | 已提供列表、详情、创建、删除；owner 内 `object_key` 唯一，当前由客户端提供存储元数据 |
| `/v2/media/bindings` | 待验证 | 已提供按目标列表、创建、删除；owner 内同一 `asset_id + target_type + target_id` 去重 |
| `/v2/agent/conversations` | 待验证 | 已提供列表、详情、创建、更新（PUT）、删除（DELETE）；按 owner 维度维护标题、状态、摘要、最近消息时间；状态枚举约束为 `[active, closed, archived]`；删除时级联删除消息与草稿（服务级 + DB 级 `ON DELETE CASCADE`） |
| `/v2/agent/conversations/{conversationId}/messages` | 待验证 | 已提供列表、创建；追加消息时自动刷新会话摘要与最后消息时间；`closed/archived` 状态会话拒绝新消息写入 |
| `/v2/agent/drafts` | 待验证 | 已提供列表、创建、更新、删除；conversation 引用保持 owner 内一致性；草稿状态枚举约束为 `[active, archived]` |
| `/v2` DTO 命名空间扩展 | 待验证 | 已新增 `api/dto/v2/media`、`api/dto/v2/agent` |
