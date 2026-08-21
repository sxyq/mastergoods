# Web 实现索引

## 文档信息

| 字段 | 内容 |
|---|---|
| 文档类型 | 实现说明 |
| 当前状态 | 待验证 |
| 适用端 | Web |
| 依据源码 | `Code/frontend/web/src/` |
| 依据测试 | `testing/web/` |
| 依据证据 | 无 8220 运行证据 |
| 最后核对 | 2026-08-20 |

## 一、工程结构（真实源码）

| 目录 | 说明 | 关键文件 |
|---|---|---|
| app | 布局、路由、会话 | `App.vue`、`layouts/AppLayout.vue`、`router/routes.ts`、`router/stitch-screens.ts`、`stores/session.ts` |
| entities | 业务实体 | `agent/`、`auth/roles.ts`、`finance/`、`import-job/`、`inventory/`、`media/`、`order/`、`partner/`、`product/`、`report/`、`screen/`、`sync/` |
| features | 可复用业务功能 | `features/` |
| pages | 页面 | `agent/AgentPage.vue`、`auth/LoginPage.vue`、`dashboard/DashboardPage.vue`、`documents/`、`archives/`、`finance/`、`inventory/`、`planning/`、`reports/`、`settings/` |
| shared | API、组件、工具 | `api/client.ts`、`api/config.ts`、`api/contracts.ts`、`api/agent-stream.ts`、`ui/PageEmptyState.vue`、`ui/PageStatusBanner.vue`、`utils/` |
| public | 静态资源 | `stitch_exports/` |

## 二、Agent 实现索引

| 文件 | 职责 |
|---|---|
| `pages/agent/AgentPage.vue` | Agent 页面（会话/消息/草稿/输入/取消/审计） |
| `shared/api/agent-stream.ts` | SSE 流式（fetch + ReadableStream） |
| `entities/agent/` | Agent 实体类型 |

## 三、路由（routes.ts 关键行）

- `'/agent': AgentPage`（第 66 行，顶级路由）。
- `'/login'`、`'/403'`（ForbiddenPage）。
- `/` 使用 AppLayout 包裹业务子路由（settings/roles、settings/database 等）。
- Stitch 屏幕路由由 `stitch-screens.ts` + `resolveScreenComponent` 动态生成。

## 四、文档

- `Code/frontend/web/docs/WEB_DEVELOPMENT_ROADMAP.md`：开发路线图（注：`Code/frontend/web/README.md` 不存在）。

## 对应实现

- Web 代码：`Code/frontend/web/src/`
- 后端代码：`Code/backend/`
- Android 代码：不适用
- iOS 代码：不适用
- Agent 代码：`pages/agent/AgentPage.vue`、`shared/api/agent-stream.ts`

## 对应接口

- 接口路径：`/v2/*`
- 请求模型：`shared/api/contracts.ts`
- 响应模型：同上
- SSE 事件：`agent-stream.ts`

## 对应测试

- 单元测试：`testing/web/单元测试/TEST_PLAN.md`
- 功能测试：`testing/web/功能测试/TEST_PLAN.md`
- 性能测试：`testing/web/性能测试/TEST_PLAN.md`

## 当前限制

- 未完成内容：Agent 主流程测试、图表渲染核对
- Blocked 内容：无
- Deferred 内容：多模态
- historical-only 内容：无
