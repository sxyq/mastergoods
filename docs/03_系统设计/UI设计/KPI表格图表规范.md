# KPI 表格图表规范

## 文档信息

| 字段 | 内容 |
|---|---|
| 文档类型 | 系统设计 |
| 当前状态 | Android 已完成；iOS/Web 待验证 |
| 适用端 | 多端 |
| 依据源码 | Android `feature/agent/result/ResultBlockRenderer.kt`、`AgentResultBlockModels.kt`；iOS `AgentChatView.swift`（resultBlockView：kpis/headers/rows/items）；Web `AgentPage.vue`（tableHeaders/tableRows） |
| 依据测试 | `ResultBlockRendererContractTest.kt`、`AgentStoredResultBlockParseTest.kt` |
| 依据证据 | `testing/.artifacts/2026-08-18-8220-current-baseline/current-8220-baseline.md` |
| 最后核对 | 2026-08-20 |

## 一、结果块渲染规范

| 块类型 | 渲染要求 | 后端过滤 |
|---|---|---|
| kpi_grid | KPI 卡片组（数值 + 标签） | 始终显示（可视化请求时） |
| table / rank_list | 表头 + 行数据 | TABLE_BLOCK_TYPES |
| line_chart / bar_chart 等 | 图表（仅真实数据） | CHART_BLOCK_TYPES + hasMeaningfulChartData |
| draft / draft_card | 草稿卡片（可操作） | ALWAYS_VISIBLE_BLOCK_TYPES |

## 二、空数据规则

- 图表块无真实数据时后端直接过滤（`hasMeaningfulChartData`），前端不渲染占位图表。
- 表格/排行块在可视化请求模式下渲染。

## 三、iOS 渲染细节（真实源码）

- `resultBlockView`：kpis 枚举渲染 KPI；headers/rows 渲染表格；items.prefix(6) 渲染列表；`draft_card` 渲染 AgentDraftCardBlock。
- 助手气泡展示 `structuredData.prefix(2)`（前 2 个结果块）。

## 对应实现

- Android 代码：`feature/agent/result/ResultBlockRenderer.kt`
- iOS 代码：`Features/Agent/AgentChatView.swift`
- Web 代码：`pages/agent/AgentPage.vue`
- 后端代码：`V2AgentAiService.selectVisibleResultBlocks()`
- Agent 代码：`ResultVisualizationTool.java`

## 对应接口

- 接口路径：无
- 请求模型：无
- 响应模型：`AgentChatResponse.blocks`
- SSE 事件：`result_block`

## 对应测试

- 单元测试：`ResultBlockRendererContractTest.kt`、`AgentStoredResultBlockParseTest.kt`
- 功能测试：`testing/Agent/功能测试/TEST_PLAN.md`（result-block）

## 当前限制

- 未完成内容：iOS 图表渲染验证
- Blocked 内容：Android 真机
- Deferred 内容：图片结果展示
- historical-only 内容：154 环境图表证据
