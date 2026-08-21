# B11 2026-06-30 Web EntityId 收口后生产构建复验

| 字段 | 内容 |
|---|---|
| 时间 | 2026-06-30 13:51 CST |
| 执行人/agent | Codex |
| 代码状态 | `HEAD=128a3d56`；工作树为 dirty，包含本轮 Web/Android/docs 在途改动 |
| 命令 | `cd web && npm run build` |
| 结果 | PASS |
| 摘要 | 在继续把 `web/src/shared/api/client.ts` 中残留的实体主键型字段收口到 `EntityId` 后，重新执行了 Web 生产构建，`vue-tsc -b && vite build` 通过。说明本轮对 `AdminUser`、`ProductPriceLevelValue.levelId`、`ProductSupplierRelation.id/productId/supplierId`、`FinanceRecord.id`、`ImportJob.id`、`AgentTask.id`、`AgentNotification.id/taskId`、`AgentWorkbench.recentConversations[].id`、`pendingDrafts[].id`、`InventoryLedgerCreatePayload.warehouseId` 等实体 ID 口径的继续收口，没有打坏当前 Web 生产构建链。同时已额外验证 Web 运行时解析链路会先通过 `preserveUnsafeIntegers()` 再 `JSON.parse()`，能把超出 `Number.MAX_SAFE_INTEGER` 的后端长整型 ID 保成字符串。 |
| 关键输出 | `vite v4.5.14 building for production...`；`✓ 116 modules transformed.`；`dist/assets/index-a2b0d832.js`；`✓ built in 2.11s`；运行时验证样例 `{\"data\":{\"id\":9223372036854775807,\"nested\":{\"taskId\":9007199254740993},\"small\":123}}` 在 `preserveUnsafeIntegers()` 后被解析为 `id: \"9223372036854775807\"`、`taskId: \"9007199254740993\"`。 |
| 已确认收口 | `临时.md` 所要求的 Web 侧“实体 ID 不得再以 `Number()`/不安全 number 主键语义处理”的已知 blocked finding 进一步缩小；当前共享 API 合同里又收口了一批原先仍写成 `number` 的实体主键字段，而且运行时 JSON 解析链路也已证明会保留超大整型 ID 的精度。 |
| 仍未证明 | 这条证据只证明 Web 生产构建链和类型口径当前通过，不替代 Android 真机、性能、安全、生产链路联调。 |
| 附件 | 当前 turn 命令输出；相关源码见 `web/src/shared/api/client.ts`、`web/src/app/stores/session.ts`、`web/src/pages/settings/RoleAccessPage.vue`、`web/src/pages/agent/AgentPage.vue`、`web/src/entities/screen/live-screen-data.ts` |
