# Local lazy-init route probe

- `test_id`: `AG-F-LIVE-LAZY-ROUTE-001`
- `category_id`: `AG-F-ENV-LAZY`
- `wave_id`: `Wave 0`
- `result`: `Blocked`
- `environment`: macOS 本机；当前源码；Gradle wrapper 8.7；Java 21.0.11；Spring Boot 3.2.6；profile `local`；H2 file；port `18080`。
- `source_commit`: `93d085420076f4f2b6fd47faa0b662e45f029976` application source; the evidence commit is `a65f9475`.
- `service_pid`: `26537` during probe; process stopped after evidence capture.
- `account_store`: `anonymous / redacted`。
- `provider`: outbound LLM/Provider call disabled; no credential supplied.

This is an independent local HTTP client probe, not Android UI evidence. The alternate runtime option was `--spring.main.lazy-initialization=true`.
