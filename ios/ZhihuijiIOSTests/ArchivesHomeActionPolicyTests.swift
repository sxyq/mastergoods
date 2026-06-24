import XCTest
@testable import ZhihuijiIOS

final class ArchivesHomeActionPolicyTests: XCTestCase {
    func testArchivesHomeActionPolicyMatchesPermissionMatrix() {
        let archives = ArchivesHomeActionPolicy.resolve(for: PermissionPolicy.permissions(for: .manager))
        XCTAssertTrue(archives.canCreateProduct)
        XCTAssertTrue(archives.canCreateCustomer)
        XCTAssertTrue(archives.canCreateSupplier)
        XCTAssertTrue(archives.canEditPartner)

        let sales = ArchivesHomeActionPolicy.resolve(for: PermissionPolicy.permissions(for: .sales))
        XCTAssertFalse(sales.canCreateProduct)
        XCTAssertFalse(sales.canCreateCustomer)
        XCTAssertFalse(sales.canCreateSupplier)
        XCTAssertFalse(sales.canEditPartner)
    }
}
