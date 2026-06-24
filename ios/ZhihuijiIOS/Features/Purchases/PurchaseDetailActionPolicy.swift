import Foundation

struct PurchaseDetailActionPolicy: Equatable {
    let canOpenReceipt: Bool
    let canOpenReturn: Bool
    let canOpenPayOrder: Bool

    static func resolve(for permissions: Set<Permission>) -> PurchaseDetailActionPolicy {
        PurchaseDetailActionPolicy(
            canOpenReceipt: permissions.contains(.inventoryWrite),
            canOpenReturn: permissions.contains(.purchaseWrite),
            canOpenPayOrder: permissions.contains(.financeView)
        )
    }
}
