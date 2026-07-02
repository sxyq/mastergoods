# B11 2026-06-30 后端 smoke 恢复通过摘要

| 字段 | 内容 |
|---|---|
| 时间 | 2026-06-30 15:12 CST |
| 执行人/agent | Codex |
| 代码状态 | `HEAD=128a3d56`；工作树为 dirty |
| 命令 1 | `bash ./tools/b11_acceptance_check.sh backend-smoke > docs/acceptance-evidence/b11/backend/20260630-1512-backend-smoke-pass.log 2>&1` |
| 命令 2 | `bash ./tools/b11_acceptance_check.sh backend-bootjar > docs/acceptance-evidence/b11/backend/20260630-1512-backend-bootjar-pass.log 2>&1` |
| 结果 | PASS |
| 摘要 | 本轮先修复了当前工作树中阻塞 `backend-smoke` 的两处问题：1）`V2PurchaseReturnService` 对 repository 返回列表排序前先复制到 `ArrayList`，避免对不可变/受管理列表原地排序；2）`V1FinanceCompatibilityControllerTest` 当前真实响应字段为 camelCase `recordNo`，测试断言同步改为当前契约。修复后重新执行 `backend-smoke` 与 `backend-bootjar`，均 `BUILD SUCCESSFUL`。 |
| 关键输出 | `backend-smoke`: `BUILD SUCCESSFUL in 12s`；`backend-bootjar`: `BUILD SUCCESSFUL in 4s` |
| 附件 | `docs/acceptance-evidence/b11/backend/20260630-1512-backend-smoke-pass.log`、`docs/acceptance-evidence/b11/backend/20260630-1512-backend-bootjar-pass.log` |

