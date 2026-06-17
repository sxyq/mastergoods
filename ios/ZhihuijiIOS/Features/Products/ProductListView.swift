import SwiftUI

struct ProductListView: View {
    @Environment(\.appEnvironment) private var env
    @StateObject private var viewModel = ProductListViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                HStack {
                    Text("商品档案")
                        .font(.system(size: 28, weight: .bold))
                    Spacer()
                    NavigationLink {
                        ProductEditView()
                    } label: {
                        StatusChip(title: "新建", tint: ZhihuijiTheme.ColorToken.primary)
                    }
                    .buttonStyle(.plain)
                }

                TextField("搜索商品名称 / 编码", text: $viewModel.keyword)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 14)
                    .background(Color.white.opacity(0.58), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.field, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.field, style: .continuous)
                            .stroke(Color.white.opacity(0.5), lineWidth: 0.5)
                    )

                if let errorMessage = viewModel.errorMessage {
                    EmptyStateView(title: "商品读取失败", message: errorMessage)
                } else if viewModel.products.isEmpty, !viewModel.isLoading {
                    EmptyStateView(title: "暂无商品", message: "当前没有商品档案数据。")
                } else {
                    LazyVStack(spacing: 12) {
                        ForEach(viewModel.products) { product in
                            NavigationLink {
                                ProductDetailView(productId: product.id)
                            } label: {
                                VStack(alignment: .leading, spacing: 8) {
                                    Text(product.name)
                                        .font(.system(size: 16, weight: .semibold))
                                    Text(product.code)
                                        .font(.system(size: 12, weight: .medium))
                                        .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                                    HStack {
                                        Text("库存 \(String(format: "%.2f", product.stock))")
                                        Spacer()
                                        Text("售价 \(product.salePrice.currencyText)")
                                    }
                                    .font(.system(size: 13))
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                                }
                                .padding(16)
                                .background(Color.white.opacity(0.58), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous))
                                .overlay(
                                    RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous)
                                        .stroke(Color.white.opacity(0.5), lineWidth: 0.5)
                                )
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
            }
            .padding(20)
        }
        .navigationTitle("档案")
        .task {
            await viewModel.load(using: env.apiClient)
        }
    }
}

@MainActor
final class ProductListViewModel: ObservableObject {
    @Published var keyword = ""
    @Published var isLoading = false
    @Published var products: [ProductRecord] = []
    @Published var errorMessage: String?

    func load(using client: APIClient) async {
        isLoading = true
        defer { isLoading = false }
        do {
            products = try await client.fetchProducts(keyword: keyword, page: 1, size: 20)
            errorMessage = nil
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }
}
