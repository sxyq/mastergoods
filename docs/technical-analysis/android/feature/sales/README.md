# Android feature/sales 模块分析

- 对应源码目录：`master-goods-android/feature/sales`
- 关键源码：
  - `SaleOrderListScreen.kt`
  - `SaleOrderEditorScreen.kt`
  - `SaleOrderDetailScreen.kt`
  - `SalePaymentSheet.kt`
  - `SaleOrderListViewModel.kt`
  - `SaleOrderEditorViewModel.kt`
  - `SaleOrderDetailViewModel.kt`

## 模块定位

`feature/sales` 当前承接的是首版销售闭环。  
新版里，这个模块会从“开单 + 详情 + 收款”扩成更完整的销售域场景：

- 销售草稿
- 销售订单
- 出库/成交
- 收款
- 退货
- 来源渠道与经营追踪

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
| 销售列表/开单/详情/收款/取消闭环 | 新版已做 | 旧版销售域更厚 | 支撑当前销售主流程 | 页面、弹窗、ViewModel 已存在 | 业务链已走通 |
| 销售详情进入编辑与草稿改写 | 新版已做 | 旧版可在单据域内继续流转编辑 | 当前新版至少支持从详情进入编辑页，并以受控方式回写折扣与备注 | `SaleOrderDetailScreen -> SaleOrderEditorScreen(orderId)` 已接通，编辑态锁定客户与明细 | 仍不是完整订单态/出库态拆分 |
| 销售订单态/退货态/来源渠道 | 旧版存在新版未做 | 旧版 `sales + sorders` 更细 | 新版要拆清草稿、订单、出库、收款、退货 | 当前仍偏首版单据链 | 依赖后端扩域 |
| `/v2` owner-aware 销售页 | 待验证 | 旧版无统一 owner | 页面改按 owner 和 `/v2` 契约工作 | 列表/详情/提交已切到 `SaleOrderV2Repository + SaleOrderV2Dto`；本轮编辑页商品/客户搜索也已切到 `ProductV2Repository / CustomerV2Repository` | 销售编辑链已不再依赖旧主数据仓储；更厚的订单态/退货态联动仍待后续补齐 |
| “单一编辑页承担全部销售流程”思路 | 需重构 | 首版为快交付可接受 | 新版会逐步拆成更明确的销售场景页 | 当前编辑/详情页职责仍偏重 | 先做规划 |

## UI 统一约束

- 本模块后续新增业务必须继续复用当前设计图对应的页面母版，不允许因为领域变厚就切换成另一套视觉语言。
- 页面结构优先落入既有模式：列表页、详情页、编辑页、报表页、AI 页、设置页。
- 视觉基线固定为：浅蓝渐变背景、玻璃卡片、蓝色主按钮、白色次按钮、统一状态标签、五栏主壳。
- 如需新增 UI 组件，先沉淀到 `core/designsystem`，再由本模块复用；不允许长期保留 feature 私有样式组件。
- 验收时同时对照：`docs/design-mockups/01.png ~ 08.png`、`master-goods-android/UI-DESIGN-SPEC.md`、`docs/technical-analysis/android/core/designsystem/README.md`。
