import SwiftUI

struct ProductDetailView: View {
    @Environment(\.appEnvironment) private var env
    let productId: EntityID
    @StateObject private var viewModel = ProductDetailViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                if let errorMessage = viewModel.errorMessage {
                    EmptyStateView(title: "商品详情读取失败", message: errorMessage)
                } else if let product = viewModel.product {
                    header(product)

                    NavigationLink {
                        ProductEditView(productId: product.id)
                    } label: {
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text("编辑商品")
                                    .font(.system(size: 16, weight: .semibold))
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                                Text("修改基础字段、价格与库存阈值。")
                                    .font(.system(size: 13))
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                            }
                            Spacer()
                            Image(systemName: "chevron.right")
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                        }
                        .padding(16)
                        .glassCard()
                    }
                    .buttonStyle(.plain)

                    priceSection(product)
                    supplierSection(product)
                    inventorySection(product)
                } else {
                    LoadingStateView(message: "正在加载商品详情...")
                }
            }
            .padding(20)
        }
        .navigationTitle("商品详情")
        .task {
            await viewModel.load(productId: productId, client: env.apiClient)
        }
    }

    private func header(_ product: ProductRecord) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(product.name)
                        .font(.system(size: 22, weight: .bold))
                    Text(product.code)
                        .font(.system(size: 13))
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                }
                Spacer()
                StatusChip(
                    title: product.status == 1 ? "启用" : "停用",
                    tint: product.status == 1 ? ZhihuijiTheme.ColorToken.success : ZhihuijiTheme.ColorToken.warning
                )
            }

            HStack {
                metric("分类", product.categoryName ?? "-")
                metric("单位", product.unitName ?? "-")
                metric("库存", String(format: "%.2f", product.stock))
            }
        }
        .padding(16)
        .glassCard()
    }

    private func priceSection(_ product: ProductRecord) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("价格信息")
                .font(.system(size: 18, weight: .semibold))
            HStack {
                metric("零售价", product.salePrice.currencyText)
                metric("进货价", product.purchasePrice.currencyText)
                metric("安全库存", String(format: "%.2f", product.safeStock))
            }
            if let levels = product.priceLevels, !levels.isEmpty {
                ForEach(levels) { level in
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(level.name)
                                .font(.system(size: 14, weight: .semibold))
                            Text(level.code)
                                .font(.system(size: 11))
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                        }
                        Spacer()
                        Text(level.price.currencyText)
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                    }
                    .padding(12)
                    .glassCard(cornerRadius: 12)
                }
            }
        }
    }

    private func supplierSection(_ product: ProductRecord) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("供应商关系")
                .font(.system(size: 18, weight: .semibold))
            if let defaultSupplier = product.defaultSupplier {
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(defaultSupplier.supplierName)
                            .font(.system(size: 15, weight: .semibold))
                        Text(defaultSupplier.supplierPhone ?? "无联系电话")
                            .font(.system(size: 12))
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                    }
                    Spacer()
                    StatusChip(title: "默认", tint: ZhihuijiTheme.ColorToken.primary)
                }
                .padding(14)
                .glassCard(cornerRadius: 12)
            }
            if let relations = product.supplierRelations, !relations.isEmpty {
                ForEach(relations.filter { !($0.isDefault ?? false) }) { relation in
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(relation.supplierName)
                                .font(.system(size: 14, weight: .semibold))
                            Text(relation.supplierPhone ?? "无联系电话")
                                .font(.system(size: 12))
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                        }
                        Spacer()
                        if let price = relation.lastPurchasePrice {
                            Text(price.currencyText)
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                        }
                    }
                    .padding(12)
                    .glassCard(cornerRadius: 12)
                }
            } else if product.defaultSupplier == nil {
                EmptyStateView(title: "暂无供应商关系", message: "当前商品还没有绑定默认供应商。")
            }
        }
    }

    private func inventorySection(_ product: ProductRecord) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("库存动作")
                .font(.system(size: 18, weight: .semibold))
            NavigationLink {
                InventoryAdjustView()
            } label: {
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("库存调整")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                        Text("当前库存 \(String(format: "%.2f", product.stock))，可直接进入调整与快照。")
                            .font(.system(size: 13))
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                    }
                    Spacer()
                    Image(systemName: "chevron.right")
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                }
                .padding(16)
                .glassCard()
            }
            .buttonStyle(.plain)
        }
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
final class ProductDetailViewModel: ObservableObject {
    @Published var product: ProductRecord?
    @Published var errorMessage: String?

    func load(productId: EntityID, client: APIClient) async {
        do {
            product = try await client.fetchProduct(id: productId)
            errorMessage = nil
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }
}
