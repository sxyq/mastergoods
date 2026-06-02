# Android feature/purchases 模块分析

- 对应源码目录：`master-goods-android/feature/purchases`
- 关键源码：
  - `PurchaseOrderListScreen.kt`
  - `PurchaseOrderEditorScreen.kt`
  - `PurchaseOrderDetailScreen.kt`
  - `PurchaseOrderViewModel.kt`
  - `PurchaseOrderEditorViewModel.kt`
  - `PurchaseOrderDetailViewModel.kt`

## 模块定位

`feature/purchases` 当前承接的是首版采购闭环。  
新版里，它会从“采购单”扩展到更完整的采购域：

- 采购订单
- 待收货/收货入库
- 采购明细
- 应付与付款联动

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
| 采购列表/开单/详情闭环 | 新版已做 | 旧版采购域更厚 | 支撑当前采购主流程 | 页面与 ViewModel 已存在 | 业务链已走通 |
| 采购订单态/待收货/应付联动 | 旧版存在新版未做 | 旧版有 `porders` 等更细分层 | 新版采购域要拆清订单态和入库态 | 当前仍偏首版采购单 | 依赖后端扩域 |
| `/v2` owner-aware 采购页 | 新版待做 | 旧版无统一 owner | 页面改按 owner 和 `/v2` 契约工作 | 后端已提供 `/v2/purchase-orders/*`，安卓采购页仍主要消费 `/v1` | 下一阶段开始切换 data/viewmodel |
