# Client contract repair: C

Environment: local source tree on `codex/publish-local-updates`. No browser, real HTTP/SSE, adb, APK, emulator, device, or deployment execution.

| test_id | scope | command/result | evidence | result |
|---|---|---|---|---|
| WEB-C-CONTRACT-001 | Web API payload and route static contract | `node testing/web/单元测试/scripts/verify_client_contract.mjs`; `/v2/pay-orders`, `idempotency_key`, snake_case `conversation_id`, and string EntityId forwarding matched | `web-static-contract.txt` | Passed |
| WEB-C-BUILD-001 | Web typecheck/build | `npm run build`; `vue-tsc -b` and Vite build completed, 116 modules transformed | `web-build.txt` | Passed |
| AND-C-UNIT-001 | Android model/network/order tests and payments compile | `./gradlew --no-daemon --console=plain -Pksp.incremental=false :core:model:testDebugUnitTest :core:network:testDebugUnitTest :data:order:testDebugUnitTest :feature:payments:compileDebugKotlin`; 139 tasks completed, build successful | `android-client-tests-rerun.txt` | Passed |
| AND-C-ENV-001 | Initial Android attempt | The existing centralized KSP incremental cache referenced a missing `Code/tmp` path; module generated outputs were cleaned and the focused command was rerun with incremental KSP disabled | `android-client-tests.txt`, `android-clean.txt`, `android-clean-supplier.txt` | Deferred |
| WEB-C-RUNTIME-001 | Browser, real login, and real SSE | Excluded from this client-only round | none | Blocked |
| AND-C-RUNTIME-001 | adb, APK, emulator, device, UI, screenshot, and logcat | Excluded from this client-only round | none | Blocked |

The current source changes are limited to Web/Android client files. Existing external backend modifications were preserved and are excluded from the client commit.
