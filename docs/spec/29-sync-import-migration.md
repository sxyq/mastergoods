# 29 同步与迁移

## 需求表

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| `sync_cursors` owner 复合键 | 新版已做 | 旧版按全局 client 维度 | owner_user_id + client_id | `SyncCursorEntity` / `SyncCursorId` + `V2SyncService` 已按 owner+client 落地 | `/v1` 兼容层仍保留旧合同 |
| `/v2/sync/cursor/*` | 新版已做 | 旧版没有显式 cursor 读取/确认接口 | 支持读取与确认 owner 私有同步基线 | 已新增 `GET /v2/sync/cursor/{clientId}` 与 `POST /v2/sync/cursor/ack` | 当前 cursor 已升级为 opaque token 语义，兼容旧数值 cursor 输入；实际推进语义以 `ack` 为准 |
| `/v2/sync/pull` | 新版已做 | 旧版 pull 范围窄且不含 owner 私有客户端语义 | 覆盖新版核心主数据与单据域 pull 语义 | 已覆盖商品/伙伴/单据/财务/库存核心实体的首轮下发 | B06 复核已确认：包含 `pay_order`、`bill_fund_link`、`sales_return(_item)`、`purchase_receipt(_item)`、`inventory_ledger`、`inventory_snapshot`、`inventory_monthly_stats`；并补稳定 cursor，避免同一 `updated_at` 跨页漏数；`pull` 只返回 `next_cursor`，不提前改写服务端游标 |
| `/v2/sync/upload` | 待验证 | 旧版上传范围更窄 | 覆盖新版可写核心领域模型 | 已支持主数据、单据、财务、库存调整等可写实体的首轮上传 | B06 复核已确认：`pay_order`、`bill_fund_link`、`sales_return(_item)`、`purchase_receipt(_item)` 可上传；`inventory_ledger/snapshot/monthly_stats` 当前仍是 pull-only 设计 |
| `/v1/sync/*` 兼容层 | 需重构 | 旧版合同仍被当前安卓兼容层使用 | 逐步降级为兼容入口 | 当前仍存在首版 `SyncService` + `/v1/sync/*` | 安卓迁移完成后再评估收口 |
| `/v1/sync/pay_order` 兼容补强 | 新版已做 | 首版 payload 未携带付款账户 | 兼容层最少保证 `pay_order.account_id` 不丢失 | `SyncService` 的 pull/upload 已补 `account_id` 读写 | 这是 B05 对 B06 数据正确性的最小修复，不代表 `/v1/sync` 已完成整体重构 |
| `import_jobs` 任务模型 | 新版已做 | 旧版只有离线迁移脚本，没有 server 侧导入任务实体 | 可追踪、可重试、可审计的导入任务记录 | 已新增 `import_jobs`、`ImportJobEntity`、`ImportJobRepository`、`V2ImportJobService` | 首轮先落任务模型与接口，不直接做大迁移执行器 |
| `/v2/import-jobs/*` | 新版已做 | 旧版无统一导入任务接口 | 提供创建、查询、重试、取消 | 已新增 `/v2/import-jobs`、`/{id}`、`/{id}/retry`、`/{id}/cancel` | idempotency_key 以 owner 维度去重；当前状态流收口为 `pending/running -> cancel`、`failed/cancelled -> retry` |
| import executor / replay worker | 新版待做 | 旧版无 server 侧执行器 | 后台执行导入、记录心跳、回放游标、写入 summary | 当前未实现后台 worker | B06 首轮只完成任务模型，不冒进引入执行器 |
| 服务器导入与客户端职责切分 | 新版已做 | 旧版无此边界 | 客户端只提交任务/轮询状态，服务端负责导入编排 | 当前文档与 `/v2/import-jobs/*` 已明确边界 | 安卓 UI/Repository 后续再接入 |

## 当前首轮结论

1. B06 首轮已经把同步边界从“全局 client 思维”收口为 `owner_user_id + client_id`。
2. `/v2/sync/*` 已成为新版同步合同入口，`/v1/sync/*` 只保留兼容职责；当前推进顺序应是 `pull -> 本地应用 -> ack`。
3. `import_jobs` 与 `/v2/import-jobs/*` 已把导入任务的状态流、重试、取消、幂等键和审计字段立住，且当前只允许对失败/已取消任务执行 retry。
4. 这轮没有实现后台导入执行器，也没有改安卓导入 UI，因此 B06 整体状态仍应视为“首轮落地，待联调验证”。
5. B04/B05 审计修复后，已补 `pay_order.account_id` 的 `/v1/sync` 兼容映射，避免付款账户信息在兼容同步链路中继续漂移。
6. `next_cursor` / `last_cursor` 不应再被客户端当成纯时间戳；它现在是稳定分页 token，客户端只能原样保存、原样回传。
