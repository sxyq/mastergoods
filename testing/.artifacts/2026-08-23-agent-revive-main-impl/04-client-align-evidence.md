# P1 客户端三端对齐证据 — 2026-08-23

- 代理：client-align（子代理编码）+ team-lead（主控验证/提交）
- 范围：Web / Android / iOS 草稿二次授权、SSE 终态、无障碍对齐

## 修改文件
1. Code/frontend/web/src/pages/agent/AgentPage.vue（+15）：handleStreamEvent 入口新增终态幂等守卫——一旦消息已有 runTrace.terminal，后续重复/乱序/迟到事件直接 return，不再覆盖终态、不再把 isStreaming 置回。导入 isTerminalEvent。
2. Code/frontend/ios/ZhihuijiIOS/Features/Agent/AgentViewModel.swift（+7）：新增 deinit 取消本地 streamTask，对齐 Web onBeforeUnmount / Android onCleared 的"返回/关闭/进程回收不产生写入"语义。
3. Android：审查后 8 项要求均已满足，无改动。

## 验证（主控独立复核）
- Web `cd Code/frontend/web && npm run build`：vue-tsc -b && vite build，exit 0，116 modules transformed，built in 1.78s。**真实通过**。
- Web 改动 diff 复核：终态守卫逻辑正确（终态与非终态事件均检查 target.runTrace.terminal）。
- iOS 改动 diff 复核：deinit 仅 cancel 本地流，不触发写入；遵循现有 streamTask 模式。
- Android `./gradlew :app:compileDebugKotlin` + 纯单元测试：子代理报告 BUILD SUCCESSFUL（10 executed 147 up-to-date）；Android 无 diff，主控未重复跑（无改动）。

## 各端要求落实（子代理报告，主控抽查关键项）
- Web：页面离开停 SSE+cancel(1325-1331)、fetchSidePanel allSettled+重试(275-300)、loadDrafts 错误+重试(832-840)、草稿按钮 canWrite(2268-2285)、空值校验(866-882)、AbortError 不覆盖取消(452-458)、**本次新增终态守卫(506-520)**、draft_created→CONFIRMATION_PENDING(539-543)、四终态 showSuccess:false(1381-1404)、审计/轨迹/compacted 展示。
- Android：idempotency_key 序列化(OrderV2Models.kt:261-271 + 测试)、inventory List/Page 差异(repo .map content)、409/422 错误模型(SafeApiCall.kt HttpErrorKind)、safeApiCall/safeApiUnitCall data=null、AgentSseClient 终态/取消/断线/重复/迟到、owner/store 服务端会话、草稿二次确认全流程(confirmDraft→confirmAgentDraftV2 非正式接口)、纯 Kotlin 测试。
- iOS：草稿生成→CONFIRMATION_PENDING→覆盖式确认弹窗→独立草稿确认 API→正式写入；确认前不显示完成；四终态不显示成功；拒绝/关闭/返回/中断不写入；重复确认幂等；VoiceOver/Dynamic Type/安全区/深色模式/Increase Contrast/Reduce Motion；**本次新增 deinit**。

## Blocked / Deferred
- Web 浏览器真实登录联调：Deferred（需 dev server + 真实后端）。
- Android 真机/模拟器/adb/APK UI 验证：Deferred（仅 compileDebugKotlin + 纯单元测试）。
- iOS 真机/模拟器 UI 验证 + xcodebuild/test：**Blocked**（本机仅 Command Line Tools，无完整 Xcode，xcodebuild 报 "requires Xcode"；未伪造通过）。
- 真实跨 owner/store、同店多成员并发：Blocked（需真实多账号环境）。
