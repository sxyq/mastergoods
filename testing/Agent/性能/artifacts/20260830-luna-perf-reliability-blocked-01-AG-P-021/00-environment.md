# 环境

- test_id: `AG-P-021`
- wave_id: `20260830-luna-perf-reliability-blocked-01`
- source_commit: `c4ae9b86dc8582e48613b43d6318c8ef3e02204e`
- requested_executor: `gpt-5.6-luna / max`
- result: `Blocked`
- sample_count: `0`
- local_agent_service: `unavailable`
- approved_authorization_session: `unavailable`
- PostgreSQL: `unavailable`
- Android: device_count=1, package_installed=True, focused_flow_run_count=0
- blocker: 没有 PostgreSQL 服务或获批目标；未使用 H2 替代。
- retry_condition: 提供获批 PostgreSQL 目标、脱敏数据规模和查询计数后重试；生产查询计划需单独批准。
- deferred_scope: 生产 PostgreSQL EXPLAIN/EXPLAIN ANALYZE：Deferred，等待批准目标和维护窗口。
- authentication_values_inspected: `false`
