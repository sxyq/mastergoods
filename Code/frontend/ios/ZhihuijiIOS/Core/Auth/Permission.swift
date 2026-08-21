import Foundation

enum Permission: String, Codable, CaseIterable, Hashable {
    case dashboardView = "dashboard:view"
    case salesView = "sales:view"
    case salesWrite = "sales:write"
    case purchaseView = "purchase:view"
    case purchaseWrite = "purchase:write"
    case archivesView = "archives:view"
    case archivesWrite = "archives:write"
    case inventoryView = "inventory:view"
    case inventoryWrite = "inventory:write"
    case financeView = "finance:view"
    case financeWrite = "finance:write"
    case reportsView = "reports:view"
    case agentView = "agent:view"
    case agentWrite = "agent:write"
    case databaseManage = "database:manage"
    case settingsManage = "settings:manage"
    case usersManage = "users:manage"

    var displayName: String {
        switch self {
        case .dashboardView: return "首页查看"
        case .salesView: return "销售查看"
        case .salesWrite: return "销售写入"
        case .purchaseView: return "采购查看"
        case .purchaseWrite: return "采购写入"
        case .archivesView: return "档案查看"
        case .archivesWrite: return "档案写入"
        case .inventoryView: return "库存查看"
        case .inventoryWrite: return "库存写入"
        case .financeView: return "资金查看"
        case .financeWrite: return "资金写入"
        case .reportsView: return "报表查看"
        case .agentView: return "AI 查看"
        case .agentWrite: return "AI 写入"
        case .databaseManage: return "数据库管理"
        case .settingsManage: return "设置管理"
        case .usersManage: return "店员管理"
        }
    }
}
