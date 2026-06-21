import SwiftUI

struct InventoryAdjustView: View {
    @Environment(\.appEnvironment) private var env
    @EnvironmentObject private var session: AppSession
    @StateObject private var viewModel = InventoryAdjustViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                headerSection
                formSection
                productSection
                ledgerSection
            }
            .padding(20)
        }
        .navigationTitle("库存调整")
        .safeAreaInset(edge: .bottom) {
            bottomActionBar
        }
        .task {
            await viewModel.load(client: env.apiClient)
        }
    }

    private var headerSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("库存调整")
                .font(ZhihuijiTheme.Typography.pageTitle)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)

            Text("直接做真实库存流水调整，同时能顺手生成盘点快照。")
                .font(ZhihuijiTheme.Typography.body)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)

            if let statusMessage = viewModel.statusMessage {
                inventoryBanner(text: statusMessage, tint: ZhihuijiTheme.ColorToken.success, isError: false)
            }

            if let errorMessage = viewModel.errorMessage {
                inventoryBanner(text: errorMessage, tint: ZhihuijiTheme.ColorToken.danger, isError: true)
            }

            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                MetricCard(
                    title: "可选商品",
                    value: "\(viewModel.products.count)",
                    subtitle: "真实档案",
                    tint: ZhihuijiTheme.ColorToken.primary
                )
                MetricCard(
                    title: "最近流水",
                    value: "\(viewModel.ledgerEntries.count)",
                    subtitle: "当前商品",
                    tint: ZhihuijiTheme.ColorToken.primaryBright
                )
                MetricCard(
                    title: "当前库存",
                    value: viewModel.selectedProduct.map { String(format: "%.2f", $0.stock) } ?? "--",
                    subtitle: viewModel.selectedProduct?.name ?? "未选商品",
                    tint: ZhihuijiTheme.ColorToken.success
                )
                MetricCard(
                    title: "调整后",
                    value: viewModel.previewStockText,
                    subtitle: viewModel.sourceType.title,
                    tint: viewModel.previewDeltaIsNegative ? ZhihuijiTheme.ColorToken.warning : ZhihuijiTheme.ColorToken.primary
                )
            }
        }
    }

    private var formSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("新增调整")
                .font(ZhihuijiTheme.Typography.sectionTitle)
            TextField("搜索商品", text: $viewModel.keyword)
                .fieldBackground()
            Picker("来源类型", selection: $viewModel.sourceType) {
                ForEach(InventorySourceType.allCases) { type in
                    Text(type.title).tag(type)
                }
            }
            .pickerStyle(.segmented)
            Text(viewModel.sourceType.helperText)
                .font(ZhihuijiTheme.Typography.caption)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
            TextField("数量变更（负数表示扣减）", text: $viewModel.quantityChangeText)
                .fieldBackground()
            TextField("单价（可选）", text: $viewModel.unitCostText)
                .fieldBackground()
            TextField("来源单号（可选）", text: $viewModel.sourceNo)
                .fieldBackground()
            TextField("备注", text: $viewModel.notes, axis: .vertical)
                .fieldBackground()
            if let selectedProduct = viewModel.selectedProduct {
                HStack {
                    Text("当前商品：\(selectedProduct.name)")
                        .font(ZhihuijiTheme.Typography.bodyMedium)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                    Spacer()
                    Text("预计结存 \(viewModel.previewStockText)")
                        .font(ZhihuijiTheme.Typography.bodyMedium)
                        .foregroundStyle(viewModel.previewDeltaIsNegative ? ZhihuijiTheme.ColorToken.warning : ZhihuijiTheme.ColorToken.primary)
                }
                .padding(12)
                .background(Color.white.opacity(0.42), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.cardSmall, style: .continuous))
            }
            HStack(spacing: 12) {
                PrimaryGlassButton(title: viewModel.isSubmitting ? "提交中..." : "提交调整", systemImage: "arrow.left.arrow.right.circle.fill", disabled: viewModel.isSubmitting || !session.hasPermission(.inventoryWrite) || viewModel.selectedProduct == nil) {
                    Task { await viewModel.createAdjustment(client: env.apiClient) }
                }
                PrimaryGlassButton(title: viewModel.isSubmitting ? "处理中..." : "创建快照", systemImage: "camera.metering.center.weighted", disabled: viewModel.isSubmitting || !session.hasPermission(.inventoryWrite) || viewModel.selectedProduct == nil) {
                    Task { await viewModel.createSnapshot(client: env.apiClient) }
                }
            }
        }
        .padding(16)
        .glassCard()
    }

    private var productSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("选择商品")
                .font(ZhihuijiTheme.Typography.sectionTitle)
            if viewModel.filteredProducts.isEmpty, !viewModel.isLoading {
                EmptyStateView(title: "没有找到匹配商品", message: "换个关键词，或者先刷新商品档案。")
            }
            ForEach(viewModel.filteredProducts.prefix(8)) { product in
                Button {
                    Task { await viewModel.selectProduct(product, client: env.apiClient) }
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
                            Text("库存 \(String(format: "%.2f", product.stock))")
                                .font(ZhihuijiTheme.Typography.bodyMedium)
                            if let selected = viewModel.selectedProduct, selected.id == product.id {
                                StatusChip(title: "已选", tint: ZhihuijiTheme.ColorToken.primary)
                            }
                        }
                    }
                    .padding(14)
                    .background((viewModel.selectedProduct?.id == product.id ? ZhihuijiTheme.ColorToken.primary.opacity(0.12) : Color.white.opacity(0.58)), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous))
                }
                .buttonStyle(.plain)
            }
        }
    }

    private var ledgerSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("最近库存流水")
                .font(ZhihuijiTheme.Typography.sectionTitle)
            if viewModel.ledgerEntries.isEmpty {
                EmptyStateView(title: "暂无库存流水", message: "选中商品后可以查看它的最近库存变化。")
            } else {
                ForEach(viewModel.ledgerEntries.prefix(8)) { entry in
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(entry.productName)
                                .font(ZhihuijiTheme.Typography.cardTitle)
                            Text(entry.sourceType)
                                .font(ZhihuijiTheme.Typography.caption)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                            Text(entry.createdAt.dateTimeText)
                                .font(ZhihuijiTheme.Typography.caption)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                        }
                        Spacer()
                        VStack(alignment: .trailing, spacing: 4) {
                            Text((entry.quantityChange >= 0 ? "+" : "") + String(format: "%.2f", entry.quantityChange))
                                .font(ZhihuijiTheme.Typography.bodyMedium)
                                .foregroundStyle(entry.quantityChange >= 0 ? ZhihuijiTheme.ColorToken.success : ZhihuijiTheme.ColorToken.danger)
                            Text("结存 \(String(format: "%.2f", entry.quantityAfter ?? 0))")
                                .font(ZhihuijiTheme.Typography.caption)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                        }
                    }
                    .padding(14)
                    .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
                }
            }
        }
    }

    private var bottomActionBar: some View {
        HStack(spacing: 12) {
            InventorySecondaryActionButton(
                title: viewModel.isLoading ? "刷新中..." : "刷新",
                systemImage: "arrow.clockwise",
                tint: ZhihuijiTheme.ColorToken.primary,
                disabled: viewModel.isLoading
            ) {
                Task { await viewModel.load(client: env.apiClient) }
            }

            PrimaryGlassButton(
                title: viewModel.isSubmitting ? "提交中..." : "提交调整",
                systemImage: "arrow.left.arrow.right.circle.fill",
                disabled: viewModel.isSubmitting || !session.hasPermission(.inventoryWrite) || viewModel.selectedProduct == nil || viewModel.quantityChangeText.nilIfBlank == nil
            ) {
                Task { await viewModel.createAdjustment(client: env.apiClient) }
            }
        }
        .padding(.horizontal, 20)
        .padding(.top, 12)
        .padding(.bottom, 12)
        .background(.ultraThinMaterial)
        .overlay(alignment: .top) {
            Rectangle()
                .fill(Color.white.opacity(0.42))
                .frame(height: 0.5)
        }
    }

    private func inventoryBanner(text: String, tint: Color, isError: Bool) -> some View {
        HStack(alignment: .top, spacing: 10) {
            Circle()
                .fill(tint.opacity(0.15))
                .frame(width: 26, height: 26)
                .overlay(
                    Image(systemName: isError ? "exclamationmark.triangle.fill" : "checkmark.circle.fill")
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
final class InventoryAdjustViewModel: ObservableObject {
    @Published var isLoading = false
    @Published var isSubmitting = false
    @Published var keyword = ""
    @Published var products: [ProductRecord] = []
    @Published var selectedProduct: ProductRecord?
    @Published var ledgerEntries: [InventoryLedgerEntry] = []
    @Published var sourceType: InventorySourceType = .adjust
    @Published var quantityChangeText = ""
    @Published var unitCostText = ""
    @Published var sourceNo = ""
    @Published var notes = ""
    @Published var statusMessage: String?
    @Published var errorMessage: String?

    var filteredProducts: [ProductRecord] {
        guard let keyword = keyword.nilIfBlank?.lowercased() else { return products }
        return products.filter {
            $0.name.lowercased().contains(keyword) || $0.code.lowercased().contains(keyword)
        }
    }

    var previewDelta: Double {
        Double(quantityChangeText) ?? 0
    }

    var previewStockText: String {
        guard let selectedProduct else { return "--" }
        return String(format: "%.2f", selectedProduct.stock + previewDelta)
    }

    var previewDeltaIsNegative: Bool {
        previewDelta < 0
    }

    func load(client: APIClient) async {
        isLoading = true
        statusMessage = nil
        errorMessage = nil
        defer { isLoading = false }
        do {
            products = try await client.fetchProducts(page: 1, size: 40)
            if let first = products.first {
                await selectProduct(first, client: client)
            } else {
                selectedProduct = nil
                ledgerEntries = []
            }
            errorMessage = nil
        } catch {
            products = []
            selectedProduct = nil
            ledgerEntries = []
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    func selectProduct(_ product: ProductRecord, client: APIClient) async {
        selectedProduct = product
        do {
            let now = Int64(Date().timeIntervalSince1970 * 1000)
            let monthAgo = now - 30 * 24 * 60 * 60 * 1000
            ledgerEntries = try await client.fetchInventoryLedger(productId: product.id, startAt: monthAgo, endAt: now, page: 1, size: 50)
            unitCostText = String(format: "%.2f", product.purchasePrice)
            errorMessage = nil
        } catch {
            ledgerEntries = []
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    func createAdjustment(client: APIClient) async {
        guard let selectedProduct else {
            errorMessage = "请先选择商品"
            return
        }
        guard let quantityChange = Double(quantityChangeText), quantityChange != 0 else {
            errorMessage = "请输入非 0 的调整数量"
            return
        }
        isSubmitting = true
        defer { isSubmitting = false }
        do {
            let created = try await client.createInventoryLedgerEntry(
                payload: InventoryLedgerCreatePayload(
                    productId: selectedProduct.id,
                    sourceType: sourceType.apiValue,
                    sourceId: nil,
                    sourceNo: sourceNo.nilIfBlank,
                    quantityChange: quantityChange,
                    unitCost: Double(unitCostText),
                    warehouseId: nil,
                    notes: notes.nilIfBlank
                )
            )
            ledgerEntries.insert(created, at: 0)
            quantityChangeText = ""
            sourceNo = ""
            notes = ""
            products = try await client.fetchProducts(page: 1, size: 40)
            if let refreshed = products.first(where: { $0.id == selectedProduct.id }) {
                self.selectedProduct = refreshed
            }
            statusMessage = "已完成一条\(sourceType.title)流水，最新库存已刷新。"
            errorMessage = nil
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
            statusMessage = nil
        }
    }

    func createSnapshot(client: APIClient) async {
        guard let selectedProduct else {
            errorMessage = "请先选择商品"
            return
        }
        isSubmitting = true
        defer { isSubmitting = false }
        do {
            _ = try await client.createInventorySnapshot(
                payload: InventorySnapshotCreatePayload(
                    productId: selectedProduct.id,
                    snapshotDate: Int64(Date().timeIntervalSince1970 * 1000),
                    warehouseId: nil
                )
            )
            statusMessage = "已为 \(selectedProduct.name) 生成实时库存快照。"
            errorMessage = nil
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
            statusMessage = nil
        }
    }
}

private struct InventorySecondaryActionButton: View {
    let title: String
    let systemImage: String
    let tint: Color
    var disabled = false
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                Image(systemName: systemImage)
                    .font(ZhihuijiTheme.Typography.bodyMedium)
                Text(title)
                    .font(ZhihuijiTheme.Typography.bodyMedium)
            }
            .foregroundStyle(disabled ? ZhihuijiTheme.ColorToken.textTertiary : tint)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(
                (disabled ? Color.white.opacity(0.38) : tint.opacity(0.10)),
                in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.pill, style: .continuous)
            )
            .overlay(
                RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.pill, style: .continuous)
                    .stroke(Color.white.opacity(0.45), lineWidth: ZhihuijiTheme.Stroke.hairline)
            )
        }
        .buttonStyle(.plain)
        .disabled(disabled)
        .opacity(disabled ? 0.7 : 1)
    }
}

enum InventorySourceType: String, CaseIterable, Identifiable {
    case adjust
    case receipt
    case returnOut = "return"

    var id: String { rawValue }
    var title: String {
        switch self {
        case .adjust: return "调整"
        case .receipt: return "入库"
        case .returnOut: return "退货"
        }
    }
    var helperText: String {
        switch self {
        case .adjust: return "适合盘盈盘亏、破损、更正库存等人工调整。"
        case .receipt: return "适合补录采购入库带来的数量增加。"
        case .returnOut: return "适合补录采购退货导致的数量扣减。"
        }
    }
    var apiValue: String {
        switch self {
        case .adjust: return "inventory_adjust"
        case .receipt: return "purchase_receipt"
        case .returnOut: return "purchase_return"
        }
    }
}
