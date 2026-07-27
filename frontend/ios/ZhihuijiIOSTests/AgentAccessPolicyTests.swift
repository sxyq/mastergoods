import XCTest
@testable import ZhihuijiIOS

final class AgentAccessPolicyTests: XCTestCase {
    func testAgentAccessPolicyMatchesPermissionMatrix() {
        let owner = AgentAccessPolicy.resolve(for: Set(Permission.allCases))
        XCTAssertTrue(owner.canViewAgent)
        XCTAssertTrue(owner.canWriteAgent)

        let assistant = AgentAccessPolicy.resolve(for: PermissionPolicy.permissions(for: .assistant))
        XCTAssertTrue(assistant.canViewAgent)
        XCTAssertFalse(assistant.canWriteAgent)

        let finance = AgentAccessPolicy.resolve(for: PermissionPolicy.permissions(for: .finance))
        XCTAssertTrue(finance.canViewAgent)
        XCTAssertFalse(finance.canWriteAgent)

        let warehouse = AgentAccessPolicy.resolve(for: PermissionPolicy.permissions(for: .warehouse))
        XCTAssertTrue(warehouse.canViewAgent)
        XCTAssertFalse(warehouse.canWriteAgent)
    }
}
