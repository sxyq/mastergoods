import XCTest
@testable import ZhihuijiIOS

final class PayOrderDetailActionPolicyTests: XCTestCase {
    func testPayOrderDetailActionPolicyMatchesPermissionMatrix() {
        let finance = PayOrderDetailActionPolicy.resolve(for: PermissionPolicy.permissions(for: .finance))
        XCTAssertTrue(finance.canWriteFinance)

        let owner = PayOrderDetailActionPolicy.resolve(for: Set(Permission.allCases))
        XCTAssertTrue(owner.canWriteFinance)

        let sales = PayOrderDetailActionPolicy.resolve(for: PermissionPolicy.permissions(for: .sales))
        XCTAssertFalse(sales.canWriteFinance)

        let assistant = PayOrderDetailActionPolicy.resolve(for: PermissionPolicy.permissions(for: .assistant))
        XCTAssertFalse(assistant.canWriteFinance)
    }
}
