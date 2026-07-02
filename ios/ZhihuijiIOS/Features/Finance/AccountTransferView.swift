import SwiftUI

struct AccountTransferView: View {
    @Environment(\.appEnvironment) private var env
    @EnvironmentObject private var session: AppSession
    @StateObject private var viewModel = AccountTransferViewModel()

    private var actionPolicy: FinanceRecordActionPolicy {
        FinanceRecordActionPolicy.resolve(for: session.permissions)
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                headerCard

                if let errorMessage = viewModel.errorMessage {
                    EmptyStateView(title: "转账记录读取失败", message: errorMessage)
                } else if viewModel.transfers.isEmpty, !viewModel.isLoading {
                    EmptyStateView(title: "暂无转账记录", message: "当前没有账户转账记录，可点击下方按钮新建转账。")
                } else {
                    LazyVStack(spacing: 12) {
                        ForEach(viewModel.transfers) { transfer in
                            transferRow(transfer)
                        }
                    }
                }

                if actionPolicy.canWriteFinance {
                    PrimaryGlassButton(
                        title: viewModel.isLoading ? "刷新中..." : "新建转账",
                        systemImage: "arrow.left.arrow.right.circle.fill",
                        disabled: viewModel.isLoading || viewModel.availableAccounts.count < 2
                    ) {
                        viewModel.beginCreate()
                    }
                }
            }
            .padding(20)
        }
        .navigationTitle("账户转账")
        .task {
            await viewModel.load(using: env.apiClient)
        }
        .sheet(item: $viewModel.editor) { editor in
            AccountTransferEditorSheet(
                editor: editor,
                accounts: viewModel.availableAccounts,
                isSubmitting: viewModel.isEditorSubmitting,
                errorMessage: viewModel.editorErrorMessage
            ) {
                Task { await viewModel.submitEditor(client: env.apiClient) }
            }
        }
    }

    private var headerCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 6) {
                    Text("账户转账")
                        .font(ZhihuijiTheme.Typography.pageTitle)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                    Text("在资金账户之间进行转账，自动调整双方余额。")
                        .font(ZhihuijiTheme.Typography.body)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                }
                Spacer()
                StatusChip(
                    title: "共 \(viewModel.transfers.count) 笔",
                    tint: ZhihuijiTheme.ColorToken.primary
                )
            }

            HStack(spacing: 10) {
                summaryBadge(
                    title: "转账总金额",
                    value: viewModel.totalTransferAmount.currencyText,
                    tint: ZhihuijiTheme.ColorToken.warning
                )
                summaryBadge(
                    title: "已完成",
                    value: "\(viewModel.completedCount)",
                    tint: ZhihuijiTheme.ColorToken.success
                )
            }
        }
        .padding(18)
        .glassCard()
    }

    private func transferRow(_ transfer: AccountTransferRecord) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(transfer.transferNo)
                        .font(ZhihuijiTheme.Typography.bodyMedium)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                    HStack(spacing: 6) {
                        Text(transfer.fromAccountName)
                            .font(ZhihuijiTheme.Typography.caption)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                        Image(systemName: "arrow.right")
                            .font(ZhihuijiTheme.Typography.caption)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                        Text(transfer.toAccountName)
                            .font(ZhihuijiTheme.Typography.caption)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                    }
                }

                Spacer()

                VStack(alignment: .trailing, spacing: 4) {
                    Text(transfer.amount.currencyText)
                        .font(ZhihuijiTheme.Typography.bodyMedium)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.warning)
                    StatusChip(
                        title: transfer.statusLabel,
                        tint: transfer.statusTint
                    )
                }
            }

            HStack {
                if let fee = transfer.fee, fee > 0 {
                    Text("手续费 \(fee.currencyText)")
                        .font(ZhihuijiTheme.Typography.caption)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                }
                if let notes = transfer.notes?.nilIfBlank {
                    Text(notes)
                        .font(ZhihuijiTheme.Typography.caption)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                        .lineLimit(1)
                }
                Spacer()
                Text(transfer.createdAt.dateTimeText)
                    .font(ZhihuijiTheme.Typography.caption)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
            }
        }
        .padding(16)
        .glassCard()
    }

    private func summaryBadge(title: String, value: String, tint: Color) -> some View {
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

@MainActor
final class AccountTransferViewModel: ObservableObject {
    @Published var isLoading = false
    @Published var transfers: [AccountTransferRecord] = []
    @Published var accounts: [AccountRecord] = []
    @Published var errorMessage: String?
    @Published var editor: AccountTransferEditorState?
    @Published var isEditorSubmitting = false
    @Published var editorErrorMessage: String?

    var availableAccounts: [AccountRecord] {
        accounts.filter { $0.status == 1 }
    }

    var totalTransferAmount: Double {
        transfers.filter { $0.status == AccountTransferStatus.completed.rawValue }
            .reduce(0) { $0 + $1.amount }
    }

    var completedCount: Int {
        transfers.filter { $0.status == AccountTransferStatus.completed.rawValue }.count
    }

    func load(using client: APIClient) async {
        isLoading = true
        defer { isLoading = false }
        do {
            async let transfersTask = client.fetchAccountTransfers()
            async let accountsTask = client.fetchAccounts()
            transfers = try await transfersTask
            accounts = try await accountsTask
            errorMessage = nil
        } catch {
            transfers = []
            accounts = []
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    func beginCreate() {
        editor = AccountTransferEditorState(accounts: availableAccounts)
        editorErrorMessage = nil
    }

    func submitEditor(client: APIClient) async {
        guard let editor = editor else { return }
        guard let payload = editor.buildPayload() else {
            editorErrorMessage = editor.validationMessage ?? "请填写完整的转账信息。"
            return
        }

        isEditorSubmitting = true
        defer { isEditorSubmitting = false }

        do {
            let record = try await client.createAccountTransfer(payload: payload)
            transfers.insert(record, at: 0)
            // 刷新账户余额
            accounts = try await client.fetchAccounts()
            editorErrorMessage = nil
            self.editor = nil
        } catch {
            editorErrorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }
}

@MainActor
final class AccountTransferEditorState: ObservableObject, Identifiable {
    let id = UUID()
    let accounts: [AccountRecord]

    @Published var fromAccountId: EntityID?
    @Published var toAccountId: EntityID?
    @Published var amountText: String
    @Published var feeText: String
    @Published var notes: String

    init(accounts: [AccountRecord]) {
        self.accounts = accounts
        self.fromAccountId = accounts.first?.id
        self.toAccountId = accounts.count > 1 ? accounts[1].id : accounts.first?.id
        self.amountText = "0.00"
        self.feeText = "0.00"
        self.notes = ""
    }

    var validationMessage: String? {
        guard let fromId = fromAccountId, let toId = toAccountId else {
            return "请选择转出和转入账户。"
        }
        if fromId == toId {
            return "转出和转入账户不能相同。"
        }
        guard let amount = Double(amountText), amount > 0 else {
            return "转账金额必须大于 0。"
        }
        if let fee = Double(feeText), fee < 0 {
            return "手续费不能为负数。"
        }
        return nil
    }

    func buildPayload() -> AccountTransferCreatePayload? {
        guard validationMessage == nil else { return nil }
        guard let fromAccountId, let toAccountId,
              let amount = Double(amountText), amount > 0 else {
            return nil
        }
        let fee = Double(feeText) ?? 0
        return AccountTransferCreatePayload(
            fromAccountId: fromAccountId,
            toAccountId: toAccountId,
            amount: amount,
            fee: fee > 0 ? fee : nil,
            notes: notes.nilIfBlank
        )
    }
}

private struct AccountTransferEditorSheet: View {
    @ObservedObject var editor: AccountTransferEditorState
    let accounts: [AccountRecord]
    let isSubmitting: Bool
    let errorMessage: String?
    let onSave: () -> Void
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text("新建转账")
                        .font(ZhihuijiTheme.Typography.pageTitle)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)

                    if let errorMessage {
                        EmptyStateView(title: "转账失败", message: errorMessage)
                    }

                    VStack(alignment: .leading, spacing: 12) {
                        Text("转账信息")
                            .font(ZhihuijiTheme.Typography.sectionTitle)

                        Picker("转出账户", selection: $editor.fromAccountId) {
                            Text("请选择").tag(Optional<EntityID>.none)
                            ForEach(accounts) { account in
                                Text("\(account.name)（\(account.typeLabel)）").tag(Optional(account.id))
                            }
                        }
                        .pickerStyle(.menu)
                        .fieldBackground()

                        Picker("转入账户", selection: $editor.toAccountId) {
                            Text("请选择").tag(Optional<EntityID>.none)
                            ForEach(accounts) { account in
                                Text("\(account.name)（\(account.typeLabel)）").tag(Optional(account.id))
                            }
                        }
                        .pickerStyle(.menu)
                        .fieldBackground()

                        TextField("转账金额", text: $editor.amountText)
                            .fieldBackground()
                            .keyboardType(.decimalPad)

                        TextField("手续费（可选）", text: $editor.feeText)
                            .fieldBackground()
                            .keyboardType(.decimalPad)

                        TextField("备注（可选）", text: $editor.notes, axis: .vertical)
                            .lineLimit(2...4)
                            .fieldBackground()
                    }
                    .padding(16)
                    .glassCard()

                    if accounts.count < 2 {
                        EmptyStateView(
                            title: "可用账户不足",
                            message: "至少需要 2 个启用状态的账户才能进行转账。"
                        )
                    }

                    PrimaryGlassButton(
                        title: isSubmitting ? "提交中..." : "确认转账",
                        systemImage: "arrow.left.arrow.right.circle.fill",
                        disabled: isSubmitting || accounts.count < 2
                    ) {
                        onSave()
                    }
                }
                .padding(20)
            }
            .navigationTitle("新建转账")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("关闭") { dismiss() }
                }
            }
        }
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
    }
}
