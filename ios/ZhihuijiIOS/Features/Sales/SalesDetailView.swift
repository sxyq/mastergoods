import SwiftUI

struct SalesDetailView: View {
    @Environment(\.appEnvironment) private var env
    let orderId: EntityID
    @StateObject private var viewModel = SalesDetailViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                if let errorMessage = viewModel.errorMessage {
                    EmptyStateView(title: "销售详情读取失败", message: errorMessage)
                } else if let order = viewModel.order {
                    header(order: order)

                    VStack(alignment: .leading, spacing: 12) {
                        Text("商品明细")
                            .font(.system(size: 18, weight: .semibold))
                        ForEach(order.items) { item in
                            VStack(alignment: .leading, spacing: 6) {
                                Text(item.productName)
                                    .font(.system(size: 15, weight: .semibold))
                                Text(item.productCode)
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
                            .background(Color.white.opacity(0.58), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous))
                        }
                    }

                    VStack(alignment: .leading, spacing: 12) {
                        HStack {
                            Text("收款记录")
                                .font(.system(size: 18, weight: .semibold))
                            Spacer()
                            NavigationLink {
                                SalesPaymentView(initialOrderId: order.id)
                            } label: {
                                StatusChip(title: "去收款", tint: ZhihuijiTheme.ColorToken.primary)
                            }
                            .buttonStyle(.plain)
                        }

                        if viewModel.payments.isEmpty {
                            EmptyStateView(title: "暂无收款记录", message: "当前销售单还没有收款记录。")
                        } else {
                            ForEach(viewModel.payments) { payment in
                                HStack {
                                    VStack(alignment: .leading, spacing: 4) {
                                        Text(payment.amount.currencyText)
                                            .font(.system(size: 15, weight: .semibold))
                                        Text(payment.createdAt.dateTimeText)
                                            .font(.system(size: 12))
                                            .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                                    }
                                    Spacer()
                                    VStack(alignment: .trailing, spacing: 4) {
                                        Text(SalePaymentMethod(rawValue: payment.method)?.label ?? "其他")
                                            .font(.system(size: 13, weight: .medium))
                                        if let referenceNo = payment.referenceNo, !referenceNo.isEmpty {
                                            Text(referenceNo)
                                                .font(.system(size: 12))
                                                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                                        }
                                    }
                                }
                                .padding(14)
                                .background(Color.white.opacity(0.58), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous))
                            }
                        }
                    }
                } else {
                    LoadingStateView(message: "正在加载销售详情...")
                }
            }
            .padding(20)
        }
        .navigationTitle("销售详情")
        .task {
            await viewModel.load(orderId: orderId, client: env.apiClient)
        }
    }

    @ViewBuilder
    private func header(order: SalesOrder) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(order.orderNo)
                        .font(.system(size: 20, weight: .bold))
                    Text(order.customerName ?? "散客")
                        .font(.system(size: 14))
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                }
                Spacer()
                StatusChip(title: order.statusLabel, tint: order.statusTint)
            }

            HStack {
                metric("总额", order.totalAmount.currencyText)
                metric("已收", order.paidAmount.currencyText)
                metric("待收", order.outstandingAmount.currencyText)
            }
            if let notes = order.notes, !notes.isEmpty {
                Text(notes)
                    .font(.system(size: 13))
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
            }
        }
        .padding(16)
        .background(Color.white.opacity(0.58), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous)
                .stroke(Color.white.opacity(0.5), lineWidth: 0.5)
        )
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
final class SalesDetailViewModel: ObservableObject {
    @Published var order: SalesOrder?
    @Published var payments: [SalePaymentRecord] = []
    @Published var errorMessage: String?

    func load(orderId: EntityID, client: APIClient) async {
        do {
            async let order = client.fetchSaleOrder(id: orderId)
            async let payments = client.fetchSaleOrderPayments(id: orderId)
            self.order = try await order
            self.payments = try await payments
            errorMessage = nil
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }
}
