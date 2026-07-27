# Repository Guidelines

## Project Structure & Module Organization

This is a mixed 智慧记 / Master-Goods repository. Backend Spring Boot code lives in `src/main/java/com/zhihuiji/backend`, tests in `src/test/java`, and Flyway migrations in `src/main/resources/db/migration`. The Android app is under `frontend/android/`, split into `app`, `core`, `data`, `feature`, `benchmark`, and `backdrop` modules. The PC admin is a Vue/Vite app in `frontend/web/` with source in `frontend/web/src`. Native iOS work is in `frontend/ios/ZhihuijiIOS` with tests in `frontend/ios/ZhihuijiIOSTests`. Operational files live in `deploy/`, docs in `docs/`, testing plans and ledgers in `testing/`, and utility scripts in `backend/tools/`.

## Build, Test, and Development Commands

- `./gradlew bootRun`: run the backend locally with Java 21.
- `./gradlew test`: run backend JUnit tests and generate JaCoCo reports.
- `cd frontend/web && npm run dev`: start the Web admin Vite server.
- `cd frontend/web && npm run build`: type-check and build the Web admin.
- `cd frontend/android && ./gradlew :app:compileDebugKotlin`: compile Android app Kotlin.
- `cd frontend/android && ./gradlew assembleDebug`: build a debug APK.
- iOS validation requires local Xcode tools; use `xcodebuild` only when available.

## Coding Style & Naming Conventions

Use existing module patterns before adding abstractions. Java backend classes use standard Spring naming such as `*Controller`, `*Service`, `*Repository`, and DTO packages by API version. Kotlin Android code follows Compose/MVVM conventions: `*Screen`, `*ViewModel`, repository classes in `data/*`, shared models in `core:model`. Vue files use PascalCase component names, for example `SalesPaymentPage.vue`. Keep JSON/API fields aligned with backend contracts; large IDs should not be coerced into unsafe JavaScript numbers.

## Testing Guidelines

Backend tests use JUnit 5 and Spring test slices; name files `*Test.java`. Add migration SQL tests when changing database structure. Web changes must pass `npm run build`. Android changes should compile affected modules at minimum, and broader work should run `assembleDebug`. For iOS, do not claim build success unless `xcodebuild` or Swift tooling actually ran.

## Commit & Pull Request Guidelines

Recent history uses short imperative messages such as `Improve AI stream flush cadence` plus occasional Chinese auto-backup commits. Prefer focused, descriptive commits scoped to one subsystem. PRs should include summary, changed areas, validation commands, screenshots for UI changes, and linked issues or deployment notes where relevant.

## Security & Configuration Tips

Do not commit secrets, generated evidence, `web/dist`, `node_modules`, Gradle caches, APK/JAR artifacts, or server keys. Treat backend controllers, DTOs, migrations, and live config as the source of truth over stale docs.

## Reusable Assets Inventory (Global Reuse First)

Before adding new components, classes, or helpers, check this inventory. Reuse and extend existing assets in place; new files require justification.

### Backend Reusable Classes (`src/main/java/com/zhihuiji/backend/api/common/`)

| Class | Purpose | Reuse Pattern |
|-------|---------|---------------|
| `ApiResponse` | Unified API response envelope with `success(T)` / `failure(int,String)` factories and `CODE_*` constants | All controller return types; never build raw response maps |
| `BusinessException` | Business error with code (default `CODE_UNPROCESSABLE_ENTITY`) | Throw in service layer; caught by `GlobalExceptionHandler` |
| `GlobalExceptionHandler` | Centralized exception→`ApiResponse` mapping (validation/constraint/business/access denied/unknown) | Do not catch and map manually in controllers |
| `IdGenerator` | `@Component` Spring Bean generating IDs via `SecureRandom` + collision check | Inject and call instance `nextId()`; do not create separate ID generators or call statically |
| `PaginationUtils` | `slice(list, page, size)` with `MAX_SIZE` clamp | Use in controller `list` endpoints; do not inline `subList` math |
| `ParseUtils` | `parseLong/parseDouble/parseInteger` (Template Method pattern, null-safe) | Use for query param parsing; do not write try/catch parse blocks |
| `OrderStatus` / `PayOrderStatus` / `PaymentStatus` / `PurchaseOrderStatus` / `PurchaseReceiptStatus` / `PurchaseReturnStatus` / `SalesReturnStatus` / `PaymentType` | Status enums with `code()` / `fromCode(Integer)` / `isValid(Integer)` | Follow this enum pattern for new status types; do not create `final class` + int constants |

### Backend Design Patterns in Use

- **Template Method**: `ParseUtils.parse(String, Function<String,T>)` — shared try/catch + blank check, public methods delegate
- **Factory Method**: `ApiResponse.success()` / `failure()` / `BusinessException` constructors
- **Strategy / Lookup Table**: Status enum `fromCode` linear scan; `GlobalExceptionHandler` dispatch by exception type
- **Centralized Error Handling**: `GlobalExceptionHandler` `@RestControllerAdvice` — controllers stay thin

### Android Reusable Components

#### `core/common/` Shared Utilities

| Object | Purpose | Reuse Pattern |
|--------|---------|---------------|
| `MoneyFormatter` | ThreadLocal `DecimalFormat` currency formatting (`format(BigDecimal?)`, `format(Double?)`, `formatWithoutSymbol`, `formatSigned`) | Import and call; do not create local `DecimalFormat` instances |
| `TimeFormatter` | ThreadLocal date/datetime/time formatting (`formatDate`, `formatDateTime`, `formatTime`, `formatOrDash`) | Import and call; do not create local `SimpleDateFormat` |
| `StatusLabels` | 14 status→label functions via `mapOf` lookup tables (`saleOrderStatus`, `purchaseOrderStatus`, `payOrderStatus`, `financeType`, `customerStatus`, `customerListStatus`, `productStatus`, `customerLevel`, `paymentMethod`, `paymentType`, `inventoryFlowType`, `stockStatus`, `supplierStatus`) | Call instead of writing `when` blocks for status labels |
| `UiMessage` | Typed UI message data class with `error/warning/info/success` factories + `fromThrowable` | Use for ViewModel error state; do not create ad-hoc message wrappers |
| `ResultExt` | `requireData()` extension on `ApiResponse` (deprecated, throws `BusinessException`) | Legacy; prefer explicit error handling |

#### `core/database/` Entity Mappers

| File | Purpose | Reuse Pattern |
|------|---------|---------------|
| `EntityMappers.kt` | `toDto()` / `toEntity()` extension functions for all entities (Product, Customer, Supplier, SaleOrder, PurchaseOrder, PayOrder, FinanceRecord) + `toDtoList()` / `toEntityList()` private helpers | Call `.toDto()` / `.toEntity()`; do not write manual field-by-field mapping |

#### `core/designsystem/` Theme Constants

- `TextPrimary`, `TextSecondary`, etc. — semantic color tokens; use instead of hardcoded `Color(0xFF...)`
- `roundedCardShape` — file-level shape constant pattern; extract repeated shapes to file-level `val`

#### Android Design Patterns in Use

- **Lookup Table**: `StatusLabels` — `mapOf(code, label)` + single-line function, replaces `when` multi-branch
- **ThreadLocal Formatter**: `MoneyFormatter` / `TimeFormatter` — thread-safe `DecimalFormat`/`SimpleDateFormat` reuse
- **Expression Body**: `MoneyFormatter.format*` — block body → expression body to reduce lines
- **File-level Constants**: `roundedCardShape`, `fridaMapPatterns`, `AppLaunchLoadingBrush` — extract repeated values to file-level `val`
- **Internal Visibility**: `AuthBackgroundBrush` / `AuthCardShape` — `private` → `internal` for cross-file module reuse without public API leak

### Web Reusable Assets

#### `frontend/web/src/shared/utils/business.ts` Helpers

| Function | Purpose | Reuse Pattern |
|----------|---------|---------------|
| `formatCurrency` / `formatNumber` / `formatPercent` | Currency/number/percent formatting | Import; do not create local formatters |
| `formatDate` / `formatDateTime` | Date formatting | Import; do not create local date formatters |
| `saleShippingStatus` / `salePaymentStatus` / `saleOrderStatusLabel` | Sales order status labels | Import; do not reimplement |
| `purchaseReceiptStatus` / `purchaseReceiptFlowStatus` / `purchasePaymentStatus` / `purchaseOrderStatusLabel` | Purchase status labels | Import |
| `purchaseReturnStatusLabel` / `purchaseReturnRefundStatus` | Purchase return labels | Import |
| `salesReturnStatusLabel` / `salesReturnStatusTokens` | Sales return labels | Import |
| `financeTypeLabel` / `financeMethodLabel` | Finance labels | Import |
| `reportRangeForPeriod` / `todayStartAt` / `weekStartAt` / `monthStartAt` | Report period range builders | Import; do not write local `buildRange` |
| `readQueryId` / `sameEntityId` | BigInt-safe ID parsing/comparison | Use for all route query ID parsing; **never** use `Number()` for entity IDs (snowflake ID precision risk) |

#### `frontend/web/src/app/stores/session.ts` Store

| Export | Purpose |
|--------|---------|
| `session.token` / `session.refreshToken` / `session.userId` | Auth state |
| `session.hasPermission(perm)` / `session.hasAnyPermission(perms)` | Client-side authZ (OWNER = all; api = backend Set; demo = role-based) |
| `session.login` / `session.logout` / `session.refreshProfile` / `session.refreshStoreContext` | Auth actions |
| `session.switchRole` / `session.switchMember` / `session.enterDemo` | Local role/member switching |

#### `frontend/web/src/entities/auth/roles.ts`

- `roleLabels` / `roleDescriptions` / `rolePermissions` / `rolePermissionSets` — static role config
- `canAccess(role, perm)` — role-based permission check for demo mode

#### `frontend/web/src/entities/screen/live-screen-data.ts`

- `loadLiveScreenData` — route-dispatched data loader
- `mapSalesOrders` / `mapPurchaseOrders` / `mapProducts` / `mapCustomers` / `mapSuppliers` / `mapFinanceRecords` etc. — data mapping functions (single-pass, pre-allocated)

#### Web Design Patterns in Use

- **Field Table Driven**: `session.ts` `persist` / `clearPersisted` — `PERSISTED_FIELDS` array drives localStorage read/write, eliminates duplicated key lists
- **Lookup Table**: `screenComponentByRoute` — route→component map; `statusTabs` — tab config arrays
- **Extract Shared Fetch**: `AgentPage.vue` `fetchSidePanel` — single function reused by `loadPage` / `refreshSidePanel`
- **Remove Duplicate Helpers**: `live-screen-data.ts` — deleted 7 functions duplicated in `business.ts`, switched to imports

### Database Migration & Multi-Tenant Isolation Pattern

Flyway migrations in `src/main/resources/db/migration/` follow a deliberate multi-tenant evolution:

| Migration | Purpose | Reuse Pattern |
|-----------|---------|---------------|
| `V1__init` – `V6__add_foreign_keys` | Early schema (pre-multi-tenant) — base tables, indexes, FKs | Read-only; do not modify applied migrations |
| `V7__owner_scope_foundation` | **Multi-tenant foundation** — adds `owner_user_id BIGINT NOT NULL` to all 14 business tables, backfills via `SYSTEM-LEGACY-OWNER`, drops global unique constraints, adds owner-scoped unique indexes (`uq_products_owner_code`, `uq_customers_owner_phone`, etc.) | Reference for any new multi-tenant migration; always include `owner_user_id` column + owner-scoped index |
| `V8+` | New domain tables — all include `owner_user_id` from creation | New tables MUST have `owner_user_id BIGINT NOT NULL` + `CREATE INDEX ... ON (owner_user_id, ...)` |

**Multi-tenant isolation strategy**: No database-level RLS (Row Level Security). Isolation is enforced at the **application layer** via JPQL queries with `ownerUserId` parameter (all repositories filter by `ownerUserId`) + `CurrentOwnerService.requireCurrentOwnerUserId()`. No `GRANT` statements (single DB user). All `@Query` annotations in repositories include `ownerUserId`. **Exception**: `ImportJobRepository` system-level worker queries (polling/claiming pending/running jobs) intentionally omit `ownerUserId` — the background executor is system-scoped across tenants; the repository is annotated with explicit system-level worker comments, and callers must enforce authorization via worker context.

### Android Security Configuration

| File | Purpose | Reuse Pattern |
|------|---------|---------------|
| `app/src/debug/res/xml/network_security_config.xml` + `app/src/release/res/xml/network_security_config.xml` | Network security — **both** debug and release set `cleartextTrafficPermitted="false"` (blocks all HTTP) | Do not enable cleartext; use HTTPS only |
| `app/src/main/AndroidManifest.xml` | Minimal permissions (`INTERNET` + `ACCESS_NETWORK_STATE` only), `allowBackup="false"`, no `android:debuggable` | Follow minimal-permission principle; keep `allowBackup="false"` |
| `app/proguard-rules.pro` | Keeps `*Annotation*`, `Signature`, `InnerClasses`, `EnclosingMethod` + kotlinx.serialization `$$serializer` classes | Do not remove serializer keep rules or deserialization breaks |

### Static Admin Console (`src/main/resources/static/admin-console/`)

| File | Purpose | Reuse Pattern |
|------|---------|---------------|
| `app.js` `escapeHtml(value)` | XSS prevention — escapes `&` `<` `>` `"` before `innerHTML` assignment | Use for ALL user-controllable data inserted via `innerHTML`; never insert raw API strings |
| `app.js` `request(path)` | Fetch wrapper — hardcoded API paths only, no user-controlled URL | Reference pattern for admin console API calls |

**Admin console XSS pattern**: `renderAccounts` / `renderUsers` use `escapeHtml()` for nickname/phone fields; `renderSummary` uses static metadata + numeric values (safe). Demo account passwords shown in plaintext is intentional for local demo seed feature.

### Third-Party & Test Modules (Read-Only Reference)

| Module | Nature | Audit Stance |
|--------|--------|--------------|
| `frontend/android/backdrop/` (`com.kyant.backdrop`) | Third-party Compose rendering library — Blur/Shadow/Highlight/RenderEffect/RuntimeShader graphics | Read-only REVIEWED; do not refactor third-party rendering code |
| `frontend/android/benchmark/` | Macrobenchmark instrumentation — `MacrobenchmarkRule` + UiAutomator flows | Read-only REVIEWED; test-only, no production security surface |
| `frontend/android/core/model/src/test/` | Serialization contract tests — verify `@SerialName` snake_case + `ignoreUnknownKeys` backward compat | Read-only REVIEWED; IDs use `Long` (safe) |

### Cross-Platform ID Safety Rule

- **Backend**: IDs are `Long` (Java) — safe up to 2^63
- **Android**: IDs are `Long` (Kotlin) — safe up to 2^63
- **Web**: IDs MUST use `string` / `BigInt` / `EntityId` type — **never** `Number()` (loses precision above 2^53). Use `readQueryId` for route query parsing and `sameEntityId` for comparison. The historical `ProductEditPage.vue` `Number(route.query.id)` blocked finding has been closed; current route query parsing in `ProductEditPage.vue` already uses `readQueryId(route.query.id)`.

### Audit Reuse Checklist (Before Writing New Code)

1. Am I formatting money? → Use `MoneyFormatter` (Android) / `formatCurrency` (Web)
2. Am I formatting dates? → Use `TimeFormatter` (Android) / `formatDate` (Web)
3. Am I mapping a status code to a label? → Use `StatusLabels.*` (Android) / `business.ts` (Web)
4. Am I parsing a query param? → Use `ParseUtils.*` (Backend) / `readQueryId` (Web)
5. Am I paginating? → Use `PaginationUtils.slice` (Backend)
6. Am I building an API response? → Use `ApiResponse.success/failure` (Backend)
7. Am I mapping an entity to DTO? → Use `EntityMappers.toDto/toEntity` (Android)
8. Am I writing a `when` with >2 branches for Int→String? → Convert to `mapOf` lookup table
9. Am I writing a `for` loop to build a list? → Use `stream().map().toList()` (Backend) / `map{}` (Android)
10. Am I creating a local `DecimalFormat`/`SimpleDateFormat`? → Use ThreadLocal formatters instead
11. Am I creating a new database table? → MUST include `owner_user_id BIGINT NOT NULL` + owner-scoped index (follow V7+ pattern)
12. Am I writing a repository `@Query`? → MUST include `ownerUserId` parameter for multi-tenant isolation
13. Am I inserting user data via `innerHTML` (Web/JS)? → MUST call `escapeHtml()` first (admin-console pattern)
14. Am I writing an Android network config? → Keep `cleartextTrafficPermitted="false"` in both debug and release
