import SwiftUI

struct SalesReturnView: View {
    @Environment(\.appEnvironment) private var env
    @EnvironmentObject private var session: AppSession
    @StateObject private var viewModel = SalesReturnViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                header

                if let errorMessage = viewModel.errorMessage {
                    EmptyStateView(title: "销售退货加载失败", message: errorMessage)
                }

                Picker("模式", selection: $viewModel.mode) {
                    ForEach(SalesReturnMode.allCases) { mode in
                        Text(mode.title).tag(mode)
                    }
                }
                .pickerStyle(.segmented)

                if viewModel.mode == .manage {
                    manageSection
                } else {
                    createSection
                }
            }
            .padding(20)
        }
        .navigationTitle("销售退货")
        .task {
            await viewModel.load(client: env.apiClient)
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("销售退货")
                .font(.system(size: 28, weight: .bold))
                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
            Text("保持与安卓一致的移动工作流：上面筛选，下面列表与详情分层展开。")
                .font(.system(size: 14))
                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
        }
    }

    private var manageSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            TextField("搜索退货单号 / 客户", text: $viewModel.keyword)
                .fieldBackground()

            Picker("状态", selection: $viewModel.statusFilter) {
                ForEach(SalesReturnStatusFilter.allCases) { filter in
                    Text(filter.title).tag(filter)
                }
            }
            .pickerStyle(.segmented)

            PrimaryGlassButton(
                title: viewModel.isLoading ? "刷新中..." : "刷新退货单",
                systemImage: "arrow.clockwise",
                disabled: viewModel.isLoading
            ) {
                Task { await viewModel.reloadReturns(client: env.apiClient) }
            }

            if viewModel.returns.isEmpty, !viewModel.isLoading {
                EmptyStateView(title: "暂无退货单", message: "当前筛选条件下还没有销售退货记录。")
            } else {
                LazyVStack(spacing: 12) {
                    ForEach(viewModel.returns) { item in
                        Button {
                            Task { await viewModel.selectReturn(id: item.id, client: env.apiClient) }
                        } label: {
                            SalesReturnCard(item: item, isSelected: viewModel.selectedReturn?.id == item.id)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }

            if let selected = viewModel.selectedReturn {
                SalesReturnDetailCard(item: selected)

                if selected.canEditDraft, session.hasPermission(.salesWrite) {
                    VStack(alignment: .leading, spacing: 12) {
                        Text("草稿处理")
                            .font(.system(size: 18, weight: .semibold))
                        TextField("备注", text: $viewModel.notesDraft, axis: .vertical)
                            .fieldBackground()

                        HStack(spacing: 12) {
                            PrimaryGlassButton(
                                title: viewModel.isSubmitting ? "保存中..." : "保存草稿",
                                systemImage: "square.and.arrow.down.fill",
                                disabled: viewModel.isSubmitting
                            ) {
                                Task { await viewModel.saveDraft(client: env.apiClient) }
                            }
                            PrimaryGlassButton(
                                title: viewModel.isSubmitting ? "确认中..." : "确认退货",
                                systemImage: "checkmark.seal.fill",
                                disabled: viewModel.isSubmitting
                            ) {
                                Task { await viewModel.confirmReturn(client: env.apiClient) }
                            }
                        }
                    }
                    .padding(16)
                    .glassCard()
                }

                if selected.canRefund, session.hasPermission(.financeWrite) {
                    VStack(alignment: .leading, spacing: 12) {
                        Text("退款处理")
                            .font(.system(size: 18, weight: .semibold))
                        TextField("退款金额", text: $viewModel.refundAmountText)
                            .fieldBackground()
                        Picker("退款方式", selection: $viewModel.refundMethod) {
                            ForEach(SalePaymentMethod.allCases) { method in
                                Text(method.label).tag(method)
                            }
                        }
                        .pickerStyle(.menu)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 14)
                        .background(Color.white.opacity(0.58), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.field, style: .continuous))
                        TextField("参考流水号", text: $viewModel.refundReferenceNo)
                            .fieldBackground()
                        PrimaryGlassButton(
                            title: viewModel.isSubmitting ? "退款提交中..." : "提交退款",
                            systemImage: "arrow.uturn.backward.circle.fill",
                            disabled: viewModel.isSubmitting
                        ) {
                            Task { await viewModel.submitRefund(client: env.apiClient) }
                        }
                    }
                    .padding(16)
                    .glassCard()
                }

                if selected.canCancel, session.hasPermission(.salesWrite) {
                    PrimaryGlassButton(
                        title: viewModel.isSubmitting ? "取消中..." : "取消退货单",
                        systemImage: "xmark.circle.fill",
                        disabled: viewModel.isSubmitting
                    ) {
                        Task { await viewModel.cancelReturn(client: env.apiClient) }
                    }
                }
            }
        }
        .onChange(of: viewModel.statusFilter) { _, _ in
            Task { await viewModel.reloadReturns(client: env.apiClient) }
        }
    }

    private var createSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("来源销售单")
                .font(.system(size: 18, weight: .semibold))

            if viewModel.saleOrders.isEmpty {
                EmptyStateView(title: "暂无可选销售单", message: "请先确保销售单已经同步到当前账号。")
            } else {
                ForEach(viewModel.saleOrders.prefix(8)) { order in
                    Button {
                        Task { await viewModel.selectSourceOrder(id: order.id, client: env.apiClient) }
                    } label: {
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(order.orderNo)
                                    .font(.system(size: 15, weight: .semibold))
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                                Text(order.customerName ?? "散客")
                                    .font(.system(size: 12))
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                            }
                            Spacer()
                            Text(order.totalAmount.currencyText)
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                        }
                        .padding(14)
                        .background(
                            (viewModel.sourceOrder?.id == order.id ? ZhihuijiTheme.ColorToken.primary.opacity(0.12) : Color.white.opacity(0.58)),
                            in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous)
                        )
                    }
                    .buttonStyle(.plain)
                }
            }

            if let sourceOrder = viewModel.sourceOrder {
                VStack(alignment: .leading, spacing: 12) {
                    Text("退货商品")
                        .font(.system(size: 18, weight: .semibold))
                    Text(sourceOrder.orderNo)
                        .font(.system(size: 14, weight: .medium))
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)

                    ForEach($viewModel.draftItems) { $item in
                        VStack(alignment: .leading, spacing: 10) {
                            Text(item.productName)
                                .font(.system(size: 15, weight: .semibold))
                            if !item.productCode.isEmpty {
                                Text(item.productCode)
                                    .font(.system(size: 12))
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                            }
                            Stepper(
                                value: $item.quantity,
                                in: 0 ... item.maxQuantity,
                                step: 1
                            ) {
                                Text("退货数量 \(String(format: "%.0f", item.quantity)) / \(String(format: "%.0f", item.maxQuantity))")
                                    .font(.system(size: 13))
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                            }
                            TextField("退货单价", text: $item.unitPriceText)
                                .fieldBackground()
                        }
                        .padding(14)
                        .glassCard()
                    }

                    TextField("备注", text: $viewModel.createNotes, axis: .vertical)
                        .fieldBackground()

                    PrimaryGlassButton(
                        title: viewModel.isSubmitting ? "创建中..." : "创建退货单",
                        systemImage: "plus.circle.fill",
                        disabled: viewModel.isSubmitting || !session.hasPermission(.salesWrite)
                    ) {
                        Task { await viewModel.createReturn(client: env.apiClient) }
                    }
                }
            }
        }
    }
}

@MainActor
final class SalesReturnViewModel: ObservableObject {
    @Published var mode: SalesReturnMode = .manage
    @Published var keyword = ""
    @Published var statusFilter: SalesReturnStatusFilter = .all
    @Published var isLoading = false
    @Published var isSubmitting = false
    @Published var returns: [SalesReturnRecord] = []
    @Published var selectedReturn: SalesReturnRecord?
    @Published var saleOrders: [SalesOrderSummary] = []
    @Published var sourceOrder: SalesOrder?
    @Published var draftItems: [SalesReturnDraftItem] = []
    @Published var notesDraft = ""
    @Published var createNotes = ""
    @Published var refundAmountText = ""
    @Published var refundMethod: SalePaymentMethod = .cash
    @Published var refundReferenceNo = ""
    @Published var errorMessage: String?
    @Published var successMessage: String?

    func load(client: APIClient) async {
        isLoading = true
        defer { isLoading = false }
        do {
            async let returnsTask = fetchReturns(client: client)
            async let ordersTask = client.fetchSaleOrders(page: 1, size: 20)
            let fetchedReturns = try await returnsTask
            let fetchedOrders = try await ordersTask
            returns = fetchedReturns
            saleOrders = fetchedOrders
            if selectedReturn == nil {
                selectedReturn = fetchedReturns.first
                hydrateSelected()
            }
            errorMessage = nil
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    func reloadReturns(client: APIClient) async {
        isLoading = true
        defer { isLoading = false }
        do {
            returns = try await fetchReturns(client: client)
            if let selectedId = selectedReturn?.id {
                selectedReturn = returns.first(where: { $0.id == selectedId }) ?? returns.first
            } else {
                selectedReturn = returns.first
            }
            hydrateSelected()
            errorMessage = nil
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    func selectReturn(id: EntityID, client: APIClient) async {
        do {
            selectedReturn = try await client.fetchSalesReturn(id: id)
            hydrateSelected()
            errorMessage = nil
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    func selectSourceOrder(id: EntityID, client: APIClient) async {
        do {
            let order = try await client.fetchSaleOrder(id: id)
            sourceOrder = order
            draftItems = order.items.map {
                SalesReturnDraftItem(
                    productId: EntityID(rawValue: String($0.productId)),
                    productCode: $0.productCode,
                    productName: $0.productName,
                    maxQuantity: max($0.quantity, 0),
                    quantity: 0,
                    unitPriceText: String(format: "%.2f", $0.unitPrice)
                )
            }
            errorMessage = nil
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    func saveDraft(client: APIClient) async {
        guard let selectedReturn else { return }
        await submit(client: client) {
            let updated = try await client.updateSalesReturnDraft(
                id: selectedReturn.id,
                payload: SalesReturnDraftPayload(notes: self.notesDraft.nilIfBlank)
            )
            self.apply(updated)
        }
    }

    func confirmReturn(client: APIClient) async {
        guard let selectedReturn else { return }
        await submit(client: client) {
            let updated = try await client.confirmSalesReturn(
                id: selectedReturn.id,
                payload: SalesReturnConfirmPayload(notes: self.notesDraft.nilIfBlank)
            )
            self.apply(updated)
        }
    }

    func submitRefund(client: APIClient) async {
        guard let selectedReturn, let amount = Double(refundAmountText), amount > 0 else {
            errorMessage = "请输入正确的退款金额"
            return
        }
        await submit(client: client) {
            let updated = try await client.addSalesReturnRefund(
                id: selectedReturn.id,
                payload: SalesReturnRefundPayload(
                    amount: amount,
                    method: self.refundMethod.rawValue,
                    referenceNo: self.refundReferenceNo.nilIfBlank
                )
            )
            self.refundReferenceNo = ""
            self.apply(updated)
        }
    }

    func cancelReturn(client: APIClient) async {
        guard let selectedReturn else { return }
        await submit(client: client) {
            let updated = try await client.cancelSalesReturn(id: selectedReturn.id)
            self.apply(updated)
        }
    }

    func createReturn(client: APIClient) async {
        guard let selectedSourceOrder = sourceOrder else {
            errorMessage = "请先选择来源销售单"
            return
        }
        let items = draftItems.compactMap { item -> SalesReturnCreateItemPayload? in
            guard item.quantity > 0, let unitPrice = Double(item.unitPriceText) else { return nil }
            return SalesReturnCreateItemPayload(productId: item.productId, quantity: item.quantity, unitPrice: unitPrice)
        }
        guard !items.isEmpty else {
            errorMessage = "至少选择一条退货商品"
            return
        }
        await submit(client: client) {
            let created = try await client.createSalesReturn(
                payload: SalesReturnCreatePayload(
                    originalOrderId: selectedSourceOrder.id,
                    customerId: selectedSourceOrder.customerId.map { EntityID(rawValue: String($0)) },
                    customerName: selectedSourceOrder.customerName,
                    items: items,
                    notes: self.createNotes.nilIfBlank
                )
            )
            self.mode = .manage
            self.createNotes = ""
            self.sourceOrder = nil
            self.draftItems = []
            self.returns.insert(created, at: 0)
            self.apply(created)
        }
    }

    private func fetchReturns(client: APIClient) async throws -> [SalesReturnRecord] {
        try await client.fetchSalesReturns(
            keyword: keyword.nilIfBlank,
            status: statusFilter.apiValue,
            page: 1,
            size: 20
        )
    }

    private func hydrateSelected() {
        notesDraft = selectedReturn?.notes ?? ""
        if let remaining = selectedReturn?.remainingRefundAmount {
            refundAmountText = String(format: "%.2f", remaining)
        }
    }

    private func apply(_ record: SalesReturnRecord) {
        selectedReturn = record
        if let index = returns.firstIndex(where: { $0.id == record.id }) {
            returns[index] = record
        } else {
            returns.insert(record, at: 0)
        }
        hydrateSelected()
        errorMessage = nil
    }

    private func submit(client: APIClient, operation: @escaping () async throws -> Void) async {
        isSubmitting = true
        defer { isSubmitting = false }
        do {
            try await operation()
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }
}

private struct SalesReturnCard: View {
    let item: SalesReturnRecord
    let isSelected: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(item.returnNo)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                    Text(item.customerName ?? "散客")
                        .font(.system(size: 12))
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                }
                Spacer()
                StatusChip(title: item.statusLabel, tint: item.statusTint)
            }
            HStack {
                Text("退货 \(item.totalAmount.currencyText)")
                Spacer()
                Text("已退 \(item.refundAmount.currencyText)")
            }
            .font(.system(size: 13, weight: .medium))
            .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
        }
        .padding(16)
        .background(
            (isSelected ? ZhihuijiTheme.ColorToken.primary.opacity(0.12) : ZhihuijiTheme.ColorToken.glassHigh),
            in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous)
        )
        .overlay(
            RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous)
                .stroke(ZhihuijiTheme.ColorToken.glassBorder, lineWidth: 0.5)
        )
    }
}

private struct SalesReturnDetailCard: View {
    let item: SalesReturnRecord

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(item.returnNo)
                        .font(.system(size: 18, weight: .bold))
                    Text("来源单 \(item.originalOrderId.rawValue)")
                        .font(.system(size: 12))
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                }
                Spacer()
                StatusChip(title: item.statusLabel, tint: item.statusTint)
            }

            HStack {
                metric("退货金额", item.totalAmount.currencyText)
                metric("已退款", item.refundAmount.currencyText)
                metric("待退款", item.remainingRefundAmount.currencyText)
            }

            Divider()

            ForEach(item.items) { detail in
                VStack(alignment: .leading, spacing: 6) {
                    Text(detail.productName)
                        .font(.system(size: 15, weight: .semibold))
                    Text(detail.productCode)
                        .font(.system(size: 12))
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                    HStack {
                        Text("数量 \(String(format: "%.0f", detail.quantity))")
                        Spacer()
                        Text(detail.amount.currencyText)
                    }
                    .font(.system(size: 13))
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                }
                .padding(12)
                .background(Color.white.opacity(0.4), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
            }

            if let notes = item.notes, !notes.isEmpty {
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

enum SalesReturnMode: String, CaseIterable, Identifiable {
    case manage
    case create

    var id: String { rawValue }

    var title: String {
        switch self {
        case .manage: return "退货单管理"
        case .create: return "新建退货单"
        }
    }
}

enum SalesReturnStatusFilter: String, CaseIterable, Identifiable {
    case all
    case draft
    case confirmed
    case completed
    case cancelled

    var id: String { rawValue }

    var apiValue: Int? {
        switch self {
        case .all: return nil
        case .draft: return 0
        case .confirmed: return 1
        case .completed: return 2
        case .cancelled: return 3
        }
    }

    var title: String {
        switch self {
        case .all: return "全部"
        case .draft: return "草稿"
        case .confirmed: return "确认"
        case .completed: return "完成"
        case .cancelled: return "取消"
        }
    }
}

struct SalesReturnDraftItem: Identifiable {
    let productId: EntityID
    let productCode: String
    let productName: String
    let maxQuantity: Double
    var quantity: Double
    var unitPriceText: String

    var id: EntityID { productId }
}
