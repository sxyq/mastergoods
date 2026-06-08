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
| `/v2` owner-aware 采购页 | 待验证 | 旧版无统一 owner | 页面改按 owner 和 `/v2` 契约工作 | 列表/详情/提交已切到 `PurchaseOrderV2Repository + PurchaseOrderV2Dto`；本轮编辑页商品/供应商搜索也已切到 `ProductV2Repository / SupplierV2Repository` | 采购编辑链已不再依赖旧主数据仓储；收货态与更厚联动仍待后续补齐 |

## UI 统一约束

- 本模块后续新增业务必须继续复用当前设计图对应的页面母版，不允许因为领域变厚就切换成另一套视觉语言。
- 页面结构优先落入既有模式：列表页、详情页、编辑页、报表页、AI 页、设置页。
- 视觉基线固定为：浅蓝渐变背景、玻璃卡片、蓝色主按钮、白色次按钮、统一状态标签、五栏主壳。
- 如需新增 UI 组件，先沉淀到 `core/designsystem`，再由本模块复用；不允许长期保留 feature 私有样式组件。
- 验收时当前优先对照：`docs/spec/42-android-liquid-glass-ui-refactor-plan.md`、Stitch 导出清单、`master-goods-android/UI-DESIGN-SPEC.md`、`docs/technical-analysis/android/core/designsystem/README.md`；`docs/design-mockups/` 仅作历史参考。
