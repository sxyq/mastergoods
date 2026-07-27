# core/datastore 模块开发说明

- 当前状态：脚手架已创建，DataStore 未开始。
- 实际源码目录：`core/datastore/src/main/java/com/zhihuiji/core/datastore`
- 目标：保存会话、服务器地址、clientId、同步游标和本地设置。

## 需要创建的类

- `SessionStore`
- `SettingsStore`
- `SyncPreferenceStore`

## 需要实现的关键函数

- `SessionStore.observeSession()`
- `SessionStore.saveSession(token, refreshToken, userId)`
- `SessionStore.clearSession()`
- `SessionStore.requireAccessToken()`
- `SettingsStore.observeBaseUrl()`
- `SettingsStore.saveBaseUrl(baseUrl: String)`
- `SettingsStore.observeClientId()`
- `SettingsStore.saveClientId(clientId: String)`
- `SyncPreferenceStore.observeCursor(entityType: String)`
- `SyncPreferenceStore.saveCursor(entityType: String, cursor: Long)`
- `SyncPreferenceStore.clearAll()`

## 验收标准

- 更换服务器地址、退出登录、清缓存都能只改这里的持久化数据。

## UI 设计规范支撑

- 设置页需要展示服务器地址、clientId、同步状态和缓存状态，这些值必须能以 Flow 形式持续观察。
- 登录态变化要能驱动 `GlassScaffold` 底部导航和主路由切换。

## UI 联动约束

- 本模块虽然不直接负责页面绘制，但其输出的数据结构、状态枚举、错误语义和交互支撑能力必须服务于统一的 Android UI 基线。
- 后续新增业务不能倒逼页面切换成另一套视觉风格；应优先通过补充 `core/designsystem` 通用组件或扩展既有页面母版来承接。
- 需要映射到 UI 的状态、金额、风险、同步结果等，应继续服从统一的颜色语义、状态标签和信息层级。
- Android 当前视觉真源以 Stitch 导出、`docs/spec/42-android-liquid-glass-ui-refactor-plan.md` 与 `master-goods-android/UI-DESIGN-SPEC.md` 为准；`docs/design-mockups/` 仅作历史参考。
