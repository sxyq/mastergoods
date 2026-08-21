import SwiftUI

struct DailyExpenseView: View {
    @Environment(\.appEnvironment) private var env
    @StateObject private var viewModel = DailyExpenseViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("每日支出")
                    .font(ZhihuijiTheme.Typography.pageTitle)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)

                summarySection

                boundaryNotice

                PrimaryGlassButton(
                    title: viewModel.isLoading ? "刷新中..." : "刷新支出",
                    systemImage: "arrow.clockwise",
                    disabled: viewModel.isLoading
                ) {
                    Task { await viewModel.load(using: env.apiClient) }
                }

                if let errorMessage = viewModel.errorMessage {
                    EmptyStateView(title: "支出数据读取失败", message: errorMessage)
                } else if viewModel.groups.isEmpty, !viewModel.isLoading {
                    EmptyStateView(title: "暂无支出记录", message: "当前没有可按日汇总的支出流水。")
                } else {
                    LazyVStack(spacing: 16) {
                        ForEach(viewModel.groups) { group in
                            DailyExpenseGroupCard(group: group)
                        }
                    }
                }
            }
            .padding(20)
        }
        .navigationTitle("每日支出")
        .task {
            await viewModel.load(using: env.apiClient)
        }
    }

    private var summarySection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("支出汇总")
                .font(ZhihuijiTheme.Typography.sectionTitle)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)

            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                MetricCard(
                    title: "本周支出",
                    value: viewModel.weekTotal.currencyText,
                    subtitle: "本周支出小计",
                    tint: ZhihuijiTheme.ColorToken.warning
                )
                MetricCard(
                    title: "本月支出",
                    value: viewModel.monthTotal.currencyText,
                    subtitle: "本月支出小计",
                    tint: ZhihuijiTheme.ColorToken.danger
                )
                MetricCard(
                    title: "日均支出",
                    value: viewModel.dailyAverage.currencyText,
                    subtitle: "按已加载天数平均",
                    tint: ZhihuijiTheme.ColorToken.primary
                )
                MetricCard(
                    title: "支出笔数",
                    value: "\(viewModel.totalCount)",
                    subtitle: "已加载流水",
                    tint: ZhihuijiTheme.ColorToken.primaryBright
                )
            }
        }
    }

    private var boundaryNotice: some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: "info.circle.fill")
                .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                .padding(.top, 1)
            Text(DailyExpenseViewModel.boundaryNotice)
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
}

private struct DailyExpenseGroupCard: View {
    let group: DailyExpenseGroup

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(group.dateText)
                        .font(ZhihuijiTheme.Typography.bodyMedium)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                    Text("\(group.records.count) 笔支出")
                        .font(ZhihuijiTheme.Typography.caption)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                }
                Spacer()
                Text(group.totalAmount.currencyText)
                    .font(ZhihuijiTheme.Typography.bodyMedium)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.warning)
            }

            VStack(spacing: 10) {
                ForEach(group.records) { record in
                    DailyExpenseRecordRow(record: record)
                }
            }
        }
        .padding(16)
        .glassCard()
    }
}

private struct DailyExpenseRecordRow: View {
    let record: FinanceRecord

    var body: some View {
        HStack(spacing: 12) {
            Circle()
                .fill(ZhihuijiTheme.ColorToken.warning.opacity(0.16))
                .frame(width: 36, height: 36)
                .overlay(
                    Image(systemName: "arrow.up.circle.fill")
                        .foregroundStyle(ZhihuijiTheme.ColorToken.warning)
                )

            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text(record.recordNo)
                        .font(ZhihuijiTheme.Typography.cardTitle)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                    Spacer()
                    Text(record.amount.currencyText)
                        .font(ZhihuijiTheme.Typography.bodyMedium)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.warning)
                }
                Text(record.category)
                    .font(ZhihuijiTheme.Typography.caption)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                HStack {
                    Text(record.partnerName?.nilIfBlank ?? "无往来方")
                    Spacer()
                    Text(record.createdAt.dateTimeText)
                }
                .font(ZhihuijiTheme.Typography.caption)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                if let notes = record.notes?.nilIfBlank {
                    Text(notes)
                        .font(ZhihuijiTheme.Typography.caption)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                        .lineLimit(2)
                }
            }
        }
        .padding(12)
        .background(Color.white.opacity(0.42), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.cardSmall, style: .continuous))
    }
}

@MainActor
final class DailyExpenseViewModel: ObservableObject {
    nonisolated static let boundaryNotice = "每日支出按日期分组聚合支出流水（type=2）。当前 /v2/finance-records 暂未提供 createdAfter/createdBefore 服务端筛选，这里拉取较大范围后在前端按 createdAt 分组与汇总。"

    @Published var groups: [DailyExpenseGroup] = []
    @Published var isLoading = false
    @Published var errorMessage: String?

    var weekTotal: Double {
        let calendar = Calendar.current
        let now = Date()
        let weekStart = calendar.date(from: calendar.dateComponents([.yearForWeekOfYear, .weekOfYear], from: now)) ?? now
        let weekStartMs = Int64(weekStart.timeIntervalSince1970 * 1000)
        return groups.flatMap(\.records)
            .filter { $0.createdAt >= weekStartMs }
            .reduce(0) { $0 + $1.amount }
    }

    var monthTotal: Double {
        let calendar = Calendar.current
        let now = Date()
        let components = calendar.dateComponents([.year, .month], from: now)
        let monthStart = calendar.date(from: components) ?? now
        let monthStartMs = Int64(monthStart.timeIntervalSince1970 * 1000)
        return groups.flatMap(\.records)
            .filter { $0.createdAt >= monthStartMs }
            .reduce(0) { $0 + $1.amount }
    }

    var dailyAverage: Double {
        guard !groups.isEmpty else { return 0 }
        return groups.map(\.totalAmount).reduce(0, +) / Double(groups.count)
    }

    var totalCount: Int {
        groups.reduce(0) { $0 + $1.records.count }
    }

    func load(using client: APIClient) async {
        isLoading = true
        defer { isLoading = false }
        do {
            let records = try await client.fetchFinanceRecords(
                keyword: nil,
                type: FinanceRecordType.expense.rawValue,
                page: 1,
                size: 100
            )
            groups = Self.groupByDate(records)
            errorMessage = nil
        } catch {
            groups = []
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    private static func groupByDate(_ records: [FinanceRecord]) -> [DailyExpenseGroup] {
        let grouped = Dictionary(grouping: records) { record -> String in
            let date = Date(timeIntervalSince1970: TimeInterval(record.createdAt) / 1000)
            return Self.dayFormatter.string(from: date)
        }

        return grouped.map { (dateText, records) in
            DailyExpenseGroup(
                dateText: dateText,
                records: records.sorted { $0.createdAt > $1.createdAt },
                totalAmount: records.reduce(0) { $0 + $1.amount }
            )
        }
        .sorted { $0.dateText > $1.dateText }
    }

    private static let dayFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "zh_CN")
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter
    }()
}

struct DailyExpenseGroup: Identifiable, Equatable {
    let id = UUID()
    let dateText: String
    let records: [FinanceRecord]
    let totalAmount: Double
}
