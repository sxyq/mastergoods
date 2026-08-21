import XCTest
@testable import ZhihuijiIOS

final class SalesEditActionPolicyTests: XCTestCase {
    func testSalesEditActionPolicyMatchesPermissionMatrix() {
        let sales = SalesEditActionPolicy.resolve(for: PermissionPolicy.permissions(for: .sales))
        XCTAssertTrue(sales.canCreateSale)

        let owner = SalesEditActionPolicy.resolve(for: Set(Permission.allCases))
        XCTAssertTrue(owner.canCreateSale)

        let finance = SalesEditActionPolicy.resolve(for: PermissionPolicy.permissions(for: .finance))
        XCTAssertFalse(finance.canCreateSale)

        let assistant = SalesEditActionPolicy.resolve(for: PermissionPolicy.permissions(for: .assistant))
        XCTAssertFalse(assistant.canCreateSale)
    }
}
