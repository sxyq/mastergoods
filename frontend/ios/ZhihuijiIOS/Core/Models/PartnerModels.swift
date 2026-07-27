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

// MARK: - Contact (FE8 联系人)

struct ContactRecord: Identifiable, Codable, Equatable {
    let id: EntityID
    let partnerType: String
    let partnerId: EntityID
    let name: String
    let phone: String?
    let title: String?
    let isPrimary: Bool
    let createdAt: Int64
    let updatedAt: Int64
}

struct ContactWritePayload: Codable, Equatable {
    let partnerId: EntityID
    let name: String
    let phone: String?
    let title: String?
    let isPrimary: Bool?
}

enum PartnerContactKind: String, CaseIterable, Identifiable {
    case customer
    case supplier

    var id: String { rawValue }

    var label: String {
        switch self {
        case .customer: return "客户"
        case .supplier: return "供应商"
        }
    }

    var apiPath: String {
        switch self {
        case .customer: return "/v2/customer-contacts"
        case .supplier: return "/v2/supplier-contacts"
        }
    }

    var queryParam: String {
        switch self {
        case .customer: return "customer_id"
        case .supplier: return "supplier_id"
        }
    }
}

extension ContactRecord {
    var kind: PartnerContactKind? {
        PartnerContactKind(rawValue: partnerType)
    }
}
