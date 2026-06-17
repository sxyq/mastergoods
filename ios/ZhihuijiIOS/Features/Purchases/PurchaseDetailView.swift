import SwiftUI

struct PurchaseDetailView: View {
    @Environment(\.appEnvironment) private var env
    let orderId: EntityID
    @StateObject private var viewModel = PurchaseDetailViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                if let errorMessage = viewModel.errorMessage {
                    EmptyStateView(title: "采购详情读取失败", message: errorMessage)
                } else if let order = viewModel.order {
                    header(order: order)

                    HStack(spacing: 12) {
                        NavigationLink {
                            PurchaseReceiptView(initialOrderId: order.id)
                        } label: {
                            StatusChip(title: "去入库", tint: ZhihuijiTheme.ColorToken.success)
                        }
                        .buttonStyle(.plain)

                        NavigationLink {
                            PurchaseReturnView(initialOrderId: order.id)
                        } label: {
                            StatusChip(title: "采购退货", tint: ZhihuijiTheme.ColorToken.warning)
                        }
                        .buttonStyle(.plain)

                        NavigationLink {
                            PayOrderDetailView(initialOrderId: nil, initialKeyword: order.supplierName ?? "")
                        } label: {
                            StatusChip(title: "查看付款单", tint: ZhihuijiTheme.ColorToken.primary)
                        }
                        .buttonStyle(.plain)
                    }

                    VStack(alignment: .leading, spacing: 12) {
                        Text("商品明细")
                            .font(.system(size: 18, weight: .semibold))
                        ForEach(order.items) { item in
                            VStack(alignment: .leading, spacing: 6) {
                                Text(item.productName)
                                    .font(.system(size: 15, weight: .semibold))
                                Text(item.productCode ?? "")
                                    .font(.system(size: 12))
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                                HStack {
                                    Text("数量 \(String(format: "%.2f", item.quantity))")
                                    Spacer()
                                    Text(item.amount.currencyText)
                                }
                                .font(.system(size: 13))
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                            }
                            .padding(14)
                            .glassCard(cornerRadius: 12)
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
                        .font(.system(size: 20, weight: .bold))
                    Text(order.supplierName ?? "未命名供应商")
                        .font(.system(size: 14))
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
                    .font(.system(size: 13))
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
            }
        }
        .padding(16)
        .glassCard()
    }

    private func metric(_ title: String, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(.system(size: 12))
                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
            Text(value)
                .font(.system(size: 14, weight: .semibold))
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
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }
}
