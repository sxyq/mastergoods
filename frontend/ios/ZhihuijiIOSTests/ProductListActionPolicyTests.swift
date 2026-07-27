import XCTest
@testable import ZhihuijiIOS

final class ProductListActionPolicyTests: XCTestCase {
    func testProductListActionPolicyMatchesPermissionMatrix() {
        let archives = ProductListActionPolicy.resolve(for: PermissionPolicy.permissions(for: .manager))
        XCTAssertTrue(archives.canCreateProduct)

        let owner = ProductListActionPolicy.resolve(for: Set(Permission.allCases))
        XCTAssertTrue(owner.canCreateProduct)

        let sales = ProductListActionPolicy.resolve(for: PermissionPolicy.permissions(for: .sales))
        XCTAssertFalse(sales.canCreateProduct)

        let assistant = ProductListActionPolicy.resolve(for: PermissionPolicy.permissions(for: .assistant))
        XCTAssertFalse(assistant.canCreateProduct)
    }
}
