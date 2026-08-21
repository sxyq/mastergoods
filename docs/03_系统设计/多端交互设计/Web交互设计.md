# Web 交互设计

## 文档信息

| 字段 | 内容 |
|---|---|
| 文档类型 | 系统设计 |
| 当前状态 | 待验证 |
| 适用端 | Web |
| 依据源码 | `Code/frontend/web/src/pages/agent/AgentPage.vue`、`shared/api/agent-stream.ts`、`app/router/routes.ts`、`app/layouts/AppLayout.vue` |
| 依据测试 | `testing/web/功能测试/TEST_PLAN.md` |
| 依据证据 | 无 8220 运行证据（Agent 主流程本轮未展开） |
| 最后核对 | 2026-08-20 |

## 一、页面与路由

- Agent 页面真实入口：`pages/agent/AgentPage.vue`（`<script setup lang="ts">`，Vue 3）。
- 路由：`app/router/routes.ts` 第 66 行 `'/agent': AgentPage`（顶级路由）。
- 布局：`app/layouts/AppLayout.vue`（PC 宽屏布局，侧边导航）。
- 设计参考：`Code/frontend/web/docs/WEB_DEVELOPMENT_ROADMAP.md`（注：`Code/frontend/web/README.md` 不存在，实际为 docs/WEB_DEVELOPMENT_ROADMAP.md）。

## 二、页面能力（AgentPage.vue 源码事实）

| 能力 | 实现 |
|---|---|
| 会话列表 | `conversations` ref；`fetchAgentConversations(session.token, {page:0, limit:50})` |
| 侧栏工作台 | `fetchSidePanel()`：workbench + conversations + drafts + tasks + notifications |
| 消息渲染 | `messages` ref；`UiMessage`（toolCalls、resultBlocks、draft） |
| 流式对话 | `streamAgentChat(...)`（agent-stream.ts） |
| 草稿 | `draftForm`（标题/类型/内容 JSON/状态）；`cancelAgentDraftAction`；draftTypeSuggestions |
| 取消运行 | `cancelAgentRun` |
| 审计 | `fetchAgentRunAudit` |
| 会话选择 | `selectedConversationId`；`queryConversationId`（从 route.query.conversationId 读取） |
| 快捷问题 | route.query.q |

## 三、SSE 流式（agent-stream.ts）

- `streamAgentChat()`：`fetch(API_BASE_URL + '/v2/agent/chat/stream', { Accept: 'text/event-stream', stream: true })`。
- 解析：`ReadableStream` + `TextDecoder` 分块解码。
- 事件类型：`run_started`、`safety_check_*`、`plan_delta`、`tool_*`、`answer_delta`、`answer_completed`、`result_block`、`draft_created`、`context_compacted`、`run_completed`、`run_cancelled`、`error`。

## 四、表格与图表

- `AgentPage.vue` 定义 `tableHeaders` / `tableRows`（表格数据模型）。
- 图表：`entities/screen/`（Stitch 屏幕）与报表页；Agent 图表渲染需在 AgentPage 源码中确认（待验证）。

## 五、PC 布局与交互

- `AppLayout.vue` 提供宽屏布局。
- 键盘/鼠标/滚动交互：待验证（Agent 页面主流程本轮未展开测试）。

## 六、权限与路由

- `/agent` 路由；403 页 `ForbiddenPage.vue`。
- 角色：`entities/auth/roles.ts`。
- session：`app/stores/session.ts`。

## 对应实现

- Web 代码：`pages/agent/AgentPage.vue`、`shared/api/agent-stream.ts`、`app/router/routes.ts`
- 后端代码：`api/controller/v2/V2AgentController.java`
- Android 代码：不适用
- iOS 代码：不适用
- Agent 代码：`V2AgentAiService`

## 对应接口

- 接口路径：`/v2/agent/*`
- 请求模型：`V2AgentDtos`
- 响应模型：`V2AgentDtos`
- SSE 事件：`agent-stream.ts` 事件类型清单

## 对应测试

- 单元测试：`testing/web/单元测试/TEST_PLAN.md`
- 功能测试：`testing/web/功能测试/TEST_PLAN.md`
- 性能测试：`testing/web/性能测试/TEST_PLAN.md`

## 当前限制

- 未完成内容：Agent 主流程测试；图表渲染源码核对；键盘/滚动交互验证
- Blocked 内容：无
- Deferred 内容：多模态
- historical-only 内容：无
