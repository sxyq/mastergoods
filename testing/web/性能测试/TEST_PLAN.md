# Web 性能测试执行手册

## Objective

建立页面加载、路由切换、数据表格、图表和 Agent 流式渲染的可重复性能基线。

## Scenarios

1. 登录后打开 dashboard。
2. 加载 inventory 或 finance 大列表。
3. 打开 reports 并渲染图表。
4. Agent 多工具调用、多结果块和长会话流式渲染。
5. 重复切换会话并观察内存变化。

## Metrics

- FCP、LCP、首个 Agent 事件延迟
- 交互延迟、脚本耗时、长任务数量
- 渲染帧率、列表滚动丢帧、内存增长
- 请求吞吐、失败率和取消响应延迟

## Tools

- Lighthouse
- Chrome Performance
- Playwright timing capture

## Acceptance

每个场景保存运行配置、原始测量和指标摘要；浏览器、真实登录和真实 SSE 未执行时记录为 `Blocked` 或 `Deferred`，不写成通过。
