import Foundation

final class APIClient {
    private let baseURL: URL
    private let tokenStore: AuthTokenStore
    private let session: URLSession
    private let decoder: JSONDecoder
    private let encoder: JSONEncoder
    private let refreshCoordinator = TokenRefreshCoordinator()

    init(baseURL: URL, tokenStore: AuthTokenStore, session: URLSession = .shared) {
        self.baseURL = baseURL
        self.tokenStore = tokenStore
        self.session = session
        self.decoder = JSONDecoder()
        self.encoder = JSONEncoder()
        decoder.keyDecodingStrategy = .convertFromSnakeCase
        encoder.keyEncodingStrategy = .convertToSnakeCase
    }

    func send<Response: Codable>(
        _ endpoint: APIEndpoint,
        method: String = "GET",
        body: Encodable? = nil,
        authorized: Bool = true,
        queryItems: [URLQueryItem] = []
    ) async throws -> Response {
        try await send(
            path: endpoint.path,
            method: method,
            body: body,
            authorized: authorized,
            queryItems: queryItems
        )
    }

    func send<Response: Codable>(
        path: String,
        method: String = "GET",
        body: Encodable? = nil,
        authorized: Bool = true,
        queryItems: [URLQueryItem] = []
    ) async throws -> Response {
        let (data, response) = try await performRequest(
            path: path,
            method: method,
            body: body,
            authorized: authorized,
            queryItems: queryItems,
            retryOnAuthFailure: authorized
        )
        return try decodeEnvelope(response: response, data: data)
    }

    func login(phone: String, password: String) async throws -> AuthPayload {
        try await send(
            .login,
            method: "POST",
            body: LoginRequest(phone: phone, password: password),
            authorized: false
        )
    }

    func refresh(refreshToken: String) async throws -> AuthPayload {
        try await send(
            .refresh,
            method: "POST",
            body: RefreshRequest(refreshToken: refreshToken),
            authorized: false
        )
    }

    func logout() async throws {
        let _: EmptyPayload = try await send(.logout, method: "POST")
    }

    func fetchCurrentUser() async throws -> UserProfile {
        try await send(.currentUser)
    }

    func fetchCurrentStore() async throws -> CurrentStoreProfile {
        try await send(.currentStore)
    }

    func fetchStoreMembers() async throws -> [StoreStaffMember] {
        try await send(.storeMembers)
    }

    func createStoreMember(_ payload: StoreMemberCreatePayload) async throws -> StoreStaffMember {
        try await send(.storeMembers, method: "POST", body: payload)
    }

    func updateStoreMember(userId: EntityID, payload: StoreMemberUpdatePayload) async throws -> StoreStaffMember {
        try await send(path: "/v2/stores/current/members/\(userId.rawValue)", method: "PUT", body: payload)
    }

    func fetchSalesSummary(startAt: Int64, endAt: Int64) async throws -> SalesSummaryReport {
        try await send(
            .dashboardSummary,
            queryItems: [
                URLQueryItem(name: "start_at", value: String(startAt)),
                URLQueryItem(name: "end_at", value: String(endAt)),
            ]
        )
    }

    func fetchSaleOrders(keyword: String? = nil, paymentStatus: Int? = nil, page: Int? = nil, size: Int? = nil) async throws -> [SalesOrderSummary] {
        var queryItems: [URLQueryItem] = []
        if let keyword, !keyword.isEmpty {
            queryItems.append(URLQueryItem(name: "keyword", value: keyword))
        }
        if let paymentStatus {
            queryItems.append(URLQueryItem(name: "payment_status", value: String(paymentStatus)))
        }
        if let page {
            queryItems.append(URLQueryItem(name: "page", value: String(page)))
        }
        if let size {
            queryItems.append(URLQueryItem(name: "size", value: String(size)))
        }
        return try await send(.saleOrders, queryItems: queryItems)
    }

    func fetchSaleOrder(id: EntityID) async throws -> SalesOrder {
        try await send(path: "/v2/sale-orders/\(id.rawValue)")
    }

    func createSaleOrder(payload: SaleOrderCreatePayload) async throws -> SalesOrder {
        try await send(path: "/v2/sale-orders", method: "POST", body: payload)
    }

    func fetchSaleOrderPayments(id: EntityID) async throws -> [SalePaymentRecord] {
        try await send(path: "/v2/sale-orders/\(id.rawValue)/payments")
    }

    func createSaleOrderPayment(id: EntityID, payload: SalePaymentCreatePayload) async throws -> SalePaymentRecord {
        try await send(path: "/v2/sale-orders/\(id.rawValue)/payments", method: "POST", body: payload)
    }

    func fetchSalesReturns(keyword: String? = nil, status: Int? = nil, page: Int? = nil, size: Int? = nil) async throws -> [SalesReturnRecord] {
        var queryItems: [URLQueryItem] = []
        if let keyword, !keyword.isEmpty {
            queryItems.append(URLQueryItem(name: "keyword", value: keyword))
        }
        if let status {
            queryItems.append(URLQueryItem(name: "status", value: String(status)))
        }
        if let page {
            queryItems.append(URLQueryItem(name: "page", value: String(page)))
        }
        if let size {
            queryItems.append(URLQueryItem(name: "size", value: String(size)))
        }
        return try await send(path: "/v2/sales-returns", queryItems: queryItems)
    }

    func fetchSalesReturn(id: EntityID) async throws -> SalesReturnRecord {
        try await send(path: "/v2/sales-returns/\(id.rawValue)")
    }

    func fetchSalesReturnsByOrder(orderId: EntityID) async throws -> [SalesReturnRecord] {
        try await send(path: "/v2/sales-returns/by-order/\(orderId.rawValue)")
    }

    func createSalesReturn(payload: SalesReturnCreatePayload) async throws -> SalesReturnRecord {
        try await send(path: "/v2/sales-returns", method: "POST", body: payload)
    }

    func updateSalesReturnDraft(id: EntityID, payload: SalesReturnDraftPayload) async throws -> SalesReturnRecord {
        try await send(path: "/v2/sales-returns/\(id.rawValue)/draft", method: "PUT", body: payload)
    }

    func confirmSalesReturn(id: EntityID, payload: SalesReturnConfirmPayload) async throws -> SalesReturnRecord {
        try await send(path: "/v2/sales-returns/\(id.rawValue)/confirm", method: "PUT", body: payload)
    }

    func addSalesReturnRefund(id: EntityID, payload: SalesReturnRefundPayload) async throws -> SalesReturnRecord {
        try await send(path: "/v2/sales-returns/\(id.rawValue)/refunds", method: "POST", body: payload)
    }

    func cancelSalesReturn(id: EntityID) async throws -> SalesReturnRecord {
        try await send(path: "/v2/sales-returns/\(id.rawValue)/cancel", method: "PUT")
    }

    func fetchPurchaseOrders(keyword: String? = nil, status: Int? = nil, page: Int? = nil, size: Int? = nil) async throws -> [PurchaseOrderSummary] {
        var queryItems: [URLQueryItem] = []
        if let keyword, !keyword.isEmpty {
            queryItems.append(URLQueryItem(name: "keyword", value: keyword))
        }
        if let status {
            queryItems.append(URLQueryItem(name: "status", value: String(status)))
        }
        if let page {
            queryItems.append(URLQueryItem(name: "page", value: String(page)))
        }
        if let size {
            queryItems.append(URLQueryItem(name: "size", value: String(size)))
        }
        return try await send(.purchaseOrders, queryItems: queryItems)
    }

    func fetchPurchaseOrder(id: EntityID) async throws -> PurchaseOrder {
        try await send(path: "/v2/purchase-orders/\(id.rawValue)")
    }

    func createPurchaseOrder(payload: PurchaseOrderCreatePayload) async throws -> PurchaseOrder {
        try await send(path: "/v2/purchase-orders", method: "POST", body: payload)
    }

    func fetchPurchaseReceipts(keyword: String? = nil, status: Int? = nil, page: Int? = nil, size: Int? = nil) async throws -> [PurchaseReceiptRecord] {
        var queryItems: [URLQueryItem] = []
        if let keyword, !keyword.isEmpty { queryItems.append(URLQueryItem(name: "keyword", value: keyword)) }
        if let status { queryItems.append(URLQueryItem(name: "status", value: String(status))) }
        if let page { queryItems.append(URLQueryItem(name: "page", value: String(page))) }
        if let size { queryItems.append(URLQueryItem(name: "size", value: String(size))) }
        return try await send(path: "/v2/purchase-receipts", queryItems: queryItems)
    }

    func fetchPurchaseReceipt(id: EntityID) async throws -> PurchaseReceiptRecord {
        try await send(path: "/v2/purchase-receipts/\(id.rawValue)")
    }

    func fetchPurchaseReceiptsByOrder(orderId: EntityID) async throws -> [PurchaseReceiptRecord] {
        try await send(path: "/v2/purchase-receipts/by-order/\(orderId.rawValue)")
    }

    func createPurchaseReceipt(payload: PurchaseReceiptCreatePayload) async throws -> PurchaseReceiptRecord {
        try await send(path: "/v2/purchase-receipts", method: "POST", body: payload)
    }

    func updatePurchaseReceiptDraft(id: EntityID, payload: PurchaseReceiptDraftPayload) async throws -> PurchaseReceiptRecord {
        try await send(path: "/v2/purchase-receipts/\(id.rawValue)/draft", method: "PUT", body: payload)
    }

    func confirmPurchaseReceipt(id: EntityID) async throws -> PurchaseReceiptRecord {
        try await send(path: "/v2/purchase-receipts/\(id.rawValue)/confirm", method: "PUT")
    }

    func cancelPurchaseReceipt(id: EntityID) async throws -> PurchaseReceiptRecord {
        try await send(path: "/v2/purchase-receipts/\(id.rawValue)/cancel", method: "PUT")
    }

    func fetchPurchaseReturns(keyword: String? = nil, status: Int? = nil, page: Int? = nil, size: Int? = nil) async throws -> [PurchaseReturnRecord] {
        var queryItems: [URLQueryItem] = []
        if let keyword, !keyword.isEmpty { queryItems.append(URLQueryItem(name: "keyword", value: keyword)) }
        if let status { queryItems.append(URLQueryItem(name: "status", value: String(status))) }
        if let page { queryItems.append(URLQueryItem(name: "page", value: String(page))) }
        if let size { queryItems.append(URLQueryItem(name: "size", value: String(size))) }
        return try await send(path: "/v2/purchase-returns", queryItems: queryItems)
    }

    func fetchPurchaseReturn(id: EntityID) async throws -> PurchaseReturnRecord {
        try await send(path: "/v2/purchase-returns/\(id.rawValue)")
    }

    func fetchPurchaseReturnsByOrder(orderId: EntityID) async throws -> [PurchaseReturnRecord] {
        try await send(path: "/v2/purchase-returns/by-order/\(orderId.rawValue)")
    }

    func createPurchaseReturn(payload: PurchaseReturnCreatePayload) async throws -> PurchaseReturnRecord {
        try await send(path: "/v2/purchase-returns", method: "POST", body: payload)
    }

    func updatePurchaseReturnDraft(id: EntityID, payload: PurchaseReturnDraftPayload) async throws -> PurchaseReturnRecord {
        try await send(path: "/v2/purchase-returns/\(id.rawValue)/draft", method: "PUT", body: payload)
    }

    func confirmPurchaseReturn(id: EntityID, payload: PurchaseReturnConfirmPayload) async throws -> PurchaseReturnRecord {
        try await send(path: "/v2/purchase-returns/\(id.rawValue)/confirm", method: "PUT", body: payload)
    }

    func addPurchaseReturnRefund(id: EntityID, payload: PurchaseReturnRefundPayload) async throws -> PurchaseReturnRecord {
        try await send(path: "/v2/purchase-returns/\(id.rawValue)/refunds", method: "POST", body: payload)
    }

    func cancelPurchaseReturn(id: EntityID) async throws -> PurchaseReturnRecord {
        try await send(path: "/v2/purchase-returns/\(id.rawValue)/cancel", method: "PUT")
    }

    func fetchProducts(keyword: String? = nil, page: Int? = nil, size: Int? = nil) async throws -> [ProductRecord] {
        var queryItems: [URLQueryItem] = []
        if let keyword, !keyword.isEmpty {
            queryItems.append(URLQueryItem(name: "keyword", value: keyword))
        }
        if let page {
            queryItems.append(URLQueryItem(name: "page", value: String(page)))
        }
        if let size {
            queryItems.append(URLQueryItem(name: "size", value: String(size)))
        }
        return try await send(.products, queryItems: queryItems)
    }

    func fetchProduct(id: EntityID) async throws -> ProductRecord {
        try await send(path: "/v2/products/\(id.rawValue)")
    }

    func createProduct(payload: ProductWritePayload) async throws -> ProductRecord {
        try await send(path: "/v2/products", method: "POST", body: payload)
    }

    func updateProduct(id: EntityID, payload: ProductWritePayload) async throws -> ProductRecord {
        try await send(path: "/v2/products/\(id.rawValue)", method: "PUT", body: payload)
    }

    func fetchProductCategories() async throws -> [ProductCategoryRecord] {
        try await send(path: "/v2/product-categories")
    }

    func fetchProductUnits() async throws -> [ProductUnitRecord] {
        try await send(path: "/v2/product-units")
    }

    func fetchProductPriceLevels() async throws -> [ProductPriceLevelRecord] {
        try await send(path: "/v2/product-price-levels")
    }

    func fetchCustomers(keyword: String? = nil, status: Int? = nil, groupId: EntityID? = nil, page: Int? = nil, size: Int? = nil) async throws -> [CustomerRecord] {
        var queryItems: [URLQueryItem] = []
        if let keyword, !keyword.isEmpty { queryItems.append(URLQueryItem(name: "keyword", value: keyword)) }
        if let status { queryItems.append(URLQueryItem(name: "status", value: String(status))) }
        if let groupId { queryItems.append(URLQueryItem(name: "group_id", value: groupId.rawValue)) }
        if let page { queryItems.append(URLQueryItem(name: "page", value: String(page))) }
        if let size { queryItems.append(URLQueryItem(name: "size", value: String(size))) }
        return try await send(path: "/v2/customers", queryItems: queryItems)
    }

    func fetchCustomer(id: EntityID) async throws -> CustomerRecord {
        try await send(path: "/v2/customers/\(id.rawValue)")
    }

    func createCustomer(payload: CustomerWritePayload) async throws -> CustomerRecord {
        try await send(path: "/v2/customers", method: "POST", body: payload)
    }

    func updateCustomer(id: EntityID, payload: CustomerWritePayload) async throws -> CustomerRecord {
        try await send(path: "/v2/customers/\(id.rawValue)", method: "PUT", body: payload)
    }

    func fetchCustomerGroups() async throws -> [PartnerGroupRecord] {
        try await send(path: "/v2/customer-groups")
    }

    func fetchSuppliers(keyword: String? = nil, status: Int? = nil, groupId: EntityID? = nil, page: Int? = nil, size: Int? = nil) async throws -> [SupplierRecord] {
        var queryItems: [URLQueryItem] = []
        if let keyword, !keyword.isEmpty { queryItems.append(URLQueryItem(name: "keyword", value: keyword)) }
        if let status { queryItems.append(URLQueryItem(name: "status", value: String(status))) }
        if let groupId { queryItems.append(URLQueryItem(name: "group_id", value: groupId.rawValue)) }
        if let page { queryItems.append(URLQueryItem(name: "page", value: String(page))) }
        if let size { queryItems.append(URLQueryItem(name: "size", value: String(size))) }
        return try await send(path: "/v2/suppliers", queryItems: queryItems)
    }

    func fetchSupplier(id: EntityID) async throws -> SupplierRecord {
        try await send(path: "/v2/suppliers/\(id.rawValue)")
    }

    func createSupplier(payload: SupplierWritePayload) async throws -> SupplierRecord {
        try await send(path: "/v2/suppliers", method: "POST", body: payload)
    }

    func updateSupplier(id: EntityID, payload: SupplierWritePayload) async throws -> SupplierRecord {
        try await send(path: "/v2/suppliers/\(id.rawValue)", method: "PUT", body: payload)
    }

    func fetchSupplierGroups() async throws -> [PartnerGroupRecord] {
        try await send(path: "/v2/supplier-groups")
    }

    func fetchFinanceRecords(keyword: String? = nil, type: Int? = nil, page: Int? = nil, size: Int? = nil) async throws -> [FinanceRecord] {
        var queryItems: [URLQueryItem] = []
        if let keyword, !keyword.isEmpty {
            queryItems.append(URLQueryItem(name: "keyword", value: keyword))
        }
        if let type {
            queryItems.append(URLQueryItem(name: "type", value: String(type)))
        }
        if let page {
            queryItems.append(URLQueryItem(name: "page", value: String(page)))
        }
        if let size {
            queryItems.append(URLQueryItem(name: "size", value: String(size)))
        }
        return try await send(path: "/v1/finance-records", queryItems: queryItems)
    }

    func createFinanceRecord(payload: FinanceRecordCreatePayload) async throws -> FinanceRecord {
        try await send(path: "/v1/finance-records", method: "POST", body: payload)
    }

    func fetchPayOrders(keyword: String? = nil, status: Int? = nil, page: Int? = nil, size: Int? = nil) async throws -> [PayOrder] {
        var queryItems: [URLQueryItem] = []
        if let keyword, !keyword.isEmpty {
            queryItems.append(URLQueryItem(name: "keyword", value: keyword))
        }
        if let status {
            queryItems.append(URLQueryItem(name: "status", value: String(status)))
        }
        if let page {
            queryItems.append(URLQueryItem(name: "page", value: String(page)))
        }
        if let size {
            queryItems.append(URLQueryItem(name: "size", value: String(size)))
        }
        return try await send(path: "/v2/pay-orders", queryItems: queryItems)
    }

    func fetchPayOrder(id: EntityID) async throws -> PayOrder {
        try await send(path: "/v2/pay-orders/\(id.rawValue)")
    }

    func createPayOrder(payload: PayOrderCreatePayload) async throws -> PayOrder {
        try await send(path: "/v2/pay-orders", method: "POST", body: payload)
    }

    func updatePayOrderStatus(id: EntityID, status: Int) async throws -> PayOrder {
        try await send(path: "/v2/pay-orders/\(id.rawValue)/status", method: "PUT", body: PayOrderStatusPayload(status: status))
    }

    func fetchSalesTrend(startAt: Int64, endAt: Int64, bucket: String) async throws -> [SalesTrendPoint] {
        try await send(
            .reports,
            queryItems: [
                URLQueryItem(name: "start_at", value: String(startAt)),
                URLQueryItem(name: "end_at", value: String(endAt)),
                URLQueryItem(name: "bucket", value: bucket),
            ]
        )
    }

    func fetchProfitSummary(startAt: Int64, endAt: Int64) async throws -> ProfitSummaryReport {
        try await send(path: "/v1/reports/profit-summary", queryItems: [
            URLQueryItem(name: "start_at", value: String(startAt)),
            URLQueryItem(name: "end_at", value: String(endAt)),
        ])
    }

    func fetchRefundRecords(startAt: Int64, endAt: Int64, limit: Int = 10) async throws -> [RefundRecordReport] {
        try await send(path: "/v1/reports/refund-records", queryItems: [
            URLQueryItem(name: "start_at", value: String(startAt)),
            URLQueryItem(name: "end_at", value: String(endAt)),
            URLQueryItem(name: "limit", value: String(limit)),
        ])
    }

    func fetchStockOutRecords(startAt: Int64, endAt: Int64, limit: Int = 10) async throws -> [StockOutRecordReport] {
        try await send(path: "/v1/reports/stock-out-records", queryItems: [
            URLQueryItem(name: "start_at", value: String(startAt)),
            URLQueryItem(name: "end_at", value: String(endAt)),
            URLQueryItem(name: "limit", value: String(limit)),
        ])
    }

    func fetchTopProducts(startAt: Int64, endAt: Int64, limit: Int = 10) async throws -> [TopSellingProductReport] {
        try await send(path: "/v1/reports/top-products", queryItems: [
            URLQueryItem(name: "start_at", value: String(startAt)),
            URLQueryItem(name: "end_at", value: String(endAt)),
            URLQueryItem(name: "limit", value: String(limit)),
        ])
    }

    func fetchProfitByProducts(startAt: Int64, endAt: Int64, limit: Int = 10) async throws -> [ProfitByProductReport] {
        try await send(path: "/v1/reports/profit-by-products", queryItems: [
            URLQueryItem(name: "start_at", value: String(startAt)),
            URLQueryItem(name: "end_at", value: String(endAt)),
            URLQueryItem(name: "limit", value: String(limit)),
        ])
    }

    func fetchProfitByCustomers(startAt: Int64, endAt: Int64, limit: Int = 10) async throws -> [ProfitByCustomerReport] {
        try await send(path: "/v1/reports/profit-by-customers", queryItems: [
            URLQueryItem(name: "start_at", value: String(startAt)),
            URLQueryItem(name: "end_at", value: String(endAt)),
            URLQueryItem(name: "limit", value: String(limit)),
        ])
    }

    func fetchInventoryFlowReport(startAt: Int64, endAt: Int64, limit: Int = 10) async throws -> [InventoryFlowRecordReport] {
        try await send(path: "/v1/reports/inventory-flow", queryItems: [
            URLQueryItem(name: "start_at", value: String(startAt)),
            URLQueryItem(name: "end_at", value: String(endAt)),
            URLQueryItem(name: "limit", value: String(limit)),
        ])
    }

    func fetchCustomerSalesReport(startAt: Int64, endAt: Int64, limit: Int = 10) async throws -> [CustomerSalesReport] {
        try await send(path: "/v1/reports/customer-sales", queryItems: [
            URLQueryItem(name: "start_at", value: String(startAt)),
            URLQueryItem(name: "end_at", value: String(endAt)),
            URLQueryItem(name: "limit", value: String(limit)),
        ])
    }

    func fetchTopReceivableCustomers(limit: Int = 10) async throws -> [CustomerReceivableReport] {
        try await send(path: "/v1/reports/top-receivable-customers", queryItems: [
            URLQueryItem(name: "limit", value: String(limit)),
        ])
    }

    func fetchLowStockProducts(limit: Int = 10) async throws -> [LowStockProductReport] {
        try await send(path: "/v1/reports/low-stock-products", queryItems: [
            URLQueryItem(name: "limit", value: String(limit)),
        ])
    }

    func fetchReconciliationSummary(startAt: Int64, endAt: Int64) async throws -> ReconciliationSummaryReport {
        try await send(path: "/v1/reports/reconciliation-summary", queryItems: [
            URLQueryItem(name: "start_at", value: String(startAt)),
            URLQueryItem(name: "end_at", value: String(endAt)),
        ])
    }

    func fetchCashflowSummary(startAt: Int64, endAt: Int64) async throws -> CashflowSummaryReport {
        try await send(path: "/v1/reports/cashflow-summary", queryItems: [
            URLQueryItem(name: "start_at", value: String(startAt)),
            URLQueryItem(name: "end_at", value: String(endAt)),
        ])
    }

    func fetchInventorySnapshots(snapshotDate: Int64? = nil, startDate: Int64? = nil, endDate: Int64? = nil, page: Int? = nil, size: Int? = nil) async throws -> [InventorySnapshotSummary] {
        var queryItems: [URLQueryItem] = []
        if let snapshotDate {
            queryItems.append(URLQueryItem(name: "snapshotDate", value: String(snapshotDate)))
        }
        if let startDate {
            queryItems.append(URLQueryItem(name: "startDate", value: String(startDate)))
        }
        if let endDate {
            queryItems.append(URLQueryItem(name: "endDate", value: String(endDate)))
        }
        if let page {
            queryItems.append(URLQueryItem(name: "page", value: String(page)))
        }
        if let size {
            queryItems.append(URLQueryItem(name: "size", value: String(size)))
        }
        return try await send(path: "/v2/inventory/snapshots", queryItems: queryItems)
    }

    func fetchInventoryMonthlyStats(year: Int, month: Int, page: Int? = nil, size: Int? = nil) async throws -> [InventoryMonthlyStats] {
        var queryItems = [
            URLQueryItem(name: "year", value: String(year)),
            URLQueryItem(name: "month", value: String(month)),
        ]
        if let page {
            queryItems.append(URLQueryItem(name: "page", value: String(page)))
        }
        if let size {
            queryItems.append(URLQueryItem(name: "size", value: String(size)))
        }
        return try await send(path: "/v2/inventory/monthly-stats", queryItems: queryItems)
    }

    func fetchInventoryLedger(productId: EntityID? = nil, startAt: Int64? = nil, endAt: Int64? = nil, page: Int? = nil, size: Int? = nil) async throws -> [InventoryLedgerEntry] {
        var queryItems: [URLQueryItem] = []
        if let productId { queryItems.append(URLQueryItem(name: "productId", value: productId.rawValue)) }
        if let startAt { queryItems.append(URLQueryItem(name: "startAt", value: String(startAt))) }
        if let endAt { queryItems.append(URLQueryItem(name: "endAt", value: String(endAt))) }
        if let page { queryItems.append(URLQueryItem(name: "page", value: String(page))) }
        if let size { queryItems.append(URLQueryItem(name: "size", value: String(size))) }
        return try await send(path: "/v2/inventory/ledger", queryItems: queryItems)
    }

    func createInventoryLedgerEntry(payload: InventoryLedgerCreatePayload) async throws -> InventoryLedgerEntry {
        try await send(path: "/v2/inventory/ledger", method: "POST", body: payload)
    }

    func createInventorySnapshot(payload: InventorySnapshotCreatePayload) async throws -> InventorySnapshotSummary {
        try await send(path: "/v2/inventory/snapshots", method: "POST", body: payload)
    }

    func fetchMediaAssets() async throws -> [MediaAssetRecord] {
        try await send(.mediaAssets)
    }

    func fetchMediaAsset(id: EntityID) async throws -> MediaAssetRecord {
        try await send(path: "/v2/media/assets/\(id.rawValue)")
    }

    func createMediaAsset(payload: MediaAssetCreatePayload) async throws -> MediaAssetRecord {
        try await send(path: "/v2/media/assets", method: "POST", body: payload)
    }

    func deleteMediaAsset(id: EntityID) async throws {
        let _: EmptyPayload = try await send(path: "/v2/media/assets/\(id.rawValue)", method: "DELETE")
    }

    func fetchMediaBindings(targetType: String, targetId: EntityID) async throws -> [MediaBindingRecord] {
        try await send(
            .mediaBindings,
            queryItems: [
                URLQueryItem(name: "target_type", value: targetType),
                URLQueryItem(name: "target_id", value: targetId.rawValue),
            ]
        )
    }

    func createMediaBinding(payload: MediaBindingCreatePayload) async throws -> MediaBindingRecord {
        try await send(path: "/v2/media/bindings", method: "POST", body: payload)
    }

    func deleteMediaBinding(id: EntityID) async throws {
        let _: EmptyPayload = try await send(path: "/v2/media/bindings/\(id.rawValue)", method: "DELETE")
    }

    func fetchSyncHealth() async throws -> SyncHealthRecord {
        try await send(.syncHealth)
    }

    func fetchSyncCursor(clientId: String) async throws -> SyncCursorRecord {
        try await send(path: "/v2/sync/cursor/\(clientId)")
    }

    func acknowledgeSyncCursor(payload: SyncCursorAckPayload) async throws -> SyncCursorRecord {
        try await send(path: "/v2/sync/cursor/ack", method: "POST", body: payload)
    }

    func uploadSyncChanges(payload: SyncUploadPayload) async throws -> SyncUploadResponse {
        try await send(path: "/v2/sync/upload", method: "POST", body: payload)
    }

    func pullSyncChanges(payload: SyncPullPayload) async throws -> SyncPullResponse {
        try await send(path: "/v2/sync/pull", method: "POST", body: payload)
    }

    func fetchImportJobs(status: String? = nil) async throws -> [ImportJobRecord] {
        var queryItems: [URLQueryItem] = []
        if let status, !status.isEmpty {
            queryItems.append(URLQueryItem(name: "status", value: status))
        }
        return try await send(.importJobs, queryItems: queryItems)
    }

    func fetchImportJob(id: EntityID) async throws -> ImportJobRecord {
        try await send(path: "/v2/import-jobs/\(id.rawValue)")
    }

    func createImportJob(payload: ImportJobCreatePayload) async throws -> ImportJobRecord {
        try await send(path: "/v2/import-jobs", method: "POST", body: payload)
    }

    func retryImportJob(id: EntityID, payload: ImportJobRetryPayload? = nil) async throws -> ImportJobRecord {
        if let payload {
            return try await send(path: "/v2/import-jobs/\(id.rawValue)/retry", method: "POST", body: payload)
        }
        return try await send(path: "/v2/import-jobs/\(id.rawValue)/retry", method: "POST")
    }

    func cancelImportJob(id: EntityID) async throws -> ImportJobRecord {
        try await send(path: "/v2/import-jobs/\(id.rawValue)/cancel", method: "POST")
    }

    func importLegacySQLite(payload: LegacySQLiteImportPayload) async throws -> LegacySQLiteImportResult {
        try await send(path: "/v2/import-jobs/legacy-sqlite", method: "POST", body: payload)
    }

    func fetchAgentWorkbench() async throws -> AgentWorkbench {
        try await send(.agentWorkbench)
    }

    func fetchAgentConversations(page: Int? = nil, limit: Int? = nil) async throws -> [AgentConversationSummary] {
        var queryItems: [URLQueryItem] = []
        if let page {
            queryItems.append(URLQueryItem(name: "page", value: String(page)))
        }
        if let limit {
            queryItems.append(URLQueryItem(name: "limit", value: String(limit)))
        }
        return try await send(path: "/v2/agent/conversations", queryItems: queryItems)
    }

    func createAgentConversation(title: String, status: String? = nil) async throws -> AgentConversationSummary {
        struct Payload: Codable {
            let title: String
            let status: String?
        }
        return try await send(path: "/v2/agent/conversations", method: "POST", body: Payload(title: title, status: status))
    }

    func updateAgentConversation(id: EntityID, title: String? = nil, status: String? = nil) async throws -> AgentConversationSummary {
        struct Payload: Codable {
            let title: String?
            let status: String?
        }
        return try await send(path: "/v2/agent/conversations/\(id.rawValue)", method: "PUT", body: Payload(title: title, status: status))
    }

    func deleteAgentConversation(id: EntityID) async throws {
        let _: EmptyPayload = try await send(path: "/v2/agent/conversations/\(id.rawValue)", method: "DELETE")
    }

    func fetchAgentMessages(conversationId: EntityID, page: Int? = nil, limit: Int? = nil) async throws -> [AgentMessage] {
        var queryItems: [URLQueryItem] = []
        if let page {
            queryItems.append(URLQueryItem(name: "page", value: String(page)))
        }
        if let limit {
            queryItems.append(URLQueryItem(name: "limit", value: String(limit)))
        }
        return try await send(path: "/v2/agent/conversations/\(conversationId.rawValue)/messages", queryItems: queryItems)
    }

    func fetchAgentDrafts(conversationId: EntityID? = nil, page: Int? = nil, limit: Int? = nil) async throws -> [AgentDraft] {
        var queryItems: [URLQueryItem] = []
        if let conversationId {
            queryItems.append(URLQueryItem(name: "conversation_id", value: conversationId.rawValue))
        }
        if let page {
            queryItems.append(URLQueryItem(name: "page", value: String(page)))
        }
        if let limit {
            queryItems.append(URLQueryItem(name: "limit", value: String(limit)))
        }
        return try await send(path: "/v2/agent/drafts", queryItems: queryItems)
    }

    func createAgentDraft(payload: AgentDraftCreatePayload) async throws -> AgentDraft {
        try await send(path: "/v2/agent/drafts", method: "POST", body: payload)
    }

    func updateAgentDraft(id: EntityID, payload: AgentDraftUpdatePayload) async throws -> AgentDraft {
        try await send(path: "/v2/agent/drafts/\(id.rawValue)", method: "PUT", body: payload)
    }

    func deleteAgentDraft(id: EntityID) async throws {
        let _: EmptyPayload = try await send(path: "/v2/agent/drafts/\(id.rawValue)", method: "DELETE")
    }

    func fetchAgentTasks() async throws -> [AgentTask] {
        try await send(path: "/v2/agent/tasks")
    }

    func fetchAgentNotifications(unreadOnly: Bool? = nil) async throws -> [AgentNotification] {
        var queryItems: [URLQueryItem] = []
        if let unreadOnly {
            queryItems.append(URLQueryItem(name: "unread_only", value: unreadOnly ? "true" : "false"))
        }
        return try await send(path: "/v2/agent/notifications", queryItems: queryItems)
    }

    func markAgentNotificationRead(id: EntityID) async throws -> AgentNotificationReadResponse {
        try await send(path: "/v2/agent/notifications/\(id.rawValue)/read", method: "POST")
    }

    func chatWithAgent(conversationId: EntityID?, message: String, stream: Bool = false) async throws -> AgentChatResponse {
        try await send(
            path: "/v2/agent/chat",
            method: "POST",
            body: AgentChatPayload(conversationId: conversationId, message: message, stream: stream)
        )
    }

    func cancelAgentRun(runId: String) async throws -> AgentRunCancelResponse {
        try await send(path: "/v2/agent/runs/\(runId)/cancel", method: "POST")
    }

    func fetchAgentRunAudit(runId: String) async throws -> AgentRunAudit {
        try await send(path: "/v2/agent/runs/\(runId)/audit")
    }

    func streamAgentChat(conversationId: EntityID?, message: String) throws -> AsyncThrowingStream<AgentStreamEvent, Error> {
        let request = try makeRequest(
            path: "/v2/agent/chat/stream",
            method: "POST",
            body: AgentChatPayload(conversationId: conversationId, message: message, stream: true),
            authorized: true,
            queryItems: []
        )

        return AsyncThrowingStream { continuation in
            let task = Task {
                do {
                    var request = request
                    request.setValue("text/event-stream", forHTTPHeaderField: "Accept")
                    let (bytes, response) = try await session.bytes(for: request)
                    try self.validate(response: response, data: Data())

                    var dataLines: [String] = []

                    func flushEvent() throws {
                        guard !dataLines.isEmpty else { return }
                        let payloadText = dataLines.joined(separator: "\n")
                        dataLines.removeAll(keepingCapacity: true)
                        guard let data = payloadText.data(using: .utf8) else { return }
                        let event = try self.decoder.decode(AgentStreamEvent.self, from: data)
                        continuation.yield(event)
                    }

                    for try await rawLine in bytes.lines {
                        if Task.isCancelled {
                            break
                        }
                        if rawLine.isEmpty {
                            try flushEvent()
                            continue
                        }
                        if rawLine.hasPrefix(":") {
                            continue
                        }
                        if rawLine.hasPrefix("data:") {
                            let content = String(rawLine.dropFirst(5)).trimmingCharacters(in: .whitespaces)
                            dataLines.append(content)
                        }
                    }

                    try flushEvent()
                    continuation.finish()
                } catch {
                    continuation.finish(throwing: error)
                }
            }

            continuation.onTermination = { @Sendable _ in
                task.cancel()
            }
        }
    }

    private func decodeEnvelope<Response: Codable>(response: URLResponse, data: Data) throws -> Response {
        try validate(response: response, data: data)
        let payload: APIEnvelope<Response>
        do {
            payload = try decoder.decode(APIEnvelope<Response>.self, from: data)
        } catch {
            throw APIError.decoding(error.localizedDescription)
        }
        if payload.code != 0 {
            throw APIError.server(
                status: (response as? HTTPURLResponse)?.statusCode ?? -1,
                message: payload.message ?? "服务端返回错误"
            )
        }
        return payload.data
    }

    private func performRequest(
        path: String,
        method: String,
        body: Encodable?,
        authorized: Bool,
        queryItems: [URLQueryItem],
        retryOnAuthFailure: Bool
    ) async throws -> (Data, URLResponse) {
        let request = try makeRequest(
            path: path,
            method: method,
            body: body,
            authorized: authorized,
            queryItems: queryItems
        )
        let (data, response) = try await session.data(for: request)

        if retryOnAuthFailure,
           authorized,
           let http = response as? HTTPURLResponse,
           http.statusCode == 401 {
            try await refreshAccessTokenIfNeeded()
            let retriedRequest = try makeRequest(
                path: path,
                method: method,
                body: body,
                authorized: authorized,
                queryItems: queryItems
            )
            return try await session.data(for: retriedRequest)
        }

        return (data, response)
    }

    private func refreshAccessTokenIfNeeded() async throws {
        guard let refreshToken = tokenStore.readRefreshToken()?.nilIfBlank else {
            NotificationCenter.default.post(name: .zhihuijiUnauthorized, object: nil)
            throw APIError.unauthorized
        }

        let payload = try await refreshCoordinator.refresh { [self] in
            let request = try makeRequest(
                path: APIEndpoint.refresh.path,
                method: "POST",
                body: RefreshRequest(refreshToken: refreshToken),
                authorized: false,
                queryItems: []
            )
            let (data, response) = try await session.data(for: request)
            let payload: AuthPayload = try decodeEnvelope(response: response, data: data)
            tokenStore.save(accessToken: payload.token, refreshToken: payload.refreshToken ?? refreshToken)
            return payload
        }

        tokenStore.save(accessToken: payload.token, refreshToken: payload.refreshToken ?? refreshToken)
    }

    private func makeRequest(
        path: String,
        method: String,
        body: Encodable?,
        authorized: Bool,
        queryItems: [URLQueryItem]
    ) throws -> URLRequest {
        guard var components = URLComponents(url: baseURL, resolvingAgainstBaseURL: false) else {
            throw APIError.invalidURL
        }
        components.path = normalizedPath(base: components.path, append: path)
        if !queryItems.isEmpty {
            components.queryItems = queryItems
        }
        guard let url = components.url else {
            throw APIError.invalidURL
        }

        var request = URLRequest(url: url)
        request.httpMethod = method
        request.timeoutInterval = 20
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        if let body {
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            request.httpBody = try AnyEncodable(body).encode(using: encoder)
        }
        if authorized, let token = tokenStore.readAccessToken(), !token.isEmpty {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        return request
    }

    private func normalizedPath(base: String, append: String) -> String {
        let basePart = base == "/" ? "" : base.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        let appendPart = append.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        let combined = [basePart, appendPart].filter { !$0.isEmpty }.joined(separator: "/")
        return "/" + combined
    }

    private func validate(response: URLResponse, data: Data) throws {
        guard let http = response as? HTTPURLResponse else {
            throw APIError.invalidResponse
        }
        switch http.statusCode {
        case 200 ..< 300:
            return
        case 401:
            NotificationCenter.default.post(name: .zhihuijiUnauthorized, object: nil)
            throw APIError.unauthorized
        case 403:
            NotificationCenter.default.post(
                name: .zhihuijiForbidden,
                object: nil,
                userInfo: ["message": errorMessage(from: data) ?? APIError.forbidden.errorDescription ?? "当前账号没有权限访问该数据"]
            )
            throw APIError.forbidden
        default:
            let message = errorMessage(from: data) ?? String(data: data, encoding: .utf8) ?? "服务端返回错误"
            throw APIError.server(status: http.statusCode, message: message)
        }
    }

    private func errorMessage(from data: Data) -> String? {
        struct ErrorEnvelope: Decodable {
            let message: String?
        }

        return try? decoder.decode(ErrorEnvelope.self, from: data).message?.nilIfBlank
    }
}

private struct AnyEncodable: Encodable {
    private let encodeBlock: (Encoder) throws -> Void

    init(_ value: Encodable) {
        encodeBlock = value.encode
    }

    func encode(to encoder: Encoder) throws {
        try encodeBlock(encoder)
    }

    func encode(using encoder: JSONEncoder) throws -> Data {
        try encoder.encode(self)
    }
}

private struct EmptyPayload: Codable {}

private actor TokenRefreshCoordinator {
    private var currentTask: Task<AuthPayload, Error>?

    func refresh(using operation: @escaping @Sendable () async throws -> AuthPayload) async throws -> AuthPayload {
        if let currentTask {
            return try await currentTask.value
        }

        let task = Task { try await operation() }
        currentTask = task
        defer { currentTask = nil }
        return try await task.value
    }
}
