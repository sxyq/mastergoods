import Foundation
import XCTest
@testable import ZhihuijiIOS

final class APIClientSessionTests: XCTestCase {
    override func setUp() {
        super.setUp()
        MockURLProtocol.requestCount = 0
        MockURLProtocol.didStopLoading = false
        MockURLProtocol.holdOpen = false
    }

    override func tearDown() {
        MockURLProtocol.requestHandler = nil
        MockURLProtocol.requestCount = 0
        MockURLProtocol.didStopLoading = false
        MockURLProtocol.holdOpen = false
        super.tearDown()
    }

    func testAuthorizedRequestRefreshesTokenAfter401AndRetries() async throws {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockURLProtocol.self]
        let session = URLSession(configuration: config)
        let tokenStore = AuthTokenStore()
        tokenStore.clear()
        tokenStore.save(accessToken: "expired-token", refreshToken: "refresh-token")

        MockURLProtocol.requestHandler = { request in
            guard let path = request.url?.path else {
                throw URLError(.badURL)
            }

            switch path {
            case "/v2/stores/current":
                MockURLProtocol.requestCount += 1
                if MockURLProtocol.requestCount == 1 {
                    XCTAssertEqual(request.value(forHTTPHeaderField: "Authorization"), "Bearer expired-token")
                    return Self.response(statusCode: 401, body: Data())
                } else {
                    XCTAssertEqual(request.value(forHTTPHeaderField: "Authorization"), "Bearer refreshed-token")
                    return Self.response(
                        statusCode: 200,
                        body: Self.currentStorePayload.data(using: .utf8) ?? Data()
                    )
                }
            case "/v1/auth/refresh":
                XCTAssertEqual(request.httpMethod, "POST")
                return Self.response(
                    statusCode: 200,
                    body: Self.refreshPayload.data(using: .utf8) ?? Data()
                )
            default:
                throw URLError(.fileDoesNotExist)
            }
        }

        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)
        let profile = try await client.fetchCurrentStore()

        XCTAssertEqual(profile.storeId.rawValue, "90001")
        XCTAssertEqual(profile.currentUserName, "Test User")
        XCTAssertEqual(tokenStore.readAccessToken(), "refreshed-token")
    }

    func testAuthorizedRequestPostsUnauthorizedWhenRefreshTokenIsMissing() async throws {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockURLProtocol.self]
        let session = URLSession(configuration: config)
        let tokenStore = AuthTokenStore()
        tokenStore.clear()
        tokenStore.save(accessToken: "expired-token", refreshToken: nil)
        let notificationExpectation = expectation(description: "unauthorized notification posted")
        let observer = NotificationCenter.default.addObserver(
            forName: .zhihuijiUnauthorized,
            object: nil,
            queue: nil
        ) { _ in
            notificationExpectation.fulfill()
        }
        defer {
            NotificationCenter.default.removeObserver(observer)
        }

        MockURLProtocol.requestHandler = { request in
            guard request.url?.path == "/v2/stores/current" else {
                throw URLError(.fileDoesNotExist)
            }
            MockURLProtocol.requestCount += 1
            XCTAssertEqual(request.value(forHTTPHeaderField: "Authorization"), "Bearer expired-token")
            return Self.response(statusCode: 401, body: Data())
        }

        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)

        do {
            _ = try await client.fetchCurrentStore()
            XCTFail("Expected unauthorized error")
        } catch let error as APIError {
            XCTAssertEqual(error, .unauthorized)
        }

        await fulfillment(of: [notificationExpectation], timeout: 1.0)
        XCTAssertEqual(MockURLProtocol.requestCount, 1)
    }

    func testStreamAgentChatCancelsUnderlyingRequestWhenConsumerStops() async throws {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockURLProtocol.self]
        let session = URLSession(configuration: config)
        let tokenStore = AuthTokenStore()
        tokenStore.clear()
        tokenStore.save(accessToken: "stream-token", refreshToken: "refresh-token")

        MockURLProtocol.holdOpen = true
        MockURLProtocol.requestHandler = { request in
            guard request.url?.path == "/v2/agent/chat/stream" else {
                throw URLError(.fileDoesNotExist)
            }
            XCTAssertEqual(request.value(forHTTPHeaderField: "Authorization"), "Bearer stream-token")
            return Self.streamResponse()
        }

        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)
        let stream = try client.streamAgentChat(conversationId: nil, message: "hello")

        var seenEvent = false
        for try await event in stream {
            XCTAssertEqual(event.eventType, "message_delta")
            XCTAssertEqual(event.runId, "run-stream-1")
            seenEvent = true
            break
        }

        XCTAssertTrue(seenEvent)
        try await Task.sleep(nanoseconds: 150_000_000)
        XCTAssertTrue(MockURLProtocol.didStopLoading)
    }

    func testForbiddenResponsePostsAccessIssueNotification() async throws {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockURLProtocol.self]
        let session = URLSession(configuration: config)
        let tokenStore = AuthTokenStore()
        tokenStore.clear()
        tokenStore.save(accessToken: "access-token", refreshToken: "refresh-token")
        let notificationExpectation = expectation(description: "forbidden notification posted")
        var notificationMessage: String?
        let observer = NotificationCenter.default.addObserver(
            forName: .zhihuijiForbidden,
            object: nil,
            queue: nil
        ) { note in
            notificationMessage = note.userInfo?["message"] as? String
            notificationExpectation.fulfill()
        }
        defer {
            NotificationCenter.default.removeObserver(observer)
        }

        MockURLProtocol.requestHandler = { request in
            guard request.url?.path == "/v2/stores/current" else {
                throw URLError(.fileDoesNotExist)
            }
            XCTAssertEqual(request.value(forHTTPHeaderField: "Authorization"), "Bearer access-token")
            return Self.response(
                statusCode: 403,
                body: Data(#"{"code":403,"message":"No permission"}"#.utf8)
            )
        }

        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)

        do {
            _ = try await client.fetchCurrentStore()
            XCTFail("Expected forbidden error")
        } catch let error as APIError {
            XCTAssertEqual(error, .forbidden)
        }

        await fulfillment(of: [notificationExpectation], timeout: 1.0)
        XCTAssertEqual(notificationMessage, "No permission")
        XCTAssertEqual(tokenStore.readAccessToken(), "access-token")
    }

    func testServerErrorUsesEnvelopeMessage() async throws {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockURLProtocol.self]
        let session = URLSession(configuration: config)
        let tokenStore = AuthTokenStore()
        tokenStore.clear()
        tokenStore.save(accessToken: "access-token", refreshToken: "refresh-token")

        MockURLProtocol.requestHandler = { request in
            guard request.url?.path == "/v2/stores/current" else {
                throw URLError(.fileDoesNotExist)
            }
            return Self.response(
                statusCode: 500,
                body: Data(#"{"code":500,"message":"Store service unavailable"}"#.utf8)
            )
        }

        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)

        do {
            _ = try await client.fetchCurrentStore()
            XCTFail("Expected server error")
        } catch let error as APIError {
            XCTAssertEqual(error, .server(status: 500, message: "Store service unavailable"))
        }
    }

    func testConfiguredBaseURLPathPrefixIsPreserved() async throws {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockURLProtocol.self]
        let session = URLSession(configuration: config)
        let tokenStore = AuthTokenStore()
        tokenStore.clear()
        tokenStore.save(accessToken: "access-token", refreshToken: "refresh-token")

        MockURLProtocol.requestHandler = { request in
            XCTAssertEqual(request.url?.path, "/api/v2/stores/current")
            XCTAssertEqual(request.value(forHTTPHeaderField: "Authorization"), "Bearer access-token")
            return Self.response(
                statusCode: 200,
                body: Self.currentStorePayload.data(using: .utf8) ?? Data()
            )
        }

        let client = APIClient(baseURL: URL(string: "https://example.com/api")!, tokenStore: tokenStore, session: session)
        let profile = try await client.fetchCurrentStore()

        XCTAssertEqual(profile.storeId.rawValue, "90001")
    }

    func testMediaClientMethodsUseBackendContractPathsAndPayloads() async throws {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockURLProtocol.self]
        let session = URLSession(configuration: config)
        let tokenStore = AuthTokenStore()
        tokenStore.clear()
        tokenStore.save(accessToken: "access-token", refreshToken: "refresh-token")

        MockURLProtocol.requestHandler = { request in
            XCTAssertEqual(request.value(forHTTPHeaderField: "Authorization"), "Bearer access-token")
            let path = try XCTUnwrap(request.url?.path)

            switch (request.httpMethod, path) {
            case ("GET", "/v2/media/assets"):
                return Self.response(statusCode: 200, body: Self.mediaAssetListEnvelope.data(using: .utf8) ?? Data())
            case ("GET", "/v2/media/assets/99001"):
                return Self.response(statusCode: 200, body: Self.mediaAssetEnvelope.data(using: .utf8) ?? Data())
            case ("POST", "/v2/media/assets"):
                let json = try Self.requestJSON(request)
                XCTAssertEqual(json["asset_type"] as? String, "product_cover")
                XCTAssertEqual(json["object_key"] as? String, "products/50001/cover.png")
                XCTAssertEqual(json["metadata_json"] as? String, #"{"source":"ios"}"#)
                return Self.response(statusCode: 200, body: Self.mediaAssetEnvelope.data(using: .utf8) ?? Data())
            case ("GET", "/v2/media/bindings"):
                let query = try Self.queryDictionary(request)
                XCTAssertEqual(query["target_type"], "product")
                XCTAssertEqual(query["target_id"], "50001")
                return Self.response(statusCode: 200, body: Self.mediaBindingListEnvelope.data(using: .utf8) ?? Data())
            case ("POST", "/v2/media/bindings"):
                let json = try Self.requestJSON(request)
                XCTAssertEqual(json["asset_id"] as? String, "99001")
                XCTAssertEqual(json["target_type"] as? String, "product")
                XCTAssertEqual(json["target_id"] as? String, "50001")
                XCTAssertEqual(json["sort_order"] as? Int, 1)
                return Self.response(statusCode: 200, body: Self.mediaBindingEnvelope.data(using: .utf8) ?? Data())
            case ("DELETE", "/v2/media/bindings/88001"), ("DELETE", "/v2/media/assets/99001"):
                return Self.response(statusCode: 200, body: Self.emptyEnvelope.data(using: .utf8) ?? Data())
            default:
                throw URLError(.fileDoesNotExist)
            }
        }

        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)

        let assets = try await client.fetchMediaAssets()
        let asset = try await client.fetchMediaAsset(id: "99001")
        let createdAsset = try await client.createMediaAsset(
            payload: MediaAssetCreatePayload(
                assetType: "product_cover",
                storageProvider: "object_storage",
                bucketName: "master-goods",
                objectKey: "products/50001/cover.png",
                originalFileName: "cover.png",
                mimeType: "image/png",
                sizeBytes: 2048,
                checksum: "sha256:cover",
                width: 800,
                height: 600,
                metadataJson: #"{"source":"ios"}"#
            )
        )
        let bindings = try await client.fetchMediaBindings(targetType: "product", targetId: "50001")
        let binding = try await client.createMediaBinding(
            payload: MediaBindingCreatePayload(
                assetId: "99001",
                targetType: "product",
                targetId: "50001",
                sortOrder: 1
            )
        )
        try await client.deleteMediaBinding(id: "88001")
        try await client.deleteMediaAsset(id: "99001")

        XCTAssertEqual(assets.first?.id.rawValue, "99001")
        XCTAssertEqual(asset.objectKey, "products/50001/cover.png")
        XCTAssertEqual(createdAsset.mimeType, "image/png")
        XCTAssertEqual(bindings.first?.targetId.rawValue, "50001")
        XCTAssertEqual(binding.assetId.rawValue, "99001")
    }

    func testSyncClientMethodsUseBackendContractPathsAndPayloads() async throws {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockURLProtocol.self]
        let session = URLSession(configuration: config)
        let tokenStore = AuthTokenStore()
        tokenStore.clear()
        tokenStore.save(accessToken: "access-token", refreshToken: "refresh-token")

        MockURLProtocol.requestHandler = { request in
            XCTAssertEqual(request.value(forHTTPHeaderField: "Authorization"), "Bearer access-token")
            let path = try XCTUnwrap(request.url?.path)

            switch (request.httpMethod, path) {
            case ("GET", "/v2/sync/health"):
                return Self.response(statusCode: 200, body: Self.syncHealthEnvelope.data(using: .utf8) ?? Data())
            case ("GET", "/v2/sync/cursor/ios-device-1"):
                return Self.response(statusCode: 200, body: Self.syncCursorEnvelope.data(using: .utf8) ?? Data())
            case ("POST", "/v2/sync/cursor/ack"):
                let json = try Self.requestJSON(request)
                XCTAssertEqual(json["client_id"] as? String, "ios-device-1")
                XCTAssertEqual(json["cursor"] as? String, "cursor-ack")
                return Self.response(statusCode: 200, body: Self.syncCursorEnvelope.data(using: .utf8) ?? Data())
            case ("POST", "/v2/sync/upload"):
                let json = try Self.requestJSON(request)
                XCTAssertEqual(json["client_id"] as? String, "ios-device-1")
                XCTAssertEqual(json["last_sync_cursor"] as? String, "cursor-001")
                let changes = try XCTUnwrap(json["changes"] as? [[String: Any]])
                XCTAssertEqual(changes.first?["entity_type"] as? String, "product")
                XCTAssertEqual(changes.first?["entity_id"] as? String, "50001")
                return Self.response(statusCode: 200, body: Self.syncUploadEnvelope.data(using: .utf8) ?? Data())
            case ("POST", "/v2/sync/pull"):
                let json = try Self.requestJSON(request)
                XCTAssertEqual(json["client_id"] as? String, "ios-device-1")
                XCTAssertEqual(json["since_cursor"] as? String, "cursor-001")
                XCTAssertEqual(json["limit"] as? Int, 50)
                return Self.response(statusCode: 200, body: Self.syncPullEnvelope.data(using: .utf8) ?? Data())
            default:
                throw URLError(.fileDoesNotExist)
            }
        }

        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)
        let change = SyncChangeRecord(
            entityType: "product",
            entityId: "50001",
            operation: "upsert",
            payload: #"{"name":"iOS product"}"#,
            updatedAt: 1710000000000
        )

        let health = try await client.fetchSyncHealth()
        let cursor = try await client.fetchSyncCursor(clientId: "ios-device-1")
        let acknowledged = try await client.acknowledgeSyncCursor(payload: SyncCursorAckPayload(clientId: "ios-device-1", cursor: "cursor-ack"))
        let upload = try await client.uploadSyncChanges(payload: SyncUploadPayload(clientId: "ios-device-1", changes: [change], lastSyncCursor: "cursor-001"))
        let pull = try await client.pullSyncChanges(payload: SyncPullPayload(clientId: "ios-device-1", sinceCursor: "cursor-001", limit: 50))

        XCTAssertEqual(health.status, "ok")
        XCTAssertEqual(cursor.clientId, "ios-device-1")
        XCTAssertEqual(acknowledged.lastCursor, "cursor-002")
        XCTAssertEqual(upload.acceptedCount, 1)
        XCTAssertEqual(pull.changes.first?.entityId.rawValue, "50001")
    }

    func testImportJobClientMethodsUseBackendContractPathsAndPayloads() async throws {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockURLProtocol.self]
        let session = URLSession(configuration: config)
        let tokenStore = AuthTokenStore()
        tokenStore.clear()
        tokenStore.save(accessToken: "access-token", refreshToken: "refresh-token")

        MockURLProtocol.requestHandler = { request in
            XCTAssertEqual(request.value(forHTTPHeaderField: "Authorization"), "Bearer access-token")
            let path = try XCTUnwrap(request.url?.path)

            switch (request.httpMethod, path) {
            case ("GET", "/v2/import-jobs"):
                let query = try Self.queryDictionary(request)
                XCTAssertEqual(query["status"], "failed")
                return Self.response(statusCode: 200, body: Self.importJobListEnvelope.data(using: .utf8) ?? Data())
            case ("GET", "/v2/import-jobs/77001"):
                return Self.response(statusCode: 200, body: Self.importJobEnvelope.data(using: .utf8) ?? Data())
            case ("POST", "/v2/import-jobs"):
                let json = try Self.requestJSON(request)
                XCTAssertEqual(json["client_id"] as? String, "ios-device-1")
                XCTAssertEqual(json["source_type"] as? String, "legacy_sqlite")
                XCTAssertEqual(json["idempotency_key"] as? String, "import-001")
                return Self.response(statusCode: 200, body: Self.importJobEnvelope.data(using: .utf8) ?? Data())
            case ("POST", "/v2/import-jobs/77001/retry"):
                let json = try Self.requestJSON(request)
                XCTAssertEqual(json["replay_cursor"] as? String, "cursor-retry")
                return Self.response(statusCode: 200, body: Self.importJobEnvelope.data(using: .utf8) ?? Data())
            case ("POST", "/v2/import-jobs/77001/cancel"):
                return Self.response(statusCode: 200, body: Self.importJobEnvelope.data(using: .utf8) ?? Data())
            case ("POST", "/v2/import-jobs/legacy-sqlite"):
                let json = try Self.requestJSON(request)
                XCTAssertEqual(json["legacy_db_path"] as? String, "/tmp/legacy.db")
                XCTAssertEqual(json["reset_owned_data"] as? Bool, true)
                return Self.response(statusCode: 200, body: Self.legacySQLiteResultEnvelope.data(using: .utf8) ?? Data())
            default:
                throw URLError(.fileDoesNotExist)
            }
        }

        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)

        let jobs = try await client.fetchImportJobs(status: "failed")
        let job = try await client.fetchImportJob(id: "77001")
        let created = try await client.createImportJob(
            payload: ImportJobCreatePayload(
                clientId: "ios-device-1",
                sourceType: "legacy_sqlite",
                sourceUri: "file:///tmp/legacy.db",
                sourceChecksum: "sha256:legacy",
                idempotencyKey: "import-001",
                replayCursor: "cursor-001",
                optionsJson: #"{"dry_run":false}"#
            )
        )
        let retried = try await client.retryImportJob(id: "77001", payload: ImportJobRetryPayload(replayCursor: "cursor-retry"))
        let cancelled = try await client.cancelImportJob(id: "77001")
        let legacyResult = try await client.importLegacySQLite(payload: LegacySQLiteImportPayload(legacyDbPath: "/tmp/legacy.db", resetOwnedData: true))

        XCTAssertEqual(jobs.first?.id.rawValue, "77001")
        XCTAssertEqual(job.clientId, "ios-device-1")
        XCTAssertEqual(created.sourceType, "legacy_sqlite")
        XCTAssertEqual(retried.id.rawValue, "77001")
        XCTAssertEqual(cancelled.id.rawValue, "77001")
        XCTAssertEqual(legacyResult.products, 3)
    }

    func testFinanceRecordsUseExistingV1EndpointAndKeepCashChangeBoundaryExplicit() async throws {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockURLProtocol.self]
        let session = URLSession(configuration: config)
        let tokenStore = AuthTokenStore()
        tokenStore.clear()
        tokenStore.save(accessToken: "access-token", refreshToken: "refresh-token")

        MockURLProtocol.requestHandler = { request in
            XCTAssertEqual(request.value(forHTTPHeaderField: "Authorization"), "Bearer access-token")
            let path = try XCTUnwrap(request.url?.path)
            XCTAssertNotEqual(path, "/v2/cash-change-records")

            switch (request.httpMethod, path) {
            case ("GET", "/v1/finance-records"):
                let query = try Self.queryDictionary(request)
                XCTAssertEqual(query["keyword"], "rent")
                XCTAssertEqual(query["type"], "2")
                XCTAssertEqual(query["page"], "1")
                XCTAssertEqual(query["size"], "20")
                return Self.response(
                    statusCode: 200,
                    body: Self.financeRecordListEnvelope.data(using: .utf8) ?? Data()
                )
            case ("POST", "/v1/finance-records"):
                let json = try Self.requestJSON(request)
                XCTAssertEqual(json["type"] as? Int, 2)
                XCTAssertEqual(json["category"] as? String, "rent")
                XCTAssertEqual(json["partner_name"] as? String, "mall")
                XCTAssertEqual((json["amount"] as? NSNumber)?.doubleValue, 1200.0)
                XCTAssertEqual(json["method"] as? Int, 1)
                return Self.response(
                    statusCode: 200,
                    body: Self.financeRecordEnvelope.data(using: .utf8) ?? Data()
                )
            default:
                throw URLError(.fileDoesNotExist)
            }
        }

        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)
        let records = try await client.fetchFinanceRecords(keyword: "rent", type: 2, page: 1, size: 20)
        let created = try await client.createFinanceRecord(
            payload: FinanceRecordCreatePayload(
                type: 2,
                category: "rent",
                partnerName: "mall",
                amount: 1200,
                method: 1,
                notes: "monthly rent"
            )
        )

        XCTAssertEqual(records.first?.recordNo, "FR-001")
        XCTAssertEqual(created.category, "rent")
        XCTAssertTrue(FinanceRecordViewModel.cashChangeBoundaryNotice.contains("/v1/finance-records"))
        XCTAssertTrue(FinanceRecordViewModel.cashChangeBoundaryNotice.contains("cash-change records"))
    }

    @MainActor
    func testReportsViewModelClearsStaleSectionsWhenReportAPIsFail() async throws {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockURLProtocol.self]
        let session = URLSession(configuration: config)
        let tokenStore = AuthTokenStore()
        tokenStore.clear()
        tokenStore.save(accessToken: "access-token", refreshToken: "refresh-token")

        MockURLProtocol.requestHandler = { request in
            guard request.url?.path.hasPrefix("/v1/reports/") == true else {
                throw URLError(.fileDoesNotExist)
            }
            return Self.response(
                statusCode: 500,
                body: Data(#"{"code":500,"message":"report unavailable","data":null}"#.utf8)
            )
        }

        let viewModel = ReportsViewModel()
        viewModel.salesSummary = SalesSummaryReport(startAt: 1, endAt: 2, totalSalesAmount: 10, totalPaidAmount: 9, totalRefundAmount: 0, totalUnpaidAmount: 1, totalOrderCount: 1)
        viewModel.salesTrend = [SalesTrendPoint(startAt: 1, endAt: 2, totalSalesAmount: 10, totalOrderCount: 1)]
        viewModel.profitSummary = ProfitSummaryReport(startAt: 1, endAt: 2, estimatedCostAmount: 6, estimatedProfitAmount: 4, estimatedProfitRate: 0.4)
        viewModel.cashflowSummary = CashflowSummaryReport(startAt: 1, endAt: 2, totalIncomeAmount: 10, totalExpenseAmount: 3, netCashFlow: 7, totalRecordCount: 2)
        viewModel.reconciliation = ReconciliationSummaryReport(startAt: 1, endAt: 2, totalReceivableAmount: 8, totalPayableAmount: 1, totalReceivableCustomerCount: 1, totalPayableSupplierCount: 1, totalReceivedAmount: 2, totalPaidAmount: 1, netCashFlow: 1)
        viewModel.topProducts = [TopSellingProductReport(productId: "10001", productCode: "P-001", productName: "Old product", totalQuantity: 1, totalAmount: 10)]
        viewModel.productProfits = [ProfitByProductReport(productId: "10001", productCode: "P-001", productName: "Old product", totalSalesAmount: 10, totalCostAmount: 6, totalProfitAmount: 4, profitRate: 0.4)]
        viewModel.customerSales = [CustomerSalesReport(customerId: "20001", customerName: "Old customer", totalOrders: 1, totalAmount: 10)]
        viewModel.receivableCustomers = [CustomerReceivableReport(customerId: "20001", customerName: "Old customer", phone: nil, balance: 10)]
        viewModel.refunds = [RefundRecordReport(paymentId: "30001", orderId: "40001", orderNo: "S-001", customerName: nil, refundAmount: 1, method: 1, referenceNo: nil, createdAt: 1)]
        viewModel.stockOutRecords = [StockOutRecordReport(orderId: "40001", orderNo: "S-001", customerId: nil, customerName: nil, productId: "10001", productCode: "P-001", productName: "Old product", quantity: 1, unitPrice: 1, amount: 1, itemCreatedAt: 1, orderCreatedAt: 1)]
        viewModel.lowStockProducts = [LowStockProductReport(productId: "10001", productCode: "P-001", productName: "Old product", stock: 1, safeStock: 5)]

        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)
        await viewModel.load(client: client)

        XCTAssertNil(viewModel.salesSummary)
        XCTAssertTrue(viewModel.salesTrend.isEmpty)
        XCTAssertNil(viewModel.profitSummary)
        XCTAssertNil(viewModel.cashflowSummary)
        XCTAssertNil(viewModel.reconciliation)
        XCTAssertTrue(viewModel.topProducts.isEmpty)
        XCTAssertTrue(viewModel.productProfits.isEmpty)
        XCTAssertTrue(viewModel.customerSales.isEmpty)
        XCTAssertTrue(viewModel.receivableCustomers.isEmpty)
        XCTAssertTrue(viewModel.refunds.isEmpty)
        XCTAssertTrue(viewModel.stockOutRecords.isEmpty)
        XCTAssertTrue(viewModel.lowStockProducts.isEmpty)
        XCTAssertNotNil(viewModel.errorMessage)
    }

    func testInventoryClientMethodsUseBoundedQueryParameters() async throws {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockURLProtocol.self]
        let session = URLSession(configuration: config)
        let tokenStore = AuthTokenStore()
        tokenStore.clear()
        tokenStore.save(accessToken: "access-token", refreshToken: "refresh-token")

        MockURLProtocol.requestHandler = { request in
            XCTAssertEqual(request.value(forHTTPHeaderField: "Authorization"), "Bearer access-token")
            let path = try XCTUnwrap(request.url?.path)
            let query = try Self.queryDictionary(request)

            switch path {
            case "/v2/inventory/snapshots":
                XCTAssertEqual(query["snapshotDate"], "1710000000000")
                XCTAssertEqual(query["page"], "1")
                XCTAssertEqual(query["size"], "20")
                return Self.response(statusCode: 200, body: Self.emptyArrayEnvelope.data(using: .utf8) ?? Data())
            case "/v2/inventory/monthly-stats":
                XCTAssertEqual(query["year"], "2026")
                XCTAssertEqual(query["month"], "6")
                XCTAssertEqual(query["page"], "1")
                XCTAssertEqual(query["size"], "20")
                return Self.response(statusCode: 200, body: Self.emptyArrayEnvelope.data(using: .utf8) ?? Data())
            case "/v2/inventory/ledger":
                XCTAssertEqual(query["productId"], "50001")
                XCTAssertEqual(query["startAt"], "1710000000000")
                XCTAssertEqual(query["endAt"], "1710003600000")
                XCTAssertEqual(query["page"], "1")
                XCTAssertEqual(query["size"], "50")
                return Self.response(statusCode: 200, body: Self.emptyArrayEnvelope.data(using: .utf8) ?? Data())
            default:
                throw URLError(.fileDoesNotExist)
            }
        }

        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)

        let snapshots = try await client.fetchInventorySnapshots(snapshotDate: 1710000000000, page: 1, size: 20)
        let monthlyStats = try await client.fetchInventoryMonthlyStats(year: 2026, month: 6, page: 1, size: 20)
        let ledger = try await client.fetchInventoryLedger(productId: "50001", startAt: 1710000000000, endAt: 1710003600000, page: 1, size: 50)

        XCTAssertTrue(snapshots.isEmpty)
        XCTAssertTrue(monthlyStats.isEmpty)
        XCTAssertTrue(ledger.isEmpty)
    }

    @MainActor
    func testInventorySnapshotViewModelClearsStaleSectionsWhenInventoryAPIsFail() async throws {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockURLProtocol.self]
        let session = URLSession(configuration: config)
        let tokenStore = AuthTokenStore()
        tokenStore.clear()
        tokenStore.save(accessToken: "access-token", refreshToken: "refresh-token")

        MockURLProtocol.requestHandler = { request in
            guard let path = request.url?.path else {
                throw URLError(.badURL)
            }
            switch path {
            case "/v2/products", "/v1/reports/low-stock-products", "/v2/inventory/snapshots", "/v2/inventory/monthly-stats":
                return Self.response(
                    statusCode: 500,
                    body: Data(#"{"code":500,"message":"inventory unavailable","data":null}"#.utf8)
                )
            default:
                throw URLError(.fileDoesNotExist)
            }
        }

        let viewModel = InventorySnapshotViewModel()
        viewModel.products = [
            ProductRecord(
                id: "50001",
                code: "P-001",
                name: "Old product",
                categoryId: nil,
                categoryName: nil,
                unitId: nil,
                unitName: nil,
                salePrice: 12,
                purchasePrice: 8,
                priceLevels: nil,
                defaultSupplier: nil,
                supplierRelations: nil,
                stock: 3,
                safeStock: 5,
                status: 1,
                createdAt: nil,
                updatedAt: nil
            ),
        ]
        viewModel.lowStockProducts = [
            LowStockProductReport(productId: "50001", productCode: "P-001", productName: "Old product", stock: 3, safeStock: 5),
        ]
        viewModel.todaySnapshots = [
            InventorySnapshotSummary(id: "60001", productId: "50001", productCode: "P-001", productName: "Old product", warehouseId: nil, quantity: 3, unitCost: 8, totalValue: 24, snapshotDate: 1710000000000, createdAt: 1710000000000),
        ]
        viewModel.snapshots = viewModel.todaySnapshots
        viewModel.monthlyStats = [
            InventoryMonthlyStats(id: "70001", productId: "50001", productCode: "P-001", productName: "Old product", warehouseId: nil, month: 6, year: 2026, quantityIn: 10, quantityOut: 3, quantityAdjust: nil, quantityBegin: 0, quantityEnd: 7, totalCostIn: 80, totalCostOut: 24, createdAt: nil, updatedAt: nil),
        ]

        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)
        await viewModel.load(client: client)

        XCTAssertTrue(viewModel.products.isEmpty)
        XCTAssertTrue(viewModel.lowStockProducts.isEmpty)
        XCTAssertTrue(viewModel.todaySnapshots.isEmpty)
        XCTAssertTrue(viewModel.snapshots.isEmpty)
        XCTAssertTrue(viewModel.monthlyStats.isEmpty)
        XCTAssertNotNil(viewModel.errorMessage)
    }

    @MainActor
    func testArchivesViewModelClearsStaleCustomersWhenPartnerAPIsFail() async throws {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockURLProtocol.self]
        let session = URLSession(configuration: config)
        let tokenStore = AuthTokenStore()
        tokenStore.clear()
        tokenStore.save(accessToken: "access-token", refreshToken: "refresh-token")

        MockURLProtocol.requestHandler = { request in
            switch request.url?.path {
            case "/v2/customers", "/v2/customer-groups":
                return Self.response(
                    statusCode: 500,
                    body: Data(#"{"code":500,"message":"customer archive unavailable","data":null}"#.utf8)
                )
            default:
                throw URLError(.fileDoesNotExist)
            }
        }

        let viewModel = ArchivesHomeViewModel()
        viewModel.selectedTab = .customers
        viewModel.customers = [
            CustomerRecord(
                id: "80001",
                name: "Old customer",
                phone: "13800001111",
                level: 1,
                groupId: "81001",
                groupName: "VIP",
                primaryContactName: nil,
                primaryContactPhone: nil,
                address: nil,
                notes: nil,
                balance: 10,
                status: 1,
                createdAt: nil,
                updatedAt: nil
            ),
        ]
        viewModel.customerGroups = [
            PartnerGroupRecord(id: "81001", partnerType: "customer", name: "VIP", status: 1, sortOrder: 1, createdAt: nil, updatedAt: nil),
        ]

        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)
        await viewModel.load(using: client)

        XCTAssertTrue(viewModel.customers.isEmpty)
        XCTAssertTrue(viewModel.customerGroups.isEmpty)
        XCTAssertEqual(viewModel.errorMessage, "customer archive unavailable")
    }

    @MainActor
    func testArchivesViewModelClearsStaleSuppliersWhenPartnerAPIsFail() async throws {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockURLProtocol.self]
        let session = URLSession(configuration: config)
        let tokenStore = AuthTokenStore()
        tokenStore.clear()
        tokenStore.save(accessToken: "access-token", refreshToken: "refresh-token")

        MockURLProtocol.requestHandler = { request in
            switch request.url?.path {
            case "/v2/suppliers", "/v2/supplier-groups":
                return Self.response(
                    statusCode: 500,
                    body: Data(#"{"code":500,"message":"supplier archive unavailable","data":null}"#.utf8)
                )
            default:
                throw URLError(.fileDoesNotExist)
            }
        }

        let viewModel = ArchivesHomeViewModel()
        viewModel.selectedTab = .suppliers
        viewModel.suppliers = [
            SupplierRecord(
                id: "90001",
                name: "Old supplier",
                phone: "13800002222",
                groupId: "91001",
                groupName: "Main",
                primaryContactName: nil,
                primaryContactPhone: nil,
                address: nil,
                notes: nil,
                balance: 10,
                status: 1,
                createdAt: nil,
                updatedAt: nil
            ),
        ]
        viewModel.supplierGroups = [
            PartnerGroupRecord(id: "91001", partnerType: "supplier", name: "Main", status: 1, sortOrder: 1, createdAt: nil, updatedAt: nil),
        ]

        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)
        await viewModel.load(using: client)

        XCTAssertTrue(viewModel.suppliers.isEmpty)
        XCTAssertTrue(viewModel.supplierGroups.isEmpty)
        XCTAssertEqual(viewModel.errorMessage, "supplier archive unavailable")
    }

    @MainActor
    func testSalesListViewModelClearsStaleOrdersWhenAPIAndDetailFail() async throws {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockURLProtocol.self]
        let session = URLSession(configuration: config)
        let tokenStore = AuthTokenStore()
        tokenStore.clear()
        tokenStore.save(accessToken: "access-token", refreshToken: "refresh-token")

        MockURLProtocol.requestHandler = { request in
            switch request.url?.path {
            case "/v2/sale-orders", "/v2/sale-orders/50001", "/v2/sale-orders/50001/payments":
                return Self.response(
                    statusCode: 500,
                    body: Data(#"{"code":500,"message":"sale orders unavailable","data":null}"#.utf8)
                )
            default:
                throw URLError(.fileDoesNotExist)
            }
        }

        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)
        let listViewModel = SalesListViewModel()
        listViewModel.orders = [Self.sampleSalesOrder()]

        await listViewModel.load(using: client)

        XCTAssertTrue(listViewModel.orders.isEmpty)
        XCTAssertEqual(listViewModel.errorMessage, "sale orders unavailable")

        let detailViewModel = SalesDetailViewModel()
        detailViewModel.order = Self.sampleSalesOrder()
        detailViewModel.payments = [
            SalePaymentRecord(id: "60001", orderId: "50001", amount: 10, method: 1, referenceNo: nil, type: nil, createdAt: 1710000000000),
        ]

        await detailViewModel.load(orderId: "50001", client: client)

        XCTAssertNil(detailViewModel.order)
        XCTAssertTrue(detailViewModel.payments.isEmpty)
        XCTAssertEqual(detailViewModel.errorMessage, "sale orders unavailable")
    }

    @MainActor
    func testPurchaseListViewModelClearsStaleOrdersWhenAPIAndDetailFail() async throws {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockURLProtocol.self]
        let session = URLSession(configuration: config)
        let tokenStore = AuthTokenStore()
        tokenStore.clear()
        tokenStore.save(accessToken: "access-token", refreshToken: "refresh-token")

        MockURLProtocol.requestHandler = { request in
            switch request.url?.path {
            case "/v2/purchase-orders", "/v2/purchase-orders/70001":
                return Self.response(
                    statusCode: 500,
                    body: Data(#"{"code":500,"message":"purchase orders unavailable","data":null}"#.utf8)
                )
            default:
                throw URLError(.fileDoesNotExist)
            }
        }

        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)
        let listViewModel = PurchaseListViewModel()
        listViewModel.orders = [Self.samplePurchaseOrder()]

        await listViewModel.load(using: client)

        XCTAssertTrue(listViewModel.orders.isEmpty)
        XCTAssertEqual(listViewModel.errorMessage, "purchase orders unavailable")

        let detailViewModel = PurchaseDetailViewModel()
        detailViewModel.order = Self.samplePurchaseOrder()

        await detailViewModel.load(orderId: "70001", client: client)

        XCTAssertNil(detailViewModel.order)
        XCTAssertEqual(detailViewModel.errorMessage, "purchase orders unavailable")
    }

    @MainActor
    func testProductViewModelsClearStaleDataWhenAPIsFail() async throws {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockURLProtocol.self]
        let session = URLSession(configuration: config)
        let tokenStore = AuthTokenStore()
        tokenStore.clear()
        tokenStore.save(accessToken: "access-token", refreshToken: "refresh-token")

        MockURLProtocol.requestHandler = { request in
            switch request.url?.path {
            case "/v2/products",
                 "/v2/products/90001",
                 "/v2/product-categories",
                 "/v2/product-units",
                 "/v2/product-price-levels",
                 "/v2/suppliers":
                return Self.response(
                    statusCode: 500,
                    body: Data(#"{"code":500,"message":"products unavailable","data":null}"#.utf8)
                )
            default:
                throw URLError(.fileDoesNotExist)
            }
        }

        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)
        let product = Self.sampleProduct()

        let listViewModel = ProductListViewModel()
        listViewModel.products = [product]
        await listViewModel.load(using: client)
        XCTAssertTrue(listViewModel.products.isEmpty)
        XCTAssertEqual(listViewModel.errorMessage, "products unavailable")

        let detailViewModel = ProductDetailViewModel()
        detailViewModel.product = product
        await detailViewModel.load(productId: product.id, client: client)
        XCTAssertNil(detailViewModel.product)
        XCTAssertEqual(detailViewModel.errorMessage, "products unavailable")

        let editViewModel = ProductEditViewModel()
        editViewModel.categories = [
            ProductCategoryRecord(id: "101", name: "Old category", status: 1, sortOrder: 1, createdAt: nil, updatedAt: nil),
        ]
        editViewModel.units = [
            ProductUnitRecord(id: "201", name: "Old unit", status: 1, sortOrder: 1, createdAt: nil, updatedAt: nil),
        ]
        editViewModel.priceLevels = [
            EditablePriceLevel(levelId: "301", code: "VIP", name: "VIP", priceText: "9.00"),
        ]
        editViewModel.supplierDirectory = [
            SupplierRecord(id: "401", name: "Old supplier", phone: "13800001111", groupId: nil, groupName: nil, primaryContactName: nil, primaryContactPhone: nil, address: nil, notes: nil, balance: nil, status: 1, createdAt: nil, updatedAt: nil),
        ]
        editViewModel.supplierRelations = [
            EditableSupplierRelation(
                supplierId: "401",
                supplierName: "Old supplier",
                supplierPhone: "13800001111",
                isDefault: true,
                purchasePriorityText: "1",
                lastPurchasePriceText: "8.00",
                notes: nil
            ),
        ]
        editViewModel.selectedCategoryId = "101"
        editViewModel.selectedUnitId = "201"
        editViewModel.loadedProductId = product.id
        editViewModel.relationErrorMessage = "old relation error"

        await editViewModel.load(productId: product.id, client: client)

        XCTAssertTrue(editViewModel.categories.isEmpty)
        XCTAssertTrue(editViewModel.units.isEmpty)
        XCTAssertTrue(editViewModel.priceLevels.isEmpty)
        XCTAssertTrue(editViewModel.supplierDirectory.isEmpty)
        XCTAssertTrue(editViewModel.supplierRelations.isEmpty)
        XCTAssertNil(editViewModel.selectedCategoryId)
        XCTAssertNil(editViewModel.selectedUnitId)
        XCTAssertNil(editViewModel.loadedProductId)
        XCTAssertNil(editViewModel.relationErrorMessage)
        XCTAssertEqual(editViewModel.errorMessage, "products unavailable")
    }

    @MainActor
    func testStaffManagementViewModelClearsStaleMembersWhenAPIUnavailable() async throws {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockURLProtocol.self]
        let session = URLSession(configuration: config)
        let tokenStore = AuthTokenStore()
        tokenStore.clear()
        tokenStore.save(accessToken: "access-token", refreshToken: "refresh-token")

        MockURLProtocol.requestHandler = { request in
            switch request.url?.path {
            case "/v2/stores/current/members":
                return Self.response(
                    statusCode: 500,
                    body: Data(#"{"code":500,"message":"staff unavailable","data":null}"#.utf8)
                )
            default:
                throw URLError(.fileDoesNotExist)
            }
        }

        let member = Self.sampleStaffMember()
        let viewModel = StaffManagementViewModel()
        viewModel.members = [member]
        viewModel.editingMember = member
        viewModel.successMessage = "old success"

        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)
        await viewModel.load(using: client)

        XCTAssertTrue(viewModel.members.isEmpty)
        XCTAssertNil(viewModel.editingMember)
        XCTAssertNil(viewModel.successMessage)
        XCTAssertEqual(viewModel.errorMessage, "staff unavailable")
    }

    @MainActor
    func testMediaAssetsViewModelClearsStaleServerDataWhenAPIsFail() async throws {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockURLProtocol.self]
        let session = URLSession(configuration: config)
        let tokenStore = AuthTokenStore()
        tokenStore.clear()
        tokenStore.save(accessToken: "access-token", refreshToken: "refresh-token")

        MockURLProtocol.requestHandler = { request in
            switch request.url?.path {
            case "/v2/media/assets", "/v2/media/bindings":
                return Self.response(
                    statusCode: 500,
                    body: Data(#"{"code":500,"message":"media unavailable","data":null}"#.utf8)
                )
            default:
                throw URLError(.fileDoesNotExist)
            }
        }

        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)
        let viewModel = MediaAssetsViewModel()
        viewModel.assets = [Self.sampleMediaAsset()]
        viewModel.bindings = [Self.sampleMediaBinding()]

        await viewModel.load(using: client)

        XCTAssertTrue(viewModel.assets.isEmpty)
        XCTAssertTrue(viewModel.bindings.isEmpty)
        XCTAssertEqual(viewModel.errorMessage, "media unavailable")

        viewModel.bindings = [Self.sampleMediaBinding()]
        viewModel.bindingTargetType = "product"
        viewModel.bindingTargetId = "90001"

        await viewModel.loadBindings(using: client)

        XCTAssertTrue(viewModel.bindings.isEmpty)
        XCTAssertEqual(viewModel.errorMessage, "media unavailable")
    }

    @MainActor
    func testSyncImportViewModelClearsStaleServerDataWhenAPIsFail() async throws {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockURLProtocol.self]
        let session = URLSession(configuration: config)
        let tokenStore = AuthTokenStore()
        tokenStore.clear()
        tokenStore.save(accessToken: "access-token", refreshToken: "refresh-token")

        MockURLProtocol.requestHandler = { request in
            switch request.url?.path {
            case "/v2/sync/health",
                 "/v2/sync/cursor/ios-test-client",
                 "/v2/import-jobs",
                 "/v2/import-jobs/legacy-sqlite",
                 "/v2/sync/upload",
                 "/v2/sync/pull":
                return Self.response(
                    statusCode: 500,
                    body: Data(#"{"code":500,"message":"sync unavailable","data":null}"#.utf8)
                )
            default:
                throw URLError(.fileDoesNotExist)
            }
        }

        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)
        let viewModel = SyncImportViewModel()
        viewModel.clientId = "ios-test-client"
        viewModel.health = Self.sampleSyncHealth()
        viewModel.cursor = Self.sampleSyncCursor()
        viewModel.importJobs = [Self.sampleImportJob()]
        viewModel.importResult = Self.sampleLegacyImportResult()
        viewModel.syncUploadResponse = SyncUploadResponse(acceptedCount: 1, failedCount: 0, status: "ok", nextCursor: "cursor-002")
        viewModel.syncPullResponse = SyncPullResponse(changes: [], effectiveCursor: "cursor-001", nextCursor: "cursor-002", hasMore: false)

        await viewModel.load(using: client)

        XCTAssertNil(viewModel.health)
        XCTAssertNil(viewModel.cursor)
        XCTAssertTrue(viewModel.importJobs.isEmpty)
        XCTAssertNil(viewModel.importResult)
        XCTAssertNil(viewModel.syncUploadResponse)
        XCTAssertNil(viewModel.syncPullResponse)
        XCTAssertEqual(viewModel.errorMessage, "sync unavailable")

        viewModel.legacyDbPath = "/tmp/legacy.db"
        viewModel.importResult = Self.sampleLegacyImportResult()
        await viewModel.importLegacySQLite(using: client)
        XCTAssertNil(viewModel.importResult)
        XCTAssertEqual(viewModel.errorMessage, "sync unavailable")

        viewModel.syncUploadEntityType = "product"
        viewModel.syncUploadEntityId = "90001"
        viewModel.syncUploadPayloadJson = #"{"name":"iOS"}"#
        viewModel.syncUploadUpdatedAtText = "1710000000000"
        viewModel.syncUploadResponse = SyncUploadResponse(acceptedCount: 1, failedCount: 0, status: "ok", nextCursor: "cursor-002")
        await viewModel.uploadSyncChange(using: client)
        XCTAssertNil(viewModel.syncUploadResponse)
        XCTAssertEqual(viewModel.errorMessage, "sync unavailable")

        viewModel.syncPullResponse = SyncPullResponse(changes: [], effectiveCursor: "cursor-001", nextCursor: "cursor-002", hasMore: false)
        await viewModel.pullSyncChanges(using: client)
        XCTAssertNil(viewModel.syncPullResponse)
        XCTAssertEqual(viewModel.errorMessage, "sync unavailable")
    }

    @MainActor
    func testAgentViewModelClearsStaleSectionsWhenInitialLoadFails() async throws {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockURLProtocol.self]
        let session = URLSession(configuration: config)
        let tokenStore = AuthTokenStore()
        tokenStore.clear()
        tokenStore.save(accessToken: "access-token", refreshToken: "refresh-token")

        MockURLProtocol.requestHandler = { request in
            switch request.url?.path {
            case "/v2/agent/workbench",
                 "/v2/agent/conversations",
                 "/v2/agent/drafts",
                 "/v2/agent/tasks",
                 "/v2/agent/notifications":
                return Self.response(
                    statusCode: 500,
                    body: Data(#"{"code":500,"message":"agent unavailable","data":null}"#.utf8)
                )
            default:
                throw URLError(.fileDoesNotExist)
            }
        }

        let viewModel = AgentViewModel()
        viewModel.workbench = Self.sampleAgentWorkbench()
        viewModel.conversations = [Self.sampleAgentConversation()]
        viewModel.selectedConversationId = "agent-conv-1"
        viewModel.messages = [Self.sampleAgentMessage()]
        viewModel.drafts = [Self.sampleAgentDraft()]
        viewModel.tasks = [Self.sampleAgentTask()]
        viewModel.notifications = [Self.sampleAgentNotification()]
        viewModel.editingDraft = Self.sampleAgentDraft()

        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)
        await viewModel.load(using: client)

        XCTAssertNil(viewModel.workbench)
        XCTAssertTrue(viewModel.conversations.isEmpty)
        XCTAssertNil(viewModel.selectedConversationId)
        XCTAssertTrue(viewModel.messages.isEmpty)
        XCTAssertTrue(viewModel.drafts.isEmpty)
        XCTAssertNil(viewModel.editingDraft)
        XCTAssertTrue(viewModel.tasks.isEmpty)
        XCTAssertTrue(viewModel.notifications.isEmpty)
        XCTAssertNotNil(viewModel.errorMessage)
    }

    @MainActor
    func testAgentViewModelClearsMessagesAndDraftsWhenConversationRefreshFails() async throws {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockURLProtocol.self]
        let session = URLSession(configuration: config)
        let tokenStore = AuthTokenStore()
        tokenStore.clear()
        tokenStore.save(accessToken: "access-token", refreshToken: "refresh-token")

        MockURLProtocol.requestHandler = { request in
            switch request.url?.path {
            case "/v2/agent/conversations/agent-conv-1/messages",
                 "/v2/agent/drafts":
                return Self.response(
                    statusCode: 500,
                    body: Data(#"{"code":500,"message":"agent conversation unavailable","data":null}"#.utf8)
                )
            default:
                throw URLError(.fileDoesNotExist)
            }
        }

        let viewModel = AgentViewModel()
        viewModel.selectedConversationId = "agent-conv-1"
        viewModel.messages = [Self.sampleAgentMessage()]
        viewModel.drafts = [Self.sampleAgentDraft()]
        viewModel.editingDraft = Self.sampleAgentDraft()

        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)
        await viewModel.selectConversation("agent-conv-1", client: client)

        XCTAssertEqual(viewModel.selectedConversationId?.rawValue, "agent-conv-1")
        XCTAssertTrue(viewModel.messages.isEmpty)
        XCTAssertTrue(viewModel.drafts.isEmpty)
        XCTAssertNil(viewModel.editingDraft)
        XCTAssertEqual(viewModel.errorMessage, "agent conversation unavailable")
    }

    private static func sampleAgentWorkbench() -> AgentWorkbench {
        AgentWorkbench(
            greeting: "Hello",
            kpiCards: [],
            quickQuestions: ["今日回款风险？"],
            recentConversations: [],
            pendingDrafts: [],
            riskAlerts: [],
            todaySummary: "Old summary",
            status: "ok",
            dataPolicy: nil,
            capabilities: [],
            warnings: []
        )
    }

    private static func sampleAgentConversation() -> AgentConversationSummary {
        AgentConversationSummary(
            id: "agent-conv-1",
            title: "Old conversation",
            status: AgentContractStatus.active,
            latestSummary: "Old summary",
            createdAt: 1710000000000,
            updatedAt: 1710000000000,
            lastMessageAt: 1710000000000
        )
    }

    private static func sampleAgentMessage() -> AgentMessage {
        AgentMessage(
            id: "agent-message-1",
            conversationId: "agent-conv-1",
            role: "assistant",
            messageType: "text",
            content: "Old answer",
            structuredDataJson: nil,
            createdAt: 1710000000000
        )
    }

    private static func sampleAgentDraft() -> AgentDraft {
        AgentDraft(
            id: "agent-draft-1",
            conversationId: "agent-conv-1",
            draftType: "question",
            title: "Old draft",
            contentJson: #"{"question":"Old question"}"#,
            status: AgentContractStatus.active,
            createdAt: 1710000000000,
            updatedAt: 1710000000000
        )
    }

    private static func sampleAgentTask() -> AgentTask {
        AgentTask(
            id: "agent-task-1",
            taskType: "analysis",
            title: "Old task",
            triggerSource: "manual",
            status: "running",
            statusLabel: "Running",
            progress: 10,
            inputText: nil,
            resultJson: nil,
            createdAt: 1710000000000,
            updatedAt: 1710000000000,
            completedAt: nil
        )
    }

    private static func sampleAgentNotification() -> AgentNotification {
        AgentNotification(
            id: "agent-notification-1",
            taskId: "agent-task-1",
            title: "Old notification",
            body: "Old body",
            level: "info",
            isRead: false,
            isDelivered: true,
            createdAt: 1710000000000
        )
    }

    private static func sampleMediaAsset() -> MediaAssetRecord {
        MediaAssetRecord(
            id: "99001",
            assetType: "product_cover",
            storageProvider: "object_storage",
            bucketName: "master-goods",
            objectKey: "products/90001/cover.png",
            originalFileName: "cover.png",
            mimeType: "image/png",
            sizeBytes: 2048,
            checksum: "sha256:cover",
            width: 800,
            height: 600,
            metadataJson: #"{"source":"ios"}"#,
            createdAt: 1710000000000,
            updatedAt: 1710000000000
        )
    }

    private static func sampleMediaBinding() -> MediaBindingRecord {
        MediaBindingRecord(
            id: "99002",
            assetId: "99001",
            targetType: "product",
            targetId: "90001",
            sortOrder: 1,
            createdAt: 1710000000000
        )
    }

    private static func sampleSyncHealth() -> SyncHealthRecord {
        SyncHealthRecord(
            status: "ok",
            message: "ready",
            ownerScoped: true,
            serverTime: 1710000000000,
            supportedEntityTypes: ["product"],
            uploadableEntityTypes: ["product"]
        )
    }

    private static func sampleSyncCursor() -> SyncCursorRecord {
        SyncCursorRecord(clientId: "ios-test-client", lastCursor: "cursor-001", updatedAt: 1710000000000)
    }

    private static func sampleImportJob() -> ImportJobRecord {
        ImportJobRecord(
            id: "77001",
            clientId: "ios-test-client",
            sourceType: "legacy_sqlite",
            sourceUri: "file:///tmp/legacy.db",
            sourceChecksum: "sha256:legacy",
            idempotencyKey: "import-001",
            status: "running",
            stage: "validate",
            retryCount: 0,
            replayCursor: "cursor-001",
            summaryJson: nil,
            optionsJson: nil,
            failureCode: nil,
            failureMessage: nil,
            createdAt: 1710000000000,
            updatedAt: 1710000000000,
            startedAt: 1710000000000,
            finishedAt: nil,
            lastHeartbeatAt: 1710000000000
        )
    }

    private static func sampleLegacyImportResult() -> LegacySQLiteImportResult {
        LegacySQLiteImportResult(
            userId: "70001",
            phone: "13800000001",
            nickname: "Importer",
            legacyDbPath: "/tmp/legacy.db",
            accounts: 1,
            customers: 2,
            suppliers: 3,
            products: 4,
            saleOrders: 5,
            saleOrderItems: 6,
            payments: 7,
            purchaseOrders: 8,
            purchaseOrderItems: 9,
            payOrders: 10,
            financeRecords: 11,
            inventorySnapshots: 12
        )
    }

    private static func sampleStaffMember() -> StoreStaffMember {
        StoreStaffMember(
            userId: "30001",
            phone: "13800003001",
            nickname: "Old staff",
            role: .sales,
            title: "Sales",
            status: 1,
            permissions: [.salesView, .salesWrite],
            createdAt: 1710000000000,
            updatedAt: 1710000000000,
            activeSessions: 1,
            storeId: "90001",
            storeName: "Test Store"
        )
    }

    private static func sampleProduct() -> ProductRecord {
        ProductRecord(
            id: "90001",
            code: "P-001",
            name: "Old product",
            categoryId: 101,
            categoryName: "Old category",
            unitId: 201,
            unitName: "Old unit",
            salePrice: 12,
            purchasePrice: 8,
            priceLevels: [
                ProductPriceLevelValue(levelId: "301", code: "VIP", name: "VIP", price: 9, status: 1, sortOrder: 1),
            ],
            defaultSupplier: nil,
            supplierRelations: nil,
            stock: 3,
            safeStock: 5,
            status: 1,
            createdAt: 1710000000000,
            updatedAt: 1710000000000
        )
    }

    private static func sampleSalesOrder() -> SalesOrderSummary {
        SalesOrderSummary(
            id: "50001",
            orderNo: "SO-001",
            customerId: 80001,
            customerName: "Old customer",
            items: [
                SalesOrderItem(
                    id: "51001",
                    orderId: "50001",
                    productId: 90001,
                    productCode: "P-001",
                    productName: "Old product",
                    customerId: 80001,
                    customerName: "Old customer",
                    quantity: 1,
                    unitPrice: 10,
                    amount: 10,
                    createdAt: 1710000000000
                ),
            ],
            subtotalAmount: 10,
            discountAmount: 0,
            totalAmount: 10,
            paidAmount: 0,
            notes: nil,
            status: 0,
            createdAt: 1710000000000,
            updatedAt: 1710000000000
        )
    }

    private static func samplePurchaseOrder() -> PurchaseOrderSummary {
        PurchaseOrderSummary(
            id: "70001",
            orderNo: "PO-001",
            supplierId: "90001",
            supplierName: "Old supplier",
            items: [
                PurchaseOrderItem(
                    id: "71001",
                    orderId: "70001",
                    productId: "90001",
                    productCode: "P-001",
                    productName: "Old product",
                    quantity: 1,
                    unitCost: 8,
                    amount: 8,
                    createdAt: 1710000000000
                ),
            ],
            totalAmount: 8,
            paidAmount: 0,
            receivedAmount: 0,
            notes: nil,
            status: 0,
            createdAt: 1710000000000,
            updatedAt: 1710000000000
        )
    }

    private static func requestJSON(_ request: URLRequest) throws -> [String: Any] {
        let data = try XCTUnwrap(requestBodyData(request))
        return try XCTUnwrap(try JSONSerialization.jsonObject(with: data) as? [String: Any])
    }

    private static func requestBodyData(_ request: URLRequest) -> Data? {
        if let body = request.httpBody {
            return body
        }
        guard let stream = request.httpBodyStream else {
            return nil
        }
        stream.open()
        defer { stream.close() }

        var data = Data()
        let bufferSize = 1024
        let buffer = UnsafeMutablePointer<UInt8>.allocate(capacity: bufferSize)
        defer { buffer.deallocate() }

        while stream.hasBytesAvailable {
            let read = stream.read(buffer, maxLength: bufferSize)
            if read > 0 {
                data.append(buffer, count: read)
            } else {
                break
            }
        }
        return data
    }

    private static func queryDictionary(_ request: URLRequest) throws -> [String: String] {
        let url = try XCTUnwrap(request.url)
        let components = try XCTUnwrap(URLComponents(url: url, resolvingAgainstBaseURL: false))
        return Dictionary(uniqueKeysWithValues: (components.queryItems ?? []).map { ($0.name, $0.value ?? "") })
    }

    private static func response(statusCode: Int, body: Data) throws -> (HTTPURLResponse, Data) {
        guard let url = URL(string: "https://example.com") else {
            throw URLError(.badURL)
        }
        let response = HTTPURLResponse(
            url: url,
            statusCode: statusCode,
            httpVersion: "HTTP/1.1",
            headerFields: ["Content-Type": "application/json"]
        )!
        return (response, body)
    }

    private static func streamResponse() throws -> (HTTPURLResponse, Data) {
        guard let url = URL(string: "https://example.com") else {
            throw URLError(.badURL)
        }
        let response = HTTPURLResponse(
            url: url,
            statusCode: 200,
            httpVersion: "HTTP/1.1",
            headerFields: ["Content-Type": "text/event-stream"]
        )!
        let body = """
        data: {"event_type":"message_delta","run_id":"run-stream-1","delta":"hello","timestamp":1710000000000}

        """
        return (response, Data(body.utf8))
    }

    private static let refreshPayload = """
    {
      "code": 0,
      "message": "ok",
      "data": {
        "user_id": "70001",
        "token": "refreshed-token",
        "refresh_token": "refresh-token",
        "expires_in": 7200
      }
    }
    """

    private static let currentStorePayload = """
    {
      "code": 0,
      "message": "ok",
      "data": {
        "store_id": "90001",
        "store_name": "Test Store",
        "owner_user_id": "70001",
        "current_user_id": "70001",
        "current_user_name": "Test User",
        "current_user_phone": "13800000001",
        "role": "OWNER",
        "title": "Store Owner",
        "status": 1,
        "permissions": ["dashboard:view", "database:manage"],
        "member_count": 3,
        "enabled_member_count": 3,
        "disabled_member_count": 0
      }
    }
    """

    private static let emptyEnvelope = """
    {
      "code": 0,
      "message": "ok",
      "data": {}
    }
    """

    private static let emptyArrayEnvelope = """
    {
      "code": 0,
      "message": "ok",
      "data": []
    }
    """

    private static let financeRecordEnvelope = """
    {
      "code": 0,
      "message": "ok",
      "data": {
        "id": "66001",
        "record_no": "FR-001",
        "type": 2,
        "category": "rent",
        "partner_name": "mall",
        "amount": 1200.0,
        "method": 1,
        "notes": "monthly rent",
        "created_at": 1710000000000,
        "updated_at": 1710000000000
      }
    }
    """

    private static let financeRecordListEnvelope = """
    {
      "code": 0,
      "message": "ok",
      "data": [
        {
          "id": "66001",
          "record_no": "FR-001",
          "type": 2,
          "category": "rent",
          "partner_name": "mall",
          "amount": 1200.0,
          "method": 1,
          "notes": "monthly rent",
          "created_at": 1710000000000,
          "updated_at": 1710000000000
        }
      ]
    }
    """

    private static let mediaAssetEnvelope = """
    {
      "code": 0,
      "message": "ok",
      "data": {
        "id": "99001",
        "asset_type": "product_cover",
        "storage_provider": "object_storage",
        "bucket_name": "master-goods",
        "object_key": "products/50001/cover.png",
        "original_file_name": "cover.png",
        "mime_type": "image/png",
        "size_bytes": 2048,
        "checksum": "sha256:cover",
        "width": 800,
        "height": 600,
        "metadata_json": "{\\"source\\":\\"ios\\"}",
        "created_at": 1710000000000,
        "updated_at": 1710000000000
      }
    }
    """

    private static let mediaAssetListEnvelope = """
    {
      "code": 0,
      "message": "ok",
      "data": [
        {
          "id": "99001",
          "asset_type": "product_cover",
          "storage_provider": "object_storage",
          "bucket_name": "master-goods",
          "object_key": "products/50001/cover.png",
          "original_file_name": "cover.png",
          "mime_type": "image/png",
          "size_bytes": 2048,
          "checksum": "sha256:cover",
          "width": 800,
          "height": 600,
          "metadata_json": "{\\"source\\":\\"ios\\"}",
          "created_at": 1710000000000,
          "updated_at": 1710000000000
        }
      ]
    }
    """

    private static let mediaBindingEnvelope = """
    {
      "code": 0,
      "message": "ok",
      "data": {
        "id": "88001",
        "asset_id": "99001",
        "target_type": "product",
        "target_id": "50001",
        "sort_order": 1,
        "created_at": 1710000000000
      }
    }
    """

    private static let mediaBindingListEnvelope = """
    {
      "code": 0,
      "message": "ok",
      "data": [
        {
          "id": "88001",
          "asset_id": "99001",
          "target_type": "product",
          "target_id": "50001",
          "sort_order": 1,
          "created_at": 1710000000000
        }
      ]
    }
    """

    private static let syncHealthEnvelope = """
    {
      "code": 0,
      "message": "ok",
      "data": {
        "status": "ok",
        "message": "ready",
        "owner_scoped": true,
        "server_time": 1710000000000,
        "supported_entity_types": ["product", "customer"],
        "uploadable_entity_types": ["product"]
      }
    }
    """

    private static let syncCursorEnvelope = """
    {
      "code": 0,
      "message": "ok",
      "data": {
        "client_id": "ios-device-1",
        "last_cursor": "cursor-002",
        "updated_at": 1710000000000
      }
    }
    """

    private static let syncUploadEnvelope = """
    {
      "code": 0,
      "message": "ok",
      "data": {
        "accepted_count": 1,
        "failed_count": 0,
        "status": "accepted",
        "next_cursor": "cursor-002"
      }
    }
    """

    private static let syncPullEnvelope = """
    {
      "code": 0,
      "message": "ok",
      "data": {
        "changes": [
          {
            "entity_type": "product",
            "entity_id": "50001",
            "operation": "upsert",
            "payload": "{\\"name\\":\\"iOS product\\"}",
            "updated_at": 1710000000000
          }
        ],
        "effective_cursor": "cursor-001",
        "next_cursor": "cursor-002",
        "has_more": false
      }
    }
    """

    private static let importJobEnvelope = """
    {
      "code": 0,
      "message": "ok",
      "data": {
        "id": "77001",
        "client_id": "ios-device-1",
        "source_type": "legacy_sqlite",
        "source_uri": "file:///tmp/legacy.db",
        "source_checksum": "sha256:legacy",
        "idempotency_key": "import-001",
        "status": "failed",
        "stage": "validate",
        "retry_count": 1,
        "replay_cursor": "cursor-001",
        "summary_json": "{\\"products\\":3}",
        "options_json": "{\\"dry_run\\":false}",
        "failure_code": "worker_failed",
        "failure_message": "worker failed",
        "created_at": 1710000000000,
        "updated_at": 1710000000001,
        "started_at": 1710000000000,
        "finished_at": 1710000000001,
        "last_heartbeat_at": 1710000000000
      }
    }
    """

    private static let importJobListEnvelope = """
    {
      "code": 0,
      "message": "ok",
      "data": [
        {
          "id": "77001",
          "client_id": "ios-device-1",
          "source_type": "legacy_sqlite",
          "source_uri": "file:///tmp/legacy.db",
          "source_checksum": "sha256:legacy",
          "idempotency_key": "import-001",
          "status": "failed",
          "stage": "validate",
          "retry_count": 1,
          "replay_cursor": "cursor-001",
          "summary_json": "{\\"products\\":3}",
          "options_json": "{\\"dry_run\\":false}",
          "failure_code": "worker_failed",
          "failure_message": "worker failed",
          "created_at": 1710000000000,
          "updated_at": 1710000000001,
          "started_at": 1710000000000,
          "finished_at": 1710000000001,
          "last_heartbeat_at": 1710000000000
        }
      ]
    }
    """

    private static let legacySQLiteResultEnvelope = """
    {
      "code": 0,
      "message": "ok",
      "data": {
        "user_id": "70001",
        "phone": "13800000001",
        "nickname": "Test User",
        "legacy_db_path": "/tmp/legacy.db",
        "accounts": 1,
        "customers": 2,
        "suppliers": 1,
        "products": 3,
        "sale_orders": 4,
        "sale_order_items": 5,
        "payments": 6,
        "purchase_orders": 7,
        "purchase_order_items": 8,
        "pay_orders": 9,
        "finance_records": 10,
        "inventory_snapshots": 11
      }
    }
    """
}

final class MockURLProtocol: URLProtocol {
    static var requestHandler: ((URLRequest) throws -> (HTTPURLResponse, Data))?
    static var requestCount = 0
    static var didStopLoading = false
    static var holdOpen = false

    override class func canInit(with request: URLRequest) -> Bool {
        true
    }

    override class func canonicalRequest(for request: URLRequest) -> URLRequest {
        request
    }

    override func startLoading() {
        guard let handler = Self.requestHandler else {
            client?.urlProtocol(self, didFailWithError: URLError(.unsupportedURL))
            return
        }
        do {
            let (response, data) = try handler(request)
            client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
            if !data.isEmpty {
                client?.urlProtocol(self, didLoad: data)
            }
            if !Self.holdOpen {
                client?.urlProtocolDidFinishLoading(self)
            }
        } catch {
            client?.urlProtocol(self, didFailWithError: error)
        }
    }

    override func stopLoading() {
        Self.didStopLoading = true
    }
}
