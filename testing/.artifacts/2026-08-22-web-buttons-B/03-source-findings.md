# Source findings

## Failed findings

1. `WEB-B-F-001`: `stopStreaming()` aborts and requests server cancellation, but `onBeforeUnmount()` only restores `document.body.style.overflow`. Navigating away during an active stream has no page-lifecycle cancellation path. Evidence: `AgentPage.vue:438-451` and `1153-1156`.
2. `WEB-B-F-002`: `fetchSidePanel()` uses one `Promise.all` for workbench, conversations, drafts, tasks, and notifications. A single rejected request prevents all five assignments. Evidence: `AgentPage.vue:260-273` and `276-300`.
3. `WEB-B-F-003`: pending-draft confirm/cancel buttons are rendered without `v-if="canWrite"` or a `!canWrite` disabled expression. The handlers return early, so a read-only member sees an action that has no effect. Evidence: `AgentPage.vue:822-848` and `1894-1923`.
4. `WEB-B-F-004`: `saveDraft()` returns silently when title, type, or content is blank. The structured validation branch sets `error`, but the basic required-field branch does not. Evidence: `AgentPage.vue:736-746`.
5. `WEB-B-F-005`: `loadDrafts()` catches and ignores refresh errors. The caller can close the editor or dismiss a pending draft while the list refresh fails, with no visible retry state. Evidence: `AgentPage.vue:707-714`, `764-766`, and `828-842`.
6. `WEB-B-F-006`: stopping a stream first sets a friendly local message, then `AbortController.abort()` can reject the stream promise. `sendMessage()` catches that rejection and calls `markStreamingMessageError()` again, which can replace the stop message with the browser abort error. Evidence: `AgentPage.vue:422-435` and `438-450`.

## Positive source checks

- Route metadata and the global guard require `agent:view`; page write controls use `agent:write` in the normal conversation, send, quick-question, and side-panel draft paths.
- Shared request handling converts non-success responses into `ApiError`, emits 401/403 auth events, attempts one refresh for ordinary authenticated requests, and redirects through `main.ts`.
- SSE parsing handles chunk boundaries, `[DONE]`, malformed JSON, tool progress/completion/failure, answer deltas, result blocks, drafts, cancellation, and terminal errors.
- Markdown, result-block values, and draft fields are escaped before the two `v-html` surfaces; links are limited to `http`, `https`, and `mailto`.

These are source-level conclusions. They do not upgrade the blocked runtime cases.
