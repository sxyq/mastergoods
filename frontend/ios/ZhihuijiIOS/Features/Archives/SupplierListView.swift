import SwiftUI

struct SupplierListView: View {
    @Environment(\.appEnvironment) private var env
    @EnvironmentObject private var session: AppSession
    @StateObject private var viewModel = SupplierListViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                headerCard

                searchField

                if let errorMessage = viewModel.errorMessage {
                    EmptyStateView(title: "供应商读取失败", message: errorMessage)
                } else if viewModel.suppliers.isEmpty, !viewModel.isLoading {
                    EmptyStateView(title: "暂无供应商", message: "当前没有供应商档案数据，可调整关键词后重试。")
                } else {
                    LazyVStack(spacing: 12) {
                        ForEach(viewModel.suppliers) { supplier in
                            NavigationLink {
                                SupplierDetailView(supplierId: supplier.id)
                            } label: {
                                supplierRow(supplier)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }

                PrimaryGlassButton(
                    title: viewModel.isLoading ? "刷新中..." : "刷新供应商",
                    systemImage: "arrow.clockwise",
                    disabled: viewModel.isLoading
                ) {
                    Task { await viewModel.load(using: env.apiClient) }
                }
            }
            .padding(20)
        }
        .navigationTitle("供应商档案")
        .task {
            await viewModel.load(using: env.apiClient)
        }
    }

    private var headerCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 6) {
                    Text("供应商档案")
                        .font(ZhihuijiTheme.Typography.pageTitle)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                    Text("独立管理供应商资料、应付余额与对账记录。")
                        .font(ZhihuijiTheme.Typography.body)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                }
                Spacer()
                StatusChip(
                    title: "共 \(viewModel.suppliers.count) 家",
                    tint: ZhihuijiTheme.ColorToken.warning
                )
            }

            HStack(spacing: 10) {
                archiveBadge(
                    title: "应付总额",
                    value: viewModel.totalPayable.currencyText,
                    tint: ZhihuijiTheme.ColorToken.warning
                )
                archiveBadge(
                    title: "启用供应商",
                    value: "\(viewModel.enabledCount)",
                    tint: ZhihuijiTheme.ColorToken.success
                )
            }
        }
        .padding(18)
        .glassCard()
    }

    private var searchField: some View {
        VStack(alignment: .leading, spacing: 10) {
            TextField("搜索供应商名称 / 手机号", text: $viewModel.keyword)
                .fieldBackground()
                .onSubmit {
                    Task { await viewModel.load(using: env.apiClient) }
                }

            Picker("状态", selection: $viewModel.statusFilter) {
                ForEach(SupplierStatusFilter.allCases) { filter in
                    Text(filter.title).tag(filter)
                }
            }
            .pickerStyle(.segmented)
            .onChange(of: viewModel.statusFilter) { _, _ in
                Task { await viewModel.load(using: env.apiClient) }
            }
        }
    }

    private func supplierRow(_ supplier: SupplierRecord) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(supplier.name)
                        .font(ZhihuijiTheme.Typography.bodyMedium)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                    Text(supplier.phone)
                        .font(ZhihuijiTheme.Typography.caption)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                }
                Spacer()
                StatusChip(
                    title: supplier.status == 1 ? "启用" : "停用",
                    tint: supplier.status == 1 ? ZhihuijiTheme.ColorToken.success : ZhihuijiTheme.ColorToken.warning
                )
            }

            if let groupName = supplier.groupName?.nilIfBlank {
                Text("分组 \(groupName)")
                    .font(ZhihuijiTheme.Typography.captionSemibold)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.warning)
            }

            HStack {
                if let contact = supplier.primaryContactName?.nilIfBlank {
                    Text("联系人 \(contact)")
                        .font(ZhihuijiTheme.Typography.caption)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                }
                Spacer()
                if let balance = supplier.balance {
                    Text("应付 \(balance.currencyText)")
                        .font(ZhihuijiTheme.Typography.bodyMedium)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.warning)
                }
            }

            HStack {
                Spacer()
                Image(systemName: "chevron.right")
                    .font(ZhihuijiTheme.Typography.captionSemibold)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
            }
        }
        .padding(16)
        .glassCard()
    }

    private func archiveBadge(title: String, value: String, tint: Color) -> some View {
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
final class SupplierListViewModel: ObservableObject {
    @Published var keyword = ""
    @Published var statusFilter: SupplierStatusFilter = .all
    @Published var isLoading = false
    @Published var suppliers: [SupplierRecord] = []
    @Published var errorMessage: String?

    var totalPayable: Double {
        suppliers.compactMap { $0.balance }.filter { $0 > 0 }.reduce(0, +)
    }

    var enabledCount: Int {
        suppliers.filter { $0.status == 1 }.count
    }

    func load(using client: APIClient) async {
        isLoading = true
        defer { isLoading = false }
        do {
            suppliers = try await client.fetchSuppliers(
                keyword: keyword.nilIfBlank,
                status: statusFilter.apiValue,
                groupId: nil,
                page: 1,
                size: 30
            )
            errorMessage = nil
        } catch {
            suppliers = []
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }
}

enum SupplierStatusFilter: String, CaseIterable, Identifiable {
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

    var apiValue: Int? {
        switch self {
        case .all: return nil
        case .enabled: return 1
        case .disabled: return 0
        }
    }
}
