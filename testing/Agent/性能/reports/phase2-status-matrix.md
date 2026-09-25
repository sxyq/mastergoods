# 第二阶段 Agent 性能专项状态矩阵（2026-09-22）

本矩阵只汇总 `testing/Agent/性能/TEST_PLAN.md` 定义的 AG-P-001 至 AG-P-027。它区分真实行为结果、样本规模和性能/SLA结论；没有正式 SLA 时，性能/SLA结论保持 `Deferred`，不能由一次测量推出达标。

## 证据边界

- 当前证据目录为 `testing/Agent/性能/artifacts/20260908-layer1-cloud-initial-measurement/`。数字文件实际覆盖 `898–991` 中的 93 个文件，其中 `911–991` 有 80 个文件；当前目录最大编号为 `1016`，缺少 `967`。`992–1007` 是真实 GLM `AG-P-001` 历史波次，`1008` 是当时的运行元数据复核；`1009–1016` 是 2026-09-22 Stream R04，1 条预热失败、正式请求 0/30。没有生成文件填补 `967`。
- `911–991` 的 JSON/JSONL 没有逐记录保存 model、Provider、image 或 endpoint 字段。模型和运行组合只在关联文档、文件名和 `1008` 当前运行复核中得到支持；涉及该批次的结论保留这一限制。
- Qwen、GLM、DeepSeek、不同镜像、不同 Provider 和不同传输方式分开统计。现有成功样本只形成同一组合下的阶段测量，不能拼成跨组合百分位。
- 正式百分位只使用严格有效请求；认证失败、参数拒绝、Provider 不可用、设备缺失、采集异常和错误终态单独保留。每个新增 GLM波次的 `redaction_status` 均为脱敏完成，未保存凭据、完整请求或完整回答。
- 数据库计数顺序统一为 `agent_conversations/agent_messages/agent_drafts/agent_run_audits/agent_run_audit_events/products/customers/finance_records`。会话、消息和审计留痕的增加不等于业务表写入；正式业务表变化单独判断。
- `898+` 原有 27 条 DB 快照没有 `redaction_status` 字段；其顶层内容为时间、计数和 SSH 结果。`metadata_only_no_payloads`、`resource_summary_only` 属于明确脱敏状态。缺字段及 904/929 空 JSONL 的详细编号见阶段报告 2026-09-22 章节，原始文件不改写。
- 当前既有 `active_runs=2`，为 `ui-seed-run-002`、`ui-seed-run-003`；当前采集以这两条记录为起点，保护全部 31 个既有会话。历史波次中的清理后 0 只适用于当时。

## 逐项矩阵

| 专项 | 计划样本 | 实际有效/异常样本 | 模型、镜像、传输与场景 | 最终状态 | 性能/SLA结论 | 证据 | 缺失指标、失败或停止原因 | 下一步条件 |
|---|---|---|---|---|---|---|---|---|
| AG-P-001 | 并发1；短问题 REST 与 stream 各30个有效请求 | 历史 GLM stream 30条：27条严格有效、2条 `LLM_ANSWER_UNAVAILABLE`、1条采集器错误；REST 30/30严格有效。2026-09-22 R04：预热失败1条、正式0/30 | GLM `glm-5.3-flash`；当前组合见1009；Provider `oneapi`；Wire `chat_completions`；REST/SSE及波次分开 | Passed-small-sample | Deferred；历史小样本保留；R04没有成功或正式性能样本 | `992–999`、`1000–1007`、`1008`、`1009–1016`；历史 Qwen `569`、`613` | R04为 `LLM_ANSWER_UNAVAILABLE`；error事件缺省audit/trace字段被采集器额外报作身份差异，非空标识无混合；按请求Token、heap、连接池和Provider request ID unavailable | 先验证当前组合能够完成预热，再执行30条正式样本；连续Provider错误达到2次即停止扩展 |
| AG-P-002 | 46个只读工具；每工具非流式10次、流式10次，共920条 | Qwen 与 GLM 仅有少量真实工具样本；GLM `product_catalog_lookup` 10/10、`inventory_panorama_lookup` 10/10，均为 stream；覆盖2/46工具、缺非流式 | Qwen 历史波次与 GLM `976–991` 分开；当前 GLM组合由 `1008` 复核 | Passed-small-sample | Deferred；不能由2个工具样本代表46个工具分布 | Qwen `577`、`586`、`596`；GLM `976–991`、`1008` | 工具耗时、Repository 查询数和逐工具完整清单未由采集器保存；绝大多数工具未执行 | 需要逐工具调度清单、46×10×2真实请求、请求级工具耗时/查询计数和每工具独立统计 |
| AG-P-003 | simple、multi-filter、large 三组各20次 | Qwen三组各5条，共15条，均为小波次 | Qwen `qwen3.8-flash`；同组镜像与 Provider 见阶段报告989–992段 | Passed-small-sample | Deferred；large组已有阶段测量，样本不足以推出完整分布 | `623`、`631`、`639` | 参数解析、SQL明细、序列化耗时和完整结果规模指标缺失 | 同一 Qwen组合完成三组各20条，并保留查询、序列化和结果大小字段 |
| AG-P-004 | 14个创建工具各20次，仅创建草稿 | 两轮各5条；首轮含 `TOOL_ARGUMENTS_INVALID`，补充轮含 `AGENT_ITERATION_EXHAUSTED`；草稿0条 | Qwen `qwen3.8-flash`；真实 Agent SSE；创建意图未生成可用草稿 | Failed | Failed；未产生草稿，不能进入确认性能 | `661–676` | 模型/Agent 参数规划原因未从源码或 Provider 请求级关联确认；正式样本远低于计划 | 先确认参数规划根因并取得可用真实草稿，再逐工具执行；不得把失败请求算成功 |
| AG-P-005 | 每类创建动作20次成功确认 | 0条二阶段正式确认；AG-P-004没有有效草稿 | 无可归属的二阶段确认组合 | Blocked | Blocked；缺少可确认的真实草稿前置条件 | `661–676`；`TEST_PLAN.md:24` | 没有有效草稿、事务/锁等待/提交回滚数据 | AG-P-004产生真实有效草稿，并完成确认前后数据库、audit和清理核对 |
| AG-P-006 | 短问题、单工具、多工具、创建各30次 | Qwen正式5条；GLM SSE R01/R02共35条，主要为单只读工具；创建与多工具类别未覆盖 | Qwen历史组合、GLM `935–975` 分开；SSE | Passed-small-sample | Deferred；已有首事件、首工具、首回答小样本，类别覆盖和样本量不足 | `677–684`、`935–975`、`1008` | P95/P99需按独立记录复算；创建、多工具类别与完整资源指标缺失 | 统一场景分类后完成四类各30条，并保留类别级百分位 |
| AG-P-007 | 每类输入30次；验证事件丢失、重复、终态唯一 | 已有 Qwen5条、GLM35条完整流小样本；正式样本的 SSE/audit 对齐与唯一终态已观测，预热异常另记 | Qwen与GLM分开；Agent SSE | Passed-small-sample | Deferred；没有断线补发能力的重连结论，完整类别样本不足 | `677–684`、`935–975`、`1008` | 断线重连属于 AG-P-017；GLM原始文件缺少逐波次运行元数据；Token与资源字段缺失 | 补齐四类输入各30条，并把断线、重连、去重单独放入 AG-P-017 |
| AG-P-008 | 2/3/4/6工具计划各20次 | Qwen修复后5条成功；修复前5条耗尽，均为小波次 | Qwen `qwen3.8-flash`；真实 Agent SSE；旧失败与修复后镜像分开 | Passed-small-sample | Deferred；只证明一类多工具小样本行为 | `647`、`656`、`659`、`660` | 每轮起止时间、模型耗时、工具耗时和四档完整覆盖缺失 | 按2/3/4/6工具计划各20条执行，并保留轮次与调用预算字段 |
| AG-P-009 | 20个短会话，每个10轮 | 3个会话×3轮；严格干净完成0/3，含工具失败和1条 `LLM_ANSWER_UNAVAILABLE` | Qwen `qwen3.8-flash`；真实 Agent SSE | Failed-small-sample | Failed；没有形成不触发压缩的干净短会话结论 | `745–752` | 样本只有3×3；上下文构建和 Provider 时延的完整分解缺失 | 先稳定三轮只读链路，再完成20×10；确认未触发压缩时没有调用压缩 Provider |
| AG-P-010 | 10个会话，每个至少30轮，至少3个完整轮次 | Qwen R06 为10/10会话、300 turn、43次 `context_compacted`；R03/R04/R05及GLM长会话失败记录独立保留 | Qwen R06镜像 `20260915T0002-agent-sse-timeout-300s`；GLM `919–934` 不与Qwen合并 | Passed-small-sample | Deferred；R06样本完整但压缩耗时、摘要质量和 checkpoint 复用仍不可用 | `898–910`、`919–934`、`1008` | `compaction_ms`、确定性/语义比例、checkpoint保存/复用、按请求Token/s、JVM heap和连接池等待缺失；GLM长会话在第5轮失败且未触发压缩 | 保留R06成功与失败波次，补齐压缩观测字段后再给专项级结论 |
| AG-P-011 | 超预算10条、接近预算10条 | 0条二阶段正式样本 | 无可归属的正式模型组合 | Deferred | Deferred；没有真实拒绝时延和错误终态测量 | `TEST_PLAN.md:30`；阶段报告末段暂停说明 | 未验证超预算不静默截断，也未区分业务拒绝与 Provider 不可用 | 明确输入预算测量方式和错误分类后执行，禁止把 Provider 失败当业务拒绝 |
| AG-P-012 | 并发1/5/10/20，各30个有效请求 | 0条二阶段正式样本；第一层并发证据不能替代本专项 | 无当前组合的正式多用户流样本 | Deferred | Deferred；缺少当前组合的多用户流吞吐与资源数据 | `TEST_PLAN.md:31`；阶段报告 `998`、`1147` | 缺少当前组合的跨 owner/store、DB pool、线程和P95数据 | 先确认当前组合稳定，再逐档执行；任一串线、清理失败或持续5xx立即停止后续档位 |
| AG-P-013 | 同一会话并发2/5/10，各20组 | Qwen共享会话并发5条，5/5严格完成；GLM共享并发2波次5/5为小样本 | Qwen `753–761`；GLM `951–958`；共享会话各自清理 | Passed-small-sample | Deferred；只覆盖并发5/2的小样本，未覆盖三档计划 | `753–761`、`951–958`、`1008` | GLM共享波次部分记录没有逐请求 cleanup 字段；排队、乱序与锁等待未完整记录 | 补齐并发2/5/10各20组，保留共享会话级最终清理记录 |
| AG-P-014 | 并发2/5/10确认，各20个真实草稿 | 0条二阶段正式样本；没有有效草稿供竞争确认 | 无可用真实草稿 | Blocked | Blocked；创建草稿前置条件未满足 | `TEST_PLAN.md:33`；`661–676` | 未取得409/500、正式记录数、锁等待和清理证据 | 先完成AG-P-004并保留真实草稿，再按档位确认；同一草稿正式记录最多一条 |
| AG-P-015 | 顺序重复、网络重试、重复点击各20组 | 0条二阶段正式样本 | 无当前组合的重复请求样本 | Deferred | Deferred；没有重复请求行为测量 | `TEST_PLAN.md:34`；阶段报告 `1147` | 未验证去重命中、payload冲突、重复写入和响应一致性 | 明确幂等键或现行去重语义后，使用真实 Agent/确认路径执行并清理 |
| AG-P-016 | 首事件前、工具中、回答中、完成后各20次 | 20条小样本；前两类取消有效，后段取消为 `not_found` 或 no-op，回答阶段未完全中止 | Qwen `762–801`；真实 Agent SSE | Failed-small-sample | Failed；后段取消时延和 Provider 中止不满足专项不变量 | `828`及 `828-phase2-ag-p016-cancel-conclusion-20260914.md` | 计划样本未补足；回答中取消仍可能出现后续回答阶段 | 在同一镜像下复测四个断点并把 no-op 与真正中止分开统计 |
| AG-P-017 | 每个断点20次 | 12条小样本；断线后无 `id:`/`Last-Event-ID` 补发，最终出现 `STREAM_ERROR` | Qwen `803–827`；真实 Agent SSE | Failed-small-sample | Failed；当前协议不支持可验证的事件补发恢复 | `829-phase2-ag-p017-disconnect-conclusion-20260914.md` | 未实现或未确认服务端补发、去重和最终写入去重 | 若协议补发能力明确并部署，再重新执行；否则保留 Failed/Deferred，不写恢复通过 |
| AG-P-018 | KPI/表格/趋势/搜索/组合，小中大各30次 | 0条二阶段正式结果块样本；客户端正式性能尚未进入 | 无可归属的客户端正式组合 | Blocked | Deferred；客户端解析、首屏、OOM和图表一致性不可测 | `TEST_PLAN.md:37`；客户端性能报告与阶段报告 `1147` | 无物理设备/客户端正式条件；服务端响应字节不能替代客户端指标 | 二阶段收口后按客户端计划取得设备并独立执行，服务端和客户端证据分开 |
| AG-P-019 | 正常/延迟/超时/429各20次 | 0条；未向生产 Provider 注入延迟或错误 | 未执行 | Blocked | Deferred；安全隔离 Mock 不存在时不能制造费用或故障 | `TEST_PLAN.md:38` | 没有受控 Mock 的 Provider/Agent/资源释放数据 | 取得隔离 Mock 和获批运行窗口后执行；没有隔离条件继续保持 Blocked |
| AG-P-020 | `result_limit` 0/1/5/10/11各20次，并保留50/100条超量返回 | 0条二阶段正式样本 | 无当前组合的搜索上限样本 | Deferred | Deferred；没有当前组合的上限、截断和解析时延证据 | `TEST_PLAN.md:39` | 未验证11和非法值的执行前拒绝，也未验证 Provider 超量返回截断 | 先确定安全的超量返回入口，再执行合法值和拒绝值；生产 Provider 不做故障注入 |
| AG-P-021 | 小中大数据量各20次 | Qwen分页修复后5条成功；只读真实 Agent 小测 | Qwen `735–743`；真实 Agent SSE/云端 PostgreSQL | Passed-small-sample | Deferred；没有三种数据量各20条、SQL明细和生产查询计划 | `735–743` | SQL数量、单条耗时、内存峰值和 `EXPLAIN` 数据缺失 | 取得允许的生产数据库观测窗口后补样本；否则保持小样本与 Deferred |
| AG-P-022 | 至少30分钟或每并发档500个有效请求 | 0条二阶段正式 Soak 样本 | 无长时波次 | Deferred | Deferred；没有长时漂移、连接泄漏和持续资源数据 | `TEST_PLAN.md:41`；阶段报告 `1147` | 无长时波次；JVM heap、DB pool和长时SSE统计缺失 | 先完成当前组合稳定性与清理验证，再申请连续运行窗口后执行 |
| AG-P-023 | 有物理设备时每核心流程10次 | 0条二阶段真实 Agent 展示性能样本；历史有设备前置与模拟器诊断，但不能替代本专项 | Android 条件不足；服务端样本不替代客户端 | Blocked | Deferred；设备、首屏、帧率、APP内存不可用 | `TEST_PLAN.md:42`、`:82`；`android-p023-summary-20260829-agent-live-04.md` | 当前物理设备条件未满足，未启动替代模拟结论 | 设备可被 ADB 稳定发现后按客户端计划独立执行 |
| AG-P-024 | 有签名、Xcode和设备时每核心流程10次 | 0条二阶段 iOS 正式样本 | iOS 条件未确认 | Deferred | Deferred；iOS签名、Xcode、渲染和断线恢复指标未取得 | `TEST_PLAN.md:43`；`testing/Agent/客户端/TEST_PLAN.md` | 没有可验证 iOS 构建/设备条件；Web或Android不能代替 | 取得签名、Xcode和设备后独立执行，不用其他端推断 |
| AG-P-025 | 记忆存在时20次 | 0条二阶段正式样本 | 无记忆专项正式样本 | Deferred | Deferred；未测召回、异步提取和主回答不受阻塞 | `TEST_PLAN.md:44`；阶段报告 `1147` | 记忆数据、异步耗时和失败丢弃行为未采集 | 准备脱敏记忆夹具和数据库前后计数后执行 |
| AG-P-026 | 1张和9张附件各10次 | 0条；未对生产 Provider 发起多模态费用请求 | 未执行 | Blocked | Deferred；没有受控多模态 Provider和附件测试条件 | `TEST_PLAN.md:45` | 图片读取/编码、Provider耗时和半写入未验证 | 取得隔离 Provider、受控附件和清理方案后执行；不直接调用生产生图/多模态能力制造费用 |
| AG-P-027 | 草稿/拒绝/确认；并发1/5/10；Mock正常/慢/超时/取消；参考图0/1 | 0条二阶段正式样本 | 未执行 | Blocked | Deferred；真实费用、客户端UI和生产查询计划缺少安全隔离条件 | `TEST_PLAN.md:46`、`:97`；阶段报告 `1147` | 没有隔离图片 Provider Mock；未验证工具阶段0调用、确认最多1次和临时资源清理 | 取得隔离 Mock、附件条件、数据库观测和批准窗口后执行；禁止直接调用生产生图 Provider |

## 收口判断

- 27项中 `Passed-small-sample` 9项，`Failed` 1项，`Failed-small-sample` 3项，`Blocked` 7项，`Deferred` 7项；没有计划级 `Passed`。
- 已执行证据均来自真实 Agent 入口；有工具的样本经过真实工具和云端数据库链路。新 GLM `AG-P-001` 两种传输方式均完成数据库 before/after、audit 和 cleanup 记录：stream 为 `992–999`，REST 为 `1000–1007`，运行组合复核为 `1008`。
- 新 GLM stream 波次数据库计数为 `25/121/0/2668/24896/693/84/2661` → `25/121/0/2699/25020/693/84/2661`；REST 波次为 `25/121/0/2699/25020/693/84/2661` → `25/121/0/2730/25051/693/84/2661`。两波次业务表均未变化，cleanup 均为30/30，采集结束后的 `active_runs=0` 已由采集器的清理路径核对。
- 新 GLM stream 正式样本为 `27/30` 严格有效，P50/P95/P99/最大完成耗时为 `15668.52/19182.18/20641.90/21082.17 ms`；REST 为 `30/30` 严格有效，P50/P95/P99/最大为 `15410.75/20427.99/36992.96/43241.33 ms`。这两个分布只属于当前 GLM运行组合，不能和 Qwen、DeepSeek或旧镜像合并。
- 可观测资源的 SSH采样在两个新波次均为5/5成功；JVM heap-used、连接池等待、SQL数量/耗时、按请求 Token usage/Token/s 和 Provider request ID 均为 `unavailable`，原因已写入采集器输出和本矩阵。
- 第二阶段仍未达到收口条件。671条 Agent功能分支、Android/Web/iOS正式测试和日报/周报/月报验证继续暂停；本矩阵不把历史客户端样本或第一层并发样本替代二阶段计划样本。
- 2026-09-22 R04 已记录为独立失败预热波次：1条预热、0条正式、cleanup HTTP 200、43张业务表计数未变，audit增加1条、audit events增加3条，既有active_runs仍为2。新增记录没有改变上述27项历史状态数量。CSV原文仍有19行列数异常，本轮记录按20列追加；异常行号和test_id见阶段报告。
