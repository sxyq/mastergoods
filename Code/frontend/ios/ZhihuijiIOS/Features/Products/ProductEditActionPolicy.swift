import Foundation

struct ProductEditActionPolicy: Equatable {
    let canSaveProduct: Bool

    static func resolve(for permissions: Set<Permission>) -> ProductEditActionPolicy {
        ProductEditActionPolicy(canSaveProduct: permissions.contains(.archivesWrite))
    }
}
