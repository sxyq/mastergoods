import SwiftUI

struct AppRouter: View {
    @EnvironmentObject private var session: AppSession
    @Environment(\.appEnvironment) private var env

    var body: some View {
        Group {
            switch session.phase {
            case .booting:
                LoadingStateView(message: "正在启动智慧记...")
            case .ready:
                switch session.auth {
                case .loggedOut:
                    LoginView()
                case .loggedIn:
                    if let errorMessage = session.storeLoadError {
                        StoreHydrationErrorView(message: errorMessage) {
                            Task {
                                await session.hydrateStore(using: env.apiClient)
                            }
                        } onLogout: {
                            session.logout()
                        }
                    } else if session.currentStore == nil {
                        LoadingStateView(message: "正在同步门店与权限...")
                    } else {
                        RootTabView()
                    }
                }
            }
        }
        .zhihuijiBackground()
        .task(id: session.auth) {
            guard case .loggedIn = session.auth, session.currentStore == nil else { return }
            await session.hydrateStore(using: env.apiClient)
        }
        .sheet(item: $session.accessIssue) { issue in
            AccessIssueView(issue: issue) {
                session.clearAccessIssue()
            }
        }
    }
}

private struct StoreHydrationErrorView: View {
    let message: String
    let onRetry: () -> Void
    let onLogout: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            EmptyStateView(
                title: "门店与权限同步失败",
                message: message
            )

            PrimaryGlassButton(
                title: "重试同步",
                systemImage: "arrow.clockwise",
                action: onRetry
            )

            Button(action: onLogout) {
                Text("退出登录")
                    .font(ZhihuijiTheme.Typography.bodyMedium)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.warning)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(
                        Color.white.opacity(0.56),
                        in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.pill, style: .continuous)
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.pill, style: .continuous)
                            .stroke(Color.white.opacity(0.48), lineWidth: ZhihuijiTheme.Stroke.hairline)
                    )
            }
            .buttonStyle(.plain)
        }
        .padding(20)
    }
}

private struct RootTabView: View {
    @EnvironmentObject private var session: AppSession

    private var hasVisibleTabs: Bool {
        !TopLevelTabKey.visibleTabs(for: session.permissions).isEmpty
    }

    var body: some View {
        let visibleTabs = TopLevelTabKey.visibleTabs(for: session.permissions)

        if hasVisibleTabs {
            TabView {
                ForEach(visibleTabs) { tab in
                    tabRootView(for: tab)
                        .tabItem { Label(tab.title, systemImage: tab.systemImage) }
                }
            }
            .tint(ZhihuijiTheme.ColorToken.primary)
        } else {
            NoAvailableModuleView()
        }
    }

    @ViewBuilder
    private func tabRootView(for tab: TopLevelTabKey) -> some View {
        switch tab {
        case .dashboard:
            TopLevelNavigationShell {
                DashboardView()
            }
        case .documents:
            TopLevelNavigationShell {
                DocumentsHomeView()
            }
        case .archives:
            TopLevelNavigationShell {
                ArchivesHomeView()
            }
        case .reports:
            TopLevelNavigationShell {
                ReportsView()
            }
        case .agent:
            TopLevelNavigationShell {
                AgentChatView()
            }
        }
    }
}

enum TopLevelTabKey: String, CaseIterable, Identifiable {
    case dashboard
    case documents
    case archives
    case reports
    case agent

    var id: String { rawValue }

    var title: String {
        switch self {
        case .dashboard: return "首页"
        case .documents: return "单据"
        case .archives: return "档案"
        case .reports: return "报表"
        case .agent: return "助手"
        }
    }

    var systemImage: String {
        switch self {
        case .dashboard: return "house.fill"
        case .documents: return "doc.text.fill"
        case .archives: return "shippingbox.fill"
        case .reports: return "chart.xyaxis.line"
        case .agent: return "sparkles"
        }
    }

    func isVisible(for permissions: Set<Permission>) -> Bool {
        switch self {
        case .dashboard:
            return permissions.contains(.dashboardView)
        case .documents:
            return permissions.contains(where: {
                [.salesView, .purchaseView, .financeView, .inventoryView].contains($0)
            })
        case .archives:
            return permissions.contains(.archivesView)
        case .reports:
            return permissions.contains(.reportsView)
        case .agent:
            return permissions.contains(.agentView)
        }
    }

    static func visibleTabs(for permissions: Set<Permission>) -> [TopLevelTabKey] {
        allCases.filter { $0.isVisible(for: permissions) }
    }
}

private struct TopLevelNavigationShell<Content: View>: View {
    let content: Content

    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    var body: some View {
        NavigationStack {
                content
                .toolbar {
#if os(iOS)
                    ToolbarItem(placement: .topBarTrailing) {
                        NavigationLink {
                            SettingsView()
                        } label: {
                            Image(systemName: "gearshape.fill")
                                .font(ZhihuijiTheme.Typography.bodyMedium)
                        }
                    }
#else
                    ToolbarItem {
                        NavigationLink {
                            SettingsView()
                        } label: {
                            Image(systemName: "gearshape.fill")
                                .font(ZhihuijiTheme.Typography.bodyMedium)
                        }
                    }
#endif
                }
        }
    }
}

private struct NoAvailableModuleView: View {
    @EnvironmentObject private var session: AppSession

    var body: some View {
        NavigationStack {
            VStack(spacing: 18) {
                Spacer()

                Circle()
                    .fill(ZhihuijiTheme.ColorToken.warning.opacity(0.14))
                    .frame(width: 72, height: 72)
                    .overlay(
                        Image(systemName: "person.crop.circle.badge.exclamationmark")
                            .font(ZhihuijiTheme.Typography.pageTitle)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.warning)
                    )

                VStack(spacing: 8) {
                    Text("暂无可用模块")
                        .font(ZhihuijiTheme.Typography.pageTitle)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)

                    Text("当前账号已经登录，但没有任何可显示的移动端模块权限。请联系店长调整角色或权限。")
                        .font(ZhihuijiTheme.Typography.body)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                        .multilineTextAlignment(.center)
                }

                if let store = session.currentStore {
                    VStack(alignment: .leading, spacing: 8) {
                        Text(store.storeName)
                            .font(ZhihuijiTheme.Typography.cardTitle)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                        Text("\(store.currentUserName) · \(store.role.label)")
                            .font(ZhihuijiTheme.Typography.body)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                        Text("已同步权限 \(session.permissions.count) 项")
                            .font(ZhihuijiTheme.Typography.captionSemibold)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(16)
                    .glassCard()
                }

                NavigationLink {
                    SettingsView()
                } label: {
                    HStack {
                        Image(systemName: "gearshape.fill")
                        Text("进入设置")
                            .font(ZhihuijiTheme.Typography.bodyMedium)
                    }
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(
                        LinearGradient(
                            colors: [ZhihuijiTheme.ColorToken.primaryBright, ZhihuijiTheme.ColorToken.primary],
                            startPoint: .leading,
                            endPoint: .trailing
                        ),
                        in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.pill, style: .continuous)
                    )
                }
                .buttonStyle(.plain)

                Button {
                    session.logout()
                } label: {
                    Text("退出登录")
                        .font(ZhihuijiTheme.Typography.bodyMedium)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.warning)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(
                            Color.white.opacity(0.56),
                            in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.pill, style: .continuous)
                        )
                        .overlay(
                            RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.pill, style: .continuous)
                                .stroke(Color.white.opacity(0.48), lineWidth: ZhihuijiTheme.Stroke.hairline)
                        )
                }
                .buttonStyle(.plain)

                Spacer()
            }
            .padding(24)
            .navigationTitle("访问受限")
            .zhihuijiBackground()
        }
    }
}

private struct AccessIssueView: View {
    let issue: AccessIssue
    let onDismiss: () -> Void

    var body: some View {
        NavigationStack {
            VStack(spacing: 18) {
                Spacer()

                Circle()
                    .fill(ZhihuijiTheme.ColorToken.warning.opacity(0.14))
                    .frame(width: 72, height: 72)
                    .overlay(
                        Image(systemName: "lock.shield.fill")
                            .font(ZhihuijiTheme.Typography.pageTitle)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.warning)
                    )

                VStack(spacing: 8) {
                    Text(issue.title)
                        .font(ZhihuijiTheme.Typography.pageTitle)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                    Text(issue.message)
                        .font(ZhihuijiTheme.Typography.body)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                        .multilineTextAlignment(.center)
                }

                Button(action: onDismiss) {
                    Text("我知道了")
                        .font(ZhihuijiTheme.Typography.bodyMedium)
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(
                            LinearGradient(
                                colors: [ZhihuijiTheme.ColorToken.primaryBright, ZhihuijiTheme.ColorToken.primary],
                                startPoint: .leading,
                                endPoint: .trailing
                            ),
                            in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.pill, style: .continuous)
                        )
                }
                .buttonStyle(.plain)

                Spacer()
            }
            .padding(24)
            .navigationTitle("访问受限")
            .glassCard()
            .padding(20)
            .background(Color.clear)
        }
        .presentationDetents([.medium])
        .presentationDragIndicator(.visible)
    }
}
