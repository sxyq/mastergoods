import Foundation

struct SalesPaymentActionPolicy: Equatable {
    let canWriteFinance: Bool

    static func resolve(for permissions: Set<Permission>) -> SalesPaymentActionPolicy {
        SalesPaymentActionPolicy(canWriteFinance: permissions.contains(.financeWrite))
    }
}
