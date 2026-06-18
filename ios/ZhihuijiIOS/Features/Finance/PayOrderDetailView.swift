import SwiftUI

struct PayOrderDetailView: View {
    @Environment(\.appEnvironment) private var env
    let initialOrderId: EntityID?
    let initialKeyword: String
    @StateObject private var viewModel = PayOrderDetailViewModel()
    @State private var isSupplierSheetPresented = false

    init(initialOrderId: EntityID? = nil, initialKeyword: String = "") {
        self.initialOrderId = initialOrderId
        self.initialKeyword = initialKeyword
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("付款单")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)

                TextField("搜索付款单号 / 供应商", text: $viewModel.keyword)
                    .fieldBackground()
                    .onSubmit {
                        Task { await viewModel.load(client: env.apiClient, preferredId: initialOrderId) }
                    }

                Picker("状态", selection: $viewModel.statusFilter) {
                    ForEach(PayOrderStatusFilter.allCases) { filter in
                        Text(filter.title).tag(filter)
                    }
                }
                .pickerStyle(.segmented)

                PrimaryGlassButton(
                    title: viewModel.isLoading ? "刷新中..." : "刷新付款单",
                    systemImage: "arrow.clockwise",
                    disabled: viewModel.isLoading
                ) {
                    Task { await viewModel.load(client: env.apiClient, preferredId: initialOrderId) }
                }

                if let errorMessage = viewModel.errorMessage {
                    EmptyStateView(title: "付款单加载失败", message: errorMessage)
                }

                if let infoMessage = viewModel.infoMessage {
                    infoBanner(text: infoMessage, tint: ZhihuijiTheme.ColorToken.primaryBright)
                }

                if viewModel.orders.isEmpty, !viewModel.isLoading {
                    EmptyStateView(title: "暂无付款单", message: "可以直接在下面新建一张付款单。")
                } else {
                    ForEach(viewModel.orders.prefix(10)) { order in
                        let isSelected = viewModel.selectedOrder?.id == order.id
                        Button {
                            Task { await viewModel.select(id: order.id, client: env.apiClient) }
                        } label: {
                            PayOrderCard(order: order, isSelected: isSelected)
                        }
                        .buttonStyle(.plain)
                    }
                }

                if let order = viewModel.selectedOrder {
                    VStack(alignment: .leading, spacing: 12) {
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(order.orderNo)
                                    .font(.system(size: 18, weight: .bold))
                                Text(order.supplierName ?? "未命名供应商")
                                    .font(.system(size: 13))
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                            }
                            Spacer()
                            StatusChip(title: order.statusLabel, tint: order.statusTint)
                        }
                        HStack {
                            metric("金额", order.amount.currencyText)
                            metric("方式", SalePaymentMethod(rawValue: order.method)?.label ?? "其他")
                            metric("创建", order.createdAt.dateText)
                        }
                        HStack {
                            metric("参考号", order.referenceNo?.nilIfBlank ?? "-")
                            metric("账户", order.accountId?.rawValue ?? "-")
                            metric("更新", order.updatedAt.dateText)
                        }
                        if let notes = order.notes, !notes.isEmpty {
                            Text(notes)
                                .font(.system(size: 13))
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                        }

                        HStack(spacing: 12) {
                            actionButton(title: "设草稿", status: 0)
                            actionButton(title: "设已付", status: 1)
                            actionButton(title: "设取消", status: 2)
                        }
                    }
                    .padding(16)
                    .glassCard()
                }

                createForm
            }
            .padding(20)
        }
        .navigationTitle("付款单")
        .task {
            viewModel.keyword = initialKeyword
            await viewModel.load(client: env.apiClient, preferredId: initialOrderId)
        }
        .onChange(of: viewModel.statusFilter) { _, _ in
            Task { await viewModel.load(client: env.apiClient, preferredId: initialOrderId) }
        }
        .onChange(of: viewModel.createSupplierName) { _, newValue in
            viewModel.syncManualSupplierName(newValue)
        }
        .sheet(isPresented: $isSupplierSheetPresented) {
            PayOrderSupplierSheet(
                suppliers: viewModel.suppliers,
                onSelect: { supplier in
                    viewModel.selectSupplierForCreate(supplier)
                    isSupplierSheetPresented = false
                }
            )
        }
    }

    private var createForm: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("新建付款单")
                .font(.system(size: 18, weight: .semibold))
            Button {
                isSupplierSheetPresented = true
            } label: {
                HStack(spacing: 12) {
                    Circle()
                        .fill(ZhihuijiTheme.ColorToken.warning.opacity(0.14))
                        .frame(width: 38, height: 38)
                        .overlay(
                            Image(systemName: "shippingbox.fill")
                                .foregroundStyle(ZhihuijiTheme.ColorToken.warning)
                        )
                    VStack(alignment: .leading, spacing: 4) {
                        Text(viewModel.selectedSupplier?.name ?? "选择供应商")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                        Text(viewModel.selectedSupplier?.phone ?? "优先从真实供应商档案中选择，可保留手动补录。")
                            .font(.system(size: 12))
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                    }
                    Spacer()
                    Image(systemName: "chevron.right")
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                }
                .padding(14)
                .glassCard(cornerRadius: 12)
            }
            .buttonStyle(.plain)
            TextField("供应商名称", text: $viewModel.createSupplierName)
                .fieldBackground()
            TextField("金额", text: $viewModel.createAmountText)
                .fieldBackground()
            Picker("付款方式", selection: $viewModel.createMethod) {
                ForEach(SalePaymentMethod.allCases) { method in
                    Text(method.label).tag(method)
                }
            }
            .pickerStyle(.menu)
            .padding(.horizontal, 14)
            .padding(.vertical, 14)
            .background(Color.white.opacity(0.58), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.field, style: .continuous))
            TextField("参考号", text: $viewModel.createReferenceNo)
                .fieldBackground()
            TextField("账户 ID（可选）", text: $viewModel.createAccountId)
                .fieldBackground()
            TextField("备注", text: $viewModel.createNotes, axis: .vertical)
                .fieldBackground()
            Picker("初始状态", selection: $viewModel.createStatus) {
                ForEach(PayOrderStatusFilter.creatableCases) { filter in
                    Text(filter.title).tag(filter)
                }
            }
            .pickerStyle(.segmented)
            PrimaryGlassButton(
                title: viewModel.isSubmitting ? "创建中..." : "创建付款单",
                systemImage: "plus.circle.fill",
                disabled: viewModel.isSubmitting
            ) {
                Task { await viewModel.create(client: env.apiClient) }
            }
        }
        .padding(16)
        .glassCard()
    }

    private func infoBanner(text: String, tint: Color) -> some View {
        HStack(alignment: .top, spacing: 10) {
            Circle()
                .fill(tint.opacity(0.14))
                .frame(width: 26, height: 26)
                .overlay(
                    Image(systemName: "info.circle.fill")
                        .font(.system(size: 12))
                        .foregroundStyle(tint)
                )
            Text(text)
                .font(.system(size: 12))
                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                .fixedSize(horizontal: false, vertical: true)
            Spacer()
        }
        .padding(12)
        .background(Color.white.opacity(0.42), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private func actionButton(title: String, status: Int) -> some View {
        Button {
            Task { await viewModel.updateStatus(client: env.apiClient, status: status) }
        } label: {
            Text(title)
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 10)
                .background(Color.white.opacity(0.56), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
        .buttonStyle(.plain)
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
final class PayOrderDetailViewModel: ObservableObject {
    @Published var keyword = ""
    @Published var statusFilter: PayOrderStatusFilter = .all
    @Published var isLoading = false
    @Published var isSubmitting = false
    @Published var orders: [PayOrder] = []
    @Published var selectedOrder: PayOrder?
    @Published var suppliers: [SupplierRecord] = []
    @Published var selectedSupplier: SupplierRecord?
    @Published var createSupplierName = ""
    @Published var createAmountText = ""
    @Published var createMethod: SalePaymentMethod = .bank
    @Published var createReferenceNo = ""
    @Published var createNotes = ""
    @Published var createAccountId = ""
    @Published var createStatus: PayOrderStatusFilter = .draft
    @Published var errorMessage: String?
    @Published var infoMessage: String?

    func load(client: APIClient, preferredId: EntityID?) async {
        isLoading = true
        defer { isLoading = false }
        do {
            async let payOrdersTask = client.fetchPayOrders(
                keyword: keyword.nilIfBlank,
                status: statusFilter.apiValue,
                page: 1,
                size: 20
            )
            async let suppliersTask = client.fetchSuppliers(page: 1, size: 60)
            orders = try await payOrdersTask
            suppliers = try await suppliersTask
            let targetId = preferredId ?? selectedOrder?.id ?? orders.first?.id
            if let targetId {
                await select(id: targetId, client: client)
            }
            errorMessage = nil
            infoMessage = suppliers.isEmpty ? "当前还没有同步到供应商档案，新建付款单时将回退为手动录入名称。" : nil
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    func select(id: EntityID, client: APIClient) async {
        do {
            selectedOrder = try await client.fetchPayOrder(id: id)
            errorMessage = nil
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    func create(client: APIClient) async {
        guard let amount = Double(createAmountText), amount > 0 else {
            errorMessage = "请输入正确的付款金额"
            return
        }
        let supplierName = selectedSupplier?.name ?? createSupplierName.nilIfBlank
        guard let supplierName else {
            errorMessage = "请输入供应商名称"
            return
        }
        isSubmitting = true
        defer { isSubmitting = false }
        do {
            let created = try await client.createPayOrder(
                payload: PayOrderCreatePayload(
                    supplierId: selectedSupplier?.id,
                    supplierName: supplierName,
                    amount: amount,
                    method: createMethod.rawValue,
                    referenceNo: createReferenceNo.nilIfBlank,
                    notes: createNotes.nilIfBlank,
                    accountId: createAccountId.nilIfBlank.map { EntityID(rawValue: $0) },
                    status: createStatus.apiValue ?? 0
                )
            )
            orders.insert(created, at: 0)
            selectedOrder = created
            createSupplierName = ""
            createAmountText = ""
            createReferenceNo = ""
            createNotes = ""
            createAccountId = ""
            selectedSupplier = nil
            errorMessage = nil
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    func updateStatus(client: APIClient, status: Int) async {
        guard let selectedOrder else { return }
        isSubmitting = true
        defer { isSubmitting = false }
        do {
            let updated = try await client.updatePayOrderStatus(id: selectedOrder.id, status: status)
            self.selectedOrder = updated
            if let index = orders.firstIndex(where: { $0.id == updated.id }) {
                orders[index] = updated
            }
            errorMessage = nil
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    func selectSupplierForCreate(_ supplier: SupplierRecord) {
        selectedSupplier = supplier
        createSupplierName = supplier.name
    }

    func syncManualSupplierName(_ name: String) {
        guard let selectedSupplier else { return }
        if name != selectedSupplier.name {
            self.selectedSupplier = nil
        }
    }
}

private struct PayOrderCard: View {
    let order: PayOrder
    let isSelected: Bool

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(order.orderNo)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                Text(order.supplierName ?? "未命名供应商")
                    .font(.system(size: 12))
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                Text(order.amount.currencyText)
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
            }
            Spacer()
            StatusChip(title: order.statusLabel, tint: order.statusTint)
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

enum PayOrderStatusFilter: String, CaseIterable, Identifiable {
    case all
    case draft
    case paid
    case cancelled

    static var creatableCases: [PayOrderStatusFilter] { [.draft, .paid, .cancelled] }

    var id: String { rawValue }

    var title: String {
        switch self {
        case .all: return "全部"
        case .draft: return "草稿"
        case .paid: return "已付"
        case .cancelled: return "取消"
        }
    }

    var apiValue: Int? {
        switch self {
        case .all: return nil
        case .draft: return 0
        case .paid: return 1
        case .cancelled: return 2
        }
    }
}

private struct PayOrderSupplierSheet: View {
    @Environment(\.dismiss) private var dismiss
    let suppliers: [SupplierRecord]
    let onSelect: (SupplierRecord) -> Void
    @State private var keyword = ""

    private var filteredSuppliers: [SupplierRecord] {
        guard let keyword = keyword.nilIfBlank?.lowercased() else {
            return suppliers
        }
        return suppliers.filter { supplier in
            supplier.name.lowercased().contains(keyword) || supplier.phone.lowercased().contains(keyword)
        }
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    TextField("搜索供应商名称 / 电话", text: $keyword)
                        .fieldBackground()

                    if filteredSuppliers.isEmpty {
                        EmptyStateView(title: "没有可选供应商", message: "当前门店还没有可用供应商，或者筛选结果为空。")
                    } else {
                        LazyVStack(spacing: 10) {
                            ForEach(filteredSuppliers) { supplier in
                                Button {
                                    onSelect(supplier)
                                } label: {
                                    HStack(spacing: 12) {
                                        Circle()
                                            .fill(ZhihuijiTheme.ColorToken.warning.opacity(0.14))
                                            .frame(width: 40, height: 40)
                                            .overlay(
                                                Image(systemName: "shippingbox.fill")
                                                    .foregroundStyle(ZhihuijiTheme.ColorToken.warning)
                                            )
                                        VStack(alignment: .leading, spacing: 4) {
                                            Text(supplier.name)
                                                .font(.system(size: 15, weight: .semibold))
                                                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                                            Text(supplier.phone)
                                                .font(.system(size: 12))
                                                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                                        }
                                        Spacer()
                                        Image(systemName: "checkmark.circle")
                                            .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                                    }
                                    .padding(14)
                                    .glassCard(cornerRadius: 12)
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                }
                .padding(20)
            }
            .navigationTitle("选择供应商")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("关闭") { dismiss() }
                }
            }
        }
        .zhihuijiBackground()
    }
}
