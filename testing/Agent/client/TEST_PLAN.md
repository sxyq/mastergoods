# Agent 客户端联调测试规划（client）

更新日期：2026-08-28。Android 与 iOS 使用相同的服务端断言，客户端只记录各自版本、设备标签与展示状态。Web 仅作协议与展示对照。登录必须走 APP 正常登录流程，测试人员不手工提取/复制 Cookie、Session Token、Authorization 或完整认证载荷。

## 一、执行前提

| 端 | 前提 | 缺失时 |
|---|---|---|
| Android | 真机/模拟器 + 安装测试版 APK；ADB 可用 | 记 `Blocked` |
| iOS | 真实设备或签名环境 + 本机 Xcode | 记 `Blocked` |
| Web | 后端服务可达 | 记 `Blocked`（仅协议对照） |

## 二、核心流程（AG-CLI-*，每端独立记录）

| 编号 | 流程 | 服务端事实 | 客户端验收 |
|---|---|---|---|
| AG-CLI-AND-001 / IOS-001 | 登录与会话列表 | 200 会话列表、current owner/store | 会话进入/恢复；版本记录 |
| AG-CLI-AND-002 / IOS-002 | 单只读工具流式 | run 事件序列与 DB 事实 | 增量不重复不丢字；工具过程展示；回答完整 |
| AG-CLI-AND-003 / IOS-003 | 多工具 + 图表 | 多工具链、result_block | 图表与 facts 一致；空数据空状态 |
| AG-CLI-AND-004 / IOS-004 | 草稿确认/拒绝弹窗 | draft_created、confirm/cancel 结果 | 覆盖式弹窗明确“草稿→确认/拒绝→正式写入”；拒绝后正式表无变化 |
| AG-CLI-AND-005 / IOS-005 | 历史恢复 | 历史消息 run_id/回答/工具过程 | 服务器事实与历史一致；排序正确 |
| AG-CLI-AND-006 / IOS-006 | 取消与断线 | run_cancelled/断线终态 | 停止后无增量；断线不重复展示；可恢复重连 |
| AG-CLI-AND-007 / IOS-007 | 压缩事件展示 | context_compacted | 展示条数/原因；不展示敏感原文 |
| AG-CLI-AND-008 / IOS-008 | 错误与重试 | 401/403/409/422/429/5xx 或 SSE error | 分别展示登录失效/无权限/冲突/参数/限流/服务异常；可重试 |
| AG-CLI-AND-009 / IOS-009 | 后台/前台切换（iOS 另测） | 运行状态 | 恢复后状态一致；无重复事件 |
| AG-CLI-WEB-001 | 协议对照 | 同上服务端事实 | 事件/Schema 与移动端一致；ID 全程字符串/BigInt，禁止 `Number()` |

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

## 四、证据存放

`client/artifacts/<日期>-<波次>-<用例>/`：请求摘要（脱敏）、UI 树/截图（有设备时）、原始 SSE（来自服务端观察）；`client/logs/`（logcat/Console 脱敏）；`client/reports/`（设备观测报告）。缺少设备时不创建伪证据，仅记录 `Blocked`。