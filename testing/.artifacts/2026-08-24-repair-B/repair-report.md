# Repair-B 客户端报告

## 当前状态

- 总需求：3 个客户端平台修复、客户端测试和证据提交。
- 已完成：客户端源码契约修复、Android 主编译、Android 网络测试源码编译、证据生成和路径级暂存。
- 部分完成：Android 指定单元测试启动后因环境内存异常未完成。
- 阻塞：Web 构建缺少 Node；iOS 缺少完整 Xcode。
- Web 静态契约脚本已写入 `web-contract-test.mjs`，因同一 Node 环境缺失未执行。

## 实际修改

### Web

- `Code/frontend/web/src/shared/api/agent-stream.ts`：保留 SSE `id`，发送 `Last-Event-ID`，断线恢复时按 SSE id、事件 id、seq、tool call id 去重，终态后屏蔽迟到事件，并导出纯事件解析/接收保护函数。
- `Code/frontend/web/src/shared/api/client.ts`：草稿确认发送 `Idempotency-Key`，支持调用方复用幂等键。
- `Code/frontend/web/src/pages/agent/AgentPage.vue`：确认重试复用同一幂等键，确认前保持待确认展示和局部错误。

### Android

- `Code/frontend/android/core/network/src/main/java/com/zhihuiji/core/network/AgentSseClient.kt`：SSE `Last-Event-ID` 恢复、事件去重和终态保护。
- `Code/frontend/android/core/network/src/main/java/com/zhihuiji/core/network/ZhihuijiV2Api.kt`、`Code/frontend/android/data/agent/src/main/java/com/zhihuiji/data/agent/conversation/AgentV2Repository.kt`：确认接口贯穿幂等键请求。
- `Code/frontend/android/core/model/src/main/java/com/zhihuiji/core/model/v2/agent/conversation/AgentChatRequestResponse.kt`：补充非流式终态字段和确认请求模型。
- `Code/frontend/android/core/model/src/main/java/com/zhihuiji/core/model/v2/agent/stream/AgentRunTraceModels.kt`、`Code/frontend/android/core/model/src/main/java/com/zhihuiji/core/model/v2/agent/conversation/AgentChatModels.kt`、`Code/frontend/android/feature/agent/src/main/java/com/zhihuiji/feature/agent/conversation/AgentChatScreen.kt`：`CONFIRMATION_PENDING` 不再映射为完成。
- `Code/frontend/android/feature/agent/src/main/java/com/zhihuiji/feature/agent/conversation/AgentChatViewModel.kt`、`Code/frontend/android/feature/agent/src/main/java/com/zhihuiji/feature/agent/task/DraftListViewModel.kt`：确认重试复用幂等键。
- `Code/frontend/android/core/network/src/test/java/com/zhihuiji/core/network/AgentSseClientCancellationTest.kt`、`Code/frontend/android/core/model/src/test/java/com/zhihuiji/core/model/v2/agent/AgentRunTraceModelsTest.kt`、`Code/frontend/android/data/agent/src/test/java/com/zhihuiji/data/agent/conversation/AgentV2RepositoryTest.kt`：补充恢复、终态和幂等键测试覆盖。

### iOS

- `Code/frontend/ios/ZhihuijiIOS/Core/API/APIClient.swift`：SSE `Last-Event-ID` 恢复、事件去重、终态屏蔽和确认幂等头。
- `Code/frontend/ios/ZhihuijiIOS/Core/Models/AgentModels.swift`：统一 `summary_preview` 字段读取，保留旧 `summary` 兼容字段，补非流式终态字段和无障碍策略。
- `Code/frontend/ios/ZhihuijiIOS/Features/Agent/AgentViewModel.swift`：确认重试复用幂等键并统一摘要预览展示。
- `Code/frontend/ios/ZhihuijiIOS/Features/Agent/AgentChatView.swift`：接入 Dynamic Type、Increase Contrast、Reduce Motion 和 VoiceOver 状态。
- `Code/frontend/ios/ZhihuijiIOSTests/AgentTerminalStatusTests.swift`：补充摘要字段、幂等头和无障碍策略断言。

## 范围外对象

Agent A 的 `Code/backend/` 修改和已有 `docs/` 修改均保留，未暂存、未提交。

## 风险

- Android 指定单元测试尚无完整通过证据，当前结果为 `Failed`，原因是运行环境 `OutOfMemoryError`。
- Web 和 iOS 仅能记录 `Blocked`，没有真实浏览器、模拟器、登录或业务写入验证。
