# Android feature/finance 模块分析

- 对应源码目录：`master-goods-android/feature/finance`
- 关键源码：
  - `FinanceRecordListScreen.kt`
  - `FinanceRecordEditorSheet.kt`
  - `FinanceViewModel.kt`

## 模块定位

`feature/finance` 当前只是轻量流水入口。  
新版里，它要逐步演化成财务域前台，承接：

- 账户与余额
- 收支流水
- 转账
- 找零/零钱
- 项目维度
- 单据资金关联

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
| 财务页首轮页面 | 待验证 | 旧版财务域更厚 | 支撑新版账户/转账视图主链 | 页面、弹窗、ViewModel 已存在 | 当前实现已转到 `accounts + transfers` 视图，不再是旧 `finance_records` 流水写入闭环 |
| 账户/转账/找零/项目等场景 | 旧版存在新版未做 | 旧版有更完整财务体系 | 新版财务页要覆盖更厚场景 | 当前只覆盖账户列表、转账记录与新增账户 | 找零、项目、完整账户动作仍待后端/客户端继续扩域 |
| `/v2` owner-aware 财务页 | 待验证 | 旧版无统一 owner | 按 owner 和新版接口重做状态管理 | 已切到 FinanceV2Repository + AccountV2Dto/AccountTransferV2Dto | 本模块已使用 V2 Repository 替代 V1 Repository |

## UI 统一约束

- 本模块后续新增业务必须继续复用当前设计图对应的页面母版，不允许因为领域变厚就切换成另一套视觉语言。
- 页面结构优先落入既有模式：列表页、详情页、编辑页、报表页、AI 页、设置页。
- 视觉基线固定为：浅蓝渐变背景、玻璃卡片、蓝色主按钮、白色次按钮、统一状态标签、五栏主壳。
- 如需新增 UI 组件，先沉淀到 `core/designsystem`，再由本模块复用；不允许长期保留 feature 私有样式组件。
- 验收时同时对照：`docs/design-mockups/01.png ~ 08.png`、`master-goods-android/UI-DESIGN-SPEC.md`、`docs/technical-analysis/android/core/designsystem/README.md`。
