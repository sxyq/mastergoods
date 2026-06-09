# 44 AI 助手功能审查门禁

> 状态：后续审查 / 修改 AI 助手的执行版需求门禁
> 日期：2026-06-09
> 关系：本文件是 `43-ai-assistant-requirements.md` 的执行清单版；若两者冲突，以当前源码、真实运行证据和本文件门禁为准，再同步修正 `43`。

## 1. 审查目标

AI 助手必须像真实 ChatGPT-like agent：先理解用户问题，创建真实 run，调用当前账号可访问的真实业务工具，边生成回答边自然呈现工具结果、Markdown 和图表，并保留可审计证据。任何模拟数据、模拟过程、假流式、假任务、假通知、默认报表看板或本地拆字动画，都不能作为生产体验通过。

本文件只定义 AI 助手审查门禁，不声明全局 UI 一比一还原已经完成。全局 UI 仍需逐页设计稿截图、真机截图和交互证据。

## 2. 必须保留的产品体验

| 编号 | 体验要求 | 失败条件 |
|---|---|---|
| UX-AI-001 | AI 首页是干净提问入口，只说明可按问题查询真实数据、生成 Markdown / 表格 / 图表 / 依据。 | 首屏默认展示销售额、KPI、今日经营摘要、风险看板、销售趋势、净现金流、默认统计图或报表页重复内容。 |
| UX-AI-002 | 用户消息、AI 回复、工具过程、结构化结果、错误 / 降级状态必须一眼可分辨。 | 所有内容同色同层级，用户无法区分“回答”“工具提示”“结果块”“错误”。 |
| UX-AI-003 | 工具提示必须真实、短暂、可自动收敛。 | 工具已完成后仍长期显示“查询中”；没有后端事件却本地展示固定步骤；失败工具被显示成成功。 |
| UX-AI-004 | 结构化结果不能抢在首段回答前作为主内容展开。 | 用户看到完整表格 / 图表先出现，等一会儿才出现 AI 回答。首段回答前只能显示轻量“已取得真实结果，正在组织回答”类 pending 提示。 |
| UX-AI-005 | Markdown 是一等内容能力，标题、列表、表格、引用、代码、链接、粗体、斜体、行内代码必须可读。 | Markdown 被纯文本糊成一团；链接 URL 丢失；代码尾部空白被吞；表格错列或崩溃后静默消失。 |
| UX-AI-006 | 图表只能渲染后端真实 result block。 | Android 为了美观补示例数据、0 值假图、默认排行、默认趋势。 |
| UX-AI-007 | 停止生成必须优先调用服务端 cancel；失败时诚实提示本机已停止接收但服务端取消未确认。 | 本地取消后伪造 `run_cancelled`，或取消失败仍显示“已成功取消”。 |
| UX-AI-008 | 清空聊天或离开流式收集时不得只清本地 UI；有 active run 时必须释放 SSE 连接并请求服务端 cancel。 | 用户点清空后 UI 消失，但后端 run 继续执行、模型继续消耗或稍后写入任务 / 通知。 |
| UX-AI-009 | 安全拦截必须是明确终态。 | 高风险问题触发 `safety_check_blocked` 后 assistant 仍显示流式、等待、工具查询中或可继续停止。 |

## 3. 后端真实 Agent 门禁

| 编号 | 后端要求 | 必需证据 |
|---|---|---|
| BE-AI-001 | `/v2/agent/chat` 与 `/v2/agent/chat/stream` 是唯一生产验收入口；admin smoke / demo seed 不能作为 AI 能力证据。 | HTTP 请求 / 响应、controller 路由、profile 隔离测试。 |
| BE-AI-002 | 每次聊天必须创建 `run_id`，并贯穿 HTTP、SSE、tool event、audit、Android RunTrace。 | 同一 `run_id` 的 raw SSE、run audit JSON、Android UI tree。 |
| BE-AI-003 | Planner 只能选择白名单只读工具；关键词兜底只能用于选工具，不能生成最终事实结论。 | plan 事件、tool name 白名单、回答引用工具证据。 |
| BE-AI-004 | 所有工具必须 owner-scoped，关键金额 / 数量 / 排名必须来自当前 owner 数据。 | repository 查询条件、两账号隔离测试、真实 DB / HTTP 证据。 |
| BE-AI-005 | 工具失败必须发送 `tool_failed`，最终回答必须说明部分失败，不能用 0 或空列表替代失败结果。 | 强制工具失败测试、raw SSE、最终回答截图。 |
| BE-AI-006 | 规则摘要 / 模型不可用路径不得发送 `answer_delta` 假流式。 | `AGENT_LLM_ENABLED=false` 抓包，确认无 `answer_delta(delta_source=model_stream)`。 |
| BE-AI-007 | 只有 provider streaming 回调可以发送 `answer_delta(delta_source=model_stream)`。 | provider enabled 抓包、模型配置、run audit、源码路径。 |
| BE-AI-008 | 服务端补充查询边界、截断、部分失败说明时，必须标记为 `delta_source=server_notice`，不得伪装模型 token。 | raw SSE 和 Android 来源标签截图。 |
| BE-AI-009 | result block 必须包含真实来源、查询窗口、截断 / 空态 / 错误信息。 | `result_blocks` JSON、evidence card、Android 渲染截图。 |
| BE-AI-010 | run audit 写入失败不能阻塞 SSE，也不能污染业务错误；审计有损必须暴露 warning。 | 单测、audit response、warning 字段。 |

## 4. Android 生产路径门禁

| 编号 | Android 要求 | 必需证据 |
|---|---|---|
| AND-AI-001 | Chat 主链路必须走 `AgentV2Repository.chatStream()` 和 `AgentSseClient`，不得本地构造 AI 回答。 | 代码路径、禁止项扫描。 |
| AND-AI-002 | 不得使用 `delay` / `timer` / `substring` / `chunkSize` 拆完整回答制造打字机效果。 | 禁止项扫描逐项解释；若命中只能用于合并服务端 delta 或普通字符串裁剪。 |
| AND-AI-003 | `AnswerCompleted` / `RunCompleted` 只能补全最终状态，不能触发本地完整回答 reveal。 | ViewModel 测试、录屏或连续截图。 |
| AND-AI-004 | 早到的 result block 在首段回答前只能进入 pending 状态，不展开明细。 | ViewModel 单测、真机截图。 |
| AND-AI-005 | Markdown 解析失败必须降级为安全可读文本，不得丢正文。 | Markdown 单测、真实回答截图。 |
| AND-AI-006 | 已知 result block 解析失败、未知 block、坏 SSE 帧、空图表必须显示错误 / 空态卡，不得静默吞掉。 | Renderer 单测、坏块截图。 |
| AND-AI-007 | AI 首页远程同步失败时不得显示“已同步”或默认能力承诺，只保留对话入口和真实失败提示。 | 断网 / 403 / 500 截图、UI tree。 |
| AND-AI-008 | SSE Flow 取消必须传递到底层 OkHttp call，包含 execute 阻塞和 response body 读取阶段。 | `AgentSseClientCancellationTest`、真机停止 / 清空抓包。 |
| AND-AI-009 | `SafetyCheckBlocked` 必须关闭 message streaming、关闭停止按钮并写入错误 / RunTrace 安全结果。 | ViewModel 单测、真机高风险问题截图。 |

## 5. 禁止项扫描门禁

每次 AI 助手审查必须扫描以下词，并解释所有命中是否在生产 AI 链路：

- `mock`
- `demo`
- `fake`
- `sample`
- `placeholder`
- `模拟`
- `演示`
- `假数据`
- `delay`
- `timer`
- `substring`
- `chunkSize`

自动失败规则：

- 命中项用于生成 AI 回答、工具过程、任务、通知、草稿、图表、表格、统计值或模型流式 token。
- `substring` / `chunkSize` 用于本地拆完整回答，或规则摘要分块冒充模型输出。
- Android 为 result block 补示例数据、默认趋势、默认排行或 0 值假图。
- admin smoke、demo seed、local 初始化数据被当作生产 AI 验收证据。

允许但必须说明：

- 输入框 `placeholder` 文案。
- UI 合帧节流，例如只合并服务端真实 `answer_delta`。
- 普通摘要裁剪，例如标题、日志片段、错误摘要 `take(160)`。
- local-only 测试数据，且被 profile / 测试名 / 路径证明不进入生产 AI 入口。

推荐命令：

```bash
python3 tools/ai_agent_forbidden_scan.py --output docs/acceptance-evidence/ai-agent/<timestamp>-forbidden-scan.md
```

报告状态为 `pass-for-static-scan` 时，只说明静态扫描命中已经逐项解释；它不替代真实 HTTP、SSE、审计、provider `model_stream`、Android 截图、UI tree 或性能证据。报告状态为 `fail-needs-review` 时，不得把 AGT-P0-001 / AGT-P0-005 / UX-AI-004 标为通过。

## 6. P0 证据包

每个真实问题必须独立归档一个证据包，至少包含：

- `00-request.json`：问题、账号、base URL、环境、模型配置摘要。
- `01-http-response.json`：非流式或 stream 建立响应。
- `02-raw-sse.log`：原始 SSE，保留事件顺序。
- `03-run-audit.json`：后端 run audit。
- `04-tool-results.json`：工具名称、输入摘要、查询窗口、返回数、总数、截断、错误。
- `05-android-chat.png`：真机对话截图。
- `06-android-ui-tree.xml`：真机 UI tree。
- `07-logcat.txt`：关键 logcat。
- `08-gfxinfo.txt`：帧耗时 / jank 证据。
- `09-forbidden-scan.md`：禁止项扫描和解释。
- `10-conclusion.md`：按本文件编号逐项给 `pass` / `partial` / `fail`。
- `11-cancel-evidence.md`：停止生成和清空聊天的 HTTP/SSE 抓包、run audit、active run 收尾、Android UI 提示。
- `12-safety-block-evidence.md`：安全拦截 raw SSE、Android 错误终态截图、RunTrace safety result。

接口侧取消证据可先用以下命令生成；它不会替代 Android 停止按钮 / 清空聊天截图、UI tree 或 logcat：

```bash
TOKEN="<bearer-token>" ./tools/ai_agent_evidence_capture.sh cancel-test
```

接口侧安全拦截证据可先用以下命令生成；它不会替代 Android 错误终态截图、停止按钮消失证据或 RunTrace safety result：

```bash
TOKEN="<bearer-token>" ./tools/ai_agent_evidence_capture.sh safety-test
```

Android 侧场景证据可用以下命令分别采集；这些命令只证明设备可见状态，仍必须和对应 HTTP/SSE/audit 证据包对齐：

```bash
python3 tools/capture_ai_chat_device_evidence.py --scenario safety-block
python3 tools/capture_ai_chat_device_evidence.py --scenario stop
python3 tools/capture_ai_chat_device_evidence.py --scenario clear
```

没有 Android 截图和 UI tree 时，只能证明接口侧，结论最高为 `partial`。没有 provider `model_stream` 抓包时，ChatGPT-like 真模型流式体验最高为 `partial`。

## 7. 三个必测真实问题

P0 每次回归至少覆盖：

- “哪些商品库存不足，风险最高？”
- “哪些客户还有应收款，金额是多少？”
- “最近销售、采购和财务情况怎么样？”

每个回答都必须说明：

- 查询的是哪个账号 / owner 范围。
- 调用了哪些工具。
- 查询窗口和排序 / limit。
- 关键数字的 evidence 来源。
- 是否截断。
- 哪些工具失败或未接入。
- 建议动作只是建议，不是已执行写操作。

## 8. 性能门禁

不改变 UI 视觉的性能优化必须用证据证明，不允许只凭主观“更流畅”：

| 指标 | P0 要求 |
|---|---|
| 首事件耗时 | 记录 `run_started` 或第一个 SSE 事件耗时。 |
| 首工具耗时 | 记录第一个 `tool_started` / `tool_completed`。 |
| 首回答耗时 | 记录第一个 `answer_delta(model_stream)` 或 `answer_completed`。 |
| 首结果块耗时 | 记录第一个正式 result block 可见时间。 |
| 总耗时 | 记录 run completed / failed / cancelled。 |
| Android 帧表现 | 至少记录 `gfxinfo` 或等价 frame timing；高刷新设备需确认无明显长帧。 |
| 审计状态 | 记录 emitted event count、persisted event count、dropped / failed audit write。 |

性能优化不得改变视觉层级、颜色、布局、动画语义或内容顺序；任何视觉变化都必须回到 UI 设计验收。

采集 AI 后端流式耗时并附加 Android 帧统计时，使用：

```bash
python3 tools/ai_agent_performance_evidence.py --android-gfxinfo docs/acceptance-evidence/ai-agent/<device-package>/09-gfxinfo.txt
```

`--android-gfxinfo` 会解析 `dumpsys gfxinfo` 的 total frames、janky frames、P50/P90/P95/P99、missed vsync 和 slow UI thread；它仍不替代 Android 首次可见耗时、provider `model_stream` 抓包或真机录屏。

## 9. 审查结论格式

每次审查必须输出：

| 项 | 内容 |
|---|---|
| 代码范围 | 本轮读 / 写文件列表 |
| 运行环境 | backend profile、base URL、Android 设备、模型配置摘要 |
| 需求结果 | 按本文件 ID 列 `pass` / `partial` / `fail` |
| 关键证据 | HTTP、SSE、audit、截图、UI tree、测试命令 |
| 失败项 | 失败原因、用户影响、修复建议 |
| 不可替代证据 | 明确哪些结论还缺真机、provider、生产或大数据量证据 |
| 多 agent 状态 | Android / 后端 / 验证 agent 是否参与；若额度不足，记录失败原因 |

不能用“已实现”“看起来正常”“单测通过”替代上述证据。
