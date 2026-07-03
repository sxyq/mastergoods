# iOS 性能测试全覆盖方案

## Objective

建立 iOS 端启动、导航、列表、Agent 和图形渲染基线，作为后续 SwiftUI 性能回归标准。

## Scope

- app launch
- dashboard first render
- long list scroll
- agent chat render
- conversation switch

## Metrics

- launch time
- first screen render
- dropped frames
- memory growth
- CPU spikes during agent updates

## Recommended Tools

- Instruments Time Profiler
- Allocations
- Leaks
- Core Animation
- OS signposts if added later

## Scenarios

1. cold launch
2. dashboard open
3. agent page open
4. 20-turn conversation scroll
5. repeated conversation switching

## Exit Criteria

1. Each scenario has one reproducible benchmark procedure.
2. Performance evidence is exportable and comparable release to release.
