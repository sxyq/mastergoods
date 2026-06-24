import SwiftUI

struct FinanceRecordView: View {
    @Environment(\.appEnvironment) private var env
    @EnvironmentObject private var session: AppSession
    @StateObject private var viewModel = FinanceRecordViewModel()

    private var actionPolicy: FinanceRecordActionPolicy {
        FinanceRecordActionPolicy.resolve(for: session.permissions)
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("资金流水")
                    .font(ZhihuijiTheme.Typography.pageTitle)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)

                TextField("搜索流水号 / 分类 / 往来方", text: $viewModel.keyword)
                    .fieldBackground()
                    .onSubmit {
                        Task { await viewModel.load(client: env.apiClient) }
                    }

                Picker("类型", selection: $viewModel.typeFilter) {
                    ForEach(FinanceTypeFilter.allCases) { filter in
                        Text(filter.title).tag(filter)
                    }
                }
                .pickerStyle(.segmented)

                summarySection

                cashChangeSection

                NavigationLink {
                    PayOrderDetailView()
                } label: {
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("付款单工作台")
                                .font(ZhihuijiTheme.Typography.bodyMedium)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                            Text("查看付款单状态、创建付款单、切换已付款。")
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

                NavigationLink {
                    DailyExpenseView()
                } label: {
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("每日支出")
                                .font(ZhihuijiTheme.Typography.bodyMedium)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                            Text("按日期分组查看支出流水，汇总本周/本月支出。")
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

                if actionPolicy.canWriteFinance {
                    financeCreateForm
                } else {
                    EmptyStateView(
                        title: "资金写入受限",
                        message: "当前账号只有资金查看权限，不能新增日常流水或现金调整。"
                    )
                }

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
        .sheet(isPresented: $viewModel.isCashChangeSheetPresented) {
            CashChangeFormSheet(viewModel: viewModel) {
                await viewModel.createCashChange(client: env.apiClient)
            }
        }
    }

    private var cashChangeSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("现金调整")
                        .font(ZhihuijiTheme.Typography.sectionTitle)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                    Text("登记应收/实收差额，关联单据并写入 /v2/cash-change-records。")
                        .font(ZhihuijiTheme.Typography.caption)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
                Spacer()
                Button {
                    viewModel.isCashChangeSheetPresented = true
                } label: {
                    StatusChip(title: "新建调整", tint: ZhihuijiTheme.ColorToken.primary)
                }
                .buttonStyle(.plain)
                .disabled(!actionPolicy.canWriteFinance)
                .opacity(actionPolicy.canWriteFinance ? 1 : 0.5)
            }

            if viewModel.cashChangeRecords.isEmpty {
                Text("暂无现金调整记录")
                    .font(ZhihuijiTheme.Typography.caption)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
            } else {
                LazyVStack(spacing: 10) {
                    ForEach(viewModel.cashChangeRecords) { record in
                        CashChangeRecordCard(record: record, canDelete: actionPolicy.canWriteFinance) {
                            Task { await viewModel.deleteCashChange(id: record.id, client: env.apiClient) }
                        }
                    }
                }
            }
        }
        .padding(14)
        .background(Color.white.opacity(0.52), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.cardSmall, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.cardSmall, style: .continuous)
                .stroke(ZhihuijiTheme.ColorToken.primary.opacity(0.18), lineWidth: ZhihuijiTheme.Stroke.hairline)
        )
    }

    private var financeCreateForm: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("记一笔日常流水")
                .font(ZhihuijiTheme.Typography.sectionTitle)
            Picker("方向", selection: $viewModel.createType) {
                ForEach(FinanceRecordType.allCases) { item in
                    Text(item.label).tag(item)
                }
            }
            .pickerStyle(.segmented)

            quickCategorySection

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
                disabled: viewModel.isSubmitting || !actionPolicy.canWriteFinance
            ) {
                Task { await viewModel.createRecord(client: env.apiClient) }
            }
        }
        .padding(16)
        .glassCard()
    }

    private var summarySection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("经营摘要")
                .font(ZhihuijiTheme.Typography.sectionTitle)
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                MetricCard(
                    title: "收入",
                    value: viewModel.incomeTotal.currencyText,
                    subtitle: viewModel.summarySubtitle,
                    tint: ZhihuijiTheme.ColorToken.success
                )
                MetricCard(
                    title: "支出",
                    value: viewModel.expenseTotal.currencyText,
                    subtitle: viewModel.summarySubtitle,
                    tint: ZhihuijiTheme.ColorToken.warning
                )
                MetricCard(
                    title: "净额",
                    value: viewModel.netTotal.currencyText,
                    subtitle: "当前筛选",
                    tint: viewModel.netTotal >= 0 ? ZhihuijiTheme.ColorToken.primary : ZhihuijiTheme.ColorToken.danger
                )
                MetricCard(
                    title: "笔数",
                    value: "\(viewModel.records.count)",
                    subtitle: "可见流水",
                    tint: ZhihuijiTheme.ColorToken.primaryBright
                )
            }
        }
    }

    private var quickCategorySection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("常用分类")
                .font(ZhihuijiTheme.Typography.captionSemibold)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    ForEach(viewModel.quickCategories, id: \.self) { item in
                        Button {
                            viewModel.category = item
                        } label: {
                            Text(item)
                                .font(ZhihuijiTheme.Typography.captionSemibold)
                                .foregroundStyle(viewModel.category == item ? .white : ZhihuijiTheme.ColorToken.textPrimary)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 8)
                                .background(
                                    (viewModel.category == item
                                        ? LinearGradient(colors: [ZhihuijiTheme.ColorToken.primaryBright, ZhihuijiTheme.ColorToken.primary], startPoint: .leading, endPoint: .trailing)
                                        : LinearGradient(colors: [Color.white.opacity(0.58), Color.white.opacity(0.58)], startPoint: .leading, endPoint: .trailing)
                                    ),
                                    in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.cardSmall, style: .continuous)
                                )
                                .overlay(
                                    RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.cardSmall, style: .continuous)
                                        .stroke(Color.white.opacity(0.45), lineWidth: ZhihuijiTheme.Stroke.hairline)
                                )
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.vertical, 2)
            }
        }
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

    @Published var isCashChangeSheetPresented = false
    @Published var isSubmittingCashChange = false
    @Published var cashChangeRecords: [CashChangeRecord] = []
    @Published var cashChangeOrderType = "sale_order"
    @Published var cashChangeOrderId = ""
    @Published var cashChangeReceivableText = ""
    @Published var cashChangeReceivedText = ""
    @Published var cashChangeAccountId = ""
    @Published var cashChangeNotes = ""

    var incomeTotal: Double {
        records.filter { $0.type == FinanceRecordType.income.rawValue }.reduce(0) { $0 + $1.amount }
    }

    var expenseTotal: Double {
        records.filter { $0.type == FinanceRecordType.expense.rawValue }.reduce(0) { $0 + $1.amount }
    }

    var netTotal: Double {
        incomeTotal - expenseTotal
    }

    var summarySubtitle: String {
        switch typeFilter {
        case .all:
            return "当前全部"
        case .income:
            return "当前收入"
        case .expense:
            return "当前支出"
        }
    }

    var quickCategories: [String] {
        switch createType {
        case .income:
            return ["销售回款", "其他收入", "退款回冲", "预收款"]
        case .expense:
            return ["房租水电", "办公采购", "工资福利", "物流运费", "营销支出"]
        }
    }

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
        await loadCashChangeRecords(client: client)
    }

    func loadCashChangeRecords(client: APIClient) async {
        do {
            cashChangeRecords = try await client.fetchCashChangeRecords()
        } catch {
            cashChangeRecords = []
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

    func createCashChange(client: APIClient) async {
        guard let receivable = Double(cashChangeReceivableText) else {
            errorMessage = "请输入正确的应收金额"
            return
        }
        guard let received = Double(cashChangeReceivedText) else {
            errorMessage = "请输入正确的实收金额"
            return
        }
        isSubmittingCashChange = true
        defer { isSubmittingCashChange = false }
        do {
            let payload = CashChangeRecordCreatePayload(
                orderType: cashChangeOrderType,
                orderId: cashChangeOrderId.nilIfBlank.map { EntityID(rawValue: $0) },
                receivable: receivable,
                received: received,
                accountId: cashChangeAccountId.nilIfBlank.map { EntityID(rawValue: $0) },
                status: 1,
                notes: cashChangeNotes.nilIfBlank
            )
            let created = try await client.createCashChangeRecord(payload: payload)
            cashChangeRecords.insert(created, at: 0)
            cashChangeOrderId = ""
            cashChangeReceivableText = ""
            cashChangeReceivedText = ""
            cashChangeNotes = ""
            isCashChangeSheetPresented = false
            errorMessage = nil
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    func deleteCashChange(id: EntityID, client: APIClient) async {
        do {
            try await client.deleteCashChangeRecord(id: id)
            cashChangeRecords.removeAll { $0.id == id }
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
                        .font(ZhihuijiTheme.Typography.cardTitle)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                    Spacer()
                    StatusChip(title: record.typeLabel, tint: record.typeTint)
                }
                Text(record.category)
                    .font(ZhihuijiTheme.Typography.caption)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                Text(record.partnerName ?? "无往来方")
                    .font(ZhihuijiTheme.Typography.caption)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                HStack {
                    Text(record.amount.currencyText)
                    Spacer()
                    Text(record.createdAt.dateTimeText)
                }
                .font(ZhihuijiTheme.Typography.caption)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                HStack {
                    Text(record.methodLabel)
                    Spacer()
                    Text(record.updatedAt.dateText)
                }
                .font(ZhihuijiTheme.Typography.caption)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                if let notes = record.notes?.nilIfBlank {
                    Text(notes)
                        .font(ZhihuijiTheme.Typography.caption)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                        .lineLimit(2)
                }
            }
        }
        .padding(16)
        .glassCard()
    }
}

private struct CashChangeRecordCard: View {
    let record: CashChangeRecord
    let canDelete: Bool
    let onDelete: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            Circle()
                .fill(record.statusTint.opacity(0.16))
                .frame(width: 42, height: 42)
                .overlay(
                    Image(systemName: "yensign.circle.fill")
                        .foregroundStyle(record.statusTint)
                )

            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text(record.orderType)
                        .font(ZhihuijiTheme.Typography.cardTitle)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                    Spacer()
                    StatusChip(title: record.statusLabel, tint: record.statusTint)
                }
                if let orderId = record.orderId {
                    Text("单据 \(orderId.rawValue)")
                        .font(ZhihuijiTheme.Typography.caption)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                }
                if let accountName = record.accountName?.nilIfBlank {
                    Text("账户 \(accountName)")
                        .font(ZhihuijiTheme.Typography.caption)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                }
                HStack {
                    Text("应收 \(record.receivable.currencyText)")
                    Spacer()
                    Text("实收 \(record.received.currencyText)")
                }
                .font(ZhihuijiTheme.Typography.caption)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                HStack {
                    Text("找零 \(record.changeAmount.currencyText)")
                    Spacer()
                    Text(record.createdAt.dateTimeText)
                }
                .font(ZhihuijiTheme.Typography.caption)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                if let notes = record.notes?.nilIfBlank {
                    Text(notes)
                        .font(ZhihuijiTheme.Typography.caption)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                        .lineLimit(2)
                }
            }

            if canDelete {
                Button(role: .destructive, action: onDelete) {
                    Image(systemName: "trash")
                        .font(ZhihuijiTheme.Typography.captionSemibold)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.danger)
                        .padding(8)
                        .background(ZhihuijiTheme.ColorToken.danger.opacity(0.10), in: Circle())
                }
                .buttonStyle(.plain)
            }
        }
        .padding(16)
        .glassCard()
    }
}

private struct CashChangeFormSheet: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var viewModel: FinanceRecordViewModel
    let onSubmit: () async -> Void

    private var orderTypeOptions: [String] {
        ["sale_order", "purchase_order", "finance_record", "manual"]
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    Text("关联单据")
                        .font(ZhihuijiTheme.Typography.sectionTitle)
                    Picker("单据类型", selection: $viewModel.cashChangeOrderType) {
                        ForEach(orderTypeOptions, id: \.self) { option in
                            Text(option).tag(option)
                        }
                    }
                    .pickerStyle(.menu)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 14)
                    .background(Color.white.opacity(0.58), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.field, style: .continuous))
                    TextField("单据 ID（可选）", text: $viewModel.cashChangeOrderId)
                        .fieldBackground()

                    Text("金额")
                        .font(ZhihuijiTheme.Typography.sectionTitle)
                    TextField("应收金额", text: $viewModel.cashChangeReceivableText)
                        .fieldBackground()
                        .decimalInputKeyboard()
                    TextField("实收金额", text: $viewModel.cashChangeReceivedText)
                        .fieldBackground()
                        .decimalInputKeyboard()

                    Text("资金账户")
                        .font(ZhihuijiTheme.Typography.sectionTitle)
                    TextField("账户 ID（可选）", text: $viewModel.cashChangeAccountId)
                        .fieldBackground()
                        .numberInputKeyboard()

                    Text("备注")
                        .font(ZhihuijiTheme.Typography.sectionTitle)
                    TextField("备注（可选）", text: $viewModel.cashChangeNotes, axis: .vertical)
                        .lineLimit(2...4)
                        .fieldBackground()

                    PrimaryGlassButton(
                        title: viewModel.isSubmittingCashChange ? "保存中..." : "提交现金调整",
                        systemImage: "plus.circle.fill",
                        disabled: viewModel.isSubmittingCashChange
                    ) {
                        Task { await onSubmit() }
                    }
                }
                .padding(20)
            }
            .navigationTitle("现金调整")
            .inlineNavigationTitle()
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("关闭") { dismiss() }
                }
            }
        }
        .zhihuijiBackground()
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
