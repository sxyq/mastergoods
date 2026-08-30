test_id: AG-CLI-AND-001
category_id: AG-CLI-AND-001
wave_id: 20260831-agent-live-05-C-W0
environment: Android Emulator `Zhihuiji_API34`, serial `emulator-5554`, Android 14 / API 34, 720x1280
account_store_label: account=unbound; store=unbound
precondition: Existing installed package was available; no safe login state or user-provided development password was available in scope.
redacted_input: none; no phone number, password, token, cookie, or authorization value was entered.
operation: Start the requested AVD, wait for `sys.boot_completed=1`, resolve the installed package activity, launch `com.zhihuiji.app/.MainActivity`, dump UI XML, capture a screenshot, and save redacted logcat.
expected_tools_order: no Agent tools; no network action expected from the preparation step.
loop_compaction: not exercised; no Agent run.
http_sse: no request was sent from the app in this preparation step; runtime endpoint remains unconfirmed.
formal_answer: not applicable; no Agent answer was generated.
db_before: not queried from the client preparation step.
db_after: no client-driven database change observed.
boundary: no click, text input, login, APK install, or local 18080 access was performed.
acceptance: AVD booted; package exists; `MainActivity` resolves; initial UI is an empty login screen; source release defaults point to the public HTTPS API and disallow cleartext in release configuration.
evidence: 00-environment.md, 08-ui.xml, 08-ui-summary.txt, 08-logcat-redacted.log, 00-baseline.png, 08-app-observation.md, 10-conclusion.md.
cleanup: none; emulator data was not wiped and no account/session data was touched.
result: Blocked

Observed client state:

- `/Users/sunyiyang/Library/Android/sdk/platform-tools/adb` and `/Users/sunyiyang/Library/Android/sdk/emulator/emulator` were used.
- `sys.boot_completed=1`, Android release `14`, API level `34`, serial `emulator-5554`.
- Package `com.zhihuiji.app` is installed, version `1.0.0`, and resolves to `com.zhihuiji.app/.MainActivity` with the launcher intent.
- The initial UI had empty `手机号` and `密码` fields. No click or text input occurred. The UI XML and screenshot were captured after launch.
- Static release configuration in `Code/frontend/android/core/datastore/src/main/java/com/zhihuiji/core/datastore/SettingsStore.kt` and `Code/frontend/android/core/network/build.gradle.kts` identifies `https://zhj-api.sxyq27.online/` as the default production endpoint and release HTTPS enforcement. This is source evidence; it is not a runtime HTTP capture.
- No login state or development password was available through an allowed channel. Login-dependent tests are therefore blocked/deferred until the user supplies a safe UI login condition.
