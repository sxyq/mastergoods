import SwiftUI

struct SalesEditView: View {
    @Environment(\.appEnvironment) private var env
    @EnvironmentObject private var session: AppSession
    @StateObject private var viewModel = SalesEditViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("销售开单")
                    .font(ZhihuijiTheme.Typography.pageTitle)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)

                if let errorMessage = viewModel.errorMessage {
                    EmptyStateView(title: "销售开单准备失败", message: errorMessage)
                }

                customerSection
                productSection
                summarySection
            }
            .padding(20)
        }
        .navigationTitle("销售开单")
        .task {
            await viewModel.load(client: env.apiClient)
        }
    }

    private var customerSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("客户")
                .font(ZhihuijiTheme.Typography.sectionTitle)
            TextField("搜索客户", text: $viewModel.customerKeyword)
                .fieldBackground()
            ForEach(viewModel.filteredCustomers.prefix(6)) { customer in
                Button {
                    viewModel.selectedCustomer = customer
                } label: {
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(customer.name)
                                .font(ZhihuijiTheme.Typography.cardTitle)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                            Text(customer.phone)
                                .font(ZhihuijiTheme.Typography.caption)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                        }
                        Spacer()
                        if viewModel.selectedCustomer?.id == customer.id {
                            StatusChip(title: "已选", tint: ZhihuijiTheme.ColorToken.primary)
                        }
                    }
                    .padding(14)
                    .background((viewModel.selectedCustomer?.id == customer.id ? ZhihuijiTheme.ColorToken.primary.opacity(0.12) : Color.white.opacity(0.58)), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous))
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
                            Text(product.salePrice.currencyText)
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
                Text("开单明细")
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
                        TextField("单价", text: $item.unitPriceText)
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
                metric("小计", viewModel.subtotal.currencyText)
                metric("优惠", viewModel.discountAmount.currencyText)
                metric("合计", viewModel.total.currencyText)
            }
            TextField("优惠金额", text: $viewModel.discountAmountText)
                .fieldBackground()
            TextField("备注", text: $viewModel.notes, axis: .vertical)
                .fieldBackground()
            PrimaryGlassButton(title: viewModel.isSubmitting ? "创建中..." : "创建销售单", systemImage: "plus.circle.fill", disabled: viewModel.isSubmitting || viewModel.items.isEmpty || !session.hasPermission(.salesWrite)) {
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
}

@MainActor
final class SalesEditViewModel: ObservableObject {
    @Published var isSubmitting = false
    @Published var errorMessage: String?
    @Published var customers: [CustomerRecord] = []
    @Published var products: [ProductRecord] = []
    @Published var customerKeyword = ""
    @Published var productKeyword = ""
    @Published var selectedCustomer: CustomerRecord?
    @Published var items: [EditableSaleItem] = []
    @Published var discountAmountText = "0.00"
    @Published var notes = ""

    var filteredCustomers: [CustomerRecord] {
        guard let keyword = customerKeyword.nilIfBlank?.lowercased() else { return customers }
        return customers.filter { $0.name.lowercased().contains(keyword) || $0.phone.lowercased().contains(keyword) }
    }

    var filteredProducts: [ProductRecord] {
        guard let keyword = productKeyword.nilIfBlank?.lowercased() else { return products }
        return products.filter { $0.name.lowercased().contains(keyword) || $0.code.lowercased().contains(keyword) }
    }

    var subtotal: Double {
        items.reduce(0) { $0 + $1.amount }
    }

    var discountAmount: Double {
        Double(discountAmountText) ?? 0
    }

    var total: Double {
        max(subtotal - discountAmount, 0)
    }

    func load(client: APIClient) async {
        do {
            async let customersTask = client.fetchCustomers(page: 1, size: 30)
            async let productsTask = client.fetchProducts(page: 1, size: 40)
            customers = try await customersTask
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
                EditableSaleItem(
                    productId: product.id,
                    productName: product.name,
                    quantity: 1,
                    unitPriceText: String(format: "%.2f", product.salePrice)
                )
            )
        }
    }

    func removeItem(id: UUID) {
        items.removeAll { $0.id == id }
    }

    func submit(client: APIClient) async {
        let createItems = items.compactMap { item -> SaleOrderCreateItemPayload? in
            guard let unitPrice = Double(item.unitPriceText) else { return nil }
            return SaleOrderCreateItemPayload(productId: item.productId, quantity: item.quantity, unitPrice: unitPrice)
        }
        guard !createItems.isEmpty else {
            errorMessage = "请至少选择一个商品"
            return
        }

        isSubmitting = true
        defer { isSubmitting = false }
        do {
            _ = try await client.createSaleOrder(
                payload: SaleOrderCreatePayload(
                    customerId: selectedCustomer?.id,
                    customerName: selectedCustomer?.name,
                    items: createItems,
                    notes: notes.nilIfBlank,
                    discountAmount: discountAmount
                )
            )
            items = []
            notes = ""
            discountAmountText = "0.00"
            errorMessage = nil
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }
}

struct EditableSaleItem: Identifiable {
    let id = UUID()
    let productId: EntityID
    let productName: String
    var quantity: Double
    var unitPriceText: String

    var amount: Double {
        (Double(unitPriceText) ?? 0) * quantity
    }
}
