# SSE / Audit / UI Reconciliation

| seq | event_id | raw SSE event_type | audit event_type | delta_source | mode | llm_status | Android RunTrace row | conclusion |
|---|---|---|---|---|---|---|---|---|
| 1 | `03d3daad-6633-4c14-b6cf-b55d39820452:1` | `run_started` | `run_started` | `` | `n/a` | `n/a` | Run lifecycle row | pass |
| 2 | `03d3daad-6633-4c14-b6cf-b55d39820452:2` | `safety_check_started` | `safety_check_started` | `` | `n/a` | `n/a` | RunTrace process row | pass |
| 3 | `03d3daad-6633-4c14-b6cf-b55d39820452:3` | `safety_check_passed` | `safety_check_passed` | `` | `n/a` | `n/a` | RunTrace process row | pass |
| 4 | `03d3daad-6633-4c14-b6cf-b55d39820452:4` | `plan_delta` | `plan_delta` | `` | `n/a` | `n/a` | RunTrace process row | pass |
| 5 | `03d3daad-6633-4c14-b6cf-b55d39820452:5` | `tool_started` | `tool_started` | `` | `n/a` | `n/a` | RunTrace tool card: inventory_low_stock_lookup | pass |
| 6 | `03d3daad-6633-4c14-b6cf-b55d39820452:6` | `tool_completed` | `tool_completed` | `` | `n/a` | `n/a` | RunTrace tool card: inventory_low_stock_lookup | pass |
| 7 | `03d3daad-6633-4c14-b6cf-b55d39820452:7` | `result_block` | `result_block` | `` | `n/a` | `n/a` | RunTrace process row | pass |
| 8 | `03d3daad-6633-4c14-b6cf-b55d39820452:8` | `result_block` | `result_block` | `` | `n/a` | `n/a` | RunTrace process row | pass |
| 9 | `03d3daad-6633-4c14-b6cf-b55d39820452:9` | `tool_started` | `tool_started` | `` | `n/a` | `n/a` | RunTrace tool card: product_catalog_lookup | pass |
| 10 | `03d3daad-6633-4c14-b6cf-b55d39820452:10` | `tool_completed` | `tool_completed` | `` | `n/a` | `n/a` | RunTrace tool card: product_catalog_lookup | pass |
| 11 | `03d3daad-6633-4c14-b6cf-b55d39820452:11` | `result_block` | `result_block` | `` | `n/a` | `n/a` | RunTrace process row | pass |
| 12 | `03d3daad-6633-4c14-b6cf-b55d39820452:12` | `result_block` | `result_block` | `` | `n/a` | `n/a` | RunTrace process row | pass |
| 13 | `03d3daad-6633-4c14-b6cf-b55d39820452:13` | `result_block` | `result_block` | `` | `n/a` | `n/a` | RunTrace process row | pass |
| 14 | `03d3daad-6633-4c14-b6cf-b55d39820452:14` | `answer_completed` | `answer_completed` | `` | `tool_query_rule_summary` | `disabled` | Chat answer / completion state | pass |
| 15 | `03d3daad-6633-4c14-b6cf-b55d39820452:15` | `run_completed` | `run_completed` | `` | `tool_query_rule_summary` | `disabled` | Run lifecycle row | pass |

Status: pass-for-interface

SSE and server audit events match by seq, event_id, event_type. answer_delta events are limited to model_stream or post-model server_notice. Android UI evidence is still required before full P0 pass.
