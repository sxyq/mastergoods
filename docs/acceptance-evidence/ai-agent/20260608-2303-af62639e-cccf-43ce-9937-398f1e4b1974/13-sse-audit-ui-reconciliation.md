# SSE / Audit / UI Reconciliation

| seq | event_id | raw SSE event_type | audit event_type | Android RunTrace row | conclusion |
|---|---|---|---|---|---|
| 1 | `af62639e-cccf-43ce-9937-398f1e4b1974:1` | `run_started` | `run_started` | Run lifecycle row | pass |
| 2 | `af62639e-cccf-43ce-9937-398f1e4b1974:2` | `safety_check_started` | `safety_check_started` | RunTrace process row | pass |
| 3 | `af62639e-cccf-43ce-9937-398f1e4b1974:3` | `safety_check_passed` | `safety_check_passed` | RunTrace process row | pass |
| 4 | `af62639e-cccf-43ce-9937-398f1e4b1974:4` | `plan_delta` | `plan_delta` | RunTrace process row | pass |
| 5 | `af62639e-cccf-43ce-9937-398f1e4b1974:5` | `tool_started` | `tool_started` | RunTrace tool card: customer_receivable_lookup | pass |
| 6 | `af62639e-cccf-43ce-9937-398f1e4b1974:6` | `tool_completed` | `tool_completed` | RunTrace tool card: customer_receivable_lookup | pass |
| 7 | `af62639e-cccf-43ce-9937-398f1e4b1974:7` | `result_block` | `result_block` | RunTrace process row | pass |
| 8 | `af62639e-cccf-43ce-9937-398f1e4b1974:8` | `result_block` | `result_block` | RunTrace process row | pass |
| 9 | `af62639e-cccf-43ce-9937-398f1e4b1974:9` | `result_block` | `result_block` | RunTrace process row | pass |
| 10 | `af62639e-cccf-43ce-9937-398f1e4b1974:10` | `tool_started` | `tool_started` | RunTrace tool card: sale_order_lookup | pass |
| 11 | `af62639e-cccf-43ce-9937-398f1e4b1974:11` | `tool_completed` | `tool_completed` | RunTrace tool card: sale_order_lookup | pass |
| 12 | `af62639e-cccf-43ce-9937-398f1e4b1974:12` | `result_block` | `result_block` | RunTrace process row | pass |
| 13 | `af62639e-cccf-43ce-9937-398f1e4b1974:13` | `result_block` | `result_block` | RunTrace process row | pass |
| 14 | `af62639e-cccf-43ce-9937-398f1e4b1974:14` | `result_block` | `result_block` | RunTrace process row | pass |
| 15 | `af62639e-cccf-43ce-9937-398f1e4b1974:15` | `answer_completed` | `answer_completed` | Chat answer / completion state | pass |
| 16 | `af62639e-cccf-43ce-9937-398f1e4b1974:16` | `run_completed` | `run_completed` | Run lifecycle row | pass |

Status: pass-for-interface

SSE and server audit events match by seq, event_id, and event_type. Android UI evidence is still required before full P0 pass.
