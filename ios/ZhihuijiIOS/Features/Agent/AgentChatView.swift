import SwiftUI

struct AgentChatView: View {
    @Environment(\.appEnvironment) private var env
    @StateObject private var viewModel = AgentViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                header
                workbenchSection
                conversationsSection
                messagesSection
                latestRunSection
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
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("AI 助手")
                .font(.system(size: 28, weight: .bold))
                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
            Text("延续安卓移动端的助手气质：先看经营提示，再进会话，再发问。")
                .font(.system(size: 14))
                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)

            if let errorMessage = viewModel.errorMessage {
                Text(errorMessage)
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(ZhihuijiTheme.ColorToken.danger)
            }
        }
    }

    @ViewBuilder
    private var workbenchSection: some View {
        if let workbench = viewModel.workbench {
            VStack(alignment: .leading, spacing: 14) {
                Text(workbench.greeting)
                    .font(.system(size: 18, weight: .semibold))
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

    private var conversationsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("最近会话")
                    .font(.system(size: 18, weight: .semibold))
                Spacer()
                if viewModel.isLoading {
                    ProgressView()
                        .scaleEffect(0.8)
                }
            }

            if viewModel.conversations.isEmpty {
                EmptyStateView(title: "暂无会话", message: "直接发一个问题，系统会自动创建新会话。")
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 10) {
                        ForEach(viewModel.conversations) { conversation in
                            Button {
                                Task { await viewModel.selectConversation(conversation.id, client: env.apiClient) }
                            } label: {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(conversation.title)
                                        .font(.system(size: 14, weight: .semibold))
                                        .lineLimit(1)
                                    Text((conversation.lastMessageAt ?? conversation.updatedAt).dateTimeText)
                                        .font(.system(size: 11))
                                }
                                .foregroundStyle(viewModel.selectedConversationId == conversation.id ? .white : ZhihuijiTheme.ColorToken.textPrimary)
                                .padding(.horizontal, 14)
                                .padding(.vertical, 12)
                                .frame(width: 180, alignment: .leading)
                                .background(
                                    (viewModel.selectedConversationId == conversation.id
                                        ? LinearGradient(colors: [ZhihuijiTheme.ColorToken.primaryBright, ZhihuijiTheme.ColorToken.primary], startPoint: .leading, endPoint: .trailing)
                                        : LinearGradient(colors: [Color.white.opacity(0.58), Color.white.opacity(0.58)], startPoint: .leading, endPoint: .trailing)
                                    ),
                                    in: RoundedRectangle(cornerRadius: 14, style: .continuous)
                                )
                            }
                            .buttonStyle(.plain)
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

            if viewModel.messages.isEmpty {
                EmptyStateView(title: "还没有消息", message: "先发起一个问题，助手会把会话沉淀下来。")
            } else {
                VStack(spacing: 12) {
                    ForEach(viewModel.messages) { message in
                        HStack {
                            if message.isAssistant {
                                assistantBubble(message)
                                Spacer(minLength: 32)
                            } else {
                                Spacer(minLength: 32)
                                userBubble(message)
                            }
                        }
                    }
                }
            }
        }
    }

    @ViewBuilder
    private var latestRunSection: some View {
        if let response = viewModel.latestResponse {
            VStack(alignment: .leading, spacing: 12) {
                Text("本次运行摘要")
                    .font(.system(size: 18, weight: .semibold))
                if let planSummary = response.planSummary, !planSummary.isEmpty {
                    Text(planSummary)
                        .font(.system(size: 13))
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                }

                if !response.toolCalls.isEmpty {
                    ForEach(response.toolCalls.prefix(5)) { tool in
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(tool.toolName)
                                    .font(.system(size: 14, weight: .semibold))
                                Text(tool.resultSummary ?? tool.inputSummary ?? "已执行")
                                    .font(.system(size: 12))
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                            }
                            Spacer()
                            Text(tool.status ?? "done")
                                .font(.system(size: 12, weight: .semibold))
                                .foregroundStyle(tintForStatus(tool.status))
                        }
                        .padding(12)
                        .glassCard(cornerRadius: 12)
                    }
                }
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
            TextField("问点什么，比如今天低库存有哪些？", text: $viewModel.draftQuestion, axis: .vertical)
                .fieldBackground()
            PrimaryGlassButton(
                title: viewModel.isSending ? "发送中..." : "发送",
                systemImage: "paperplane.fill",
                disabled: viewModel.isSending || viewModel.draftQuestion.nilIfBlank == nil
            ) {
                Task { await viewModel.send(using: env.apiClient) }
            }
        }
        .padding(16)
        .glassCard()
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

    private func assistantBubble(_ message: AgentMessage) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(message.content)
                .font(.system(size: 14))
                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
                .background(Color.white.opacity(0.62), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            Text(message.createdAt.dateTimeText)
                .font(.system(size: 11))
                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
        }
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
        case "failed", "error":
            return ZhihuijiTheme.ColorToken.danger
        case "completed", "success":
            return ZhihuijiTheme.ColorToken.success
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
}
