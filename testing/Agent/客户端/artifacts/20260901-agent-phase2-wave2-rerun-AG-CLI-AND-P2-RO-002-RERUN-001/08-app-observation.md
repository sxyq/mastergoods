# Android observation

- Device: `emulator-5554`; package: `com.zhihuiji.app` version `1.0.0`.
- From a blank Agent conversation, UI tree showed the input field at `[270,1084][574,1196]`; after keyboard focus the send action was at `[610,526][658,574]`.
- Entered `Summarize cashflow for the last 30 days` and tapped the send action at `(634,550)`.
- The App rendered one completed execution step sourced from `cashflow_summary_lookup`. It showed total income `¥0.00`, total expenses `¥0.00`, net cash flow `¥0.00`, record count `0`, and a no-activity message for the requested period.
- The App request log showed HTTP 200 and `text/event-stream`; crash buffer contained zero lines.
- `08-ui-input-summary.txt`, `08-ui-after-summary.txt`, `08-app-result.png`, `08-http-logcat-redacted.txt`, and `08-crash-buffer.txt` are the supporting device artifacts.
