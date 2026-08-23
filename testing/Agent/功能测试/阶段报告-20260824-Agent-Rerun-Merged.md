# Agent 当前重测最终合并报告（2026-08-24）

## 合并结论

本报告合并 B 的当前服务 live 门禁结果和 C 的本地 Gradle 证据。B 的 live_scope 结果优先；C 的本地单元/专项通过只作为 `unit_scope_evidence`，没有升级为 Agent E2E 通过。

当前 94 条逻辑用例的合并结果为：`Blocked=83`、`Deferred=11`、`Passed=0`、`Failed=0`。B 的 401 认证门禁阻止了所有真实业务 HTTP/SSE 结果，历史 154/8220、旧 Provider、旧容器/数据库和旧部署结果未计入。

## 94 条结果

| 范围 | 数量 | Live Blocked | Live Deferred | C unit_scope_evidence |
|---|---:|---:|---:|---:|
| `AG-FT-BE-ALL-001..063` | 63 | 62 | 1 | 15 |
| `LOOP-001..010` | 10 | 4 | 6 | 10 |
| `DRAFT-001..003` | 3 | 3 | 0 | 3 |
| `CTX-001..014` | 14 | 12 | 2 | 14 |
| `REC-001..002` | 2 | 0 | 2 | 2 |
| `SEARCH-001..002` | 2 | 2 | 0 | 1 |
| **合计** | **94** | **83** | **11** | **45** |

Deferred live 逻辑项为：`AG-FT-BE-ALL-057`、`LOOP-003`、`LOOP-004`、`LOOP-006`、`LOOP-008`、`LOOP-009`、`LOOP-010`、`CTX-005`、`CTX-010`、`REC-001`、`REC-002`。

## C 本地证据

- Full suite：592 tests，591 passed，1 failed；日志为 `testing/.artifacts/2026-08-24-agent-rerun-C/gradle-test.log`。
- 唯一失败测试：`com.zhihuiji.backend.api.controller.V2BillDomainControllerTest.purchaseReceiptListReturnsSnakeCaseFields()`，原因是 `java.lang.AssertionError: No value at JSON path "$.data[0].receipt_no"`，源码行 `V2BillDomainControllerTest.java:221`。
- XML：`testing/.artifacts/2026-08-24-agent-rerun-C/full-suite/junit-test-results/TEST-com.zhihuiji.backend.api.controller.V2BillDomainControllerTest.xml`。
- HTML：`testing/.artifacts/2026-08-24-agent-rerun-C/full-suite/gradle-test-report/classes/com.zhihuiji.backend.api.controller.V2BillDomainControllerTest.html`。
- 该失败属于账单控制器测试，没有按 `source_test_id` 匹配到 94 条 Agent 逻辑用例，因此没有虚构 Agent `Failed` 行；相关 Agent case 仍按 live 优先结果和 C `Deferred` 记录。
- Agent core：274/274；owner scope：22/22；SQL migrations：19/19。对应日志为 `agent-core-gradle.log`、`owner-scope-gradle.log`、`sql-migrations-gradle.log`。
- C 台账 94 条均为 `Deferred`；其中 45 条的 `actual` 和 `current_rerun_20260824` 保留 `unit_scope_evidence=Passed`。

## 矩阵更新证据

更新对象：`testing/Agent/功能测试/functional_feature_matrix.csv` 的最后字段 `current_rerun_20260824`。

| 项目 | 更新前 | 更新后 |
|---|---:|---:|
| 物理文件行数 | 2107 | 2107 |
| 物理数据行数 | 2106 | 2106 |
| 逻辑 source ID | 94 | 94 |
| 明确匹配逻辑 ID | 0 | 94 |
| 明确匹配物理行 | 0 | 278 |
| 未匹配物理行 | - | 1828 |
| malformed 结构行 | 8 | 8（原样保留） |
| 重复 scenario ID | 1 | 1（原样保留） |

匹配规则为 `scenario_id` 或 `test_id` 精确匹配。34 个逻辑 ID通过 `scenario_id` 匹配，60 个逻辑 ID通过 `test_id` 匹配，合计 94 个，无未匹配逻辑 ID。历史重复执行行全部保留，因此物理匹配行数为 278。

未匹配逻辑 ID清单：`无`。未匹配物理行不是当前 94 条逻辑用例，当前字段统一保持 Deferred。物理结构异常行号为 4-8、1807-1809；没有重排、修复或删除这些历史行。

HEAD 前缀校验确认 2106 条数据行除最后字段外保持不变。当前字段物理结果分布为 `Blocked=264`、`Deferred=1842`；该物理分布包含历史执行重复行，94 条逻辑统计仍以本报告上表为准。

## B live 证据

- B 结果台账：`testing/Agent/功能测试/agent-rerun-20260824-B.csv`。
- B 报告：`testing/Agent/功能测试/阶段报告-20260824-Agent-Rerun-B.md`。
- B 已确认 `127.0.0.1:8080` Agent 入口返回 401，缺少安全认证、当前 Provider、PostgreSQL、第二 owner/store 和可确认部署 provenance；没有发送业务 POST/SSE。
- B 的提交 `1f0fd320` 只包含 B 自有 CSV/报告，作为本合并的 live 输入读取，不重新提交。

## 历史限制与校验

- 历史 malformed 行、重复 scenario ID、旧环境字段和旧证据路径均保留为历史材料，不改变历史字段值。
- `git diff --check` 已执行；矩阵原始历史行含既有 trailing-whitespace 告警，按保留物理行和旧值要求未清理。该告警不代表新增业务字段被改写。
- 本阶段没有重跑 Gradle、HTTP、SSE、Provider、数据库或性能测试。

## 提交边界

允许提交且仅提交：

- `testing/Agent/功能测试/agent-rerun-20260824-C.csv`
- `testing/Agent/功能测试/阶段报告-20260824-Agent-Rerun-C.md`
- `testing/Agent/功能测试/functional_feature_matrix.csv`
- `testing/Agent/功能测试/阶段报告-20260824-Agent-Rerun-Merged.md`

用户 docs、B CSV、B 报告、`testing/.artifacts`、业务源码、迁移、配置和 `data` 目录均排除。
