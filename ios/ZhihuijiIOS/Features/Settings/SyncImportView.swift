import SwiftUI

struct SyncImportView: View {
    @Environment(\.appEnvironment) private var env
    @EnvironmentObject private var session: AppSession
    @StateObject private var viewModel = SyncImportViewModel()

    private var canManageDatabase: Bool {
        PermissionPolicy.canManageDatabase(session.permissions)
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                workerBoundarySection
                if !canManageDatabase {
                    databasePermissionSection
                }
                clientSection
                jobCreateSection
                importResultSection
                healthSection
                cursorSection
                uploadSection
                pullSection
                jobsSection
            }
            .padding(20)
        }
        .navigationTitle("同步与导入")
        .task {
            await viewModel.load(using: env.apiClient)
        }
    }

    private var workerBoundarySection: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 10) {
                Image(systemName: "info.circle.fill")
                    .foregroundStyle(ZhihuijiTheme.ColorToken.warning)
                Text("任务生命周期以服务端为准")
                    .font(ZhihuijiTheme.Typography.bodyMedium)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
            }
            Text("iOS 当前只调用已有 `/v2/sync/*` 与 `/v2/import-jobs/*` 接口并展示返回状态；后台 worker、重试/取消是否真正生效仍需要服务端运行时证据验证。")
                .font(ZhihuijiTheme.Typography.body)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(16)
        .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
    }

    private var databasePermissionSection: some View {
        EmptyStateView(
            title: "同步与导入受限",
            message: "当前账号没有 database:manage 权限；本页不会触发同步、导入、重试、取消或手工上传动作。"
        )
    }

    private var clientSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("客户端")
                .font(ZhihuijiTheme.Typography.sectionTitle)

            VStack(alignment: .leading, spacing: 10) {
                TextField("Client ID", text: $viewModel.clientId)
                    .fieldBackground()

                TextField("Legacy SQLite path", text: $viewModel.legacyDbPath)
                    .fieldBackground()

                Toggle("导入前重置当前账号数据", isOn: $viewModel.resetOwnedData)
                    .font(ZhihuijiTheme.Typography.body)
                    .tint(ZhihuijiTheme.ColorToken.primary)

                Text("Client ID 用于隔离 `/v2/sync/*` 与 `/v2/import-jobs/*` 的服务端状态。")
                    .font(ZhihuijiTheme.Typography.caption)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
            }
            .padding(16)
            .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)

            PrimaryGlassButton(title: "保存并刷新", systemImage: "arrow.clockwise", disabled: !canManageDatabase) {
                Task {
                    await viewModel.persistClientIDAndReload(using: env.apiClient)
                }
            }

            PrimaryGlassButton(title: "同步直导 Legacy SQLite", systemImage: "square.and.arrow.down", disabled: !canManageDatabase) {
                Task {
                    await viewModel.importLegacySQLite(using: env.apiClient)
                }
            }
        }
    }

    private var jobCreateSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("创建导入任务")
                .font(ZhihuijiTheme.Typography.sectionTitle)

            VStack(alignment: .leading, spacing: 10) {
                TextField("Source type，如 legacy_sqlite", text: $viewModel.jobSourceType)
                    .fieldBackground()
                TextField("Source URI，legacy_sqlite 必填", text: $viewModel.jobSourceUri)
                    .fieldBackground()
                TextField("Source checksum，可选", text: $viewModel.jobSourceChecksum)
                    .fieldBackground()
                TextField("Idempotency key，可选", text: $viewModel.jobIdempotencyKey)
                    .fieldBackground()
                TextField("Replay cursor，可选", text: $viewModel.jobReplayCursor)
                    .fieldBackground()
                TextField("Options JSON，可选", text: $viewModel.jobOptionsJson, axis: .vertical)
                    .lineLimit(3 ... 6)
                    .fieldBackground()
            }
            .padding(16)
            .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)

            PrimaryGlassButton(title: "创建任务", systemImage: "plus", disabled: !canManageDatabase) {
                Task {
                    await viewModel.createImportJob(using: env.apiClient)
                }
            }
        }
    }

    private var importResultSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("同步直导结果")
                .font(ZhihuijiTheme.Typography.sectionTitle)

            if let result = viewModel.importResult {
                VStack(alignment: .leading, spacing: 10) {
                    metadataRow(title: "账号", value: "\(result.nickname) / \(result.phone)")
                    metadataRow(title: "来源", value: result.legacyDbPath)
                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                        MetricCard(title: "商品", value: "\(result.products)", subtitle: "已导入", tint: ZhihuijiTheme.ColorToken.primary)
                        MetricCard(title: "销售", value: "\(result.saleOrders)", subtitle: "已导入", tint: ZhihuijiTheme.ColorToken.success)
                        MetricCard(title: "采购", value: "\(result.purchaseOrders)", subtitle: "已导入", tint: ZhihuijiTheme.ColorToken.warning)
                        MetricCard(title: "财务", value: "\(result.financeRecords)", subtitle: "已导入", tint: ZhihuijiTheme.ColorToken.primaryBright)
                    }
                }
                .padding(16)
                .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
            } else {
                EmptyStateView(title: "暂无直导结果", message: "运行 legacy SQLite 同步直导后，这里显示服务端返回的真实数量。")
            }
        }
    }

    private var healthSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("同步健康状态")
                    .font(ZhihuijiTheme.Typography.sectionTitle)
                Spacer()
                if viewModel.isLoading {
                    ProgressView()
                        .scaleEffect(0.8)
                        .tint(ZhihuijiTheme.ColorToken.primary)
                }
            }

            if let health = viewModel.health {
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                    MetricCard(title: "状态", value: health.status, subtitle: health.message, tint: ZhihuijiTheme.ColorToken.primary)
                    MetricCard(title: "账号隔离", value: health.ownerScoped ? "是" : "否", subtitle: "服务端 owner scope", tint: health.ownerScoped ? ZhihuijiTheme.ColorToken.success : ZhihuijiTheme.ColorToken.warning)
                    MetricCard(title: "支持类型", value: "\(health.supportedEntityTypes.count)", subtitle: health.supportedEntityTypes.joined(separator: " / "), tint: ZhihuijiTheme.ColorToken.primaryBright)
                    MetricCard(title: "可上传", value: "\(health.uploadableEntityTypes.count)", subtitle: health.uploadableEntityTypes.joined(separator: " / "), tint: ZhihuijiTheme.ColorToken.success)
                }
            } else if viewModel.isLoading {
                LoadingStateView(message: "正在读取同步健康状态...")
            } else if let errorMessage = viewModel.errorMessage {
                EmptyStateView(title: "同步健康状态加载失败", message: errorMessage)
            } else {
                EmptyStateView(title: "暂无同步状态", message: "刷新后读取服务端当前状态。")
            }
        }
    }

    private var cursorSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("同步 Cursor")
                .font(ZhihuijiTheme.Typography.sectionTitle)

            if let cursor = viewModel.cursor {
                VStack(alignment: .leading, spacing: 10) {
                    metadataRow(title: "Client ID", value: cursor.clientId)
                    metadataRow(title: "Last Cursor", value: cursor.lastCursor.nilIfBlank ?? "--")
                    metadataRow(title: "更新时间", value: cursor.updatedAt.dateTimeText)
                }
                .padding(16)
                .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
            } else {
                EmptyStateView(title: "暂无 cursor", message: "服务端返回 cursor 后，这里会显示当前客户端基线。")
            }
        }
    }

    private var uploadSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Upload 手工变更")
                .font(ZhihuijiTheme.Typography.sectionTitle)

            VStack(alignment: .leading, spacing: 10) {
                TextField("Entity type，如 product", text: $viewModel.syncUploadEntityType)
                    .fieldBackground()
                TextField("Entity ID", text: $viewModel.syncUploadEntityId)
                    .fieldBackground()
                TextField("Operation，如 upsert / delete", text: $viewModel.syncUploadOperation)
                    .fieldBackground()
                TextField("Payload JSON，可选", text: $viewModel.syncUploadPayloadJson, axis: .vertical)
                    .lineLimit(3 ... 6)
                    .fieldBackground()
                TextField("Updated at 毫秒时间戳，可选", text: $viewModel.syncUploadUpdatedAtText)
                    .fieldBackground()
                TextField("Last sync cursor，可选", text: $viewModel.syncUploadLastCursor)
                    .fieldBackground()
                Text("这里只调用 `/v2/sync/upload` 上传一条手工变更，用于 API 验证；完整本地变更队列、冲突处理和离线同步仍未完成。")
                    .font(ZhihuijiTheme.Typography.caption)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .padding(16)
            .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)

            PrimaryGlassButton(title: "上传一条变更", systemImage: "arrow.up.circle", disabled: !canManageDatabase) {
                Task {
                    await viewModel.uploadSyncChange(using: env.apiClient)
                }
            }

            if let response = viewModel.syncUploadResponse {
                HStack(spacing: 12) {
                    MetricCard(title: "Accepted", value: "\(response.acceptedCount)", subtitle: response.status, tint: ZhihuijiTheme.ColorToken.success)
                    MetricCard(title: "Failed", value: "\(response.failedCount)", subtitle: response.nextCursor, tint: response.failedCount > 0 ? ZhihuijiTheme.ColorToken.danger : ZhihuijiTheme.ColorToken.primary)
                }
            } else {
                EmptyStateView(title: "尚未 upload", message: "填写单条变更后，可验证服务端 upload 接口返回。")
            }
        }
    }

    private var pullSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Pull 预览")
                .font(ZhihuijiTheme.Typography.sectionTitle)

            VStack(alignment: .leading, spacing: 10) {
                TextField("Since cursor，留空使用服务端默认基线", text: $viewModel.syncPullSinceCursor)
                    .fieldBackground()
                TextField("Limit，可选，必须大于 0", text: $viewModel.syncPullLimitText)
                    .fieldBackground()
                Text("这里只做 read-only pull 预览，不写入本地库，也不代表完整双向同步已完成。")
                    .font(ZhihuijiTheme.Typography.caption)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .padding(16)
            .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)

            PrimaryGlassButton(title: "拉取预览", systemImage: "arrow.down.circle", disabled: !canManageDatabase) {
                Task {
                    await viewModel.pullSyncChanges(using: env.apiClient)
                }
            }

            if let response = viewModel.syncPullResponse {
                VStack(alignment: .leading, spacing: 12) {
                    HStack {
                        metadataRow(title: "Effective", value: response.effectiveCursor ?? "--")
                        Spacer()
                        metadataRow(title: "Next", value: response.nextCursor)
                        Spacer()
                        StatusChip(title: response.hasMore ? "has more" : "done", tint: response.hasMore ? ZhihuijiTheme.ColorToken.warning : ZhihuijiTheme.ColorToken.success)
                    }

                    let unsupportedTypes = viewModel.unsupportedPulledEntityTypes
                    if !unsupportedTypes.isEmpty {
                        Text("服务端 health 未声明支持：\(unsupportedTypes.joined(separator: ", "))")
                            .font(ZhihuijiTheme.Typography.caption)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.warning)
                            .fixedSize(horizontal: false, vertical: true)
                    }

                    if response.changes.isEmpty {
                        EmptyStateView(title: "暂无 changes", message: "服务端没有返回可预览的增量变更。")
                    } else {
                        VStack(spacing: 10) {
                            ForEach(response.changes.prefix(8)) { change in
                                HStack(alignment: .top, spacing: 12) {
                                    VStack(alignment: .leading, spacing: 4) {
                                        Text(change.entityType)
                                            .font(ZhihuijiTheme.Typography.bodyMedium)
                                            .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                                        Text(change.entityId.rawValue)
                                            .font(ZhihuijiTheme.Typography.caption)
                                            .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                                        if let updatedAt = change.updatedAt {
                                            Text(updatedAt.dateTimeText)
                                                .font(ZhihuijiTheme.Typography.caption)
                                                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                                        }
                                    }
                                    Spacer()
                                    StatusChip(title: change.operation, tint: ZhihuijiTheme.ColorToken.primary)
                                }
                                .padding(12)
                                .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
                            }
                        }
                    }
                }
            } else {
                EmptyStateView(title: "尚未 pull", message: "输入 cursor/limit 后可预览服务端返回的 changes。")
            }
        }
    }

    private var jobsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("导入任务")
                .font(ZhihuijiTheme.Typography.sectionTitle)

            if viewModel.importJobs.isEmpty {
                EmptyStateView(title: "暂无导入任务", message: "创建任务或刷新后，这里展示服务端任务列表。")
            } else {
                VStack(spacing: 10) {
                    ForEach(viewModel.importJobs) { job in
                        VStack(alignment: .leading, spacing: 10) {
                            HStack(alignment: .top) {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(job.sourceType)
                                        .font(ZhihuijiTheme.Typography.bodyMedium)
                                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                                    Text(job.sourceUri?.nilIfBlank ?? job.idempotencyKey?.nilIfBlank ?? "Job #\(job.id.rawValue)")
                                        .font(ZhihuijiTheme.Typography.caption)
                                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                                }
                                Spacer()
                                StatusChip(title: job.status, tint: tint(for: job.status))
                            }

                            HStack {
                                metadataRow(title: "重试次数", value: "\(job.retryCount)")
                                Spacer()
                                metadataRow(title: "创建时间", value: job.createdAt.dateTimeText)
                            }

                            if let failureMessage = job.failureMessage?.nilIfBlank {
                                Text(failureMessage)
                                    .font(ZhihuijiTheme.Typography.caption)
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.warning)
                                    .fixedSize(horizontal: false, vertical: true)
                            }

                            HStack(spacing: 12) {
                                Button {
                                    Task { await viewModel.retryImportJob(job, using: env.apiClient) }
                                } label: {
                                    Label("重试", systemImage: "arrow.clockwise")
                                        .font(ZhihuijiTheme.Typography.captionSemibold)
                                }
                                .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                                .buttonStyle(.plain)
                                .disabled(viewModel.isLoading || !canManageDatabase)
                                .opacity(canManageDatabase ? 1 : 0.45)

                                Button {
                                    Task { await viewModel.cancelImportJob(job, using: env.apiClient) }
                                } label: {
                                    Label("取消", systemImage: "xmark.circle")
                                        .font(ZhihuijiTheme.Typography.captionSemibold)
                                }
                                .foregroundStyle(ZhihuijiTheme.ColorToken.warning)
                                .buttonStyle(.plain)
                                .disabled(viewModel.isLoading || !canManageDatabase)
                                .opacity(canManageDatabase ? 1 : 0.45)

                                Spacer()
                            }
                        }
                        .padding(14)
                        .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
                    }
                }
            }
        }
    }

    private func metadataRow(title: String, value: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(title)
                .font(ZhihuijiTheme.Typography.caption)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
            Text(value)
                .font(ZhihuijiTheme.Typography.captionSemibold)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                .lineLimit(1)
        }
    }

    private func tint(for status: String) -> Color {
        switch status.lowercased() {
        case "running":
            return ZhihuijiTheme.ColorToken.primary
        case "succeeded", "completed":
            return ZhihuijiTheme.ColorToken.success
        case "failed":
            return ZhihuijiTheme.ColorToken.danger
        case "cancelled", "canceled":
            return ZhihuijiTheme.ColorToken.warning
        default:
            return ZhihuijiTheme.ColorToken.textTertiary
        }
    }
}

@MainActor
final class SyncImportViewModel: ObservableObject {
    @Published var clientId: String
    @Published var health: SyncHealthRecord?
    @Published var cursor: SyncCursorRecord?
    @Published var importJobs: [ImportJobRecord] = []
    @Published var importResult: LegacySQLiteImportResult?
    @Published var legacyDbPath: String
    @Published var resetOwnedData: Bool
    @Published var jobSourceType = "legacy_sqlite"
    @Published var jobSourceUri = ""
    @Published var jobSourceChecksum = ""
    @Published var jobIdempotencyKey = ""
    @Published var jobReplayCursor = ""
    @Published var jobOptionsJson = ""
    @Published var syncUploadEntityType = ""
    @Published var syncUploadEntityId = ""
    @Published var syncUploadOperation = "upsert"
    @Published var syncUploadPayloadJson = ""
    @Published var syncUploadUpdatedAtText = ""
    @Published var syncUploadLastCursor = ""
    @Published var syncUploadResponse: SyncUploadResponse?
    @Published var syncPullSinceCursor = ""
    @Published var syncPullLimitText = "50"
    @Published var syncPullResponse: SyncPullResponse?
    @Published var isLoading = false
    @Published var errorMessage: String?

    private static let clientIdKey = "zhihuiji.ios.sync.client_id"
    private static let legacyDbPathKey = "zhihuiji.ios.sync.legacy_db_path"

    init() {
        clientId = UserDefaults.standard.string(forKey: Self.clientIdKey) ?? Self.makeDefaultClientId()
        legacyDbPath = UserDefaults.standard.string(forKey: Self.legacyDbPathKey) ?? ""
        resetOwnedData = false
    }

    func persistClientIDAndReload(using client: APIClient) async {
        persistClientID()
        await load(using: client)
    }

    func load(using client: APIClient) async {
        isLoading = true
        errorMessage = nil
        persistClientID()
        do {
            async let healthResult = client.fetchSyncHealth()
            async let cursorResult = client.fetchSyncCursor(clientId: clientId)
            async let jobsResult = client.fetchImportJobs()

            health = try await healthResult
            cursor = try await cursorResult
            importJobs = try await jobsResult
        } catch {
            clearLoadedServerState()
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    func importLegacySQLite(using client: APIClient) async {
        guard let trimmedPath = legacyDbPath.nilIfBlank else {
            errorMessage = "请填写 legacy SQLite 路径。"
            return
        }

        UserDefaults.standard.set(trimmedPath, forKey: Self.legacyDbPathKey)
        isLoading = true
        errorMessage = nil
        do {
            importResult = try await client.importLegacySQLite(
                payload: LegacySQLiteImportPayload(
                    legacyDbPath: trimmedPath,
                    resetOwnedData: resetOwnedData ? true : nil
                )
            )
        } catch {
            importResult = nil
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    func createImportJob(using client: APIClient) async {
        guard let payload = makeImportJobCreatePayload() else { return }

        isLoading = true
        errorMessage = nil
        do {
            let job = try await client.createImportJob(payload: payload)
            importJobs.removeAll { $0.id == job.id }
            importJobs.insert(job, at: 0)
            await loadJobs(using: client)
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    func retryImportJob(_ job: ImportJobRecord, using client: APIClient) async {
        isLoading = true
        errorMessage = nil
        do {
            let retried = try await client.retryImportJob(id: job.id, payload: makeImportJobRetryPayload())
            replaceImportJob(retried)
            await loadJobs(using: client)
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    func cancelImportJob(_ job: ImportJobRecord, using client: APIClient) async {
        isLoading = true
        errorMessage = nil
        do {
            let cancelled = try await client.cancelImportJob(id: job.id)
            replaceImportJob(cancelled)
            await loadJobs(using: client)
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    func uploadSyncChange(using client: APIClient) async {
        guard let payload = makeSyncUploadPayload() else { return }

        isLoading = true
        errorMessage = nil
        do {
            syncUploadResponse = try await client.uploadSyncChanges(payload: payload)
        } catch {
            syncUploadResponse = nil
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    func pullSyncChanges(using client: APIClient) async {
        guard let payload = makeSyncPullPayload() else { return }

        isLoading = true
        errorMessage = nil
        do {
            syncPullResponse = try await client.pullSyncChanges(payload: payload)
        } catch {
            syncPullResponse = nil
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    func makeImportJobCreatePayload() -> ImportJobCreatePayload? {
        guard let sourceType = jobSourceType.nilIfBlank else {
            errorMessage = "请填写 source type。"
            return nil
        }
        let sourceUri = jobSourceUri.nilIfBlank
        if sourceType == "legacy_sqlite", sourceUri == nil {
            errorMessage = "legacy_sqlite 任务必须填写 source URI。"
            return nil
        }
        if let options = jobOptionsJson.nilIfBlank, !Self.isJSONObject(options) {
            errorMessage = "Options JSON 必须是合法 JSON 对象。"
            return nil
        }

        persistClientID()
        errorMessage = nil
        return ImportJobCreatePayload(
            clientId: clientId,
            sourceType: sourceType,
            sourceUri: sourceUri,
            sourceChecksum: jobSourceChecksum.nilIfBlank,
            idempotencyKey: jobIdempotencyKey.nilIfBlank,
            replayCursor: jobReplayCursor.nilIfBlank,
            optionsJson: jobOptionsJson.nilIfBlank
        )
    }

    func makeImportJobRetryPayload() -> ImportJobRetryPayload? {
        guard let replayCursor = jobReplayCursor.nilIfBlank else {
            return nil
        }
        return ImportJobRetryPayload(replayCursor: replayCursor)
    }

    func makeSyncUploadPayload() -> SyncUploadPayload? {
        guard let entityType = syncUploadEntityType.nilIfBlank else {
            errorMessage = "请填写 upload entity type。"
            return nil
        }
        guard let entityId = syncUploadEntityId.nilIfBlank else {
            errorMessage = "请填写 upload entity ID。"
            return nil
        }
        guard let operation = syncUploadOperation.nilIfBlank else {
            errorMessage = "请填写 upload operation。"
            return nil
        }
        if operation != "delete",
           let payload = syncUploadPayloadJson.nilIfBlank,
           !Self.isJSONObject(payload) {
            errorMessage = "Upload payload JSON 必须是合法 JSON 对象。"
            return nil
        }

        let updatedAt: Int64?
        if let rawUpdatedAt = syncUploadUpdatedAtText.nilIfBlank {
            guard let parsedUpdatedAt = Int64(rawUpdatedAt), parsedUpdatedAt > 0 else {
                errorMessage = "Updated at 必须是大于 0 的毫秒时间戳。"
                return nil
            }
            updatedAt = parsedUpdatedAt
        } else {
            updatedAt = nil
        }

        persistClientID()
        errorMessage = nil
        let change = SyncChangeRecord(
            entityType: entityType,
            entityId: EntityID(rawValue: entityId),
            operation: operation,
            payload: syncUploadPayloadJson.nilIfBlank,
            updatedAt: updatedAt
        )
        return SyncUploadPayload(
            clientId: clientId,
            changes: [change],
            lastSyncCursor: syncUploadLastCursor.nilIfBlank
        )
    }

    func makeSyncPullPayload() -> SyncPullPayload? {
        let limit: Int?
        if let rawLimit = syncPullLimitText.nilIfBlank {
            guard let parsedLimit = Int(rawLimit), parsedLimit > 0 else {
                errorMessage = "Pull limit 必须是大于 0 的数值。"
                return nil
            }
            limit = parsedLimit
        } else {
            limit = nil
        }

        persistClientID()
        errorMessage = nil
        return SyncPullPayload(
            clientId: clientId,
            sinceCursor: syncPullSinceCursor.nilIfBlank,
            limit: limit
        )
    }

    var unsupportedPulledEntityTypes: [String] {
        guard let health, let syncPullResponse else { return [] }
        let supported = Set(health.supportedEntityTypes)
        return Array(Set(syncPullResponse.changes.map(\.entityType)).subtracting(supported)).sorted()
    }

    private func persistClientID() {
        let normalized = clientId.nilIfBlank ?? Self.makeDefaultClientId()
        clientId = normalized
        UserDefaults.standard.set(normalized, forKey: Self.clientIdKey)
    }

    private func loadJobs(using client: APIClient) async {
        do {
            let jobs = try await client.fetchImportJobs()
            importJobs = jobs
        } catch {
            importJobs = []
            errorMessage = error.localizedDescription
        }
    }

    private func clearLoadedServerState() {
        health = nil
        cursor = nil
        importJobs = []
        importResult = nil
        syncUploadResponse = nil
        syncPullResponse = nil
    }

    private func replaceImportJob(_ job: ImportJobRecord) {
        if let index = importJobs.firstIndex(where: { $0.id == job.id }) {
            importJobs[index] = job
        } else {
            importJobs.insert(job, at: 0)
        }
    }

    private static func makeDefaultClientId() -> String {
        "ios-\(String(UUID().uuidString.prefix(8)).lowercased())"
    }

    private static func isJSONObject(_ raw: String) -> Bool {
        guard let data = raw.data(using: .utf8),
              let object = try? JSONSerialization.jsonObject(with: data) else {
            return false
        }
        return object is [String: Any]
    }
}
