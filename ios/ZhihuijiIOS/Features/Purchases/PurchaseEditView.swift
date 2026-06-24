import SwiftUI

struct PurchaseEditView: View {
    @Environment(\.appEnvironment) private var env
    @EnvironmentObject private var session: AppSession
    @StateObject private var viewModel = PurchaseEditViewModel()

    private var actionPolicy: PurchaseEditActionPolicy {
        PurchaseEditActionPolicy.resolve(for: session.permissions)
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("采购开单")
                    .font(ZhihuijiTheme.Typography.pageTitle)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)

                if let errorMessage = viewModel.errorMessage {
                    EmptyStateView(title: "采购开单准备失败", message: errorMessage)
                }

                supplierSection
                settlementSection
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
                .font(ZhihuijiTheme.Typography.sectionTitle)
            TextField("搜索供应商", text: $viewModel.supplierKeyword)
                .fieldBackground()
            ForEach(viewModel.filteredSuppliers.prefix(6)) { supplier in
                Button {
                    viewModel.selectedSupplier = supplier
                } label: {
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(supplier.name)
                                .font(ZhihuijiTheme.Typography.cardTitle)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                            Text(supplier.phone)
                                .font(ZhihuijiTheme.Typography.caption)
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

    private var settlementSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("结算与入库")
                .font(ZhihuijiTheme.Typography.sectionTitle)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)

            HStack(spacing: 12) {
                settlementMethodField
                disabledSelectField(title: "入库仓库", value: "默认仓库")
            }
        }
        .padding(16)
        .glassCard()
    }

    private var settlementMethodField: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("结算方式")
                .font(ZhihuijiTheme.Typography.captionSemibold)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
            Picker("结算方式", selection: $viewModel.settlementMethod) {
                Text("现金").tag(1)
                Text("银行转账").tag(2)
                Text("支票").tag(3)
                Text("其他").tag(4)
            }
            .pickerStyle(.menu)
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(Color.white.opacity(0.34), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.cardSmall, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.cardSmall, style: .continuous)
                    .stroke(Color.white.opacity(0.52), lineWidth: ZhihuijiTheme.Stroke.hairline)
            )
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var productSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("商品")
                .font(ZhihuijiTheme.Typography.sectionTitle)
            TextField("搜索商品", text: $viewModel.productKeyword)
                .fieldBackground()
            ForEach(viewModel.filteredProducts.prefix(8)) { product in
                Button {
                    viewModel.addOrIncrementProduct(product)
                } label: {
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(product.name)
                                .font(ZhihuijiTheme.Typography.cardTitle)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                            Text(product.code)
                                .font(ZhihuijiTheme.Typography.caption)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                        }
                        Spacer()
                        VStack(alignment: .trailing, spacing: 4) {
                            Text(product.purchasePrice.currencyText)
                                .font(ZhihuijiTheme.Typography.captionSemibold)
                            Text("库存 \(String(format: "%.2f", product.stock))")
                                .font(ZhihuijiTheme.Typography.caption)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                        }
                    }
                    .padding(14)
                    .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
                }
                .buttonStyle(.plain)
            }

            if !viewModel.items.isEmpty {
                Text("采购明细")
                    .font(ZhihuijiTheme.Typography.bodyMedium)
                ForEach($viewModel.items) { $item in
                    VStack(alignment: .leading, spacing: 10) {
                        Text(item.productName)
                            .font(ZhihuijiTheme.Typography.cardTitle)
                        HStack {
                            Stepper(value: $item.quantity, in: 1 ... 999, step: 1) {
                                Text("数量 \(String(format: "%.0f", item.quantity))")
                                    .font(ZhihuijiTheme.Typography.body)
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                            }
                            Spacer()
                            Button("移除") {
                                viewModel.removeItem(id: item.id)
                            }
                            .font(ZhihuijiTheme.Typography.captionSemibold)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.danger)
                        }
                        TextField("单价", text: $item.unitCostText)
                            .fieldBackground()
                    }
                    .padding(14)
                    .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
                }
            }
        }
        .padding(16)
        .glassCard()
    }

    private var summarySection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("金额与备注")
                .font(ZhihuijiTheme.Typography.sectionTitle)
            HStack {
                metric("合计", viewModel.total.currencyText)
                metric("供应商", viewModel.selectedSupplier?.name ?? "未选")
                metric("明细", "\(viewModel.items.count) 项")
            }
            TextField("备注", text: $viewModel.notes, axis: .vertical)
                .fieldBackground()
            Picker("初始状态", selection: $viewModel.initialStatus) {
                Text("草稿").tag(0)
                Text("已入库").tag(1)
            }
            .pickerStyle(.segmented)
            PrimaryGlassButton(title: viewModel.isSubmitting ? "创建中..." : "创建采购单", systemImage: "plus.circle.fill", disabled: viewModel.isSubmitting || viewModel.items.isEmpty || !actionPolicy.canCreatePurchase) {
                Task { await viewModel.submit(client: env.apiClient) }
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

    private func disabledSelectField(title: String, value: String) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title)
                .font(ZhihuijiTheme.Typography.captionSemibold)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
            HStack(spacing: 8) {
                Text(value)
                    .font(ZhihuijiTheme.Typography.caption)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                    .lineLimit(1)
                Spacer()
                Image(systemName: "chevron.down")
                    .font(ZhihuijiTheme.Typography.caption)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 10)
            .background(Color.white.opacity(0.34), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.cardSmall, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.cardSmall, style: .continuous)
                    .stroke(Color.white.opacity(0.52), lineWidth: ZhihuijiTheme.Stroke.hairline)
            )
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
    @Published var settlementMethod = 1

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
                    settlementMethod: settlementMethod,
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
