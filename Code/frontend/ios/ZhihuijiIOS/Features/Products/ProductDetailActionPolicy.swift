import Foundation

struct ProductDetailActionPolicy: Equatable {
    let canEditProduct: Bool
    let canOpenInventoryAdjust: Bool

    static func resolve(for permissions: Set<Permission>) -> ProductDetailActionPolicy {
        ProductDetailActionPolicy(
            canEditProduct: permissions.contains(.archivesWrite),
            canOpenInventoryAdjust: permissions.contains(.inventoryWrite)
        )
    }
}
