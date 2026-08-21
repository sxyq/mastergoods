import XCTest
@testable import ZhihuijiIOS

final class InventorySnapshotActionPolicyTests: XCTestCase {
    func testInventorySnapshotActionPolicyMatchesPermissionMatrix() {
        let owner = InventorySnapshotActionPolicy.resolve(for: Set(Permission.allCases))
        XCTAssertTrue(owner.canManageInventory)

        let warehouse = InventorySnapshotActionPolicy.resolve(for: PermissionPolicy.permissions(for: .warehouse))
        XCTAssertTrue(warehouse.canManageInventory)

        let sales = InventorySnapshotActionPolicy.resolve(for: PermissionPolicy.permissions(for: .sales))
        XCTAssertFalse(sales.canManageInventory)

        let assistant = InventorySnapshotActionPolicy.resolve(for: PermissionPolicy.permissions(for: .assistant))
        XCTAssertFalse(assistant.canManageInventory)
    }
}
