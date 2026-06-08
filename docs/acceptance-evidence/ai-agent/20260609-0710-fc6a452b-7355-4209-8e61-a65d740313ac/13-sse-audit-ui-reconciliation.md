# SSE / Audit / UI Reconciliation

| seq | event_id | raw SSE event_type | audit event_type | delta_source | mode | llm_status | Android RunTrace row | conclusion |
|---|---|---|---|---|---|---|---|---|
| 1 | `fc6a452b-7355-4209-8e61-a65d740313ac:1` | `run_started` | `run_started` | `` | `n/a` | `n/a` | Run lifecycle row | pass |
| 2 | `fc6a452b-7355-4209-8e61-a65d740313ac:2` | `safety_check_started` | `safety_check_started` | `` | `n/a` | `n/a` | RunTrace process row | pass |
| 3 | `fc6a452b-7355-4209-8e61-a65d740313ac:3` | `safety_check_passed` | `safety_check_passed` | `` | `n/a` | `n/a` | RunTrace process row | pass |
| 4 | `fc6a452b-7355-4209-8e61-a65d740313ac:4` | `plan_delta` | `plan_delta` | `` | `n/a` | `n/a` | RunTrace process row | pass |
| 5 | `fc6a452b-7355-4209-8e61-a65d740313ac:5` | `tool_started` | `tool_started` | `` | `n/a` | `n/a` | RunTrace tool card: inventory_low_stock_lookup | pass |
| 6 | `fc6a452b-7355-4209-8e61-a65d740313ac:6` | `tool_completed` | `tool_completed` | `` | `n/a` | `n/a` | RunTrace tool card: inventory_low_stock_lookup | pass |
| 7 | `fc6a452b-7355-4209-8e61-a65d740313ac:7` | `tool_started` | `tool_started` | `` | `n/a` | `n/a` | RunTrace tool card: customer_receivable_lookup | pass |
| 8 | `fc6a452b-7355-4209-8e61-a65d740313ac:8` | `tool_completed` | `tool_completed` | `` | `n/a` | `n/a` | RunTrace tool card: customer_receivable_lookup | pass |
| 9 | `fc6a452b-7355-4209-8e61-a65d740313ac:9` | `answer_completed` | `answer_completed` | `` | `tool_query_rule_summary` | `disabled` | Chat answer / completion state | pass |
| 10 | `fc6a452b-7355-4209-8e61-a65d740313ac:10` | `result_block` | `result_block` | `` | `n/a` | `n/a` | RunTrace process row | pass |
| 11 | `fc6a452b-7355-4209-8e61-a65d740313ac:11` | `result_block` | `result_block` | `` | `n/a` | `n/a` | RunTrace process row | pass |
| 12 | `fc6a452b-7355-4209-8e61-a65d740313ac:12` | `result_block` | `result_block` | `` | `n/a` | `n/a` | RunTrace process row | pass |
| 13 | `fc6a452b-7355-4209-8e61-a65d740313ac:13` | `result_block` | `result_block` | `` | `n/a` | `n/a` | RunTrace process row | pass |
| 14 | `fc6a452b-7355-4209-8e61-a65d740313ac:14` | `result_block` | `result_block` | `` | `n/a` | `n/a` | RunTrace process row | pass |
| 15 | `fc6a452b-7355-4209-8e61-a65d740313ac:15` | `result_block` | `result_block` | `` | `n/a` | `n/a` | RunTrace process row | pass |
| 16 | `fc6a452b-7355-4209-8e61-a65d740313ac:16` | `run_completed` | `run_completed` | `` | `tool_query_rule_summary` | `disabled` | Run lifecycle row | pass |

Status: pass-for-interface

SSE and server audit events match by seq, event_id, event_type. answer_delta events are limited to model_stream or post-model server_notice. Android UI evidence is still required before full P0 pass.
