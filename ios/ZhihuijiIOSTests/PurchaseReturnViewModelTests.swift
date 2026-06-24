import Foundation
import XCTest
@testable import ZhihuijiIOS

final class PurchaseReturnViewModelTests: XCTestCase {
    override func setUp() {
        super.setUp()
        MockURLProtocol.requestHandler = nil
        MockURLProtocol.requestCount = 0
        MockURLProtocol.didStopLoading = false
        MockURLProtocol.holdOpen = false
    }

    override func tearDown() {
        MockURLProtocol.requestHandler = nil
        MockURLProtocol.requestCount = 0
        MockURLProtocol.didStopLoading = false
        MockURLProtocol.holdOpen = false
        super.tearDown()
    }

    @MainActor
    func testLoadPopulatesReturnsAndPurchaseOrders() async {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockURLProtocol.self]
        let session = URLSession(configuration: config)
        let tokenStore = AuthTokenStore()
        tokenStore.clear()
        tokenStore.save(accessToken: "access-token", refreshToken: "refresh-token")

        MockURLProtocol.requestHandler = { request in
            guard let path = request.url?.path else {
                throw URLError(.badURL)
            }
            switch path {
            case "/v2/purchase-returns":
                return (Self.response(statusCode: 200), Self.purchaseReturnListEnvelope)
            case "/v2/purchase-orders":
                return (Self.response(statusCode: 200), Self.purchaseOrderListEnvelope)
            default:
                throw URLError(.fileDoesNotExist)
            }
        }

        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)
        let viewModel = PurchaseReturnViewModel()
        await viewModel.load(client: client, preferredOrderId: nil)

        XCTAssertEqual(viewModel.returns.count, 1)
        XCTAssertEqual(viewModel.returns.first?.returnNo, "PR-001")
        XCTAssertEqual(viewModel.purchaseOrders.count, 1)
        XCTAssertEqual(viewModel.purchaseOrders.first?.orderNo, "PO-001")
        XCTAssertNotNil(viewModel.selectedReturn)
        XCTAssertEqual(viewModel.selectedReturn?.returnNo, "PR-001")
        XCTAssertNil(viewModel.errorMessage)
        XCTAssertFalse(viewModel.isLoading)
    }

    @MainActor
    func testCreateReturnSuccessfullyCreatesRecord() async {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockURLProtocol.self]
        let session = URLSession(configuration: config)
        let tokenStore = AuthTokenStore()
        tokenStore.clear()
        tokenStore.save(accessToken: "access-token", refreshToken: "refresh-token")

        MockURLProtocol.requestHandler = { request in
            switch (request.httpMethod, request.url?.path) {
            case ("POST", "/v2/purchase-returns"):
                return (Self.response(statusCode: 200), Self.purchaseReturnEnvelope)
            default:
                throw URLError(.fileDoesNotExist)
            }
        }

        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)
        let viewModel = PurchaseReturnViewModel()
        viewModel.selectSourceOrder(Self.samplePurchaseOrder())
        viewModel.draftItems[0].quantity = 2

        await viewModel.createReturn(client: client)

        XCTAssertEqual(viewModel.returns.count, 1)
        XCTAssertEqual(viewModel.returns.first?.returnNo, "PR-001")
        XCTAssertEqual(viewModel.mode, .manage)
        XCTAssertNil(viewModel.sourceOrder)
        XCTAssertTrue(viewModel.draftItems.isEmpty)
        XCTAssertNotNil(viewModel.selectedReturn)
        XCTAssertNil(viewModel.errorMessage)
        XCTAssertFalse(viewModel.isSubmitting)
    }

    private static func response(statusCode: Int) -> HTTPURLResponse {
        HTTPURLResponse(
            url: URL(string: "https://example.com")!,
            statusCode: statusCode,
            httpVersion: "HTTP/1.1",
            headerFields: ["Content-Type": "application/json"]
        )!
    }

    private static func samplePurchaseOrder() -> PurchaseOrderSummary {
        PurchaseOrderSummary(
            id: "70001",
            orderNo: "PO-001",
            supplierId: "90001",
            supplierName: "Test Supplier",
            items: [
                PurchaseOrderItem(
                    id: "71001",
                    orderId: "70001",
                    productId: "50001",
                    productCode: "P-001",
                    productName: "Test Product",
                    quantity: 10,
                    unitCost: 8,
                    amount: 80,
                    createdAt: 1710000000000
                ),
            ],
            totalAmount: 80,
            paidAmount: 0,
            receivedAmount: 0,
            settlementMethod: nil,
            warehouseId: nil,
            notes: nil,
            status: 0,
            createdAt: 1710000000000,
            updatedAt: 1710000000000
        )
    }

    private static let purchaseReturnListEnvelope = Data(
        """
        {
          "code": 0,
          "message": "ok",
          "data": [
            {
              "id": "72001",
              "return_no": "PR-001",
              "purchase_order_id": "70001",
              "supplier_id": "90001",
              "supplier_name": "Test Supplier",
              "items": [],
              "refunds": [],
              "total_amount": 100.0,
              "refund_amount": 0.0,
              "status": 0,
              "notes": null,
              "created_at": 1710000000000,
              "updated_at": 1710000000000
            }
          ]
        }
        """.utf8
    )

    private static let purchaseReturnEnvelope = Data(
        """
        {
          "code": 0,
          "message": "ok",
          "data": {
            "id": "72001",
            "return_no": "PR-001",
            "purchase_order_id": "70001",
            "supplier_id": "90001",
            "supplier_name": "Test Supplier",
            "items": [],
            "refunds": [],
            "total_amount": 100.0,
            "refund_amount": 0.0,
            "status": 0,
            "notes": null,
            "created_at": 1710000000000,
            "updated_at": 1710000000000
          }
        }
        """.utf8
    )

    private static let purchaseOrderListEnvelope = Data(
        """
        {
          "code": 0,
          "message": "ok",
          "data": [
            {
              "id": "70001",
              "order_no": "PO-001",
              "supplier_id": "90001",
              "supplier_name": "Test Supplier",
              "items": [],
              "total_amount": 80.0,
              "paid_amount": 0.0,
              "received_amount": 0.0,
              "notes": null,
              "status": 0,
              "created_at": 1710000000000,
              "updated_at": 1710000000000
            }
          ]
        }
        """.utf8
    )
}
