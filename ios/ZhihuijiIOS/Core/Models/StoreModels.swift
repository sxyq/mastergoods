import Foundation

enum StoreRole: String, Codable, CaseIterable {
    case owner = "OWNER"
    case manager = "MANAGER"
    case sales = "SALES"
    case purchasing = "PURCHASING"
    case warehouse = "WAREHOUSE"
    case finance = "FINANCE"
    case assistant = "ASSISTANT"

    var label: String {
        switch self {
        case .owner: return "店长（总）"
        case .manager: return "店长助理"
        case .sales: return "销售员工"
        case .purchasing: return "采购员工"
        case .warehouse: return "仓库员工"
        case .finance: return "财务员工"
        case .assistant: return "AI/只读助理"
        }
    }
}

struct CurrentStoreProfile: Codable, Equatable {
    let storeId: EntityID
    let storeName: String
    let ownerUserId: EntityID
    let currentUserId: EntityID
    let currentUserName: String
    let currentUserPhone: String
    let role: StoreRole
    let title: String
    let status: Int
    let permissions: [Permission]
    let memberCount: Int
    let enabledMemberCount: Int
    let disabledMemberCount: Int
}

struct StoreStaffMember: Codable, Identifiable, Equatable {
    let userId: EntityID
    let phone: String
    let nickname: String
    let role: StoreRole
    let title: String
    let status: Int
    let permissions: [Permission]
    let createdAt: Int64
    let updatedAt: Int64
    let activeSessions: Int
    let storeId: EntityID
    let storeName: String

    var id: EntityID { userId }
}

struct StoreMemberCreatePayload: Codable {
    let phone: String
    let password: String
    let nickname: String
    let role: StoreRole
    let title: String?
    let status: Int?
}

struct StoreMemberUpdatePayload: Codable {
    let nickname: String?
    let password: String?
    let role: StoreRole?
    let title: String?
    let status: Int?
    let keepSessions: Bool
}
