# Agent 性能 阶段报告

- wave_id: `20260830-luna-perf-reliability-blocked-01`
- requested_executor: `gpt-5.6-luna / max`
- source_commit: `c4ae9b86dc8582e48613b43d6318c8ef3e02204e`
- evidence_mode: `环境边界与脱敏占位`

## 当前需求与状态

本批未开始有效负载。真实 sample_count=0。没有可用的本地 Agent 服务、获批授权会话或 PostgreSQL。Android 模拟器可用，匹配服务与 App 会话缺失。

| 范围 | 总数 | Passed | Failed | Blocked | Deferred |
|---|---:|---:|---:|---:|---:|
| 性能父用例 | 27 | 0 | 0 | 27 | 0 |

## 本轮实际完成

完成只读环境探测和逐用例 00-10 证据。并发 1/5/10/20、SSE、Provider、数据库与写入负载均未发送。

## 修改或操作对象

新增本波次 `性能/artifacts`、`性能/reports` 和 `性能/logs` 文件。未修改业务源码、迁移、生产配置或生产数据。

## 验证结果

| 指标 | 结果 |
|---|---:|
| valid_request_count | 0 |
| P50/P95/P99 | NA |
| TTFB/首 SSE/首工具/首回答/完成时延 | NA |
| tool_duration/error_rate | NA |
| Perfetto/gfxinfo/meminfo/Simpleperf | 0 份 |
| Android device/build/run count | emulator-redacted-01 / 1.0.0 (1) / 0 |

## 剩余工作与风险

真实 Provider、生产 PostgreSQL 查询计划和真实跨 owner/store 负载保持 Deferred 子范围。各父用例当前为 Blocked，重试条件已写入台账和 10-conclusion.md。
