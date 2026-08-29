# Environment

- wave_id: `20260830-terra-android-wave-01`
- test_id: `AG-CLI-AND-006`
- source_commit: `5185c21d26f46e8a9e1bab3860c4cb4108446a34`
- app: `com.zhihuiji.app` debug `1.0.0 (1)`; build passed, install was not reached
- target: AVD `Zhihuiji_API34`, expected `emulator-5554`, Android API 34, 720x1280
- device boundary: ADB connected transiently, then the emulator exited before `sys.boot_completed=1`; software-GPU retry reached the same boundary
- service boundary: anonymous HTTPS root probe returned HTTP 403; no authentication request was sent
- credentials: no fixture, token, cookie, authorization, password, private key, or complete auth payload was read or retained
