import XCTest
@testable import ZhihuijiIOS

final class ProductEditActionPolicyTests: XCTestCase {
    func testProductEditActionPolicyMatchesPermissionMatrix() {
        let archives = ProductEditActionPolicy.resolve(for: PermissionPolicy.permissions(for: .manager))
        XCTAssertTrue(archives.canSaveProduct)

        let owner = ProductEditActionPolicy.resolve(for: Set(Permission.allCases))
        XCTAssertTrue(owner.canSaveProduct)

        let sales = ProductEditActionPolicy.resolve(for: PermissionPolicy.permissions(for: .sales))
        XCTAssertFalse(sales.canSaveProduct)
    }
}
