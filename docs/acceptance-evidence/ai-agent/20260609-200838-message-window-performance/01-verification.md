# Verification

## Code Evidence

- Android `AgentChatViewModel.loadConversation()` now calls `AgentV2Repository.listRecentMessages(conversationId)` instead of the no-query `listMessages(conversationId)` path.
- Android `AgentV2Repository.listRecentMessages()` fixes the first history window to `page=0` and `limit=80`.
- Backend `V2AgentConversationService.listMessages()` queries messages by `createdAt DESC, id DESC` for the bounded window, then returns that window sorted by `createdAt ASC, id ASC` so the chat timeline order does not visually change.

## Commands Run

```bash
JAVA_HOME=/Users/sunyiyang/.local/jdks/temurin-21/Contents/Home \
  ./master-goods-android/gradlew \
  -p /Users/sunyiyang/Desktop/Project/master-goods/master-goods-android \
  :feature:agent:testDebugUnitTest :data:agent:testDebugUnitTest \
  --console=plain \
  -Dorg.gradle.java.home=/Users/sunyiyang/.local/jdks/temurin-21/Contents/Home
```

Result: `BUILD SUCCESSFUL in 15s`.

```bash
JAVA_HOME=/Users/sunyiyang/.local/jdks/temurin-21/Contents/Home \
  /Users/sunyiyang/.gradle/wrapper/dists/gradle-8.9-bin/90cnw93cvbtalezasaz0blq0a/gradle-8.9/bin/gradle \
  test \
  --tests 'com.zhihuiji.backend.application.service.v2.V2AgentConversationServiceTest' \
  --tests 'com.zhihuiji.backend.application.service.v2.V2AgentAiServiceTest' \
  --tests 'com.zhihuiji.backend.api.controller.AdminControllerProdProfileTest' \
  --console=plain
```

Result: `BUILD SUCCESSFUL in 45s`.

```bash
python3 tools/ai_agent_forbidden_scan.py --self-test
```

Result: `ai_agent_forbidden_scan self-test passed`.

```bash
python3 tools/ai_agent_forbidden_scan.py \
  --output docs/acceptance-evidence/ai-agent/20260609-200838-message-window-performance/09-forbidden-scan.md
```

Result: report status `pass-for-static-scan`, `Needs evidence hits: 0`.

```bash
git diff --check
```

Result: no output.

## Blocked Device Evidence

```bash
/Users/sunyiyang/Library/Android/sdk/platform-tools/adb devices
```

Result: device list was empty. This package therefore cannot claim Android screenshot, UI tree, logcat, gfxinfo, first-visible latency, transient tool hint, or cancel-run UX acceptance.
