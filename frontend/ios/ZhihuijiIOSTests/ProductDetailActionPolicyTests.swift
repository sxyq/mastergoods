import XCTest
@testable import ZhihuijiIOS

final class ProductDetailActionPolicyTests: XCTestCase {
    func testProductDetailActionPolicyMatchesPermissionMatrix() {
        let manager = ProductDetailActionPolicy.resolve(for: PermissionPolicy.permissions(for: .manager))
        XCTAssertTrue(manager.canEditProduct)
        XCTAssertTrue(manager.canOpenInventoryAdjust)

        let sales = ProductDetailActionPolicy.resolve(for: PermissionPolicy.permissions(for: .sales))
        XCTAssertFalse(sales.canEditProduct)
        XCTAssertFalse(sales.canOpenInventoryAdjust)

        let warehouse = ProductDetailActionPolicy.resolve(for: PermissionPolicy.permissions(for: .warehouse))
        XCTAssertFalse(warehouse.canEditProduct)
        XCTAssertTrue(warehouse.canOpenInventoryAdjust)
    }
}
