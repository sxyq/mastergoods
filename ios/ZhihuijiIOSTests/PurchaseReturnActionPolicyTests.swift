import XCTest
@testable import ZhihuijiIOS

final class PurchaseReturnActionPolicyTests: XCTestCase {
    func testPurchaseReturnActionPolicyMatchesPermissionMatrix() {
        let purchasing = PermissionPolicy.permissions(for: .purchasing)
        let finance = PermissionPolicy.permissions(for: .finance)

        let draft = PurchaseReturnRecord(
            id: "1",
            returnNo: "PR-001",
            purchaseOrderId: "10",
            supplierId: nil,
            supplierName: "供应商",
            items: [],
            refunds: [],
            totalAmount: 0,
            refundAmount: 0,
            status: 0,
            notes: nil,
            createdAt: 0,
            updatedAt: 0
        )

        let purchasingPolicy = PurchaseReturnActionPolicy.resolve(for: purchasing, returnRecord: draft)
        XCTAssertTrue(purchasingPolicy.canEditDraft)
        XCTAssertFalse(purchasingPolicy.canRefund)
        XCTAssertTrue(purchasingPolicy.canCancel)
        XCTAssertTrue(purchasingPolicy.canCreateReturn)

        let financePolicy = PurchaseReturnActionPolicy.resolve(for: finance, returnRecord: draft)
        XCTAssertFalse(financePolicy.canEditDraft)
        XCTAssertTrue(financePolicy.canRefund)
        XCTAssertFalse(financePolicy.canCancel)
        XCTAssertFalse(financePolicy.canCreateReturn)
    }
}
