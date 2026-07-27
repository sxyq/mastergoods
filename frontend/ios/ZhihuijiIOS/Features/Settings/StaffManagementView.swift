import SwiftUI

struct StaffManagementView: View {
    @Environment(\.appEnvironment) private var env
    @EnvironmentObject private var session: AppSession
    @StateObject private var viewModel = StaffManagementViewModel()

    private var actionPolicy: StaffManagementActionPolicy {
        StaffManagementActionPolicy.resolve(for: session.permissions)
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                headerCard

                StaffCreateCard(
                    phone: $viewModel.createPhone,
                    nickname: $viewModel.createNickname,
                    password: $viewModel.createPassword,
                    role: $viewModel.createRole,
                    title: $viewModel.createTitle,
                    isSaving: viewModel.isSaving,
                    canManage: actionPolicy.canManageStaff,
                    onSubmit: {
                        Task { await viewModel.createMember(using: env.apiClient) }
                    }
                )

                filterSection

                if let errorMessage = viewModel.errorMessage {
                    EmptyStateView(title: "店员读取失败", message: errorMessage)
                } else if viewModel.members.isEmpty, !viewModel.isLoading {
                    EmptyStateView(title: "暂无店员", message: "当前门店还没有可显示的成员数据。")
                } else {
                    if let successMessage = viewModel.successMessage {
                        successBanner(successMessage)
                    }

                    LazyVStack(spacing: 12) {
                        ForEach(viewModel.filteredMembers) { member in
                            memberCard(member)
                        }
                    }
                }
            }
            .padding(20)
        }
        .navigationTitle("店员与权限")
        .task {
            await viewModel.load(using: env.apiClient)
        }
        .sheet(item: $viewModel.editingMember) { member in
            NavigationStack {
                StaffEditSheet(
                    viewModel: viewModel,
                    member: member,
                    canManage: actionPolicy.canManageStaff,
                    onSave: {
                        Task { await viewModel.saveEditingMember(using: env.apiClient) }
                    }
                )
                .navigationTitle("编辑店员")
                .toolbar {
                    ToolbarItem {
                        Button("关闭") {
                            viewModel.cancelEditing()
                        }
                    }
                }
            }
            .presentationDetents([.large])
            .presentationDragIndicator(.visible)
        }
    }

    private var headerCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("店员与权限")
                .font(ZhihuijiTheme.Typography.pageTitle)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
            Text("门店端员工账号、角色、状态和会话策略都在这里集中管理。")
                .font(ZhihuijiTheme.Typography.body)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)

            HStack(spacing: 10) {
                statChip(title: "总人数 \(viewModel.members.count)", tint: ZhihuijiTheme.ColorToken.primary)
                statChip(title: "启用 \(viewModel.enabledCount)", tint: ZhihuijiTheme.ColorToken.success)
                statChip(title: "停用 \(viewModel.disabledCount)", tint: ZhihuijiTheme.ColorToken.warning)
            }

            HStack(spacing: 10) {
                statChip(title: "活跃会话 \(viewModel.totalActiveSessions)", tint: ZhihuijiTheme.ColorToken.primaryBright)
                statChip(title: actionPolicy.canManageStaff ? "可管理员工" : "仅可查看", tint: actionPolicy.canManageStaff ? ZhihuijiTheme.ColorToken.success : ZhihuijiTheme.ColorToken.warning)
            }
        }
        .padding(18)
        .glassCard()
    }

    private var filterSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            TextField("搜索手机号 / 昵称 / 岗位", text: $viewModel.keyword)
                .fieldBackground()

            HStack(spacing: 10) {
                ForEach(StaffStatusFilter.allCases) { filter in
                    Button {
                        viewModel.statusFilter = filter
                    } label: {
                        Text(filter.title)
                            .font(ZhihuijiTheme.Typography.captionSemibold)
                            .foregroundStyle(viewModel.statusFilter == filter ? .white : ZhihuijiTheme.ColorToken.textSecondary)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 9)
                            .background(
                                (viewModel.statusFilter == filter
                                    ? LinearGradient(colors: [ZhihuijiTheme.ColorToken.primaryBright, ZhihuijiTheme.ColorToken.primary], startPoint: .leading, endPoint: .trailing)
                                    : LinearGradient(colors: [Color.white.opacity(0.54), Color.white.opacity(0.54)], startPoint: .leading, endPoint: .trailing)
                                ),
                                in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.pill, style: .continuous)
                            )
                    }
                    .buttonStyle(.plain)
                }
                Spacer()
            }
        }
    }

    private func successBanner(_ text: String) -> some View {
        HStack(spacing: 10) {
            Image(systemName: "checkmark.circle.fill")
                .foregroundStyle(ZhihuijiTheme.ColorToken.success)
            Text(text)
                .font(ZhihuijiTheme.Typography.bodyMedium)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
            Spacer()
        }
        .padding(14)
        .background(
            ZhihuijiTheme.ColorToken.success.opacity(0.10),
            in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.cardSmall, style: .continuous)
        )
    }

    private func memberCard(_ member: StoreStaffMember) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(member.nickname)
                        .font(ZhihuijiTheme.Typography.cardTitle)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                    Text(member.phone)
                        .font(ZhihuijiTheme.Typography.caption)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 6) {
                    StatusChip(
                        title: member.status == 1 ? "启用" : "停用",
                        tint: member.status == 1 ? ZhihuijiTheme.ColorToken.success : ZhihuijiTheme.ColorToken.warning
                    )
                    StatusChip(title: member.role.label, tint: tintForRole(member.role))
                }
            }

            Text("\(member.title) · 活跃会话 \(member.activeSessions)")
                .font(ZhihuijiTheme.Typography.body)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)

            if !member.permissions.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(member.permissions.sorted { $0.displayName < $1.displayName }.prefix(6), id: \.self) { permission in
                            Text(permission.displayName)
                                .font(ZhihuijiTheme.Typography.caption)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                                .padding(.horizontal, 10)
                                .padding(.vertical, 7)
                                .background(
                                    Color.white.opacity(0.42),
                                    in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.pill, style: .continuous)
                                )
                        }
                    }
                }
            }

            HStack(spacing: 10) {
                SecondaryActionButton(
                    title: "编辑",
                    systemImage: "slider.horizontal.3",
                    tint: ZhihuijiTheme.ColorToken.primary
                ) {
                    viewModel.beginEditing(member)
                }
                .disabled(!actionPolicy.canManageStaff || viewModel.isSaving)
                .opacity((!actionPolicy.canManageStaff || viewModel.isSaving) ? 0.6 : 1)

                if member.role == .owner {
                    SecondaryActionButton(
                        title: "店长账号",
                        systemImage: "crown.fill",
                        tint: ZhihuijiTheme.ColorToken.warning,
                        disabled: true
                    ) {}
                } else {
                    SecondaryActionButton(
                        title: member.status == 1 ? "停用" : "启用",
                        systemImage: member.status == 1 ? "pause.circle.fill" : "play.circle.fill",
                        tint: member.status == 1 ? ZhihuijiTheme.ColorToken.warning : ZhihuijiTheme.ColorToken.success
                    ) {
                        Task { await viewModel.quickToggleMember(member, using: env.apiClient) }
                    }
                    .disabled(!actionPolicy.canManageStaff || viewModel.isSaving)
                    .opacity((!actionPolicy.canManageStaff || viewModel.isSaving) ? 0.6 : 1)
                }

                if member.activeSessions > 0, member.role != .owner {
                    SecondaryActionButton(
                        title: "强制下线",
                        systemImage: "rectangle.portrait.and.arrow.right",
                        tint: ZhihuijiTheme.ColorToken.danger
                    ) {
                        Task { await viewModel.revokeSessions(for: member, using: env.apiClient) }
                    }
                    .disabled(!actionPolicy.canManageStaff || viewModel.isSaving)
                    .opacity((!actionPolicy.canManageStaff || viewModel.isSaving) ? 0.6 : 1)
                }
            }
        }
        .padding(16)
        .glassCard()
    }

    private func statChip(title: String, tint: Color) -> some View {
        Text(title)
            .font(ZhihuijiTheme.Typography.captionSemibold)
            .foregroundStyle(tint)
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(
                tint.opacity(0.12),
                in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.pill, style: .continuous)
            )
            .overlay(
                RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.pill, style: .continuous)
                    .stroke(Color.white.opacity(0.45), lineWidth: ZhihuijiTheme.Stroke.hairline)
            )
    }

    private func tintForRole(_ role: StoreRole) -> Color {
        switch role {
        case .owner:
            return ZhihuijiTheme.ColorToken.warning
        case .manager:
            return ZhihuijiTheme.ColorToken.primaryBright
        case .finance:
            return ZhihuijiTheme.ColorToken.success
        case .assistant:
            return ZhihuijiTheme.ColorToken.textTertiary
        default:
            return ZhihuijiTheme.ColorToken.primary
        }
    }
}

@MainActor
final class StaffManagementViewModel: ObservableObject {
    @Published var keyword = ""
    @Published var isLoading = false
    @Published var isSaving = false
    @Published var members: [StoreStaffMember] = []
    @Published var errorMessage: String?
    @Published var successMessage: String?
    @Published var createPhone = ""
    @Published var createNickname = ""
    @Published var createPassword = ""
    @Published var createRole: StoreRole = .sales
    @Published var createTitle = "销售员工"
    @Published var statusFilter: StaffStatusFilter = .all
    @Published var editingMember: StoreStaffMember?
    @Published var editNickname = ""
    @Published var editPassword = ""
    @Published var editRole: StoreRole = .sales
    @Published var editTitle = ""
    @Published var editStatus = 1
    @Published var editKeepSessions = true

    var filteredMembers: [StoreStaffMember] {
        let trimmed = keyword.trimmingCharacters(in: .whitespacesAndNewlines)
        let scoped = members.filter { member in
            switch statusFilter {
            case .all:
                return true
            case .enabled:
                return member.status == 1
            case .disabled:
                return member.status != 1
            }
        }
        guard !trimmed.isEmpty else { return scoped }
        return scoped.filter {
            $0.nickname.localizedCaseInsensitiveContains(trimmed)
                || $0.phone.localizedCaseInsensitiveContains(trimmed)
                || $0.title.localizedCaseInsensitiveContains(trimmed)
                || $0.role.label.localizedCaseInsensitiveContains(trimmed)
        }
    }

    var enabledCount: Int {
        members.filter { $0.status == 1 }.count
    }

    var disabledCount: Int {
        members.filter { $0.status != 1 }.count
    }

    var totalActiveSessions: Int {
        members.reduce(0) { $0 + $1.activeSessions }
    }

    func load(using client: APIClient) async {
        isLoading = true
        defer { isLoading = false }
        do {
            members = try await client.fetchStoreMembers()
            errorMessage = nil
        } catch {
            members = []
            editingMember = nil
            successMessage = nil
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    func createMember(using client: APIClient) async {
        let phone = createPhone.trimmingCharacters(in: .whitespacesAndNewlines)
        let nickname = createNickname.trimmingCharacters(in: .whitespacesAndNewlines)
        let password = createPassword.trimmingCharacters(in: .whitespacesAndNewlines)
        if phone.isEmpty || nickname.isEmpty || password.isEmpty {
            errorMessage = "手机号、昵称和初始密码都需要填写"
            successMessage = nil
            return
        }

        isSaving = true
        defer { isSaving = false }
        do {
            let created = try await client.createStoreMember(
                StoreMemberCreatePayload(
                    phone: phone,
                    password: password,
                    nickname: nickname,
                    role: createRole,
                    title: createTitle.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? createRole.label : createTitle,
                    status: 1
                )
            )
            createPhone = ""
            createNickname = ""
            createPassword = ""
            createRole = .sales
            createTitle = "销售员工"
            await load(using: client)
            successMessage = "已创建店员 \(created.nickname)"
            errorMessage = nil
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
            successMessage = nil
        }
    }

    func beginEditing(_ member: StoreStaffMember) {
        editingMember = member
        editNickname = member.nickname
        editPassword = ""
        editRole = member.role
        editTitle = member.title
        editStatus = member.status
        editKeepSessions = true
        errorMessage = nil
        successMessage = nil
    }

    func cancelEditing() {
        editingMember = nil
        editNickname = ""
        editPassword = ""
        editRole = .sales
        editTitle = ""
        editStatus = 1
        editKeepSessions = true
    }

    func saveEditingMember(using client: APIClient) async {
        guard let member = editingMember else { return }
        let nickname = editNickname.trimmingCharacters(in: .whitespacesAndNewlines)
        let title = editTitle.trimmingCharacters(in: .whitespacesAndNewlines)
        if nickname.isEmpty || title.isEmpty {
            errorMessage = "昵称和岗位名称不能为空"
            return
        }

        isSaving = true
        defer { isSaving = false }
        do {
            let updated = try await client.updateStoreMember(
                userId: member.userId,
                payload: StoreMemberUpdatePayload(
                    nickname: nickname,
                    password: editPassword.nilIfBlank,
                    role: member.role == .owner ? nil : editRole,
                    title: title,
                    status: member.role == .owner ? nil : editStatus,
                    keepSessions: editKeepSessions
                )
            )
            applyUpdatedMember(updated)
            successMessage = "已更新 \(updated.nickname)"
            errorMessage = nil
            cancelEditing()
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
            successMessage = nil
        }
    }

    func quickToggleMember(_ member: StoreStaffMember, using client: APIClient) async {
        guard member.role != .owner else { return }
        isSaving = true
        defer { isSaving = false }
        do {
            let updated = try await client.updateStoreMember(
                userId: member.userId,
                payload: StoreMemberUpdatePayload(
                    nickname: member.nickname,
                    password: nil,
                    role: member.role,
                    title: member.title,
                    status: member.status == 1 ? 0 : 1,
                    keepSessions: false
                )
            )
            applyUpdatedMember(updated)
            successMessage = updated.status == 1 ? "已启用 \(updated.nickname)" : "已停用 \(updated.nickname)"
            errorMessage = nil
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
            successMessage = nil
        }
    }

    func revokeSessions(for member: StoreStaffMember, using client: APIClient) async {
        guard member.role != .owner else { return }
        isSaving = true
        defer { isSaving = false }
        do {
            let updated = try await client.updateStoreMember(
                userId: member.userId,
                payload: StoreMemberUpdatePayload(
                    nickname: member.nickname,
                    password: nil,
                    role: member.role,
                    title: member.title,
                    status: member.status,
                    keepSessions: false
                )
            )
            applyUpdatedMember(updated)
            successMessage = "已让 \(updated.nickname) 重新登录"
            errorMessage = nil
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
            successMessage = nil
        }
    }

    private func applyUpdatedMember(_ member: StoreStaffMember) {
        if let index = members.firstIndex(where: { $0.userId == member.userId }) {
            members[index] = member
        } else {
            members.insert(member, at: 0)
        }
    }
}

private struct StaffCreateCard: View {
    @Binding var phone: String
    @Binding var nickname: String
    @Binding var password: String
    @Binding var role: StoreRole
    @Binding var title: String
    let isSaving: Bool
    let canManage: Bool
    let onSubmit: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("新建店员")
                .font(ZhihuijiTheme.Typography.sectionTitle)

            TextField("手机号", text: $phone)
                .fieldBackground()
            TextField("昵称", text: $nickname)
                .fieldBackground()
            SecureField("初始密码", text: $password)
                .fieldBackground()

            rolePicker(selection: $role, includesOwner: false)

            TextField("岗位名称", text: $title)
                .fieldBackground()

            PrimaryGlassButton(
                title: isSaving ? "创建中..." : "创建店员",
                systemImage: "person.badge.plus",
                disabled: isSaving || !canManage,
                action: onSubmit
            )
        }
        .padding(16)
        .glassCard()
    }
}

private struct StaffEditSheet: View {
    @ObservedObject var viewModel: StaffManagementViewModel
    let member: StoreStaffMember
    let canManage: Bool
    let onSave: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                VStack(alignment: .leading, spacing: 8) {
                    Text(member.phone)
                        .font(ZhihuijiTheme.Typography.caption)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                    HStack(spacing: 10) {
                        StatusChip(
                            title: member.status == 1 ? "当前启用" : "当前停用",
                            tint: member.status == 1 ? ZhihuijiTheme.ColorToken.success : ZhihuijiTheme.ColorToken.warning
                        )
                        StatusChip(title: member.role.label, tint: ZhihuijiTheme.ColorToken.primary)
                    }
                }
                .padding(16)
                .glassCard()

                VStack(alignment: .leading, spacing: 12) {
                    Text("基础信息")
                        .font(ZhihuijiTheme.Typography.sectionTitle)
                    TextField("昵称", text: $viewModel.editNickname)
                        .fieldBackground()
                    TextField("岗位名称", text: $viewModel.editTitle)
                        .fieldBackground()
                    SecureField("新密码（留空则不修改）", text: $viewModel.editPassword)
                        .fieldBackground()
                }
                .padding(16)
                .glassCard()

                VStack(alignment: .leading, spacing: 12) {
                    Text("角色与状态")
                        .font(ZhihuijiTheme.Typography.sectionTitle)
                    rolePicker(selection: $viewModel.editRole, includesOwner: false)
                        .disabled(member.role == .owner)
                        .opacity(member.role == .owner ? 0.55 : 1)

                    Picker("账号状态", selection: $viewModel.editStatus) {
                        Text("启用").tag(1)
                        Text("停用").tag(0)
                    }
                    .pickerStyle(.segmented)
                    .disabled(member.role == .owner)
                    .opacity(member.role == .owner ? 0.55 : 1)

                    Toggle(isOn: $viewModel.editKeepSessions) {
                        VStack(alignment: .leading, spacing: 3) {
                            Text("保留当前会话")
                                .font(ZhihuijiTheme.Typography.bodyMedium)
                            Text("关闭后会让该账号重新登录，适合角色或密码变更后立即生效。")
                                .font(ZhihuijiTheme.Typography.caption)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                        }
                    }
                    .tint(ZhihuijiTheme.ColorToken.primary)
                }
                .padding(16)
                .glassCard()

                PrimaryGlassButton(
                    title: viewModel.isSaving ? "保存中..." : "保存修改",
                    systemImage: "square.and.arrow.down.fill",
                    disabled: viewModel.isSaving || !canManage,
                    action: onSave
                )
            }
            .padding(20)
        }
    }
}

enum StaffStatusFilter: String, CaseIterable, Identifiable {
    case all
    case enabled
    case disabled

    var id: String { rawValue }

    var title: String {
        switch self {
        case .all: return "全部"
        case .enabled: return "启用"
        case .disabled: return "停用"
        }
    }
}

private struct SecondaryActionButton: View {
    let title: String
    let systemImage: String
    let tint: Color
    var disabled = false
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                Image(systemName: systemImage)
                Text(title)
                    .font(ZhihuijiTheme.Typography.captionSemibold)
            }
            .foregroundStyle(disabled ? ZhihuijiTheme.ColorToken.textTertiary : tint)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 11)
            .background(
                (disabled ? Color.white.opacity(0.38) : tint.opacity(0.10)),
                in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.cardSmall, style: .continuous)
            )
            .overlay(
                RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.cardSmall, style: .continuous)
                    .stroke(Color.white.opacity(0.45), lineWidth: ZhihuijiTheme.Stroke.hairline)
            )
        }
        .buttonStyle(.plain)
        .disabled(disabled)
        .opacity(disabled ? 0.7 : 1)
    }
}

private func rolePicker(selection: Binding<StoreRole>, includesOwner: Bool) -> some View {
    Picker("角色", selection: selection) {
        ForEach(StoreRole.allCases.filter { includesOwner || $0 != .owner }, id: \.self) { role in
            Text(role.label).tag(role)
        }
    }
    .pickerStyle(.menu)
    .padding(.horizontal, 14)
    .padding(.vertical, 14)
    .background(Color.white.opacity(0.58), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.field, style: .continuous))
    .overlay(
        RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.field, style: .continuous)
            .stroke(Color.white.opacity(0.5), lineWidth: ZhihuijiTheme.Stroke.hairline)
    )
}
