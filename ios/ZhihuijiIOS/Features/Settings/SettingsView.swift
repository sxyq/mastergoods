import SwiftUI

struct SettingsView: View {
    @EnvironmentObject private var session: AppSession
    @EnvironmentObject private var environmentStore: AppEnvironmentStore
    @Environment(\.appEnvironment) private var env

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                headerCard
                statusOverview
                managementSection
                syncSection
                mediaSection
                permissionSection
                securitySection
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

    private var statusOverview: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("状态概览")
                .font(ZhihuijiTheme.Typography.sectionTitle)

            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                MetricCard(
                    title: "当前角色",
                    value: session.currentStore?.role.label ?? "--",
                    subtitle: session.currentStore?.title ?? "未同步",
                    tint: ZhihuijiTheme.ColorToken.primary
                )
                MetricCard(
                    title: "可用权限",
                    value: "\(session.permissions.count)",
                    subtitle: "真实门店权限",
                    tint: ZhihuijiTheme.ColorToken.success
                )
                MetricCard(
                    title: "门店成员",
                    value: session.currentStore.map { "\($0.memberCount)" } ?? "--",
                    subtitle: "已同步到本地",
                    tint: ZhihuijiTheme.ColorToken.primaryBright
                )
                MetricCard(
                    title: "同步状态",
                    value: session.isAuthenticated ? "在线" : "离线",
                    subtitle: environmentStore.current.apiBaseURL.host ?? "本地配置",
                    tint: session.isAuthenticated ? ZhihuijiTheme.ColorToken.success : ZhihuijiTheme.ColorToken.warning
                )
            }
        }
    }

    private var headerCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("系统设置")
                .font(ZhihuijiTheme.Typography.pageTitle)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)

            if let store = session.currentStore {
                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 6) {
                        Text(store.storeName)
                            .font(ZhihuijiTheme.Typography.cardTitle)
                        Text(store.currentUserName)
                            .font(ZhihuijiTheme.Typography.bodyMedium)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                        Text(store.role.label)
                            .font(ZhihuijiTheme.Typography.caption)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                    }
                    Spacer()
                    StatusChip(title: store.role.label, tint: ZhihuijiTheme.ColorToken.primary)
                }
            } else {
                Text("正在同步当前门店与成员信息。")
                    .font(ZhihuijiTheme.Typography.body)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
            }
        }
        .padding(18)
        .glassCard()
    }

    private var managementSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("门店管理")
                .font(ZhihuijiTheme.Typography.sectionTitle)

            if session.hasPermission(.usersManage) {
                NavigationLink {
                    StaffManagementView()
                } label: {
                    settingsEntryCard(
                        title: "店员与权限",
                        subtitle: "创建员工、调整角色、启停账号",
                        icon: "person.2.fill",
                        tint: ZhihuijiTheme.ColorToken.primary
                    )
                }
                .buttonStyle(.plain)
            }

            settingsInfoCard(
                title: "门店账号体系",
                subtitle: "一个门店默认有一名店长（总）和多名员工；导航、页面和写操作会按权限裁剪。",
                tint: ZhihuijiTheme.ColorToken.success
            )
        }
    }

    private var syncSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("同步与导入")
                .font(ZhihuijiTheme.Typography.sectionTitle)

            if session.hasPermission(.databaseManage) {
                NavigationLink {
                    SyncImportView()
                } label: {
                    settingsEntryCard(
                        title: "同步状态 / 导入任务",
                        subtitle: "查看 `/v2/sync/*` 与 `/v2/import-jobs/*` 的客户端状态",
                        icon: "arrow.triangle.2.circlepath",
                        tint: ZhihuijiTheme.ColorToken.primaryBright
                    )
                }
                .buttonStyle(.plain)
            } else {
                settingsInfoCard(
                    title: "同步与导入受限",
                    subtitle: "当前账号没有 `database:manage` 权限，因此这里不显示同步与导入入口。",
                    tint: ZhihuijiTheme.ColorToken.textTertiary
                )
            }
        }
    }

    private var mediaSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("媒体资产")
                .font(ZhihuijiTheme.Typography.sectionTitle)

            if session.hasPermission(.databaseManage) {
                NavigationLink {
                    MediaAssetsView()
                } label: {
                    settingsEntryCard(
                        title: "媒体对象与绑定",
                        subtitle: "查看 /v2/media/* 的真实对象、绑定和删除链路，不伪造本地上传。",
                        icon: "photo.on.rectangle.angled",
                        tint: ZhihuijiTheme.ColorToken.primaryBright
                    )
                }
                .buttonStyle(.plain)
            } else {
                settingsInfoCard(
                    title: "媒体入口受限",
                    subtitle: "当前账号没有 database:manage 权限，因此这里不显示媒体对象管理入口。",
                    tint: ZhihuijiTheme.ColorToken.textTertiary
                )
            }
        }
    }

    private var permissionSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("当前权限")
                .font(ZhihuijiTheme.Typography.sectionTitle)

            if session.permissions.isEmpty {
                EmptyStateView(title: "暂无权限数据", message: "当前账号还没有同步到门店权限信息。")
            } else {
                LazyVGrid(columns: [GridItem(.adaptive(minimum: 120), spacing: 10)], spacing: 10) {
                    ForEach(session.permissions.map(\.rawValue).sorted(), id: \.self) { permission in
                        StatusChip(title: permission, tint: ZhihuijiTheme.ColorToken.primary)
                    }
                }
                .padding(14)
                .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
            }
        }
    }

    private var securitySection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("账号与安全")
                .font(ZhihuijiTheme.Typography.sectionTitle)

            settingsInfoCard(
                title: "登录态管理",
                subtitle: "iOS 端 token 保存在 Keychain；当接口返回 401 时会清空会话并回到登录页。",
                tint: ZhihuijiTheme.ColorToken.warning
            )

            settingsInfoCard(
                title: "数据权限",
                subtitle: session.hasPermission(.databaseManage)
                    ? "当前账号具备数据库管理权限；同步与导入入口可见。"
                    : "当前账号没有数据库管理权限，因此这里只保留业务相关设置与安全信息。",
                tint: session.hasPermission(.databaseManage) ? ZhihuijiTheme.ColorToken.primaryBright : ZhihuijiTheme.ColorToken.textTertiary
            )

            settingsInfoCard(
                title: "接口地址",
                subtitle: environmentStore.current.apiBaseURL.absoluteString,
                tint: ZhihuijiTheme.ColorToken.primary
            )
        }
    }

    private func settingsEntryCard(title: String, subtitle: String, icon: String, tint: Color) -> some View {
        HStack(spacing: 14) {
            Circle()
                .fill(tint.opacity(0.14))
                .frame(width: 42, height: 42)
                .overlay(
                    Image(systemName: icon)
                        .foregroundStyle(tint)
                )

            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(ZhihuijiTheme.Typography.bodyMedium)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                Text(subtitle)
                    .font(ZhihuijiTheme.Typography.body)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
            }

            Spacer()
            Image(systemName: "chevron.right")
                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
        }
        .padding(16)
        .glassCard()
    }

    private func settingsInfoCard(title: String, subtitle: String, tint: Color) -> some View {
        HStack(alignment: .top, spacing: 12) {
            Circle()
                .fill(tint.opacity(0.14))
                .frame(width: 34, height: 34)
                .overlay(
                    Image(systemName: "checkmark.shield.fill")
                        .font(ZhihuijiTheme.Typography.body)
                        .foregroundStyle(tint)
                )

            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(ZhihuijiTheme.Typography.bodyMedium)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                Text(subtitle)
                    .font(ZhihuijiTheme.Typography.body)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            Spacer()
        }
        .padding(16)
        .glassCard()
    }
}
