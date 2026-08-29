# Android Agent live test report

- wave_id: `20260830-terra-android-wave-01`
- model: `gpt-5.6-terra / Medium`
- source_commit: `5185c21d26f46e8a9e1bab3860c4cb4108446a34`
- worktree: existing dirty worktree preserved; 64 pre-existing changed/untracked paths were not staged
- app: `com.zhihuiji.app`, debug `1.0.0 (1)`, Activity `com.zhihuiji.app/.MainActivity`
- AVD target: `Zhihuiji_API34`, expected serial `emulator-5554`, Android API 34, 720x1280
- service target: `https://zhj-api.sxyq27.online/`; anonymous root probe in this batch returned HTTP 403

## Result

| scope | Passed | Failed | Blocked | Deferred |
|---|---:|---:|---:|---:|
| source/APK metadata | 2 | 0 | 0 | 0 |
| debug build | 1 | 0 | 0 | 0 |
| emulator boot/connect | 0 | 0 | 1 | 0 |
| APK install and normal UI login | 0 | 0 | 1 | 0 |
| AG-CLI-AND-001..010 | 0 | 0 | 10 | 0 |
| Agent tools: 46 READ_ONLY + 15 CREATE_ONLY | 0 | 0 | 61 | 0 |

## Boundary

The debug build completed from the existing worktree. The AVD connected briefly, then exited before `sys.boot_completed=1`; a retry with the same AVD and software GPU also exited before boot completion. The APK was therefore not installed in this batch, and the normal Android login UI could not be reached. No fixture was entered, no login request was sent, and no session credential or authentication payload was read, printed, or saved.

Because the device/session precondition failed, no Agent prompt, SSE stream, tool call, formal answer, result block, draft transition, image Provider call, Loop, compaction, cancellation, reconnect, retry, history, or foreground/background Agent flow was executed. These are `Blocked`, not historical passes.

The prior 20260829 report remains historical evidence only. Its login result was HTTP 422 twice and must not be merged with this batch's emulator boot failure.

## Evidence

- Per-flow evidence: `testing/Agent/客户端/artifacts/20260830-terra-android-wave-01-AG-CLI-AND-001/` through `...-010/`
- Tool ledger: `testing/Agent/客户端/reports/20260830-terra-android-wave-01-tool-status.csv`
- No raw emulator log was retained because the launcher output could contain device key material; only the redacted environment summaries were retained.
