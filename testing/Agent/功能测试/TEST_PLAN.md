# Agent 功能测试全覆盖方案

## Objective

以真实用户工作流为中心，验证 Agent 在后端、安卓、Web、iOS 之间的能力一致性和可交付性。

## Full Functional Matrix

### Conversation Lifecycle

1. create new conversation
2. continue existing conversation
3. rename or update conversation metadata if supported
4. delete conversation
5. reload history after app restart or page refresh

### Messaging

1. plain text ask
2. follow-up ask in same conversation
3. empty tool result ask
4. blocked ask
5. stream interruption and retry
6. cancel in-flight run

### Tooling and Evidence

1. read-only business lookup
2. draft creation tool
3. evidence block rendering
4. tool audit visibility
5. run audit readback

### Multimodal

1. upload image
2. ask with image
3. text-to-image
4. image-to-image
5. missing image asset error

### Draft Flow

1. create draft from Agent
2. list pending drafts
3. confirm draft
4. cancel draft

## Cross-Platform Validation

The same scenario must be validated on:

1. backend API level
2. Android client
3. Web client
4. iOS client where implemented

## Release Gates

1. no missing audit trail
2. no silent stream failure
3. conversation history survives reload
4. multimodal flow returns usable output

## Exit Criteria

1. All scenario families above have execution evidence.
2. At least one end-to-end run per client is recorded against a real backend.
