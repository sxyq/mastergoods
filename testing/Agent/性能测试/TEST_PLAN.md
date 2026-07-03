# Agent 性能测试全覆盖方案

## Objective

建立 Agent 端到端性能基线，覆盖规划、工具查询、流式输出、审计写入、跨端渲染和多模态链路。

## Metrics

- request accepted latency
- run started latency
- first tool event latency
- first answer delta latency
- final answer latency
- cancel latency
- audit event persistence lag
- Android first visible delta latency
- Web stream render latency
- image generation latency

## Scenario Set

1. short text lookup
2. multi-tool business lookup
3. blocked safety request
4. cancelled stream
5. multimodal image question
6. text-to-image generation
7. long 30-turn conversation

## Environment Matrix

- local backend
- deployed backend
- Android physical device
- Web desktop browser
- iOS simulator or device when available

## Threshold Strategy

Track separately:

1. backend-only latency
2. end-to-end latency
3. client rendering latency

## Deliverables

- backend timing report
- Android benchmark timing
- Web streaming timing
- end-to-end comparison sheet

## Exit Criteria

1. Every critical Agent scenario has a measured baseline.
2. Cancellation, streaming and multimodal flows all have timing evidence.
