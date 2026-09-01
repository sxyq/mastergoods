# Agent 客户端联调测试规划（客户端）

更新日期：2026-09-01。Android 与 iOS 使用相同的服务端断言，客户端分别记录各自版本、设备标签与展示状态；Web 仅作协议与展示对照。登录必须走 APP 正常登录流程，测试人员不手工提取/复制 Cookie、Session Token、Authorization 或完整认证载荷。每个平台使用独立 `test_id`，不能用 Android 结果代表 iOS，也不能用 Web 协议解析代表真实设备展示。

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
