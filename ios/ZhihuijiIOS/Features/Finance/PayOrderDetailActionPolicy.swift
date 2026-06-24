import Foundation

struct PayOrderDetailActionPolicy: Equatable {
    let canWriteFinance: Bool

    static func resolve(for permissions: Set<Permission>) -> PayOrderDetailActionPolicy {
        PayOrderDetailActionPolicy(canWriteFinance: permissions.contains(.financeWrite))
    }
}
