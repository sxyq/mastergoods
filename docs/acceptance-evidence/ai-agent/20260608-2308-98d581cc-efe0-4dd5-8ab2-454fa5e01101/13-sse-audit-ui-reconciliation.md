# SSE / Audit / UI Reconciliation

| seq | event_id | raw SSE event_type | audit event_type | Android RunTrace row | conclusion |
|---|---|---|---|---|---|
| 1 | `98d581cc-efe0-4dd5-8ab2-454fa5e01101:1` | `run_started` | `run_started` | Run lifecycle row | pass |
| 2 | `98d581cc-efe0-4dd5-8ab2-454fa5e01101:2` | `safety_check_started` | `safety_check_started` | RunTrace process row | pass |
| 3 | `98d581cc-efe0-4dd5-8ab2-454fa5e01101:3` | `safety_check_passed` | `safety_check_passed` | RunTrace process row | pass |
| 4 | `98d581cc-efe0-4dd5-8ab2-454fa5e01101:4` | `plan_delta` | `plan_delta` | RunTrace process row | pass |
| 5 | `98d581cc-efe0-4dd5-8ab2-454fa5e01101:5` | `tool_started` | `tool_started` | RunTrace tool card: supplier_payable_lookup | pass |
| 6 | `98d581cc-efe0-4dd5-8ab2-454fa5e01101:6` | `tool_completed` | `tool_completed` | RunTrace tool card: supplier_payable_lookup | pass |
| 7 | `98d581cc-efe0-4dd5-8ab2-454fa5e01101:7` | `result_block` | `result_block` | RunTrace process row | pass |
| 8 | `98d581cc-efe0-4dd5-8ab2-454fa5e01101:8` | `result_block` | `result_block` | RunTrace process row | pass |
| 9 | `98d581cc-efe0-4dd5-8ab2-454fa5e01101:9` | `result_block` | `result_block` | RunTrace process row | pass |
| 10 | `98d581cc-efe0-4dd5-8ab2-454fa5e01101:10` | `tool_started` | `tool_started` | RunTrace tool card: purchase_order_lookup | pass |
| 11 | `98d581cc-efe0-4dd5-8ab2-454fa5e01101:11` | `tool_completed` | `tool_completed` | RunTrace tool card: purchase_order_lookup | pass |
| 12 | `98d581cc-efe0-4dd5-8ab2-454fa5e01101:12` | `result_block` | `result_block` | RunTrace process row | pass |
| 13 | `98d581cc-efe0-4dd5-8ab2-454fa5e01101:13` | `result_block` | `result_block` | RunTrace process row | pass |
| 14 | `98d581cc-efe0-4dd5-8ab2-454fa5e01101:14` | `tool_started` | `tool_started` | RunTrace tool card: finance_record_lookup | pass |
| 15 | `98d581cc-efe0-4dd5-8ab2-454fa5e01101:15` | `tool_completed` | `tool_completed` | RunTrace tool card: finance_record_lookup | pass |
| 16 | `98d581cc-efe0-4dd5-8ab2-454fa5e01101:16` | `result_block` | `result_block` | RunTrace process row | pass |
| 17 | `98d581cc-efe0-4dd5-8ab2-454fa5e01101:17` | `result_block` | `result_block` | RunTrace process row | pass |
| 18 | `98d581cc-efe0-4dd5-8ab2-454fa5e01101:18` | `result_block` | `result_block` | RunTrace process row | pass |
| 19 | `98d581cc-efe0-4dd5-8ab2-454fa5e01101:19` | `tool_started` | `tool_started` | RunTrace tool card: sales_overview_lookup | pass |
| 20 | `98d581cc-efe0-4dd5-8ab2-454fa5e01101:20` | `tool_completed` | `tool_completed` | RunTrace tool card: sales_overview_lookup | pass |
| 21 | `98d581cc-efe0-4dd5-8ab2-454fa5e01101:21` | `result_block` | `result_block` | RunTrace process row | pass |
| 22 | `98d581cc-efe0-4dd5-8ab2-454fa5e01101:22` | `result_block` | `result_block` | RunTrace process row | pass |
| 23 | `98d581cc-efe0-4dd5-8ab2-454fa5e01101:23` | `result_block` | `result_block` | RunTrace process row | pass |
| 24 | `98d581cc-efe0-4dd5-8ab2-454fa5e01101:24` | `result_block` | `result_block` | RunTrace process row | pass |
| 25 | `98d581cc-efe0-4dd5-8ab2-454fa5e01101:25` | `result_block` | `result_block` | RunTrace process row | pass |
| 26 | `98d581cc-efe0-4dd5-8ab2-454fa5e01101:26` | `result_block` | `result_block` | RunTrace process row | pass |
| 27 | `98d581cc-efe0-4dd5-8ab2-454fa5e01101:27` | `answer_completed` | `answer_completed` | Chat answer / completion state | pass |
| 28 | `98d581cc-efe0-4dd5-8ab2-454fa5e01101:28` | `run_completed` | `run_completed` | Run lifecycle row | pass |

Status: pass-for-interface

SSE and server audit events match by seq, event_id, and event_type. Android UI evidence is still required before full P0 pass.
