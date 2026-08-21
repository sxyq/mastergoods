import SwiftUI

struct InventorySnapshotView: View {
    @Environment(\.appEnvironment) private var env
    @EnvironmentObject private var session: AppSession
    @StateObject private var viewModel = InventorySnapshotViewModel()

    private var actionPolicy: InventorySnapshotActionPolicy {
        InventorySnapshotActionPolicy.resolve(for: session.permissions)
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("库存盘点")
                    .font(ZhihuijiTheme.Typography.pageTitle)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)

                Text("按安卓移动端习惯先看风险，再看最近盘点和月度变化。")
                    .font(ZhihuijiTheme.Typography.body)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)

                PrimaryGlassButton(
                    title: viewModel.isLoading ? "刷新中..." : "刷新库存概览",
                    systemImage: "arrow.clockwise",
                    disabled: viewModel.isLoading
                ) {
                    Task { await viewModel.load(client: env.apiClient) }
                }

                if let statusMessage = viewModel.statusMessage {
                    statusBanner(text: statusMessage, tint: ZhihuijiTheme.ColorToken.primaryBright)
                }

                if actionPolicy.canManageInventory {
                    NavigationLink {
                        InventoryAdjustView()
                    } label: {
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text("库存调整与快照")
                                    .font(ZhihuijiTheme.Typography.bodyMedium)
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                                Text("做真实库存流水调整，并为选中商品生成盘点快照。")
                                    .font(ZhihuijiTheme.Typography.body)
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
                }

                NavigationLink {
                    InventoryLedgerView()
                } label: {
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("库存台账")
                                .font(ZhihuijiTheme.Typography.bodyMedium)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                            Text("查看库存进出流水明细，支持按商品筛选与汇总。")
                                .font(ZhihuijiTheme.Typography.body)
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

                summarySection
                pendingSnapshotsSection
                lowStockSection
                snapshotsSection
                monthlyStatsSection
            }
            .padding(20)
        }
        .navigationTitle("库存")
        .safeAreaInset(edge: .bottom) {
            bottomActionBar
        }
        .task {
            await viewModel.load(client: env.apiClient)
        }
    }

    private var summarySection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("盘点摘要")
                .font(ZhihuijiTheme.Typography.sectionTitle)
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                MetricCard(
                    title: "今日快照",
                    value: "\(viewModel.todaySnapshots.count)",
                    subtitle: "已生成",
                    tint: ZhihuijiTheme.ColorToken.primary
                )
                MetricCard(
                    title: "待生成",
                    value: "\(viewModel.pendingSnapshotProducts.count)",
                    subtitle: "今日未盘",
                    tint: viewModel.pendingSnapshotProducts.isEmpty ? ZhihuijiTheme.ColorToken.success : ZhihuijiTheme.ColorToken.warning
                )
                MetricCard(
                    title: "低库存",
                    value: "\(viewModel.lowStockProducts.count)",
                    subtitle: "当前预警",
                    tint: ZhihuijiTheme.ColorToken.danger
                )
                MetricCard(
                    title: "快照货值",
                    value: viewModel.todaySnapshotValue.currencyText,
                    subtitle: "今日库存",
                    tint: ZhihuijiTheme.ColorToken.primaryBright
                )
            }
        }
    }

    private var pendingSnapshotsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("今日待盘商品")
                        .font(ZhihuijiTheme.Typography.sectionTitle)
                    Text("先补齐今天还没生成快照的商品，再去做库存调整或复核。")
                        .font(ZhihuijiTheme.Typography.caption)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                }
                Spacer()
            }

                PrimaryGlassButton(
                    title: viewModel.isSubmitting ? "生成中..." : "一键生成今日快照",
                    systemImage: "checklist.checked",
                    disabled: viewModel.isSubmitting || viewModel.pendingSnapshotProducts.isEmpty || !actionPolicy.canManageInventory
                ) {
                    Task { await viewModel.createTodaySnapshots(client: env.apiClient) }
                }

            if viewModel.pendingSnapshotProducts.isEmpty {
                EmptyStateView(title: "今日快照已齐", message: "当前商品都已有今日盘点快照。")
            } else {
                ForEach(viewModel.pendingSnapshotProducts.prefix(6)) { product in
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(product.name)
                                .font(ZhihuijiTheme.Typography.cardTitle)
                            Text(product.code)
                                .font(ZhihuijiTheme.Typography.caption)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                        }
                        Spacer()
                        VStack(alignment: .trailing, spacing: 4) {
                            Text("库存 \(String(format: "%.2f", product.stock))")
                                .font(ZhihuijiTheme.Typography.bodyMedium)
                            Text(product.categoryName ?? "未分组")
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

    private var lowStockSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("低库存提醒")
                .font(ZhihuijiTheme.Typography.sectionTitle)
            if viewModel.lowStockProducts.isEmpty {
                EmptyStateView(title: "暂无低库存商品", message: "当前安全库存指标看起来比较平稳。")
            } else {
                ForEach(viewModel.lowStockProducts.prefix(6)) { product in
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
                                .foregroundStyle(ZhihuijiTheme.ColorToken.danger)
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

    private var snapshotsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("最近盘点快照")
                .font(ZhihuijiTheme.Typography.sectionTitle)
            if viewModel.snapshots.isEmpty {
                EmptyStateView(title: "暂无盘点快照", message: "当前没有可用的库存快照记录。")
            } else {
                ForEach(viewModel.snapshots.prefix(6)) { snapshot in
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(snapshot.productName)
                                .font(ZhihuijiTheme.Typography.cardTitle)
                            Text(snapshot.productCode)
                                .font(ZhihuijiTheme.Typography.caption)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                            Text(snapshot.snapshotDate.dateText)
                                .font(ZhihuijiTheme.Typography.caption)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                        }
                        Spacer()
                        VStack(alignment: .trailing, spacing: 4) {
                            Text("数量 \(String(format: "%.2f", snapshot.quantity))")
                                .font(ZhihuijiTheme.Typography.bodyMedium)
                            if let totalValue = snapshot.totalValue {
                                Text(totalValue.currencyText)
                                    .font(ZhihuijiTheme.Typography.caption)
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                            }
                        }
                    }
                    .padding(14)
                    .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
                }
            }
        }
    }

    private var monthlyStatsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("本月进出概览")
                .font(ZhihuijiTheme.Typography.sectionTitle)
            if viewModel.monthlyStats.isEmpty {
                EmptyStateView(title: "暂无月度统计", message: "当前月份还没有月度库存统计数据。")
            } else {
                ForEach(viewModel.monthlyStats.prefix(6)) { item in
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(item.productName)
                                .font(ZhihuijiTheme.Typography.cardTitle)
                            Text(item.productCode ?? "")
                                .font(ZhihuijiTheme.Typography.caption)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                        }
                        Spacer()
                        VStack(alignment: .trailing, spacing: 4) {
                            Text("入 \(String(format: "%.2f", item.quantityIn ?? 0)) / 出 \(String(format: "%.2f", item.quantityOut ?? 0))")
                                .font(ZhihuijiTheme.Typography.captionSemibold)
                            Text("结余 \(String(format: "%.2f", item.quantityEnd ?? 0))")
                                .font(ZhihuijiTheme.Typography.bodyMedium)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                        }
                    }
                    .padding(14)
                    .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
                }
            }
        }
    }

    private var bottomActionBar: some View {
        HStack(spacing: 12) {
            InventorySecondaryActionButton(
                title: viewModel.isLoading ? "刷新中..." : "刷新",
                systemImage: "arrow.clockwise",
                tint: ZhihuijiTheme.ColorToken.primary,
                disabled: viewModel.isLoading
            ) {
                Task { await viewModel.load(client: env.apiClient) }
            }

            PrimaryGlassButton(
                title: viewModel.isSubmitting ? "生成中..." : "一键生成今日快照",
                systemImage: "checklist.checked",
                disabled: viewModel.isSubmitting || viewModel.pendingSnapshotProducts.isEmpty || !actionPolicy.canManageInventory
            ) {
                Task { await viewModel.createTodaySnapshots(client: env.apiClient) }
            }
        }
        .padding(.horizontal, 20)
        .padding(.top, 12)
        .padding(.bottom, 12)
        .background(.ultraThinMaterial)
        .overlay(alignment: .top) {
            Rectangle()
                .fill(Color.white.opacity(0.42))
                    .frame(height: ZhihuijiTheme.Stroke.hairline)
        }
    }

    private func statusBanner(text: String, tint: Color) -> some View {
        HStack(alignment: .top, spacing: 10) {
            Circle()
                .fill(tint.opacity(0.14))
                .frame(width: 26, height: 26)
                .overlay(
                    Image(systemName: "info.circle.fill")
                        .font(ZhihuijiTheme.Typography.caption)
                        .foregroundStyle(tint)
                )
            Text(text)
                .font(ZhihuijiTheme.Typography.caption)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                .fixedSize(horizontal: false, vertical: true)
            Spacer()
        }
        .padding(12)
        .background(Color.white.opacity(0.42), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.cardSmall, style: .continuous))
    }
}

private struct InventorySecondaryActionButton: View {
    let title: String
    let systemImage: String
    let tint: Color
    var disabled = false
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                Image(systemName: systemImage)
                    .font(ZhihuijiTheme.Typography.bodyMedium)
                Text(title)
                    .font(ZhihuijiTheme.Typography.bodyMedium)
            }
            .foregroundStyle(disabled ? ZhihuijiTheme.ColorToken.textTertiary : tint)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(
                (disabled ? Color.white.opacity(0.38) : tint.opacity(0.10)),
                in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.pill, style: .continuous)
            )
            .overlay(
                RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.pill, style: .continuous)
                    .stroke(Color.white.opacity(0.45), lineWidth: ZhihuijiTheme.Stroke.hairline)
            )
        }
        .buttonStyle(.plain)
        .disabled(disabled)
        .opacity(disabled ? 0.7 : 1)
    }
}

@MainActor
final class InventorySnapshotViewModel: ObservableObject {
    @Published var isLoading = false
    @Published var isSubmitting = false
    @Published var products: [ProductRecord] = []
    @Published var lowStockProducts: [LowStockProductReport] = []
    @Published var todaySnapshots: [InventorySnapshotSummary] = []
    @Published var snapshots: [InventorySnapshotSummary] = []
    @Published var monthlyStats: [InventoryMonthlyStats] = []
    @Published var statusMessage: String?
    @Published var errorMessage: String?

    var pendingSnapshotProducts: [ProductRecord] {
        let snapshottedIds = Set(todaySnapshots.map(\.productId))
        return products.filter { !snapshottedIds.contains($0.id) }
    }

    var todaySnapshotValue: Double {
        todaySnapshots.compactMap(\.totalValue).reduce(0, +)
    }

    func load(client: APIClient) async {
        isLoading = true
        statusMessage = nil
        errorMessage = nil
        defer { isLoading = false }

        let calendar = Calendar.current
        let now = Date()
        let month = calendar.component(.month, from: now)
        let year = calendar.component(.year, from: now)
        let todayStart = Int64(calendar.startOfDay(for: now).timeIntervalSince1970 * 1000)
        let endAt = Int64(now.timeIntervalSince1970 * 1000)
        let startAt = Int64((calendar.date(byAdding: .day, value: -30, to: now) ?? now).timeIntervalSince1970 * 1000)

        async let productsTask = capture { try await client.fetchProducts(page: 1, size: 60) }
        async let lowStockTask = capture { try await client.fetchLowStockProducts(limit: 10) }
        async let todaySnapshotsTask = capture { try await client.fetchInventorySnapshots(snapshotDate: todayStart, page: 1, size: 100) }
        async let snapshotsTask = capture { try await client.fetchInventorySnapshots(startDate: startAt, endDate: endAt, page: 1, size: 60) }
        async let monthlyStatsTask = capture { try await client.fetchInventoryMonthlyStats(year: year, month: month, page: 1, size: 60) }

        let productsResult = await productsTask
        let lowStockResult = await lowStockTask
        let todaySnapshotsResult = await todaySnapshotsTask
        let snapshotsResult = await snapshotsTask
        let monthlyStatsResult = await monthlyStatsTask

        var failures: [String] = []

        switch productsResult {
        case let .success(value): products = value
        case .failure:
            products = []
            failures.append("商品档案")
        }
        switch lowStockResult {
        case let .success(value): lowStockProducts = value
        case .failure:
            lowStockProducts = []
            failures.append("低库存")
        }
        switch todaySnapshotsResult {
        case let .success(value): todaySnapshots = value.sorted(by: { $0.productName.localizedCompare($1.productName) == .orderedAscending })
        case .failure:
            todaySnapshots = []
            failures.append("今日快照")
        }
        switch snapshotsResult {
        case let .success(value): snapshots = value.sorted(by: { $0.snapshotDate > $1.snapshotDate })
        case .failure:
            snapshots = []
            failures.append("盘点快照")
        }
        switch monthlyStatsResult {
        case let .success(value): monthlyStats = value
        case .failure:
            monthlyStats = []
            failures.append("月度统计")
        }

        errorMessage = failures.isEmpty ? nil : "以下分区暂未成功拉取：\(failures.joined(separator: "、"))"
    }

    func createTodaySnapshots(client: APIClient) async {
        guard !pendingSnapshotProducts.isEmpty else {
            statusMessage = "今日快照已经齐全，无需重复生成。"
            return
        }

        isSubmitting = true
        defer { isSubmitting = false }

        let todayStart = Int64(Calendar.current.startOfDay(for: Date()).timeIntervalSince1970 * 1000)
        var successCount = 0
        var failedCount = 0

        for product in pendingSnapshotProducts {
            do {
                _ = try await client.createInventorySnapshot(
                    payload: InventorySnapshotCreatePayload(
                        productId: product.id,
                        snapshotDate: todayStart,
                        warehouseId: nil
                    )
                )
                successCount += 1
            } catch {
                failedCount += 1
            }
        }

        if failedCount == 0 {
            statusMessage = "已生成 \(successCount) 条今日盘点快照。"
        } else {
            statusMessage = "已生成 \(successCount) 条今日盘点快照，另有 \(failedCount) 条失败。"
        }

        await load(client: client)
    }

    private func capture<T>(_ operation: @escaping () async throws -> T) async -> Result<T, Error> {
        do {
            return .success(try await operation())
        } catch {
            return .failure(error)
        }
    }
}
