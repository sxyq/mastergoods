# Agent live 阶段报告

- `wave_id`: `20260829-agent-live-lazy-01`
- `scope`: local `bootRun` alternate startup and anonymous Agent route boundary.
- `result`: `Blocked`
- `source_commit`: `93d085420076f4f2b6fd47faa0b662e45f029976` application source; no source/config change in this batch.

## Batch result

- `spring.main.lazy-initialization=true` allowed the current source to start on `18080`; the eager `AdminAgentController` construction failure was avoided at startup.
- Anonymous probes of health, tools, chat, stream, and image REST returned JSON `403`.
- No request reached `ToolPlanner`, `ToolExecutor`, Agent business services, audit, or the H2 business snapshot path.
- No Provider request was made. No Android Agent session or `run_id` was available.

## Evidence and limits

- Detailed fixed-layout evidence: `testing/Agent/功能/artifacts/20260829-agent-live-lazy-route-probe-01/`.
- Batch ledger: `testing/Agent/功能/reports/live_execution_ledger.csv`.
- The alternative startup confirms process availability and the anonymous security boundary only. Authenticated tool selection, SSE terminal state, owner/store checks, image generation, and database alignment remain untested until an approved session is supplied.
