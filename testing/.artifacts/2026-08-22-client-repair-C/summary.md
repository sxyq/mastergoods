# Client repair evidence: C

## Scope

- Environment: local source tree on `codex/publish-local-updates`; current remote/API records remain 8220 scope.
- Historical 154 server and July device artifacts were read as historical evidence only and were not used as current runtime results.
- No browser click, Playwright, login, adb, APK, emulator, device, screenshot, or logcat execution.
- No credentials, cookies, tokens, or complete authentication payloads recorded.

## Results

| test_id | category_id | wave_id | environment | account/store | precondition | operation | expected | actual | evidence_path | cleanup | result |
|---|---|---|---|---|---|---|---|---|---|---|---|
| WEB-C-001 | WEB-SOURCE | client-repair-C | local source | none | AgentPage existing user changes preserved | `vue-tsc -b && vite build` | Web source type-checks and builds | 116 modules transformed; build succeeded | `testing/.artifacts/2026-08-22-client-repair-C/summary.md` | build output outside Git | Passed |
| AND-C-001 | AND-CONTRACT | client-repair-C | local source | none | Android API/model sources | debug compile and unit tests for core:model, core:network, data:sync | affected modules compile and tests pass | 85 Gradle tasks succeeded; all selected tests passed | `testing/.artifacts/2026-08-22-client-repair-C/summary.md` | Gradle temporary output | Passed |
| WEB-C-002 | WEB-RUNTIME | client-repair-C | current 8220 unavailable to browser test | no authenticated session | no browser runtime/login | click and SSE lifecycle evidence | no runtime evidence was collected | browser and real login excluded by scope | `testing/web/功能测试/阶段报告-20260822-client-repair-C.md` | none | Blocked |
| AND-C-002 | AND-RUNTIME | client-repair-C | current 8220, no device | no authenticated session | no adb/device | real HTTP, UI and SSE device evidence | no device evidence was collected | adb/device execution excluded by scope | `testing/安卓/功能测试/阶段报告-20260822-client-repair-C.md` | none | Blocked |

## Code checks

- `git diff --check`: Passed.
- Backend, migrations, backup/export data, credentials, and device artifacts were not part of this client evidence.
