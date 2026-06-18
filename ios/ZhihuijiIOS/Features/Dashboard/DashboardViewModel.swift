import Foundation

@MainActor
final class DashboardViewModel: ObservableObject {
    @Published var isLoading = false
    @Published var kpis: [KPIBlock] = [
        KPIBlock(id: "sales", title: "销售额", value: "¥0.00", subtitle: "今日经营"),
        KPIBlock(id: "receivable", title: "已收款", value: "¥0.00", subtitle: "待同步摘要"),
        KPIBlock(id: "stock", title: "退款", value: "¥0.00", subtitle: "待同步摘要"),
        KPIBlock(id: "cash", title: "订单数", value: "0", subtitle: "待同步摘要"),
    ]
    @Published var lowStockProducts: [LowStockProductReport] = []
    @Published var errorMessage: String?
    @Published var scopeLabel = "今日经营"

    func load(using client: APIClient) async {
        isLoading = true
        defer { isLoading = false }
        let range = DateRange.today
        async let summaryTask = capture {
            try await client.fetchSalesSummary(startAt: range.startAt, endAt: range.endAt)
        }
        async let lowStockTask = capture {
            try await client.fetchLowStockProducts(limit: 6)
        }

        let summaryResult = await summaryTask
        let lowStockResult = await lowStockTask
        var failures: [String] = []

        switch summaryResult {
        case let .success(summary):
            kpis = [
                KPIBlock(id: "sales", title: "销售额", value: summary.totalSalesAmount.currencyText, subtitle: "今日销售"),
                KPIBlock(id: "paid", title: "已收款", value: summary.totalPaidAmount.currencyText, subtitle: "今日到账"),
                KPIBlock(id: "refund", title: "退款", value: summary.totalRefundAmount.currencyText, subtitle: "今日退款"),
                KPIBlock(id: "orders", title: "订单数", value: "\(summary.totalOrderCount)", subtitle: "今日成交"),
            ]
        case .failure:
            failures.append("经营汇总")
        }

        switch lowStockResult {
        case let .success(products):
            lowStockProducts = products
        case .failure:
            failures.append("库存提醒")
        }

        errorMessage = failures.isEmpty ? nil : "以下分区暂未成功拉取：\(failures.joined(separator: "、"))"
    }

    private func capture<T>(_ operation: @escaping () async throws -> T) async -> Result<T, Error> {
        do {
            return .success(try await operation())
        } catch {
            return .failure(error)
        }
    }
}

private struct DateRange {
    let startAt: Int64
    let endAt: Int64

    static let today: DateRange = {
        let calendar = Calendar.current
        let start = calendar.startOfDay(for: Date())
        let end = calendar.date(byAdding: DateComponents(day: 1, second: -1), to: start) ?? Date()
        return DateRange(
            startAt: Int64(start.timeIntervalSince1970 * 1000),
            endAt: Int64(end.timeIntervalSince1970 * 1000)
        )
    }()
}

extension Double {
    var currencyText: String {
        "¥" + String(format: "%.2f", self)
    }
}
