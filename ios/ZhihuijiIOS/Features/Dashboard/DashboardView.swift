import SwiftUI

struct DashboardView: View {
    @EnvironmentObject private var session: AppSession
    @Environment(\.appEnvironment) private var env
    @StateObject private var viewModel = DashboardViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                heroCard
                metricSection
                reminderSection
                quickEntrySection
            }
            .padding(20)
        }
        .navigationTitle("首页")
        .task {
            await viewModel.load(using: env.apiClient)
        }
    }

    private var heroCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 6) {
                    Text("智慧记")
                        .font(.system(size: 28, weight: .bold))
                        .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                    Text(session.currentStore?.storeName ?? "当前门店")
                        .font(.system(size: 15, weight: .medium))
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                    Text("\(session.currentStore?.role.label ?? "当前角色") · \(session.currentStore?.currentUserName ?? "当前账号")")
                        .font(.system(size: 13))
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                }
                Spacer()
                StatusChip(title: viewModel.scopeLabel, tint: ZhihuijiTheme.ColorToken.primary)
            }

            if let errorMessage = viewModel.errorMessage {
                Text(errorMessage)
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(ZhihuijiTheme.ColorToken.warning)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 10)
                    .background(ZhihuijiTheme.ColorToken.warning.opacity(0.10), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
            } else {
                Text("今天的销售、到账、退款和库存提醒已经聚合到同一个移动工作台里。")
                    .font(.system(size: 13))
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
            }
        }
        .padding(18)
        .glassCard()
    }

    private var metricSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("经营概览")
                    .font(.system(size: 20, weight: .semibold))
                Spacer()
                if viewModel.isLoading {
                    ProgressView()
                        .scaleEffect(0.8)
                        .tint(ZhihuijiTheme.ColorToken.primary)
                }
            }

            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                ForEach(Array(viewModel.kpis.enumerated()), id: \.element.id) { index, item in
                    MetricCard(
                        title: item.title,
                        value: item.value,
                        subtitle: item.subtitle,
                        tint: metricTint(for: index)
                    )
                }
            }
        }
    }

    private var reminderSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("库存提醒")
                .font(.system(size: 20, weight: .semibold))

            if viewModel.lowStockProducts.isEmpty {
                EmptyStateView(title: "暂无低库存预警", message: "当前没有商品低于安全库存，补货压力比较平稳。")
            } else {
                VStack(spacing: 10) {
                    ForEach(viewModel.lowStockProducts) { product in
                        HStack(spacing: 12) {
                            Circle()
                                .fill(ZhihuijiTheme.ColorToken.warning.opacity(0.14))
                                .frame(width: 34, height: 34)
                                .overlay(
                                    Image(systemName: "shippingbox.fill")
                                        .font(.system(size: 14))
                                        .foregroundStyle(ZhihuijiTheme.ColorToken.warning)
                                )
                            VStack(alignment: .leading, spacing: 4) {
                                Text(product.productName)
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                                Text(product.productCode)
                                    .font(.system(size: 12))
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                            }
                            Spacer()
                            VStack(alignment: .trailing, spacing: 4) {
                                Text("库存 \(formattedQuantity(product.stock))")
                                    .font(.system(size: 12, weight: .semibold))
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.danger)
                                Text("安全线 \(formattedQuantity(product.safeStock))")
                                    .font(.system(size: 11))
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                            }
                        }
                        .padding(14)
                        .glassCard(cornerRadius: 12)
                    }
                }
            }
        }
    }

    private var quickEntrySection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("快捷入口")
                .font(.system(size: 20, weight: .semibold))

            if session.hasPermission(.salesView) {
                NavigationLink {
                    SalesListView()
                } label: {
                    dashboardEntryCard(
                        title: "销售单据",
                        subtitle: "查看列表、详情、收款与退货",
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
                    dashboardEntryCard(
                        title: "采购单据",
                        subtitle: "进入采购、入库、退货全链路",
                        icon: "shippingbox.fill",
                        tint: ZhihuijiTheme.ColorToken.warning
                    )
                }
                .buttonStyle(.plain)
            }

            if session.hasPermission(.agentView) {
                NavigationLink {
                    AgentChatView()
                } label: {
                    dashboardEntryCard(
                        title: "AI 助手",
                        subtitle: "查看经营提示，直接发问并追踪运行轨迹",
                        icon: "sparkles",
                        tint: ZhihuijiTheme.ColorToken.success
                    )
                }
                .buttonStyle(.plain)
            }
        }
    }

    private func dashboardEntryCard(title: String, subtitle: String, icon: String, tint: Color) -> some View {
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
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                Text(subtitle)
                    .font(.system(size: 13))
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
            }

            Spacer()
            Image(systemName: "chevron.right")
                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
        }
        .padding(16)
        .glassCard()
    }

    private func metricTint(for index: Int) -> Color {
        switch index {
        case 1:
            return ZhihuijiTheme.ColorToken.success
        case 2:
            return ZhihuijiTheme.ColorToken.warning
        case 3:
            return ZhihuijiTheme.ColorToken.primaryBright
        default:
            return ZhihuijiTheme.ColorToken.primary
        }
    }

    private func formattedQuantity(_ value: Double) -> String {
        if value.rounded() == value {
            return String(Int(value))
        }
        return String(format: "%.2f", value)
    }
}
