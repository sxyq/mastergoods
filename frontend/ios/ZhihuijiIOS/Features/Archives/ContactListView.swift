import SwiftUI

struct ContactListView: View {
    @Environment(\.appEnvironment) private var env
    @EnvironmentObject private var session: AppSession
    let kind: PartnerContactKind
    let partnerId: EntityID
    @StateObject private var viewModel: ContactListViewModel

    private var actionPolicy: ArchivesHomeActionPolicy {
        ArchivesHomeActionPolicy.resolve(for: session.permissions)
    }

    init(kind: PartnerContactKind, partnerId: EntityID) {
        self.kind = kind
        self.partnerId = partnerId
        _viewModel = StateObject(wrappedValue: ContactListViewModel(kind: kind, partnerId: partnerId))
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                headerCard

                if let errorMessage = viewModel.errorMessage {
                    EmptyStateView(title: "联系人读取失败", message: errorMessage)
                } else if viewModel.contacts.isEmpty, !viewModel.isLoading {
                    EmptyStateView(title: "暂无联系人", message: "当前\(kind.label)还没有联系人记录，可点击下方按钮新增。")
                } else {
                    LazyVStack(spacing: 12) {
                        ForEach(viewModel.contacts) { contact in
                            contactRow(contact)
                        }
                    }
                }

                if actionPolicy.canEditPartner {
                    PrimaryGlassButton(
                        title: viewModel.isLoading ? "刷新中..." : "新增联系人",
                        systemImage: "person.badge.plus",
                        disabled: viewModel.isLoading
                    ) {
                        viewModel.beginCreate()
                    }
                }
            }
            .padding(20)
        }
        .navigationTitle("\(kind.label)联系人")
        .task {
            await viewModel.load(using: env.apiClient)
        }
        .sheet(item: $viewModel.editor) { editor in
            ContactEditorSheet(
                editor: editor,
                isSubmitting: viewModel.isEditorSubmitting,
                errorMessage: viewModel.editorErrorMessage
            ) {
                Task { await viewModel.submitEditor(client: env.apiClient) }
            } onDelete: { id in
                Task { await viewModel.deleteContact(id: id, client: env.apiClient) }
            }
        }
    }

    private var headerCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 6) {
                    Text("\(kind.label)联系人")
                        .font(ZhihuijiTheme.Typography.pageTitle)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                    Text("管理\(kind.label)的联系人信息，可设置主要联系人。")
                        .font(ZhihuijiTheme.Typography.body)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                }
                Spacer()
                StatusChip(
                    title: "共 \(viewModel.contacts.count) 位",
                    tint: kind == .customer ? ZhihuijiTheme.ColorToken.success : ZhihuijiTheme.ColorToken.warning
                )
            }

            HStack(spacing: 10) {
                summaryBadge(
                    title: "主要联系人",
                    value: "\(viewModel.primaryCount)",
                    tint: ZhihuijiTheme.ColorToken.primary
                )
                summaryBadge(
                    title: "有电话",
                    value: "\(viewModel.withPhoneCount)",
                    tint: ZhihuijiTheme.ColorToken.success
                )
            }
        }
        .padding(18)
        .glassCard()
    }

    private func contactRow(_ contact: ContactRecord) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .top) {
                Circle()
                    .fill((kind == .customer ? ZhihuijiTheme.ColorToken.success : ZhihuijiTheme.ColorToken.warning).opacity(0.14))
                    .frame(width: 40, height: 40)
                    .overlay(
                        Image(systemName: "person.fill")
                            .foregroundStyle(kind == .customer ? ZhihuijiTheme.ColorToken.success : ZhihuijiTheme.ColorToken.warning)
                    )

                VStack(alignment: .leading, spacing: 4) {
                    HStack(spacing: 6) {
                        Text(contact.name)
                            .font(ZhihuijiTheme.Typography.bodyMedium)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                        if contact.isPrimary {
                            StatusChip(title: "主要", tint: ZhihuijiTheme.ColorToken.primary)
                        }
                    }
                    if let title = contact.title?.nilIfBlank {
                        Text(title)
                            .font(ZhihuijiTheme.Typography.caption)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                    }
                }

                Spacer()

                if actionPolicy.canEditPartner {
                    Button {
                        viewModel.beginEdit(contact)
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

            if let phone = contact.phone?.nilIfBlank {
                HStack(spacing: 6) {
                    Image(systemName: "phone.fill")
                        .font(ZhihuijiTheme.Typography.caption)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                    Text(phone)
                        .font(ZhihuijiTheme.Typography.caption)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
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
final class ContactListViewModel: ObservableObject {
    let kind: PartnerContactKind
    let partnerId: EntityID

    @Published var isLoading = false
    @Published var contacts: [ContactRecord] = []
    @Published var errorMessage: String?
    @Published var editor: ContactEditorState?
    @Published var isEditorSubmitting = false
    @Published var editorErrorMessage: String?

    init(kind: PartnerContactKind, partnerId: EntityID) {
        self.kind = kind
        self.partnerId = partnerId
    }

    var primaryCount: Int {
        contacts.filter { $0.isPrimary }.count
    }

    var withPhoneCount: Int {
        contacts.filter { $0.phone?.nilIfBlank != nil }.count
    }

    func load(using client: APIClient) async {
        isLoading = true
        defer { isLoading = false }
        do {
            switch kind {
            case .customer:
                contacts = try await client.fetchCustomerContacts(customerId: partnerId)
            case .supplier:
                contacts = try await client.fetchSupplierContacts(supplierId: partnerId)
            }
            errorMessage = nil
        } catch {
            contacts = []
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    func beginCreate() {
        editor = ContactEditorState(kind: kind, partnerId: partnerId, mode: .create)
        editorErrorMessage = nil
    }

    func beginEdit(_ contact: ContactRecord) {
        editor = ContactEditorState(kind: kind, partnerId: partnerId, mode: .edit, contact: contact)
        editorErrorMessage = nil
    }

    func submitEditor(client: APIClient) async {
        guard let editor = editor else { return }
        guard let payload = editor.buildPayload() else {
            editorErrorMessage = "请填写联系人姓名。"
            return
        }

        isEditorSubmitting = true
        defer { isEditorSubmitting = false }

        do {
            let record: ContactRecord
            switch kind {
            case .customer:
                if let id = editor.recordId {
                    record = try await client.updateCustomerContact(id: id, payload: payload)
                } else {
                    record = try await client.createCustomerContact(payload: payload)
                }
            case .supplier:
                if let id = editor.recordId {
                    record = try await client.updateSupplierContact(id: id, payload: payload)
                } else {
                    record = try await client.createSupplierContact(payload: payload)
                }
            }
            upsert(record)
            editorErrorMessage = nil
            self.editor = nil
        } catch {
            editorErrorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    func deleteContact(id: EntityID, client: APIClient) async {
        do {
            switch kind {
            case .customer:
                try await client.deleteCustomerContact(id: id)
            case .supplier:
                try await client.deleteSupplierContact(id: id)
            }
            contacts.removeAll { $0.id == id }
            editor = nil
            editorErrorMessage = nil
        } catch {
            editorErrorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    private func upsert(_ contact: ContactRecord) {
        if let index = contacts.firstIndex(where: { $0.id == contact.id }) {
            contacts[index] = contact
        } else {
            contacts.insert(contact, at: 0)
        }
    }
}

@MainActor
final class ContactEditorState: ObservableObject, Identifiable {
    let id = UUID()
    let kind: PartnerContactKind
    let partnerId: EntityID
    let mode: ContactEditorMode
    let recordId: EntityID?

    @Published var name: String
    @Published var phone: String
    @Published var title: String
    @Published var isPrimary: Bool

    init(kind: PartnerContactKind, partnerId: EntityID, mode: ContactEditorMode) {
        self.kind = kind
        self.partnerId = partnerId
        self.mode = mode
        self.recordId = nil
        self.name = ""
        self.phone = ""
        self.title = ""
        self.isPrimary = false
    }

    init(kind: PartnerContactKind, partnerId: EntityID, mode: ContactEditorMode, contact: ContactRecord) {
        self.kind = kind
        self.partnerId = partnerId
        self.mode = mode
        self.recordId = contact.id
        self.name = contact.name
        self.phone = contact.phone ?? ""
        self.title = contact.title ?? ""
        self.isPrimary = contact.isPrimary
    }

    var titleText: String {
        switch (kind, mode) {
        case (.customer, .create): return "新建客户联系人"
        case (.customer, .edit): return "编辑客户联系人"
        case (.supplier, .create): return "新建供应商联系人"
        case (.supplier, .edit): return "编辑供应商联系人"
        }
    }

    var submitTitle: String {
        mode == .create ? "创建联系人" : "保存修改"
    }

    func buildPayload() -> ContactWritePayload? {
        guard let name = name.nilIfBlank else { return nil }
        return ContactWritePayload(
            partnerId: partnerId,
            name: name,
            phone: phone.nilIfBlank,
            title: title.nilIfBlank,
            isPrimary: isPrimary
        )
    }
}

enum ContactEditorMode {
    case create
    case edit
}

private struct ContactEditorSheet: View {
    @ObservedObject var editor: ContactEditorState
    let isSubmitting: Bool
    let errorMessage: String?
    let onSave: () -> Void
    let onDelete: (EntityID) -> Void
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text(editor.titleText)
                        .font(ZhihuijiTheme.Typography.pageTitle)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)

                    if let errorMessage {
                        EmptyStateView(title: "联系人保存失败", message: errorMessage)
                    }

                    VStack(alignment: .leading, spacing: 12) {
                        Text("联系人信息")
                            .font(ZhihuijiTheme.Typography.sectionTitle)
                        TextField("姓名", text: $editor.name)
                            .fieldBackground()
                        TextField("电话（可选）", text: $editor.phone)
                            .fieldBackground()
                            .keyboardType(.phonePad)
                        TextField("职务（可选）", text: $editor.title)
                            .fieldBackground()
                        Toggle("设为主要联系人", isOn: $editor.isPrimary)
                            .tint(ZhihuijiTheme.ColorToken.primary)
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
                                Text("删除此联系人")
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
            .navigationTitle(editor.titleText)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("关闭") { dismiss() }
                }
            }
        }
        .presentationDetents([.medium, .large])
        .presentationDragIndicator(.visible)
    }
}
