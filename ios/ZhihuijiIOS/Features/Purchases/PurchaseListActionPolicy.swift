import Foundation

struct PurchaseListActionPolicy: Equatable {
    let canOpenCreate: Bool

    static func resolve(for permissions: Set<Permission>) -> PurchaseListActionPolicy {
        PurchaseListActionPolicy(canOpenCreate: permissions.contains(.purchaseWrite))
    }
}
