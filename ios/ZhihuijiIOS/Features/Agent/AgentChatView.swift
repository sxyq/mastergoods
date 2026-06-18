import SwiftUI

struct AgentChatView: View {
    @EnvironmentObject private var session: AppSession
    @Environment(\.appEnvironment) private var env
    @StateObject private var viewModel = AgentViewModel()

    private var currentRun: AgentLiveRunPreview? {
        viewModel.liveRun ?? viewModel.latestResponse?.livePreview
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                headerCard
                workbenchSection
                pendingDraftSection
                conversationsSection
                messagesSection
                runSection
                taskSection
                notificationSection
                composerSection
            }
            .padding(20)
        }
        .navigationTitle("AI 助手")
        .task {
            await viewModel.load(using: env.apiClient)
        }
        .sheet(isPresented: $viewModel.isAuditPresented) {
            NavigationStack {
                auditSheetContent
                    .navigationTitle("运行审计")
                    .toolbar {
                        ToolbarItem {
                            Button("关闭") {
                                viewModel.isAuditPresented = false
                            }
                        }
                    }
            }
            .presentationDetents([.medium, .large])
            .presentationDragIndicator(.visible)
        }
        .sheet(item: $viewModel.editingDraft) { draft in
            NavigationStack {
                AgentDraftSheet(
                    draft: draft,
                    title: $viewModel.draftEditorTitle,
                    content: $viewModel.draftEditorContent,
                    status: $viewModel.draftEditorStatus,
                    isSaving: viewModel.isDraftSaving,
                    onUse: {
                        viewModel.applyDraftToComposer()
                    },
                    onSave: {
                        Task { await viewModel.saveEditingDraft(using: env.apiClient) }
                    },
                    onDelete: {
                        Task { await viewModel.deleteEditingDraft(using: env.apiClient) }
                    }
                )
                .navigationTitle("编辑草稿")
                .toolbar {
                    ToolbarItem {
                        Button("关闭") {
                            viewModel.editingDraft = nil
                        }
                    }
                }
            }
            .presentationDetents([.medium, .large])
            .presentationDragIndicator(.visible)
        }
    }

    private var headerCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("AI 助手")
                        .font(.system(size: 28, weight: .bold))
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                    Text("延续安卓移动端的经营助手体验：先看经营提示，再进入会话，再做连续追问。")
                        .font(.system(size: 14))
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                }
                Spacer()
                StatusChip(
                    title: session.hasPermission(.agentWrite) ? "可提问" : "只读",
                    tint: session.hasPermission(.agentWrite) ? ZhihuijiTheme.ColorToken.primary : ZhihuijiTheme.ColorToken.warning
                )
            }

            if let errorMessage = viewModel.errorMessage {
                Text(errorMessage)
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(ZhihuijiTheme.ColorToken.danger)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 10)
                    .background(ZhihuijiTheme.ColorToken.danger.opacity(0.08), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
            }
        }
        .padding(16)
        .glassCard()
    }

    @ViewBuilder
    private var workbenchSection: some View {
        if let workbench = viewModel.workbench {
            VStack(alignment: .leading, spacing: 14) {
                Text(workbench.greeting)
                    .font(.system(size: 19, weight: .semibold))
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)

                if let summary = workbench.todaySummary, !summary.isEmpty {
                    Text(summary)
                        .font(.system(size: 13))
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                }

                if !workbench.kpiCards.isEmpty {
                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                        ForEach(workbench.kpiCards) { card in
                            MetricCard(
                                title: card.label,
                                value: card.value,
                                subtitle: card.trendValue ?? "实时指标",
                                tint: tintForTrend(card.trendDirection)
                            )
                        }
                    }
                }

                if !workbench.quickQuestions.isEmpty {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 10) {
                            ForEach(workbench.quickQuestions, id: \.self) { question in
                                Button {
                                    viewModel.draftQuestion = question
                                } label: {
                                    Text(question)
                                        .font(.system(size: 13, weight: .medium))
                                        .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                                        .padding(.horizontal, 14)
                                        .padding(.vertical, 10)
                                        .background(Color.white.opacity(0.48), in: Capsule())
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                }

                if !workbench.riskAlerts.isEmpty {
                    VStack(spacing: 10) {
                        ForEach(workbench.riskAlerts) { alert in
                            HStack(alignment: .top, spacing: 12) {
                                Circle()
                                    .fill(riskTint(alert.level).opacity(0.16))
                                    .frame(width: 30, height: 30)
                                    .overlay(
                                        Image(systemName: "exclamationmark.triangle.fill")
                                            .font(.system(size: 12))
                                            .foregroundStyle(riskTint(alert.level))
                                    )
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(alert.title)
                                        .font(.system(size: 14, weight: .semibold))
                                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                                    Text(alert.description)
                                        .font(.system(size: 12))
                                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                                }
                                Spacer()
                            }
                            .padding(12)
                            .glassCard(cornerRadius: 12)
                        }
                    }
                }
            }
            .padding(16)
            .glassCard()
        }
    }

    @ViewBuilder
    private var pendingDraftSection: some View {
        if let drafts = viewModel.workbench?.pendingDrafts, !drafts.isEmpty {
            VStack(alignment: .leading, spacing: 12) {
                Text("待处理草稿")
                    .font(.system(size: 18, weight: .semibold))
                ForEach(drafts.prefix(4)) { draft in
                    Button {
                        Task { await viewModel.openDraft(draft, client: env.apiClient) }
                    } label: {
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(draft.title)
                                    .font(.system(size: 14, weight: .semibold))
                                Text(draft.draftType)
                                    .font(.system(size: 12))
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                            }
                            Spacer()
                            VStack(alignment: .trailing, spacing: 4) {
                                Text(draft.createdAt.dateTimeText)
                                    .font(.system(size: 11))
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                                Image(systemName: "chevron.right")
                                    .font(.system(size: 11, weight: .semibold))
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                            }
                        }
                        .padding(14)
                        .glassCard(cornerRadius: 12)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private var conversationsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("最近会话")
                    .font(.system(size: 18, weight: .semibold))
                Spacer()
                if session.hasPermission(.agentWrite) {
                    Button {
                        Task { await viewModel.createConversation(using: env.apiClient) }
                    } label: {
                        HStack(spacing: 6) {
                            Image(systemName: "plus")
                            Text(viewModel.isConversationSaving ? "处理中..." : "新会话")
                                .font(.system(size: 12, weight: .semibold))
                        }
                        .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(Color.white.opacity(0.52), in: Capsule())
                    }
                    .buttonStyle(.plain)
                    .disabled(viewModel.isConversationSaving)
                }
                if viewModel.isLoading {
                    ProgressView()
                        .scaleEffect(0.8)
                }
            }

            if viewModel.conversations.isEmpty {
                EmptyStateView(title: "暂无会话", message: "直接发一个问题，系统会自动沉淀成一段新会话。")
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 10) {
                        ForEach(viewModel.conversations) { conversation in
                            Button {
                                Task { await viewModel.selectConversation(conversation.id, client: env.apiClient) }
                            } label: {
                                VStack(alignment: .leading, spacing: 6) {
                                    HStack(alignment: .top, spacing: 8) {
                                        Text(conversation.title)
                                            .font(.system(size: 14, weight: .semibold))
                                            .lineLimit(1)
                                        Spacer(minLength: 4)
                                        if session.hasPermission(.agentWrite), viewModel.selectedConversationId == conversation.id {
                                            Image(systemName: "trash")
                                                .font(.system(size: 11, weight: .semibold))
                                                .foregroundStyle(Color.white.opacity(0.92))
                                                .padding(6)
                                                .background(Color.white.opacity(0.18), in: Circle())
                                        }
                                    }
                                    Text(conversation.latestSummary?.nilIfBlank ?? "进入后可查看完整消息")
                                        .font(.system(size: 12))
                                        .lineLimit(2)
                                    Text((conversation.lastMessageAt ?? conversation.updatedAt).dateTimeText)
                                        .font(.system(size: 11))
                                        .foregroundStyle(viewModel.selectedConversationId == conversation.id ? Color.white.opacity(0.82) : ZhihuijiTheme.ColorToken.textTertiary)
                                }
                                .foregroundStyle(viewModel.selectedConversationId == conversation.id ? .white : ZhihuijiTheme.ColorToken.textPrimary)
                                .padding(.horizontal, 14)
                                .padding(.vertical, 12)
                                .frame(width: 204, alignment: .leading)
                                .background(
                                    (viewModel.selectedConversationId == conversation.id
                                        ? LinearGradient(colors: [ZhihuijiTheme.ColorToken.primaryBright, ZhihuijiTheme.ColorToken.primary], startPoint: .leading, endPoint: .trailing)
                                        : LinearGradient(colors: [Color.white.opacity(0.58), Color.white.opacity(0.58)], startPoint: .leading, endPoint: .trailing)
                                    ),
                                    in: RoundedRectangle(cornerRadius: 14, style: .continuous)
                                )
                            }
                            .buttonStyle(.plain)
                            .contextMenu {
                                if session.hasPermission(.agentWrite) {
                                    Button(role: .destructive) {
                                        Task { await viewModel.deleteConversation(conversation, client: env.apiClient) }
                                    } label: {
                                        Label("删除会话", systemImage: "trash")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private var messagesSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("对话记录")
                .font(.system(size: 18, weight: .semibold))

            if viewModel.messages.isEmpty, currentRun == nil {
                EmptyStateView(title: "还没有消息", message: "先发起一个问题，助手会把推理过程和结果沉淀下来。")
            } else {
                VStack(spacing: 12) {
                    ForEach(viewModel.messages) { message in
                        HStack(alignment: .bottom) {
                            if message.isAssistant {
                                assistantBubble(
                                    content: message.content,
                                    timestamp: message.createdAt.dateTimeText,
                                    structuredData: nil,
                                    isLive: false
                                )
                                Spacer(minLength: 32)
                            } else {
                                Spacer(minLength: 32)
                                userBubble(message)
                            }
                        }
                    }

                    if let currentRun {
                        HStack(alignment: .bottom) {
                            assistantBubble(
                                content: currentRun.answer.nilIfBlank ?? "正在整理答案...",
                                timestamp: viewModel.isSending ? (viewModel.isStopping ? "停止中..." : "生成中...") : "刚刚",
                                structuredData: currentRun.resultBlocks,
                                isLive: viewModel.isSending
                            )
                            Spacer(minLength: 32)
                        }
                    }
                }
            }
        }
    }

    @ViewBuilder
    private var runSection: some View {
        if let currentRun {
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Text("本次运行轨迹")
                        .font(.system(size: 18, weight: .semibold))
                    Spacer()
                    if let llmStatus = currentRun.llmStatus?.nilIfBlank {
                        StatusChip(title: llmStatus, tint: tintForStatus(llmStatus))
                    }
                }

                if let planSummary = currentRun.planSummary?.nilIfBlank {
                    VStack(alignment: .leading, spacing: 6) {
                        Text("执行计划")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                        Text(planSummary)
                            .font(.system(size: 13))
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                    }
                    .padding(14)
                    .glassCard(cornerRadius: 12)
                }

                if !currentRun.toolCalls.isEmpty {
                    VStack(alignment: .leading, spacing: 10) {
                        Text("工具轨迹")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                        ForEach(currentRun.toolCalls) { tool in
                            HStack(alignment: .top, spacing: 12) {
                                Circle()
                                    .fill(tintForStatus(tool.status).opacity(0.18))
                                    .frame(width: 28, height: 28)
                                    .overlay(
                                        Image(systemName: iconForToolStatus(tool.status))
                                            .font(.system(size: 12, weight: .semibold))
                                            .foregroundStyle(tintForStatus(tool.status))
                                    )
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(tool.toolName)
                                        .font(.system(size: 14, weight: .semibold))
                                    if let inputSummary = tool.inputSummary?.nilIfBlank {
                                        Text(inputSummary)
                                            .font(.system(size: 12))
                                            .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                                    }
                                    if let resultSummary = tool.resultSummary?.nilIfBlank {
                                        Text(resultSummary)
                                            .font(.system(size: 12, weight: .medium))
                                            .foregroundStyle(tintForStatus(tool.status))
                                    }
                                }
                                Spacer()
                                Text(tool.status ?? "pending")
                                    .font(.system(size: 12, weight: .semibold))
                                    .foregroundStyle(tintForStatus(tool.status))
                            }
                            .padding(12)
                            .glassCard(cornerRadius: 12)
                        }
                    }
                }

                if !currentRun.resultBlocks.isEmpty {
                    VStack(alignment: .leading, spacing: 10) {
                        Text("结构化结果")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                        ForEach(currentRun.resultBlocks) { block in
                            resultBlockView(block)
                        }
                    }
                }

                Button {
                    Task { await viewModel.loadAudit(runId: currentRun.runId, client: env.apiClient) }
                } label: {
                    HStack {
                        Image(systemName: "doc.text.magnifyingglass")
                        Text("查看完整审计")
                            .font(.system(size: 14, weight: .semibold))
                    }
                    .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(Color.white.opacity(0.54), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                }
                .buttonStyle(.plain)
            }
            .padding(16)
            .glassCard()
        }
    }

    private var taskSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("近期任务")
                .font(.system(size: 18, weight: .semibold))
            if viewModel.tasks.isEmpty {
                EmptyStateView(title: "暂无任务", message: "当前没有挂起中的 AI 任务。")
            } else {
                ForEach(viewModel.tasks.prefix(4)) { task in
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(task.title)
                                .font(.system(size: 14, weight: .semibold))
                            Text(task.statusLabel ?? task.status ?? task.taskType)
                                .font(.system(size: 12))
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                        }
                        Spacer()
                        if let progress = task.progress {
                            Text("\(progress)%")
                                .font(.system(size: 12, weight: .semibold))
                                .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                        }
                    }
                    .padding(14)
                    .glassCard(cornerRadius: 12)
                }
            }
        }
    }

    private var notificationSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("通知")
                .font(.system(size: 18, weight: .semibold))
            if viewModel.notifications.isEmpty {
                EmptyStateView(title: "暂无通知", message: "这里会展示 AI 风险提醒和执行结果。")
            } else {
                ForEach(viewModel.notifications.prefix(5)) { notification in
                    Button {
                        Task { await viewModel.markNotificationRead(notification, client: env.apiClient) }
                    } label: {
                        HStack(alignment: .top, spacing: 12) {
                            Circle()
                                .fill(notificationTint(notification).opacity(0.16))
                                .frame(width: 30, height: 30)
                                .overlay(
                                    Image(systemName: notification.isRead == true ? "bell.badge.slash.fill" : "bell.fill")
                                        .font(.system(size: 12))
                                        .foregroundStyle(notificationTint(notification))
                                )
                            VStack(alignment: .leading, spacing: 4) {
                                Text(notification.title)
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                                Text(notification.body)
                                    .font(.system(size: 12))
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                                Text(notification.createdAt.dateTimeText)
                                    .font(.system(size: 11))
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                            }
                            Spacer()
                        }
                        .padding(14)
                        .glassCard(cornerRadius: 12)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private var composerSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("发起提问")
                .font(.system(size: 18, weight: .semibold))

            if session.hasPermission(.agentWrite) {
                TextField("问点什么，比如今天哪些客户欠款最高？", text: $viewModel.draftQuestion, axis: .vertical)
                    .lineLimit(3 ... 6)
                    .agentComposerBehavior()
                    .fieldBackground()
                    .onSubmit {
                        Task { await viewModel.send(using: env.apiClient) }
                    }

                HStack(spacing: 10) {
                    PrimaryGlassButton(
                        title: viewModel.isSending ? "生成中..." : "发送",
                        systemImage: "paperplane.fill",
                        disabled: viewModel.isSending || viewModel.draftQuestion.nilIfBlank == nil
                    ) {
                        Task { await viewModel.send(using: env.apiClient) }
                    }

                    Button {
                        Task { await viewModel.saveQuestionAsDraft(using: env.apiClient) }
                    } label: {
                        HStack(spacing: 8) {
                            Image(systemName: "square.and.arrow.down")
                            Text(viewModel.isDraftSaving ? "保存中..." : "存草稿")
                                .font(.system(size: 15, weight: .semibold))
                        }
                        .foregroundStyle(viewModel.draftQuestion.nilIfBlank == nil ? ZhihuijiTheme.ColorToken.textTertiary : ZhihuijiTheme.ColorToken.primary)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(Color.white.opacity(0.54), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.pill, style: .continuous))
                        .overlay(
                            RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.pill, style: .continuous)
                                .stroke(Color.white.opacity(0.45), lineWidth: 0.8)
                        )
                    }
                    .buttonStyle(.plain)
                    .disabled(viewModel.isDraftSaving || viewModel.draftQuestion.nilIfBlank == nil)
                    .opacity((viewModel.isDraftSaving || viewModel.draftQuestion.nilIfBlank == nil) ? 0.55 : 1)

                    Button {
                        Task { await viewModel.stopStreaming(using: env.apiClient) }
                    } label: {
                        HStack(spacing: 8) {
                            Image(systemName: "stop.fill")
                            Text(viewModel.isStopping ? "停止中..." : "停止")
                                .font(.system(size: 15, weight: .semibold))
                        }
                        .foregroundStyle(viewModel.isSending ? ZhihuijiTheme.ColorToken.danger : ZhihuijiTheme.ColorToken.textTertiary)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(Color.white.opacity(0.54), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.pill, style: .continuous))
                        .overlay(
                            RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.pill, style: .continuous)
                                .stroke(viewModel.isSending ? ZhihuijiTheme.ColorToken.danger.opacity(0.25) : Color.white.opacity(0.45), lineWidth: 0.8)
                        )
                    }
                    .buttonStyle(.plain)
                    .disabled(!viewModel.isSending || viewModel.isStopping)
                    .opacity((!viewModel.isSending || viewModel.isStopping) ? 0.55 : 1)
                }
            } else {
                EmptyStateView(title: "当前账号仅可查看", message: "你可以浏览会话、通知和结构化结果，但没有提问权限。")
            }
        }
        .padding(16)
        .glassCard()
    }

    @ViewBuilder
    private var auditSheetContent: some View {
        if viewModel.isAuditLoading {
            LoadingStateView(message: "正在拉取运行审计...")
                .padding(20)
        } else if let audit = viewModel.auditDetail {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(audit.runId)
                                .font(.system(size: 16, weight: .semibold))
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                                .lineLimit(1)
                            Text("事件数 \(audit.eventCount ?? audit.events.count)")
                                .font(.system(size: 12))
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                        }
                        Spacer()
                        StatusChip(title: audit.status ?? "unknown", tint: tintForStatus(audit.status))
                    }

                    if !audit.warnings.isEmpty {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("警告")
                                .font(.system(size: 13, weight: .semibold))
                            ForEach(audit.warnings, id: \.self) { warning in
                                Text(warning)
                                    .font(.system(size: 12))
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.warning)
                            }
                        }
                        .padding(14)
                        .glassCard(cornerRadius: 12)
                    }

                    VStack(alignment: .leading, spacing: 10) {
                        Text("事件时间线")
                            .font(.system(size: 15, weight: .semibold))
                        ForEach(audit.events) { event in
                            VStack(alignment: .leading, spacing: 6) {
                                HStack {
                                    Text(event.eventType)
                                        .font(.system(size: 13, weight: .semibold))
                                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                                    Spacer()
                                    Text(event.createdAt.dateTimeText)
                                        .font(.system(size: 11))
                                        .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                                }
                                if let payload = event.payload {
                                    Text(payload.previewText)
                                        .font(.system(size: 12))
                                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                                        .fixedSize(horizontal: false, vertical: true)
                                }
                            }
                            .padding(12)
                            .glassCard(cornerRadius: 12)
                        }
                    }
                }
                .padding(20)
            }
        } else {
            EmptyStateView(title: "审计暂不可用", message: "当前没有拿到这次运行的审计详情。")
                .padding(20)
        }
    }

    private func userBubble(_ message: AgentMessage) -> some View {
        VStack(alignment: .trailing, spacing: 4) {
            Text(message.content)
                .font(.system(size: 14))
                .foregroundStyle(.white)
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
                .background(
                    LinearGradient(
                        colors: [ZhihuijiTheme.ColorToken.primaryBright, ZhihuijiTheme.ColorToken.primary],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    ),
                    in: RoundedRectangle(cornerRadius: 16, style: .continuous)
                )
            Text(message.createdAt.dateTimeText)
                .font(.system(size: 11))
                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
        }
    }

    private func assistantBubble(
        content: String,
        timestamp: String,
        structuredData: [AgentResultBlock]?,
        isLive: Bool
    ) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(isLive ? "助手生成中" : "助手")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                if isLive {
                    ProgressView()
                        .scaleEffect(0.7)
                        .tint(ZhihuijiTheme.ColorToken.primary)
                }
                Spacer()
            }

            Text(content)
                .font(.system(size: 14))
                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                .fixedSize(horizontal: false, vertical: true)

            if let structuredData, !structuredData.isEmpty {
                VStack(spacing: 8) {
                    ForEach(structuredData.prefix(2)) { block in
                        resultBlockCompactView(block)
                    }
                }
            }

            Text(timestamp)
                .font(.system(size: 11))
                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(Color.white.opacity(0.62), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(Color.white.opacity(0.46), lineWidth: 0.5)
        )
    }

    private func resultBlockCompactView(_ block: AgentResultBlock) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(block.title?.nilIfBlank ?? block.blockType)
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
            Text(block.data?.previewText ?? "已生成结构化结果")
                .font(.system(size: 11))
                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                .lineLimit(3)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(10)
        .background(Color.white.opacity(0.44), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private func resultBlockView(_ block: AgentResultBlock) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(block.title?.nilIfBlank ?? block.blockType)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)

            if block.blockType == "kpi_grid", let kpis = block.data?.objectValue?["kpis"]?.arrayValue, !kpis.isEmpty {
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 10) {
                    ForEach(Array(kpis.enumerated()), id: \.offset) { _, item in
                        VStack(alignment: .leading, spacing: 6) {
                            Text(item.objectValue?["label"]?.stringValue ?? "指标")
                                .font(.system(size: 12))
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                            Text(item.objectValue?["value"]?.displayText ?? "-")
                                .font(.system(size: 18, weight: .bold))
                                .foregroundStyle(tintForTrend(item.objectValue?["trend_direction"]?.stringValue))
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(12)
                        .background(Color.white.opacity(0.46), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                    }
                }
            } else if block.blockType == "table",
                      let payload = block.data?.objectValue,
                      let headers = payload["headers"]?.arrayValue,
                      let rows = payload["rows"]?.arrayValue {
                VStack(alignment: .leading, spacing: 8) {
                    HStack(alignment: .top) {
                        ForEach(Array(headers.enumerated()), id: \.offset) { _, header in
                            Text(header.displayText)
                                .font(.system(size: 11, weight: .semibold))
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                                .frame(maxWidth: .infinity, alignment: .leading)
                        }
                    }
                    ForEach(Array(rows.prefix(5).enumerated()), id: \.offset) { _, row in
                        HStack(alignment: .top) {
                            ForEach(Array((row.arrayValue ?? []).enumerated()), id: \.offset) { _, cell in
                                Text(cell.displayText)
                                    .font(.system(size: 11))
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                            }
                        }
                        .padding(.vertical, 6)
                        Divider()
                    }
                }
            } else if block.blockType == "rank_list",
                      let items = block.data?.objectValue?["items"]?.arrayValue,
                      !items.isEmpty {
                VStack(spacing: 8) {
                    ForEach(Array(items.prefix(6).enumerated()), id: \.offset) { index, item in
                        HStack {
                            Text("\(index + 1)")
                                .font(.system(size: 12, weight: .bold))
                                .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                                .frame(width: 18)
                            Text(item.rankTitle)
                                .font(.system(size: 12, weight: .medium))
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                            Spacer()
                            Text(item.rankValue)
                                .font(.system(size: 12, weight: .semibold))
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                        }
                    }
                }
            } else if block.blockType == "risk_card",
                      let payload = block.data?.objectValue {
                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        StatusChip(
                            title: payload["level"]?.stringValue ?? "risk",
                            tint: riskTint(payload["level"]?.stringValue ?? "low")
                        )
                        Spacer()
                    }
                    Text(payload["title"]?.displayText ?? block.title?.nilIfBlank ?? "风险提醒")
                        .font(.system(size: 14, weight: .semibold))
                    Text(payload["description"]?.displayText ?? "已生成风险摘要")
                        .font(.system(size: 12))
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                }
            } else {
                Text(block.data?.previewText ?? "已生成结构化结果")
                    .font(.system(size: 12))
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .padding(14)
        .glassCard(cornerRadius: 12)
    }

    private func tintForTrend(_ trend: String?) -> Color {
        switch trend?.lowercased() {
        case "up", "rise", "positive":
            return ZhihuijiTheme.ColorToken.success
        case "down", "fall", "negative":
            return ZhihuijiTheme.ColorToken.danger
        default:
            return ZhihuijiTheme.ColorToken.primary
        }
    }

    private func riskTint(_ level: String) -> Color {
        switch level.lowercased() {
        case "high", "danger":
            return ZhihuijiTheme.ColorToken.danger
        case "medium", "warning":
            return ZhihuijiTheme.ColorToken.warning
        default:
            return ZhihuijiTheme.ColorToken.primary
        }
    }

    private func tintForStatus(_ status: String?) -> Color {
        switch status?.lowercased() {
        case "failed", "error", "blocked", "cancelled":
            return ZhihuijiTheme.ColorToken.danger
        case "completed", "success", "done":
            return ZhihuijiTheme.ColorToken.success
        case "running", "in_progress":
            return ZhihuijiTheme.ColorToken.primary
        default:
            return ZhihuijiTheme.ColorToken.primary
        }
    }

    private func notificationTint(_ notification: AgentNotification) -> Color {
        switch notification.level?.lowercased() {
        case "high", "danger":
            return ZhihuijiTheme.ColorToken.danger
        case "medium", "warning":
            return ZhihuijiTheme.ColorToken.warning
        default:
            return ZhihuijiTheme.ColorToken.primary
        }
    }

    private func iconForToolStatus(_ status: String?) -> String {
        switch status?.lowercased() {
        case "failed", "error":
            return "exclamationmark.circle.fill"
        case "completed", "success", "done":
            return "checkmark.circle.fill"
        default:
            return "ellipsis.circle.fill"
        }
    }
}

private extension AgentChatResponse {
    var livePreview: AgentLiveRunPreview {
        AgentLiveRunPreview(
            runId: runId,
            conversationId: conversationId,
            answer: answer,
            planSummary: planSummary,
            toolCalls: toolCalls,
            resultBlocks: resultBlocks.isEmpty ? blocks : resultBlocks,
            mode: mode,
            llmStatus: llmStatus,
            planSource: planSource
        )
    }
}

private struct AgentDraftSheet: View {
    let draft: AgentDraft
    @Binding var title: String
    @Binding var content: String
    @Binding var status: String
    let isSaving: Bool
    let onUse: () -> Void
    let onSave: () -> Void
    let onDelete: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                VStack(alignment: .leading, spacing: 8) {
                    HStack(spacing: 10) {
                        StatusChip(title: draft.draftType, tint: ZhihuijiTheme.ColorToken.primary)
                        StatusChip(
                            title: status.nilIfBlank ?? "open",
                            tint: status == "archived" ? ZhihuijiTheme.ColorToken.warning : ZhihuijiTheme.ColorToken.success
                        )
                    }
                    Text("创建于 \(draft.createdAt.dateTimeText)")
                        .font(.system(size: 12))
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                }
                .padding(16)
                .glassCard()

                VStack(alignment: .leading, spacing: 12) {
                    Text("草稿内容")
                        .font(.system(size: 18, weight: .semibold))
                    TextField("草稿标题", text: $title)
                        .fieldBackground()
                    TextField("草稿内容", text: $content, axis: .vertical)
                        .lineLimit(5 ... 10)
                        .fieldBackground()
                    Picker("状态", selection: $status) {
                        Text("Open").tag("open")
                        Text("Archived").tag("archived")
                    }
                    .pickerStyle(.segmented)
                }
                .padding(16)
                .glassCard()

                HStack(spacing: 12) {
                    PrimaryGlassButton(
                        title: isSaving ? "处理中..." : "回填输入框",
                        systemImage: "arrow.up.left.and.arrow.down.right",
                        disabled: isSaving,
                        action: onUse
                    )
                    Button(action: onSave) {
                        HStack(spacing: 8) {
                            Image(systemName: "square.and.arrow.down.fill")
                            Text(isSaving ? "保存中..." : "保存草稿")
                                .font(.system(size: 15, weight: .semibold))
                        }
                        .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(Color.white.opacity(0.54), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.pill, style: .continuous))
                        .overlay(
                            RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.pill, style: .continuous)
                                .stroke(Color.white.opacity(0.45), lineWidth: 0.8)
                        )
                    }
                    .buttonStyle(.plain)
                    .disabled(isSaving)
                    .opacity(isSaving ? 0.55 : 1)
                }

                Button(action: onDelete) {
                    HStack(spacing: 8) {
                        Image(systemName: "trash")
                        Text(isSaving ? "处理中..." : "删除草稿")
                            .font(.system(size: 15, weight: .semibold))
                    }
                    .foregroundStyle(ZhihuijiTheme.ColorToken.danger)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(ZhihuijiTheme.ColorToken.danger.opacity(0.08), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.pill, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.pill, style: .continuous)
                            .stroke(ZhihuijiTheme.ColorToken.danger.opacity(0.18), lineWidth: 0.8)
                    )
                }
                .buttonStyle(.plain)
                .disabled(isSaving)
                .opacity(isSaving ? 0.55 : 1)
            }
            .padding(20)
        }
    }
}

private extension View {
    @ViewBuilder
    func agentComposerBehavior() -> some View {
        #if os(iOS)
        self
            .textInputAutocapitalization(.never)
            .submitLabel(.send)
        #else
        self
        #endif
    }
}

private extension JSONValue {
    var objectValue: [String: JSONValue]? {
        if case let .object(value) = self { return value }
        return nil
    }

    var arrayValue: [JSONValue]? {
        if case let .array(value) = self { return value }
        return nil
    }

    var stringValue: String? {
        if case let .string(value) = self { return value }
        return nil
    }

    var numberValue: Double? {
        if case let .number(value) = self { return value }
        return nil
    }

    var displayText: String {
        switch self {
        case let .string(value):
            return value
        case let .number(value):
            if value.rounded() == value {
                return String(Int(value))
            }
            return String(format: "%.2f", value)
        case let .bool(value):
            return value ? "是" : "否"
        case let .array(value):
            return value.prefix(4).map(\.displayText).joined(separator: "、")
        case let .object(value):
            return value.values.prefix(4).map(\.displayText).joined(separator: "、")
        case .null:
            return "-"
        }
    }

    var previewText: String {
        switch self {
        case let .object(value):
            return value.map { "\($0.key)：\($0.value.displayText)" }
                .prefix(6)
                .joined(separator: " | ")
        default:
            return displayText
        }
    }

    var rankTitle: String {
        objectValue?["label"]?.stringValue
            ?? objectValue?["title"]?.stringValue
            ?? objectValue?["name"]?.stringValue
            ?? objectValue?["customer_name"]?.stringValue
            ?? objectValue?["supplier_name"]?.stringValue
            ?? displayText
    }

    var rankValue: String {
        objectValue?["value"]?.displayText
            ?? objectValue?["amount"]?.displayText
            ?? objectValue?["balance"]?.displayText
            ?? objectValue?["description"]?.displayText
            ?? "-"
    }
}
