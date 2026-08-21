import XCTest
@testable import ZhihuijiIOS

final class SettingsVisibilityPolicyTests: XCTestCase {
    func testSettingsVisibilityPolicyMatchesRoleCapabilities() {
        let owner = SettingsVisibilityPolicy.resolve(for: Set(Permission.allCases))
        XCTAssertTrue(owner.canManageStaff)
        XCTAssertTrue(owner.canAccessSyncImport)
        XCTAssertTrue(owner.canAccessMediaAssets)
        XCTAssertTrue(owner.canAccessPlanning)

        let sales = SettingsVisibilityPolicy.resolve(for: PermissionPolicy.permissions(for: .sales))
        XCTAssertFalse(sales.canManageStaff)
        XCTAssertFalse(sales.canAccessSyncImport)
        XCTAssertFalse(sales.canAccessMediaAssets)
        XCTAssertTrue(sales.canAccessPlanning)

        let warehouse = SettingsVisibilityPolicy.resolve(for: PermissionPolicy.permissions(for: .warehouse))
        XCTAssertFalse(warehouse.canManageStaff)
        XCTAssertFalse(warehouse.canAccessSyncImport)
        XCTAssertFalse(warehouse.canAccessMediaAssets)
        XCTAssertTrue(warehouse.canAccessPlanning)

        let manager = SettingsVisibilityPolicy.resolve(for: PermissionPolicy.permissions(for: .manager))
        XCTAssertTrue(manager.canManageStaff)
        XCTAssertFalse(manager.canAccessSyncImport)
        XCTAssertFalse(manager.canAccessMediaAssets)
        XCTAssertTrue(manager.canAccessPlanning)
    }
}
