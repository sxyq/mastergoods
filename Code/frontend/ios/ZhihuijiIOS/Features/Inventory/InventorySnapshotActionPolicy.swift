import Foundation

struct InventorySnapshotActionPolicy: Equatable {
    let canManageInventory: Bool

    static func resolve(for permissions: Set<Permission>) -> InventorySnapshotActionPolicy {
        InventorySnapshotActionPolicy(canManageInventory: permissions.contains(.inventoryWrite))
    }
}
