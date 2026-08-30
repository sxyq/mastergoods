Device: `Zhihuiji_API34` / `emulator-5554` / Android 14 API 34 / 720x1280.

Package and activity:

- package: `com.zhihuiji.app`
- resolved activity: `com.zhihuiji.app/.MainActivity`
- installed version: `1.0.0`
- boot property: `sys.boot_completed=1`

Screen observation:

- MainActivity is visible on the initial login screen.
- The UI tree contains empty phone and password EditText nodes. No account/store label was exposed.
- No click, swipe, text input, login, or Agent request was performed.
- UI XML: `08-ui.xml`; compact tree: `08-ui-summary.txt`; screenshot: `00-baseline.png`; redacted logcat: `08-logcat-redacted.log`.

Endpoint boundary:

- Static production source evidence points to `https://zhj-api.sxyq27.online/`.
- Release configuration sets cleartext base URLs to false and enforces the trusted release host.
- Because no app request was sent, runtime routing to the public host is not yet confirmed. No local `18080` endpoint was accessed.
