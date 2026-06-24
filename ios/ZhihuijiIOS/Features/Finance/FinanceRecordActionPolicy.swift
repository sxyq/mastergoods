import Foundation

struct FinanceRecordActionPolicy: Equatable {
    let canWriteFinance: Bool

    static func resolve(for permissions: Set<Permission>) -> FinanceRecordActionPolicy {
        FinanceRecordActionPolicy(canWriteFinance: permissions.contains(.financeWrite))
    }
}
