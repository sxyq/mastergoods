# Android observation

- `test_id`: `AG-S-REMOTE-ENV-001`
- `category_id`: `AG-S-ENV-REMOTE`
- `wave_id`: `Wave 2`
- `result`: `Blocked`
- `device`: `emulator-5554`
- `package`: `com.zhihuiji.app`
- `observation`: `MainActivity` is on the login form. A phone field is prefilled in the UI; its value is intentionally not recorded. The password field is masked. No login action was submitted because no approved test password/session was available.
- `run_id`: none observed in filtered logcat output.
- `evidence_limit`: this is Android UI context only; no Agent HTTP/SSE request from the app was captured in this probe.
