# Agent 上下文窗口核对阶段报告

核查日期：2026-08-24（Asia/Shanghai）
核查角色：Agent E  只读源码、配置、测试和服务器部署核对

## 结论

当前本地源码 commit 为 `f07529903904832767ad6a3fade820d3aabbc4c0`。生产构造路径的配置上限是 `32,768`，当前模型 `gpt-5.6-luna` 没有模型专属窗口覆盖，窗口解析结果落到保守默认 `8,192`。

按 `8,192` 的实际解析结果计算，系统、工具结果、历史消息与检查点预算分别为 `819`、`1,638`、`1,969` tokens；正式回答预算为 `1,228` tokens。模型请求的 `max_tokens` 另由 `AgentLlmProperties` 配置，当前默认值是 `4,096`，它没有被 `ContextBuilder` 直接用于 `reservedOutputBudget`。

`8.220.206.9` 当前运行 `sxyq27-zhj-api:20260818`，镜像 digest 为 `sha256:3b526c02...f6fffc48`，容器内外 JAR SHA-256 均为 `4289b733...1832edd2`。服务器显式配置的模型和线路与当前源码默认值一致，也没有发现上下文窗口覆盖键。服务器部署目录没有 Git 元数据或提交标记，镜像 tag 早于本地源码快照，无法确认服务器编译实现与当前源码完全一致，状态记为“部分匹配，完整值未确认”。

没有认证信息可用于安全长会话诊断；只读 GET `/actuator/health` 返回 HTTP 403，因此没有伪造 `context_compacted` 的运行时事件值。

## 精确窗口与预算

| 项目 | 当前值 | 证据与计算 |
|---|---:|---|
| `configuredMaximum` | `32768` | `ContextWindowResolver.CONFIGURED_MAXIMUM_DEFAULT`；生产构造器使用该常量 |
| 模型专属 override | 无 | `KNOWN_MODEL_WINDOWS` 为空，生产构造器传入 `Map.of()`；测试构造器支持 `provider:model:wireApi` 键 |
| 默认/保守窗口 | `8192` | `CONSERVATIVE_FALLBACK_WINDOW` |
| 当前解析窗口 | `8192` | `gpt-5.6-luna` 不在已知模型表，服务器也没有窗口覆盖 |
| 可用窗口 | `8192` | `usableWindow = providerWindow`；生产构造器没有单独配置绑定 |
| 正式回答预留 | `1228` | `floor(8192 * 0.15)` |
| 模型请求 `max_tokens` | `4096` | `AgentLlmProperties` 和三个 application 配置的默认值 |

当前模型没有 `provider` 配置，解析 override key 时使用有效引用 `default`；服务器环境也没有单独的 provider 字段。模型引用为 `gpt-5.6-luna`，线路为 `chat_completions`。

### 当前解析结果的预算组成

未知模型触发 `degradedEstimate`，安全余量从 `10%` 提升为 `20%`。每项由 `Math.floor(window * ratio)` 计算，历史预算取扣除各固定项后的剩余值。

| 预算项 | 比例 | 当前 tokens |
|---|---:|---:|
| 系统规则 | 10% | 819 |
| owner/store 作用域 | 3% | 245 |
| 当前用户问题 | 8% | 655 |
| 当前轮工具结果 | 20% | 1,638 |
| 正式回答预留 | 15% | 1,228 |
| 安全余量 | 20%（降级后） | 1,638 |
| 历史消息与检查点 | 扣除后的剩余 | 1,969 |

`HISTORY_RATIO` 常量的名义比例是 `34%`，适用于未触发降级安全余量提升的路径；当前未知模型路径使用 `20%` 安全余量，所以实际历史预算为 `1,969`，约占 `24%`。测试中的已知模型覆盖场景使用 `64,000` 窗口，安全余量为 `10%`，用于验证名义比例。

## 压缩触发条件

当前 `ContextBuilder` 的实际布尔条件是：

```text
estimatedInputTokens > historyBudget * 0.70
OR messagesAfterBoundary.size() > 24
```

其中：

```text
estimatedInputTokens
= systemTokens + scopeTokens + checkpointTokens + historyTokens + currentQuestionTokens
```

`ContextCompactionService` 的类注释还列出工具结果超过预算、Provider 返回上下文超限、检查点失效后重建仍超预算三项场景。当前 `compactIfNeeded` 只接收 `ContextBuilder` 生成的布尔值，以上三项没有在该方法中形成独立输入分支。进入压缩服务后，至少要有两个已完成的 user/assistant 轮次；服务选择更早的完整历史段，保留当前问题和最近轮次。

阈值证据：`ContextBuilder.COMPACTION_THRESHOLD_RATIO = 0.70`，历史消息上限为 `24`。

## TokenEstimator 算法

`TokenEstimator` 不调用 Provider tokenizer，也不读取 Provider usage：

1. 空值或空白文本记为 `0`。
2. 非空文本按 UTF-16 字符长度估算，先限制到 `64,000` 字符，再返回 `ceil(length / 3.0)`，非空结果至少为 `1`。
3. JSON 消息使用 `JsonNode.toString()` 后加每消息固定开销 `4`；空节点和缺失节点为 `0`，序列化异常退化为 `4`。
4. 消息列表逐条累加 JSON 估算。
5. 已格式化历史文本按文本估算值再加 `4 * 换行数`。

## Checkpoint 摘要与 token estimate

检查点 `summary_body` 是确定性摘要或通过结构校验的语义摘要。保存时：

- `estimated_input_tokens = tokenEstimator.estimate(summaryBody)`，表示该摘要作为上下文输入时的估算量；当前压缩结果的 `inputTokenEstimate` 也沿用这个摘要估算值。
- `estimated_output_tokens = 0`，当前客户端没有把语义压缩 Provider 的实际输出 usage 写入检查点，因此该值不是 Provider 报告值。
- `ContextBuilder.ContextBudget.estimatedInputTokens` 是另一层全量输入预算估算，包含系统规则、作用域、检查点摘要、历史和当前问题，不包含工具结果和正式输出。
- 复用检查点时，`CompactionResult.reused` 从已保存的两个 estimate 字段读取；没有运行时重新 tokenize。

## `context_compacted` SSE payload

事件名为 `context_compacted`。`SseStreamEmitter.emitContextCompacted` 生成的基础字段为：

| 字段 | 说明 |
|---|---|
| `event_type` | 固定为 `context_compacted` |
| `run_id` | 当前运行 ID |
| `compacted_count` | 本次纳入摘要的消息数 |
| `input_token_estimate` | `CompactionResult.inputTokenEstimate()` |
| `output_token_estimate` | `CompactionResult.outputTokenEstimate()`，当前新检查点为 `0` |
| `reason` | 默认 `context_budget_threshold`，复用时可为 `checkpoint_reused` |
| `reused` | 是否复用已有检查点 |
| `audit_id` / `trace_id` | 运行审计和链路标识 |
| `timestamp` | 当前毫秒时间戳 |
| `checkpoint_id` | 有检查点 ID 时发送 |
| `source_boundary_message_id` | 有边界时发送 |
| `summary_preview` | 有摘要时发送；事件不另带检查点实体字段 |

`V2AgentAiService` 的流式路径在压缩发生后发送此事件；非流式路径同样使用压缩后的历史和摘要，但没有发送 SSE。

## 源码、配置、测试与文档证据

| 范围 | 当前确认 |
|---|---|
| `AgentLlmProperties` | `agent.llm` 的模型、线路和 `maxTokens=4096` 默认值见 `/Users/sunyiyang/Desktop/Project/master-goods/Code/backend/src/main/java/com/zhihuiji/backend/infrastructure/config/AgentLlmProperties.java:7`、`:14`、`:17` |
| 窗口解析 | 上限、保守窗口、空的已知模型表和生产 `Map.of()` 见 `/Users/sunyiyang/Desktop/Project/master-goods/Code/backend/src/main/java/com/zhihuiji/backend/application/service/v2/agent/context/ContextWindowResolver.java:25`、`:31`、`:37`、`:50` |
| 预算构建 | 比例、降级余量、触发条件、预算取整和 `ContextBudget` 字段见 `/Users/sunyiyang/Desktop/Project/master-goods/Code/backend/src/main/java/com/zhihuiji/backend/application/service/v2/agent/context/ContextBuilder.java:45`、`:63`、`:108`、`:148`、`:174`、`:203` |
| 压缩服务 | 触发说明、完整轮次选择、检查点保存和两个 estimate 字段见 `/Users/sunyiyang/Desktop/Project/master-goods/Code/backend/src/main/java/com/zhihuiji/backend/application/service/v2/agent/context/ContextCompactionService.java:31`、`:125`、`:510`、`:462`、`:611` |
| 估算算法 | 见 `/Users/sunyiyang/Desktop/Project/master-goods/Code/backend/src/main/java/com/zhihuiji/backend/application/service/v2/agent/context/TokenEstimator.java:22`、`:27`、`:32`、`:40`、`:58`、`:97` |
| AI 调用上下文 | 非流式和流式均先 build/compact；只有流式路径 emit 事件，见 `/Users/sunyiyang/Desktop/Project/master-goods/Code/backend/src/main/java/com/zhihuiji/backend/application/service/v2/V2AgentAiService.java:354`、`:361`、`:742`、`:749`、`:759` |
| SSE payload | 见 `/Users/sunyiyang/Desktop/Project/master-goods/Code/backend/src/main/java/com/zhihuiji/backend/application/service/v2/agent/component/SseStreamEmitter.java:419` |
| 应用配置 | `application.yml`、`application-local.yml`、`application-prod.yml` 均只声明 `agent.llm`，没有 `agent.context` 窗口键 |
| 单元测试 | `ContextWindowResolverTest`、`ContextBuilderTest`、`TokenEstimatorTest`、`ContextCompactionServiceTest` 覆盖窗口、预算、估算、触发和检查点行为 |
| 优化文档 | `/Users/sunyiyang/Desktop/Project/master-goods/docs/04_详细设计与实现/Agent三要素与上下文压缩优化执行计划.md:535` 起记录预算方案；源码是本次运行值的依据 |

## 服务器只读核查

核查目标：`root@8.220.206.9`，使用用户指定的本机 RSA key 临时 `0600` 副本；未输出 key 内容和任何认证载荷。

| 项目 | 结果 |
|---|---|
| SSH / TCP 22 | 成功 |
| hostname / 时间 | `iZmj78qgg67a6wdpd8u262Z` / `2026-08-24T09:24:31+08:00` |
| 实际 Agent 容器 | `sxyq27-zhj-api`，未发现 `master-goods` 同名容器 |
| 镜像 | `sxyq27-zhj-api:20260818`，ID `sha256:3b526c02ba425908cf4859625568c964c113e53903f8693b1456a573f6fffc48` |
| JAR 摘要 | `/opt/sxyq27/master-goods/zhihuiji-backend-0.1.0.jar` 与 `/app/app.jar` 同为 `4289b73346780986647ed1140fa92552656e7cc2ba53a54826e8eaa01832edd2` |
| Spring profile | `prod` |
| 模型 / 线路 | `gpt-5.6-luna` / `chat_completions` |
| 窗口覆盖 | `runtime.env`、`compose.yml` 未发现 `AGENT_CONTEXT_*`、`maximum-window` 或 `window-overrides` |
| `max_tokens` 覆盖 | `AGENT_LLM_MAX_TOKENS` 未设置；当前源码默认为 `4096`，但服务器编译 commit 未确认 |
| 部署 commit | 服务器部署目录无 Git 仓库、release marker 或 commit marker，无法确认 |
| 只读健康 GET | `/actuator/health` 返回 HTTP 403；没有认证，不继续发起长会话 |

服务器配置可确认的部分只有模型、线路、生产 profile 和覆盖键缺失。镜像 tag `20260818`、镜像创建时间 `2026-08-19` 与本地当前源码 commit 的时间线存在差异，服务器没有可追溯的源码 commit，因此完整值状态为：`partial_match_unconfirmed`。

## 验证与本轮对象

本轮总需求 1 项：源码/配置/测试/服务器窗口核对并生成证据。源码核对、服务器只读核对、报告和 JSON 已完成；服务器提交号追溯部分完成，结果为无法确认。

已执行并通过：

```text
./Code/backend/gradlew -p Code/backend test \
  --tests com.zhihuiji.backend.application.service.v2.agent.context.ContextWindowResolverTest \
  --tests com.zhihuiji.backend.application.service.v2.agent.context.ContextBuilderTest \
  --tests com.zhihuiji.backend.application.service.v2.agent.context.TokenEstimatorTest \
  --tests com.zhihuiji.backend.application.service.v2.agent.context.ContextCompactionServiceTest
```

结果：`BUILD SUCCESSFUL`，四个指定上下文测试套件通过。

本轮新增对象：

- `/Users/sunyiyang/Desktop/Project/master-goods/testing/Agent/功能测试/阶段报告-20260824-Agent-Context-Window-E.md`
- `/Users/sunyiyang/Desktop/Project/master-goods/testing/.artifacts/2026-08-24-agent-context-window-E/context-window.json`

未修改业务源码、数据库、服务器配置、容器、Nginx、`functional_feature_matrix.csv`、用户 docs、D/B 文件和 data 目录。

## 剩余风险

1. 当前有效窗口是未知模型的保守值 `8,192`；服务器的编译提交号不可追溯，无法证明它已包含当前窗口组件的全部实现。
2. 当前上下文预算预留输出为 `1,228`，模型请求上限却是 `4,096`。两者来自不同配置路径，存在实际输入加输出超出窗口的风险，当前核查不改动该行为。
3. `context_compacted` 的新检查点 `output_token_estimate` 固定为 `0`，没有 Provider usage 证据；本轮也没有认证条件下的真实 SSE 事件。
