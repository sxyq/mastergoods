import XCTest
@testable import ZhihuijiIOS

final class SettingsSecurityPolicyTests: XCTestCase {
    func testSettingsSecurityPolicyMatchesPermissionMatrix() {
        let owner = SettingsSecurityPolicy.resolve(for: Set(Permission.allCases))
        XCTAssertTrue(owner.canManageDatabase)

        let manager = SettingsSecurityPolicy.resolve(for: PermissionPolicy.permissions(for: .manager))
        XCTAssertFalse(manager.canManageDatabase)

        let finance = SettingsSecurityPolicy.resolve(for: PermissionPolicy.permissions(for: .finance))
        XCTAssertFalse(finance.canManageDatabase)
    }
}
