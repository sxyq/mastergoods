import SwiftUI

struct InventoryLedgerView: View {
    @Environment(\.appEnvironment) private var env
    @StateObject private var viewModel = InventoryLedgerViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("库存台账")
                    .font(ZhihuijiTheme.Typography.pageTitle)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)

                Text("按时间倒序展示库存流水，支持按商品筛选，顶部汇总进出与调整合计。")
                    .font(ZhihuijiTheme.Typography.body)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)

                filterSection
                summarySection

                if let errorMessage = viewModel.errorMessage {
                    EmptyStateView(title: "台账加载失败", message: errorMessage)
                }

                ledgerSection
            }
            .padding(20)
        }
        .navigationTitle("库存台账")
        .task {
            await viewModel.load(using: env.apiClient)
        }
    }

    private var filterSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("筛选条件")
                .font(ZhihuijiTheme.Typography.sectionTitle)

            HStack(spacing: 12) {
                Picker("商品", selection: $viewModel.selectedProductId) {
                    Text("全部商品").tag(EntityID?.none)
                    ForEach(viewModel.products) { product in
                        Text(product.name).tag(EntityID?.some(product.id))
                    }
                }
                .pickerStyle(.menu)
                .fieldBackground()

                PrimaryGlassButton(
                    title: viewModel.isLoading ? "刷新中..." : "查询",
                    systemImage: "line.3.horizontal.decrease.circle",
                    disabled: viewModel.isLoading
                ) {
                    Task { await viewModel.load(using: env.apiClient) }
                }
            }

            if viewModel.selectedProductId != nil {
                Button {
                    viewModel.clearFilter()
                    Task { await viewModel.load(using: env.apiClient) }
                } label: {
                    HStack(spacing: 6) {
                        Image(systemName: "xmark.circle.fill")
                        Text("清除筛选")
                            .font(ZhihuijiTheme.Typography.captionSemibold)
                    }
                    .foregroundStyle(ZhihuijiTheme.ColorToken.warning)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(16)
        .glassCard()
    }

    private var summarySection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("流水汇总")
                .font(ZhihuijiTheme.Typography.sectionTitle)

            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                MetricCard(
                    title: "流水条数",
                    value: "\(viewModel.entries.count)",
                    subtitle: "当前筛选结果",
                    tint: ZhihuijiTheme.ColorToken.primary
                )
                MetricCard(
                    title: "净变化",
                    value: String(format: "%.2f", viewModel.netChange),
                    subtitle: "数量合计",
                    tint: viewModel.netChange >= 0 ? ZhihuijiTheme.ColorToken.success : ZhihuijiTheme.ColorToken.danger
                )
                MetricCard(
                    title: "入库合计",
                    value: String(format: "%.2f", viewModel.totalIn),
                    subtitle: "正向流水",
                    tint: ZhihuijiTheme.ColorToken.success
                )
                MetricCard(
                    title: "出库合计",
                    value: String(format: "%.2f", viewModel.totalOut),
                    subtitle: "负向流水",
                    tint: ZhihuijiTheme.ColorToken.danger
                )
            }
        }
    }

    private var ledgerSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("流水明细")
                    .font(ZhihuijiTheme.Typography.sectionTitle)
                Spacer()
                if viewModel.isLoading {
                    ProgressView()
                        .scaleEffect(0.8)
                }
            }

            if viewModel.entries.isEmpty {
                EmptyStateView(title: "暂无台账流水", message: "当前筛选条件下没有库存流水记录。")
            } else {
                ForEach(viewModel.entries) { entry in
                    ledgerRow(entry)
                }
            }
        }
    }

    private func ledgerRow(_ entry: InventoryLedgerEntry) -> some View {
        HStack(alignment: .top, spacing: 12) {
            Circle()
                .fill(quantityTint(entry.quantityChange).opacity(0.16))
                .frame(width: 36, height: 36)
                .overlay(
                    Image(systemName: entry.quantityChange >= 0 ? "arrow.down.circle.fill" : "arrow.up.circle.fill")
                        .font(ZhihuijiTheme.Typography.bodyMedium)
                        .foregroundStyle(quantityTint(entry.quantityChange))
                )

            VStack(alignment: .leading, spacing: 6) {
                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(entry.productName)
                            .font(ZhihuijiTheme.Typography.cardTitle)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                        if let code = entry.productCode?.nilIfBlank {
                            Text(code)
                                .font(ZhihuijiTheme.Typography.caption)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                        }
                    }
                    Spacer()
                    Text(String(format: "%@%.2f", entry.quantityChange >= 0 ? "+" : "", entry.quantityChange))
                        .font(ZhihuijiTheme.Typography.bodyMedium)
                        .foregroundStyle(quantityTint(entry.quantityChange))
                }

                HStack(spacing: 8) {
                    StatusChip(title: entry.sourceType, tint: sourceTint(entry.sourceType))
                    if let sourceNo = entry.sourceNo?.nilIfBlank {
                        Text(sourceNo)
                            .font(ZhihuijiTheme.Typography.caption)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                    }
                }

                HStack(spacing: 10) {
                    if let before = entry.quantityBefore {
                        Text("前 \(String(format: "%.2f", before))")
                            .font(ZhihuijiTheme.Typography.caption)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                    }
                    if let after = entry.quantityAfter {
                        Text("后 \(String(format: "%.2f", after))")
                            .font(ZhihuijiTheme.Typography.caption)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                    }
                    if let unitCost = entry.unitCost {
                        Text(unitCost.currencyText)
                            .font(ZhihuijiTheme.Typography.caption)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                    }
                }

                if let notes = entry.notes?.nilIfBlank {
                    Text(notes)
                        .font(ZhihuijiTheme.Typography.caption)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                        .fixedSize(horizontal: false, vertical: true)
                }

                Text(entry.createdAt.dateTimeText)
                    .font(ZhihuijiTheme.Typography.caption)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
            }
        }
        .padding(14)
        .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
    }

    private func quantityTint(_ value: Double) -> Color {
        value >= 0 ? ZhihuijiTheme.ColorToken.success : ZhihuijiTheme.ColorToken.danger
    }

    private func sourceTint(_ sourceType: String) -> Color {
        switch sourceType.lowercased() {
        case "sale", "sales", "sales_return", "outbound":
            return ZhihuijiTheme.ColorToken.danger
        case "purchase", "purchase_receipt", "purchase_return", "inbound":
            return ZhihuijiTheme.ColorToken.success
        case "adjust", "adjustment", "snapshot":
            return ZhihuijiTheme.ColorToken.warning
        default:
            return ZhihuijiTheme.ColorToken.primary
        }
    }
}

@MainActor
final class InventoryLedgerViewModel: ObservableObject {
    @Published var isLoading = false
    @Published var entries: [InventoryLedgerEntry] = []
    @Published var products: [ProductRecord] = []
    @Published var selectedProductId: EntityID?
    @Published var errorMessage: String?

    var totalIn: Double {
        entries.filter { $0.quantityChange > 0 }.reduce(0) { $0 + $1.quantityChange }
    }

    var totalOut: Double {
        entries.filter { $0.quantityChange < 0 }.reduce(0) { $0 + abs($1.quantityChange) }
    }

    var netChange: Double {
        entries.reduce(0) { $0 + $1.quantityChange }
    }

    func load(using client: APIClient) async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }

        async let productsTask = capture { try await client.fetchProducts(page: 1, size: 100) }
        async let ledgerTask = capture { try await client.fetchInventoryLedger(productId: self.selectedProductId, page: 1, size: 100) }

        let productsResult = await productsTask
        let ledgerResult = await ledgerTask

        var failures: [String] = []

        switch productsResult {
        case let .success(value):
            products = value
        case .failure:
            products = []
            failures.append("商品列表")
        }

        switch ledgerResult {
        case let .success(value):
            entries = value.sorted(by: { $0.createdAt > $1.createdAt })
        case .failure:
            entries = []
            failures.append("台账流水")
        }

        errorMessage = failures.isEmpty ? nil : "以下分区暂未成功拉取：\(failures.joined(separator: "、"))"
    }

    func clearFilter() {
        selectedProductId = nil
    }

    private func capture<T>(_ operation: @escaping () async throws -> T) async -> Result<T, Error> {
        do {
            return .success(try await operation())
        } catch {
            return .failure(error)
        }
    }
}
