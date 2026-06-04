# Android feature/suppliers 模块分析

- 对应源码目录：`master-goods-android/feature/suppliers`
- 关键源码：
  - `SupplierListScreen.kt`
  - `SupplierEditorScreen.kt`
  - `SupplierDetailScreen.kt`
  - `SupplierViewModel.kt`
  - `SupplierEditorViewModel.kt`
  - `SupplierDetailViewModel.kt`

## 模块定位

`feature/suppliers` 在新版中承接供应商档案域。  
后续要覆盖：

- 供应商基础档案
- 联系人与分组
- 标签与价格策略
- 与采购/应付的经营联动

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
| 供应商列表/编辑/详情闭环 | 新版已做 | 旧版供应商域更厚 | 支撑当前供应商主流程 | 页面与 ViewModel 已存在 | 业务链已走通 |
| 联系人/分组/价格等级等供应商画像 | 旧版存在新版未做 | 旧版 `companies` 字段更厚 | 新版供应商页要补足画像与经营能力 | 当前仍是基础字段集 | 依赖后端扩域 |
| `/v2` owner-aware 供应商页 | 待验证 | 旧版无统一 owner | 页面改按 owner 与新版 DTO 工作 | 已切到 SupplierV2Repository + SupplierV2Dto | 本模块已使用 V2 Repository 替代 V1 Repository；UI 不在本阶段修改 |

## UI 统一约束

- 本模块后续新增业务必须继续复用当前设计图对应的页面母版，不允许因为领域变厚就切换成另一套视觉语言。
- 页面结构优先落入既有模式：列表页、详情页、编辑页、报表页、AI 页、设置页。
- 视觉基线固定为：浅蓝渐变背景、玻璃卡片、蓝色主按钮、白色次按钮、统一状态标签、五栏主壳。
- 如需新增 UI 组件，先沉淀到 `core/designsystem`，再由本模块复用；不允许长期保留 feature 私有样式组件。
- 验收时同时对照：`docs/design-mockups/01.png ~ 08.png`、`master-goods-android/UI-DESIGN-SPEC.md`、`docs/technical-analysis/android/core/designsystem/README.md`。
