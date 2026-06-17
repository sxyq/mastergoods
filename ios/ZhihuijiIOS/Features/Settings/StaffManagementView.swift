import SwiftUI

struct StaffManagementView: View {
    @Environment(\.appEnvironment) private var env
    @StateObject private var viewModel = StaffManagementViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("店员与权限")
                    .font(.system(size: 28, weight: .bold))

                StaffCreateCard(
                    phone: $viewModel.createPhone,
                    nickname: $viewModel.createNickname,
                    password: $viewModel.createPassword,
                    role: $viewModel.createRole,
                    title: $viewModel.createTitle,
                    isSaving: viewModel.isSaving,
                    onSubmit: {
                        Task {
                            await viewModel.createMember(using: env.apiClient)
                        }
                    }
                )

                TextField("搜索手机号 / 昵称 / 岗位", text: $viewModel.keyword)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 14)
                    .background(Color.white.opacity(0.58), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.field, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.field, style: .continuous)
                            .stroke(Color.white.opacity(0.5), lineWidth: 0.5)
                    )

                if let errorMessage = viewModel.errorMessage {
                    EmptyStateView(title: "店员读取失败", message: errorMessage)
                } else if let successMessage = viewModel.successMessage {
                    EmptyStateView(title: "操作成功", message: successMessage)
                } else if viewModel.members.isEmpty, !viewModel.isLoading {
                    EmptyStateView(title: "暂无店员", message: "当前门店还没有可显示的成员数据。")
                } else {
                    LazyVStack(spacing: 12) {
                        ForEach(viewModel.filteredMembers) { member in
                            VStack(alignment: .leading, spacing: 8) {
                                HStack {
                                    VStack(alignment: .leading, spacing: 4) {
                                        Text(member.nickname)
                                            .font(.system(size: 16, weight: .semibold))
                                        Text(member.phone)
                                            .font(.system(size: 12))
                                            .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                                    }
                                    Spacer()
                                    StatusChip(
                                        title: member.status == 1 ? "启用" : "停用",
                                        tint: member.status == 1 ? ZhihuijiTheme.ColorToken.success : ZhihuijiTheme.ColorToken.warning
                                    )
                                }
                                Text("\(member.role.label) · \(member.title)")
                                    .font(.system(size: 13))
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                                Text("活跃会话 \(member.activeSessions)")
                                    .font(.system(size: 12, weight: .medium))
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                            }
                            .padding(16)
                            .background(Color.white.opacity(0.58), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous))
                            .overlay(
                                RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous)
                                    .stroke(Color.white.opacity(0.5), lineWidth: 0.5)
                            )
                        }
                    }
                }
            }
            .padding(20)
        }
        .task {
            await viewModel.load(using: env.apiClient)
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

    var filteredMembers: [StoreStaffMember] {
        let trimmed = keyword.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return members }
        return members.filter {
            $0.nickname.localizedCaseInsensitiveContains(trimmed)
                || $0.phone.localizedCaseInsensitiveContains(trimmed)
                || $0.title.localizedCaseInsensitiveContains(trimmed)
                || $0.role.label.localizedCaseInsensitiveContains(trimmed)
        }
    }

    func load(using client: APIClient) async {
        isLoading = true
        defer { isLoading = false }
        do {
            members = try await client.fetchStoreMembers()
            errorMessage = nil
        } catch {
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
}

private struct StaffCreateCard: View {
    @Binding var phone: String
    @Binding var nickname: String
    @Binding var password: String
    @Binding var role: StoreRole
    @Binding var title: String
    let isSaving: Bool
    let onSubmit: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("新建店员")
                .font(.system(size: 18, weight: .semibold))

            TextField("手机号", text: $phone)
                .fieldBackground()
            TextField("昵称", text: $nickname)
                .fieldBackground()
            SecureField("初始密码", text: $password)
                .fieldBackground()
            Picker("角色", selection: $role) {
                ForEach(StoreRole.allCases.filter { $0 != .owner }, id: \.self) { role in
                    Text(role.label).tag(role)
                }
            }
            .pickerStyle(.menu)
            .padding(.horizontal, 14)
            .padding(.vertical, 14)
            .background(Color.white.opacity(0.58), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.field, style: .continuous))

            TextField("岗位名称", text: $title)
                .fieldBackground()

            PrimaryGlassButton(title: isSaving ? "创建中..." : "创建店员", systemImage: "person.badge.plus", disabled: isSaving, action: onSubmit)
        }
        .padding(16)
        .background(Color.white.opacity(0.58), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous)
                .stroke(Color.white.opacity(0.5), lineWidth: 0.5)
        )
    }
}
