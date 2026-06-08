# Android feature/products 模块分析

- 对应源码目录：`master-goods-android/feature/products`
- 关键源码：
  - `ProductListScreen.kt`
  - `ProductEditorScreen.kt`
  - `StockAdjustSheet.kt`
  - `ProductListViewModel.kt`
  - `ProductEditorViewModel.kt`

## 模块定位

`feature/products` 当前是基础商品页。  
新版里，它会向更完整的商品目录域界面演进，承接：

- 基础档案
- 分类与单位
- 多价格层级
- 商品-供应商关系
- 库存与媒体信息

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
| 商品列表/编辑/库存调整闭环 | 新版已做 | 旧版商品域更厚 | 支撑当前商品主流程 | 页面、弹窗、ViewModel 已存在 | 业务链已走通；已切到 ProductV2Repository |
| 多单位、多价格、供应关系页面 | 待验证 | 旧版商品能力明显更厚 | 新版商品页要超过旧版 | 数据层已就绪，UI 仍为基础字段集 | 后端第三阶段已补 `product-price-levels` 与 `product-supplier-relations`，数据层已切 V2，页面层仍未接入 |
| `/v2` owner-aware 商品页 | 待验证 | 旧版无统一 owner | 页面状态管理与表单改为新版契约 | 已切到 ProductV2Repository + ProductV2Dto | 本模块已使用 V2 Repository 替代 V1 Repository；UI 不在本阶段修改 |

## 后续页面拆分建议

| 页面/区域 | 状态 | 新版目标 |
|---|---|---|
| 商品列表页筛选区 | 新版待做 | 分类、单位、低库存、状态联合筛选 |
| 商品编辑页价格区 | 新版待做 | 支持基础价 + 多价格层级编辑 |
| 商品编辑页供应关系区 | 新版待做 | 支持默认供应商、优先级、最近采购价、备注 |
| 商品详情页供应链摘要 | 新版待做 | 汇总默认供应商与供应关系列表 |

## UI 统一约束

- 本模块后续新增业务必须继续复用当前设计图对应的页面母版，不允许因为领域变厚就切换成另一套视觉语言。
- 页面结构优先落入既有模式：列表页、详情页、编辑页、报表页、AI 页、设置页。
- 视觉基线固定为：浅蓝渐变背景、玻璃卡片、蓝色主按钮、白色次按钮、统一状态标签、五栏主壳。
- 如需新增 UI 组件，先沉淀到 `core/designsystem`，再由本模块复用；不允许长期保留 feature 私有样式组件。
- 验收时当前优先对照：`docs/spec/42-android-liquid-glass-ui-refactor-plan.md`、Stitch 导出清单、`master-goods-android/UI-DESIGN-SPEC.md`、`docs/technical-analysis/android/core/designsystem/README.md`；`docs/design-mockups/` 仅作历史参考。
