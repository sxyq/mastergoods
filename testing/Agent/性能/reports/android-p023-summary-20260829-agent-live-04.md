# AG-P-023 Android Device Summary

The corrected Wave 0 preflight counted one device: `emulator-5554` (`Zhihuiji_API34`, Android 14/API 34). The current debug APK is `com.zhihuiji.app` version `1.0.0`, versionCode `1`, with `DEBUGGABLE` package flags. The APK was rebuilt successfully with `./gradlew :app:assembleDebug` and installed to the emulator.

The app launched into the login page. The bounded device sample has 10 ADB launch attempts. Host-command timings were mean 131ms, P50 110ms, P95 250ms, minimum 90ms and maximum 250ms; see `../artifacts/20260829-agent-live-01-AG-P-023/startup-samples.tsv`.

Frame evidence in `../artifacts/20260829-agent-live-01-AG-P-023/gfxinfo-current.txt`: 141 rendered frames, 87 janky frames (61.70%), 50th percentile 44ms, 90th 77ms, 95th 85ms, 99th 300ms. Memory evidence in `meminfo-current.txt`: TOTAL PSS 135042 KB, Java Heap 13028 KB, Native Heap 18160 KB, one Activity and seven Views.

Perfetto evidence is `../artifacts/20260829-agent-live-01-AG-P-023/agent-p023.pftrace` at 5.0MB. It was collected with scheduler/frequency/idle/activity/window/gfx/view/binder/dalvik categories. Host `trace_processor_shell` was unavailable, so no trace SQL interpretation is claimed. The emulator reported software GL, which limits comparison to this diagnostic run.

The local Spring service was not available for the corrected live-04 preflight. No authenticated Agent request, remote development endpoint load, provider call or database query was sent. Therefore the planned Agent display result is `Blocked`, while the device shell sampling itself is recorded as actual evidence.
