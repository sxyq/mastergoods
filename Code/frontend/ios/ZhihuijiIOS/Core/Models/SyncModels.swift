import Foundation

struct SyncHealthRecord: Codable, Equatable {
    let status: String
    let message: String
    let ownerScoped: Bool
    let serverTime: Int64
    let supportedEntityTypes: [String]
    let uploadableEntityTypes: [String]
}

struct SyncCursorRecord: Codable, Equatable {
    let clientId: String
    let lastCursor: String
    let updatedAt: Int64
}

struct SyncCursorAckPayload: Codable {
    let clientId: String
    let cursor: String
}

struct SyncChangeRecord: Identifiable, Codable, Equatable {
    let entityType: String
    let entityId: EntityID
    let operation: String
    let payload: String?
    let updatedAt: Int64?

    var id: String {
        "\(entityType):\(entityId.rawValue):\(operation):\(updatedAt ?? 0)"
    }
}

struct SyncUploadPayload: Codable {
    let clientId: String
    let changes: [SyncChangeRecord]
    let lastSyncCursor: String?
}

struct SyncUploadResponse: Codable, Equatable {
    let acceptedCount: Int
    let failedCount: Int
    let status: String
    let nextCursor: String
}

struct SyncPullPayload: Codable {
    let clientId: String
    let sinceCursor: String?
    let limit: Int?
}

struct SyncPullResponse: Codable, Equatable {
    let changes: [SyncChangeRecord]
    let effectiveCursor: String?
    let nextCursor: String
    let hasMore: Bool
}

struct ImportJobRecord: Identifiable, Codable, Equatable {
    let id: EntityID
    let clientId: String
    let sourceType: String
    let sourceUri: String?
    let sourceChecksum: String?
    let idempotencyKey: String?
    let status: String
    let stage: String?
    let retryCount: Int
    let replayCursor: String?
    let summaryJson: String?
    let optionsJson: String?
    let failureCode: String?
    let failureMessage: String?
    let createdAt: Int64
    let updatedAt: Int64
    let startedAt: Int64?
    let finishedAt: Int64?
    let lastHeartbeatAt: Int64?
}

struct ImportJobCreatePayload: Codable {
    let clientId: String
    let sourceType: String
    let sourceUri: String?
    let sourceChecksum: String?
    let idempotencyKey: String?
    let replayCursor: String?
    let optionsJson: String?
}

struct ImportJobRetryPayload: Codable {
    let replayCursor: String?
}

struct LegacySQLiteImportPayload: Codable {
    let legacyDbPath: String
    let resetOwnedData: Bool?
}

struct LegacySQLiteImportResult: Codable, Equatable {
    let userId: EntityID
    let phone: String
    let nickname: String
    let legacyDbPath: String
    let accounts: Int
    let customers: Int
    let suppliers: Int
    let products: Int
    let saleOrders: Int
    let saleOrderItems: Int
    let payments: Int
    let purchaseOrders: Int
    let purchaseOrderItems: Int
    let payOrders: Int
    let financeRecords: Int
    let inventorySnapshots: Int
}
