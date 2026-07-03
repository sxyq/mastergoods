# 后端单元测试全覆盖方案

## Objective

为 Spring Boot 后端建立函数级全覆盖测试体系，覆盖 `controller`、`service`、`repository`、`security`、`ai`、`migration` 六层，并输出可追踪的覆盖账本。

## Scope

源码主范围：

- `src/main/java/com/zhihuiji/backend/api`
- `src/main/java/com/zhihuiji/backend/application`
- `src/main/java/com/zhihuiji/backend/domain`
- `src/main/java/com/zhihuiji/backend/infrastructure`
- `src/main/resources/db/migration`

现有测试基础：

- `src/test/java/com/zhihuiji/backend/api/controller/*`
- `src/test/java/com/zhihuiji/backend/application/service/*`
- `src/test/java/com/zhihuiji/backend/application/service/v2/*`
- `src/test/java/com/zhihuiji/backend/infrastructure/repository/*`
- `src/test/java/com/zhihuiji/backend/infrastructure/db/*`
- `src/test/java/com/zhihuiji/backend/infrastructure/ai/*`

## Coverage Standard

### Must Cover

1. Every public method in handwritten production classes.
2. Every permission and owner-scope branch.
3. Every validation failure branch.
4. Every migration script.
5. Every AI fallback and stream error branch.
6. Every agent audit branch: running, completed, blocked, cancelled, failed.

### Allowable Exemptions

- Entity getters/setters with no business logic
- Spring configuration wiring with zero custom logic
- Generated code and framework boilerplate

Each exemption must be listed in a coverage ledger.

## Test Layers

### 1. Controller Slice Tests

Tooling:

- `@WebMvcTest`
- `MockMvc`
- mocked service dependencies

Per endpoint minimum cases:

1. Success
2. Validation failure
3. Permission failure or forbidden branch
4. Empty result or not found branch
5. Owner-scope isolation branch where applicable

Priority controllers:

- `api/controller/v2/V2AgentController`
- `api/controller/v2/*` all domain controllers
- compatibility controllers under `api/controller`
- sync/import controllers
- report controllers

### 2. Service Unit Tests

Tooling:

- JUnit 5
- Mockito
- deterministic fake repositories or mocked collaborators

Per public method minimum cases:

1. Normal success
2. Invalid input
3. Missing dependency data
4. Cross-tenant access denied
5. Persistence failure or downstream failure
6. Derived field updates and summary updates

Mandatory high-priority classes:

- `V2AgentAiService`
- `V2AgentConversationService`
- `AgentImageService`
- `AgentDraftConfirmService`
- `ToolPlanner`
- `AnswerSynthesizer`
- `RunAuditService`
- `SseStreamEmitter`
- `LongCatAnthropicClient`

### 3. Repository Tests

Tooling:

- `@DataJpaTest`
- H2-compatible assertions

Per repository family minimum cases:

1. Owner filter correctness
2. Pagination correctness
3. Sort order correctness
4. Null/empty filters
5. Unique constraint and FK behavior where applicable

### 4. Migration SQL Tests

Every migration file must have:

1. Table existence assertions
2. Index existence assertions
3. Constraint existence assertions
4. Owner-scope column assertions
5. Agent and media schema assertions where relevant

### 5. Security and Config Tests

Must verify:

1. store permission checks
2. local profile guards
3. admin console restrictions
4. CORS and profile-sensitive behavior

## Function Coverage Ledger Requirements

Create and maintain:

- `backend-function-coverage-ledger.csv`

Columns:

- class
- method
- layer
- has_test
- test_class
- test_method
- branch_notes
- exempt
- exemption_reason

## Command Set

Primary commands:

```bash
./gradlew test
./gradlew jacocoTestReport
```

Recommended targeted commands:

```bash
./gradlew test --tests "com.zhihuiji.backend.application.service.v2.V2AgentAiServiceTest"
./gradlew test --tests "com.zhihuiji.backend.api.controller.V2AgentMediaControllerTest"
```

## Exit Criteria

1. All handwritten backend public methods are mapped in the ledger.
2. No release-critical backend package is below 95 percent line coverage.
3. Agent backend package is at or above 98 percent method mapping completeness.
4. Every migration has a validation test.
5. All owner-scope repositories have positive and negative isolation tests.

## Deliverables

- JaCoCo XML
- JaCoCo HTML
- coverage ledger CSV
- failure gap list
- rerun command log
