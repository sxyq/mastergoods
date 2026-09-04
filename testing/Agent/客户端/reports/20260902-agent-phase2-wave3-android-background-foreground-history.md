# Phase 2 Android real-click evidence: background/foreground and history recovery

## Result

| test_id | scope | result |
|---|---|---|
| `AG-CLI-AND-P2-BGFG-001` | Send a real query, press Home during the request, restore from Recent Tasks, verify the result, and remove conversation `149` in the App | `Passed` |
| `AG-CLI-AND-P2-HISTORY-001` | Open an existing completed conversation from the App history list and compare the displayed run trace with the server audit | `Passed` |

## Real Android actions

1. The App input field and send action were located from the UI tree.
2. A query was entered into the App and the send action was tapped once.
3. Android Home was pressed while the public stream request was still in flight.
4. Recent Tasks was opened and the `com.zhihuiji.app` task card was tapped from the UI tree.
5. The restored result page showed the complete answer without a duplicate block.
6. The Agent home history list was opened. The current card showed `Show sales, purchases, i` and `4 条消息 · 2026-09-02 01:10`.
7. The current conversation was opened in the App conversation list and deleted using the UI-tree-derived delete action.
8. `pm clear com.zhihuiji.app` returned `Success`; a restart showed the login page.
9. Separately, an existing history card was selected from the Agent home. Conversation `138` loaded without sending a new message.

## Server observations

- The background/foreground request returned HTTP 200 `text/event-stream` and produced run `12de88ba-faed-4ed6-bb6b-8a41d595b0ab`, conversation `149`, two tool calls, 17 ordered events, and `audit_lossy=false`.
- Its persisted tool order was `cross_analysis_lookup` followed by `cashflow_summary_lookup`; both start and complete events paired by call ID.
- Before deleting the test conversation, the aggregate counts were `agent_conversations=6`, `agent_messages=20`, `agent_run_audits=18`, `agent_run_audit_events=258`, `agent_drafts=0`, `agent_tasks=0`.
- After App deletion and local cleanup, counts were `agent_conversations=4`, `agent_messages=10`, `agent_run_audits=18`, `agent_run_audit_events=258`, `agent_drafts=0`, `agent_tasks=0`.
- Runs and audit events were retained after the conversation deletion by the server's audit retention relationship; business counts stayed `products=693`, `finance_records=2661`, `stores=2`, `store_memberships=2`.
- The history recovery request loaded conversation `138` with run `03b74144-8b8d-4b74-a5cd-07e35c3acfb0`, one tool, seven ordered events, and unchanged counts.

## Evidence

- Background/foreground: `testing/Agent/客户端/artifacts/20260902-agent-phase2-wave3-AG-CLI-AND-009-001/`
- History recovery: `testing/Agent/客户端/artifacts/20260902-agent-phase2-wave4-AG-CLI-AND-005-001/`
- The background/foreground artifact records that raw SSE body capture was unavailable; the server audit sequence and filtered App endpoint logs are retained instead.
- The history artifact records no new SSE because the flow is a GET-based recovery path.
