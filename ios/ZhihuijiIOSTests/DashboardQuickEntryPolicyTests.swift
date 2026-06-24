import XCTest
@testable import ZhihuijiIOS

final class DashboardQuickEntryPolicyTests: XCTestCase {
    func testDashboardQuickEntryPolicyMatchesPermissionMatrix() {
        let owner = DashboardQuickEntryPolicy.resolve(for: Set(Permission.allCases))
        XCTAssertTrue(owner.canOpenSales)
        XCTAssertTrue(owner.canOpenPurchase)
        XCTAssertTrue(owner.canOpenAgent)
        XCTAssertTrue(owner.canOpenInventory)
        XCTAssertTrue(owner.canOpenFinance)

        let sales = DashboardQuickEntryPolicy.resolve(for: PermissionPolicy.permissions(for: .sales))
        XCTAssertTrue(sales.canOpenSales)
        XCTAssertFalse(sales.canOpenPurchase)
        XCTAssertTrue(sales.canOpenAgent)
        XCTAssertFalse(sales.canOpenInventory)
        XCTAssertTrue(sales.canOpenFinance)

        let assistant = DashboardQuickEntryPolicy.resolve(for: PermissionPolicy.permissions(for: .assistant))
        XCTAssertFalse(assistant.canOpenSales)
        XCTAssertFalse(assistant.canOpenPurchase)
        XCTAssertTrue(assistant.canOpenAgent)
        XCTAssertFalse(assistant.canOpenInventory)
        XCTAssertFalse(assistant.canOpenFinance)
    }
}
