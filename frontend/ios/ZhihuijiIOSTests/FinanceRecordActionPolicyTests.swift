import XCTest
@testable import ZhihuijiIOS

final class FinanceRecordActionPolicyTests: XCTestCase {
    func testFinanceRecordActionPolicyMatchesPermissionMatrix() {
        let owner = FinanceRecordActionPolicy.resolve(for: Set(Permission.allCases))
        XCTAssertTrue(owner.canWriteFinance)

        let finance = FinanceRecordActionPolicy.resolve(for: PermissionPolicy.permissions(for: .finance))
        XCTAssertTrue(finance.canWriteFinance)

        let sales = FinanceRecordActionPolicy.resolve(for: PermissionPolicy.permissions(for: .sales))
        XCTAssertFalse(sales.canWriteFinance)

        let assistant = FinanceRecordActionPolicy.resolve(for: PermissionPolicy.permissions(for: .assistant))
        XCTAssertFalse(assistant.canWriteFinance)
    }
}
