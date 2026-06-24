import SwiftUI

struct AgentWorkbenchView: View {
    @Environment(\.appEnvironment) private var env
    @StateObject private var viewModel = AgentWorkbenchViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                headerSection
                if let errorMessage = viewModel.errorMessage {
                    EmptyStateView(title: "工作台加载失败", message: errorMessage)
                }
                summarySection
                kpiSection
                quickEntrySection
                pendingDraftSection
                recentConversationSection
                riskAlertSection
                capabilitySection
            }
            .padding(20)
        }
        .navigationTitle("AI 工作台")
        .task {
            await viewModel.load(using: env.apiClient)
        }
    }

    private var headerSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            if let workbench = viewModel.workbench {
                Text(workbench.greeting)
                    .font(ZhihuijiTheme.Typography.pageTitle)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)

                if let summary = workbench.todaySummary?.nilIfBlank {
                    Text(summary)
                        .font(ZhihuijiTheme.Typography.body)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                        .fixedSize(horizontal: false, vertical: true)
                }

                if let status = workbench.status?.nilIfBlank {
                    StatusChip(title: status, tint: ZhihuijiTheme.ColorToken.primary)
                }
            } else if viewModel.isLoading {
                Text("正在加载工作台...")
                    .font(ZhihuijiTheme.Typography.body)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
            } else {
                Text("AI 工作台")
                    .font(ZhihuijiTheme.Typography.pageTitle)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                Text("先看经营提示，再进入会话或处理待办。")
                    .font(ZhihuijiTheme.Typography.body)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
            }
        }
        .padding(16)
        .glassCard()
    }

    private var summarySection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("今日概览")
                .font(ZhihuijiTheme.Typography.sectionTitle)

            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                MetricCard(
                    title: "待办草稿",
                    value: "\(viewModel.workbench?.pendingDrafts.count ?? 0)",
                    subtitle: "待处理",
                    tint: ZhihuijiTheme.ColorToken.warning
                )
                MetricCard(
                    title: "最近会话",
                    value: "\(viewModel.workbench?.recentConversations.count ?? 0)",
                    subtitle: "近期活跃",
                    tint: ZhihuijiTheme.ColorToken.primary
                )
                MetricCard(
                    title: "风险提醒",
                    value: "\(viewModel.workbench?.riskAlerts.count ?? 0)",
                    subtitle: "需关注",
                    tint: ZhihuijiTheme.ColorToken.danger
                )
                MetricCard(
                    title: "快捷入口",
                    value: "\(viewModel.workbench?.quickQuestions.count ?? 0)",
                    subtitle: "常用提问",
                    tint: ZhihuijiTheme.ColorToken.primaryBright
                )
            }
        }
    }

    @ViewBuilder
    private var kpiSection: some View {
        if let workbench = viewModel.workbench, !workbench.kpiCards.isEmpty {
            VStack(alignment: .leading, spacing: 12) {
                Text("经营指标")
                    .font(ZhihuijiTheme.Typography.sectionTitle)

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
        }
    }

    @ViewBuilder
    private var quickEntrySection: some View {
        if let workbench = viewModel.workbench, !workbench.quickQuestions.isEmpty {
            VStack(alignment: .leading, spacing: 12) {
                Text("快捷入口")
                    .font(ZhihuijiTheme.Typography.sectionTitle)

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 10) {
                        ForEach(workbench.quickQuestions, id: \.self) { question in
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
                Text("待办草稿")
                    .font(ZhihuijiTheme.Typography.sectionTitle)

                ForEach(drafts) { draft in
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(draft.title)
                                .font(ZhihuijiTheme.Typography.bodyMedium)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                            Text(draft.draftType)
                                .font(ZhihuijiTheme.Typography.caption)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                        }
                        Spacer()
                        Text(draft.createdAt.dateTimeText)
                            .font(ZhihuijiTheme.Typography.caption)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                    }
                    .padding(14)
                    .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
                }
            }
        }
    }

    @ViewBuilder
    private var recentConversationSection: some View {
        if let conversations = viewModel.workbench?.recentConversations, !conversations.isEmpty {
            VStack(alignment: .leading, spacing: 12) {
                Text("最近会话")
                    .font(ZhihuijiTheme.Typography.sectionTitle)

                ForEach(conversations) { conversation in
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(conversation.title)
                                .font(ZhihuijiTheme.Typography.bodyMedium)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                            if let count = conversation.messageCount {
                                Text("\(count) 条消息")
                                    .font(ZhihuijiTheme.Typography.caption)
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                            }
                        }
                        Spacer()
                        if let lastAt = conversation.lastMessageAt {
                            Text(lastAt.dateTimeText)
                                .font(ZhihuijiTheme.Typography.caption)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                        }
                    }
                    .padding(14)
                    .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
                }
            }
        }
    }

    @ViewBuilder
    private var riskAlertSection: some View {
        if let alerts = viewModel.workbench?.riskAlerts, !alerts.isEmpty {
            VStack(alignment: .leading, spacing: 12) {
                Text("风险提醒")
                    .font(ZhihuijiTheme.Typography.sectionTitle)

                ForEach(alerts) { alert in
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
                                .fixedSize(horizontal: false, vertical: true)
                        }
                        Spacer()
                    }
                    .padding(12)
                    .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
                }
            }
        }
    }

    @ViewBuilder
    private var capabilitySection: some View {
        if let capabilities = viewModel.workbench?.capabilities, !capabilities.isEmpty {
            VStack(alignment: .leading, spacing: 12) {
                Text("能力清单")
                    .font(ZhihuijiTheme.Typography.sectionTitle)

                ForEach(capabilities) { capability in
                    HStack(alignment: .top, spacing: 12) {
                        Circle()
                            .fill(ZhihuijiTheme.ColorToken.primary.opacity(0.14))
                            .frame(width: 30, height: 30)
                            .overlay(
                                Image(systemName: "sparkles")
                                    .font(ZhihuijiTheme.Typography.caption)
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                            )
                        VStack(alignment: .leading, spacing: 4) {
                            Text(capability.title)
                                .font(ZhihuijiTheme.Typography.bodyMedium)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                            Text(capability.description)
                                .font(ZhihuijiTheme.Typography.caption)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                        Spacer()
                    }
                    .padding(12)
                    .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
                }
            }
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
}

@MainActor
final class AgentWorkbenchViewModel: ObservableObject {
    @Published var isLoading = false
    @Published var workbench: AgentWorkbench?
    @Published var errorMessage: String?

    func load(using client: APIClient) async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }

        do {
            workbench = try await client.fetchAgentWorkbench()
        } catch {
            workbench = nil
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }
}
