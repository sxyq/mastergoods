import Foundation

struct DocumentsHomeAccessPolicy: Equatable {
    let canViewSales: Bool
    let canViewPurchase: Bool
    let canViewFinance: Bool
    let canViewInventory: Bool

    static func resolve(for permissions: Set<Permission>) -> DocumentsHomeAccessPolicy {
        DocumentsHomeAccessPolicy(
            canViewSales: permissions.contains(.salesView),
            canViewPurchase: permissions.contains(.purchaseView),
            canViewFinance: permissions.contains(.financeView),
            canViewInventory: permissions.contains(.inventoryView)
        )
    }
}
