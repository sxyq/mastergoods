import Foundation

struct InventoryAdjustActionPolicy: Equatable {
    let canAdjustInventory: Bool

    static func resolve(for permissions: Set<Permission>) -> InventoryAdjustActionPolicy {
        InventoryAdjustActionPolicy(canAdjustInventory: permissions.contains(.inventoryWrite))
    }
}
