import SwiftUI

struct PurchaseListView: View {
    @Environment(\.appEnvironment) private var env
    @EnvironmentObject private var session: AppSession
    @StateObject private var viewModel = PurchaseListViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                HStack {
                    Text("采购单")
                        .font(ZhihuijiTheme.Typography.pageTitle)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                    Spacer()
                    if session.hasPermission(.purchaseWrite) {
                        NavigationLink {
                            PurchaseEditView()
                        } label: {
                            StatusChip(title: "开单", tint: ZhihuijiTheme.ColorToken.primary)
                        }
                        .buttonStyle(.plain)
                    }
                }

                TextField("搜索采购单号 / 供应商", text: $viewModel.keyword)
                    .fieldBackground()

                Picker("状态", selection: $viewModel.statusFilter) {
                    ForEach(PurchaseStatusFilter.allCases) { filter in
                        Text(filter.title).tag(filter)
                    }
                }
                .pickerStyle(.segmented)

                PrimaryGlassButton(
                    title: viewModel.isLoading ? "刷新中..." : "刷新采购单",
                    systemImage: "arrow.clockwise",
                    disabled: viewModel.isLoading
                ) {
                    Task { await viewModel.load(using: env.apiClient) }
                }

                if let errorMessage = viewModel.errorMessage {
                    EmptyStateView(title: "采购单拉取失败", message: errorMessage)
                } else if viewModel.orders.isEmpty, !viewModel.isLoading {
                    EmptyStateView(title: "暂无采购单", message: "当前筛选条件下没有采购单数据。")
                } else {
                    LazyVStack(spacing: 12) {
                        ForEach(viewModel.orders) { order in
                            NavigationLink {
                                PurchaseDetailView(orderId: order.id)
                            } label: {
                                PurchaseOrderCard(order: order)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
            }
            .padding(20)
        }
        .navigationTitle("采购业务")
        .task {
            await viewModel.load(using: env.apiClient)
        }
        .onChange(of: viewModel.statusFilter) { _, _ in
            Task { await viewModel.load(using: env.apiClient) }
        }
    }
}

@MainActor
final class PurchaseListViewModel: ObservableObject {
    @Published var keyword = ""
    @Published var statusFilter: PurchaseStatusFilter = .all
    @Published var isLoading = false
    @Published var orders: [PurchaseOrderSummary] = []
    @Published var errorMessage: String?

    func load(using client: APIClient) async {
        isLoading = true
        defer { isLoading = false }
        do {
            orders = try await client.fetchPurchaseOrders(
                keyword: keyword.nilIfBlank,
                status: statusFilter.apiValue,
                page: 1,
                size: 20
            )
            errorMessage = nil
        } catch {
            orders = []
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }
}

private struct PurchaseOrderCard: View {
    let order: PurchaseOrderSummary

    var body: some View {
        GlassListRow {
            VStack(alignment: .leading, spacing: ZhihuijiTheme.Spacing.sm) {
                HStack {
                    VStack(alignment: .leading, spacing: ZhihuijiTheme.Spacing.xs) {
                        Text(order.orderNo)
                            .font(ZhihuijiTheme.Typography.cardTitle)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                        Text(order.supplierName ?? "未命名供应商")
                            .font(ZhihuijiTheme.Typography.body)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                    }
                    Spacer()
                    StatusChip(title: order.statusLabel, tint: order.statusTint)
                }

                HStack(alignment: .center) {
                    AmountText(value: order.totalAmount.currencyText, tint: ZhihuijiTheme.ColorToken.dataTextPrimary)
                    Spacer()
                    Text("已付 \(order.paidAmount.currencyText)")
                        .font(ZhihuijiTheme.Typography.captionSemibold)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                }

                TimestampText(value: order.createdAt)
            }
        }
    }
}

enum PurchaseStatusFilter: String, CaseIterable, Identifiable {
    case all
    case draft
    case received
    case confirmed

    var id: String { rawValue }

    var title: String {
        switch self {
        case .all: return "全部"
        case .draft: return "草稿"
        case .received: return "入库"
        case .confirmed: return "确认"
        }
    }

    var apiValue: Int? {
        switch self {
        case .all: return nil
        case .draft: return 0
        case .received: return 1
        case .confirmed: return 2
        }
    }
}
