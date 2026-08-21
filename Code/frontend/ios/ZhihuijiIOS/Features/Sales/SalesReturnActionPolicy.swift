import Foundation

struct SalesReturnActionPolicy: Equatable {
    let canEditDraft: Bool
    let canRefund: Bool
    let canCancel: Bool
    let canCreateReturn: Bool

    static func resolve(for permissions: Set<Permission>, returnRecord: SalesReturnRecord? = nil) -> SalesReturnActionPolicy {
        let canWriteSales = permissions.contains(.salesWrite)
        let canWriteFinance = permissions.contains(.financeWrite)
        return SalesReturnActionPolicy(
            canEditDraft: canWriteSales && (returnRecord?.canEditDraft ?? false),
            canRefund: canWriteFinance && (returnRecord?.canRefund ?? false),
            canCancel: canWriteSales && (returnRecord?.canCancel ?? false),
            canCreateReturn: canWriteSales
        )
    }
}
