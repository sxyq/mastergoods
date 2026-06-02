# feature/settings 模块开发说明

- 当前状态：脚手架已创建，页面未开始。
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
- 清理缓存
- 退出登录

## UI 设计规范

- 对照设计图 `02.png` 的设置页实现（来源见 `docs/design-mockups`）。
- 顶部为返回按钮和标题“设置”。
- 设置项按账号与安全、服务端设置、通用设置、关于分组，每组使用毛玻璃列表卡。
- 每个设置项左侧可放蓝色线性图标，右侧展示当前值和箭头。
- 同步状态使用绿色 `StatusPill`，缓存大小使用弱文本。
- 退出登录使用红色描边大按钮，不能使用蓝色主按钮。

## 验收标准

- 设置页能独立承接所有基础运维入口。
