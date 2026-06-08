# feature/settings 模块开发说明

- 当前状态：设置页主体已接入，当前真实实现包含账号信息、受控服务端地址展示、手动同步、导入任务列表/空态、退出登录；缓存统计与独立清理入口仍未实现。
- 实际源码目录：`feature/settings/src/main/java/com/zhihuiji/feature/settings`
- 目标：实现服务器设置、账号信息、同步状态、退出登录。

## 需要创建的类

- `SettingsScreen`
- `SettingsViewModel`

## 需要实现的关键函数

- `SettingsViewModel.loadSettings()`
- `SettingsViewModel.saveBaseUrl(baseUrl: String)`
- `SettingsViewModel.loadCurrentUser()`
- `SettingsViewModel.loadSyncHealth()`
- `SettingsViewModel.runManualSync()`
- `SettingsViewModel.clearLocalCache()`
- `SettingsViewModel.logout()`

## 页面内容

- 服务器地址
- 当前账号
- clientId
- 同步健康状态
- 手动同步
- 导入任务
- 通用设置中的缓存说明
- 退出登录

## UI 设计规范

- 历史视觉参考曾对照旧设计图 `02.png`，当前请优先对照 Stitch 导出与 `docs/spec/42-android-liquid-glass-ui-refactor-plan.md`； 的设置页实现。
- 顶部为返回按钮和标题“设置”。
- 设置项按账号与安全、服务端设置、通用设置、关于分组，每组使用毛玻璃列表卡。
- 每个设置项左侧可放蓝色线性图标，右侧展示当前值和箭头。
- 同步状态使用绿色 `StatusPill`，缓存大小使用弱文本。
- 退出登录使用红色描边大按钮，不能使用蓝色主按钮。
- 页面必须诚实反映真实能力：未接入缓存统计、缓存清理、环境切换时，只展示说明和当前受控行为，不伪造已可用入口。

## 验收标准

- 设置页能独立承接账号查看、受控服务端信息查看、手动同步、导入任务观察和退出登录。
- 尚未接通的缓存清理/统计能力要保持文档与页面口径一致，不能继续写成“已完成”。

## UI 统一约束

- 本模块后续新增业务必须继续复用当前设计图对应的页面母版，不允许因为领域变厚就切换成另一套视觉语言。
- 页面结构优先落入既有模式：列表页、详情页、编辑页、报表页、AI 页、设置页。
- 视觉基线固定为：浅蓝渐变背景、玻璃卡片、蓝色主按钮、白色次按钮、统一状态标签、五栏主壳。
- 如需新增 UI 组件，先沉淀到 `core/designsystem`，再由本模块复用；不允许长期保留 feature 私有样式组件。
- 验收时当前优先对照：`docs/spec/42-android-liquid-glass-ui-refactor-plan.md`、Stitch 导出清单、`master-goods-android/UI-DESIGN-SPEC.md`、`docs/technical-analysis/android/core/designsystem/README.md`；`docs/design-mockups/` 仅作历史参考。
