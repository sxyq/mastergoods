import Foundation

struct ProductRecord: Identifiable, Codable, Equatable {
    let id: EntityID
    let code: String
    let name: String
    let categoryId: Int64?
    let categoryName: String?
    let unitId: Int64?
    let unitName: String?
    let salePrice: Double
    let purchasePrice: Double
    let priceLevels: [ProductPriceLevelValue]?
    let defaultSupplier: ProductSupplierRelation?
    let supplierRelations: [ProductSupplierRelation]?
    let stock: Double
    let safeStock: Double
    let status: Int
    let createdAt: Int64?
    let updatedAt: Int64?
}

struct ProductPriceLevelValue: Identifiable, Codable, Equatable {
    let levelId: EntityID
    let code: String
    let name: String
    let price: Double
    let status: Int?
    let sortOrder: Int?

    var id: EntityID { levelId }
}

struct ProductSupplierRelation: Identifiable, Codable, Equatable {
    let id: EntityID?
    let productId: EntityID?
    let supplierId: EntityID
    let supplierName: String
    let supplierPhone: String?
    let isDefault: Bool?
    let purchasePriority: Int?
    let lastPurchasePrice: Double?
    let notes: String?
    let createdAt: Int64?
    let updatedAt: Int64?

    var stableId: String { id?.rawValue ?? supplierId.rawValue }
}

struct ProductCategoryRecord: Identifiable, Codable, Equatable {
    let id: EntityID
    let name: String
    let status: Int?
    let sortOrder: Int?
    let createdAt: Int64?
    let updatedAt: Int64?
}

struct ProductUnitRecord: Identifiable, Codable, Equatable {
    let id: EntityID
    let name: String
    let status: Int?
    let sortOrder: Int?
    let createdAt: Int64?
    let updatedAt: Int64?
}

struct ProductPriceLevelRecord: Identifiable, Codable, Equatable {
    let id: EntityID
    let code: String
    let name: String
    let status: Int?
    let sortOrder: Int?
    let createdAt: Int64?
    let updatedAt: Int64?
}

struct ProductWritePayload: Codable {
    let code: String
    let name: String
    let categoryId: EntityID
    let unitId: EntityID
    let salePrice: Double
    let purchasePrice: Double
    let priceLevels: [ProductPriceLevelWritePayload]?
    let supplierRelations: [ProductSupplierRelationWritePayload]?
    let stock: Double
    let safeStock: Double
    let status: Int
}

struct ProductPriceLevelWritePayload: Codable {
    let levelId: EntityID
    let price: Double
}

struct ProductSupplierRelationWritePayload: Codable {
    let productId: EntityID
    let supplierId: EntityID
    let isDefault: Bool?
    let purchasePriority: Int?
    let lastPurchasePrice: Double?
    let notes: String?
}
