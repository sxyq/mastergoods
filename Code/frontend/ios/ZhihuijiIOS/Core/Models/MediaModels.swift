import Foundation

struct MediaAssetRecord: Identifiable, Codable, Equatable {
    let id: EntityID
    let assetType: String
    let storageProvider: String
    let bucketName: String?
    let objectKey: String
    let originalFileName: String
    let mimeType: String
    let sizeBytes: Int64
    let checksum: String?
    let width: Int?
    let height: Int?
    let metadataJson: String?
    let createdAt: Int64
    let updatedAt: Int64
}

struct MediaAssetCreatePayload: Codable {
    let assetType: String
    let storageProvider: String
    let bucketName: String?
    let objectKey: String
    let originalFileName: String
    let mimeType: String
    let sizeBytes: Int64
    let checksum: String?
    let width: Int?
    let height: Int?
    let metadataJson: String?
}

struct MediaBindingRecord: Identifiable, Codable, Equatable {
    let id: EntityID
    let assetId: EntityID
    let targetType: String
    let targetId: EntityID
    let sortOrder: Int?
    let createdAt: Int64
}

struct MediaBindingCreatePayload: Codable {
    let assetId: EntityID
    let targetType: String
    let targetId: EntityID
    let sortOrder: Int?
}
