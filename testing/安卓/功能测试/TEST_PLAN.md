# 安卓功能测试全覆盖方案

## Objective

在真实用户路径层面覆盖 Android 端全部核心功能，并验证其与真实后端的联动行为。

## Scope

应用功能域：

- login and registration
- dashboard and home
- products
- customers
- suppliers
- sales
- purchases
- payments and finance
- reports
- sync/import
- settings
- agent

## Functional Matrix Standard

每个功能必须至少覆盖：

1. 主路径
2. 空数据路径
3. 错误提示路径
4. 断网或服务失败路径
5. 账号/权限差异路径

## Test Levels

### 1. Instrumented UI Tests

Use:

- `androidTest`
- Compose UI test APIs
- Espresso or UiAutomator where needed

### 2. Real Backend Validation

Use:

- local backend
- deployed backend on release candidate environment

Validate:

- data displayed is real
- write operations succeed
- refresh after navigation works

## Android Agent Functional Coverage

Mandatory scenarios:

1. open Agent page
2. load conversation list
3. switch conversation
4. delete conversation
5. send text question
6. receive streaming answer
7. cancel run
8. reload message history
9. upload image
10. send multimodal question
11. generate image
12. dismiss generated image
13. draft confirmation flow
14. blocked safety flow
15. retry after stream failure

## Device Matrix

Minimum devices:

1. one physical Android phone
2. one API 34 emulator
3. one smaller screen profile

## Evidence Requirements

Per scenario:

- device or emulator identifier
- build version
- backend target
- screenshot or screen recording
- adb logcat snippet if failed

## Suggested Commands

```bash
cd master-goods-android
./gradlew connectedDebugAndroidTest
adb devices
```

## Exit Criteria

1. Every top-level Android feature has a scenario row.
2. Agent main flow is verified against a real backend.
3. All critical business create/update flows are validated on-device.
