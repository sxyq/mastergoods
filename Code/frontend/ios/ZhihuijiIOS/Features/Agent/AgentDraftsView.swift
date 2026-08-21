import SwiftUI

struct AgentDraftsView: View {
    @Environment(\.appEnvironment) private var env
    @EnvironmentObject private var session: AppSession
    @StateObject private var viewModel = AgentDraftsViewModel()

    private var canWrite: Bool {
        AgentAccessPolicy.resolve(for: session.permissions).canWriteAgent
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                headerSection
                summarySection
                if let errorMessage = viewModel.errorMessage {
                    EmptyStateView(title: "草稿加载失败", message: errorMessage)
                }
                draftsSection
            }
            .padding(20)
        }
        .navigationTitle("AI 草稿")
        .task {
            await viewModel.load(using: env.apiClient)
        }
        .sheet(isPresented: $viewModel.isEditorPresented) {
            NavigationStack {
                AgentDraftEditorSheet(
                    title: $viewModel.editorTitle,
                    content: $viewModel.editorContent,
                    status: $viewModel.editorStatus,
                    draftType: $viewModel.editorDraftType,
                    isSaving: viewModel.isSaving,
                    mode: viewModel.editorMode,
                    canWrite: canWrite,
                    onSave: {
                        Task { await viewModel.save(using: env.apiClient) }
                    },
                    onDelete: {
                        Task { await viewModel.delete(using: env.apiClient) }
                    }
                )
                .navigationTitle(viewModel.editorMode.title)
                .toolbar {
                    ToolbarItem {
                        Button("关闭") {
                            viewModel.dismissEditor()
                        }
                    }
                }
            }
            .presentationDetents([.medium, .large])
            .presentationDragIndicator(.visible)
        }
    }

    private var headerSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("AI 草稿")
                        .font(ZhihuijiTheme.Typography.pageTitle)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                    Text(canWrite ? "集中管理 AI 生成的草稿，可创建、编辑、归档与删除。" : "当前账号可查看 AI 草稿，需要 agent:write 才能创建、编辑或删除。")
                        .font(ZhihuijiTheme.Typography.body)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                }
                Spacer()
                PrimaryGlassButton(
                    title: viewModel.isLoading ? "刷新中..." : "刷新",
                    systemImage: "arrow.clockwise",
                    disabled: viewModel.isLoading
                ) {
                    Task { await viewModel.load(using: env.apiClient) }
                }
                .frame(maxWidth: 160)
            }
        }
        .padding(16)
        .glassCard()
    }

    private var summarySection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("草稿概览")
                .font(ZhihuijiTheme.Typography.sectionTitle)

            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                MetricCard(
                    title: "草稿总数",
                    value: "\(viewModel.drafts.count)",
                    subtitle: "已加载",
                    tint: ZhihuijiTheme.ColorToken.primary
                )
                MetricCard(
                    title: "进行中",
                    value: "\(viewModel.activeCount)",
                    subtitle: "未归档",
                    tint: ZhihuijiTheme.ColorToken.success
                )
                MetricCard(
                    title: "已归档",
                    value: "\(viewModel.archivedCount)",
                    subtitle: "历史草稿",
                    tint: ZhihuijiTheme.ColorToken.warning
                )
                MetricCard(
                    title: "草稿类型",
                    value: "\(viewModel.draftTypes.count)",
                    subtitle: "覆盖范围",
                    tint: ZhihuijiTheme.ColorToken.primaryBright
                )
            }

            if canWrite {
                PrimaryGlassButton(
                    title: "新建草稿",
                    systemImage: "plus.circle.fill",
                    disabled: viewModel.isSaving
                ) {
                    viewModel.beginCreate()
                }
            } else {
                HStack(alignment: .top, spacing: 10) {
                    Image(systemName: "lock.shield.fill")
                        .foregroundStyle(ZhihuijiTheme.ColorToken.warning)
                        .padding(.top, 1)
                    Text("只读模式下不会显示草稿写入操作。")
                        .font(ZhihuijiTheme.Typography.caption)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                    Spacer()
                }
                .padding(14)
                .background(Color.white.opacity(0.52), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.cardSmall, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.cardSmall, style: .continuous)
                        .stroke(ZhihuijiTheme.ColorToken.warning.opacity(0.18), lineWidth: ZhihuijiTheme.Stroke.hairline)
                )
            }
        }
    }

    private var draftsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("草稿列表")
                .font(ZhihuijiTheme.Typography.sectionTitle)

            if viewModel.drafts.isEmpty {
                EmptyStateView(title: "暂无草稿", message: canWrite ? "还没有 AI 草稿，可点击上方新建。" : "还没有可查看的 AI 草稿。")
            } else {
                ForEach(viewModel.drafts) { draft in
                    if canWrite {
                        Button {
                            viewModel.beginEdit(draft)
                        } label: {
                            draftRow(draft, canWrite: true)
                        }
                        .buttonStyle(.plain)
                        .contextMenu {
                            Button(role: .destructive) {
                                viewModel.beginDelete(draft)
                                Task { await viewModel.delete(using: env.apiClient) }
                            } label: {
                                Label("删除草稿", systemImage: "trash")
                            }
                        }
                    } else {
                        draftRow(draft)
                    }
                }
            }
        }
    }

    private func draftRow(_ draft: AgentDraft, canWrite: Bool = false) -> some View {
        HStack(alignment: .top, spacing: 12) {
            Circle()
                .fill(draftTint(draft.status).opacity(0.16))
                .frame(width: 36, height: 36)
                .overlay(
                    Image(systemName: "doc.text.fill")
                        .font(ZhihuijiTheme.Typography.bodyMedium)
                        .foregroundStyle(draftTint(draft.status))
                )

            VStack(alignment: .leading, spacing: 6) {
                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(draft.title)
                            .font(ZhihuijiTheme.Typography.cardTitle)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                        Text(draft.draftType)
                            .font(ZhihuijiTheme.Typography.caption)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                    }
                    Spacer()
                    if canWrite {
                        Image(systemName: "chevron.right")
                            .font(ZhihuijiTheme.Typography.captionSemibold)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                    }
                }

                Text(viewModel.previewContent(draft.contentJson))
                    .font(ZhihuijiTheme.Typography.caption)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                    .lineLimit(2)

                HStack(spacing: 10) {
                    StatusChip(
                        title: draft.status ?? AgentContractStatus.active,
                        tint: draftTint(draft.status)
                    )
                    Text("更新于 \(draft.updatedAt.dateTimeText)")
                        .font(ZhihuijiTheme.Typography.caption)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                }
            }
        }
        .padding(14)
        .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
    }

    private func draftTint(_ status: String?) -> Color {
        switch status?.lowercased() {
        case AgentContractStatus.archived:
            return ZhihuijiTheme.ColorToken.warning
        case AgentContractStatus.closed:
            return ZhihuijiTheme.ColorToken.textTertiary
        default:
            return ZhihuijiTheme.ColorToken.primary
        }
    }
}

private struct AgentDraftEditorSheet: View {
    @Binding var title: String
    @Binding var content: String
    @Binding var status: String
    @Binding var draftType: String
    let isSaving: Bool
    let mode: AgentDraftEditorMode
    let canWrite: Bool
    let onSave: () -> Void
    let onDelete: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                VStack(alignment: .leading, spacing: 12) {
                    Text("草稿信息")
                        .font(ZhihuijiTheme.Typography.sectionTitle)

                    VStack(alignment: .leading, spacing: 8) {
                        Text("标题")
                            .font(ZhihuijiTheme.Typography.captionSemibold)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                        TextField("草稿标题", text: $title)
                            .fieldBackground()
                            .disabled(!canWrite)
                    }

                    VStack(alignment: .leading, spacing: 8) {
                        Text("类型")
                            .font(ZhihuijiTheme.Typography.captionSemibold)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                        TextField("如 question / order / report", text: $draftType)
                            .fieldBackground()
                            .disabled(!canWrite)
                    }

                    VStack(alignment: .leading, spacing: 8) {
                        Text("内容")
                            .font(ZhihuijiTheme.Typography.captionSemibold)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                        TextField("草稿内容", text: $content, axis: .vertical)
                            .lineLimit(5 ... 10)
                            .fieldBackground()
                            .disabled(!canWrite)
                    }

                    Picker("状态", selection: $status) {
                        Text("进行中").tag(AgentContractStatus.active)
                        Text("已归档").tag(AgentContractStatus.archived)
                    }
                    .pickerStyle(.segmented)
                    .disabled(!canWrite)
                }
                .padding(16)
                .glassCard()

                HStack(spacing: 12) {
                    if canWrite {
                        PrimaryGlassButton(
                            title: isSaving ? "保存中..." : "保存草稿",
                            systemImage: "square.and.arrow.down.fill",
                            disabled: isSaving,
                            action: onSave
                        )
                    }

                    if mode == .edit, canWrite {
                        Button(action: onDelete) {
                            HStack(spacing: 8) {
                                Image(systemName: "trash")
                                Text(isSaving ? "处理中..." : "删除草稿")
                                    .font(ZhihuijiTheme.Typography.bodyMedium)
                            }
                            .foregroundStyle(ZhihuijiTheme.ColorToken.danger)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(
                                ZhihuijiTheme.ColorToken.danger.opacity(0.08),
                                in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.pill, style: .continuous)
                            )
                            .overlay(
                                RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.pill, style: .continuous)
                                    .stroke(ZhihuijiTheme.ColorToken.danger.opacity(0.18), lineWidth: ZhihuijiTheme.Stroke.hairline)
                            )
                        }
                        .buttonStyle(.plain)
                        .disabled(isSaving)
                        .opacity(isSaving ? 0.55 : 1)
                    }
                }
            }
            .padding(20)
        }
    }
}

enum AgentDraftEditorMode {
    case create
    case edit

    var title: String {
        switch self {
        case .create: return "新建草稿"
        case .edit: return "编辑草稿"
        }
    }
}

@MainActor
final class AgentDraftsViewModel: ObservableObject {
    @Published var isLoading = false
    @Published var isSaving = false
    @Published var drafts: [AgentDraft] = []
    @Published var errorMessage: String?
    @Published var isEditorPresented = false
    @Published var editorTitle = ""
    @Published var editorContent = ""
    @Published var editorStatus = AgentContractStatus.active
    @Published var editorDraftType = "question"
    @Published var editorMode: AgentDraftEditorMode = .create

    private var editingDraftId: EntityID?

    var activeCount: Int {
        drafts.filter { ($0.status ?? AgentContractStatus.active) == AgentContractStatus.active }.count
    }

    var archivedCount: Int {
        drafts.filter { $0.status == AgentContractStatus.archived }.count
    }

    var draftTypes: Set<String> {
        Set(drafts.map(\.draftType))
    }

    func load(using client: APIClient) async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }

        do {
            drafts = try await client.fetchAgentDrafts(limit: 100)
        } catch {
            drafts = []
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    func beginCreate() {
        editorMode = .create
        editingDraftId = nil
        editorTitle = ""
        editorContent = ""
        editorStatus = AgentContractStatus.active
        editorDraftType = "question"
        errorMessage = nil
        isEditorPresented = true
    }

    func beginEdit(_ draft: AgentDraft) {
        editorMode = .edit
        editingDraftId = draft.id
        editorTitle = draft.title
        editorContent = decodeContent(draft.contentJson)
        editorStatus = draft.status ?? AgentContractStatus.active
        editorDraftType = draft.draftType
        errorMessage = nil
        isEditorPresented = true
    }

    func beginDelete(_ draft: AgentDraft) {
        editingDraftId = draft.id
        editorMode = .edit
    }

    func dismissEditor() {
        isEditorPresented = false
        editingDraftId = nil
    }

    func save(using client: APIClient) async {
        let title = editorTitle.trimmingCharacters(in: .whitespacesAndNewlines)
        let content = editorContent.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !title.isEmpty, !content.isEmpty else {
            errorMessage = "草稿标题和内容都不能为空"
            return
        }
        guard !isSaving else { return }

        isSaving = true
        defer { isSaving = false }

        let contentJson = makeContentJSON(content)

        do {
            switch editorMode {
            case .create:
                let created = try await client.createAgentDraft(
                    payload: AgentDraftCreatePayload(
                        conversationId: nil,
                        draftType: editorDraftType.nilIfBlank ?? "question",
                        title: title,
                        contentJson: contentJson,
                        status: editorStatus
                    )
                )
                drafts.insert(created, at: 0)
            case .edit:
                guard let id = editingDraftId else {
                    errorMessage = "没有找到要更新的草稿"
                    return
                }
                let updated = try await client.updateAgentDraft(
                    id: id,
                    payload: AgentDraftUpdatePayload(
                        conversationId: nil,
                        draftType: editorDraftType.nilIfBlank ?? "question",
                        title: title,
                        contentJson: contentJson,
                        status: editorStatus
                    )
                )
                if let index = drafts.firstIndex(where: { $0.id == updated.id }) {
                    drafts[index] = updated
                } else {
                    drafts.insert(updated, at: 0)
                }
            }
            errorMessage = nil
            dismissEditor()
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    func delete(using client: APIClient) async {
        guard let id = editingDraftId else { return }
        guard !isSaving else { return }

        isSaving = true
        defer { isSaving = false }

        do {
            try await client.deleteAgentDraft(id: id)
            drafts.removeAll { $0.id == id }
            errorMessage = nil
            dismissEditor()
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    func previewContent(_ contentJson: String) -> String {
        decodeContent(contentJson)
    }

    private func makeContentJSON(_ content: String) -> String {
        let payload: [String: String] = ["content": content]
        if let data = try? JSONSerialization.data(withJSONObject: payload, options: []),
           let text = String(data: data, encoding: .utf8) {
            return text
        }
        return content
    }

    private func decodeContent(_ contentJson: String) -> String {
        guard let data = contentJson.data(using: .utf8),
              let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
        else {
            return contentJson
        }
        if let content = object["content"] as? String, !content.isEmpty {
            return content
        }
        if let question = object["question"] as? String, !question.isEmpty {
            return question
        }
        if let title = object["title"] as? String, !title.isEmpty {
            return title
        }
        return contentJson
    }
}
