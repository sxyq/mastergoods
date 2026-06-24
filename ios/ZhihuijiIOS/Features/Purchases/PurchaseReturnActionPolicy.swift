import Foundation

struct PurchaseReturnActionPolicy: Equatable {
    let canEditDraft: Bool
    let canRefund: Bool
    let canCancel: Bool
    let canCreateReturn: Bool

    static func resolve(for permissions: Set<Permission>, returnRecord: PurchaseReturnRecord? = nil) -> PurchaseReturnActionPolicy {
        let canWritePurchase = permissions.contains(.purchaseWrite)
        let canWriteFinance = permissions.contains(.financeWrite)
        return PurchaseReturnActionPolicy(
            canEditDraft: canWritePurchase && (returnRecord?.canEditDraft ?? false),
            canRefund: canWriteFinance && (returnRecord?.canRefund ?? false),
            canCancel: canWritePurchase && (returnRecord?.canCancel ?? false),
            canCreateReturn: canWritePurchase
        )
    }
}
