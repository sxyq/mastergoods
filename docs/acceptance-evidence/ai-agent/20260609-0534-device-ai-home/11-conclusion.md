# Device AI Home Evidence Conclusion

Status: `blocked-by-locked-device`

This package does not prove AI home cleanliness. The device was visible through
ADB and `adb reverse tcp:18080 tcp:18080` succeeded, but both screenshot and UI
tree captures stayed on the device lock screen instead of `com.zhihuiji.app`.

Captured facts:

- `01-adb-devices.txt` shows device `d715a3a4` online.
- `02-resolve-activity.txt` resolves `com.zhihuiji.app/.MainActivity`.
- `03-adb-reverse.txt` confirms reverse mapping for port `18080`.
- `05-ui-tree-initial-clean.xml` and `09-ui-tree-after-wake-clean.xml` contain
  lock-screen clock / carrier / flashlight / camera nodes, not AI home nodes.
- `06-screenshot-initial.png` is black and `10-screenshot-after-wake.png`
  captures the lock screen.

Next required evidence:

- Unlock the device and keep it awake.
- Open AI Assistant in `com.zhihuiji.app`.
- Recapture screenshot and UI tree showing the initial AI screen.
- Confirm the initial screen has no default sales amount, KPI cards, report
  charts, risk list, or business dashboard summary.

