# 安卓单元测试全覆盖方案

## Objective

为 Android 多模块工程建立函数级测试映射，覆盖 `core`、`data`、`feature`、`app` 四层，并确保 Agent 链路优先达到发布级可回归状态。

## Scope

模块范围：

- `app`
- `core/common`
- `core/database`
- `core/datastore`
- `core/designsystem`
- `core/model`
- `core/network`
- `data/*`
- `feature/*`

## Coverage Policy

### Must Cover

1. Every handwritten public function in repositories and view models.
2. Every parser, formatter, mapper and status label function.
3. Every network request wrapper and SSE parser branch.
4. Every Agent UI state transition.
5. Every deep link and launch extra parser.

### Exemption Rules

- trivial Compose preview functions
- generated Hilt glue
- pure data class default methods

## Unit Test Layers

### 1. Core Logic

Targets:

- `MoneyFormatter`
- `TimeFormatter`
- `StatusLabels`
- serialization models
- network config builders

### 2. Network Layer

Targets:

- request contract
- response field mapping
- safe api wrappers
- SSE event parsing
- cancellation and retry behavior

### 3. Data Repositories

Each repository method must have:

1. success case
2. API error case
3. null or empty data case
4. serialization compatibility case where needed

### 4. Feature ViewModels

Each ViewModel action must verify:

- state before
- triggering input
- resulting state
- error path
- concurrent/duplicate guard if present

Priority focus:

- `feature/agent/AgentChatViewModel`
- auth screens
- dashboard view models
- business domain view models

## Agent-Specific Unit Coverage

Must cover:

1. local message insertion
2. conversation switching
3. message reload
4. stream start and stop
5. safety blocked state
6. draft confirm state
7. result block parsing
8. tool status timeline
9. image upload state
10. image generation state

## Tools

- JUnit
- coroutine test utilities
- fake repository and fake SSE flows
- Robolectric where framework behavior is unavoidable

## Command Set

```bash
cd master-goods-android
./gradlew test
./gradlew :feature:agent:test
./gradlew :core:network:test
```

## Coverage Ledger

Create:

- `android-function-coverage-ledger.csv`

Columns:

- module
- class
- function
- has_test
- test_file
- test_case
- exempt
- note

## Exit Criteria

1. Every handwritten repository and view model function is mapped.
2. Agent Android classes have complete state-transition coverage.
3. No critical module remains without at least one failure-path test.
