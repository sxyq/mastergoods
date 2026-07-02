# 40 批次开发总控文档

## 文档职责

本文件是当前项目从**后端重构**到**安卓数据层迁移**再到**安卓 UI 重构与联调验收**的唯一批次总控文档。

后续执行规则固定如下：

1. 每完成一个批次、一个子项、或一组强相关代码改动，必须先更新对应 spec / technical-analysis 文档，再回写本文件中的状态。
2. 本文件必须始终反映：
   - 当前已完成批次
   - 当前进行中批次
   - 当前剩余批次
   - 已知文档不一致点
   - 每批次的验收标准
3. 如果某项能力代码已经完成，但本文件未打标，则视为**文档未完成**。
4. 会员体系当前不纳入新版，统一标记为 `新版需要去掉`，不得在后端与安卓重构批次中偷偷恢复。

## 六态标记

- `新版已做`
- `新版待做`
- `旧版存在新版未做`
- `新版需要去掉`
- `需重构`
- `待验证`

## 已审阅范围

本轮已按目录逐个核对 `docs/` 下的文本类文档，当前审阅基线为 **72 个文本文档**。

### 已逐个审阅的文本文档

1. `docs/README.md`
2. `docs/android-kingdee-data-migration.md`
3. `docs/android-security-hardening-audit.md`
4. `docs/design-mockups/README.md`
5. `docs/spec/42-android-liquid-glass-ui-refactor-plan.md`
6. `docs/spec/00-product-overview.md`
7. `docs/spec/01-status-taxonomy.md`
8. `docs/spec/02-domain-model-overview.md`
9. `docs/spec/10-auth-and-tenant.md`
10. `docs/spec/20-product-domain.md`
11. `docs/spec/21-partner-domain.md`
12. `docs/spec/22-sales-domain.md`
13. `docs/spec/23-purchase-domain.md`
14. `docs/spec/24-finance-domain.md`
15. `docs/spec/25-inventory-domain.md`
16. `docs/spec/26-membership-domain.md`
17. `docs/spec/27-media-attachments-domain.md`
18. `docs/spec/28-agent-domain.md`
19. `docs/spec/29-sync-import-migration.md`
20. `docs/spec/30-api-contracts.md`
21. `docs/spec/31-android-impact.md`
22. `docs/spec/32-rollout-and-compatibility.md`
23. `docs/technical-analysis/CHANGELOG.md`
24. `docs/technical-analysis/INDEX.md`
25. `docs/technical-analysis/android/README.md`
26. `docs/technical-analysis/android/app/README.md`
27. `docs/technical-analysis/android/backdrop/README.md`
28. `docs/technical-analysis/android/core/README.md`
29. `docs/technical-analysis/android/core/common/README.md`
30. `docs/technical-analysis/android/core/database/README.md`
31. `docs/technical-analysis/android/core/datastore/README.md`
32. `docs/technical-analysis/android/core/designsystem/README.md`
33. `docs/technical-analysis/android/core/model/README.md`
34. `docs/technical-analysis/android/core/network/README.md`
35. `docs/technical-analysis/android/data/README.md`
36. `docs/technical-analysis/android/data/agent/README.md`
37. `docs/technical-analysis/android/data/auth/README.md`
38. `docs/technical-analysis/android/data/customer/README.md`
39. `docs/technical-analysis/android/data/finance/README.md`
40. `docs/technical-analysis/android/data/order/README.md`
41. `docs/technical-analysis/android/data/product/README.md`
42. `docs/technical-analysis/android/data/report/README.md`
43. `docs/technical-analysis/android/data/supplier/README.md`
44. `docs/technical-analysis/android/data/sync/README.md`
45. `docs/technical-analysis/android/feature/README.md`
46. `docs/technical-analysis/android/feature/agent/README.md`
47. `docs/technical-analysis/android/feature/auth/README.md`
48. `docs/technical-analysis/android/feature/customers/README.md`
49. `docs/technical-analysis/android/feature/dashboard/README.md`
50. `docs/technical-analysis/android/feature/finance/README.md`
51. `docs/technical-analysis/android/feature/payments/README.md`
52. `docs/technical-analysis/android/feature/products/README.md`
53. `docs/technical-analysis/android/feature/purchases/README.md`
54. `docs/technical-analysis/android/feature/reports/README.md`
55. `docs/technical-analysis/android/feature/sales/README.md`
56. `docs/technical-analysis/android/feature/settings/README.md`
57. `docs/technical-analysis/android/feature/suppliers/README.md`
58. `docs/technical-analysis/server/README.md`
59. `docs/technical-analysis/server/api/README.md`
60. `docs/technical-analysis/server/api/common/README.md`
61. `docs/technical-analysis/server/api/controller/README.md`
62. `docs/technical-analysis/server/api/dto/README.md`
63. `docs/technical-analysis/server/entity/README.md`
64. `docs/technical-analysis/server/infrastructure/README.md`
65. `docs/technical-analysis/server/infrastructure/ai/README.md`
66. `docs/technical-analysis/server/infrastructure/config/README.md`
67. `docs/technical-analysis/server/infrastructure/security/README.md`
68. `docs/technical-analysis/server/repository/README.md`
69. `docs/technical-analysis/server/resources/README.md`
70. `docs/technical-analysis/server/service/README.md`
71. `docs/spec/40-batch-development-master-plan.md`
72. `docs/technical-analysis/android/data/media/README.md`
72. `docs/spec/41-b11-acceptance-matrix.md`

### UI 真源目录说明

当前 Android UI 设计输入分为两层：

- 当前正式真源：
  - `docs/spec/42-android-liquid-glass-ui-refactor-plan.md`
  - `stitch_exports/visual-design_system_framework_14840154594131085259/manifest.tsv`
- 历史参考：
  - `docs/design-mockups/README.md`

说明：

- `docs/design-mockups/` 已降级为历史参考，不再单独作为当前 Android UI 的唯一视觉真源。
- 旧的 `01.png ~ 08.png` 已从工作树移除，避免继续作为当前 UI 输入。
- 本批次不把 Stitch PNG 逐张视觉比对结果写入此文档；实际 UI 逐页贴合按 `B10` 与专项 UI 计划执行。

## 当前基线

| 批次 | 状态 | 当前结论 |
|---|---|---|
| B00 文档一致性收口 | 新版已做 | 4处文档漂移已收口：partner spec、domain overview、server README、server/api README |
| B01 账号隔离底座 | 新版已做 | owner 底座、历史回填、`/v1` 与首批 `/v2` 的边界已经建立 |
| B02 商品域与伙伴域首批扩域 | 新版已做 | 分类、单位、分组、联系人等第一批扩域已落地 |
| B03 商品多价格与供应关系扩域 | 新版已做 | 价格层级、商品供应关系与 `/v2/products` 扩域返回已落地 |
| B04 财务与库存底座扩域 | 新版已做 | 账户、转账、单据资金关联、库存账本、快照、月统计已落地；审计指出的 inventory by-source 漂移已用真实 API 收口，定向回归已通过 |
| B05 单据域增强 | 新版已做 | 销售草稿确认、独立退货、采购收货、付款账户联动已落地；本轮已补 `pay_order` 幂等、退货数量校验、伙伴一致性校验，定向回归已通过 |
| B06 同步与导入链路 | 待验证 | `/v2/sync/*` 与 `/v2/import-jobs/*` 首轮已落地；本轮已复核受 B05 影响的数据对象，并补 ` /v1/sync pay_order.account_id ` 兼容映射；2026-06-30 又补拿到本地 `sync health -> upload -> pull -> ack -> 实体回查` 与 `import-jobs pending -> succeeded` 真实闭环证据 |
| B07 媒体与 AI 扩域 | 待验证 | media/agent 后端首轮合同已落地，已补会话更新/删除、状态约束与级联删除；2026-06-30 又补拿到媒体上传/绑定/内容读取真闭环，且把超限误报 `500` 收口为 `413`；AI 当前已验证无 provider 时的真实工具查询退化语义，但真实 provider 流式联调仍缺 |
| B08 安卓 core/data `/v2` 迁移 | 待验证 | `core/model/v2`、`ZhihuijiV2Api` 与 `data/*V2Repository` 已覆盖 `product/partner/order/finance/inventory/sync-import/agent/media`；本地 owner-aware Room 扩域缓存仍待后续推进 |
| B09 安卓 feature `/v2` 迁移 | 待验证 | 商品、伙伴、单据、财务、报表、助手、设置 feature 层已切到 /v2 数据链；本地编译与 V2 契约测试通过；本轮又补 Web 侧实体 ID 精度收口与 Android 网络修复后的本地构建复验，并在真机上完成了本地 HTTPS 冷启动登录、首页和 `单据 / 档案 / 报表 / 助手` 一级页签 smoke，但更深层业务编辑、同步与上传链路仍待验证 |
| B10 安卓 UI 重构与设计稿贴合 | 待验证 | 已按 B09 UI 审核修复商品详情路径、报表时间区间真实刷新、容器壳页顶栏母版统一，以及档案/单据/财务列表的胶囊主操作；本轮继续把 Agent 工作台/任务/通知/草稿页的假数据占位收口为明确待联调空态，并修正草稿页 tab/计数/展示不一致；同时把 dashboard/reports 的误导性本地聚合语义进一步降级为诚实态说明；最新真机已补登录页、首页、旧状态权限页与一级页签 smoke 证据，但 dashboard/reports 视觉贴合与更多业务页逐页截图核对仍未完成 |
| B11 联调、测试、性能、安全、发布验收 | 待验证 | 验收矩阵、证据目录约定与本地复验脚本入口已建立；Android/Web 定向本地自动化与构建在当前工作树上仍通过，本轮新增 `20260630-1508-android-device-local-https-home-smoke.md`、`20260630-1512-backend-recovery-summary.md`、`20260630-1552-sync-import-media-ai-local-validation.md`、`20260630-1556-android-device-home-tabs-gfx-mem.md` 与对应日志后，后端 `backend-smoke` / `bootJar`、本地 `sync/import/media` 真闭环、AI 无 provider 时的真实退化语义、真机识别、冷启动登录首页、四个一级页签 smoke、以及一份真机基础性能采样都已拿到当天强证据；`20260630-1338-124-154-readonly-status.md` 也已只读确认当前真实生产拓扑为 `124` 公网边缘 + `154` 应用主机，且后端容器在线。但真机实际同步/上传/AI 深链路、重场景性能与生产环境发布级现场证据仍缺，故不能按全部完成记账 |

## B00 已处理记录

以下 4 项已在 `B00` 中完成收口，保留为处理记录，后续无需再按“待处理”理解：

| 文档 | 状态 | 问题 | 处理方向 |
|---|---|---|---|
| `docs/spec/21-partner-domain.md` | 新版已做 | 已修正：v2伙伴域接口改为“已落地” | ✅ B00 已收口 |
| `docs/spec/02-domain-model-overview.md` | 新版已做 | 已补齐批次顺序、扩域结果与单据域 `/v2` 契约描述 | ✅ B00 已收口 |
| `docs/technical-analysis/server/README.md` | 新版已做 | 已修正：/v2和owner改为"新版已做" | ✅ B00 已收口 |
| `docs/technical-analysis/server/api/README.md` | 新版已做 | 已修正：/v2和owner过滤改为"新版已做" | ✅ B00 已收口 |

## 总目标

从当前“可运行的首版系统”升级为“结构上可持续扩展、领域能力明确超过旧版、文档能直接驱动研发”的新版系统。

总路线分三层：

1. 后端先完成 owner 私有边界与 `/v2` 主领域收口。
2. 安卓再完成 `core/model`、`core/network`、`data/*` 到 `feature/*` 的 `/v2` 迁移。
3. 最后做 UI 重构、设计稿贴合、真机联调、性能和发布验收。

## 全局剩余总览

| 领域 | 状态 | 当前差距 | 备注 |
|---|---|---|---|
| auth / tenant | 新版已做 | 仍需文档收口与联调验证 | owner 底座已存在 |
| product | 需重构 | 媒体、多单位换算等仍缺 | 第三阶段只补到价格层级和供应关系 |
| partner | 需重构 | tags、价格策略、厚画像仍缺 | 第二阶段只补到分组和联系人 |
| sales | 新版已做 | 草稿确认、独立退货已落地；更厚订单态分层仍待后续扩域 | B05 已落地核心增强 |
| purchase | 新版已做 | 采购收货、付款账户联动已落地；更厚订单态仍待后续扩域 | B05 已落地核心增强 |
| finance | 新版已做 | 账户、转账、单据资金关联已落地；找零待补 | B04 已落地核心底座 |
| inventory | 新版已做 | 账本、快照、月统计已落地 | B04 已落地核心底座 |
| media | 待验证 | `/v2/media/assets/*`、`/v2/media/bindings/*` 首轮已落地；V14 迁移已补 `ON DELETE CASCADE`；真实上传链与更多业务挂接仍待联调 | 与安卓上传链路联动 |
| agent | 待验证 | 会话、消息、草稿首轮已落地；已补会话更新/删除（PUT/DELETE）、状态约束（`[active, closed, archived]`/`[active, archived]`）、`closed/archived` 拒绝新消息、级联删除（服务级 + DB 级 `ON DELETE CASCADE`）；推荐结果缓存等扩展仍待后续批次推进 | 当前已具备工作台 + `/v2/agent` 首轮能力 |
| sync / import | 待验证 | `/v2/sync/*` 与 `import_jobs` 首轮已落地，执行器与安卓接入未完成 | 不应再按全局语义推进 |
| android data migration | 待验证 | `product/partner/order/finance/inventory/sync-import/agent/media` 的 `/v2` model/network/repository 首轮承接已落地；owner-aware Room 扩域缓存与更完整本地数据链仍待推进 | 当前仍保留 `/v1` 兼容层与后续本地缓存扩域空间 |
| android UI | 新版待做 | 与设计稿仍有明显差距 | 必须放在数据链稳定之后 |
| membership | 新版需要去掉 | 当前阶段不纳入 | 恢复需单独立项 |

## 批次总表

| 批次 | 名称 | 状态 | 目标 | 主要联动文档 |
|---|---|---|---|---|
| B00 | 文档一致性收口 | 新版已做 | 收口现有 spec 与 technical-analysis 漂移 | `00`、`02`、`20`、`21`、`30`、`31`、`32`、server/android technical-analysis |
| B01 | 账号隔离底座 | 新版已做 | owner 私有边界、历史回填、`/v1` 兼容过滤 | `02`、`10`、`30`、`32` |
| B02 | 商品域与伙伴域首批扩域 | 新版已做 | 分类、单位、分组、联系人 | `20`、`21`、`30`、`31`、`32` |
| B03 | 商品多价格与供应关系扩域 | 新版已做 | 多价格、默认供应商、供应关系 | `20`、`30`、`31`、`32` |
| B04 | 财务与库存底座扩域 | 新版已做 | 账户、转账、账本、快照、月统计 | `24`、`25`、`30`、`31`、`32` |
| B05 | 单据域增强 | 新版已做 | 销售草稿确认、退货、采购收货、付款账户联动增强 | `22`、`23`、`24`、`30`、`31`、`32` |
| B06 | 同步与导入链路 | 待验证 | owner 私有同步、导入任务、迁移编排 | `29`、`30`、`31`、`32` |
| B07 | 媒体与 AI 扩域 | 待验证 | 媒体附件域、AI 会话/消息/草稿首轮合同 + 会话更新/删除、状态约束与级联删除 | `27`、`28`、`30`、`31`、`32` |
| B08 | 安卓 core/data `/v2` 迁移 | 待验证 | `product/partner/order/finance/inventory/sync-import/agent/media` 的 `core/model`、`core/network`、`data/*V2Repository` 首轮切到 `/v2` | `31`、android `core/*`、`data/*` |
| B09 | 安卓 feature `/v2` 迁移 | 待验证 | 商品、伙伴、单据、财务、报表、助手逐域切换 | `31`、android `feature/*` |
| B10 | 安卓 UI 重构与设计稿贴合 | 待验证 | 严格按设计稿完成主壳、页面、图表、交互与视觉修整 | `31`、`design-mockups/README.md`、android `feature/*`、`core/designsystem` |
| B11 | 联调、测试、性能、安全、发布验收 | 待验证 | 后端、安卓、真机、当前生产拓扑统一验收；当前已补齐后端 smoke / bootJar 最新通过证据、真机本地 HTTPS 登录首页与一级页签 smoke 证据，并已只读确认 `124`/`154` 在线，但同步/导入、媒体/AI 深链路、性能与发布级证据仍缺 | `32`、`41`、测试报告、发布文档 |

## 批次详细规划

### B00 文档一致性收口

| 事项 | 状态 | 说明 | 完成标准 | 联动文档 |
|---|---|---|---|---|
| 收口 partner 域 spec 口径 | 新版已做 | 把已落地 `/v2` 能力从“待补”改成真实状态 | `21-partner-domain.md` 与代码一致 | `docs/spec/21-partner-domain.md` |
| 收口 domain overview | 新版已做 | 补齐第三阶段商品扩域结果 | `02-domain-model-overview.md` 不再遗漏价格层级与供应关系 | `docs/spec/02-domain-model-overview.md` |
| 收口 server 高层文档 | 新版已做 | 修复 `/v2` 尚未开始的陈旧表述 | `server/README.md`、`server/api/README.md` 与代码一致 | `docs/technical-analysis/server/README.md`、`docs/technical-analysis/server/api/README.md` |
| 回填本总控文档的批次日志 | 新版已做 | 把 B00 的处理结果和当前基线收口到这里 | B00 有清晰结论、处理记录与打标 | 本文件 |

### B01 账号隔离底座

| 事项 | 状态 | 说明 | 完成标准 | 联动文档 |
|---|---|---|---|---|
| 核心业务表补 `owner_user_id` | 新版已做 | 账号隔离底座已落地 | 业务查询与写入不再按全局共享语义运行 | `02`、`10`、`30`、server `entity/repository/service` |
| 历史数据回填默认 owner | 新版已做 | 保留用户承接旧全局数据 | 历史数据不再长期空 owner | `02`、`10`、`32` |
| `/v1` 兼容层按 owner 过滤 | 新版已做 | 兼容期行为统一到 owner 边界 | 登录用户不再看到其他 owner 数据 | `30`、`32` |

### B02 商品域与伙伴域首批扩域

| 事项 | 状态 | 说明 | 完成标准 | 联动文档 |
|---|---|---|---|---|
| `product_categories` | 新版已做 | 商品分类 owner 私有表 | `/v2/product-categories/*` 可用 | `20`、`30`、server docs |
| `product_units` | 新版已做 | 商品单位 owner 私有表 | `/v2/product-units/*` 可用 | `20`、`30`、server docs |
| `partner_groups` | 新版已做 | 客户/供应商分组 owner 私有表 | `/v2/customer-groups/*`、`/v2/supplier-groups/*` 可用 | `21`、`30`、server docs |
| `partner_contacts` | 新版已做 | 客户/供应商联系人 owner 私有表 | `/v2/customer-contacts/*`、`/v2/supplier-contacts/*` 可用 | `21`、`30`、server docs |

### B03 商品多价格与供应关系扩域

| 事项 | 状态 | 说明 | 完成标准 | 联动文档 |
|---|---|---|---|---|
| `product_price_levels` | 新版已做 | owner 私有价格层级表 | `/v2/product-price-levels/*` 可用 | `20`、`30`、server docs |
| `product_supplier_relations` | 新版已做 | 商品与供应商关系表 | `/v2/product-supplier-relations/*` 可用 | `20`、`30`、server docs |
| `/v2/products` 扩域读模型 | 新版已做 | 返回 `price_levels/default_supplier/supplier_relations` | 安卓后续可直接基于该结构设计新模型 | `20`、`30`、`31` |

### B04 财务与库存底座扩域

| 事项 | 状态 | 说明 | 完成标准 | 联动文档 |
|---|---|---|---|---|
| 账户主数据 `accounts` | 新版已做 | 取代轻量方法枚举 | `/v2/accounts` CRUD + owner 过滤 + 编码唯一；create/update 请求已拆分 | `24`、`30`、`31`、server docs |
| 账户转账 `account_transfers` | 新版已做 | 建立账户间转账能力 | `/v2/account-transfers` 创建+列表+详情，owner 内唯一 + 冲突重试与业务异常兜底 | `24`、`30`、`31` |
| 单据资金关联 | 新版已做 | 明确单据与收付、找零、账户变动关系 | `/v2/bill-fund-links` 创建+按单据查+按账户查+删除 | `24`、`30`、`31` |
| 库存账本 `inventory_ledger` | 新版已做 | 建立可追溯库存流水 | `/v2/inventory/ledger` 创建+按商品查+按时间查+按来源查 | `25`、`30`、`31` |
| 库存快照与月统计 | 新版已做 | 支撑报表和同步 | `/v2/inventory/snapshots` + `/v2/inventory/monthly-stats`，并补数据库级唯一约束 | `25`、`30`、`31` |

### B05 单据域增强

| 事项 | 状态 | 说明 | 完成标准 | 联动文档 |
|---|---|---|---|---|
| 销售草稿/预订单 | 新版已做 | 旧版订单态更厚 | `/v2/sale-orders` 已补草稿更新与 confirm 端点 | 以等价增强方式落地，后续如需独立 drafts 路由再扩域 | `22`、`30`、`31` |
| 销售退货 | 新版已做 | 不再用负数语义替代退货单 | 独立退货模型与 API 可用 | `sales_returns`、`sales_return_items` 与 `/v2/sales-returns` 已落地 | `22`、`24`、`30` |
| 采购订单/收货分态 | 新版已做 | 从“采购单闭环”升级为多态流转 | 采购订单与收货/入库分离 | `purchase_receipts`、`purchase_receipt_items` 与 `/v2/purchase-receipts` 已落地 | `23`、`30`、`31` |
| 付款增强 | 新版已做 | 付款状态、账户联动、来源追踪增强 | pay order 与 finance/account 能闭环 | `PayOrderEntity.account_id`、余额扣减与 `bill_fund_link` 已落地；本轮已补 `PAID` 幂等保护与回滚保护 | `23`、`24`、`30` |

### B06 同步与导入链路

| 事项 | 状态 | 说明 | 完成标准 | 联动文档 |
|---|---|---|---|---|
| owner 私有同步 | 新版已做 | 同步不再按全局客户端语义运行 | pull/upload/cursor 全部 owner 化 | `V2SyncService`、`V2SyncController` 与 `sync_cursors(owner_user_id + client_id)` 已落地首轮，并补稳定 cursor token 以避免同时间戳跨页漏数；当前已收口为 `pull -> 本地应用 -> ack` 才推进服务端游标 | `29`、`30`、`31` |
| 导入任务编排 | 待验证 | 旧数据导入放在结构稳定后推进 | 有导入任务表、状态流与回放策略 | `import_jobs`、`V2ImportJobService`、`/v2/import-jobs/*` 已落地首轮；后台执行器未补 | 当前状态流已收口为 `failed/cancelled -> retry`、`pending/running -> cancel` | `29`、`30`、`32` |
| 服务器导入与客户端职责切分 | 新版已做 | 避免安卓端直接承担过重迁移职责 | 导入边界与验收标准清楚 | 当前约束为“安卓提交任务/轮询状态，服务端负责编排与回放” | `29`、`31`、`32` |

### B07 媒体与 AI 扩域

| 事项 | 状态 | 说明 | 完成标准 | 联动文档 |
|---|---|---|---|---|
| 商品/单据媒体附件域 | 待验证 | 商品图片、附件、上传元数据 | `/v2/media/assets/*`、`/v2/media/bindings/*` 可用 | 已落地 `media_assets/media_bindings`、对应 entity/repository/service/controller，并通过定向回归；V14 迁移已补 `ON DELETE CASCADE` | `27`、`30`、`31` |
| AI 会话与消息模型 | 待验证 | 当前 AI 还偏首版工作台结构 | 会话、消息、草稿、推荐结果缓存有明确模型 | 已落地 `agent_conversations/agent_messages/agent_drafts`、`V2AgentConversationService` 与 `/v2/agent/*` 首轮合同，并通过定向回归；已新增 `PUT /v2/agent/conversations/{id}`（更新标题/状态）与 `DELETE /v2/agent/conversations/{id}`（级联删除消息与草稿）；会话状态约束 `[active, closed, archived]`，草稿状态约束 `[active, archived]`；`closed/archived` 会话拒绝新消息；已增 `AgentConversationUpdateRequest` DTO、`AgentMessageRepository.deleteAllByOwnerUserIdAndConversationId`、`AgentDraftRepository.deleteAllByOwnerUserIdAndConversationId`；已移除废弃 DTO（`AgentMessageListResponse`、`MediaBindingListResponse`） | `28`、`30`、`31` |
| owner 私有 AI 上下文 | 待验证 | AI 不应越过账号边界 | owner 上下文在会话、任务、通知中统一生效 | 首轮会话/消息/草稿查询与写入均已走 `CurrentOwnerService`，推荐结果缓存仍待后续扩展 | `28`、`30`、`31` |

### B08 安卓 core/data `/v2` 迁移

| 事项 | 状态 | 说明 | 完成标准 | 联动文档 |
|---|---|---|---|---|
| `core/model/v2/*` | 待验证 | 不在旧 `/v1` 模型上无限加字段 | `/v1` 与 `/v2` 模型并存且边界清晰 | 已新增 `core/model/v2/{product,partner,order,finance,inventory,sync,agent,media}` 并通过 `:core:model:compileDebugKotlin`；B08 修复：5 个 Filter 类已补齐 `@Serializable` + `@SerialName`，序列化测试已覆盖 | `31`、android `core/model` |
| `core/network` `/v2` 契约 | 待验证 | 与 server `api/dto/v2/*` 对齐 | Retrofit 或等价接口完成分组 | `ZhihuijiV2Api` 已覆盖 `product/partner/order/finance/inventory/sync-import/agent/media`，并通过 `ZhihuijiV2ApiContractTest`；B08 修复：agent/media 方法名统一 `V2` 后缀，`@Query` 参数名已验证与后端一致并加注释，契约测试已对 agent/media 全量端点做 HTTP 方法、路径与关键 query 参数断言 | `31`、android `core/network` |
| `core/database` owner 与扩域缓存 | 新版待做 | 支撑 `/v2` 主档与扩域表本地缓存 | Room schema 与 owner 语义一致 | `31`、android `core/database` |
| `data/*` Repository 迁移 | 待验证 | 围绕 `/v2` 与 owner 私有语义改造 | `data/product`、`data/customer`、`data:supplier`、`data/order`、`data/finance`、`data/sync` 等完成首轮迁移 | 已新增 `ProductV2Repository`、`CustomerV2Repository`、`SupplierV2Repository`、`SaleOrderV2Repository`、`SalesReturnV2Repository`、`PurchaseOrderV2Repository`、`PurchaseReceiptV2Repository`、`PayOrderV2Repository`、`FinanceV2Repository`、`SyncV2Repository`，并完成对应模块编译；B08 修复：`AgentV2Repository.deleteDraft()`、`MediaV2Repository.deleteAsset()`/`deleteBinding()` 已改用 `safeApiUnitCall`，`AgentV2RepositoryTest` / `FinanceV2RepositoryTest` 已直接调用真实 Repository 方法验证 API 委派链路 | `31`、android `data/*` |

### B09 安卓 feature `/v2` 迁移

| 事项 | 状态 | 说明 | 完成标准 | 联动文档 |
|---|---|---|---|---|
| 商品域 feature 迁移 | 待验证 | 接入分类、单位、多价格、供应关系；本轮已补商品“列表 -> 详情 -> 编辑”导航链，避免继续用编辑页代替详情页 | 商品列表、详情、编辑链路完成 `/v2` 切换 | `31`、android `feature/products` |
| 伙伴域 feature 迁移 | 待验证 | 接入分组、联系人与扩域读模型 | 客户/供应商页完成 `/v2` 切换 | `31`、android `feature/customers`、`feature/suppliers` |
| 单据域 feature 迁移 | 待验证 | 跟随 B05 的销售/采购/付款增强 | 销售/采购/付款页改成新版流程 | `31`、android `feature/sales`、`feature/purchases`、`feature/payments` |
| 财务/报表/助手/设置迁移 | 待验证 | 适配账户、报表聚合、同步与导入任务 | feature 层职责按新版后端重排 | `31`、android `feature/finance`、`feature/reports`、`feature/agent`、`feature/settings` |

### B10 安卓 UI 重构与设计稿贴合

| 事项 | 状态 | 说明 | 完成标准 | 联动文档 |
|---|---|---|---|---|
| 主壳与导航视觉收口 | 待验证 | 顶级单据/档案列表不再用顶部普通新增图标替代主操作，子列表嵌入五栏主壳时仍保留右下蓝色 FAB | 真机与设计稿主结构一致 | `31`、`design-mockups/README.md`、android `app`/`core/designsystem` |
| 仪表盘与报表视觉重构 | 待验证 | 图表、统计卡片、密度、层级仍需继续重做；本轮已先修复报表时间区间 Tab 只改展示、不驱动真实数据刷新的问题，但报表母版与视觉贴合仍未收口 | 首页与报表页达到设计稿级别 | `31`、android `feature/dashboard`、`feature/reports` |
| 档案与单据编辑视觉重构 | 待验证 | 销售状态 Tab 已收口到真实 `/v2` 状态（全部/草稿/已完成/已取消/已确认）；客户状态标签已改为客户语义（正常/欠款/已停用）；商品缺货状态色修正；feature 主按钮调用已由废弃 `PrimaryGradientButton` 收口到 `PrimaryButton` | 商品/客户/供应商/单据编辑页达到设计稿级别 | `31`、android `feature/*` |
| AI 助手视觉与流程贴合 | 待验证 | `43-ai-assistant-requirements.md` 已升级为 v20 需求 / 审查基线，并新增 `AI_AGENT_P0_EVIDENCE_MATRIX.md` 把 AGT-P0-001..019 映射到现有接口、Android 设备、锁屏阻塞和性能证据；当前 Android 已补“首段回答前 pending result_block 不抢占主内容”和短暂真实工具提示口径，但真实 provider `model_stream`、取消端到端、生产 profile、性能和逐屏 UI 证据仍为 partial | Agent 域页面达到设计稿级别，并通过真实 provider / 真机 / 审计证据证明 ChatGPT-like agent 体验 | `43`、`31`、android `feature/agent`、`docs/acceptance-evidence/ai-agent/AI_AGENT_P0_EVIDENCE_MATRIX.md` |
| 字体、间距、反馈、动效修整 | 待验证 | 设置页同步类型由单行右侧文本改成可换行展示，降低 `/v2` 长字段导致的溢出风险 | 全局统一视觉与交互反馈 | `31`、android `core/designsystem`、`feature/*` |

### B11 联调、测试、性能、安全、发布验收

| 事项 | 状态 | 说明 | 完成标准 | 联动文档 |
|---|---|---|---|---|
| B11 验收矩阵与复验入口 | 新版已做 | 建立统一矩阵、证据目录、脚本化命令入口 | `docs/spec/41-b11-acceptance-matrix.md` 与 `tools/b11_acceptance_check.sh` 存在，后续 agent 可按矩阵复验 | `32`、`41`、server/android technical-analysis |
| 后端 `/v2` 覆盖测试 | 新版已做 | controller / service / migration / compatibility 都要有本轮真实命令输出 | 已通过 `tools/b11_acceptance_check.sh backend-smoke`，证据见 `docs/acceptance-evidence/b11/backend/20260603-1709-backend-smoke.md` | `32`、`41`、server technical-analysis |
| 安卓 `/v2` 编译/契约测试 | 新版已做 | model/network/repository 契约与 `assembleDebug` 都要跑 | 已通过 `tools/b11_acceptance_check.sh android-contract` 与 `android-assemble`，证据见 `docs/acceptance-evidence/b11/android/` | `32`、`41`、android technical-analysis |
| 安卓 `/v2` 真机联调 | 待验证 | 当前生产拓扑、登录、业务链、导入、同步、报表、助手都要跑；当前已推进到“本机可识别真机、冷启动登录可进首页、四个一级页签 smoke 通过” | 有可复验的测试记录和截图；更深层业务链仍待继续补齐 | `32`、`41`、android technical-analysis |
| 性能与稳定性 | 待验证 | 列表、图表、同步、图片上传与大单据要压测 | 有 CPU、内存、帧率、接口时延记录 | `32`、`41`、测试报告 |
| 安全与发布 | 待验证 | owner 边界、安全头、签名、混淆、日志控制、上线清单 | 可进入发布候选状态 | `32`、`41`、安全与发布文档 |

## 更新规则

后续每一轮开发都必须同时做下面三件事：

1. 更新对应源码。
2. 更新被影响的 spec / technical-analysis 文档。
3. 更新本文件：
   - `当前基线`
   - `批次总表`
   - 对应批次的详细表
   - 如有必要，补一条批次日志

## 批次日志模板

后续每完成一批，按下面格式在本文件底部追加：

| 日期 | 批次 | 状态变更 | 代码范围 | 文档范围 | 备注 |
|---|---|---|---|---|---|
| `2026-06-02` | `B00` | `新版待做 -> 新版已做` | 无代码改动 | `21-partner-domain.md`、`02-domain-model-overview.md`、`server/README.md`、`server/api/README.md`、`40-batch-development-master-plan.md` | 4处文档漂移收口：partner spec /v2 待补→已落地，domain overview 重排批次顺序并补齐扩域结果，server README /v2 尚未开始→已落地，server/api README 无v2包→14个Controller+5个DTO包 |
| `2026-06-02` | `B04` | `新版已做 -> 新版已做（补强）` | `src/main/resources/db/migration/V10__finance_and_inventory_foundation.sql`、`src/main/java/com/zhihuiji/backend/domain/entity/*`、`src/main/java/com/zhihuiji/backend/api/dto/v2/finance/V2FinanceDtos.java`、`src/main/java/com/zhihuiji/backend/application/service/v2/*`、`src/test/java/com/zhihuiji/backend/*` | `24-finance-domain.md`、`25-inventory-domain.md`、`30-api-contracts.md`、`40-batch-development-master-plan.md`、server technical-analysis | 根据审核补齐：转账唯一约束与重试逻辑、库存快照/月统计唯一约束、账户 create/update DTO 拆分、迁移测试/服务测试/controller 测试/v1 兼容测试 |
| `2026-06-02` | `B05` | `新版待做 -> 新版已做` | `V11__bill_domain_enhancement.sql`、`V2SalesReturn*`、`V2PurchaseReceipt*`、`V2SaleOrderService/Controller`、`V2PayOrderService`、B05 相关测试 | `22-sales-domain.md`、`23-purchase-domain.md`、`24-finance-domain.md`、`30-api-contracts.md`、`31-android-impact.md`、`40-batch-development-master-plan.md`、server/android technical-analysis | 单据增强收口：销售草稿确认、独立退货、采购收货、付款账户联动已落地，并补 controller/migration 测试证据 |
| `2026-06-02` | `B06` | `新版待做 -> 待验证` | `V12__sync_and_import_owner_upgrade.sql`、`ImportJobEntity`、`ImportJobRepository`、`V2SyncService`、`V2ImportJobService`、`V2SyncController`、`V2ImportJobController`、相关 DTO/Repository` | `29-sync-import-migration.md`、`30-api-contracts.md`、`31-android-impact.md`、`32-rollout-and-compatibility.md`、`40-batch-development-master-plan.md`、server/android technical-analysis | 首轮 owner 私有 `/v2/sync/*` 与 `import_jobs` 已落地；本轮补了稳定 cursor token 以避免跨页漏数，并修正为 `pull` 不自动推进游标、仅 `ack` 推进；已用本机 Gradle 8.7 + JDK 21 跑通 `V2SyncServiceTest`、`V2ImportJobServiceTest`、`V2SyncImportControllerTest` |
| `2026-06-03` | `B07/B08` | `新版待做 -> 待验证` | `src/main/java/com/zhihuiji/backend/api/controller/v2/V2MediaController.java`、`src/main/java/com/zhihuiji/backend/api/controller/v2/V2AgentController.java`、`src/main/java/com/zhihuiji/backend/application/service/v2/V2MediaService.java`、`src/main/java/com/zhihuiji/backend/application/service/v2/V2AgentConversationService.java`、`src/test/java/com/zhihuiji/backend/application/service/v2/V2MediaServiceTest.java`、`src/test/java/com/zhihuiji/backend/application/service/v2/V2AgentConversationServiceTest.java`、`src/test/java/com/zhihuiji/backend/api/controller/V2AgentMediaControllerTest.java`、`master-goods-android/core/model/src/main/java/com/zhihuiji/core/model/v2/*`、`master-goods-android/core/network/src/main/java/com/zhihuiji/core/network/ZhihuijiV2Api.kt`、`master-goods-android/core/network/src/test/java/com/zhihuiji/core/network/ZhihuijiV2ApiContractTest.kt`、`master-goods-android/data/agent/src/main/java/com/zhihuiji/data/agent/*V2Repository.kt` | `27-media-attachments-domain.md`、`28-agent-domain.md`、`30-api-contracts.md`、`31-android-impact.md`、`32-rollout-and-compatibility.md`、`40-batch-development-master-plan.md`、server/android technical-analysis | 收口 B07/B08 首轮真实状态：media/agent 后端合同已落地并通过定向回归；Android 已落地 `agent/media` 的 `/v2` model/network/repository 承接，并在本机 Gradle 环境完成契约/编译验证 |
| `2026-06-03` | `B08` | `待验证 -> 待验证（扩域补齐）` | `master-goods-android/core/model/src/main/java/com/zhihuiji/core/model/v2/{product,partner,order,finance,inventory,sync}/*`、`master-goods-android/core/network/src/main/java/com/zhihuiji/core/network/ZhihuijiV2Api.kt`、`master-goods-android/core/network/src/main/java/com/zhihuiji/core/network/SafeApiCall.kt`、`master-goods-android/core/network/src/test/java/com/zhihuiji/core/network/ZhihuijiV2ApiContractTest.kt`、`master-goods-android/data/{product,customer,supplier,order,finance,sync}/src/main/java/com/zhihuiji/data/**/*V2Repository.kt` | `31-android-impact.md`、`32-rollout-and-compatibility.md`、`40-batch-development-master-plan.md`、android technical-analysis、对应 `data/*/DEVELOPMENT.md` | B08 剩余 core/data `/v2` 承接已补齐到 `product/partner/order/finance/inventory/sync-import`，并在本机离线 Gradle 缓存环境跑通编译与契约测试；本地 owner-aware Room 扩域缓存仍留待后续批次 |
| `2026-06-03` | `B08` | `待验证 -> 待验证（非阻塞修复收尾）` | `OrderV2Models.kt`（5 个 Filter 类补 `@Serializable`/`@SerialName`）、`ZhihuijiV2Api.kt`（agent/media 方法名加 `V2` 后缀、`@Query` 参数注释）、`AgentV2Repository.kt`（`deleteDraft` 改 `safeApiUnitCall`）、`MediaV2Repository.kt`（`deleteAsset`/`deleteBinding` 改 `safeApiUnitCall`）、`V2ModelSerializationTest.kt`、`SafeApiCallBehaviorTest.kt`、`FinanceV2RepositoryTest.kt`、`AgentV2RepositoryTest.kt`、`ZhihuijiV2ApiContractTest.kt`（扩到 agent/media 端点级 HTTP/path/query 断言） | `30-api-contracts.md`、`31-android-impact.md`、`32-rollout-and-compatibility.md`、`40-batch-development-master-plan.md`、android technical-analysis | B08 审核报告 6 个非阻塞问题全部修复：Filter 类注解补齐、API 命名统一 V2 后缀、delete 操作改用 `safeApiUnitCall`、`@Query` 参数名已验证与后端一致；新增 4 个测试文件 + 扩展 1 个契约测试，且 Repository 测试已直接调用真实仓储方法验证委派链路 |
| `2026-06-03` | `B11` | `新版待做 -> 待验证` | `tools/b11_acceptance_check.sh` | `41-b11-acceptance-matrix.md`、`32-rollout-and-compatibility.md`、`40-batch-development-master-plan.md`、server/android technical-analysis | 建立 B11 验收矩阵、证据目录约定、后端/Android 本地复验脚本入口；真机、117、性能稳定性、安全发布仍必须以真实证据升级状态 |
| `2026-06-03` | `B10` | `新版待做 -> 待验证` | `DocumentsScreen.kt`、`ArchivesScreen.kt`、`ProductListScreen.kt`、`CustomerListScreen.kt`、`SupplierListScreen.kt`、`SaleOrderListScreen.kt`、`PurchaseOrderListScreen.kt`、`PayOrderListScreen.kt`、`AgentTaskScreen.kt`、`SettingsScreen.kt`、feature 层 `PrimaryButton` 调用收口 | `31-android-impact.md`、`40-batch-development-master-plan.md`、android feature/core DEVELOPMENT 与 technical-analysis | 根据 B09 UI 审核修复：顶级列表保留右下蓝色 FAB，销售状态 Tab 覆盖真实 `/v2` 状态含 CONFIRMED(3)，客户状态标签不再复用供应商语义，Archives 顶部空筛选按钮已移除，AI 状态标签复用 `StatusPill`，设置页同步类型避免溢出，商品缺货状态色不再成功态，feature 主按钮调用收口到 `PrimaryButton`；最小上下文审核发现的销售 CONFIRMED 状态、客户状态、脚本口径与空筛选按钮问题已完成修复；仍待真机截图与发布级视觉验收 |
| `2026-06-02` | `B04/B05/B06` | `问题修复收口` | `V2PayOrderService`、`BillFundLinkRepository`、`V2SalesReturnService`、`V2SalesReturnDtos`、`V2PurchaseReceiptService`、`V2InventoryController`、`SyncService`、对应测试 | `22`、`23`、`24`、`25`、`29`、`30`、`31`、`32`、`40`、server/android technical-analysis | 先修 B04/B05 对 B06 数据正确性的阻塞问题：`PAID` 幂等、退货数量正数校验、客户/供应商 owner 一致性校验、inventory by-source 真 API；并复核 sync 影响面 |
| `2026-06-02` | `B04/B05` | `待验证 -> 新版已做` | `gradle.properties`、`V2PayOrderService`、`BillFundLinkRepository`、`V2SalesReturnService`、`V2SalesReturnDtos`、`V2PurchaseReceiptService`、`V2InventoryController`、`SyncService`、对应测试 | `22`、`23`、`24`、`25`、`29`、`30`、`31`、`32`、`40`、server/android technical-analysis | 已用本机 Gradle 8.7 + JDK 21 实跑定向回归；同时移除仓库内写死的 Windows `org.gradle.java.home`，恢复本机构建可移植性 |
| `2026-05-30` | `B04` | `新版待做 -> 新版已做` | 新增7个Entity+7个Repository+4个V2Service+4个V2Controller+2个V2DTO包+V10迁移脚本 | `24-finance-domain.md`、`25-inventory-domain.md`、`30-api-contracts.md`、`40-batch-development-master-plan.md` | 财务域：accounts/account_transfers/bill_fund_links/cash_change_records；库存域：inventory_ledger/inventory_snapshots/inventory_monthly_stats |
| `2026-06-03` | `B07` | `待验证 -> 待验证（修复增强）` | `V14__b07_cascade_and_constraints.sql`、`AgentConversationUpdateRequest` DTO、`V2AgentConversationService`（update/delete）、`V2AgentController`（PUT/DELETE）、`AgentMessageRepository.deleteAllByOwnerUserIdAndConversationId`、`AgentDraftRepository.deleteAllByOwnerUserIdAndConversationId`、移除 `AgentMessageListResponse`/`MediaBindingListResponse`；安卓 `UpdateAgentConversationRequest`、`updateAgentConversationV2`/`deleteAgentConversationV2`、`updateConversation`/`deleteConversation`、契约测试对 agent/media 全量端点做 HTTP/path/query 断言 | `27-media-attachments-domain.md`、`28-agent-domain.md`、`30-api-contracts.md`、`31-android-impact.md`、`32-rollout-and-compatibility.md`、`40-batch-development-master-plan.md` | B07 修复增强：会话更新/删除端点、状态枚举约束、closed/archived 拒绝新消息、级联删除（服务级 + DB 级 ON DELETE CASCADE）、批量删除方法、废弃 DTO 清理、安卓端 conversation update/delete 承接 |
| 2026-06-03 | B09 | 新版待做 -> 待验证 | `feature/products`、`feature/customers`、`feature/suppliers`、`feature/sales`、`feature/purchases`、`feature/payments`、`feature/finance`、`feature/reports`、`feature/agent`、`feature/settings`、`feature/dashboard` 首轮接入 `/v2` | 31, 32, 40, android feature/* technical-analysis | `assembleDebug` + `ZhihuijiV2ApiContractTest` 通过；但不是“全链纯 /v2”：`sales/purchases/payments` 编辑搜索仍混用旧主数据仓储，`agent/settings` 仍有任务/通知、auth、sync apply 等子链待补；报表/仪表盘为客户端聚合(待验证)；媒体上传链待验证；库存调整待切到 `/v2/inventory/ledger` |
| 2026-06-03 | Android 文档设计基线统一 | 新版待做 -> 新版待做（文档基线统一） | 无代码改动 | `docs/design-mockups/README.md`、`docs/spec/31-android-impact.md`、`docs/spec/40-batch-development-master-plan.md`、`docs/technical-analysis/android/**/README.md`、`master-goods-android/**/DEVELOPMENT.md`、`master-goods-android/UI-DESIGN-SPEC.md` | 已把设计稿真源、页面母版真源、`core/designsystem` 实现真源三层关系写回 Android 全链路文档；后续新增业务必须复用现有页面母版与统一视觉语言，但这仍不等于 B10 UI 实装完成 |
| 2026-06-03 | B09 UI 审核 + B10/B11 启动 | 待验证/新版待做 -> 保持原状态（进行中记录） | 暂无本地代码定版；已派出 B09 UI 规范审核、B10 UI 贴合开发、B11 测试验收开发三个子智能体 | `31`、`32`、`40`、android technical-analysis、Android feature/core 文档 | 本条只记录监督启动，不改变批次状态；后续必须等开发、最小上下文审核、修复、主 agent 复核和编译测试完成后再调整 B10/B11 状态 |
| 2026-06-03 | B09/B10/B11 二轮监督推进 | 保持原状态（进行中记录） | 暂无本地代码定版；主 agent 已重新校准 `UI-DESIGN-SPEC`、设计稿映射、dashboard/reports 当前实现，并再次派出 B09 UI 审查子智能体与 B10/B11 开发子智能体并行推进 | `31`、`32`、`40`、`docs/design-mockups/README.md`、android dashboard/reports technical-analysis | 本条只记录“监督中且仍在推进”的过程状态：当前明确不把已有首轮 `/v2` 接线误判为 B09/B10 全部完成；待子智能体回传后，还需继续做最小上下文复核、问题修复与最终编译测试 |
| 2026-06-03 | B09 二轮 UI 审查回传 | 保持原状态（进行中记录） | 暂无本地代码定版；B09 UI 审查子智能体已回传首批明确问题：报表页仍是单页客户端聚合、Agent workbench/tasks/notifications 仍为占位态、`DocumentsScreen`/`ArchivesScreen` 未统一到顶栏母版、多列表页主操作仍是圆形 `FloatingActionButton`、商品模块缺详情路径 | `31`、`40`、`UI-DESIGN-SPEC.md`、android `feature/reports`、`feature/agent`、`app/navigation`、多列表页源码 | 本条只记录首批审查发现，不直接改批次状态；主 agent 已基于这些发现再次派出最小上下文复核子智能体，后续需据此拆成“本轮立即修复”和“因后端/范围限制留在后续批次”的两类处理 |
| 2026-06-03 | B09/B10 最小上下文复核 + 定向修复 | 保持原状态（进行中记录） | `MainNavGraph.kt`、`ArchivesScreen.kt`、`SubNavGraph.kt`、`ProductListScreen.kt`、`ProductDetailScreen.kt`、`ReportScreen.kt`、`ReportViewModel.kt`、dashboard/reports/products DEVELOPMENT | `40-batch-development-master-plan.md`、`31-android-impact.md`、android `feature/products` / `feature/reports` DEVELOPMENT | 最小上下文复核确认本轮应立即修两项：1）商品模块缺详情页路径；2）报表时间区间 Tab 不驱动真实刷新。现已补商品 `列表 -> 详情 -> 编辑` 导航链，并让 `selectedPeriod` 下沉到 `ReportViewModel` 触发真实数据筛选；同时确认 Agent workbench/tasks/notifications 占位态、容器壳页顶栏母版不统一、多列表页圆形 FAB 等问题仍存在，但因后端端点缺口或改动面过大暂不在本轮冒进收口 |
| 2026-06-03 | B10 壳页与列表主操作继续收口 | 保持原状态（进行中记录） | `DocumentsScreen.kt`、`ArchivesScreen.kt`、`FloatingPrimaryActionButton.kt`、`CustomerListScreen.kt`、`SupplierListScreen.kt`、`SaleOrderListScreen.kt`、`PurchaseOrderListScreen.kt`、`PayOrderListScreen.kt`、`FinanceRecordListScreen.kt` | `40-batch-development-master-plan.md`、android `app` / `core/designsystem` / `feature/{customers,suppliers,sales,purchases,payments,finance}` DEVELOPMENT | 由于 B10/B11 开发子智能体额度耗尽，本轮改由主 agent 本地继续收口：`DocumentsScreen` / `ArchivesScreen` 已统一到 `GlassTopBar` 母版，档案/单据/财务等主列表已切到 `FloatingPrimaryActionButton` 胶囊主操作；随后已用本机 JDK 21 定向跑通 `:app`、`feature:{products,customers,suppliers,sales,purchases,payments,finance}` 的 `compileDebugKotlin`。当前仍不把 Agent 占位链路、dashboard/reports 逐像素贴合、真机截图验收误报为完成 |
| 2026-06-03 | B10 Agent 诚实态收口 + B11 口径收紧 | 保持原状态（进行中记录） | `AgentWorkbenchScreen.kt`、`AgentTaskScreen.kt`、`OperationDraftScreen.kt`、`AgentChatScreen.kt` | `40-batch-development-master-plan.md`、`32-rollout-and-compatibility.md`、`41-b11-acceptance-matrix.md`、android `feature/agent` DEVELOPMENT | 本轮继续收口 Agent 域但不伪造后端能力：问答结果不再展示假金额/假趋势/固定日期，工作台缺少聚合端点时不再展示假 KPI/假洞察，任务/通知缺端点时改为明确空态，草稿页无真实数据时不再展示示例编号；同时根据最小上下文 B11 复核，把“Android 本地自动化已做”的口径收紧为“定向覆盖已做”，避免掩盖 repository 覆盖仍偏局部的事实 |
| 2026-06-03 | B11 本地复验 | 待验证 -> 待验证（本地自动化已补齐，定向覆盖） | `tools/b11_acceptance_check.sh` | `32-rollout-and-compatibility.md`、`40-batch-development-master-plan.md`、`41-b11-acceptance-matrix.md`、`docs/acceptance-evidence/b11/*` | 已跑通 `backend-smoke`、`android-contract`、`android-assemble`、`android-assemble-release`、`backend-bootjar`；脚本补了后端根目录无 `./gradlew` 时复用 Android wrapper 的 fallback，并新增 release/bootJar 入口；原始日志与统一代码状态快照已补档，其中 `android-contract` 脚本化强证的是 `core:model`、`core:network`、`data:agent`、`data:finance`，其后又额外补齐了 `product/customer/supplier/order/sync` 的 repository 委派定向单测；真机/117/性能/安全发布仍保持 `待验证` |
| 2026-06-03 | B11 真机阻塞取证 | 待验证 -> 待验证（阻塞显式化） | 无源码改动 | `32-rollout-and-compatibility.md`、`40-batch-development-master-plan.md`、`41-b11-acceptance-matrix.md`、`docs/acceptance-evidence/b11/android/20260603-1712-emulator-blocked.md` | 已确认当前宿主缺少 `adb` / `emulator`，因此 B11 真机/模拟器 UI smoke、截图与 logcat 不能在本机继续推进；后续需切到具备 Android 工具链或真机接入的环境 |
| 2026-06-03 | B11 发布静态清单 | 待验证 -> 待验证（静态项补齐） | 无源码改动 | `41-b11-acceptance-matrix.md`、`32-rollout-and-compatibility.md`、`40-batch-development-master-plan.md`、`docs/acceptance-evidence/b11/security-release/20260603-1720-android-release-security-checklist.md`、`docs/acceptance-evidence/b11/backend/20260603-1720-117-release-static-checklist.md` | 已补 Android release 安全配置与 117/后端发布静态入口清单；仍缺 release 动态安装验证、117 主机真实 smoke、性能记录与安全现场证据 |
| 2026-06-03 | B11 发布构建验证 | 待验证 -> 待验证（构建级证据补齐） | 无业务源码改动 | `41-b11-acceptance-matrix.md`、`32-rollout-and-compatibility.md`、`40-batch-development-master-plan.md`、`docs/acceptance-evidence/b11/android/20260603-1730-android-assemble-release.md`、`docs/acceptance-evidence/b11/backend/20260603-1732-backend-bootjar.md` | 已跑通 Android `assembleRelease` 与后端 `bootJar`；当前剩余项除真机/117 现场、性能记录与安全现场证据外，主要是动态安装/运行、现场 smoke 与更强发布级证据，而不再是 Android repository 委派覆盖缺口 |
| 2026-06-03 | B10/B11 三轮最小上下文复核 + 原始日志补档 | 保持原状态（进行中记录） | `OperationDraftScreen.kt`、`31-android-impact.md`、`41-b11-acceptance-matrix.md`、`32-rollout-and-compatibility.md`、`docs/acceptance-evidence/b11/20260603-code-state.md`、`docs/acceptance-evidence/b11/*/*.log` | `31`、`32`、`40`、`41`、android `feature/agent`、B11 evidence | 最小上下文复核继续抓到两项真实问题并已收口：1）草稿页当前草稿与列表过滤/计数不一致；2）`31-android-impact` 把助手域写得过满。随后已重新跑通 `backend-smoke`、`android-contract`、`android-assemble`、`android-assemble-release`、`backend-bootjar` 五条本地命令并补档原始日志与统一代码状态快照；同时重新跑通 `:feature:agent:compileDebugKotlin` 与 `:app:compileDebugKotlin`。当前仍不把真机/117/性能/安全现场证据误报为完成 |
| 2026-06-03 | B10 dashboard/reports 诚实态收口 | 保持原状态（进行中记录） | `DashboardScreen.kt`、`ReportScreen.kt`、dashboard/reports technical-analysis | `40-batch-development-master-plan.md`、android `feature/dashboard` / `feature/reports` README | 最小上下文复核又抓到 5 项本地可修问题，当前已收口其中最关键的误导性语义：1）报表时间 Tab 不再暗示整页所有卡片都按所选周期重算；2）本地伪造的销售趋势折线已降级为待联调说明卡；3）首页“待审核销售单”改成销售单概览提醒；4）dashboard/reports 的 `FilterChipRow` 明确改写成“关注视角切换”；5）低库存列表右侧主指标改成缺口量而非售价。随后已重新跑通 `:feature:dashboard:compileDebugKotlin`、`:feature:reports:compileDebugKotlin`、`:app:compileDebugKotlin` |
| 2026-06-03 | B09 壳页母版与重选行为再收口 | 保持原状态（进行中记录） | `DocumentsScreen.kt`、`ArchivesScreen.kt`、`app/DEVELOPMENT.md` | `40-batch-development-master-plan.md`、android `app` DEVELOPMENT | 主 agent 继续本地收口一类低风险但真实影响体验的壳页偏差：1）移除 `DocumentsScreen` / `ArchivesScreen` 顶栏没有业务语义的伪动作图标，避免和“主操作交给子页右下胶囊按钮”的规范冲突；2）主底栏重复点击当前入口时，不再强行把容器页切回第一个子 Tab，而是保留当前子 Tab 并仅触发所在列表回到顶部。该项属于 B09/B10 UI 行为一致性补强，不把设计稿级逐页贴合误报为已完成 |
| 2026-06-03 | B09 四块最小上下文复审回传 + 二次修复 | 保持原状态（进行中记录） | `AgentWorkbenchScreen.kt`、`AgentTaskScreen.kt`、`OperationDraftScreen.kt`、`AgentChatScreen.kt`、`ReportScreen.kt`、`DocumentsScreen.kt`、`ArchivesScreen.kt` | `40-batch-development-master-plan.md`、`UI-DESIGN-SPEC.md`、android `feature/agent` / `feature/reports` / `app` | 新一轮最小上下文子智能体已稳定回传 6 条 B09 剩余问题；主 agent 本轮已直接收口其中 5 条本地可修项：1）给 `agent` 四个独立页面补统一浅蓝毛玻璃背景；2）继续清理 dashboard/reports/agent 的研发态直出文案；3）把报表“销售趋势待联调”大卡改成 `ChartCard` 内空态；4）把 `DocumentsScreen` / `ArchivesScreen` 顶栏标题固定回“单据”/“档案”；5）移除草稿页不可用的更多按钮和禁用编辑按钮。随后已重新跑通 `:feature:agent:compileDebugKotlin`、`:feature:dashboard:compileDebugKotlin`、`:feature:reports:compileDebugKotlin`、`:app:compileDebugKotlin`。当前仍保留一项后续视觉差距：底栏仍是通用 Material 图标，尚未替换为更贴设计稿的线性资产 |
| 2026-06-03 | B09/B11 子智能体复核后的二次收口 | 保持原状态（进行中记录） | `AgentWorkbenchScreen.kt`、`OperationDraftScreen.kt`、`DashboardScreen.kt`、`ReportScreen.kt`、`41-b11-acceptance-matrix.md`、`40-batch-development-master-plan.md`、`docs/acceptance-evidence/b11/*` | `UI-DESIGN-SPEC.md`、`41`、B11 evidence、android `feature/agent` / `feature/dashboard` / `feature/reports` | 根据子智能体最新复核，本轮继续收口 4 类真实问题：1）`DashboardScreen` / `ReportScreen` 在壳页嵌入模式下也统一保留浅蓝玻璃背景；2）`AgentWorkbenchScreen` 顶级页改回 `GlassTopBar` 母版，避免助手一级页面顶栏结构漂移；3）`OperationDraftScreen` 的草稿状态改成 `StatusPill` 语义标签；4）B11 证据摘要统一改写为“以当前补档日志为准”，并把 `41` 中的日志命名规则、阻塞证据元数据缺口与 `40` 的审阅基线同步写实，不再混用早期临时耗时口径 |
| 2026-06-03 | B10 Agent 间距收口 + 最小上下文复核 | 保持原状态（进行中记录） | `AgentWorkbenchScreen.kt`、`AgentTaskScreen.kt`、`AgentChatScreen.kt`、`OperationDraftScreen.kt` | `UI-DESIGN-SPEC.md`、`40-batch-development-master-plan.md`、android `feature/agent` | 按设计规范继续收口 Agent 四页根容器水平边距：先委派 worker 子智能体定位 4 处 `12dp -> 16dp` 变更，但该 worker 未成功落盘，随后由主 agent 直接接管实现；再委派最小上下文 explorer 只审这 4 个文件，回传“无发现”；最终重新跑通 `:feature:agent:compileDebugKotlin`、`:app:compileDebugKotlin`，并补了一次 `./tools/b11_acceptance_check.sh android-assemble`，当前不把这类本地 UI 收口误报成真机视觉验收完成 |
| 2026-06-03 | B10 auth/settings 壳层统一 + B11 Android 本地复验补强 | 保持原状态（进行中记录） | `LoginScreen.kt`、`RegisterScreen.kt`、`SettingsScreen.kt`、`40-batch-development-master-plan.md` | `UI-DESIGN-SPEC.md`、android `feature/auth` / `feature/settings`、`41-b11-acceptance-matrix.md` | 子智能体最新复核继续抓到 3 个本地可修问题：`login/register/settings` 仍绕开统一页面壳层，且设置页水平边距仍是 `12dp`。本轮已由主 agent 把三页统一接回 `GlassScaffold`，注册页返回区改回 `GlassTopBar`，设置页主内容边距同步收口到 `16dp`；随后最小上下文 explorer 对这 3 个文件复核“无发现”，并重新跑通 `:feature:auth:compileDebugKotlin`、`:feature:settings:compileDebugKotlin`、`:app:compileDebugKotlin`。同时补强 B11 Android 本地复验：`android-contract` 串行复跑通过，`android-assemble-release` 在提权后成功通过；其中遇到的 `~/.gradle/...zip.lck` 问题已确认是沙箱写锁限制，不是代码失败 |
| 2026-06-03 | B09 销售/采购/付款详情编辑页复审收口 | 保持原状态（进行中记录） | `PrimaryGradientButton.kt`、`SaleOrderDetailScreen.kt`、`SaleOrderEditorScreen.kt`、`PurchaseOrderDetailScreen.kt`、`PurchaseOrderEditorScreen.kt`、`PayOrderDetailScreen.kt`、`PayOrderEditorScreen.kt` | `UI-DESIGN-SPEC.md`、`40-batch-development-master-plan.md`、android `feature/{sales,purchases,payments}` / `core/designsystem` | 先由最小上下文子智能体审查销售/采购/付款 6 个详情/编辑页，回传 5 类本地可修问题；主 agent 随后完成收口：1）6 个页面根容器水平边距统一从 `12dp` 收到 `16dp`；2）销售详情“收款记录为空”改为真实 `EmptyState`，去掉伪操作文案；3）付款详情未待处理时不再渲染空 `BottomActionBar`；4）付款金额/支出语义改用危险色；5）补齐“扫码添加/收款/确认付款/创建付款单/添加商品/提交采购单”等关键动作图标，并给 `SecondaryOutlineButton` / `DangerOutlineButton` 增加可选图标能力。之后再次委派同一最小上下文子智能体复核，先回传 2 条低优先级图标问题，主 agent 继续修复后得到“无发现”；最终重新跑通 `:feature:sales:compileDebugKotlin`、`:feature:purchases:compileDebugKotlin`、`:feature:payments:compileDebugKotlin`、`:app:compileDebugKotlin`。该项属于 B09/B10 本地 UI 一致性补强，不把真机视觉验收误报为完成 |
| 2026-06-03 | B11 Android repository 委派覆盖补齐 | 待验证 -> 保持原状态（本地定向覆盖补强） | `data/{product,customer,supplier,order,sync}/build.gradle.kts`、`ProductV2RepositoryTest.kt`、`CustomerV2RepositoryTest.kt`、`SupplierV2RepositoryTest.kt`、`OrderV2RepositoryTest.kt`、`SyncV2RepositoryTest.kt` | `41-b11-acceptance-matrix.md`、`32-rollout-and-compatibility.md`、`docs/acceptance-evidence/b11/android/20260603-2354-android-repository-delegation.md` | 为了收口 `41` 中“Android `/v2` repository 委派仍待验证”的本地缺口，主 agent 本轮补齐了 `data:product`、`data:customer`、`data:supplier`、`data:order`、`data:sync` 五个模块的 `testImplementation` 依赖与定向单测，覆盖列表过滤参数、删除/状态更新委派，以及 `sync` 的 `pull -> apply -> ack(next_cursor)` 客户端确认语义；随后已重新跑通 `:data:product:testDebugUnitTest`、`:data:customer:testDebugUnitTest`、`:data:supplier:testDebugUnitTest`、`:data:order:testDebugUnitTest`、`:data:sync:testDebugUnitTest`。该项只把 Android repository 委派层提升为“本地已验证（定向单测）”，仍不把真实后端 HTTP 联调、真机同步链路和发布验收误报为完成 |
| 2026-06-04 | B10 档案详情/编辑页壳层统一复核收口 | 保持原状态（进行中记录） | `ProductEditorScreen.kt`、`ProductDetailScreen.kt`、`CustomerEditorScreen.kt`、`CustomerDetailScreen.kt`、`SupplierEditorScreen.kt`、`SupplierDetailScreen.kt` | `UI-DESIGN-SPEC.md`、`40-batch-development-master-plan.md` | 先由最小上下文子智能体复核商品/客户/供应商 6 个档案详情/编辑页，确认唯一真实问题是仍未使用统一 `GlassScaffold` 壳层；主 agent 随后把 6 个页面从 `Column(...).glassBackground()` 收口到 `GlassScaffold`。第二轮最小上下文复核继续抓到 6 处重复 `paddingValues` 导致的安全区重复问题，主 agent 已逐一修复。随后本机重新跑通 `:feature:products:compileDebugKotlin`、`:feature:customers:compileDebugKotlin`、`:feature:suppliers:compileDebugKotlin`、`:app:compileDebugKotlin`。该项属于 B09/B10 页面母版一致性补强，不把真机视觉验收误报为完成 |
| 2026-06-04 | B10 客户/供应商详情页空态兜底补强 | 保持原状态（进行中记录） | `CustomerDetailScreen.kt`、`SupplierDetailScreen.kt` | `40-batch-development-master-plan.md` | 最小上下文子智能体在二次复核中继续抓到两个真实结构问题：客户/供应商详情页在数据未返回时会塌成只剩顶栏。主 agent 随后为两页补上带 `weight(1f)` 的 `EmptyState` 正文兜底，并保持编辑按钮只在存在真实详情数据时渲染；之后再次委派最小上下文子智能体复核“无发现”，并在提权后重新跑通 `:feature:customers:compileDebugKotlin`、`:feature:suppliers:compileDebugKotlin`、`:app:compileDebugKotlin`。该项属于 B09/B10 详情页结构完整性补强，不把真机视觉验收误报为完成 |
| 2026-06-04 | B09/B10/B11 持续监督推进 | 保持原状态（进行中记录） | `DashboardScreen.kt`、`ReportScreen.kt`、`AgentChatScreen.kt`、`40-batch-development-master-plan.md` | `UI-DESIGN-SPEC.md`、`32-rollout-and-compatibility.md`、`40-batch-development-master-plan.md` | 本轮继续按“先监督、再最小上下文复核、再本地修复”推进：已重新派出 B09 UI 规范审查子智能体；受线程额度限制，B10/B11 并行 worker 暂由主 agent 接管本地可闭环项，先收口 dashboard/reports 外层 16dp 边距与 agent 问答页诚实态文案，避免把“当前已接入数据”误写成完整能力；待审查回传后继续做最小上下文复核、问题修复与编译测试。 |
| 2026-06-04 | B09 审查回传后的壳层统一与问答页补强 | 保持原状态（进行中记录） | `DashboardScreen.kt`、`ReportScreen.kt`、`AgentTaskScreen.kt`、`AgentChatScreen.kt`、`OperationDraftScreen.kt`、`DocumentsScreen.kt`、`ArchivesScreen.kt` | `UI-DESIGN-SPEC.md`、`40-batch-development-master-plan.md` | B09 审查子智能体回传后，主 agent 先收口一批本轮可闭环问题：1）把 `dashboard/reports/agent task/chat` 与 `Documents/Archives` 接回统一 `GlassScaffold`，修复“所有 feature 页面必须使用统一壳层”的硬性规范缺口；2）继续把 AI 问答首屏改成“机器人头像 + 欢迎语 + 诚实态说明”的正式入口，而不是普通占位说明卡；3）为操作草稿卡补上“内容状态 / 数据来源”两行可扫读信息，但不伪造 `contentJson` 明细；4）保留任务/通知端点缺失、AI 工作台聚合数据缺失这两类受后端限制的问题为后续联调项。随后已在提权环境重新跑通 `:feature:dashboard:compileDebugKotlin`、`:feature:reports:compileDebugKotlin`、`:feature:agent:compileDebugKotlin`、`:app:compileDebugKotlin`。 |
| 2026-06-04 | B11 UI 收口后的定向编译证据补档 | 保持原状态（进行中记录） | `docs/acceptance-evidence/b11/android/20260604-0915-android-ui-targeted-compile.md`、`41-b11-acceptance-matrix.md`、`32-rollout-and-compatibility.md`、`40-batch-development-master-plan.md` | `32-rollout-and-compatibility.md`、`41-b11-acceptance-matrix.md`、`40-batch-development-master-plan.md` | 为避免“代码已改但证据未补”的状态漂移，主 agent 本轮把 2026-06-04 的定向 Kotlin 编译结果单独补档到 B11 证据目录，并同步写回 `41` 与 `32`：该条证据只证明最新 `dashboard/reports/agent/app` UI 收口后的编译健康度，不夸大为真机、117、性能或发布完成。当前由于子 agent 线程额度仍未释放，最小上下文复核暂由主 agent 按同一小范围手工执行。 |
| 2026-06-04 | B11 UI 收口后的整包 debug 构建复验 | 保持原状态（进行中记录） | `docs/acceptance-evidence/b11/android/20260604-0930-android-assemble.md`、`41-b11-acceptance-matrix.md`、`32-rollout-and-compatibility.md`、`40-batch-development-master-plan.md` | `32-rollout-and-compatibility.md`、`41-b11-acceptance-matrix.md`、`40-batch-development-master-plan.md` | 在最新 UI 收口和局部 Kotlin 编译通过后，主 agent 又重新执行了一次完整 `./tools/b11_acceptance_check.sh android-assemble`，结果 `BUILD SUCCESSFUL in 10s`。这条证据进一步证明本轮 UI 变更没有打坏整包 Android debug 构建；但它仍然只是 dirty worktree 上的本地构建复验，不替代真机截图、117 现场、性能或发布级安全验收。 |
| 2026-06-04 | B11 UI 收口后的整包 release 构建复验 | 保持原状态（进行中记录） | `docs/acceptance-evidence/b11/android/20260604-0945-android-assemble-release.md`、`41-b11-acceptance-matrix.md`、`32-rollout-and-compatibility.md`、`40-batch-development-master-plan.md` | `32-rollout-and-compatibility.md`、`41-b11-acceptance-matrix.md`、`40-batch-development-master-plan.md` | 在最新 UI 收口通过定向 Kotlin 与 debug assemble 后，主 agent 又重新执行了一次完整 `./tools/b11_acceptance_check.sh android-assemble-release`，结果 `BUILD SUCCESSFUL in 1m 32s`。这条证据进一步证明本轮 UI 变更没有打坏 Android release 构建链；但它仍然只是 dirty worktree 上的本地 release 构建复验，不替代 release 包安装、真机运行截图、117 现场、性能或发布级安全验收。 |
| 2026-06-04 | B10 finance 列表页最低母版能力补齐 + B11 定向复验 | 保持原状态（进行中记录） | `FinanceRecordListScreen.kt`、`feature/finance/DEVELOPMENT.md`、`docs/acceptance-evidence/b11/android/20260604-0203-finance-app-targeted-compile.md`、`40-batch-development-master-plan.md` | `UI-DESIGN-SPEC.md`、android `feature/finance` DEVELOPMENT、B11 evidence | 最小上下文 UI 审核虽然误报了已修掉的重复 FAB，但仍指出一条真实缺口：`finance` 列表页缺少搜索/筛选/状态切换之一，不满足列表页母版要求。主 agent 随后只在 UI 层补上“搜索账户/转账单号”与“全部/账户/转账”分段切换，并把 `feature/finance/DEVELOPMENT.md` 对齐到项目当前已记录的 `accounts + transfers` 首轮 scope；`DocumentsScreen` 中 finance 子页继续由自身承接右下主操作，没有再额外扩写业务职责。修复后已重新跑通 `:feature:finance:compileDebugKotlin` 与 `:app:compileDebugKotlin`，结果 `BUILD SUCCESSFUL in 10s`；该项只证明本轮 finance UI 收口后的本地编译健康度，不替代真机/117/性能/发布验收。 |
| 2026-06-04 | B09/B10 详情页诚实态最终收口 + B11 定向复验 | 保持原状态（进行中记录） | `ProductDetailScreen.kt`、`PurchaseOrderEditorScreen.kt`、`docs/acceptance-evidence/b11/android/20260604-0227-android-ui-honesty-final-compile.md`、`40-batch-development-master-plan.md` | `UI-DESIGN-SPEC.md`、`32-rollout-and-compatibility.md`、`41-b11-acceptance-matrix.md` | 继续按“最小上下文复核 -> 最小修复 -> 再编译”的闭环推进后，主 agent 确认 5 个列表页空态布局已正确落盘，但仍抓到两类剩余诚实态问题：1）`PurchaseOrderEditorScreen` 仍把纯返回动作伪装成“保存草稿”；2）`ProductDetailScreen` 在未拿到真实详情时仍会用默认草稿充当详情。随后仅做最小范围修复：采购编辑页次按钮改为禁用的“草稿功能待实现”，商品详情页改为只有拿到真实 `existingId` 时才展示详情与编辑按钮，否则显示明确空态。修复后重新跑通 `:feature:products`、`:feature:customers`、`:feature:suppliers`、`:feature:sales`、`:feature:purchases`、`:feature:payments` 与 `:app` 的 `compileDebugKotlin`，结果 `BUILD SUCCESSFUL in 10s`。该项只证明最新一轮详情/编辑页诚实态收口后的本地 Kotlin 健康度，不替代真机/117/性能/发布验收。 |
| 2026-06-09 | AI 助手需求基线与 P0 证据矩阵校准 | 保持原状态（进行中记录） | `tools/ai_agent_evidence_capture.sh`、`LocalProfileGuardTest.java` | `43-ai-assistant-requirements.md`、`AI_AGENT_P0_EVIDENCE_MATRIX.md`、`40-batch-development-master-plan.md`、android `feature/agent` technical-analysis | 本轮继续推进用户要求的 AI 助手需求文档和后续审查基线：`43` 升级到 v20，明确 `result_block` 早于回答文本时必须先作为 pending 状态进入客户端，只显示轻量真实结果提示，不展开数据明细，不能把查询数据抢在回答前作为主内容显示；工具提示必须真实、短暂、可自动收敛；新增 AGT-P0-001..019 证据矩阵，统一记录接口证据、Android rule-summary 设备证据、锁屏阻塞证据、性能 partial 证据和缺口；增强 `ai_agent_evidence_capture.sh` 的 `11-latency.md`，从 run audit 派生首事件、首工具、首 result block、首 answer_delta、首 model_stream/server_notice、完成态、工具耗时合计 / 最大值和事件计数；新增 `LocalProfileGuardTest`，把 admin/demo/local seed 组件不得在 `prod` profile 注册固定为回归门禁。该项补强审查基线、接口侧性能证据工具和 demo 隔离门禁，不把 provider `model_stream`、cancel 端到端、生产 HTTP 证据、Android 首次可见耗时或全屏 UI 验收误报为完成。 |
| 2026-06-09 | AI agent 三问性能采样与多 agent 审查补强 | 保持原状态（进行中记录） | `tools/ai_agent_performance_evidence.py`、`docs/acceptance-evidence/performance/20260609-090033-ai-agent-performance/`、`AI_AGENT_P0_EVIDENCE_MATRIX.md`、`43-ai-assistant-requirements.md` | `V2AgentAiService.java`、Android `feature/agent`、`40-batch-development-master-plan.md` | 按用户要求继续优先推进剩余 AI 助手能力和性能：主 agent 新增 `ai_agent_performance_evidence.py`，可对默认 3 个真实业务问题重复采样 `/v2/agent/chat/stream`、保存 raw SSE / events / run audit，并汇总首事件、首工具、首 answer_delta、answer_completed、首 result block、run_completed、工具耗时和顺序风险；离线自测覆盖 provider `model_stream` 与非模型 data-before-answer 风险。随后临时启动 local H2 后端（`AGENT_LLM_ENABLED=false`、`AUTH_INVITE_CODE=021218`），注册本地账号并采集 `20260609-090033-ai-agent-performance`：3/3 HTTP 200 且 completed，p50 total 494.3ms、p95 total 857.59ms，rule-summary 模式未出现 result block 早于 answer_completed；该证据已写入 P0 矩阵和 `43`。并行两个只读子 agent 分别审查后端 AI 工具性能和 Android 流式渲染：确认工具多为 TopN/pageable/聚合，风险主要在会话/消息列表未分页、顺序工具 / 逐事件 audit、长 Markdown / 大表格重组；Android 未发现本地伪造打字，48ms 仅合并服务端 delta。该项仍只能证明接口侧 rule-summary 性能和代码风险边界，不替代 provider `model_stream`、并发/慢模型、中断、真机首帧和 frame timing 验收。 |
| 2026-06-09 | AI SSE 审计热路径减负 | 保持原状态（进行中记录） | `V2AgentAiService.java`、`V2AgentAiServiceTest.java`、`43-ai-assistant-requirements.md`、`AI_AGENT_P0_EVIDENCE_MATRIX.md` | `/v2/agent/chat/stream`、`agent_run_audits`、`agent_run_audit_events` | 继续按“不改 UI 实现优化全链路性能”推进：并行只读子 agent 复核确认当前 SSE 发送在后台 `agent-sse-stream` 线程执行，但每个事件后同步保存审计 event 并额外 `find + save` run summary 的 `event_count`，DB 抖动会拖慢后续事件；另一个子 agent 确认会话 / 消息 / 草稿列表仍有全量返回风险，但分页 envelope 会影响 Android `List` 合同，建议后续以可选 `page/limit` 保持数组形态。主 agent 本轮先落地低风险热路径修复：保留每条 `agent_run_audit_events` 事件 payload 持久化，移除每事件 run summary 计数读写，改为 `ActiveAgentRun` 在事件落库成功后内存计数，`finishRunAudit()` 于 completed / failed / blocked / cancelled 终态一次写回 `event_count`。`V2AgentAiServiceTest.streamEventsIncludeCompatibleEnvelopeMetadata` 新增断言 run summary 只保存 create + finish 两次且 `eventCount == events.size()`，后端 agent 单测通过。该项减少每 SSE 事件一次主表读写，但尚未把事件 payload insert 异步化，也不替代 provider / Android 性能证据。 |
| 2026-06-09 | AI SSE 审计事件异步化 | 保持原状态（进行中记录） | `V2AgentAiService.java`、`AgentRunAuditEventRepository.java`、`V2AgentAiServiceTest.java`、`43-ai-assistant-requirements.md`、`AI_AGENT_P0_EVIDENCE_MATRIX.md` | `/v2/agent/chat/stream`、`agent_run_audits`、`agent_run_audit_events` | 在上一轮去掉 run summary 逐事件读写后，本轮继续把事件 payload 保存从 `sendEvent` 热路径移出：SSE 发送后进入 per-run 有序 audit write chain，由独立 `agent-audit-write` executor 持久化；`finishRunAudit()` 和 `getRunAudit()` 等待该 run 队列 drain，终态 `eventCount` 改为 repository `countByRunId`，不再依赖 active run 内存计数。新增 / 强化测试：正常路径持久化事件 `seq/event_id` 连续；慢审计写阻塞时 SSE 仍能继续发送到 `run_completed`，收尾再等待 drain；单个审计写失败不会导致流式失败，也不会污染后续事件写入。该项进一步降低 DB 写抖动对可见流式输出的影响，但仍需 provider `model_stream`、并发慢 DB、bounded queue / backpressure、Android 首帧和 frame timing 证据。 |
| 2026-06-09 | AI audit write 有界队列与有损审计提示 | 保持原状态（进行中记录） | `V2AgentAiService.java`、`AgentRunAuditEntity.java`、`V17__agent_run_audit_loss_metrics.sql`、`V2AgentAiServiceTest.java`、`43-ai-assistant-requirements.md`、`AI_AGENT_P0_EVIDENCE_MATRIX.md` | `agent-audit-write`、`agent_run_audits.emitted_event_count`、`agent_run_audits.audit_write_dropped_count`、`agent_run_audit_events` | 延续异步审计优化，避免默认 fixed thread pool 的无界队列在高频 `answer_delta` 或多 run 并发下堆积内存：`auditWriteExecutor` 改为有界 `ThreadPoolExecutor`，每个 run 的 audit chain 使用显式 `executor.execute` 提交；executor 拒绝时不会悬挂 future chain，而是增加 `audit_write_dropped_count` 并让链正常完成；repository save 抛错时增加 `audit_write_failed_count`，后续事件仍可继续保存。`finishRunAudit()` 保持业务 `status/mode/llmStatus/errorCode` 不被审计有损污染，V17 新增 `emitted_event_count`、`audit_write_dropped_count`、`audit_write_failed_count`、`audit_lossy` 四个持久字段，`event_count` 表示已持久化事件数，`emitted_event_count` 表示服务端已发送事件数，`getRunAudit` 响应额外返回 `warnings`；新增 `rejectedAuditWriteRecordsDropNoticeWithoutFailingStream` 固定 run completed、`errorCode=null`、`audit_lossy=true`、`getRunAudit` 不挂起和 event_count 为已持久化数量。该项补齐 bounded queue / backpressure 代码门禁，但仍需 metrics、真实 DB 慢写和并发压测证据。 |
| 2026-06-30 | B11 / Web ID 精度 / Android 网络修复复验 | 保持原状态（进行中记录） | `web/src/app/stores/session.ts`、`web/src/pages/settings/RoleAccessPage.vue`、`web/src/pages/agent/AgentPage.vue`、`web/src/shared/api/client.ts`、`web/src/entities/screen/live-screen-data.ts`、`web/src/pages/archives/ProductArchivePage.vue`、`web/src/pages/documents/PurchaseReturnPage.vue`、`web/src/pages/documents/SalesReturnPage.vue`、`web/src/pages/finance/PayOrderDetailPage.vue`、`web/src/pages/inventory/InventorySnapshotPage.vue`、`master-goods-android/core/datastore/src/main/java/com/zhihuiji/core/datastore/SettingsStore.kt`、`master-goods-android/core/network/src/main/java/com/zhihuiji/core/network/NetworkModule.kt`、`master-goods-android/feature/auth/src/main/java/com/zhihuiji/feature/auth/AuthViewModel.kt`、`docs/acceptance-evidence/b11/android/20260630-1342-android-assemble-release.md`、`docs/acceptance-evidence/b11/backend/20260630-1338-124-154-readonly-status.md`、`docs/acceptance-evidence/b11/backend/20260630-1406-backend-smoke-current-failures.md`、`docs/acceptance-evidence/b11/backend/20260630-1512-backend-recovery-summary.md`、`docs/acceptance-evidence/b11/android/20260630-1508-android-device-local-https-home-smoke.md`、`docs/acceptance-evidence/b11/web/20260630-1351-web-id-entityid-build.md` | `41-b11-acceptance-matrix.md`、`40-batch-development-master-plan.md`、`docs/acceptance-evidence/b11/android/20260630-1326-android-web-current-status.md`、`docs/acceptance-evidence/b11/android/20260630-1342-android-assemble-release.md`、`docs/acceptance-evidence/b11/android/20260630-1508-android-device-local-https-home-smoke.md`、`docs/acceptance-evidence/b11/backend/20260630-1338-124-154-readonly-status.md`、`docs/acceptance-evidence/b11/backend/20260630-1406-backend-smoke-current-failures.md`、`docs/acceptance-evidence/b11/backend/20260630-1512-backend-recovery-summary.md`、`docs/acceptance-evidence/b11/web/20260630-1351-web-id-entityid-build.md` | 本轮把 Web 侧剩余实体 ID 精度问题继续收口到 `EntityId` / `readQueryId` / `sameEntityId`，并在继续把 `client.ts` 残留实体主键型字段收口后重新跑通 `npm run build`；同时在 Android 侧把默认 baseUrl、legacy host 迁移、HTTPS 强制与 release trusted-host 回退逻辑落盘，并额外收口“切换联调服务器地址后清理旧会话与本地缓存”以避免跨环境残留旧状态。后续重新跑通 `:core:datastore:testDebugUnitTest`、`:core:network:testDebugUnitTest`、`:app:assembleDebug`、`:app:assembleRelease`、`backend-smoke` 与 `backend-bootjar`，且真机已从“无在线设备”推进到“本机可识别设备、冷启动登录本地 HTTPS 后端可进首页，并打开 `单据 / 档案 / 报表 / 助手` 四个一级页签，日志可见多条 `/v2` 请求 `200 OK`”。服务器侧也已只读确认当前真实生产拓扑为 `124` 公网边缘 + `154` 应用主机，且 `zhihuiji154-backend` 容器在线。该项如今已证明当前工作树上的本地构建/测试恢复通过、真机主链路恢复、服务器在线现状，但仍不替代同步/导入、媒体/AI 深链路、性能或发布完成。 |

## 当前结论

1. `docs/` 下的文本文档已经完成多轮逐项收口，并建立了 Android UI 设计基线、B10 修复记录与 B11 验收矩阵。
2. 后端主体能力已经覆盖 B01-B07 的核心 `/v2` 领域，但发布级完成仍取决于 B11 的后端复验、当前生产拓扑联调、安全与发布清单。
3. Android 已进入 B08/B09/B10 阶段：`core/model`、`core/network`、`data/*`、`feature/*` 已完成首轮 `/v2` 接线与 UI 母版修复，但 Room owner-aware 扩域缓存、真机联调、截图验收仍为 `待验证`。
4. B10 当前修复重点已经进一步收口：右下胶囊主操作、商品详情路径、报表真实时间筛选、壳页统一 `GlassTopBar`、真实状态 Tab、客户状态语义、统一 `StatusPill`、设置页长文本、商品缺货状态色、feature 主按钮入口，以及销售/采购/付款详情编辑页的 `16dp` 边距、空态/底部操作区/支出语义色与关键动作图标一致性；本轮又把商品/客户/供应商 6 个档案详情/编辑页统一接回 `GlassScaffold`，修掉了重复 `paddingValues` 导致的安全区重复问题，并把 `dashboard/reports/agent task/chat` 与 `Documents/Archives` 继续收口到统一壳层，补强了 AI 问答首屏与草稿卡的诚实态可扫读信息；最新一轮又为 `finance` 列表页补上了最低母版要求的搜索入口与账户/转账分段切换。
5. AI 助手当前已有独立需求 / 审查基线：`43-ai-assistant-requirements.md` 和 `AI_AGENT_P0_EVIDENCE_MATRIX.md` 明确了真实 agentic、无模拟数据、真实查询、真实工具提示、Markdown / result block、provider stream、取消、审计和性能的 P0 门禁；现有证据仍以 `partial` 为主，不能替代真实 provider `model_stream`、取消端到端、生产 profile、性能和最新真机 UI 证据。
6. B11 当前已完成矩阵、脚本入口与大量本地自动化/构建复验；服务器只读现状也已确认当前真实生产拓扑为 `124` 公网边缘 + `154` 应用主机，且后端在线。2026-06-30 本轮又补齐了当前工作树上的 `backend-smoke` / `bootJar` 最新通过证据，以及真机冷启动登录首页与四个一级页签 smoke 证据。但同步/导入、媒体上传、真实 provider AI、性能、安全发布与生产业务现场仍不得仅凭本地编译、只读探针或局部 smoke 升级为 `新版已做`。
7. 下一步应优先解决两类当前阻塞：一是恢复这台 Mac 对安卓真机的识别，再继续真机/当前生产拓扑联调与截图验收；二是取得用户许可后修复当前工作树上的后端 smoke 失败。不要再把“主体已实现”误写成“全部完成”。
