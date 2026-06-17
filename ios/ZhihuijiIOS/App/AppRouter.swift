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
                    RootTabView()
                }
            }
        }
        .zhihuijiBackground()
        .task(id: session.auth) {
            guard case .loggedIn = session.auth, session.currentStore == nil else { return }
            await session.hydrateStore(using: env.apiClient)
        }
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
                NavigationStack { ProductListView() }
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
