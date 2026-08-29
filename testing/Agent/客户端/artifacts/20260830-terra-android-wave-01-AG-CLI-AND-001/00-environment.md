# Environment

- wave_id: `20260830-terra-android-wave-01`
- source_commit: `5185c21d26f46e8a9e1bab3860c4cb4108446a34`
- app: `com.zhihuiji.app` debug `1.0.0 (1)`; APK build passed but install was not reached
- target: AVD `Zhihuiji_API34`, expected `emulator-5554`, Android API 34, 720x1280
- device evidence: ADB connected transiently, then the emulator exited before `sys.boot_completed=1`; retry with software GPU had the same boundary
- service: anonymous HTTPS root probe returned HTTP 403; no authentication request was sent
- credentials: no fixture, token, cookie, authorization, password, private key, or complete auth payload was read or retained
