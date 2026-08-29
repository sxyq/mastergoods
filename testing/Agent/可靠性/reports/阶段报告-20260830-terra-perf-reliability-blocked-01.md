# Agent 可靠性 stage report

- wave_id: `20260830-terra-perf-reliability-blocked-01`
- planned parent cases: 13
- derived cases recorded: 13
- Passed: 0
- Failed: 0
- Blocked: 13
- Deferred: 0
- evidence completeness: 100% for required 00-10 files

## Result

All cases were attempted at the environment boundary. The local Spring service was not listening on port 18080. No Android device was attached. No PostgreSQL target was available. The Python WebDAV service on port 8080 was explicitly excluded. Therefore every workload has sample_count=0 and is recorded as Blocked.

No API key, token, cookie, password, or complete authentication payload was collected. Historical Android/backend summaries were used only as correlation context and do not change this wave result.
