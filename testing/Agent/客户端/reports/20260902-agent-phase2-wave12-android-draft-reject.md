# 2026-09-02 Android Wave 12: create_product draft rejection

## Result

| Test ID | Plan reference | Scope | Result |
|---|---|---|---|
| `AG-CLI-AND-P2-DRAFT-REJECT-001-RERUN-001` | `AG-CLI-AND-004` | Android real input, send, draft confirmation, reject, draft/conversation cleanup | `Failed` |
| Server audit and PostgreSQL before/after | `AG-CLI-AND-004` | Run/event/database assertion for the same run | `Blocked` |

## Actual execution

- Device: `emulator-5554`, Android API 34, `com.zhihuiji.app` 1.0.0.
- Target: 8220 public API `https://zhj-api.sxyq27.online/`.
- Prompt: `新增一个商品编码为EVALONLY20260902名称为阶段二真实点击商品先生成草稿不要直接保存`.
- The App `EditText` and send node were read from UI tree. Send bounds were `[586,502][682,598]`; center `(634,550)` was tapped once.
- The App received HTTP 200 `text/event-stream` and showed the confirmation overlay. The `拒绝` node bounds were `[260,803][376,899]`; center `(318,851)` was tapped once.
- The App draft list later showed `create_product` with `已取消（未执行）`; the cancelled draft was deleted from the App.

## Findings

The server-side cancellation path returned HTTP 200. The conversation view still displayed a stale `active` status and the message `后端返回了 draft_card 数据块，但当前 Android 端无法解析其字段。` The draft list, which was loaded separately, showed the correct cancelled state. The client therefore failed to reconcile the `draft_card` data and cancellation result in the conversation detail.

The run ID, raw SSE event sequence, and current PostgreSQL counts were not collected. The SSH probe to `root@8.220.206.9` returned `Permission denied (publickey)`, so no database-level no-write claim is made.

## Cleanup

The App deleted draft `9` with HTTP 200. Conversation `161` was deleted after one timeout and one successful retry with HTTP 200. The App list no longer showed the test conversation. `pm clear com.zhihuiji.app` returned `Success`; restart showed the login page; crash buffer lines after clear: `0`.

Evidence directory: `testing/Agent/客户端/artifacts/20260902-agent-phase2-wave12-AG-CLI-AND-P2-DRAFT-REJECT-RERUN-001/`.
