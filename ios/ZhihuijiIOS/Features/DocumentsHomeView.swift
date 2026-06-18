import SwiftUI

struct DocumentsHomeView: View {
    @EnvironmentObject private var session: AppSession

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                headerCard
                documentFlowSection
                shortcutSection
            }
            .padding(20)
        }
        .navigationTitle("单据")
    }

    private var headerCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 6) {
                    Text("单据中心")
                        .font(.system(size: 28, weight: .bold))
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                    Text("销售、采购、收款、退货都按移动端工作流聚合在这里。")
                        .font(.system(size: 14))
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                }
                Spacer()
                if let role = session.currentStore?.role {
                    StatusChip(title: role.label, tint: ZhihuijiTheme.ColorToken.primary)
                }
            }

            HStack(spacing: 10) {
                documentBadge(title: "销售", enabled: session.hasPermission(.salesView), tint: ZhihuijiTheme.ColorToken.primary)
                documentBadge(title: "采购", enabled: session.hasPermission(.purchaseView), tint: ZhihuijiTheme.ColorToken.warning)
                documentBadge(title: "财务", enabled: session.hasPermission(.financeView), tint: ZhihuijiTheme.ColorToken.success)
            }
        }
        .padding(18)
        .glassCard()
    }

    private var documentFlowSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("业务入口")
                .font(.system(size: 20, weight: .semibold))

            if session.hasPermission(.salesView) {
                NavigationLink {
                    SalesListView()
                } label: {
                    documentsEntryCard(
                        title: "销售业务",
                        subtitle: "销售列表、详情、收款、退货",
                        detail: "适合销售员工和店长快速处理成交与回款",
                        icon: "cart.fill",
                        tint: ZhihuijiTheme.ColorToken.primary
                    )
                }
                .buttonStyle(.plain)
            }

            if session.hasPermission(.purchaseView) {
                NavigationLink {
                    PurchaseListView()
                } label: {
                    documentsEntryCard(
                        title: "采购业务",
                        subtitle: "采购列表、详情、入库、退货",
                        detail: "适合采购和仓库角色处理到货、退货与付款链路",
                        icon: "shippingbox.fill",
                        tint: ZhihuijiTheme.ColorToken.warning
                    )
                }
                .buttonStyle(.plain)
            }
        }
    }

    private var shortcutSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("能力联动")
                .font(.system(size: 20, weight: .semibold))

            if session.hasPermission(.financeView) {
                NavigationLink {
                    FinanceRecordView()
                } label: {
                    documentsMiniCard(
                        title: "资金流水",
                        subtitle: "查看收支、回款和日常支出",
                        icon: "creditcard.fill",
                        tint: ZhihuijiTheme.ColorToken.success
                    )
                }
                .buttonStyle(.plain)
            }

            if session.hasPermission(.inventoryView) {
                NavigationLink {
                    InventorySnapshotView()
                } label: {
                    documentsMiniCard(
                        title: "库存联动",
                        subtitle: "查看低库存、快照和月度统计",
                        icon: "cube.box.fill",
                        tint: ZhihuijiTheme.ColorToken.primaryBright
                    )
                }
                .buttonStyle(.plain)
            }
        }
    }

    private func documentsEntryCard(
        title: String,
        subtitle: String,
        detail: String,
        icon: String,
        tint: Color
    ) -> some View {
        HStack(spacing: 14) {
            Circle()
                .fill(tint.opacity(0.15))
                .frame(width: 42, height: 42)
                .overlay(
                    Image(systemName: icon)
                        .foregroundStyle(tint)
                )

            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                Text(subtitle)
                    .font(.system(size: 13))
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                Text(detail)
                    .font(.system(size: 12))
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                    .fixedSize(horizontal: false, vertical: true)
            }

            Spacer()
            Image(systemName: "chevron.right")
                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
        }
        .padding(16)
        .glassCard()
    }

    private func documentsMiniCard(title: String, subtitle: String, icon: String, tint: Color) -> some View {
        HStack(spacing: 14) {
            Circle()
                .fill(tint.opacity(0.15))
                .frame(width: 38, height: 38)
                .overlay(
                    Image(systemName: icon)
                        .font(.system(size: 14))
                        .foregroundStyle(tint)
                )

            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                Text(subtitle)
                    .font(.system(size: 12))
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
            }

            Spacer()
            Image(systemName: "chevron.right")
                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
        }
        .padding(14)
        .glassCard(cornerRadius: 14)
    }

    private func documentBadge(title: String, enabled: Bool, tint: Color) -> some View {
        Text(title)
            .font(.system(size: 12, weight: .semibold))
            .foregroundStyle(enabled ? tint : ZhihuijiTheme.ColorToken.textTertiary)
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(
                (enabled ? tint.opacity(0.12) : Color.white.opacity(0.40)),
                in: Capsule()
            )
            .overlay(
                Capsule()
                    .stroke(Color.white.opacity(0.45), lineWidth: 0.5)
            )
    }
}
