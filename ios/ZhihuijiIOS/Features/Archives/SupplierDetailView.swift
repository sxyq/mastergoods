import SwiftUI

struct SupplierDetailView: View {
    @Environment(\.appEnvironment) private var env
    @EnvironmentObject private var session: AppSession
    let supplierId: EntityID
    @StateObject private var viewModel = SupplierDetailViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                if let errorMessage = viewModel.errorMessage {
                    EmptyStateView(title: "供应商详情读取失败", message: errorMessage)
                } else if let supplier = viewModel.supplier {
                    headerCard(supplier)
                    statementEntryCard(supplier)
                    basicInfoSection(supplier)
                    contactSection(supplier)
                    businessSection
                } else {
                    LoadingStateView(message: "正在加载供应商详情...")
                }
            }
            .padding(20)
        }
        .navigationTitle("供应商详情")
        .task {
            await viewModel.load(supplierId: supplierId, client: env.apiClient)
        }
    }

    private func headerCard(_ supplier: SupplierRecord) -> some View {
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

            if let balance = supplier.balance {
                metricBadge(
                    title: "应付余额",
                    value: balance.currencyText,
                    tint: balance >= 0 ? ZhihuijiTheme.ColorToken.warning : ZhihuijiTheme.ColorToken.success
                )
            }
        }
        .padding(18)
        .glassCard()
    }

    private func statementEntryCard(_ supplier: SupplierRecord) -> some View {
        NavigationLink {
            SupplierStatementView(supplierId: supplier.id)
        } label: {
            HStack(spacing: 12) {
                Circle()
                    .fill(ZhihuijiTheme.ColorToken.warning.opacity(0.16))
                    .frame(width: 42, height: 42)
                    .overlay(
                        Image(systemName: "doc.text.magnifyingglass")
                            .foregroundStyle(ZhihuijiTheme.ColorToken.warning)
                    )

                VStack(alignment: .leading, spacing: 4) {
                    Text("查看对账单")
                        .font(ZhihuijiTheme.Typography.bodyMedium)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                    Text("按供应商维度汇总应付、已付与待付明细。")
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

    private func basicInfoSection(_ supplier: SupplierRecord) -> some View {
        detailSection(
            title: "基础信息",
            rows: [
                ("联系人", supplier.primaryContactName),
                ("联系电话", supplier.primaryContactPhone),
                ("分组", supplier.groupName),
                ("创建时间", supplier.createdAt?.dateTimeText),
            ]
        )
    }

    private func contactSection(_ supplier: SupplierRecord) -> some View {
        detailSection(
            title: "联系与备注",
            rows: [
                ("地址", supplier.address),
                ("备注", supplier.notes),
            ]
        )
    }

    private var businessSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("经营往来")
                    .font(ZhihuijiTheme.Typography.sectionTitle)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                Spacer()
                StatusChip(
                    title: "\(viewModel.transactions.count) 笔",
                    tint: ZhihuijiTheme.ColorToken.primary
                )
            }

            if viewModel.transactions.isEmpty {
                EmptyStateView(title: "暂无往来记录", message: "当前供应商没有可展示的资金流水。")
            } else {
                VStack(spacing: 10) {
                    ForEach(viewModel.transactions) { record in
                        SupplierTransactionRow(record: record)
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func detailSection(title: String, rows: [(String, String?)]) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(title)
                .font(ZhihuijiTheme.Typography.sectionTitle)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)

            VStack(spacing: 10) {
                ForEach(rows.filter { $0.1?.nilIfBlank != nil }, id: \.0) { row in
                    HStack(alignment: .top) {
                        Text(row.0)
                            .font(ZhihuijiTheme.Typography.captionSemibold)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                            .frame(width: 72, alignment: .leading)
                        Text(row.1 ?? "-")
                            .font(ZhihuijiTheme.Typography.body)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                            .fixedSize(horizontal: false, vertical: true)
                        Spacer()
                    }
                }
            }
            .padding(16)
            .glassCard()
        }
    }

    private func metricBadge(title: String, value: String, tint: Color) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title)
                .font(ZhihuijiTheme.Typography.captionSemibold)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
            Text(value)
                .font(ZhihuijiTheme.Typography.bodyMedium)
                .foregroundStyle(tint)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(14)
        .background(tint.opacity(0.10), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.cardSmall, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.cardSmall, style: .continuous)
                .stroke(Color.white.opacity(0.45), lineWidth: ZhihuijiTheme.Stroke.hairline)
        )
    }
}

private struct SupplierTransactionRow: View {
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
                    Text(record.amount.currencyText)
                        .font(ZhihuijiTheme.Typography.bodyMedium)
                        .foregroundStyle(record.typeTint)
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
            }
        }
        .padding(14)
        .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
    }
}

@MainActor
final class SupplierDetailViewModel: ObservableObject {
    @Published var supplier: SupplierRecord?
    @Published var transactions: [FinanceRecord] = []
    @Published var errorMessage: String?

    func load(supplierId: EntityID, client: APIClient) async {
        do {
            let record = try await client.fetchSupplier(id: supplierId)
            supplier = record
            errorMessage = nil
            await loadTransactions(for: record, client: client)
        } catch {
            supplier = nil
            transactions = []
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    private func loadTransactions(for supplier: SupplierRecord, client: APIClient) async {
        do {
            let records = try await client.fetchFinanceRecords(
                keyword: supplier.name,
                type: nil,
                page: 1,
                size: 20
            )
            transactions = records.filter { $0.partnerName?.nilIfBlank == supplier.name }
        } catch {
            transactions = []
        }
    }
}
