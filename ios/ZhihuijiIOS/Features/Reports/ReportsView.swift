import SwiftUI

struct ReportsView: View {
    @Environment(\.appEnvironment) private var env
    @StateObject private var viewModel = ReportsViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("经营报表")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)

                Picker("时间范围", selection: $viewModel.range) {
                    ForEach(ReportRange.allCases) { range in
                        Text(range.title).tag(range)
                    }
                }
                .pickerStyle(.segmented)

                PrimaryGlassButton(
                    title: viewModel.isLoading ? "刷新中..." : "刷新报表",
                    systemImage: "arrow.clockwise",
                    disabled: viewModel.isLoading
                ) {
                    Task { await viewModel.load(client: env.apiClient) }
                }

                if let errorMessage = viewModel.errorMessage {
                    EmptyStateView(title: "部分报表加载失败", message: errorMessage)
                }

                actionRow
                summaryStrip
                overviewSection
                trendSection
                topProductsSection
                productProfitSection
                customerSection
                riskSection
            }
            .padding(20)
        }
        .navigationTitle("报表")
        .task {
            await viewModel.load(client: env.apiClient)
        }
        .onChange(of: viewModel.range) { _, _ in
            Task { await viewModel.load(client: env.apiClient) }
        }
    }

    private var actionRow: some View {
        HStack(spacing: 10) {
            SecondaryReportActionButton(
                title: "刷新",
                systemImage: "arrow.clockwise",
                tint: ZhihuijiTheme.ColorToken.primary,
                disabled: viewModel.isLoading
            ) {
                Task { await viewModel.load(client: env.apiClient) }
            }

            SecondaryReportActionButton(
                title: "导出 CSV",
                systemImage: "square.and.arrow.up",
                tint: ZhihuijiTheme.ColorToken.success,
                disabled: viewModel.isLoading
            ) {
                viewModel.exportStatus = "当前导出仅在页面内准备了结构，后续可接系统分享或文件写出。"
            }

            SecondaryReportActionButton(
                title: "打印",
                systemImage: "printer.fill",
                tint: ZhihuijiTheme.ColorToken.warning,
                disabled: viewModel.isLoading
            ) {
                viewModel.exportStatus = "当前打印入口已保留，后续可直连系统打印面板。"
            }
        }
    }

    private var summaryStrip: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("报表状态")
                    .font(.system(size: 18, weight: .semibold))
                Spacer()
                StatusChip(title: viewModel.range.title, tint: ZhihuijiTheme.ColorToken.primary)
            }

            if let exportStatus = viewModel.exportStatus {
                Text(exportStatus)
                    .font(.system(size: 12))
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(12)
                    .background(Color.white.opacity(0.42), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
            }

            HStack(spacing: 10) {
                reportChip(title: "销售趋势 \(viewModel.salesTrend.count)", tint: ZhihuijiTheme.ColorToken.primary)
                reportChip(title: "热销商品 \(viewModel.topProducts.count)", tint: ZhihuijiTheme.ColorToken.success)
                reportChip(title: "风险项 \(viewModel.refunds.count + viewModel.stockOutRecords.count + viewModel.lowStockProducts.count)", tint: ZhihuijiTheme.ColorToken.warning)
            }
        }
        .padding(16)
        .glassCard()
    }

    private var overviewSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("经营总览")
                .font(.system(size: 18, weight: .semibold))
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                MetricCard(title: "销售额", value: viewModel.salesSummary?.totalSalesAmount.currencyText ?? "--", subtitle: rangeSubtitle, tint: ZhihuijiTheme.ColorToken.primary)
                MetricCard(title: "毛利", value: viewModel.profitSummary?.estimatedProfitAmount.currencyText ?? "--", subtitle: "估算利润", tint: ZhihuijiTheme.ColorToken.success)
                MetricCard(title: "净现金", value: viewModel.cashflowSummary?.netCashFlow.currencyText ?? "--", subtitle: "现金流", tint: ZhihuijiTheme.ColorToken.warning)
                MetricCard(title: "待收款", value: viewModel.reconciliation?.totalReceivableAmount.currencyText ?? "--", subtitle: "客户应收", tint: ZhihuijiTheme.ColorToken.primaryBright)
            }
        }
    }

    private var trendSection: some View {
        VStack(alignment: .leading, spacing: 12) {
                Text("销售趋势")
                    .font(.system(size: 18, weight: .semibold))
            if viewModel.salesTrend.isEmpty {
                EmptyStateView(title: "暂无趋势数据", message: "当前时间范围没有可用的趋势点。")
            } else {
                VStack(spacing: 10) {
                    ForEach(viewModel.salesTrend) { point in
                        VStack(alignment: .leading, spacing: 6) {
                            HStack {
                                Text(point.startAt.dateText)
                                    .font(.system(size: 12))
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                                Spacer()
                                Text(point.totalSalesAmount.currencyText)
                                    .font(.system(size: 13, weight: .semibold))
                            }
                            GeometryReader { proxy in
                                let width = max(proxy.size.width * viewModel.widthRatio(for: point), 6)
                                ZStack(alignment: .leading) {
                                    Capsule().fill(Color.white.opacity(0.35))
                                    Capsule().fill(ZhihuijiTheme.ColorToken.primary)
                                        .frame(width: width)
                                }
                            }
                            .frame(height: 8)
                            Text("订单 \(point.totalOrderCount)")
                                .font(.system(size: 12))
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                        }
                    }
                }
                .padding(16)
                .glassCard()
            }
        }
    }

    private var topProductsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("热销商品")
                .font(.system(size: 18, weight: .semibold))
            if viewModel.topProducts.isEmpty {
                EmptyStateView(title: "暂无商品分析", message: "当前时间范围没有热销商品数据。")
            } else {
                VStack(spacing: 10) {
                    ForEach(viewModel.topProducts.prefix(5)) { product in
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(product.productName)
                                    .font(.system(size: 15, weight: .semibold))
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                                Text(product.productCode)
                                    .font(.system(size: 12))
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                            }
                            Spacer()
                            VStack(alignment: .trailing, spacing: 4) {
                                Text(product.totalAmount.currencyText)
                                    .font(.system(size: 14, weight: .semibold))
                                Text("销量 \(String(format: "%.0f", product.totalQuantity))")
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

    private var productProfitSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("商品利润")
                .font(.system(size: 18, weight: .semibold))
            if viewModel.productProfits.isEmpty {
                EmptyStateView(title: "暂无商品利润数据", message: "当前时间范围没有可分析的商品利润记录。")
            } else {
                VStack(spacing: 10) {
                    ForEach(viewModel.productProfits.prefix(5)) { item in
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(item.productName)
                                    .font(.system(size: 15, weight: .semibold))
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                                Text(item.productCode)
                                    .font(.system(size: 12))
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                            }
                            Spacer()
                            VStack(alignment: .trailing, spacing: 4) {
                                Text(item.totalProfitAmount.currencyText)
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.success)
                                Text("利润率 \(String(format: "%.1f%%", item.profitRate * 100))")
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

    private var customerSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("客户分析")
                .font(.system(size: 18, weight: .semibold))

            if viewModel.customerSales.isEmpty, viewModel.receivableCustomers.isEmpty {
                EmptyStateView(title: "暂无客户分析", message: "当前时间范围没有可展示的客户经营数据。")
            } else {
                if !viewModel.customerSales.isEmpty {
                    VStack(alignment: .leading, spacing: 10) {
                        Text("客户销售")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                        ForEach(viewModel.customerSales.prefix(4)) { item in
                            HStack {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(item.customerName)
                                        .font(.system(size: 15, weight: .semibold))
                                    Text("订单 \(item.totalOrders)")
                                        .font(.system(size: 12))
                                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                                }
                                Spacer()
                                Text(item.totalAmount.currencyText)
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                            }
                            .padding(14)
                            .glassCard(cornerRadius: 12)
                        }
                    }
                }

                if !viewModel.receivableCustomers.isEmpty {
                    VStack(alignment: .leading, spacing: 10) {
                        Text("应收排行")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                        ForEach(viewModel.receivableCustomers.prefix(4)) { item in
                            HStack {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(item.customerName)
                                        .font(.system(size: 15, weight: .semibold))
                                    Text(item.phone?.nilIfBlank ?? "无联系电话")
                                        .font(.system(size: 12))
                                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                                }
                                Spacer()
                                Text(item.balance.currencyText)
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.warning)
                            }
                            .padding(14)
                            .glassCard(cornerRadius: 12)
                        }
                    }
                }
            }
        }
    }

    private var riskSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("风险提醒")
                .font(.system(size: 18, weight: .semibold))

            if viewModel.refunds.isEmpty, viewModel.stockOutRecords.isEmpty, viewModel.lowStockProducts.isEmpty {
                EmptyStateView(title: "暂无风险项", message: "退款和低库存指标当前都比较平稳。")
            } else {
                VStack(spacing: 10) {
                    ForEach(viewModel.refunds.prefix(3)) { refund in
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(refund.orderNo)
                                    .font(.system(size: 15, weight: .semibold))
                                Text(refund.customerName ?? "散客")
                                    .font(.system(size: 12))
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                            }
                            Spacer()
                            VStack(alignment: .trailing, spacing: 4) {
                                Text(refund.refundAmount.currencyText)
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.danger)
                                Text(refund.createdAt.dateText)
                                    .font(.system(size: 12))
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                            }
                        }
                        .padding(14)
                        .glassCard(cornerRadius: 12)
                    }

                    ForEach(viewModel.stockOutRecords.prefix(3)) { item in
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(item.productName)
                                    .font(.system(size: 15, weight: .semibold))
                                Text(item.customerName?.nilIfBlank ?? item.orderNo)
                                    .font(.system(size: 12))
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                            }
                            Spacer()
                            VStack(alignment: .trailing, spacing: 4) {
                                Text("-" + String(format: "%.2f", item.quantity))
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.warning)
                                Text(item.amount.currencyText)
                                    .font(.system(size: 12))
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                            }
                        }
                        .padding(14)
                        .glassCard(cornerRadius: 12)
                    }

                    ForEach(viewModel.lowStockProducts.prefix(3)) { product in
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(product.productName)
                                    .font(.system(size: 15, weight: .semibold))
                                Text(product.productCode)
                                    .font(.system(size: 12))
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                            }
                            Spacer()
                            VStack(alignment: .trailing, spacing: 4) {
                                Text("库存 \(String(format: "%.2f", product.stock))")
                                    .font(.system(size: 14, weight: .semibold))
                                Text("安全库存 \(String(format: "%.2f", product.safeStock))")
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

    private var rangeSubtitle: String {
        switch viewModel.range {
        case .today: return "今日"
        case .week: return "本周"
        case .month: return "本月"
        }
    }

private func reportChip(title: String, tint: Color) -> some View {
        Text(title)
            .font(.system(size: 12, weight: .semibold))
            .foregroundStyle(tint)
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(tint.opacity(0.12), in: Capsule())
            .overlay(
                Capsule().stroke(Color.white.opacity(0.45), lineWidth: 0.5)
            )
    }
}

private struct SecondaryReportActionButton: View {
    let title: String
    let systemImage: String
    let tint: Color
    var disabled = false
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                Image(systemName: systemImage)
                    .font(.system(size: 13, weight: .semibold))
                Text(title)
                    .font(.system(size: 13, weight: .semibold))
            }
            .foregroundStyle(disabled ? ZhihuijiTheme.ColorToken.textTertiary : tint)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 11)
            .background((disabled ? Color.white.opacity(0.38) : tint.opacity(0.10)), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .stroke(Color.white.opacity(0.45), lineWidth: 0.5)
            )
        }
        .buttonStyle(.plain)
        .disabled(disabled)
        .opacity(disabled ? 0.7 : 1)
    }
}

@MainActor
final class ReportsViewModel: ObservableObject {
    @Published var range: ReportRange = .today
    @Published var isLoading = false
    @Published var exportStatus: String?
    @Published var salesSummary: SalesSummaryReport?
    @Published var salesTrend: [SalesTrendPoint] = []
    @Published var profitSummary: ProfitSummaryReport?
    @Published var cashflowSummary: CashflowSummaryReport?
    @Published var reconciliation: ReconciliationSummaryReport?
    @Published var topProducts: [TopSellingProductReport] = []
    @Published var productProfits: [ProfitByProductReport] = []
    @Published var customerSales: [CustomerSalesReport] = []
    @Published var receivableCustomers: [CustomerReceivableReport] = []
    @Published var refunds: [RefundRecordReport] = []
    @Published var stockOutRecords: [StockOutRecordReport] = []
    @Published var lowStockProducts: [LowStockProductReport] = []
    @Published var errorMessage: String?

    func load(client: APIClient) async {
        isLoading = true
        exportStatus = nil
        defer { isLoading = false }

        let dateRange = range.dateRange
        let bucket = range.bucket
        async let salesSummaryTask = capture { try await client.fetchSalesSummary(startAt: dateRange.startAt, endAt: dateRange.endAt) }
        async let salesTrendTask = capture { try await client.fetchSalesTrend(startAt: dateRange.startAt, endAt: dateRange.endAt, bucket: bucket) }
        async let profitTask = capture { try await client.fetchProfitSummary(startAt: dateRange.startAt, endAt: dateRange.endAt) }
        async let cashflowTask = capture { try await client.fetchCashflowSummary(startAt: dateRange.startAt, endAt: dateRange.endAt) }
        async let reconciliationTask = capture { try await client.fetchReconciliationSummary(startAt: dateRange.startAt, endAt: dateRange.endAt) }
        async let productsTask = capture { try await client.fetchTopProducts(startAt: dateRange.startAt, endAt: dateRange.endAt, limit: 8) }
        async let productProfitsTask = capture { try await client.fetchProfitByProducts(startAt: dateRange.startAt, endAt: dateRange.endAt, limit: 8) }
        async let customerSalesTask = capture { try await client.fetchCustomerSalesReport(startAt: dateRange.startAt, endAt: dateRange.endAt, limit: 8) }
        async let receivableCustomersTask = capture { try await client.fetchTopReceivableCustomers(limit: 8) }
        async let refundsTask = capture { try await client.fetchRefundRecords(startAt: dateRange.startAt, endAt: dateRange.endAt, limit: 6) }
        async let stockOutTask = capture { try await client.fetchStockOutRecords(startAt: dateRange.startAt, endAt: dateRange.endAt, limit: 6) }
        async let stockTask = capture { try await client.fetchLowStockProducts(limit: 6) }

        let salesSummaryResult = await salesSummaryTask
        let salesTrendResult = await salesTrendTask
        let profitResult = await profitTask
        let cashflowResult = await cashflowTask
        let reconciliationResult = await reconciliationTask
        let productsResult = await productsTask
        let productProfitsResult = await productProfitsTask
        let customerSalesResult = await customerSalesTask
        let receivableCustomersResult = await receivableCustomersTask
        let refundsResult = await refundsTask
        let stockOutResult = await stockOutTask
        let stockResult = await stockTask

        var failedSections: [String] = []

        switch salesSummaryResult {
        case let .success(value): salesSummary = value
        case .failure: failedSections.append("销售汇总")
        }
        switch salesTrendResult {
        case let .success(value): salesTrend = value
        case .failure: failedSections.append("销售趋势")
        }
        switch profitResult {
        case let .success(value): profitSummary = value
        case .failure: failedSections.append("利润汇总")
        }
        switch cashflowResult {
        case let .success(value): cashflowSummary = value
        case .failure: failedSections.append("现金流")
        }
        switch reconciliationResult {
        case let .success(value): reconciliation = value
        case .failure: failedSections.append("对账")
        }
        switch productsResult {
        case let .success(value): topProducts = value
        case .failure: failedSections.append("热销商品")
        }
        switch productProfitsResult {
        case let .success(value): productProfits = value
        case .failure: failedSections.append("商品利润")
        }
        switch customerSalesResult {
        case let .success(value): customerSales = value
        case .failure: failedSections.append("客户销售")
        }
        switch receivableCustomersResult {
        case let .success(value): receivableCustomers = value
        case .failure: failedSections.append("应收排行")
        }
        switch refundsResult {
        case let .success(value): refunds = value
        case .failure: failedSections.append("退款记录")
        }
        switch stockOutResult {
        case let .success(value): stockOutRecords = value
        case .failure: failedSections.append("出库记录")
        }
        switch stockResult {
        case let .success(value): lowStockProducts = value
        case .failure: failedSections.append("低库存")
        }
        errorMessage = failedSections.isEmpty ? nil : "以下分区暂未成功拉取：\(failedSections.joined(separator: "、"))"
    }

    func widthRatio(for point: SalesTrendPoint) -> CGFloat {
        let maxAmount = salesTrend.map(\.totalSalesAmount).max() ?? 0
        guard maxAmount > 0 else { return 0.1 }
        return CGFloat(point.totalSalesAmount / maxAmount)
    }

    private func capture<T>(_ operation: @escaping () async throws -> T) async -> Result<T, Error> {
        do {
            return .success(try await operation())
        } catch {
            return .failure(error)
        }
    }
}

enum ReportRange: String, CaseIterable, Identifiable {
    case today
    case week
    case month

    var id: String { rawValue }

    var title: String {
        switch self {
        case .today: return "今日"
        case .week: return "本周"
        case .month: return "本月"
        }
    }

    var bucket: String {
        self == .today ? "hour6" : "day"
    }

    var dateRange: (startAt: Int64, endAt: Int64) {
        let calendar = Calendar.current
        let now = Date()
        switch self {
        case .today:
            let start = calendar.startOfDay(for: now)
            let end = calendar.date(byAdding: DateComponents(day: 1, second: -1), to: start) ?? now
            return (Int64(start.timeIntervalSince1970 * 1000), Int64(end.timeIntervalSince1970 * 1000))
        case .week:
            let start = calendar.date(from: calendar.dateComponents([.yearForWeekOfYear, .weekOfYear], from: now)) ?? now
            let end = calendar.date(byAdding: DateComponents(day: 7, second: -1), to: start) ?? now
            return (Int64(start.timeIntervalSince1970 * 1000), Int64(end.timeIntervalSince1970 * 1000))
        case .month:
            let components = calendar.dateComponents([.year, .month], from: now)
            let start = calendar.date(from: components) ?? now
            let end = calendar.date(byAdding: DateComponents(month: 1, second: -1), to: start) ?? now
            return (Int64(start.timeIntervalSince1970 * 1000), Int64(end.timeIntervalSince1970 * 1000))
        }
    }
}
