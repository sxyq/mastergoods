import Foundation

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
    @Published var auditDetail: AgentRunAudit?
    @Published var isAuditLoading = false
    @Published var isAuditPresented = false
    @Published var editingDraft: AgentDraft?
    @Published var draftEditorTitle = ""
    @Published var draftEditorContent = ""
    @Published var draftEditorStatus = AgentContractStatus.active

    private var streamTask: Task<Void, Never>?

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

    private func consume(_ event: AgentStreamEvent) {
        switch event.eventType {
        case "run_started":
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
        case "result_block":
            ensureLiveRun(from: event)
            if let block = event.block, !(liveRun?.resultBlocks.contains(block) ?? false) {
                liveRun?.resultBlocks.append(block)
            }
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
        case "run_cancelled":
            ensureLiveRun(from: event)
            liveRun?.llmStatus = "cancelled"
        case "error", "safety_check_blocked":
            errorMessage = event.safeMessage ?? event.errorSummary ?? "AI 助手运行失败"
        default:
            break
        }
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
