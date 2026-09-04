# Agent 客户端联调测试规划（客户端）

更新日期：2026-09-04。Android 与 iOS 使用相同的服务端断言，客户端分别记录各自版本、设备标签与展示状态；Web 仅作协议与展示对照。登录必须走 APP 正常登录流程，测试人员不手工提取/复制 Cookie、Session Token、Authorization 或完整认证载荷。每个平台使用独立 `test_id`，不能用 Android 结果代表 iOS，也不能用 Web 协议解析代表真实设备展示。

## 一、执行前提

| 端 | 前提 | 缺失时 |
|---|---|---|
| Android | 真机/模拟器 + 安装测试版 APK；ADB 可用 | 记 `Blocked` |
| iOS | 真实设备或签名环境 + 本机 Xcode | 记 `Blocked` |
| Web | 后端服务可达 | 记 `Blocked`（仅协议对照） |

## 二、核心流程（AG-CLI-*，每端独立记录）

| 编号 | 流程 | 服务端事实 | 客户端验收 |
|---|---|---|---|
| AG-CLI-AND-001 | Android 登录与会话列表 | 200 会话列表、current owner/store | 会话进入/恢复；记录 APK、设备和服务版本 |
| AG-CLI-AND-002 | Android 单只读工具流式 | run 事件序列与 DB 事实 | 增量不重复不丢字；工具过程展示；回答完整 |
| AG-CLI-AND-003 | Android 多工具 + 图表 | 多工具链、result_block | 图表与 facts 一致；空数据空状态 |
| AG-CLI-AND-004 | Android 草稿确认/拒绝弹窗 | draft_created、confirm/cancel 结果 | 覆盖式弹窗明确“草稿→确认/拒绝→正式写入”；拒绝后正式表无变化 |
| AG-CLI-AND-005 | Android 历史恢复 | 历史消息 run_id/回答/工具过程 | 服务器事实与历史一致；排序正确 |
| AG-CLI-AND-006 | Android 取消与断线 | run_cancelled/断线终态 | 停止后无增量；断线不重复展示；可恢复重连 |
| AG-CLI-AND-007 | Android 压缩事件展示 | context_compacted | 展示条数/原因；不展示敏感原文 |
| AG-CLI-AND-008 | Android 错误与重试 | 401/403/409/422/429/5xx 或 SSE error | 分别展示登录失效/无权限/冲突/参数/限流/服务异常；可重试 |
| AG-CLI-AND-009 | Android 后台/前台切换 | 运行状态 | 恢复后状态一致；无重复事件 |
| AG-CLI-IOS-001 | iOS 登录与会话列表 | 200 会话列表、current owner/store | 会话进入/恢复；记录签名、设备和服务版本 |
| AG-CLI-IOS-002 | iOS 单只读工具流式 | run 事件序列与 DB 事实 | 增量不重复不丢字；工具过程展示；回答完整 |
| AG-CLI-IOS-003 | iOS 多工具 + 图表 | 多工具链、result_block | 图表与 facts 一致；空数据空状态 |
| AG-CLI-IOS-004 | iOS 草稿确认/拒绝弹窗 | draft_created、confirm/cancel 结果 | 覆盖式弹窗明确“草稿→确认/拒绝→正式写入”；拒绝后正式表无变化 |
| AG-CLI-IOS-005 | iOS 历史恢复 | 历史消息 run_id/回答/工具过程 | 服务器事实与历史一致；排序正确 |
| AG-CLI-IOS-006 | iOS 取消与断线 | run_cancelled/断线终态 | 停止后无增量；断线不重复展示；可恢复重连 |
| AG-CLI-IOS-007 | iOS 压缩事件展示 | context_compacted | 展示条数/原因；不展示敏感原文 |
| AG-CLI-IOS-008 | iOS 错误与重试 | 401/403/409/422/429/5xx 或 SSE error | 分别展示登录失效/无权限/冲突/参数/限流/服务异常；可重试 |
| AG-CLI-IOS-009 | iOS 后台/前台切换 | 运行状态 | 恢复后状态一致；无重复事件 |
| AG-CLI-WEB-001 | 协议对照 | 同上服务端事实 | 事件/Schema 与移动端一致；ID 全程字符串/BigInt，禁止 `Number()` |
| AG-CLI-AND-010 | Android 生图草稿确认 | `image_generate` 产生 `draft_card`；覆盖式确认/拒绝；确认后显示结果或安全错误 | 拒绝不显示已保存；确认后结果与 `image_url/revised_prompt` 一致；Provider/设备缺失记 `Blocked` | 输入、UI 树/截图、SSE/REST 摘要、audit、清理、状态 |
| AG-CLI-IOS-010 | iOS 生图草稿确认 | 同上，使用独立 iOS `test_id` | 不用 Android/Web 结果替代；确认、拒绝、失败状态与服务端一致；设备缺失记 `Blocked` | 输入、UI 树/截图、响应摘要、audit、清理、状态 |

## 三、展示断言（服务端→客户端一致性）

| 展示对象 | 服务端事实 | 客户端验收 |
|---|---|---|
| 工具过程 | tool_started/completed/failed/skipped、名称、状态、顺序 | 显示进行中/完成/失败；不把工具名当正式正文 |
| 正式回答 | answer_delta 聚合、answer_completed、terminal_status | 流式增量无重复无丢字；失败/取消不显示成功 |
| 表格/KPI | result_block.block_type 与 data | 列名、数值、排序、总数一致；空数据空状态 |
| 图表/趋势 | 上游 facts、时间桶、标签、数值 | 只来自真实 facts；无数据不画假趋势 |
| 搜索摘要 | web_search_lookup 的 title/summary/URL/citation | 展示标题、摘要、来源链接；Provider 不可用显示错误 |
| 草稿确认 | draft_id/type/title/content/status | 覆盖式弹窗；确认/拒绝明确；未确认无“已保存”文案 |
| 错误 | HTTP/SSE 错误与终态 | 分状态展示；可恢复状态提供重试 |
| 取消/断线 | run_cancelled、断线终态 | 停止后不增量；不可恢复明确终止 |
| 生图结果 | 确认后的 `image_url` 或受控 data URL、`revised_prompt` | 只在确认成功后展示；不显示 key、完整 b64 或内部错误；结果失败可重试 |

## 四、证据存放

`客户端/artifacts/<日期>-<波次>-<用例>/`：请求摘要（脱敏）、UI 树/截图（有设备时）、原始 SSE（来自服务端观察）；`客户端/logs/`（logcat/Console 脱敏）；`客户端/reports/`（设备观测报告）。缺少设备时不创建伪证据，仅记录 `Blocked`。纯 Kotlin/Swift 单元测试结果只能写入单元类别，不能充当本类别真实设备结果。

## 阶段二 Wave 0 历史实测记录

| 用例 | 平台与范围 | 实际事实 | 状态 |
|---|---|---|---|
| `AG-CLI-AND-P2-LOGIN-001` | Android App 真实登录 | 正常登录流程发出的请求已到达 `8.220.206.9` 上的公网 API，接口返回 HTTP 422。该响应是实际服务端结果，不能记为 `Passed`。 | `Blocked` |
| `AG-CLI-IOS-P2-LOGIN-001` | iOS 登录 | 本轮不执行 iOS 实测。 | `Deferred` |

Wave 0 的账号核查显示，四个指定账号后缀 `8111`、`8112`、`8113`、`8114` 均不存在。此次未重置密码、未创建账号，也未执行其他测试数据准备，因此无法取得可验证的登录会话；Wave 1-4 未进入。

本轮运行模型实际为 `gpt-5.6-luna`，目标模型为 `glm-5.3-flash`，两者不一致，记录为环境差异。该差异不能作为目标模型已验证的依据。

脱敏证据目录：`testing/Agent/客户端/artifacts/20260901-agent-phase2-wave0-AG-CLI-AND-P2-LOGIN-001/`。

以上记录保留为 2026-09-01 凌晨的历史前置探针。随后用户授权使用 8220 现有启用账号进行实测，账号前置已重新核对；不得用本节旧状态覆盖下面的最新结果。

## 2026-09-01 Wave 0 增量复核

| 用例 | 平台与范围 | 实际事实 | 状态 |
|---|---|---|---|
| `AG-CLI-AND-P2-ENV-002` | Android 环境与 APK | `Zhihuiji_API34` 已启动为 `emulator-5554`；`com.zhihuiji.app` 1.0.0 已安装；默认 API 为 `https://zhj-api.sxyq27.online/`；登录页 UI 树、截图和空崩溃缓冲已保存 | `Passed` |
| `AG-CLI-AND-P2-SERVER-002` | 8220 服务前置 | 直连 root SSH、API/PostgreSQL/Redis/Nginx、Flyway V42 和公网匿名拒绝均已核对；主机 `aegis.service` 为 failed，未执行修复 | `Passed` |
| `AG-CLI-AND-P2-PROVIDER-002` | Provider 目标配置 | 运行模型为 `gpt-5.6-luna`，目标模型为 `glm-5.3-flash`，两者未对齐 | `Blocked` |
| `AG-CLI-AND-P2-LOGIN-002` | Android 正常登录 | 本轮停留在登录前页面，没有已确认的测试账号和密码，未提交登录请求 | `Blocked` |

本次增量证据目录：`testing/Agent/客户端/artifacts/20260901-phase2-wave0-AG-CLI-AND-PRELOGIN-001/`。在账号和 Provider 前置满足前，不进入 Wave 1-4；不创建账号、不重置密码、不从服务器读取认证材料。

证据边界：该目录是登录前的设备与 UI 采集，不包含登录后的 HTTP、SSE、工具、audit 或数据库业务证据。原始 `03-login-ui.xml` 尾部混入 `/dev/tty` 命令状态文本，不能作为 XML 解析输入；登录页结构以 `04-login-ui-summary.txt` 和 `05-login-screen.png` 为准。当前 UI XML 已重新采集并通过 `xmllint`，未写入版本库。

## 2026-09-01 Wave 1 Android 首轮真实实测

| 用例 | 前置与输入 | 实际工具/客户端观察 | 数据与清理 | 状态 |
|---|---|---|---|---|
| `AG-CLI-AND-P2-LOGIN-003` | 8220 现有启用账号；`emulator-5554`；`com.zhihuiji.app` 1.0.0 | Android UI 登录成功；首页真实门店信息加载；进入 Agent 工作台和对话页；无 crash | 未创建业务数据；本地清理后回到登录页 | `Passed` |
| `AG-CLI-AND-P2-RO-001` | 已登录会话；输入“查看当前商品列表” | `product_catalog_lookup` 已被原生工具链选择并执行；SSE/审计可关联；工具查询报事务异常；App 展示失败态 | `agent_conversations=2`、`agent_messages=6`、`agent_run_audits=3`、`agent_run_audit_events=23`、`agent_drafts=0`、`agent_tasks=0` 为本轮可确认观测；正式业务表无本轮写入 | `Failed` |
| `AG-CLI-AND-P2-RO-002` | 已登录会话；输入近 30 天现金流查询 | `cashflow_summary_lookup` 已被原生工具链选择并执行；SSE/审计可关联；工具查询报事务异常；App 展示失败态和无数据说明 | 同一批次计数；未观察到正式业务表写入 | `Failed` |
| `AG-CLI-AND-P2-RO-003` | 已登录会话；输入当前门店查询 | `store_info_lookup` 已被原生工具链选择并执行；SSE/审计可关联；工具查询报事务异常；App 展示失败态和可重试入口 | 同一批次计数；未观察到正式业务表写入 | `Failed` |

三个失败用例的共同错误为：`InvalidDataAccessApiUsageException: Query requires transaction be in progress, but no transaction is known to be in progress`。这是 8220 服务端工具执行事务边界的可复现失败，不是登录失败或 Android 解析失败。对应 run 标识、工具摘要、数据库摘要和 UI 证据见 `客户端/reports/20260901-agent-phase2-wave1-android-readonly.md`。

本轮结束时 Android 会话抽屉显示无历史会话；随后 `pm clear com.zhihuiji.app` 返回 `Success`，App 重启后回到登录页。服务器端会话失效已执行，但 SSH 公钥会话在最终复核时无法复用，删除会话后的 PostgreSQL 最终计数未确认；该清理核对项不能标记为 `Passed`。

本轮未继续创建类工具、草稿确认、生图、取消/断线、重连、压缩或性能用例。下一步是修复并部署查询事务边界，再重跑 `AG-CLI-AND-P2-RO-001` 至 `003`；运行模型切换到 `glm-5.3-flash` 仍需单独授权。

## 2026-09-01 Wave 2 Android 只读重测

本节新增独立重测记录，不修改上节首轮 `Failed` 结果。三条用例均从 Android App 真实输入并点击发送，服务端使用已部署镜像 `1429d657`。

| 用例 | 实际工具与运行 | App 展示与服务端事实 | 数据与清理 | 状态 |
|---|---|---|---|---|
| `AG-CLI-AND-P2-RO-001-RERUN-001` | `product_catalog_lookup`；run `0fedfbad-8a32-415a-a1f0-f05539c4fae9`；conversation `140` | HTTP 200 SSE；`tool_started → tool_completed`；显示商品列表和来源 | `products=693` 前后不变；会话经 App 删除 | `Passed` |
| `AG-CLI-AND-P2-RO-002-RERUN-001` | `cashflow_summary_lookup`；run `afd437b2-f06f-454b-a6c8-b30d2d3a5ee0`；conversation `141` | HTTP 200 SSE；显示收入、支出、净现金流和记录数，空流水提示一致 | `finance_records=2661` 前后不变；会话经 App 删除 | `Passed` |
| `AG-CLI-AND-P2-RO-003-RERUN-001` | `store_info_lookup`；run `721a6483-067f-400a-953c-769f047daf69`；conversation `142` | HTTP 200 SSE；显示当前门店正常状态和 1 名成员，来源一致 | `stores=2`、`store_memberships=2` 前后不变；会话经 App 删除 | `Passed` |

重测证据报告：`客户端/reports/20260901-agent-phase2-wave2-android-readonly-rerun.md`。每条用例均有独立标准 `00–10` 证据目录，包含脱敏输入、HTTP 摘要、事件序列、工具轨迹、run audit、数据库前后计数、UI 摘要、截图、脱敏 logcat、crash buffer、清理和结论。最终服务器计数为 `agent_conversations=4`、`agent_messages=14`、`agent_run_audits=10`、`agent_run_audit_events=133`、`agent_drafts=0`、`agent_tasks=0`；正式业务表无新增。

本轮使用的实际运行模型仍为 `gpt-5.6-luna/chat_completions`，目标 `glm-5.3-flash` 保持 `Blocked`。创建类工具、取消/断线、重连、上下文压缩、生图、性能和 iOS 没有执行，不能由只读重测代替。

## 2026-09-02 Wave 3/4 Android 真实点击

本轮使用 `emulator-5554` 上的 `com.zhihuiji.app` 1.0.0，直接连接 8220 公网 API。所有操作均先从 UI tree 取得 bounds，再执行模拟器点击；不使用脚本请求代替 App 展示。

| 用例 | 实际事实 | 数据与清理 | 状态 |
|---|---|---|---|
| `AG-CLI-AND-P2-BGFG-001` | App 输入并点击发送；请求未完成时按 Home 置后台；从最近任务点击 App 卡片恢复；恢复后显示完整销售/采购、库存和现金流回答，无重复块；服务端 run `12de88ba-faed-4ed6-bb6b-8a41d595b0ab` 为 `completed`，2 个工具、17 个连续事件、`audit_lossy=false` | 删除当前测试会话 `149` 使用 App 会话列表；`agent_conversations=6→4`、`agent_messages=20→10`；审计 18/258 保留；正式表 `products=693`、`finance_records=2661`、`stores=2`、`store_memberships=2` 不变；`pm clear` 后回到登录页 | `Passed` |
| `AG-CLI-AND-P2-HISTORY-001` | 从 Agent 首页历史列表点击既有会话 `138`；App 加载 2 条消息、`思考过程`、`执行过程 · 1 个步骤`、商品证据和完成回答；服务端 GET 消息与 run trace 均为 HTTP 200，run `03b74144-8b8d-4b74-a5cd-07e35c3acfb0` 为 `completed`，1 个工具、7 个连续事件 | 选择历史会话未发送新消息；`agent_conversations=5`、`agent_messages=14`、`agent_run_audits=18`、`agent_run_audit_events=258` 前后不变；选中的会话未删除 | `Passed` |

证据报告：`客户端/reports/20260902-agent-phase2-wave3-android-background-foreground-history.md`。后台/前台证据目录为 `客户端/artifacts/20260902-agent-phase2-wave3-AG-CLI-AND-009-001/`，历史恢复证据目录为 `客户端/artifacts/20260902-agent-phase2-wave4-AG-CLI-AND-005-001/`。后台用例的 raw SSE 正文未从 Android 进程取得，已在证据中标明；服务端审计事件序列、App 端点日志、UI tree、截图和 crash buffer 已保存。

识别当前会话 `149` 前，曾点击并删除会话列表首项，服务端计数由 `6/20` 变为 `5/14`；之后通过 Agent 首页明确定位 `149` 并完成目标清理。该额外删除动作与当前后台/前台用例分开记录，审计记录保留，未影响正式业务表。

本轮运行时仍为 `gpt-5.6-luna/chat_completions`，目标 `glm-5.3-flash` 为 `Blocked`。创建类草稿、取消/断线/重连、压缩、生图、性能和 iOS 仍未执行。

## 2026-09-02 Wave 11 Android 真实点击复核

本节是一次聚焦客户端操作的独立复核，不替换 Wave 2 已完成的服务端观测结果。操作从已登录 App 的会话列表开始，所有点击坐标均由紧接着的 UI tree 得出。

| 用例 | 实际事实 | 证据与边界 | 状态 |
|---|---|---|---|
| `AG-CLI-AND-P2-REALCLICK-001` | 新建对话；在 App `EditText` 输入 `Show the current store information`；点击 `发送` 一次；先看到处理中，再看到 `store_info_lookup`、当前门店、正常状态和 1 名成员 | App 进程日志记录公网 `/v2/agent/chat/stream` 返回 HTTP 200 `text/event-stream`；UI tree 和截图已保存；服务端 run audit、SSE 原文和数据库前后计数未采集 | `Passed` |

本轮通过 App 会话入口打开历史列表，但新会话卡片没有立即出现在加载结果中，因此没有猜测 conversation ID，也没有执行服务端删除。随后 `pm clear com.zhihuiji.app` 返回 `Success`，重启后登录页可见，crash buffer 为 0 行。客户端证据目录为 `客户端/artifacts/20260902-agent-phase2-wave11-AG-CLI-AND-P2-REALCLICK-001/`，报告为 `客户端/reports/20260902-agent-phase2-wave11-android-real-click.md`。

本轮只证明真实 Android 点击流和 App 端展示，不证明本轮数据库没有写入，也不提供本轮 server audit 完整性结论；服务端证据缺失保持 `Blocked`。已有 Wave 2 的三条只读用例仍以其独立 `00–10` 证据目录和服务器计数为准。

## 2026-09-02 Wave 12 Android 创建草稿拒绝真实点击

本轮继续使用 `emulator-5554` 上的 `com.zhihuiji.app` 1.0.0，通过 8220 公网 API 执行 `AG-CLI-AND-004` 的 `create_product` 草稿拒绝分支。发送和拒绝都依据紧接着采集的 UI tree bounds 点击；输入内容在 App `EditText` 中确认后才发送。

| 用例 | 实际事实 | 数据与清理 | 状态 |
|---|---|---|---|
| `AG-CLI-AND-P2-DRAFT-REJECT-001-RERUN-001` | 输入“新增一个商品编码为 EVALONLY20260902 名称为阶段二真实点击商品，先生成草稿，不要直接保存”；点击发送后 App 收到 HTTP 200 `text/event-stream`，出现“操作确认”弹窗；点击“拒绝”后 `POST /v2/agent/drafts/9/cancel` 返回 HTTP 200。会话详情提示无法解析 `draft_card` 字段并保留旧的 active 文案，草稿列表单独回读为“已取消（未执行）” | App 删除取消草稿返回 HTTP 200；会话 `161` 首次删除超时，重试返回 HTTP 200 且列表移除；`pm clear com.zhihuiji.app` 成功并回到登录页；当前 PostgreSQL before/after 未采集 | `Failed` |

证据报告：`客户端/reports/20260902-agent-phase2-wave12-android-draft-reject.md`；证据目录：`客户端/artifacts/20260902-agent-phase2-wave12-AG-CLI-AND-P2-DRAFT-REJECT-RERUN-001/`。本轮真实点击、草稿取消和清理均有 App UI tree/截图/脱敏 logcat；run audit、raw SSE 和数据库 before/after 因 SSH 公钥探针失败记为 `Blocked`，未把 HTTP 200 扩展为数据库通过结论。

本轮发现的客户端问题是：`draft_card` 在会话详情无法解析，拒绝后详情状态没有刷新；独立草稿列表能显示取消状态。运行时仍为 `gpt-5.6-luna/chat_completions`，目标 `glm-5.3-flash` 未加载，模型前置保持 `Blocked`。本轮未执行确认写入、重复确认、并发确认、生图、iOS 或性能用例。

## 2026-09-03 Wave 14 Android 创建草稿拒绝复测

本轮继续使用 `emulator-5554` 上的 `com.zhihuiji.app` 1.0.0，通过 8220 公网 API 执行 `AG-CLI-AND-004` 的 `create_product` 草稿拒绝分支。发送和拒绝均依据最新 UI tree 的可点击父节点 bounds 真实点击；输入文本在 App `EditText` 中逐段输入并完成精确校验。

| 用例 | 实际事实 | 数据与清理 | 状态 |
|---|---|---|---|
| `AG-CLI-AND-P2-DRAFT-REJECT-001-RERUN-002` | 最终提示词为“新增一个商品 商品编码是 EVALONLY20260903D 名称是阶段2 中文点击商品先生成草稿不要直接保存”；App 真实点击发送后收到 HTTP 200 `text/event-stream`，出现“操作确认”；服务端 run `5f72e106-5fce-4318-a112-058bf044b982` 选择并完成 `create_product`，生成 draft `12`；点击“拒绝”后 App 详情显示 `cancelled` 和“草稿已取消，未执行任何业务写入” | 发送前 `11/27/40/482/0/693/2661`；拒绝后 `12/29/41/499/1/693/2661`；App 删除草稿和两条测试会话后 `10/25/41/499/0/693/2661`；正式商品数无变化；`pm clear` 后登录页可见，crash buffer 0 行 | `Passed` |
| 同一 Wave 首次无分隔输入尝试 | run `45ef8240-b523-4891-995a-fd879adc777b` 选择了 `create_product`，但 `code` 为空并返回 `TOOL_ARGUMENTS_INVALID`；未生成草稿 | 未产生草稿或正式业务写入；保留失败审计，随后由 App 删除会话 | `Failed` |

本轮最终 run 的审计事件为 `run_started → plan_delta → tool_started → tool_completed → draft_created → result_block/answer_delta → answer_completed → run_completed`，共 17 个连续事件，`audit_lossy=false`。App 端详情已经正确解析草稿字段并刷新为取消状态，上一轮发现的 `draft_card` 解析问题在本次复测中未再出现。

8220 实时核查：Nginx、Docker、API、PostgreSQL、Redis 和转发服务均 active/running，PostgreSQL healthy，failed systemd units 为 0；公网 API 匿名入口返回 401，属于认证保护。运行时仍是 `gpt-5.6-luna/chat_completions`，目标 `glm-5.3-flash` 继续为 `Blocked`；确认写入、重复/并发确认、生图、性能和 iOS 未执行。

证据报告：`客户端/reports/20260903-agent-phase2-wave14-android-draft-reject-rerun-002.md`；证据目录：`客户端/artifacts/20260903-agent-phase2-wave14-AG-CLI-AND-P2-DRAFT-REJECT-RERUN-002/`。

## 2026-09-03 Wave 17 Android 创建草稿确认实测

本轮使用 `emulator-5554` 上的 `com.zhihuiji.app` 1.0.0，连接 8220 公网 API。先按 UI tree 定位并真实点击发送按钮，输入为“新增 一个 商品 商品编码 是 9172603842 名称是 阶段确认商品 先 生成 草稿 不要 直接 保存”。App 日志记录 `POST https://zhj-api.sxyq27.online/v2/agent/chat/stream` 返回 HTTP 200 `text/event-stream`；服务端 run `f3cc466a-cef1-4d26-84af-49d4dd0d47fe`、conversation `175` 生成 draft `14`。

| 用例 | 实际事实 | 数据、清理与证据 | 状态 |
|---|---|---|---|
| `AG-CLI-AND-P2-DRAFT-CONFIRM-001` | 草稿弹窗真实出现“操作确认”“新建商品：阶段确认商品”“草稿状态：待确认”；草稿字段为 `code=9172603842`、`name=阶段确认商品`。依据 UI tree 点击“允许一次”后，App 真实调用 `POST /v2/agent/drafts/14/confirm`，返回 HTTP `422`，界面显示“请求参数未通过校验，请检查输入内容”；没有确认成功或正式商品写入 | 确认点击前 `agent_conversations=12`、`agent_messages=29`、`agent_drafts=1`、`agent_run_audits=46`、`agent_run_audit_events=532`、`products=693`、`finance_records=2661`。随后依据 UI tree 点击“拒绝”，draft `14` 变为 `cancelled`，App 显示“草稿已取消，未执行任何业务写入”；Wave 17 会话 `173/174/175` 和测试草稿已由 App 清理，最终 `agent_conversations=10`、`agent_messages=25`、`agent_drafts=0`、`agent_run_audits=47`、`agent_run_audit_events=545`、`products=693`、`finance_records=2661`，目标商品编码记录、目标草稿和目标会话均为 0。证据：`客户端/artifacts/20260903-agent-phase2-wave17-AG-CLI-AND-P2-DRAFT-CONFIRM-001/` | `Failed` |
| 数据库分类/单位前置 | `category_count=0`、`unit_count=0`、`products_with_category_id=0`、`products_with_unit_id=0`；商品的分类/单位显示文本存在历史值，但对应 ID 全部为空。当前确认写入缺少可用的分类 ID 和单位 ID，因此确认请求无法取得 HTTP 2xx 业务成功结果 | `547-reference-category-unit-check.txt`、`539-server-after-confirm-failure-evidence.txt`、`546-db-after-failed-confirm-and-reference-data.txt` | `Blocked` |
| 确认成功、重复确认、并发确认 | 本轮在确认接口返回 `422` 且分类/单位参考数据为空后停止这些分支；未取得正式写入、重复确认或并发竞争的真实结果 | 解除条件：补齐当前 owner 可用的分类和单位记录并用其 ID 重新生成草稿，或调整并部署确认契约后重新执行；不得用本轮 `422` 或 Wave 14 的拒绝结果代替确认成功证据 | `Blocked` |

本轮 8220 服务前置核查显示 API、PostgreSQL、Redis 和 Nginx 正常，PostgreSQL 为 `healthy`；Android 最终通过 `pm clear com.zhihuiji.app` 回到登录页，crash buffer 为 0 行。关键证据包括 `40-server-preflight.txt`、`41-nginx-test.txt`、`524-after-send-app-logcat-redacted.txt`、`538-after-confirm-app-logcat-redacted.txt`、`525-confirm-before.xml`、`532-after-confirm-1s.xml`、`544-after-confirm-failure-reject.xml`、`572-db-after-cleanup-conversations.txt` 和 `582-db-final-recount.txt`。

运行时仍为 `gpt-5.6-luna/chat_completions`，目标 `glm-5.3-flash` 未验证，记为 `Blocked`。在分类/单位数据或确认契约满足前，不继续确认成功、重复确认和并发确认测试；本轮未修改源码、数据库、线上服务、账号、密码或证据文件。

## 2026-09-04 Wave 18 修复回归与公网入口复核

本节记录当前源码和新安装包的一致性检查，不替代真实服务器 App 联调结果。

| 用例 | 实际事实 | 状态 |
|---|---|---|
| `AG-CLI-AND-P2-DRAFT-CONFIRM-CUSTOMER-UI-FIX-RERUN-005` | 强制执行 `:feature:agent:testDebugUnitTest`，115 个任务执行并 `BUILD SUCCESSFUL`；强制执行 `:app:assembleDebug`，648 个任务执行并 `BUILD SUCCESSFUL`；APK 安装到 `emulator-5554`，宿主包与设备安装包 SHA-256 均为 `f7b92adb686c54be0450038fb854375a256f8f48f34ef61e6b07c7f19c902495`；App 登录页和 crash buffer 0 行已核对 | `Blocked` |

公网前置仍未通过：`zhj-api.sxyq27.online` HTTPS 和强制直连 8220 均为 HTTP `000`/`SSL_ERROR_SYSCALL`；8220 API、PostgreSQL、Redis 运行正常，但 8220 没有 `443` 监听；124 的 `/zhj-api/` 返回 `410`。本轮未输入凭据、未点击登录、未发送 Agent 请求，未产生 session、run、conversation、draft、消息、审计或业务写入。

证据报告：`testing/Agent/客户端/reports/20260903-agent-phase2-wave18-android-create-customer-confirm.md`。本轮证据目录：`testing/Agent/客户端/artifacts/20260904-agent-phase2-wave18-android-create-customer-confirm-005/`。代码级检查通过不能替代公网 Android 真实点击；入口恢复后仍需从登录页执行 `create_customer` 草稿确认、服务端 before/after、重复确认和并发确认。
补充入口诊断（2026-09-04）：`testing/Agent/客户端/artifacts/20260904-agent-phase2-wave18-android-create-customer-confirm-006/` 确认 8220 的 `zhj-api.sxyq27.online.conf` 位于 `sites-available` 但未启用，且只有 HTTP 80 配置；未找到匹配的 `zhj-api` 证书。API 容器本机可达，公网 TLS 仍失败。启用站点、配置证书和 reload Nginx 需单独授权；在入口恢复前不执行 Android 登录或 Agent 请求。

## 2026-09-04 Wave 18 入口恢复后的真实确认复测

本节追加入口恢复后的 `007` 执行结果；前面 Wave 18 的入口不可用记录仍保留为历史 `Blocked`，不能覆盖本节的真实 App 证据。

| 用例 | 实际事实 | 数据与清理 | 状态 |
|---|---|---|---|
| `AG-CLI-AND-P2-DRAFT-CONFIRM-CUSTOMER-REAL-RERUN-007` | `emulator-5554` 中按 UI tree 真实输入和点击发送、确认；`create_customer` 生成 draft `16`，确认请求 HTTP `200`，客户 `88` 写入，draft 为 `confirmed`。确认后卡片同时显示 `状态：confirmed`、旧的等待确认文案和“业务数据已写入”状态 | 客户 `83 -> 84`；业务引用 `create_customer:88`；确认后对应 audit 仍为 `confirmation_pending` | `Failed` |
| `AG-CLI-AND-P2-DRAFT-CONFIRM-CUSTOMER-REPEAT-007` | 对 draft `16` 发起重复确认，返回 `code=0`、`confirmed`；没有重复创建客户 | 目标客户记录保持 1 条 | `Passed` |
| `AG-CLI-AND-P2-DRAFT-CONFIRM-CUSTOMER-CONCURRENT-007` | 对 draft `18` 执行 App 确认和竞争确认；两个请求均 HTTP `200`/幂等返回；只写入客户 `89` 一条，draft `18` 为 `confirmed`；draft `17` 未确认 | conversation `178` 的旁支运行另有 `STREAM_ERROR`，不扩展为整体成功；draft `17` 后续清理 | `Passed` |
| `AG-CLI-AND-P2-DRAFT-CONFIRM-CUSTOMER-AUDIT-007` | 草稿确认成功后 run audit 仍为 `confirmation_pending`，确认服务没有同步审计确认结果或确认事件 | 需明确确认后的审计终态/关联字段；当前不把 audit 改写为 `completed` | `Failed` |
| `AG-CLI-AND-P2-DRAFT-CONFIRM-CUSTOMER-CLEANUP-007` | App 删除 draft `16/17/18` 和 conversation `177/178/179`；客户页面删除只清理本地 Room/同步队列，未观察到远端客户删除请求；随后 8220 本机 API 精确删除客户 `88/89`，均 HTTP `200` | 清理后 `customers=83`、`agent_drafts=0`；目标残留 `0|0|0|0`；`pm clear` 成功，最终登录页可见，crash buffer 为空 | `Passed` |

007 的最终 PostgreSQL 计数为 `users=3`、`stores=2`、`store_memberships=2`、`products=693`、`customers=83`、`finance_records=2661`、`agent_conversations=10`、`agent_messages=25`、`agent_drafts=0`、`agent_run_audits=53`、`agent_run_audit_events=604`。本节新增证据目录为 `客户端/artifacts/20260904-agent-phase2-wave18-android-create-customer-confirm-007/`，详见对应 Wave 18 报告和客户端 live ledger。

当前运行模型仍是 `gpt-5.6-luna/chat_completions`，目标 `glm-5.3-flash` 为 `Blocked`。本轮没有执行生图、取消/断线、上下文压缩、性能或 iOS；iOS 为 `Deferred`。App 客户远端删除缺口需单独修复和复测。

## 2026-09-04 Wave 19 Android `create_customer` 确认与审计核查

本节追加 Wave 19 的独立真实 Android 证据，不删除或改写 Wave 18 历史记录。证据目录为 `客户端/artifacts/20260904-agent-phase2-wave19-android-confirm-audit-009/`，正式报告为 `客户端/reports/20260904-agent-phase2-wave19-android-confirm-audit-009.md`。

| 用例 | 实际事实 | 数据与清理 | 状态 |
|---|---|---|---|
| `AG-CLI-AND-P2-DRAFT-CONFIRM-CUSTOMER-REAL-RERUN-009` | App 中按 UI tree 真实输入“新建客户名称测试客户手机[REDACTED_TEST_PHONE]先生成草稿不要直接保存”；依据 `[586,502][682,598]` 真实点击发送。App 记录 `POST /v2/agent/chat/stream` HTTP `200`、`text/event-stream`；生成 run `539a6a1a-a6bd-44c9-a9cb-ec7fd54fef45`、conversation `182`、draft `21`。确认弹窗出现后，依据 `[392,803][592,899]` 真实点击“允许一次”，确认接口 HTTP `200`；确认后 UI 显示 `confirmed` 和“业务数据已写入” | 确认前客户数 `83`、draft `21` 为 `active`；确认后客户 `92` 仅一条、draft `21` 为 `confirmed`，业务引用为 `create_customer:92`；未观察到重复客户 | `Passed` |
| `AG-CLI-AND-P2-DRAFT-CONFIRM-CUSTOMER-AUDIT-009` | 服务端生成阶段事件按 `run_started → plan_delta → tool_started → tool_completed → draft_created → answer_delta/result_block → answer_completed → run_completed` 保存；`run_completed` 的原始终态为 `CONFIRMATION_PENDING`。确认后同一 run 追加 `draft_confirmed` 事件，事件序号为 `14`；audit 状态仍为 `confirmation_pending`，与原始生成终态相符 | 发送前至确认后 audit 数为 `58→59`；audit event 全局数为 `651→664→665`；draft、客户和业务引用一致 | `Passed` |
| `AG-CLI-AND-P2-DRAFT-CONFIRM-CUSTOMER-CLEANUP-009` | 真实点击“清空对话”、打开会话列表并删除目标会话；会话删除 HTTP `200`。随后精确删除客户 `92`，HTTP `200`；`pm clear com.zhihuiji.app` 返回 `Success`，重启后回到登录页，crash buffer 为空 | 清理后 `agent_drafts=0`、`agent_conversations=10`、`agent_messages=25`、`customers=83`；目标 draft、conversation、messages、customer 和手机号记录均为 `0`；审计记录保留为 `agent_run_audits=59`、`agent_run_audit_events=665` | `Passed` |

本 Wave 的确认接口、draft `21`、客户 `92`、审计 `draft_confirmed` 事件和清理结果均有脱敏证据。`confirmation_pending` 表示原始草稿生成 run 的终态，后续确认由独立 `draft_confirmed` 事件记录；本轮不把该字段单独判为失败。

## 2026-09-04 Wave 21 Android `create_customer` 确认、审计与清理

本节追加 Wave 21 的独立真实 Android 证据，不删除或改写 Wave 18、Wave 19 历史记录。证据目录为 `客户端/artifacts/20260904-agent-phase2-wave21-android-confirm-audit-011/`，正式报告为 `客户端/reports/20260904-agent-phase2-wave21-android-confirm-audit-011.md`。

| 用例 | 实际事实 | 数据与清理 | 状态 |
|---|---|---|---|
| `AG-CLI-AND-P2-DRAFT-CONFIRM-CUSTOMER-REAL-RERUN-011` | App 中使用真实 Gboard 输入客户创建提示；依据 UI tree 真实点击发送中心 `(634,1100)` 和“允许一次”中心 `(492,851)`。`POST /v2/agent/chat/stream` 返回 HTTP `200`、`text/event-stream`；`POST /v2/agent/drafts/23/confirm` 返回 HTTP `200`；run `d990139c-8828-41c9-8f08-b5d57bee2176`、conversation `184`、draft `23` | draft `23` 为 `confirmed`，业务引用为 `create_customer:94`；客户 `94` 只有一条记录；确认后 App 显示“业务数据已写入” | `Passed` |
| `AG-CLI-AND-P2-DRAFT-CONFIRM-CUSTOMER-AUDIT-011` | 生成事件序号连续为 `1–15`，`run_completed` 的原始终态为 `confirmation_pending`；确认后同一 run 追加 `draft_confirmed`，`audit_lossy=false` | 确认后 audit 为 `61` 条、事件为 `695` 条；原始 run 状态保持不变，确认动作有独立事件记录 | `Passed` |
| `AG-CLI-AND-P2-DRAFT-CONFIRM-CUSTOMER-CLEANUP-011` | 真实点击清空对话、打开会话列表并删除目标会话；进入客户详情后真实点击删除；精确服务端清理残留客户；`pm clear` 后重启 Activity | 清理后 `agent_drafts=0`、`agent_conversations=10`、`agent_messages=25`、`customers=84`；目标 draft、conversation、messages、客户、手机号、日志和 tombstone 均为 `0`；登录页可见，crash buffer 为 `0` 行 | `Passed` |
| `AG-CLI-AND-P2-CUSTOMER-DELETE-SYNC-011` | App 客户删除调用 `/v2/sync/upload` 返回 HTTP `200`，cursor/pull 返回 HTTP `200`，Worker 为 `SUCCESS`；本地 outbox 进入 `blocked` | 错误为 `sync version conflict: expected 0, current 1`；服务端复核没有 tombstone，客户行仍存在；测试侧再用精确 API 清理，故客户端同步分支为 `Failed` | `Failed` |

Wave 21 的确认前计数为 `customers=84`、`agent_conversations=10`、`agent_messages=25`、`agent_drafts=0`、`agent_run_audits=60`、`agent_run_audit_events=680`；确认后客户变为 `85`，清理后回到 `84`。实际运行模型为 `gpt-5.6-luna/chat_completions`，目标 `glm-5.3-flash` 为 `Blocked`。截图留在本地证据目录，未加入 Git；同步删除缺口需在后续修复并重新执行。
