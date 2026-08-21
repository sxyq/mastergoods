import Foundation

struct InventorySnapshotSummary: Identifiable, Codable, Equatable {
    let id: EntityID
    let productId: EntityID
    let productCode: String
    let productName: String
    let warehouseId: EntityID?
    let quantity: Double
    let unitCost: Double?
    let totalValue: Double?
    let snapshotDate: Int64
    let createdAt: Int64
}

struct InventoryLedgerEntry: Identifiable, Codable, Equatable {
    let id: EntityID
    let productId: EntityID
    let productCode: String?
    let productName: String
    let warehouseId: EntityID?
    let quantityBefore: Double?
    let quantityChange: Double
    let quantityAfter: Double?
    let unitCost: Double?
    let sourceType: String
    let sourceId: EntityID?
    let sourceNo: String?
    let notes: String?
    let createdAt: Int64
}

struct InventoryLedgerCreatePayload: Codable {
    let productId: EntityID
    let sourceType: String
    let sourceId: EntityID?
    let sourceNo: String?
    let quantityChange: Double
    let unitCost: Double?
    let warehouseId: EntityID?
    let notes: String?
}

struct InventoryMonthlyStats: Identifiable, Codable, Equatable {
    let id: EntityID
    let productId: EntityID
    let productCode: String?
    let productName: String
    let warehouseId: EntityID?
    let month: Int
    let year: Int
    let quantityIn: Double?
    let quantityOut: Double?
    let quantityAdjust: Double?
    let quantityBegin: Double?
    let quantityEnd: Double?
    let totalCostIn: Double?
    let totalCostOut: Double?
    let createdAt: Int64?
    let updatedAt: Int64?
}

struct InventorySnapshotCreatePayload: Codable {
    let productId: EntityID
    let snapshotDate: Int64
    let warehouseId: EntityID?
}
