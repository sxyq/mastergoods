import SwiftUI

struct InventoryAdjustView: View {
    @Environment(\.appEnvironment) private var env
    @EnvironmentObject private var session: AppSession
    @StateObject private var viewModel = InventoryAdjustViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("库存调整")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)

                Text("直接做真实库存流水调整，同时能顺手生成盘点快照。")
                    .font(.system(size: 14))
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)

                formSection
                productSection
                ledgerSection
            }
            .padding(20)
        }
        .navigationTitle("库存调整")
        .task {
            await viewModel.load(client: env.apiClient)
        }
    }

    private var formSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("新增调整")
                .font(.system(size: 18, weight: .semibold))
            TextField("搜索商品", text: $viewModel.keyword)
                .fieldBackground()
            Picker("来源类型", selection: $viewModel.sourceType) {
                ForEach(InventorySourceType.allCases) { type in
                    Text(type.title).tag(type)
                }
            }
            .pickerStyle(.segmented)
            TextField("数量变更（负数表示扣减）", text: $viewModel.quantityChangeText)
                .fieldBackground()
            TextField("单价（可选）", text: $viewModel.unitCostText)
                .fieldBackground()
            TextField("来源单号（可选）", text: $viewModel.sourceNo)
                .fieldBackground()
            TextField("备注", text: $viewModel.notes, axis: .vertical)
                .fieldBackground()
            HStack(spacing: 12) {
                PrimaryGlassButton(title: viewModel.isSubmitting ? "提交中..." : "提交调整", systemImage: "arrow.left.arrow.right.circle.fill", disabled: viewModel.isSubmitting || !session.hasPermission(.inventoryWrite) || viewModel.selectedProduct == nil) {
                    Task { await viewModel.createAdjustment(client: env.apiClient) }
                }
                PrimaryGlassButton(title: viewModel.isSubmitting ? "处理中..." : "创建快照", systemImage: "camera.metering.center.weighted", disabled: viewModel.isSubmitting || !session.hasPermission(.inventoryWrite) || viewModel.selectedProduct == nil) {
                    Task { await viewModel.createSnapshot(client: env.apiClient) }
                }
            }
            if let errorMessage = viewModel.errorMessage {
                Text(errorMessage)
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(ZhihuijiTheme.ColorToken.danger)
            }
        }
        .padding(16)
        .glassCard()
    }

    private var productSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("选择商品")
                .font(.system(size: 18, weight: .semibold))
            ForEach(viewModel.filteredProducts.prefix(8)) { product in
                Button {
                    Task { await viewModel.selectProduct(product, client: env.apiClient) }
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
                            Text("库存 \(String(format: "%.2f", product.stock))")
                                .font(.system(size: 13, weight: .medium))
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
                .font(.system(size: 18, weight: .semibold))
            if viewModel.ledgerEntries.isEmpty {
                EmptyStateView(title: "暂无库存流水", message: "选中商品后可以查看它的最近库存变化。")
            } else {
                ForEach(viewModel.ledgerEntries.prefix(8)) { entry in
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(entry.productName)
                                .font(.system(size: 15, weight: .semibold))
                            Text(entry.sourceType)
                                .font(.system(size: 12))
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                            Text(entry.createdAt.dateTimeText)
                                .font(.system(size: 11))
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                        }
                        Spacer()
                        VStack(alignment: .trailing, spacing: 4) {
                            Text((entry.quantityChange >= 0 ? "+" : "") + String(format: "%.2f", entry.quantityChange))
                                .font(.system(size: 14, weight: .semibold))
                                .foregroundStyle(entry.quantityChange >= 0 ? ZhihuijiTheme.ColorToken.success : ZhihuijiTheme.ColorToken.danger)
                            Text("结存 \(String(format: "%.2f", entry.quantityAfter ?? 0))")
                                .font(.system(size: 12))
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                        }
                    }
                    .padding(14)
                    .glassCard(cornerRadius: 12)
                }
            }
        }
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
    @Published var errorMessage: String?

    var filteredProducts: [ProductRecord] {
        guard let keyword = keyword.nilIfBlank?.lowercased() else { return products }
        return products.filter {
            $0.name.lowercased().contains(keyword) || $0.code.lowercased().contains(keyword)
        }
    }

    func load(client: APIClient) async {
        isLoading = true
        defer { isLoading = false }
        do {
            products = try await client.fetchProducts(page: 1, size: 40)
            if let first = products.first {
                await selectProduct(first, client: client)
            }
            errorMessage = nil
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    func selectProduct(_ product: ProductRecord, client: APIClient) async {
        selectedProduct = product
        do {
            let now = Int64(Date().timeIntervalSince1970 * 1000)
            let monthAgo = now - 30 * 24 * 60 * 60 * 1000
            ledgerEntries = try await client.fetchInventoryLedger(productId: product.id, startAt: monthAgo, endAt: now)
            unitCostText = String(format: "%.2f", product.purchasePrice)
            errorMessage = nil
        } catch {
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
            errorMessage = nil
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
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
            errorMessage = nil
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
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
    var apiValue: String {
        switch self {
        case .adjust: return "inventory_adjust"
        case .receipt: return "purchase_receipt"
        case .returnOut: return "purchase_return"
        }
    }
}
