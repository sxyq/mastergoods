import SwiftUI

struct SettingsView: View {
    @EnvironmentObject private var session: AppSession
    @Environment(\.appEnvironment) private var env

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("系统设置")
                    .font(.system(size: 28, weight: .bold))
                if let store = session.currentStore {
                    VStack(alignment: .leading, spacing: 8) {
                        Text(store.storeName)
                            .font(.system(size: 18, weight: .semibold))
                        Text("\(store.role.label) · \(store.currentUserName)")
                            .font(.system(size: 13))
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                    }
                    .padding(16)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(.white.opacity(0.58), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous)
                            .stroke(Color.white.opacity(0.5), lineWidth: 0.5)
                    )
                }
                if session.hasPermission(.usersManage) {
                    NavigationLink("店员与权限") {
                        StaffManagementView()
                    }
                    .buttonStyle(.plain)
                    .padding(16)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(.white.opacity(0.58), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous)
                            .stroke(Color.white.opacity(0.5), lineWidth: 0.5)
                    )
                }
                EmptyStateView(title: "设置壳已建立", message: "后续接真实门店成员、数据库状态和同步设置。")
                PrimaryGlassButton(title: "退出登录", systemImage: "rectangle.portrait.and.arrow.right") {
                    Task {
                        try? await env.apiClient.logout()
                        session.logout()
                    }
                }
            }
            .padding(20)
        }
        .navigationTitle("设置")
    }
}
