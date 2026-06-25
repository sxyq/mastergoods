# Repository Guidelines

## Project Structure & Module Organization

This is a mixed 智慧记 / Master-Goods repository. Backend Spring Boot code lives in `src/main/java/com/zhihuiji/backend`, tests in `src/test/java`, and Flyway migrations in `src/main/resources/db/migration`. The Android app is under `master-goods-android/`, split into `app`, `core`, `data`, `feature`, `benchmark`, and `backdrop` modules. The PC admin is a Vue/Vite app in `web/` with source in `web/src`. Native iOS work is in `ios/ZhihuijiIOS` with tests in `ios/ZhihuijiIOSTests`. Operational files live in `deploy/`, docs in `docs/`, and utility scripts in `tools/`.

## Build, Test, and Development Commands

- `./gradlew bootRun`: run the backend locally with Java 21.
- `./gradlew test`: run backend JUnit tests and generate JaCoCo reports.
- `cd web && npm run dev`: start the Web admin Vite server.
- `cd web && npm run build`: type-check and build the Web admin.
- `cd master-goods-android && ./gradlew :app:compileDebugKotlin`: compile Android app Kotlin.
- `cd master-goods-android && ./gradlew assembleDebug`: build a debug APK.
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

Before adding new components, classes, or helpers, check this inventory. New files are disallowed during the audit; reuse and extend existing assets in place.

### Backend Reusable Classes (`src/main/java/com/zhihuiji/backend/api/common/`)

| Class | Purpose | Reuse Pattern |
|-------|---------|---------------|
| `ApiResponse` | Unified API response envelope with `success(T)` / `failure(int,String)` factories and `CODE_*` constants | All controller return types; never build raw response maps |
| `BusinessException` | Business error with code (default `CODE_UNPROCESSABLE_ENTITY`) | Throw in service layer; caught by `GlobalExceptionHandler` |
| `GlobalExceptionHandler` | Centralized exception→`ApiResponse` mapping (validation/constraint/business/access denied/unknown) | Do not catch and map manually in controllers |
| `IdGenerator` | Snowflake-style ID via `SecureRandom` + collision check | Inject and call `nextId()`; do not create separate ID generators |
| `PaginationUtils` | `slice(list, page, size)` with `MAX_SIZE` clamp | Use in controller `list` endpoints; do not inline `subList` math |
| `ParseUtils` | `parseLong/parseDouble/parseInteger` (Template Method pattern, null-safe) | Use for query param parsing; do not write try/catch parse blocks |
| `OrderStatus` / `PayOrderStatus` / `PurchaseOrderStatus` / `PurchaseReceiptStatus` / `PurchaseReturnStatus` / `SalesReturnStatus` / `PaymentType` | Status enums with `code()` / `fromCode(Integer)` / `isValid(Integer)` | Follow this enum pattern for new status types; do not create `final class` + int constants |

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

#### `web/src/shared/utils/business.ts` Helpers

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

#### `web/src/app/stores/session.ts` Store

| Export | Purpose |
|--------|---------|
| `session.token` / `session.refreshToken` / `session.userId` | Auth state |
| `session.hasPermission(perm)` / `session.hasAnyPermission(perms)` | Client-side authZ (OWNER = all; api = backend Set; demo = role-based) |
| `session.login` / `session.logout` / `session.refreshProfile` / `session.refreshStoreContext` | Auth actions |
| `session.switchRole` / `session.switchMember` / `session.enterDemo` | Local role/member switching |

#### `web/src/entities/auth/roles.ts`

- `roleLabels` / `roleDescriptions` / `rolePermissions` / `rolePermissionSets` — static role config
- `canAccess(role, perm)` — role-based permission check for demo mode

#### `web/src/entities/screen/live-screen-data.ts`

- `loadLiveScreenData` — route-dispatched data loader
- `mapSalesOrders` / `mapPurchaseOrders` / `mapProducts` / `mapCustomers` / `mapSuppliers` / `mapFinanceRecords` etc. — data mapping functions (single-pass, pre-allocated)

#### Web Design Patterns in Use

- **Field Table Driven**: `session.ts` `persist` / `clearPersisted` — `PERSISTED_FIELDS` array drives localStorage read/write, eliminates duplicated key lists
- **Lookup Table**: `screenComponentByRoute` — route→component map; `statusTabs` — tab config arrays
- **Extract Shared Fetch**: `AgentPage.vue` `fetchSidePanel` — single function reused by `loadPage` / `refreshSidePanel`
- **Remove Duplicate Helpers**: `live-screen-data.ts` — deleted 7 functions duplicated in `business.ts`, switched to imports

### Cross-Platform ID Safety Rule

- **Backend**: IDs are `Long` (Java) — safe up to 2^63
- **Android**: IDs are `Long` (Kotlin) — safe up to 2^63
- **Web**: IDs MUST use `string` / `BigInt` / `EntityId` type — **never** `Number()` (loses precision above 2^53). Use `readQueryId` for route query parsing and `sameEntityId` for comparison. The `ProductEditPage.vue` `Number(route.query.id)` pattern is a known BLOCKED finding (requires API client signature change to `EntityId`).

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
