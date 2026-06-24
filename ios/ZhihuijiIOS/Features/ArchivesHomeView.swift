import SwiftUI

struct ArchivesHomeView: View {
    @EnvironmentObject private var session: AppSession
    @Environment(\.appEnvironment) private var env
    @StateObject private var viewModel = ArchivesHomeViewModel()
    @State private var customerDetail: CustomerRecord?
    @State private var supplierDetail: SupplierRecord?

    private var actionPolicy: ArchivesHomeActionPolicy {
        ArchivesHomeActionPolicy.resolve(for: session.permissions)
    }

    var availableTabs: [ArchiveTab] {
        ArchivesHomeAccessPolicy.resolve(for: session.permissions).availableTabs
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                headerCard
                tabStrip
                searchField
                contentSection
            }
            .padding(20)
        }
        .navigationTitle("档案")
        .task {
            viewModel.ensureSelectedTab(in: availableTabs)
            await viewModel.load(using: env.apiClient)
        }
        .onChange(of: availableTabs.map(\.id).joined(separator: ",")) { _, _ in
            viewModel.ensureSelectedTab(in: availableTabs)
            Task { await viewModel.load(using: env.apiClient) }
        }
        .onChange(of: viewModel.selectedTab) { _, _ in
            Task { await viewModel.load(using: env.apiClient) }
        }
        .sheet(item: $customerDetail) { customer in
            PartnerDetailSheet(
                title: "客户档案",
                name: customer.name,
                phone: customer.phone,
                contactName: customer.primaryContactName,
                contactPhone: customer.primaryContactPhone,
                groupName: customer.groupName,
                address: customer.address,
                notes: customer.notes,
                balance: customer.balance,
                levelText: customer.level.map { "客户等级 \($0)" },
                status: customer.status,
                tint: ZhihuijiTheme.ColorToken.success
            )
        }
        .sheet(item: $supplierDetail) { supplier in
            PartnerDetailSheet(
                title: "供应商档案",
                name: supplier.name,
                phone: supplier.phone,
                contactName: supplier.primaryContactName,
                contactPhone: supplier.primaryContactPhone,
                groupName: supplier.groupName,
                address: supplier.address,
                notes: supplier.notes,
                balance: supplier.balance,
                levelText: nil,
                status: supplier.status,
                tint: ZhihuijiTheme.ColorToken.warning
            )
        }
        .sheet(item: $viewModel.partnerEditor) { editor in
            PartnerEditorSheet(
                editor: editor,
                isSubmitting: viewModel.isEditorSubmitting,
                errorMessage: viewModel.editorErrorMessage
            ) {
                Task { await viewModel.submitPartnerEditor(client: env.apiClient) }
            }
        }
    }

    private var headerCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 6) {
                    Text("档案管理")
                        .font(ZhihuijiTheme.Typography.pageTitle)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                    Text("按移动端语义集中管理商品、客户和供应商。")
                        .font(ZhihuijiTheme.Typography.body)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                }
                Spacer()
                if viewModel.selectedTab == .products, actionPolicy.canCreateProduct {
                    NavigationLink {
                        ProductEditView()
                    } label: {
                        StatusChip(title: "新增商品", tint: ZhihuijiTheme.ColorToken.primary)
                    }
                    .buttonStyle(.plain)
                } else if actionPolicy.canCreateCustomer || actionPolicy.canCreateSupplier {
                    Button {
                        switch viewModel.selectedTab {
                        case .customers:
                            viewModel.beginCreateCustomer()
                        case .suppliers:
                            viewModel.beginCreateSupplier()
                        case .products:
                            break
                        }
                    } label: {
                        StatusChip(
                            title: viewModel.selectedTab == .customers ? "新增客户" : "新增供应商",
                            tint: viewModel.selectedTab == .customers ? ZhihuijiTheme.ColorToken.success : ZhihuijiTheme.ColorToken.warning
                        )
                    }
                    .buttonStyle(.plain)
                }
            }

            HStack(spacing: 10) {
                archiveBadge(title: "商品", enabled: availableTabs.contains(.products), tint: ZhihuijiTheme.ColorToken.primary)
                archiveBadge(title: "客户", enabled: availableTabs.contains(.customers), tint: ZhihuijiTheme.ColorToken.success)
                archiveBadge(title: "供应商", enabled: availableTabs.contains(.suppliers), tint: ZhihuijiTheme.ColorToken.warning)
            }
        }
        .padding(18)
        .glassCard()
    }

    private var tabStrip: some View {
        HStack(spacing: 10) {
            ForEach(availableTabs) { tab in
                Button {
                    viewModel.selectedTab = tab
                } label: {
                    Text(tab.title)
                        .font(ZhihuijiTheme.Typography.bodyMedium)
                        .foregroundStyle(viewModel.selectedTab == tab ? .white : ZhihuijiTheme.ColorToken.textPrimary)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .background(
                            (viewModel.selectedTab == tab
                                ? LinearGradient(colors: [ZhihuijiTheme.ColorToken.primaryBright, ZhihuijiTheme.ColorToken.primary], startPoint: .leading, endPoint: .trailing)
                                : LinearGradient(colors: [Color.white.opacity(0.58), Color.white.opacity(0.58)], startPoint: .leading, endPoint: .trailing)
                            ),
                            in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.cardSmall, style: .continuous)
                        )
                }
                .buttonStyle(.plain)
            }
        }
    }

    private var searchField: some View {
        VStack(alignment: .leading, spacing: 10) {
            TextField(viewModel.selectedTab.searchPlaceholder, text: $viewModel.keyword)
                .fieldBackground()
                .onSubmit {
                    Task { await viewModel.load(using: env.apiClient) }
                }

            if viewModel.selectedTab != .products {
                Picker("状态", selection: $viewModel.partnerStatusFilter) {
                    ForEach(ArchivePartnerStatusFilter.allCases) { filter in
                        Text(filter.title).tag(filter)
                    }
                }
                .pickerStyle(.segmented)
                .onChange(of: viewModel.partnerStatusFilter) { _, _ in
                    Task { await viewModel.load(using: env.apiClient) }
                }

                partnerGroupStrip
            }

            Button {
                Task { await viewModel.load(using: env.apiClient) }
            } label: {
                HStack {
                    Image(systemName: "magnifyingglass")
                    Text(viewModel.isLoading ? "搜索中..." : "刷新当前档案")
                        .font(ZhihuijiTheme.Typography.captionSemibold)
                }
                .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                .padding(.horizontal, 14)
                .padding(.vertical, 10)
                .background(Color.white.opacity(0.48), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.pill, style: .continuous))
            }
            .buttonStyle(.plain)
        }
    }

    @ViewBuilder
    private var partnerGroupStrip: some View {
        let groups = viewModel.currentPartnerGroups
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 10) {
                groupFilterChip(title: "全部分组", isSelected: viewModel.currentGroupFilter == nil) {
                    viewModel.clearCurrentGroupFilter()
                    Task { await viewModel.load(using: env.apiClient) }
                }
                ForEach(groups) { group in
                    groupFilterChip(title: group.name, isSelected: viewModel.currentGroupFilter == group.id) {
                        viewModel.selectCurrentGroupFilter(group.id)
                        Task { await viewModel.load(using: env.apiClient) }
                    }
                }
            }
            .padding(.vertical, 2)
        }
    }

    @ViewBuilder
    private var contentSection: some View {
        if let errorMessage = viewModel.errorMessage {
            EmptyStateView(title: "\(viewModel.selectedTab.title)读取失败", message: errorMessage)
        } else if viewModel.isEmpty {
            EmptyStateView(title: "暂无\(viewModel.selectedTab.title)", message: viewModel.selectedTab.emptyMessage)
        } else {
            LazyVStack(spacing: 12) {
                switch viewModel.selectedTab {
                case .products:
                    ForEach(viewModel.products) { product in
                        NavigationLink {
                            ProductDetailView(productId: product.id)
                        } label: {
                            VStack(alignment: .leading, spacing: 8) {
                                Text(product.name)
                                    .font(ZhihuijiTheme.Typography.bodyMedium)
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                                Text(product.code)
                                    .font(ZhihuijiTheme.Typography.captionSemibold)
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                                HStack {
                                    Text("库存 \(formattedQuantity(product.stock))")
                                    Spacer()
                                    Text("售价 \(product.salePrice.currencyText)")
                                }
                                .font(ZhihuijiTheme.Typography.body)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                            }
                            .padding(16)
                            .glassCard()
                        }
                        .buttonStyle(.plain)
                    }
                case .customers:
                    ForEach(viewModel.customers) { customer in
                        partnerRow(
                            detailAction: {
                                Task { customerDetail = await viewModel.fetchCustomerDetail(customer.id, using: env.apiClient) }
                            },
                            editAction: actionPolicy.canEditPartner ? {
                                viewModel.beginEditCustomer(customer)
                            } : nil
                        ) {
                            partnerCard(
                                title: customer.name,
                                subtitle: customer.phone,
                                contact: customer.primaryContactName,
                                secondary: customer.address,
                                groupName: customer.groupName,
                                balance: customer.balance,
                                status: customer.status,
                                tint: ZhihuijiTheme.ColorToken.success
                            )
                        }
                    }
                case .suppliers:
                    ForEach(viewModel.suppliers) { supplier in
                        partnerRow(
                            detailAction: {
                                Task { supplierDetail = await viewModel.fetchSupplierDetail(supplier.id, using: env.apiClient) }
                            },
                            editAction: actionPolicy.canEditPartner ? {
                                viewModel.beginEditSupplier(supplier)
                            } : nil
                        ) {
                            partnerCard(
                                title: supplier.name,
                                subtitle: supplier.phone,
                                contact: supplier.primaryContactName,
                                secondary: supplier.address,
                                groupName: supplier.groupName,
                                balance: supplier.balance,
                                status: supplier.status,
                                tint: ZhihuijiTheme.ColorToken.warning
                            )
                        }
                    }
                }
            }
        }
    }

    private func partnerRow<Content: View>(
        detailAction: @escaping () -> Void,
        editAction: (() -> Void)?,
        @ViewBuilder content: () -> Content
    ) -> some View {
        VStack(spacing: 8) {
            Button(action: detailAction) {
                content()
            }
            .buttonStyle(.plain)

            HStack(spacing: 10) {
                ArchiveSecondaryActionButton(
                    title: "查看详情",
                    systemImage: "eye.fill",
                    tint: ZhihuijiTheme.ColorToken.primary,
                    action: detailAction
                )
                if let editAction {
                    ArchiveSecondaryActionButton(
                        title: "编辑档案",
                        systemImage: "pencil.circle.fill",
                        tint: ZhihuijiTheme.ColorToken.primaryBright,
                        action: editAction
                    )
                }
            }
        }
    }

    private func partnerCard(
        title: String,
        subtitle: String,
        contact: String?,
        secondary: String?,
        groupName: String?,
        balance: Double?,
        status: Int?,
        tint: Color
    ) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(ZhihuijiTheme.Typography.bodyMedium)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                    Text(subtitle)
                        .font(ZhihuijiTheme.Typography.caption)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                }
                Spacer()
                StatusChip(
                    title: status == 1 ? "启用" : "停用",
                    tint: status == 1 ? tint : ZhihuijiTheme.ColorToken.warning
                )
            }

            if let groupName = groupName?.nilIfBlank {
                Text("分组 \(groupName)")
                    .font(ZhihuijiTheme.Typography.captionSemibold)
                    .foregroundStyle(tint)
            }
            if let contact = contact?.nilIfBlank {
                Text("联系人 \(contact)")
                    .font(ZhihuijiTheme.Typography.body)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
            }
            if let secondary = secondary?.nilIfBlank {
                Text(secondary)
                    .font(ZhihuijiTheme.Typography.caption)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            if let balance {
                Text("往来余额 \(balance.currencyText)")
                    .font(ZhihuijiTheme.Typography.bodyMedium)
                    .foregroundStyle(tint)
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

    private func archiveBadge(title: String, enabled: Bool, tint: Color) -> some View {
        Text(title)
            .font(ZhihuijiTheme.Typography.captionSemibold)
            .foregroundStyle(enabled ? tint : ZhihuijiTheme.ColorToken.textTertiary)
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(
                (enabled ? tint.opacity(0.12) : Color.white.opacity(0.40)),
                in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.pill, style: .continuous)
            )
            .overlay(
                RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.pill, style: .continuous)
                    .stroke(Color.white.opacity(0.45), lineWidth: ZhihuijiTheme.Stroke.hairline)
            )
    }

    private func groupFilterChip(title: String, isSelected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(ZhihuijiTheme.Typography.captionSemibold)
                .foregroundStyle(isSelected ? .white : ZhihuijiTheme.ColorToken.textPrimary)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(
                    (isSelected
                        ? LinearGradient(colors: [ZhihuijiTheme.ColorToken.primaryBright, ZhihuijiTheme.ColorToken.primary], startPoint: .leading, endPoint: .trailing)
                        : LinearGradient(colors: [Color.white.opacity(0.58), Color.white.opacity(0.58)], startPoint: .leading, endPoint: .trailing)
                    ),
                    in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.pill, style: .continuous)
                )
                .overlay(
                    RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.pill, style: .continuous)
                        .stroke(Color.white.opacity(0.45), lineWidth: ZhihuijiTheme.Stroke.hairline)
                )
        }
        .buttonStyle(.plain)
    }

    private func formattedQuantity(_ value: Double) -> String {
        if value.rounded() == value {
            return String(Int(value))
        }
        return String(format: "%.2f", value)
    }
}

@MainActor
final class ArchivesHomeViewModel: ObservableObject {
    @Published var selectedTab: ArchiveTab = .products
    @Published var keyword = ""
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var partnerStatusFilter: ArchivePartnerStatusFilter = .all
    @Published var products: [ProductRecord] = []
    @Published var customers: [CustomerRecord] = []
    @Published var suppliers: [SupplierRecord] = []
    @Published var customerGroups: [PartnerGroupRecord] = []
    @Published var supplierGroups: [PartnerGroupRecord] = []
    @Published var customerGroupFilter: EntityID?
    @Published var supplierGroupFilter: EntityID?
    @Published var partnerEditor: PartnerEditorState?
    @Published var isEditorSubmitting = false
    @Published var editorErrorMessage: String?

    var isEmpty: Bool {
        switch selectedTab {
        case .products:
            return products.isEmpty && !isLoading
        case .customers:
            return customers.isEmpty && !isLoading
        case .suppliers:
            return suppliers.isEmpty && !isLoading
        }
    }

    var currentPartnerGroups: [PartnerGroupRecord] {
        switch selectedTab {
        case .customers:
            return customerGroups
        case .suppliers:
            return supplierGroups
        case .products:
            return []
        }
    }

    var currentGroupFilter: EntityID? {
        switch selectedTab {
        case .customers:
            return customerGroupFilter
        case .suppliers:
            return supplierGroupFilter
        case .products:
            return nil
        }
    }

    func ensureSelectedTab(in tabs: [ArchiveTab]) {
        guard !tabs.isEmpty else { return }
        if !tabs.contains(selectedTab) {
            selectedTab = tabs[0]
        }
    }

    func load(using client: APIClient) async {
        isLoading = true
        defer { isLoading = false }
        do {
            switch selectedTab {
            case .products:
                products = try await client.fetchProducts(keyword: keyword.nilIfBlank, page: 1, size: 30)
            case .customers:
                async let customersTask = client.fetchCustomers(
                    keyword: keyword.nilIfBlank,
                    status: partnerStatusFilter.apiValue,
                    groupId: customerGroupFilter,
                    page: 1,
                    size: 30
                )
                async let groupsTask = client.fetchCustomerGroups()
                customers = try await customersTask
                customerGroups = try await groupsTask
                if let customerGroupFilter, customerGroups.contains(where: { $0.id == customerGroupFilter }) == false {
                    self.customerGroupFilter = nil
                }
            case .suppliers:
                async let suppliersTask = client.fetchSuppliers(
                    keyword: keyword.nilIfBlank,
                    status: partnerStatusFilter.apiValue,
                    groupId: supplierGroupFilter,
                    page: 1,
                    size: 30
                )
                async let groupsTask = client.fetchSupplierGroups()
                suppliers = try await suppliersTask
                supplierGroups = try await groupsTask
                if let supplierGroupFilter, supplierGroups.contains(where: { $0.id == supplierGroupFilter }) == false {
                    self.supplierGroupFilter = nil
                }
            }
            errorMessage = nil
        } catch {
            clearLoadedDataForSelectedTab()
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    private func clearLoadedDataForSelectedTab() {
        switch selectedTab {
        case .products:
            products = []
        case .customers:
            customers = []
            customerGroups = []
        case .suppliers:
            suppliers = []
            supplierGroups = []
        }
    }

    func fetchCustomerDetail(_ id: EntityID, using client: APIClient) async -> CustomerRecord? {
        do {
            errorMessage = nil
            return try await client.fetchCustomer(id: id)
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
            return nil
        }
    }

    func fetchSupplierDetail(_ id: EntityID, using client: APIClient) async -> SupplierRecord? {
        do {
            errorMessage = nil
            return try await client.fetchSupplier(id: id)
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
            return nil
        }
    }

    func beginCreateCustomer() {
        partnerEditor = PartnerEditorState(kind: .customers, mode: .create, groups: customerGroups)
        editorErrorMessage = nil
    }

    func beginEditCustomer(_ customer: CustomerRecord) {
        partnerEditor = PartnerEditorState(customer: customer, groups: customerGroups)
        editorErrorMessage = nil
    }

    func beginCreateSupplier() {
        partnerEditor = PartnerEditorState(kind: .suppliers, mode: .create, groups: supplierGroups)
        editorErrorMessage = nil
    }

    func beginEditSupplier(_ supplier: SupplierRecord) {
        partnerEditor = PartnerEditorState(supplier: supplier, groups: supplierGroups)
        editorErrorMessage = nil
    }

    func submitPartnerEditor(client: APIClient) async {
        guard let editor = partnerEditor else { return }
        guard let payload = editor.buildPayload() else {
            editorErrorMessage = editor.kind == .customers ? "请填写完整客户信息" : "请填写完整供应商信息"
            return
        }

        isEditorSubmitting = true
        defer { isEditorSubmitting = false }

        do {
            switch payload {
            case let .customer(request):
                let record: CustomerRecord
                if let id = editor.recordId {
                    record = try await client.updateCustomer(id: id, payload: request)
                } else {
                    record = try await client.createCustomer(payload: request)
                }
                upsertCustomer(record)
            case let .supplier(request):
                let record: SupplierRecord
                if let id = editor.recordId {
                    record = try await client.updateSupplier(id: id, payload: request)
                } else {
                    record = try await client.createSupplier(payload: request)
                }
                upsertSupplier(record)
            }
            editorErrorMessage = nil
            partnerEditor = nil
        } catch {
            editorErrorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    private func upsertCustomer(_ customer: CustomerRecord) {
        if let index = customers.firstIndex(where: { $0.id == customer.id }) {
            customers[index] = customer
        } else {
            customers.insert(customer, at: 0)
        }
    }

    private func upsertSupplier(_ supplier: SupplierRecord) {
        if let index = suppliers.firstIndex(where: { $0.id == supplier.id }) {
            suppliers[index] = supplier
        } else {
            suppliers.insert(supplier, at: 0)
        }
    }

    func selectCurrentGroupFilter(_ id: EntityID) {
        switch selectedTab {
        case .customers:
            customerGroupFilter = id
        case .suppliers:
            supplierGroupFilter = id
        case .products:
            break
        }
    }

    func clearCurrentGroupFilter() {
        switch selectedTab {
        case .customers:
            customerGroupFilter = nil
        case .suppliers:
            supplierGroupFilter = nil
        case .products:
            break
        }
    }
}

private struct PartnerDetailSheet: View {
    let title: String
    let name: String
    let phone: String
    let contactName: String?
    let contactPhone: String?
    let groupName: String?
    let address: String?
    let notes: String?
    let balance: Double?
    let levelText: String?
    let status: Int?
    let tint: Color
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    VStack(alignment: .leading, spacing: 10) {
                        HStack {
                            VStack(alignment: .leading, spacing: 6) {
                                Text(name)
                                    .font(ZhihuijiTheme.Typography.pageTitle)
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                                Text(phone)
                                    .font(ZhihuijiTheme.Typography.body)
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                            }
                            Spacer()
                            StatusChip(
                                title: status == 1 ? "启用" : "停用",
                                tint: status == 1 ? tint : ZhihuijiTheme.ColorToken.warning
                            )
                        }

                        if let balance {
                            Text("往来余额 \(balance.currencyText)")
                                .font(ZhihuijiTheme.Typography.bodyMedium)
                                .foregroundStyle(tint)
                        }
                    }
                    .padding(18)
                    .glassCard()

                    detailSection(
                        title: "基础信息",
                        rows: [
                            ("联系人", contactName),
                            ("联系电话", contactPhone),
                            ("分组", groupName),
                            ("等级", levelText),
                        ]
                    )

                    detailSection(
                        title: "联系与备注",
                        rows: [
                            ("地址", address),
                            ("备注", notes),
                        ]
                    )
                }
                .padding(20)
            }
            .navigationTitle(title)
            .toolbar {
                ToolbarItem {
                    Button("关闭") { dismiss() }
                }
            }
        }
        .presentationDetents([.medium, .large])
        .presentationDragIndicator(.visible)
    }

    @ViewBuilder
    private func detailSection(title: String, rows: [(String, String?)]) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(title)
                .font(ZhihuijiTheme.Typography.sectionTitle)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)

            VStack(spacing: 10) {
                ForEach(rows.filter { $0.1?.nilIfBlank != nil }, id: \.0) { row in
                    HStack(alignment: .top) {
                        Text(row.0)
                            .font(ZhihuijiTheme.Typography.captionSemibold)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                            .frame(width: 72, alignment: .leading)
                        Text(row.1 ?? "-")
                            .font(ZhihuijiTheme.Typography.body)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                            .fixedSize(horizontal: false, vertical: true)
                        Spacer()
                    }
                }
            }
            .padding(16)
            .glassCard()
        }
    }
}

enum PartnerEditorMode {
    case create
    case edit
}

enum PartnerEditorPayload {
    case customer(CustomerWritePayload)
    case supplier(SupplierWritePayload)
}

enum ArchivePartnerStatusFilter: String, CaseIterable, Identifiable {
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

@MainActor
final class PartnerEditorState: ObservableObject, Identifiable {
    let id = UUID()
    let kind: ArchiveTab
    let mode: PartnerEditorMode
    let recordId: EntityID?
    let groups: [PartnerGroupRecord]

    @Published var name: String
    @Published var phone: String
    @Published var levelText: String
    @Published var contactName: String
    @Published var contactPhone: String
    @Published var address: String
    @Published var notes: String
    @Published var balanceText: String
    @Published var selectedGroupId: EntityID?
    @Published var status: Int

    init(kind: ArchiveTab, mode: PartnerEditorMode, groups: [PartnerGroupRecord]) {
        self.kind = kind
        self.mode = mode
        self.recordId = nil
        self.groups = groups
        self.name = ""
        self.phone = ""
        self.levelText = "1"
        self.contactName = ""
        self.contactPhone = ""
        self.address = ""
        self.notes = ""
        self.balanceText = "0.00"
        self.selectedGroupId = nil
        self.status = 1
    }

    init(customer: CustomerRecord, groups: [PartnerGroupRecord]) {
        kind = .customers
        mode = .edit
        recordId = customer.id
        self.groups = groups
        name = customer.name
        phone = customer.phone
        levelText = String(customer.level ?? 1)
        contactName = customer.primaryContactName ?? ""
        contactPhone = customer.primaryContactPhone ?? ""
        address = customer.address ?? ""
        notes = customer.notes ?? ""
        balanceText = String(format: "%.2f", customer.balance ?? 0)
        selectedGroupId = customer.groupId
        status = customer.status ?? 1
    }

    init(supplier: SupplierRecord, groups: [PartnerGroupRecord]) {
        kind = .suppliers
        mode = .edit
        recordId = supplier.id
        self.groups = groups
        name = supplier.name
        phone = supplier.phone
        levelText = "1"
        contactName = supplier.primaryContactName ?? ""
        contactPhone = supplier.primaryContactPhone ?? ""
        address = supplier.address ?? ""
        notes = supplier.notes ?? ""
        balanceText = String(format: "%.2f", supplier.balance ?? 0)
        selectedGroupId = supplier.groupId
        status = supplier.status ?? 1
    }

    var title: String {
        switch (kind, mode) {
        case (.customers, .create): return "新建客户"
        case (.customers, .edit): return "编辑客户"
        case (.suppliers, .create): return "新建供应商"
        case (.suppliers, .edit): return "编辑供应商"
        default: return "编辑档案"
        }
    }

    var submitTitle: String {
        mode == .create ? "创建档案" : "保存修改"
    }

    func buildPayload() -> PartnerEditorPayload? {
        guard let name = name.nilIfBlank,
              let phone = phone.nilIfBlank,
              let balance = Double(balanceText) else {
            return nil
        }

        switch kind {
        case .customers:
            guard let level = Int(levelText) else { return nil }
            return .customer(
                CustomerWritePayload(
                    name: name,
                    phone: phone,
                    level: level,
                    groupId: selectedGroupId,
                    primaryContactName: contactName.nilIfBlank,
                    primaryContactPhone: contactPhone.nilIfBlank,
                    address: address.nilIfBlank,
                    notes: notes.nilIfBlank,
                    balance: balance,
                    status: status
                )
            )
        case .suppliers:
            return .supplier(
                SupplierWritePayload(
                    name: name,
                    phone: phone,
                    groupId: selectedGroupId,
                    primaryContactName: contactName.nilIfBlank,
                    primaryContactPhone: contactPhone.nilIfBlank,
                    address: address.nilIfBlank,
                    notes: notes.nilIfBlank,
                    balance: balance,
                    status: status
                )
            )
        case .products:
            return nil
        }
    }
}

private struct PartnerEditorSheet: View {
    @ObservedObject var editor: PartnerEditorState
    let isSubmitting: Bool
    let errorMessage: String?
    let onSave: () -> Void
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text(editor.title)
                        .font(ZhihuijiTheme.Typography.pageTitle)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)

                    if let errorMessage {
                        EmptyStateView(title: "档案保存失败", message: errorMessage)
                    }

                    VStack(alignment: .leading, spacing: 12) {
                        Text("基础信息")
                            .font(ZhihuijiTheme.Typography.sectionTitle)
                        TextField(editor.kind == .customers ? "客户名称" : "供应商名称", text: $editor.name)
                            .fieldBackground()
                        TextField("手机号", text: $editor.phone)
                            .fieldBackground()
                        if editor.kind == .customers {
                            TextField("客户等级", text: $editor.levelText)
                                .fieldBackground()
                        }
                        if !editor.groups.isEmpty {
                            Picker("分组", selection: $editor.selectedGroupId) {
                                Text("未分组").tag(Optional<EntityID>.none)
                                ForEach(editor.groups) { group in
                                    Text(group.name).tag(Optional(group.id))
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
                        TextField("联系人", text: $editor.contactName)
                            .fieldBackground()
                        TextField("联系电话", text: $editor.contactPhone)
                            .fieldBackground()
                        TextField("往来余额", text: $editor.balanceText)
                            .fieldBackground()
                        Picker("状态", selection: $editor.status) {
                            Text("停用").tag(0)
                            Text("启用").tag(1)
                        }
                        .pickerStyle(.segmented)
                    }
                    .padding(16)
                    .glassCard()

                    VStack(alignment: .leading, spacing: 12) {
                        Text("联系与备注")
                            .font(ZhihuijiTheme.Typography.sectionTitle)
                        TextField("地址", text: $editor.address, axis: .vertical)
                            .lineLimit(2 ... 4)
                            .fieldBackground()
                        TextField("备注", text: $editor.notes, axis: .vertical)
                            .lineLimit(2 ... 5)
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
                }
                .padding(20)
            }
            .toolbar {
                ToolbarItem {
                    Button("关闭") { dismiss() }
                }
            }
        }
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
    }
}

private struct ArchiveSecondaryActionButton: View {
    let title: String
    let systemImage: String
    let tint: Color
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                Image(systemName: systemImage)
                Text(title)
                    .font(ZhihuijiTheme.Typography.captionSemibold)
            }
            .foregroundStyle(tint)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 11)
            .background(tint.opacity(0.10), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.cardSmall, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.cardSmall, style: .continuous)
                    .stroke(Color.white.opacity(0.45), lineWidth: ZhihuijiTheme.Stroke.hairline)
            )
        }
        .buttonStyle(.plain)
    }
}
