# Android Agent Live Wave 0 基线摘要

- `wave_id`: `20260829-agent-android-wave0`
- `category_id`: `CLI`
- `captured_at`: `2026-08-29`（Asia/Shanghai）
- `git_head`: `93d085420076f4f2b6fd47faa0b662e45f029976`
- `worktree`: 保留既有 docs 修改和未跟踪 admin/performance/reliability 文件；本轮未修改源码、迁移、配置或既有测试计划/历史结果。

## 设备

- AVD: `Zhihuiji_API34`
- device serial: `emulator-5554`
- model: `sdk_gphone64_arm64`（AVD profile: Pixel 6）
- Android: `14` / API `34`
- display: `720x1280`, density `320`
- `sys.boot_completed=1`
- device network: emulator route present; no airplane mode; Android-side network is available for HTTPS checks.

## APK

- package: `com.zhihuiji.app`
- variant: `debug`
- version: `1.0.0`, `versionCode=1`, `targetSdk=35`, `minSdk=26`
- artifact: `/Users/sunyiyang/Desktop/Project/master-goods/Code/frontend/android/app/build/outputs/apk/debug/app-debug.apk`
- SHA-256: `69bf652b888a4ea5393834e544fc22aa76c4327b2add66ce0920c1e8dc1ca40d`
- build: `./gradlew :app:assembleDebug --offline --console=plain` -> `BUILD SUCCESSFUL`
- install: absolute-path ADB streamed install -> `Success`
- launch Activity: `com.zhihuiji.app/.MainActivity`
- login UI: opened; UI tree contains `手机号`, `密码`, and the login action.
- launch screenshot: `/tmp/master-goods-wave0-launch.png`（未作为交付证据提交，后续正式证据只放本目录）

## Backend and base URL

- App debug default base URL: `https://zhj-api.sxyq27.online/`
- local backend port: `18080` has no listener.
- `127.0.0.1:8080`: Python WebDAV process; excluded from backend validation.
- anonymous HTTPS probe to `/`: HTTP `403`, JSON response.
- anonymous HTTPS probe to `/v1/auth/users/me`: HTTP `403`, JSON response.
- service application version: not determinable without an authenticated/authorized version endpoint; the observed `nginx/1.24.0 (Ubuntu)` header is edge-server evidence only and is not reported as the backend application version.
- authenticated session: none; the authorized local development fixture was entered only through the App UI and was not persisted or written to evidence.
- App login attempt: `POST https://zhj-api.sxyq27.online/v1/auth/login` returned HTTP `422` twice; the App remained on the login screen and showed `请求参数未通过校验`.
- image Provider: not invoked; no real Provider request was made.

## Wave 0 executable/blocking matrix

| item | result | evidence |
|---|---|---|
| AVD online and booted | Passed | `adb devices -l`, `getprop sys.boot_completed=1` |
| APK debug build | Passed | Gradle output and APK metadata above |
| APK install and Activity launch | Passed | ADB install output, package metadata, UI dump |
| App normal login | Blocked | App login endpoint returned 422 twice; no authenticated session |
| Agent conversation/tool execution | Blocked | login and service preconditions unavailable |
| 61-tool UI/SSE/result verification | Blocked | requires authenticated Agent service; no tool was claimed executed |
| image_generate draft/confirm/Provider result | Blocked | authenticated Agent service and Provider confirmation unavailable |

## Evidence references

- UI tree summary: captured at `/tmp/master-goods-wave0-ui-summary.txt` for this run; only the pre-input tree is retained in the artifact directory.
- UI screenshot: captured at `/tmp/master-goods-wave0-launch.png` for this run.
- Client test plan: `testing/Agent/客户端/TEST_PLAN.md`.

The current App/device baseline is usable for retry. The backend/authenticated-session blocker is retained as a live-wave condition; no historical test result was changed.
