import Foundation

struct PartnerSummary: Identifiable, Codable, Equatable {
    let id: EntityID
    let name: String
    let phone: String?
    let balance: Double?
}

struct PartnerGroupRecord: Identifiable, Codable, Equatable {
    let id: EntityID
    let partnerType: String
    let name: String
    let status: Int?
    let sortOrder: Int?
    let createdAt: Int64?
    let updatedAt: Int64?
}

struct CustomerRecord: Identifiable, Codable, Equatable {
    let id: EntityID
    let name: String
    let phone: String
    let level: Int?
    let groupId: EntityID?
    let groupName: String?
    let primaryContactName: String?
    let primaryContactPhone: String?
    let address: String?
    let notes: String?
    let balance: Double?
    let status: Int?
    let createdAt: Int64?
    let updatedAt: Int64?
}

struct SupplierRecord: Identifiable, Codable, Equatable {
    let id: EntityID
    let name: String
    let phone: String
    let groupId: EntityID?
    let groupName: String?
    let primaryContactName: String?
    let primaryContactPhone: String?
    let address: String?
    let notes: String?
    let balance: Double?
    let status: Int?
    let createdAt: Int64?
    let updatedAt: Int64?
}

struct CustomerWritePayload: Codable, Equatable {
    let name: String
    let phone: String
    let level: Int
    let groupId: EntityID?
    let primaryContactName: String?
    let primaryContactPhone: String?
    let address: String?
    let notes: String?
    let balance: Double?
    let status: Int?
}

struct SupplierWritePayload: Codable, Equatable {
    let name: String
    let phone: String
    let groupId: EntityID?
    let primaryContactName: String?
    let primaryContactPhone: String?
    let address: String?
    let notes: String?
    let balance: Double?
    let status: Int?
}
