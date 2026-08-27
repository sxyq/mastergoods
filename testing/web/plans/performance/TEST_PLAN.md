# Web 性能测试全覆盖方案

## Objective

为 Web 管理端建立加载、渲染、交互和 Agent streaming 的前端性能基线。

## Scope

- initial page load
- route switch
- table rendering
- chart rendering
- agent stream rendering

## Metrics

- first contentful paint
- largest contentful paint
- interaction latency
- scripting time
- memory growth during long agent sessions

## Tools

- Lighthouse
- Chrome Performance panel
- Playwright timing capture

## Critical Scenarios

1. login to dashboard
2. inventory or finance large table load
3. reports page chart rendering
4. agent page stream rendering with multiple blocks

## Exit Criteria

1. Every critical web flow has a reproducible benchmark script.
2. Agent long-session rendering has at least one memory profile.
