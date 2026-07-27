# data/sync 模块开发说明

- 当前状态：`SyncRepository` 已实现 `/v1` 基础手动同步；`SyncV2Repository` 已实现 `/v2` health/cursor/pull/ack/upload/import-jobs 与 inventory 读模型首轮承接。
- 实际源码目录：`data/sync/src/main/java/com/zhihuiji/data/sync`
- 目标：封装健康检查、手动同步、游标持久化和拉取变更应用。

## 需要创建的类

- `SyncRepository`
- `ManualSyncUseCase`
- `SyncV2Repository`

## 需要实现的关键函数

- `healthCheck(): SyncHealthResult`
- `pull(clientId: String, cursorMap: Map<String, Long>): PullResult`
- `upload(clientId: String, changes: List<PendingChange>): UploadResult`
- `applyPulledChanges(result: PullResult)`
- `runManualSync()`
- `clearSyncState()`
- `/v2`：
  - `health()/cursor()/acknowledgeCursor()/pull()/upload()`
  - `listImportJobs()/getImportJob()/createImportJob()/retryImportJob()/cancelImportJob()`
  - `listInventoryLedger()/listInventoryLedgerBySource()/createInventoryLedgerEntry()`
  - `listInventorySnapshots()/createInventorySnapshot()/listInventoryMonthlyStats()`

## 风险说明

- 当前后端 `upload` 更偏向上传游标，不是完整离线回写。
- 第一版建议同步只做“服务端拉取 + 本地缓存更新”。

## 验收标准

- 设置页能查看同步健康状态并触发手动同步。

## UI 设计规范支撑

- 同步健康状态要能展示为设置页的绿色或红色状态标签。
- 手动同步需要提供加载中、成功、失败三种 UI 状态。

## UI 联动约束

- 本模块虽然不直接负责页面绘制，但其输出的数据结构、状态枚举、错误语义和交互支撑能力必须服务于统一的 Android UI 基线。
- 后续新增业务不能倒逼页面切换成另一套视觉风格；应优先通过补充 `core/designsystem` 通用组件或扩展既有页面母版来承接。
- 需要映射到 UI 的状态、金额、风险、同步结果等，应继续服从统一的颜色语义、状态标签和信息层级。
- Android 当前视觉真源以 Stitch 导出、`docs/spec/42-android-liquid-glass-ui-refactor-plan.md` 与 `master-goods-android/UI-DESIGN-SPEC.md` 为准；`docs/design-mockups/` 仅作历史参考。
