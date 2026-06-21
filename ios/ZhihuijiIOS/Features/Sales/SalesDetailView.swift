import SwiftUI

struct SalesDetailView: View {
    @Environment(\.appEnvironment) private var env
    @EnvironmentObject private var session: AppSession
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
                            .font(ZhihuijiTheme.Typography.sectionTitle)
                        ForEach(order.items) { item in
                            VStack(alignment: .leading, spacing: 6) {
                                Text(item.productName)
                                    .font(ZhihuijiTheme.Typography.cardTitle)
                                Text(item.productCode)
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
                            .background(Color.white.opacity(0.58), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous))
                        }
                    }

                    VStack(alignment: .leading, spacing: 12) {
                        HStack {
                            Text("收款记录")
                                .font(ZhihuijiTheme.Typography.sectionTitle)
                            Spacer()
                            if session.hasPermission(.financeWrite) {
                                NavigationLink {
                                    SalesPaymentView(initialOrderId: order.id)
                                } label: {
                                    StatusChip(title: "去收款", tint: ZhihuijiTheme.ColorToken.primary)
                                }
                                .buttonStyle(.plain)
                            }
                        }

                        if viewModel.payments.isEmpty {
                            EmptyStateView(title: "暂无收款记录", message: "当前销售单还没有收款记录。")
                        } else {
                            ForEach(viewModel.payments) { payment in
                                HStack {
                                    VStack(alignment: .leading, spacing: 4) {
                                        Text(payment.amount.currencyText)
                                            .font(ZhihuijiTheme.Typography.cardTitle)
                                        Text(payment.createdAt.dateTimeText)
                                            .font(ZhihuijiTheme.Typography.caption)
                                            .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                                    }
                                    Spacer()
                                    VStack(alignment: .trailing, spacing: 4) {
                                        Text(SalePaymentMethod(rawValue: payment.method)?.label ?? "其他")
                                            .font(ZhihuijiTheme.Typography.bodyMedium)
                                        if let referenceNo = payment.referenceNo, !referenceNo.isEmpty {
                                            Text(referenceNo)
                                                .font(ZhihuijiTheme.Typography.caption)
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
                        .font(ZhihuijiTheme.Typography.pageTitle)
                    Text(order.customerName ?? "散客")
                        .font(ZhihuijiTheme.Typography.body)
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
                    .font(ZhihuijiTheme.Typography.caption)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
            }
        }
        .padding(16)
        .background(Color.white.opacity(0.58), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous)
                .stroke(Color.white.opacity(0.5), lineWidth: ZhihuijiTheme.Stroke.hairline)
        )
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
            self.order = nil
            self.payments = []
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }
}
