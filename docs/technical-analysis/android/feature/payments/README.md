# Android feature/payments 模块分析

- 对应源码目录：`master-goods-android/feature/payments`
- 关键源码：
  - `PayOrderListScreen.kt`
  - `PayOrderEditorScreen.kt`
  - `PayOrderDetailScreen.kt`
  - `PayOrderViewModel.kt`
  - `PayOrderEditorViewModel.kt`
  - `PayOrderDetailViewModel.kt`

## 模块定位

`feature/payments` 当前承接的是付款单首版流程。  
新版里，它会与采购域、财务域一起重新划分职责，重点覆盖：

- 付款单创建与确认
- 付款与应付状态联动
- 账户、项目、来源信息
- owner 私有付款与结算视图

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
| 付款单列表/新建/详情/状态更新 | 新版已做 | 旧版付款域更细 | 支撑当前付款业务链 | 页面与 ViewModel 已存在 | 闭环已走通 |
| 与采购订单、账户主数据的深联动 | 旧版存在新版未做 | 旧版财务与采购联动更完整 | 新版付款页要感知账户、项目、来源 | 当前仍是首版付款单交互 | 依赖后端扩域 |
| `/v2` owner-aware 付款页 | 新版待做 | 旧版无统一 owner | 按 owner 和 `/v2` 契约重做 | 后端已提供 `/v2/pay-orders/*`，安卓付款页仍消费 `/v1` | 下一阶段开始切换 data/viewmodel |
| “付款只是采购附属弹窗”思路 | 需重构 | 首版可接受 | 新版付款应成为清晰的结算场景 | 当前页面仍偏轻量 | 财务域重构后再落地 |
