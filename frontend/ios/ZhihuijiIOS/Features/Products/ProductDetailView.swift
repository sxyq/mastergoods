import SwiftUI

struct ProductDetailView: View {
    @Environment(\.appEnvironment) private var env
    @EnvironmentObject private var session: AppSession
    let productId: EntityID
    @StateObject private var viewModel = ProductDetailViewModel()

    private var actionPolicy: ProductDetailActionPolicy {
        ProductDetailActionPolicy.resolve(for: session.permissions)
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                if let errorMessage = viewModel.errorMessage {
                    EmptyStateView(title: "商品详情读取失败", message: errorMessage)
                } else if let product = viewModel.product {
                    header(product)

                    if actionPolicy.canEditProduct {
                        NavigationLink {
                            ProductEditView(productId: product.id)
                        } label: {
                            HStack {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text("编辑商品")
                                        .font(ZhihuijiTheme.Typography.bodyMedium)
                                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                                    Text("修改基础字段、价格与库存阈值。")
                                        .font(ZhihuijiTheme.Typography.body)
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

                    priceSection(product)
                    supplierSection(product)
                    if actionPolicy.canOpenInventoryAdjust {
                        inventorySection(product)
                    }
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
                        .font(ZhihuijiTheme.Typography.pageTitle)
                    Text(product.code)
                        .font(ZhihuijiTheme.Typography.caption)
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
                .font(ZhihuijiTheme.Typography.sectionTitle)
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
                                .font(ZhihuijiTheme.Typography.bodyMedium)
                            Text(level.code)
                                .font(ZhihuijiTheme.Typography.caption)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                        }
                        Spacer()
                        Text(level.price.currencyText)
                            .font(ZhihuijiTheme.Typography.bodyMedium)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                    }
                    .padding(12)
                    .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
                }
            }
        }
    }

    private func supplierSection(_ product: ProductRecord) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("供应商关系")
                .font(ZhihuijiTheme.Typography.sectionTitle)
            if let defaultSupplier = product.defaultSupplier {
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(defaultSupplier.supplierName)
                            .font(ZhihuijiTheme.Typography.bodyMedium)
                        Text(defaultSupplier.supplierPhone ?? "无联系电话")
                            .font(ZhihuijiTheme.Typography.caption)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                    }
                    Spacer()
                    StatusChip(title: "默认", tint: ZhihuijiTheme.ColorToken.primary)
                }
                .padding(14)
                .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
            }
            if let relations = product.supplierRelations, !relations.isEmpty {
                ForEach(relations.filter { !($0.isDefault ?? false) }) { relation in
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(relation.supplierName)
                                .font(ZhihuijiTheme.Typography.bodyMedium)
                            Text(relation.supplierPhone ?? "无联系电话")
                                .font(ZhihuijiTheme.Typography.caption)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                        }
                        Spacer()
                        if let price = relation.lastPurchasePrice {
                            Text(price.currencyText)
                                .font(ZhihuijiTheme.Typography.bodyMedium)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                        }
                    }
                    .padding(12)
                    .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
                }
            } else if product.defaultSupplier == nil {
                EmptyStateView(title: "暂无供应商关系", message: "当前商品还没有绑定默认供应商。")
            }
        }
    }

    private func inventorySection(_ product: ProductRecord) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("库存动作")
                .font(ZhihuijiTheme.Typography.sectionTitle)
            NavigationLink {
                InventoryAdjustView()
            } label: {
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("库存调整")
                            .font(ZhihuijiTheme.Typography.bodyMedium)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                        Text("当前库存 \(String(format: "%.2f", product.stock))，可直接进入调整与快照。")
                            .font(ZhihuijiTheme.Typography.body)
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
                .font(ZhihuijiTheme.Typography.caption)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
            Text(value)
                .font(ZhihuijiTheme.Typography.bodyMedium)
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
            product = nil
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }
}
