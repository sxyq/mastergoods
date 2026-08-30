# 环境

- test_id: `AG-R-009`
- wave_id: `20260830-luna-perf-reliability-blocked-01`
- source_commit: `c4ae9b86dc8582e48613b43d6318c8ef3e02204e`
- requested_executor: `gpt-5.6-luna / max`
- result: `Blocked`
- sample_count: `0`
- local_agent_service: `unavailable`
- approved_authorization_session: `unavailable`
- PostgreSQL: `unavailable`
- Android: device_count=1, package_installed=True, focused_flow_run_count=0
- blocker: 本地 Agent 服务、获批会话和隔离 Provider Mock 均未就绪。
- retry_condition: 启动与 source_commit 对应的隔离 Agent 服务，提供获批测试会话和脱敏 owner/store 标签后重试。 另需获批的隔离 Provider Mock；真实 Provider 需单独批准。
- deferred_scope: none
- authentication_values_inspected: `false`
