import SwiftUI

struct RoleAccessView: View {
    @EnvironmentObject private var session: AppSession
    @StateObject private var viewModel = RoleAccessViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                headerCard

                ForEach(viewModel.roleEntries, id: \.role) { entry in
                    roleCard(entry)
                }
            }
            .padding(20)
        }
        .navigationTitle("角色权限")
    }

    private var headerCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("角色权限矩阵")
                .font(ZhihuijiTheme.Typography.pageTitle)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
            Text("门店各角色拥有的权限范围，与后端 StoreAccessPolicy 保持一致。店长可在店员管理中为员工分配角色。")
                .font(ZhihuijiTheme.Typography.body)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)

            HStack(spacing: 10) {
                statChip(title: "角色 \(viewModel.roleEntries.count)", tint: ZhihuijiTheme.ColorToken.primary)
                statChip(title: "权限 \(Permission.allCases.count)", tint: ZhihuijiTheme.ColorToken.primaryBright)
                if let currentRole = session.currentStore?.role {
                    statChip(title: "当前 \(currentRole.label)", tint: ZhihuijiTheme.ColorToken.success)
                }
            }
        }
        .padding(18)
        .glassCard()
    }

    private func roleCard(_ entry: RoleAccessEntry) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    HStack(spacing: 8) {
                        Image(systemName: roleIcon(entry.role))
                            .foregroundStyle(tintForRole(entry.role))
                        Text(entry.role.label)
                            .font(ZhihuijiTheme.Typography.cardTitle)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                    }
                    Text(roleDescription(entry.role))
                        .font(ZhihuijiTheme.Typography.caption)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 6) {
                    StatusChip(
                        title: "\(entry.permissions.count) 项权限",
                        tint: tintForRole(entry.role)
                    )
                    if session.currentStore?.role == entry.role {
                        StatusChip(title: "当前角色", tint: ZhihuijiTheme.ColorToken.success)
                    }
                }
            }

            LazyVGrid(columns: [GridItem(.adaptive(minimum: 100), spacing: 8)], spacing: 8) {
                ForEach(entry.permissions, id: \.self) { permission in
                    Text(permission.displayName)
                        .font(ZhihuijiTheme.Typography.caption)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                        .frame(maxWidth: .infinity)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 7)
                        .background(
                            Color.white.opacity(0.42),
                            in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.pill, style: .continuous)
                        )
                }
            }
        }
        .padding(16)
        .glassCard()
    }

    private func statChip(title: String, tint: Color) -> some View {
        Text(title)
            .font(ZhihuijiTheme.Typography.captionSemibold)
            .foregroundStyle(tint)
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(
                tint.opacity(0.12),
                in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.pill, style: .continuous)
            )
            .overlay(
                RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.pill, style: .continuous)
                    .stroke(Color.white.opacity(0.45), lineWidth: ZhihuijiTheme.Stroke.hairline)
            )
    }

    private func roleIcon(_ role: StoreRole) -> String {
        switch role {
        case .owner: return "crown.fill"
        case .manager: return "person.badge.shield.checkmark.fill"
        case .sales: return "cart.fill"
        case .purchasing: return "bag.fill"
        case .warehouse: return "shippingbox.fill"
        case .finance: return "creditcard.fill"
        case .assistant: return "sparkles"
        }
    }

    private func roleDescription(_ role: StoreRole) -> String {
        switch role {
        case .owner: return "门店最高权限，可管理所有业务模块与店员账号"
        case .manager: return "店长助理，覆盖主要业务操作与店员管理，不含数据库与设置管理"
        case .sales: return "销售员工，负责销售单据与档案查看"
        case .purchasing: return "采购员工，负责采购单据与档案查看"
        case .warehouse: return "仓库员工，负责库存盘点与档案查看"
        case .finance: return "财务员工，负责资金记录与报表查看"
        case .assistant: return "AI/只读助理，仅可查看首页、报表与 AI 模块"
        }
    }

    private func tintForRole(_ role: StoreRole) -> Color {
        switch role {
        case .owner:
            return ZhihuijiTheme.ColorToken.warning
        case .manager:
            return ZhihuijiTheme.ColorToken.primaryBright
        case .finance:
            return ZhihuijiTheme.ColorToken.success
        case .assistant:
            return ZhihuijiTheme.ColorToken.textTertiary
        default:
            return ZhihuijiTheme.ColorToken.primary
        }
    }
}

@MainActor
final class RoleAccessViewModel: ObservableObject {
    let roleEntries: [RoleAccessEntry]

    init() {
        roleEntries = StoreRole.allCases.map { role in
            RoleAccessEntry(
                role: role,
                permissions: PermissionPolicy.permissions(for: role)
                    .sorted { $0.displayName < $1.displayName }
            )
        }
    }
}

struct RoleAccessEntry: Identifiable, Equatable {
    let role: StoreRole
    let permissions: [Permission]

    var id: String { role.rawValue }
}
