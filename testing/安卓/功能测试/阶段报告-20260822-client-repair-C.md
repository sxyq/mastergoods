# Android API contract repair report: C

## Scope and status

This stage repaired source-level API contracts only. Current 8220 authenticated HTTP, adb, APK, emulator, device, UI, screenshot, and logcat execution is `Blocked` and is not replaced by historical 154/device artifacts.

## Changes

| Finding | Repair | Result |
|---|---|---|
| Payment idempotency field missing | `CreatePayOrderV2Request` now serializes `idempotency_key`. | Passed |
| Inventory Page/List mismatch | Ledger, snapshots, and monthly stats Retrofit responses use `PageResponse<T>` with `content`; repository callers retain list behavior and expose page/size. | Passed |
| 409/422 unstable client handling | `NetworkException.kind` exposes stable conflict/validation categories and messages. | Passed |
| SSE duplicate events | Events with `event_id` or `seq` are deduplicated before Flow emission; completion/cancellation and cancellation of the OkHttp call remain covered. | Passed |
| owner/store spoofing risk | No owner/store fields were added to client requests; context remains server-session controlled. | Passed |

## Verification

- `:core:model:compileDebugKotlin --no-build-cache --rerun-tasks`: Passed.
- `:core:model:testDebugUnitTest :core:network:testDebugUnitTest :data:sync:testDebugUnitTest --no-build-cache --rerun-tasks`: Passed.
- Selected tests include idempotency serialization, inventory query contract, stable 409/422 classification, SSE cancellation, terminal events, parse errors, retry behavior, and duplicate event suppression.
- Real Android HTTP, authentication, owner/store switching, adb, APK, emulator, device UI, screenshot, and logcat: Blocked.

## Files

- `Code/frontend/android/core/model/src/main/java/com/zhihuiji/core/model/PageResponse.kt`
- `Code/frontend/android/core/model/src/main/java/com/zhihuiji/core/model/v2/order/OrderV2Models.kt`
- `Code/frontend/android/core/model/src/test/java/com/zhihuiji/core/model/v2/V2ModelSerializationTest.kt`
- `Code/frontend/android/core/network/src/main/java/com/zhihuiji/core/network/ZhihuijiV2Api.kt`
- `Code/frontend/android/core/network/src/main/java/com/zhihuiji/core/network/SafeApiCall.kt`
- `Code/frontend/android/core/network/src/main/java/com/zhihuiji/core/network/AgentSseClient.kt`
- `Code/frontend/android/core/network/src/test/java/com/zhihuiji/core/network/AgentSseClientCancellationTest.kt`
- `Code/frontend/android/core/network/src/test/java/com/zhihuiji/core/network/SafeApiCallBehaviorTest.kt`
- `Code/frontend/android/core/network/src/test/java/com/zhihuiji/core/network/ZhihuijiV2ApiContractTest.kt`
- `Code/frontend/android/data/sync/src/main/java/com/zhihuiji/data/sync/InventoryV2Repository.kt`

No backend source, migration, deployment configuration, data backup/export directory, or credentials were changed.
