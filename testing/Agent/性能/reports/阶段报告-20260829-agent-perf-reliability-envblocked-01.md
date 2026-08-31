# Agent 性能 stage report

- wave_id: `20260829-agent-perf-reliability-envblocked-01`
- planned parent cases: 27
- derived cases recorded: 27
- Passed: 0
- Failed: 0
- Blocked: 27
- Deferred: 0
- evidence completeness: 100% for required 00-10 files

## Result

All cases were attempted at the environment boundary. The local Spring service was not listening on port 18080. An Android emulator/device was available. No PostgreSQL target was available. The Python WebDAV service on port 8080 was explicitly excluded. Therefore every workload has sample_count=0 and is recorded as Blocked.

No API key, token, cookie, password, or complete authentication payload was collected. Historical Android/backend summaries were used only as correlation context and do not change this wave result.
