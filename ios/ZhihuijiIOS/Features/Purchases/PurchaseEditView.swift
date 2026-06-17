import SwiftUI

struct PurchaseEditView: View {
    @Environment(\.appEnvironment) private var env
    @StateObject private var viewModel = PurchaseEditViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("采购开单")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)

                if let errorMessage = viewModel.errorMessage {
                    EmptyStateView(title: "采购开单准备失败", message: errorMessage)
                }

                supplierSection
                productSection
                summarySection
            }
            .padding(20)
        }
        .navigationTitle("采购开单")
        .task {
            await viewModel.load(client: env.apiClient)
        }
    }

    private var supplierSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("供应商")
                .font(.system(size: 18, weight: .semibold))
            TextField("搜索供应商", text: $viewModel.supplierKeyword)
                .fieldBackground()
            ForEach(viewModel.filteredSuppliers.prefix(6)) { supplier in
                Button {
                    viewModel.selectedSupplier = supplier
                } label: {
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(supplier.name)
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                            Text(supplier.phone)
                                .font(.system(size: 12))
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                        }
                        Spacer()
                        if viewModel.selectedSupplier?.id == supplier.id {
                            StatusChip(title: "已选", tint: ZhihuijiTheme.ColorToken.primary)
                        }
                    }
                    .padding(14)
                    .background((viewModel.selectedSupplier?.id == supplier.id ? ZhihuijiTheme.ColorToken.primary.opacity(0.12) : Color.white.opacity(0.58)), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous))
                }
                .buttonStyle(.plain)
            }
        }
        .padding(16)
        .glassCard()
    }

    private var productSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("商品")
                .font(.system(size: 18, weight: .semibold))
            TextField("搜索商品", text: $viewModel.productKeyword)
                .fieldBackground()
            ForEach(viewModel.filteredProducts.prefix(8)) { product in
                Button {
                    viewModel.addOrIncrementProduct(product)
                } label: {
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(product.name)
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                            Text(product.code)
                                .font(.system(size: 12))
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                        }
                        Spacer()
                        VStack(alignment: .trailing, spacing: 4) {
                            Text(product.purchasePrice.currencyText)
                                .font(.system(size: 13, weight: .semibold))
                            Text("库存 \(String(format: "%.2f", product.stock))")
                                .font(.system(size: 12))
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                        }
                    }
                    .padding(14)
                    .glassCard(cornerRadius: 12)
                }
                .buttonStyle(.plain)
            }

            if !viewModel.items.isEmpty {
                Text("采购明细")
                    .font(.system(size: 15, weight: .semibold))
                ForEach($viewModel.items) { $item in
                    VStack(alignment: .leading, spacing: 10) {
                        Text(item.productName)
                            .font(.system(size: 15, weight: .semibold))
                        HStack {
                            Stepper(value: $item.quantity, in: 1 ... 999, step: 1) {
                                Text("数量 \(String(format: "%.0f", item.quantity))")
                                    .font(.system(size: 13))
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                            }
                            Spacer()
                            Button("移除") {
                                viewModel.removeItem(id: item.id)
                            }
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundStyle(ZhihuijiTheme.ColorToken.danger)
                        }
                        TextField("单价", text: $item.unitCostText)
                            .fieldBackground()
                    }
                    .padding(14)
                    .glassCard(cornerRadius: 12)
                }
            }
        }
        .padding(16)
        .glassCard()
    }

    private var summarySection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("金额与备注")
                .font(.system(size: 18, weight: .semibold))
            HStack {
                metric("合计", viewModel.total.currencyText)
                metric("供应商", viewModel.selectedSupplier?.name ?? "未选")
            }
            TextField("备注", text: $viewModel.notes, axis: .vertical)
                .fieldBackground()
            Picker("初始状态", selection: $viewModel.initialStatus) {
                Text("草稿").tag(0)
                Text("已入库").tag(1)
            }
            .pickerStyle(.segmented)
            PrimaryGlassButton(title: viewModel.isSubmitting ? "创建中..." : "创建采购单", systemImage: "plus.circle.fill", disabled: viewModel.isSubmitting || viewModel.items.isEmpty) {
                Task { await viewModel.submit(client: env.apiClient) }
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
final class PurchaseEditViewModel: ObservableObject {
    @Published var isSubmitting = false
    @Published var errorMessage: String?
    @Published var suppliers: [SupplierRecord] = []
    @Published var products: [ProductRecord] = []
    @Published var supplierKeyword = ""
    @Published var productKeyword = ""
    @Published var selectedSupplier: SupplierRecord?
    @Published var items: [EditablePurchaseItem] = []
    @Published var notes = ""
    @Published var initialStatus = 0

    var filteredSuppliers: [SupplierRecord] {
        guard let keyword = supplierKeyword.nilIfBlank?.lowercased() else { return suppliers }
        return suppliers.filter { $0.name.lowercased().contains(keyword) || $0.phone.lowercased().contains(keyword) }
    }

    var filteredProducts: [ProductRecord] {
        guard let keyword = productKeyword.nilIfBlank?.lowercased() else { return products }
        return products.filter { $0.name.lowercased().contains(keyword) || $0.code.lowercased().contains(keyword) }
    }

    var total: Double {
        items.reduce(0) { $0 + $1.amount }
    }

    func load(client: APIClient) async {
        do {
            async let suppliersTask = client.fetchSuppliers(page: 1, size: 30)
            async let productsTask = client.fetchProducts(page: 1, size: 40)
            suppliers = try await suppliersTask
            products = try await productsTask
            errorMessage = nil
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    func addOrIncrementProduct(_ product: ProductRecord) {
        if let index = items.firstIndex(where: { $0.productId == product.id }) {
            items[index].quantity += 1
        } else {
            items.append(
                EditablePurchaseItem(
                    productId: product.id,
                    productCode: product.code,
                    productName: product.name,
                    quantity: 1,
                    unitCostText: String(format: "%.2f", product.purchasePrice)
                )
            )
        }
    }

    func removeItem(id: UUID) {
        items.removeAll { $0.id == id }
    }

    func submit(client: APIClient) async {
        let createItems = items.compactMap { item -> PurchaseOrderCreateItemPayload? in
            guard let unitCost = Double(item.unitCostText) else { return nil }
            return PurchaseOrderCreateItemPayload(
                productId: item.productId,
                productCode: item.productCode,
                productName: item.productName,
                quantity: item.quantity,
                unitCost: unitCost
            )
        }
        guard !createItems.isEmpty else {
            errorMessage = "请至少选择一个商品"
            return
        }

        isSubmitting = true
        defer { isSubmitting = false }
        do {
            _ = try await client.createPurchaseOrder(
                payload: PurchaseOrderCreatePayload(
                    supplierId: selectedSupplier?.id,
                    supplierName: selectedSupplier?.name,
                    items: createItems,
                    notes: notes.nilIfBlank,
                    status: initialStatus
                )
            )
            items = []
            notes = ""
            errorMessage = nil
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }
}

struct EditablePurchaseItem: Identifiable {
    let id = UUID()
    let productId: EntityID
    let productCode: String
    let productName: String
    var quantity: Double
    var unitCostText: String

    var amount: Double {
        (Double(unitCostText) ?? 0) * quantity
    }
}
