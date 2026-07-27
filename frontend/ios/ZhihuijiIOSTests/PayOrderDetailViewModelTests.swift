import Foundation
import XCTest
@testable import ZhihuijiIOS

final class PayOrderDetailViewModelTests: XCTestCase {
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
    func testLoadPopulatesOrdersAndSelectsDetail() async {
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
            case "/v2/pay-orders":
                return (Self.response(statusCode: 200), Self.payOrderListEnvelope)
            case "/v2/suppliers":
                return (Self.response(statusCode: 200), Self.supplierListEnvelope)
            case "/v2/pay-orders/75001":
                return (Self.response(statusCode: 200), Self.payOrderEnvelope)
            default:
                throw URLError(.fileDoesNotExist)
            }
        }

        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)
        let viewModel = PayOrderDetailViewModel()
        await viewModel.load(client: client, preferredId: nil)

        XCTAssertEqual(viewModel.orders.count, 1)
        XCTAssertEqual(viewModel.orders.first?.orderNo, "PAY-001")
        XCTAssertNotNil(viewModel.selectedOrder)
        XCTAssertEqual(viewModel.selectedOrder?.orderNo, "PAY-001")
        XCTAssertEqual(viewModel.suppliers.count, 1)
        XCTAssertNil(viewModel.errorMessage)
        XCTAssertFalse(viewModel.isLoading)
    }

    @MainActor
    func testUpdateStatusSuccessfullyUpdatesPayOrder() async {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockURLProtocol.self]
        let session = URLSession(configuration: config)
        let tokenStore = AuthTokenStore()
        tokenStore.clear()
        tokenStore.save(accessToken: "access-token", refreshToken: "refresh-token")

        MockURLProtocol.requestHandler = { request in
            switch (request.httpMethod, request.url?.path) {
            case ("PUT", "/v2/pay-orders/75001/status"):
                return (Self.response(statusCode: 200), Self.payOrderPaidEnvelope)
            default:
                throw URLError(.fileDoesNotExist)
            }
        }

        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)
        let viewModel = PayOrderDetailViewModel()
        viewModel.selectedOrder = Self.samplePayOrder()

        await viewModel.updateStatus(client: client, status: 1)

        XCTAssertEqual(viewModel.selectedOrder?.status, 1)
        XCTAssertEqual(viewModel.selectedOrder?.orderNo, "PAY-001")
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

    private static func samplePayOrder() -> PayOrder {
        PayOrder(
            id: "75001",
            orderNo: "PAY-001",
            supplierId: "90001",
            supplierName: "Test Supplier",
            amount: 500,
            method: 4,
            referenceNo: nil,
            notes: nil,
            accountId: nil,
            status: 0,
            createdAt: 1710000000000,
            updatedAt: 1710000000000
        )
    }

    private static let payOrderListEnvelope = Data(
        """
        {
          "code": 0,
          "message": "ok",
          "data": [
            {
              "id": "75001",
              "order_no": "PAY-001",
              "supplier_id": "90001",
              "supplier_name": "Test Supplier",
              "amount": 500.0,
              "method": 4,
              "reference_no": null,
              "notes": null,
              "account_id": null,
              "status": 0,
              "created_at": 1710000000000,
              "updated_at": 1710000000000
            }
          ]
        }
        """.utf8
    )

    private static let payOrderEnvelope = Data(
        """
        {
          "code": 0,
          "message": "ok",
          "data": {
            "id": "75001",
            "order_no": "PAY-001",
            "supplier_id": "90001",
            "supplier_name": "Test Supplier",
            "amount": 500.0,
            "method": 4,
            "reference_no": null,
            "notes": null,
            "account_id": null,
            "status": 0,
            "created_at": 1710000000000,
            "updated_at": 1710000000000
          }
        }
        """.utf8
    )

    private static let payOrderPaidEnvelope = Data(
        """
        {
          "code": 0,
          "message": "ok",
          "data": {
            "id": "75001",
            "order_no": "PAY-001",
            "supplier_id": "90001",
            "supplier_name": "Test Supplier",
            "amount": 500.0,
            "method": 4,
            "reference_no": null,
            "notes": null,
            "account_id": null,
            "status": 1,
            "created_at": 1710000000000,
            "updated_at": 1710000000000
          }
        }
        """.utf8
    )

    private static let supplierListEnvelope = Data(
        """
        {
          "code": 0,
          "message": "ok",
          "data": [
            {
              "id": "90001",
              "name": "Test Supplier",
              "phone": "13800001111",
              "group_id": null,
              "group_name": null,
              "primary_contact_name": null,
              "primary_contact_phone": null,
              "address": null,
              "notes": null,
              "balance": null,
              "status": 1,
              "created_at": null,
              "updated_at": null
            }
          ]
        }
        """.utf8
    )
}
