import Foundation

struct SalesDetailActionPolicy: Equatable {
    let canOpenPayment: Bool

    static func resolve(for permissions: Set<Permission>) -> SalesDetailActionPolicy {
        SalesDetailActionPolicy(canOpenPayment: permissions.contains(.financeWrite))
    }
}
