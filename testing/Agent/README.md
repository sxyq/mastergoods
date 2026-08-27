# Agent 端测试资料总览

更新时间：2026-08-28

本目录是 Agent（后端 `/v2/agent`、Android/iOS APP、Web 协议对照）的**唯一**测试资料组织点。以测试类别为单位拆分规划文档、执行台账、脚本、日志、报告和原始证据，取代旧的单份《Agent 综合功能与性能测试方案》（已删除）与历史分类占位目录（observability 等已并入对应类别，本目录不再出现）。

## 一、目录结构

```text
testing/Agent/
├── README.md                  # 本文件：总览、分类说明、状态口径、证据规范、执行顺序、维护规则
├── 代码事实基线.md             # 从当前源码核准的功能与工具基线（工具清单/调用链/SSE/终态/预算/表/路由/配置）
├── 映射台账.md                 # 功能域与工具到测试类别的映射、编号规则、断言链
├── 功能/TEST_PLAN.md          # 功能测试
├── 安全/TEST_PLAN.md          # 安全与租户隔离测试（含敏感信息扫描；不设独立“审计”类别）
├── 性能/TEST_PLAN.md          # 性能、并发、长会话与 Soak 测试
├── 单元/TEST_PLAN.md          # 单元与组件测试（映射现有测试类，登记缺失项）
├── 契约/TEST_PLAN.md          # API、SSE 与序列化契约测试
├── 集成/TEST_PLAN.md          # 服务、Provider、数据库与事务集成测试
├── 可靠性/TEST_PLAN.md        # 超时、取消、断线、重试与恢复测试
├── 数据/TEST_PLAN.md          # 数据一致性与清理测试
├── 客户端/TEST_PLAN.md        # Android/iOS 联调与 Web 协议对照
├── 脚本/README.md + <类别>/   # 各类别执行脚本
└── <类别>/{artifacts,logs,reports}/.gitkeep   # 各类别证据、日志、报告目录
```

类别目录均同时承担执行证据存放：`artifacts/<日期>-<波次>-<用例>/`、`logs/`、`reports/`。脚本只放 `脚本/<类别>/`。

## 二、分类说明

| 类别 | category_id | 范围 | 判定边界 |
|---|---|---|---|
| 功能测试 | `F` | 会话/消息、60 个工具逐项、多工具链、Loop、SSE、草稿与二次授权、结果块、上下文压缩、多模态图片、长期记忆、海报、Web 搜索、任务/通知/工作台 | 以业务终态、事件顺序、回答完整性、数据库变化为准；HTTP 200 不等于业务成功 |
| 安全与租户隔离 | `S` | 未登录/权限不足、IDOR、store/owner 伪造、Prompt 注入、工具越界、未注册工具、确认重放、付款幂等、SSRF、路径穿越、SSE 串线、错误泄露、敏感信息扫描、并发身份切换、压缩脱敏 | 越权/未确认写入/敏感泄露均为 0；拒绝必须可审计且错误码稳定 |
| 性能测试 | `P` | 时延基线、SSE 首事件/完整流、工具与循环、压缩、并发流式、草稿确认竞争、取消/重连时延、结果块规模、Provider 慢响应、分页、长会话 Soak | 无 SLA 前只建可复现基线，记 `Deferred`；有效请求 5xx=0 是起点而非通过条件 |
| 单元/组件测试 | `U` | ToolPlanner、ToolExecutor、ToolRegistry、ToolArgumentsValidator、ContextBuilder、ContextCompactionService、ContextWindowResolver、TokenEstimator、SafetyGuard、RunAuditService、SseStreamEmitter、AnswerSynthesizer、AgentIterationPolicy、AgentRunState、AgentPromptCatalog、ToolInvocationIdentity、AgentMemoryService、AgentDraftConfirmService、AgentImageService、DTO/实体映射、Android/iOS 组件 | 以现有 JUnit/Kotlin 测试与新增测试为准；目标模块全部 `Passed`，失败单独登记 |
| API/序列化契约 | `C` | 24 个 `/v2/agent` 端点、SSE 事件字段契约、snake_case、错误码、分页、ID 精度（Web 不超过 2^53） | 请求/响应/事件可双向解析；错误码稳定；客户端不崩溃 |
| 集成测试 | `I` | V2AgentAiService 全链路、LongCatAnthropicClient、WebSearch/Image Provider、Repository、事务边界、真实数据库、记忆落库 | 实际调用链与预期一致；事务提交/回滚正确；无重复写入 |
| 可靠性与故障 | `R` | Provider 超时/429/空响应/非法 JSON、SSE 断线/取消/重复事件、Last-Event-ID、重连、资源释放 | 状态机收敛；允许的重试不产生重复写入；失败不伪装成功 |
| 数据一致性 | `D` | 草稿边界、正式表变化、重复确认、确认失败回滚、同 key 幂等、测试数据清理、迁移数据核对 | before/after 差异准确；清理后无预期外残留 |
| 客户端联调 | `CLI` | Android/iOS 输入、收流、工具过程、结果块/图表、草稿弹窗、历史恢复、后台切换；Web 协议对照 | 服务端与 APP 展示一致；设备/签名条件缺失记 `Blocked` |
| 脚本 | `脚本/` | 各类别执行脚本、数据准备、清理与探针 | 脚本本身纳入 Git 检查，凭据/密钥不得入脚本 |

旧文档中的“可观测性与审计 `O`”类别不再单列：run-trace/audit 对齐作为每条用例的固定观察项（见下文“最小字段”），敏感信息扫描归入 `S`，SSE/audit 字段契约归入 `C`。

## 三、状态口径

每条用例/记录的状态值**只允许**：

| 状态 | 含义 |
|---|---|
| `Passed` | 已执行，实际证据满足全部验收条件 |
| `Failed` | 已执行，实际行为不满足预期，有对应证据 |
| `Blocked` | 未执行，因为真实环境/Provider/数据库/设备条件不满足或未知 |
| `Deferred` | 已计划，尚未执行（本轮所有规划用例的初始状态） |

未执行内容不得写成 `Passed`。任何一条记录缺实际输入、工具调用链或证据路径时保持 `Deferred`，不能用相邻工具结果代替。

## 四、每条用例的最小字段

统一覆盖：测试目标、前置条件、输入提示词或请求参数、预期调用工具、预期工具调用顺序、Loop 循环与上下文压缩行为、预期 SSE 或普通响应、正式回答内容、数据库/草稿/审计/业务表变化、边界条件、验收条件、日志/脚本/原始证据位置。

| 字段 | 内容 | 必填 |
|---|---|---|
| `test_id` | 唯一编号，如 `AG-F-TOOL-RO-001`、`AG-S-010`、`AG-P-001` | 是 |
| `category_id` | `F/S/P/U/C/I/R/D/CLI` | 是 |
| `test_objective` | 测试目标 | 是 |
| `preconditions` | 前置条件（环境、账号、数据、Provider、设备） | 是 |
| `input` | 输入提示词或请求参数 | 是 |
| `expected_tools` | 预期调用工具（含允许的依赖工具） | 是 |
| `expected_order` | 预期工具调用顺序 | 是 |
| `loop_and_compaction` | 预计 Loop 轮数/终止点、是否会触发上下文压缩、检查点行为 | 是 |
| `expected_response` | 预期 SSE 事件序列或 REST 响应 | 是 |
| `expected_answer` | 正式回答内容要求（非空、可追溯、不得编造） | 是 |
| `db_changes` | 数据库、草稿、审计与业务表变化（before/after） | 是 |
| `boundaries` | 边界条件（空数据、非法参数、权限、跨域、重复、并发、分页） | 是 |
| `acceptance` | 验收条件 | 是 |
| `evidence_path` | 日志、脚本与原始证据存放位置 | 是 |
| `result` | `Passed/Failed/Blocked/Deferred` | 是 |

## 五、证据与脱敏规范

单条用例目录 `testing/Agent/<类别>/artifacts/<日期>-<波次>-<用例>/` 按顺序保存：

```text
00-environment.md           # 服务版本、Provider、设备、账号脱敏标签
01-input-redacted.json      # 脱敏后的输入/请求
02-http-response.json       # REST 响应或错误信封
03-raw-sse.log              # 原始 SSE 事件（脱敏）
04-tool-trace.jsonl         # 工具调用链/参数摘要/结果摘要/耗时
05-run-audit.json           # run audit（含 lossy 计数与告警）
06-database-before.json
07-database-after.json
08-app-observation.md       # APP 展示观察（设备条件具备时）
09-cleanup.json             # 清理结果
10-conclusion.md            # 结论（结论同时回写对应 TEST_PLAN 的用例行）
```

禁止写入：Authorization、Cookie、Session Token、密码、私钥、API key、模型密钥、完整认证载荷、未脱敏手机号/地址、其他 owner 的完整业务数据。提交前按这些模式扫描，命中任何一项不得标记为可交付。

## 六、执行顺序

1. 建立环境记录：工作树版本、服务版本、APP 版本、Provider 配置、数据库、账号与设备。
2. 执行 `单元/`（现有测试 + 新增组件测试）与 `契约/`（静态契约核对，不依赖真机）。
3. 前置验证认证、会话、owner/store、清理权限；前置不满足记 `Blocked`。
4. 按 `功能/` 执行 46 个只读工具（非流式 + 流式）与 14 个创建工具（草稿/拒绝/确认/重复确认）。
5. 执行多工具链、Loop 六种终态、SSE 取消/断线/重连、上下文压缩 14 个场景。
6. 执行多模态图片、长期记忆、海报、Web 搜索与新建任务/通知/工作台功能。
7. 按 `安全/` 执行越权/注入/幂等/敏感信息专项（真实账号与真实服务）。
8. 按 `性能/` 先做单用户基线，再并发、长会话、取消、Soak。
9. 按 `客户端/` 在真实设备上执行 Android/iOS 联调；Web 只做协议对照。
10. 汇总：回写各档用例的 `result`；未满足证据要求的项目不得填 `Passed`。

## 七、维护规则

1. 本目录是 Agent 测试唯一体系；不再新建第二份 Agent 方案、阶段报告或重复台账。
2. 新增工具、新增创建类草稿类型、压缩策略或 SSE 事件变更时，先更新 `代码事实基线.md` 与 `映射台账.md`，再补充对应类别用例。
3. 结果状态只能使用四值口径；历史旧状态（如部分完成、未闭环）必须在迁移时折算为 `Passed/Failed/Blocked/Deferred` 之一。
4. 旧的《Agent 综合功能与性能测试方案》已删除，其编号体系（`AG-F-*`、`AG-S-*`、`AG-P-*`、`AG-C-*`、`AG-U-*`、`AG-CTX-*`）由各类别文档按原编号续用，避免追溯断裂。
5. `testing/已知问题与解除条件.md` 中引用 `testing/Agent/功能测试/functional_feature_matrix.csv`、`testing/Agent/功能测试/...` 的历史条目（第 6、7 条）属于旧结构的失效引用；现状是台账并入各 `TEST_PLAN.md` 的用例行，不再存在独立的 Agent CSV 台账。根目录该文档不在本次调整范围，相关 Agent 问题状态以上方链接原文为准。
6. Git 检查只纳入本文档体系、对应类别脚本与脱敏文本证据；凭据、APK、JAR、`dist`、`node_modules`、Gradle 缓存和运行数据库文件不得提交。