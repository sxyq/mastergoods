# 31 Android 影响面

## 范围说明

本文件只讨论**安卓端在新版后端与新版领域模型下的设计、规划和迁移影响**，不展开具体 UI 视觉样式，也不在本阶段要求修改安卓代码。

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
4. 本阶段不讨论具体视觉样式，只讨论页面职责、数据流和模块规划。
5. 会员体系当前不纳入安卓新版范围，统一标记为 `新版需要去掉`。

## 影响总表

| 影响点 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| 应用启动与会话恢复 | 需重构 | 旧版无统一账号归属语义 | 登录后拉起 owner 私有数据上下文 | 当前只恢复 `users/sessions` 与基础主流程 | 需要引入 owner bootstrap |
| 导航与全局状态 | 需重构 | 旧版无 `/v2` 场景编排 | AppState 感知账号、导入任务、同步状态、环境策略 | 当前导航仍主要服务 `/v1` 首版链路 | 不先改 UI，先定状态模型 |
| `core/model` | 需重构 | 旧版本地字段更厚，但无 `/v2` | `/v1` 与 `/v2` 模型并存，明确边界 | 当前大多是 `/v1` 轻模型 | 要拆出 `core/model/v2/*` |
| `core/network` | 需重构 | 旧版无统一服务端契约 | 新增 `/v2` 接口命名空间与请求模型 | 当前只覆盖 `/v1` 主域接口 | 与 server `api/dto/v2/*` 对齐 |
| `core/database` | 需重构 | 旧版账本本地表更厚 | Room 能缓存 owner 维度与扩域表 | 当前实体仍是首版缓存模型 | 后续禁止依赖破坏式迁移 |
| `core/datastore` | 需重构 | 旧版多为单机配置 | 持久化 owner、导入任务、同步基线、环境策略 | 当前只覆盖 session/baseUrl/cursor | 仍缺 owner/import 状态 |
| `data/*` Repository | 需重构 | 旧版无统一 owner | Repository 默认消费 owner 私有 `/v2` 资源 | 当前主要围绕 `/v1` 接口 | 会发生目录与职责扩容 |
| 单据域 `/v2` 首批后端能力 | 新版已做 | 旧版无 `/v2` | 为安卓后续迁移提供稳定订单域目标 | 后端已提供 `/v2/sale-orders`、`/v2/purchase-orders`、`/v2/pay-orders` | 安卓尚未接入 |
| 商品与伙伴域 `/v2` 首批后端能力 | 新版已做 | 旧版无 `/v2` | 为安卓商品档案与往来单位迁移提供稳定目标 | 后端已提供 `/v2/products`、`/v2/product-categories`、`/v2/product-units`、`/v2/customers`、`/v2/suppliers`、`/v2/*-groups`、`/v2/*-contacts` | 安卓尚未接入 |
| 商品多价格与供应关系 `/v2` 扩域能力 | 新版已做 | 旧版有多价格和供应关系 | 为安卓商品详情/编辑页提供第三阶段稳定目标 | 后端已提供 `/v2/product-price-levels`、`/v2/product-supplier-relations`，且 `/v2/products` 已返回 `price_levels/default_supplier/supplier_relations` | 安卓尚未接入，文档需先同步 |
| `feature/*` 页面职责 | 需重构 | 旧版业务域更厚 | 页面按新版领域拆清责任边界 | 当前仍偏首版“一个页面带完整流程” | 先改规划再改 UI |
| 商品多价格/多单位/供应关系 | 旧版存在新版未做 | 旧版商品域更厚 | 安卓支持商品扩域表单与读模型 | 当前商品页只覆盖基础字段 | 依赖后端先扩域 |
| 客户/供应商分组与联系人 | 旧版存在新版未做 | 旧版往来单位画像更厚 | 安卓支持更强档案管理 | 当前客户/供应商页字段偏少 | 依赖后端 DTO 扩展 |
| 销售/采购订单态与退货态 | 旧版存在新版未做 | 旧版有 `sorders/porders` 等分层 | 安卓拆成草稿/订单/出入库/退货等流程 | 当前单据页仍偏首版闭环 | 会影响 data 和 feature 两层 |
| 财务账户与项目体系 | 旧版存在新版未做 | 旧版有 `accts/projects/smallchange` | 安卓支持账户、转账、找零、项目 | 当前财务页只有轻量流水 | 是重点扩域面 |
| 库存账本与快照 | 旧版存在新版未做 | 旧版库存统计更厚 | 安卓支持库存流水、快照、月统计读模型 | 当前只有基础库存调整与报表投影 | 依赖后端与 Room 扩域 |
| 导入与同步任务 | 新版待做 | 旧版无账号私有 server import | 安卓支持账号私有导入任务与 owner 分桶同步 | 当前只有基础 pull/upload | 不在本阶段实现代码 |
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
| `core/model/v2/*` | 新版待做 | 以领域分包承载 `/v2` 模型 | 不覆盖旧 `/v1` 模型 |
| `core/network/v2` 或 `/v2` 接口分组 | 新版待做 | 新增 `/v2` Retrofit 契约 | 是否单文件或分组文件，代码阶段再定 |
| `core/database/entity/v2` 或增量扩域实体 | 新版待做 | 为 owner 与扩域表缓存做准备 | 不要求立刻拆目录 |
| `data/catalog` / `data/partner` / `data/inventory` 等 | 新版待做 | 围绕领域扩容重组 Repository | 现阶段只做规划 |
| `feature/*` 子流程拆分 | 新版待做 | 按领域边界重组页面职责 | 本阶段不做 UI 实装 |

## 页面与场景职责重排

| 场景 | 状态 | 新版职责 | 当前实现 | 备注 |
|---|---|---|---|---|
| 登录/注册 | 新版已做 | 继续保留 | 已有 `feature/auth` | 后续增加 owner bootstrap 提示 |
| 首页 | 需重构 | 从“首版快捷入口”升级为 owner 视角的经营总览与任务入口 | 已有 `feature/dashboard` | 不先讨论视觉样式 |
| 商品 | 需重构 | 基础档案 + 分类/单位/价格层级/供应关系/媒体 | 当前只有基础商品编辑与库存调整 | 后续接 `/v2` 时需要补多价格表单、默认供应商、供应关系列表 |
| 客户/供应商 | 需重构 | 基础档案 + 分组/联系人/价格策略/标签 | 当前只覆盖基础档案 | 要与 partner 域一起演进 |
| 销售 | 需重构 | 草稿/订单/出库/收款/退货/来源追踪 | 当前偏“单据闭环” | 受后端最大影响之一 |
| 采购 | 需重构 | 采购订单/收货入库/应付/付款联动 | 当前偏“采购单闭环” | 需要拆态 |
| 财务 | 需重构 | 账户、流水、转账、找零、项目、单据资金关联 | 当前只有轻量流水 | 需要从 domain 重新设计 |
| 报表 | 需重构 | 销售、采购、库存、账户、现金流等 owner 私有统计 | 当前报表仍偏首版 | 取决于后端聚合接口 |
| 助手 | 新版已做 | 保持 AI 领先域，并补 owner 私有上下文 | 当前已拆出工作台/问答/草稿/任务 | 先稳定契约，不先改视觉 |
| 设置 | 需重构 | 账号、同步、导入、环境、安全、诊断 | 当前设置页仍偏首版 | 要成为全局运维入口 |

## 安卓端暂不做的内容

| 对象 | 状态 | 备注 |
|---|---|---|
| 会员相关模块、模型、页面、入口 | 新版需要去掉 | 当前阶段全部排除 |
| 具体 UI 视觉重绘 | 待验证 | 等后端与 `/v2` 稳定后再推进 |
| 旧数据导入到服务器的客户端执行逻辑 | 新版待做 | 先做规范，不在本阶段写实现 |

## 建议迁移顺序

1. 完成安卓文档层对后端 `/v2`、owner、扩域表的同步。
2. 后端完成 `owner_user_id` 与 `/v2` 契约后，再补安卓：
   - `core/model/v2`
   - `core/network` `/v2` 契约
   - `data/*` 新仓储或仓储扩展
   - `product` 域优先接入 `product-categories / product-units / product-price-levels / product-supplier-relations / 扩域后的 /v2/products`
3. 最后再进入 `feature/*` 和 UI 实装。

## 当前结论

- 安卓当前实现仍然是一个**可运行的 `/v1` 首版应用**。
- 后端单据域与商品/伙伴域 `/v2` 首批接口已经具备，且商品域已进入第三阶段扩域；安卓后续应从 `order`、`product`、`partner` 三个域并行规划切换。
- 新版安卓不是简单补字段，而是要围绕：
  - owner 私有数据边界
  - 更厚的商品/往来单位/单据/财务/库存领域
  - `/v2` 契约
  做结构性升级。
- 本文件之后，具体模块落点以 `docs/technical-analysis/android/*` 的分模块文档为准。
