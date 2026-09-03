# Agent 端测试资料总览

更新时间：2026-09-03

本目录是 Agent（后端 `/v2/agent`、Android/iOS APP、Web 协议对照）的**唯一**测试资料组织点。以测试类别为单位拆分规划文档、执行台账、脚本、日志、报告和原始证据，取代旧的单份《Agent 综合功能与性能测试方案》（已删除）与历史分类占位目录（observability 等已并入对应类别，本目录不再出现）。

## 一、目录结构

```text
testing/Agent/
├── README.md                  # 本文件：总览、分类说明、状态口径、证据规范、执行顺序、维护规则
├── 执行步骤临时文档.md         # 开发先行、部署核对、真实测试批次与收尾步骤
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
| 功能测试 | `F` | 会话/消息、61 个工具逐项、多工具链、Loop、SSE、草稿与二次授权、结果块、上下文压缩、多模态图片、Agent 生图、长期记忆、海报、Web 搜索、任务/通知/工作台 | 以业务终态、事件顺序、回答完整性、数据库变化为准；HTTP 200 不等于业务成功 |
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

开发、自动化验证、服务器部署和真实 App 测试的先后关系，以[执行步骤临时文档](执行步骤临时文档.md)为准。该文档是本轮执行编排入口；本文件和各分类 `TEST_PLAN.md` 继续作为状态、字段和证据规范。

## 三、状态口径

每条用例/记录的状态值**只允许**：

| 状态 | 含义 |
|---|---|
| `Passed` | 已执行，实际证据满足全部验收条件 |
| `Failed` | 已执行，实际行为不满足预期，有对应证据 |
| `Blocked` | 未执行，因为真实环境/Provider/数据库/设备条件不满足或未知 |
| `Deferred` | 已计划，尚未执行（本轮所有规划用例的初始状态） |

未执行内容不得写成 `Passed`。任何一条记录缺实际输入、工具调用链或证据路径时保持 `Deferred`，不能用相邻工具结果代替。

## 四、规划数量与执行覆盖口径

规划文档中的父场景、可执行用例和实际执行记录是三个不同概念，统计时必须分开：

| 统计项 | 计算方式 | 当前规划基线 | 说明 |
|---|---|---:|---|
| 工具静态映射率 | 已映射工具数 / 源码注册工具数 | 61 / 61 | 46 个 `READ_ONLY` + 15 个 `CREATE_ONLY`；只说明名称和类型已对齐，不等于运行通过 |
| 工具分支规划数 | 46 × 11 + 15 × 11 | 671 | 每个派生用例都必须使用独立 `test_id`，不能只引用父卡 |
| 父场景数 | 各 `TEST_PLAN.md` 中带 `AG-` 的规划行 | 以当前文件实际行数为准 | 父场景用于组织范围，不能直接计入执行覆盖 |
| 已执行覆盖率 | `Passed + Failed + Blocked` / 应执行的派生用例数 | 执行前为 0% | `Deferred` 不进入已执行分子 |
| 有效通过率 | `Passed` / (`Passed + Failed`) | 执行后计算 | `Blocked` 单独报告，不与失败混算 |
| 证据完整率 | 满足必填证据的已执行记录 / 已执行记录 | 执行前为 0% | 缺任一关键证据时不能标 `Passed` |

### 4.1 工具分支的独立编号

功能父卡采用如下派生编号，执行台账、日志目录和报告必须使用同一个编号：

```text
AG-F-TOOL-RO-001-B01 ... AG-F-TOOL-RO-001-B11
AG-F-DRAFT-CO-001-B01 ... AG-F-DRAFT-CO-001-B11
```

只读工具的 `B01` 至 `B11` 依次对应成功有数据、成功空数据、非法参数、未登录、无权限、跨 owner/store、数量或分页边界、重复请求、流式路径、清理、审计；需要比较 REST 与 SSE 时在编号后追加 `-REST` 或 `-SSE`。创建工具的 `B01` 至 `B11` 依次对应草稿生成、用户拒绝、用户确认、重复确认、确认失败、非法参数、未登录、无权限、跨 owner/store、并发确认、清理；审计字段是每一条分支的必填观察项，不另占编号。每一条都要记录真实输入、实际工具链、响应、数据差异和结果。

多工具、Loop、上下文、契约、安全、可靠性、数据和客户端场景仍使用自身的 `AG-*` 编号；同一父场景在非流式和流式执行时增加 `-REST`、`-SSE` 后缀，不能共用一条结果。

## 五、每条用例的最小字段

统一覆盖：测试目标、前置条件、输入提示词或请求参数、预期调用工具、预期工具调用顺序、Loop 循环与上下文压缩行为、预期 SSE 或普通响应、正式回答内容、数据库/草稿/审计/业务表变化、边界条件、验收条件、日志/脚本/原始证据位置。

| 字段 | 内容 | 必填 |
|---|---|---|
| `test_id` | 唯一编号，如 `AG-F-TOOL-RO-001`、`AG-S-010`、`AG-P-001` | 是 |
| `category_id` | `F/S/P/U/C/I/R/D/CLI` | 是 |
| `wave_id` | 执行波次，如 `20260828-agent-plan-01` | 是 |
| `environment` | 服务版本、数据库类型、Provider、设备条件 | 是 |
| `account_store_label` | 脱敏后的调用者、owner、store 标签 | 是 |
| `test_objective` | 测试目标 | 是 |
| `preconditions` | 前置条件（环境、账号、数据、Provider、设备） | 是 |
| `input` | 输入提示词或请求参数 | 是 |
| `operation` | 实际执行步骤、请求方式和重试/并发动作 | 是 |
| `expected_tools` | 预期调用工具（含允许的依赖工具） | 是 |
| `expected_order` | 预期工具调用顺序 | 是 |
| `loop_and_compaction` | 预计 Loop 轮数/终止点、是否会触发上下文压缩、检查点行为 | 是 |
| `model_input_summary` | 脱敏后的模型可见输入摘要、模型名和 wire API | 执行后是 |
| `expected_response` | 预期 SSE 事件序列或 REST 响应 | 是 |
| `expected_answer` | 正式回答内容要求（非空、可追溯、不得编造） | 是 |
| `model_output_summary` | 脱敏后的模型输出摘要、工具选择或正式回答摘要 | 执行后是 |
| `actual` | 实际状态码、工具调用、事件、回答和数据变化 | 执行后是 |
| `db_changes` | 数据库、草稿、审计与业务表变化（before/after） | 是 |
| `boundaries` | 边界条件（空数据、非法参数、权限、跨域、重复、并发、分页） | 是 |
| `acceptance` | 验收条件 | 是 |
| `evidence_path` | 日志、脚本与原始证据存放位置 | 是 |
| `cleanup_action` | 清理动作及其结果路径 | 执行后是 |
| `result` | `Passed/Failed/Blocked/Deferred` | 是 |

### 5.1 执行台账格式

各类别的执行台账放在对应的 `reports/` 目录，建议文件名为 `live_execution_ledger.csv`。一行只表示一个独立派生用例，不把多个账号、端点、工具或状态合并到一行。CSV 表头固定为：

```text
test_id,category_id,wave_id,environment,account_store_label,preconditions,input,operation,expected_tools,expected_order,loop_and_compaction,model_input_summary,expected_response,expected_answer,model_output_summary,actual,db_changes,boundaries,acceptance,evidence_path,cleanup_action,result
```

父卡只保留规划说明；当父卡还没有派生执行记录时，不能填入 `Passed`。每个类别报告应同时给出：父卡数量、派生应执行数量、Passed、Failed、Blocked、Deferred、证据完整率和未覆盖项。

## 六、证据与脱敏规范

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

## 七、执行顺序

1. 先按[执行步骤临时文档](执行步骤临时文档.md)完成 D0-D7：开发、自动化测试、构建、提交和版本核对。
2. 开发验收未完成时不得进入真实业务测试；失败项单独登记为 `Failed`，缺环境项登记为 `Blocked` 或 `Deferred`。
3. 服务器部署后重新核对 8220 服务、Provider、数据库、App 版本和公网地址，不能用本地服务代替目标服务。
4. 前置验证认证、会话、owner/store、清理权限；前置不满足记 `Blocked`。
5. 按 `功能/` 执行 46 个只读工具（非流式 + 流式）与 15 个创建工具（草稿/拒绝/确认/重复确认），其中 `image_generate` 的 Provider 调用只发生在确认后。
6. 执行多工具链、Loop 六种终态、SSE 取消/断线/重连、上下文压缩 14 个场景。
7. 执行 `契约/`、`集成/`、`数据/`、`安全/` 和 `可靠性/` 分类中的对应真实场景，再执行多模态图片、长期记忆、海报、Web 搜索与任务/通知/工作台功能。
8. 按 `客户端/` 在真实 Android/iOS App 中执行输入、点击和展示核对；Web 只做协议对照。
9. 按 `性能/` 先做单用户基线，再做并发、长会话、取消、Soak，并单独记录有效样本。
10. 汇总：回写各类台账的 `result`；未满足证据要求的项目不得填 `Passed`。

## 八、维护规则

1. 本目录是 Agent 测试唯一体系；不再新建第二份 Agent 方案、阶段报告或重复台账。
2. 新增工具、新增创建类草稿类型、压缩策略或 SSE 事件变更时，先更新 `代码事实基线.md` 与 `映射台账.md`，再补充对应类别用例。
3. 结果状态只能使用四值口径；历史旧状态（如部分完成、未闭环）必须在迁移时折算为 `Passed/Failed/Blocked/Deferred` 之一。
4. 旧的《Agent 综合功能与性能测试方案》已删除，其编号体系（`AG-F-*`、`AG-S-*`、`AG-P-*`、`AG-C-*`、`AG-U-*`、`AG-CTX-*`）由各类别文档按原编号续用，避免追溯断裂。
5. 根目录 `testing/已知问题与解除条件.md` 如仍保留旧目录文字，只作为历史记录处理，不作为本目录入口；当前台账并入各 `TEST_PLAN.md` 的用例行，不再存在独立的 Agent CSV 台账。本轮不修改根目录历史文档。
6. Git 检查只纳入本文档体系、对应类别脚本与脱敏文本证据；凭据、APK、JAR、`dist`、`node_modules`、Gradle 缓存和运行数据库文件不得提交。

## 九、阶段二 Wave 0 历史基线（2026-09-01，后续已更新）

| 项目 | 当前事实 | 状态 |
|---|---|---|
| 8220 运行环境 | 直连 `8.220.206.9` 成功；API 镜像为 `sxyq27-zhj-api:20260901T014000-agent-owner-bill-c012290b`，JAR 摘要为 `d9ac0b508fd55082905fd30608cf6221ff4e97e279496a08ce87a448ad318d77`，Flyway 为 V42，API/PostgreSQL/Redis/Nginx 运行正常 | `Passed` |
| Android 登录 | `emulator-5554` 上的 `com.zhihuiji.app` 已通过真实 UI 请求公网 `/v1/auth/login`；服务端返回 HTTP 422，未建立可验证 session | `Blocked` |
| 账号前置 | 四个指定账号后缀 `8111`、`8112`、`8113`、`8114` 均不存在；未重置密码、未创建账号、未准备业务夹具 | `Blocked` |
| Agent Wave 1-4 | 因没有可验证登录会话，SSE、工具、草稿、审计、取消、重连和性能链路未执行 | `Blocked` |
| 模型 | 运行时实际模型为 `gpt-5.6-luna`，目标模型为 `glm-5.3-flash`；目标模型和 Provider 实际上下文窗口未验证 | `Blocked` |
| iOS | 本轮不修改、不构建、不部署、不执行 iOS 测试 | `Deferred` |

本轮脱敏证据位于 `testing/Agent/客户端/artifacts/20260901-agent-phase2-wave0-AG-CLI-AND-P2-LOGIN-001/`。HTTP 422 仅证明请求已到达服务端并收到响应，不能作为客户端、Agent 或目标模型通过依据。

2026-09-01 10:20–10:25 补充执行 `AG-W0-ANON-ROUTE-002`：24 条 Agent 路由和 22 条管理员 GET 路由均返回 `401 application/json;charset=UTF-8`，根入口与 `/healthz` 也返回 `401`；8220 PostgreSQL 认证探针前后计数无变化。该报告只覆盖匿名拒绝边界，不改变登录、Provider、Wave 1–4 或各父场景的 `Blocked`/`Deferred` 状态，详见 `安全/reports/20260901-phase2-wave0-anonymous-route-matrix.md`。

## 十、阶段二 Wave 1 Android 首轮真实实测（2026-09-01）

本节为最新状态快照，更新并覆盖“账号不存在、无法登录”的前置判断；第九节保留为历史 Wave 0 记录。

| 用例 | 实际执行 | 状态 |
|---|---|---|
| `AG-CLI-AND-P2-LOGIN-003` | 使用 8220 现有启用账号之一，通过 Android UI 登录公网 API；进入真实首页、Agent 工作台和对话页；会话抽屉可打开；App 无崩溃 | `Passed` |
| `AG-CLI-AND-P2-RO-001` | 输入“查看当前商品列表”；SSE 和原生工具选择已发生，`product_catalog_lookup` 在服务端查询阶段失败；App 展示失败态和可重试入口 | `Failed` |
| `AG-CLI-AND-P2-RO-002` | 输入近 30 天现金流查询；`cashflow_summary_lookup` 在服务端查询阶段失败；App 展示失败态和无数据说明 | `Failed` |
| `AG-CLI-AND-P2-RO-003` | 输入当前门店查询；`store_info_lookup` 在服务端查询阶段失败；App 展示失败态和可重试入口 | `Failed` |

三条只读用例均真实到达 8220，并形成 SSE、工具选择、audit 和 Android 失败展示。三个 run 的共同错误为：`InvalidDataAccessApiUsageException: Query requires transaction be in progress, but no transaction is known to be in progress`。该问题已在商品、现金流和门店三个不同查询工具中复现，属于服务端工具执行事务边界问题；本轮未修改生产代码。

本轮可确认的 Agent 观测计数为：`agent_conversations=2`、`agent_messages=6`、`agent_run_audits=3`、`agent_run_audit_events=23`、`agent_drafts=0`、`agent_tasks=0`；正式业务表未观察到本轮写入。Android 会话抽屉在清理前显示无历史会话，之后执行 `pm clear com.zhihuiji.app` 成功并回到登录页。由于本轮结束时 SSH 公钥会话无法复用，服务器端删除会话后的 PostgreSQL 最终计数未再次读取，保留为未确认项。

证据报告：`testing/Agent/客户端/reports/20260901-agent-phase2-wave1-android-readonly.md`。设备截图和 UI 摘要位于 `testing/Agent/客户端/artifacts/20260901-agent-phase2-wave1-AG-CLI-AND-LOGIN-003/`；run 标识和脱敏数据库摘要见报告。报告不保存明文密码、密码哈希、Token、Cookie、Authorization 或完整认证载荷。

当前阶段二结论：登录前置已满足，Android 登录为 `Passed`；首轮三个只读 Agent 用例为 `Failed`；创建类工具、取消/断线/恢复、压缩、生图、性能和管理员带权限页面仍为 `Blocked` 或 `Deferred`，不能以本轮结果代替；iOS 为 `Deferred`。下一步应先修复并部署查询事务边界，再从 Android 重跑这三个只读用例；本轮未获授权切换 8220 运行模型，因此实际模型仍为 `gpt-5.6-luna`，目标模型 `glm-5.3-flash` 未验证。

## 十一、阶段二 Wave 2 Android 只读重测（2026-09-01）

三条首轮失败用例已在修复后的 8220 镜像上完成独立 Android 重测。每条均执行了真实 App 输入、UI 树定位发送按钮、点击发送、等待结果页、服务器审计序列核对、数据库前后计数核对和 App 会话删除；原首轮记录保持不变。

| 用例 | 结果 | 关键事实 |
|---|---|---|
| `AG-CLI-AND-P2-RO-001-RERUN-001` | `Passed` | `product_catalog_lookup` 完成；App 显示商品列表；`products=693` 无变化 |
| `AG-CLI-AND-P2-RO-002-RERUN-001` | `Passed` | `cashflow_summary_lookup` 完成；App 显示零流水事实；`finance_records=2661` 无变化 |
| `AG-CLI-AND-P2-RO-003-RERUN-001` | `Passed` | `store_info_lookup` 完成；App 显示当前门店事实；`stores=2`、`store_memberships=2` 无变化 |

三条请求均为 HTTP 200 `text/event-stream`，run 终态为 `completed`，事件序号连续，`tool_count=1`，没有 Android crash。重测报告为 `客户端/reports/20260901-agent-phase2-wave2-android-readonly-rerun.md`，每条用例的标准 `00–10` 证据目录位于 `客户端/artifacts/20260901-agent-phase2-wave2-rerun-AG-CLI-AND-P2-RO-00*-RERUN-001/`。清理后 `agent_conversations=4`、`agent_messages=14`；审计记录保留，正式业务表无新增。

本轮只验证了当前运行时 `gpt-5.6-luna/chat_completions`。目标 `glm-5.3-flash`、创建类工具、取消/断线/恢复、上下文压缩、生图、性能和 iOS 仍保持 `Blocked` 或 `Deferred`。

## 十二、2026-09-02 Android 真实点击补充

本轮继续按客户端计划在 `emulator-5554` 中执行真实 UI 点击，新增两条结果：

| 用例 | 结果 | 证据 |
|---|---|---|
| `AG-CLI-AND-P2-BGFG-001` | `Passed`：发送后请求仍在进行时按 Home 置后台，从最近任务点击 App 卡片恢复；结果完整且无重复；当前测试会话 `149` 已从 App 会话列表删除 | `testing/Agent/客户端/artifacts/20260902-agent-phase2-wave3-AG-CLI-AND-009-001/` |
| `AG-CLI-AND-P2-HISTORY-001` | `Passed`：从 Agent 首页点击既有会话 `138`，消息、执行步骤、回答与服务器 run trace 一致；没有新消息或数据变化 | `testing/Agent/客户端/artifacts/20260902-agent-phase2-wave4-AG-CLI-AND-005-001/` |

报告为 `testing/Agent/客户端/reports/20260902-agent-phase2-wave3-android-background-foreground-history.md`。本轮最终服务器计数为 `agent_conversations=4`、`agent_messages=10`、`agent_run_audits=18`、`agent_run_audit_events=258`、`agent_drafts=0`、`agent_tasks=0`；正式业务计数为 `products=693`、`finance_records=2661`、`stores=2`、`store_memberships=2`。`pm clear com.zhihuiji.app` 返回 `Success`，重启后为登录页，crash buffer 无匹配行。后台用例 raw SSE 正文未捕获，已按证据限制记录，不能用它声称完整 wire-level SSE 覆盖。

当前仍保持：运行时 `gpt-5.6-luna/chat_completions` 与目标 `glm-5.3-flash` 不一致，记为 `Blocked`；创建类工具、取消/断线、重连、压缩、生图、性能和 iOS 未完成。

## 十三、2026-09-02 Android 真实点击复核

新增一条聚焦客户端操作的复核记录：`AG-CLI-AND-P2-REALCLICK-001`。测试从已登录 App 的会话列表开始，依据 UI tree 点击“新建对话”、在 App 输入框输入提示词并点击“发送”。App 先展示处理中，随后展示 `store_info_lookup`、当前门店、正常状态和 1 名成员；App 进程日志记录公网 `/v2/agent/chat/stream` 返回 HTTP 200 `text/event-stream`。客户端点击流记为 `Passed`。

证据报告为 `testing/Agent/客户端/reports/20260902-agent-phase2-wave11-android-real-click.md`，标准目录为 `testing/Agent/客户端/artifacts/20260902-agent-phase2-wave11-AG-CLI-AND-P2-REALCLICK-001/`。历史列表未即时显示新卡片，因此没有猜测 conversation ID 或服务端删除结果；`pm clear com.zhihuiji.app` 返回 `Success`，重启后登录页可见，crash buffer 为 0 行。本轮服务端 run audit、原始 SSE 和数据库 before/after 未采集，保持 `Blocked`，不替代 Wave 2 的服务端闭环证据。

## 十四、2026-09-02 Wave 12 Android 创建草稿拒绝

`AG-CLI-AND-P2-DRAFT-REJECT-001-RERUN-001` 已在 Android App 中完成真实输入、发送、草稿确认弹窗和拒绝点击。App 真实调用 8220 `/v2/agent/chat/stream` 返回 HTTP 200 `text/event-stream`；随后显示 `create_product` 草稿，标题为“新建商品：阶段二真实点击商品”。拒绝动作调用 `/v2/agent/drafts/9/cancel` 并返回 HTTP 200，草稿列表显示“已取消（未执行）”。

客户端结果为 `Failed`：会话详情无法解析 `draft_card` 字段，拒绝后仍显示旧的 active 状态；独立草稿列表状态正确。服务端 run audit、原始 SSE 与 PostgreSQL before/after 因 SSH 公钥探针失败记为 `Blocked`，没有将这些缺口写成通过结论。证据报告和目录分别为 `testing/Agent/客户端/reports/20260902-agent-phase2-wave12-android-draft-reject.md` 与 `testing/Agent/客户端/artifacts/20260902-agent-phase2-wave12-AG-CLI-AND-P2-DRAFT-REJECT-RERUN-001/`。

清理已完成：取消草稿删除 HTTP 200；会话 `161` 删除首次超时，重试 HTTP 200 后从 App 列表消失；`pm clear com.zhihuiji.app` 返回 `Success`，重启回到登录页，crash buffer 为 0 行。运行模型仍为 `gpt-5.6-luna/chat_completions`，目标 `glm-5.3-flash` 仍为 `Blocked`；确认写入、重复/并发确认、生图、iOS 和性能未执行。

## 十五、2026-09-03 Wave 14 Android 创建草稿拒绝复测

本轮重新按客户端计划在 `emulator-5554` 的 `com.zhihuiji.app` 1.0.0 中完成创建商品草稿拒绝分支。最终提示词为“新增一个商品 商品编码是 EVALONLY20260903D 名称是阶段2 中文点击商品先生成草稿不要直接保存”；App 通过 `/v2/agent/chat/stream` 收到 HTTP 200 `text/event-stream`，显示覆盖式“操作确认”，再依据 UI tree 点击“拒绝”。

| 用例 | 结果 | 关键事实 |
|---|---|---|
| `AG-CLI-AND-P2-DRAFT-REJECT-001-RERUN-002` | `Passed` | run `5f72e106-5fce-4318-a112-058bf044b982`、conversation `169`、draft `12`；`create_product` 参数包含编码和名称；17 个审计事件连续；App 会话详情和草稿列表均显示 `cancelled`；正式商品数 `693` 无变化 |
| 同一 Wave 首次无分隔输入尝试 | `Failed` | run `45ef8240-b523-4891-995a-fd879adc777b` 选择 `create_product` 但工具参数 `code` 为空，返回 `TOOL_ARGUMENTS_INVALID`，未生成草稿 |

数据库计数为：发送前 `agent_conversations=11`、`agent_messages=27`、`agent_run_audits=40`、`agent_run_audit_events=482`、`agent_drafts=0`、`products=693`、`finance_records=2661`；拒绝后清理前为 `12/29/41/499/1/693/2661`；App 删除草稿和两条测试会话后为 `10/25/41/499/0/693/2661`。`pm clear com.zhihuiji.app` 返回 `Success`，重启最终显示登录页，crash buffer 为 0 行。

报告为 `testing/Agent/客户端/reports/20260903-agent-phase2-wave14-android-draft-reject-rerun-002.md`，证据目录为 `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave14-AG-CLI-AND-P2-DRAFT-REJECT-RERUN-002/`。本轮未执行确认写入、重复/并发确认、生图、性能或 iOS；运行时仍为 `gpt-5.6-luna/chat_completions`，目标 `glm-5.3-flash` 继续为 `Blocked`。

## 十六、2026-09-03 阶段二 Wave 17 Android 创建草稿确认

本轮在 `emulator-5554` 的 `com.zhihuiji.app` 1.0.0 中，按 UI tree 定位并真实点击发送和确认按钮，连接 8220 公网 API。提示词包含商品编码 `9172603842` 和名称“阶段确认商品”，App 真实调用 `/v2/agent/chat/stream` 返回 HTTP 200 `text/event-stream`，服务端 run `f3cc466a-cef1-4d26-84af-49d4dd0d47fe`、conversation `175` 生成 draft `14`，草稿字段中的编码和名称正确。

| 用例 | 结果 | 关键事实 |
|---|---|---|
| `AG-CLI-AND-P2-DRAFT-CONFIRM-001` | `Failed` | 真实点击“允许一次”后，App 调用 `/v2/agent/drafts/14/confirm` 返回 HTTP `422`，界面显示参数校验失败；没有把确认写入或正式商品创建写成通过 |
| Wave 17 拒绝与清理 | `Passed` | 真实点击“拒绝”后 draft `14` 为 `cancelled`，App 显示未执行任何业务写入；Wave 17 会话 `173/174/175` 和测试草稿经 App 清理，最终目标编码、目标草稿和目标会话均为 0，`products=693` 保持不变 |
| 分类/单位数据前置 | `Blocked` | PostgreSQL 核对为 `category_count=0`、`unit_count=0`、`products_with_category_id=0`、`products_with_unit_id=0`；商品历史显示文本存在，但没有可供确认请求使用的分类/单位 ID |
| 确认成功、重复确认、并发确认 | `Blocked` | 需先补齐当前 owner 可用的分类和单位记录并重新生成草稿，或调整并部署确认契约；条件满足后再逐项执行真实 Android 流程 |

Wave 17 最终计数为 `agent_conversations=10`、`agent_messages=25`、`agent_drafts=0`、`agent_run_audits=47`、`agent_run_audit_events=545`、`products=693`、`finance_records=2661`。服务前置证据显示 API、PostgreSQL、Redis 和 Nginx 正常，Android 清理后回到登录页且 crash buffer 为 0 行。详细证据目录为 `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave17-AG-CLI-AND-P2-DRAFT-CONFIRM-001/`，其中 `528-server-before-confirm-evidence.txt`、`539-server-after-confirm-failure-evidence.txt`、`547-reference-category-unit-check.txt`、`572-db-after-cleanup-conversations.txt` 和 `582-db-final-recount.txt` 是本轮关键摘要。

运行时实际模型仍为 `gpt-5.6-luna/chat_completions`，目标 `glm-5.3-flash` 未验证，记为 `Blocked`。本轮未修改源码、数据库、线上服务、账号、密码或证据文件；在前置条件满足前不继续确认成功、重复确认和并发确认测试。

## 十七、2026-09-04 阶段二 Wave 18 修复回归与 APK 一致性

在公网入口恢复前，对当前 Android Agent 修复做了可重复的代码级检查：`./gradlew :feature:agent:testDebugUnitTest --rerun-tasks --console=plain` 实际执行 115 个任务并通过；`./gradlew :app:assembleDebug --rerun-tasks --console=plain` 实际执行 648 个任务并通过。新 APK 安装到 `emulator-5554`，宿主构建包和设备安装包 SHA-256 均为 `f7b92adb686c54be0450038fb854375a256f8f48f34ef61e6b07c7f19c902495`，UI tree 显示干净登录页，crash buffer 为 0 行。

本轮真实运行前置仍为 `Blocked`：`zhj-api.sxyq27.online` 和强制直连 8220 的 HTTPS 均在 TLS 阶段失败，8220 API、PostgreSQL、Redis 虽正常运行，但没有公网 `443` 监听；124 的 `/zhj-api/` 为 `410`。本轮没有输入账号、点击登录或调用 Agent，也没有产生数据库变化。详细记录见 `testing/Agent/客户端/reports/20260903-agent-phase2-wave18-android-create-customer-confirm.md` 的 2026-09-04 记录和 `testing/Agent/客户端/artifacts/20260904-agent-phase2-wave18-android-create-customer-confirm-005/`。
