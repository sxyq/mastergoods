# 31 Android 影响面

## 范围说明

本文件只讨论**安卓端在新版后端与新版领域模型下的设计、规划和迁移影响**。它不承担逐像素视觉稿说明，但会明确 Android 统一设计基线、页面母版约束和模块联动边界；当前已进入 B10 局部 UI 母版修复，但发布级视觉验收仍必须以真机截图和逐页核对为准。

关联主规范：

- [00-product-overview.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/spec/00-product-overview.md)
- [10-auth-and-tenant.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/spec/10-auth-and-tenant.md)
- [20-product-domain.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/spec/20-product-domain.md)
- [21-partner-domain.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/spec/21-partner-domain.md)
- [22-sales-domain.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/spec/22-sales-domain.md)
- [23-purchase-domain.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/spec/23-purchase-domain.md)
- [24-finance-domain.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/spec/24-finance-domain.md)
- [25-inventory-domain.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/spec/25-inventory-domain.md)
- [27-media-attachments-domain.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/spec/27-media-attachments-domain.md)
- [28-agent-domain.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/spec/28-agent-domain.md)
- [29-sync-import-migration.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/spec/29-sync-import-migration.md)
- [30-api-contracts.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/spec/30-api-contracts.md)

## 六态标记

- `新版已做`
- `新版待做`
- `旧版存在新版未做`
- `新版需要去掉`
- `需重构`
- `待验证`

## Android 总体迁移原则

1. 安卓端保留当前 `/v1` 可运行链路，作为兼容层。
2. 新版安卓能力围绕 `/v2` 重新建模，不在旧模型上无限叠字段。
3. 先按**账号归属、领域扩容、同步导入**三条主线调整文档和模块边界，再进入代码实现。
4. 视觉风格不重新发明：新增业务必须继续服从 Stitch 新设计与 `core/designsystem`，只扩业务，不换视觉语言。
5. 会员体系当前不纳入安卓新版范围，统一标记为 `新版需要去掉`。

## Android UI 统一基线

Android 端后续不论新增多少业务域，都要继续共用同一套视觉基线：

- 当前视觉真源：
  - [42-android-liquid-glass-ui-refactor-plan.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/spec/42-android-liquid-glass-ui-refactor-plan.md)
  - [manifest.tsv](/Users/sunyiyang/Desktop/Project/master-goods/web/public/stitch_exports/visual-design_system_framework_14840154594131085259/manifest.tsv)
  - [UI-DESIGN-SPEC.md](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/UI-DESIGN-SPEC.md)
- 实现真源：
  - `master-goods-android/core/designsystem/` 源码

统一要求：

- 保持浅蓝极光渐变背景、液态玻璃卡片、品牌蓝主按钮、五栏主壳、轻量经营工具气质。
- 新业务页面优先落入现有母版：
  - 列表页
  - 详情页
  - 编辑页
  - 报表页
  - AI 页
  - 设置页
- 允许新增的是领域组件，不允许新增另一套视觉系统。
- 后续任何 `/v2` 扩域实现，如果让新页面明显偏离当前 Stitch 设计或本轮 UI 重构计划，应先视为设计系统或页面职责问题，而不是直接接受漂移。

## B04/B05 审计修复后的规划同步

| 影响点 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| 付款单模型的 `accountId` | 新版已做 | 首版兼容链容易丢失付款账户 | 安卓后续 `/v2` 模型必须显式承接 `accountId` | 后端 `/v2/pay-orders` 已稳定返回 `account_id`，`/v1/sync` 兼容 payload 也已补 `account_id` | 安卓本阶段只改规划文档，不改代码 |
| 销售退货创建约束 | 新版已做 | 首版容易把 `customerName` 当可信输入，且数量约束偏弱 | 安卓后续创建退货必须传正数数量并优先引用可信 `customerId` / `originalOrderId` | 后端已改为 DTO + service 双重校验 | UI 不需要现在重做，但表单模型要预留校验提示 |
| 采购收货创建约束 | 新版已做 | 首版容易把 `supplierName` 当可信输入 | 安卓后续创建收货必须传可信 `supplierId` / `purchaseOrderId` | 后端已改为 owner 内 supplier 校验与订单一致性校验 | 后续 feature/purchases 要按此重建数据流 |
| 库存按来源查询 | 新版已做 | 文档曾声明但 API 未显式暴露 | 安卓后续可按单据来源回查库存流水 | 后端已补 `GET /v2/inventory/ledger/by-source` | 报表/详情页规划可直接基于该能力 |
| B06 兼容同步修复 | 待验证 | 首版兼容同步不完整 | 安卓迁移前至少保证兼容链不继续丢关键字段 | `/v1/sync` 已补 `pay_order.account_id`，`/v2/sync` 已覆盖退货/收货/资金关联/库存账本等实体 | 仍待未来客户端联调；`/v2` cursor 现必须按 opaque token 处理，且流程应为 `pull -> 本地应用 -> ack` |
| B06 导入任务状态门控 | 待验证 | 首版无 server import 状态机 | 安卓后续只在合法状态下暴露 retry/cancel 交互 | 后端当前已收口为 `failed/cancelled -> retry`、`pending/running -> cancel` | 本阶段不改安卓代码，但后续 ViewModel / UI 状态机必须按此约束设计 |

## 影响总表

| 影响点 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| 应用启动与会话恢复 | 需重构 | 旧版无统一账号归属语义 | 登录后拉起 owner 私有数据上下文 | 当前只恢复 `users/sessions` 与基础主流程 | 需要引入 owner bootstrap |
| 导航与全局状态 | 需重构 | 旧版无 `/v2` 场景编排 | AppState 感知账号、导入任务、同步状态、环境策略 | 当前导航仍主要服务 `/v1` 首版链路 | 不先改 UI，先定状态模型 |
| `core/model` | 待验证 | 旧版本地字段更厚，但无 `/v2` | `/v1` 与 `/v2` 模型并存，明确边界 | 已新增 `core/model/v2/{product,partner,order,finance,inventory,sync,agent,media}` 并完成本地 Kotlin 编译；已新增 `UpdateAgentConversationRequest` 模型；B08 修复：5 个 Filter 类已补齐 `@Serializable` + `@SerialName` | feature 层尚未切换到这些模型 |
| `core/network` | 待验证 | 旧版无统一服务端契约 | 新增 `/v2` 接口命名空间与请求模型 | `ZhihuijiV2Api` 已覆盖 `product/partner/order/finance/inventory/sync-import/agent/media`，并通过契约测试；已新增 `updateAgentConversationV2`、`deleteAgentConversationV2` API 方法；B08 修复：agent/media 方法名统一加 `V2` 后缀，`@Query` 参数名已验证与后端一致，agent/media 契约测试已细化到 HTTP 方法、路径与关键 query 参数级别断言 | 与 server `api/dto/v2/*` 对齐 |
| `core/database` | 需重构 | 旧版账本本地表更厚 | Room 能缓存 owner 维度与扩域表 | 当前实体仍是首版缓存模型 | 后续禁止依赖破坏式迁移 |
| `core/datastore` | 需重构 | 旧版多为单机配置 | 持久化 owner、导入任务、同步基线、环境策略 | 当前只覆盖 session/baseUrl/cursor | 仍缺 owner/import 状态 |
| `data/*` Repository | 待验证 | 旧版无统一 owner | Repository 默认消费 owner 私有 `/v2` 资源 | 已新增 `data/product`、`data/customer`、`data/supplier`、`data/order`、`data/finance`、`data/sync` 的并行 `/v2` Repository，同时保留 `/v1` 兼容仓储；`data/agent` 已新增 `updateConversation`、`deleteConversation` 方法；B08 修复：`AgentV2Repository.deleteDraft()`、`MediaV2Repository.deleteAsset()`/`deleteBinding()` 已改用 `safeApiUnitCall`；`AgentV2RepositoryTest`、`FinanceV2RepositoryTest` 已直接调用真实 Repository 方法验证 API 委派链路；B09 本轮已把 `sales/purchases/payments` 编辑搜索链切到 `ProductV2Repository / CustomerV2Repository / SupplierV2Repository`，并让 `SyncV2Repository` 支持 `pull -> apply -> ack(next_cursor)` 的真实本地应用链 | Room owner-aware 扩域缓存仍待后续补齐；当前本地 apply 只覆盖现有 Room 可承接实体 |
| 单据域 `/v2` 首批后端能力 | 新版已做 | 旧版无 `/v2` | 为安卓后续迁移提供稳定订单域目标 | 后端已提供 `/v2/sale-orders`、`/v2/purchase-orders`、`/v2/pay-orders` | 安卓已接入首轮 `/v2` feature 数据链，扩域态与真机联调仍待验证 |
| 商品与伙伴域 `/v2` 首批后端能力 | 新版已做 | 旧版无 `/v2` | 为安卓商品档案与往来单位迁移提供稳定目标 | 后端已提供 `/v2/products`、`/v2/product-categories`、`/v2/product-units`、`/v2/customers`、`/v2/suppliers`、`/v2/*-groups`、`/v2/*-contacts` | 安卓已接入首轮 `/v2` data/feature 链路，owner-aware Room 缓存与真机联调仍待验证 |
| 商品多价格与供应关系 `/v2` 扩域能力 | 新版已做 | 旧版有多价格和供应关系 | 为安卓商品详情/编辑页提供第三阶段稳定目标 | 后端已提供 `/v2/product-price-levels`、`/v2/product-supplier-relations`，且 `/v2/products` 已返回 `price_levels/default_supplier/supplier_relations` | 安卓已接入首轮 `/v2` 表单/读模型链路，扩域承载与真机验收仍待验证 |
| `feature/*` 页面职责 | 待验证 | 旧版业务域更厚 | 页面按新版领域拆清责任边界 | feature 层已切到 `/v2` 首轮数据链；`sales/purchases/payments` 的列表、详情、提交、编辑搜索已统一走 `/v2` 仓储；`settings` 已具备真实手动同步本地应用链 | 仍未进入 UI 重构，且 `auth`、`agent` 部分子链、owner-aware Room 扩域缓存仍待后续收口 |
| 商品多价格/多单位/供应关系 | 待验证 | 旧版商品域更厚 | 安卓支持商品扩域表单与读模型 | `/v2` 模型、Repository 与 feature 首轮接线已完成；UI 表单仍需真机验收 | 不再依赖后端先扩域，当前风险转为 Android 联调与页面承载 |
| 客户/供应商分组与联系人 | 待验证 | 旧版往来单位画像更厚 | 安卓支持更强档案管理 | `/v2` 模型、Repository 与 feature 首轮接线已完成；联系人/分组交互仍需真机验收 | 不再依赖后端 DTO 扩展，当前风险转为 Android 联调与页面承载 |
| 销售/采购订单态与退货态 | 旧版存在新版未做 | 旧版有 `sorders/porders` 等分层 | 安卓拆成草稿/订单/出入库/退货等流程 | 后端已补销售退货 `/v2/sales-returns`、采购收货 `/v2/purchase-receipts`、草稿确认增强；安卓端尚未接入 | 会影响 data 和 feature 两层 |
| 财务账户与项目体系 | 新版已做 | 旧版有 `accts/projects/smallchange` | 安卓支持账户、转账、找零、项目 | 后端 `/v2/accounts`、`/v2/account-transfers`、`/v2/bill-fund-links` 已落地；cash_change_records 骨架已建 | 安卓端待 B08/B09 接入 |
| 库存账本与快照 | 新版已做 | 旧版库存统计更厚 | 安卓支持库存流水、快照、月统计读模型 | 后端 `/v2/inventory/ledger`、`/v2/inventory/snapshots`、`/v2/inventory/monthly-stats` 已落地 | 安卓端待 B08/B09 接入 |
| 导入与同步任务 | 需重构 | 旧版无账号私有 server import | 安卓支持账号私有导入任务与 owner 分桶同步 | 后端已落地 `/v2/sync/*` 与 `/v2/import-jobs/*`，`feature/settings -> SyncV2Repository` 已具备 `pull -> apply -> ack(next_cursor)` 手动同步链 | 仍缺 owner-aware 扩域缓存、导入任务 UI 收口与真机联调；后续只给 `failed/cancelled` 任务暴露 retry，只给 `pending/running` 暴露 cancel |
| 会员体系 | 新版需要去掉 | 旧版可推断存在会员扩展 | 当前新版不纳入 | 安卓侧不应新增会员模块或模型 | 如恢复需重新立项 |

## 目标模块结构

### 兼容层

- 继续保留当前：
  - `app`
  - `core/common`
  - `core/model`
  - `core/network`
  - `core/database`
  - `core/datastore`
  - `data/*`
  - `feature/*`

### 新版扩展方向

| 层级 | 新版规划 | 状态 | 备注 |
|---|---|---|---|
| `core/model/v2/*` | 待验证 | 已新增 `v2/product`、`v2/partner`、`v2/order`、`v2/finance`、`v2/inventory`、`v2/sync`，并保留既有 `v2/agent`（含 `UpdateAgentConversationRequest`）、`v2/media` | `/v1` 与 `/v2` 模型并存，等待 feature 接线验证 |
| `core/network/v2` 或 `/v2` 接口分组 | 待验证 | `ZhihuijiV2Api` 已从 `agent/media` 扩到 `product/partner/order/finance/inventory/sync-import` 全域首轮契约；已新增 `updateAgentConversationV2`、`deleteAgentConversationV2` 方法；契约测试已对 agent/media 全量端点做 HTTP 方法、路径与关键 query 参数断言 | 当前先集中在单一 Retrofit 接口，后续如有必要再按领域拆文件 |
| `core/database/entity/v2` 或增量扩域实体 | 新版待做 | 为 owner 与扩域表缓存做准备 | 不要求立刻拆目录 |
| `data/catalog` / `data/partner` / `data/inventory` 等 | 新版待做 | 围绕领域扩容重组 Repository | 现阶段只做规划 |
| `feature/*` 子流程拆分 | 待验证 | 按领域边界重组页面职责 | B10 已开始按 UI 母版修复 feature 层：顶级列表保留右下蓝色主操作，销售状态 Tab 收口到真实 `/v2` 状态，AI 任务/通知复用统一 `StatusPill`，设置页同步类型避免长文本溢出 |

## 页面与场景职责重排

| 场景 | 状态 | 新版职责 | 当前实现 | 备注 |
|---|---|---|---|---|
| 登录/注册 | 新版已做 | 继续保留 | 已有 `feature/auth` | 后续增加 owner bootstrap 提示 |
| 首页 | 需重构 | 从“首版快捷入口”升级为 owner 视角的经营总览与任务入口 | 已有 `feature/dashboard` | 不先讨论视觉样式 |
| 商品 | 需重构 | 基础档案 + 分类/单位/价格层级/供应关系/媒体 | 当前只有基础商品编辑与库存调整 | 后续接 `/v2` 时需要补多价格表单、默认供应商、供应关系列表 |
| 客户/供应商 | 需重构 | 基础档案 + 分组/联系人/价格策略/标签 | 当前只覆盖基础档案 | 要与 partner 域一起演进 |
| 销售 | 需重构 | 草稿/订单/出库/收款/退货/来源追踪 | 后端已补退货 `/v2/sales-returns`、草稿确认增强；安卓端仍偏"单据闭环" | 受后端最大影响之一 |
| 采购 | 需重构 | 采购订单/收货入库/应付/付款联动 | 后端已补收货 `/v2/purchase-receipts`、付款账户关联增强；安卓端仍偏"采购单闭环" | 需要拆态 |
| 财务 | 需重构 | 账户、流水、转账、找零、项目、单据资金关联 | 后端 `/v2` 已落地账户/转账/单据资金关联，安卓端待接入 | 需要从 domain 重新设计，接 `/v2` 契约 |
| 报表 | 需重构 | 销售、采购、库存、账户、现金流等 owner 私有统计 | 当前报表仍偏首版 | 取决于后端聚合接口 |
| 助手 | 待验证 | 保持 AI 领先域，并补 owner 私有上下文 | 当前已拆出工作台/问答/草稿/任务，但问答迁移、草稿编辑链与任务/通知完整联调仍未收口 | 先稳定契约，不先改视觉，也不把占位态误报为完成 |
| 设置 | 需重构 | 账号、同步、导入、环境、安全、诊断 | 当前设置页仍偏首版 | 要成为全局运维入口 |

## 安卓端暂不做的内容

| 对象 | 状态 | 备注 |
|---|---|---|
| 会员相关模块、模型、页面、入口 | 新版需要去掉 | 当前阶段全部排除 |
| 具体 UI 视觉重绘 | 待验证 | B10 已开始修复 UI 母版与状态语义问题，但仍需真机截图、逐页核对和最小上下文审核 |
| 旧数据导入到服务器的客户端执行逻辑 | 新版待做 | 先做规范，不在本阶段写实现 |

## 建议迁移顺序

1. 完成安卓文档层对后端 `/v2`、owner、扩域表的同步。
2. 在保留当前 `/v1` 可运行链路的前提下，继续扩展安卓：
   - `core/model/v2` 从已落地的 `agent/media` 继续扩到 `product/partner/order/finance/inventory/sync`
   - `core/network` `/v2` 契约从已落地的 `ZhihuijiV2Api` 继续补齐其他领域
   - `data/*` 新仓储或仓储扩展从 `data/agent` 首轮接入继续外扩
   - `product` 域优先接入 `product-categories / product-units / product-price-levels / product-supplier-relations / 扩域后的 /v2/products`
3. feature 层已进入 B09 首轮切换：多数主列表/详情/提交链路已切到 `/v2`；本轮又补齐了 `sales/purchases/payments` 的编辑搜索链和 `settings` 的 `pull -> apply -> ack(next_cursor)` 手动同步链；后续再进入 UI 重构与真机联调阶段。

## 当前结论

- 安卓当前实现是一个**已接入 `/v2` 首轮迁移的可运行应用**，但不是“全链纯 `/v2`”。
- B08 已在 `core/model/v2`、`core/network/ZhihuijiV2Api`、`data/*V2Repository` 落地 `product/partner/order/finance/inventory/sync-import/agent/media` 的 `/v2` 首轮承接代码，并已完成本地编译/契约验证。
- 后端单据域与商品/伙伴域 `/v2` 首批接口已经具备，且商品域已进入第三阶段扩域；安卓后续应从 `order`、`product`、`partner` 三个域并行规划切换。
- B06 首轮后，后端已额外具备 `/v2/sync/*` 与 `/v2/import-jobs/*`，安卓后续需要把 `data/sync`、`core/datastore`、`core/network`、`feature/settings` 一起纳入迁移范围。
- 安卓后续接 `/v2/sync` 时，不能把 `next_cursor` 解析成 long 再自行计算；当前 `feature/settings -> SyncV2Repository` 已按 `pull -> 本地应用 -> ack(next_cursor)` 收口手动同步链，但本地只会应用当前 Room 可承接的实体，扩域缓存仍待后续批次推进。
- 新版安卓不是简单补字段，而是要围绕：
  - owner 私有数据边界
  - 更厚的商品/往来单位/单据/财务/库存领域
  - `/v2` 契约
  做结构性升级。
- 本文件之后，具体模块落点以 `master-goods-android/` 源码目录为准。
