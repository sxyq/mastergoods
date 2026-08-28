# Agent 客户端联调测试规划（客户端）

更新日期：2026-08-28。Android 与 iOS 使用相同的服务端断言，客户端分别记录各自版本、设备标签与展示状态；Web 仅作协议与展示对照。登录必须走 APP 正常登录流程，测试人员不手工提取/复制 Cookie、Session Token、Authorization 或完整认证载荷。每个平台使用独立 `test_id`，不能用 Android 结果代表 iOS，也不能用 Web 协议解析代表真实设备展示。

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
