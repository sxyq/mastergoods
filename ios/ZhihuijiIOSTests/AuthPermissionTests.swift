import XCTest
@testable import ZhihuijiIOS

final class AuthPermissionTests: XCTestCase {
    func testPermissionRawValue() {
        XCTAssertEqual(Permission.usersManage.rawValue, "users:manage")
        XCTAssertEqual(Permission.agentView.rawValue, "agent:view")
    }
}
