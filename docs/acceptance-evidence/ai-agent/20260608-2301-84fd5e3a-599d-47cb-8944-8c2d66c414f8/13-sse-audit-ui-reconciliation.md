# SSE / Audit / UI Reconciliation

| seq | event_id | raw SSE event_type | audit event_type | Android RunTrace row | conclusion |
|---|---|---|---|---|---|
| 1 | `84fd5e3a-599d-47cb-8944-8c2d66c414f8:1` | `run_started` | `run_started` | Run lifecycle row | pass |
| 2 | `84fd5e3a-599d-47cb-8944-8c2d66c414f8:2` | `safety_check_started` | `safety_check_started` | RunTrace process row | pass |
| 3 | `84fd5e3a-599d-47cb-8944-8c2d66c414f8:3` | `safety_check_passed` | `safety_check_passed` | RunTrace process row | pass |
| 4 | `84fd5e3a-599d-47cb-8944-8c2d66c414f8:4` | `plan_delta` | `plan_delta` | RunTrace process row | pass |
| 5 | `84fd5e3a-599d-47cb-8944-8c2d66c414f8:5` | `tool_started` | `tool_started` | RunTrace tool card: inventory_low_stock_lookup | pass |
| 6 | `84fd5e3a-599d-47cb-8944-8c2d66c414f8:6` | `tool_completed` | `tool_completed` | RunTrace tool card: inventory_low_stock_lookup | pass |
| 7 | `84fd5e3a-599d-47cb-8944-8c2d66c414f8:7` | `result_block` | `result_block` | RunTrace process row | pass |
| 8 | `84fd5e3a-599d-47cb-8944-8c2d66c414f8:8` | `result_block` | `result_block` | RunTrace process row | pass |
| 9 | `84fd5e3a-599d-47cb-8944-8c2d66c414f8:9` | `tool_started` | `tool_started` | RunTrace tool card: product_catalog_lookup | pass |
| 10 | `84fd5e3a-599d-47cb-8944-8c2d66c414f8:10` | `tool_completed` | `tool_completed` | RunTrace tool card: product_catalog_lookup | pass |
| 11 | `84fd5e3a-599d-47cb-8944-8c2d66c414f8:11` | `result_block` | `result_block` | RunTrace process row | pass |
| 12 | `84fd5e3a-599d-47cb-8944-8c2d66c414f8:12` | `result_block` | `result_block` | RunTrace process row | pass |
| 13 | `84fd5e3a-599d-47cb-8944-8c2d66c414f8:13` | `result_block` | `result_block` | RunTrace process row | pass |
| 14 | `84fd5e3a-599d-47cb-8944-8c2d66c414f8:14` | `answer_completed` | `answer_completed` | Chat answer / completion state | pass |
| 15 | `84fd5e3a-599d-47cb-8944-8c2d66c414f8:15` | `run_completed` | `run_completed` | Run lifecycle row | pass |

Status: pass-for-interface

SSE and server audit events match by seq, event_id, and event_type. Android UI evidence is still required before full P0 pass.
