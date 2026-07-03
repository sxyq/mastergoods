# Web 功能测试全覆盖方案

## Objective

覆盖 Web 管理端全部用户可见功能，并验证与真实后端契约、权限和大 ID 安全规则一致。

## Scope

功能域：

- auth
- dashboard
- archives
- agent
- documents
- finance
- inventory
- planning
- reports
- settings

## Functional Standard

每个功能最少覆盖：

1. 页面可进入
2. 数据加载成功
3. 空态可展示
4. 错误态可恢复
5. 权限受限时表现正确
6. 大 ID 路由读取正确

## Agent Web Scenarios

1. enter Agent page
2. load real conversation list
3. switch conversations
4. send text message
5. receive stream updates
6. view result blocks and evidence refs
7. cancel running request
8. verify audit trace panel

## Tools

- Playwright
- local or staged backend

## Commands

```bash
cd web
npm run dev
npm run build
```

## Exit Criteria

1. Every top-level web route has scenario coverage.
2. Agent and ID-safety critical paths are validated against real backend responses.
