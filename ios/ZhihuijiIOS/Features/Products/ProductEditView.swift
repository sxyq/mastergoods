import SwiftUI

struct ProductEditView: View {
    @Environment(\.appEnvironment) private var env
    let productId: EntityID?
    @StateObject private var viewModel = ProductEditViewModel()

    init(productId: EntityID? = nil) {
        self.productId = productId
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text(productId == nil ? "新建商品" : "编辑商品")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)

                if let errorMessage = viewModel.errorMessage {
                    EmptyStateView(title: "商品表单加载失败", message: errorMessage)
                }

                formSection
                pricingSection
                relationSection
            }
            .padding(20)
        }
        .navigationTitle("商品编辑")
        .task {
            await viewModel.load(productId: productId, client: env.apiClient)
        }
    }

    private var formSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("基础信息")
                .font(.system(size: 18, weight: .semibold))
            TextField("商品编码", text: $viewModel.code)
                .fieldBackground()
            TextField("商品名称", text: $viewModel.name)
                .fieldBackground()
            Picker("分类", selection: $viewModel.selectedCategoryId) {
                ForEach(viewModel.categories) { item in
                    Text(item.name).tag(Optional(item.id))
                }
            }
            .pickerStyle(.menu)
            .padding(.horizontal, 14)
            .padding(.vertical, 14)
            .background(Color.white.opacity(0.58), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.field, style: .continuous))
            Picker("单位", selection: $viewModel.selectedUnitId) {
                ForEach(viewModel.units) { item in
                    Text(item.name).tag(Optional(item.id))
                }
            }
            .pickerStyle(.menu)
            .padding(.horizontal, 14)
            .padding(.vertical, 14)
            .background(Color.white.opacity(0.58), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.field, style: .continuous))
            Picker("状态", selection: $viewModel.status) {
                Text("停用").tag(0)
                Text("启用").tag(1)
            }
            .pickerStyle(.segmented)
        }
        .padding(16)
        .glassCard()
    }

    private var pricingSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("价格与库存")
                .font(.system(size: 18, weight: .semibold))
            TextField("零售价", text: $viewModel.salePriceText)
                .fieldBackground()
            TextField("进货价", text: $viewModel.purchasePriceText)
                .fieldBackground()
            TextField("当前库存", text: $viewModel.stockText)
                .fieldBackground()
            TextField("安全库存", text: $viewModel.safeStockText)
                .fieldBackground()

            if !viewModel.priceLevels.isEmpty {
                Text("价格层级")
                    .font(.system(size: 15, weight: .semibold))
                ForEach($viewModel.priceLevels) { $level in
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(level.name)
                                .font(.system(size: 14, weight: .semibold))
                            Text(level.code)
                                .font(.system(size: 11))
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                        }
                        Spacer()
                        TextField("价格", text: $level.priceText)
                            .multilineTextAlignment(.trailing)
                            .frame(width: 120)
                            .fieldBackground()
                    }
                }
            }

            PrimaryGlassButton(
                title: viewModel.isSubmitting ? "保存中..." : (productId == nil ? "创建商品" : "保存商品"),
                systemImage: "square.and.arrow.down.fill",
                disabled: viewModel.isSubmitting
            ) {
                Task { await viewModel.submit(productId: productId, client: env.apiClient) }
            }
        }
        .padding(16)
        .glassCard()
    }

    private var relationSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("供应关系预览")
                .font(.system(size: 18, weight: .semibold))
            if viewModel.supplierRelations.isEmpty {
                EmptyStateView(title: "暂无供应关系", message: "当前页面先保留已有供应关系，不在这里新增。")
            } else {
                ForEach(viewModel.supplierRelations, id: \.stableId) { relation in
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(relation.supplierName)
                                .font(.system(size: 14, weight: .semibold))
                            Text(relation.supplierPhone ?? "无联系电话")
                                .font(.system(size: 12))
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                        }
                        Spacer()
                        if relation.isDefault == true {
                            StatusChip(title: "默认", tint: ZhihuijiTheme.ColorToken.primary)
                        }
                    }
                    .padding(12)
                    .glassCard(cornerRadius: 12)
                }
            }
        }
    }
}

@MainActor
final class ProductEditViewModel: ObservableObject {
    @Published var isSubmitting = false
    @Published var errorMessage: String?
    @Published var code = ""
    @Published var name = ""
    @Published var selectedCategoryId: EntityID?
    @Published var selectedUnitId: EntityID?
    @Published var salePriceText = ""
    @Published var purchasePriceText = ""
    @Published var stockText = ""
    @Published var safeStockText = ""
    @Published var status = 1
    @Published var categories: [ProductCategoryRecord] = []
    @Published var units: [ProductUnitRecord] = []
    @Published var priceLevels: [EditablePriceLevel] = []
    @Published var supplierRelations: [ProductSupplierRelation] = []
    @Published var loadedProductId: EntityID?

    func load(productId: EntityID?, client: APIClient) async {
        do {
            async let categoriesTask = client.fetchProductCategories()
            async let unitsTask = client.fetchProductUnits()
            async let levelsTask = client.fetchProductPriceLevels()
            categories = try await categoriesTask
            units = try await unitsTask
            let masterLevels = try await levelsTask

            if let productId {
                let product = try await client.fetchProduct(id: productId)
                loadedProductId = product.id
                code = product.code
                name = product.name
                selectedCategoryId = product.categoryId.map { EntityID(rawValue: String($0)) }
                selectedUnitId = product.unitId.map { EntityID(rawValue: String($0)) }
                salePriceText = String(format: "%.2f", product.salePrice)
                purchasePriceText = String(format: "%.2f", product.purchasePrice)
                stockText = String(format: "%.2f", product.stock)
                safeStockText = String(format: "%.2f", product.safeStock)
                status = product.status
                supplierRelations = product.supplierRelations ?? []
                priceLevels = masterLevels.map { level in
                    let existing = product.priceLevels?.first(where: { $0.levelId == level.id })
                    return EditablePriceLevel(
                        levelId: level.id,
                        code: level.code,
                        name: level.name,
                        priceText: String(format: "%.2f", existing?.price ?? 0)
                    )
                }
            } else {
                if selectedCategoryId == nil { selectedCategoryId = categories.first?.id }
                if selectedUnitId == nil { selectedUnitId = units.first?.id }
                priceLevels = masterLevels.map {
                    EditablePriceLevel(levelId: $0.id, code: $0.code, name: $0.name, priceText: "0.00")
                }
            }
            errorMessage = nil
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    func submit(productId: EntityID?, client: APIClient) async {
        guard let categoryId = selectedCategoryId, let unitId = selectedUnitId else {
            errorMessage = "请选择分类和单位"
            return
        }
        guard
            let salePrice = Double(salePriceText),
            let purchasePrice = Double(purchasePriceText),
            let stock = Double(stockText),
            let safeStock = Double(safeStockText),
            let code = code.nilIfBlank,
            let name = name.nilIfBlank
        else {
            errorMessage = "请填写完整且合法的商品信息"
            return
        }

        let payload = ProductWritePayload(
            code: code,
            name: name,
            categoryId: categoryId,
            unitId: unitId,
            salePrice: salePrice,
            purchasePrice: purchasePrice,
            priceLevels: priceLevels.compactMap { level in
                guard let price = Double(level.priceText) else { return nil }
                return ProductPriceLevelWritePayload(levelId: level.levelId, price: price)
            },
            supplierRelations: supplierRelations.compactMap { relation in
                guard let loadedProductId = loadedProductId ?? productId else { return nil }
                return ProductSupplierRelationWritePayload(
                    productId: loadedProductId,
                    supplierId: relation.supplierId,
                    isDefault: relation.isDefault,
                    purchasePriority: relation.purchasePriority,
                    lastPurchasePrice: relation.lastPurchasePrice,
                    notes: relation.notes
                )
            },
            stock: stock,
            safeStock: safeStock,
            status: status
        )

        isSubmitting = true
        defer { isSubmitting = false }
        do {
            if let productId {
                let updated = try await client.updateProduct(id: productId, payload: payload)
                loadedProductId = updated.id
            } else {
                let created = try await client.createProduct(payload: payload)
                loadedProductId = created.id
            }
            errorMessage = nil
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }
}

struct EditablePriceLevel: Identifiable {
    let levelId: EntityID
    let code: String
    let name: String
    var priceText: String

    var id: EntityID { levelId }
}
