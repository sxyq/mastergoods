# AI Agent Interface Evidence Index - 2026-06-08

This index summarizes the interface-level evidence captured against the local
backend on `http://localhost:18080` with `SPRING_PROFILES_ACTIVE=local` and
`AGENT_LLM_ENABLED=false`.

## Captured Runs

| Question | Evidence package | Tools | Interface result |
|---|---|---:|---|
| 哪些商品库存不足，风险最高？ | `20260608-2301-84fd5e3a-599d-47cb-8944-8c2d66c414f8` | 2 | `pass-for-interface` SSE/audit reconciliation; workbench cleanliness captured |
| 哪些客户还有应收款，金额是多少？ | `20260608-2303-af62639e-cccf-43ce-9937-398f1e4b1974` | 1 | `pass-for-interface` SSE/audit reconciliation; workbench cleanliness captured |
| 最近销售采购和财务情况怎么样？ | `20260608-2308-98d581cc-efe0-4dd5-8ab2-454fa5e01101` | 4 | `pass-for-interface` SSE/audit reconciliation; includes KPI, table, line chart, bar chart, donut chart, risk card, and evidence card blocks |

## Bug Found And Fixed

The multi-domain business overview run originally produced valid SSE result
blocks but failed while persisting the assistant message because
`agent_messages.structured_data_json` was limited to `VARCHAR(4000)`.

The fix widens `structured_data_json` to `TEXT` in both JPA mapping and the
production Flyway migration path. After restarting the local backend, the same
question completed with `run_completed`, `status=completed`, `tool_count=4`,
and `event_count=28` in package
`20260608-2308-98d581cc-efe0-4dd5-8ab2-454fa5e01101`.

## Still Not Full P0 Pass

These packages only prove the backend/interface path. Full AI assistant P0 is
still intentionally marked partial until a real Android device or emulator adds:

- AI home screenshot and UI tree proving the first screen is clean.
- Chat answer screenshot proving Markdown rendering is readable.
- Expanded RunTrace screenshot proving tool/audit/mode fields are visible.
- Result block screenshots proving chart/table/evidence cards render correctly.
- Real provider `model_stream` evidence with `AGENT_LLM_ENABLED=true` and a
  configured model key.
