import XCTest
@testable import ZhihuijiIOS

final class PurchaseDetailActionPolicyTests: XCTestCase {
    func testPurchaseDetailActionPolicyMatchesPermissionMatrix() {
        let owner = PurchaseDetailActionPolicy.resolve(for: Set(Permission.allCases))
        XCTAssertTrue(owner.canOpenReceipt)
        XCTAssertTrue(owner.canOpenReturn)
        XCTAssertTrue(owner.canOpenPayOrder)

        let warehouse = PurchaseDetailActionPolicy.resolve(for: PermissionPolicy.permissions(for: .warehouse))
        XCTAssertTrue(warehouse.canOpenReceipt)
        XCTAssertFalse(warehouse.canOpenReturn)
        XCTAssertFalse(warehouse.canOpenPayOrder)

        let purchasing = PurchaseDetailActionPolicy.resolve(for: PermissionPolicy.permissions(for: .purchasing))
        XCTAssertFalse(purchasing.canOpenReceipt)
        XCTAssertTrue(purchasing.canOpenReturn)
        XCTAssertFalse(purchasing.canOpenPayOrder)

        let finance = PurchaseDetailActionPolicy.resolve(for: PermissionPolicy.permissions(for: .finance))
        XCTAssertFalse(finance.canOpenReceipt)
        XCTAssertFalse(finance.canOpenReturn)
        XCTAssertTrue(finance.canOpenPayOrder)
    }
}
