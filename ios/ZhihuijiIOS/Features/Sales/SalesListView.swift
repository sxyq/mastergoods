import SwiftUI

struct SalesListView: View {
    @Environment(\.appEnvironment) private var env
    @StateObject private var viewModel = SalesListViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                HStack {
                    Text("销售单")
                        .font(.system(size: 28, weight: .bold))
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                    Spacer()
                    NavigationLink {
                        SalesEditView()
                    } label: {
                        StatusChip(title: "开单", tint: ZhihuijiTheme.ColorToken.primary)
                    }
                    .buttonStyle(.plain)
                }

                TextField("搜索单号 / 客户", text: $viewModel.keyword)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 14)
                    .background(Color.white.opacity(0.58), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.field, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.field, style: .continuous)
                            .stroke(Color.white.opacity(0.5), lineWidth: 0.5)
                    )

                PrimaryGlassButton(title: viewModel.isLoading ? "刷新中..." : "刷新销售单", systemImage: "arrow.clockwise", disabled: viewModel.isLoading) {
                    Task {
                        await viewModel.load(using: env.apiClient)
                    }
                }

                if let errorMessage = viewModel.errorMessage {
                    EmptyStateView(title: "销售单拉取失败", message: errorMessage)
                } else if viewModel.orders.isEmpty, !viewModel.isLoading {
                    EmptyStateView(title: "暂无销售单", message: "当前查询条件下没有销售单数据。")
                } else {
                    LazyVStack(spacing: 12) {
                        ForEach(viewModel.orders) { order in
                            NavigationLink {
                                SalesDetailView(orderId: order.id)
                            } label: {
                                SalesOrderCard(order: order)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
            }
            .padding(20)
        }
        .navigationTitle("单据")
        .task {
            await viewModel.load(using: env.apiClient)
        }
    }
}

@MainActor
final class SalesListViewModel: ObservableObject {
    @Published var keyword = ""
    @Published var isLoading = false
    @Published var orders: [SalesOrderSummary] = []
    @Published var errorMessage: String?

    func load(using client: APIClient) async {
        isLoading = true
        defer { isLoading = false }
        do {
            orders = try await client.fetchSaleOrders(keyword: keyword, page: 1, size: 20)
            errorMessage = nil
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }
}

private struct SalesOrderCard: View {
    let order: SalesOrderSummary

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(order.orderNo)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                    Text(order.customerName ?? "散客")
                        .font(.system(size: 13))
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                }
                Spacer()
                StatusChip(title: order.statusLabel, tint: order.statusTint)
            }

            HStack {
                Label(order.totalAmount.currencyText, systemImage: "yensign.circle.fill")
                Spacer()
                Text("已收 \(order.paidAmount.currencyText)")
            }
            .font(.system(size: 13, weight: .medium))
            .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
        }
        .padding(16)
        .background(Color.white.opacity(0.58), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous)
                .stroke(Color.white.opacity(0.5), lineWidth: 0.5)
        )
    }
}
