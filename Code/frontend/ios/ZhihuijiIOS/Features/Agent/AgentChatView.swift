import SwiftUI

struct AgentChatView: View {
    @EnvironmentObject private var session: AppSession
    @Environment(\.appEnvironment) private var env
    @StateObject private var viewModel = AgentViewModel()

    private var access: AgentAccessPolicy {
        AgentAccessPolicy.resolve(for: session.permissions)
    }

    private var currentRun: AgentLiveRunPreview? {
        viewModel.liveRun ?? viewModel.latestResponse?.livePreview
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                headerCard
                dedicatedPageEntries
                workbenchSection
                pendingDraftSection
                conversationsSection
                messagesSection
                runSection
                terminalStatusSection
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
        .sheet(isPresented: $viewModel.isConfirmationPresented) {
            NavigationStack {
                AgentDraftConfirmationSheet(
                    draft: viewModel.pendingConfirmationDraft,
                    state: viewModel.confirmationState,
                    onConfirm: {
                        Task { await viewModel.confirmPendingDraft(using: env.apiClient) }
                    },
                    onReject: {
                        viewModel.rejectPendingDraft()
                    },
                    onDismiss: {
                        viewModel.dismissConfirmation()
                    }
                )
                .navigationTitle("草稿二次确认")
                .toolbar {
                    ToolbarItem {
                        Button("关闭") {
                            viewModel.dismissConfirmation()
                        }
                    }
                }
            }
            .presentationDetents([.medium, .large])
            .presentationDragIndicator(.visible)
            // 关闭弹窗（下拉、返回）不会触发确认；只有 "确认" 按钮才会调用草稿确认接口。
            .interactiveDismissDisabled(viewModel.confirmationState == .confirming)
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
                        .font(ZhihuijiTheme.Typography.pageTitle)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                }
                Spacer()
                StatusChip(
                    title: access.canWriteAgent ? "可提问" : "只读",
                    tint: access.canWriteAgent ? ZhihuijiTheme.ColorToken.primary : ZhihuijiTheme.ColorToken.warning
                )
            }

            if let errorMessage = viewModel.errorMessage {
                Text(errorMessage)
                    .font(ZhihuijiTheme.Typography.bodyMedium)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.danger)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 10)
                    .background(
                        ZhihuijiTheme.ColorToken.danger.opacity(0.08),
                        in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.cardSmall, style: .continuous)
                    )
            }

            if let notice = viewModel.contextCompactedNotice?.nilIfBlank {
                HStack(alignment: .top, spacing: 10) {
                    Image(systemName: "arrow.triangle.2.circlepath")
                        .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                        .padding(.top, 1)
                    Text(notice)
                        .font(ZhihuijiTheme.Typography.caption)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .padding(12)
                .background(
                    ZhihuijiTheme.ColorToken.primary.opacity(0.08),
                    in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.cardSmall, style: .continuous)
                )
            }
        }
        .padding(16)
        .glassCard()
    }

    @ViewBuilder
    private var dedicatedPageEntries: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("专页入口")
                .font(ZhihuijiTheme.Typography.sectionTitle)

            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                NavigationLink {
                    AgentWorkbenchView()
                } label: {
                    dedicatedEntryCard(
                        title: "工作台",
                        subtitle: "经营概览、KPI、待办与风险",
                        icon: "square.grid.2x2.fill",
                        tint: ZhihuijiTheme.ColorToken.primary
                    )
                }
                .buttonStyle(.plain)

                NavigationLink {
                    AgentDraftsView()
                } label: {
                    dedicatedEntryCard(
                        title: "草稿管理",
                        subtitle: "创建、编辑、归档与删除草稿",
                        icon: "doc.text.fill",
                        tint: ZhihuijiTheme.ColorToken.primaryBright
                    )
                }
                .buttonStyle(.plain)

                NavigationLink {
                    AgentTasksView()
                } label: {
                    dedicatedEntryCard(
                        title: "任务与通知",
                        subtitle: "查看任务进度与风险通知",
                        icon: "bell.badge.fill",
                        tint: ZhihuijiTheme.ColorToken.warning
                    )
                }
                .buttonStyle(.plain)
            }
        }
    }

    private func dedicatedEntryCard(title: String, subtitle: String, icon: String, tint: Color) -> some View {
        HStack(spacing: 12) {
            Circle()
                .fill(tint.opacity(0.14))
                .frame(width: 38, height: 38)
                .overlay(
                    Image(systemName: icon)
                        .font(ZhihuijiTheme.Typography.bodyMedium)
                        .foregroundStyle(tint)
                )
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(ZhihuijiTheme.Typography.bodyMedium)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                Text(subtitle)
                    .font(ZhihuijiTheme.Typography.caption)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            Spacer(minLength: 0)
            Image(systemName: "chevron.right")
                .font(ZhihuijiTheme.Typography.captionSemibold)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
        }
        .padding(14)
        .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
    }

    @ViewBuilder
    private var workbenchSection: some View {
        if let workbench = viewModel.workbench {
            VStack(alignment: .leading, spacing: 14) {
                if !workbench.kpiCards.isEmpty {
                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                        ForEach(workbench.kpiCards) { card in
                            MetricCard(
                                title: card.label,
                                value: card.value,
                                subtitle: card.trendValue?.nilIfBlank,
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
                                        .font(ZhihuijiTheme.Typography.bodyMedium)
                                        .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                                        .padding(.horizontal, 14)
                                        .padding(.vertical, 10)
                                        .background(
                                            Color.white.opacity(0.48),
                                            in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.pill, style: .continuous)
                                        )
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
                                            .font(ZhihuijiTheme.Typography.caption)
                                            .foregroundStyle(riskTint(alert.level))
                                    )
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(alert.title)
                                        .font(ZhihuijiTheme.Typography.bodyMedium)
                                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                                    Text(alert.description)
                                        .font(ZhihuijiTheme.Typography.caption)
                                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                                }
                                Spacer()
                            }
                            .padding(12)
                            .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
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
                    .font(ZhihuijiTheme.Typography.sectionTitle)
                ForEach(drafts.prefix(4)) { draft in
                    Button {
                        Task { await viewModel.openDraft(draft, client: env.apiClient) }
                    } label: {
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(draft.title)
                                    .font(ZhihuijiTheme.Typography.bodyMedium)
                                Text(draft.draftType)
                                    .font(ZhihuijiTheme.Typography.caption)
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                            }
                            Spacer()
                            VStack(alignment: .trailing, spacing: 4) {
                                Text(draft.createdAt.dateTimeText)
                                    .font(ZhihuijiTheme.Typography.caption)
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                                Image(systemName: "chevron.right")
                                    .font(ZhihuijiTheme.Typography.captionSemibold)
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                            }
                        }
                        .padding(14)
                        .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
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
                    .font(ZhihuijiTheme.Typography.sectionTitle)
                Spacer()
                if access.canWriteAgent {
                    Button {
                        Task { await viewModel.createConversation(using: env.apiClient) }
                    } label: {
                        HStack(spacing: 6) {
                            Image(systemName: "plus")
                            Text(viewModel.isConversationSaving ? "处理中..." : "新会话")
                                .font(ZhihuijiTheme.Typography.captionSemibold)
                        }
                        .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(
                            Color.white.opacity(0.52),
                            in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.pill, style: .continuous)
                        )
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
                                            .font(ZhihuijiTheme.Typography.bodyMedium)
                                            .lineLimit(1)
                                        Spacer(minLength: 4)
                                        if access.canWriteAgent, viewModel.selectedConversationId == conversation.id {
                                            Image(systemName: "trash")
                                                .font(ZhihuijiTheme.Typography.captionSemibold)
                                                .foregroundStyle(Color.white.opacity(0.92))
                                                .padding(6)
                                                .background(Color.white.opacity(0.18), in: Circle())
                                        }
                                    }
                                    Text(conversation.latestSummary?.nilIfBlank ?? "进入后可查看完整消息")
                                        .font(ZhihuijiTheme.Typography.caption)
                                        .lineLimit(2)
                                    Text((conversation.lastMessageAt ?? conversation.updatedAt).dateTimeText)
                                        .font(ZhihuijiTheme.Typography.caption)
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
                                    in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.field, style: .continuous)
                                )
                            }
                            .buttonStyle(.plain)
                            .contextMenu {
                                if access.canWriteAgent {
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
                .font(ZhihuijiTheme.Typography.sectionTitle)

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
                                    structuredData: message.resultBlocks,
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
                                content: currentRun.answer,
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
                        .font(ZhihuijiTheme.Typography.sectionTitle)
                    Spacer()
                    if let terminal = viewModel.terminalStatus {
                        StatusChip(
                            title: terminal.displayLabel,
                            tint: terminal.tint
                        )
                        .accessibilityLabel("运行终态")
                        .accessibilityValue(terminal.voiceOverLabel)
                    } else if let llmStatus = currentRun.llmStatus?.nilIfBlank {
                        StatusChip(title: llmStatus, tint: tintForStatus(llmStatus))
                    }
                }

                if let planSummary = currentRun.planSummary?.nilIfBlank {
                    VStack(alignment: .leading, spacing: 6) {
                        Text("执行计划")
                            .font(ZhihuijiTheme.Typography.bodyMedium)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                        Text(planSummary)
                            .font(ZhihuijiTheme.Typography.body)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                    }
                    .padding(14)
                    .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
                }

                if !currentRun.toolCalls.isEmpty {
                    VStack(alignment: .leading, spacing: 10) {
                        Text("工具轨迹")
                            .font(ZhihuijiTheme.Typography.bodyMedium)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                        ForEach(currentRun.toolCalls) { tool in
                            HStack(alignment: .top, spacing: 12) {
                                Circle()
                                    .fill(tintForStatus(tool.status).opacity(0.18))
                                    .frame(width: 28, height: 28)
                                    .overlay(
                                        Image(systemName: iconForToolStatus(tool.status))
                                            .font(ZhihuijiTheme.Typography.captionSemibold)
                                            .foregroundStyle(tintForStatus(tool.status))
                                    )
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(tool.toolName)
                                        .font(ZhihuijiTheme.Typography.bodyMedium)
                                    if let inputSummary = tool.inputSummary?.nilIfBlank {
                                        Text(inputSummary)
                                            .font(ZhihuijiTheme.Typography.caption)
                                            .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                                    }
                                    if let resultSummary = tool.resultSummary?.nilIfBlank {
                                        Text(resultSummary)
                                            .font(ZhihuijiTheme.Typography.caption)
                                            .foregroundStyle(tintForStatus(tool.status))
                                    }
                                }
                                Spacer()
                                Text(tool.status ?? "pending")
                                    .font(ZhihuijiTheme.Typography.captionSemibold)
                                    .foregroundStyle(tintForStatus(tool.status))
                            }
                            .padding(12)
                            .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
                        }
                    }
                }

                if !currentRun.resultBlocks.isEmpty {
                    VStack(alignment: .leading, spacing: 10) {
                        Text("结构化结果")
                            .font(ZhihuijiTheme.Typography.bodyMedium)
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
                            .font(ZhihuijiTheme.Typography.bodyMedium)
                    }
                    .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(
                        Color.white.opacity(0.54),
                        in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.field, style: .continuous)
                    )
                }
                .buttonStyle(.plain)
                .accessibilityLabel("查看完整审计")
                .accessibilityHint("打开本次运行的审计详情")
            }
            .padding(16)
            .glassCard()
        }
    }

    /// 终态展示区块。仅当后端下发 terminal_status 时显示。
    /// 成功（COMPLETED）显示成功样式；非成功终态（FAILED/BLOCKED/CANCELLED/EXHAUSTED）
    /// 不显示成功样式，避免误导用户。
    @ViewBuilder
    private var terminalStatusSection: some View {
        if let terminal = viewModel.terminalStatus {
            VStack(alignment: .leading, spacing: 10) {
                HStack(alignment: .top, spacing: 12) {
                    Circle()
                        .fill(terminal.tint.opacity(0.18))
                        .frame(width: 36, height: 36)
                        .overlay(
                            Image(systemName: terminal.iconName)
                                .font(ZhihuijiTheme.Typography.bodyMedium)
                                .foregroundStyle(terminal.tint)
                        )
                        .accessibilityHidden(true)

                    VStack(alignment: .leading, spacing: 6) {
                        Text(terminal.displayTitle)
                            .font(ZhihuijiTheme.Typography.cardTitle)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                            .accessibilityAddTraits(.isHeader)

                        if let message = viewModel.terminalMessage?.nilIfBlank ?? terminal.defaultMessage?.nilIfBlank {
                            Text(message)
                                .font(ZhihuijiTheme.Typography.body)
                                .foregroundStyle(terminal.isSuccessful ? ZhihuijiTheme.ColorToken.success : ZhihuijiTheme.ColorToken.textSecondary)
                                .fixedSize(horizontal: false, vertical: true)
                        }

                        if !viewModel.completedTools.isEmpty {
                            Text("已完成工具：\(viewModel.completedTools.joined(separator: "、"))")
                                .font(ZhihuijiTheme.Typography.caption)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                                .fixedSize(horizontal: false, vertical: true)
                        }

                        if !viewModel.missingTargetTools.isEmpty {
                            Text("未完成的目标工具：\(viewModel.missingTargetTools.joined(separator: "、"))")
                                .font(ZhihuijiTheme.Typography.caption)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.warning)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                    }

                    Spacer(minLength: 0)
                }
                .padding(14)
                .background(
                    terminal.tint.opacity(0.08),
                    in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.cardSmall, style: .continuous)
                )
                .overlay(
                    RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.cardSmall, style: .continuous)
                        .stroke(terminal.tint.opacity(0.22), lineWidth: ZhihuijiTheme.Stroke.hairline)
                )
                .accessibilityElement(children: .combine)
                .accessibilityLabel("运行终态")
                .accessibilityValue(terminal.voiceOverLabel)
            }
        }
    }

    private var taskSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("近期任务")
                .font(ZhihuijiTheme.Typography.sectionTitle)
            if viewModel.tasks.isEmpty {
                EmptyStateView(title: "暂无任务", message: "当前没有挂起中的 AI 任务。")
            } else {
                ForEach(viewModel.tasks.prefix(4)) { task in
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(task.title)
                                .font(ZhihuijiTheme.Typography.bodyMedium)
                            Text(task.statusLabel ?? task.status ?? task.taskType)
                                .font(ZhihuijiTheme.Typography.caption)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                        }
                        Spacer()
                        if let progress = task.progress {
                            Text("\(progress)%")
                                .font(ZhihuijiTheme.Typography.captionSemibold)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                        }
                    }
                    .padding(14)
                    .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
                }
            }
        }
    }

    private var notificationSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("通知")
                .font(ZhihuijiTheme.Typography.sectionTitle)
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
                                        .font(ZhihuijiTheme.Typography.caption)
                                        .foregroundStyle(notificationTint(notification))
                                )
                            VStack(alignment: .leading, spacing: 4) {
                                Text(notification.title)
                                    .font(ZhihuijiTheme.Typography.bodyMedium)
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                                Text(notification.body)
                                    .font(ZhihuijiTheme.Typography.caption)
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                                Text(notification.createdAt.dateTimeText)
                                    .font(ZhihuijiTheme.Typography.caption)
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                            }
                            Spacer()
                        }
                        .padding(14)
                        .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private var composerSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("发起提问")
                .font(ZhihuijiTheme.Typography.sectionTitle)

            if access.canWriteAgent {
                TextField("输入问题", text: $viewModel.draftQuestion, axis: .vertical)
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
                                .font(ZhihuijiTheme.Typography.bodyMedium)
                        }
                        .foregroundStyle(viewModel.draftQuestion.nilIfBlank == nil ? ZhihuijiTheme.ColorToken.textTertiary : ZhihuijiTheme.ColorToken.primary)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(
                            Color.white.opacity(0.54),
                            in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.pill, style: .continuous)
                        )
                        .overlay(
                            RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.pill, style: .continuous)
                                .stroke(Color.white.opacity(0.45), lineWidth: ZhihuijiTheme.Stroke.hairline)
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
                                .font(ZhihuijiTheme.Typography.bodyMedium)
                        }
                        .foregroundStyle(viewModel.isSending ? ZhihuijiTheme.ColorToken.danger : ZhihuijiTheme.ColorToken.textTertiary)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(
                            Color.white.opacity(0.54),
                            in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.pill, style: .continuous)
                        )
                        .overlay(
                            RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.pill, style: .continuous)
                                .stroke(viewModel.isSending ? ZhihuijiTheme.ColorToken.danger.opacity(0.25) : Color.white.opacity(0.45), lineWidth: ZhihuijiTheme.Stroke.hairline)
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
                                .font(ZhihuijiTheme.Typography.cardTitle)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                                .lineLimit(1)
                            Text("事件数 \(audit.eventCount ?? audit.events.count)")
                                .font(ZhihuijiTheme.Typography.caption)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                        }
                        Spacer()
                        StatusChip(title: audit.status ?? "unknown", tint: tintForStatus(audit.status))
                    }

                    if !audit.warnings.isEmpty {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("警告")
                                .font(ZhihuijiTheme.Typography.captionSemibold)
                            ForEach(audit.warnings, id: \.self) { warning in
                                Text(warning)
                                    .font(ZhihuijiTheme.Typography.caption)
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.warning)
                            }
                        }
                        .padding(14)
                        .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
                    }

                    VStack(alignment: .leading, spacing: 10) {
                        Text("事件时间线")
                            .font(ZhihuijiTheme.Typography.bodyMedium)
                        ForEach(audit.events) { event in
                            VStack(alignment: .leading, spacing: 6) {
                                HStack {
                                    Text(event.eventType)
                                        .font(ZhihuijiTheme.Typography.captionSemibold)
                                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                                    Spacer()
                                    Text(event.createdAt.dateTimeText)
                                        .font(ZhihuijiTheme.Typography.caption)
                                        .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                                }
                                if let payload = event.payload {
                                    Text(payload.previewText)
                                        .font(ZhihuijiTheme.Typography.caption)
                                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                                        .fixedSize(horizontal: false, vertical: true)
                                }
                            }
                            .padding(12)
                            .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
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
                .font(ZhihuijiTheme.Typography.body)
                .foregroundStyle(.white)
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
                .background(
                    LinearGradient(
                        colors: [ZhihuijiTheme.ColorToken.primaryBright, ZhihuijiTheme.ColorToken.primary],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    ),
                    in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous)
                )
            Text(message.createdAt.dateTimeText)
                .font(ZhihuijiTheme.Typography.caption)
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
                    .font(ZhihuijiTheme.Typography.captionSemibold)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                if isLive {
                    ProgressView()
                        .scaleEffect(0.7)
                        .tint(ZhihuijiTheme.ColorToken.primary)
                }
                Spacer()
            }

            if let visibleContent = content.nilIfBlank {
                Text(visibleContent)
                    .font(ZhihuijiTheme.Typography.body)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                    .fixedSize(horizontal: false, vertical: true)
            } else if isLive {
                ProgressView()
                    .tint(ZhihuijiTheme.ColorToken.primary)
            }

            if let structuredData, !structuredData.isEmpty {
                VStack(spacing: 8) {
                    ForEach(structuredData.prefix(2)) { block in
                        resultBlockCompactView(block)
                    }
                }
            }

            Text(timestamp)
                .font(ZhihuijiTheme.Typography.caption)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(Color.white.opacity(0.62), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous)
                .stroke(Color.white.opacity(0.46), lineWidth: ZhihuijiTheme.Stroke.hairline)
        )
    }

    private func resultBlockCompactView(_ block: AgentResultBlock) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(block.title?.nilIfBlank ?? block.blockType)
                .font(ZhihuijiTheme.Typography.captionSemibold)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
            if let previewText = block.data?.previewText.nilIfBlank {
                Text(previewText)
                    .font(ZhihuijiTheme.Typography.caption)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                    .lineLimit(3)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(ZhihuijiTheme.Spacing.sm)
        .background(
            ZhihuijiTheme.ColorToken.surfaceWhite.opacity(0.68),
            in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.cardSmall, style: .continuous)
        )
    }

    @ViewBuilder
    private func resultBlockView(_ block: AgentResultBlock) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(block.title?.nilIfBlank ?? block.blockType)
                .font(ZhihuijiTheme.Typography.cardTitle)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)

            if ["text", "markdown"].contains(normalizedBlockType(for: block)),
               let payload = block.data?.decode(as: AgentTextBlockData.self),
               let markdown = payload.markdown?.nilIfBlank ?? payload.text?.nilIfBlank {
                AgentMarkdownBlock(markdown: markdown)
            } else if block.blockType == "kpi_grid", let kpis = block.data?.objectValue?["kpis"]?.arrayValue, !kpis.isEmpty {
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 10) {
                    ForEach(Array(kpis.enumerated()), id: \.offset) { _, item in
                        VStack(alignment: .leading, spacing: 6) {
                            Text(item.objectValue?["label"]?.stringValue ?? "指标")
                                .font(ZhihuijiTheme.Typography.caption)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                            Text(item.objectValue?["value"]?.displayText ?? "-")
                                .font(ZhihuijiTheme.Typography.amount)
                                .foregroundStyle(tintForTrend(item.objectValue?["trend_direction"]?.stringValue))
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(12)
                        .background(
                            ZhihuijiTheme.ColorToken.surfaceWhite.opacity(0.72),
                            in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.cardSmall, style: .continuous)
                        )
                    }
                }
            } else if block.blockType == "table",
                      let payload = block.data?.objectValue,
                      let headers = payload["headers"]?.arrayValue,
                      let rows = payload["rows"]?.arrayValue {
                ScrollView(.horizontal, showsIndicators: false) {
                    VStack(alignment: .leading, spacing: 8) {
                        HStack(alignment: .top, spacing: 10) {
                            ForEach(Array(headers.enumerated()), id: \.offset) { _, header in
                                Text(header.displayText)
                                    .font(ZhihuijiTheme.Typography.captionSemibold)
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                                    .frame(width: 108, alignment: .leading)
                            }
                        }
                        ForEach(Array(rows.prefix(5).enumerated()), id: \.offset) { _, row in
                            HStack(alignment: .top, spacing: 10) {
                                ForEach(Array((row.arrayValue ?? []).enumerated()), id: \.offset) { _, cell in
                                    Text(cell.displayText)
                                        .font(ZhihuijiTheme.Typography.caption)
                                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                                        .frame(width: 108, alignment: .leading)
                                        .fixedSize(horizontal: false, vertical: true)
                                }
                            }
                            .padding(.vertical, 6)
                            Divider()
                        }
                    }
                }
            } else if block.blockType == "rank_list",
                      let items = block.data?.objectValue?["items"]?.arrayValue,
                      !items.isEmpty {
                VStack(spacing: 8) {
                    ForEach(Array(items.prefix(6).enumerated()), id: \.offset) { index, item in
                        HStack {
                            Text("\(index + 1)")
                                .font(ZhihuijiTheme.Typography.captionSemibold)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                                .frame(width: 18)
                            Text(item.rankTitle)
                                .font(ZhihuijiTheme.Typography.bodyMedium)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                            Spacer()
                            Text(item.rankValue)
                                .font(ZhihuijiTheme.Typography.bodyMedium)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                        }
                    }
                }
            } else if isLineChartBlock(block),
                      let payload = block.data?.decode(as: AgentLineChartBlockData.self) {
                AgentLineChartBlock(data: payload)
            } else if isBarChartBlock(block),
                      let payload = block.data?.decode(as: AgentBarChartBlockData.self) {
                AgentBarChartBlock(data: payload)
            } else if isDonutChartBlock(block),
                      let payload = block.data?.decode(as: AgentDonutChartBlockData.self) {
                AgentDonutChartBlock(data: payload)
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
                        .font(ZhihuijiTheme.Typography.cardTitle)
                    Text(payload["description"]?.displayText ?? "已生成风险摘要")
                        .font(ZhihuijiTheme.Typography.body)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                }
            } else if isEvidenceBlock(block),
                      let payload = block.data?.decode(as: AgentEvidenceCardBlockData.self) {
                AgentEvidenceBlock(data: payload)
            } else if block.blockType == "draft_card",
                      let payload = block.data?.decode(as: AgentDraftCardBlockData.self) {
                AgentDraftCardBlock(data: payload)
            } else {
                AgentUnknownResultBlock(block: block)
            }
        }
        .padding(14)
        .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
    }

    private func normalizedBlockType(for block: AgentResultBlock) -> String {
        let raw = block.blockType.lowercased()
        guard raw == "chart" else { return raw }
        return zhihuijiInferChartBlockType(block) ?? raw
    }

    private func isLineChartBlock(_ block: AgentResultBlock) -> Bool {
        ["line_chart", "area_chart", "trend_chart"].contains(normalizedBlockType(for: block))
    }

    private func isBarChartBlock(_ block: AgentResultBlock) -> Bool {
        ["bar_chart", "column_chart", "horizontal_bar_chart"].contains(normalizedBlockType(for: block))
    }

    private func isDonutChartBlock(_ block: AgentResultBlock) -> Bool {
        ["donut_chart", "pie_chart"].contains(normalizedBlockType(for: block))
    }

    private func isEvidenceBlock(_ block: AgentResultBlock) -> Bool {
        ["evidence_card", "evidence"].contains(normalizedBlockType(for: block))
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
                            title: status.nilIfBlank ?? AgentContractStatus.active,
                            tint: status == AgentContractStatus.archived ? ZhihuijiTheme.ColorToken.warning : ZhihuijiTheme.ColorToken.success
                        )
                    }
                    Text("创建于 \(draft.createdAt.dateTimeText)")
                        .font(ZhihuijiTheme.Typography.caption)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                }
                .padding(16)
                .glassCard()

                VStack(alignment: .leading, spacing: 12) {
                    Text("草稿内容")
                        .font(ZhihuijiTheme.Typography.sectionTitle)
                    TextField("草稿标题", text: $title)
                        .fieldBackground()
                    TextField("草稿内容", text: $content, axis: .vertical)
                        .lineLimit(5 ... 10)
                        .fieldBackground()
                    Picker("状态", selection: $status) {
                        Text("进行中").tag(AgentContractStatus.active)
                        Text("已归档").tag(AgentContractStatus.archived)
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
                                .font(ZhihuijiTheme.Typography.bodyMedium)
                        }
                        .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(
                            Color.white.opacity(0.54),
                            in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.pill, style: .continuous)
                        )
                        .overlay(
                            RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.pill, style: .continuous)
                                .stroke(Color.white.opacity(0.45), lineWidth: ZhihuijiTheme.Stroke.hairline)
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
                            .font(ZhihuijiTheme.Typography.bodyMedium)
                    }
                    .foregroundStyle(ZhihuijiTheme.ColorToken.danger)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(
                        ZhihuijiTheme.ColorToken.danger.opacity(0.08),
                        in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.pill, style: .continuous)
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.pill, style: .continuous)
                            .stroke(ZhihuijiTheme.ColorToken.danger.opacity(0.18), lineWidth: ZhihuijiTheme.Stroke.hairline)
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

func zhihuijiInferChartBlockType(_ block: AgentResultBlock) -> String? {
    if let chartType = block.data?.objectValue?["chart_type"]?.stringValue
        ?? block.data?.objectValue?["type"]?.stringValue
        ?? block.data?.objectValue?["style"]?.stringValue {
        switch normalizedChartAlias(chartType) {
        case "line", "linechart", "trendchart", "areachart", "line_chart", "trend_chart", "area_chart":
            return "line_chart"
        case "bar", "barchart", "columnchart", "horizontalbarchart", "bar_chart", "column_chart", "horizontal_bar_chart":
            return "bar_chart"
        case "pie", "piechart", "donut", "donutchart", "pie_chart", "donut_chart":
            return "donut_chart"
        default:
            break
        }
    }

    if block.data?.objectValue?["segments"]?.arrayValue != nil {
        return "donut_chart"
    }
    if let payload = block.data?.objectValue {
        if payload["labels"]?.arrayValue != nil, payload["series"]?.arrayValue != nil {
            return "line_chart"
        }
        if payload["categories"]?.arrayValue != nil, payload["series"]?.arrayValue != nil {
            return "bar_chart"
        }
        if payload["series"]?.arrayValue != nil {
            return "line_chart"
        }
        if payload["values"]?.arrayValue != nil {
            return "bar_chart"
        }
    }
    if block.data?.objectValue?["series"]?.arrayValue != nil {
        return "line_chart"
    }
    return nil
}

func normalizedChartAlias(_ raw: String) -> String {
    raw.lowercased().replacingOccurrences(of: "_", with: "")
}

private struct AgentMarkdownBlock: View {
    let markdown: String

    var body: some View {
        Group {
            if let attributed = renderedMarkdown {
                Text(attributed)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
            } else {
                Text(markdown)
                    .font(ZhihuijiTheme.Typography.body)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .fixedSize(horizontal: false, vertical: true)
    }

    private var renderedMarkdown: AttributedString? {
        try? AttributedString(
            markdown: markdown,
            options: AttributedString.MarkdownParsingOptions(
                interpretedSyntax: .full,
                failurePolicy: .returnPartiallyParsedIfPossible
            )
        )
    }
}

private struct AgentEvidenceBlock: View {
    let data: AgentEvidenceCardBlockData

    var body: some View {
        VStack(alignment: .leading, spacing: ZhihuijiTheme.Spacing.sm) {
            if data.items.isEmpty {
                Text("本轮查询没有返回可展示的证据明细")
                    .font(ZhihuijiTheme.Typography.body)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
            } else {
                ForEach(data.items.prefix(6)) { item in
                    VStack(alignment: .leading, spacing: 4) {
                        HStack(alignment: .top, spacing: ZhihuijiTheme.Spacing.sm) {
                            Text(item.label)
                                .font(ZhihuijiTheme.Typography.body)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                                .frame(maxWidth: .infinity, alignment: .leading)
                            Text(item.value)
                                .font(ZhihuijiTheme.Typography.bodyMedium)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                                .multilineTextAlignment(.trailing)
                        }
                        if let source = item.displaySource {
                            Text(source)
                                .font(ZhihuijiTheme.Typography.caption)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                        }
                        if let audit = item.auditSummary {
                            Text(audit)
                                .font(ZhihuijiTheme.Typography.caption)
                                .foregroundStyle(item.isTruncated == true ? ZhihuijiTheme.ColorToken.warning : ZhihuijiTheme.ColorToken.textTertiary)
                        }
                    }
                    .padding(.vertical, 2)
                }
            }
        }
    }
}

private struct AgentDraftCardBlock: View {
    let data: AgentDraftCardBlockData

    var body: some View {
        VStack(alignment: .leading, spacing: ZhihuijiTheme.Spacing.sm) {
            HStack(spacing: ZhihuijiTheme.Spacing.sm) {
                StatusChip(title: data.draftType, tint: ZhihuijiTheme.ColorToken.primary)
                if let partnerName = data.partnerName?.nilIfBlank {
                    StatusChip(title: partnerName, tint: ZhihuijiTheme.ColorToken.warning)
                }
                if let status = data.status?.nilIfBlank {
                    StatusChip(title: AgentDraftStatus.displayLabel(status), tint: AgentDraftStatus.tint(status))
                }
            }

            Text(data.summary)
                .font(ZhihuijiTheme.Typography.body)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                .fixedSize(horizontal: false, vertical: true)

            HStack {
                if let itemCount = data.itemCount {
                    Text("\(itemCount) 项")
                        .font(ZhihuijiTheme.Typography.caption)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                        .accessibilityLabel("草稿包含 \(itemCount) 项")
                }
                Spacer()
                if let totalAmount = data.totalAmount?.nilIfBlank {
                    AmountText(value: totalAmount, tint: ZhihuijiTheme.ColorToken.primary)
                        .accessibilityLabel("草稿金额 \(totalAmount)")
                }
            }

            Text("这是 AI 草稿，当前不会直接写入正式业务数据。")
                .font(ZhihuijiTheme.Typography.caption)
                .foregroundStyle(ZhihuijiTheme.ColorToken.warning)
                .accessibilityLabel("二次确认提示：草稿不会自动写入正式业务")

            if let warnings = data.warnings, !warnings.isEmpty {
                VStack(alignment: .leading, spacing: 4) {
                    ForEach(warnings, id: \.self) { warning in
                        Text("• \(warning)")
                            .font(ZhihuijiTheme.Typography.caption)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.warning)
                    }
                }
            }
        }
        .accessibilityElement(children: .contain)
    }
}

/// 草稿二次确认弹窗。
/// - 重要：仅 `确认` 按钮会调用草稿确认接口。`拒绝`/`关闭`/`下拉`/系统中断
///   都不会触发正式写入。
private struct AgentDraftConfirmationSheet: View {
    let draft: AgentDraft?
    let state: DraftConfirmationState
    let onConfirm: () -> Void
    let onReject: () -> Void
    let onDismiss: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                if let draft {
                    draftSummarySection(draft)
                    actionSection(draft)
                } else {
                    EmptyStateView(title: "草稿不可用", message: "草稿已被移除或不存在，无需再次确认。")
                }
            }
            .padding(20)
        }
        .accessibilityAction(named: "关闭") { onDismiss() }
    }

    @ViewBuilder
    private func draftSummarySection(_ draft: AgentDraft) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("二次确认")
                .font(ZhihuijiTheme.Typography.sectionTitle)
                .accessibilityAddTraits(.isHeader)

            Text("AI 草稿不会自动写入正式业务。请核验关键对象、数量与金额后再确认。")
                .font(ZhihuijiTheme.Typography.body)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                .fixedSize(horizontal: false, vertical: true)

            VStack(alignment: .leading, spacing: 10) {
                summaryRow(label: "业务动作", value: draft.draftType)
                summaryRow(label: "关键对象", value: draft.title)
                summaryRow(label: "草稿状态", value: AgentDraftStatus.displayLabel(draft.status))
                if let conversationId = draft.conversationId {
                    summaryRow(label: "所属会话", value: conversationId.rawValue)
                }
                summaryRow(label: "创建时间", value: draft.createdAt.dateTimeText)
            }
        }
        .padding(16)
        .glassCard()
    }

    private func summaryRow(label: String, value: String) -> some View {
        HStack(alignment: .top, spacing: 12) {
            Text(label)
                .font(ZhihuijiTheme.Typography.caption)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                .frame(width: 80, alignment: .leading)
            Text(value)
                .font(ZhihuijiTheme.Typography.bodyMedium)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                .frame(maxWidth: .infinity, alignment: .leading)
                .fixedSize(horizontal: false, vertical: true)
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(label)：\(value)")
    }

    @ViewBuilder
    private func actionSection(_ draft: AgentDraft) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            if AgentDraftStatus.isConfirmed(draft.status) {
                Text("该草稿已确认，无需再次操作。")
                    .font(ZhihuijiTheme.Typography.body)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.success)
                    .accessibilityLabel("草稿已确认")
            } else if AgentDraftStatus.isTerminal(draft.status) {
                Text("草稿当前状态为 \(AgentDraftStatus.displayLabel(draft.status))，无法再确认。")
                    .font(ZhihuijiTheme.Typography.body)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
            } else {
                PrimaryGlassButton(
                    title: confirmButtonTitle,
                    systemImage: "checkmark.shield.fill",
                    disabled: isConfirmDisabled,
                    action: onConfirm
                )
                .accessibilityLabel("确认写入草稿")
                .accessibilityHint("将调用草稿确认接口，确认后才会写入正式业务数据")

                Button {
                    onReject()
                } label: {
                    HStack(spacing: 8) {
                        Image(systemName: "xmark.circle.fill")
                        Text("拒绝")
                            .font(ZhihuijiTheme.Typography.bodyMedium)
                    }
                    .foregroundStyle(ZhihuijiTheme.ColorToken.danger)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(
                        ZhihuijiTheme.ColorToken.danger.opacity(0.08),
                        in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.pill, style: .continuous)
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.pill, style: .continuous)
                            .stroke(ZhihuijiTheme.ColorToken.danger.opacity(0.18), lineWidth: ZhihuijiTheme.Stroke.hairline)
                    )
                }
                .buttonStyle(.plain)
                .disabled(state == .confirming)
                .opacity(state == .confirming ? 0.55 : 1)
                .accessibilityLabel("拒绝写入")
                .accessibilityHint("不调用正式业务接口，仅本地标记为已拒绝")

                if case let .failed(message) = state {
                    Text("确认失败：\(message)")
                        .font(ZhihuijiTheme.Typography.caption)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.danger)
                        .accessibilityLabel("确认失败提示")
                        .accessibilityValue(message)
                }

                Text("关闭弹窗或返回页面都不会触发写入。")
                    .font(ZhihuijiTheme.Typography.caption)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                    .accessibilityLabel("关闭弹窗或返回页面都不会触发写入")
            }
        }
        .padding(16)
        .glassCard()
    }

    private var confirmButtonTitle: String {
        switch state {
        case .confirming: return "确认中..."
        case .confirmed: return "已确认"
        default: return "确认"
        }
    }

    private var isConfirmDisabled: Bool {
        switch state {
        case .confirming, .confirmed: return true
        default: return false
        }
    }
}

private extension TerminalStatus {
    var displayLabel: String {
        switch self {
        case .completed: return "已完成"
        case .confirmationPending: return "待确认"
        case .failed: return "失败"
        case .blocked: return "已阻止"
        case .cancelled: return "已取消"
        case .exhausted: return "轮次耗尽"
        }
    }

    var displayTitle: String {
        switch self {
        case .completed: return "运行完成"
        case .confirmationPending: return "等待二次确认"
        case .failed: return "运行失败"
        case .blocked: return "运行被阻止"
        case .cancelled: return "运行已取消"
        case .exhausted: return "本轮轮次耗尽"
        }
    }

    var defaultMessage: String? {
        switch self {
        case .completed: return "AI 助手已完成本轮分析。"
        case .confirmationPending: return "草稿已生成，请在二次确认弹窗中确认后再写入正式业务。"
        case .failed: return "本轮运行失败，请重试或调整问题。"
        case .blocked: return "本轮运行被安全策略阻止，请检查权限或问题内容。"
        case .cancelled: return "运行已取消。"
        case .exhausted: return "本轮可用轮次已用完，请稍后再试。"
        }
    }

    var iconName: String {
        switch self {
        case .completed: return "checkmark.circle.fill"
        case .confirmationPending: return "shield.lefthalf.filled.badge.checkmark"
        case .failed: return "exclamationmark.circle.fill"
        case .blocked: return "hand.raised.fill"
        case .cancelled: return "xmark.circle.fill"
        case .exhausted: return "arrow.uturn.backward.circle.fill"
        }
    }

    var tint: Color {
        switch self {
        case .completed: return ZhihuijiTheme.ColorToken.success
        case .confirmationPending: return ZhihuijiTheme.ColorToken.warning
        case .failed, .blocked, .cancelled, .exhausted: return ZhihuijiTheme.ColorToken.danger
        }
    }

    var voiceOverLabel: String {
        switch self {
        case .completed: return "运行已完成，结果可用"
        case .confirmationPending: return "运行已完成，等待你确认草稿后再写入正式业务"
        case .failed: return "运行失败"
        case .blocked: return "运行被阻止"
        case .cancelled: return "运行已取消"
        case .exhausted: return "本轮轮次耗尽"
        }
    }
}

extension AgentDraftStatus {
    static func displayLabel(_ status: String?) -> String {
        switch status?.lowercased() {
        case AgentDraftStatus.pendingConfirmation: return "待确认"
        case AgentDraftStatus.confirmed: return "已确认"
        case AgentDraftStatus.rejected: return "已拒绝"
        case AgentDraftStatus.cancelled: return "已取消"
        case AgentDraftStatus.expired: return "已过期"
        case AgentDraftStatus.archived: return "已归档"
        case AgentDraftStatus.active: return "进行中"
        default: return status ?? "草稿"
        }
    }

    static func tint(_ status: String?) -> Color {
        switch status?.lowercased() {
        case AgentDraftStatus.pendingConfirmation, AgentDraftStatus.active:
            return ZhihuijiTheme.ColorToken.primary
        case AgentDraftStatus.confirmed:
            return ZhihuijiTheme.ColorToken.success
        case AgentDraftStatus.rejected, AgentDraftStatus.cancelled, AgentDraftStatus.expired:
            return ZhihuijiTheme.ColorToken.danger
        case AgentDraftStatus.archived:
            return ZhihuijiTheme.ColorToken.warning
        default:
            return ZhihuijiTheme.ColorToken.textTertiary
        }
    }
}

private struct AgentUnknownResultBlock: View {
    let block: AgentResultBlock

    private var fallbackTitle: String {
        block.title?.nilIfBlank ?? "未识别的结构化结果"    }

    private var fallbackPreview: String {
        block.data?.previewText.nilIfBlank ?? "当前结果块没有可直接识别的字段。"
    }

    var body: some View {
        VStack(alignment: .leading, spacing: ZhihuijiTheme.Spacing.xs) {
            Text(fallbackTitle)
                .font(ZhihuijiTheme.Typography.bodyMedium)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
            Text("结构化结果类型：\(block.blockType)")
                .font(ZhihuijiTheme.Typography.captionSemibold)
                .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
            Text("原始字段预览")
                .font(ZhihuijiTheme.Typography.captionSemibold)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
            Text(fallbackPreview)
                .font(ZhihuijiTheme.Typography.caption)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                .fixedSize(horizontal: false, vertical: true)
        }
    }
}

private struct AgentLineChartBlock: View {
    let data: AgentLineChartBlockData

    private var series: [AgentChartSeries] {
        data.series.filter { !$0.data.isEmpty }
    }

    private var labels: [String] {
        data.labels
    }

    private var minValue: Double {
        min(0.0, series.flatMap(\.data).min() ?? 0.0)
    }

    private var maxValue: Double {
        max(0.0, series.flatMap(\.data).max() ?? 0.0)
    }

    private var range: Double {
        let delta = maxValue - minValue
        return delta == 0 ? 1 : delta
    }

    var body: some View {
        VStack(alignment: .leading, spacing: ZhihuijiTheme.Spacing.sm) {
            if series.isEmpty || labels.isEmpty {
                Text("本轮查询没有返回可绘制的趋势数据")
                    .font(ZhihuijiTheme.Typography.body)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
            } else {
                GeometryReader { proxy in
                    let size = proxy.size
                    ZStack {
                        RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.cardSmall, style: .continuous)
                            .fill(ZhihuijiTheme.ColorToken.surfaceWhite.opacity(0.78))

                        ForEach(0..<4, id: \.self) { index in
                            Path { path in
                                let y = size.height * CGFloat(index) / 3
                                path.move(to: CGPoint(x: 0, y: y))
                                path.addLine(to: CGPoint(x: size.width, y: y))
                            }
                            .stroke(ZhihuijiTheme.ColorToken.glassBorderSoft, lineWidth: 1)
                        }

                        ForEach(Array(series.enumerated()), id: \.offset) { index, item in
                            let points = points(for: item, in: size)
                            Path { path in
                                guard let first = points.first else { return }
                                path.move(to: first)
                                for point in points.dropFirst() {
                                    path.addLine(to: point)
                                }
                            }
                            .stroke(color(for: item.color, index: index), style: StrokeStyle(lineWidth: 2.5, lineCap: .round, lineJoin: .round))

                            ForEach(Array(points.enumerated()), id: \.offset) { _, point in
                                Circle()
                                    .fill(color(for: item.color, index: index))
                                    .frame(width: 7, height: 7)
                                    .position(point)
                            }
                        }
                    }
                }
                .frame(height: 168)

                HStack(alignment: .top) {
                    ForEach(Array(labels.enumerated()), id: \.offset) { _, label in
                        Text(label)
                            .font(ZhihuijiTheme.Typography.caption)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                            .frame(maxWidth: .infinity)
                            .multilineTextAlignment(.center)
                    }
                }

                VStack(alignment: .leading, spacing: 6) {
                    ForEach(Array(series.enumerated()), id: \.offset) { index, item in
                        HStack(spacing: 8) {
                            Circle()
                                .fill(color(for: item.color, index: index))
                                .frame(width: 8, height: 8)
                            Text(item.name)
                                .font(ZhihuijiTheme.Typography.caption)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                        }
                    }
                }
            }
        }
    }

    private func points(for series: AgentChartSeries, in size: CGSize) -> [CGPoint] {
        let values = Array(series.data.prefix(labels.count))
        guard !values.isEmpty else { return [] }

        return values.enumerated().map { index, value in
            let x = labels.count <= 1
                ? size.width / 2
                : size.width * CGFloat(index) / CGFloat(max(labels.count - 1, 1))
            let ratio = (maxValue - value) / range
            let y = size.height * CGFloat(ratio)
            return CGPoint(x: x, y: y.clamped(to: 0...size.height))
        }
    }

    private func color(for rawHex: String?, index: Int) -> Color {
        Color.chartColor(rawHex, fallbackIndex: index)
    }
}

private struct AgentBarChartBlock: View {
    let data: AgentBarChartBlockData

    private var series: [AgentChartSeries] {
        data.series.filter { !$0.data.isEmpty }
    }

    private var labels: [String] {
        data.labels
    }

    private var maxValue: Double {
        max(series.flatMap(\.data).max() ?? 0.0, 1.0)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: ZhihuijiTheme.Spacing.sm) {
            if series.isEmpty || labels.isEmpty {
                Text("本轮查询没有返回可绘制的柱状数据")
                    .font(ZhihuijiTheme.Typography.body)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
            } else {
                GeometryReader { proxy in
                    let groupHeight = max(proxy.size.height - 28, 40)
                    HStack(alignment: .bottom, spacing: 10) {
                        ForEach(Array(labels.enumerated()), id: \.offset) { labelIndex, label in
                            VStack(spacing: 8) {
                                HStack(alignment: .bottom, spacing: 5) {
                                    ForEach(Array(series.enumerated()), id: \.offset) { seriesIndex, item in
                                        let value = item.data.indices.contains(labelIndex) ? max(item.data[labelIndex], 0.0) : 0.0
                                        RoundedRectangle(cornerRadius: 6, style: .continuous)
                                            .fill(color(for: item.color, index: seriesIndex))
                                            .frame(
                                                maxWidth: .infinity,
                                                minHeight: value == 0 ? 4 : CGFloat(value / maxValue) * groupHeight,
                                                maxHeight: CGFloat(value / maxValue) * groupHeight
                                            )
                                    }
                                }
                                .frame(maxHeight: .infinity, alignment: .bottom)

                                Text(label)
                                    .font(ZhihuijiTheme.Typography.caption)
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                                    .multilineTextAlignment(.center)
                                    .frame(maxWidth: .infinity)
                            }
                        }
                    }
                    .padding(.horizontal, ZhihuijiTheme.Spacing.xs)
                }
                .frame(height: 184)
                .padding(.vertical, ZhihuijiTheme.Spacing.xs)
                .background(
                    ZhihuijiTheme.ColorToken.surfaceWhite.opacity(0.74),
                    in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.cardSmall, style: .continuous)
                )

                VStack(alignment: .leading, spacing: 6) {
                    ForEach(Array(series.enumerated()), id: \.offset) { index, item in
                        HStack(spacing: 8) {
                            RoundedRectangle(cornerRadius: 4, style: .continuous)
                                .fill(color(for: item.color, index: index))
                                .frame(width: 10, height: 10)
                            Text(item.name)
                                .font(ZhihuijiTheme.Typography.caption)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                        }
                    }
                }
            }
        }
    }

    private func color(for rawHex: String?, index: Int) -> Color {
        Color.chartColor(rawHex, fallbackIndex: index)
    }
}

private struct AgentDonutChartBlock: View {
    let data: AgentDonutChartBlockData

    private var segments: [AgentChartSegment] {
        data.segments.filter { $0.value > 0 }
    }

    private var total: Double {
        segments.reduce(0) { $0 + $1.value }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: ZhihuijiTheme.Spacing.sm) {
            if segments.isEmpty || total <= 0 {
                Text("本轮查询没有返回可绘制的占比数据")
                    .font(ZhihuijiTheme.Typography.body)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
            } else {
                HStack(spacing: ZhihuijiTheme.Spacing.md) {
                    ZStack {
                        Circle()
                            .stroke(ZhihuijiTheme.ColorToken.surfaceGray, lineWidth: 16)

                        ForEach(Array(segments.enumerated()), id: \.offset) { index, segment in
                            let start = startTrim(at: index)
                            let end = endTrim(at: index)
                            Circle()
                                .trim(from: start, to: end)
                                .stroke(
                                    color(for: segment.color, index: index),
                                    style: StrokeStyle(lineWidth: 16, lineCap: .round)
                                )
                                .rotationEffect(.degrees(-90))
                        }

                        VStack(spacing: 2) {
                            Text("合计")
                                .font(ZhihuijiTheme.Typography.caption)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                            Text(chartNumber(total))
                                .font(ZhihuijiTheme.Typography.bodyMedium)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                        }
                    }
                    .frame(width: 136, height: 136)

                    VStack(alignment: .leading, spacing: 8) {
                        ForEach(Array(segments.enumerated()), id: \.offset) { index, segment in
                            let percent = total == 0 ? 0 : segment.value / total * 100
                            HStack(alignment: .top, spacing: 8) {
                                Circle()
                                    .fill(color(for: segment.color, index: index))
                                    .frame(width: 9, height: 9)
                                    .padding(.top, 3)
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(segment.name)
                                        .font(ZhihuijiTheme.Typography.body)
                                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                                    Text("\(chartNumber(segment.value)) · \(String(format: "%.1f", percent))%")
                                        .font(ZhihuijiTheme.Typography.caption)
                                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private func startTrim(at index: Int) -> CGFloat {
        guard total > 0 else { return 0 }
        let previous = segments.prefix(index).reduce(0) { $0 + $1.value }
        return CGFloat(previous / total)
    }

    private func endTrim(at index: Int) -> CGFloat {
        guard total > 0 else { return 0 }
        let current = segments.prefix(index + 1).reduce(0) { $0 + $1.value }
        return CGFloat(current / total)
    }

    private func color(for rawHex: String?, index: Int) -> Color {
        Color.chartColor(rawHex, fallbackIndex: index)
    }

    private func chartNumber(_ value: Double) -> String {
        if value.rounded() == value {
            return String(Int(value))
        }
        return String(format: "%.2f", value)
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

extension JSONValue {
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

    var boolValue: Bool? {
        if case let .bool(value) = self { return value }
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
            return value
                .sorted { $0.key < $1.key }
                .map { "\($0.key)：\($0.value.displayText)" }
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

private extension AgentEvidenceCardBlockData.EvidenceItem {
    var displaySource: String? {
        source?.nilIfBlank.map { raw in
            "来源: \(raw.replacingOccurrences(of: "tool:", with: ""))"
        }
    }

    var auditSummary: String? {
        var parts: [String] = []
        if let toolCallId = toolCallId?.nilIfBlank {
            parts.append("调用 \(toolCallId)")
        }
        if let scope = queryWindow?.queryWindowSummary {
            parts.append("范围 \(scope)")
        }
        if isTruncated == true {
            parts.append("结果已截断")
        }
        return parts.isEmpty ? nil : parts.joined(separator: " · ")
    }
}

private extension JSONValue {
    var queryWindowSummary: String? {
        guard let objectValue else { return previewText.nilIfBlank }

        var parts: [String] = []
        if let ownerScope = objectValue["owner_scope"]?.stringValue?.nilIfBlank {
            parts.append(ownerScope == "current_owner" ? "当前账号" : ownerScope)
        }
        if let windowDays = objectValue["window_days"]?.numberValue {
            parts.append("近 \(Int(windowDays)) 天")
        }
        if let limit = objectValue["limit"]?.numberValue {
            parts.append("上限 \(Int(limit))")
        }
        if let rankLimit = objectValue["rank_limit"]?.numberValue {
            parts.append("排行 \(Int(rankLimit))")
        }
        if let lowStockLimit = objectValue["low_stock_limit"]?.numberValue {
            parts.append("低库存 \(Int(lowStockLimit))")
        }
        if objectValue["is_truncated"]?.boolValue == true {
            parts.append("已截断")
        }
        return parts.isEmpty ? previewText.nilIfBlank : parts.joined(separator: " · ")
    }
}

private extension Color {
    static let chartPalette: [Color] = [
        ZhihuijiTheme.ColorToken.primary,
        ZhihuijiTheme.ColorToken.success,
        ZhihuijiTheme.ColorToken.warning,
        Color(red: 0.58, green: 0.40, blue: 0.98),
        Color(red: 0.18, green: 0.68, blue: 0.88),
    ]

    static func chartColor(_ rawHex: String?, fallbackIndex: Int) -> Color {
        if let rawHex, let parsed = Color(hex: rawHex) {
            return parsed
        }
        return chartPalette[fallbackIndex % chartPalette.count]
    }

    init?(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        guard [6, 8].contains(hex.count), let value = UInt64(hex, radix: 16) else {
            return nil
        }

        let red: Double
        let green: Double
        let blue: Double
        let alpha: Double

        if hex.count == 8 {
            red = Double((value & 0xFF00_0000) >> 24) / 255
            green = Double((value & 0x00FF_0000) >> 16) / 255
            blue = Double((value & 0x0000_FF00) >> 8) / 255
            alpha = Double(value & 0x0000_00FF) / 255
        } else {
            red = Double((value & 0xFF0000) >> 16) / 255
            green = Double((value & 0x00FF00) >> 8) / 255
            blue = Double(value & 0x0000FF) / 255
            alpha = 1
        }

        self.init(red: red, green: green, blue: blue, opacity: alpha)
    }
}

private extension CGFloat {
    func clamped(to limits: ClosedRange<CGFloat>) -> CGFloat {
        Swift.min(Swift.max(self, limits.lowerBound), limits.upperBound)
    }
}
