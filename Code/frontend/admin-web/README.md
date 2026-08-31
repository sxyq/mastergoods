# 超级管理员后台前端

本目录是独立的超级管理员 Web 工程。它只调用受保护的 `/v2/admin/*` API，用于平台级用户、门店、Agent 运行、审计和系统配置管理。

现有 `Code/frontend/web/` 是店主/门店业务端，继续使用 `https://sxyq27.online/zhj/`。两个工程不共享路由、页面、会话状态或构建产物。

## 当前阶段

独立工程已使用 Vue 3、TypeScript 与 Vite 实现。它以 `Temp/grok-style-admin-preview/` 的 `grok2api` 白底简约风格为视觉基线，并使用 Lucide 线性图标、近白背景、细灰边框和紧凑数据面板。

当前页面包括登录、平台总览、用户与门店成员、Agent 运行、Agent 配置、操作审计、系统状态与受控拒绝页。登录和续期使用统一认证的 `/v2/auth/login`、`/v2/auth/refresh`、`/v2/auth/logout`，业务查询使用 `/v2/admin/*`。access token 与 refresh token 仅保留在当前页面内存；浏览器整页刷新后需要重新登录。运行详情的 SSE 通过带 `Authorization` 头的 `fetch` 消费，不把凭据放入 URL。

## 当前结构

```text
admin-web/
├── src/
│   ├── app/
│   │   ├── layouts/                # 管理员应用壳层
│   │   ├── router/                 # 独立管理员路由
│   │   └── stores/                 # 管理员会话和范围状态
│   ├── features/
│   │   ├── overview/               # 平台总览
│   │   └── agent-observability/    # 运行、事件、用量和上下文
│   ├── pages/                      # 路由页面组合
│   └── shared/
│       ├── api/                    # `/v2/admin/*` 客户端
│       ├── components/             # 无业务耦合的通用组件
│       └── utils/                  # 格式化、ID 与时间工具
├── vite.config.ts                  # 生产基址 `/zhj-admin/`
└── package.json
```

静态构建使用：

```bash
npm run build
```

门店 Owner 在当前接口中用于范围校验和归属展示，页面以只读形式显示。现有 `PATCH /v2/admin/stores/{storeId}` 只更新名称和状态；owner 转移需要后端提供专用事务接口、审计和验收后再增加前端操作。

本工程不在本地启动前后端服务。公网入口为 `https://sxyq27.online/zhj-admin/`，静态构建和 Nginx 路由已在 124 节点发布；真实管理员登录、接口数据和高风险操作仍按测试计划执行。
