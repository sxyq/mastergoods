import XCTest
@testable import ZhihuijiIOS

final class StaffManagementActionPolicyTests: XCTestCase {
    func testStaffManagementActionPolicyMatchesPermissionMatrix() {
        let owner = StaffManagementActionPolicy.resolve(for: Set(Permission.allCases))
        XCTAssertTrue(owner.canManageStaff)

        let manager = StaffManagementActionPolicy.resolve(for: PermissionPolicy.permissions(for: .manager))
        XCTAssertTrue(manager.canManageStaff)

        let sales = StaffManagementActionPolicy.resolve(for: PermissionPolicy.permissions(for: .sales))
        XCTAssertFalse(sales.canManageStaff)

        let assistant = StaffManagementActionPolicy.resolve(for: PermissionPolicy.permissions(for: .assistant))
        XCTAssertFalse(assistant.canManageStaff)
    }
}
