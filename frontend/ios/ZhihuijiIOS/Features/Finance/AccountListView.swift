import SwiftUI

struct AccountListView: View {
    @Environment(\.appEnvironment) private var env
    @EnvironmentObject private var session: AppSession
    @StateObject private var viewModel = AccountListViewModel()

    private var actionPolicy: FinanceRecordActionPolicy {
        FinanceRecordActionPolicy.resolve(for: session.permissions)
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                headerCard

                if let errorMessage = viewModel.errorMessage {
                    EmptyStateView(title: "账户读取失败", message: errorMessage)
                } else if viewModel.accounts.isEmpty, !viewModel.isLoading {
                    EmptyStateView(title: "暂无账户", message: "当前没有资金账户，可点击下方按钮新增。")
                } else {
                    LazyVStack(spacing: 12) {
                        ForEach(viewModel.accounts) { account in
                            accountRow(account)
                        }
                    }
                }

                if actionPolicy.canWriteFinance {
                    PrimaryGlassButton(
                        title: viewModel.isLoading ? "刷新中..." : "新增账户",
                        systemImage: "plus.circle.fill",
                        disabled: viewModel.isLoading
                    ) {
                        viewModel.beginCreate()
                    }
                }
            }
            .padding(20)
        }
        .navigationTitle("资金账户")
        .task {
            await viewModel.load(using: env.apiClient)
        }
        .sheet(item: $viewModel.editor) { editor in
            AccountEditorSheet(
                editor: editor,
                isSubmitting: viewModel.isEditorSubmitting,
                errorMessage: viewModel.editorErrorMessage
            ) {
                Task { await viewModel.submitEditor(client: env.apiClient) }
            } onDelete: { id in
                Task { await viewModel.deleteAccount(id: id, client: env.apiClient) }
            }
        }
    }

    private var headerCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 6) {
                    Text("资金账户")
                        .font(ZhihuijiTheme.Typography.pageTitle)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                    Text("管理现金、银行、支付宝、微信等收款账户与余额。")
                        .font(ZhihuijiTheme.Typography.body)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                }
                Spacer()
                StatusChip(
                    title: "共 \(viewModel.accounts.count) 个",
                    tint: ZhihuijiTheme.ColorToken.primary
                )
            }

            HStack(spacing: 10) {
                summaryBadge(
                    title: "账户总余额",
                    value: viewModel.totalBalance.currencyText,
                    tint: ZhihuijiTheme.ColorToken.warning
                )
                summaryBadge(
                    title: "启用账户",
                    value: "\(viewModel.activeCount)",
                    tint: ZhihuijiTheme.ColorToken.success
                )
                summaryBadge(
                    title: "默认账户",
                    value: "\(viewModel.defaultCount)",
                    tint: ZhihuijiTheme.ColorToken.primary
                )
            }
        }
        .padding(18)
        .glassCard()
    }

    private func accountRow(_ account: AccountRecord) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .top) {
                Circle()
                    .fill(account.typeTint.opacity(0.14))
                    .frame(width: 40, height: 40)
                    .overlay(
                        Image(systemName: account.typeSystemImage)
                            .foregroundStyle(account.typeTint)
                    )

                VStack(alignment: .leading, spacing: 4) {
                    HStack(spacing: 6) {
                        Text(account.name)
                            .font(ZhihuijiTheme.Typography.bodyMedium)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                        if account.isDefault {
                            StatusChip(title: "默认", tint: ZhihuijiTheme.ColorToken.primary)
                        }
                    }
                    Text(account.code)
                        .font(ZhihuijiTheme.Typography.caption)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                }

                Spacer()

                VStack(alignment: .trailing, spacing: 4) {
                    Text(account.balance.currencyText)
                        .font(ZhihuijiTheme.Typography.bodyMedium)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.warning)
                    StatusChip(
                        title: account.statusLabel,
                        tint: account.statusTint
                    )
                }
            }

            HStack(spacing: 10) {
                Text(account.typeLabel)
                    .font(ZhihuijiTheme.Typography.captionSemibold)
                    .foregroundStyle(account.typeTint)
                if let notes = account.notes?.nilIfBlank {
                    Text(notes)
                        .font(ZhihuijiTheme.Typography.caption)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                        .lineLimit(1)
                }
                Spacer()
                if actionPolicy.canWriteFinance {
                    Button {
                        viewModel.beginEdit(account)
                    } label: {
                        HStack(spacing: 4) {
                            Image(systemName: "pencil.circle.fill")
                            Text("编辑")
                                .font(ZhihuijiTheme.Typography.captionSemibold)
                        }
                        .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                    }
                    .buttonStyle(.plain)
                }
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
final class AccountListViewModel: ObservableObject {
    @Published var isLoading = false
    @Published var accounts: [AccountRecord] = []
    @Published var errorMessage: String?
    @Published var editor: AccountEditorState?
    @Published var isEditorSubmitting = false
    @Published var editorErrorMessage: String?

    var totalBalance: Double {
        accounts.reduce(0) { $0 + $1.balance }
    }

    var activeCount: Int {
        accounts.filter { $0.status == 1 }.count
    }

    var defaultCount: Int {
        accounts.filter { $0.isDefault }.count
    }

    func load(using client: APIClient) async {
        isLoading = true
        defer { isLoading = false }
        do {
            accounts = try await client.fetchAccounts()
            errorMessage = nil
        } catch {
            accounts = []
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    func beginCreate() {
        editor = AccountEditorState(mode: .create)
        editorErrorMessage = nil
    }

    func beginEdit(_ account: AccountRecord) {
        editor = AccountEditorState(mode: .edit, account: account)
        editorErrorMessage = nil
    }

    func submitEditor(client: APIClient) async {
        guard let editor = editor else { return }
        guard let payload = editor.buildPayload() else {
            editorErrorMessage = "请填写账户编码、名称并选择账户类型。"
            return
        }

        isEditorSubmitting = true
        defer { isEditorSubmitting = false }

        do {
            let record: AccountRecord
            if let id = editor.recordId {
                record = try await client.updateAccount(id: id, payload: payload)
            } else {
                record = try await client.createAccount(payload: payload)
            }
            upsert(record)
            editorErrorMessage = nil
            self.editor = nil
        } catch {
            editorErrorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    func deleteAccount(id: EntityID, client: APIClient) async {
        do {
            try await client.deleteAccount(id: id)
            accounts.removeAll { $0.id == id }
            editor = nil
            editorErrorMessage = nil
        } catch {
            editorErrorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    private func upsert(_ account: AccountRecord) {
        if let index = accounts.firstIndex(where: { $0.id == account.id }) {
            accounts[index] = account
        } else {
            accounts.insert(account, at: 0)
        }
    }
}

@MainActor
final class AccountEditorState: ObservableObject, Identifiable {
    let id = UUID()
    let mode: AccountEditorMode
    let recordId: EntityID?

    @Published var code: String
    @Published var name: String
    @Published var type: Int
    @Published var balanceText: String
    @Published var isDefault: Bool
    @Published var status: Int
    @Published var sortOrderText: String
    @Published var notes: String

    init(mode: AccountEditorMode) {
        self.mode = mode
        self.recordId = nil
        self.code = ""
        self.name = ""
        self.type = AccountType.cash.rawValue
        self.balanceText = "0.00"
        self.isDefault = false
        self.status = 1
        self.sortOrderText = ""
        self.notes = ""
    }

    init(mode: AccountEditorMode, account: AccountRecord) {
        self.mode = mode
        self.recordId = account.id
        self.code = account.code
        self.name = account.name
        self.type = account.type
        self.balanceText = String(format: "%.2f", account.balance)
        self.isDefault = account.isDefault
        self.status = account.status
        self.sortOrderText = account.sortOrder.map(String.init) ?? ""
        self.notes = account.notes ?? ""
    }

    var title: String {
        mode == .create ? "新建账户" : "编辑账户"
    }

    var submitTitle: String {
        mode == .create ? "创建账户" : "保存修改"
    }

    func buildPayload() -> AccountWritePayload? {
        guard let code = code.nilIfBlank,
              let name = name.nilIfBlank,
              let balance = Double(balanceText) else {
            return nil
        }
        return AccountWritePayload(
            code: code,
            name: name,
            type: type,
            balance: balance,
            isDefault: isDefault,
            status: status,
            sortOrder: sortOrderText.nilIfBlank.flatMap(Int.init),
            notes: notes.nilIfBlank
        )
    }
}

enum AccountEditorMode {
    case create
    case edit
}

private struct AccountEditorSheet: View {
    @ObservedObject var editor: AccountEditorState
    let isSubmitting: Bool
    let errorMessage: String?
    let onSave: () -> Void
    let onDelete: (EntityID) -> Void
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text(editor.title)
                        .font(ZhihuijiTheme.Typography.pageTitle)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)

                    if let errorMessage {
                        EmptyStateView(title: "账户保存失败", message: errorMessage)
                    }

                    VStack(alignment: .leading, spacing: 12) {
                        Text("基础信息")
                            .font(ZhihuijiTheme.Typography.sectionTitle)
                        TextField("账户编码", text: $editor.code)
                            .fieldBackground()
                        TextField("账户名称", text: $editor.name)
                            .fieldBackground()

                        Picker("账户类型", selection: $editor.type) {
                            ForEach(AccountType.allCases) { accountType in
                                Text(accountType.label).tag(accountType.rawValue)
                            }
                        }
                        .pickerStyle(.segmented)

                        TextField("当前余额", text: $editor.balanceText)
                            .fieldBackground()
                            .keyboardType(.decimalPad)

                        TextField("排序（可选）", text: $editor.sortOrderText)
                            .fieldBackground()
                            .keyboardType(.numberPad)

                        Picker("状态", selection: $editor.status) {
                            Text("停用").tag(0)
                            Text("启用").tag(1)
                        }
                        .pickerStyle(.segmented)

                        Toggle("设为默认账户", isOn: $editor.isDefault)
                            .tint(ZhihuijiTheme.ColorToken.primary)
                    }
                    .padding(16)
                    .glassCard()

                    VStack(alignment: .leading, spacing: 12) {
                        Text("备注")
                            .font(ZhihuijiTheme.Typography.sectionTitle)
                        TextField("账户备注（可选）", text: $editor.notes, axis: .vertical)
                            .lineLimit(2...4)
                            .fieldBackground()
                    }
                    .padding(16)
                    .glassCard()

                    PrimaryGlassButton(
                        title: isSubmitting ? "保存中..." : editor.submitTitle,
                        systemImage: "square.and.arrow.down.fill",
                        disabled: isSubmitting,
                        action: onSave
                    )

                    if editor.mode == .edit, let recordId = editor.recordId {
                        Button(role: .destructive) {
                            onDelete(recordId)
                        } label: {
                            HStack {
                                Image(systemName: "trash")
                                Text("删除此账户")
                                    .font(ZhihuijiTheme.Typography.bodyMedium)
                            }
                            .foregroundStyle(ZhihuijiTheme.ColorToken.danger)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(
                                ZhihuijiTheme.ColorToken.danger.opacity(0.10),
                                in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.pill, style: .continuous)
                            )
                        }
                        .buttonStyle(.plain)
                        .disabled(isSubmitting)
                    }
                }
                .padding(20)
            }
            .navigationTitle(editor.title)
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
