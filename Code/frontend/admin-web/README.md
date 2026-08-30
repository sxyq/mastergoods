# 超级管理员后台前端

本目录是独立的超级管理员 Web 工程。它只调用受保护的 `/v2/admin/*` API，用于平台级用户、门店、Agent 运行、审计和系统配置管理。

现有 `Code/frontend/web/` 是店主/门店业务端，继续使用 `https://sxyq27.online/zhj/`。两个工程不共享路由、页面、会话状态或构建产物。

## 当前阶段

当前仅建立目录骨架，不包含页面、路由、接口客户端、样式或构建配置。正式实现开始前，选择 Vue 3 + TypeScript + Vite，并以 `Temp/grok-style-admin-preview/` 的 `grok2api` 白底简约风格为视觉基线。

## 规划结构

```text
admin-web/
├── public/                         # 独立静态资源
├── src/
│   ├── app/
│   │   ├── layouts/                # 管理员应用壳层
│   │   ├── router/                 # 独立管理员路由
│   │   └── stores/                 # 管理员会话和范围状态
│   ├── entities/admin/             # 管理员领域类型和展示模型
│   ├── features/
│   │   ├── auth/                   # 登录和会话恢复
│   │   ├── overview/               # 平台总览
│   │   ├── organization/           # 用户、门店、成员关系
│   │   ├── agent-observability/    # 运行、事件、用量和上下文
│   │   ├── audit/                  # 管理员审计和导出
│   │   └── system/                 # 配置、健康和保留策略
│   ├── pages/                      # 路由页面组合
│   └── shared/
│       ├── api/                    # `/v2/admin/*` 客户端
│       ├── components/             # 无业务耦合的通用组件
│       ├── config/                 # 公开运行配置
│       └── utils/                  # 格式化、ID 与时间工具
└── tests/
    ├── unit/                       # 组件和状态测试
    └── e2e/                        # 浏览器验收场景
```

计划公网入口为 `https://sxyq27.online/zhj-admin/`，在独立构建和 Nginx 路由完成前不发布。
