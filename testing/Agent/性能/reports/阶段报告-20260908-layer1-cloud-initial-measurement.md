# Agent 性能阶段报告：第一层失败定位与稳定复测

账号范围：8220 测试服务器上的所有账号都是测试账号，均可用于本项目测试。

## 当前需求与状态

> 文档按波次保留历史记录。最新状态见文末“第二阶段执行记录（2026-09-22）”；其他日期的结果只适用于各自波次。

当前目标为完成 AG-P-001 至 AG-P-027 的第二阶段性能专项。27 项仍为 9 项 `Passed-small-sample`、1 项 `Failed`、3 项 `Failed-small-sample`、7 项 `Blocked`、7 项 `Deferred`，计划级 `Passed` 为 0。第二阶段未收口，671 条功能分支、三端正式测试和日报/周报/月报验证均未启动。

2026-09-22 的 GLM Stream R04 在 1 条预热后停止：`LLM_ANSWER_UNAVAILABLE` 1 条，正式请求 0/30；证据为 `1009–1016`。cleanup HTTP 200，43 张业务表计数未变，31 个既有会话和 2 条 ui-seed 运行记录均保留。历史 CSV 仍有 19 行列数异常，原文尚未改动；本轮波次记录按 20 列追加。

### 第一层历史状态（2026-09-14）

当前需求与状态：第一层真实云端 Agent 性能优化闭环。用户指定的候选镜像为 `sxyq27-zhj-api:20260913T-schema-qwen-status-enum`，但正式波次开始前只读核实到实际运行镜像为 `sxyq27-zhj-api:20260914T0405-agent-integer-string-normalization`；本轮未切换环境，稳定复测继续使用后者和 `qwen3.8-flash`。当前接受的并发 1/2/4/8 均为 20/20，上一轮 Provider 限流导致的 4 条 `LLM_ANSWER_UNAVAILABLE` 单独保留；第二层和 671 条 Agent 测试仍未启动。文末章节为当前判定。

- 本轮实际运行容器为 `sxyq27-zhj-api:20260914T0405-agent-integer-string-normalization`，启动时间为 `2026-09-13T20:07:58.416162103Z`；历史镜像和历史完整并发结果继续按原波次保留，未与本轮样本合并。
- 本轮诊断波次先完成并发 1、2；Provider 限流波次停止后，在同一组合的稳定窗口完成并发 2、4、8，各级均为 2 个预热和 20 个正式请求。
- 当前正式组合的 Agent、Provider、模型、Wire API、SSE 地址和账号标签已核实一致；应用日志已按 `run_id` 关联到 Provider 429，Provider request ID、upstream request ID 和 `Retry-After` 仍未取得。
- 第二层已完成数量：0；第二层剩余内容：尚未开始。
- 第一层统一复测已完成；第二层性能专项测量和后续性能动作本轮不自动开始。
- 671 条 Agent 功能测试不允许开始。

- 当前 Goal：完成第一层真实云端 Agent 性能优化闭环。
- 当前线上镜像：`sxyq27-zhj-api:20260914T0405-agent-integer-string-normalization`，容器启动于 `2026-09-13T20:07:58.416162103Z`。
- `product_catalog_lookup.status` 的整数枚举及整数字符串规范化属于此前已部署改动；本轮没有新增源码、测试文件、采集器脚本或部署配置，也没有重新部署云端。

## 本轮实际完成

- 使用真实云端账号 `suffix-2002` 完成认证，密码只从本机 Keychain 读取并留在进程内存。
- 复核 Agent、Provider、模型、Wire API、线上镜像和容器启动时间。
- 复核此前真实云端 SSE 采集器完成的并发 1、2、4、8 波次；每级 2 个预热请求和 20 个正式请求，未执行并发 16。该波次使用修复前线上镜像，保留为失败定位证据。
- 每条正式请求即时写入 JSONL 并执行 flush/fsync；预热请求摘要保存在 `49-stability-rerun-summary.json`；查询 audit 后再清理会话。
- 远程资源采样 20/20 次 SSH 返回 0，覆盖正式请求运行期间。
- 取得同一轮数据库起点和终点，只读验证业务表未发生非预期变化。
- 在修复镜像上追加小规模复测：并发 1、2、4 各 2 个预热请求和 5 个正式请求；未新增并发 8 或 16。
- 修复后小规模复测共 15 个正式请求、6 个预热请求，逐条 JSONL 写入并执行 flush/fsync；资源采样 15/15 次 SSH 返回 0。
- 将 `TOOL_ARGUMENTS_INVALID` 与通用 `TOOL_QUERY_FAILED` 分开记录；采集器按安全摘要区分参数、数据库、权限和超时类别。
- 在修复镜像上重新执行完整复测并发 1、2，各 2 个预热和 20 个正式请求；并发 2 出现 1 个 `LLM_ANSWER_UNAVAILABLE` 后停止后续级别。
- 失败请求、SSE、audit、数据库、资源和 Provider 日志时间窗均已保存；没有修改源码或再次部署。
- 历史核对曾确认 `sxyq27-zhj-api:20260911T1900-agent-audit-counter-fix`；本轮当前运行版本以文末“最后并发级别复测”记录为准。
- 本轮在当前镜像完成 4 个预热请求和 40 个正式请求：并发 1、2 各 2 个预热和 20 个正式请求；每条正式记录均即时写入 JSONL 并执行 flush/fsync，先查询 audit 后清理会话。
- 当前镜像并发 1、2 均为 HTTP 200、20/20 `COMPLETED`，Agent 侧 429/5xx/超时、工具错误、SSE 丢失、SSE 重复、终态异常、身份混合和 audit 计数不一致均为 0；40/40 清理成功。
- 当前镜像并发 1 证据为 `120-audit-counter-fix-full-c1-20260912-collector-format-validation.json`、`121-audit-counter-fix-full-c1-20260912-db-before.json`、`122-audit-counter-fix-full-c1-20260912-warmup.jsonl`、`123-audit-counter-fix-full-c1-20260912-concurrency-1.jsonl`、`124-audit-counter-fix-full-c1-20260912-failure-analysis.json`、`125-audit-counter-fix-full-c1-20260912-resource-samples.jsonl`、`126-audit-counter-fix-full-c1-20260912-summary.json`、`127-audit-counter-fix-full-c1-20260912-db-after.json`。
- 当前镜像并发 2 证据为 `128-audit-counter-fix-full-c2-20260912-collector-format-validation.json`、`129-audit-counter-fix-full-c2-20260912-db-before.json`、`130-audit-counter-fix-full-c2-20260912-warmup.jsonl`、`131-audit-counter-fix-full-c2-20260912-concurrency-2.jsonl`、`132-audit-counter-fix-full-c2-20260912-failure-analysis.json`、`133-audit-counter-fix-full-c2-20260912-resource-samples.jsonl`、`134-audit-counter-fix-full-c2-20260912-summary.json`、`135-audit-counter-fix-full-c2-20260912-db-after.json`。
- 当前镜像并发 4 证据为 `137-audit-counter-fix-full-c4-20260912-collector-format-validation.json`、`138-audit-counter-fix-full-c4-20260912-db-before.json`、`139-audit-counter-fix-full-c4-20260912-warmup.jsonl`、`140-audit-counter-fix-full-c4-20260912-concurrency-4.jsonl`、`141-audit-counter-fix-full-c4-20260912-failure-analysis.json`、`142-audit-counter-fix-full-c4-20260912-resource-samples.jsonl`、`143-audit-counter-fix-full-c4-20260912-summary.json`、`144-audit-counter-fix-full-c4-20260912-db-after.json`。
- 使用当前修复镜像新增 1 条正式只读 Agent 请求和 1 条预热请求，专门验证失败流的 audit 聚合计数；请求经过真实 Agent、工具、云端 PostgreSQL、SSE 和 audit。
- 该正式请求的 SSE 与 audit 均为 11 个事件，`event_count=11`、`emitted_event_count=11`，事件类型、事件 ID 和序号一致；清理 HTTP 200。
- 已读取 124 New API 的只读 SQLite 日志，取得 4 条精确上游 429 记录及 New API `request_id`；`upstream_request_id`、`Retry-After` 和 Agent 请求标识均缺失。

## 线上环境

| 项目 | 当前值 |
|---|---|
| Agent | `https://zhj-api.sxyq27.online/` |
| SSE | `/v2/agent/chat/stream` |
| Provider | `https://oneapi.sxyq27.online/v1` |
| 模型 | `deepseek-v4-flash-0731` |
| Wire API | `chat_completions` |
| API 镜像 | `sxyq27-zhj-api:20260913T0345-agent-provider-concurrency-1` |
| API 容器启动 | `2026-09-12T19:55:21.949880168Z` |
| Flyway | V44，已安装 44 个迁移，启动日志显示无需迁移 |
| 实际工具 | `product_catalog_lookup` |

## 样本与并发结果

串行正式样本 50 个：冷请求 5、热 conversation 10、单工具 20、多工具 10、REST 5，全部完成。此前完整并发波次共 80 个正式请求，全部 HTTP 200；58 个完成、22 个失败，失败率 27.5%。该波次发生在修复部署前，并发 4 和 8 的百分位只使用已完成请求计算。

| 并发 | 正式请求 | 完成 | 失败 | P50 | P95 | 最大耗时 | 错误码 |
|---:|---:|---:|---:|---:|---:|---:|---|
| 1 | 20 | 20 | 0 | 15766.83 ms | 33520.35 ms | 37317.91 ms | 无 |
| 2 | 20 | 20 | 0 | 17429.40 ms | 26743.78 ms | 28224.67 ms | 无 |
| 4 | 20 | 10 | 10 | 9101.99 ms | 24746.24 ms | 26191.62 ms | `LLM_ANSWER_UNAVAILABLE` |
| 8 | 20 | 8 | 12 | 9810.01 ms | 22820.96 ms | 24362.21 ms | `LLM_ANSWER_UNAVAILABLE` |

并发 1/2/4/8 的 HTTP 5xx、客户端 HTTP 429、缺失终态、SSE 丢失、SSE 事件 ID 重复、终态不唯一、跨请求身份混合和 audit/SSE 事件不一致均为 0。80/80 audit 查询成功，80/80 清理请求返回 HTTP 2xx。

## 小规模复现

修复后小规模复测使用相同账号、模型、输入、SSE 地址和统计方式，线上镜像为 `20260910T1440-agent-tool-error-code`。并发 8/16 没有新增请求。

| 并发 | 正式请求 | 完成终态 | 终态失败 | 降级完成 | P50 | P95 | P99 | 最大耗时 | 工具失败事件 |
|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 1 | 5 | 5 | 0 | 0 | 10473.75 ms | 16925.11 ms | 17943.37 ms | 18197.93 ms | 0 |
| 2 | 5 | 5 | 0 | 0 | 8735.06 ms | 11682.12 ms | 12221.79 ms | 12356.71 ms | 0 |
| 4 | 5 | 5 | 0 | 0 | 8606.89 ms | 12375.94 ms | 13012.30 ms | 13171.39 ms | 0 |

- 修复后 15 个正式请求均无工具失败；SSE 丢失、事件重复、序号异常、身份混合和 audit/SSE 不一致均为 0；15/15 清理请求返回 HTTP 2xx。
- 修复前小规模证据中的 1 条事件显示 `status` 提交为 boolean，而工具声明要求 integer；参数校验在工具实现和 PostgreSQL 查询前返回。`65-small-rerun-failure-analysis-corrected.json` 保留为修复依据。
- `69`–`78` 为修复后小规模原始证据与汇总，`80` 为修复后失败分析。

## 审计计数修复后 10 条小规模验证

该波次使用当前审计计数修复镜像，包含并发 1、2 各 2 个预热和 5 个正式请求，共 10 个正式请求。它是当前镜像的早期小规模验证，不能代替本报告后续的每级 20 条正式样本，也不能覆盖并发 4、8。

| 并发 | 正式请求 | 完成 | 失败 | P50 | P95 | P99 | 最大耗时 | 首事件 P50 | 首工具 P50 | 首回答 P50 |
|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 1 | 5 | 5 | 0 | 4332.94 ms | 6294.18 ms | 6447.95 ms | 6486.39 ms | 4307.92 ms | 4308.64 ms | 4308.75 ms |
| 2 | 5 | 5 | 0 | 6752.88 ms | 8669.59 ms | 8760.21 ms | 8782.87 ms | 6736.15 ms | 6736.23 ms | 6736.31 ms |

- 10/10 为 `COMPLETED`，HTTP 200、Agent 侧 429/5xx/超时、工具失败和采集器错误均为 0；SSE 丢失、SSE 重复、终态异常、身份混合和 audit 计数不一致均为 0；10/10 清理成功。
- 该波次完整证据为 `112-audit-fix-small-rerun-20260912-db-before.json`、`113-audit-counter-validation-conclusion-20260912.md`、`113-audit-fix-small-rerun-20260912-warmup.jsonl`、`114-audit-fix-small-rerun-20260912-concurrency-1.jsonl`、`115-audit-fix-small-rerun-20260912-concurrency-2.jsonl`、`116-audit-fix-small-rerun-20260912-failure-analysis.json`、`117-audit-fix-small-rerun-20260912-resource-samples.jsonl`、`118-audit-fix-small-rerun-20260912-summary.json`、`119-audit-fix-small-rerun-20260912-db-after.json`。同编号的 `112-audit-counter-root-cause-20260912.json` 为计数根因证据，不能与数据库起点文件混写。

## 旧修复镜像完整复测

本轮使用镜像 `20260910T1440-agent-tool-error-code`，按并发 1、2、4、8 顺序执行；并发 2 出现真实 Agent 失败后停止并发 4/8。

| 并发 | 正式请求 | 完成 | 失败 | P50 | P95 | P99 | 最大耗时 | 错误码 |
|---:|---:|---:|---:|---:|---:|---:|---:|---|
| 1 | 20 | 20 | 0 | 9709.10 ms | 15628.52 ms | 17092.27 ms | 17458.21 ms | 无 |
| 2 | 20 | 19 | 1 | 10828.19 ms | 13261.94 ms | 13877.37 ms | 14031.23 ms | `LLM_ANSWER_UNAVAILABLE` |
| 4 | 未执行 | - | - | - | - | - | - | 因并发 2 失败停止 |
| 8 | 未执行 | - | - | - | - | - | - | 因并发 2 失败停止 |

并发 2 的百分位只基于 19 个完成请求。40 个正式请求全部 HTTP 200；40/40 audit 查询成功，40/40 清理请求返回 HTTP 2xx。SSE 丢失、重复、终态不唯一、跨请求身份混合和 audit 事件列表与 SSE 的类型/ID/序号不一致均为 0。失败请求的 audit 顶层 `event_count=4`、`emitted_event_count=4`，但事件列表有 6 项，审计聚合计数不一致 1 条。

## 当前审计计数修复镜像完整诊断复测

早期当前镜像为 `sxyq27-zhj-api:20260911T1900-agent-audit-counter-fix`，真实链路使用 `suffix-2002`、`deepseek-v4-flash-0731`、`https://oneapi.sxyq27.online/v1` 和 `/v2/agent/chat/stream`。该段记录只覆盖并发 1、2；每级 2 个预热请求和 20 个正式请求，不能代替并发 4、8 的正式样本。最新线上镜像和完整复测结果以本报告“Provider 并发优化后的完整复测”一节为准。

| 并发 | 正式请求 | 完成 | 失败 | 首 SSE P50 | 首事件 P50 | 首工具 P50 | 首回答 P50 | 完成 P50 | 完成 P95 | 完成 P99 | 最大耗时 |
|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 1 | 20 | 20 | 0 | 10714.93 ms | 10715.83 ms | 10716.01 ms | 10716.10 ms | 10728.36 ms | 22877.92 ms | 28236.94 ms | 29576.70 ms |
| 2 | 20 | 20 | 0 | 10965.10 ms | 10965.30 ms | 10965.37 ms | 10965.43 ms | 10974.39 ms | 24090.33 ms | 27604.93 ms | 28483.58 ms |

- 40/40 正式请求为 HTTP 200 和 `COMPLETED`；`LLM_ANSWER_UNAVAILABLE`、5xx、429、超时、工具失败、采集器错误均为 0。
- 每条记录均保留 run、conversation、audit、trace、事件类型、事件 ID、序号和各时间点；并发 1 的事件数为 8–10，并发 2 的事件数为 8–11。
- 40/40 请求均只有一个合法终态，SSE 序号连续、事件 ID 不重复、SSE/audit 类型-ID-序号一致；SSE 与 audit 顶层 `event_count`、`emitted_event_count` 和持久化事件列表逐条相等，审计计数不一致为 0。
- 当前镜像 40 条正式结果是并发 1、2 诊断样本；本轮另有并发 4 的 20 条正式样本。并发 4 出现失败后按停止规则未执行并发 8。

## 当前审计计数修复镜像并发 4 复测

并发 4 执行 2 个预热请求和 20 个正式请求。因正式请求出现失败，按停止规则未执行并发 8。

| 并发 | 正式请求 | 完成 | 失败 | P50 | P95 | P99 | 最大耗时 | 错误类型 |
|---:|---:|---:|---:|---:|---:|---:|---:|---|
| 4 | 20 | 16 | 4 | 11437.15 ms | 22109.69 ms | 27177.25 ms | 28444.14 ms | `LLM_ANSWER_UNAVAILABLE` 4 |

- 20/20 为 HTTP 200；4 条失败均为 `provider_error`，audit 顶层 `llm_status=model_empty_or_ungrounded`。
- 失败请求的 `request_index` 为 16、17、18、19；其中 3 条为 `llm_planning_failed`，1 条为 `native_tool_use` 且 `product_catalog_lookup` 已完成。
- 失败 run 关联为 `92a53240-8e3b-4865-9bdc-0f008cbaa658`、`42d9e560-c10d-4e62-980b-75601a13b793`、`117da8f3-1ce9-4a36-afc2-ba7fb8e2b792`、`31b91fb1-9911-49dc-9eb5-719dfb23136c`；完整 conversation、audit、trace 关联保存在 `141-audit-counter-fix-full-c4-20260912-failure-analysis.json`。
- 20/20 均有唯一终态，SSE 无丢失、重复、序号异常或身份混合；20/20 audit 可查询，SSE/audit 类型、ID、序号一致，`event_count`、`emitted_event_count` 与事件列表一致。
- 20/20 清理成功；工具失败事件为 0；并发 8 未执行。

## 审计计数修复镜像并发总表（2026-09-12，历史波次）

| 并发 | 正式请求 | 完成 | 失败 | P50 | P95 | P99 | 最大耗时 | 结果 |
|---:|---:|---:|---:|---:|---:|---:|---:|---|
| 1 | 20 | 20 | 0 | 10728.36 ms | 22877.92 ms | 28236.94 ms | 29576.70 ms | Passed |
| 2 | 20 | 20 | 0 | 10974.39 ms | 24090.33 ms | 27604.93 ms | 28483.58 ms | Passed |
| 4 | 20 | 16 | 4 | 11437.15 ms | 22109.69 ms | 27177.25 ms | 28444.14 ms | Blocked |
| 8 | 未执行 | - | - | - | - | - | - | 因并发 4 失败停止 |

早期审计计数修复镜像正式请求合计 60 条，56 条完成、4 条失败；失败率 6.67%。最新 r2 优化镜像完整复测另有 60 条正式请求，52 条干净完成、1 条降级完成、7 条终态失败；Provider 请求级关联仍未取得，Token usage 不能按请求计算。

## 失败定位

- 大样本复测的 22 个终态失败请求均为 `LLM_ANSWER_UNAVAILABLE`，audit 的 `llm_status` 均为 `model_empty_or_ungrounded`，按观测结果归类为模型/Provider 结果不可用；具体 Provider 内部原因仍未确认。
- 17 个请求在工具调用前失败：`plan_source=llm_planning_failed`、`tool_count=0`。
- 5 个请求在 `product_catalog_lookup` 完成后失败：`plan_source=native_tool_use`、`tool_count=1`。
- 所有失败均能关联 run、conversation、audit、trace；SSE 与 audit 的事件类型、事件 ID、序号一致。
- API 容器日志时间窗没有出现完整的 `LLM_ANSWER_UNAVAILABLE`、`model_empty_or_ungrounded`、数据库/连接池、OOM 或 5xx 文本；命中未带 run/audit 标识的 `oneapi` 和 429 相关日志行。该信号不能与失败请求逐条关联，当前不能进一步认定 Provider 的具体内部原因。
- 修复后小规模复测没有终态 `LLM_ANSWER_UNAVAILABLE`、工具失败或采集器错误；SSE/audit 链路保持一致。
- 当前审计计数修复镜像新增的并发 1、2 完整诊断波次没有终态 `LLM_ANSWER_UNAVAILABLE`、工具失败或采集器错误；这说明该错误未在本轮 40 条正式请求中出现，但不能替代历史失败原因的请求级 Provider 关联。
- 修复后 API 日志窗口观察到一次 Provider continuation HTTP 503，但日志没有 run/audit/trace 字段，无法与具体请求关联。Provider 内部响应、数据库异常、连接池状态和 relay 内部状态仍未确认。
- 当前证据确认工具参数问题已在修复后 1/2/4 小波次消失；当前镜像并发 1、2 的 40 条正式请求也未出现工具参数异常，完整 1/2/4/8 稳定性仍因并发 4、8 未执行而未完成。
- 修复镜像完整复测中，并发 2 的失败请求在 `tool_completed` 后进入 `error -> run_failed`，错误码为 `LLM_ANSWER_UNAVAILABLE`。旧的 8220 API 时间窗摘要记录 5 条 Provider HTTP 429、1 条 continuation failure 和 1 条重试耗尽；124 New API SQLite 在精确窗口按状态字段取得 4 条 429，每条有 New API `request_id`，但没有 `upstream_request_id`、`Retry-After`、run/audit/trace 字段。两组记录都不能把某个 Provider 请求唯一归给失败 run。
- 8220 旧容器已被后续部署替换，历史 Docker 日志当前无法重新读取；日本 WorkBuddy 和 Nginx 对该窗口没有可关联记录。因此 Provider 限流、relay 重试策略和模型端限制仍未分开确认。
- 该失败请求的 audit 事件列表仍与 SSE 的 6 个事件完全对应，但 audit 顶层事件计数写为 4；源码原因已经定位为失败流先完成 audit，再发送 `error` 和 `run_failed`，使这两个异步审计写入未进入顶层计数。这个审计聚合计数问题与 Provider 失败分开记录。
- 旧修复镜像的并发 4/8 因并发 2 失败未执行；审计计数修复镜像并发 4 已出现 4 条 `LLM_ANSWER_UNAVAILABLE`，因此并发 8 未执行。最新 Provider 并发上限优化镜像并发 4 又出现 7 条 `LLM_ANSWER_UNAVAILABLE`，因此仍未执行并发 8；当前整体 1/2/4/8 稳定性未通过。

## 审计计数修复后的单条验证

- 当前修复镜像上的正式请求：1/1 `COMPLETED`，HTTP 200，清理 HTTP 200，无工具错误、5xx、429 或采集器错误。
- SSE 事件顺序为 `run_started -> plan_delta -> tool_started -> tool_completed -> answer_delta -> answer_delta -> answer_delta -> answer_delta -> answer_delta -> answer_completed -> run_completed`。
- SSE 事件数、audit 事件列表数、`event_count` 和 `emitted_event_count` 均为 11；事件类型、事件 ID、序号和唯一终态均通过。
- 本轮仅验证审计计数修复，不能替代并发 1/2 的 20 条正式样本复测。Provider 请求级关联能力仍不足，因此完整稳定复测仍需暂缓。

## 资源与数据库

- 大样本资源证据 `48-resource-samples-stability-rerun.jsonl` 共 20 条，五个阶段均有 4 条；修复后小规模资源证据 `76-post-fix-small-rerun-resource-samples.jsonl` 共 15 条，五个阶段均有 3 条；两组所有 `ssh_exit_code=0`。
- 正式请求期间采集到主机 CPU、内存、swap、load；Agent、PostgreSQL、Redis 容器 CPU、内存、PIDs；active runs；PostgreSQL active/idle 连接；JVM RSS/线程数和 Nginx TCP 连接数。
- 修复后正式期间主机 swap 使用量为 0；并发 4 时 Agent 容器约 477.5 MiB、43 PIDs，PostgreSQL 约 79.31 MiB、11 PIDs，JVM RSS 约 421948 kB、43 线程；清理后 active runs 为 0。JVM heap-used 和连接池等待为 `unavailable`。
- 数据库起点 `42-db-before-stability-rerun.json`：`21/69/0/372/3586/693/84/2661`，依次对应 conversations/messages/drafts/run_audits/audit_events/products/customers/finance_records。
- 数据库终点 `51-db-after-stability-rerun.json`：`21/69/0/460/4240/693/84/2661`。审计与事件增加属于本轮测试留痕；正式业务表保持不变。
- 修复后同轮数据库起点 `70-post-fix-small-rerun-db-before.json`：`21/69/0/481/4423/693/84/2661`；终点 `78-post-fix-small-rerun-db-after.json`：`21/69/0/502/4602/693/84/2661`。审计增加 21 条、审计事件增加 179 条；products、customers、finance_records 均为 0 变化。
- 修复后正式请求期间 active runs 采样为并发 1/2/4 分别为 1/2/4，清理后为 0；主机 swap 使用量始终为 0。
- 修复镜像完整复测资源证据 `87-postfix-full-c1-resource-samples.jsonl`、`95-postfix-full-c2-resource-samples.jsonl` 各覆盖五个阶段，SSH 10/10 返回 0；并发 1/2 正式期间 active runs 分别为 1/2，清理后均为 0。JVM heap-used 和连接池等待仍为 `unavailable`。
- 完整复测数据库起点和终点：并发 1 为 `502/4602 -> 524/4788`，并发 2 为 `524/4788 -> 546/4972`，依次为 run audits/audit events；`products`、`customers`、`finance_records` 均无变化。
- 单条 audit 验证数据库起点和终点：`agent_run_audits 562 -> 564`，`agent_run_audit_events 5115 -> 5138`；`products 693`、`customers 84`、`finance_records 2661` 保持不变。资源证据 `107-audit-counter-validation-resource-samples.jsonl` 五个阶段 SSH 均返回 0，正式期间 `active_runs=1`，清理后为 0。
- 当前镜像并发 1 资源证据 `125-audit-counter-fix-full-c1-20260912-resource-samples.jsonl` 五个阶段 SSH 均返回 0，包含 `during_formal`；并发 2 资源证据 `133-audit-counter-fix-full-c2-20260912-resource-samples.jsonl` 五个阶段 SSH 均返回 0，包含 `during_formal`。两级正式期间 `active_runs` 分别采到 1、2，清理后均为 0。
- 当前镜像资源样本显示主机 2 核、内存总量 1691447296 bytes、swap 使用量为 0；Agent 容器内存约 452.7–460.1 MiB、PIDs 40–45，PostgreSQL 约 62.71–69.08 MiB、11 PIDs，Redis 约 7.426–14.36 MiB、6 PIDs，Redis `connected_clients=1` 且 `blocked_clients=0`。JVM RSS 约 443512–450988 kB、线程 40–45；JVM heap-used 和连接池等待仍为 `unavailable`。
- 当前镜像并发 1 数据库起点 `121-audit-counter-fix-full-c1-20260912-db-before.json` 为 `21/69/0/564/5138/693/84/2661`，终点 `127-audit-counter-fix-full-c1-20260912-db-after.json` 为 `21/69/0/586/5335/693/84/2661`；并发 2 起点 `129-audit-counter-fix-full-c2-20260912-db-before.json` 为 `21/69/0/586/5335/693/84/2661`，终点 `135-audit-counter-fix-full-c2-20260912-db-after.json` 为 `21/69/0/608/5529/693/84/2661`。审计留痕增加，`products`、`customers`、`finance_records` 均未变化。
- 当前镜像并发 4 资源证据 `142-audit-counter-fix-full-c4-20260912-resource-samples.jsonl` 五个阶段 SSH 均返回 0，正式期间 `active_runs=4`，清理后为 0；主机 swap 使用量为 0，Agent 容器最高约 463.2 MiB/43 PIDs，PostgreSQL 约 68.02 MiB/11 PIDs，JVM RSS 最高约 460988 kB/45 线程，Redis `connected_clients=1`、`blocked_clients=0`。JVM heap-used 和连接池等待仍为 `unavailable`。
- 当前镜像并发 4 数据库起点 `138-audit-counter-fix-full-c4-20260912-db-before.json` 为 `21/69/0/608/5529/693/84/2661`，终点 `144-audit-counter-fix-full-c4-20260912-db-after.json` 为 `21/69/0/630/5709/693/84/2661`；只有审计留痕增加，`products`、`customers`、`finance_records` 未变化。

## 并发 4 时间窗日志关联补充

- 本轮并发 4 正式请求时间窗为 `2026-09-12T13:00:39.403491Z` 至 `2026-09-12T13:01:58.843004Z`，四个失败 run 的 run/conversation/audit/trace 关联见 `141-audit-counter-fix-full-c4-20260912-failure-analysis.json`。
- 8220 API 日志在同一时间窗观察到 Provider/relay HTTP 429、continuation HTTP 503、重试尝试和重试耗尽标记；没有取得 Provider request_id、upstream_request_id、Retry-After、请求级重试次数或 continuation failure 的具体原因。
- API 日志没有 run_id、audit_id 或 trace_id，四个失败 run 的请求级 Provider 关联数为 0。因此 429/503 只能记录为时间窗信号，不能认定为任一失败 run 的唯一根因。完整证据见 `146-concurrency-4-provider-log-correlation-20260912.json`。
- 该时间窗内 SSE 和 audit 仍逐条一致，审计顶层计数与事件列表一致；资源采样覆盖正式运行期间，数据库业务表未变化，清理全部成功。

## Token 与客户端

- Provider 和 Agent audit 未提供可按请求关联的 usage。修复后 API 日志出现 prompt/completion 数值，但没有 run/audit/trace 关联字段，因此本报告不计算 Token/s，也不把日志数值归入单条样本。
- 当前镜像并发 1、2 的正式请求同样未取得可按请求关联的 Token usage，不计算 Token/s。
- Android 没有在线 ADB 设备，未取得真实 Agent UI 性能样本。
- Web 未执行真实浏览器流程；iOS 缺少完整 Xcode 和设备条件，未取得真实样本。

## 修改、测试和部署

- 修改的业务源码：`Code/backend/src/main/java/com/zhihuiji/backend/application/service/v2/V2AgentAiService.java`、`Code/backend/src/main/java/com/zhihuiji/backend/application/service/v2/agent/component/SseStreamEmitter.java`，只传播工具参数错误码并区分通用工具失败码。
- 修改的测试文件：`Code/backend/src/test/java/com/zhihuiji/backend/application/service/v2/agent/component/SseStreamEmitterTest.java`，增加参数错误码保留测试。
- 修改的测试脚本：`testing/Agent/脚本/性能/20260909_layer1_concurrency_collector.py`，完善默认 SSH key 和安全失败分类。
- 未修改数据库迁移、部署配置、Android/Web/iOS 客户端代码；后端完整测试和 `bootJar` 均通过，采集器 `py_compile` 通过。
- 审计计数修复涉及 `Code/backend/src/main/java/com/zhihuiji/backend/application/service/v2/V2AgentAiService.java` 和 `Code/backend/src/test/java/com/zhihuiji/backend/application/service/v2/V2AgentAiServiceTest.java`；本轮没有新增源码或测试脚本改动。
- 当前线上镜像为 `sxyq27-zhj-api:20260913T-perf-provider-concurrency-2-r2`，容器内 `/app/app.jar` SHA-256 为 `89bc2b2d62123b49b708159fb6e4b7480d219835421a1519a70556760fddab53`；本轮未再次部署云端。
- 本轮定向测试 `./Code/backend/gradlew -p Code/backend test --tests com.zhihuiji.backend.application.service.v2.V2AgentAiServiceTest` 通过；采集器脚本未改动。
- 本轮只执行云端只读复测和报告同步，没有修改业务源码、测试文件、测试脚本、部署配置，也没有再次部署云端。

## Provider 并发优化后的完整复测

本轮使用 `sxyq27-zhj-api:20260913T-perf-provider-concurrency-2-r2`，Agent、Provider、模型、Wire API、账号和输入保持与前一轮一致。优化后当前运行态证据为 `177-perf-provider-concurrency-2-r2-full-20260913-current-runtime-deployment.json`。采集器以并发 1、2、4 顺序执行，每级 2 个预热请求和 20 个正式请求；每条正式记录即时 flush/fsync，查询 audit 后清理会话。

| 并发 | 正式请求 | 完成 | 失败 | P50 | P95 | P99 | 最大耗时 | 错误分类 | 清理 |
|---:|---:|---:|---:|---:|---:|---:|---:|---|---:|
| 1 | 20 | 20 | 0 | 7197.74 ms | 14217.86 ms | 14920.79 ms | 15096.52 ms | 无 | 20/20 |
| 2 | 20 | 20 | 0 | 9783.64 ms | 18631.33 ms | 22933.08 ms | 24008.52 ms | `TOOL_ARGUMENTS_INVALID` 1 条降级完成 | 20/20 |
| 4 | 20 | 13 | 7 | 12372.90 ms | 22835.19 ms | 23466.21 ms | 23623.97 ms | `LLM_ANSWER_UNAVAILABLE` 7 条 | 20/20 |
| 8 | 未执行 | - | - | - | - | - | - | 并发 4 失败后停止 | - |

- 并发 2 的 1 条请求事件为 `run_started -> plan_delta -> tool_started -> tool_failed -> answer_delta -> answer_delta -> answer_completed -> run_completed`；audit 与 SSE 一致，但不能将其视为无工具错误的正常完成。
- 并发 4 的 7 条失败均有 run、conversation、audit、trace 关联，SSE 事件分别为 3 或 6 项；失败流的 audit `event_count`、`emitted_event_count` 与事件列表一致。
- 60/60 正式请求 HTTP 200，5xx、Agent 429、采集器错误、SSE 丢失、SSE 重复、终态异常、身份混合和 audit 计数不一致均为 0。
- 当前并发 4 失败请求和完整时间信息见 `173-perf-provider-concurrency-2-r2-full-20260913-failure-analysis.json`、`172-perf-provider-concurrency-2-r2-full-20260913-concurrency-4.jsonl`、`180-perf-provider-concurrency-2-r2-full-20260913-stop-conclusion.md`。

## 优化前后对照

优化项 `L1-PERF-001` 在前一轮完成：`AgentLlmProperties` 增加 Provider 并发上限配置，`LongCatAnthropicClient` 对同步和流式 Provider 请求使用上限为 2 的信号量；定向测试和 `bootJar` 通过，部署到当前 r2 镜像。本轮没有新增源码、测试或部署改动。

| 并发 | 优化前完成/请求 | 优化后完成/请求 | P50 变化 | P95 变化 | 结果 |
|---:|---:|---:|---:|---:|---|
| 1 | 20/20 | 20/20 | -32.91% | -37.85% | Improved |
| 2 | 20/20 | 20/20，含 1 条工具参数异常降级完成 | -10.85% | -22.66% | Regressed |
| 4 | 16/20 | 13/20 | +8.18% | +3.28% | Regressed |

并发 4 的百分位基于不同的完成子集，不能单独据此宣称延迟改善。完整对照和资源变化见 `179-perf-provider-concurrency-2-r2-full-20260913-optimization-comparison.json`。

## 本轮 Provider 日志关联

`178-perf-provider-concurrency-2-r2-full-20260913-provider-log-correlation.json` 记录了当前完整波次时间窗。并发 4 窗口内 8220 API 有 36 条 Provider 429 相关日志和 3 条重试/续答相关日志，124 New API 有 25 条 429 状态日志；这些是日志行计数，不是已关联的失败请求数。Provider、8220 API 和 124 New API 日志均没有 run/audit/trace 字段，失败 run 与 Provider request ID 的请求级关联数为 0；`upstream_request_id`、`Retry-After`、单请求重试次数和 continuation failure 具体原因仍未取得。Japan WorkBuddy、TRAE 和 Nginx 时间窗内未出现 429/503 信号。

## 本轮资源与数据库

资源证据 `174-perf-provider-concurrency-2-r2-full-20260913-resource-samples.jsonl` 共 15 条，五个阶段均覆盖正式请求期间，`ssh_exit_code=0` 为 15/15。并发 4 正式期间最高约 Agent 463.9MiB/45 PIDs、PostgreSQL 71.4MiB/11 PIDs、JVM RSS 473184 kB/45 线程，active runs=4；swap=0，Redis `connected_clients=1`、`blocked_clients=0`；JVM heap-used 和连接池等待为 unavailable。

数据库起点 `168-perf-provider-concurrency-2-r2-full-20260913-db-before.json` 为 `21/69/0/672/6078/693/84/2661`，终点 `176-perf-provider-concurrency-2-r2-full-20260913-db-after.json` 为 `21/69/0/738/6648/693/84/2661`，顺序为 conversations/messages/drafts/run_audits/audit_events/products/customers/finance_records。审计增加为测试留痕；正式业务表没有变化。

## 最新结论

第一层测量完成，但稳定性未通过；第二层和 671 条 Agent 测试暂不开始。

## 工具失败终态处理（2026-09-13）

- 复核 `173-perf-provider-concurrency-2-r2-full-20260913-failure-analysis.json` 后确认：并发 2 的一条 `TOOL_ARGUMENTS_INVALID` 含 `tool_failed`，但此前仍被记录为 `COMPLETED`；这属于应用完成语义问题，与 Provider 时间窗信号分开。
- 已修改 `Code/backend/src/main/java/com/zhihuiji/backend/application/service/v2/V2AgentAiService.java` 的 `resolveRunOutcome`：没有任何成功工具结果且存在工具失败时，返回 `EXHAUSTED` 和 `AGENT_TOOL_EXECUTION_FAILED`；部分工具成功时的部分回答行为保持不变。
- 新增测试覆盖该语义；`V2AgentAiServiceTest`、`AnswerSynthesizerTest` 和 `bootJar` 均通过。
- 新部署镜像为 `sxyq27-zhj-api:20260913T0245-agent-tool-failure-terminal`，容器启动时间为 `2026-09-12T18:43:16.038810705Z`，JAR SHA-256 为 `0e5ade9578ba18e51639d1b8533bcc710cbc2ed2bd832022d55f32e36d01b65d`。部署记录见 `183-tool-failure-terminal-deployment-20260913.json`。
- 该次工具失败终态处理未取得新的真实 Agent 样本：当时本机没有 `AGENT_TEST_PHONE`，受保护存储项目仅提供登录口令，无法安全确定登录账号标识；未猜测账号、未发送模拟请求。后续本轮已通过 8220 只读查询取得尾号为 `2002` 的唯一账号标识并完成并发 4 复测。
- 原有 Provider 失败仍未按请求关联到 8220 或 Provider request ID；并发 4 的历史 7 条 `LLM_ANSWER_UNAVAILABLE` 仍然有效，尚未形成稳定复测依据。

本轮证据：`181-tool-failure-terminal-semantics-20260913.json`、`182-tool-failure-terminal-validation-20260913.md`、`183-tool-failure-terminal-deployment-20260913.json`。

该次工具失败终态处理修改了 `Code/backend/src/main/java/com/zhihuiji/backend/application/service/v2/V2AgentAiService.java` 和 `Code/backend/src/test/java/com/zhihuiji/backend/application/service/v2/V2AgentAiServiceTest.java`，并部署了 `183-tool-failure-terminal-deployment-20260913.json` 所记录的新镜像；本轮并发 4 复测未新增源码、测试或部署配置修改。Token usage、Token/s、JVM heap-used、连接池等待和 Android/Web/iOS 真实性能样本仍未取得。

## 判定与证据

- 第一层是否真正完成：未完成。最新并发 4 复测使用运行中的 `20260913T0245-agent-tool-failure-terminal`，8/20 完成、12/20 为 `LLM_ANSWER_UNAVAILABLE`；并发 8 未执行，整体稳定性未通过。并发 1、2 的 20 条结果来自用户指定的 `20260911T1900-agent-audit-counter-fix` 历史证据，版本差异不能忽略。
- 第二层是否允许开始：不允许。
- 671 条 Agent 测试是否允许开始：不允许。
- 证据目录：`testing/Agent/性能/artifacts/20260908-layer1-cloud-initial-measurement/`。
- 最新证据：`110-current-runtime-deployment-20260912.json`、`111-provider-request-correlation-20260912.json`、`112-audit-counter-root-cause-20260912.json`、`112-audit-fix-small-rerun-20260912-db-before.json`、`113-audit-counter-validation-conclusion-20260912.md`、`113-audit-fix-small-rerun-20260912-warmup.jsonl`、`114-audit-fix-small-rerun-20260912-concurrency-1.jsonl`、`115-audit-fix-small-rerun-20260912-concurrency-2.jsonl`、`116-audit-fix-small-rerun-20260912-failure-analysis.json`、`117-audit-fix-small-rerun-20260912-resource-samples.jsonl`、`118-audit-fix-small-rerun-20260912-summary.json`、`119-audit-fix-small-rerun-20260912-db-after.json`，以及本轮 `120`–`145`、`167`–`180` 的完整文件名。旧证据均保留。
- `111-provider-request-correlation-20260912.json` 只确认时间窗口内 124 New API 记录了 4 条上游 429，并取得 New API `request_id`；没有取得 `upstream_request_id`、`Retry-After`、run/audit/trace、请求级重试次数或 continuation failure 具体原因，不能把 429 写成某个失败 run 的唯一根因。
- 本轮最终结论：第一层测量完成，但稳定性未通过；第二层和 671 条 Agent 测试暂不开始。

## 最后并发级别复测（2026-09-13）

- 本轮没有重复并发 1、2。并发 4 执行 2 个预热请求和 20 个正式请求；每条正式请求经过真实云端 Agent、`product_catalog_lookup`、云端 PostgreSQL、SSE、audit，并即时写入 JSONL 后执行 flush/fsync。
- 用户指定的 `sxyq27-zhj-api:20260911T1900-agent-audit-counter-fix` 在 8220 上可见，但不在运行容器中。实际运行容器为 `sxyq27-zhj-api:20260913T0245-agent-tool-failure-terminal`，本轮未切换镜像、未修改源码、未修改测试或部署配置、未重新部署。

| 并发 | 正式请求 | 完成 | 失败 | P50 | P95 | P99 | 最大耗时 | 错误 | 版本与结果 |
|---:|---:|---:|---:|---:|---:|---:|---:|---|---|
| 1 | 20 | 20 | 0 | 10728.36 ms | 22877.92 ms | 28236.94 ms | 29576.70 ms | 无 | `20260911T1900`，既有证据，Passed |
| 2 | 20 | 20 | 0 | 10974.39 ms | 24090.33 ms | 27604.93 ms | 28483.58 ms | 无 | `20260911T1900`，既有证据，Passed |
| 4 | 20 | 8 | 12 | 14913.92 ms | 17928.10 ms | 18448.17 ms | 18578.19 ms | `LLM_ANSWER_UNAVAILABLE` x12 | `20260913T0245`，本轮，Blocked |
| 8 | 未执行 | - | - | - | - | - | - | 因并发 4 失败停止 | 未执行 |

并发 4 的百分位只基于 8 个完成请求，不能代表 20 个请求的完整分布。失败请求索引为 8–19；每条均有 run、conversation、audit、trace，失败分析和事件级信息保存在 `188-audit-fix-successor-c4-20260913-failure-analysis.json` 与 `187-audit-fix-successor-c4-20260913-concurrency-4.jsonl`。

## 最后并发级别链路和资源

- 20/20 HTTP 200；20/20 audit 查询成功；SSE 丢失、重复、序号不连续、终态不唯一、身份混合、audit 类型/ID/序号不一致和 audit 计数不一致均为 0。
- 20/20 清理返回 HTTP 200。正式期间 `active_runs=4`，清理后为 0；`products`、`customers`、`finance_records` 保持 `693`、`84`、`2661`。
- 起点 `185-audit-fix-successor-c4-20260913-db-before.json` 为 `21/69/0/738/6648/693/84/2661`；终点 `191-audit-fix-successor-c4-20260913-db-after.json` 为 `21/69/0/760/6792/693/84/2661`。新增只发生在测试审计留痕表。
- `189-audit-fix-successor-c4-20260913-resource-samples.jsonl` 的五个阶段均有 SSH 成功样本，5/5 返回 0；正式期间 Agent 约 433.9 MiB/44 PIDs，PostgreSQL 约 75 MiB/14 PIDs，JVM RSS 约 443916 kB/44 线程，Redis `connected_clients=1`、`blocked_clients=0`，主机 swap 为 0。
- JVM heap-used、连接池等待、按请求 Token usage 和 Token/s 仍 unavailable；Android、Web、iOS 仍没有本轮真实性能样本。

## 最后并发级别 Provider 观察

- 8220 API 日志时间窗为 `2026-09-13T03:01:35+08:00` 至 `2026-09-13T03:03:10+08:00`，出现 64 条 Provider 429/status=429 相关行、20 条重试耗尽相关行、4 条 continuation 相关行和 12 条空回答拒绝相关行。
- 日志没有 run、audit、trace、Provider request ID、upstream request ID 或 `Retry-After`，所以 Provider 429 仍是时间窗信号，不能指定为某个失败 run 的唯一根因。当前 124 New API 的本轮时间窗日志未取得，原因和已知缺口见 `192-audit-fix-successor-c4-20260913-runtime-provider-correlation.json`；历史 124 关联结论仍见 `111-provider-request-correlation-20260912.json`。

## 本轮结论

第一层测量完成，但稳定性未通过；第二层和 671 条 Agent 测试暂不开始。

- 第一层未完成：并发 4 的 20 条中有 12 条真实 Agent 终态失败，并发 8 按规则未执行。
- 第二层不允许开始；671 条 Agent 测试不允许开始；Android、Web、iOS 业务测试不开始。
- 本轮没有新增源码、测试文件、采集器脚本或部署配置修改，也没有重新部署云端；只新增本轮证据并更新本报告和性能台账。
- 本轮证据：`184-audit-fix-successor-c4-20260913-collector-format-validation.json`、`185-audit-fix-successor-c4-20260913-db-before.json`、`186-audit-fix-successor-c4-20260913-warmup.jsonl`、`187-audit-fix-successor-c4-20260913-concurrency-4.jsonl`、`188-audit-fix-successor-c4-20260913-failure-analysis.json`、`189-audit-fix-successor-c4-20260913-resource-samples.jsonl`、`190-audit-fix-successor-c4-20260913-summary.json`、`191-audit-fix-successor-c4-20260913-db-after.json`、`192-audit-fix-successor-c4-20260913-runtime-provider-correlation.json`、`193-audit-fix-successor-c4-stop-conclusion-20260913.md`。

## Provider 并发上限 1 优化闭环（2026-09-13）

### 优化动作

- 优化项：`L1-PERF-002`。
- 依据：并发 4 在 Provider 并发上限 2 时出现 7/20 和 12/20 的 `LLM_ANSWER_UNAVAILABLE`；8220 日志有 429、continuation 和空回答信号，而 API、PostgreSQL、Redis、SSE 与 audit 没有相应异常。
- 改动：`Code/backend/src/main/resources/application-prod.yml` 增加 `max-concurrent-requests: ${AGENT_LLM_MAX_CONCURRENT_REQUESTS:1}`，保留环境变量覆盖能力。权限、owner/store 隔离、工具、事务、审计、SSE 终态和草稿确认逻辑未改动。
- 定向测试：`LongCatAnthropicClientTest`、`V2AgentAiServiceTest` 和 `bootJar` 均通过。
- 部署：`sxyq27-zhj-api:20260913T0345-agent-provider-concurrency-1`，容器启动时间 `2026-09-12T19:55:21.949880168Z`，JAR SHA-256 为 `3471e322206f7f0fdd4ecb865c913cedfed156c7b10104f5a38dd96f4aec5716`；API 容器重建，PostgreSQL 和 Redis 未重建。

### 小规模复测

| 并发 | 预热 | 正式 | 完成 | 失败 | P50 | P95 | P99 | 最大耗时 | 结果 |
|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|
| 1 | 2 | 5 | 5 | 0 | 5968.69 ms | 9097.74 ms | 9478.29 ms | 9573.43 ms | Passed |
| 2 | 2 | 5 | 5 | 0 | 6864.72 ms | 7196.17 ms | 7196.32 ms | 7196.36 ms | Passed |
| 4 | 2 | 5 | 5 | 0 | 12581.87 ms | 16756.34 ms | 17128.77 ms | 17221.88 ms | Passed |

首次并发 2 小波次有 1 条预热 `IncompleteRead`，未取得会话 ID；根据后续数据库增量定位到会话 863，并以测试账号清理成功，随后数据库恢复到 `21/69` 会话/消息。该波次保留为采集器受影响证据；并发 2 重做波次无采集异常。小规模三组正式请求均无工具错误、5xx、429、SSE 丢失/重复、audit 计数差异或身份混合，清理全部成功。

### 新镜像完整复测

| 并发 | 预热 | 正式 | 完成 | 失败 | P50 | P95 | P99 | 最大耗时 | 错误 | 结果 |
|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|---|
| 1 | 2 | 20 | 20 | 0 | 6172.84 ms | 11107.89 ms | 11375.57 ms | 11442.49 ms | 无 | Passed |
| 2 | 2 | 20 | 19 | 1 | 10037.12 ms | 21020.25 ms | 22845.75 ms | 23302.13 ms | `LLM_ANSWER_UNAVAILABLE` x1 | Blocked |
| 4 | 未执行 | 未执行 | - | - | - | - | - | - | 因并发 2 失败停止 | 未执行 |
| 8 | 未执行 | 未执行 | - | - | - | - | - | - | 因并发 2 失败停止 | 未执行 |

失败请求为并发 2 index 19，run `dd68a4a7-e4b2-4280-a42b-509c52f9d75e`，audit 状态为 `model_empty_or_ungrounded`。请求经过真实工具并完成 `product_catalog_lookup`，随后在最终回答阶段失败；SSE 和 audit 均为 `run_started -> plan_delta -> tool_started -> tool_completed -> error -> run_failed`，6 个事件逐项一致，清理 HTTP 200。

### Provider、资源与数据库

- 失败请求时间窗的 8220 API 日志有 4 条 429/status=429、1 条 continuation、1 条空回答；没有 run/audit/trace、Provider request ID、upstream request ID、`Retry-After` 或单请求重试次数，不能把 429指定为该 run 的唯一原因。124 New API 当前请求级日志仍未取得，原因和历史缺口见 `244-perf-provider-concurrency-1-full-20260913-provider-correlation.json`。
- 并发 1/2 完整波次 40 条正式请求的 SSE 丢失、重复、序号异常、终态不唯一、身份混合和 audit 类型/ID/序号或计数不一致均为 0；40/40 清理成功。正式期间 active runs 分别采到 1、2，清理后均为 0。
- 并发 1 资源证据 `233-perf-provider-concurrency-1-full-20260913-resource-samples.jsonl` 在正式期间采到 Agent 429.5 MiB/43 PIDs、PostgreSQL 67.54 MiB/11 PIDs、JVM RSS 433468 kB/43 线程；并发 2 资源证据 `241-perf-provider-concurrency-1-full-20260913-resource-samples.jsonl` 采到 Agent 444.5 MiB/43 PIDs、PostgreSQL 70.49 MiB/11 PIDs、JVM RSS 446592 kB/43 线程；两组 swap=0、Redis connected_clients=1、blocked_clients=0，SSH 阶段样本全部返回 0。
- 并发 1 数据库起点 `229-perf-provider-concurrency-1-full-20260913-db-before.json` 为 `21/69/0/788/7036/693/84/2661`，终点 `235-perf-provider-concurrency-1-full-20260913-db-after.json` 为 `21/69/0/810/7227/693/84/2661`；并发 2 起点 `237-perf-provider-concurrency-1-full-20260913-db-before.json` 为 `21/69/0/810/7227/693/84/2661`，终点 `243-perf-provider-concurrency-1-full-20260913-db-after.json` 为 `21/69/0/832/7422/693/84/2661`。审计留痕增加，`products`、`customers`、`finance_records` 未变化。
- JVM heap-used、连接池等待、按请求 Token usage 和 Token/s 仍 unavailable；Android、Web、iOS 未执行。

### 优化前后判断

- 与前一轮 Provider 并发上限 2 的完整并发 1 相比，当前并发 1 的 P50/P95/P99/最大耗时分别下降约 14.24%/21.87%/23.76%/24.20%，结果为 `Improved`。
- 当前并发 2 虽然小规模通过，完整波次仍有 1 条 `LLM_ANSWER_UNAVAILABLE`；相对前一轮 20/20 的结果，稳定性为 `Regressed`，不能继续完整并发 4/8。
- 优化对 Provider 429 的影响仍未能完成请求级归属；现有证据只能确认时间窗信号，不能确认具体 Provider、relay 或模型内部原因。

### 本轮结论

第一层测量完成，但稳定性未通过；第二层和 671 条 Agent 测试暂不开始。

本轮已更新阶段报告和性能台账，新增部署证据 `194-provider-concurrency-1-deployment-20260913.json`、清理证据 `211-perf-provider-concurrency-1-small-cleanup-20260913.json`、Provider 关联证据 `244-perf-provider-concurrency-1-full-20260913-provider-correlation.json`、停止结论 `245-perf-provider-concurrency-1-full-20260913-stop-conclusion.md` 和优化对照 `246-perf-provider-concurrency-1-optimization-comparison-20260913.json`；完整原始 JSONL、资源、数据库和汇总文件均保留在当前证据目录。

## 最后并发级别复测（2026-09-13，当前运行镜像）

本轮没有重复并发 1、2。当前 8220 实际运行容器为 `sxyq27-zhj-api:20260913T0345-agent-provider-concurrency-1`，用户指定的 `sxyq27-zhj-api:20260911T1900-agent-audit-counter-fix` 虽在主机上存在，但未作为运行容器使用。Agent、Provider、模型、Wire API、账号标签和 SSE 路径保持为本轮既定值；本轮没有切换镜像、修改源码、修改测试或重新部署。

并发 4 执行 2 个预热请求和 20 个正式请求。每条正式请求经过真实云端 Agent、`product_catalog_lookup`、云端 PostgreSQL、SSE 和 audit，随后查询 audit 再清理会话；正式记录逐条写入 JSONL 并执行 flush/fsync。

| 并发 | 预热 | 正式 | 完成 | 失败 | P50 | P95 | P99 | 最大耗时 | 结果 |
|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|
| 1 | 2 | 20 | 20 | 0 | 6172.84 ms | 11107.89 ms | 11375.57 ms | 11442.49 ms | 当前镜像既有结果 |
| 2 | 2 | 20 | 19 | 1 | 10037.12 ms | 21020.25 ms | 22845.75 ms | 23302.13 ms | `LLM_ANSWER_UNAVAILABLE`，停止后续级别 |
| 4 | 2 | 20 | 19 | 1 | 16406.36 ms | 19372.86 ms | 20462.01 ms | 20734.30 ms | `LLM_ANSWER_UNAVAILABLE`，本轮停止 |
| 8 | - | - | - | - | - | - | - | - | 并发 4 失败后未执行 |

并发 4 失败请求为 `request_index=19`，run=`b3209f37-c9c0-491e-be41-8f40b7961c7c`，conversation=`949`，audit 和 trace 均已取得；audit `llm_status=model_empty_or_ungrounded`，SSE 类型为 `run_started -> plan_delta -> tool_started -> tool_completed -> error -> run_failed`。20/20 请求 HTTP 200，Agent 429=0，HTTP 5xx=0，20/20 只有一个合法终态，SSE 丢失=0，SSE 重复=0，身份混合=0，audit 类型/ID/seq/计数不一致=0，20/20 清理返回 HTTP 200。

### 数据库与资源

并发 4 起点 `248-perf-layer1-final-c4-current-20260913-db-before.json` 为 `21/69/0/832/7422/693/84/2661`，终点 `254-perf-layer1-final-c4-current-20260913-db-after.json` 为 `21/69/0/854/7602/693/84/2661`，顺序为 conversations/messages/drafts/run_audits/audit_events/products/customers/finance_records。审计留痕增加，products、customers、finance_records 未变化。

资源文件 `252-perf-layer1-final-c4-current-20260913-resource-samples.jsonl` 的五个阶段均有样本，SSH 成功为 5/5，并覆盖正式请求期间。正式期间 active_runs=4，清理后为 0；Agent 约 442 MiB、43 PIDs，PostgreSQL 约 65.43 MiB、11 PIDs，JVM RSS 约 451032 kB、43 线程，Redis connected_clients=1、blocked_clients=0，主机 swap=0。全阶段最高 Agent 约 444.2 MiB/45 PIDs、PostgreSQL 约 67.99 MiB/11 PIDs、JVM RSS 约 453336 kB/45 线程。JVM heap-used 和连接池等待不可取得。

### Provider 关联

`255-perf-layer1-final-c4-current-20260913-provider-correlation.json` 记录了本轮关联结果。8220 API 日志时间窗为 `2026-09-13T05:06:00+08:00` 至 `2026-09-13T05:07:50+08:00`，共 36 行，其中 16 行含 Provider 429 信号、1 行含 continuation 信号；没有 run_id、audit_id、trace_id、Provider request_id、upstream_request_id、Retry-After 或单请求重试次数。124 New API 日志因本机记录的 SSH 密钥文件不可用而未取得，也未使用替代密钥或跳板。Provider 429 仍只属于时间窗信号，不能指定为失败 run 的唯一原因。

### 本轮结论

本轮新增证据为 `247-perf-layer1-final-c4-current-20260913-collector-format-validation.json`、`248-perf-layer1-final-c4-current-20260913-db-before.json`、`249-perf-layer1-final-c4-current-20260913-warmup.jsonl`、`250-perf-layer1-final-c4-current-20260913-concurrency-4.jsonl`、`251-perf-layer1-final-c4-current-20260913-failure-analysis.json`、`252-perf-layer1-final-c4-current-20260913-resource-samples.jsonl`、`253-perf-layer1-final-c4-current-20260913-summary.json`、`254-perf-layer1-final-c4-current-20260913-db-after.json`、`255-perf-layer1-final-c4-current-20260913-provider-correlation.json` 和 `256-perf-layer1-final-c4-current-20260913-stop-conclusion.md`。

当前镜像并发 4 未达到 20/20，因而并发 8 不执行。第一层测量完成，但稳定性未通过；第二层和 671 条 Agent 测试暂不开始。按请求可取得的 Token usage、Token/s、JVM heap-used、连接池等待以及 Android、Web、iOS 真实性能样本仍不可取得。

## 最终并发 4/8 复测（2026-09-13，当前实际运行镜像）

以下记录为本报告最新状态。前文的镜像与结果属于历史波次，不能与本轮的 4/8 请求合并为同一版本样本。本轮没有重复并发 1、2，也没有启动第二层、671 条 Agent 测试或 Android/Web/iOS 业务测试。

### 当前线上条件

- Agent SSE：`https://zhj-api.sxyq27.online/v2/agent/chat/stream`。
- Provider：`https://oneapi.sxyq27.online/v1`。
- 模型：`deepseek-v4-flash-0731`；Wire API：`chat_completions`；账号标签：`suffix-2002`。
- 实际运行镜像：`sxyq27-zhj-api:20260913T0607-agent-continuation-retry`；容器启动时间：`2026-09-12T22:10:45.132973724Z`。
- Agent `/healthz` 和 Provider `/healthz` 均返回 HTTP 200。实际运行环境中的 Provider URL、模型和 Wire API 与测试条件一致。
- 本轮没有新增源码、测试脚本或部署配置修改，也没有再次部署云端。当前镜像内的续答重试和 Provider 请求并发限制来自前序波次。

### 并发结果总表

并发 1、2 使用已有审计计数修复镜像证据；并发 4、8 使用当前实际运行镜像。版本差异已单独列出，不能把四级结果写成同一镜像的一次波次。

| 并发 | 预热 | 正式 | 完成 | 失败 | P50 | P95 | P99 | 最大耗时 | 错误类型 | 结果 |
|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|---|
| 1 | 2 | 20 | 20/20 | 0 | 10728.36 ms | 22877.92 ms | 28236.94 ms | 29576.70 ms | 无 | 已有镜像，Passed |
| 2 | 2 | 20 | 20/20 | 0 | 10974.39 ms | 24090.33 ms | 27604.93 ms | 28483.58 ms | 无 | 已有镜像，Passed |
| 4 | 2 | 20 | 20/20 | 0 | 25078.85 ms | 31468.35 ms | 32877.96 ms | 33230.36 ms | 无 | 当前镜像，Passed |
| 8 | 2 | 20 | 14/20 | 6 | 26230.35 ms | 44691.84 ms | 44969.38 ms | 45038.77 ms | `LLM_ANSWER_UNAVAILABLE` x6 | 当前镜像，Blocked |

并发 4 的百分位基于 20 条完成请求。并发 8 的百分位基于 14 条完成请求，不能代表 20 条请求的完整延迟分布。并发 8 的失败 `request_index` 为 8、9、10、11、12、15；6 条均有 run、conversation、audit、trace 关联，audit `llm_status=model_empty_or_ungrounded`，错误分类为 `provider_error`。对应 run 为：8=`1544fe3e-51ef-4d32-8241-01a2e7032a7c`、9=`acfd8324-59f8-44b1-942d-4ed79ce2fe7b`、10=`84137673-d2d8-4063-93a1-15006bb6c28e`、11=`73738e1a-3f7f-4879-8a6e-7662388cd2ce`、12=`fc85f7ad-f7c6-4e0f-8549-79577b1d8e56`、15=`4549343a-15ed-45d4-8fb7-2b64f52c6914`；正式请求时间为 `2026-09-12T22:37:15.479148+00:00` 至 `2026-09-12T22:37:59.801054+00:00`。其中 5 条在 `product_catalog_lookup` 完成后失败，1 条在规划阶段失败。

### SSE、audit、工具与清理

- 当前镜像并发 4/8 的 40 条正式请求均为 HTTP 200；Agent 侧 429、HTTP 5xx、采集器错误和工具失败事件均为 0。
- 40 条请求均只有一个合法终态；事件 ID 不重复，seq 连续，未发现 SSE 丢失、跨请求身份混合或终态重复。
- 40/40 audit 查询成功。SSE 与 audit 的事件类型、事件 ID、seq、事件列表数量、`event_count` 和 `emitted_event_count` 均一致，audit 计数不一致为 0。
- 40/40 会话清理返回 HTTP 200；并发 4 和 8 的资源样本在清理后均显示 `active_runs=0`。

### 数据库与资源

- 并发 4 起点 `258-perf-layer1-final-c4-retry-20260913-db-before.json`：`21/69/0/854/7602/693/84/2661`；终点 `264-perf-layer1-final-c4-retry-20260913-db-after.json`：`21/69/0/876/7809/693/84/2661`。
- 并发 8 起点 `266-perf-layer1-final-c8-retry-20260913-db-before.json`：`21/69/0/876/7809/693/84/2661`；终点 `272-perf-layer1-final-c8-retry-20260913-db-after.json`：`21/69/0/898/7984/693/84/2661`。
- 上述顺序为 `agent_conversations/agent_messages/agent_drafts/agent_run_audits/agent_run_audit_events/products/customers/finance_records`。新增只出现在 Agent 测试审计留痕；`products`、`customers`、`finance_records` 未发生变化。
- `262-perf-layer1-final-c4-retry-20260913-resource-samples.jsonl` 和 `270-perf-layer1-final-c8-retry-20260913-resource-samples.jsonl` 的五个阶段均为 `ssh_exit_code=0`，并覆盖正式请求期间。并发 4 正式期间 `active_runs=4`，Agent 约 394.4 MiB/43 PIDs，PostgreSQL 约 84.3 MiB/11 PIDs，JVM RSS 417052 kB/43 线程；并发 8 正式期间 `active_runs=8`，Agent 约 431.7 MiB/44 PIDs，PostgreSQL 约 97.04 MiB/17 PIDs，JVM RSS 451736 kB/44 线程。
- 资源采样的全阶段最高值约为：并发 4 Agent 429.3 MiB/45 PIDs、PostgreSQL 87.61 MiB/12 PIDs、JVM RSS 449220 kB/45 线程；并发 8 Agent 435.8 MiB/45 PIDs、PostgreSQL 99.44 MiB/17 PIDs、JVM RSS 455832 kB/45 线程。主机 swap 为 0；JVM heap-used 和连接池等待仍 unavailable。

### Provider 关联

当前波次关联证据为 `274-perf-layer1-final-provider-correlation-20260913.json`。8220 API 日志窗口为 `2026-09-12T22:33:19Z` 至 `2026-09-12T22:38:35Z`，共 93 行，其中 42 行含 429 信号、15 行含 continuation 信号、8 行含 retry 信号；没有 503 信号。日志没有 run、audit、trace、Provider request ID、upstream request ID 或 `Retry-After` 字段。124 New API 本轮未取得日志，因本机当前记录的 124 SSH 密钥路径不可用，未使用替代密钥或跳板。

因此，Provider 请求级关联仍无法取得；429、continuation 和 retry 只能作为时间窗口信号，不能指定为某一条失败 run 的唯一原因。`LLM_ANSWER_UNAVAILABLE` 的观测分类仍是 Provider/模型结果不可用，具体外部根因未确认。

### 第一层判定

- 第一层是否完成：未完成。并发 8 的 20 条正式请求中有 6 条 `LLM_ANSWER_UNAVAILABLE`。
- 第二层性能专项是否允许开始：不允许。
- 671 条 Agent 测试是否允许开始：不允许。
- Android、Web、iOS 业务测试：本轮不开始。
- Token usage、Token/s：无法按请求取得，不能计算 Token/s。

结论保持为：第一层测量完成，但稳定性未通过；第二层和 671 条 Agent 测试暂不开始。

## 第二阶段 AG-P-002 单只读工具首轮小测（2026-09-13）

- 当前 Goal：第二阶段真实云端 Agent 性能专项测试与优化。
- 线上实际运行镜像为 `sxyq27-zhj-api:20260913T-schema-qwen-status`，模型为 `qwen3.8-flash`，Provider 为 `https://oneapi.sxyq27.online/v1`，SSE 为 `https://zhj-api.sxyq27.online/v2/agent/chat/stream`，账号标签为 `suffix-2002`。
- AG-P-002 使用 1 个预热和 5 个正式请求，并发 1。5/5 完成，HTTP 200 为 5/5，429、5xx、超时、工具错误均为 0；首事件/首工具/首回答 P50 为 9341.31/9341.36/9341.40 ms，完整耗时 P50/P95/P99/最大为 9356.47/15867.35/16668.38/16868.64 ms。
- 5 条请求均调用真实 `product_catalog_lookup`。SSE 丢失、重复、多终态、身份混合和 audit 事件对应不一致均为 0；清理 5/5 成功。证据为 `400-phase2-ag-p002-single-tool-baseline-20260913-collector-format-validation.json` 至 `407-phase2-ag-p002-single-tool-baseline-20260913-db-after.json`，结论见 `408-phase2-ag-p002-single-tool-conclusion-20260913.md`。
- 数据库起点 `401-phase2-ag-p002-single-tool-baseline-20260913-db-before.json`，终点 `407-phase2-ag-p002-single-tool-baseline-20260913-db-after.json`。审计留痕增加；`products`、`customers`、`finance_records` 保持不变。资源证据 `405-phase2-ag-p002-single-tool-baseline-20260913-resource-samples.jsonl` 五阶段 SSH 均为 0，正式期间 `active_runs=1`；Agent 约 458.6 MiB，PostgreSQL 约 59.32 MiB，JVM RSS 462468 kB/43 线程，Redis connected=1/blocked=0，主机 swap=0。
- 首事件、首工具和首回答几乎同时出现，说明主要待测区间位于模型/Provider 首事件之前；当前没有证据证明项目源码、数据库查询或 SSE flush 是主要瓶颈。Provider request ID、upstream request ID、Retry-After、按请求 Token usage、JVM heap-used 和连接池等待仍未取得。
- 本轮未修改业务源码、测试脚本或部署配置，也未部署云端。下一项执行多工具连续调用小测；本轮 5 条样本只用于专项定位，不替代完整样本。

## 第二阶段 AG-P-008 多工具连续调用首轮小测（2026-09-13）

- 复用同一线上镜像、模型、Provider、账号和真实 SSE 链路；输入要求同时查询商品和客户。1 个预热、5 个正式请求，并发 1。
- 5/5 HTTP 200 且有唯一 `run_completed`；其中 1/5 先产生 `product_catalog_lookup` 的 `TOOL_ARGUMENTS_INVALID`，随后继续调用客户查询工具并完成回答，属于工具失败后的降级完成。4/5 无工具错误，不能把 5/5 记为无错误多工具成功。
- P50/P95/P99/最大耗时为 15676.02/23747.63/25276.86/25659.17 ms；首事件/首工具/首回答 P50 为 9239.58/9239.87/15665.95 ms。SSE 丢失、重复、多终态、身份混合和 audit 对应不一致均为 0，清理 5/5 成功。
- 起点 `411-phase2-ag-p008-multitool-baseline-20260913-db-before.json`，终点 `417-phase2-ag-p008-multitool-baseline-20260913-db-after.json`；仅审计留痕增加，`products`、`customers`、`finance_records` 未变化。资源证据为 `415-phase2-ag-p008-multitool-baseline-20260913-resource-samples.jsonl`，五阶段 SSH 均为 0，正式期间采样正常，主机 swap 为 0。
- 该异常与 AG-P-002 单工具小测的模型/工具参数边界一致，但当前仍未取得 Provider 请求级参数原文或服务端细粒度参数链路，不能确认项目源码根因。已记录于 `418-phase2-ag-p008-multitool-conclusion-20260913.md` 和 `419-phase2-ag-p008-tool-error-analysis-20260913.json`。
- 本轮仅为采集器增加 `AGENT_TEST_PROMPT` 输入参数并通过 `py_compile`；未修改业务源码、部署配置或线上镜像。多工具专项暂缓扩大样本，下一步先做参数生成证据定位，再决定是否有最小源码动作；第二阶段未完成，671 条测试不启动。

## 最终统一复测（2026-09-13）

### 唯一正式组合

- 本轮唯一组合为：镜像 `sxyq27-zhj-api:20260913T-schema-qwen-status`；模型 `qwen3.8-flash`；Provider `https://oneapi.sxyq27.online/v1`；Wire API `chat_completions`；Agent SSE `https://zhj-api.sxyq27.online/v2/agent/chat/stream`；账号标签 `suffix-2002`。
- 固定输入为“请查询一个商品的基本信息并简要回答。”，工具由真实 Agent 自动选择，观测为 `product_catalog_lookup`。规则为每级 2 个预热和 20 个正式请求，正式请求即时 JSONL flush/fsync，查询 audit 后清理，流读取上限 300 秒；正式波次开始后未切换环境。
- 历史 deepseek、其他 qwen 镜像和本轮结果分开保留，未合并统计。

### 本轮结果

- 并发 1：20 个正式请求中 19 个完成、1 个 `EXHAUSTED`；错误码 `AGENT_TOOL_EXECUTION_FAILED`，失败前出现 `TOOL_ARGUMENTS_INVALID`。19 个完成请求 P50/P95/P99/最大为 18457.24/33202.68/37057.50/38021.21 ms；首事件/首工具/首回答 P50 为 18449.19/18449.31/18449.36 ms。
- HTTP 200 为 20/20，429、5xx、采集超时为 0。SSE 丢失、重复、终态异常、身份混合和 audit 计数不一致均为 0；清理 20/20 成功。并发 2、4、8 按停止规则未执行。
- 失败请求 `run_id=46a3268f-2f4e-4482-a6af-71a4c2ee6496`，`conversation_id=1193`，audit 与 trace 关联正常；SSE 事件为 `run_started -> plan_delta -> tool_started -> tool_failed -> answer_delta -> answer_completed -> run_exhausted`，SSE 与 audit 类型、ID、序号和计数一致。完整失败分析见 `424-final-unified-qwen38-20260913-failure-analysis.json`，统一结论见 `428-final-unified-qwen38-conclusion-20260913.md`。
- 数据库起点 `421-final-unified-qwen38-20260913-db-before.json`，终点 `427-final-unified-qwen38-20260913-db-after.json`；仅 Agent 审计留痕增加，`products`、`customers`、`finance_records` 未变化。资源证据 `425-final-unified-qwen38-20260913-resource-samples.jsonl` 五阶段 SSH 均为 0；正式期间主机 swap=0，Agent 约 460.3 MiB，PostgreSQL 约 62.03 MiB，JVM RSS 464224 kB/43 线程；heap-used、连接池等待 unavailable。
- Provider request ID、upstream request ID、Retry-After 和按请求 Token usage 仍 unavailable，不能计算 Token/s。本轮没有新增业务源码或线上部署，采集器仅使用已加入的 `AGENT_TEST_PROMPT` 输入参数。

### 第一层判定

- 第一层优化和复测尚未稳定完成；同一正式组合只完成到并发 1，且有 1 条工具参数错误导致的 `EXHAUSTED`。优化前后完整对照数据暂不成立。
- 第二层性能专项：暂不开始。671 条 Agent 测试：暂不开始。Android、Web、iOS：暂不开始。

## 波次记录：2026-09-14统一复测（已在文末重新确认）

### 当前 Goal 与正式组合

当前 Goal 为第一层真实云端 Agent 性能优化闭环。本轮唯一正式组合为：镜像 `sxyq27-zhj-api:20260913T-schema-qwen-status-enum`，模型 `qwen3.8-flash`，Provider `https://oneapi.sxyq27.online/v1`，Wire API `chat_completions`，Agent SSE `https://zhj-api.sxyq27.online/v2/agent/chat/stream`，账号标签 `suffix-2002`。固定提示词为“请查询一个商品的基本信息并简要回答。”，工具由真实 Agent 自动选择 `product_catalog_lookup`，查询使用云端 PostgreSQL。

线上容器在本轮开始前后均为 running，启动时间 `2026-09-13T13:40:55.993627487Z`，重启次数为 0；实际运行参数见 `474-final-unified-qwen38-enum-reset-20260914-runtime-after-c4.json`。为恢复现有应用测试账号登录，按用户授权将 `suffix-2002` 对应的现有账号密码更新为本机受保护凭据；未创建账号，凭据未进入报告、台账、日志或证据。

此前 qwen/deepseek 的其他镜像和历史波次继续保留，本轮没有拼接或合并不同环境结果。正式请求期间没有切换镜像、模型、Provider、账号、提示词、工具 schema 或统计规则。此前短暂的第二阶段单工具、多工具小测仍只作为历史证据，本轮没有扩大。

### 正式结果

每级使用 2 个预热和 20 个正式请求；正式请求逐条写入 JSONL 并 flush/fsync，查询 audit 后清理会话。

| 并发 | 正式请求 | 完成 | 失败 | P50(ms) | P95(ms) | P99(ms) | 最大(ms) | 结果 |
|---:|---:|---:|---:|---:|---:|---:|---:|---|
| 1 | 20 | 20 | 0 | 38969.88 | 71771.02 | 75105.61 | 75939.26 | 通过 |
| 2 | 20 | 20 | 0 | 45651.38 | 72995.70 | 76002.56 | 76754.27 | 通过 |
| 4 | 20 | 19 | 1 | 100605.21 | 143128.32 | 149800.82 | 151468.95 | 未通过 |
| 8 | 未执行 | - | - | - | - | - | 并发 4 失败后停止 |

- 并发 1 首事件、首工具、首回答 P50 为 `38953.06/38953.19/38953.27 ms`；并发 2 为 `45639.32/45639.45/45639.53 ms`；并发 4 为 `100596.53/100596.72/100596.84 ms`。
- 并发 1、2 均 HTTP 200 20/20，无 Agent 429、HTTP 5xx、超时、工具错误、SSE 丢失、重复、乱序、身份混合或 audit 不一致；cleanup 均为 20/20。
- 并发 4 的第 7 条请求 HTTP 200 后出现客户端 `IncompleteRead`，未收到 SSE 元数据。服务端 audit 后续恢复为 `run_id=9ab7d65b-075e-417e-bea1-5de7a7d6d01c`、`conversation_id=1277`、`status=failed`、`error_code=STREAM_ERROR`；已补充 cleanup，HTTP 200。该请求使并发 4 失败 1 条，且服务端 `event_count=6`、`emitted_event_count=7`，存在审计计数差异。
- 并发 4 的 19 条完成请求真实调用 `product_catalog_lookup`；失败请求也完成了工具调用，但流在回答阶段中断。Provider 402/429/503 在本波次采集结果中均为 0；底层 Provider 请求级 ID、Retry-After 和具体外部原因仍未取得。

### 数据库、SSE 与资源

- 数据库起点为 `450-final-unified-qwen38-enum-reset-20260913-c1-db-before.json`：`agent_conversations/agent_messages/agent_drafts/agent_run_audits/agent_run_audit_events/products/customers/finance_records = 22/71/0/1128/9855/693/84/2661`。
- 并发 1 终点为 `456-final-unified-qwen38-enum-reset-20260913-c1-db-after.json`：审计计数 `1150/10050`；并发 2 终点为 `464-final-unified-qwen38-enum-reset-20260913-c2-db-after.json`：`1172/10237`；并发 4 终点为 `472-final-unified-qwen38-enum-reset-20260913-c4-db-after.json`：`1194/10416`。补充清理后的最终计数见 `476-final-unified-qwen38-enum-reset-20260914-db-after-orphan-cleanup.json`：`22/71/0/1194/10416/693/84/2661`，`active_runs=0`。
- `products`、`customers`、`finance_records` 在起止计数中均保持不变。并发 1、2 的每个请求都有唯一终态，SSE 与 audit 的事件类型、ID、序号和计数一致。并发 4 的异常请求服务端只持久化 6 个事件但发出 7 个事件，见 `473-final-unified-qwen38-enum-reset-20260914-c4-orphan-audit-recovery.json`。
- 资源采样分别为 `454-final-unified-qwen38-enum-reset-20260913-c1-resource-samples.jsonl`、`462-final-unified-qwen38-enum-reset-20260913-c2-resource-samples.jsonl`、`470-final-unified-qwen38-enum-reset-20260913-c4-resource-samples.jsonl`。三组 SSH 阶段均为 0，并覆盖正式期间；并发 4 正式期间 Agent 约 `472.2 MiB`、PostgreSQL 约 `78.67 MiB`、Redis 约 `9.688 MiB`、JVM RSS `455248 kB`/44 线程、主机 swap 为 0；清理后 active runs 为 0。JVM heap-used、连接池等待和按请求 Token usage unavailable，Token/s 不计算。

### 优化前后与验收

本轮没有新增业务源码、测试脚本或部署配置，也没有重新部署云端；之前的 `product_catalog_lookup.status` 整数枚举 `0/1` schema 优化属于前置波次，定向测试和 `bootJar` 已通过。当前波次相对前置小规模验证的结果显示并发 1、2 可完成，但并发 4 出现服务端流失败，不能形成完整稳定的优化前后对照。

本轮新增证据：`449-final-unified-qwen38-enum-reset-20260913-c1-collector-format-validation.json` 至 `456-final-unified-qwen38-enum-reset-20260913-c1-db-after.json`、`457-final-unified-qwen38-enum-reset-20260913-c2-collector-format-validation.json` 至 `464-final-unified-qwen38-enum-reset-20260913-c2-db-after.json`、`465-final-unified-qwen38-enum-reset-20260913-c4-collector-format-validation.json` 至 `472-final-unified-qwen38-enum-reset-20260913-c4-db-after.json`，以及 `473-final-unified-qwen38-enum-reset-20260914-c4-orphan-audit-recovery.json`、`474-final-unified-qwen38-enum-reset-20260914-runtime-after-c4.json`、`475-final-unified-qwen38-enum-reset-20260914-c4-orphan-cleanup.json`、`476-final-unified-qwen38-enum-reset-20260914-db-after-orphan-cleanup.json`、`477-final-unified-qwen38-enum-reset-20260914-wave-summary.json`、`478-final-unified-qwen38-enum-reset-20260914-conclusion.md`。

第一层优化和统一复测尚未完成；第二层及 671 条 Agent 测试暂不开始。Android、Web、iOS 测试暂不开始。下一项工作是定位并修复 `STREAM_ERROR`/`IncompleteRead` 及审计事件计数差异，然后在不切换正式组合的前提下重新复测并发 4，之后才能重新评估并发 8。

## 历史波次记录：2026-09-13统一复测（已被 2026-09-14 波次取代）

### 当前 Goal 与唯一正式组合

当前 Goal 为第一层真实云端 Agent 性能优化闭环。当前唯一正式组合为：镜像 `sxyq27-zhj-api:20260913T-schema-qwen-status-enum`，模型 `qwen3.8-flash`，Provider `https://oneapi.sxyq27.online/v1`，Wire API `chat_completions`，Agent SSE `https://zhj-api.sxyq27.online/v2/agent/chat/stream`，账号标签 `suffix-2002`。固定提示词为“请查询一个商品的基本信息并简要回答。”，真实 Agent 自动选择 `product_catalog_lookup` 并访问云端 PostgreSQL。容器启动时间为 `2026-09-13T13:40:55.993627487Z`，JAR SHA-256 见 `438-status-enum-fix-runtime-deployment-20260913.json`。

正式波次开始后没有切换镜像、模型、Provider、账号、提示词、工具 schema 或统计规则。历史 deepseek、其他 qwen 镜像和其他波次继续保留，未与本轮合并。

### 本轮执行与结果

本轮执行 2 个预热和并发 1 的 20 个正式请求。第 1 级出现 1 条真实云端 Agent 的 `LLM_ANSWER_UNAVAILABLE` 后，依停止规则未执行并发 2、4、8。

| 并发 | 预热 | 正式 | 完成 | 失败 | P50(ms) | P95(ms) | P99(ms) | 最大(ms) | 结果 |
|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|
| 1 | 2 | 20 | 19 | 1 | 21043.72 | 33593.94 | 57164.14 | 63056.69 | 未通过 |
| 2 | - | - | - | - | - | - | - | - | 未执行 |
| 4 | - | - | - | - | - | - | - | - | 未执行 |
| 8 | - | - | - | - | - | - | - | - | 未执行 |

并发 1 的首事件、首工具、首回答 P50 分别为 `20602.42 ms`、`20602.53 ms`、`20602.60 ms`。HTTP 200 为 20/20，Agent 429、HTTP 5xx、采集超时均为 0。SSE 丢失、重复、终态异常、身份混合和 audit 计数不一致均为 0；cleanup 为 20/20，active runs 清理后回到 0。Token usage 无法按请求取得，因此不计算 Token/s。

失败请求的 run、conversation、audit、trace 均已关联。SSE 与 audit 均为 6 个事件：`run_started -> plan_delta -> tool_started -> tool_completed -> error -> run_failed`；事件类型、ID、序号和计数一致。该请求没有 `TOOL_ARGUMENTS_INVALID`，失败类别为 `provider_error`，错误码为 `LLM_ANSWER_UNAVAILABLE`。

### 数据库和资源

数据库起点为 `441-latest-unified-qwen38-enum-20260913-db-before.json`，终点为 `447-latest-unified-qwen38-enum-20260913-db-after.json`。审计留痕按预期增加；`products=693`、`customers=84`、`finance_records=2661` 未变化。

`445-latest-unified-qwen38-enum-20260913-resource-samples.jsonl` 的五个阶段 SSH 均为 0，并覆盖正式请求运行期间。正式期间 Agent 约 `460.3 MiB`，PostgreSQL 约 `62.03 MiB`，JVM RSS `464224 kB`、43 线程，主机 swap 为 0。JVM heap-used、连接池等待仍 unavailable；Android、Web、iOS 尚无真实性能样本。

### 优化动作、错误归类与限制

此前已完成 `product_catalog_lookup.status` 的整数枚举 `0/1` schema 优化，并在 `ProductCatalogLookupToolTest`、`ToolExecutorTest` 和 `bootJar` 中通过验证；该动作及部署属于前置波次。本轮唯一正式波次未新增源码、测试、采集器或部署配置改动，也没有重新部署。前置小规模 5/5 通过，当前完整波次仍有 1 条模型/Provider 结果不可用，因此优化前后完整数据尚不能形成同条件对照。

当前仍未取得 Provider request ID、upstream request ID、Retry-After、按请求 Token usage、JVM heap-used 和连接池等待；不能把时间窗口信号进一步归因到 Provider 的具体外部组件。

本轮证据为：`440-latest-unified-qwen38-enum-20260913-collector-format-validation.json`、`441-latest-unified-qwen38-enum-20260913-db-before.json`、`442-latest-unified-qwen38-enum-20260913-warmup.jsonl`、`443-latest-unified-qwen38-enum-20260913-concurrency-1.jsonl`、`444-latest-unified-qwen38-enum-20260913-failure-analysis.json`、`445-latest-unified-qwen38-enum-20260913-resource-samples.jsonl`、`446-latest-unified-qwen38-enum-20260913-summary.json`、`447-latest-unified-qwen38-enum-20260913-db-after.json`、`448-latest-unified-qwen38-enum-20260913-conclusion.md`。

### 第一层验收结论

第一层优化和统一复测尚未完成；第二层及 671 条 Agent 测试暂不开始。Android、Web、iOS 测试也暂不开始。下一项工作是处理并重新验证当前 `LLM_ANSWER_UNAVAILABLE` 的模型/Provider 结果不可用问题，之后才能重新评估并发 2、4、8。

## status 枚举约束修复与真实云端验证（2026-09-13）

- 针对最终统一复测中 `product_catalog_lookup` 的 `TOOL_ARGUMENTS_INVALID`，在现有工具 schema 上增加整数枚举 `0/1`；布尔值仍被拒绝，不进入 Repository。定向 `ProductCatalogLookupToolTest`、`ToolExecutorTest` 和 `bootJar` 通过。
- 已部署镜像 `sxyq27-zhj-api:20260913T-schema-qwen-status-enum`，容器启动时间 `2026-09-13T13:40:55Z`，JAR SHA-256 为 `9ea58919d66845c901db8265eaff872c799d0487d9e1132822f501046aa3a1b9`。部署证据为 `438-status-enum-fix-runtime-deployment-20260913.json`。
- 在同一账号、qwen3.8-flash、Provider、SSE、固定提示词和真实工具链路上执行 1 个预热和 5 个正式请求：5/5 HTTP 200，5/5 完成，`TOOL_ARGUMENTS_INVALID=0`，工具错误、429、5xx、超时均为 0；P50/P95/P99/最大耗时 20449.15/32162.57/33355.58/33653.83 ms；首事件/首工具/首回答 P50 20413.66/20413.85/20413.91 ms。
- SSE 丢失、重复、终态异常、身份混合和 audit 对应不一致均为 0，清理 5/5 成功。数据库起点为 `431-after-status-enum-fix-qwen38-20260913-db-before.json`，终点为 `437-after-status-enum-fix-qwen38-20260913-db-after.json`；`products`、`customers`、`finance_records` 未变化。完整结论见 `439-status-enum-fix-validation-conclusion-20260913.md`。
- 该结果只证明小规模参数异常未复现，不能代替统一并发 1/2/4/8 完整复测。当前第一层仍未完成，第二层、671 条 Agent 测试和 Android/Web/iOS 测试继续暂缓。

## qwen3.8-flash schema 优化闭环与完整复测（2026-09-13）

### 当前需求与状态

- 当前 Goal：第一层真实云端 Agent 性能优化闭环。
- 当前线上镜像：sxyq27-zhj-api:20260913T-schema-qwen-status；容器启动时间：2026-09-13T06:36:53.224039389Z；状态 running，重启次数 0。
- Agent SSE：https://zhj-api.sxyq27.online/v2/agent/chat/stream；Provider：https://oneapi.sxyq27.online/v1；模型：qwen3.8-flash；Wire API：chat_completions；账号标签：suffix-2002。
- 本轮没有启动第二层、671 条 Agent 测试或 Android/Web/iOS 业务测试。

### 优化动作与验证

小规模 qwen 波次发现一次真实 product_catalog_lookup 工具调用将 status 生成为 boolean，服务端按 integer schema 正确拒绝，未进入 PostgreSQL。最小改动仅增强 ProductCatalogLookupTool 的 schema 描述，明确 status 只能使用整数 0/1，禁止 true/false；没有放宽校验，也没有改变工具执行、权限、owner/store 隔离、审计、事务或 SSE 终态。

定向测试 ProductCatalogLookupToolTest 与 bootJar 通过。部署 JAR SHA-256 为 d7fc9e0f6934c0874ba8bba9b2e7669b4efea4f8b204617b8ed4a5f64ef02878。数据库、Redis 未重建；部署过程中 Compose 曾重新创建 PostgreSQL 容器，但持久卷未改变、数据未删除，随后只使用 --no-deps 重建 API 容器并核实服务正常。

### qwen 完整并发结果

| 并发 | 预热 | 正式 | 完成 | 失败/采集异常 | P50 | P95 | P99 | 最大耗时 | 清理 | 结论 |
|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|
| 1 | 2 | 20 | 20/20 | 0 | 11197.24 ms | 31427.90 ms | 75339.16 ms | 86316.98 ms | 20/20 | Passed |
| 2 | 2 | 20 | 20/20 | 0 | 23535.81 ms | 67665.77 ms | 79931.99 ms | 82998.54 ms | 20/20 | Passed |
| 4 | 2 | 20 | 20/20 | 0 | 49236.69 ms | 57652.49 ms | 59684.75 ms | 60192.82 ms | 20/20 | Passed |
| 8 | 2 | 20 | 19/20 | 1 collector timeout | 98615.73 ms | 127517.60 ms | 128399.62 ms | 128620.13 ms | 19/20 | Blocked |

并发 1/2/4 的 60 条正式请求均为 HTTP 200、真实工具完成、无 429/5xx/超时、无工具错误；SSE 丢失、重复、终态重复、身份混合和 audit 计数差异均为 0。并发 8 的 19 条完成请求同样通过上述一致性校验；第 15 条正式请求 HTTP 200 后 150 秒内没有读到 SSE，采集器记录 timeout，没有 run_id、conversation_id、audit_id、trace_id，也没有执行会话清理，因此不能计为成功，也不能把它直接写成 Provider 根因。

### 数据库、资源与 Provider 信号

- 完整 1/2/4 波次数据库起点 371-qwen38-full-c124-schema-db-before.json：21/69/0/956/8375/693/84/2661；终点 379-qwen38-full-c124-schema-db-after.json：21/69/0/1022/8923/693/84/2661。
- 完整 8 波次数据库起点 381-qwen38-full-c8-schema-db-before.json：21/69/0/1022/8923/693/84/2661；终点 387-qwen38-full-c8-schema-db-after.json：22/71/0/1044/9117/693/84/2661。业务表 products、customers、finance_records 未变化；8 波次的 conversation/message 增加包含未完成采集请求的留痕，清理仍需补查。
- 资源采样 377-qwen38-full-c124-schema-resource-samples.jsonl、385-qwen38-full-c8-schema-resource-samples.jsonl 五阶段 SSH 均为 0，覆盖正式请求期间。c1/2/4 波次最高 Agent 431.3 MiB、PostgreSQL 78.21 MiB、JVM RSS 442680 kB/43 线程；c8 最高 Agent 441.9 MiB、PostgreSQL 81.78 MiB、JVM RSS 453536 kB/44 线程，正式期间 active_runs=8，清理后为 0，主机 swap 为 0。
- 8220 日志证据 389-qwen38-c8-log-correlation-20260913.json 在窗口内确认一次 Provider HTTP 503，Provider 队列等待最高 72403 ms；没有取得 Provider request_id、upstream_request_id 或 Retry-After。该窗口信号不能唯一归属到采集器超时请求。
- Token usage 仅在部分服务日志出现，仍不能按每条 Agent 请求可靠关联；Token/s、JVM heap-used、连接池等待、客户端 Android/Web/iOS 指标仍 unavailable。

### 证据与结论

本轮证据：346-qwen38-small-c1-schema-collector-format-validation.json 至 369-qwen38-small-c4-schema-db-after.json、370-qwen38-full-c124-schema-collector-format-validation.json 至 379-qwen38-full-c124-schema-db-after.json、380-qwen38-full-c8-schema-collector-format-validation.json 至 387-qwen38-full-c8-schema-db-after.json、388-qwen38-schema-runtime-deployment-20260913.json、389-qwen38-c8-log-correlation-20260913.json、390-qwen38-schema-optimization-comparison-20260913.json、391-qwen38-layer1-closeout-20260913.md。

本轮完成了一次 schema 稳定性优化闭环；qwen 并发 1/2/4 完成，8 级仍有 1 条采集超时，Provider 503 与高队列等待仍是外部依赖风险。第一层测量完成，但稳定性未通过；第二层和 671 条 Agent 测试暂不开始。

## qwen3.8-flash 并发 8 采集器复测与第一层完成（2026-09-13）

- 针对上一波并发 8 的单条 150 秒读取超时，只调整测试采集器：新增环境变量 AGENT_STREAM_TIMEOUT_SECONDS，默认读取超时 300 秒；未修改业务源码、部署配置或服务端语义。py_compile 和 git diff --check 通过。
- 当前线上镜像仍为 sxyq27-zhj-api:20260913T-schema-qwen-status，模型 qwen3.8-flash，Provider https://oneapi.sxyq27.online/v1，SSE https://zhj-api.sxyq27.online/v2/agent/chat/stream，Wire API chat_completions，账号标签 suffix-2002。
- 修复采集器后并发 8：2 个预热、20 个正式请求，20/20 COMPLETED；P50 97020.72 ms，P95 119952.72 ms，P99 121051.17 ms，最大 121325.78 ms。HTTP 200 为 20/20，429/5xx/采集超时均为 0，SSE 丢失/重复、终态重复、身份混合和 audit 计数差异均为 0，清理 20/20 成功。
- 数据库起点 391-qwen38-full-c8-timeout300-db-before.json：22/71/0/1044/9117/693/84/2661；终点 397-qwen38-full-c8-timeout300-db-after.json：22/71/0/1066/9308/693/84/2661。仅 Agent 审计留痕增加，products、customers、finance_records 未变化；正式期间 active_runs 最高 8，清理后为 0。
- 资源证据 395-qwen38-full-c8-timeout300-resource-samples.jsonl 五阶段 SSH 均为 0；最高 Agent 449.3 MiB、PostgreSQL 79.66 MiB、JVM RSS 460960 kB/45 线程，主机 swap 为 0。JVM heap-used、连接池等待仍 unavailable。

### 第一层最终总表

| 并发 | 正式请求 | 完成 | P50 | P95 | P99 | 最大耗时 | 错误 | 结果 |
|---:|---:|---:|---:|---:|---:|---:|---|---|
| 1 | 20 | 20/20 | 11197.24 ms | 31427.90 ms | 75339.16 ms | 86316.98 ms | 0 | Passed |
| 2 | 20 | 20/20 | 23535.81 ms | 67665.77 ms | 79931.99 ms | 82998.54 ms | 0 | Passed |
| 4 | 20 | 20/20 | 49236.69 ms | 57652.49 ms | 59684.75 ms | 60192.82 ms | 0 | Passed |
| 8 | 20 | 20/20 | 97020.72 ms | 119952.72 ms | 121051.17 ms | 121325.78 ms | 0 | Passed |

当前 qwen 镜像共 80 条正式请求全部完成，均通过真实 Agent、真实 product_catalog_lookup、云端 PostgreSQL、SSE 和 audit 链路。80/80 只有一个合法终态；SSE 与 audit 的事件类型、ID、seq、计数一致；未发现工具错误、LLM_ANSWER_UNAVAILABLE、Agent 429、HTTP 5xx、业务表变化或身份混合。Provider request_id、upstream_request_id、Retry-After 仍未取得；日志可见的 Provider 503 和高队列等待属于外部依赖风险，不能唯一归属到某一条 Agent 请求。Token usage 不能按请求可靠关联，Token/s 不计算。

### 第一层判定

- 第一层真实云端 Agent 性能优化闭环：完成。已完成 qwen schema 描述优化、定向测试、云端部署、优化后小规模 1/2/4 复测、完整 1/2/4/8 复测和采集器超时复测。
- 当前性能结果：并发 1/2/4/8 稳定性均通过；并发 8 的 P95 约 119.95 秒，属于需要在第二层继续分析的高延迟风险，不代表达到任何未定义 SLA。
- 第二层性能专项：允许开始，但本轮不启动。
- 671 条 Agent 测试：暂不启动；需先完成第二层性能专项并确认云端依赖风险。
- Android、Web、iOS 业务测试：暂不启动。

最终结论：第一层真实云端 Agent 性能优化闭环已完成；第二层性能专项允许进入但本轮不启动，671 条 Agent 测试暂不开始。

## qwen3.8-flash 模型切换与单条验证（2026-09-13）

- 根据后续测试要求，将 8220 `/opt/sxyq27/master-goods/runtime.env` 的 `AGENT_LLM_MODEL` 从 `deepseek-v4-flash-0731` 改为 `qwen3.8-flash`，并按现有 Compose 重新创建 Agent backend；PostgreSQL 和 Redis 未重建。
- Provider `/v1/models` 已列出 `qwen3.8-flash`。当前容器仍为 `sxyq27-zhj-api:20260913T0815-agent-provider-402-stop`，模型、Provider、Wire API 和 SSE 地址分别为 `qwen3.8-flash`、`https://oneapi.sxyq27.online/v1`、`chat_completions` 和 `https://zhj-api.sxyq27.online/v2/agent/chat/stream`。
- 模型切换后执行 1 个预热和 1 个正式真实云端 Agent 请求。正式请求 HTTP 200、终态 `completed`、耗时 6684.30 ms；真实调用 `product_catalog_lookup`。SSE 7 个事件，audit 事件列表 7 项，`event_count=7`、`emitted_event_count=7`，类型、ID 和 seq 一致；清理 HTTP 200。
- 数据库从 `21/69/0/919/8047/693/84/2661` 变为 `21/69/0/921/8062/693/84/2661`，仅增加 Agent 审计留痕；`products`、`customers`、`finance_records` 未变化。
- 这只是模型切换后的单条验证，不能替代并发 1/2/4/8 的正式样本。证据为 `328-qwen38-flash-runtime-switch-20260913.json` 和 `329-qwen38-flash-single-validation-conclusion-20260913.md`。本轮不将第一层写为完成，也不启动第二层或 671 条测试。

## 独立 Provider 402 诊断（2026-09-13 08:48）

- 从当前运行的 Agent 容器内部读取受保护运行凭据，向同一 Provider、同一模型和 `chat_completions` 入口发送了两次独立诊断请求，均返回 HTTP 402。
- 该请求没有经过 Agent、工具、PostgreSQL、SSE 或 audit，不计入正式性能样本，也不能替代真实 Agent 结果；响应正文已丢弃。
- 两次 `/chat/completions` 诊断只收到 `HTTP/2 402` 状态行，没有返回 `Retry-After`、Provider request ID 或限流响应头；同一运行凭据访问 `/models` 返回 HTTP 200，且返回列表包含 `deepseek-v4-flash-0731`。该结果与 8220 日志中的 5 个失败 run 相互印证：Provider 基础/models 路由和目标模型列表可访问，拒绝发生在 chat completion 路径或其选中的渠道、模型和账号策略。账单、额度、relay、上游模型或账号侧具体原因仍未确认。
- 一次先前的诊断命令因本地输出过滤语法错误没有取得状态码，未计入诊断数量；未保存其响应正文。本轮没有修改源码、测试文件或部署配置。

结论保持为：第一层测量完成，但稳定性未通过；第二层和 671 条 Agent 测试暂不开始。

## 波次数据记录：2026-09-14统一复测（最终有效波次明细）

### 当前需求与唯一正式组合

当前 Goal 为第一层真实云端 Agent 性能优化闭环。本节保留 2026-09-14 波次的完整明细，最终判定见文末章节。本轮唯一正式组合为：线上镜像 `sxyq27-zhj-api:20260913T-schema-qwen-status-enum`，模型 `qwen3.8-flash`，Provider `https://oneapi.sxyq27.online/v1`，Wire API `chat_completions`，Agent SSE `https://zhj-api.sxyq27.online/v2/agent/chat/stream`，账号标签 `suffix-2002`。固定提示词为“请查询一个商品的基本信息并简要回答。”，由真实 Agent 自动选择 `product_catalog_lookup`，查询使用云端 PostgreSQL。

容器启动时间为 `2026-09-13T13:40:55.993627487Z`，状态为 running，重启次数为 0。正式波次开始后没有切换镜像、模型、Provider、账号、提示词、工具 schema 或统计规则。历史 deepseek、其他 qwen 镜像和此前波次均单独保留，未与本轮合并。

### 正式结果

每级使用 2 个预热和 20 个正式请求。正式请求逐条写入 JSONL 并执行 flush/fsync，查询 audit 后清理会话。并发 4 出现失败后按停止规则未执行并发 8。

| 并发 | 正式请求 | 完成 | 失败 | P50(ms) | P95(ms) | P99(ms) | 最大(ms) | 错误 | 结果 |
|---:|---:|---:|---:|---:|---:|---:|---:|---|---|
| 1 | 20 | 20 | 0 | 38969.88 | 71771.02 | 75105.61 | 75939.26 | 0 | 通过 |
| 2 | 20 | 20 | 0 | 45651.38 | 72995.70 | 76002.56 | 76754.27 | 0 | 通过 |
| 4 | 20 | 19 | 1 | 100605.21 | 143128.32 | 149800.82 | 151468.95 | `STREAM_ERROR`/`IncompleteRead` 1 条 | 未通过 |
| 8 | 未执行 | - | - | - | - | - | - | 并发 4 失败后停止 | 未执行 |

并发 1、2 的 40 条正式请求均为 HTTP 200，真实调用 `product_catalog_lookup`，无 Agent 429、HTTP 5xx、超时、工具错误、SSE 丢失、SSE 重复、事件乱序、身份混合或 audit 不一致，清理均为 20/20。并发 4 的 19 条完成请求真实调用工具并完成回答。第 7 条请求 HTTP 200 后客户端出现 `IncompleteRead`，服务端 audit 恢复为 `run_id=9ab7d65b-075e-417e-bea1-5de7a7d6d01c`、`conversation_id=1277`、`status=failed`、`error_code=STREAM_ERROR`。该请求使本级失败 1 条，audit 持久化事件数为 6，发出事件数为 7，形成一次 SSE 与 audit 聚合计数差异。孤立会话随后补充清理并返回 HTTP 200，因此最终清理为 20/20；该请求仍保留为失败，未计入完成数。

本轮正式结果中的 Agent 402、429、503 和 HTTP 5xx 均为 0。历史日志时间窗中的 Provider 信号没有取得 Provider request ID、upstream request ID 或 `Retry-After`，也无法把外部信号逐条归属到本轮失败请求，不能把它写成该请求的唯一原因。

### 数据库、SSE 与资源

数据库计数顺序为 `agent_conversations/agent_messages/agent_drafts/agent_run_audits/agent_run_audit_events/products/customers/finance_records`。本轮起点 `450-final-unified-qwen38-enum-reset-20260913-c1-db-before.json` 为 `22/71/0/1128/9855/693/84/2661`。并发 1 终点 `456-final-unified-qwen38-enum-reset-20260913-c1-db-after.json` 的审计计数为 `1150/10050`，并发 2 终点 `464-final-unified-qwen38-enum-reset-20260913-c2-db-after.json` 为 `1172/10237`，并发 4 终点 `472-final-unified-qwen38-enum-reset-20260913-c4-db-after.json` 为 `1194/10416`。孤立会话清理后的最终计数见 `476-final-unified-qwen38-enum-reset-20260914-db-after-orphan-cleanup.json`：`22/71/0/1194/10416/693/84/2661`，`active_runs=0`。`products`、`customers`、`finance_records` 均未变化。

资源采样文件 `454-final-unified-qwen38-enum-reset-20260913-c1-resource-samples.jsonl`、`462-final-unified-qwen38-enum-reset-20260913-c2-resource-samples.jsonl` 和 `470-final-unified-qwen38-enum-reset-20260913-c4-resource-samples.jsonl` 的各阶段 SSH 返回码均为 0，且覆盖正式请求运行期间。并发 1/2/4 正式期间 Agent 最高约 `465.9/468.4/472.2 MiB`，PostgreSQL 约 `73.26/76.20/78.67 MiB`，JVM RSS 约 `451324/451104/455248 kB`，线程数为 `43/43/44`，Redis 约 `9.688 MiB`，主机 swap 为 0。清理后 `active_runs=0`。JVM heap-used、连接池等待、按请求 Token usage 和 Token/s 仍 unavailable，Token/s 不计算。

### 优化动作与验收

`product_catalog_lookup.status` 的整数枚举 `0/1` schema 描述优化属于此前波次，本轮只验证该镜像，没有新增业务源码、测试脚本或部署配置，也没有重新部署云端。此前优化的定向测试和 `bootJar` 结果继续按原证据保留。本轮并发 1、2 通过，但并发 4 出现服务端流失败和 audit 计数差异，不能形成稳定的 1/2/4/8 完整统一复测，也不能把历史不同镜像结果拼接为通过结论。

本轮证据文件为：

- `449-final-unified-qwen38-enum-reset-20260913-c1-collector-format-validation.json`
- `450-final-unified-qwen38-enum-reset-20260913-c1-db-before.json`
- `451-final-unified-qwen38-enum-reset-20260913-c1-warmup.jsonl`
- `452-final-unified-qwen38-enum-reset-20260913-c1-concurrency-1.jsonl`
- `453-final-unified-qwen38-enum-reset-20260913-c1-failure-analysis.json`
- `454-final-unified-qwen38-enum-reset-20260913-c1-resource-samples.jsonl`
- `455-final-unified-qwen38-enum-reset-20260913-c1-summary.json`
- `456-final-unified-qwen38-enum-reset-20260913-c1-db-after.json`
- `457-final-unified-qwen38-enum-reset-20260913-c2-collector-format-validation.json`
- `458-final-unified-qwen38-enum-reset-20260913-c2-db-before.json`
- `459-final-unified-qwen38-enum-reset-20260913-c2-warmup.jsonl`
- `460-final-unified-qwen38-enum-reset-20260913-c2-concurrency-2.jsonl`
- `461-final-unified-qwen38-enum-reset-20260913-c2-failure-analysis.json`
- `462-final-unified-qwen38-enum-reset-20260913-c2-resource-samples.jsonl`
- `463-final-unified-qwen38-enum-reset-20260913-c2-summary.json`
- `464-final-unified-qwen38-enum-reset-20260913-c2-db-after.json`
- `465-final-unified-qwen38-enum-reset-20260913-c4-collector-format-validation.json`
- `466-final-unified-qwen38-enum-reset-20260913-c4-db-before.json`
- `467-final-unified-qwen38-enum-reset-20260913-c4-warmup.jsonl`
- `468-final-unified-qwen38-enum-reset-20260913-c4-concurrency-4.jsonl`
- `469-final-unified-qwen38-enum-reset-20260913-c4-failure-analysis.json`
- `470-final-unified-qwen38-enum-reset-20260913-c4-resource-samples.jsonl`
- `471-final-unified-qwen38-enum-reset-20260913-c4-summary.json`
- `472-final-unified-qwen38-enum-reset-20260913-c4-db-after.json`
- `473-final-unified-qwen38-enum-reset-20260914-c4-orphan-audit-recovery.json`
- `474-final-unified-qwen38-enum-reset-20260914-runtime-after-c4.json`
- `475-final-unified-qwen38-enum-reset-20260914-c4-orphan-cleanup.json`
- `476-final-unified-qwen38-enum-reset-20260914-db-after-orphan-cleanup.json`
- `477-final-unified-qwen38-enum-reset-20260914-wave-summary.json`
- `478-final-unified-qwen38-enum-reset-20260914-conclusion.md`

### 第一层判定

本轮同一镜像、同一模型、同一 Provider、同一账号、同一提示词下，并发 1 和 2 各 20/20 完成，并发 4 为 19/20 并出现 `STREAM_ERROR`，并发 8 未执行。因此第一层优化和统一复测尚未完成；第二层及 671 条 Agent 测试暂不开始。Android、Web、iOS 测试暂不开始。

## L1-PERF-004 402 终止重试复测（2026-09-13）

### 当前需求与状态

- 当前 Goal：完成第一层真实云端 Agent 性能优化闭环。
- 当前阶段：第一层失败定位、性能优化和优化后小规模复测；第二层、671 条 Agent 测试及 Android/Web/iOS 业务测试均未开始。
- 当前运行镜像：`sxyq27-zhj-api:20260913T0815-agent-provider-402-stop`，容器启动时间 `2026-09-13T00:18:40.059089921Z`，容器状态为 `running`。
- Agent SSE、Provider、模型、Wire API 和账号标签保持为 `https://zhj-api.sxyq27.online/v2/agent/chat/stream`、`https://oneapi.sxyq27.online/v1`、`deepseek-v4-flash-0731`、`chat_completions`、`suffix-2002`。

### 优化动作与验证

- L1-PERF-003 已在上一小规模闭环中部署：规划失败直接保留失败终态，跳过无真实工具事实的最终回答重试；证据为 `287-optimization-planning-failure-fast-20260913.json` 和 `288-planning-failure-fast-runtime-deployment-20260913.json`。
- L1-PERF-004 修改 `Code/backend/src/main/java/com/zhihuiji/backend/infrastructure/ai/LongCatAnthropicClient.java`：Provider HTTP 402 与既有 429 一样，在当前请求内停止重复重试；没有改变成功响应、503 续答重试、权限、owner/store 隔离、事务、审计、SSE 终态或草稿确认。
- 测试文件 `Code/backend/src/test/java/com/zhihuiji/backend/infrastructure/ai/LongCatAnthropicClientTest.java` 新增 402 停止重试用例。`LongCatAnthropicClientTest`、`V2AgentAiServiceTest`、`AnswerSynthesizerTest` 和 `bootJar` 均通过。
- 本轮部署只重建 API 容器；PostgreSQL、Redis 未重建。容器内外 JAR SHA-256 均为 `8175915fd0226ab71a5f6e0a3dec167d1c054122e99c3ad1e2c2bbccf3964f28`。部署证据为 `308-provider-402-stop-runtime-deployment-20260913.json`。

### 当前镜像小规模结果

| 并发 | 预热 | 正式 | 完成 | 失败 | P50 | P95 | P99 | 最大耗时 | 结果 |
|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|
| 1 | 2 | 5 | 0/5 | 5 | 不适用 | 不适用 | 不适用 | 不适用 | `LLM_ANSWER_UNAVAILABLE` x5，Blocked |
| 2 | 未执行 | 未执行 | - | - | - | - | - | - | 因并发 1 失败停止 |
| 4 | 未执行 | 未执行 | - | - | - | - | - | - | 因并发 1 失败停止 |
| 8 | 未执行 | 未执行 | - | - | - | - | - | - | 因并发 1 失败停止 |

5 条正式请求均为 HTTP 200；audit 均为 `status=failed`、`llm_status=llm_planning_failed`、`plan_source=llm_planning_failed`、`tool_count=0`。每条流均为 `run_started -> error -> run_failed`，`event_count=3`、`emitted_event_count=3`，SSE 与 audit 类型、ID、序号和计数一致；SSE 丢失、重复、身份混合和多终态均为 0；5/5 清理返回 HTTP 200。由于没有完成请求，P50/P95/P99/最大完成耗时不适用。

### Provider 关联与重试变化

- `309-provider-402-stop-provider-log-correlation-20260913.json` 记录了 5/5 run 与 8220 API 的本地 `client_request_id` 关联。每条失败请求观察到 1 次 `tool_planning` 和 1 次兼容 JSON 规划请求，两次均为 HTTP 402；兼容规划均只出现 `attempt 1/3`，没有第 2、3 次请求。
- 兼容 JSON 规划的客户端日志标签仍为 `final_answer_json`，实际调用来自规划兼容路径；L1-PERF-003 已使回答层在 `llm_planning_failed` 后不再发起真实终答请求。
- 124 New API 的本轮日志未取得：本机记录的 `/Users/sunyiyang/Downloads/234.pem` 不存在；未使用替代密钥或跳板。Provider request ID、upstream request ID、`Retry-After` 和 Provider 响应正文仍未取得。当前只能确认 8220 API 请求收到 402，无法确认外部具体原因。

### 数据库、资源和限制

- 数据库起点 `301-planning-failure-fast-402-stop-small-c1-20260913-db-before.json`：`21/69/0/912/8026/693/84/2661`；终点 `307-planning-failure-fast-402-stop-small-c1-20260913-db-after.json`：`21/69/0/919/8047/693/84/2661`。只有审计留痕增加，`products`、`customers`、`finance_records` 未变化。
- `305-planning-failure-fast-402-stop-small-c1-20260913-resource-samples.jsonl` 五个阶段的 SSH 均返回 0；Agent RSS 最高约 451.1 MiB、PostgreSQL 约 67.11 MiB、JVM RSS 438336 kB/43 线程、Redis `connected_clients=1`、`blocked_clients=0`、主机 swap=0。资源阶段标记均有样本，但 `active_runs` 观测值为 0；没有取得 JVM heap-used 或连接池等待。
- Token usage、Token/s、Android、Web、iOS 真实性能样本仍不可得。外层采集命令最后因 zsh 变量名 `status` 为只读而返回 1；采集器本身已经完成逐条 JSONL flush/fsync，summary、failure-analysis、resource 和 db-after 文件均已生成，该包装错误不计入 Agent 失败。

### 本轮结论

L1-PERF-004 减少了 Provider 402 失败路径的无效重试，失败路径的中位耗时从上一波的 `4845.37 ms` 变为 `4517.84 ms`；没有恢复 Provider 返回能力，5/5 仍为 `LLM_ANSWER_UNAVAILABLE`。该前后耗时只用于失败路径参考，不能当作成功请求性能结论。

第一层测量完成，但稳定性未通过；第二层和 671 条 Agent 测试暂不开始。

本轮证据：`300-planning-failure-fast-402-stop-small-c1-20260913-collector-format-validation.json`、`301-planning-failure-fast-402-stop-small-c1-20260913-db-before.json`、`302-planning-failure-fast-402-stop-small-c1-20260913-warmup.jsonl`、`303-planning-failure-fast-402-stop-small-c1-20260913-concurrency-1.jsonl`、`304-planning-failure-fast-402-stop-small-c1-20260913-failure-analysis.json`、`305-planning-failure-fast-402-stop-small-c1-20260913-resource-samples.jsonl`、`306-planning-failure-fast-402-stop-small-c1-20260913-summary.json`、`307-planning-failure-fast-402-stop-small-c1-20260913-db-after.json`、`308-provider-402-stop-runtime-deployment-20260913.json`、`309-provider-402-stop-provider-log-correlation-20260913.json`、`310-provider-402-stop-optimization-comparison-20260913.json`、`311-provider-402-stop-small-c1-conclusion-20260913.md`。

本轮新增证据：`257-perf-layer1-final-c4-retry-20260913-collector-format-validation.json`、`258-perf-layer1-final-c4-retry-20260913-db-before.json`、`259-perf-layer1-final-c4-retry-20260913-warmup.jsonl`、`260-perf-layer1-final-c4-retry-20260913-concurrency-4.jsonl`、`261-perf-layer1-final-c4-retry-20260913-failure-analysis.json`、`262-perf-layer1-final-c4-retry-20260913-resource-samples.jsonl`、`263-perf-layer1-final-c4-retry-20260913-summary.json`、`264-perf-layer1-final-c4-retry-20260913-db-after.json`、`265-perf-layer1-final-c8-retry-20260913-collector-format-validation.json`、`266-perf-layer1-final-c8-retry-20260913-db-before.json`、`267-perf-layer1-final-c8-retry-20260913-warmup.jsonl`、`268-perf-layer1-final-c8-retry-20260913-concurrency-8.jsonl`、`269-perf-layer1-final-c8-retry-20260913-failure-analysis.json`、`270-perf-layer1-final-c8-retry-20260913-resource-samples.jsonl`、`271-perf-layer1-final-c8-retry-20260913-summary.json`、`272-perf-layer1-final-c8-retry-20260913-db-after.json`、`273-perf-layer1-final-current-runtime-20260913.json`、`274-perf-layer1-final-provider-correlation-20260913.json` 和 `275-perf-layer1-final-c4-c8-conclusion-20260913.md`。

## 当前关联镜像小规模复核（2026-09-13）

本节是当前报告的最新状态。前序完整并发 4/8 波次使用过其他镜像，继续保留为历史证据；本节只记录当前实际运行镜像的新增结果。本轮没有重复旧镜像的完整并发 1、2，也没有启动并发 4、8、第二层、671 条 Agent 测试或 Android/Web/iOS 业务测试。

### 线上条件与执行范围

- 实际运行镜像：`sxyq27-zhj-api:20260913T0732-agent-provider-correlation`；容器启动时间：`2026-09-12T23:31:50.230043291Z`；容器状态为 `running`，重启次数为 0。
- Agent SSE：`https://zhj-api.sxyq27.online/v2/agent/chat/stream`；Provider：`https://oneapi.sxyq27.online/v1`；模型：`deepseek-v4-flash-0731`；Wire API：`chat_completions`；账号标签：`suffix-2002`。
- 运行环境变量已核实为上述模型、Provider URL 和 Wire API；认证凭据没有进入命令输出、报告或证据。
- 当前镜像执行 2 个预热和 5 个正式请求，正式记录逐条写入 JSONL 并执行 flush/fsync；每条请求都先查询 audit，再清理会话。

### 当前镜像正式结果

| 并发 | 预热 | 正式 | 完成 | 失败 | P50 | P95 | P99 | 最大耗时 | 错误 | 结果 |
|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|---|
| 1 | 2 | 5 | 0/5 | 5 | 不适用 | 不适用 | 不适用 | 不适用 | `LLM_ANSWER_UNAVAILABLE` x5 | Blocked |
| 2 | 未执行 | 未执行 | - | - | - | - | - | - | 按停止规则不执行 | 未执行 |
| 4 | 未执行 | 未执行 | - | - | - | - | - | - | 并发 1 失败后停止 | 未执行 |
| 8 | 未执行 | 未执行 | - | - | - | - | - | - | 并发 1 失败后停止 | 未执行 |

5 条正式请求的 audit 均为 `status=failed`、`llm_status=model_empty_or_ungrounded`、`plan_source=llm_planning_failed`、`tool_count=0`；未发生工具调用或 PostgreSQL 业务查询。失败请求完成耗时为 2158.23、3166.56、6611.66、9983.94、10636.48 ms，本轮没有可用于 P50/P95/P99 的完成样本。

SSE 和 audit 结果均正常：5/5 只有一个合法终态，5/5 事件序号连续，事件 ID 无重复，未发现事件丢失、身份混合或 audit/SSE 类型、ID、序号和计数差异。每条失败流均为 `run_started -> error -> run_failed`，SSE、audit 事件列表、`event_count` 和 `emitted_event_count` 均为 3。5/5 清理请求返回 HTTP 200。

### Provider 请求级关联

本轮新增证据 `285-post-correlation-small-c1-provider-correlation-20260913.json`。5 个失败 run 均与 8220 API 容器中的本地 `client_request_id` 关联成功：

- 3 条最终流请求返回 HTTP 429；2 条最终流请求返回 HTTP 402。
- 每条请求都出现 Provider 请求失败记录；规划和 JSON 重试记录显示 `ResourceAccessException`，这些记录没有暴露上游 HTTP 状态。
- 本地 `client_request_id` 可取得；真正的 Provider request id、upstream request id、`Retry-After` 和响应正文仍未取得。
- 124 New API 本轮日志未取得。因此可以确认本轮失败发生在 Agent 到 Provider 的请求链路，无法进一步确认 429/402 的外部具体原因。
- 这项 Provider 证据与 audit 聚合计数结论分开：本轮 5/5 audit 计数持续一致，失败原因不是 SSE 或 audit 聚合。

### 数据库与资源

- 数据库起点 `278-post-correlation-small-c1-20260913-db-before.json`：`agent_conversations/agent_messages/agent_drafts/agent_run_audits/agent_run_audit_events/products/customers/finance_records = 21/69/0/898/7984/693/84/2661`。
- 数据库终点 `284-post-correlation-small-c1-20260913-db-after.json`：`21/69/0/905/8005/693/84/2661`。变化只出现在测试审计留痕；`products`、`customers`、`finance_records` 没有变化。
- `282-post-correlation-small-c1-20260913-resource-samples.jsonl` 的 5 个阶段均为 `ssh_exit_code=0`，并包含正式请求期间样本；正式期间 `active_runs=1`，清理后为 0。
- 正式期间 Agent 约 420.7 MiB/45 PIDs，PostgreSQL 约 77.93 MiB/11 PIDs，Redis 约 3.746 MiB/6 PIDs，JVM RSS 约 406780 kB/45 线程；本波次主机 swap 使用量为 0。JVM heap-used 和连接池等待仍 unavailable。

### 本轮判断

- 当前镜像小规模并发 1 失败，不能继续并发 2、4、8；没有新的完整并发 4/8 样本。
- 本轮没有新增源码、测试文件、采集器脚本或部署配置改动，也没有重新部署云端；新增对象是当前证据目录中的运行记录、采集结果、Provider 关联证据和本报告、性能台账。
- 第一层仍未完成。第二层性能专项、671 条 Agent 测试和客户端业务测试均暂不开始。
- Token usage、Token/s、JVM heap-used、连接池等待以及 Android、Web、iOS 真实性能样本仍不可取得。

本轮新增证据：`276-current-runtime-deployment-20260913.json`、`277-post-correlation-small-c1-20260913-collector-format-validation.json`、`278-post-correlation-small-c1-20260913-db-before.json`、`279-post-correlation-small-c1-20260913-warmup.jsonl`、`280-post-correlation-small-c1-20260913-concurrency-1.jsonl`、`281-post-correlation-small-c1-20260913-failure-analysis.json`、`282-post-correlation-small-c1-20260913-resource-samples.jsonl`、`283-post-correlation-small-c1-20260913-summary.json`、`284-post-correlation-small-c1-20260913-db-after.json`、`285-post-correlation-small-c1-provider-correlation-20260913.json`、`286-post-correlation-small-c1-stop-conclusion-20260913.md`。

结论保持为：第一层测量完成，但稳定性未通过；第二层和 671 条 Agent 测试暂不开始。

## 8220 当前 Provider 402 日志复核（2026-09-13 08:42）

- 当前线上容器仍为 `sxyq27-zhj-api:20260913T0815-agent-provider-402-stop`；本次只读取 8220 Agent 容器日志，没有发送新的 Agent 请求，也没有切换镜像。
- 在已有失败窗口中，5 个 formal run 均能通过 `run_id` 和本地 `client_request_id` 关联到 8220 日志；`tool_planning` 5/5 与兼容规划 5/5 均收到 HTTP 402，观测到的 `queue_wait_ms` 为 0。
- Provider request ID、upstream request ID、`Retry-After`、响应正文和 124 New API 日志仍未取得。当前证据可以确认 Agent 到 Provider 的 402，不能确认账单、额度、relay 或上游模型的具体原因。
- 日志中通用的“failed after 3 retries”文字与请求开始/失败成对记录不一致；本轮按实际请求记录和 `309-provider-402-stop-provider-log-correlation-20260913.json` 的调用数统计，不把该文字当作真实请求次数。本轮不修改源码或部署配置。
- 该复核不计入新的性能样本。证据文件为 `312-provider-402-live-log-recheck-20260913.json`。

结论保持为：第一层测量完成，但稳定性未通过；第二层和 671 条 Agent 测试暂不开始。

## 历史波次记录：2026-09-13统一复测（已被 2026-09-14 波次取代）

### 唯一正式组合

- 本轮唯一组合为：镜像 `sxyq27-zhj-api:20260913T-schema-qwen-status`；模型 `qwen3.8-flash`；Provider `https://oneapi.sxyq27.online/v1`；Wire API `chat_completions`；Agent SSE `https://zhj-api.sxyq27.online/v2/agent/chat/stream`；账号标签 `suffix-2002`。
- 固定输入为“请查询一个商品的基本信息并简要回答。”，工具由真实 Agent 自动选择，观测为 `product_catalog_lookup`。每级使用 2 个预热和 20 个正式请求，正式请求即时 JSONL flush/fsync，查询 audit 后清理，流读取上限 300 秒；波次开始后未切换环境。
- 历史 deepseek、其他 qwen 镜像和本轮结果分开保留，未合并统计。

### 本轮结果

- 并发 1：20 个正式请求中 19 个完成、1 个 `EXHAUSTED`；错误码 `AGENT_TOOL_EXECUTION_FAILED`，失败前出现 `TOOL_ARGUMENTS_INVALID`。19 个完成请求 P50/P95/P99/最大为 18457.24/33202.68/37057.50/38021.21 ms；首事件/首工具/首回答 P50 为 18449.19/18449.31/18449.36 ms。
- HTTP 200 为 20/20，429、5xx、采集超时为 0。SSE 丢失、重复、终态异常、身份混合和 audit 计数不一致均为 0；清理 20/20 成功。并发 2、4、8 按停止规则未执行。
- 失败请求的 run、conversation、audit、trace 均关联正常；SSE 为 `run_started -> plan_delta -> tool_started -> tool_failed -> answer_delta -> answer_completed -> run_exhausted`，SSE 与 audit 类型、ID、序号和计数一致。详细证据见 `424-final-unified-qwen38-20260913-failure-analysis.json`、`428-final-unified-qwen38-conclusion-20260913.md` 和 `429-final-unified-qwen38-failure-analysis-20260913.json`。
- 数据库起点 `421-final-unified-qwen38-20260913-db-before.json`，终点 `427-final-unified-qwen38-20260913-db-after.json`；仅 Agent 审计留痕增加，`products`、`customers`、`finance_records` 未变化。资源证据 `425-final-unified-qwen38-20260913-resource-samples.jsonl` 五阶段 SSH 均为 0；正式期间主机 swap=0，Agent 约 460.3 MiB，PostgreSQL 约 62.03 MiB，JVM RSS 464224 kB/43 线程；heap-used、连接池等待 unavailable。
- Provider request ID、upstream request ID、Retry-After 和按请求 Token usage 仍 unavailable，不能计算 Token/s。本轮没有新增业务源码或线上部署；采集器仅使用已有的 `AGENT_TEST_PROMPT` 输入参数。

### 第一层判定

- 第一层优化和复测尚未稳定完成；同一正式组合只完成到并发 1，且有 1 条工具参数错误导致的 `EXHAUSTED`。优化前后完整对照数据暂不成立。
- 第二层性能专项：暂不开始。671 条 Agent 测试：暂不开始。Android、Web、iOS：暂不开始。

## 第一层最新统一复测（2026-09-14 实际运行组合）

### 当前需求与唯一正式组合

当前 Goal 为第一层真实云端 Agent 性能优化闭环。本节覆盖 `null-sentinel-fix-20260914-full` 唯一正式波次，替代前文作为当前判定依据。用户指定的候选镜像为 `sxyq27-zhj-api:20260913T-schema-qwen-status-enum`；开始前实际核实到并持续运行的镜像为 `sxyq27-zhj-api:20260914T0405-agent-integer-string-normalization`，因此本波次固定使用实际镜像，未与候选镜像或其他历史波次合并。实际运行组合为：镜像 `sxyq27-zhj-api:20260914T0405-agent-integer-string-normalization`，模型 `qwen3.8-flash`，Provider `https://oneapi.sxyq27.online/v1`，Wire API `chat_completions`，Agent SSE `https://zhj-api.sxyq27.online/v2/agent/chat/stream`，账号标签 `suffix-2002`。固定提示词为“请查询一个商品的基本信息并简要回答。”，工具由真实 Agent 自动选择 `product_catalog_lookup`，查询使用云端 PostgreSQL。

容器启动时间为 `2026-09-13T20:07:58.416162103Z`，当前状态为 running，重启次数为 0。正式波次期间没有切换镜像、模型、Provider、账号、提示词、工具 schema 或统计规则。历史 deepseek、其他 qwen 镜像和其他波次继续单独保留，未与本波次合并。

### 正式结果

每个已执行级别使用 2 个预热请求和 20 个正式请求；正式请求逐条 JSONL 写入并 flush/fsync，先查询 audit 后清理会话。并发 4 出现失败后按停止规则未执行并发 8。并发 4 的 P50/P95/P99/最大耗时仅基于 17 个完成请求。

| 并发 | 正式 | 完成 | 失败 | P50(ms) | P95(ms) | P99(ms) | 最大(ms) | 首 SSE P50(ms) | 首事件 P50(ms) | 首工具 P50(ms) | 首回答 P50(ms) | 错误 |
|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|
| 1 | 20 | 20 | 0 | 6823.47 | 12959.62 | 29952.97 | 34201.31 | 288.23 | 288.26 | 4270.69 | 6807.71 | 无 |
| 2 | 20 | 20 | 0 | 10914.29 | 24799.24 | 25277.73 | 25397.35 | 291.71 | 291.78 | 5059.31 | 10898.32 | 无 |
| 4 | 20 | 17 | 3 | 20556.94 | 22236.35 | 22656.53 | 22761.57 | 297.27 | 297.34 | 9904.18 | 20554.59 | `LLM_ANSWER_UNAVAILABLE` x3 |
| 8 | 未执行 | - | - | - | - | - | - | - | - | - | - | 并发 4 失败后停止 |

60 条正式请求均返回 HTTP 200；完成 57 条，失败 3 条。3 条失败的 `run_id` 为 `5508ba32-571c-40eb-81b8-b6362ff63542`、`70fd4704-7586-4b37-98c7-2c5308ed9410`、`9eeec4ba-527d-4513-b3ed-e2fe010afe37`，均为 `LLM_ANSWER_UNAVAILABLE`，audit 的 `llm_status=llm_planning_failed`，没有工具事件，也没有该失败 run 的 PostgreSQL 业务查询记录。采集器将其归为 `provider_error`，但请求级外部根因仍未确认。

### SSE、audit、清理与数据库

- 60/60 请求只有一个合法终态；SSE 丢失、重复、seq 不连续、身份混合和 audit/SSE 事件类型、ID、seq、计数差异均为 0。
- 3 条失败流均为 `run_started -> error -> run_failed`，SSE 和 audit 各 3 个事件，`event_count=3`、`emitted_event_count=3`。
- 60/60 cleanup 返回 HTTP 200；三个级别清理后 `active_runs=0`。
- 数据库计数顺序为 `agent_conversations/agent_messages/agent_drafts/agent_run_audits/agent_run_audit_events/products/customers/finance_records`。起点 `22/71/0/1318/11472/693/84/2661`，终点 `22/71/0/1384/12019/693/84/2661`；增加的是 Agent 审计留痕，`products`、`customers`、`finance_records` 保持不变。

### Provider 与资源证据

8220 的 `sxyq27-zhj-api` 容器日志在失败时间窗内没有 3 个失败 run 的 Provider 请求日志行；124 New API 日志 `/opt/sxyq27/new-api/logs/oneapi-20260913201120.log` 在本地时间 04:21:55–04:21:57 记录 7 个 channel #24、模型 `qwen3.8-flash` 的 HTTP 429，消息属于请求频率限制。New API request ID、Provider request ID、upstream request ID、Retry-After 和每个 run 的重试次数无法建立对应关系。因此只能确认时间窗级的外部限流信号，不能把它写成任一失败 run 的唯一原因；SSE、数据库和采集器均没有证据显示为失败来源。详见 `520-null-sentinel-fix-20260914-provider-log-correlation.json`。

资源采样 `516-null-sentinel-fix-20260914-full-resource-samples.jsonl` 共 15 条，SSH 15/15 成功，覆盖并发 1、2、4 的正式期间。Agent 内存峰值约 445.5 MiB，PostgreSQL 约 71.83 MiB，JVM RSS 峰值 457212 kB，线程峰值 45，Redis 未见阻塞客户端，主机 swap 使用量为 0。JVM heap-used、连接池等待、按请求 Token usage、Token/s 和 Android/Web/iOS 客户端指标仍 unavailable。

### 优化动作与最终判定

此前的最小改动为按工具 schema 将严格整数字符串规范化为整数，涉及 `Code/backend/src/main/java/com/zhihuiji/backend/application/service/v2/agent/tool/ToolArgumentsValidator.java`、`Code/backend/src/main/java/com/zhihuiji/backend/application/service/v2/agent/tool/ToolExecutor.java` 和对应 `ToolExecutorTest.java`；定向测试及 `bootJar` 已通过并已部署。本轮没有新增源码、测试脚本、部署配置或云端部署。

本轮并发 1、2 通过，并发 4 出现 3 条 Agent 终态失败；并发 8 未执行，不能形成同一组合下的 1/2/4/8 稳定结果。第一层优化和统一复测尚未完成；第二层及 671 条 Agent 测试暂不开始。Android、Web、iOS 测试暂不开始。

本节证据：`508-null-sentinel-fix-20260914-full-collector-format-validation.json`、`509-null-sentinel-fix-20260914-full-db-before.json`、`510-null-sentinel-fix-20260914-full-warmup.jsonl`、`511-null-sentinel-fix-20260914-full-concurrency-1.jsonl`、`512-null-sentinel-fix-20260914-full-concurrency-2.jsonl`、`513-null-sentinel-fix-20260914-full-concurrency-4.jsonl`、`515-null-sentinel-fix-20260914-full-failure-analysis.json`、`516-null-sentinel-fix-20260914-full-resource-samples.jsonl`、`517-null-sentinel-fix-20260914-full-summary.json`、`518-null-sentinel-fix-20260914-full-db-after.json`、`519-null-sentinel-fix-20260914-current-runtime-deployment.json`、`520-null-sentinel-fix-20260914-provider-log-correlation.json`、`521-null-sentinel-fix-20260914-full-failure-analysis.json`、`522-null-sentinel-fix-20260914-full-analysis-conclusion.md`。

### 第一层判定

同一镜像、同一模型、同一 Provider、同一账号、同一提示词下，并发 1 和 2 各 20/20 完成，并发 4 为 17/20，并发 8 未执行。因此第一层优化和统一复测尚未完成；第二层及 671 条 Agent 测试暂不开始。Android、Web、iOS 测试暂不开始。

## 第二阶段进入条件复核（2026-09-14）

状态：`L1_NOT_COMPLETE`。

本次只读取第一层最新实际波次，未创建第二阶段性能样本。当前实际组合为镜像 `sxyq27-zhj-api:20260914T0405-agent-integer-string-normalization`、模型 `qwen3.8-flash`、Provider `https://oneapi.sxyq27.online/v1`、Wire API `chat_completions`、Agent SSE `https://zhj-api.sxyq27.online/v2/agent/chat/stream`、账号标签 `suffix-2002`。第一层并发 1 为 20/20，并发 2 为 20/20，并发 4 为 17/20 并出现 3 条 `LLM_ANSWER_UNAVAILABLE`，并发 8 未执行。第一层要求的 1/2/4/8 全部 20/20、无终态失败和并发 8 证据尚未满足。

因此 AG-P-001 至 AG-P-027 不启动，671 条 Agent 测试、Android/Web/iOS 业务测试和日报功能不启动。现有第一层 Provider 时间窗口 429 仍没有与具体 run 建立请求级对应关系，不能把它写成单个失败 run 的唯一原因。下一步需要先处理或明确归类 `LLM_ANSWER_UNAVAILABLE`，再使用同一实际组合完成第一层剩余复测。历史 Qwen、DeepSeek 和其他镜像结果继续单独保留，不与第二阶段或本波次合并。

本复核依据：`517-null-sentinel-fix-20260914-full-summary.json`、`518-null-sentinel-fix-20260914-full-db-after.json`、`519-null-sentinel-fix-20260914-current-runtime-deployment.json`、`520-null-sentinel-fix-20260914-provider-log-correlation.json`、`521-null-sentinel-fix-20260914-full-failure-analysis.json`、`522-null-sentinel-fix-20260914-full-analysis-conclusion.md`。

## 第一层当前波次诊断复测（2026-09-14）

本节是当前 Goal 的最新结果。前文不同模型、不同镜像和外部 Provider 异常波次继续保留，不能与本节混合计算。

### 唯一正式组合

- 镜像：`sxyq27-zhj-api:20260914T0405-agent-integer-string-normalization`
- 模型：`qwen3.8-flash`
- Provider：`https://oneapi.sxyq27.online/v1`
- Wire API：`chat_completions`
- Agent SSE：`https://zhj-api.sxyq27.online/v2/agent/chat/stream`
- 账号标签：`suffix-2002`
- 固定提示词：`请查询一个商品的基本信息并简要回答。`
- 工具：真实 Agent 自动选择 `product_catalog_lookup`
- 容器启动时间：`2026-09-13T20:07:58.416162103Z`

本波次未切换镜像、模型、Provider、账号、提示词、工具 schema、超时或统计方式。凭据只从本机受保护存储读取。

### 统一结果

| 并发 | 正式请求 | 完成 | 失败 | P50(ms) | P95(ms) | P99(ms) | 最大(ms) | 首事件 P50(ms) | 首工具 P50(ms) | 首回答 P50(ms) |
|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 1 | 20 | 20 | 0 | 6349.52 | 8233.58 | 8830.08 | 8979.20 | 293.09 | 3384.29 | 6336.41 |
| 2 | 20 | 20 | 0 | 9438.47 | 12153.05 | 13481.87 | 13814.08 | 298.08 | 4804.18 | 9427.15 |
| 4 | 20 | 20 | 0 | 20777.29 | 25489.65 | 27521.11 | 28028.97 | 339.75 | 10330.99 | 20760.39 |
| 8 | 20 | 20 | 0 | 45665.40 | 54818.39 | 56537.69 | 56967.51 | 286.13 | 22448.14 | 45654.81 |

80/80 正式请求均通过真实云端 Agent、云端模型、真实 `product_catalog_lookup` 和云端 PostgreSQL 完成。HTTP 5xx、Agent 429、Provider 402/429/503、超时、重试耗尽、`TOOL_ARGUMENTS_INVALID`、`LLM_ANSWER_UNAVAILABLE`、SSE 丢失、SSE 重复、seq 不连续、身份混合和 audit 不一致均为 0。80/80 cleanup 成功，清理后 `active_runs=0`。

### 外部 Provider 波次

同一组合的前一轮并发 2 曾出现 4 条 `LLM_ANSWER_UNAVAILABLE`。8220 应用日志已经按 `run_id` 关联到 `chat/completions` 的 Provider HTTP 429，并记录 `tool_planning`、`final_answer_json` 和重试耗尽；失败流没有工具事件，也没有业务 PostgreSQL 查询。Provider request ID、upstream request ID 和 `Retry-After` 未取得。该轮单独标记为外部供应商限流，不计入本节 80 条稳定正式样本，也没有因此改动业务逻辑。

### 修复与优化结果

- 项目侧修复已完成：`product_catalog_lookup.status` 保持整数枚举 `0/1`，严格整数字符串在工具执行前规范化为 JSON 整数；布尔值、小数和非法文本继续拒绝。
- 定向 `ToolExecutorTest`、`ProductCatalogLookupToolTest` 通过；当前镜像为该修复版本。本轮没有新增源码、采集器脚本、部署配置或云端部署。
- 历史波次出现工具参数类型异常；当前 80 条正式请求中工具参数错误为 0，工具与数据库查询链路均真实执行。耗时差异受 Provider 时间窗口影响，未据此宣称服务端耗时改善。

### 数据库、资源和指标缺口

数据库计数顺序为 `agent_conversations/agent_messages/agent_drafts/agent_run_audits/agent_run_audit_events/products/customers/finance_records`。接受的 1/2/4/8 结果起点为 `22/71/0/1384/12019/693/84/2661`，终点为 `22/71/0/1494/12956/693/84/2661`。只有 Agent 审计记录增加，`products`、`customers`、`finance_records` 未变化。

资源采样 SSH 全部成功：Agent 约 `448.1-466.7 MiB`，PostgreSQL 约 `47.67-92.68 MiB`，JVM RSS `449864-451272 kB`，线程 `40-44`，Redis blocked clients 为 `0`，主机 swap 为 `0`。JVM heap-used、连接池等待、真实 Token usage、Token/s 和 Android/Web/iOS 客户端指标为 `unavailable`。

### 第一层结论

当前唯一组合下并发 1/2/4/8 各 20/20 完成，SSE、audit、终态、身份关联、清理和业务数据均正常；外部 Provider 限流已独立归类。**第一层真实云端 Agent 性能优化闭环完成。**

允许进入第二层性能专项测试，但本轮不自动启动第二层；671 条 Agent 测试、Android/Web/iOS 测试仍需等待下一条指令。

本节证据：`523-l1-provider-external-20260914-collector-format-validation.json`、`524-l1-provider-external-20260914-db-before.json`、`526-l1-provider-external-20260914-concurrency-1.jsonl`、`527-l1-provider-external-20260914-concurrency-2.jsonl`、`530-l1-provider-external-20260914-failure-analysis.json`、`531-l1-provider-external-20260914-resource-samples.jsonl`、`532-l1-provider-external-20260914-summary.json`、`533-l1-provider-external-20260914-db-after.json`、`534-l1-provider-external-20260914-current-runtime-deployment.json`、`536-l1-provider-stable-retry-20260914-c2-collector-format-validation.json`、`539-l1-provider-stable-retry-20260914-c2-concurrency-2.jsonl`、`542-l1-provider-stable-retry-20260914-c2-summary.json`、`543-l1-provider-stable-retry-20260914-c2-db-after.json`、`544-l1-provider-stable-retry-20260914-c4-collector-format-validation.json`、`547-l1-provider-stable-retry-20260914-c4-concurrency-4.jsonl`、`550-l1-provider-stable-retry-20260914-c4-summary.json`、`551-l1-provider-stable-retry-20260914-c4-db-after.json`、`552-l1-provider-stable-retry-20260914-c8-collector-format-validation.json`、`555-l1-provider-stable-retry-20260914-c8-concurrency-8.jsonl`、`558-l1-provider-stable-retry-20260914-c8-summary.json`、`559-l1-provider-stable-retry-20260914-c8-db-after.json`、`560-l1-provider-stable-retry-20260914-current-runtime.json`、`561-l1-provider-stable-retry-20260914-final-summary.json`、`562-l1-provider-stable-retry-20260914-conclusion.md`。

## 第二阶段证据同步（2026-09-14）

第一层已由 `561-l1-provider-stable-retry-20260914-final-summary.json` 和 `562-l1-provider-stable-retry-20260914-conclusion.md` 单独验收完成。本节开始记录第二阶段专项；第二阶段样本全部使用当前实际组合：镜像 `sxyq27-zhj-api:20260914T0845-agent-negated-write-intent`，模型 `qwen3.8-flash`，Provider `https://oneapi.sxyq27.online/v1`，Wire API `chat_completions`，Agent SSE `https://zhj-api.sxyq27.online/v2/agent/chat/stream`，账号标签 `suffix-2002`。未把 DeepSeek、其他 Qwen 镜像或第一层其他波次并入本节。

### 已完成专项

- `AG-P-001` 无工具基线：SSE 小测 `569-phase2-ag-p001-no-tool-20260914-summary.json` 为 5/5 完成；REST 补充 `613-phase2-ag-p001-no-tool-rest-20260914-summary.json` 也为 5/5 完成。SSE 与 REST 分开统计；REST 没有 SSE 事件，audit 只有 `run_started`，按 REST 语义计数一致。Token usage 和 Token/s unavailable。
- `AG-P-002` 单只读工具：`577-phase2-ag-p002-single-readonly-20260914-summary.json`、`586-phase2-ag-p002-concurrency-2-4-20260914-summary.json` 和 `596-phase2-ag-p002-concurrency-permit-2-20260914-summary.json` 均为真实 Agent 样本。并发许可由 1 调整为 2 后，并发 2 与 4 的小样本 P50 分别从 10648.81/18797.72 ms 变为 8012.44/13155.37 ms；并发 1 从 6072.31 变为 7186.66 ms，样本量小，保留长尾风险。调整记录见 `607-phase2-ag-p002-concurrency-permit-2-conclusion-20260914.md`。
- `AG-P-003` 参数复杂度和结果规模：simple、filtered、large 三组均为真实 Agent 小测，证据分别为 `623-phase2-ag-p003-simple-20260914-summary.json`、`631-phase2-ag-p003-filtered-20260914-summary.json`、`639-phase2-ag-p003-large-20260914-summary.json`；large 组 5/5 完成，P50/P95/P99/最大为 7856.80/8577.14/8604.03/8610.75 ms。当前证据支持结果块增大带来响应耗时上升，尚未证明数据库或序列化路径存在可改缺陷。
- `AG-P-008` 多工具连续调用：修复前 `647-phase2-ag-p008-multitool-20260914-summary.json` 的 5 条请求均耗尽；源码根因是否定写入语句中的“创建”被误判为写入目标。最小修复部署到当前镜像后，`656-phase2-ag-p008-negated-write-intent-fix-20260914-summary.json` 为 5/5 完成，P50/P95/P99/最大为 14291.31/37715.40/40442.87/41124.74 ms；前后对照见 `659-phase2-ag-p008-negated-write-intent-fix-comparison-20260914.json`，结论见 `660-phase2-ag-p008-negated-write-intent-fix-conclusion-20260914.md`。

上述 4 个专项目前均为小规模专项证据，不代表各场景计划的 20 条正式样本。每条记录均沿用真实 Agent SSE、真实工具和云端 PostgreSQL 链路；正式业务表没有变化，审计留痕增加，cleanup 成功，active runs 在清理后归零。当前可取得的资源包括 API、PostgreSQL、JVM RSS/线程、Redis 和主机 swap；JVM heap-used、连接池等待、按请求 Token usage/Token/s 仍 unavailable。

### 第二阶段当前状态

`AG-P-001`、`AG-P-002`、`AG-P-003`、`AG-P-008` 已有真实样本；其余 `AG-P-004` 至 `AG-P-027` 尚未全部完成，后续继续按专项顺序逐项采集并在有证据时选择单一优化因素。`AG-P-023`、`AG-P-024` 的客户端设备指标按现有环境记录为 Blocked，不能用服务端数据替代。671 条 Agent 测试、Android/Web/iOS 业务测试和日报功能继续暂停。

### 第二阶段新增证据（AG-P-004、AG-P-006、AG-P-007）

当前实际运行组合经 8220 只读容器核实为：镜像 `sxyq27-zhj-api:20260914T0845-agent-negated-write-intent`，模型 `qwen3.8-flash`，Provider `https://oneapi.sxyq27.online/v1`，Wire API `chat_completions`，Agent SSE `https://zhj-api.sxyq27.online/v2/agent/chat/stream`，账号标签 `suffix-2002`；容器启动时间为 `2026-09-14T01:01:52.920127143Z`，`AGENT_LLM_MAX_CONCURRENT_REQUESTS=2`。凭据仍只从本机受保护存储读取。本段没有切换环境。

- `AG-P-004` 草稿请求首轮 5 条全部未完成：4 条 `AGENT_TOOL_EXECUTION_FAILED`，均含 `TOOL_ARGUMENTS_INVALID`；1 条 `LLM_ANSWER_UNAVAILABLE`。5 条均有真实 `create_product` 工具事件，草稿表保持 0，正式业务表未变化，cleanup 为 5/5。证据为 `661-phase2-ag-p004-draft-20260914-collector-format-validation.json`、`662-phase2-ag-p004-draft-20260914-db-before.json`、`663-phase2-ag-p004-draft-20260914-warmup.jsonl`、`664-phase2-ag-p004-draft-20260914-concurrency-1.jsonl`、`665-phase2-ag-p004-draft-20260914-failure-analysis.json`、`666-phase2-ag-p004-draft-20260914-resource-samples.jsonl`、`667-phase2-ag-p004-draft-20260914-summary.json`、`668-phase2-ag-p004-draft-20260914-db-after.json`。
- `AG-P-004` 显式补充 5 条全部以 `AGENT_ITERATION_EXHAUSTED` 结束；均真实调用过 `product_catalog_lookup`，没有生成草稿。1 条正式流只收到 `run_started`，其 audit 已保存完整事件，属于采集到的 SSE/audit 不一致。证据为 `669-phase2-ag-p004-draft-explicit-20260914-collector-format-validation.json`、`670-phase2-ag-p004-draft-explicit-20260914-db-before.json`、`671-phase2-ag-p004-draft-explicit-20260914-warmup.jsonl`、`672-phase2-ag-p004-draft-explicit-20260914-concurrency-1.jsonl`、`673-phase2-ag-p004-draft-explicit-20260914-failure-analysis.json`、`674-phase2-ag-p004-draft-explicit-20260914-resource-samples.jsonl`、`675-phase2-ag-p004-draft-explicit-20260914-summary.json`、`676-phase2-ag-p004-draft-explicit-20260914-db-after.json`。现有证据没有确认项目源码中的参数生成原因，暂不改源码；模型/Agent 规划结果需单独归类。
- `AG-P-006/007` 正式 5 条全部完成。P50/P95/P99/最大完成耗时为 `13096.72/19842.69/20537.52/20711.23 ms`；首事件、首工具、首回答 P50 为 `293.81/5429.64/13083.73 ms`。正式样本 HTTP、终态、SSE、audit、身份和 cleanup 均正常。证据为 `677-phase2-ag-p006-p007-sse-full-20260914-collector-format-validation.json`、`678-phase2-ag-p006-p007-sse-full-20260914-db-before.json`、`680-phase2-ag-p006-p007-sse-full-20260914-concurrency-1.jsonl`、`681-phase2-ag-p006-p007-sse-full-20260914-failure-analysis.json`、`682-phase2-ag-p006-p007-sse-full-20260914-resource-samples.jsonl`、`683-phase2-ag-p006-p007-sse-full-20260914-summary.json`、`684-phase2-ag-p006-p007-sse-full-20260914-db-after.json`。
- `AG-P-006/007` 的 2 条预热请求中有 1 条只收到 `run_started`，但 audit 有完整 8 个事件；该异常单独记为预热传输/采集异常，不计入 5 条正式完成样本的成功指标。正式请求没有 SSE 丢失、重复、乱序、身份混合或 audit 计数不一致。

本轮数据库计数顺序仍为 `agent_conversations/agent_messages/agent_drafts/agent_run_audits/agent_run_audit_events/products/customers/finance_records`。`AG-P-004` 首轮为 `22/71/0/1572/13629/693/84/2661` → `22/71/0/1579/13701/693/84/2661`；显式补充为 `22/71/0/1579/13701/693/84/2661` → `22/71/0/1586/13806/693/84/2661`；`AG-P-006/007` 为 `22/71/0/1586/13806/693/84/2661` → `22/71/0/1593/13863/693/84/2661`。只增加 Agent 审计记录，业务表没有变化。三轮正式样本均 cleanup 成功，清理后 active runs 为 0。JVM heap-used、连接池等待、Provider 等待、按请求 Token usage/Token/s 仍 unavailable。

上述结果是第二阶段的小规模真实云端样本，不能代替 TEST_PLAN 规定的完整样本量。当前状态：`AG-P-004 Failed`（模型/Agent 工具规划或参数结果未确认源码原因），`AG-P-006/007 Passed-small-sample`（预热异常另记），其他专项继续按条件执行；671 条 Agent 测试、Android/Web/iOS 业务测试和日报功能继续暂停。

### 第二阶段新增证据（AG-P-021，采集器分类顺序修复后）

`AG-P-021` 前一轮 727-734 号证据的 5 条正式请求均已收到完整 SSE 和 audit，但采集器在生成 `stream_validation` 前先计算 `failure_class`，所以完成请求被误标为 `collector_error`。已在 `testing/Agent/脚本/性能/20260909_layer1_concurrency_collector.py` 调整执行顺序；没有修改业务源码、部署配置或线上服务。语法验证通过。

修复后的本轮仍使用同一实际组合：镜像 `sxyq27-zhj-api:20260914T0845-agent-negated-write-intent`，模型 `qwen3.8-flash`，Provider `https://oneapi.sxyq27.online/v1`，Wire API `chat_completions`，Agent SSE `https://zhj-api.sxyq27.online/v2/agent/chat/stream`，账号标签 `suffix-2002`。执行 1 个预热请求和 5 个正式请求，正式请求 5/5 完成，P50/P95/P99/最大完成耗时为 `12411.05/30537.36/33860.71/34691.55 ms`；首事件、首工具、首回答 P50 为 `288.50/5706.11/12402.08 ms`。

5 条正式请求均真实调用 `product_catalog_lookup`，SSE 事件类型、ID、seq 和 audit 列表一致，计数一致，单一终态，身份未混合；工具错误、Agent 429、5xx、超时、SSE 丢失/重复均为 0。5/5 cleanup 返回 HTTP 200，清理后 active runs 为 0。真实 Token usage、Token/s、JVM heap-used 和连接池等待仍为 `unavailable`。

本轮数据库计数为 `agent_conversations/agent_messages/agent_drafts/agent_run_audits/agent_run_audit_events/products/customers/finance_records`：`22/71/0/1635/14337/693/84/2661` → `22/71/0/1641/14407/693/84/2661`。只有 Agent 审计记录增加，products、customers、finance_records 未变化。资源采样取得 API、PostgreSQL、Redis、JVM RSS/线程、主机 swap；详细数值见 `740-phase2-ag-p021-pagination-small-rerun-20260914-resource-samples.jsonl`。

本轮证据均保留在原目录：`735-phase2-ag-p021-pagination-small-rerun-20260914-collector-format-validation.json`、`736-phase2-ag-p021-pagination-small-rerun-20260914-db-before.json`、`737-phase2-ag-p021-pagination-small-rerun-20260914-warmup.jsonl`、`738-phase2-ag-p021-pagination-small-rerun-20260914-concurrency-1.jsonl`、`739-phase2-ag-p021-pagination-small-rerun-20260914-failure-analysis.json`、`740-phase2-ag-p021-pagination-small-rerun-20260914-resource-samples.jsonl`、`741-phase2-ag-p021-pagination-small-rerun-20260914-summary.json`、`742-phase2-ag-p021-pagination-small-rerun-20260914-db-after.json`、`743-phase2-ag-p021-pagination-small-rerun-conclusion-20260914.md`。

`AG-P-021` 当前为 `Passed-small-sample`，不能替代小/中/大数据量各 20 条完整专项。采集器分类顺序问题已处理；剩余分页专项仍需补足样本并取得 SQL 数量、每条 SQL 耗时、连接池等待和查询计划等数据。

### 第二阶段新增证据（AG-P-009，长会话上下文短会话小测，2026-09-14）

本轮继续使用同一实际组合：镜像 `sxyq27-zhj-api:20260914T0845-agent-negated-write-intent`，模型 `qwen3.8-flash`，Provider `https://oneapi.sxyq27.online/v1`，Wire API `chat_completions`，Agent SSE `https://zhj-api.sxyq27.online/v2/agent/chat/stream`，账号标签 `suffix-2002`。固定会话包含 3 轮真实 Agent 输入：查询商品基本信息、基于上一轮补充库存摘要、总结上一轮结果；每条会话均在完成 audit 查询后清理。凭据只从本机受保护存储读取，证据没有保存凭据、请求体或回答正文。

- 1 个预热会话和 3 个正式会话均通过真实云端 Agent SSE、真实工具和云端 PostgreSQL 链路；正式请求 HTTP 200 为 3/3。
- 3 条正式会话中有 2 条终态为 `COMPLETED`，但均包含 `inventory_panorama_lookup` 的 `TOOL_QUERY_FAILED` 后继续完成，按“降级完成”记录，不能计入干净完成；另 1 条终态为 `FAILED`，错误为 `LLM_ANSWER_UNAVAILABLE`，audit 的 `llm_status=model_empty_or_ungrounded`，没有工具执行。严格有效完成为 0/3，完成耗时百分位、首工具和首回答百分位均为 `unavailable`；原始总耗时为 `98132.09`、`144896.27`、`202030.92 ms`，仅作为失败样本诊断信息。
- 3 条会话合计 9 个 turn，SSE 事件与 audit 事件逐项一致，seq 连续、事件 ID 无重复、每个 turn 只有一个终态、身份未混合；SSE/audit 不一致为 0。工具失败事件共 3 个，Agent 429、HTTP 5xx 和采集超时为 0，cleanup 为 3/3 HTTP 200，清理后 `active_runs=0`。
- 数据库计数顺序为 `agent_conversations/agent_messages/agent_drafts/agent_run_audits/agent_run_audit_events/products/customers/finance_records`：`22/71/0/1641/14407/693/84/2661` → `22/71/0/1653/14522/693/84/2661`。只有 Agent 审计记录增加，业务表没有变化。
- 资源采样取得 API、PostgreSQL、Redis、JVM RSS/线程和主机 swap。正式期间 API 约 `444.4 MiB`、PostgreSQL 约 `57.49 MiB`、Redis 约 `17.78 MiB`，JVM RSS `472004 kB`、线程 `45`，主机 swap 为 `0`；JVM heap-used、连接池等待、Provider 等待、真实 Token usage 和 Token/s 仍为 `unavailable`。
- `TOOL_QUERY_FAILED` 的具体数据库/工具内部原因和 `LLM_ANSWER_UNAVAILABLE` 的上游请求级原因，当前证据均未确认；本轮没有修改业务源码、测试脚本或部署，也没有把模型/Provider 直连结果计入 Agent 样本。

本轮完整证据为 `745-phase2-ag-p009-context-short-small-20260914-collector-format-validation.json`、`746-phase2-ag-p009-context-short-small-20260914-db-before.json`、`747-phase2-ag-p009-context-short-small-20260914-warmup.jsonl`、`748-phase2-ag-p009-context-short-small-20260914-formal.jsonl`、`749-phase2-ag-p009-context-short-small-20260914-failure-analysis.json`、`750-phase2-ag-p009-context-short-small-20260914-resource-samples.jsonl`、`751-phase2-ag-p009-context-short-small-20260914-summary.json`、`752-phase2-ag-p009-context-short-small-20260914-db-after.json`。这是 AG-P-009 的小规模真实样本，不能替代计划要求的完整样本，也不能写成 Passed。

### 第二阶段新增证据（AG-P-013，同一会话并发小测，2026-09-14）

本轮仍使用镜像 `sxyq27-zhj-api:20260914T0845-agent-negated-write-intent`、模型 `qwen3.8-flash`、Provider `https://oneapi.sxyq27.online/v1`、Wire API `chat_completions`、Agent SSE `https://zhj-api.sxyq27.online/v2/agent/chat/stream` 和账号标签 `suffix-2002`。先执行 1 个独立预热请求，再创建一个共享会话，将 5 个正式请求并发提交到同一 `conversation_id`；每条正式记录即时 flush/fsync，全部 audit 查询完成后再清理共享会话。

- 5 条正式请求 HTTP 200 为 5/5，终态 `COMPLETED` 为 5/5，严格有效完成为 5/5。P50/P95/P99/最大耗时为 `62858.08/104332.96/109250.05/110479.32 ms`；首事件/首工具/首回答 P50 为 `296.57/20736.31/62843.25 ms`。真实 Token usage、Token/s、Provider request ID 和请求级 Provider 等待仍为 `unavailable`。
- 5 条请求均真实调用 `product_catalog_lookup`，工具失败、Agent 429、HTTP 5xx、超时均为 0。每条流只有一个 `run_completed`，SSE 与 audit 的事件类型、事件 ID、seq 和数量一致，身份未混合；共享会话清理 HTTP 200，清理后 `active_runs=0`。
- 数据库计数顺序为 `agent_conversations/agent_messages/agent_drafts/agent_run_audits/agent_run_audit_events/products/customers/finance_records`：`22/71/0/1653/14522/693/84/2661` → `22/71/0/1659/14573/693/84/2661`。只有 Agent 审计记录增加，业务表没有变化。
- 资源采样 SSH 全部成功；正式期间 API 约 `445.9 MiB`、PostgreSQL 约 `59.85 MiB`、Redis 约 `17.78 MiB`，JVM RSS `470948 kB`、线程 `43`，主机 swap 为 `0`。JVM heap-used、连接池等待和客户端指标为 `unavailable`。
- 采集器此前在共享会话模式下没有保留最终共享清理返回值，已在 `testing/Agent/脚本/性能/20260909_layer1_concurrency_collector.py` 做最小测试脚本改动；本轮汇总记录 `cleanup_target_count=1`、`per_request_cleanup_deferred=true`、`shared_cleanup_status=200`。没有修改业务源码、部署配置或线上镜像。

本轮完整证据为 `753-phase2-ag-p013-shared-c5-small-20260914-collector-format-validation.json`、`754-phase2-ag-p013-shared-c5-small-20260914-db-before.json`、`755-phase2-ag-p013-shared-c5-small-20260914-warmup.jsonl`、`756-phase2-ag-p013-shared-c5-small-20260914-formal.jsonl`、`757-phase2-ag-p013-shared-c5-small-20260914-failure-analysis.json`、`758-phase2-ag-p013-shared-c5-small-20260914-resource-samples.jsonl`、`759-phase2-ag-p013-shared-c5-small-20260914-summary.json`、`760-phase2-ag-p013-shared-c5-small-20260914-db-after.json`、`761-phase2-ag-p013-shared-c5-small-20260914-conclusion.md`。这是并发 5 的小规模真实样本，不能替代计划要求的并发 2/5/10 各 20 组完整数据。


### 第二阶段新增证据（AG-P-016、AG-P-017、AG-P-010，当前审计序号镜像）

本轮继续使用唯一实际组合：镜像 `sxyq27-zhj-api:20260914T1225-agent-audit-seq-rollback`，模型 `qwen3.8-flash`，Provider `https://oneapi.sxyq27.online/v1`，Wire API `chat_completions`，Agent SSE `https://zhj-api.sxyq27.online/v2/agent/chat/stream`，账号标签 `suffix-2002`。凭据仅从本机受保护存储读取，未进入报告、台账、日志或证据。

- `AG-P-016`：762–801 共 20 条正式请求，首事件前取消 4/4、工具执行中取消 4/4；回答中、长回答中和完成后共 12 条已进入后续状态，取消返回 `not_found` 或正常 no-op。20/20 cleanup 成功，SSE/audit 一致，业务表未变化。总体记为 `Failed-small-sample`，原因是后段取消未能中止已经产生的回答阶段；完整计划样本尚未补足。独立结论见 `828-phase2-ag-p016-cancel-conclusion-20260914.md`。

- `AG-P-017`：803–827 共 12 条正式断线请求。当前镜像的审计序号回收已使 `event_count` 与 `emitted_event_count` 一致，清理成功；但 Agent SSE 没有协议层 `id:`、没有读取 `Last-Event-ID`，断线后没有事件补发，最终观测到 `STREAM_ERROR`。重连指标记为 `Deferred`，专项当前为 `Failed-small-sample`，不能写成恢复通过。独立结论见 `829-phase2-ag-p017-disconnect-conclusion-20260914.md`。

- `AG-P-010`：828–835 的 30 轮配置只实际完成到第 2 轮；第 1 轮真实调用 `product_catalog_lookup` 完成，第 2 轮真实调用 `inventory_panorama_lookup` 后返回 `TOOL_QUERY_FAILED` 和 `AGENT_TOOL_EXECUTION_FAILED/EXHAUSTED`。Provider 请求为 2xx，未到达上下文压缩边界；正式会话 cleanup 成功、`active_runs=0`、业务表未变化。专项记为 `Failed`，压缩触发率、压缩耗时和 checkpoint 数据为 `unavailable`。独立结论见 `836-phase2-ag-p010-context-compaction-conclusion-20260914.md`。

以上三项证据均来自真实云端 Agent、真实工具、云端 PostgreSQL 和 SSE，失败样本保留，未把 Provider 直连结果计入。第二阶段仍未完成，671 条 Agent 测试、Android/Web/iOS 业务测试和日报功能继续暂停。

### 第二阶段新增证据（AG-P-010 修复后 30 轮，2026-09-14）

本轮使用独立的最新线上组合：镜像 `sxyq27-zhj-api:20260914T1749-inventory-empty-panorama-fix`，模型 `qwen3.8-flash`，Provider `https://oneapi.sxyq27.online/v1`，Wire API `chat_completions`，Agent SSE `https://zhj-api.sxyq27.online/v2/agent/chat/stream`，账号标签 `suffix-2002`。该组合与 828–836 的旧镜像波次分开统计，没有合并 DeepSeek、其他 Qwen 镜像或其他测试输入。

- 已部署的最小源码改动位于 `Code/backend/src/main/java/com/zhihuiji/backend/application/service/v2/agent/tool/readonly/InventoryPanoramaLookupTool.java`：空商品结果不再对不可变空列表排序；回归测试为 `Code/backend/src/test/java/com/zhihuiji/backend/application/service/v2/agent/tool/readonly/InventoryPanoramaLookupToolTest.java`。本轮复测没有新增源码、采集器或部署配置改动。
- 正式会话完成 `30/30` 轮，最终终态为 `COMPLETED`，会话耗时 `1235019.89 ms`。第 23–30 轮各出现 1 个 `context_compacted` 事件，共 8 次；正式会话调用了 `product_catalog_lookup` 与 `inventory_panorama_lookup`，没有工具失败、`TOOL_ARGUMENTS_INVALID`、`LLM_ANSWER_UNAVAILABLE`、Agent 429、HTTP 5xx 或采集器错误。
- 正式会话 30 个 turn 均只有一个合法终态；SSE 事件 ID 无重复、seq 连续、身份未混合；SSE 与 audit 的事件类型、ID、seq、事件数量和 `event_count/emitted_event_count` 一致。cleanup 为 HTTP 200，清理后 `active_runs=0`。
- 预热会话推进到第 20/30 轮时出现 `LLM_ANSWER_UNAVAILABLE`，audit 为 `model_empty_or_ungrounded`。该会话不是正式样本。失败 run `7f453bdd-19a1-4ebb-80ba-5e0b717b1b3c` 的 8220 日志显示相关 Provider 请求均为 `2xx`，随后出现 `untrusted_numeric_value`；Provider request ID、upstream request ID 和 Retry-After 未取得，故该异常单独归为模型结果不可用的外部信号。
- 正式会话级 P50/P95/P99/最大耗时在单个正式会话样本下均为 `1235019.89 ms`，只作本次会话描述。按请求 Token usage、Token/s、JVM heap-used、连接池等待、SQL 数量、单条数据库耗时、查询计划和客户端指标仍为 `unavailable`。
- 数据库计数顺序为 `agent_conversations/agent_messages/agent_drafts/agent_run_audits/agent_run_audit_events/products/customers/finance_records`：`22/71/0/1709/15004/693/84/2661` → `22/71/0/1759/15544/693/84/2661`。增加的 50 条 audit 对应预热 20 轮与正式 30 轮；`products`、`customers`、`finance_records` 和 `agent_drafts` 未变化。
- 正式期间资源为：API 容器约 `415.2 MiB`、PostgreSQL 约 `82.10 MiB`、Redis 约 `19.43 MiB`、JVM RSS `431232 kB`、线程 `45`、主机可用内存 `645242880` bytes、swap `0`。清理后 API 容器约 `413.4 MiB`、PostgreSQL 约 `84.71 MiB`、Redis 约 `19.36 MiB`、JVM RSS `439828 kB`、线程 `43`、主机可用内存 `615727104` bytes、swap `0`。
- 当前 `AG-P-010` 记为 `Passed-small-sample`：已有 1 个正式 30 轮会话并确认触发压缩，但计划要求的 10 个会话、压缩耗时分布和 checkpoint 保存/复用数据尚未补足。预热中的模型结果不可用不指向本轮源码改动。

本轮证据为 `845-phase2-ag-p010-context-empty-fix-30turn-20260914-collector-format-validation.json`、`846-phase2-ag-p010-context-empty-fix-30turn-20260914-db-before.json`、`847-phase2-ag-p010-context-empty-fix-30turn-20260914-warmup.jsonl`、`848-phase2-ag-p010-context-empty-fix-30turn-20260914-formal.jsonl`、`849-phase2-ag-p010-context-empty-fix-30turn-20260914-failure-analysis.json`、`850-phase2-ag-p010-context-empty-fix-30turn-20260914-resource-samples.jsonl`、`851-phase2-ag-p010-context-empty-fix-30turn-20260914-summary.json`、`852-phase2-ag-p010-context-empty-fix-30turn-20260914-db-after.json`、`853-phase2-ag-p010-context-empty-fix-runtime-deployment-20260914.json`、`854-phase2-ag-p010-context-empty-fix-30turn-conclusion-20260914.md`。

第二阶段整体仍未完成；`AG-P-004`、`AG-P-016`、`AG-P-017` 等已有失败或样本不足项仍需按各自结论处理，其余专项继续使用同一实际组合逐项执行。671 条 Agent 测试、Android/Web/iOS 业务测试和日报功能继续暂停。
### 第二阶段新增证据（AG-P-010 重试，2026-09-14）

本轮继续使用同一正式环境：镜像 `sxyq27-zhj-api:20260914T1749-inventory-empty-panorama-fix`，模型 `qwen3.8-flash`，Provider `https://oneapi.sxyq27.online/v1`，Wire API `chat_completions`，Agent SSE `https://zhj-api.sxyq27.online/v2/agent/chat/stream`，账号标签 `suffix-2002`。8220 容器只读探针再次确认镜像为 running、启动时间为 `2026-09-14T09:52:07.72882233Z`、重启次数为 `0`；环境变量确认模型、Provider 和 Wire API 与上述组合一致。凭据仍只从本机受保护存储读取。

- 预热会话完成 30/30 轮，触发 `context_compacted` 7 次；预热不计入正式样本。
- 正式配置为 1 个 30 轮会话。HTTP 200 为 1/1，但在第 5 轮终态为 `LLM_ANSWER_UNAVAILABLE`，严格完成为 0/1；有效 P50/P95/P99/最大耗时均为 `unavailable`，失败请求诊断耗时为 `116834.77 ms`，不用于成功性能百分位。
- 第 1、2、4 轮完成；第 3 轮模型选择 `account_balance_lookup` 并产生 `TOOL_ARGUMENTS_INVALID`，随后调用 `product_catalog_lookup` 后完成，属于工具参数错误后的降级完成；第 5 轮为 `LLM_ANSWER_UNAVAILABLE`，audit 的 `llm_status` 为 `model_empty_or_ungrounded`，没有工具事件。
- 正式 5 个 turn 共 48 个 SSE/audit 事件。事件类型、事件 ID、seq 和数量逐项一致，seq 连续、事件 ID 无重复、每个 turn 只有一个终态、身份未混合。清理 HTTP 200，清理后 `active_runs=0`。
- 采集器没有取得 Provider request ID、upstream request ID 或 Retry-After；现有时间窗口未见 402/429/503。无法把该失败 run 与单个 Provider 请求唯一对应，因此不把 Provider 错误写成唯一原因。当前证据归类为模型规划参数异常和模型结果不可用，未定位到业务源码、SSE、审计或数据库路径的新问题。

数据库计数顺序为 `agent_conversations/agent_messages/agent_drafts/agent_run_audits/agent_run_audit_events/products/customers/finance_records`：`24/101/0/1779/15722/693/84/2661` → `24/101/0/1814/16091/693/84/2661`。只增加 Agent 审计记录，`products`、`customers`、`finance_records` 和 `agent_drafts` 未变化。

资源文件 `865-phase2-ag-p010-context-empty-fix-30turn-r02-retry-20260914-resource-samples.jsonl` 显示：API 容器 `415.8–421.2 MiB`，PostgreSQL `88.27–96.95 MiB`，Redis `19.36–19.61 MiB`，JVM RSS `442280–447380 kB`、线程 `40–45`，主机可用内存约 `633–642 MiB`，swap 使用 `0`；JVM heap-used、连接池等待、Provider 按请求等待、客户端资源和按请求 Token/s 仍为 `unavailable`。

本轮没有修改业务源码、测试脚本或部署配置，也没有重新部署。845–854 的 30 轮成功会话继续作为独立 `Passed-small-sample` 保留，不能与本轮失败拼成一次通过结果。860–867 本轮记为 `Failed-small-sample`；AG-P-010 仍未达到 `TEST_PLAN.md` 要求的 10 个会话、每个至少 30 轮、至少 3 个完整轮次，压缩耗时分布和 checkpoint 保存/复用数据仍不足。第二阶段尚未完成，671 条 Agent 测试、Android/Web/iOS 测试和日报功能继续暂停。

本轮证据：`860-phase2-ag-p010-context-empty-fix-30turn-r02-retry-20260914-collector-format-validation.json`、`861-phase2-ag-p010-context-empty-fix-30turn-r02-retry-20260914-db-before.json`、`862-phase2-ag-p010-context-empty-fix-30turn-r02-retry-20260914-warmup.jsonl`、`863-phase2-ag-p010-context-empty-fix-30turn-r02-retry-20260914-formal.jsonl`、`864-phase2-ag-p010-context-empty-fix-30turn-r02-retry-20260914-failure-analysis.json`、`865-phase2-ag-p010-context-empty-fix-30turn-r02-retry-20260914-resource-samples.jsonl`、`866-phase2-ag-p010-context-empty-fix-30turn-r02-retry-20260914-summary.json`、`867-phase2-ag-p010-context-empty-fix-30turn-r02-retry-20260914-db-after.json`、`868-phase2-ag-p010-context-empty-fix-r02-retry-conclusion-20260914.md`。
### 第二阶段新增证据（AG-P-010 固定提示词序列补充波次，2026-09-14）

由于 860–867 波次没有保存用户消息正文，本轮没有猜测或复用无法确认的输入。新波次使用同一线上环境和一组单独记录的五轮只读提示词序列：商品基本信息、上一轮商品库存状态、上一轮商品价格和状态、上一轮结果核对、商品/库存/价格/状态汇总。环境仍为镜像 `sxyq27-zhj-api:20260914T1749-inventory-empty-panorama-fix`、模型 `qwen3.8-flash`、Provider `https://oneapi.sxyq27.online/v1`、Wire API `chat_completions`、Agent SSE `https://zhj-api.sxyq27.online/v2/agent/chat/stream`、账号标签 `suffix-2002`。8220 只读容器探针确认启动时间 `2026-09-14T09:52:07.72882233Z`、状态 `running`、重启次数 `0`。

- 预热会话 30/30 轮完成，触发 `context_compacted` 5 次；预热不计入正式样本。
- 正式会话 30/30 轮完成，HTTP 200，终态 `COMPLETED`，严格有效完成 `1/1`。会话耗时 `732795.60 ms`；单个正式会话样本下 P50/P95/P99/最大均为 `732795.60 ms`。首事件、首工具、首回答分别为 `294.95/40215.28/46212.36 ms`。
- 正式会话在第 26–30 轮各触发 1 次上下文压缩，共 5 次；30 个 turn 均继续完成。真实调用了 `product_catalog_lookup` 和 `inventory_panorama_lookup`，工具错误、`TOOL_ARGUMENTS_INVALID`、`LLM_ANSWER_UNAVAILABLE`、Agent 429、HTTP 5xx、超时和采集器错误均为 `0`。
- 正式 30 个 turn 共 321 个 SSE 事件和 321 个 audit 事件；`event_count=321`、`emitted_event_count=321`。类型、ID、seq 和数量一致，seq 无缺口，事件 ID 无重复，每个 turn 只有一个合法终态，身份混合为 `0`。cleanup HTTP 200，清理后 `active_runs=0`。

数据库计数顺序为 `agent_conversations/agent_messages/agent_drafts/agent_run_audits/agent_run_audit_events/products/customers/finance_records`：`24/101/0/1814/16091/693/84/2661` → `24/101/0/1874/16742/693/84/2661`。只增加 Agent 审计记录，`agent_drafts`、`products`、`customers`、`finance_records` 未变化。

资源文件 `874-phase2-ag-p010-context-fixed-sequence-r03-20260914-resource-samples.jsonl` 显示：API 容器 `421.2–432.7 MiB`，PostgreSQL `67.1–73.14 MiB`，Redis `19.36 MiB`，JVM RSS `447448–459180 kB`、线程 `40–43`，主机可用内存约 `597–629 MiB`，swap 使用 `0`，Redis blocked clients 为 `0`。JVM heap-used、连接池等待、Provider 按请求等待、客户端资源和按请求 Token/s 仍为 `unavailable`。

本波次没有新增源码、测试脚本、部署配置或线上部署动作；成功样本与 845–854、860–868 的其他波次分开保存，未拼接成更大的成功结论。`AG-P-010` 仍未满足 `TEST_PLAN.md` 要求的 `10 会话 × 至少 30 轮，至少 3 个完整轮次`，压缩耗时分布、确定性/语义比例和 checkpoint 保存/复用数据仍不足。当前波次为 `Passed-small-sample`；第二阶段尚未完成，671 条 Agent 测试、Android/Web/iOS 测试和日报功能继续暂停。

本轮证据：`869-phase2-ag-p010-context-fixed-sequence-r03-20260914-collector-format-validation.json`、`870-phase2-ag-p010-context-fixed-sequence-r03-20260914-db-before.json`、`871-phase2-ag-p010-context-fixed-sequence-r03-20260914-warmup.jsonl`、`872-phase2-ag-p010-context-fixed-sequence-r03-20260914-formal.jsonl`、`873-phase2-ag-p010-context-fixed-sequence-r03-20260914-failure-analysis.json`、`874-phase2-ag-p010-context-fixed-sequence-r03-20260914-resource-samples.jsonl`、`875-phase2-ag-p010-context-fixed-sequence-r03-20260914-summary.json`、`876-phase2-ag-p010-context-fixed-sequence-r03-20260914-db-after.json`、`877-phase2-ag-p010-context-fixed-sequence-r03-conclusion-20260914.md`。

### 第二阶段新增证据（AG-P-010 固定提示词序列 R04，2026-09-14）

本波次继续使用唯一线上组合：镜像 `sxyq27-zhj-api:20260914T1749-inventory-empty-panorama-fix`、模型 `qwen3.8-flash`、Provider `https://oneapi.sxyq27.online/v1`、Wire API `chat_completions`、Agent SSE `https://zhj-api.sxyq27.online/v2/agent/chat/stream`、账号标签 `suffix-2002`。8220 只读容器再次核实状态为 `running`，启动时间为 `2026-09-14T09:52:07.72882233Z`，重启次数为 `0`。本波次使用 1 个预热会话和 8 个正式会话，每个会话最多 30 轮，并发为 1；五类只读提示词序列与 R03 相同。凭据只从本机受保护存储读取。

- 正式结果为：HTTP 200 `8/8`，严格有效完成 `3/8`，终态为 `COMPLETED` `4/8`，Agent 失败 `3/8`，采集器失败 `2/8`。请求 0、1、2 分别完成 30/30 轮并触发 5、7、6 次 `context_compacted`。
- 仅 3 条干净完成计算的 P50/P95/P99/最大耗时为 `1273318.95/1277625.05/1278007.82/1278103.51 ms`；首事件、首工具、首回答 P50 为 `302.32/57896.68/67586.36 ms`。Token usage、Token/s、JVM heap-used、连接池等待和按请求 Provider 等待仍为 `unavailable`。
- 请求 3、6、7 在第 5、6、7 轮分别以 `LLM_ANSWER_UNAVAILABLE` 结束。请求 4 完成 30/30 轮但第 4 轮 SSE 只收到 `run_started`，对应 audit 有 10 个完整事件；请求 5 在第 28/30 轮结束，SSE 与 audit 各有 5 个事件但该轮仍为 `running`。这两条按采集器失败处理，未计入成功。
- 3 条干净完成会话的 90 个 turn 均有唯一合法终态，事件 ID 无重复，seq 连续，SSE 与 audit 的事件类型、ID、seq 和数量一致。全波次工具失败事件、`TOOL_ARGUMENTS_INVALID`、Agent 429、HTTP 5xx、超时和身份混合均为 `0`；正式 cleanup `8/8` 返回 HTTP 200。
- 8220 API 日志中，请求 4 的 `tool_planning` 与 `tool_continuation` 均为 2xx 且有响应；请求 5 的两次 Provider 请求也为 2xx，但排队等待为 `41702 ms` 和 `68125 ms`；请求 6 出现 `ResourceAccessException`。日志取得了 Agent 侧 `client_request_id`，没有取得 Provider request ID、upstream request ID 或 `Retry-After`，也没有确认请求 4 SSE 缺口的具体服务端来源。
- 数据库计数顺序为 `agent_conversations/agent_messages/agent_drafts/agent_run_audits/agent_run_audit_events/products/customers/finance_records`：`24/101/0/1874/16742/693/84/2661` → `24/101/0/2070/18830/693/84/2661`。只有 Agent 审计相关记录增加，`agent_conversations`、`agent_messages`、`agent_drafts`、`products`、`customers`、`finance_records` 未发生非预期变化；资源采样结束后 `active_runs=0`。
- 资源快照显示 API 容器 `432.5–441.6 MiB`、PostgreSQL `71.79–73.27 MiB`、Redis `19.36 MiB`、JVM RSS `459180–464116 kB`、线程 `40–44`、主机可用内存 `582615040–607350784 bytes`、swap `0`、Redis blocked clients `0`。这些是阶段快照，不能代表完整波峰。
- 本波次没有修改业务源码、测试脚本或部署配置，也没有重新部署。`LLM_ANSWER_UNAVAILABLE` 和 Provider 访问异常作为外部依赖诊断保留，没有据此修改业务逻辑；请求 4 的 SSE/audit 差异仍需单独定位。

本波次状态为 `Failed-small-sample`。AG-P-010 尚未达到 `TEST_PLAN.md` 的 `10 会话 × 至少 30 轮，至少 3 个完整轮次` 要求；压缩耗时分布、确定性/语义比例和 checkpoint 保存/复用数据仍不完整。第二阶段尚未完成，671 条 Agent 测试、Android/Web/iOS 测试和日报功能继续暂停。

本波次完整证据为 `878-phase2-ag-p010-context-fixed-sequence-r04-20260914-collector-format-validation.json`、`879-phase2-ag-p010-context-fixed-sequence-r04-20260914-db-before.json`、`880-phase2-ag-p010-context-fixed-sequence-r04-20260914-warmup.jsonl`、`881-phase2-ag-p010-context-fixed-sequence-r04-20260914-formal.jsonl`、`882-phase2-ag-p010-context-fixed-sequence-r04-20260914-failure-analysis.json`、`883-phase2-ag-p010-context-fixed-sequence-r04-20260914-resource-samples.jsonl`、`884-phase2-ag-p010-context-fixed-sequence-r04-20260914-summary.json`、`885-phase2-ag-p010-context-fixed-sequence-r04-20260914-db-after.json`、`886-phase2-ag-p010-context-fixed-sequence-r04-conclusion-20260914.md`。

### 第二阶段新增证据（AG-P-010 SSE 生命周期处理后固定提示词序列 R05，2026-09-15）

本波次使用唯一线上组合：镜像 `sxyq27-zhj-api:20260915T0002-agent-sse-timeout-300s`、模型 `qwen3.8-flash`、Provider `https://oneapi.sxyq27.online/v1`、Wire API `chat_completions`、Agent SSE `https://zhj-api.sxyq27.online/v2/agent/chat/stream`、账号标签 `suffix-2002`。8220 只读核实容器状态为 `running`，启动时间为 `2026-09-14T16:09:19.495233362Z`，健康接口 HTTP 200。波次为 1 个预热会话和 5 个正式会话，每个最多 30 轮，并发 1，五类只读提示词与 R03/R04 独立保留且一致。

- 正式结果：HTTP 200 `5/5`，严格有效完成 `2/5`，终态 `COMPLETED` `3/5`；请求 0 为 `LLM_ANSWER_UNAVAILABLE`，请求 2 为 SSE 观测不完整，请求 3 为 `AGENT_TOOL_EXECUTION_FAILED` / `TOOL_ARGUMENTS_INVALID`。严格完成请求的 P50/P95/P99/最大耗时为 `668359.59/677169.32/677952.41/678148.18 ms`；首事件、首工具、首回答 P50 为 `295.23/34494.17/40319.87 ms`。
- 两条严格完成会话各触发 `6` 次 `context_compacted`；请求 2 也触发 `6` 次，但因第 25 轮 SSE 只有 `run_started` 1 项而 audit 有 14 项，未计入严格成功。上下文压缩耗时、checkpoint 保存/复用和 Token/s 仍为 `unavailable`。
- 工具失败事件 `1`、SSE/audit 不一致会话 `1`、身份混合 `0`、Agent 429 `0`、HTTP 5xx `0`、超时 `0`；正式 cleanup `5/5` 返回 HTTP 200，结束后 `active_runs=0`。已完整观测的 turn 中 SSE/audit 类型、ID、seq 和计数一致；请求 2 的提前结束仍未取得明确网络或 Provider 根因。
- `product_catalog_lookup` schema 当前明确要求 `status` 为整数枚举 `0/1`；请求 3 的原始模型参数未保存，现有证据无法确认是模型输出、Provider 适配还是业务转换导致，因此本波次没有新增源码改动。
- 8220 日志已按失败 run、conversation、audit、trace 查询：目标 Provider 操作均记录为 `2xx`，取得了 Agent `run_id` 到 API `client_request_id` 的映射；Provider request ID、upstream request ID 和 `Retry-After` 未取得。详见 `897-phase2-ag-p010-context-fixed-sequence-r05-provider-log-correlation-20260915.json`，不能把时间窗结果写成单一 Provider 根因。
- 数据库计数顺序为 `agent_conversations/agent_messages/agent_drafts/agent_run_audits/agent_run_audit_events/products/customers/finance_records`：`24/101/0/2070/18830/693/84/2661` → `24/101/0/2202/20246/693/84/2661`。仅 Agent 审计记录增加，业务表未变化。
- 资源采样显示 API 容器 `347.9–419.6 MiB`、PostgreSQL `70.16–89.97 MiB`、Redis `7.371–20.5 MiB`、JVM RSS `378824–447980 kB`、线程 `33–44`、主机可用内存 `613429248–702967808 bytes`、swap `0`、Redis blocked clients `0`；JVM heap-used、连接池等待、客户端资源和按请求 Token/s 仍为 `unavailable`。
- R04 暴露的 `180000 ms` SSE 生命周期问题已处理为 `300000 ms`，定向测试和 `bootJar` 已通过；R05 使用新镜像 `sxyq27-zhj-api:20260915T0002-agent-sse-timeout-300s`，PostgreSQL 和 Redis 未重建。R05 仍为 `Failed-small-sample`，不能写成 AG-P-010 通过。

本波次完整证据为 `887-phase2-ag-p010-context-fixed-sequence-r05-sse-timeout-fix-20260915-collector-format-validation.json`、`888-phase2-ag-p010-context-fixed-sequence-r05-sse-timeout-fix-20260915-db-before.json`、`889-phase2-ag-p010-context-fixed-sequence-r05-sse-timeout-fix-20260915-warmup.jsonl`、`890-phase2-ag-p010-context-fixed-sequence-r05-sse-timeout-fix-20260915-formal.jsonl`、`891-phase2-ag-p010-context-fixed-sequence-r05-sse-timeout-fix-20260915-failure-analysis.json`、`892-phase2-ag-p010-context-fixed-sequence-r05-sse-timeout-fix-20260915-resource-samples.jsonl`、`893-phase2-ag-p010-context-fixed-sequence-r05-sse-timeout-fix-20260915-summary.json`、`894-phase2-ag-p010-context-fixed-sequence-r05-sse-timeout-fix-20260915-db-after.json`、`895-phase2-ag-p010-context-fixed-sequence-r05-runtime-deployment-20260915.json`、`896-phase2-ag-p010-context-fixed-sequence-r05-conclusion-20260915.md`、`897-phase2-ag-p010-context-fixed-sequence-r05-provider-log-correlation-20260915.json`。

第二阶段仍未完成；671 条 Agent 测试、Android/Web/iOS 测试和日报功能继续暂停。

### 第二阶段新增证据（AG-P-010 固定提示词序列 R06，2026-09-15）

本波次继续使用唯一线上组合：镜像 `sxyq27-zhj-api:20260915T0002-agent-sse-timeout-300s`、模型 `qwen3.8-flash`、Provider `https://oneapi.sxyq27.online/v1`、Wire API `chat_completions`、Agent SSE `https://zhj-api.sxyq27.online/v2/agent/chat/stream`、账号标签 `suffix-2002`。R06 由初次采集 `898–901` 和同环境补充采集 `902–909` 组成；初次采集包含 1 个预热和 5 个正式会话，补充采集不重复预热并增加 5 个正式会话。两次采集均为并发 1、每条正式会话最多 30 轮、固定五类只读提示词。

- 正式结果：10/10 会话均为 `30/30`、HTTP 200、`COMPLETED`，严格有效 `10/10`，cleanup `10/10` 返回 HTTP 200；`failure_class`、`collector_error`、工具失败、`TOOL_ARGUMENTS_INVALID`、`LLM_ANSWER_UNAVAILABLE`、Agent 429、HTTP 5xx 和超时均为 `0`。
- 300 个 turn 的 SSE/audit 事件类型、事件 ID、seq 和数量逐项一致，seq 连续、事件 ID 无重复、每轮终态唯一、身份混合为 `0`。共观察到 43 次 `context_compacted`，每条正式会话出现 2–7 次，触发轮次位于第 24–30 轮范围内。
- 10 条完整会话总耗时 P50/P95/P99/最大为 `453556.52/553187.47/564433.65/567245.20 ms`。该数值仅描述本波次，不能直接外推长期性能；压缩耗时、确定性/语义比例和 checkpoint 保存/复用仍为 `unavailable`。
- R06 资源证据完整范围仅来自补充采集 `907`：API 容器 `435.9–437 MiB`、PostgreSQL `76.27–84.73 MiB`、Redis `20.5 MiB`、主机可用内存 `593829888–612925440 bytes`、swap `0`。初次采集 5 条正式会话没有独立资源文件，不能把该范围写成整个波次的完整峰值。
- 数据库计数顺序为 `agent_conversations/agent_messages/agent_drafts/agent_run_audits/agent_run_audit_events/products/customers/finance_records`：`24/101/0/2202/20246/693/84/2661` → `25/121/0/2542/23812/693/84/2661`。仅 Agent 会话与审计记录增加，业务表未变化；采集结束后 `active_runs=0`。
- R06 当前波次记为 `Passed-small-sample`。R03、R04、R05 保持独立结果和失败记录，不与 R06 拼接成跨镜像分布。独立结论见 `910-phase2-ag-p010-context-fixed-sequence-r06-conclusion-20260915.md`。

本波次证据为 `898-phase2-ag-p010-context-fixed-sequence-r06-sse-timeout-fix-20260915-collector-format-validation.json`、`899-phase2-ag-p010-context-fixed-sequence-r06-sse-timeout-fix-20260915-db-before.json`、`900-phase2-ag-p010-context-fixed-sequence-r06-sse-timeout-fix-20260915-warmup.jsonl`、`901-phase2-ag-p010-context-fixed-sequence-r06-sse-timeout-fix-20260915-formal.jsonl`、`902-phase2-ag-p010-context-fixed-sequence-r06-continuation-20260915-collector-format-validation.json`、`903-phase2-ag-p010-context-fixed-sequence-r06-continuation-20260915-db-before.json`、`904-phase2-ag-p010-context-fixed-sequence-r06-continuation-20260915-warmup.jsonl`、`905-phase2-ag-p010-context-fixed-sequence-r06-continuation-20260915-formal.jsonl`、`906-phase2-ag-p010-context-fixed-sequence-r06-continuation-20260915-failure-analysis.json`、`907-phase2-ag-p010-context-fixed-sequence-r06-continuation-20260915-resource-samples.jsonl`、`908-phase2-ag-p010-context-fixed-sequence-r06-continuation-20260915-summary.json`、`909-phase2-ag-p010-context-fixed-sequence-r06-continuation-20260915-db-after.json`、`910-phase2-ag-p010-context-fixed-sequence-r06-conclusion-20260915.md`。

## 第二阶段新增证据（GLM 911–1008，2026-09-18）

### 证据序列与运行组合

在本轮新增采集开始前，性能证据目录的实际最大编号为 `991`，`967` 缺失；`911–991` 实际存在 80 个文件，`898–991` 实际存在 93 个文件。该范围按文件名分为 speed、两组 30 轮尝试、SSE/无工具/共享会话小波次和两个 AG-P-002 工具小波次。`929` 是空的 warmup 文件，`967` 没有找到对应文件；没有用人工内容补齐缺号。

`911–991` 的 JSON/JSONL 没有逐条保存 model、Provider、image 和 endpoint 字段，因此这些文件只能以文件名、关联文档和运行时复核进行归属，不能宣称每一条原始记录自带完整运行组合。新增 `1008-glm53flash-p001-no-tool-20260918-runtime-recheck.json` 保存了本轮只读运行复核：模型 `glm-5.3-flash`、Provider `https://oneapi.sxyq27.online/v1`、Wire API `chat_completions`、Agent 镜像 `sxyq27-zhj-api:20260915T0002-agent-sse-timeout-300s`；API、PostgreSQL、Redis 均为运行状态，PostgreSQL 健康状态为 `healthy`，公开 `/healthz` 返回 HTTP 200，API 容器重启次数为0。该复核支持当前组合归属，但不追溯替代旧文件缺少的逐波次启动元数据。

### 911–991 已有 GLM 证据

- `911–918` 为单轮速度探针，1 条预热、5 条正式，正式 5/5 完成；P50/P95/P99/最大完成耗时为 `14671.80/15514.47/15584.26/15601.71 ms`，正式 cleanup 5/5。
- `919–926` 和 `927–934` 为两次长会话尝试，均在第5轮结束，终态为 `LLM_ANSWER_UNAVAILABLE`，没有到达上下文压缩边界；`929` 的 warmup 文件为空，按缺少样本处理。
- `935–975` 共75条正式请求：SSE R01 为25/25完成，无工具 R01 为24/25完成，共享会话并发2为5/5完成，SSE R02为10/10完成，无工具 R02为9/10完成。两条失败样本保留为 Provider 错误类别，不能混入成功百分位。
- `976–983` 是 `product_catalog_lookup` 的10条 stream 请求，10/10完成；P50/P95/P99/最大为 `17416.42/20536.95/20587.57/20600.23 ms`。`984–991` 是 `inventory_panorama_lookup` 的10条 stream 请求，10/10完成；P50/P95/P99/最大为 `18798.65/30685.76/36214.62/37596.83 ms`。这20条只覆盖2个只读工具，不能替代46个工具各10条非流式和流式样本。
- 上述 GLM 文件的 `redaction_status` 均显示为脱敏完成；文件没有保存完整请求、完整回答或授权材料。工具小波次的数据库前后计数只显示 Agent 审计相关记录增加，`products`、`customers`、`finance_records` 未变化。

### AG-P-001 GLM stream 波次（992–999）

本波次使用已核实的 GLM 组合、并发1、1条预热和30条正式请求；输入为短的无工具说明请求，正式请求均通过真实 Agent SSE 入口发送。证据为 `992-glm53flash-p001-no-tool-r03-20260918-collector-format-validation.json`、`993-glm53flash-p001-no-tool-r03-20260918-db-before.json`、`994-glm53flash-p001-no-tool-r03-20260918-warmup.jsonl`、`995-glm53flash-p001-no-tool-r03-20260918-formal.jsonl`、`996-glm53flash-p001-no-tool-r03-20260918-failure-analysis.json`、`997-glm53flash-p001-no-tool-r03-20260918-resource-samples.jsonl`、`998-glm53flash-p001-no-tool-r03-20260918-summary.json`、`999-glm53flash-p001-no-tool-r03-20260918-db-after.json`。

- 正式30条全部 HTTP 200；严格有效完成27条，终态 `COMPLETED` 28条，2条为 `LLM_ANSWER_UNAVAILABLE`，另1条被采集器判为采集异常。没有工具调用、Agent 429、HTTP 5xx、超时或工具失败事件；总体 audit/SSE 不一致计数为1，身份混合为0。
- 严格有效样本的完成耗时 P50/P95/P99/最大为 `15668.52/19182.18/20641.90/21082.17 ms`；首事件 P50 为 `280.12 ms`，首回答 P50 为 `15661.21 ms`，首工具不适用。该分布只属于当前 GLM stream 小波次。
- 数据库计数为 `25/121/0/2668/24896/693/84/2661` → `25/121/0/2699/25020/693/84/2661`。仅 Agent 会话审计记录增加，业务表没有变化；30/30 cleanup 返回 HTTP 200。

### AG-P-001 GLM REST 波次（1000–1007）

本波次沿用同一 GLM 组合、并发1、1条预热和30条正式请求，使用 Agent REST `/v2/agent/chat`，不与 stream 分布合并。证据为 `1000-glm53flash-p001-no-tool-rest-r01-20260918-collector-format-validation.json`、`1001-glm53flash-p001-no-tool-rest-r01-20260918-db-before.json`、`1002-glm53flash-p001-no-tool-rest-r01-20260918-warmup.jsonl`、`1003-glm53flash-p001-no-tool-rest-r01-20260918-formal.jsonl`、`1004-glm53flash-p001-no-tool-rest-r01-20260918-failure-analysis.json`、`1005-glm53flash-p001-no-tool-rest-r01-20260918-resource-samples.jsonl`、`1006-glm53flash-p001-no-tool-rest-r01-20260918-summary.json`、`1007-glm53flash-p001-no-tool-rest-r01-20260918-db-after.json`。

- 正式30条全部 HTTP 200、严格有效完成30/30；终态失败、采集异常、工具调用、Agent 429、HTTP 5xx、超时和 REST/audit 不一致均为0。REST 没有 SSE 事件，audit 观察与 REST 终态、身份和工具列表一致；30/30 cleanup 返回 HTTP 200。
- 完成耗时 P50/P95/P99/最大为 `15410.75/20427.99/36992.96/43241.33 ms`；响应字节合计 `42460`。首事件、首工具和首回答不适用于 REST 传输。
- 数据库计数为 `25/121/0/2699/25020/693/84/2661` → `25/121/0/2730/25051/693/84/2661`。只有 Agent 审计记录增加，业务表没有变化。

### 新 GLM 波次资源与最终状态

两个新波次的远端资源采样均为5条、SSH返回码均为0；采样显示 API 容器内存约 `427.6–455.2 MiB`、PostgreSQL `68.06–92.7 MiB`、Redis `3.398–19.59 MiB`，主机可用内存约 `197447680–274739200 bytes`，swap 使用量为0，API容器 PIDs `40–45`。正式期间 `active_runs=1` 的采样存在，清理后采样回到 `0`。当前探针没有提供可用的 JVM heap-used、连接池等待、SQL数量/单条耗时、按请求 Token usage/Token/s 或 Provider request ID，这些字段均保留为 `unavailable` 并说明原因。

本次新 GLM stream/REST 波次已追加到唯一状态矩阵 `testing/Agent/性能/reports/phase2-status-matrix.md`。矩阵当前把27项归类为：`Passed-small-sample` 9项、`Failed` 1项、`Failed-small-sample` 3项、`Blocked` 7项、`Deferred` 7项，没有计划级 `Passed`。旧父用例行和历史波次没有覆盖或删除；收口映射通过矩阵和随后追加的台账行提供。

第二阶段仍未达到计划样本和指标完整的收口条件。671条 Agent 功能分支、Android/Web/iOS正式测试和日报/周报/月报验证继续暂停。所有新波次均使用真实 Agent、真实 REST/SSE、真实工具链路（有工具的场景）和云端 PostgreSQL；失败样本、清理记录、业务表不变结果以及 unavailable 指标均保留。

## 第二阶段执行记录（2026-09-22）

### 当前文件与运行条件

- 新波次前的证据目录有 1007 个普通文件，最大编号 1008，967 缺失，1009 及以上尚无文件。R04 后新增 1009–1016，共 8 个文件；没有填补 967。
- `898+` 原有 109 个 JSON/JSONL 文件中，有 319 条顶层逻辑记录；292 条具有 `redaction_status`，27 条 DB 快照缺少此字段。27 条的顶层键均为 `captured_at/counts/error_type/ssh_exit_code`。缺少字段的编号为 899、903、909、912、918、920、926、928、934、936、942、944、950、952、958、960、966、969、975、977、983、985、991、993、999、1001、1007；原文件保留。904、929 是空 JSONL，不计作已执行预热。
- 原目录的 609 个 JSON、348 个 JSONL 均可解析，JSONL 非空记录 2329 条。顶层与嵌套状态包括 `redacted_no_payloads`、`redacted_no_credentials_or_payloads`、`metadata_only_no_payloads`、`resource_summary_only`；后两种属于明确的元数据/资源脱敏状态。这里统计的是文件记录，含汇总中的重复表示，不能直接当作真实请求总数。
- 运行时于 `2026-09-22T12:09:40.041047+00:00` 核对到 `glm-5.3-flash`、`https://oneapi.sxyq27.online/v1`、`chat_completions`、镜像 `sxyq27-zhj-api:20260915T0002-agent-sse-timeout-300s`。API 启动于 `2026-09-18T04:52:35.540932366Z`，重启次数 0；API/PostgreSQL/Redis 均运行，PostgreSQL 为 healthy，公网 `/healthz` 直连 HTTP 200。
- 账号为 `existing-account-2002`，owner/store 标签为 `existing-owner-4` / `existing-store-2`，权限为 OWNER；受保护密码仅用于进程内登录，并通过 `/v1/auth/users/me` 确认当前账号。未创建账号、未重置密码。
- 当前既有 `active_runs=2`，分别为 `ui-seed-run-002` 和 `ui-seed-run-003`。旧报告中的 0 只代表旧波次当时的清理后读数，不能作为当前清理目标。

### CSV 原文的逐行结果

原文件共 138 个物理行、137 条数据记录，没有多行 CSV 记录。表头为 20 列；使用 Ruby `CSV.parse_line` 逐物理行解析，以下 19 行与表头不符。不得通过按表头补空的读取结果声称只有第 7 行异常。下表记录本轮追加行之前的原有行号。

| 物理行 | test_id | 实际列数 |
|---:|---|---:|
| 2 | AG-P-004-PHASE2-DRAFT-20260914-001 | 18 |
| 3 | AG-P-004-PHASE2-DRAFT-EXPLICIT-20260914-001 | 18 |
| 4 | AG-P-006-007-PHASE2-SSE-FULL-20260914-001 | 18 |
| 7 | AG-P-L1-STATUS-ENUM-FIX-VALIDATION-20260913-001 | 21 |
| 8 | AG-P-L1-FINAL-UNIFIED-QWEN38-C1-20260913-001 | 19 |
| 9 | AG-P-008-PHASE2-MULTITOOL-20260913-001 | 19 |
| 10 | AG-P-002-PHASE2-SINGLE-TOOL-20260913-001 | 19 |
| 84 | AG-P-L1-QWEN38-SINGLE-VALIDATION-20260913-001 | 18 |
| 85 | AG-P-L1-QWEN38-SCHEMA-OPT-20260913-001 | 18 |
| 86 | AG-P-L1-QWEN38-FULL-C124-20260913-001 | 18 |
| 87 | AG-P-L1-QWEN38-FULL-C8-20260913-001 | 18 |
| 95 | AG-P-001-PHASE2-REAL-CLOUD-20260914-001 | 18 |
| 96 | AG-P-002-PHASE2-CONCURRENCY-PERMIT-20260914-001 | 18 |
| 97 | AG-P-003-PHASE2-RESULT-SIZE-20260914-001 | 18 |
| 98 | AG-P-008-PHASE2-NEGATED-WRITE-FIX-20260914-001 | 18 |
| 99 | AG-P-021-PHASE2-PAGINATION-SMALL-RERUN-20260914-001 | 19 |
| 102 | AG-P-016-PHASE2-CANCEL-SMALL-20260914-001 | 18 |
| 103 | AG-P-017-PHASE2-DISCONNECT-SMALL-20260914-001 | 18 |
| 104 | AG-P-010-PHASE2-CONTEXT-COMPACTION-20260914-001 | 17 |

字段归属仍在逐行核对，以上原文暂时保留；没有补写历史测量值或更改历史状态。R04 新增记录为 20 列。

### AG-P-001 GLM Stream R04（1009–1016）

| 项目 | 实际结果 |
|---|---|
| 条件 | 当前 GLM 组合；并发 1；计划 1 条预热、30 条正式；短无工具输入 |
| 预热 | 1 条；HTTP 200；Agent 终态 failed；`LLM_ANSWER_UNAVAILABLE`；有效 0 条 |
| 正式请求 | 0/30；停止后没有继续发送 |
| 失败预热时序 | 响应头 278.32 ms；首事件 278.60 ms；响应完成 19496.80 ms；无首回答；不进入成功百分位 |
| SSE/audit | 3 个事件；事件类型、ID、序号一致；序号连续；唯一终态；重复 0；非空身份标识无混合 |
| 清理 | audit 读取结束早于 cleanup；清理身份核对通过；本波次新会话 cleanup HTTP 200；既有会话未清理 |
| 数据库 | 会话 31→31；消息 133→133；草稿 4→4；audit 2735→2736；audit events 25061→25064；43 张业务表计数均未变化 |
| 既有运行记录 | `active_runs` 2→2；两条 ui-seed ID 集合未变化 |
| 资源 | 4 条采样，SSH 4/4 成功；没有正式请求资源样本 |
| 结论 | 预热失败；正式样本未执行；性能/SLA 仍为 Deferred；AG-P-001 历史小样本状态保留 |

R04 采集器额外给出 `transport_audit_identity_mismatch` 停止原因：`error` 事件缺少 `audit_id`、`trace_id`，其余两个事件的非空值均唯一且与 audit 一致。当前 `V2AgentAiService.java` 的错误事件构造未提供这两个字段，`RunAuditService.prepareSend` 只补充 conversation、序号和事件 ID。该读数不能证明串会话。采集器 r2 已改为比较非空身份标识，同时单列缺省次数；真实不同标识仍会停止，既有会话仍禁止清理。原 1009–1016 文件不改写，Provider 失败仍为失败样本。

本轮仅修改已有采集器、阶段报告、状态矩阵和性能台账。串行路径已验证预热失败停止、正式首条异常停止、既有会话保护、正常 1+30 调度，以及跨波次连续 Provider 错误计数；这些无网络用例不计入云端样本。每条真实记录和资源记录即时 flush/fsync；波次前保存既有会话集合于进程内，证据只记录数量。

证据：`testing/Agent/性能/artifacts/20260908-layer1-cloud-initial-measurement/1009-glm53flash-p001-no-tool-r04-20260922-collector-format-validation.json`、`1010-glm53flash-p001-no-tool-r04-20260922-db-before.json`、`1011-glm53flash-p001-no-tool-r04-20260922-warmup.jsonl`、`1012-glm53flash-p001-no-tool-r04-20260922-formal.jsonl`、`1013-glm53flash-p001-no-tool-r04-20260922-failure-analysis.json`、`1014-glm53flash-p001-no-tool-r04-20260922-resource-samples.jsonl`、`1015-glm53flash-p001-no-tool-r04-20260922-summary.json`、`1016-glm53flash-p001-no-tool-r04-20260922-db-after.json`。

当前仍缺按请求 Token usage/Token/s、Provider request ID、JVM heap-used、连接池等待、SQL 数量和逐条耗时；AG-P-010 另缺压缩耗时、摘要类型比例、checkpoint 保存/复用。第二阶段未收口，后续功能、客户端正式测试和报表验证继续暂停。
