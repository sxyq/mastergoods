# 阶段报告：20260824 Agent Rerun C

## 当前状态

本轮只根据已有 artifacts 收尾，没有再次运行测试。基线 commit 为 `3ced2d07acac6505e3fcb557f994b09f7db64c31`。本 C 阶段没有修改业务源码、迁移、配置、用户 docs 或 B 报告；共享矩阵在最终合并阶段按物理行末字段更新。

## 94 条结果

独立台账 `testing/Agent/功能测试/agent-rerun-20260824-C.csv` 覆盖 94/94 条基线，每条都有本轮 `test_id`、`source_test_id` 和 `result`。本地单元/专项通过只记录为 `unit_scope_evidence`，完整 Agent case 因未执行真实 HTTP、Provider、生产数据库、第二 owner 或组合链路统一为 `Deferred`。没有使用 154/8220 历史结果替代本轮结果。

| 范围 | 总数 | Passed | Failed | Blocked | Deferred |
|---|---:|---:|---:|---:|---:|
| AG-FT-BE-ALL-001..063 | 63 | 0 | 0 | 0 | 63 |
| LOOP-001..010 | 10 | 0 | 0 | 0 | 10 |
| DRAFT-001..003 | 3 | 0 | 0 | 0 | 3 |
| CTX-001..014 | 14 | 0 | 0 | 0 | 14 |
| REC-001..002 | 2 | 0 | 0 | 0 | 2 |
| SEARCH-001..002 | 2 | 0 | 0 | 0 | 2 |
| 合计 | 94 | 0 | 0 | 0 | 94 |

## 已执行结果

- Full suite：`./Code/backend/gradlew -p Code/backend test`，结果 `Failed`，592 tests，591 passed，1 failed；该失败没有匹配到 94 条 Agent source ID。
- Agent core：`agent-core-gradle.log`，结果 `Passed`，274/274 tests passed。
- Owner scope：`owner-scope-gradle.log`，结果 `Passed`，22/22 tests passed。
- SQL migrations：`sql-migrations-gradle.log`，结果 `Passed`，19/19 tests passed。

Full suite 唯一失败来自 Gradle JUnit XML 和 HTML 报告：`com.zhihuiji.backend.api.controller.V2BillDomainControllerTest.purchaseReceiptListReturnsSnakeCaseFields()`，异常为 `java.lang.AssertionError`（`java.lang.AssertionError: No value at JSON path "$.data[0].receipt_no"`）。HTML 报告确认：`是`。它属于非 Agent 账单控制器测试，没有计入 94 条 Agent 结果。

## 本地断言证据

- Loop 终态：`V2AgentToolSelectionRegressionTest`。
- native transcript `call_id` 配对：`LongCatAnthropicClientTest`。
- `tool_started`、`tool_completed`、`tool_failed`、formal answer、audit/run-trace：`V2AgentAiServiceTest`、`V2AgentConversationServiceTest`。
- draft boundary：`AgentDraftConfirmServiceTest`、`V2AgentToolSelectionRegressionTest`。
- `context_compacted`：`V2AgentAiServiceTest`。
- checkpoint 复用、失效、并发、隔离和失败降级：`ContextCompactionServiceTest`、`ContextBuilderTest`、`ContextWindowResolverTest`、`AgentContextCheckpointRepositoryTest`。
- `ToolExecutor` 没有独立测试类，本轮通过 `V2AgentAiServiceTest` 和 `V2AgentToolSelectionRegressionTest` 的当前源码路径覆盖。

## 证据与缺口

原始日志、JUnit XML、Gradle HTML 报告和脱敏本地 trace 位于 `testing/.artifacts/2026-08-24-agent-rerun-C/`。报告副本中的 Spring Boot 自动生成开发密码已替换为 `[REDACTED]`，未保留凭据、Cookie、Token 或完整认证载荷。

真实 HTTP、Provider、生产数据库、第二 owner 和组合链路未执行，94 条完整 case 均保持 `Deferred`。45 条本地范围通过只写入独立 CSV 的 `actual` 和 `current_rerun_20260824` 的 `unit_scope_evidence`，不计入 E2E 通过率。具体说明在独立 CSV 的 `actual`、`evidence_path` 和 `cleanup` 字段中。
