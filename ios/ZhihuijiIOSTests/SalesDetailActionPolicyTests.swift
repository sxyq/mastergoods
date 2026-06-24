import XCTest
@testable import ZhihuijiIOS

final class SalesDetailActionPolicyTests: XCTestCase {
    func testSalesDetailActionPolicyMatchesPermissionMatrix() {
        let owner = SalesDetailActionPolicy.resolve(for: Set(Permission.allCases))
        XCTAssertTrue(owner.canOpenPayment)

        let sales = SalesDetailActionPolicy.resolve(for: PermissionPolicy.permissions(for: .sales))
        XCTAssertFalse(sales.canOpenPayment)

        let finance = SalesDetailActionPolicy.resolve(for: PermissionPolicy.permissions(for: .finance))
        XCTAssertTrue(finance.canOpenPayment)

        let assistant = SalesDetailActionPolicy.resolve(for: PermissionPolicy.permissions(for: .assistant))
        XCTAssertFalse(assistant.canOpenPayment)
    }
}
