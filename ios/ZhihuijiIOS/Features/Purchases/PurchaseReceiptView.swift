import SwiftUI

struct PurchaseReceiptView: View {
    @Environment(\.appEnvironment) private var env
    @EnvironmentObject private var session: AppSession
    let initialOrderId: EntityID?
    @StateObject private var viewModel = PurchaseReceiptViewModel()

    init(initialOrderId: EntityID? = nil) {
        self.initialOrderId = initialOrderId
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                header

                if let errorMessage = viewModel.errorMessage {
                    EmptyStateView(title: "采购入库加载失败", message: errorMessage)
                }

                Picker("模式", selection: $viewModel.mode) {
                    ForEach(PurchaseReceiptMode.allCases) { mode in
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
        .navigationTitle("采购入库")
        .task {
            await viewModel.load(client: env.apiClient, preferredOrderId: initialOrderId)
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("采购入库")
                .font(ZhihuijiTheme.Typography.pageTitle)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
            Text("对齐安卓的移动工作流：先选采购单，再确认入库明细与状态。")
                .font(ZhihuijiTheme.Typography.body)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
        }
    }

    private var manageSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            TextField("搜索收货单号 / 供应商", text: $viewModel.keyword)
                .fieldBackground()
            Picker("状态", selection: $viewModel.statusFilter) {
                ForEach(PurchaseReceiptStatusFilter.allCases) { filter in
                    Text(filter.title).tag(filter)
                }
            }
            .pickerStyle(.segmented)
            PrimaryGlassButton(title: viewModel.isLoading ? "刷新中..." : "刷新收货单", systemImage: "arrow.clockwise", disabled: viewModel.isLoading) {
                Task { await viewModel.reloadReceipts(client: env.apiClient) }
            }

            if viewModel.receipts.isEmpty, !viewModel.isLoading {
                EmptyStateView(title: "暂无收货单", message: "当前筛选条件下还没有采购入库记录。")
            } else {
                LazyVStack(spacing: 12) {
                    ForEach(viewModel.receipts) { item in
                        Button {
                            Task { await viewModel.selectReceipt(id: item.id, client: env.apiClient) }
                        } label: {
                            PurchaseReceiptCard(item: item, isSelected: viewModel.selectedReceipt?.id == item.id)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }

            if let selected = viewModel.selectedReceipt {
                PurchaseReceiptDetailCard(item: selected)

                if selected.canEditDraft, session.hasPermission(.inventoryWrite) {
                    VStack(alignment: .leading, spacing: 12) {
                        Text("草稿处理")
                            .font(ZhihuijiTheme.Typography.sectionTitle)
                        TextField("备注", text: $viewModel.notesDraft, axis: .vertical)
                            .fieldBackground()
                        HStack(spacing: 12) {
                            PrimaryGlassButton(title: viewModel.isSubmitting ? "保存中..." : "保存草稿", systemImage: "square.and.arrow.down.fill", disabled: viewModel.isSubmitting) {
                                Task { await viewModel.saveDraft(client: env.apiClient) }
                            }
                            PrimaryGlassButton(title: viewModel.isSubmitting ? "确认中..." : "确认入库", systemImage: "checkmark.seal.fill", disabled: viewModel.isSubmitting) {
                                Task { await viewModel.confirmReceipt(client: env.apiClient) }
                            }
                        }
                    }
                    .padding(16)
                    .glassCard()
                }

                if selected.canCancel, session.hasPermission(.inventoryWrite) {
                    PrimaryGlassButton(title: viewModel.isSubmitting ? "取消中..." : "取消收货单", systemImage: "xmark.circle.fill", disabled: viewModel.isSubmitting) {
                        Task { await viewModel.cancelReceipt(client: env.apiClient) }
                    }
                }
            }
        }
        .onChange(of: viewModel.statusFilter) { _, _ in
            Task { await viewModel.reloadReceipts(client: env.apiClient) }
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
                    Text("入库明细")
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
                                Text("入库数量 \(String(format: "%.0f", item.quantity)) / \(String(format: "%.0f", item.maxQuantity))")
                                    .font(ZhihuijiTheme.Typography.body)
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                            }
                            TextField("入库单价", text: $item.unitCostText)
                                .fieldBackground()
                        }
                        .padding(14)
                        .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
                    }
                    TextField("备注", text: $viewModel.createNotes, axis: .vertical)
                        .fieldBackground()
                    PrimaryGlassButton(title: viewModel.isSubmitting ? "创建中..." : "创建收货单", systemImage: "plus.circle.fill", disabled: viewModel.isSubmitting || !session.hasPermission(.inventoryWrite)) {
                        Task { await viewModel.createReceipt(client: env.apiClient) }
                    }
                }
            }
        }
    }
}

@MainActor
final class PurchaseReceiptViewModel: ObservableObject {
    @Published var mode: PurchaseReceiptMode = .manage
    @Published var keyword = ""
    @Published var statusFilter: PurchaseReceiptStatusFilter = .all
    @Published var isLoading = false
    @Published var isSubmitting = false
    @Published var receipts: [PurchaseReceiptRecord] = []
    @Published var selectedReceipt: PurchaseReceiptRecord?
    @Published var purchaseOrders: [PurchaseOrderSummary] = []
    @Published var sourceOrder: PurchaseOrder?
    @Published var draftItems: [PurchaseReceiptDraftItem] = []
    @Published var notesDraft = ""
    @Published var createNotes = ""
    @Published var errorMessage: String?

    func load(client: APIClient, preferredOrderId: EntityID?) async {
        isLoading = true
        defer { isLoading = false }
        do {
            async let receiptsTask = fetchReceipts(client: client)
            async let ordersTask = client.fetchPurchaseOrders(page: 1, size: 20)
            let fetchedReceipts = try await receiptsTask
            let fetchedOrders = try await ordersTask
            receipts = fetchedReceipts
            purchaseOrders = fetchedOrders
            if let selectedId = selectedReceipt?.id {
                selectedReceipt = fetchedReceipts.first(where: { $0.id == selectedId }) ?? fetchedReceipts.first
            } else {
                selectedReceipt = fetchedReceipts.first
            }
            notesDraft = selectedReceipt?.notes ?? ""
            if let preferredOrderId, let preferred = fetchedOrders.first(where: { $0.id == preferredOrderId }) {
                selectSourceOrder(preferred)
                mode = .create
            }
            errorMessage = nil
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    func reloadReceipts(client: APIClient) async {
        isLoading = true
        defer { isLoading = false }
        do {
            receipts = try await fetchReceipts(client: client)
            if let selectedId = selectedReceipt?.id {
                selectedReceipt = receipts.first(where: { $0.id == selectedId }) ?? receipts.first
            } else {
                selectedReceipt = receipts.first
            }
            notesDraft = selectedReceipt?.notes ?? ""
            errorMessage = nil
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    func selectReceipt(id: EntityID, client: APIClient) async {
        do {
            selectedReceipt = try await client.fetchPurchaseReceipt(id: id)
            notesDraft = selectedReceipt?.notes ?? ""
            errorMessage = nil
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    func selectSourceOrder(_ order: PurchaseOrder) {
        sourceOrder = order
        draftItems = order.items.map {
            PurchaseReceiptDraftItem(
                productId: $0.productId,
                productCode: $0.productCode ?? "",
                productName: $0.productName,
                maxQuantity: max($0.quantity, 0),
                quantity: $0.quantity,
                unitCostText: String(format: "%.2f", $0.unitCost)
            )
        }
    }

    func saveDraft(client: APIClient) async {
        guard let selectedReceipt else { return }
        await submit {
            let updated = try await client.updatePurchaseReceiptDraft(id: selectedReceipt.id, payload: PurchaseReceiptDraftPayload(notes: self.notesDraft.nilIfBlank))
            self.apply(updated)
        }
    }

    func confirmReceipt(client: APIClient) async {
        guard let selectedReceipt else { return }
        await submit {
            let updated = try await client.confirmPurchaseReceipt(id: selectedReceipt.id)
            self.apply(updated)
        }
    }

    func cancelReceipt(client: APIClient) async {
        guard let selectedReceipt else { return }
        await submit {
            let updated = try await client.cancelPurchaseReceipt(id: selectedReceipt.id)
            self.apply(updated)
        }
    }

    func createReceipt(client: APIClient) async {
        guard let sourceOrder else {
            errorMessage = "请先选择来源采购单"
            return
        }
        let items = draftItems.compactMap { item -> PurchaseReceiptCreateItemPayload? in
            guard item.quantity > 0, let unitCost = Double(item.unitCostText) else { return nil }
            return PurchaseReceiptCreateItemPayload(
                productId: item.productId,
                productCode: item.productCode.nilIfBlank,
                productName: item.productName.nilIfBlank,
                quantity: item.quantity,
                unitCost: unitCost
            )
        }
        guard !items.isEmpty else {
            errorMessage = "至少保留一条入库商品"
            return
        }
        await submit {
            let created = try await client.createPurchaseReceipt(
                payload: PurchaseReceiptCreatePayload(
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
            self.receipts.insert(created, at: 0)
            self.apply(created)
        }
    }

    private func fetchReceipts(client: APIClient) async throws -> [PurchaseReceiptRecord] {
        try await client.fetchPurchaseReceipts(keyword: keyword.nilIfBlank, status: statusFilter.apiValue, page: 1, size: 20)
    }

    private func apply(_ record: PurchaseReceiptRecord) {
        selectedReceipt = record
        if let index = receipts.firstIndex(where: { $0.id == record.id }) {
            receipts[index] = record
        } else {
            receipts.insert(record, at: 0)
        }
        notesDraft = record.notes ?? ""
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

private struct PurchaseReceiptCard: View {
    let item: PurchaseReceiptRecord
    let isSelected: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(item.receiptNo)
                        .font(ZhihuijiTheme.Typography.cardTitle)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                    Text(item.supplierName ?? "未命名供应商")
                        .font(ZhihuijiTheme.Typography.caption)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                }
                Spacer()
                StatusChip(title: item.statusLabel, tint: item.statusTint)
            }
            Text(item.totalAmount.currencyText)
                .font(ZhihuijiTheme.Typography.bodyMedium)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
        }
        .padding(16)
        .background((isSelected ? ZhihuijiTheme.ColorToken.primary.opacity(0.12) : ZhihuijiTheme.ColorToken.glassHigh), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous).stroke(ZhihuijiTheme.ColorToken.glassBorder, lineWidth: ZhihuijiTheme.Stroke.hairline))
    }
}

private struct PurchaseReceiptDetailCard: View {
    let item: PurchaseReceiptRecord

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(item.receiptNo)
                        .font(ZhihuijiTheme.Typography.pageTitle)
                    Text("来源单 \(item.purchaseOrderId?.rawValue ?? "-")")
                        .font(ZhihuijiTheme.Typography.caption)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                }
                Spacer()
                StatusChip(title: item.statusLabel, tint: item.statusTint)
            }

            Text(item.totalAmount.currencyText)
                .font(ZhihuijiTheme.Typography.bodyMedium)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)

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
}

enum PurchaseReceiptMode: String, CaseIterable, Identifiable {
    case manage
    case create

    var id: String { rawValue }
    var title: String { self == .manage ? "收货单管理" : "新建收货单" }
}

enum PurchaseReceiptStatusFilter: String, CaseIterable, Identifiable {
    case all
    case draft
    case confirmed
    case cancelled

    var id: String { rawValue }
    var title: String {
        switch self {
        case .all: return "全部"
        case .draft: return "草稿"
        case .confirmed: return "确认"
        case .cancelled: return "取消"
        }
    }
    var apiValue: Int? {
        switch self {
        case .all: return nil
        case .draft: return 0
        case .confirmed: return 1
        case .cancelled: return 2
        }
    }
}

struct PurchaseReceiptDraftItem: Identifiable {
    let productId: EntityID?
    let productCode: String
    let productName: String
    let maxQuantity: Double
    var quantity: Double
    var unitCostText: String

    var id: String { [productId?.rawValue ?? productCode, productName].joined(separator: ":") }
}
