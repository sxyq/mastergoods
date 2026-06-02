# Android core/model 模块分析

- 对应源码目录：`master-goods-android/core/model`
- 当前模型文件：
  - `ApiResponse.kt`
  - `AuthModels.kt`
  - `ProductModels.kt`
  - `PartyModels.kt`
  - `OrderModels.kt`
  - `FinanceModels.kt`
  - `ReportModels.kt`
  - `AgentModels.kt`
  - `SyncModels.kt`
  - `StatusConstants.kt`

## 模块定位

`core/model` 是安卓侧最重要的**契约承接层**。  
新版里，它不再只是“当前接口 DTO 集合”，而要同时承担：

- `/v1` 兼容模型
- `/v2` 领域模型
- 请求、响应、筛选条件、页面输入模型的边界控制

## 状态图例

- `新版已做`
- `新版待做`
- `旧版存在新版未做`
- `新版需要去掉`
- `需重构`
- `待验证`

## 总体状态表

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| `/v1` DTO 与筛选模型 | 新版已做 | 旧版无当前新版 DTO 体系 | 继续支撑现有功能 | 当前 10+ 模型文件已存在 | 兼容层保留 |
| `/v2` 领域模型 | 新版待做 | 旧版字段更厚但无 `/v2` | 建立 owner-aware 的新版模型 | 后端单据域与商品/伙伴域 `/v2` DTO 已落地，安卓侧仍未建立 `core/model/v2` | 先从 order + product + partner 三域开始 |
| 模型分层边界 | 需重构 | 首版常把轻量请求/响应混在一起 | 拆出 request/query/summary/detail/form model | 当前仍偏首版轻模型 | 影响广泛 |
| 商品/财务/库存扩域模型 | 旧版存在新版未做 | 旧版主数据与财务字段更厚 | 新版模型要覆盖更完整经营域 | 当前模型仍偏首版闭环 | 会新增多个文件组 |
| 会员模型 | 新版需要去掉 | 旧版可存在会员扩展 | 当前阶段不纳入 | 不应新增 member 相关模型 | 如恢复需重新立项 |

## 当前模型覆盖

### 兼容保留的 `/v1` 模型

| 模型组 | 状态 | 当前用途 |
|---|---|---|
| `AuthModels.kt` | 新版已做 | 登录、注册、验证码、会话恢复 |
| `ProductModels.kt` | 新版已做 | 基础商品档案、库存调整 |
| `PartyModels.kt` | 新版已做 | 基础客户/供应商档案 |
| `OrderModels.kt` | 新版已做 | 销售、采购、付款首版链路 |
| `FinanceModels.kt` | 新版已做 | 轻量资金流水 |
| `ReportModels.kt` | 新版已做 | 首版报表投影 |
| `AgentModels.kt` | 新版已做 | AI 工作台、问答、草稿、任务、通知 |
| `SyncModels.kt` | 新版已做 | 首版 pull/upload 模型 |

### 明确需要进入 `/v2` 的模型族

| 新版模型族 | 状态 | 说明 |
|---|---|---|
| `product` | 新版待做 | 分类、单位、价格层级、供应关系、媒体 |
| `partner` | 新版待做 | 联系人、分组、标签、价格策略 |
| `sales` | 新版待做 | 草稿、订单、收款、退货、来源、状态机 |
| `purchase` | 新版待做 | 采购订单、收货/入库、应付联动 |
| `finance` | 新版待做 | 账户、转账、找零、项目、单据资金关联 |
| `inventory` | 新版待做 | 账本、快照、月统计 |
| `sync` | 新版待做 | owner 分桶、导入任务、同步批次 |
| `agent` | 新版待做 | 对话会话、消息、草稿缓存、推荐结果 |

## 与后端 DTO / 旧版能力的关键差异

### 商品域

| 能力 | 当前安卓模型 | 状态 | 新版模型应补内容 | 备注 |
|---|---|---|---|---|
| 基础商品档案 | `ProductDto` 已覆盖基础字段 | 新版已做 | 保持 | 当前能支撑首版商品页 |
| 多单位、多价格、商品-供应商关系 | 未覆盖 | 旧版存在新版未做 | `ProductUnitDto`、`ProductPriceLevelDefinitionDto`、`ProductPriceValueDto`、`ProductSupplierRelationDto` | 后端第三阶段已给出稳定 `/v2` 契约 |
| 商品媒体 | 未覆盖 | 新版待做 | `ProductMediaDto` | 与附件域联动 |

### 往来单位域

| 能力 | 当前安卓模型 | 状态 | 新版模型应补内容 | 备注 |
|---|---|---|---|---|
| 基础客户/供应商档案 | `CustomerDto` / `SupplierDto` 已覆盖基础字段 | 新版已做 | 保持 | 当前列表/编辑/详情可用 |
| 联系人、分组、价格等级、标签 | 未覆盖 | 旧版存在新版未做 | `PartnerContactDto`、`PartnerGroupDto`、`PartnerTagDto`、`PartnerPricingPolicyDto` | 后端 `/v2/customer-groups|supplier-groups|customer-contacts|supplier-contacts` 已可作为首批目标 |

### 销售与采购域

| 能力 | 当前安卓模型 | 状态 | 新版模型应补内容 | 备注 |
|---|---|---|---|---|
| 基础销售/采购/付款单 | `OrderModels.kt` 已覆盖首版主链路 | 新版已做 | 保持基础流程 | 当前能跑通列表、编辑、详情 |
| 订单态、草稿态、退货态 | 未覆盖 | 旧版存在新版未做 | `SalesDraftDto`、`SalesReturnDto`、`PurchaseReceiptDto` 等 | 单据域最大断点之一 |
| 运费、抹零、来源、账户联动 | 未系统覆盖 | 旧版存在新版未做 | 更厚的 order/payment/finance 子模型 | 影响详情页和报表 |

### 财务、库存、同步域

| 能力 | 当前安卓模型 | 状态 | 新版模型应补内容 | 备注 |
|---|---|---|---|---|
| 轻量流水 | `FinanceRecordDto` 已覆盖 | 新版已做 | 保持基础收支 | 现阶段可用 |
| 账户、转账、找零、项目 | 未覆盖 | 旧版存在新版未做 | `AccountDto`、`TransferDto`、`CashChangeDto`、`FinanceProjectDto` | 财务域升级核心 |
| 库存账本/快照/月统计 | 仅报表投影部分覆盖 | 旧版存在新版未做 | `InventoryLedgerDto`、`InventorySnapshotDto`、`InventoryMonthlyStatsDto` | 当前库存分析不足 |
| owner-aware 同步与导入 | 未覆盖 | 需重构 | `ImportJobDto`、`OwnerSyncStateDto`、带 owner 语义的同步模型 | 未来导入到账号的基础 |

## `/v2` 包结构建议

1. 保留当前 `core/model` 作为 `/v1` 兼容层。
2. 新增 `core/model/v2/` 子包：
   - `auth`
   - `product`
   - `partner`
   - `sales`
   - `purchase`
   - `finance`
   - `inventory`
   - `media`
   - `agent`
   - `sync`
3. 每个子包再区分：
   - `request`
   - `query`
   - `summary`
   - `detail`
   - `form`

## 当前结论

- 现在的 `core/model` 依然适合作为 `/v1` 兼容层。
- 后端已经给出首批 `/v2` 单据 DTO 命名空间，且商品域已进入第三阶段扩域；安卓模型迁移应该先从 `sales / purchase / pay` 与 `product` 四组开始。
- 但它已经不适合作为新版所有能力的唯一承载点。
- 安卓端后续要不要顺利跟上后端 `/v2`，很大程度取决于这里是否先完成分层规划。
