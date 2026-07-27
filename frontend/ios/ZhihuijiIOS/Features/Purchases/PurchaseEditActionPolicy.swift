import Foundation

struct PurchaseEditActionPolicy: Equatable {
    let canCreatePurchase: Bool

    static func resolve(for permissions: Set<Permission>) -> PurchaseEditActionPolicy {
        PurchaseEditActionPolicy(canCreatePurchase: permissions.contains(.purchaseWrite))
    }
}
