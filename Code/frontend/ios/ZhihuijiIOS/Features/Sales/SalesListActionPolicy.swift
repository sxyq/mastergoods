import Foundation

struct SalesListActionPolicy: Equatable {
    let canOpenCreate: Bool

    static func resolve(for permissions: Set<Permission>) -> SalesListActionPolicy {
        SalesListActionPolicy(canOpenCreate: permissions.contains(.salesWrite))
    }
}
