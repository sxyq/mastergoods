# AI Agent Latency

## curl timings

http_code=200
time_namelookup=0.000007
time_connect=0.000174
time_starttransfer=0.019792
time_total=0.056567

## audit timings

- started_at: 1780962026063
- completed_at: 1780962026111
- audit_duration_ms: 48
- tool_count: 1
- event_count: 12

## AI run timing summary

| Metric | Value |
|---|---|
| `first_event_latency_ms` | `16` |
| `first_tool_started_latency_ms` | `26` |
| `first_tool_completed_latency_ms` | `30` |
| `first_result_block_latency_ms` | `40` |
| `first_answer_delta_latency_ms` | `missing` |
| `first_model_stream_delta_latency_ms` | `missing` |
| `first_server_notice_delta_latency_ms` | `missing` |
| `answer_completed_latency_ms` | `38` |
| `run_completed_latency_ms` | `47` |
| `duration_ms` | `48` |
| `tool_duration_sum_ms` | `4` |
| `tool_duration_max_ms` | `4` |
| `tool_started_count` | `1` |
| `tool_completed_count` | `1` |
| `tool_failed_count` | `0` |
| `result_block_count` | `4` |
| `answer_delta_count` | `0` |
| `model_stream_delta_count` | `0` |
| `server_notice_delta_count` | `0` |
| `answer_completed_count` | `1` |
| `stream_interrupted_count` | `0` |
| `run_completed_count` | `1` |

## Performance review notes

- Provider-backed `model_stream` timing is missing; this run can only support rule-summary or non-model interface timing.
- Result blocks followed `answer_completed`; verify Android shows the completed answer before structured data.
- No tool_failed event was observed in this run.
- No stream_interrupted completion was observed in this run.

## UI timing

Android first-visible timing is not captured by this script. Add device-side
screen recording, logcat, or frame timing evidence before marking this package
pass.
