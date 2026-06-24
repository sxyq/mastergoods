import Foundation

struct DashboardQuickEntryPolicy: Equatable {
    let canOpenSales: Bool
    let canOpenPurchase: Bool
    let canOpenAgent: Bool
    let canOpenInventory: Bool
    let canOpenFinance: Bool

    static func resolve(for permissions: Set<Permission>) -> DashboardQuickEntryPolicy {
        DashboardQuickEntryPolicy(
            canOpenSales: permissions.contains(.salesView),
            canOpenPurchase: permissions.contains(.purchaseView),
            canOpenAgent: permissions.contains(.agentView),
            canOpenInventory: permissions.contains(.inventoryView),
            canOpenFinance: permissions.contains(.financeView)
        )
    }
}
