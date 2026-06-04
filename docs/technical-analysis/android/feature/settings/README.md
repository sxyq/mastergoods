# Android feature/settings 模块分析

- 对应源码目录：`master-goods-android/feature/settings`
- 关键源码：
  - `SettingsScreen.kt`
  - `SettingsViewModel.kt`

## 模块定位

新版里 `feature/settings` 不是“杂项设置页”，而是全局运维入口，承接：

- 账号与会话信息
- 同步状态
- 导入任务状态
- 环境与安全策略
- 诊断信息

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
| 设置页与退出登录 | 新版已做 | 旧版无当前统一设置页 | 支撑当前主流程设置入口 | 页面与 ViewModel 已存在 | 已接入退出登录 |
| release 任意改服务器地址 | 新版需要去掉 | 首版为了联调保留过宽松能力 | 正式版只允许受控主机 | 当前已通过 `isBaseUrlEditable` 收紧为受控编辑模式 | 后续继续跟 `/v2` 统一 |
| owner-aware 同步/导入状态展示 | 待验证 | 旧版无统一 owner | 展示账号私有数据状态、导入任务、同步队列 | 已切到 `SyncV2Repository + ImportJobV2Dto`，cursor 按 opaque token 处理；本轮已把 `runManualSync()` 收口为 `pull -> 本地 apply -> ack(next_cursor)` | 本模块已使用 V2 Repository 替代 V1 Repository；同步 cursor 需按 opaque token 展示/存储，不能假设为时间戳；当前本地 apply 只覆盖现有 Room 可承接实体，owner-aware 扩域缓存仍待后续补齐；导入任务只给 `failed/cancelled` 显示 retry，只给 `pending/running` 显示 cancel |
| “环境开关与业务设置混放”思路 | 需重构 | 首版常见 | 新版要清楚区分账号、同步、导入、安全、诊断 | 当前文档已调整方向 | 代码后续跟进 |

## UI 统一约束

- 本模块后续新增业务必须继续复用当前设计图对应的页面母版，不允许因为领域变厚就切换成另一套视觉语言。
- 页面结构优先落入既有模式：列表页、详情页、编辑页、报表页、AI 页、设置页。
- 视觉基线固定为：浅蓝渐变背景、玻璃卡片、蓝色主按钮、白色次按钮、统一状态标签、五栏主壳。
- 如需新增 UI 组件，先沉淀到 `core/designsystem`，再由本模块复用；不允许长期保留 feature 私有样式组件。
- 验收时同时对照：`docs/design-mockups/01.png ~ 08.png`、`master-goods-android/UI-DESIGN-SPEC.md`、`docs/technical-analysis/android/core/designsystem/README.md`。
