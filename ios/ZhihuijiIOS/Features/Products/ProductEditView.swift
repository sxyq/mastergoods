import SwiftUI
import PhotosUI

#if canImport(UIKit)
import UIKit
#endif
#if canImport(Vision)
import Vision
#endif
#if canImport(VisionKit)
import VisionKit
#endif

struct ProductEditView: View {
    @Environment(\.appEnvironment) private var env
    @EnvironmentObject private var session: AppSession
    let productId: EntityID?
    @StateObject private var viewModel = ProductEditViewModel()
    @State private var isSupplierSheetPresented = false
    @State private var isScanBoundaryPresented = false
    @State private var photoItem: PhotosPickerItem?

    private var actionPolicy: ProductEditActionPolicy {
        ProductEditActionPolicy.resolve(for: session.permissions)
    }

    init(productId: EntityID? = nil) {
        self.productId = productId
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text(productId == nil ? "新建商品" : "编辑商品")
                    .font(ZhihuijiTheme.Typography.pageTitle)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)

                if let errorMessage = viewModel.errorMessage {
                    EmptyStateView(title: "商品表单加载失败", message: errorMessage)
                }

                formSection
                productMediaSection
                pricingSection
                relationSection
            }
            .padding(20)
        }
        .navigationTitle("商品编辑")
        .task {
            await viewModel.load(productId: productId, client: env.apiClient)
        }
        .sheet(isPresented: $isSupplierSheetPresented) {
            SupplierPickerSheet(
                suppliers: viewModel.availableSupplierOptions,
                onSelect: { supplier in
                    viewModel.addSupplierRelation(from: supplier)
                    isSupplierSheetPresented = false
                }
            )
        }
        .sheet(isPresented: $isScanBoundaryPresented) {
            #if canImport(VisionKit) && canImport(UIKit)
            ProductCodeScannerSheet { scannedCode in
                viewModel.code = scannedCode
                isScanBoundaryPresented = false
            }
            #else
            ScanBoundaryFallbackSheet(message: ProductEditViewModel.scanBoundaryNotice) {
                isScanBoundaryPresented = false
            }
            #endif
        }
    }

    private var formSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("基础信息")
                .font(ZhihuijiTheme.Typography.sectionTitle)
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

    private var productMediaSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("商品图片")
                        .font(ZhihuijiTheme.Typography.sectionTitle)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                    Text("从相册选择图片上传到 /v2/media/assets/upload，保存商品后可自动绑定。")
                        .font(ZhihuijiTheme.Typography.caption)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
                Spacer()
                StatusChip(
                    title: productId == nil ? "先保存商品" : "可绑定媒体",
                    tint: productId == nil ? ZhihuijiTheme.ColorToken.warning : ZhihuijiTheme.ColorToken.primary
                )
            }

            HStack(spacing: 14) {
                Circle()
                    .fill(ZhihuijiTheme.ColorToken.primary.opacity(0.12))
                    .frame(width: 64, height: 64)
                    .overlay(
                        Image(systemName: viewModel.isUploadingImage ? "arrow.up.circle" : "photo.badge.plus")
                            .font(.system(size: 28, weight: .semibold))
                            .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                    )

                VStack(alignment: .leading, spacing: 8) {
                    Text(productId == nil ? "可先上传图片，保存商品后再绑定" : "上传图片后自动绑定到当前商品")
                        .font(ZhihuijiTheme.Typography.bodyMedium)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                    if let uploadMessage = viewModel.uploadMessage {
                        Text(uploadMessage)
                            .font(ZhihuijiTheme.Typography.caption)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                            .fixedSize(horizontal: false, vertical: true)
                    } else {
                        Text("支持 JPG / PNG 等常见图片格式，上传后调用媒体绑定接口关联商品。")
                            .font(ZhihuijiTheme.Typography.caption)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }
            }

            HStack(spacing: 10) {
                let uploadingImage = viewModel.isUploadingImage
                PhotosPicker(selection: $photoItem, matching: .images) {
                    Label(uploadingImage ? "上传中..." : "选择图片上传", systemImage: "square.and.arrow.up")
                        .font(ZhihuijiTheme.Typography.captionSemibold)
                }
                .buttonStyle(.plain)
                .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                .padding(.horizontal, 12)
                .padding(.vertical, 9)
                .background(Color.white.opacity(0.54), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.cardSmall, style: .continuous))
                .disabled(uploadingImage)
                .opacity(uploadingImage ? 0.5 : 1)

                Button {
                    isScanBoundaryPresented = true
                } label: {
                    Label("扫码商品编码", systemImage: "qrcode.viewfinder")
                        .font(ZhihuijiTheme.Typography.captionSemibold)
                }
                .buttonStyle(.plain)
                .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                .padding(.horizontal, 12)
                .padding(.vertical, 9)
                .background(Color.white.opacity(0.54), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.cardSmall, style: .continuous))

                if let productId {
                    NavigationLink {
                        MediaAssetsView(initialTargetType: "product", initialTargetId: productId.rawValue)
                    } label: {
                        Label("管理商品图片", systemImage: "link")
                            .font(ZhihuijiTheme.Typography.captionSemibold)
                    }
                    .buttonStyle(.plain)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 9)
                    .background(Color.white.opacity(0.54), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.cardSmall, style: .continuous))
                }
            }
        }
        .padding(16)
        .glassCard()
        .onChange(of: photoItem) { _, newValue in
            guard let newValue else { return }
            Task {
                await viewModel.handlePickedPhoto(item: newValue, productId: productId, client: env.apiClient)
                photoItem = nil
            }
        }
    }

    private var pricingSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("价格与库存")
                .font(ZhihuijiTheme.Typography.sectionTitle)
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
                    .font(ZhihuijiTheme.Typography.bodyMedium)
                ForEach($viewModel.priceLevels) { $level in
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(level.name)
                                .font(ZhihuijiTheme.Typography.bodyMedium)
                            Text(level.code)
                                .font(ZhihuijiTheme.Typography.caption)
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
                disabled: viewModel.isSubmitting || !actionPolicy.canSaveProduct
            ) {
                Task { await viewModel.submit(productId: productId, client: env.apiClient) }
            }
        }
        .padding(16)
        .glassCard()
    }

    private var relationSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("供应关系")
                        .font(ZhihuijiTheme.Typography.sectionTitle)
                    Text("沿用 Android 移动端表单语义，保存商品时一并同步默认供应商、优先级和最近进货价。")
                        .font(ZhihuijiTheme.Typography.caption)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                }
                Spacer()
                Button {
                    isSupplierSheetPresented = true
                } label: {
                    StatusChip(title: "添加供应商", tint: ZhihuijiTheme.ColorToken.primary)
                }
                .buttonStyle(.plain)
                .disabled(viewModel.availableSupplierOptions.isEmpty)
                .opacity(viewModel.availableSupplierOptions.isEmpty ? 0.5 : 1)
            }

            if let relationMessage = viewModel.relationMessage {
                infoBanner(text: relationMessage, tint: ZhihuijiTheme.ColorToken.primaryBright)
            }

            if let relationErrorMessage = viewModel.relationErrorMessage {
                infoBanner(text: relationErrorMessage, tint: ZhihuijiTheme.ColorToken.warning)
            }

            if viewModel.supplierRelations.isEmpty {
                EmptyStateView(
                    title: "暂无供应关系",
                    message: viewModel.availableSupplierOptions.isEmpty
                        ? "当前没有可选供应商，请先在供应商档案中补充数据。"
                        : "还没有绑定供应商，可以先添加候选供应商并在首次保存后自动同步。"
                )
            } else {
                ForEach($viewModel.supplierRelations, id: \.id) { $relation in
                    VStack(alignment: .leading, spacing: 12) {
                        HStack(alignment: .top) {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(relation.supplierName)
                                    .font(ZhihuijiTheme.Typography.bodyMedium)
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                                Text(relation.supplierPhone.nilIfBlank ?? "无联系电话")
                                    .font(ZhihuijiTheme.Typography.caption)
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                            }
                            Spacer()
                            if relation.isDefault {
                                StatusChip(title: "默认", tint: ZhihuijiTheme.ColorToken.primary)
                            }
                            Button(role: .destructive) {
                                viewModel.removeSupplierRelation(id: relation.id)
                            } label: {
                                Image(systemName: "trash")
                                    .font(ZhihuijiTheme.Typography.captionSemibold)
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.danger)
                                    .padding(8)
                                    .background(ZhihuijiTheme.ColorToken.danger.opacity(0.10), in: Circle())
                            }
                            .buttonStyle(.plain)
                        }

                        Toggle(
                            "设为默认供应商",
                            isOn: Binding(
                                get: { relation.isDefault },
                                set: { viewModel.setDefaultSupplier(id: relation.id, enabled: $0) }
                            )
                        )
                        .tint(ZhihuijiTheme.ColorToken.primary)

                        HStack(spacing: 12) {
                            TextField("采购优先级", text: $relation.purchasePriorityText)
                                .fieldBackground()
                            TextField("最近进货价", text: $relation.lastPurchasePriceText)
                                .fieldBackground()
                        }

                        TextField("备注", text: $relation.notes, axis: .vertical)
                            .lineLimit(2...4)
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

    private func infoBanner(text: String, tint: Color) -> some View {
        HStack(alignment: .top, spacing: 10) {
            Circle()
                .fill(tint.opacity(0.14))
                .frame(width: 26, height: 26)
                .overlay(
                    Image(systemName: "info.circle.fill")
                        .font(ZhihuijiTheme.Typography.caption)
                        .foregroundStyle(tint)
                )
            Text(text)
                .font(ZhihuijiTheme.Typography.caption)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                .fixedSize(horizontal: false, vertical: true)
            Spacer()
        }
        .padding(12)
        .background(Color.white.opacity(0.42), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.cardSmall, style: .continuous))
    }
}

@MainActor
final class ProductEditViewModel: ObservableObject {
    nonisolated static let scanBoundaryNotice = "支持在兼容设备上扫码识别商品编码并自动回填；若当前设备不支持系统扫码，仍可手动填写商品编码。"

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
    @Published var supplierDirectory: [SupplierRecord] = []
    @Published var supplierRelations: [EditableSupplierRelation] = []
    @Published var loadedProductId: EntityID?
    @Published var relationErrorMessage: String?
    @Published var isUploadingImage = false
    @Published var uploadMessage: String?
    @Published var lastUploadedAsset: MediaAssetRecord?

    var availableSupplierOptions: [SupplierRecord] {
        let selectedIds = Set(supplierRelations.map(\.supplierId))
        return supplierDirectory.filter { !selectedIds.contains($0.id) }
    }

    var relationMessage: String? {
        if loadedProductId == nil {
            return "新建商品时可先配置供应商，首次保存成功后会自动补绑这些关系。"
        }
        if supplierRelations.isEmpty {
            return "当前商品还没有绑定供应商，至少建议保留一个默认供应商。"
        }
        return nil
    }

    func load(productId: EntityID?, client: APIClient) async {
        do {
            async let categoriesTask = client.fetchProductCategories()
            async let unitsTask = client.fetchProductUnits()
            async let levelsTask = client.fetchProductPriceLevels()
            async let suppliersTask = client.fetchSuppliers(page: 1, size: 100)
            categories = try await categoriesTask
            units = try await unitsTask
            let masterLevels = try await levelsTask
            supplierDirectory = try await suppliersTask

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
                supplierRelations = (product.supplierRelations ?? []).map(EditableSupplierRelation.init)
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
                supplierRelations = []
            }
            errorMessage = nil
            relationErrorMessage = nil
        } catch {
            clearLoadedReferenceData()
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    private func clearLoadedReferenceData() {
        categories = []
        units = []
        priceLevels = []
        supplierDirectory = []
        supplierRelations = []
        selectedCategoryId = nil
        selectedUnitId = nil
        loadedProductId = nil
        relationErrorMessage = nil
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

        isSubmitting = true
        defer { isSubmitting = false }
        do {
            relationErrorMessage = nil
            let baseContext = ProductFormContext(
                code: code,
                name: name,
                categoryId: categoryId,
                unitId: unitId,
                salePrice: salePrice,
                purchasePrice: purchasePrice,
                stock: stock,
                safeStock: safeStock,
                status: status
            )
            if let productId {
                let updated = try await client.updateProduct(
                    id: productId,
                    payload: try makePayload(context: baseContext, relationProductId: productId, includeRelations: true)
                )
                loadedProductId = updated.id
            } else {
                let created = try await client.createProduct(
                    payload: try makePayload(context: baseContext, relationProductId: nil, includeRelations: false)
                )
                loadedProductId = created.id
                if !supplierRelations.isEmpty {
                    _ = try await client.updateProduct(
                        id: created.id,
                        payload: try makePayload(context: baseContext, relationProductId: created.id, includeRelations: true)
                    )
                }
            }
            errorMessage = nil
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    func handlePickedPhoto(item: PhotosPickerItem, productId: EntityID?, client: APIClient) async {
        guard let data = try? await item.loadTransferable(type: Data.self), !data.isEmpty else {
            uploadMessage = "无法读取所选图片，请重试。"
            return
        }
        let fileName = "product-image.jpg"
        let mimeType = Self.inferMimeType(for: fileName)

        isUploadingImage = true
        defer { isUploadingImage = false }
        do {
            let asset = try await client.uploadMediaAsset(
                fileData: data,
                fileName: fileName,
                mimeType: mimeType,
                assetType: "product_image"
            )
            lastUploadedAsset = asset

            if let productId {
                _ = try await client.createMediaBinding(
                    payload: MediaBindingCreatePayload(
                        assetId: asset.id,
                        targetType: "product",
                        targetId: productId,
                        sortOrder: nil
                    )
                )
                uploadMessage = "图片已上传并绑定到当前商品。"
            } else {
                uploadMessage = "图片已上传，保存商品后可在管理图片中绑定。"
            }
        } catch {
            uploadMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    private static func inferMimeType(for fileName: String) -> String {
        let lower = fileName.lowercased()
        if lower.hasSuffix(".png") { return "image/png" }
        if lower.hasSuffix(".jpg") || lower.hasSuffix(".jpeg") { return "image/jpeg" }
        if lower.hasSuffix(".gif") { return "image/gif" }
        if lower.hasSuffix(".webp") { return "image/webp" }
        return "image/jpeg"
    }

    func addSupplierRelation(from supplier: SupplierRecord) {
        guard supplierRelations.contains(where: { $0.supplierId == supplier.id }) == false else {
            relationErrorMessage = "该供应商已经加入当前商品。"
            return
        }
        supplierRelations.append(
            EditableSupplierRelation(
                supplierId: supplier.id,
                supplierName: supplier.name,
                supplierPhone: supplier.phone,
                isDefault: supplierRelations.isEmpty,
                purchasePriorityText: String(supplierRelations.count + 1),
                lastPurchasePriceText: purchasePriceText.nilIfBlank ?? "",
                notes: ""
            )
        )
        ensureSingleDefault()
        relationErrorMessage = nil
    }

    func removeSupplierRelation(id: UUID) {
        supplierRelations.removeAll { $0.id == id }
        ensureSingleDefault()
    }

    func setDefaultSupplier(id: UUID, enabled: Bool) {
        guard enabled else {
            if supplierRelations.count == 1 {
                supplierRelations[0].isDefault = true
            } else if let index = supplierRelations.firstIndex(where: { $0.id == id }) {
                supplierRelations[index].isDefault = false
                ensureSingleDefault()
            }
            return
        }
        for index in supplierRelations.indices {
            supplierRelations[index].isDefault = supplierRelations[index].id == id
        }
    }

    private func ensureSingleDefault() {
        guard supplierRelations.isEmpty == false else { return }
        if supplierRelations.contains(where: \.isDefault) == false {
            supplierRelations[0].isDefault = true
        }
        var foundDefault = false
        for index in supplierRelations.indices {
            if supplierRelations[index].isDefault, foundDefault {
                supplierRelations[index].isDefault = false
            } else if supplierRelations[index].isDefault {
                foundDefault = true
            }
        }
    }

    private func makePayload(
        context: ProductFormContext,
        relationProductId: EntityID?,
        includeRelations: Bool
    ) throws -> ProductWritePayload {
        let pricePayloads = try priceLevels.map { level -> ProductPriceLevelWritePayload in
            guard let price = Double(level.priceText) else {
                throw ProductEditValidationError.message("价格层级 \(level.name) 的金额格式不正确。")
            }
            return ProductPriceLevelWritePayload(levelId: level.levelId, price: price)
        }

        let relationPayloads: [ProductSupplierRelationWritePayload]?
        if includeRelations, let relationProductId {
            relationPayloads = try normalizedRelations(productId: relationProductId)
        } else {
            relationPayloads = nil
        }

        return ProductWritePayload(
            code: context.code,
            name: context.name,
            categoryId: context.categoryId,
            unitId: context.unitId,
            salePrice: context.salePrice,
            purchasePrice: context.purchasePrice,
            priceLevels: pricePayloads,
            supplierRelations: relationPayloads,
            stock: context.stock,
            safeStock: context.safeStock,
            status: context.status
        )
    }

    private func normalizedRelations(productId: EntityID) throws -> [ProductSupplierRelationWritePayload] {
        ensureSingleDefault()
        return try supplierRelations.map { relation in
            let priority: Int?
            if let text = relation.purchasePriorityText.nilIfBlank {
                guard let parsed = Int(text) else {
                    throw ProductEditValidationError.message("供应商 \(relation.supplierName) 的采购优先级必须是整数。")
                }
                priority = parsed
            } else {
                priority = nil
            }

            let lastPurchasePrice: Double?
            if let text = relation.lastPurchasePriceText.nilIfBlank {
                guard let parsed = Double(text) else {
                    throw ProductEditValidationError.message("供应商 \(relation.supplierName) 的最近进货价格式不正确。")
                }
                lastPurchasePrice = parsed
            } else {
                lastPurchasePrice = nil
            }

            return ProductSupplierRelationWritePayload(
                productId: productId,
                supplierId: relation.supplierId,
                isDefault: relation.isDefault,
                purchasePriority: priority,
                lastPurchasePrice: lastPurchasePrice,
                notes: relation.notes.nilIfBlank
            )
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

struct EditableSupplierRelation: Identifiable, Equatable {
    let id = UUID()
    let supplierId: EntityID
    let supplierName: String
    let supplierPhone: String
    var isDefault: Bool
    var purchasePriorityText: String
    var lastPurchasePriceText: String
    var notes: String

    init(
        supplierId: EntityID,
        supplierName: String,
        supplierPhone: String,
        isDefault: Bool,
        purchasePriorityText: String,
        lastPurchasePriceText: String,
        notes: String
    ) {
        self.supplierId = supplierId
        self.supplierName = supplierName
        self.supplierPhone = supplierPhone
        self.isDefault = isDefault
        self.purchasePriorityText = purchasePriorityText
        self.lastPurchasePriceText = lastPurchasePriceText
        self.notes = notes
    }

    init(from relation: ProductSupplierRelation) {
        self.supplierId = relation.supplierId
        self.supplierName = relation.supplierName
        self.supplierPhone = relation.supplierPhone ?? ""
        self.isDefault = relation.isDefault ?? false
        self.purchasePriorityText = relation.purchasePriority.map(String.init) ?? ""
        self.lastPurchasePriceText = relation.lastPurchasePrice.map { String(format: "%.2f", $0) } ?? ""
        self.notes = relation.notes ?? ""
    }
}

private struct ProductFormContext {
    let code: String
    let name: String
    let categoryId: EntityID
    let unitId: EntityID
    let salePrice: Double
    let purchasePrice: Double
    let stock: Double
    let safeStock: Double
    let status: Int
}

private struct ScanBoundaryFallbackSheet: View {
    @Environment(\.dismiss) private var dismiss
    let message: String
    let onClose: () -> Void

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    EmptyStateView(
                        title: "当前设备未启用扫码",
                        message: message
                    )
                }
                .padding(20)
            }
            .navigationTitle("扫码商品编码")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("关闭") {
                        dismiss()
                        onClose()
                    }
                }
            }
        }
        .zhihuijiBackground()
    }
}

#if canImport(VisionKit) && canImport(UIKit)
private struct ProductCodeScannerSheet: View {
    @Environment(\.dismiss) private var dismiss
    let onScan: (String) -> Void

    var body: some View {
        NavigationStack {
            ScannerContainerView(
                onScan: { code in
                    onScan(code)
                    dismiss()
                },
                onClose: {
                    dismiss()
                }
            )
            .navigationTitle("扫码商品编码")
            .inlineNavigationTitle()
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("关闭") {
                        dismiss()
                    }
                }
            }
        }
        .ignoresSafeArea()
    }
}

private struct ScannerContainerView: UIViewControllerRepresentable {
    let onScan: (String) -> Void
    let onClose: () -> Void

    func makeUIViewController(context: Context) -> ScannerHostViewController {
        ScannerHostViewController(onScan: onScan, onClose: onClose)
    }

    func updateUIViewController(_ uiViewController: ScannerHostViewController, context: Context) {}
}

private final class ScannerHostViewController: UIViewController, DataScannerViewControllerDelegate {
    private let onScan: (String) -> Void
    private let onClose: () -> Void
    private var didStart = false
    private var scannerController: DataScannerViewController?

    init(onScan: @escaping (String) -> Void, onClose: @escaping () -> Void) {
        self.onScan = onScan
        self.onClose = onClose
        super.init(nibName: nil, bundle: nil)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        nil
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        guard didStart == false else { return }
        didStart = true
        startScannerIfPossible()
    }

    private func startScannerIfPossible() {
        guard DataScannerViewController.isSupported, DataScannerViewController.isAvailable else {
            showUnavailableState()
            return
        }

        do {
            let scanner = try DataScannerViewController(
                recognizedDataTypes: [.barcode(symbologies: [.qr, .ean13, .code128])],
                qualityLevel: .balanced,
                recognizesMultipleItems: false,
                isHighlightingEnabled: true
            )
            scanner.delegate = self
            addChild(scanner)
            view.addSubview(scanner.view)
            scanner.view.frame = view.bounds
            scanner.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
            scanner.didMove(toParent: self)
            try scanner.startScanning()
            scannerController = scanner
        } catch {
            showUnavailableState()
        }
    }

    private func showUnavailableState() {
        let label = UILabel()
        label.numberOfLines = 0
        label.textAlignment = .center
        label.textColor = .secondaryLabel
        label.font = .systemFont(ofSize: 15, weight: .medium)
        label.text = "当前设备暂不支持系统扫码，仍可手动填写商品编码。"
        label.translatesAutoresizingMaskIntoConstraints = false
        view.backgroundColor = .systemBackground
        view.addSubview(label)
        NSLayoutConstraint.activate([
            label.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            label.centerYAnchor.constraint(equalTo: view.centerYAnchor),
            label.leadingAnchor.constraint(greaterThanOrEqualTo: view.leadingAnchor, constant: 24),
            label.trailingAnchor.constraint(lessThanOrEqualTo: view.trailingAnchor, constant: -24)
        ])
    }

    func dataScanner(_ dataScanner: DataScannerViewController, didAdd addedItems: [RecognizedItem], allItems: [RecognizedItem]) {
        handleRecognizedItems(addedItems)
    }

    func dataScanner(_ dataScanner: DataScannerViewController, didUpdate updatedItems: [RecognizedItem], allItems: [RecognizedItem]) {
        handleRecognizedItems(updatedItems)
    }

    private func handleRecognizedItems(_ items: [RecognizedItem]) {
        guard let code = items.compactMap(extractBarcode).first?.trimmingCharacters(in: .whitespacesAndNewlines),
              code.isEmpty == false else {
            return
        }
        onScan(code)
    }

    private func extractBarcode(from item: RecognizedItem) -> String? {
        switch item {
        case let .barcode(barcode):
            return barcode.payloadStringValue
        default:
            return nil
        }
    }
}
#endif

private enum ProductEditValidationError: LocalizedError {
    case message(String)

    var errorDescription: String? {
        switch self {
        case let .message(message):
            return message
        }
    }
}

private struct SupplierPickerSheet: View {
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
                        EmptyStateView(title: "没有可选供应商", message: "当前筛选结果为空，或者这些供应商已经全部绑定。")
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
                                                .font(ZhihuijiTheme.Typography.bodyMedium)
                                                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                                            Text(supplier.phone)
                                                .font(ZhihuijiTheme.Typography.caption)
                                                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                                        }
                                        Spacer()
                                        Image(systemName: "plus.circle.fill")
                                            .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                                    }
                                    .padding(14)
                                    .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
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
