import Foundation

struct ProductListActionPolicy: Equatable {
    let canCreateProduct: Bool

    static func resolve(for permissions: Set<Permission>) -> ProductListActionPolicy {
        ProductListActionPolicy(canCreateProduct: permissions.contains(.archivesWrite))
    }
}
