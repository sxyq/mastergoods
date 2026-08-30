# AG-CLI-AND-009 environment

- wave_id: `20260830-luna-android-wave-01`
- captured_at: `2026-08-30T12:43:26+08:00`
- source_head: `c4ae9b86dc8582e48613b43d6318c8ef3e02204e`
- requested runner: `gpt-5.6-luna / max`; runtime identifier未独立暴露
- device: `Zhihuiji_API34` / `emulator-5554` / Android 14 / API 34 / 720x1280 / density 320
- device state: `sys.boot_completed=1`; AVD 在本批安装、启动和 UI 采集期间在线
- app: `com.zhihuiji.app` debug `1.0.0 (1)`; Activity `com.zhihuiji.app/.MainActivity`
- build: `./gradlew :app:assembleDebug --console=plain` -> `BUILD SUCCESSFUL`
- install: `adb install -r` -> `Success`
- service URL: `https://zhj-api.sxyq27.online/`; host anonymous probe -> HTTP 403
- login submitted: `false`; owner/store session: `false/false`
- pre-existing worktree files: preserved and excluded from this batch
- flow: `前后台切换`
- status: `Deferred`
- reason: 没有运行中的本批 Agent run
