# Master Goods full project gap audit

Date: 2026-06-19

Scope: backend Spring Boot, Android Compose app, Web Vue app, iOS SwiftUI app, docs/specs, acceptance evidence, and tooling entry points.

Method:
- Main thread inspected project inventory, specs, source mappings, TODO/demo/mock scans, API/client contracts, and selected high-risk services.
- Backend sub-agent inspected backend specs, controllers, services, migrations, repositories, tests, and acceptance scripts.
- Android/Web sub-agent inspected Android modules, Web pages/API client, acceptance docs, and implementation honesty gates.
- This audit is source/documentation review only. No Gradle/Maven/npm/Xcode build, device benchmark, or browser visual run was executed in this pass because those commands can write build or device artifacts.

Current repo shape:
- Backend: Spring Boot 3.2.6, Java 21, JPA/Flyway/PostgreSQL/H2 local profile, Redis/Security.
- Android: Kotlin/Compose/Hilt/Retrofit/Room/DataStore, multi-module under `master-goods-android`.
- Web: Vue 3/Vite, API client under `web/src/shared/api/client.ts`, routes under `web/src/app/router/routes.ts`.
- iOS: SwiftUI app under `ios/ZhihuijiIOS`, with custom API client and feature screens.
- Static inventory in this pass: 1354 tracked project files by `rg --files`; 675 selected production-like backend/web/android/iOS source files under `src/main`, `web/src`, `master-goods-android`, and `ios`.

## Executive verdict

The project is not release-complete. It has substantial backend v2 domain coverage and real Android/Web/iOS clients, but several areas are still either unfinished or only partially evidenced:

- AI assistant cannot be marked ChatGPT-like production complete yet. Provider stream evidence, cancellation semantics, Web rendering, and Android real-device evidence remain partial.
- Finance/media/import/sync contain real backend gaps: `cash_change_records` is only schema/model/repository/DTO, media is metadata-only, import jobs are mostly orchestration records, and sync/inventory list paths are not proven scalable.
- Android and Web still contain honest incomplete UI paths and demo fallback behavior in API mode.
- iOS has concrete contract mismatches that can fail against backend, especially AI conversation/draft status values.
- Test and acceptance coverage is uneven. Existing B11/backend smoke is targeted, not a full release gate.

## P0 findings

### P0-1. AI true streaming and ChatGPT-like acceptance are still partial

Evidence:
- `docs/spec/44-ai-assistant-review-gates.md:143` states that without provider `model_stream` capture and Android UI evidence, the highest acceptable conclusion is partial.
- `src/main/resources/application-prod.yml:45` defaults `AGENT_LLM_ENABLED=false`.
- `docs/spec/43-ai-assistant-requirements.md:1333` still requires real provider SSE, real-device UI, and end-to-end evidence.
- Existing acceptance evidence under `docs/acceptance-evidence/ai-agent` is useful, but the backend sub-agent found the docs still distinguish rule-summary fallback from true provider token streaming.

Impact:
- Do not claim production ChatGPT-like AI readiness.
- Rule-summary streaming can be an honest degraded mode, but it is not proof of true provider token streaming.

Next steps:
- Capture production-like `AGENT_LLM_ENABLED=true` raw SSE for representative questions.
- Preserve run audit, HTTP headers/metrics, UI tree/screenshot/logcat/gfxinfo, and result block reconciliation.
- Update gate docs so `model_stream` means provider tokens only, while `rule_summary` remains degraded streaming.

### P0-2. AI cancel does not prove provider HTTP stream is interrupted

Evidence:
- `src/main/java/com/zhihuiji/backend/application/service/v2/V2AgentAiService.java:399` marks active run cancelled and emits cancel state.
- `src/main/java/com/zhihuiji/backend/application/service/v2/V2AgentAiService.java:3246` attempts future cancellation.
- `src/main/java/com/zhihuiji/backend/infrastructure/ai/LongCatAnthropicClient.java:272` uses blocking `HttpClient.send(... BodyHandlers.ofInputStream())`.
- `src/main/java/com/zhihuiji/backend/infrastructure/ai/LongCatAnthropicClient.java:284` reads provider stream line by line.
- `src/test/java/com/zhihuiji/backend/application/service/v2/V2AgentAiServiceTest.java:786` uses a mock stream and does not prove the provider socket/body is closed.

Impact:
- UI may show "cancelled" while backend/provider work keeps running until the remote stream ends.

Next steps:
- Thread cancellation signal or a closeable stream handle into `LongCatAnthropicClient.streamTextMessage`.
- Prefer cancellable `sendAsync` or explicitly close the response body on cancel.
- Add fake SSE server integration test that verifies connection close after cancel.

### P0-3. Web dashboard shows demo/fake data in API mode

Evidence:
- `web/docs/WEB_DEVELOPMENT_ROADMAP.md:454` forbids local fake data in API mode.
- `web/src/pages/dashboard/DashboardPage.vue:100` falls back to `demoTrendPoints` when trend data is empty.
- `web/src/pages/dashboard/DashboardPage.vue:146` falls back to `demoWorkbench` on workbench API failure.
- `web/src/pages/dashboard/DashboardPage.vue:167` writes fixed account balance `342105.5` when accounts fail.
- `web/src/pages/dashboard/DashboardPage.vue:180` `useDemoData()` writes a full fake dashboard set.

Impact:
- Real API failures can look like successful business data.
- This violates the AI/release honesty rules and can mislead operators.

Next steps:
- In API source mode, render explicit empty/error states per widget.
- Keep demo data only for non-API/demo mode.
- Add regression checks that API mode never renders demo constants.

### P0-4. Web AI output rendering is not production-grade

Evidence:
- `web/src/pages/agent/AgentPage.vue:669` renders message content as plain `<pre>`.
- `web/src/pages/agent/AgentPage.vue:713` renders result block data via `JSON.stringify(block.data)`.
- `docs/spec/44-ai-assistant-review-gates.md:21` requires readable Markdown and chart/result block gates.

Impact:
- Web cannot pass the AI assistant presentation gate.
- Tables, chart blocks, evidence cards, and malformed block fallbacks are not handled.

Next steps:
- Add Markdown renderer with safe sanitization.
- Add result block renderers for table, line/bar/pie charts, KPI/evidence cards, and unsupported block empty states.
- Add SSE/result-block parsing tests and browser screenshots.

### P0-5. iOS AI conversation and draft statuses are incompatible with backend

Evidence:
- Backend allows conversation statuses only `active`, `closed`, `archived` in `src/main/java/com/zhihuiji/backend/application/service/v2/V2AgentConversationService.java:17`.
- Backend allows draft statuses only `active`, `archived` in `src/main/java/com/zhihuiji/backend/application/service/v2/V2AgentConversationService.java:19`.
- iOS initializes and submits `open` in `ios/ZhihuijiIOS/Features/Agent/AgentViewModel.swift:27`, `:108`, `:239`, `:272`, and `:303`.
- iOS draft sheet presents `Open` tagged as `open` in `ios/ZhihuijiIOS/Features/Agent/AgentChatView.swift:955`.

Impact:
- iOS creating conversations or drafts can fail server validation.
- Existing iOS tests decoding `open` do not reflect current backend contract.

Next steps:
- Replace iOS `open` with `active` for active conversations/drafts.
- Update UI copy/tag values and tests.
- Add iOS API contract tests against backend fixture values.

## P1 findings

### P1-1. `cash_change_records` is schema/model only, without service/controller

Evidence:
- `src/main/resources/db/migration/V10__finance_and_inventory_foundation.sql:43` creates `cash_change_records`.
- `src/main/java/com/zhihuiji/backend/domain/entity/CashChangeRecordEntity.java:11` defines the entity.
- `src/main/java/com/zhihuiji/backend/infrastructure/repository/CashChangeRecordRepository.java:8` defines the repository.
- `src/main/java/com/zhihuiji/backend/api/dto/v2/finance/V2FinanceDtos.java:100` defines response DTO.
- No `V2CashChangeRecordService` or controller exists.
- `docs/spec/30-api-contracts.md:20` still says `cash_change_records` is pending.

Impact:
- Finance adjustment/change-money domain cannot be operated through API.

Next steps:
- Add `/v2/cash-change-records` list/get/create endpoints, service logic, account balance transaction handling, and tests.
- Or explicitly downgrade the B04 completion statement to "schema/model ready, API pending".

### P1-2. Media upload is metadata-only

Evidence:
- `src/main/java/com/zhihuiji/backend/api/controller/v2/V2MediaController.java` exposes `/v2/media/assets` and `/v2/media/bindings`.
- `src/main/java/com/zhihuiji/backend/application/service/v2/V2MediaService.java:35` `createAsset` only accepts `objectKey`, provider, bucket, file metadata, checksum, dimensions, and metadata JSON.
- No multipart upload, presigned URL, object storage write, or file validation chain exists.
- `docs/spec/27-media-attachments-domain.md` still marks real upload chain and Android integration as pending.

Impact:
- Product/order/media attachment UX cannot upload real files through backend.

Next steps:
- Decide storage mode: backend multipart, presigned object storage, or local/minio dev mode.
- Add upload API, validation, checksum/size limits, persistence, and client integration.

### P1-3. Import job table is not a real async worker pipeline

Evidence:
- `src/main/java/com/zhihuiji/backend/application/service/v2/V2ImportJobService.java` creates/list/get/retry/cancel job rows and changes statuses/stages.
- No executor/worker/background processor was found in that service.
- `src/main/java/com/zhihuiji/backend/api/controller/v2/V2ImportJobController.java` has a direct `legacy-sqlite` endpoint that calls `LegacySQLiteImportService.importForCurrentUser(...)` synchronously.
- `web/src/pages/settings/DatabasePage.vue` lists jobs but import submission uses direct legacy sqlite import, not a created job orchestration flow.

Impact:
- Job status can imply asynchronous orchestration, but real import is still direct/synchronous for the legacy path.

Next steps:
- Either implement worker processing for created import jobs or document job records as audit/status rows only.
- Add retry/cancel behavior tests against an actual worker lifecycle.

### P1-4. `/v2/purchase-returns` exists but is under-documented and under-tested

Evidence:
- Code exists: `src/main/java/com/zhihuiji/backend/api/controller/v2/V2PurchaseReturnController.java:20`, `src/main/java/com/zhihuiji/backend/application/service/v2/V2PurchaseReturnService.java:30`, and `src/main/resources/db/migration/V18__purchase_returns.sql:1`.
- `docs/spec/30-api-contracts.md:18` purchase contract omits purchase returns while listing orders/receipts.
- No `V2PurchaseReturn*Test` was found under `src/test`.

Impact:
- Implemented domain can drift from the public API contract and lacks regression coverage for stock/payable/refund behavior.

Next steps:
- Update API contract.
- Add controller/service tests for create, over-return rejection, confirm stock/payable effects, refunds, cancel rollback, and cross-owner reference rejection.

### P1-5. Purchase returns list has in-memory pagination and N+1 response assembly

Evidence:
- `src/main/java/com/zhihuiji/backend/api/controller/v2/V2PurchaseReturnController.java:44` slices service results in the controller.
- `src/main/java/com/zhihuiji/backend/application/service/v2/V2PurchaseReturnService.java:152` loads all matching rows then maps each entity.
- `src/main/java/com/zhihuiji/backend/application/service/v2/V2PurchaseReturnService.java:402` loads items/refunds during response mapping.

Impact:
- Large owner datasets can cause slow list responses and excessive database queries.

Next steps:
- Push pagination into repository using `Pageable`.
- Batch load return items/refunds for the page.
- Add regression tests for pagination and batch behavior.

### P1-6. `/v2/sync/pull` and Android sync are not full-scale/full-domain ready

Backend evidence:
- `src/main/java/com/zhihuiji/backend/application/service/v2/V2SyncService.java:292` collects changes across entity types in memory, sorts, then limits.
- `src/main/java/com/zhihuiji/backend/application/service/v2/V2SyncService.java:339` and related collectors use owner-wide repository fetches followed by Java filtering.
- `src/test/java/com/zhihuiji/backend/application/service/v2/V2SyncServiceTest.java:122` proves cursor ordering on a small mocked set, not scale.

Android evidence:
- `master-goods-android/data/sync/src/main/java/com/zhihuiji/data/sync/SyncV2Repository.kt:107` only applies customer, supplier, product, sale order, sale order item, purchase order, pay order, and finance record.
- Unsupported `entityType` is logged and skipped.
- `master-goods-android/core/datastore/src/main/java/com/zhihuiji/core/datastore/SyncPreferenceStore.kt` keys cursor only by entity type.
- `master-goods-android/core/database/src/main/java/com/zhihuiji/core/database/entity/SyncCursorEntity.kt` uses `entityType` as the only primary key.
- Product/customer/supplier Room entities have no owner/client dimension.

Impact:
- Multi-owner or account-switch scenarios can mix local data/cursors.
- Expanded v2 domain changes can silently fail to apply locally.

Next steps:
- Add database-level keyset/bounded queries for pull.
- Add owner/client-aware local schemas and cursor keys.
- Surface skipped entity types/counts in sync UI and logs.

### P1-7. Inventory list APIs are unpaginated

Evidence:
- `src/main/java/com/zhihuiji/backend/application/service/v2/V2InventoryService.java:37` ledger list defaults to all rows from `0L` to now.
- `src/main/java/com/zhihuiji/backend/application/service/v2/V2InventoryService.java:86` snapshot/monthly stats list paths also return list collections without pagination envelope.
- `docs/spec/41-b11-acceptance-matrix.md:138` still requires large-list/sync performance validation.

Impact:
- Large inventory ledgers can become slow or memory-heavy.

Next steps:
- Add page/size or cursor arguments.
- Make no-range default bounded, for example latest N rows.
- Document defaults in API contract.

### P1-8. Android product and purchase editing still disable real business fields

Product evidence:
- `master-goods-android/feature/products/src/main/java/com/zhihuiji/feature/products/ProductEditScreen.kt` disables wholesale price and primary supplier fields.
- Same screen states image upload, barcode scan, price levels, and primary supplier relation are not fully written yet.

Purchase evidence:
- `master-goods-android/feature/purchases/src/main/java/com/zhihuiji/feature/purchases/PurchaseOrderEditScreen.kt:288` shows settlement method/warehouse as waiting for real fields.
- `master-goods-android/feature/purchases/src/main/java/com/zhihuiji/feature/purchases/PurchaseOrderEditScreen.kt:827` shows whole-order discount as not connected.

Impact:
- Archive and purchase workflows are not feature-complete.

Next steps:
- Align backend contracts for product media, barcode, price levels, supplier relation, purchase settlement, warehouse, and discount.
- Keep disabled honest states until writes are implemented and tested.

### P1-9. Android reports are not fully wired to true trend/chart series

Evidence:
- `master-goods-android/feature/reports/DEVELOPMENT.md:44` says account balance, low stock, inventory monthly stats, sales trend series/coordinates/tooltips still need integration.
- `master-goods-android/feature/reports/src/main/java/com/zhihuiji/feature/reports/ReportViewModel.kt:48` has no real trend series field in `ReportUiState`.
- `master-goods-android/feature/reports/src/main/java/com/zhihuiji/feature/reports/ReportScreen.kt:401` draws summary-based lines instead of a real trend API series.

Impact:
- Report charts are not complete analytical views.

Next steps:
- Connect real trend APIs and chart series.
- Add empty/error states and device screenshots.

### P1-10. Android permissions default to allow before resolved

Evidence:
- `master-goods-android/app/src/main/java/com/zhihuiji/app/navigation/MainAccessViewModel.kt:69` returns true while permissions are unresolved.

Impact:
- On startup or refresh, protected routes can briefly render before backend/user permissions are known.

Next steps:
- Treat unresolved state as loading/blocked.
- Add navigation tests covering permission loading state.

### P1-11. Web permissions can fall back to local role permissions in API mode

Evidence:
- `web/src/app/stores/session.ts:68` falls back to `rolePermissions[state.currentRole]` when permissions are empty, including API source.

Impact:
- Backend-denied or not-yet-loaded permissions can be masked by local defaults.

Next steps:
- In API mode, trust only backend permissions.
- Block write buttons/routes until permissions are loaded.

### P1-12. Web AI workbench still shows KPI/risk/report content in the assistant entry

Evidence:
- `web/src/pages/agent/AgentPage.vue:757` renders workbench KPI cards.
- `web/src/pages/agent/AgentPage.vue:784` renders risk alerts.
- Android has stronger AI-home cleanliness expectations in the acceptance docs.

Impact:
- Web AI entry may violate the "clean assistant entry" gate and mix dashboard/report cards into the assistant UI.

Next steps:
- Keep only the question entry, sync/status, recent conversation shell, and honest errors unless specs explicitly allow those cards.

### P1-13. iOS endpoint coverage is behind backend/Web/Android

Evidence:
- `ios/ZhihuijiIOS/Core/API/APIEndpoint.swift` still enumerates a small core set and uses `/v1/auth/*`.
- `ios/ZhihuijiIOS/Core/API/APIClient.swift` has many direct `/v2` paths but no visible media upload, sync upload/pull/import-job orchestration, or cash-change endpoints.
- `ios/README.md` states iOS still prioritizes closing inventory/archive/reports/AI and Xcode build is blocked on the current machine.

Impact:
- iOS is a real app shell with several connected domains, but it is not at parity with backend/Web/Android.

Next steps:
- Fix status mismatch first.
- Add missing client APIs and screens only after backend contract is settled.
- Run Xcode build/test on a machine with full Xcode.

## P2 findings

### P2-1. B11/backend smoke is targeted, not full release coverage

Evidence:
- `tools/b11_acceptance_check.sh:64` runs selected backend packages/classes.
- `docs/spec/41-b11-acceptance-matrix.md:43` describes this as targeted scripted coverage and still recommends full tests for release.

Impact:
- Passing B11 smoke cannot be treated as full backend release confidence.

Next steps:
- Add full `test` logs, Flyway migration logs, controller/service test inventory, and a no-untested-v2-controller check.

### P2-2. Web lacks automated test/visual gate scripts

Evidence:
- `web/package.json:6` only has dev/build/preview scripts.
- `web/docs/WEB_DEVELOPMENT_ROADMAP.md:516` expects npm build and browser checks.

Impact:
- Web regressions are mostly manual.

Next steps:
- Add `vue-tsc`, Vitest for API/SSE parsing, and Playwright route smoke.
- Add screenshot comparisons for Stitch-derived pages.

### P2-3. Stitch/Web visual completion is mostly documented, not freshly evidenced

Evidence:
- `web/docs/WEB_DEVELOPMENT_ROADMAP.md:414` says eight PC drafts are completed.
- `web/docs/WEB_DEVELOPMENT_ROADMAP.md:439` requires visual comparison against PNG/HTML.
- This audit did not find current-run screenshots proving those checks.

Impact:
- Visual completion cannot be reasserted from this pass alone.

Next steps:
- Run browser screenshots for the eight PC routes and mobile PC-style pages.
- Record deviations and pass/fail evidence.

### P2-4. Android Liquid Glass real backdrop remains a disabled/degraded path

Evidence:
- `master-goods-android/core/designsystem/src/main/java/com/zhihuiji/core/designsystem/LiquidGlassSurface.kt:30` notes the real renderer is not in production because of Android 16 first-frame measurement crash risk.
- `docs/spec/42-android-liquid-glass-ui-refactor-plan.md:20` requires device regression before restoring blur/lens.

Impact:
- Current UI can be polished, but true backdrop/lens Liquid Glass is not fully enabled.

Next steps:
- Add isolated benchmark/crash regression and feature flag.
- Re-enable page by page with screenshots.

### P2-5. Android still has direct Material TextField usage

Evidence:
- `master-goods-android/app/src/main/java/com/zhihuiji/app/navigation/ArchivesScreen.kt:180` directly uses `TextField(...)`.

Impact:
- Design-system consistency is not fully closed.

Next steps:
- Replace with `SearchFilterBar`/`GlassTextField`, or document as an allowed exception.

### P2-6. Android benchmark coverage is narrow and requires pre-authenticated state

Evidence:
- `master-goods-android/benchmark/src/main/java/com/zhihuiji/benchmark/AppMacrobenchmark.kt:20` covers cold start, dashboard-to-assistant, and rule-summary chat.
- `master-goods-android/benchmark/src/main/java/com/zhihuiji/benchmark/BenchmarkFlows.kt:64` assumes device is already logged in.

Impact:
- Startup and AI paths have some coverage, but reports/documents/sync/editing workflows are not benchmarked.

Next steps:
- Add dashboard, reports, documents, purchase edit, sync, and true streaming AI flows.
- Make login state reproducible in benchmark setup.

## Items verified as not currently production blockers

- Local demo/admin backend endpoints are profile-gated. `src/main/java/com/zhihuiji/backend/api/controller/AdminController.java:23` uses `@Profile("local")`, and `src/test/java/com/zhihuiji/backend/api/controller/AdminControllerProdProfileTest.java` verifies `/v1/admin/demo/seed` and `/v1/admin/agent/smoke` return 404 in prod profile.
- Backend media controller is permission-protected and owner-scoped for metadata/binding records; the issue is missing binary upload, not a public unauthenticated endpoint.
- Backend v2 auth being absent is documented; clients still use `/v1/auth/*`. Treat this as an architectural backlog item unless the current release explicitly requires `/v2/auth`.

## Recommended repair order

1. Fix contract-breaking issues:
   - iOS `open` status -> `active`.
   - Web API-mode demo fallback removal.
   - Web API-mode permission fallback removal.

2. Close AI acceptance blockers:
   - Provider stream evidence and production-like config.
   - Provider cancellation semantics.
   - Web Markdown/result block renderers.
   - Android device evidence and stop/cancel evidence.

3. Close backend domain gaps:
   - `cash_change_records` API/service/tests.
   - Media real upload path.
   - Import job worker semantics or documentation downgrade.
   - Purchase returns contract/tests and pagination/N+1 fix.

4. Harden sync/inventory scale:
   - Backend keyset/bounded queries.
   - Android owner/client-aware local state.
   - Expose skipped sync entity diagnostics.
   - Paginate inventory lists.

5. Bring client parity and evidence:
   - Android product/purchase/report gaps.
   - iOS missing API coverage and Xcode validation.
   - Web/Android visual and performance evidence.

## Suggested task breakdown

- Backend P0/P1 worker:
  - AI cancel integration test and implementation.
  - Cash change records API/service/tests.
  - Purchase returns tests + pagination/N+1 fix.

- Web worker:
  - Remove API-mode demo fallback.
  - Fix API-mode permissions fallback.
  - Add Markdown/result block renderers and minimal tests.

- Android worker:
  - Permission unresolved state block.
  - Product/purchase honest disabled fields to real contracts where backend exists.
  - Sync skipped-entity diagnostics and owner-aware storage plan.

- iOS worker:
  - Fix AI status contract mismatch.
  - Add contract tests/fixtures.
  - Map missing media/sync/import/cash-change endpoint coverage.

- QA/evidence worker:
  - Run full backend tests and Flyway logs.
  - Run Web build/browser screenshots.
  - Run Android assemble, UI tree/screenshots, and benchmark flows on a controlled logged-in device.
