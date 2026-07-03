# Web 单元测试全覆盖方案

## Objective

为 Vue/Vite 管理端建立函数级单元测试方案，覆盖 `app`、`entities`、`features`、`pages`、`shared` 五层。

## Scope

源码范围：

- `web/src/app`
- `web/src/entities`
- `web/src/features`
- `web/src/pages`
- `web/src/shared`

## Must Cover

1. utility formatters and business helpers
2. route helpers and query-id safety helpers
3. store mutations and permission checks
4. API adapter and entity mapper logic
5. Agent parsing, provenance and state logic
6. page-level derived state functions

## Unit Layers

### Shared Utilities

- `business.ts`
- big-int safe id helpers
- date and number formatters

### Stores

- session persistence
- permission computation
- role switching and profile refresh branches

### Agent Web Logic

- event parsing
- side panel state helpers
- message block parsing
- evidence and trace formatting

## Tools

Recommended stack:

- Vitest
- Vue Test Utils
- jsdom

## Commands

```bash
cd web
npm run build
npm run test
```

## Exit Criteria

1. Every handwritten helper and store function is mapped.
2. BigInt-safe routing and Agent-related utility logic are fully covered.
