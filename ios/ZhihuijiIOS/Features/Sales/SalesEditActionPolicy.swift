import Foundation

struct SalesEditActionPolicy: Equatable {
    let canCreateSale: Bool

    static func resolve(for permissions: Set<Permission>) -> SalesEditActionPolicy {
        SalesEditActionPolicy(canCreateSale: permissions.contains(.salesWrite))
    }
}
