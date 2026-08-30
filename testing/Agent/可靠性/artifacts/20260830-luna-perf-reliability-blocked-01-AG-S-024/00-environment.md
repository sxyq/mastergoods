# 环境

- test_id: `AG-S-024`
- wave_id: `20260830-luna-perf-reliability-blocked-01`
- source_commit: `c4ae9b86dc8582e48613b43d6318c8ef3e02204e`
- requested_executor: `gpt-5.6-luna / max`
- result: `Blocked`
- sample_count: `0`
- local_agent_service: `unavailable`
- approved_authorization_session: `unavailable`
- PostgreSQL: `unavailable`
- Android: device_count=1, package_installed=True, focused_flow_run_count=0
- blocker: 没有获批的双 owner/store 会话和隔离数据，跨域负载未启动。
- retry_condition: 提供获批的双 owner、双 store 隔离数据和两组脱敏会话，并在隔离服务上执行交错负载。
- deferred_scope: 真实跨 owner/store：Deferred，等待获批的双身份与隔离数据。
- authentication_values_inspected: `false`
