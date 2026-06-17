import SwiftUI

struct FinanceRecordView: View {
    @Environment(\.appEnvironment) private var env
    @StateObject private var viewModel = FinanceRecordViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("资金流水")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)

                TextField("搜索流水号 / 分类 / 往来方", text: $viewModel.keyword)
                    .fieldBackground()

                Picker("类型", selection: $viewModel.typeFilter) {
                    ForEach(FinanceTypeFilter.allCases) { filter in
                        Text(filter.title).tag(filter)
                    }
                }
                .pickerStyle(.segmented)

                NavigationLink {
                    PayOrderDetailView()
                } label: {
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("付款单工作台")
                                .font(.system(size: 16, weight: .semibold))
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                            Text("查看付款单状态、创建付款单、切换已付款。")
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

                financeCreateForm

                PrimaryGlassButton(
                    title: viewModel.isLoading ? "刷新中..." : "刷新流水",
                    systemImage: "arrow.clockwise",
                    disabled: viewModel.isLoading
                ) {
                    Task { await viewModel.load(client: env.apiClient) }
                }

                if let errorMessage = viewModel.errorMessage {
                    EmptyStateView(title: "资金流水加载失败", message: errorMessage)
                } else if viewModel.records.isEmpty, !viewModel.isLoading {
                    EmptyStateView(title: "暂无资金流水", message: "当前筛选条件下没有资金记录。")
                } else {
                    LazyVStack(spacing: 12) {
                        ForEach(viewModel.records) { record in
                            FinanceRecordCard(record: record)
                        }
                    }
                }
            }
            .padding(20)
        }
        .navigationTitle("资金")
        .task {
            await viewModel.load(client: env.apiClient)
        }
        .onChange(of: viewModel.typeFilter) { _, _ in
            Task { await viewModel.load(client: env.apiClient) }
        }
    }

    private var financeCreateForm: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("记一笔日常流水")
                .font(.system(size: 18, weight: .semibold))
            Picker("方向", selection: $viewModel.createType) {
                ForEach(FinanceRecordType.allCases) { item in
                    Text(item.label).tag(item)
                }
            }
            .pickerStyle(.segmented)
            TextField("分类", text: $viewModel.category)
                .fieldBackground()
            TextField("往来方", text: $viewModel.partnerName)
                .fieldBackground()
            TextField("金额", text: $viewModel.amountText)
                .fieldBackground()
            Picker("方式", selection: $viewModel.method) {
                ForEach(SalePaymentMethod.allCases) { method in
                    Text(method.label).tag(method)
                }
            }
            .pickerStyle(.menu)
            .padding(.horizontal, 14)
            .padding(.vertical, 14)
            .background(Color.white.opacity(0.58), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.field, style: .continuous))
            TextField("备注", text: $viewModel.notes, axis: .vertical)
                .fieldBackground()
            PrimaryGlassButton(
                title: viewModel.isSubmitting ? "保存中..." : "保存流水",
                systemImage: "plus.circle.fill",
                disabled: viewModel.isSubmitting
            ) {
                Task { await viewModel.createRecord(client: env.apiClient) }
            }
        }
        .padding(16)
        .glassCard()
    }
}

@MainActor
final class FinanceRecordViewModel: ObservableObject {
    @Published var keyword = ""
    @Published var typeFilter: FinanceTypeFilter = .all
    @Published var createType: FinanceRecordType = .expense
    @Published var category = ""
    @Published var partnerName = ""
    @Published var amountText = ""
    @Published var method: SalePaymentMethod = .cash
    @Published var notes = ""
    @Published var isLoading = false
    @Published var isSubmitting = false
    @Published var records: [FinanceRecord] = []
    @Published var errorMessage: String?

    func load(client: APIClient) async {
        isLoading = true
        defer { isLoading = false }
        do {
            records = try await client.fetchFinanceRecords(
                keyword: keyword.nilIfBlank,
                type: typeFilter.apiValue,
                page: 1,
                size: 20
            )
            errorMessage = nil
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    func createRecord(client: APIClient) async {
        guard let amount = Double(amountText), amount > 0 else {
            errorMessage = "请输入正确的金额"
            return
        }
        guard let category = category.nilIfBlank else {
            errorMessage = "请输入分类"
            return
        }
        isSubmitting = true
        defer { isSubmitting = false }
        do {
            let created = try await client.createFinanceRecord(
                payload: FinanceRecordCreatePayload(
                    type: createType.rawValue,
                    category: category,
                    partnerName: partnerName.nilIfBlank,
                    amount: amount,
                    method: method.rawValue,
                    notes: notes.nilIfBlank
                )
            )
            records.insert(created, at: 0)
            amountText = ""
            self.category = ""
            partnerName = ""
            notes = ""
            errorMessage = nil
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }
}

private struct FinanceRecordCard: View {
    let record: FinanceRecord

    var body: some View {
        HStack(spacing: 12) {
            Circle()
                .fill(record.typeTint.opacity(0.16))
                .frame(width: 42, height: 42)
                .overlay(
                    Image(systemName: record.type == FinanceRecordType.income.rawValue ? "arrow.down.circle.fill" : "arrow.up.circle.fill")
                        .foregroundStyle(record.typeTint)
                )

            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text(record.recordNo)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                    Spacer()
                    StatusChip(title: record.typeLabel, tint: record.typeTint)
                }
                Text(record.category)
                    .font(.system(size: 13))
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                Text(record.partnerName ?? "无往来方")
                    .font(.system(size: 12))
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                HStack {
                    Text(record.amount.currencyText)
                    Spacer()
                    Text(record.createdAt.dateTimeText)
                }
                .font(.system(size: 12))
                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
            }
        }
        .padding(16)
        .glassCard()
    }
}

enum FinanceTypeFilter: String, CaseIterable, Identifiable {
    case all
    case income
    case expense

    var id: String { rawValue }

    var title: String {
        switch self {
        case .all: return "全部"
        case .income: return "收入"
        case .expense: return "支出"
        }
    }

    var apiValue: Int? {
        switch self {
        case .all: return nil
        case .income: return 1
        case .expense: return 2
        }
    }
}
