# SSE / Audit / UI Reconciliation

| seq | event_id | raw SSE event_type | audit event_type | Android RunTrace row | conclusion |
|---|---|---|---|---|---|
| 1 | `6a102f63-93fc-43cc-b655-cb140cb8c79e:1` | `run_started` | `run_started` | Run lifecycle row | pass |
| 2 | `6a102f63-93fc-43cc-b655-cb140cb8c79e:2` | `safety_check_started` | `safety_check_started` | RunTrace process row | pass |
| 3 | `6a102f63-93fc-43cc-b655-cb140cb8c79e:3` | `safety_check_passed` | `safety_check_passed` | RunTrace process row | pass |
| 4 | `6a102f63-93fc-43cc-b655-cb140cb8c79e:4` | `plan_delta` | `plan_delta` | RunTrace process row | pass |
| 5 | `6a102f63-93fc-43cc-b655-cb140cb8c79e:5` | `tool_started` | `tool_started` | RunTrace tool card: supplier_payable_lookup | pass |
| 6 | `6a102f63-93fc-43cc-b655-cb140cb8c79e:6` | `tool_completed` | `tool_completed` | RunTrace tool card: supplier_payable_lookup | pass |
| 7 | `6a102f63-93fc-43cc-b655-cb140cb8c79e:7` | `result_block` | `result_block` | RunTrace process row | pass |
| 8 | `6a102f63-93fc-43cc-b655-cb140cb8c79e:8` | `result_block` | `result_block` | RunTrace process row | pass |
| 9 | `6a102f63-93fc-43cc-b655-cb140cb8c79e:9` | `result_block` | `result_block` | RunTrace process row | pass |
| 10 | `6a102f63-93fc-43cc-b655-cb140cb8c79e:10` | `tool_started` | `tool_started` | RunTrace tool card: purchase_order_lookup | pass |
| 11 | `6a102f63-93fc-43cc-b655-cb140cb8c79e:11` | `tool_completed` | `tool_completed` | RunTrace tool card: purchase_order_lookup | pass |
| 12 | `6a102f63-93fc-43cc-b655-cb140cb8c79e:12` | `result_block` | `result_block` | RunTrace process row | pass |
| 13 | `6a102f63-93fc-43cc-b655-cb140cb8c79e:13` | `result_block` | `result_block` | RunTrace process row | pass |
| 14 | `6a102f63-93fc-43cc-b655-cb140cb8c79e:14` | `tool_started` | `tool_started` | RunTrace tool card: finance_record_lookup | pass |
| 15 | `6a102f63-93fc-43cc-b655-cb140cb8c79e:15` | `tool_completed` | `tool_completed` | RunTrace tool card: finance_record_lookup | pass |
| 16 | `6a102f63-93fc-43cc-b655-cb140cb8c79e:16` | `result_block` | `result_block` | RunTrace process row | pass |
| 17 | `6a102f63-93fc-43cc-b655-cb140cb8c79e:17` | `result_block` | `result_block` | RunTrace process row | pass |
| 18 | `6a102f63-93fc-43cc-b655-cb140cb8c79e:18` | `result_block` | `result_block` | RunTrace process row | pass |
| 19 | `6a102f63-93fc-43cc-b655-cb140cb8c79e:19` | `tool_started` | `tool_started` | RunTrace tool card: sales_overview_lookup | pass |
| 20 | `6a102f63-93fc-43cc-b655-cb140cb8c79e:20` | `tool_completed` | `tool_completed` | RunTrace tool card: sales_overview_lookup | pass |
| 21 | `6a102f63-93fc-43cc-b655-cb140cb8c79e:21` | `result_block` | `result_block` | RunTrace process row | pass |
| 22 | `6a102f63-93fc-43cc-b655-cb140cb8c79e:22` | `result_block` | `result_block` | RunTrace process row | pass |
| 23 | `6a102f63-93fc-43cc-b655-cb140cb8c79e:23` | `result_block` | `result_block` | RunTrace process row | pass |
| 24 | `6a102f63-93fc-43cc-b655-cb140cb8c79e:24` | `result_block` | `result_block` | RunTrace process row | pass |
| 25 | `6a102f63-93fc-43cc-b655-cb140cb8c79e:25` | `result_block` | `result_block` | RunTrace process row | pass |
| 26 | `6a102f63-93fc-43cc-b655-cb140cb8c79e:26` | `result_block` | `result_block` | RunTrace process row | pass |
| 27 | `6a102f63-93fc-43cc-b655-cb140cb8c79e:27` | `answer_completed` | `answer_completed` | Chat answer / completion state | pass |
| 28 | `6a102f63-93fc-43cc-b655-cb140cb8c79e:28` | `run_completed` | `run_completed` | Run lifecycle row | pass |

Status: pass-for-interface

SSE and server audit events match by seq, event_id, and event_type. Android UI evidence is still required before full P0 pass.
