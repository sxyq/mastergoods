# Android data/order 模块分析

- 对应源码目录：`master-goods-android/data/order`
- 关键源码：
  - `SaleOrderRepository.kt`
  - `PurchaseOrderRepository.kt`
  - `PayOrderRepository.kt`

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
| 销售详情本地回退图 | 新版已做 | 旧版本地账本天然可直接查单据明细 | 当前新版至少保证销售详情在弱网/失败时仍能读到已同步的订单项 | `SaleOrderRepository` 已接入 `SaleOrderDao.findWithItemsById()` 与 `replaceOrderGraph(s)` | 采购/付款本地明细图仍偏轻 |
| 销售订单态/采购订单态/退货态 | 旧版存在新版未做 | 旧版有 `sorders/porders` 等更细表域 | 新版要拆清草稿、订单、出入库、退货 | 当前仍以首版单据模型为主 | 会影响接口和页面 |
| owner 过滤与 `/v2` 单据契约 | 新版待做 | 旧版无统一 owner | 所有单据仓储按 owner 与 `/v2` 行为重写 | 后端已落地 `/v2` 首批单据接口，但安卓仓储仍主要依赖 `/v1` | 下一阶段优先切换 `data/order` |
| “一个仓储包办整个单据域”思路 | 需重构 | 首版为提速可接受 | 新版要按场景与聚合根细化仓储职责 | 当前 `data/order` 仍偏粗粒度 | 代码阶段再落地 |

## 新版建议拆分方向

| 仓储方向 | 状态 | 说明 |
|---|---|---|
| `sales` | 新版待做 | 销售草稿、销售订单、销售收款、销售退货 |
| `purchase` | 新版待做 | 采购订单、收货/入库、应付联动 |
| `payment` | 新版待做 | 单据付款、单据资金映射、状态更新 |
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
