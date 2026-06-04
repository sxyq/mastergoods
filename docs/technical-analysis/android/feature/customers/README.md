# Android feature/customers 模块分析

- 对应源码目录：`master-goods-android/feature/customers`
- 关键源码：
  - `CustomerListScreen.kt`
  - `CustomerEditorScreen.kt`
  - `CustomerDetailScreen.kt`
  - `CustomerViewModel.kt`
  - `CustomerEditorViewModel.kt`
  - `CustomerDetailViewModel.kt`

## 模块定位

`feature/customers` 在新版中承接的是客户档案域，不只是一个基础联系人列表。  
后续要覆盖：

- 客户基础档案
- 联系人与分组
- 标签与价格策略
- 与销售/应收的经营视角联动

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
| 客户列表/编辑/详情闭环 | 新版已做 | 旧版客户域更厚 | 支撑当前客户主流程 | 页面和 ViewModel 已完整存在 | 业务链已走通 |
| 联系人/分组/价格等级等 UI | 旧版存在新版未做 | 旧版客户画像更完整 | 新版客户页要覆盖更厚主数据 | 当前页面仍是基础字段集 | 依赖后端扩域 |
| `/v2` owner-aware 客户页 | 待验证 | 旧版无统一 owner | 页面逻辑要按 owner 与新版 DTO 工作 | 已切到 CustomerV2Repository + CustomerV2Dto | 本模块已使用 V2 Repository 替代 V1 Repository；UI 不在本阶段修改 |
| 客户状态标签 | 待验证 | 旧版/实体状态容易复用“启用/停用” | 客户列表和详情使用客户语义 | Tab 为“全部/正常/欠款/已停用”；卡片状态显示“正常/欠款/已停用” | 不再复用 `supplierStatus` 显示客户 |

## UI 统一约束

- 本模块后续新增业务必须继续复用当前设计图对应的页面母版，不允许因为领域变厚就切换成另一套视觉语言。
- 页面结构优先落入既有模式：列表页、详情页、编辑页、报表页、AI 页、设置页。
- 视觉基线固定为：浅蓝渐变背景、玻璃卡片、蓝色主按钮、白色次按钮、统一状态标签、五栏主壳。
- 如需新增 UI 组件，先沉淀到 `core/designsystem`，再由本模块复用；不允许长期保留 feature 私有样式组件。
- 验收时同时对照：`docs/design-mockups/01.png ~ 08.png`、`master-goods-android/UI-DESIGN-SPEC.md`、`docs/technical-analysis/android/core/designsystem/README.md`。
