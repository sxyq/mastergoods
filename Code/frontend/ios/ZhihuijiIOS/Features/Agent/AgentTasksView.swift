import SwiftUI

struct AgentTasksView: View {
    @Environment(\.appEnvironment) private var env
    @EnvironmentObject private var session: AppSession
    @StateObject private var viewModel = AgentTasksViewModel()

    private var canWrite: Bool {
        AgentAccessPolicy.resolve(for: session.permissions).canWriteAgent
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                headerSection
                summarySection
                if !canWrite {
                    EmptyStateView(title: "当前账号仅可查看", message: "任务和通知可浏览，但标记已读需要 agent:write 权限。")
                }
                if let errorMessage = viewModel.errorMessage {
                    EmptyStateView(title: "加载失败", message: errorMessage)
                }
                tasksSection
                notificationsSection
            }
            .padding(20)
        }
        .navigationTitle("AI 任务与通知")
        .task {
            await viewModel.load(using: env.apiClient)
        }
    }

    private var headerSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("任务与通知")
                        .font(ZhihuijiTheme.Typography.pageTitle)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                    Text("集中查看 AI 任务执行进度与风险/结果通知。")
                        .font(ZhihuijiTheme.Typography.body)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                }
                Spacer()
                PrimaryGlassButton(
                    title: viewModel.isLoading ? "刷新中..." : "刷新",
                    systemImage: "arrow.clockwise",
                    disabled: viewModel.isLoading
                ) {
                    Task { await viewModel.load(using: env.apiClient) }
                }
                .frame(maxWidth: 160)
            }
        }
        .padding(16)
        .glassCard()
    }

    private var summarySection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("概览")
                .font(ZhihuijiTheme.Typography.sectionTitle)

            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                MetricCard(
                    title: "任务总数",
                    value: "\(viewModel.tasks.count)",
                    subtitle: "已加载",
                    tint: ZhihuijiTheme.ColorToken.primary
                )
                MetricCard(
                    title: "进行中任务",
                    value: "\(viewModel.runningTaskCount)",
                    subtitle: "未完成",
                    tint: ZhihuijiTheme.ColorToken.warning
                )
                MetricCard(
                    title: "通知总数",
                    value: "\(viewModel.notifications.count)",
                    subtitle: "全部",
                    tint: ZhihuijiTheme.ColorToken.primaryBright
                )
                MetricCard(
                    title: "未读通知",
                    value: "\(viewModel.unreadNotificationCount)",
                    subtitle: "待处理",
                    tint: ZhihuijiTheme.ColorToken.danger
                )
            }
        }
    }

    private var tasksSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("任务列表")
                .font(ZhihuijiTheme.Typography.sectionTitle)

            if viewModel.tasks.isEmpty {
                EmptyStateView(title: "暂无任务", message: "当前没有挂起中的 AI 任务。")
            } else {
                ForEach(viewModel.tasks) { task in
                    taskRow(task)
                }
            }
        }
    }

    private func taskRow(_ task: AgentTask) -> some View {
        HStack(alignment: .top, spacing: 12) {
            Circle()
                .fill(statusTint(task.status).opacity(0.16))
                .frame(width: 36, height: 36)
                .overlay(
                    Image(systemName: statusIcon(task.status))
                        .font(ZhihuijiTheme.Typography.bodyMedium)
                        .foregroundStyle(statusTint(task.status))
                )

            VStack(alignment: .leading, spacing: 6) {
                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(task.title)
                            .font(ZhihuijiTheme.Typography.cardTitle)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                        Text(task.statusLabel ?? task.status ?? task.taskType)
                            .font(ZhihuijiTheme.Typography.caption)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                    }
                    Spacer()
                    if let progress = task.progress {
                        VStack(alignment: .trailing, spacing: 4) {
                            Text("\(progress)%")
                                .font(ZhihuijiTheme.Typography.captionSemibold)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                            ProgressView(value: Double(progress), total: 100)
                                .tint(ZhihuijiTheme.ColorToken.primary)
                                .frame(width: 80)
                        }
                    }
                }

                if let inputText = task.inputText?.nilIfBlank {
                    Text(inputText)
                        .font(ZhihuijiTheme.Typography.caption)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                        .lineLimit(2)
                }

                HStack(spacing: 10) {
                    StatusChip(title: task.taskType, tint: ZhihuijiTheme.ColorToken.primary)
                    if let createdAt = task.createdAt {
                        Text("创建于 \(createdAt.dateTimeText)")
                            .font(ZhihuijiTheme.Typography.caption)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                    }
                    if let completedAt = task.completedAt {
                        Text("完成于 \(completedAt.dateTimeText)")
                            .font(ZhihuijiTheme.Typography.caption)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.success)
                    }
                }
            }
        }
        .padding(14)
        .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
    }

    private var notificationsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("通知列表")
                    .font(ZhihuijiTheme.Typography.sectionTitle)
                Spacer()
                if viewModel.unreadNotificationCount > 0, canWrite {
                    Button {
                        Task { await viewModel.markAllRead(using: env.apiClient) }
                    } label: {
                        HStack(spacing: 6) {
                            Image(systemName: "checkmark.circle")
                            Text("全部已读")
                                .font(ZhihuijiTheme.Typography.captionSemibold)
                        }
                        .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                    }
                    .buttonStyle(.plain)
                }
            }

            if viewModel.notifications.isEmpty {
                EmptyStateView(title: "暂无通知", message: "这里会展示 AI 风险提醒和执行结果。")
            } else {
                ForEach(viewModel.notifications) { notification in
                    Button {
                        guard canWrite else { return }
                        Task { await viewModel.markRead(notification, using: env.apiClient) }
                    } label: {
                        notificationRow(notification)
                    }
                    .buttonStyle(.plain)
                    .disabled(!canWrite)
                    .opacity(canWrite ? 1 : 0.78)
                }
            }
        }
    }

    private func notificationRow(_ notification: AgentNotification) -> some View {
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
                HStack(alignment: .top) {
                    Text(notification.title)
                        .font(ZhihuijiTheme.Typography.bodyMedium)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                    Spacer()
                    if notification.isRead != true {
                        Circle()
                            .fill(ZhihuijiTheme.ColorToken.danger)
                            .frame(width: 8, height: 8)
                    }
                }
                Text(notification.body)
                    .font(ZhihuijiTheme.Typography.caption)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
                Text(notification.createdAt.dateTimeText)
                    .font(ZhihuijiTheme.Typography.caption)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
            }
        }
        .padding(14)
        .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
    }

    private func statusTint(_ status: String?) -> Color {
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

    private func statusIcon(_ status: String?) -> String {
        switch status?.lowercased() {
        case "failed", "error":
            return "exclamationmark.circle.fill"
        case "completed", "success", "done":
            return "checkmark.circle.fill"
        default:
            return "ellipsis.circle.fill"
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

@MainActor
final class AgentTasksViewModel: ObservableObject {
    @Published var isLoading = false
    @Published var tasks: [AgentTask] = []
    @Published var notifications: [AgentNotification] = []
    @Published var errorMessage: String?

    var runningTaskCount: Int {
        tasks.filter { status in
            let raw = status.status?.lowercased()
            return raw != "completed" && raw != "success" && raw != "done" && raw != "failed" && raw != "cancelled"
        }.count
    }

    var unreadNotificationCount: Int {
        notifications.filter { $0.isRead != true }.count
    }

    func load(using client: APIClient) async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }

        async let tasksTask = capture { try await client.fetchAgentTasks() }
        async let notificationsTask = capture { try await client.fetchAgentNotifications(unreadOnly: false) }

        let tasksResult = await tasksTask
        let notificationsResult = await notificationsTask

        var failures: [String] = []

        switch tasksResult {
        case let .success(value):
            tasks = value.sorted(by: { ($0.createdAt ?? 0) > ($1.createdAt ?? 0) })
        case .failure:
            tasks = []
            failures.append("任务")
        }

        switch notificationsResult {
        case let .success(value):
            notifications = value.sorted(by: { $0.createdAt > $1.createdAt })
        case .failure:
            notifications = []
            failures.append("通知")
        }

        errorMessage = failures.isEmpty ? nil : "以下分区暂未成功拉取：\(failures.joined(separator: "、"))"
    }

    func markRead(_ notification: AgentNotification, using client: APIClient) async {
        guard notification.isRead != true else { return }
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
            errorMessage = nil
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    func markAllRead(using client: APIClient) async {
        let unread = notifications.filter { $0.isRead != true }
        for notification in unread {
            await markRead(notification, using: client)
        }
    }

    private func capture<T>(_ operation: @escaping () async throws -> T) async -> Result<T, Error> {
        do {
            return .success(try await operation())
        } catch {
            return .failure(error)
        }
    }
}
