import XCTest
@testable import ZhihuijiIOS

final class HomeAccessPolicyTests: XCTestCase {
    func testDocumentsHomeAccessPolicyMatchesRolePermissions() {
        let sales = DocumentsHomeAccessPolicy.resolve(for: PermissionPolicy.permissions(for: .sales))
        XCTAssertTrue(sales.canViewSales)
        XCTAssertFalse(sales.canViewPurchase)
        XCTAssertTrue(sales.canViewFinance)
        XCTAssertFalse(sales.canViewInventory)

        let purchasing = DocumentsHomeAccessPolicy.resolve(for: PermissionPolicy.permissions(for: .purchasing))
        XCTAssertFalse(purchasing.canViewSales)
        XCTAssertTrue(purchasing.canViewPurchase)
        XCTAssertTrue(purchasing.canViewFinance)
        XCTAssertFalse(purchasing.canViewInventory)

        let warehouse = DocumentsHomeAccessPolicy.resolve(for: PermissionPolicy.permissions(for: .warehouse))
        XCTAssertFalse(warehouse.canViewSales)
        XCTAssertFalse(warehouse.canViewPurchase)
        XCTAssertFalse(warehouse.canViewFinance)
        XCTAssertTrue(warehouse.canViewInventory)
    }

    func testArchivesHomeAccessPolicyMatchesRolePermissions() {
        let sales = ArchivesHomeAccessPolicy.resolve(for: PermissionPolicy.permissions(for: .sales))
        XCTAssertEqual(sales.availableTabs, [.customers])

        let purchasing = ArchivesHomeAccessPolicy.resolve(for: PermissionPolicy.permissions(for: .purchasing))
        XCTAssertEqual(purchasing.availableTabs, [.suppliers])

        let warehouse = ArchivesHomeAccessPolicy.resolve(for: PermissionPolicy.permissions(for: .warehouse))
        XCTAssertEqual(warehouse.availableTabs, [])

        let owner = ArchivesHomeAccessPolicy.resolve(for: PermissionPolicy.permissions(for: .owner))
        XCTAssertEqual(owner.availableTabs, [.products, .customers, .suppliers])
    }
}
