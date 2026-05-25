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
