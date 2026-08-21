import XCTest
@testable import ZhihuijiIOS

final class SalesPaymentActionPolicyTests: XCTestCase {
    func testSalesPaymentActionPolicyMatchesPermissionMatrix() {
        let finance = SalesPaymentActionPolicy.resolve(for: PermissionPolicy.permissions(for: .finance))
        XCTAssertTrue(finance.canWriteFinance)

        let owner = SalesPaymentActionPolicy.resolve(for: Set(Permission.allCases))
        XCTAssertTrue(owner.canWriteFinance)

        let sales = SalesPaymentActionPolicy.resolve(for: PermissionPolicy.permissions(for: .sales))
        XCTAssertFalse(sales.canWriteFinance)

        let assistant = SalesPaymentActionPolicy.resolve(for: PermissionPolicy.permissions(for: .assistant))
        XCTAssertFalse(assistant.canWriteFinance)
    }
}
