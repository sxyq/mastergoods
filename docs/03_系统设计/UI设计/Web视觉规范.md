# Web 视觉规范

## 文档信息

| 字段 | 内容 |
|---|---|
| 文档类型 | 系统设计 |
| 当前状态 | 待验证 |
| 适用端 | Web |
| 依据源码 | `Code/frontend/web/src/style.css`、`app/layouts/AppLayout.vue`、`shared/ui/PageEmptyState.vue`、`PageStatusBanner.vue` |
| 依据测试 | `testing/web/功能测试/TEST_PLAN.md` |
| 依据证据 | 无 8220 运行证据 |
| 最后核对 | 2026-08-20 |

## 一、布局

- PC 宽屏布局：`app/layouts/AppLayout.vue`（侧边导航 + 内容区）。
- 页面类型：文档中心（单据）、档案、报表、规划、设置、Agent。

## 二、共享 UI 组件（真实源码）

| 组件 | 源码 | 用途 |
|---|---|---|
| `PageEmptyState` | `shared/ui/PageEmptyState.vue` | 空态 |
| `PageStatusBanner` | `shared/ui/PageStatusBanner.vue` | 状态横幅 |
| `style.css` | `src/style.css` | 全局样式 |

## 三、Agent 页面视觉

- `AgentPage.vue`：会话列表 + 消息区 + 草稿表单 + 输入区（单页）。
- 视觉细节（配色、间距）以 `style.css` 与组件实现为准，本轮未做独立视觉验收。

## 四、Stitch 设计资源

- `Code/frontend/web/public/stitch_exports/`：设计导出资源（`visual-design_system_framework_.../manifest.tsv`）。
- `entities/screen/`：Stitch 屏幕数据模型（`live-screen-data.ts`、`page-models.ts`）。

## 对应实现

- Web 代码：`src/style.css`、`app/layouts/AppLayout.vue`、`shared/ui/`
- Android 代码：不适用
- iOS 代码：不适用
- 后端代码：不适用
- Agent 代码：不适用

## 对应接口

- 接口路径：无
- 请求模型：无
- 响应模型：无
- SSE 事件：无

## 对应测试

- 功能测试：`testing/web/功能测试/TEST_PLAN.md`
- 性能测试：`testing/web/性能测试/TEST_PLAN.md`

## 当前限制

- 未完成内容：Web 视觉运行验证
- Blocked 内容：无
- Deferred 内容：无
- historical-only 内容：无
