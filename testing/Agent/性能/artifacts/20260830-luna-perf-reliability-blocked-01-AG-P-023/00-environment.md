# 环境

- test_id: `AG-P-023`
- wave_id: `20260830-luna-perf-reliability-blocked-01`
- source_commit: `c4ae9b86dc8582e48613b43d6318c8ef3e02204e`
- requested_executor: `gpt-5.6-luna / max`
- result: `Blocked`
- sample_count: `0`
- local_agent_service: `unavailable`
- approved_authorization_session: `unavailable`
- PostgreSQL: `unavailable`
- Android: device_count=1, package_installed=True, focused_flow_run_count=0
- blocker: 模拟器和 App 已就绪；匹配的 Agent 服务与获批 App 会话缺失，聚焦流程无法开始。
- retry_condition: 在当前模拟器上提供可达的匹配服务和获批 App 会话，选定一个 Agent 流程并完成 10 次独立运行后采集 Perfetto、gfxinfo 或 meminfo。
- deferred_scope: none
- authentication_values_inspected: `false`
