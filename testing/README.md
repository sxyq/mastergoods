# Master-Goods Testing Execution Index

## Objective

本目录用于承载可立即执行的测试手册，而不是泛化测试建议。

当前执行口径只做一件事：

1. 先按分类总台账锁定范围
2. 再按单元/功能/性能手册分 Wave 执行
3. 每执行一批，就更新对应 CSV 账本和证据

## Execution Start Baseline

当前验证和生产目标后端为 `8.220.206.9`，公共 API 入口为 `https://zhj-api.sxyq27.online/`。
`154.217.241.207` 已完全退役，只保留历史证据，不参与当前运行状态、通过率或发布验收。

当前 8220 重测报告：`testing/FINAL-REPORT-20260820-8220.md`。
本轮证据目录：`testing/.artifacts/2026-08-20-8220-agent-retest/`。

以下旧数据属于 154 历史 baseline，不得回写成当前 8220 结果；当前 8220 数据以本轮报告和 `testing/current-8220-wave-ledger.csv` 为准。

当前 8220 业务基线为：`users=2`、`stores=1`、`store_memberships=1`、`active_sessions=3`、`products=693`、`customers=83`、`suppliers=358`、`sale_orders=3846`、`purchase_orders=7`、`finance_records=2661`、`inventory_snapshots=693`、`accounts=5`、`payments=904`、`agent_conversations=2`、`agent_messages=4`、`agent_drafts=0`、`media_assets=0`、`media_bindings=0`。

当前执行规则：

1. Auth、session、store context、Agent conversation/message/audit/draft 相关测试，只能复用当前 8220 的有效数据。
2. 业务主表相关场景不建立长期种子库，只在执行时补最小夹具。
3. 最小夹具必须绑定现有 owner/store。
4. 每次补数必须在证据中记录：创建前状态、创建动作、验证结果、清理动作。
5. 154 上的容器、配置、数据库、模型和测试结果统一标记为 `historical-only`。

## Current 8220 Baseline (2026-08-20)

- 8220 当前运行 `sxyq27-zhj-api:20260818`，运行 JAR SHA-256 为 `4289b73346780986647ed1140fa92552656e7cc2ba53a54826e8eaa01832edd2`。
- Provider 当前为 `gpt-5.6-luna`、`https://oneapi.sxyq27.online/v1`、`chat_completions`；Key 只保存在运行时 Secret。
- Provider 非流式、SSE 和 `tool_choice=auto` 直连验证均通过。
- 当前数据库为 V29；本轮使用 owner `2`、store `1` 和真实业务数据完成后端 API 与 Agent 重测。
- Agent 60 个真实 case 中 50 个 Passed、10 个 Failed；失败集中在正式回答为空、目标工具未选择和写工具未生成草稿。
- 历史会话列表与 workbench/run-traces 结果不一致，显式 `conversation_id` 聊天返回 400，历史恢复当前保持 Failed。
- 第二个用户没有有效门店和 session，完整双 owner 验证保持 Blocked。
- 后端 JUnit 为 462 tests，0 failures、0 errors、0 skipped；这项结果与线上 API、Agent 运行结果分开统计。
- Android 本地构建和相关单元测试通过；本机没有 `adb` 命令，Android 真机 Wave 标记为 Blocked。
- 多模态、生图、图片输入和图片结果展示登记为 Deferred。

旧的 154 记录从这里开始只作为历史执行快照保留。

## Historical Execution Snapshot (154, not current)

更新时间：2026-07-27

- Android 真机 `d715a3a4` 当前在线，设备型号 `25010PN30C`。
- 当前 debug APK 已完成构建、覆盖安装和冷启动，安装时保留了既有登录数据。
- 154 容器实际 LLM 配置为 `enabled=true`、`model=gpt-5.6-luna`、`wire_api=responses`；2026-07-26 已完成服务端重部署。
- 业务 Agent 的原生工具链已通过真实回归：普通问题只返回文本；图表问题由模型选择 `sales_trend_lookup -> result_visualization` 后才返回 `kpi_grid/line_chart`。provider 隔离探针的标准 `function_call_output` 续轮仍返回 HTTP 400，保留为独立已知问题。
- 2026-07-27 真机冷启动复核：设备 `d715a3a4` `am start -W` 为 `925ms`；首页恢复当前账户真实数据（`¥37.00`、`2` 张真实订单）；正常导航进入 Agent 后显示已连接空态且不展示默认图表；本轮入口 evidence 位于 `testing/.artifacts/2026-07-27-final-android/`。
- 2026-07-27 单次 Android 性能快照：`349` 帧、`2` 个 janky frame（`0.57%`）、p95 `53ms`、p99 `250ms`；首页 PSS `224375KB`、Agent 入口 PSS `219165KB`。该快照已登记为“部分完成”，不能替代重复 p95、Perfetto 或 Macrobenchmark。
- run `2052a7da-17f0-4156-8a72-e8c0ebbd6834` 已证明线上可以调用真实 LLM 和 owner-scoped 工具，但工具参数、SSE 终态和 Android 当前会话展示仍有失败。
- 30 轮只读长历史测试创建的临时 conversation `64` 已删除；其 32 条历史消息仅用于执行期真机性能取证。
- 现有业务数据已包含前序 Wave 创建并保留的最小夹具，不能再按起始空表直接推断当前行数。每个后续场景必须先读取 pre-state。
- 已重启并部署 154 后端，仅替换应用构建；PostgreSQL 数据与结构未修改。

专项结论：

- `testing/.artifacts/2026-07-19-agent-llm-live-recheck/gpt-5.6-luna-readonly-investigation.md`
- `testing/.artifacts/2026-07-19-agent-llm-live-recheck/gpt-5.6-luna-provider-probe.md`
- `testing/已知问题与解除条件.md`

## Environment Matrix

常规测试 lane：

- `单元测试`
- `功能测试`
- `性能测试`
- `审计`

独立 lane：

- `破坏性逆向安全测试`

当前立即执行的三大主体：

- `testing/后端`
- `testing/安卓`
- `testing/Agent`

其中 Agent 本轮主执行只覆盖安卓与后端，不展开 Web/iOS 主流程。

## Execution Waves

统一术语：

- `Wave 0`: 环境、账号、数据基线确认
- `Wave 1`: 直接复用当前 8220 有效数据即可执行的场景；当前数据不足时登记为 `Blocked`
- `Wave 2`: 需要最小夹具补数的业务场景
- `Wave 3`: 长链路、高负载、稳定性场景
- `Blocked`: 前置能力缺失，允许登记，不得伪造通过

统一执行顺序：

1. 先执行 `Wave 0`
2. 再执行所有 `Wave 1`
3. 再进入 `Wave 2`
4. 最后执行 `Wave 3`

## Per-Category Execution Rules

所有主体都必须先查看并回链以下账本：

- `测试分类总台账.csv`
- `单元测试/unit_function_coverage.csv`
- `功能测试/functional_feature_matrix.csv`
- `性能测试/performance_scope_matrix.csv`

当前 Agent 方案与台账入口：

- `testing/Agent/Agent综合功能与性能测试方案.md`
- `testing/Agent/Agent执行台账.csv`
- `testing/Agent/Agent问题台账.csv`

其他主体的本轮真实执行明细同步到以下 live ledger：

- `testing/后端/功能测试/live_execution_ledger.csv`
- `testing/安卓/功能测试/live_execution_ledger.csv`
- `testing/安卓/性能测试/live_execution_ledger.csv`

执行要求：

1. 每个场景必须带 `category_id`
2. 每个函数级或场景级记录必须能回查到分类总台账
3. 所有测试状态统一落在台账，不在聊天里代替登记
4. 常规测试与逆向安全测试不得混账

## Evidence Template

所有三部分手册统一使用以下证据字段：

- `test_id`
- `category_id`
- `wave_id`
- `env`
- `account/store`
- `pre_state`
- `actions`
- `expected`
- `actual`
- `artifacts`
- `cleanup`
- `result`

## Stop Rules / Blocker Handling

以下情况统一记为 `Blocked`：

- 环境未起
- 权限未通
- 数据前置缺失
- 目标接口未启用
- 大模型 provider 未配置
- 生图 provider 未配置
- 真机不可复现

禁止写成：

- `Passed with note`
- `基本通过`
- `大致可用`

## Exit Criteria

1. 所有执行者看到文档后，不需要再决定“先测什么、用什么数据、怎么留证”。
2. 三部分九份 `TEST_PLAN.md` 都按统一 Wave 和证据模板执行。
3. 所有 live data 假设都写在文档中，不依赖聊天上下文。

## Current Release Gate (8220)

当前不满足“可进入发布前验收”。完整结果以 `testing/FINAL-REPORT-20260820-8220.md` 为准。

旧分类总台账快照仍保留在仓库中，但它们来自 154 或旧入口，不能代表当前 8220 通过率。

当前 8220 门禁摘要：

| 范围 | Passed | Failed | Blocked | Deferred | 当前说明 |
| --- | ---: | ---: | ---: | ---: | --- |
| 8220 环境、Provider、原生续轮和后端单元测试 | 1 | 0 | 0 | 0 | 运行 provenance、Provider 探针和462个 JUnit 测试完成 |
| 8220 真实后端 API | 2 | 2 | 0 | 0 | CRUD、PDF通过；付款路径缺失和媒体文件清理风险保留 |
| 8220 Agent 全量工具 | 50 | 10 | 0 | 0 | 60个真实 case，失败集中在工具选择、正式回答和草稿生成 |
| 8220 Agent 历史恢复 | 0 | 1 | 0 | 0 | 会话列表、workbench、run-traces结果不一致；显式会话请求返回400 |
| 8220 Agent owner 隔离 | 0 | 0 | 1 | 0 | 第二用户缺少有效门店和 session |
| 8220 SSE、取消和审计 | 1 | 0 | 0 | 0 | 当前10路并发和取消样本完成 |
| Android 真机 | 0 | 0 | 1 | 0 | 本机没有 adb 或在线设备 |
| Android 多模态 | 0 | 0 | 0 | 1 | 等待 Provider 与设计资料 |

当前摘要不把历史 154 台账与 8220 结果合并；`Blocked` 和 `Deferred` 均不计入 Passed。

1. Android 收到后端终端 SSE 事件后仍可能等待异常 EOF，并把已完成 run 误报为流式中断。
2. 当前 8220 已完成 Provider 非流式、SSE、`tool_choice=auto` 和原生 `function_call -> function_call_output` 续轮验证；Agent 全量功能仍有10个 Failed case。
3. 只读提示中的否定词会被写入频率限制误判。
4. IME 打开后顶部栏与状态栏重叠。
5. 32 消息长历史滚动 jank `16.76%`，PSS 单轮增长 `32688 KB`。
6. 生图 provider 未配置；相关多模态项保持 `Deferred`，不计入常规通过项。
7. 当前 Agent 历史恢复、显式 `conversation_id` 请求、第二 owner 隔离和10个失败工具 case仍需修复或补测；旧 154 结果继续按历史资料处理。
