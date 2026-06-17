import SwiftUI

struct InventorySnapshotView: View {
    @Environment(\.appEnvironment) private var env
    @StateObject private var viewModel = InventorySnapshotViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("库存盘点")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)

                Text("按安卓移动端习惯先看风险，再看最近盘点和月度变化。")
                    .font(.system(size: 14))
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)

                PrimaryGlassButton(
                    title: viewModel.isLoading ? "刷新中..." : "刷新库存概览",
                    systemImage: "arrow.clockwise",
                    disabled: viewModel.isLoading
                ) {
                    Task { await viewModel.load(client: env.apiClient) }
                }

                NavigationLink {
                    InventoryAdjustView()
                } label: {
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("库存调整与快照")
                                .font(.system(size: 16, weight: .semibold))
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                            Text("做真实库存流水调整，并为选中商品生成盘点快照。")
                                .font(.system(size: 13))
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

                if let errorMessage = viewModel.errorMessage {
                    EmptyStateView(title: "库存数据加载失败", message: errorMessage)
                }

                lowStockSection
                snapshotsSection
                monthlyStatsSection
            }
            .padding(20)
        }
        .navigationTitle("库存")
        .task {
            await viewModel.load(client: env.apiClient)
        }
    }

    private var lowStockSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("低库存提醒")
                .font(.system(size: 18, weight: .semibold))
            if viewModel.lowStockProducts.isEmpty {
                EmptyStateView(title: "暂无低库存商品", message: "当前安全库存指标看起来比较平稳。")
            } else {
                ForEach(viewModel.lowStockProducts.prefix(6)) { product in
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
                                .foregroundStyle(ZhihuijiTheme.ColorToken.danger)
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

    private var snapshotsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("最近盘点快照")
                .font(.system(size: 18, weight: .semibold))
            if viewModel.snapshots.isEmpty {
                EmptyStateView(title: "暂无盘点快照", message: "当前没有可用的库存快照记录。")
            } else {
                ForEach(viewModel.snapshots.prefix(6)) { snapshot in
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(snapshot.productName)
                                .font(.system(size: 15, weight: .semibold))
                            Text(snapshot.productCode)
                                .font(.system(size: 12))
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                            Text(snapshot.snapshotDate.dateText)
                                .font(.system(size: 11))
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                        }
                        Spacer()
                        VStack(alignment: .trailing, spacing: 4) {
                            Text("数量 \(String(format: "%.2f", snapshot.quantity))")
                                .font(.system(size: 14, weight: .semibold))
                            if let totalValue = snapshot.totalValue {
                                Text(totalValue.currencyText)
                                    .font(.system(size: 12))
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                            }
                        }
                    }
                    .padding(14)
                    .glassCard(cornerRadius: 12)
                }
            }
        }
    }

    private var monthlyStatsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("本月进出概览")
                .font(.system(size: 18, weight: .semibold))
            if viewModel.monthlyStats.isEmpty {
                EmptyStateView(title: "暂无月度统计", message: "当前月份还没有月度库存统计数据。")
            } else {
                ForEach(viewModel.monthlyStats.prefix(6)) { item in
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(item.productName)
                                .font(.system(size: 15, weight: .semibold))
                            Text(item.productCode ?? "")
                                .font(.system(size: 12))
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                        }
                        Spacer()
                        VStack(alignment: .trailing, spacing: 4) {
                            Text("入 \(String(format: "%.2f", item.quantityIn ?? 0)) / 出 \(String(format: "%.2f", item.quantityOut ?? 0))")
                                .font(.system(size: 12, weight: .medium))
                            Text("结余 \(String(format: "%.2f", item.quantityEnd ?? 0))")
                                .font(.system(size: 14, weight: .semibold))
                                .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                        }
                    }
                    .padding(14)
                    .glassCard(cornerRadius: 12)
                }
            }
        }
    }
}

@MainActor
final class InventorySnapshotViewModel: ObservableObject {
    @Published var isLoading = false
    @Published var lowStockProducts: [LowStockProductReport] = []
    @Published var snapshots: [InventorySnapshotSummary] = []
    @Published var monthlyStats: [InventoryMonthlyStats] = []
    @Published var errorMessage: String?

    func load(client: APIClient) async {
        isLoading = true
        defer { isLoading = false }

        let calendar = Calendar.current
        let now = Date()
        let month = calendar.component(.month, from: now)
        let year = calendar.component(.year, from: now)
        let endAt = Int64(now.timeIntervalSince1970 * 1000)
        let startAt = Int64((calendar.date(byAdding: .day, value: -30, to: now) ?? now).timeIntervalSince1970 * 1000)

        async let lowStockTask = capture { try await client.fetchLowStockProducts(limit: 10) }
        async let snapshotsTask = capture { try await client.fetchInventorySnapshots(startDate: startAt, endDate: endAt) }
        async let monthlyStatsTask = capture { try await client.fetchInventoryMonthlyStats(year: year, month: month) }

        let lowStockResult = await lowStockTask
        let snapshotsResult = await snapshotsTask
        let monthlyStatsResult = await monthlyStatsTask

        var failures: [String] = []

        switch lowStockResult {
        case let .success(value): lowStockProducts = value
        case .failure: failures.append("低库存")
        }
        switch snapshotsResult {
        case let .success(value): snapshots = value.sorted(by: { $0.snapshotDate > $1.snapshotDate })
        case .failure: failures.append("盘点快照")
        }
        switch monthlyStatsResult {
        case let .success(value): monthlyStats = value
        case .failure: failures.append("月度统计")
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
