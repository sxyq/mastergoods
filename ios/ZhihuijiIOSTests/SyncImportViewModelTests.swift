import XCTest
@testable import ZhihuijiIOS

@MainActor
final class SyncImportViewModelTests: XCTestCase {
    func testImportJobCreatePayloadRequiresSourceTypeAndLegacySQLiteURI() {
        let viewModel = SyncImportViewModel()
        viewModel.clientId = "ios-test-client"
        viewModel.jobSourceType = ""

        XCTAssertNil(viewModel.makeImportJobCreatePayload())
        XCTAssertEqual(viewModel.errorMessage, "请填写 source type。")

        viewModel.jobSourceType = "legacy_sqlite"
        viewModel.jobSourceUri = ""

        XCTAssertNil(viewModel.makeImportJobCreatePayload())
        XCTAssertEqual(viewModel.errorMessage, "legacy_sqlite 任务必须填写 source URI。")
    }

    func testImportJobCreatePayloadRejectsInvalidOptionsJSON() {
        let viewModel = SyncImportViewModel()
        viewModel.clientId = "ios-test-client"
        viewModel.jobSourceType = "legacy_sqlite"
        viewModel.jobSourceUri = "file:///tmp/legacy.db"
        viewModel.jobOptionsJson = "not-json"

        XCTAssertNil(viewModel.makeImportJobCreatePayload())
        XCTAssertEqual(viewModel.errorMessage, "Options JSON 必须是合法 JSON 对象。")
    }

    func testImportJobCreatePayloadUsesExplicitClientAndSourceFields() throws {
        let viewModel = SyncImportViewModel()
        viewModel.clientId = "ios-test-client"
        viewModel.jobSourceType = "legacy_sqlite"
        viewModel.jobSourceUri = "file:///tmp/legacy.db"
        viewModel.jobSourceChecksum = "sha256:legacy"
        viewModel.jobIdempotencyKey = "import-ios-001"
        viewModel.jobReplayCursor = "cursor-001"
        viewModel.jobOptionsJson = #"{"dry_run":false}"#

        let payload = try XCTUnwrap(viewModel.makeImportJobCreatePayload())

        XCTAssertEqual(payload.clientId, "ios-test-client")
        XCTAssertEqual(payload.sourceType, "legacy_sqlite")
        XCTAssertEqual(payload.sourceUri, "file:///tmp/legacy.db")
        XCTAssertEqual(payload.sourceChecksum, "sha256:legacy")
        XCTAssertEqual(payload.idempotencyKey, "import-ios-001")
        XCTAssertEqual(payload.replayCursor, "cursor-001")
        XCTAssertEqual(payload.optionsJson, #"{"dry_run":false}"#)
        XCTAssertNil(viewModel.errorMessage)
    }

    func testImportJobRetryPayloadIsNilWhenReplayCursorIsBlank() throws {
        let viewModel = SyncImportViewModel()

        XCTAssertNil(viewModel.makeImportJobRetryPayload())

        viewModel.jobReplayCursor = "cursor-retry"
        let payload = try XCTUnwrap(viewModel.makeImportJobRetryPayload())

        XCTAssertEqual(payload.replayCursor, "cursor-retry")
    }

    func testSyncUploadPayloadRequiresEntityFields() {
        let viewModel = SyncImportViewModel()
        viewModel.clientId = "ios-sync-client"

        XCTAssertNil(viewModel.makeSyncUploadPayload())
        XCTAssertEqual(viewModel.errorMessage, "请填写 upload entity type。")

        viewModel.syncUploadEntityType = "product"
        XCTAssertNil(viewModel.makeSyncUploadPayload())
        XCTAssertEqual(viewModel.errorMessage, "请填写 upload entity ID。")

        viewModel.syncUploadEntityId = "10001"
        viewModel.syncUploadOperation = ""
        XCTAssertNil(viewModel.makeSyncUploadPayload())
        XCTAssertEqual(viewModel.errorMessage, "请填写 upload operation。")
    }

    func testSyncUploadPayloadRejectsInvalidJSONAndTimestamp() {
        let viewModel = SyncImportViewModel()
        viewModel.clientId = "ios-sync-client"
        viewModel.syncUploadEntityType = "product"
        viewModel.syncUploadEntityId = "10001"
        viewModel.syncUploadOperation = "upsert"
        viewModel.syncUploadPayloadJson = "not-json"

        XCTAssertNil(viewModel.makeSyncUploadPayload())
        XCTAssertEqual(viewModel.errorMessage, "Upload payload JSON 必须是合法 JSON 对象。")

        viewModel.syncUploadPayloadJson = #"{"name":"iOS"}"#
        viewModel.syncUploadUpdatedAtText = "-1"

        XCTAssertNil(viewModel.makeSyncUploadPayload())
        XCTAssertEqual(viewModel.errorMessage, "Updated at 必须是大于 0 的毫秒时间戳。")
    }

    func testSyncUploadPayloadUsesSingleManualChange() throws {
        let viewModel = SyncImportViewModel()
        viewModel.clientId = "ios-sync-client"
        viewModel.syncUploadEntityType = "product"
        viewModel.syncUploadEntityId = "10001"
        viewModel.syncUploadOperation = "upsert"
        viewModel.syncUploadPayloadJson = #"{"name":"iOS"}"#
        viewModel.syncUploadUpdatedAtText = "1710000000000"
        viewModel.syncUploadLastCursor = "cursor-001"

        let payload = try XCTUnwrap(viewModel.makeSyncUploadPayload())

        XCTAssertEqual(payload.clientId, "ios-sync-client")
        XCTAssertEqual(payload.lastSyncCursor, "cursor-001")
        XCTAssertEqual(payload.changes.count, 1)
        XCTAssertEqual(payload.changes.first?.entityType, "product")
        XCTAssertEqual(payload.changes.first?.entityId.rawValue, "10001")
        XCTAssertEqual(payload.changes.first?.operation, "upsert")
        XCTAssertEqual(payload.changes.first?.payload, #"{"name":"iOS"}"#)
        XCTAssertEqual(payload.changes.first?.updatedAt, 1710000000000)
        XCTAssertNil(viewModel.errorMessage)
    }

    func testSyncPullPayloadValidatesLimitAndUsesClientCursor() throws {
        let viewModel = SyncImportViewModel()
        viewModel.clientId = "ios-sync-client"
        viewModel.syncPullSinceCursor = "cursor-001"
        viewModel.syncPullLimitText = "0"

        XCTAssertNil(viewModel.makeSyncPullPayload())
        XCTAssertEqual(viewModel.errorMessage, "Pull limit 必须是大于 0 的数值。")

        viewModel.syncPullLimitText = "50"
        let payload = try XCTUnwrap(viewModel.makeSyncPullPayload())

        XCTAssertEqual(payload.clientId, "ios-sync-client")
        XCTAssertEqual(payload.sinceCursor, "cursor-001")
        XCTAssertEqual(payload.limit, 50)
        XCTAssertNil(viewModel.errorMessage)
    }

    func testUnsupportedPulledEntityTypesCompareAgainstSyncHealth() {
        let viewModel = SyncImportViewModel()
        viewModel.health = SyncHealthRecord(
            status: "ok",
            message: "ready",
            ownerScoped: true,
            serverTime: 1710000000000,
            supportedEntityTypes: ["product", "customer"],
            uploadableEntityTypes: ["product"]
        )
        viewModel.syncPullResponse = SyncPullResponse(
            changes: [
                SyncChangeRecord(entityType: "product", entityId: "10001", operation: "upsert", payload: nil, updatedAt: 1710000000000),
                SyncChangeRecord(entityType: "cash_change_record", entityId: "20001", operation: "upsert", payload: nil, updatedAt: 1710000000001),
                SyncChangeRecord(entityType: "unknown_entity", entityId: "30001", operation: "delete", payload: nil, updatedAt: 1710000000002),
            ],
            effectiveCursor: "cursor-001",
            nextCursor: "cursor-002",
            hasMore: false
        )

        XCTAssertEqual(viewModel.unsupportedPulledEntityTypes, ["cash_change_record", "unknown_entity"])
    }
}
