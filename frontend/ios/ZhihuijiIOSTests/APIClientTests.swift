import XCTest
@testable import ZhihuijiIOS

final class APIClientTests: XCTestCase {
    func testEndpointPathsAreStable() {
        XCTAssertEqual(APIEndpoint.currentStore.path, "/v2/stores/current")
        XCTAssertEqual(APIEndpoint.storeMembers.path, "/v2/stores/current/members")
        XCTAssertEqual(APIEndpoint.agentWorkbench.path, "/v2/agent/workbench")
        XCTAssertEqual(APIEndpoint.refresh.path, "/v2/auth/refresh")
    }

    func testAuthPayloadDecodesLargeUserIDAsEntityID() throws {
        let data = Data(
            """
            {
              "user_id": 9223372036854775000,
              "token": "access-token",
              "refresh_token": "refresh-token",
              "expires_in": 7200
            }
            """.utf8
        )

        let payload = try JSONDecoder().decode(AuthPayload.self, from: data)
        XCTAssertEqual(payload.userId.rawValue, "9223372036854775000")
        XCTAssertEqual(payload.token, "access-token")
        XCTAssertEqual(payload.refreshToken, "refresh-token")
    }

    func testAppEnvironmentResolvesValidBaseURL() {
        let environment = AppEnvironment.environment(from: "https://example.com:8443")
        XCTAssertEqual(environment?.apiBaseURL.absoluteString, "https://example.com:8443")
        XCTAssertNil(AppEnvironment.environment(from: "not-a-url"))
    }

    func testAppEnvironmentNormalizesAndRejectsInvalidBaseURLs() {
        let trimmed = AppEnvironment.environment(from: "  http://127.0.0.1:8080  ")
        XCTAssertEqual(trimmed?.apiBaseURL.absoluteString, "http://127.0.0.1:8080")

        XCTAssertNil(AppEnvironment.environment(from: "ftp://example.com"))
        XCTAssertNil(AppEnvironment.environment(from: "https:///missing-host"))
        XCTAssertNil(AppEnvironment.environment(from: ""))
    }

    func testAppEnvironmentStripsEndpointVersionSuffixFromBaseURL() {
        let v1Root = AppEnvironment.environment(from: "https://example.com/v1")
        let v2Root = AppEnvironment.environment(from: "https://example.com/v2/")
        let prefixed = AppEnvironment.environment(from: "https://example.com/zhihuiji/v2?debug=1")
        let stablePrefix = AppEnvironment.environment(from: "https://example.com/api")

        XCTAssertEqual(v1Root?.apiBaseURL.absoluteString, "https://example.com")
        XCTAssertEqual(v2Root?.apiBaseURL.absoluteString, "https://example.com")
        XCTAssertEqual(prefixed?.apiBaseURL.absoluteString, "https://example.com/zhihuiji?debug=1")
        XCTAssertEqual(stablePrefix?.apiBaseURL.absoluteString, "https://example.com/api")
    }

    func testArchiveRelatedEndpointsAreStable() {
        XCTAssertEqual(APIEndpoint.products.path, "/v2/products")
        XCTAssertEqual(APIEndpoint.currentStore.path, "/v2/stores/current")
    }

    func testInventoryRelatedEndpointsAreStable() {
        XCTAssertEqual(APIEndpoint.inventorySnapshots.path, "/v2/inventory/snapshots")
        XCTAssertEqual(APIEndpoint.inventoryMonthlyStats.path, "/v2/inventory/monthly-stats")
        XCTAssertEqual(APIEndpoint.inventoryLedger.path, "/v2/inventory/ledger")
    }

    func testMediaAndSyncEndpointsAreStable() {
        XCTAssertEqual(APIEndpoint.mediaAssets.path, "/v2/media/assets")
        XCTAssertEqual(APIEndpoint.mediaBindings.path, "/v2/media/bindings")
        XCTAssertEqual(APIEndpoint.syncHealth.path, "/v2/sync/health")
        XCTAssertEqual(APIEndpoint.importJobs.path, "/v2/import-jobs")
    }

    func testLegacySQLiteImportPayloadEncodesSnakeCaseShape() throws {
        let encoder = JSONEncoder()
        encoder.keyEncodingStrategy = .convertToSnakeCase
        let payload = LegacySQLiteImportPayload(
            legacyDbPath: "/tmp/legacy.db",
            resetOwnedData: true
        )

        let data = try encoder.encode(payload)
        let json = try XCTUnwrap(try JSONSerialization.jsonObject(with: data) as? [String: Any])
        XCTAssertEqual(json["legacy_db_path"] as? String, "/tmp/legacy.db")
        XCTAssertEqual(json["reset_owned_data"] as? Bool, true)
    }

    func testMediaAssetCreatePayloadEncodesSnakeCaseShape() throws {
        let encoder = JSONEncoder()
        encoder.keyEncodingStrategy = .convertToSnakeCase
        let payload = MediaAssetCreatePayload(
            assetType: "product_cover",
            storageProvider: "object_storage",
            bucketName: "master-goods",
            objectKey: "assets/cover-1.png",
            originalFileName: "cover-1.png",
            mimeType: "image/png",
            sizeBytes: 2048,
            checksum: "sha256:abc123",
            width: 1280,
            height: 720,
            metadataJson: #"{"source":"ios"}"#
        )

        let data = try encoder.encode(payload)
        let json = try XCTUnwrap(try JSONSerialization.jsonObject(with: data) as? [String: Any])

        XCTAssertEqual(json["asset_type"] as? String, "product_cover")
        XCTAssertEqual(json["storage_provider"] as? String, "object_storage")
        XCTAssertEqual(json["object_key"] as? String, "assets/cover-1.png")
        XCTAssertEqual(json["metadata_json"] as? String, #"{"source":"ios"}"#)
    }

    func testMediaBindingCreatePayloadEncodesSnakeCaseShape() throws {
        let encoder = JSONEncoder()
        encoder.keyEncodingStrategy = .convertToSnakeCase
        let payload = MediaBindingCreatePayload(
            assetId: EntityID(rawValue: "990000000000001"),
            targetType: "product",
            targetId: EntityID(rawValue: "500000000000001"),
            sortOrder: 2
        )

        let data = try encoder.encode(payload)
        let json = try XCTUnwrap(try JSONSerialization.jsonObject(with: data) as? [String: Any])

        XCTAssertEqual(json["asset_id"] as? String, "990000000000001")
        XCTAssertEqual(json["target_type"] as? String, "product")
        XCTAssertEqual(json["target_id"] as? String, "500000000000001")
        XCTAssertEqual(json["sort_order"] as? Int, 2)
    }

    func testSyncUploadAndPullPayloadsEncodeSnakeCaseShape() throws {
        let encoder = JSONEncoder()
        encoder.keyEncodingStrategy = .convertToSnakeCase
        let change = SyncChangeRecord(
            entityType: "product",
            entityId: EntityID(rawValue: "500000000000001"),
            operation: "upsert",
            payload: #"{"name":"iOS"}"#,
            updatedAt: 1710000000000
        )
        let upload = SyncUploadPayload(
            clientId: "ios-device-1",
            changes: [change],
            lastSyncCursor: "cursor-001"
        )
        let pull = SyncPullPayload(
            clientId: "ios-device-1",
            sinceCursor: "cursor-001",
            limit: 50
        )

        let uploadJSON = try XCTUnwrap(try JSONSerialization.jsonObject(with: encoder.encode(upload)) as? [String: Any])
        let pullJSON = try XCTUnwrap(try JSONSerialization.jsonObject(with: encoder.encode(pull)) as? [String: Any])
        let changes = try XCTUnwrap(uploadJSON["changes"] as? [[String: Any]])
        let firstChange = try XCTUnwrap(changes.first)

        XCTAssertEqual(uploadJSON["client_id"] as? String, "ios-device-1")
        XCTAssertEqual(uploadJSON["last_sync_cursor"] as? String, "cursor-001")
        XCTAssertEqual(firstChange["entity_type"] as? String, "product")
        XCTAssertEqual(firstChange["entity_id"] as? String, "500000000000001")
        XCTAssertEqual((firstChange["updated_at"] as? NSNumber)?.int64Value, 1710000000000)
        XCTAssertEqual(pullJSON["client_id"] as? String, "ios-device-1")
        XCTAssertEqual(pullJSON["since_cursor"] as? String, "cursor-001")
        XCTAssertEqual(pullJSON["limit"] as? Int, 50)
    }

    func testImportJobPayloadsEncodeSnakeCaseShape() throws {
        let encoder = JSONEncoder()
        encoder.keyEncodingStrategy = .convertToSnakeCase
        let createPayload = ImportJobCreatePayload(
            clientId: "ios-device-1",
            sourceType: "legacy_sqlite",
            sourceUri: "file:///tmp/legacy.db",
            sourceChecksum: "sha256:abc",
            idempotencyKey: "import-001",
            replayCursor: "cursor-001",
            optionsJson: #"{"dry_run":false}"#
        )
        let retryPayload = ImportJobRetryPayload(replayCursor: "cursor-retry")

        let createJSON = try XCTUnwrap(try JSONSerialization.jsonObject(with: encoder.encode(createPayload)) as? [String: Any])
        let retryJSON = try XCTUnwrap(try JSONSerialization.jsonObject(with: encoder.encode(retryPayload)) as? [String: Any])

        XCTAssertEqual(createJSON["client_id"] as? String, "ios-device-1")
        XCTAssertEqual(createJSON["source_type"] as? String, "legacy_sqlite")
        XCTAssertEqual(createJSON["source_uri"] as? String, "file:///tmp/legacy.db")
        XCTAssertEqual(createJSON["source_checksum"] as? String, "sha256:abc")
        XCTAssertEqual(createJSON["idempotency_key"] as? String, "import-001")
        XCTAssertEqual(createJSON["replay_cursor"] as? String, "cursor-001")
        XCTAssertEqual(createJSON["options_json"] as? String, #"{"dry_run":false}"#)
        XCTAssertEqual(retryJSON["replay_cursor"] as? String, "cursor-retry")
    }

    func testAgentDraftPayloadEncodesBackendStatuses() throws {
        let encoder = JSONEncoder()
        encoder.keyEncodingStrategy = .convertToSnakeCase

        let createPayload = AgentDraftCreatePayload(
            conversationId: EntityID(rawValue: "90001"),
            draftType: "question",
            title: "补货建议",
            contentJson: #"{"question":"今天要补哪些货？"}"#,
            status: AgentContractStatus.active
        )
        let updatePayload = AgentDraftUpdatePayload(
            conversationId: EntityID(rawValue: "90001"),
            draftType: "question",
            title: "补货建议-归档",
            contentJson: #"{"question":"今天要补哪些货？"}"#,
            status: AgentContractStatus.archived
        )

        let createData = try encoder.encode(createPayload)
        let updateData = try encoder.encode(updatePayload)
        let createJSON = try XCTUnwrap(try JSONSerialization.jsonObject(with: createData) as? [String: Any])
        let updateJSON = try XCTUnwrap(try JSONSerialization.jsonObject(with: updateData) as? [String: Any])

        XCTAssertEqual(createJSON["status"] as? String, AgentContractStatus.active)
        XCTAssertEqual(updateJSON["status"] as? String, AgentContractStatus.archived)
        XCTAssertEqual(createJSON["conversation_id"] as? String, "90001")
    }

    func testAgentContractStatusConstantsMatchBackend() {
        XCTAssertEqual(AgentContractStatus.active, "active")
        XCTAssertEqual(AgentContractStatus.closed, "closed")
        XCTAssertEqual(AgentContractStatus.archived, "archived")
    }
}
