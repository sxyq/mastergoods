# SSE / Audit / UI Reconciliation

| seq | event_id | raw SSE event_type | audit event_type | delta_source | mode | llm_status | Android RunTrace row | conclusion |
|---|---|---|---|---|---|---|---|---|
| 1 | `4a6bc46e-7e10-4b9c-b680-97f2cbee51b7:1` | `run_started` | `run_started` | `` | `n/a` | `n/a` | Run lifecycle row | pass |
| 2 | `4a6bc46e-7e10-4b9c-b680-97f2cbee51b7:2` | `safety_check_started` | `safety_check_started` | `` | `n/a` | `n/a` | RunTrace process row | pass |
| 3 | `4a6bc46e-7e10-4b9c-b680-97f2cbee51b7:3` | `safety_check_passed` | `safety_check_passed` | `` | `n/a` | `n/a` | RunTrace process row | pass |
| 4 | `4a6bc46e-7e10-4b9c-b680-97f2cbee51b7:4` | `plan_delta` | `plan_delta` | `` | `n/a` | `n/a` | RunTrace process row | pass |
| 5 | `4a6bc46e-7e10-4b9c-b680-97f2cbee51b7:5` | `tool_started` | `tool_started` | `` | `n/a` | `n/a` | RunTrace tool card: customer_receivable_lookup | pass |
| 6 | `4a6bc46e-7e10-4b9c-b680-97f2cbee51b7:6` | `tool_completed` | `tool_completed` | `` | `n/a` | `n/a` | RunTrace tool card: customer_receivable_lookup | pass |
| 7 | `4a6bc46e-7e10-4b9c-b680-97f2cbee51b7:7` | `answer_completed` | `answer_completed` | `` | `tool_query_rule_summary` | `disabled` | Chat answer / completion state | pass |
| 8 | `4a6bc46e-7e10-4b9c-b680-97f2cbee51b7:8` | `result_block` | `result_block` | `` | `n/a` | `n/a` | RunTrace process row | pass |
| 9 | `4a6bc46e-7e10-4b9c-b680-97f2cbee51b7:9` | `result_block` | `result_block` | `` | `n/a` | `n/a` | RunTrace process row | pass |
| 10 | `4a6bc46e-7e10-4b9c-b680-97f2cbee51b7:10` | `result_block` | `result_block` | `` | `n/a` | `n/a` | RunTrace process row | pass |
| 11 | `4a6bc46e-7e10-4b9c-b680-97f2cbee51b7:11` | `result_block` | `result_block` | `` | `n/a` | `n/a` | RunTrace process row | pass |
| 12 | `4a6bc46e-7e10-4b9c-b680-97f2cbee51b7:12` | `run_completed` | `run_completed` | `` | `tool_query_rule_summary` | `disabled` | Run lifecycle row | pass |

Status: pass-for-interface

SSE and server audit events match by seq, event_id, event_type. answer_delta events are limited to model_stream or post-model server_notice. Android UI evidence is still required before full P0 pass.
