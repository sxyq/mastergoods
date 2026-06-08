# SSE / Audit / UI Reconciliation

| seq | event_id | raw SSE event_type | audit event_type | delta_source | mode | llm_status | Android RunTrace row | conclusion |
|---|---|---|---|---|---|---|---|---|
| 1 | `8097192b-eeeb-4ecd-be3d-c5977fb4c095:1` | `run_started` | `run_started` | `n/a` | `n/a` | `n/a` | Run lifecycle row | pass |
| 2 | `8097192b-eeeb-4ecd-be3d-c5977fb4c095:2` | `safety_check_started` | `safety_check_started` | `n/a` | `n/a` | `n/a` | RunTrace process row | pass |
| 3 | `8097192b-eeeb-4ecd-be3d-c5977fb4c095:3` | `safety_check_passed` | `safety_check_passed` | `n/a` | `n/a` | `n/a` | RunTrace process row | pass |
| 4 | `8097192b-eeeb-4ecd-be3d-c5977fb4c095:4` | `plan_delta` | `plan_delta` | `n/a` | `n/a` | `n/a` | RunTrace process row | pass |
| 5 | `8097192b-eeeb-4ecd-be3d-c5977fb4c095:5` | `tool_started` | `tool_started` | `n/a` | `n/a` | `n/a` | RunTrace tool card: inventory_low_stock_lookup | pass |
| 6 | `8097192b-eeeb-4ecd-be3d-c5977fb4c095:6` | `tool_completed` | `tool_completed` | `n/a` | `n/a` | `n/a` | RunTrace tool card: inventory_low_stock_lookup | pass |
| 7 | `8097192b-eeeb-4ecd-be3d-c5977fb4c095:7` | `result_block` | `result_block` | `n/a` | `n/a` | `n/a` | RunTrace process row | pass |
| 8 | `8097192b-eeeb-4ecd-be3d-c5977fb4c095:8` | `result_block` | `result_block` | `n/a` | `n/a` | `n/a` | RunTrace process row | pass |
| 9 | `8097192b-eeeb-4ecd-be3d-c5977fb4c095:9` | `tool_started` | `tool_started` | `n/a` | `n/a` | `n/a` | RunTrace tool card: product_catalog_lookup | pass |
| 10 | `8097192b-eeeb-4ecd-be3d-c5977fb4c095:10` | `tool_completed` | `tool_completed` | `n/a` | `n/a` | `n/a` | RunTrace tool card: product_catalog_lookup | pass |
| 11 | `8097192b-eeeb-4ecd-be3d-c5977fb4c095:11` | `result_block` | `result_block` | `n/a` | `n/a` | `n/a` | RunTrace process row | pass |
| 12 | `8097192b-eeeb-4ecd-be3d-c5977fb4c095:12` | `result_block` | `result_block` | `n/a` | `n/a` | `n/a` | RunTrace process row | pass |
| 13 | `8097192b-eeeb-4ecd-be3d-c5977fb4c095:13` | `result_block` | `result_block` | `n/a` | `n/a` | `n/a` | RunTrace process row | pass |
| 14 | `8097192b-eeeb-4ecd-be3d-c5977fb4c095:14` | `answer_completed` | `answer_completed` | `n/a` | `tool_query_rule_summary` | `disabled` | Chat answer / completion state | pass |
| 15 | `8097192b-eeeb-4ecd-be3d-c5977fb4c095:15` | `run_completed` | `run_completed` | `n/a` | `tool_query_rule_summary` | `disabled` | Run lifecycle row | pass |

Status: pass-for-interface

SSE and server audit events match by seq, event_id, event_type, and any answer_delta events declare delta_source=model_stream. Android UI evidence is still required before full P0 pass.
