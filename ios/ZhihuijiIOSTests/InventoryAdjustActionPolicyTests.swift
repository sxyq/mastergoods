import XCTest
@testable import ZhihuijiIOS

final class InventoryAdjustActionPolicyTests: XCTestCase {
    func testInventoryAdjustActionPolicyMatchesPermissionMatrix() {
        let inventory = InventoryAdjustActionPolicy.resolve(for: PermissionPolicy.permissions(for: .warehouse))
        XCTAssertTrue(inventory.canAdjustInventory)

        let owner = InventoryAdjustActionPolicy.resolve(for: Set(Permission.allCases))
        XCTAssertTrue(owner.canAdjustInventory)

        let sales = InventoryAdjustActionPolicy.resolve(for: PermissionPolicy.permissions(for: .sales))
        XCTAssertFalse(sales.canAdjustInventory)

        let assistant = InventoryAdjustActionPolicy.resolve(for: PermissionPolicy.permissions(for: .assistant))
        XCTAssertFalse(assistant.canAdjustInventory)
    }
}
