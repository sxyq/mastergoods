import SwiftUI

struct PurchaseDetailView: View {
    @Environment(\.appEnvironment) private var env
    @EnvironmentObject private var session: AppSession
    let orderId: EntityID
    @StateObject private var viewModel = PurchaseDetailViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                if let errorMessage = viewModel.errorMessage {
                    EmptyStateView(title: "采购详情读取失败", message: errorMessage)
                } else if let order = viewModel.order {
                    header(order: order)

                    if session.hasAnyPermission([.inventoryWrite, .purchaseWrite, .financeView]) {
                        HStack(spacing: 12) {
                            if session.hasPermission(.inventoryWrite) {
                                NavigationLink {
                                    PurchaseReceiptView(initialOrderId: order.id)
                                } label: {
                                    StatusChip(title: "去入库", tint: ZhihuijiTheme.ColorToken.success)
                                }
                                .buttonStyle(.plain)
                            }

                            if session.hasPermission(.purchaseWrite) {
                                NavigationLink {
                                    PurchaseReturnView(initialOrderId: order.id)
                                } label: {
                                    StatusChip(title: "采购退货", tint: ZhihuijiTheme.ColorToken.warning)
                                }
                                .buttonStyle(.plain)
                            }

                            if session.hasPermission(.financeView) {
                                NavigationLink {
                                    PayOrderDetailView(initialOrderId: nil, initialKeyword: order.supplierName ?? "")
                                } label: {
                                    StatusChip(title: "查看付款单", tint: ZhihuijiTheme.ColorToken.primary)
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }

                    VStack(alignment: .leading, spacing: 12) {
                        Text("商品明细")
                            .font(ZhihuijiTheme.Typography.sectionTitle)
                        ForEach(order.items) { item in
                            VStack(alignment: .leading, spacing: 6) {
                                Text(item.productName)
                                    .font(ZhihuijiTheme.Typography.cardTitle)
                                Text(item.productCode ?? "")
                                    .font(ZhihuijiTheme.Typography.caption)
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                                HStack {
                                    Text("数量 \(String(format: "%.2f", item.quantity))")
                                    Spacer()
                                    Text(item.amount.currencyText)
                                }
                                .font(ZhihuijiTheme.Typography.body)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                            }
                            .padding(14)
                            .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
                        }
                    }
                } else {
                    LoadingStateView(message: "正在加载采购详情...")
                }
            }
            .padding(20)
        }
        .navigationTitle("采购详情")
        .task {
            await viewModel.load(orderId: orderId, client: env.apiClient)
        }
    }

    private func header(order: PurchaseOrder) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(order.orderNo)
                        .font(ZhihuijiTheme.Typography.pageTitle)
                    Text(order.supplierName ?? "未命名供应商")
                        .font(ZhihuijiTheme.Typography.body)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                }
                Spacer()
                StatusChip(title: order.statusLabel, tint: order.statusTint)
            }

            HStack {
                metric("总额", order.totalAmount.currencyText)
                metric("已付", order.paidAmount.currencyText)
                metric("待入", order.pendingReceiptAmount.currencyText)
            }
            if let notes = order.notes, !notes.isEmpty {
                Text(notes)
                    .font(ZhihuijiTheme.Typography.caption)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
            }
        }
        .padding(16)
        .glassCard()
    }

    private func metric(_ title: String, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(ZhihuijiTheme.Typography.caption)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
            Text(value)
                .font(ZhihuijiTheme.Typography.bodyMedium)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

@MainActor
final class PurchaseDetailViewModel: ObservableObject {
    @Published var order: PurchaseOrder?
    @Published var errorMessage: String?

    func load(orderId: EntityID, client: APIClient) async {
        do {
            order = try await client.fetchPurchaseOrder(id: orderId)
            errorMessage = nil
        } catch {
            order = nil
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }
}
