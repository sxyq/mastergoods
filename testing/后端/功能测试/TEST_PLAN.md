# 后端功能测试全覆盖方案

## Objective

验证后端所有业务功能在真实 HTTP 接口层的行为闭环，包括权限、校验、owner 隔离、状态流转、审计落库和错误返回。

## Scope

功能域按业务拆分为：

1. Auth and account
2. Store and permission context
3. Product and category
4. Customer and supplier
5. Partner groups and contacts
6. Sales and sales returns
7. Purchase, receipt, return
8. Finance, payment, account transfer, cash change
9. Inventory, ledger, adjustment, count draft
10. Sync and import jobs
11. Media and upload
12. Agent conversations, messages, drafts, runs, images
13. Reports and dashboards

## Functional Matrix Standard

Each feature must contain:

1. Happy path
2. Invalid parameter path
3. Not found path
4. Permission forbidden path
5. Cross-owner isolation path
6. State transition path
7. Regression path for known bugs

## Execution Levels

### API Slice

Purpose:

- Fast regression for request and response contracts

Verification:

- HTTP status
- response envelope
- field names
- validation error mapping

### Service + DB Integration

Purpose:

- Verify actual persistence and side effects

Verification:

- data created or updated
- audit and timestamps updated
- related entity links preserved

### Release Candidate Smoke

Purpose:

- Validate the assembled app backend behavior on a running instance

Verification:

- auth
- CRUD
- import
- sync
- agent run
- media

## Agent Functional Coverage

Mandatory scenarios:

1. Create conversation, then send text question.
2. Send text question with existing conversation id.
3. Stream answer through SSE and receive ordered event sequence.
4. Safety-blocked request returns blocked status and no business tool execution.
5. Cancel active run and confirm final run status becomes `cancelled`.
6. Read run audit and confirm event sequence exists.
7. Create manual draft and confirm or cancel it.
8. Generate image from prompt.
9. Generate image from prompt plus reference asset.
10. Send multimodal question with `image_asset_ids`.
11. Reload messages and confirm user and assistant history is preserved.

## Core Domain Functional Coverage

Per domain validate:

- create
- read list
- read detail
- update
- delete or archive
- status transition
- permission isolation
- data consistency with related modules

## Test Data Strategy

Three datasets:

1. Minimal clean owner dataset
2. Dense business dataset for list, report and search paths
3. Cross-owner isolation dataset with conflicting ids or names

## Command Set

Recommended commands:

```bash
./gradlew test
./gradlew bootRun
```

Runtime validation against live local backend:

```bash
curl http://127.0.0.1:18080/...
```

## Evidence Requirements

Per scenario archive:

- request
- response
- affected database rows or repository assertions
- server log snippets when needed

## Exit Criteria

1. Every backend feature has a scenario row in the functional matrix.
2. Every release-critical endpoint has both positive and negative cases.
3. Agent chain is verified end-to-end including audit readback.
4. No domain lacks owner-isolation coverage.
