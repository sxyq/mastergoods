import SwiftUI

struct CustomerListView: View {
    @Environment(\.appEnvironment) private var env
    @EnvironmentObject private var session: AppSession
    @StateObject private var viewModel = CustomerListViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                headerCard

                searchField

                if let errorMessage = viewModel.errorMessage {
                    EmptyStateView(title: "客户读取失败", message: errorMessage)
                } else if viewModel.customers.isEmpty, !viewModel.isLoading {
                    EmptyStateView(title: "暂无客户", message: "当前没有客户档案数据，可调整关键词后重试。")
                } else {
                    LazyVStack(spacing: 12) {
                        ForEach(viewModel.customers) { customer in
                            NavigationLink {
                                CustomerDetailView(customerId: customer.id)
                            } label: {
                                customerRow(customer)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }

                PrimaryGlassButton(
                    title: viewModel.isLoading ? "刷新中..." : "刷新客户",
                    systemImage: "arrow.clockwise",
                    disabled: viewModel.isLoading
                ) {
                    Task { await viewModel.load(using: env.apiClient) }
                }
            }
            .padding(20)
        }
        .navigationTitle("客户档案")
        .task {
            await viewModel.load(using: env.apiClient)
        }
    }

    private var headerCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 6) {
                    Text("客户档案")
                        .font(ZhihuijiTheme.Typography.pageTitle)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                    Text("独立管理客户资料、应收余额与往来记录。")
                        .font(ZhihuijiTheme.Typography.body)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                }
                Spacer()
                StatusChip(
                    title: "共 \(viewModel.customers.count) 位",
                    tint: ZhihuijiTheme.ColorToken.success
                )
            }

            HStack(spacing: 10) {
                archiveBadge(
                    title: "应收总额",
                    value: viewModel.totalReceivable.currencyText,
                    tint: ZhihuijiTheme.ColorToken.warning
                )
                archiveBadge(
                    title: "启用客户",
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
            TextField("搜索客户名称 / 手机号", text: $viewModel.keyword)
                .fieldBackground()
                .onSubmit {
                    Task { await viewModel.load(using: env.apiClient) }
                }

            Picker("状态", selection: $viewModel.statusFilter) {
                ForEach(CustomerStatusFilter.allCases) { filter in
                    Text(filter.title).tag(filter)
                }
            }
            .pickerStyle(.segmented)
            .onChange(of: viewModel.statusFilter) { _, _ in
                Task { await viewModel.load(using: env.apiClient) }
            }
        }
    }

    private func customerRow(_ customer: CustomerRecord) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(customer.name)
                        .font(ZhihuijiTheme.Typography.bodyMedium)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                    Text(customer.phone)
                        .font(ZhihuijiTheme.Typography.caption)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                }
                Spacer()
                StatusChip(
                    title: customer.status == 1 ? "启用" : "停用",
                    tint: customer.status == 1 ? ZhihuijiTheme.ColorToken.success : ZhihuijiTheme.ColorToken.warning
                )
            }

            if let groupName = customer.groupName?.nilIfBlank {
                Text("分组 \(groupName)")
                    .font(ZhihuijiTheme.Typography.captionSemibold)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.success)
            }

            HStack {
                if let contact = customer.primaryContactName?.nilIfBlank {
                    Text("联系人 \(contact)")
                        .font(ZhihuijiTheme.Typography.caption)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                }
                Spacer()
                if let balance = customer.balance {
                    Text("应收 \(balance.currencyText)")
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
final class CustomerListViewModel: ObservableObject {
    @Published var keyword = ""
    @Published var statusFilter: CustomerStatusFilter = .all
    @Published var isLoading = false
    @Published var customers: [CustomerRecord] = []
    @Published var errorMessage: String?

    var totalReceivable: Double {
        customers.compactMap { $0.balance }.filter { $0 > 0 }.reduce(0, +)
    }

    var enabledCount: Int {
        customers.filter { $0.status == 1 }.count
    }

    func load(using client: APIClient) async {
        isLoading = true
        defer { isLoading = false }
        do {
            customers = try await client.fetchCustomers(
                keyword: keyword.nilIfBlank,
                status: statusFilter.apiValue,
                groupId: nil,
                page: 1,
                size: 30
            )
            errorMessage = nil
        } catch {
            customers = []
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }
}

enum CustomerStatusFilter: String, CaseIterable, Identifiable {
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
