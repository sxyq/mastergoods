import SwiftUI

struct CustomerDetailView: View {
    @Environment(\.appEnvironment) private var env
    @EnvironmentObject private var session: AppSession
    let customerId: EntityID
    @StateObject private var viewModel = CustomerDetailViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                if let errorMessage = viewModel.errorMessage {
                    EmptyStateView(title: "客户详情读取失败", message: errorMessage)
                } else if let customer = viewModel.customer {
                    headerCard(customer)
                    basicInfoSection(customer)
                    contactSection(customer)
                    contactListEntry
                    businessSection
                } else {
                    LoadingStateView(message: "正在加载客户详情...")
                }
            }
            .padding(20)
        }
        .navigationTitle("客户详情")
        .task {
            await viewModel.load(customerId: customerId, client: env.apiClient)
        }
    }

    private func headerCard(_ customer: CustomerRecord) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 6) {
                    Text(customer.name)
                        .font(ZhihuijiTheme.Typography.pageTitle)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                    Text(customer.phone)
                        .font(ZhihuijiTheme.Typography.body)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                }
                Spacer()
                StatusChip(
                    title: customer.status == 1 ? "启用" : "停用",
                    tint: customer.status == 1 ? ZhihuijiTheme.ColorToken.success : ZhihuijiTheme.ColorToken.warning
                )
            }

            if let balance = customer.balance {
                HStack(spacing: 10) {
                    metricBadge(
                        title: "应收余额",
                        value: balance.currencyText,
                        tint: balance >= 0 ? ZhihuijiTheme.ColorToken.warning : ZhihuijiTheme.ColorToken.success
                    )
                    if let level = customer.level {
                        metricBadge(
                            title: "客户等级",
                            value: "L\(level)",
                            tint: ZhihuijiTheme.ColorToken.primary
                        )
                    }
                }
            }
        }
        .padding(18)
        .glassCard()
    }

    private func basicInfoSection(_ customer: CustomerRecord) -> some View {
        detailSection(
            title: "基础信息",
            rows: [
                ("联系人", customer.primaryContactName),
                ("联系电话", customer.primaryContactPhone),
                ("分组", customer.groupName),
                ("等级", customer.level.map { "客户等级 \($0)" }),
                ("创建时间", customer.createdAt?.dateTimeText),
            ]
        )
    }

    private func contactSection(_ customer: CustomerRecord) -> some View {
        detailSection(
            title: "联系与备注",
            rows: [
                ("地址", customer.address),
                ("备注", customer.notes),
            ]
        )
    }

    private var contactListEntry: some View {
        NavigationLink {
            ContactListView(kind: .customer, partnerId: customerId)
        } label: {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("联系人管理")
                        .font(ZhihuijiTheme.Typography.bodyMedium)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                    Text("管理该客户的多个联系人，可设置主要联系人。")
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
                EmptyStateView(title: "暂无往来记录", message: "当前客户没有可展示的资金流水。")
            } else {
                VStack(spacing: 10) {
                    ForEach(viewModel.transactions) { record in
                        CustomerTransactionRow(record: record)
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

private struct CustomerTransactionRow: View {
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
final class CustomerDetailViewModel: ObservableObject {
    @Published var customer: CustomerRecord?
    @Published var transactions: [FinanceRecord] = []
    @Published var errorMessage: String?

    func load(customerId: EntityID, client: APIClient) async {
        do {
            let record = try await client.fetchCustomer(id: customerId)
            customer = record
            errorMessage = nil
            await loadTransactions(for: record, client: client)
        } catch {
            customer = nil
            transactions = []
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    private func loadTransactions(for customer: CustomerRecord, client: APIClient) async {
        do {
            let records = try await client.fetchFinanceRecords(
                keyword: customer.name,
                type: nil,
                page: 1,
                size: 20
            )
            transactions = records.filter { $0.partnerName?.nilIfBlank == customer.name }
        } catch {
            transactions = []
        }
    }
}
