import XCTest
@testable import ZhihuijiIOS

final class SalesListActionPolicyTests: XCTestCase {
    func testSalesListActionPolicyMatchesPermissionMatrix() {
        let sales = SalesListActionPolicy.resolve(for: PermissionPolicy.permissions(for: .sales))
        XCTAssertTrue(sales.canOpenCreate)

        let owner = SalesListActionPolicy.resolve(for: Set(Permission.allCases))
        XCTAssertTrue(owner.canOpenCreate)

        let finance = SalesListActionPolicy.resolve(for: PermissionPolicy.permissions(for: .finance))
        XCTAssertFalse(finance.canOpenCreate)
    }
}
