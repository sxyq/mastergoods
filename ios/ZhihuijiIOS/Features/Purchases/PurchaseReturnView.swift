import SwiftUI

struct PurchaseReturnView: View {
    @Environment(\.appEnvironment) private var env
    @EnvironmentObject private var session: AppSession
    let initialOrderId: EntityID?
    @StateObject private var viewModel = PurchaseReturnViewModel()

    init(initialOrderId: EntityID? = nil) {
        self.initialOrderId = initialOrderId
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                header

                if let errorMessage = viewModel.errorMessage {
                    EmptyStateView(title: "采购退货加载失败", message: errorMessage)
                }

                Picker("模式", selection: $viewModel.mode) {
                    ForEach(PurchaseReturnMode.allCases) { mode in
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
        .navigationTitle("采购退货")
        .task {
            await viewModel.load(client: env.apiClient, preferredOrderId: initialOrderId)
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("采购退货")
                .font(ZhihuijiTheme.Typography.pageTitle)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
            Text("和销售退货保持同一套移动交互，只是语义切成采购侧。")
                .font(ZhihuijiTheme.Typography.body)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
        }
    }

    private var manageSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            TextField("搜索退货单号 / 供应商", text: $viewModel.keyword)
                .fieldBackground()
            Picker("状态", selection: $viewModel.statusFilter) {
                ForEach(PurchaseReturnStatusFilter.allCases) { filter in
                    Text(filter.title).tag(filter)
                }
            }
            .pickerStyle(.segmented)
            PrimaryGlassButton(title: viewModel.isLoading ? "刷新中..." : "刷新采购退货单", systemImage: "arrow.clockwise", disabled: viewModel.isLoading) {
                Task { await viewModel.reloadReturns(client: env.apiClient) }
            }

            if viewModel.returns.isEmpty, !viewModel.isLoading {
                EmptyStateView(title: "暂无采购退货单", message: "当前筛选条件下还没有采购退货记录。")
            } else {
                LazyVStack(spacing: 12) {
                    ForEach(viewModel.returns) { item in
                        Button {
                            Task { await viewModel.selectReturn(id: item.id, client: env.apiClient) }
                        } label: {
                            PurchaseReturnCard(item: item, isSelected: viewModel.selectedReturn?.id == item.id)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }

            if let selected = viewModel.selectedReturn {
                PurchaseReturnDetailCard(item: selected)

                if selected.canEditDraft, session.hasPermission(.purchaseWrite) {
                    VStack(alignment: .leading, spacing: 12) {
                        Text("草稿处理")
                            .font(ZhihuijiTheme.Typography.sectionTitle)
                        TextField("备注", text: $viewModel.notesDraft, axis: .vertical)
                            .fieldBackground()
                        HStack(spacing: 12) {
                            PrimaryGlassButton(title: viewModel.isSubmitting ? "保存中..." : "保存草稿", systemImage: "square.and.arrow.down.fill", disabled: viewModel.isSubmitting) {
                                Task { await viewModel.saveDraft(client: env.apiClient) }
                            }
                            PrimaryGlassButton(title: viewModel.isSubmitting ? "确认中..." : "确认退货", systemImage: "checkmark.seal.fill", disabled: viewModel.isSubmitting) {
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
                            .font(ZhihuijiTheme.Typography.sectionTitle)
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
                        PrimaryGlassButton(title: viewModel.isSubmitting ? "退款中..." : "提交退款", systemImage: "arrow.uturn.backward.circle.fill", disabled: viewModel.isSubmitting) {
                            Task { await viewModel.submitRefund(client: env.apiClient) }
                        }
                    }
                    .padding(16)
                    .glassCard()
                }

                if selected.canCancel, session.hasPermission(.purchaseWrite) {
                    PrimaryGlassButton(title: viewModel.isSubmitting ? "取消中..." : "取消退货单", systemImage: "xmark.circle.fill", disabled: viewModel.isSubmitting) {
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
            Text("来源采购单")
                .font(ZhihuijiTheme.Typography.sectionTitle)
            if viewModel.purchaseOrders.isEmpty {
                EmptyStateView(title: "暂无可选采购单", message: "请先确保采购单已同步。")
            } else {
                ForEach(viewModel.purchaseOrders.prefix(8)) { order in
                    Button {
                        viewModel.selectSourceOrder(order)
                    } label: {
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(order.orderNo)
                                    .font(ZhihuijiTheme.Typography.cardTitle)
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                                Text(order.supplierName ?? "未命名供应商")
                                    .font(ZhihuijiTheme.Typography.caption)
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                            }
                            Spacer()
                            Text(order.totalAmount.currencyText)
                                .font(ZhihuijiTheme.Typography.bodyMedium)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                        }
                        .padding(14)
                        .background((viewModel.sourceOrder?.id == order.id ? ZhihuijiTheme.ColorToken.primary.opacity(0.12) : Color.white.opacity(0.58)), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }
            }

            if let order = viewModel.sourceOrder {
                VStack(alignment: .leading, spacing: 12) {
                    Text("退货明细")
                        .font(ZhihuijiTheme.Typography.sectionTitle)
                    Text(order.orderNo)
                        .font(ZhihuijiTheme.Typography.body)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                    ForEach($viewModel.draftItems) { $item in
                        VStack(alignment: .leading, spacing: 10) {
                            Text(item.productName)
                                .font(ZhihuijiTheme.Typography.cardTitle)
                            Text(item.productCode)
                                .font(ZhihuijiTheme.Typography.caption)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                            Stepper(value: $item.quantity, in: 0 ... item.maxQuantity, step: 1) {
                                Text("退货数量 \(String(format: "%.0f", item.quantity)) / \(String(format: "%.0f", item.maxQuantity))")
                                    .font(ZhihuijiTheme.Typography.body)
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                            }
                            TextField("退货单价", text: $item.unitCostText)
                                .fieldBackground()
                        }
                        .padding(14)
                        .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
                    }
                    TextField("备注", text: $viewModel.createNotes, axis: .vertical)
                        .fieldBackground()
                    PrimaryGlassButton(title: viewModel.isSubmitting ? "创建中..." : "创建采购退货单", systemImage: "plus.circle.fill", disabled: viewModel.isSubmitting || !session.hasPermission(.purchaseWrite)) {
                        Task { await viewModel.createReturn(client: env.apiClient) }
                    }
                }
            }
        }
    }
}

@MainActor
final class PurchaseReturnViewModel: ObservableObject {
    @Published var mode: PurchaseReturnMode = .manage
    @Published var keyword = ""
    @Published var statusFilter: PurchaseReturnStatusFilter = .all
    @Published var isLoading = false
    @Published var isSubmitting = false
    @Published var returns: [PurchaseReturnRecord] = []
    @Published var selectedReturn: PurchaseReturnRecord?
    @Published var purchaseOrders: [PurchaseOrderSummary] = []
    @Published var sourceOrder: PurchaseOrder?
    @Published var draftItems: [PurchaseReturnDraftItem] = []
    @Published var notesDraft = ""
    @Published var createNotes = ""
    @Published var refundAmountText = ""
    @Published var refundMethod: SalePaymentMethod = .cash
    @Published var refundReferenceNo = ""
    @Published var errorMessage: String?

    func load(client: APIClient, preferredOrderId: EntityID?) async {
        isLoading = true
        defer { isLoading = false }
        do {
            async let returnsTask = fetchReturns(client: client)
            async let ordersTask = client.fetchPurchaseOrders(page: 1, size: 20)
            let fetchedReturns = try await returnsTask
            let fetchedOrders = try await ordersTask
            returns = fetchedReturns
            purchaseOrders = fetchedOrders
            if let selectedId = selectedReturn?.id {
                selectedReturn = fetchedReturns.first(where: { $0.id == selectedId }) ?? fetchedReturns.first
            } else {
                selectedReturn = fetchedReturns.first
            }
            hydrateSelected()
            if let preferredOrderId, let preferred = fetchedOrders.first(where: { $0.id == preferredOrderId }) {
                selectSourceOrder(preferred)
                mode = .create
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
            selectedReturn = try await client.fetchPurchaseReturn(id: id)
            hydrateSelected()
            errorMessage = nil
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    func selectSourceOrder(_ order: PurchaseOrder) {
        sourceOrder = order
        draftItems = order.items.map {
            PurchaseReturnDraftItem(
                productId: $0.productId,
                productCode: $0.productCode ?? "",
                productName: $0.productName,
                maxQuantity: max($0.quantity, 0),
                quantity: 0,
                unitCostText: String(format: "%.2f", $0.unitCost)
            )
        }
    }

    func saveDraft(client: APIClient) async {
        guard let selectedReturn else { return }
        await submit {
            let updated = try await client.updatePurchaseReturnDraft(id: selectedReturn.id, payload: PurchaseReturnDraftPayload(notes: self.notesDraft.nilIfBlank))
            self.apply(updated)
        }
    }

    func confirmReturn(client: APIClient) async {
        guard let selectedReturn else { return }
        await submit {
            let updated = try await client.confirmPurchaseReturn(id: selectedReturn.id, payload: PurchaseReturnConfirmPayload(notes: self.notesDraft.nilIfBlank))
            self.apply(updated)
        }
    }

    func submitRefund(client: APIClient) async {
        guard let selectedReturn, let amount = Double(refundAmountText), amount > 0 else {
            errorMessage = "请输入正确的退款金额"
            return
        }
        await submit {
            let updated = try await client.addPurchaseReturnRefund(id: selectedReturn.id, payload: PurchaseReturnRefundPayload(amount: amount, method: self.refundMethod.rawValue, referenceNo: self.refundReferenceNo.nilIfBlank))
            self.refundReferenceNo = ""
            self.apply(updated)
        }
    }

    func cancelReturn(client: APIClient) async {
        guard let selectedReturn else { return }
        await submit {
            let updated = try await client.cancelPurchaseReturn(id: selectedReturn.id)
            self.apply(updated)
        }
    }

    func createReturn(client: APIClient) async {
        guard let sourceOrder else {
            errorMessage = "请先选择来源采购单"
            return
        }
        let items = draftItems.compactMap { item -> PurchaseReturnCreateItemPayload? in
            guard item.quantity > 0, let unitCost = Double(item.unitCostText) else { return nil }
            return PurchaseReturnCreateItemPayload(
                productId: item.productId,
                productCode: item.productCode.nilIfBlank,
                productName: item.productName.nilIfBlank,
                quantity: item.quantity,
                unitCost: unitCost
            )
        }
        guard !items.isEmpty else {
            errorMessage = "至少选择一条退货商品"
            return
        }
        await submit {
            let created = try await client.createPurchaseReturn(
                payload: PurchaseReturnCreatePayload(
                    purchaseOrderId: sourceOrder.id,
                    supplierId: sourceOrder.supplierId,
                    supplierName: sourceOrder.supplierName,
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

    private func fetchReturns(client: APIClient) async throws -> [PurchaseReturnRecord] {
        try await client.fetchPurchaseReturns(keyword: keyword.nilIfBlank, status: statusFilter.apiValue, page: 1, size: 20)
    }

    private func hydrateSelected() {
        notesDraft = selectedReturn?.notes ?? ""
        if let remaining = selectedReturn?.remainingRefundAmount {
            refundAmountText = String(format: "%.2f", remaining)
        }
    }

    private func apply(_ record: PurchaseReturnRecord) {
        selectedReturn = record
        if let index = returns.firstIndex(where: { $0.id == record.id }) {
            returns[index] = record
        } else {
            returns.insert(record, at: 0)
        }
        hydrateSelected()
        errorMessage = nil
    }

    private func submit(_ operation: @escaping () async throws -> Void) async {
        isSubmitting = true
        defer { isSubmitting = false }
        do {
            try await operation()
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }
}

private struct PurchaseReturnCard: View {
    let item: PurchaseReturnRecord
    let isSelected: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(item.returnNo)
                        .font(ZhihuijiTheme.Typography.cardTitle)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                    Text(item.supplierName ?? "未命名供应商")
                        .font(ZhihuijiTheme.Typography.caption)
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
            .font(ZhihuijiTheme.Typography.bodyMedium)
            .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
        }
        .padding(16)
        .background((isSelected ? ZhihuijiTheme.ColorToken.primary.opacity(0.12) : ZhihuijiTheme.ColorToken.glassHigh), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous).stroke(ZhihuijiTheme.ColorToken.glassBorder, lineWidth: ZhihuijiTheme.Stroke.hairline))
    }
}

private struct PurchaseReturnDetailCard: View {
    let item: PurchaseReturnRecord

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(item.returnNo)
                        .font(ZhihuijiTheme.Typography.pageTitle)
                    Text("来源单 \(item.purchaseOrderId?.rawValue ?? "-")")
                        .font(ZhihuijiTheme.Typography.caption)
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

            ForEach(item.items) { detail in
                VStack(alignment: .leading, spacing: 6) {
                    Text(detail.productName)
                        .font(ZhihuijiTheme.Typography.cardTitle)
                    Text(detail.productCode ?? "")
                        .font(ZhihuijiTheme.Typography.caption)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                    HStack {
                        Text("数量 \(String(format: "%.0f", detail.quantity))")
                        Spacer()
                        Text(detail.amount.currencyText)
                    }
                    .font(ZhihuijiTheme.Typography.body)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                }
                .padding(12)
                .background(Color.white.opacity(0.4), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.cardSmall, style: .continuous))
            }

            if let notes = item.notes, !notes.isEmpty {
                Text(notes)
                    .font(ZhihuijiTheme.Typography.body)
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

enum PurchaseReturnMode: String, CaseIterable, Identifiable {
    case manage
    case create

    var id: String { rawValue }
    var title: String { self == .manage ? "退货单管理" : "新建退货单" }
}

enum PurchaseReturnStatusFilter: String, CaseIterable, Identifiable {
    case all
    case draft
    case confirmed
    case completed
    case cancelled

    var id: String { rawValue }
    var title: String {
        switch self {
        case .all: return "全部"
        case .draft: return "草稿"
        case .confirmed: return "确认"
        case .completed: return "完成"
        case .cancelled: return "取消"
        }
    }
    var apiValue: Int? {
        switch self {
        case .all: return nil
        case .draft: return 0
        case .confirmed: return 1
        case .completed: return 2
        case .cancelled: return 3
        }
    }
}

struct PurchaseReturnDraftItem: Identifiable {
    let productId: EntityID?
    let productCode: String
    let productName: String
    let maxQuantity: Double
    var quantity: Double
    var unitCostText: String

    var id: String { [productId?.rawValue ?? productCode, productName].joined(separator: ":") }
}
