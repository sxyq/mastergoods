# 第一阶段 Web 按钮与交互入口阶段报告

## 当前需求与状态

本阶段完成 Agent B 的 Web 页面、按钮、菜单和交互入口审查。源码调用链矩阵已完成，静态服务前置核查已完成；真实浏览器执行保持 `Blocked`。第二阶段历史 Agent 任务审查尚未开始，等待主 Agent 继续指令后复用同一运行身份。

## 本轮实际完成

- 检查工作树、分支、Node/npm/npx、Web package/lockfile、node_modules 链接、进程和监听端口。
- 检查 `AgentPage.vue`、AppLayout、路由、main router guard、session、角色权限、共享 API、SSE 解析、Agent 后端路由。
- 建立页面 -> 按钮/菜单 -> 事件处理器 -> API/状态 -> 成功/失败状态矩阵，覆盖会话、历史、草稿、审计、通知、权限、加载/空态、401/403/422、重复点击、停止和失败重试。
- 用已有依赖直接启动并停止 Vite，确认 `/`、`/agent`、`/login` 返回 `200 text/html`。这项结果只代表 SPA 入口可提供。
- 登记 25 个 case：源码正向检查 3 个 `Passed`，源码缺陷 6 个 `Failed`，运行项 16 个 `Blocked`，`Deferred` 为 0。

## 修改或操作对象

- 新增原始证据目录：`testing/.artifacts/2026-08-22-web-buttons-B/`
  - `00-preflight.md`
  - `01-runtime-probes.md`
  - `02-source-call-chain.csv`
  - `03-source-findings.md`
  - `04-case-register.jsonl`
- 新增正式 Web 功能台账：`testing/web/功能测试/web_action_audit_20260822_B.csv`
- 新增本阶段报告：本文件。
- 未修改 `Code/frontend/web/src` 或其他业务源码、配置、数据库、部署对象；未读取或触碰 `data/server-backups/`、`data/server-exports/`；未执行 `git add -A`。

## 验证与剩余工作

### 运行阻塞

- 默认 shell 无 `node`、`npm`、`npx`。仅有 Codex 自带 Node 24.19.0 和现有 Vite/vue-tsc 可执行文件。
- Playwright skill 的 `npx` 前置条件未满足，所以没有 snapshot、click、请求监听或截图。
- Web 默认 API `127.0.0.1:18080` 连接失败；`127.0.0.1:8080` 返回 Cheroot Digest 401；历史 HTTPS Agent 地址返回 410。没有发送登录或认证载荷。

### 需主 Agent/C 后续真实重测的项目

1. 登录后从正常导航进入 Agent，验证 `agent:view` 与 `agent:write` 的入口差异。
2. 页面五路加载、加载态、空态、单接口失败和重新加载。
3. 新建、选择、历史恢复、删除和重复点击。
4. SSE 完成链：工具进度、正式回答、结果块、草稿事件、审计入口。
5. 停止生成、服务端 cancel、cancel 失败和重试；同时检查页面离开时的活动 run。
6. 草稿创建/编辑/校验/保存/删除，以及待确认草稿确认/取消和重复点击。
7. 审计加载/错误/关闭，通知已读，401 refresh、403 跳转、422 错误反馈。
8. 两个 owner 会话下的历史、草稿和审计隔离；移动端面板、遮罩、滚动恢复和截图。

源码缺陷 `WEB-B-F-001` 至 `WEB-B-F-006` 已登记为 `Failed`，本轮未修改源码。
