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
