import XCTest
@testable import ZhihuijiIOS

final class PurchaseReceiptActionPolicyTests: XCTestCase {
    func testPurchaseReceiptActionPolicyMatchesPermissionMatrix() {
        let canWrite = Set(PermissionPolicy.permissions(for: .warehouse))
        let draftReceipt = PurchaseReceiptRecord(
            id: "1",
            receiptNo: "R-001",
            orderId: "10",
            supplierName: "供应商",
            items: [],
            totalAmount: 0,
            status: 0,
            notes: nil,
            createdAt: 0,
            updatedAt: 0
        )

        let warehouse = PurchaseReceiptActionPolicy.resolve(for: canWrite, receipt: draftReceipt)
        XCTAssertTrue(warehouse.canEditDraft)
        XCTAssertTrue(warehouse.canCancel)
        XCTAssertTrue(warehouse.canCreateReceipt)

        let sales = PurchaseReceiptActionPolicy.resolve(for: PermissionPolicy.permissions(for: .sales), receipt: draftReceipt)
        XCTAssertFalse(sales.canEditDraft)
        XCTAssertFalse(sales.canCancel)
        XCTAssertFalse(sales.canCreateReceipt)

        let cancelled = PurchaseReceiptActionPolicy.resolve(for: canWrite, receipt: PurchaseReceiptRecord(
            id: "2",
            receiptNo: "R-002",
            orderId: "11",
            supplierName: "供应商",
            items: [],
            totalAmount: 0,
            status: 2,
            notes: nil,
            createdAt: 0,
            updatedAt: 0
        ))
        XCTAssertFalse(cancelled.canEditDraft)
        XCTAssertFalse(cancelled.canCancel)
        XCTAssertTrue(cancelled.canCreateReceipt)
    }
}
