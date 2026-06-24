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
                refreshSection
                if let syncStatusMessage = viewModel.syncStatusMessage {
                    statusBanner(text: syncStatusMessage, tint: ZhihuijiTheme.ColorToken.success)
                }
                if !canManageDatabase {
                    databasePermissionSection
                }
                syncStatusGroup
                importActionGroup
                importJobFilterSection
                importResultSection
                healthSection
                cursorSection
                syncActionGroup
                jobsSection
            }
            .padding(20)
        }
        .navigationTitle("同步与导入")
        .task {
            await viewModel.load(using: env.apiClient)
        }
        .onChange(of: viewModel.importJobStatusFilter) { _, _ in
            Task {
                await viewModel.load(using: env.apiClient)
            }
        }
    }

    private func statusBanner(text: String, tint: Color) -> some View {
        HStack(alignment: .top, spacing: 10) {
            Circle()
                .fill(tint.opacity(0.14))
                .frame(width: 26, height: 26)
                .overlay(
                    Image(systemName: "checkmark.circle.fill")
                        .font(ZhihuijiTheme.Typography.caption)
                        .foregroundStyle(tint)
                )
            Text(text)
                .font(ZhihuijiTheme.Typography.caption)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                .fixedSize(horizontal: false, vertical: true)
            Spacer()
        }
        .padding(12)
        .background(Color.white.opacity(0.42), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.cardSmall, style: .continuous))
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
            Text("本页展示已有 `/v2/sync/*` 与 `/v2/import-jobs/*` 接口的状态与结果，具体任务执行、重试和取消由服务端完成。")
                .font(ZhihuijiTheme.Typography.body)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(16)
        .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
    }

    private var refreshSection: some View {
        PrimaryGlassButton(
            title: viewModel.isLoading ? "刷新中..." : "刷新同步状态",
            systemImage: "arrow.clockwise",
            disabled: viewModel.isLoading
        ) {
            Task {
                await viewModel.load(using: env.apiClient)
            }
        }
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
                TextField("客户端标识", text: $viewModel.clientId)
                    .fieldBackground()

                TextField("旧库文件路径", text: $viewModel.legacyDbPath)
                    .fieldBackground()

                Toggle("导入前重置当前账号数据", isOn: $viewModel.resetOwnedData)
                    .font(ZhihuijiTheme.Typography.body)
                    .tint(ZhihuijiTheme.ColorToken.primary)

                Text("客户端标识用于区分不同设备或门店的同步状态与导入任务。")
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

            PrimaryGlassButton(title: "同步直导旧库", systemImage: "square.and.arrow.down", disabled: !canManageDatabase) {
                Task {
                    await viewModel.importLegacySQLite(using: env.apiClient)
                }
            }
        }
    }

    private var syncStatusGroup: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("同步状态")
                .font(ZhihuijiTheme.Typography.sectionTitle)

            HStack(spacing: 12) {
                MetricCard(
                    title: "健康",
                    value: viewModel.health?.status ?? "--",
                    subtitle: viewModel.health?.message ?? "刷新后读取",
                    tint: ZhihuijiTheme.ColorToken.primary
                )
                MetricCard(
                    title: "游标",
                    value: viewModel.cursor?.lastCursor.nilIfBlank ?? "--",
                    subtitle: viewModel.cursor?.updatedAt.dateTimeText ?? "等待同步",
                    tint: ZhihuijiTheme.ColorToken.success
                )
            }
        }
    }

    private var importActionGroup: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("导入动作")
                .font(ZhihuijiTheme.Typography.sectionTitle)
            clientSection
            jobCreateSection
        }
    }

    private var syncActionGroup: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("同步动作")
                .font(ZhihuijiTheme.Typography.sectionTitle)
            uploadSection
            pullSection
        }
    }

    private var importJobFilterSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("导入任务")
                    .font(ZhihuijiTheme.Typography.sectionTitle)
                Spacer()
                StatusChip(title: "\(viewModel.importJobs.count)", tint: ZhihuijiTheme.ColorToken.primary)
            }

            Picker("导入任务筛选", selection: $viewModel.importJobStatusFilter) {
                ForEach(ImportJobStatusFilter.allCases) { filter in
                    Text(filter.title).tag(filter)
                }
            }
            .pickerStyle(.segmented)

            HStack(spacing: 10) {
                StatusChip(title: "待处理 \(viewModel.importJobs.filter { $0.status == ImportJobStatusFilter.pending.apiValue }.count)", tint: ZhihuijiTheme.ColorToken.warning)
                StatusChip(title: "进行中 \(viewModel.importJobs.filter { $0.status == ImportJobStatusFilter.running.apiValue }.count)", tint: ZhihuijiTheme.ColorToken.primary)
                StatusChip(title: "失败 \(viewModel.importJobs.filter { $0.status == ImportJobStatusFilter.failed.apiValue }.count)", tint: ZhihuijiTheme.ColorToken.danger)
            }

            Text("可按任务状态筛选服务端返回的导入任务，便于快速定位待处理项。")
                .font(ZhihuijiTheme.Typography.caption)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(16)
        .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
    }

    private var jobCreateSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("创建导入任务")
                .font(ZhihuijiTheme.Typography.sectionTitle)

            VStack(alignment: .leading, spacing: 10) {
                TextField("来源类型（默认旧库同步）", text: $viewModel.jobSourceType)
                    .fieldBackground()
                TextField("来源地址（旧库同步时必填）", text: $viewModel.jobSourceUri)
                    .fieldBackground()
                TextField("来源校验值（可选）", text: $viewModel.jobSourceChecksum)
                    .fieldBackground()
                TextField("任务幂等键（可选）", text: $viewModel.jobIdempotencyKey)
                    .fieldBackground()
                TextField("回放游标（可选）", text: $viewModel.jobReplayCursor)
                    .fieldBackground()
                TextField("参数 JSON（可选）", text: $viewModel.jobOptionsJson, axis: .vertical)
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
                    metadataRow(title: "旧库", value: result.legacyDbPath)
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
                EmptyStateView(title: "暂无直导结果", message: "运行旧库同步直导后，这里显示服务端返回的真实数量。")
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
                    MetricCard(title: "账号隔离", value: health.ownerScoped ? "是" : "否", subtitle: "服务端归属范围", tint: health.ownerScoped ? ZhihuijiTheme.ColorToken.success : ZhihuijiTheme.ColorToken.warning)
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
            Text("同步游标")
                .font(ZhihuijiTheme.Typography.sectionTitle)

            if let cursor = viewModel.cursor {
                VStack(alignment: .leading, spacing: 10) {
                    metadataRow(title: "客户端标识", value: cursor.clientId)
                    metadataRow(title: "上次游标", value: cursor.lastCursor.nilIfBlank ?? "--")
                    metadataRow(title: "更新时间", value: cursor.updatedAt.dateTimeText)
                }
                .padding(16)
                .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
            } else {
                EmptyStateView(title: "暂无游标", message: "服务端返回游标后，这里会显示当前客户端基线。")
            }
        }
    }

    private var uploadSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("上传手工变更")
                .font(ZhihuijiTheme.Typography.sectionTitle)

            VStack(alignment: .leading, spacing: 10) {
                TextField("实体类型（如商品、销售单）", text: $viewModel.syncUploadEntityType)
                    .fieldBackground()
                TextField("实体 ID", text: $viewModel.syncUploadEntityId)
                    .fieldBackground()
                TextField("操作类型（如更新、删除）", text: $viewModel.syncUploadOperation)
                    .fieldBackground()
                TextField("负载 JSON（可选）", text: $viewModel.syncUploadPayloadJson, axis: .vertical)
                    .lineLimit(3 ... 6)
                    .fieldBackground()
                TextField("更新时间毫秒时间戳（可选）", text: $viewModel.syncUploadUpdatedAtText)
                    .fieldBackground()
                TextField("上次同步游标（可选）", text: $viewModel.syncUploadLastCursor)
                    .fieldBackground()
                Text("这里只调用 `/v2/sync/upload` 上传一条手工变更，用于状态核验；完整本地变更队列、冲突处理和离线同步由服务端与后续客户端能力继续承接。")
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
                    MetricCard(title: "已接收", value: "\(response.acceptedCount)", subtitle: response.status, tint: ZhihuijiTheme.ColorToken.success)
                    MetricCard(title: "失败", value: "\(response.failedCount)", subtitle: response.nextCursor, tint: response.failedCount > 0 ? ZhihuijiTheme.ColorToken.danger : ZhihuijiTheme.ColorToken.primary)
                }
            } else {
                EmptyStateView(title: "等待上传结果", message: "填写单条变更并点击上传后，这里会展示服务端 upload 接口返回。")
            }
        }
    }

    private var pullSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("拉取预览")
                .font(ZhihuijiTheme.Typography.sectionTitle)

            VStack(alignment: .leading, spacing: 10) {
                TextField("起始游标（留空则使用服务端默认基线）", text: $viewModel.syncPullSinceCursor)
                    .fieldBackground()
                TextField("数量上限（可选，必须大于 0）", text: $viewModel.syncPullLimitText)
                    .fieldBackground()
                Text("这里只做只读拉取预览，不写入本地库，也不代表完整双向同步已完成。")
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

            PrimaryGlassButton(title: "拉取并确认游标", systemImage: "checkmark.circle", disabled: !canManageDatabase) {
                Task {
                    await viewModel.pullApplyAndAck(using: env.apiClient)
                }
            }

            if let response = viewModel.syncPullResponse {
                VStack(alignment: .leading, spacing: 12) {
                    HStack {
                        metadataRow(title: "生效游标", value: response.effectiveCursor ?? "--")
                        Spacer()
                        metadataRow(title: "下一游标", value: response.nextCursor)
                        Spacer()
                        StatusChip(title: response.hasMore ? "未完成" : "已完成", tint: response.hasMore ? ZhihuijiTheme.ColorToken.warning : ZhihuijiTheme.ColorToken.success)
                    }

                    let unsupportedTypes = viewModel.unsupportedPulledEntityTypes
                    if !unsupportedTypes.isEmpty {
                        Text("服务端健康状态未声明支持：\(unsupportedTypes.map(ZhihuijiDisplayName.syncEntityType).joined(separator: ", "))")
                            .font(ZhihuijiTheme.Typography.caption)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.warning)
                            .fixedSize(horizontal: false, vertical: true)
                    }

                    if response.changes.isEmpty {
                        EmptyStateView(title: "暂无变更", message: "服务端没有返回可预览的增量变更。")
                    } else {
                        VStack(spacing: 10) {
                            ForEach(response.changes.prefix(8)) { change in
                                HStack(alignment: .top, spacing: 12) {
                                    VStack(alignment: .leading, spacing: 4) {
                                        Text(ZhihuijiDisplayName.syncEntityType(change.entityType))
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
                                    StatusChip(title: ZhihuijiDisplayName.syncOperation(change.operation), tint: ZhihuijiTheme.ColorToken.primary)
                                }
                                .padding(12)
                                .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
                            }
                        }
                    }
                }
            } else {
                        EmptyStateView(title: "等待拉取预览", message: "输入游标和数量上限后可预览服务端返回的增量变更。")
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
                                    Text(ZhihuijiDisplayName.syncSourceType(job.sourceType))
                                        .font(ZhihuijiTheme.Typography.bodyMedium)
                                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                                    Text(job.sourceUri?.nilIfBlank ?? job.idempotencyKey?.nilIfBlank ?? "任务 #\(job.id.rawValue)")
                                        .font(ZhihuijiTheme.Typography.caption)
                                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                                }
                                Spacer()
                                StatusChip(title: ZhihuijiDisplayName.importJobStatus(job.status), tint: tint(for: job.status))
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
    @Published var jobSourceType = ""
    @Published var jobSourceUri = ""
    @Published var jobSourceChecksum = ""
    @Published var jobIdempotencyKey = ""
    @Published var jobReplayCursor = ""
    @Published var jobOptionsJson = ""
    @Published var syncUploadEntityType = ""
    @Published var syncUploadEntityId = ""
    @Published var syncUploadOperation = ""
    @Published var syncUploadPayloadJson = ""
    @Published var syncUploadUpdatedAtText = ""
    @Published var syncUploadLastCursor = ""
    @Published var syncUploadResponse: SyncUploadResponse?
    @Published var syncPullSinceCursor = ""
    @Published var syncPullLimitText = "50"
    @Published var syncPullResponse: SyncPullResponse?
    @Published var importJobStatusFilter: ImportJobStatusFilter = .all
    @Published var syncStatusMessage: String?
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
        syncStatusMessage = nil
        persistClientID()
        do {
            async let healthResult = client.fetchSyncHealth()
            async let cursorResult = client.fetchSyncCursor(clientId: clientId)
            async let jobsResult = client.fetchImportJobs(status: importJobStatusFilter.apiValue)

            health = try await healthResult
            cursor = try await cursorResult
            importJobs = try await jobsResult
            syncStatusMessage = "同步状态已刷新：\(clientId)"
        } catch {
            clearLoadedServerState()
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    func importLegacySQLite(using client: APIClient) async {
        guard let trimmedPath = legacyDbPath.nilIfBlank else {
            errorMessage = "请填写旧库文件路径。"
            return
        }

        UserDefaults.standard.set(trimmedPath, forKey: Self.legacyDbPathKey)
        isLoading = true
        errorMessage = nil
        syncStatusMessage = nil
        do {
            importResult = try await client.importLegacySQLite(
                payload: LegacySQLiteImportPayload(
                    legacyDbPath: trimmedPath,
                    resetOwnedData: resetOwnedData ? true : nil
                )
            )
            syncStatusMessage = "直导已完成：\(trimmedPath)"
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
        syncStatusMessage = nil
        do {
            let job = try await client.createImportJob(payload: payload)
            importJobs.removeAll { $0.id == job.id }
            importJobs.insert(job, at: 0)
            await loadJobs(using: client)
            syncStatusMessage = "已创建导入任务：\(job.sourceType)"
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    func retryImportJob(_ job: ImportJobRecord, using client: APIClient) async {
        isLoading = true
        errorMessage = nil
        syncStatusMessage = nil
        do {
            let retried = try await client.retryImportJob(id: job.id, payload: makeImportJobRetryPayload())
            replaceImportJob(retried)
            await loadJobs(using: client)
            syncStatusMessage = "已重试导入任务：\(job.id.rawValue)"
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    func cancelImportJob(_ job: ImportJobRecord, using client: APIClient) async {
        isLoading = true
        errorMessage = nil
        syncStatusMessage = nil
        do {
            let cancelled = try await client.cancelImportJob(id: job.id)
            replaceImportJob(cancelled)
            await loadJobs(using: client)
            syncStatusMessage = "已取消导入任务：\(job.id.rawValue)"
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    func uploadSyncChange(using client: APIClient) async {
        guard let payload = makeSyncUploadPayload() else { return }

        isLoading = true
        errorMessage = nil
        syncStatusMessage = nil
        do {
            syncUploadResponse = try await client.uploadSyncChanges(payload: payload)
            syncStatusMessage = "已上传 1 条手工变更，游标同步为服务端返回结果。"
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
        syncStatusMessage = nil
        do {
            syncPullResponse = try await client.pullSyncChanges(payload: payload)
            syncStatusMessage = "已拉取同步变更预览。"
        } catch {
            syncPullResponse = nil
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    func pullApplyAndAck(using client: APIClient) async {
        guard let payload = makeSyncPullPayload() else { return }

        isLoading = true
        errorMessage = nil
        syncStatusMessage = nil
        do {
            let pulled = try await client.pullSyncChanges(payload: payload)
            syncPullResponse = pulled
            if let nextCursor = pulled.nextCursor.nilIfBlank {
                let ack = try await client.acknowledgeSyncCursor(
                    payload: SyncCursorAckPayload(
                        clientId: clientId,
                        cursor: nextCursor
                    )
                )
                cursor = ack
                syncStatusMessage = ack.lastCursor.nilIfBlank.map { "已拉取并确认游标：\($0)" } ?? "已拉取并确认游标"
            } else {
                syncStatusMessage = "已拉取变更，但服务端未返回新游标。"
            }
        } catch {
            syncPullResponse = nil
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    func makeImportJobCreatePayload() -> ImportJobCreatePayload? {
        let sourceType = jobSourceType.nilIfBlank ?? "legacy_sqlite"
        let sourceUri = jobSourceUri.nilIfBlank
        if sourceType == "legacy_sqlite", sourceUri == nil {
            errorMessage = "旧库同步任务必须填写来源地址。"
            return nil
        }
        if let options = jobOptionsJson.nilIfBlank, !Self.isJSONObject(options) {
            errorMessage = "参数 JSON 必须是合法对象。"
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
            errorMessage = "请填写实体类型。"
            return nil
        }
        guard let entityId = syncUploadEntityId.nilIfBlank else {
            errorMessage = "请填写实体 ID。"
            return nil
        }
        let operation = syncUploadOperation.nilIfBlank ?? "upsert"
        if operation != "delete",
           let payload = syncUploadPayloadJson.nilIfBlank,
           !Self.isJSONObject(payload) {
            errorMessage = "负载 JSON 必须是合法对象。"
            return nil
        }

        let updatedAt: Int64?
        if let rawUpdatedAt = syncUploadUpdatedAtText.nilIfBlank {
            guard let parsedUpdatedAt = Int64(rawUpdatedAt), parsedUpdatedAt > 0 else {
                errorMessage = "更新时间必须是大于 0 的毫秒时间戳。"
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
                errorMessage = "拉取上限必须是大于 0 的数值。"
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
            let jobs = try await client.fetchImportJobs(status: importJobStatusFilter.apiValue)
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

enum ImportJobStatusFilter: String, CaseIterable, Identifiable {
    case all
    case pending
    case running
    case succeeded
    case failed
    case cancelled

    var id: String { rawValue }

    var apiValue: String? {
        switch self {
        case .all: return nil
        default: return rawValue
        }
    }

    var title: String {
        switch self {
        case .all: return "全部"
        case .pending: return "待处理"
        case .running: return "进行中"
        case .succeeded: return "成功"
        case .failed: return "失败"
        case .cancelled: return "取消"
        }
    }
}
