# AI Agent Performance Summary

Status: `partial-rule-summary-performance`

## Sample Table

| Question | Iteration | Run | HTTP | Mode | LLM | First event | First tool | First answer delta | Answer completed | First result block | Run completed | Total |
|---|---:|---|---:|---|---|---:|---:|---:|---:|---:|---:|---:|
| 库存和客户应收情况 | 1 | `63b3316c-90c8-4892-845a-aa4e50bd4194` | 200 | `tool_query_rule_summary` | `disabled` | 529.4 | 664.39 | missing | 757.82 | 766.24 | 819.56 | 857.59 |
| 客户应收情况 | 1 | `7f89d81e-f70c-402e-afab-ad2ee051bf93` | 200 | `tool_query_rule_summary` | `disabled` | 17.54 | 76.36 | missing | 117.72 | 120.94 | 173.31 | 202.63 |
| 最近销售采购和财务情况怎么样？ | 1 | `88a32564-5319-4191-a406-75bc26ee8949` | 200 | `tool_query_rule_summary` | `disabled` | 28.31 | 63.79 | missing | 334.74 | 346.6 | 494.2 | 494.3 |

## Aggregate Metrics

| Metric | Count | P50 ms | P95 ms | Max ms | Mean ms |
|---|---:|---:|---:|---:|---:|
| `time_to_headers_ms` | `3` | `26.99` | `509.16` | `509.16` | `183.16` |
| `total_response_ms` | `3` | `494.3` | `857.59` | `857.59` | `518.17` |
| `first_event_latency_ms` | `3` | `28.31` | `529.4` | `529.4` | `191.75` |
| `first_tool_started_latency_ms` | `3` | `76.36` | `664.39` | `664.39` | `268.18` |
| `first_tool_completed_latency_ms` | `3` | `87.86` | `692.66` | `692.66` | `285.36` |
| `first_answer_delta_latency_ms` | `0` | `missing` | `missing` | `missing` | `missing` |
| `first_model_stream_delta_latency_ms` | `0` | `missing` | `missing` | `missing` | `missing` |
| `answer_completed_latency_ms` | `3` | `334.74` | `757.82` | `757.82` | `403.43` |
| `first_result_block_latency_ms` | `3` | `346.6` | `766.24` | `766.24` | `411.26` |
| `run_completed_latency_ms` | `3` | `494.2` | `819.56` | `819.56` | `495.69` |
| `tool_duration_sum_ms` | `3` | `41.0` | `152.0` | `152.0` | `71.33` |
| `tool_duration_max_ms` | `3` | `25.0` | `64.0` | `64.0` | `36.67` |

## Review Notes

- HTTP successful samples: `3/3`.
- Completed samples: `3/3`.
- Provider `model_stream` samples: `0/3`.
- Result-before-model-delta samples: `0`.
- Result-before-answer-completed non-model samples: `0`.
- Server-notice-before-model samples: `0`.

If provider `model_stream` count is zero, this package only supports rule-summary / non-model timing and must remain partial for ChatGPT-like streaming acceptance.
If Android frame timing is not attached, this package cannot pass the high-refresh mobile performance requirement by itself.
