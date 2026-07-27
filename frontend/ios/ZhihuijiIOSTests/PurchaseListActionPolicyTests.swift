import XCTest
@testable import ZhihuijiIOS

final class PurchaseListActionPolicyTests: XCTestCase {
    func testPurchaseListActionPolicyMatchesPermissionMatrix() {
        let purchasing = PurchaseListActionPolicy.resolve(for: PermissionPolicy.permissions(for: .purchasing))
        XCTAssertTrue(purchasing.canOpenCreate)

        let owner = PurchaseListActionPolicy.resolve(for: Set(Permission.allCases))
        XCTAssertTrue(owner.canOpenCreate)

        let finance = PurchaseListActionPolicy.resolve(for: PermissionPolicy.permissions(for: .finance))
        XCTAssertFalse(finance.canOpenCreate)
    }
}
