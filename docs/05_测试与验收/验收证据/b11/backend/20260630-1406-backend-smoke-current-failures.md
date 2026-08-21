# B11 2026-06-30 backend-smoke 当前工作树复验失败记录

| 字段 | 内容 |
|---|---|
| 时间 | 2026-06-30 14:06 CST |
| 执行人/agent | Codex |
| 代码状态 | `HEAD=128a3d56`；工作树为 dirty，`git status --short` 含 Android/Web/文档与用户侧 iOS/后端在途改动 |
| 命令 | `bash ./tools/b11_acceptance_check.sh backend-smoke` |
| 结果 | FAIL |
| 摘要 | 当前工作树下 `backend-smoke` 不再保持历史 PASS，实际执行在 216 个测试中出现 3 个失败，因此 B11 后端本地自动化在“发布可交付”口径下不能判为当前完成。 |
| 失败项 | `V1FinanceCompatibilityControllerTest > v1FinanceRecordsDoNotExposeB04ExpansionFields()`；`V2PurchaseReturnServiceTest > listUsesBatchQueryForItemsAndRefunds()`；`V2PurchaseReturnServiceTest > listByOrderUsesBatchQueryForItemsAndRefunds()` |
| 关键输出 | `216 tests completed, 3 failed`；`BUILD FAILED in 29s`。其中 finance 兼容测试实际响应体显示 `recordNo/partnerName/createdAt` 为 camelCase；purchase return 两条失败为 `UnsupportedOperationException`。 |
| 证据定位 | `build/test-results/test/TEST-com.zhihuiji.backend.api.controller.V1FinanceCompatibilityControllerTest.xml` 已直接记录 finance 兼容测试失败，并包含 MockMvc 实际响应体；终端复验输出记录其余两条 `V2PurchaseReturnServiceTest` 失败位置分别为第 89/105 行。 |
| 仍未动作 | 按用户当前边界，本轮未修改任何服务器后端，也未修改本地后端实现或后端测试；这里只把当前失败态落为审计证据。 |
