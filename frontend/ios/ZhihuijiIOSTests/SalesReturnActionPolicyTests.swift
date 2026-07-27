import XCTest
@testable import ZhihuijiIOS

final class SalesReturnActionPolicyTests: XCTestCase {
    func testSalesReturnActionPolicyMatchesPermissionMatrix() {
        let sales = PermissionPolicy.permissions(for: .sales)
        let finance = PermissionPolicy.permissions(for: .finance)

        let draft = SalesReturnRecord(
            id: "1",
            returnNo: "SR-001",
            originalOrderId: "10",
            customerName: "客户",
            items: [],
            refunds: [],
            totalAmount: 0,
            refundAmount: 0,
            status: 0,
            notes: nil,
            createdAt: 0,
            updatedAt: 0
        )

        let salesPolicy = SalesReturnActionPolicy.resolve(for: sales, returnRecord: draft)
        XCTAssertTrue(salesPolicy.canEditDraft)
        XCTAssertFalse(salesPolicy.canRefund)
        XCTAssertTrue(salesPolicy.canCancel)
        XCTAssertTrue(salesPolicy.canCreateReturn)

        let financePolicy = SalesReturnActionPolicy.resolve(for: finance, returnRecord: draft)
        XCTAssertFalse(financePolicy.canEditDraft)
        XCTAssertTrue(financePolicy.canRefund)
        XCTAssertFalse(financePolicy.canCancel)
        XCTAssertFalse(financePolicy.canCreateReturn)

        let assistantPolicy = SalesReturnActionPolicy.resolve(for: PermissionPolicy.permissions(for: .assistant), returnRecord: draft)
        XCTAssertFalse(assistantPolicy.canEditDraft)
        XCTAssertFalse(assistantPolicy.canRefund)
        XCTAssertFalse(assistantPolicy.canCancel)
        XCTAssertFalse(assistantPolicy.canCreateReturn)
    }
}
