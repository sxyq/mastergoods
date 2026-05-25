# data/sync 模块开发说明

- 当前状态：基础手动同步已实现（健康检查、pull、变更应用、游标持久化）；后台同步、离线回写和 WorkManager 仍开发中。
- 实际源码目录：`data/sync/src/main/java/com/zhihuiji/data/sync`
- 目标：封装健康检查、手动同步、游标持久化和拉取变更应用。

## 需要创建的类

- `SyncRepository`
- `ManualSyncUseCase`

## 需要实现的关键函数

- `healthCheck(): SyncHealthResult`
- `pull(clientId: String, cursorMap: Map<String, Long>): PullResult`
- `upload(clientId: String, changes: List<PendingChange>): UploadResult`
- `applyPulledChanges(result: PullResult)`
- `runManualSync()`
- `clearSyncState()`

## 风险说明

- 当前后端 `upload` 更偏向上传游标，不是完整离线回写。
- 第一版建议同步只做“服务端拉取 + 本地缓存更新”。

## 验收标准

- 设置页能查看同步健康状态并触发手动同步。

## UI 设计规范支撑

- 同步健康状态要能展示为设置页的绿色或红色状态标签。
- 手动同步需要提供加载中、成功、失败三种 UI 状态。
