# Android data/order 模块分析

- 对应源码目录：`master-goods-android/data/order`
- 关键源码：
  - `SaleOrderRepository.kt`
  - `PurchaseOrderRepository.kt`
  - `PayOrderRepository.kt`
  - `SaleOrderV2Repository.kt`
  - `SalesReturnV2Repository.kt`
  - `PurchaseOrderV2Repository.kt`
  - `PurchaseReceiptV2Repository.kt`
  - `PayOrderV2Repository.kt`

## 模块定位

当前 `data/order` 还是首版“三仓储并列”的结构。  
新版里，这一层会成为安卓受影响最大的地方之一，因为后端会从简单单据表扩成：

- 销售草稿/订单/出库/收款/退货
- 采购订单/收货入库/应付/付款
- 单据与资金联动

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
| 销售/采购/付款三类仓储 | 新版已做 | 旧版单据域更细但非当前架构 | 继续跑通现有主链路 | 3 个 Repository 已存在 | 当前功能可用 |
| 列表筛选向 DAO 下沉 | 新版已做 | 旧版本地账本天然依赖 SQL 检索 | 当前新版把销售/付款/资金流水的时间、金额、商品关键字等筛选更多下沉到 Room DAO | `SaleOrderDao`、`PayOrderDao`、`FinanceRecordDao` 已增强搜索参数 | 减少纯内存过滤 |
| 销售详情本地回退图 | 新版已做 | 旧版本地账本天然可直接查单据明细 | 当前新版至少保证销售详情在弱网/失败时仍能读到已同步的订单项 | `SaleOrderRepository` 已接入 `SaleOrderDao.findWithItemsById()` 与 `replaceOrderGraph(s)` | 采购/付款本地明细图仍偏轻 |
| 销售订单态/采购订单态/退货态 | 旧版存在新版未做 | 旧版有 `sorders/porders` 等更细表域 | 新版要拆清草稿、订单、出入库、退货 | 当前仍以首版单据模型为主 | 会影响接口和页面 |
| owner 过滤与 `/v2` 单据契约 | 待验证 | 旧版无统一 owner | 所有单据仓储按 owner 与 `/v2` 行为重写 | 已新增 `SaleOrderV2Repository`、`SalesReturnV2Repository`、`PurchaseOrderV2Repository`、`PurchaseReceiptV2Repository`、`PayOrderV2Repository`；B08 修复：5 个 Filter 类已补齐 `@Serializable` + `@SerialName` | 现阶段与 `/v1` 并行，feature 层尚未切换 |
| “一个仓储包办整个单据域”思路 | 需重构 | 首版为提速可接受 | 新版要按场景与聚合根细化仓储职责 | 当前 `data/order` 仍偏粗粒度 | 代码阶段再落地 |

## 新版建议拆分方向

| 仓储方向 | 状态 | 说明 |
|---|---|---|
| `sales` | 待验证 | `SaleOrderV2Repository` 已覆盖销售订单、草稿确认、收款、取消；`SalesReturnV2Repository` 已覆盖独立退货 |
| `purchase` | 待验证 | `PurchaseOrderV2Repository` 与 `PurchaseReceiptV2Repository` 已覆盖采购订单与收货/入库 |
| `payment` | 待验证 | `PayOrderV2Repository` 已覆盖付款单创建与状态更新 |
| `billing` 或 `settlement` | 新版待做 | 单据与财务域的交叉层 |

## 当前代码与新版规划的断点

| 断点 | 状态 | 当前表现 | 新版要求 |
|---|---|---|---|
| `SaleOrderRepository` | 需重构 | 仍按 `/v1` 销售单直接 CRUD | 拆成更明确的销售域场景 |
| `PurchaseOrderRepository` | 需重构 | 缺订单态/收货态分层 | 支撑采购订单与收货入库分离 |
| `PayOrderRepository` | 需重构 | 仍是轻量付款单语义 | 要与财务账户、应付状态联动 |

## 后端已提供的首批 `/v2` 目标

| 接口组 | 状态 | 安卓影响 |
|---|---|---|
| `/v2/sale-orders/*` | 新版已做 | `SaleOrderRepository` 后续应优先适配新的列表/详情/创建/收款语义 |
| `/v2/purchase-orders/*` | 新版已做 | `PurchaseOrderRepository` 后续切换到 owner-aware 的采购接口 |
| `/v2/pay-orders/*` | 新版已做 | `PayOrderRepository` 后续切换到 owner-aware 的付款接口 |

## UI 联动约束

- 本模块虽然不直接负责页面绘制，但其输出的数据结构、状态枚举、错误语义和交互支撑能力必须服务于统一的 Android UI 基线。
- 后续新增业务不能倒逼页面切换成另一套视觉风格；应优先通过补充 `core/designsystem` 通用组件或扩展既有页面母版来承接。
- 需要映射到 UI 的状态、金额、风险、同步结果等，应继续服从统一的颜色语义、状态标签和信息层级。
- Android 当前视觉真源以 Stitch 导出、`docs/spec/42-android-liquid-glass-ui-refactor-plan.md` 与 `master-goods-android/UI-DESIGN-SPEC.md` 为准；`docs/design-mockups/` 仅作历史参考。
