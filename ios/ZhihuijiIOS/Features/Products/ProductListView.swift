import SwiftUI

struct ProductListView: View {
    @Environment(\.appEnvironment) private var env
    @EnvironmentObject private var session: AppSession
    @StateObject private var viewModel = ProductListViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                HStack {
                    Text("商品档案")
                        .font(ZhihuijiTheme.Typography.pageTitle)
                    Spacer()
                    if session.hasPermission(.archivesWrite) {
                        NavigationLink {
                            ProductEditView()
                        } label: {
                            StatusChip(title: "新建", tint: ZhihuijiTheme.ColorToken.primary)
                        }
                        .buttonStyle(.plain)
                    }
                }

                TextField("搜索商品名称 / 编码", text: $viewModel.keyword)
                    .fieldBackground()

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
                                        .font(ZhihuijiTheme.Typography.bodyMedium)
                                    Text(product.code)
                                        .font(ZhihuijiTheme.Typography.captionSemibold)
                                        .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                                    HStack {
                                        Text("库存 \(String(format: "%.2f", product.stock))")
                                        Spacer()
                                        Text("售价 \(product.salePrice.currencyText)")
                                    }
                                    .font(ZhihuijiTheme.Typography.body)
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                                }
                                .padding(16)
                                .glassCard()
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
            products = []
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }
}
