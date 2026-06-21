import SwiftUI

struct SalesPaymentView: View {
    @Environment(\.appEnvironment) private var env
    @EnvironmentObject private var session: AppSession
    let initialOrderId: EntityID?
    @StateObject private var viewModel = SalesPaymentViewModel()

    init(initialOrderId: EntityID? = nil) {
        self.initialOrderId = initialOrderId
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("销售收款")
                    .font(ZhihuijiTheme.Typography.pageTitle)

                if let errorMessage = viewModel.errorMessage {
                    EmptyStateView(title: "销售收款读取失败", message: errorMessage)
                }

                if viewModel.orders.isEmpty, !viewModel.isLoading {
                    EmptyStateView(title: "暂无待收款销售单", message: "当前未找到待收款的销售单。")
                } else {
                    orderPicker
                    if let order = viewModel.selectedOrder {
                        orderSummary(order)
                        paymentForm(order)
                        paymentsSection
                    }
                }
            }
            .padding(20)
        }
        .navigationTitle("销售收款")
        .task {
            await viewModel.load(client: env.apiClient, preferredOrderId: initialOrderId)
        }
    }

    private var orderPicker: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("待收款订单")
                .font(ZhihuijiTheme.Typography.sectionTitle)

            ForEach(viewModel.orders.prefix(6)) { order in
                Button {
                    Task {
                        await viewModel.select(orderId: order.id, client: env.apiClient)
                    }
                } label: {
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(order.orderNo)
                                .font(ZhihuijiTheme.Typography.cardTitle)
                            Text(order.customerName ?? "散客")
                                .font(ZhihuijiTheme.Typography.caption)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                        }
                        Spacer()
                        Text(order.outstandingAmount.currencyText)
                            .font(ZhihuijiTheme.Typography.bodyMedium)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                    }
                    .padding(14)
                    .background(
                        (viewModel.selectedOrderId == order.id ? ZhihuijiTheme.ColorToken.primary.opacity(0.12) : Color.white.opacity(0.58)),
                        in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous)
                    )
                }
                .buttonStyle(.plain)
            }
        }
    }

    private func orderSummary(_ order: SalesOrder) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("订单信息")
                .font(ZhihuijiTheme.Typography.sectionTitle)
            Text(order.orderNo)
                .font(ZhihuijiTheme.Typography.cardTitle)
            HStack {
                metric("总额", order.totalAmount.currencyText)
                metric("已收", order.paidAmount.currencyText)
                metric("待收", order.outstandingAmount.currencyText)
            }
        }
        .padding(16)
        .background(Color.white.opacity(0.58), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous)
                .stroke(Color.white.opacity(0.5), lineWidth: ZhihuijiTheme.Stroke.hairline)
        )
    }

    private func paymentForm(_ order: SalesOrder) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("收款表单")
                .font(ZhihuijiTheme.Typography.sectionTitle)

            TextField("收款金额", text: $viewModel.amountText)
                .fieldBackground()
            Picker("收款方式", selection: $viewModel.method) {
                ForEach(SalePaymentMethod.allCases) { method in
                    Text(method.label).tag(method)
                }
            }
            .pickerStyle(.menu)
            .padding(.horizontal, 14)
            .padding(.vertical, 14)
            .background(Color.white.opacity(0.58), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.field, style: .continuous))
            TextField("参考流水号", text: $viewModel.referenceNo)
                .fieldBackground()

            if let successMessage = viewModel.successMessage {
                Text(successMessage)
                    .font(ZhihuijiTheme.Typography.bodyMedium)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.success)
            }

            PrimaryGlassButton(
                title: viewModel.isSubmitting ? "提交中..." : "确认收款",
                systemImage: "checkmark.circle.fill",
                disabled: viewModel.isSubmitting || !session.hasPermission(.financeWrite)
            ) {
                Task {
                    await viewModel.submit(client: env.apiClient)
                }
            }
        }
        .padding(16)
        .background(Color.white.opacity(0.58), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous)
                .stroke(Color.white.opacity(0.5), lineWidth: ZhihuijiTheme.Stroke.hairline)
        )
        .onChange(of: order.id) { _, _ in
            viewModel.amountText = String(format: "%.2f", order.outstandingAmount)
        }
    }

    private var paymentsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("历史收款记录")
                .font(ZhihuijiTheme.Typography.sectionTitle)

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
                        Text(SalePaymentMethod(rawValue: payment.method)?.label ?? "其他")
                            .font(ZhihuijiTheme.Typography.bodyMedium)
                    }
                    .padding(14)
                    .background(Color.white.opacity(0.58), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous))
                }
            }
        }
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
final class SalesPaymentViewModel: ObservableObject {
    @Published var isLoading = false
    @Published var isSubmitting = false
    @Published var orders: [SalesOrderSummary] = []
    @Published var selectedOrderId: EntityID?
    @Published var selectedOrder: SalesOrder?
    @Published var payments: [SalePaymentRecord] = []
    @Published var amountText = ""
    @Published var method: SalePaymentMethod = .cash
    @Published var referenceNo = ""
    @Published var errorMessage: String?
    @Published var successMessage: String?

    func load(client: APIClient, preferredOrderId: EntityID?) async {
        isLoading = true
        defer { isLoading = false }
        do {
            orders = try await client.fetchSaleOrders(paymentStatus: 0, page: 1, size: 20)
            let target = preferredOrderId ?? orders.first?.id
            if let target {
                await select(orderId: target, client: client)
            }
            errorMessage = nil
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    func select(orderId: EntityID, client: APIClient) async {
        do {
            async let order = client.fetchSaleOrder(id: orderId)
            async let payments = client.fetchSaleOrderPayments(id: orderId)
            let resolvedOrder = try await order
            let resolvedPayments = try await payments
            selectedOrderId = orderId
            selectedOrder = resolvedOrder
            self.payments = resolvedPayments
            amountText = String(format: "%.2f", resolvedOrder.outstandingAmount)
            successMessage = nil
            errorMessage = nil
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    func submit(client: APIClient) async {
        guard let selectedOrderId, let amount = Double(amountText), amount > 0 else {
            errorMessage = "请输入正确的收款金额"
            return
        }
        isSubmitting = true
        defer { isSubmitting = false }
        do {
            _ = try await client.createSaleOrderPayment(
                id: selectedOrderId,
                payload: SalePaymentCreatePayload(
                    amount: amount,
                    method: method.rawValue,
                    referenceNo: referenceNo.isEmpty ? nil : referenceNo
                )
            )
            await load(client: client, preferredOrderId: selectedOrderId)
            successMessage = "收款已提交"
            referenceNo = ""
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }
}
