# SSE / Audit / UI Reconciliation

| seq | event_id | raw SSE event_type | audit event_type | Android RunTrace row | conclusion |
|---|---|---|---|---|---|
| 1 | `9151d633-3557-43a0-b92a-0523f850e182:1` | `run_started` | `run_started` | Run lifecycle row | pass |
| 2 | `9151d633-3557-43a0-b92a-0523f850e182:2` | `safety_check_started` | `safety_check_started` | RunTrace process row | pass |
| 3 | `9151d633-3557-43a0-b92a-0523f850e182:3` | `safety_check_passed` | `safety_check_passed` | RunTrace process row | pass |
| 4 | `9151d633-3557-43a0-b92a-0523f850e182:4` | `plan_delta` | `plan_delta` | RunTrace process row | pass |
| 5 | `9151d633-3557-43a0-b92a-0523f850e182:5` | `tool_started` | `tool_started` | RunTrace tool card: inventory_low_stock_lookup | pass |
| 6 | `9151d633-3557-43a0-b92a-0523f850e182:6` | `tool_completed` | `tool_completed` | RunTrace tool card: inventory_low_stock_lookup | pass |
| 7 | `9151d633-3557-43a0-b92a-0523f850e182:7` | `result_block` | `result_block` | RunTrace process row | pass |
| 8 | `9151d633-3557-43a0-b92a-0523f850e182:8` | `result_block` | `result_block` | RunTrace process row | pass |
| 9 | `9151d633-3557-43a0-b92a-0523f850e182:9` | `tool_started` | `tool_started` | RunTrace tool card: product_catalog_lookup | pass |
| 10 | `9151d633-3557-43a0-b92a-0523f850e182:10` | `tool_completed` | `tool_completed` | RunTrace tool card: product_catalog_lookup | pass |
| 11 | `9151d633-3557-43a0-b92a-0523f850e182:11` | `result_block` | `result_block` | RunTrace process row | pass |
| 12 | `9151d633-3557-43a0-b92a-0523f850e182:12` | `result_block` | `result_block` | RunTrace process row | pass |
| 13 | `9151d633-3557-43a0-b92a-0523f850e182:13` | `result_block` | `result_block` | RunTrace process row | pass |
| 14 | `9151d633-3557-43a0-b92a-0523f850e182:14` | `answer_completed` | `answer_completed` | Chat answer / completion state | pass |
| 15 | `9151d633-3557-43a0-b92a-0523f850e182:15` | `run_completed` | `run_completed` | Run lifecycle row | pass |

Status: pass-for-interface

SSE and server audit events match by seq, event_id, and event_type. Android UI evidence is still required before full P0 pass.
