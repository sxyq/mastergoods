# Web 单元测试执行手册

## Objective

建立 Vue/Vite 函数级测试，覆盖应用状态、实体逻辑、组件辅助函数、页面派生状态和 Agent 事件解析。

## Scope

- `Code/frontend/web/src/app`
- `Code/frontend/web/src/entities`
- `Code/frontend/web/src/features`
- `Code/frontend/web/src/pages`
- `Code/frontend/web/src/shared`

## Must Cover

1. `business.ts` 格式化、状态映射和日期范围工具。
2. `readQueryId`、`sameEntityId` 等大 ID 安全逻辑。
3. session Store 的持久化、权限判断、角色切换和刷新分支。
4. API adapter、实体映射、分页和错误转换。
5. Agent SSE 事件解析、工具调用展示、草稿状态和取消状态。
6. 页面加载、空态、局部失败、重试和错误提示的派生状态。

## Commands

```bash
cd Code/frontend/web
npm run build
npm run test
```

## Acceptance

每个手写 helper 和 Store 函数都能映射到测试；测试断言返回值、状态转换和异常分支。未执行前不填写通过结果。
