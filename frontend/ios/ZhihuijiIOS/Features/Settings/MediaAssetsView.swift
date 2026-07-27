import SwiftUI

struct MediaAssetsView: View {
    @Environment(\.appEnvironment) private var env
    @EnvironmentObject private var session: AppSession
    @StateObject private var viewModel = MediaAssetsViewModel()
    private let initialTargetType: String?
    private let initialTargetId: String?

    init(initialTargetType: String? = nil, initialTargetId: String? = nil) {
        self.initialTargetType = initialTargetType
        self.initialTargetId = initialTargetId
    }

    private var canWriteMedia: Bool {
        PermissionPolicy.canWriteMedia(
            targetType: viewModel.targetType.nilIfBlank ?? initialTargetType?.nilIfBlank,
            permissions: session.permissions
        )
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                uploadBoundarySection
                targetContextSection
                searchSection
                createAssetSection
                if let errorMessage = viewModel.errorMessage {
                    EmptyStateView(title: "媒体数据加载失败", message: errorMessage)
                }
                bindingSection
                assetListSection
                bindingListSection
            }
            .padding(20)
        }
        .navigationTitle("媒体资产")
        .task {
            viewModel.applyInitialTarget(type: initialTargetType, id: initialTargetId)
            await viewModel.load(using: env.apiClient)
        }
    }

    private var targetContextSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("当前目标")
                    .font(ZhihuijiTheme.Typography.sectionTitle)
                Spacer()
                StatusChip(
                    title: canWriteMedia ? "可写" : "只读",
                    tint: canWriteMedia ? ZhihuijiTheme.ColorToken.success : ZhihuijiTheme.ColorToken.warning
                )
            }

            HStack(spacing: 10) {
                StatusChip(title: "类型 \(ZhihuijiDisplayName.mediaTargetType(viewModel.targetType.nilIfBlank ?? "--"))", tint: ZhihuijiTheme.ColorToken.primary)
                StatusChip(title: "目标 \(viewModel.targetId.nilIfBlank ?? "--")", tint: ZhihuijiTheme.ColorToken.warning)
                StatusChip(title: "对象 \(viewModel.assets.count)", tint: ZhihuijiTheme.ColorToken.primaryBright)
            }

            Text("先选定目标类型和目标 ID，再登记或绑定媒体对象；这页会始终围绕当前业务对象工作。")
                .font(ZhihuijiTheme.Typography.caption)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                .fixedSize(horizontal: false, vertical: true)

            HStack(spacing: 10) {
                PrimaryGlassButton(
                    title: viewModel.isLoading ? "刷新中..." : "刷新媒体",
                    systemImage: "arrow.clockwise",
                    disabled: viewModel.isLoading
                ) {
                    Task { await viewModel.load(using: env.apiClient) }
                }

                Button {
                    Task {
                        await viewModel.loadBindings(using: env.apiClient)
                    }
                } label: {
                    Label("查看当前绑定", systemImage: "link")
                        .font(ZhihuijiTheme.Typography.captionSemibold)
                }
                .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                .buttonStyle(.plain)
                .padding(.horizontal, 12)
                .padding(.vertical, 11)
                .background(Color.white.opacity(0.50), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.cardSmall, style: .continuous))
            }
        }
        .padding(16)
        .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
    }

    private var searchSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("快速筛选")
                .font(ZhihuijiTheme.Typography.sectionTitle)

            TextField("按对象键、文件名、目标类型或目标 ID 搜索", text: $viewModel.searchText)
                .fieldBackground()

            HStack(spacing: 10) {
                StatusChip(title: "对象 \(viewModel.filteredAssets.count)", tint: ZhihuijiTheme.ColorToken.primary)
                StatusChip(title: "绑定 \(viewModel.filteredBindings.count)", tint: ZhihuijiTheme.ColorToken.warning)
                StatusChip(title: "总对象 \(viewModel.assets.count)", tint: ZhihuijiTheme.ColorToken.success)
            }
        }
        .padding(16)
        .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
    }

    private var uploadBoundarySection: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 10) {
                Image(systemName: "info.circle.fill")
                    .foregroundStyle(ZhihuijiTheme.ColorToken.warning)
                Text("当前只登记已上传对象")
                    .font(ZhihuijiTheme.Typography.bodyMedium)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
            }
            Text("真实文件上传、对象存储预签名与校验链路仍待后端运行时补齐。本页不会生成默认对象或假文件，只把已经存在的对象键绑定到业务对象。")
                .font(ZhihuijiTheme.Typography.body)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                .fixedSize(horizontal: false, vertical: true)
            if !canWriteMedia {
                Text("当前账号没有当前目标类型的媒体写权限；本页仅展示服务端返回的真实对象和绑定记录。")
                    .font(ZhihuijiTheme.Typography.captionSemibold)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.warning)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .padding(16)
        .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
    }

    private var createAssetSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("登记媒体对象")
                .font(ZhihuijiTheme.Typography.sectionTitle)

            VStack(alignment: .leading, spacing: 10) {
                TextField("资产类型（默认商品主图）", text: $viewModel.assetType).fieldBackground()
                TextField("存储提供方（默认对象存储）", text: $viewModel.storageProvider).fieldBackground()
                TextField("存储桶，可选", text: $viewModel.bucketName).fieldBackground()
                TextField("对象键（必填）", text: $viewModel.objectKey).fieldBackground()
                TextField("原始文件名（必填）", text: $viewModel.originalFileName).fieldBackground()
                TextField("文件 MIME 类型（必填）", text: $viewModel.mimeType).fieldBackground()
                TextField("文件大小（bytes，必填）", text: $viewModel.sizeBytesText).fieldBackground()
                TextField("校验值（可选）", text: $viewModel.checksum).fieldBackground()
                TextField("宽度（可选）", text: $viewModel.widthText).fieldBackground()
                TextField("高度（可选）", text: $viewModel.heightText).fieldBackground()
                TextField("元数据 JSON（可选）", text: $viewModel.metadataJson, axis: .vertical)
                    .fieldBackground()
            }
            .padding(16)
            .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)

            PrimaryGlassButton(title: "登记对象元数据", systemImage: "plus", disabled: !canWriteMedia) {
                Task {
                    await viewModel.createAsset(using: env.apiClient)
                }
            }
        }
    }

    private var bindingSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("绑定业务对象")
                .font(ZhihuijiTheme.Typography.sectionTitle)

            VStack(alignment: .leading, spacing: 10) {
                TextField("媒体资产 ID", text: $viewModel.bindingAssetId).fieldBackground()
                TextField("目标类型（默认媒体对象）", text: $viewModel.targetType).fieldBackground()
                TextField("目标 ID", text: $viewModel.targetId).fieldBackground()
                TextField("排序，可选", text: $viewModel.sortOrderText).fieldBackground()
            }
            .padding(16)
            .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)

            PrimaryGlassButton(title: "创建绑定", systemImage: "link", disabled: !canWriteMedia) {
                Task {
                    await viewModel.createBinding(using: env.apiClient)
                }
            }
        }
    }

    private var assetListSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("媒体对象")
                    .font(ZhihuijiTheme.Typography.sectionTitle)
                Spacer()
                if viewModel.isLoading {
                    ProgressView().scaleEffect(0.8).tint(ZhihuijiTheme.ColorToken.primary)
                }
            }

            if viewModel.filteredAssets.isEmpty {
                EmptyStateView(title: "暂无媒体对象", message: "登记已上传对象后，这里会显示真实的对象键、大小和绑定入口。")
            } else {
                VStack(spacing: 10) {
                    ForEach(viewModel.filteredAssets) { asset in
                        VStack(alignment: .leading, spacing: 10) {
                            HStack(alignment: .top) {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(asset.originalFileName)
                                        .font(ZhihuijiTheme.Typography.bodyMedium)
                                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                                    Text(asset.objectKey)
                                        .font(ZhihuijiTheme.Typography.caption)
                                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                                }
                                Spacer()
                                StatusChip(title: ZhihuijiDisplayName.mediaAssetType(asset.assetType), tint: ZhihuijiTheme.ColorToken.primary)
                            }

                            HStack {
                                metadataRow(title: "存储", value: ZhihuijiDisplayName.storageProvider(asset.storageProvider))
                                Spacer()
                                metadataRow(title: "大小", value: "\(asset.sizeBytes)")
                            }

                            HStack {
                                metadataRow(title: "创建时间", value: asset.createdAt.dateTimeText)
                                Spacer()
                                Button("查看绑定") {
                                    Task {
                                        await viewModel.loadBindings(
                                            targetType: "asset",
                                            targetId: asset.id.rawValue,
                                            using: env.apiClient
                                        )
                                    }
                                }
                                .font(ZhihuijiTheme.Typography.captionSemibold)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                                .buttonStyle(.plain)
                                .disabled(!canWriteMedia)
                                .opacity(canWriteMedia ? 1 : 0.45)
                            }

                            HStack {
                                Spacer()
                                Button {
                                    Task { await viewModel.deleteAsset(id: asset.id, using: env.apiClient) }
                                } label: {
                                    Label("删除", systemImage: "trash")
                                        .font(ZhihuijiTheme.Typography.captionSemibold)
                                }
                                .foregroundStyle(ZhihuijiTheme.ColorToken.danger)
                                .buttonStyle(.plain)
                                .disabled(!canWriteMedia)
                                .opacity(canWriteMedia ? 1 : 0.45)
                            }
                        }
                        .padding(14)
                        .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
                    }
                }
            }
        }
    }

    private var bindingListSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("绑定记录")
                .font(ZhihuijiTheme.Typography.sectionTitle)

            if viewModel.filteredBindings.isEmpty {
                EmptyStateView(title: "暂无绑定", message: "选择目标类型和目标 ID 后，可查看媒体对象的业务绑定。")
            } else {
                VStack(spacing: 10) {
                    ForEach(viewModel.filteredBindings) { binding in
                        HStack(alignment: .top) {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(ZhihuijiDisplayName.mediaTargetType(binding.targetType))
                                    .font(ZhihuijiTheme.Typography.bodyMedium)
                                Text(binding.targetId.rawValue)
                                    .font(ZhihuijiTheme.Typography.caption)
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                            }
                            Spacer()
                            Text("排序 \(binding.sortOrder ?? 0)")
                                .font(ZhihuijiTheme.Typography.captionSemibold)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                            Button {
                                Task { await viewModel.deleteBinding(id: binding.id, using: env.apiClient) }
                            } label: {
                                Image(systemName: "trash")
                            }
                            .foregroundStyle(ZhihuijiTheme.ColorToken.danger)
                            .buttonStyle(.plain)
                            .disabled(!canWriteMedia)
                            .opacity(canWriteMedia ? 1 : 0.45)
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
}

@MainActor
final class MediaAssetsViewModel: ObservableObject {
    @Published var assets: [MediaAssetRecord] = []
    @Published var bindings: [MediaBindingRecord] = []
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var searchText = ""

    @Published var assetType = ""
    @Published var storageProvider = ""
    @Published var bucketName = ""
    @Published var objectKey = ""
    @Published var originalFileName = ""
    @Published var mimeType = ""
    @Published var sizeBytesText = ""
    @Published var checksum = ""
    @Published var widthText = ""
    @Published var heightText = ""
    @Published var metadataJson = ""

    @Published var bindingAssetId = ""
    @Published var targetType = ""
    @Published var targetId = ""
    @Published var sortOrderText = ""
    @Published var bindingTargetType = "asset"
    @Published var bindingTargetId = ""

    var filteredAssets: [MediaAssetRecord] {
        let keyword = searchText.nilIfBlank?.lowercased()
        guard let keyword else { return assets }
        return assets.filter { asset in
            asset.assetType.lowercased().contains(keyword)
                || asset.originalFileName.lowercased().contains(keyword)
                || asset.objectKey.lowercased().contains(keyword)
                || asset.storageProvider.lowercased().contains(keyword)
        }
    }

    var filteredBindings: [MediaBindingRecord] {
        let keyword = searchText.nilIfBlank?.lowercased()
        guard let keyword else { return bindings }
        return bindings.filter { binding in
            binding.targetType.lowercased().contains(keyword)
                || binding.targetId.rawValue.lowercased().contains(keyword)
                || binding.assetId.rawValue.lowercased().contains(keyword)
        }
    }

    func applyInitialTarget(type: String?, id: String?) {
        guard let type = type?.nilIfBlank, let id = id?.nilIfBlank else {
            return
        }
        targetType = type
        targetId = id
        bindingTargetType = type
        bindingTargetId = id
    }

    func load(using client: APIClient) async {
        isLoading = true
        errorMessage = nil
        do {
            async let assetsResult = client.fetchMediaAssets()
            assets = try await assetsResult
            if let first = assets.first {
                if bindingAssetId.nilIfBlank == nil {
                    bindingAssetId = first.id.rawValue
                }
                if bindingTargetId.nilIfBlank == nil {
                    bindingTargetId = first.id.rawValue
                }
                if targetId.nilIfBlank == nil {
                    targetId = first.id.rawValue
                }
            }
            await loadBindings(using: client)
        } catch {
            assets = []
            bindings = []
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    func loadBindings(using client: APIClient) async {
        guard let targetId = bindingTargetId.nilIfBlank else {
            bindings = []
            return
        }
        let targetType = bindingTargetType.nilIfBlank ?? "asset"
        do {
            bindings = try await client.fetchMediaBindings(targetType: targetType, targetId: EntityID(rawValue: targetId))
        } catch {
            bindings = []
            errorMessage = error.localizedDescription
        }
    }

    func loadBindings(targetType: String, targetId: String, using client: APIClient) async {
        bindingTargetType = targetType
        bindingTargetId = targetId
        await loadBindings(using: client)
    }

    func createAsset(using client: APIClient) async {
        guard let payload = makeCreateAssetPayload() else { return }
        do {
            let _ = try await client.createMediaAsset(payload: payload)
            await load(using: client)
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func makeCreateAssetPayload() -> MediaAssetCreatePayload? {
        guard let objectKey = objectKey.nilIfBlank else {
            errorMessage = "请先填写真实的对象键；iOS 当前不会生成默认对象或上传假文件。"
            return nil
        }
        guard let originalFileName = originalFileName.nilIfBlank else {
            errorMessage = "请填写原始文件名。"
            return nil
        }
        guard let mimeType = mimeType.nilIfBlank else {
            errorMessage = "请填写文件 MIME 类型。"
            return nil
        }
        guard let sizeText = sizeBytesText.nilIfBlank,
              let sizeBytes = Int64(sizeText),
              sizeBytes > 0 else {
            errorMessage = "文件大小必须是大于 0 的 bytes 数值。"
            return nil
        }
        if let metadata = metadataJson.nilIfBlank,
           !Self.isJSONObject(metadata) {
            errorMessage = "元数据 JSON 必须是合法 JSON 对象。"
            return nil
        }

        errorMessage = nil
        return MediaAssetCreatePayload(
            assetType: assetType.nilIfBlank ?? "product_cover",
            storageProvider: storageProvider.nilIfBlank ?? "object_storage",
            bucketName: bucketName.nilIfBlank,
            objectKey: objectKey,
            originalFileName: originalFileName,
            mimeType: mimeType.nilIfBlank ?? "image/png",
            sizeBytes: sizeBytes,
            checksum: checksum.nilIfBlank,
            width: Int(widthText.nilIfBlank ?? ""),
            height: Int(heightText.nilIfBlank ?? ""),
            metadataJson: metadataJson.nilIfBlank
        )
    }

    private static func isJSONObject(_ raw: String) -> Bool {
        guard let data = raw.data(using: .utf8),
              let object = try? JSONSerialization.jsonObject(with: data) else {
            return false
        }
        return object is [String: Any]
    }

    func deleteAsset(id: EntityID, using client: APIClient) async {
        do {
            try await client.deleteMediaAsset(id: id)
            await load(using: client)
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func createBinding(using client: APIClient) async {
        guard let assetId = bindingAssetId.nilIfBlank.map(EntityID.init(rawValue:)),
              let targetId = targetId.nilIfBlank.map(EntityID.init(rawValue:)) else {
            errorMessage = "媒体资产 ID 和目标 ID 都是必填项。"
            return
        }
        do {
            let payload = MediaBindingCreatePayload(
                assetId: assetId,
                targetType: targetType.nilIfBlank ?? "asset",
                targetId: targetId,
                sortOrder: Int(sortOrderText.nilIfBlank ?? "")
            )
            let _ = try await client.createMediaBinding(payload: payload)
            bindings = try await client.fetchMediaBindings(targetType: payload.targetType, targetId: payload.targetId)
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func deleteBinding(id: EntityID, using client: APIClient) async {
        do {
            try await client.deleteMediaBinding(id: id)
            if let targetId = targetId.nilIfBlank.map(EntityID.init(rawValue:)) {
                let targetType = targetType.nilIfBlank ?? "asset"
                bindings = try await client.fetchMediaBindings(targetType: targetType, targetId: targetId)
            }
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}

private extension Optional where Wrapped == String {
    var isNilOrEmpty: Bool {
        switch self {
        case .none: return true
        case .some(let value): return value.isEmpty
        }
    }
}
