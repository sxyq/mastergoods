import Foundation

struct PurchaseReceiptActionPolicy: Equatable {
    let canEditDraft: Bool
    let canCancel: Bool
    let canCreateReceipt: Bool

    static func resolve(for permissions: Set<Permission>, receipt: PurchaseReceiptRecord? = nil) -> PurchaseReceiptActionPolicy {
        let canWriteInventory = permissions.contains(.inventoryWrite)
        return PurchaseReceiptActionPolicy(
            canEditDraft: canWriteInventory && (receipt?.canEditDraft ?? false),
            canCancel: canWriteInventory && (receipt?.canCancel ?? false),
            canCreateReceipt: canWriteInventory
        )
    }
}
