# Android observation

- Device: `emulator-5554`; package: `com.zhihuiji.app` version `1.0.0`.
- From a blank Agent conversation, UI tree showed the input field at `[270,1084][574,1196]`; after keyboard focus the send action was at `[610,526][658,574]`.
- Entered `Show the current product list` and tapped the send action at `(634,550)`.
- The App opened one completed execution step and rendered product rows with the source label `product_catalog_lookup`; the result list was scrollable and the App offered copy, regenerate, and data-source actions.
- The App request log showed HTTP 200 and `text/event-stream`; crash buffer contained zero lines.
- `08-ui-input-summary.txt`, `08-ui-after-summary.txt`, `08-app-result.png`, `08-http-logcat-redacted.txt`, and `08-crash-buffer.txt` are the supporting device artifacts.
