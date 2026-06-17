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
}
