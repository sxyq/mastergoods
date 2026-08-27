# iOS 性能测试执行手册

## Objective

建立 iOS 启动、首屏、导航、长列表和 Agent 流式更新的可重复性能基线。

## Scenarios

1. 冷启动并打开 dashboard。
2. 打开 Agent 页面并接收连续事件。
3. 滚动 20 轮历史会话或长列表。
4. 重复切换会话并恢复历史。
5. 取消生成并重新发起请求。

## Metrics

- 冷启动和首屏渲染时延
- dropped frames、滚动卡顿和主线程占用
- CPU 峰值、内存增长、泄漏和 Agent 更新耗时
- 首个事件、首个回答片段和取消响应延迟

## Tools

- Instruments Time Profiler
- Allocations、Leaks、Core Animation
- OS signposts（已有埋点时）

## Acceptance

每个场景保存设备/系统、构建版本、数据量、原始 trace 和指标摘要；没有模拟器或设备时记录为 `Blocked`，不以静态检查替代性能通过。
