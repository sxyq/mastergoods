# Backend Follow-up B

## Scope

本轮只处理付款幂等校验和确定存在的 HTTP 列表内存分页。未修改 Web、Android、Agent、部署配置、生产迁移或 data 目录。

## Results

| test_id | category_id | wave_id | env | account/store | pre_state | actions | expected | actual | artifacts | cleanup | result |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| BE-FU-B-PAY-NULL-001 | BE-FUNC-PAY-IDEMPOTENCY | Wave 1 | local source/JUnit | owner context mocked: 1 | no request object | call `V2PayOrderService.create(null)` | stable business input error, no persistence | `IllegalArgumentException(付款单参数不能为空)` before repository write | `V2PayOrderServiceTest.nullRequestIsRejectedAsBusinessInputError` | none | Passed |
| BE-FU-B-PAY-KEY-002 | BE-FUNC-PAY-IDEMPOTENCY | Wave 1 | local source/JUnit | owner context mocked: 1 | public create entry point | submit blank, control-character and 129-character keys | stable 4xx/422-mappable validation errors | blank `幂等键不能为空`; control `幂等键格式不合法`; long `幂等键长度不能超过128个字符` | `V2PayOrderServiceTest.publicCreateRequiresNonBlankSafeIdempotencyKey` | none | Passed |
| BE-FU-B-PAY-HASH-003 | BE-FUNC-PAY-IDEMPOTENCY | Wave 1 | local source/JUnit | owner context mocked: 1 | existing V30/V31 row with null hash | same key with different payload must conflict | `相同幂等键不能用于不同付款请求`; no business create | `legacyPayloadMatches` compares persisted fields when hash is null | `V2PayOrderServiceTest.legacyOrderWithoutPayloadHashRejectsDifferentPayload` | none | Passed |
| BE-FU-B-PAY-RACE-004 | BE-FUNC-PAY-IDEMPOTENCY | Wave 1 | local source/JUnit | owner context mocked: 1 | unique constraint exception, committed row appears on retry | return committed row without JVM lock or 500 | retry lookup returns existing order | existing concurrency regression remains Passed | `V2PayOrderServiceTest.idempotencyConflictReturnsOrderCommittedByConcurrentRequest` | none | Passed |
| BE-FU-BE-LIST-005 | BE-PERF-DB-PAGINATION | Wave 1 | local source/compile | owner-scoped repository methods | controller list endpoints used `PaginationUtils.slice` | page/window must reach repository | V1 pay orders/sale orders, V2 purchase returns/receipts/sales returns, credits/poster generations and local admin users now use `Pageable`; no matching controller/service `PaginationUtils.slice` remains | `rg PaginationUtils.slice Code/backend/src/main/java/...` returned no matches | repository/service/controller diff | none | Passed |

## Verification

- `./gradlew compileJava --no-daemon`: Passed, `BUILD SUCCESSFUL`.
- `./gradlew test --no-daemon --tests com.zhihuiji.backend.application.service.v2.V2PayOrderServiceTest`: Passed, `BUILD SUCCESSFUL` in approximately 6 seconds; test compilation and JaCoCo task also completed.
- `git diff --check`: Passed.
- 8220 real HTTP verification: Blocked; this follow-up did not connect to the deployed service.
- PostgreSQL EXPLAIN/ANALYZE: Deferred; no production PostgreSQL service was used.
- Full legacy SQLite import: Deferred; only existing path-boundary tests are available.

## Compatibility Boundary

The public `/v2/pay-orders` controller calls `createWithRequiredIdempotencyKey` and rejects missing/blank keys. The compatibility `create` entry point still accepts a missing key for existing Agent draft confirmation payloads; changing that shared internal contract would require modifying the prohibited Agent scope. Control characters, unsupported characters and keys longer than 128 characters are rejected in both paths.

The formal payment list path remains `/v2/pay-orders`. No `/v2/payments` compatibility route was added because the current backend and clients use `/v2/pay-orders`; the historical 404 is retained as a legacy-path finding.
