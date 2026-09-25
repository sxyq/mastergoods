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
            case "/v2/auth/refresh":
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

    func testStreamAgentChatForbiddenResponseUsesEnvelopeMessage() async throws {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockURLProtocol.self]
        let session = URLSession(configuration: config)
        let tokenStore = AuthTokenStore()
        tokenStore.clear()
        tokenStore.save(accessToken: "stream-token", refreshToken: "refresh-token")
        let notificationExpectation = expectation(description: "stream forbidden notification posted")
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
            guard request.url?.path == "/v2/agent/chat/stream" else {
                throw URLError(.fileDoesNotExist)
            }
            XCTAssertEqual(request.value(forHTTPHeaderField: "Authorization"), "Bearer stream-token")
            return Self.response(
                statusCode: 403,
                body: Data(#"{"code":403,"message":"Agent write permission required"}"#.utf8)
            )
        }

        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)
        let stream = try client.streamAgentChat(conversationId: nil, message: "hello")

        do {
            for try await _ in stream {}
            XCTFail("Expected forbidden stream error")
        } catch let error as APIError {
            XCTAssertEqual(error, .forbidden)
        }

        await fulfillment(of: [notificationExpectation], timeout: 1.0)
        XCTAssertEqual(notificationMessage, "Agent write permission required")
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

    func testUploadMediaAssetPostsMultipartToUploadEndpoint() async throws {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockURLProtocol.self]
        let session = URLSession(configuration: config)
        let tokenStore = AuthTokenStore()
        tokenStore.clear()
        tokenStore.save(accessToken: "access-token", refreshToken: "refresh-token")

        MockURLProtocol.requestHandler = { request in
            XCTAssertEqual(request.value(forHTTPHeaderField: "Authorization"), "Bearer access-token")
            XCTAssertEqual(request.httpMethod, "POST")
            XCTAssertEqual(request.url?.path, "/v2/media/assets/upload")
            let contentType = try XCTUnwrap(request.value(forHTTPHeaderField: "Content-Type"))
            XCTAssertTrue(contentType.hasPrefix("multipart/form-data; boundary="))

            let bodyData = try XCTUnwrap(Self.requestBodyData(request))
            let bodyString = String(data: bodyData, encoding: .utf8) ?? ""
            XCTAssertTrue(bodyString.contains("name=\"asset_type\""))
            XCTAssertTrue(bodyString.contains("product_image"))
            XCTAssertTrue(bodyString.contains("name=\"file\""))
            XCTAssertTrue(bodyString.contains("filename=\"cover.png\""))
            XCTAssertTrue(bodyString.contains("Content-Type: image/png"))

            return Self.response(
                statusCode: 200,
                body: Self.mediaAssetEnvelope.data(using: .utf8) ?? Data()
            )
        }

        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)
        let fileData = Data([0x89, 0x50, 0x4E, 0x47])
        let asset = try await client.uploadMediaAsset(
            fileData: fileData,
            fileName: "cover.png",
            mimeType: "image/png",
            assetType: "product_image"
        )

        XCTAssertEqual(asset.id.rawValue, "99001")
        XCTAssertEqual(asset.mimeType, "image/png")
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

    private static func response(statusCode: Int, body: Data) -> (HTTPURLResponse, Data) {
        let url = URL(string: "https://example.com")!
        let response = HTTPURLResponse(
            url: url,
            statusCode: statusCode,
            httpVersion: "HTTP/1.1",
            headerFields: ["Content-Type": "application/json"]
        )!
        return (response, body)
    }

    private static func streamResponse() -> (HTTPURLResponse, Data) {
        let url = URL(string: "https://example.com")!
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
