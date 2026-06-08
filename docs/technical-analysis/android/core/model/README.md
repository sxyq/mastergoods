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
- `v2/product/ProductV2Models.kt`
- `v2/partner/PartnerV2Models.kt`
- `v2/order/OrderV2Models.kt`
- `v2/finance/FinanceV2Models.kt`
- `v2/inventory/InventoryV2Models.kt`
- `v2/sync/SyncV2Models.kt`
- `v2/agent/AgentV2Models.kt`
- `v2/media/MediaV2Models.kt`

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
| `/v2` 领域模型 | 待验证 | 旧版字段更厚但无 `/v2` | 建立 owner-aware 的新版模型 | 已新增 `core/model/v2/product`、`partner`、`order`、`finance`、`inventory`、`sync`、`agent`、`media`，并完成本地 Kotlin 编译 | feature 层尚未接线，仍需联调验证 |
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
| `product` | 待验证 | 分类、单位、价格层级、供应关系、媒体首轮模型已落地 |
| `partner` | 待验证 | 联系人、分组首轮模型已落地；标签、价格策略仍待后续扩域 |
| `sales` | 待验证 | 销售单、支付、退货首轮模型已落地；B08 修复：5 个 Filter 类已补齐 `@Serializable` + `@SerialName` |
| `purchase` | 待验证 | 采购单、采购收货首轮模型已落地 |
| `finance` | 待验证 | 账户、转账、单据资金关联首轮模型已落地 |
| `inventory` | 待验证 | 账本、快照、月统计首轮模型已落地 |
| `sync` | 待验证 | owner 分桶 cursor、导入任务、pull/upload/ack 首轮模型已落地 |
| `media` | 待验证 | 资源、绑定、上传元数据首轮模型已落地，真实上传链仍待联调 |
| `agent` | 待验证 | 对话会话、消息、草稿首轮模型已落地，推荐结果缓存仍待后续扩展 |

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
- 后端已经给出首批 `/v2` 单据 DTO 命名空间，且商品域已进入第三阶段扩域；当前安卓已把 `/v2` 首轮模型扩到 `product / partner / order / finance / inventory / sync / agent / media`。
- 但它已经不适合作为新版所有能力的唯一承载点。
- 安卓端后续要不要顺利跟上后端 `/v2`，很大程度取决于这里是否先完成分层规划。

## UI 联动约束

- 本模块虽然不直接负责页面绘制，但其输出的数据结构、状态枚举、错误语义和交互支撑能力必须服务于统一的 Android UI 基线。
- 后续新增业务不能倒逼页面切换成另一套视觉风格；应优先通过补充 `core/designsystem` 通用组件或扩展既有页面母版来承接。
- 需要映射到 UI 的状态、金额、风险、同步结果等，应继续服从统一的颜色语义、状态标签和信息层级。
- Android 当前视觉真源以 Stitch 导出、`docs/spec/42-android-liquid-glass-ui-refactor-plan.md` 与 `master-goods-android/UI-DESIGN-SPEC.md` 为准；`docs/design-mockups/` 仅作历史参考。
