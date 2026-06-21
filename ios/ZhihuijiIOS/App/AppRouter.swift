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

    var body: some View {
        TabView {
            if session.hasPermission(.dashboardView) {
                NavigationStack { DashboardView() }
                    .tabItem { Label("首页", systemImage: "house.fill") }
            }
            if session.hasAnyPermission([.salesView, .purchaseView, .financeView]) {
                NavigationStack { DocumentsHomeView() }
                    .tabItem { Label("单据", systemImage: "doc.text.fill") }
            }
            if session.hasPermission(.archivesView) {
                NavigationStack { ArchivesHomeView() }
                    .tabItem { Label("档案", systemImage: "shippingbox.fill") }
            }
            if session.hasPermission(.inventoryView) {
                NavigationStack { InventorySnapshotView() }
                    .tabItem { Label("库存", systemImage: "cube.box.fill") }
            }
            if session.hasPermission(.financeView) {
                NavigationStack { FinanceRecordView() }
                    .tabItem { Label("资金", systemImage: "creditcard.fill") }
            }
            if session.hasPermission(.reportsView) {
                NavigationStack { ReportsView() }
                    .tabItem { Label("报表", systemImage: "chart.xyaxis.line") }
            }
            if session.hasPermission(.agentView) {
                NavigationStack { AgentChatView() }
                    .tabItem { Label("AI", systemImage: "sparkles") }
            }
            NavigationStack { SettingsView() }
                .tabItem { Label("设置", systemImage: "gearshape.fill") }
        }
        .tint(ZhihuijiTheme.ColorToken.primary)
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
