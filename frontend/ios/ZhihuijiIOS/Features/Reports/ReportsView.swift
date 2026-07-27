import SwiftUI

#if canImport(UIKit)
import UIKit
#endif

struct ReportsView: View {
    @Environment(\.appEnvironment) private var env
    @StateObject private var viewModel = ReportsViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("经营报表")
                    .font(ZhihuijiTheme.Typography.pageTitle)
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
                guard let url = viewModel.writeCSVToTemporaryFile() else {
                    viewModel.exportStatus = "导出失败：当前没有可导出的报表内容。"
                    return
                }
#if canImport(UIKit)
                if presentActivitySheet(items: [url]) {
                    viewModel.exportStatus = "CSV 已生成并打开分享面板。"
                } else {
                    viewModel.exportStatus = "CSV 已生成，但分享面板未能唤起。"
                }
#else
                viewModel.exportStatus = "CSV 已生成到临时目录。"
#endif
            }

            SecondaryReportActionButton(
                title: "打印",
                systemImage: "printer.fill",
                tint: ZhihuijiTheme.ColorToken.warning,
                disabled: viewModel.isLoading
            ) {
                let printableHTML = viewModel.makePrintableHTML()
#if canImport(UIKit)
                if presentPrintPanel(html: printableHTML, jobName: "经营报表-\(viewModel.range.title)") {
                    viewModel.exportStatus = "打印面板已打开。"
                } else {
                    viewModel.exportStatus = "打印内容已准备，但系统打印面板未能唤起。"
                }
#else
                viewModel.exportStatus = "打印内容已准备。"
#endif
            }
        }
    }

    private var summaryStrip: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("报表状态")
                    .font(ZhihuijiTheme.Typography.sectionTitle)
                Spacer()
                StatusChip(title: viewModel.range.title, tint: ZhihuijiTheme.ColorToken.primary)
            }

            if let exportStatus = viewModel.exportStatus {
                Text(exportStatus)
                    .font(ZhihuijiTheme.Typography.caption)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(12)
                    .background(
                        Color.white.opacity(0.42),
                        in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.cardSmall, style: .continuous)
                    )
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
                .font(ZhihuijiTheme.Typography.sectionTitle)
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
                    .font(ZhihuijiTheme.Typography.sectionTitle)
            if viewModel.salesTrend.isEmpty {
                EmptyStateView(title: "暂无趋势数据", message: "当前时间范围没有可用的趋势点。")
            } else {
                VStack(spacing: 10) {
                    ForEach(viewModel.salesTrend) { point in
                        VStack(alignment: .leading, spacing: 6) {
                            HStack {
                                Text(point.startAt.dateText)
                                    .font(ZhihuijiTheme.Typography.caption)
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                                Spacer()
                                Text(point.totalSalesAmount.currencyText)
                                    .font(ZhihuijiTheme.Typography.captionSemibold)
                            }
                            GeometryReader { proxy in
                                let width = max(proxy.size.width * viewModel.widthRatio(for: point), 6)
                                ZStack(alignment: .leading) {
                                    RoundedRectangle(cornerRadius: 999, style: .continuous).fill(Color.white.opacity(0.35))
                                    RoundedRectangle(cornerRadius: 999, style: .continuous).fill(ZhihuijiTheme.ColorToken.primary)
                                        .frame(width: width)
                                }
                            }
                            .frame(height: 8)
                            Text("订单 \(point.totalOrderCount)")
                                .font(ZhihuijiTheme.Typography.caption)
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
                .font(ZhihuijiTheme.Typography.sectionTitle)
            if viewModel.topProducts.isEmpty {
                EmptyStateView(title: "暂无商品分析", message: "当前时间范围没有热销商品数据。")
            } else {
                VStack(spacing: 10) {
                    ForEach(viewModel.topProducts.prefix(5)) { product in
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(product.productName)
                                    .font(ZhihuijiTheme.Typography.cardTitle)
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                                Text(product.productCode)
                                    .font(ZhihuijiTheme.Typography.caption)
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                            }
                            Spacer()
                            VStack(alignment: .trailing, spacing: 4) {
                                Text(product.totalAmount.currencyText)
                                    .font(ZhihuijiTheme.Typography.bodyMedium)
                                Text("销量 \(String(format: "%.0f", product.totalQuantity))")
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
    }

    private var productProfitSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("商品利润")
                .font(ZhihuijiTheme.Typography.sectionTitle)
            if viewModel.productProfits.isEmpty {
                EmptyStateView(title: "暂无商品利润数据", message: "当前时间范围没有可分析的商品利润记录。")
            } else {
                VStack(spacing: 10) {
                    ForEach(viewModel.productProfits.prefix(5)) { item in
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(item.productName)
                                    .font(ZhihuijiTheme.Typography.cardTitle)
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                                Text(item.productCode)
                                    .font(ZhihuijiTheme.Typography.caption)
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                            }
                            Spacer()
                            VStack(alignment: .trailing, spacing: 4) {
                                Text(item.totalProfitAmount.currencyText)
                                    .font(ZhihuijiTheme.Typography.bodyMedium)
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.success)
                                Text("利润率 \(String(format: "%.1f%%", item.profitRate * 100))")
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
    }

    private var customerSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("客户分析")
                .font(ZhihuijiTheme.Typography.sectionTitle)

            if viewModel.customerSales.isEmpty, viewModel.receivableCustomers.isEmpty {
                EmptyStateView(title: "暂无客户分析", message: "当前时间范围没有可展示的客户经营数据。")
            } else {
                if !viewModel.customerSales.isEmpty {
                    VStack(alignment: .leading, spacing: 10) {
                        Text("客户销售")
                            .font(ZhihuijiTheme.Typography.bodyMedium)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                        ForEach(viewModel.customerSales.prefix(4)) { item in
                            HStack {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(item.customerName)
                                        .font(ZhihuijiTheme.Typography.cardTitle)
                                    Text("订单 \(item.totalOrders)")
                                        .font(ZhihuijiTheme.Typography.caption)
                                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                                }
                                Spacer()
                                Text(item.totalAmount.currencyText)
                                    .font(ZhihuijiTheme.Typography.bodyMedium)
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                            }
                            .padding(14)
                            .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
                        }
                    }
                }

                if !viewModel.receivableCustomers.isEmpty {
                    VStack(alignment: .leading, spacing: 10) {
                        Text("应收排行")
                            .font(ZhihuijiTheme.Typography.bodyMedium)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                        ForEach(viewModel.receivableCustomers.prefix(4)) { item in
                            HStack {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(item.customerName)
                                        .font(ZhihuijiTheme.Typography.cardTitle)
                                    Text(item.phone?.nilIfBlank ?? "无联系电话")
                                        .font(ZhihuijiTheme.Typography.caption)
                                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                                }
                                Spacer()
                                Text(item.balance.currencyText)
                                    .font(ZhihuijiTheme.Typography.bodyMedium)
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.warning)
                            }
                            .padding(14)
                            .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
                        }
                    }
                }
            }
        }
    }

    private var riskSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("风险提醒")
                .font(ZhihuijiTheme.Typography.sectionTitle)

            if viewModel.refunds.isEmpty, viewModel.stockOutRecords.isEmpty, viewModel.lowStockProducts.isEmpty {
                EmptyStateView(title: "暂无风险项", message: "退款和低库存指标当前都比较平稳。")
            } else {
                VStack(spacing: 10) {
                    ForEach(viewModel.refunds.prefix(3)) { refund in
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(refund.orderNo)
                                    .font(ZhihuijiTheme.Typography.cardTitle)
                                Text(refund.customerName ?? "散客")
                                    .font(ZhihuijiTheme.Typography.caption)
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                            }
                            Spacer()
                            VStack(alignment: .trailing, spacing: 4) {
                                Text(refund.refundAmount.currencyText)
                                    .font(ZhihuijiTheme.Typography.bodyMedium)
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.danger)
                                Text(refund.createdAt.dateText)
                                    .font(ZhihuijiTheme.Typography.caption)
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                            }
                        }
                        .padding(14)
                        .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
                    }

                    ForEach(viewModel.stockOutRecords.prefix(3)) { item in
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(item.productName)
                                    .font(ZhihuijiTheme.Typography.cardTitle)
                                Text(item.customerName?.nilIfBlank ?? item.orderNo)
                                    .font(ZhihuijiTheme.Typography.caption)
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                            }
                            Spacer()
                            VStack(alignment: .trailing, spacing: 4) {
                                Text("-" + String(format: "%.2f", item.quantity))
                                    .font(ZhihuijiTheme.Typography.bodyMedium)
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.warning)
                                Text(item.amount.currencyText)
                                    .font(ZhihuijiTheme.Typography.caption)
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                            }
                        }
                        .padding(14)
                        .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
                    }

                    ForEach(viewModel.lowStockProducts.prefix(3)) { product in
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(product.productName)
                                    .font(ZhihuijiTheme.Typography.cardTitle)
                                Text(product.productCode)
                                    .font(ZhihuijiTheme.Typography.caption)
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                            }
                            Spacer()
                            VStack(alignment: .trailing, spacing: 4) {
                                Text("库存 \(String(format: "%.2f", product.stock))")
                                    .font(ZhihuijiTheme.Typography.bodyMedium)
                                Text("安全库存 \(String(format: "%.2f", product.safeStock))")
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
            .font(ZhihuijiTheme.Typography.captionSemibold)
            .foregroundStyle(tint)
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(
                tint.opacity(0.12),
                in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.pill, style: .continuous)
            )
            .overlay(
                RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.pill, style: .continuous)
                    .stroke(Color.white.opacity(0.45), lineWidth: ZhihuijiTheme.Stroke.hairline)
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
                    .font(ZhihuijiTheme.Typography.captionSemibold)
                Text(title)
                    .font(ZhihuijiTheme.Typography.captionSemibold)
            }
            .foregroundStyle(disabled ? ZhihuijiTheme.ColorToken.textTertiary : tint)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 11)
            .background(
                (disabled ? Color.white.opacity(0.38) : tint.opacity(0.10)),
                in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.cardSmall, style: .continuous)
            )
            .overlay(
                RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.cardSmall, style: .continuous)
                    .stroke(Color.white.opacity(0.45), lineWidth: ZhihuijiTheme.Stroke.hairline)
            )
        }
        .buttonStyle(.plain)
        .disabled(disabled)
        .opacity(disabled ? 0.7 : 1)
    }
}

#if canImport(UIKit)
private func presentActivitySheet(items: [Any]) -> Bool {
    guard let presenter = topViewController() else { return false }
    let controller = UIActivityViewController(activityItems: items, applicationActivities: nil)
    if let popover = controller.popoverPresentationController {
        popover.sourceView = presenter.view
        popover.sourceRect = CGRect(x: presenter.view.bounds.midX, y: presenter.view.bounds.midY, width: 1, height: 1)
        popover.permittedArrowDirections = []
    }
    presenter.present(controller, animated: true)
    return true
}

private func presentPrintPanel(html: String, jobName: String) -> Bool {
    guard topViewController() != nil else { return false }
    let controller = UIPrintInteractionController.shared
    let printInfo = UIPrintInfo.printInfo()
    printInfo.jobName = jobName
    printInfo.outputType = .general
    controller.printInfo = printInfo
    controller.printFormatter = UIMarkupTextPrintFormatter(markupText: html)
    return controller.present(animated: true, completionHandler: { _, _, _ in })
}

private func topViewController(base: UIViewController? = nil) -> UIViewController? {
    let root: UIViewController? = base ?? {
        let activeScene = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first { $0.activationState == .foregroundActive }
        return activeScene?.windows.first(where: { $0.isKeyWindow })?.rootViewController
    }()

    if let nav = root as? UINavigationController {
        return topViewController(base: nav.visibleViewController)
    }
    if let tab = root as? UITabBarController {
        return topViewController(base: tab.selectedViewController)
    }
    if let presented = root?.presentedViewController {
        return topViewController(base: presented)
    }
    return root
}
#endif

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
        case .failure:
            salesSummary = nil
            failedSections.append("销售汇总")
        }
        switch salesTrendResult {
        case let .success(value): salesTrend = value
        case .failure:
            salesTrend = []
            failedSections.append("销售趋势")
        }
        switch profitResult {
        case let .success(value): profitSummary = value
        case .failure:
            profitSummary = nil
            failedSections.append("利润汇总")
        }
        switch cashflowResult {
        case let .success(value): cashflowSummary = value
        case .failure:
            cashflowSummary = nil
            failedSections.append("现金流")
        }
        switch reconciliationResult {
        case let .success(value): reconciliation = value
        case .failure:
            reconciliation = nil
            failedSections.append("对账")
        }
        switch productsResult {
        case let .success(value): topProducts = value
        case .failure:
            topProducts = []
            failedSections.append("热销商品")
        }
        switch productProfitsResult {
        case let .success(value): productProfits = value
        case .failure:
            productProfits = []
            failedSections.append("商品利润")
        }
        switch customerSalesResult {
        case let .success(value): customerSales = value
        case .failure:
            customerSales = []
            failedSections.append("客户销售")
        }
        switch receivableCustomersResult {
        case let .success(value): receivableCustomers = value
        case .failure:
            receivableCustomers = []
            failedSections.append("应收排行")
        }
        switch refundsResult {
        case let .success(value): refunds = value
        case .failure:
            refunds = []
            failedSections.append("退款记录")
        }
        switch stockOutResult {
        case let .success(value): stockOutRecords = value
        case .failure:
            stockOutRecords = []
            failedSections.append("出库记录")
        }
        switch stockResult {
        case let .success(value): lowStockProducts = value
        case .failure:
            lowStockProducts = []
            failedSections.append("低库存")
        }
        errorMessage = failedSections.isEmpty ? nil : "以下分区暂未成功拉取：\(failedSections.joined(separator: "、"))"
    }

    func widthRatio(for point: SalesTrendPoint) -> CGFloat {
        let maxAmount = salesTrend.map(\.totalSalesAmount).max() ?? 0
        guard maxAmount > 0 else { return 0.1 }
        return CGFloat(point.totalSalesAmount / maxAmount)
    }

    func makeCSV() -> String {
        var lines: [String] = []
        lines.append("section,label,value,meta")
        lines.append(csvRow(["overview", "销售额", salesSummary?.totalSalesAmount.currencyText ?? "--", range.title]))
        lines.append(csvRow(["overview", "毛利", profitSummary?.estimatedProfitAmount.currencyText ?? "--", "估算利润"]))
        lines.append(csvRow(["overview", "净现金", cashflowSummary?.netCashFlow.currencyText ?? "--", "现金流"]))
        lines.append(csvRow(["overview", "待收款", reconciliation?.totalReceivableAmount.currencyText ?? "--", "客户应收"]))

        if !salesTrend.isEmpty {
            for point in salesTrend {
                lines.append(csvRow(["trend", point.startAt.dateText, point.totalSalesAmount.currencyText, "订单 \(point.totalOrderCount)"]))
            }
        }

        if !topProducts.isEmpty {
            for product in topProducts {
                lines.append(csvRow(["top_product", product.productName, product.totalAmount.currencyText, "销量 \(String(format: "%.0f", product.totalQuantity))"]))
            }
        }

        if !productProfits.isEmpty {
            for item in productProfits {
                lines.append(csvRow(["product_profit", item.productName, item.totalProfitAmount.currencyText, String(format: "利润率 %.1f%%", item.profitRate * 100)]))
            }
        }

        if !customerSales.isEmpty {
            for item in customerSales {
                lines.append(csvRow(["customer_sales", item.customerName, item.totalAmount.currencyText, "订单 \(item.totalOrders)"]))
            }
        }

        if !receivableCustomers.isEmpty {
            for item in receivableCustomers {
                lines.append(csvRow(["receivable", item.customerName, item.balance.currencyText, item.phone?.nilIfBlank ?? "无联系电话"]))
            }
        }

        if !refunds.isEmpty {
            for refund in refunds {
                lines.append(csvRow(["refund", refund.orderNo, refund.refundAmount.currencyText, refund.customerName ?? "散客"]))
            }
        }

        if !stockOutRecords.isEmpty {
            for item in stockOutRecords {
                lines.append(csvRow(["stock_out", item.productName, String(format: "%.2f", item.quantity), item.amount.currencyText]))
            }
        }

        if !lowStockProducts.isEmpty {
            for product in lowStockProducts {
                lines.append(csvRow(["low_stock", product.productName, String(format: "%.2f", product.stock), String(format: "安全库存 %.2f", product.safeStock)]))
            }
        }

        return lines.joined(separator: "\n")
    }

    func writeCSVToTemporaryFile() -> URL? {
        let csv = makeCSV()
        guard !csv.isEmpty else { return nil }
        let fileURL = FileManager.default.temporaryDirectory
            .appendingPathComponent("zhihuiji-report-\(range.rawValue)-\(Int(Date().timeIntervalSince1970)).csv")
        do {
            try csv.write(to: fileURL, atomically: true, encoding: .utf8)
            return fileURL
        } catch {
            exportStatus = "CSV 写入失败：\(error.localizedDescription)"
            return nil
        }
    }

    func makePrintableHTML() -> String {
        let rows = [
            ("销售额", salesSummary?.totalSalesAmount.currencyText ?? "--"),
            ("毛利", profitSummary?.estimatedProfitAmount.currencyText ?? "--"),
            ("净现金", cashflowSummary?.netCashFlow.currencyText ?? "--"),
            ("待收款", reconciliation?.totalReceivableAmount.currencyText ?? "--")
        ]

        let rowHTML = rows.map { title, value in
            "<tr><td>\(htmlEscaped(title))</td><td>\(htmlEscaped(value))</td></tr>"
        }.joined(separator: "")

        let topProductsHTML = topProducts.prefix(5).map { product in
            "<li>\(htmlEscaped(product.productName)) - \(htmlEscaped(product.totalAmount.currencyText))</li>"
        }.joined(separator: "")

        return """
        <html>
        <head>
        <meta charset="utf-8">
        <style>
            body { font-family: -apple-system, BlinkMacSystemFont, sans-serif; color: #102033; padding: 24px; }
            h1 { font-size: 24px; margin-bottom: 8px; }
            .sub { color: #6b7785; margin-bottom: 18px; }
            table { width: 100%; border-collapse: collapse; margin-bottom: 18px; }
            td { border-bottom: 1px solid #d8e0ea; padding: 10px 8px; }
            td:first-child { width: 40%; font-weight: 600; }
            ul { padding-left: 22px; }
        </style>
        </head>
        <body>
            <h1>经营报表</h1>
            <div class="sub">时间范围：\(htmlEscaped(range.title))</div>
            <table>\(rowHTML)</table>
            <h2>热销商品</h2>
            <ul>\(topProductsHTML)</ul>
        </body>
        </html>
        """
    }

    private func csvRow(_ columns: [String]) -> String {
        columns.map(csvEscaped).joined(separator: ",")
    }

    private func csvEscaped(_ value: String) -> String {
        "\"\(value.replacingOccurrences(of: "\"", with: "\"\""))\""
    }

    private func htmlEscaped(_ value: String) -> String {
        value
            .replacingOccurrences(of: "&", with: "&amp;")
            .replacingOccurrences(of: "<", with: "&lt;")
            .replacingOccurrences(of: ">", with: "&gt;")
            .replacingOccurrences(of: "\"", with: "&quot;")
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
