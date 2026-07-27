import XCTest
@testable import ZhihuijiIOS

final class PurchaseEditActionPolicyTests: XCTestCase {
    func testPurchaseEditActionPolicyMatchesPermissionMatrix() {
        let purchase = PurchaseEditActionPolicy.resolve(for: PermissionPolicy.permissions(for: .purchasing))
        XCTAssertTrue(purchase.canCreatePurchase)

        let owner = PurchaseEditActionPolicy.resolve(for: Set(Permission.allCases))
        XCTAssertTrue(owner.canCreatePurchase)

        let finance = PurchaseEditActionPolicy.resolve(for: PermissionPolicy.permissions(for: .finance))
        XCTAssertFalse(finance.canCreatePurchase)

        let assistant = PurchaseEditActionPolicy.resolve(for: PermissionPolicy.permissions(for: .assistant))
        XCTAssertFalse(assistant.canCreatePurchase)
    }
}
