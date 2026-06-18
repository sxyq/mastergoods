import Foundation

enum APIEndpoint {
    case login
    case refresh
    case logout
    case currentUser
    case currentStore
    case storeMembers
    case dashboardSummary
    case saleOrders
    case purchaseOrders
    case products
    case inventorySnapshots
    case inventoryMonthlyStats
    case inventoryLedger
    case reports
    case agentWorkbench

    var path: String {
        switch self {
        case .login:
            return "/v1/auth/login"
        case .refresh:
            return "/v1/auth/refresh"
        case .logout:
            return "/v1/auth/logout"
        case .currentUser:
            return "/v1/auth/users/me"
        case .currentStore:
            return "/v2/stores/current"
        case .storeMembers:
            return "/v2/stores/current/members"
        case .dashboardSummary:
            return "/v1/reports/sales-summary"
        case .saleOrders:
            return "/v2/sale-orders"
        case .purchaseOrders:
            return "/v2/purchase-orders"
        case .products:
            return "/v2/products"
        case .inventorySnapshots:
            return "/v2/inventory/snapshots"
        case .inventoryMonthlyStats:
            return "/v2/inventory/monthly-stats"
        case .inventoryLedger:
            return "/v2/inventory/ledger"
        case .reports:
            return "/v1/reports/sales-trend"
        case .agentWorkbench:
            return "/v2/agent/workbench"
        }
    }
}
