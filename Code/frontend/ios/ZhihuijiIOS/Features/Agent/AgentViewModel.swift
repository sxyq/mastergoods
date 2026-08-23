import Foundation

/// 草稿二次确认的状态机。
/// - idle：未发起确认
/// - confirming：确认请求中。
/// - confirmed：后端已确认写入。
/// - rejected：用户主动拒绝（不调用正式业务接口）。
/// - failed：确认请求失败（网络、权限、草稿过期等）。
enum DraftConfirmationState: Equatable {
    case idle
    case confirming
    case confirmed
    case rejected
    case failed(String)

    var isTerminal: Bool {
        switch self {
        case .idle, .confirming: return false
        case .confirmed, .rejected, .failed: return true
        }
    }
}

@MainActor
final class AgentViewModel: ObservableObject {
    @Published var isLoading = false
    @Published var isSending = false
    @Published var isStopping = false
    @Published var isDraftSaving = false
    @Published var isConversationSaving = false
    @Published var draftQuestion = ""
    @Published var workbench: AgentWorkbench?
    @Published var conversations: [AgentConversationSummary] = []
    @Published var selectedConversationId: EntityID?
    @Published var messages: [AgentMessage] = []
    @Published var drafts: [AgentDraft] = []
    @Published var tasks: [AgentTask] = []
    @Published var notifications: [AgentNotification] = []
    @Published var errorMessage: String?
    @Published var latestResponse: AgentChatResponse?
    @Published var liveRun: AgentLiveRunPreview?
    @Published var contextCompactedNotice: String?
    @Published var auditDetail: AgentRunAudit?
    @Published var isAuditLoading = false
    @Published var isAuditPresented = false
    @Published var editingDraft: AgentDraft?
    @Published var draftEditorTitle = ""
    @Published var draftEditorContent = ""
    @Published var draftEditorStatus = AgentContractStatus.active
    @Published var terminalStatus: TerminalStatus?
    @Published var terminalMessage: String?
    @Published var pendingConfirmationDraft: AgentDraft?
    @Published var confirmationState: DraftConfirmationState = .idle
    @Published var isConfirmationPresented = false
    @Published var missingTargetTools: [String] = []
    @Published var completedTools: [String] = []

    private var streamTask: Task<Void, Never>?
    private var confirmingDraftId: EntityID?

    /// 页面离开（返回/关闭/进程回收）时取消本地 SSE 接收。
    /// 仅取消本地流，不触发正式业务写入；若需要取消服务端运行，
    /// 由 [stopStreaming] 显式调用服务端 cancel 接口。
    deinit {
        streamTask?.cancel()
    }

    func load(using client: APIClient) async {
        isLoading = true
        defer { isLoading = false }

        async let workbenchTask = capture { try await client.fetchAgentWorkbench() }
        async let conversationsTask = capture { try await client.fetchAgentConversations(limit: 20) }
        async let draftsTask = capture { try await client.fetchAgentDrafts(limit: 50) }
        async let tasksTask = capture { try await client.fetchAgentTasks() }
        async let notificationsTask = capture { try await client.fetchAgentNotifications(unreadOnly: false) }

        let workbenchResult = await workbenchTask
        let conversationsResult = await conversationsTask
        let draftsResult = await draftsTask
        let tasksResult = await tasksTask
        let notificationsResult = await notificationsTask

        var failures: [String] = []

        switch workbenchResult {
        case let .success(value):
            workbench = value
        case .failure:
            workbench = nil
            failures.append("工作台")
        }

        switch conversationsResult {
        case let .success(value):
            conversations = value
            if selectedConversationId == nil {
                selectedConversationId = value.first?.id
            } else if !value.contains(where: { $0.id == selectedConversationId }) {
                selectedConversationId = value.first?.id
            }
        case .failure:
            conversations = []
            selectedConversationId = nil
            messages = []
            failures.append("会话列表")
        }

        switch draftsResult {
        case let .success(value):
            drafts = value
        case .failure:
            drafts = []
            editingDraft = nil
            failures.append("草稿")
        }

        switch tasksResult {
        case let .success(value):
            tasks = value
        case .failure:
            tasks = []
            failures.append("任务")
        }

        switch notificationsResult {
        case let .success(value):
            notifications = value
        case .failure:
            notifications = []
            failures.append("通知")
        }

        if let selectedConversationId {
            await loadMessages(conversationId: selectedConversationId, client: client)
        }

        errorMessage = failures.isEmpty ? nil : "以下分区暂未成功拉取：\(failures.joined(separator: "、"))"
    }

    func selectConversation(_ conversationId: EntityID, client: APIClient) async {
        selectedConversationId = conversationId
        await loadMessages(conversationId: conversationId, client: client)
        await refreshDrafts(conversationId: conversationId, client: client)
    }

    func createConversation(using client: APIClient) async {
        guard !isConversationSaving else { return }
        isConversationSaving = true
        defer { isConversationSaving = false }

        do {
            let created = try await client.createAgentConversation(title: "新的经营问题", status: AgentContractStatus.active)
            await refreshConversationsIfNeeded(using: client)
            selectedConversationId = created.id
            messages = []
            await refreshDrafts(conversationId: created.id, client: client)
            errorMessage = nil
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    func deleteConversation(_ conversation: AgentConversationSummary, client: APIClient) async {
        guard !isConversationSaving else { return }
        isConversationSaving = true
        defer { isConversationSaving = false }

        do {
            try await client.deleteAgentConversation(id: conversation.id)
            conversations.removeAll { $0.id == conversation.id }
            messages = []
            drafts.removeAll { $0.conversationId == conversation.id }
            if selectedConversationId == conversation.id {
                selectedConversationId = conversations.first?.id
                if let next = selectedConversationId {
                    await loadMessages(conversationId: next, client: client)
                    await refreshDrafts(conversationId: next, client: client)
                }
            }
            await refreshWorkbenchIfNeeded(using: client)
            errorMessage = nil
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    func send(using client: APIClient) async {
        guard !isSending else { return }
        guard let question = draftQuestion.nilIfBlank else {
            errorMessage = "请输入问题"
            return
        }

        isSending = true
        errorMessage = nil
        contextCompactedNotice = nil
        appendLocalUserMessage(question)
        draftQuestion = ""

        do {
            let stream = try client.streamAgentChat(conversationId: selectedConversationId, message: question)
            streamTask?.cancel()
            streamTask = Task { [weak self] in
                guard let self else { return }
                do {
                    for try await event in stream {
                        self.consume(event)
                    }
                    await self.finishStreaming(using: client)
                } catch is CancellationError {
                    await self.finishStreaming(using: client)
                } catch {
                    self.errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
                    await self.finishStreaming(using: client)
                }
            }
        } catch {
            isSending = false
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    func stopStreaming(using client: APIClient) async {
        guard isSending, let runId = liveRun?.runId else { return }
        isStopping = true
        defer { isStopping = false }
        streamTask?.cancel()
        do {
            _ = try await client.cancelAgentRun(runId: runId)
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    func loadAudit(runId: String, client: APIClient) async {
        isAuditPresented = true
        isAuditLoading = true
        auditDetail = nil
        defer { isAuditLoading = false }
        do {
            auditDetail = try await client.fetchAgentRunAudit(runId: runId)
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    func markNotificationRead(_ notification: AgentNotification, client: APIClient) async {
        do {
            _ = try await client.markAgentNotificationRead(id: notification.id)
            if let index = notifications.firstIndex(where: { $0.id == notification.id }) {
                let current = notifications[index]
                notifications[index] = AgentNotification(
                    id: current.id,
                    taskId: current.taskId,
                    title: current.title,
                    body: current.body,
                    level: current.level,
                    isRead: true,
                    isDelivered: current.isDelivered,
                    createdAt: current.createdAt
                )
            }
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    func saveQuestionAsDraft(using client: APIClient) async {
        guard let question = draftQuestion.nilIfBlank else {
            errorMessage = "先输入问题，再存成草稿"
            return
        }
        guard !isDraftSaving else { return }

        isDraftSaving = true
        defer { isDraftSaving = false }

        do {
            let draft = try await client.createAgentDraft(
                payload: AgentDraftCreatePayload(
                    conversationId: selectedConversationId,
                    draftType: "question",
                    title: question.draftTitle,
                    contentJson: makeDraftContentJSON(question),
                    status: AgentContractStatus.active
                )
            )
            drafts.insert(draft, at: 0)
            await refreshWorkbenchIfNeeded(using: client)
            errorMessage = nil
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    func openDraft(_ pendingDraft: AgentPendingDraft, client: APIClient) async {
        if let local = drafts.first(where: { $0.id == pendingDraft.id }) {
            beginEditingDraft(local)
            return
        }
        do {
            let fetched = try await client.fetchAgentDrafts(limit: 50)
            drafts = fetched
            if let matched = fetched.first(where: { $0.id == pendingDraft.id }) {
                beginEditingDraft(matched)
            } else {
                errorMessage = "没有找到这条草稿详情"
            }
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    // MARK: - 草稿二次确认

    /// 当 SSE 推送 `draft_created` 事件或 `terminal_status = confirmationPending` 时，
    /// 由 ViewModel 决定是否需要展示二次确认弹窗。
    func requestConfirmation(for draft: AgentDraft) {
        guard !AgentDraftStatus.isTerminal(draft.status) else {
            // 已确认/已拒绝/已过期/已取消的草稿，不再二次确认。
            return
        }
        pendingConfirmationDraft = draft
        confirmationState = .idle
        isConfirmationPresented = true
        errorMessage = nil
    }

    /// 仅触发草稿确认接口。重复确认幂等：状态为 confirming/confirmed 时直接返回。
    func confirmPendingDraft(using client: APIClient) async {
        guard let draft = pendingConfirmationDraft else {
            return
        }
        // 幂等：已经在确认中或已确认，直接返回。
        switch confirmationState {
        case .confirming, .confirmed:
            return
        default:
            break
        }
        // 防止同一时间对同一草稿发起多次请求。
        if let confirmingDraftId = confirmingDraftId, confirmingDraftId == draft.id {
            return
        }
        confirmingDraftId = draft.id
        confirmationState = .confirming
        defer { confirmingDraftId = nil }

        do {
            let updated = try await client.confirmAgentDraft(id: draft.id)
            applyConfirmedDraft(updated)
            confirmationState = .confirmed
            // 确认成功后关闭弹窗、重新读取草稿、审计与业务结果。
            isConfirmationPresented = false
            await refreshAfterConfirmation(using: client)
        } catch {
            let message = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
            confirmationState = .failed(message)
            errorMessage = "草稿确认失败：\(message)"
        }
    }

    /// 用户主动拒绝，仅更新本地状态，不调用任何写入接口。
    func rejectPendingDraft() {
        confirmationState = .rejected
        isConfirmationPresented = false
        pendingConfirmationDraft = nil
    }

    /// 关闭确认弹窗：不触发写入。区分于确认。
    func dismissConfirmation() {
        isConfirmationPresented = false
        // 若仍处于 idle/confirming，关闭弹窗视为放弃确认，状态回到 idle。
        if case .idle = confirmationState {
            pendingConfirmationDraft = nil
        }
        if case .confirming = confirmationState {
            confirmationState = .idle
            pendingConfirmationDraft = nil
        }
    }

    private func applyConfirmedDraft(_ updated: AgentDraft) {
        if let index = drafts.firstIndex(where: { $0.id == updated.id }) {
            drafts[index] = updated
        } else {
            drafts.insert(updated, at: 0)
        }
        pendingConfirmationDraft = updated
    }

    private func refreshAfterConfirmation(using client: APIClient) async {
        async let workbenchRefresh: Void = refreshWorkbenchIfNeeded(using: client)
        async let draftsRefresh: Void = refreshDrafts(conversationId: selectedConversationId, client: client)
        async let tasksRefresh: Void = refreshTasksIfNeeded(using: client)
        async let notificationsRefresh: Void = refreshNotificationsIfNeeded(using: client)
        _ = await (workbenchRefresh, draftsRefresh, tasksRefresh, notificationsRefresh)
        // 若有最近 run，重新拉取审计。
        if let runId = liveRun?.runId {
            _ = try? await client.fetchAgentRunAudit(runId: runId)
        }
    }

    func beginEditingDraft(_ draft: AgentDraft) {
        editingDraft = draft
        draftEditorTitle = draft.title
        draftEditorContent = decodeDraftContent(draft.contentJson)
        draftEditorStatus = draft.status ?? AgentContractStatus.active
        errorMessage = nil
    }

    func applyDraftToComposer() {
        guard editingDraft != nil else { return }
        draftQuestion = draftEditorContent.nilIfBlank ?? draftEditorTitle
        editingDraft = nil
    }

    func saveEditingDraft(using client: APIClient) async {
        guard let draft = editingDraft else { return }
        let title = draftEditorTitle.trimmingCharacters(in: .whitespacesAndNewlines)
        let content = draftEditorContent.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !title.isEmpty, !content.isEmpty else {
            errorMessage = "草稿标题和内容都不能为空"
            return
        }
        guard !isDraftSaving else { return }

        isDraftSaving = true
        defer { isDraftSaving = false }

        do {
            let updated = try await client.updateAgentDraft(
                id: draft.id,
                payload: AgentDraftUpdatePayload(
                    conversationId: draft.conversationId,
                    draftType: draft.draftType,
                    title: title,
                    contentJson: makeDraftContentJSON(content),
                    status: draftEditorStatus.nilIfBlank ?? AgentContractStatus.active
                )
            )
            if let index = drafts.firstIndex(where: { $0.id == updated.id }) {
                drafts[index] = updated
            } else {
                drafts.insert(updated, at: 0)
            }
            await refreshWorkbenchIfNeeded(using: client)
            editingDraft = nil
            errorMessage = nil
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    func deleteEditingDraft(using client: APIClient) async {
        guard let draft = editingDraft else { return }
        guard !isDraftSaving else { return }

        isDraftSaving = true
        defer { isDraftSaving = false }

        do {
            try await client.deleteAgentDraft(id: draft.id)
            drafts.removeAll { $0.id == draft.id }
            await refreshWorkbenchIfNeeded(using: client)
            editingDraft = nil
            errorMessage = nil
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    private func appendLocalUserMessage(_ question: String) {
        let message = AgentMessage(
            id: EntityID(rawValue: "local-user-\(UUID().uuidString)"),
            conversationId: selectedConversationId,
            role: "user",
            messageType: "text",
            content: question,
            structuredDataJson: nil,
            createdAt: Int64(Date().timeIntervalSince1970 * 1000)
        )
        messages.append(message)
    }

    private func loadMessages(conversationId: EntityID, client: APIClient) async {
        do {
            messages = try await client.fetchAgentMessages(conversationId: conversationId, limit: 80)
        } catch {
            messages = []
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    private func refreshDrafts(conversationId: EntityID?, client: APIClient) async {
        do {
            drafts = try await client.fetchAgentDrafts(conversationId: conversationId, limit: 50)
        } catch {
            drafts = []
            editingDraft = nil
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    private func refreshConversationsIfNeeded(using client: APIClient) async {
        if let refreshed = try? await client.fetchAgentConversations(limit: 20) {
            conversations = refreshed
        }
    }

    private func refreshWorkbenchIfNeeded(using client: APIClient) async {
        if let refreshed = try? await client.fetchAgentWorkbench() {
            workbench = refreshed
        }
    }

    private func refreshTasksIfNeeded(using client: APIClient) async {
        if let refreshed = try? await client.fetchAgentTasks() {
            tasks = refreshed
        }
    }

    private func refreshNotificationsIfNeeded(using client: APIClient) async {
        if let refreshed = try? await client.fetchAgentNotifications(unreadOnly: false) {
            notifications = refreshed
        }
    }

    private func finishStreaming(using client: APIClient) async {
        streamTask = nil
        isSending = false
        async let refreshConversations = refreshConversationsIfNeeded(using: client)
        async let refreshWorkbench = refreshWorkbenchIfNeeded(using: client)
        async let refreshTasks = refreshTasksIfNeeded(using: client)
        async let refreshNotifications = refreshNotificationsIfNeeded(using: client)
        if let selectedConversationId {
            async let refreshMessages = loadMessages(conversationId: selectedConversationId, client: client)
            _ = await (refreshMessages, refreshConversations, refreshWorkbench, refreshTasks, refreshNotifications)
        } else {
            _ = await (refreshConversations, refreshWorkbench, refreshTasks, refreshNotifications)
        }
    }

    func consume(_ event: AgentStreamEvent) {
        switch event.eventType {
        case "run_started":
            // 启动新一轮时清空上一轮终态。
            terminalStatus = nil
            terminalMessage = nil
            missingTargetTools = []
            liveRun = AgentLiveRunPreview(
                runId: event.runId ?? UUID().uuidString,
                conversationId: event.conversationId ?? selectedConversationId,
                answer: "",
                planSummary: nil,
                toolCalls: [],
                resultBlocks: [],
                mode: event.mode,
                llmStatus: event.llmStatus,
                planSource: event.planSource
            )
            if let conversationId = event.conversationId {
                selectedConversationId = conversationId
            }
        case "plan_delta":
            ensureLiveRun(from: event)
            liveRun?.planSummary = event.content ?? event.delta
            if let planSource = event.planSource {
                liveRun?.planSource = planSource
            }
        case "tool_started":
            ensureLiveRun(from: event)
            let callId = event.toolCallId ?? [event.runId.orEmpty, event.toolName.orEmpty].joined(separator: ":")
            upsertToolCall(
                AgentToolCall(
                    toolCallId: callId,
                    toolName: event.toolName ?? "tool",
                    status: "running",
                    inputSummary: event.inputSummary,
                    returnedCount: nil,
                    totalCount: nil,
                    limit: nil,
                    isTruncated: nil,
                    durationMs: nil,
                    resultSummary: nil,
                    errorCode: nil,
                    errorMessage: nil
                )
            )
        case "tool_progress":
            ensureLiveRun(from: event)
            let callId = event.toolCallId ?? [event.runId.orEmpty, event.toolName.orEmpty].joined(separator: ":")
            upsertToolCall(
                AgentToolCall(
                    toolCallId: callId,
                    toolName: event.toolName ?? "tool",
                    status: "running",
                    inputSummary: currentToolCall(id: callId)?.inputSummary ?? event.inputSummary,
                    returnedCount: nil,
                    totalCount: nil,
                    limit: nil,
                    isTruncated: nil,
                    durationMs: nil,
                    resultSummary: event.message ?? event.resultSummary,
                    errorCode: nil,
                    errorMessage: nil
                )
            )
        case "tool_completed":
            ensureLiveRun(from: event)
            let callId = event.toolCallId ?? [event.runId.orEmpty, event.toolName.orEmpty].joined(separator: ":")
            upsertToolCall(
                AgentToolCall(
                    toolCallId: callId,
                    toolName: event.toolName ?? "tool",
                    status: "completed",
                    inputSummary: currentToolCall(id: callId)?.inputSummary ?? event.inputSummary,
                    returnedCount: nil,
                    totalCount: nil,
                    limit: nil,
                    isTruncated: nil,
                    durationMs: nil,
                    resultSummary: event.resultSummary,
                    errorCode: nil,
                    errorMessage: nil
                )
            )
            if let completedTools = event.completedTools, !completedTools.isEmpty {
                self.completedTools = completedTools
            }
        case "tool_failed":
            ensureLiveRun(from: event)
            let callId = event.toolCallId ?? [event.runId.orEmpty, event.toolName.orEmpty].joined(separator: ":")
            upsertToolCall(
                AgentToolCall(
                    toolCallId: callId,
                    toolName: event.toolName ?? "tool",
                    status: "failed",
                    inputSummary: currentToolCall(id: callId)?.inputSummary ?? event.inputSummary,
                    returnedCount: nil,
                    totalCount: nil,
                    limit: nil,
                    isTruncated: nil,
                    durationMs: nil,
                    resultSummary: event.errorSummary ?? event.safeMessage,
                    errorCode: event.errorCode,
                    errorMessage: event.safeMessage ?? event.errorSummary
                )
            )
        case "answer_delta":
            ensureLiveRun(from: event)
            liveRun?.answer += event.delta ?? event.content ?? ""
        case "answer_completed":
            ensureLiveRun(from: event)
            if let answer = event.answer?.nilIfBlank {
                liveRun?.answer = answer
            }
            liveRun?.mode = event.mode ?? liveRun?.mode
            liveRun?.llmStatus = event.llmStatus ?? liveRun?.llmStatus
            liveRun?.planSource = event.planSource ?? liveRun?.planSource
        case "result_block":
            ensureLiveRun(from: event)
            if let block = event.block, !(liveRun?.resultBlocks.contains(block) ?? false) {
                liveRun?.resultBlocks.append(block)
            }
        case "draft_created":
            ensureLiveRun(from: event)
            if let draftBlock = makeDraftCreatedBlock(from: event),
               !(liveRun?.resultBlocks.contains(draftBlock) ?? false) {
                liveRun?.resultBlocks.append(draftBlock)
            }
            // 当后端下发 status 时，将其映射为待确认草稿，触发二次确认弹窗。
            if let status = event.status?.nilIfBlank,
               AgentDraftStatus.requiresConfirmation(status),
               let draftId = event.draftId {
                let draft = AgentDraft(
                    id: draftId,
                    conversationId: event.conversationId ?? selectedConversationId,
                    draftType: event.draftType ?? "draft",
                    title: event.title ?? "AI 草稿",
                    contentJson: "",
                    status: status,
                    createdAt: event.timestamp ?? Int64(Date().timeIntervalSince1970 * 1000),
                    updatedAt: event.timestamp ?? Int64(Date().timeIntervalSince1970 * 1000)
                )
                requestConfirmation(for: draft)
            }
        case "context_compacted":
            ensureLiveRun(from: event)
            let countText = event.compactedCount.map { "\($0) 条" } ?? "部分"
            contextCompactedNotice = "已压缩 \(countText) 上下文。\(event.summary?.nilIfBlank ?? "旧消息已整理为摘要，以保持连续追问稳定。")"
        case "run_completed":
            ensureLiveRun(from: event)
            if let finalAnswer = event.finalAnswer, !finalAnswer.isEmpty {
                liveRun?.answer = finalAnswer
            }
            liveRun?.mode = event.mode ?? liveRun?.mode
            liveRun?.llmStatus = event.llmStatus ?? liveRun?.llmStatus
            liveRun?.planSource = event.planSource ?? liveRun?.planSource
            if let liveRun {
                latestResponse = makeLatestResponse(from: liveRun)
                selectedConversationId = liveRun.conversationId ?? selectedConversationId
            }
            // 终态事件统一字段：terminal_status。COMPLETED 或 CONFIRMATION_PENDING 都走这里。
            applyTerminalStatus(event)
        case "run_failed":
            ensureLiveRun(from: event)
            liveRun?.llmStatus = "failed"
            applyTerminalStatus(event)
            let message = event.safeMessage ?? event.errorSummary ?? "AI 助手运行失败"
            errorMessage = message
            terminalMessage = message
        case "run_blocked":
            ensureLiveRun(from: event)
            liveRun?.llmStatus = "blocked"
            applyTerminalStatus(event)
            let message = event.safeMessage ?? "运行被阻止"
            errorMessage = message
            terminalMessage = message
        case "run_exhausted":
            ensureLiveRun(from: event)
            liveRun?.llmStatus = "exhausted"
            applyTerminalStatus(event)
            let message = event.safeMessage ?? "本轮轮次已用完，请稍后再试"
            errorMessage = message
            terminalMessage = message
        case "run_cancelled":
            ensureLiveRun(from: event)
            liveRun?.llmStatus = "cancelled"
            applyTerminalStatus(event)
        case "error", "safety_check_blocked":
            errorMessage = event.safeMessage ?? event.errorSummary ?? "AI 助手运行失败"
        default:
            // 兼容未识别的终态事件：若携带 terminal_status，仍尝试解析。
            if let status = event.terminalStatus {
                terminalStatus = status
                terminalMessage = event.safeMessage
            }
            break
        }
    }

    private func applyTerminalStatus(_ event: AgentStreamEvent) {
        if let status = event.terminalStatus {
            terminalStatus = status
        } else {
            // 兼容老版本后端：根据 event_type 推断。
            switch event.eventType {
            case "run_completed":
                terminalStatus = .completed
            case "run_failed":
                terminalStatus = .failed
            case "run_blocked":
                terminalStatus = .blocked
            case "run_cancelled":
                terminalStatus = .cancelled
            case "run_exhausted":
                terminalStatus = .exhausted
            default:
                break
            }
        }
        if let message = event.safeMessage, !message.isEmpty {
            terminalMessage = message
        }
        if let completed = event.completedTools {
            completedTools = completed
        }
        if let missing = event.missingTargetTools, !missing.isEmpty {
            missingTargetTools = missing
        }
    }

    private func makeDraftCreatedBlock(from event: AgentStreamEvent) -> AgentResultBlock? {
        guard let draftId = event.draftId else { return nil }
        let draftType = event.draftType?.nilIfBlank ?? "draft"
        let title = event.title?.nilIfBlank ?? "AI 草稿"
        let status = event.status?.nilIfBlank ?? AgentDraftStatus.pendingConfirmation
        return AgentResultBlock(
            blockType: "draft_card",
            title: title,
            data: .object([
                "draft_id": .string(draftId.rawValue),
                "draft_type": .string(draftType),
                "title": .string(title),
                "summary": .string("草稿已生成，请在草稿管理中确认后再写入正式业务。"),
                "status": .string(status),
                "action_label": .string("二次确认"),
            ])
        )
    }

    private func makeLatestResponse(from liveRun: AgentLiveRunPreview) -> AgentChatResponse {
        AgentChatResponse(
            runId: liveRun.runId,
            conversationId: liveRun.conversationId ?? selectedConversationId ?? EntityID(rawValue: "pending"),
            answer: liveRun.answer,
            blocks: liveRun.resultBlocks,
            draftId: nil,
            safetyPassed: nil,
            safetyReason: nil,
            mode: liveRun.mode,
            llmStatus: liveRun.llmStatus,
            planSource: liveRun.planSource,
            planSummary: liveRun.planSummary,
            toolCalls: liveRun.toolCalls,
            evidenceRefs: [],
            resultBlocks: liveRun.resultBlocks,
            performanceSummary: nil,
            auditId: nil,
            traceId: nil,
            observability: nil
        )
    }

    private func ensureLiveRun(from event: AgentStreamEvent) {
        guard liveRun == nil else { return }
        liveRun = AgentLiveRunPreview(
            runId: event.runId ?? UUID().uuidString,
            conversationId: event.conversationId ?? selectedConversationId,
            answer: "",
            planSummary: nil,
            toolCalls: [],
            resultBlocks: [],
            mode: event.mode,
            llmStatus: event.llmStatus,
            planSource: event.planSource
        )
    }

    private func currentToolCall(id: String) -> AgentToolCall? {
        liveRun?.toolCalls.first(where: { $0.toolCallId == id })
    }

    private func upsertToolCall(_ toolCall: AgentToolCall) {
        guard liveRun != nil else { return }
        if let index = liveRun?.toolCalls.firstIndex(where: { $0.toolCallId == toolCall.toolCallId }) {
            liveRun?.toolCalls[index] = toolCall
        } else {
            liveRun?.toolCalls.append(toolCall)
        }
    }

    private func capture<T>(_ operation: @escaping () async throws -> T) async -> Result<T, Error> {
        do {
            return .success(try await operation())
        } catch {
            return .failure(error)
        }
    }

    private func makeDraftContentJSON(_ question: String) -> String {
        let payload: [String: String] = ["question": question]
        if let data = try? JSONSerialization.data(withJSONObject: payload, options: []),
           let text = String(data: data, encoding: .utf8) {
            return text
        }
        return question
    }

    private func decodeDraftContent(_ contentJson: String) -> String {
        guard let data = contentJson.data(using: .utf8),
              let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
        else {
            return contentJson
        }
        if let question = object["question"] as? String, !question.isEmpty {
            return question
        }
        if let content = object["content"] as? String, !content.isEmpty {
            return content
        }
        if let title = object["title"] as? String, !title.isEmpty {
            return title
        }
        return contentJson
    }
}

private extension Optional where Wrapped == String {
    var orEmpty: String { self ?? "" }
}

private extension String {
    var draftTitle: String {
        let trimmed = trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return "未命名草稿" }
        return String(trimmed.prefix(18))
    }
}
