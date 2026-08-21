import SwiftUI

struct SupplierStatementView: View {
    @Environment(\.appEnvironment) private var env
    let supplierId: EntityID
    @StateObject private var viewModel = SupplierStatementViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                if let errorMessage = viewModel.errorMessage {
                    EmptyStateView(title: "对账单读取失败", message: errorMessage)
                } else if viewModel.supplier == nil, viewModel.isLoading {
                    LoadingStateView(message: "正在加载对账单...")
                } else {
                    if let supplier = viewModel.supplier {
                        supplierCard(supplier)
                    }

                    rangePicker

                    boundaryNotice

                    summarySection

                    transactionSection
                }
            }
            .padding(20)
        }
        .navigationTitle("供应商对账单")
        .task {
            await viewModel.load(supplierId: supplierId, client: env.apiClient)
        }
        .onChange(of: viewModel.range) { _, _ in
            Task { await viewModel.reload(client: env.apiClient) }
        }
    }

    private func supplierCard(_ supplier: SupplierRecord) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 6) {
                    Text(supplier.name)
                        .font(ZhihuijiTheme.Typography.pageTitle)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                    Text(supplier.phone)
                        .font(ZhihuijiTheme.Typography.body)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                }
                Spacer()
                StatusChip(
                    title: supplier.status == 1 ? "启用" : "停用",
                    tint: supplier.status == 1 ? ZhihuijiTheme.ColorToken.success : ZhihuijiTheme.ColorToken.warning
                )
            }

            if let groupName = supplier.groupName?.nilIfBlank {
                Text("分组 \(groupName)")
                    .font(ZhihuijiTheme.Typography.captionSemibold)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.warning)
            }
        }
        .padding(18)
        .glassCard()
    }

    private var rangePicker: some View {
        Picker("时间范围", selection: $viewModel.range) {
            ForEach(SupplierStatementRange.allCases) { range in
                Text(range.title).tag(range)
            }
        }
        .pickerStyle(.segmented)
    }

    private var boundaryNotice: some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: "info.circle.fill")
                .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                .padding(.top, 1)
            Text(SupplierStatementViewModel.boundaryNotice)
                .font(ZhihuijiTheme.Typography.caption)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(14)
        .background(Color.white.opacity(0.52), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.cardSmall, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.cardSmall, style: .continuous)
                .stroke(ZhihuijiTheme.ColorToken.primary.opacity(0.18), lineWidth: ZhihuijiTheme.Stroke.hairline)
        )
    }

    private var summarySection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("对账汇总")
                .font(ZhihuijiTheme.Typography.sectionTitle)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)

            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                MetricCard(
                    title: "应付总额",
                    value: viewModel.totalPayable.currencyText,
                    subtitle: "当前范围内支出",
                    tint: ZhihuijiTheme.ColorToken.warning
                )
                MetricCard(
                    title: "已付金额",
                    value: viewModel.totalPaid.currencyText,
                    subtitle: "当前范围内支出小计",
                    tint: ZhihuijiTheme.ColorToken.success
                )
                MetricCard(
                    title: "待付余额",
                    value: viewModel.pendingAmount.currencyText,
                    subtitle: "供应商档案余额",
                    tint: ZhihuijiTheme.ColorToken.danger
                )
                MetricCard(
                    title: "交易笔数",
                    value: "\(viewModel.transactions.count)",
                    subtitle: "当前范围内流水",
                    tint: ZhihuijiTheme.ColorToken.primaryBright
                )
            }

            if let reconciliation = viewModel.reconciliation {
                VStack(alignment: .leading, spacing: 8) {
                    Text("全局对账参考")
                        .font(ZhihuijiTheme.Typography.captionSemibold)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                    HStack {
                        Text("全局应付 \(reconciliation.totalPayableAmount.currencyText)")
                        Spacer()
                        Text("全局已付 \(reconciliation.totalPaidAmount.currencyText)")
                    }
                    .font(ZhihuijiTheme.Typography.caption)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                }
                .padding(12)
                .background(Color.white.opacity(0.42), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.cardSmall, style: .continuous))
            }
        }
    }

    private var transactionSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("交易明细")
                    .font(ZhihuijiTheme.Typography.sectionTitle)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                Spacer()
                StatusChip(
                    title: "\(viewModel.transactions.count) 笔",
                    tint: ZhihuijiTheme.ColorToken.primary
                )
            }

            if viewModel.transactions.isEmpty {
                EmptyStateView(title: "暂无交易明细", message: "当前时间范围内没有该供应商的流水记录。")
            } else {
                VStack(spacing: 10) {
                    ForEach(viewModel.transactions) { record in
                        SupplierStatementTransactionRow(record: record)
                    }
                }
            }
        }
    }
}

private struct SupplierStatementTransactionRow: View {
    let record: FinanceRecord

    var body: some View {
        HStack(spacing: 12) {
            Circle()
                .fill(record.typeTint.opacity(0.16))
                .frame(width: 38, height: 38)
                .overlay(
                    Image(systemName: record.type == FinanceRecordType.income.rawValue ? "arrow.down.circle.fill" : "arrow.up.circle.fill")
                        .foregroundStyle(record.typeTint)
                )

            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text(record.recordNo)
                        .font(ZhihuijiTheme.Typography.cardTitle)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                    Spacer()
                    StatusChip(title: record.typeLabel, tint: record.typeTint)
                }
                Text(record.category)
                    .font(ZhihuijiTheme.Typography.caption)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                HStack {
                    Text(record.methodLabel)
                    Spacer()
                    Text(record.createdAt.dateTimeText)
                }
                .font(ZhihuijiTheme.Typography.caption)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                HStack {
                    Spacer()
                    Text(record.amount.currencyText)
                        .font(ZhihuijiTheme.Typography.bodyMedium)
                        .foregroundStyle(record.typeTint)
                }
            }
        }
        .padding(14)
        .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
    }
}

@MainActor
final class SupplierStatementViewModel: ObservableObject {
    nonisolated static let boundaryNotice = "当前对账单为前端聚合视图：后端暂无按供应商维度的对账单 API，这里复用 /v2/finance-records 按 partnerName 筛选并汇总，全局对账参考来自 /v2/reports/reconciliation-summary。"

    @Published var supplier: SupplierRecord?
    @Published var transactions: [FinanceRecord] = []
    @Published var reconciliation: ReconciliationSummaryReport?
    @Published var range: SupplierStatementRange = .month
    @Published var isLoading = false
    @Published var errorMessage: String?

    var totalPayable: Double {
        transactions.filter { $0.type == FinanceRecordType.expense.rawValue }.reduce(0) { $0 + $1.amount }
    }

    var totalPaid: Double {
        transactions.filter { $0.type == FinanceRecordType.expense.rawValue }.reduce(0) { $0 + $1.amount }
    }

    var pendingAmount: Double {
        supplier?.balance ?? 0
    }

    func load(supplierId: EntityID, client: APIClient) async {
        isLoading = true
        defer { isLoading = false }
        do {
            let record = try await client.fetchSupplier(id: supplierId)
            supplier = record
            errorMessage = nil
            await loadAggregates(for: record, client: client)
        } catch {
            supplier = nil
            transactions = []
            reconciliation = nil
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    func reload(client: APIClient) async {
        guard let supplier = supplier else { return }
        isLoading = true
        defer { isLoading = false }
        await loadAggregates(for: supplier, client: client)
    }

    private func loadAggregates(for supplier: SupplierRecord, client: APIClient) async {
        let dateRange = range.dateRange
        async let recordsTask = capture {
            try await client.fetchFinanceRecords(
                keyword: supplier.name,
                type: nil,
                page: 1,
                size: 50
            )
        }
        async let reconciliationTask = capture {
            try await client.fetchReconciliationSummary(startAt: dateRange.startAt, endAt: dateRange.endAt)
        }

        let recordsResult = await recordsTask
        let reconciliationResult = await reconciliationTask

        switch recordsResult {
        case let .success(value):
            transactions = filterByDateAndPartner(value, supplierName: supplier.name, startAt: dateRange.startAt, endAt: dateRange.endAt)
        case .failure:
            transactions = []
        }

        switch reconciliationResult {
        case let .success(value):
            reconciliation = value
        case .failure:
            reconciliation = nil
        }
    }

    private func filterByDateAndPartner(_ records: [FinanceRecord], supplierName: String, startAt: Int64, endAt: Int64) -> [FinanceRecord] {
        records.filter { record in
            guard record.partnerName?.nilIfBlank == supplierName else { return false }
            return record.createdAt >= startAt && record.createdAt <= endAt
        }
    }

    private func capture<T>(_ operation: @escaping () async throws -> T) async -> Result<T, Error> {
        do {
            return .success(try await operation())
        } catch {
            return .failure(error)
        }
    }
}

enum SupplierStatementRange: String, CaseIterable, Identifiable {
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
