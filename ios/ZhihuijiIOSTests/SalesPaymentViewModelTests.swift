import Foundation
import XCTest
@testable import ZhihuijiIOS

final class SalesPaymentViewModelTests: XCTestCase {
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
    func testSalesPaymentViewModelStartsEmpty() {
        let viewModel = SalesPaymentViewModel()

        XCTAssertTrue(viewModel.orders.isEmpty)
        XCTAssertTrue(viewModel.payments.isEmpty)
        XCTAssertNil(viewModel.selectedOrderId)
        XCTAssertNil(viewModel.selectedOrder)
        XCTAssertNil(viewModel.errorMessage)
        XCTAssertFalse(viewModel.isLoading)
        XCTAssertFalse(viewModel.isSubmitting)
    }

    @MainActor
    func testSalesPaymentViewModelLoadsOrdersAndPayments() async {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockURLProtocol.self]
        let session = URLSession(configuration: config)
        let tokenStore = AuthTokenStore()
        tokenStore.clear()
        tokenStore.save(accessToken: "access-token", refreshToken: "refresh-token")

        MockURLProtocol.requestHandler = { request in
            XCTAssertEqual(request.value(forHTTPHeaderField: "Authorization"), "Bearer access-token")
            guard let path = request.url?.path else {
                throw URLError(.badURL)
            }

            switch path {
            case "/v2/sale-orders":
                return (
                    HTTPURLResponse(
                        url: request.url!,
                        statusCode: 200,
                        httpVersion: "HTTP/1.1",
                        headerFields: ["Content-Type": "application/json"]
                    )!,
                    Data(
                        """
                        {
                          "code": 0,
                          "message": "ok",
                          "data": [
                            {
                              "id": "50001",
                              "order_no": "SO-001",
                              "customer_id": 80001,
                              "customer_name": "测试客户",
                              "items": [],
                              "subtotal_amount": 100.0,
                              "discount_amount": 0.0,
                              "total_amount": 100.0,
                              "paid_amount": 0.0,
                              "notes": null,
                              "status": 0,
                              "created_at": 1710000000000,
                              "updated_at": 1710000000000
                            }
                          ]
                        }
                        """.utf8
                    )
                )
            case "/v2/sale-orders/50001":
                return (
                    HTTPURLResponse(
                        url: request.url!,
                        statusCode: 200,
                        httpVersion: "HTTP/1.1",
                        headerFields: ["Content-Type": "application/json"]
                    )!,
                    Data(
                        """
                        {
                          "code": 0,
                          "message": "ok",
                          "data": {
                            "id": "50001",
                            "order_no": "SO-001",
                            "customer_id": 80001,
                            "customer_name": "测试客户",
                            "items": [],
                            "subtotal_amount": 100.0,
                            "discount_amount": 0.0,
                            "total_amount": 100.0,
                            "paid_amount": 30.0,
                            "notes": null,
                            "status": 0,
                            "created_at": 1710000000000,
                            "updated_at": 1710000000000
                          }
                        }
                        """.utf8
                    )
                )
            case "/v2/sale-orders/50001/payments":
                return (
                    HTTPURLResponse(
                        url: request.url!,
                        statusCode: 200,
                        httpVersion: "HTTP/1.1",
                        headerFields: ["Content-Type": "application/json"]
                    )!,
                    Data(
                        """
                        {
                          "code": 0,
                          "message": "ok",
                          "data": [
                            {
                              "id": "60001",
                              "order_id": "50001",
                              "amount": 30.0,
                              "method": 1,
                              "reference_no": null,
                              "type": null,
                              "created_at": 1710000000000
                            }
                          ]
                        }
                        """.utf8
                    )
                )
            default:
                throw URLError(.fileDoesNotExist)
            }
        }

        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)
        let viewModel = SalesPaymentViewModel()
        await viewModel.load(client: client, preferredOrderId: nil)

        XCTAssertEqual(viewModel.orders.count, 1)
        XCTAssertEqual(viewModel.orders.first?.orderNo, "SO-001")
        XCTAssertEqual(viewModel.selectedOrderId?.rawValue, "50001")
        XCTAssertEqual(viewModel.selectedOrder?.orderNo, "SO-001")
        XCTAssertEqual(viewModel.selectedOrder?.outstandingAmount, 70.0)
        XCTAssertEqual(viewModel.payments.count, 1)
        XCTAssertEqual(viewModel.payments.first?.amount, 30.0)
        XCTAssertEqual(viewModel.amountText, "70.00")
        XCTAssertNil(viewModel.errorMessage)
    }

    @MainActor
    func testSalesPaymentViewModelSubmitsPaymentSuccessfully() async {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockURLProtocol.self]
        let session = URLSession(configuration: config)
        let tokenStore = AuthTokenStore()
        tokenStore.clear()
        tokenStore.save(accessToken: "access-token", refreshToken: "refresh-token")

        MockURLProtocol.requestHandler = { request in
            XCTAssertEqual(request.value(forHTTPHeaderField: "Authorization"), "Bearer access-token")
            guard let path = request.url?.path else {
                throw URLError(.badURL)
            }

            switch (request.httpMethod, path) {
            case ("POST", "/v2/sale-orders/50001/payments"):
                let body = try XCTUnwrap(request.httpBody)
                let json = try JSONSerialization.jsonObject(with: body) as? [String: Any]
                XCTAssertEqual((json?["amount"] as? NSNumber)?.doubleValue, 70.0)
                XCTAssertEqual(json?["method"] as? Int, 1)
                return (
                    HTTPURLResponse(
                        url: request.url!,
                        statusCode: 200,
                        httpVersion: "HTTP/1.1",
                        headerFields: ["Content-Type": "application/json"]
                    )!,
                    Data(
                        """
                        {
                          "code": 0,
                          "message": "ok",
                          "data": {
                            "id": "60002",
                            "order_id": "50001",
                            "amount": 70.0,
                            "method": 1,
                            "reference_no": null,
                            "type": null,
                            "created_at": 1710000001000
                          }
                        }
                        """.utf8
                    )
                )
            case ("GET", "/v2/sale-orders"):
                return (
                    HTTPURLResponse(
                        url: request.url!,
                        statusCode: 200,
                        httpVersion: "HTTP/1.1",
                        headerFields: ["Content-Type": "application/json"]
                    )!,
                    Data(
                        """
                        {
                          "code": 0,
                          "message": "ok",
                          "data": [
                            {
                              "id": "50001",
                              "order_no": "SO-001",
                              "customer_id": 80001,
                              "customer_name": "测试客户",
                              "items": [],
                              "subtotal_amount": 100.0,
                              "discount_amount": 0.0,
                              "total_amount": 100.0,
                              "paid_amount": 100.0,
                              "notes": null,
                              "status": 1,
                              "created_at": 1710000000000,
                              "updated_at": 1710000000000
                            }
                          ]
                        }
                        """.utf8
                    )
                )
            case ("GET", "/v2/sale-orders/50001"):
                return (
                    HTTPURLResponse(
                        url: request.url!,
                        statusCode: 200,
                        httpVersion: "HTTP/1.1",
                        headerFields: ["Content-Type": "application/json"]
                    )!,
                    Data(
                        """
                        {
                          "code": 0,
                          "message": "ok",
                          "data": {
                            "id": "50001",
                            "order_no": "SO-001",
                            "customer_id": 80001,
                            "customer_name": "测试客户",
                            "items": [],
                            "subtotal_amount": 100.0,
                            "discount_amount": 0.0,
                            "total_amount": 100.0,
                            "paid_amount": 100.0,
                            "notes": null,
                            "status": 1,
                            "created_at": 1710000000000,
                            "updated_at": 1710000000000
                          }
                        }
                        """.utf8
                    )
                )
            case ("GET", "/v2/sale-orders/50001/payments"):
                return (
                    HTTPURLResponse(
                        url: request.url!,
                        statusCode: 200,
                        httpVersion: "HTTP/1.1",
                        headerFields: ["Content-Type": "application/json"]
                    )!,
                    Data(
                        """
                        {
                          "code": 0,
                          "message": "ok",
                          "data": [
                            {
                              "id": "60001",
                              "order_id": "50001",
                              "amount": 30.0,
                              "method": 1,
                              "reference_no": null,
                              "type": null,
                              "created_at": 1710000000000
                            },
                            {
                              "id": "60002",
                              "order_id": "50001",
                              "amount": 70.0,
                              "method": 1,
                              "reference_no": null,
                              "type": null,
                              "created_at": 1710000001000
                            }
                          ]
                        }
                        """.utf8
                    )
                )
            default:
                throw URLError(.fileDoesNotExist)
            }
        }

        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)
        let viewModel = SalesPaymentViewModel()
        viewModel.selectedOrderId = "50001"
        viewModel.amountText = "70.00"
        viewModel.method = .cash
        viewModel.referenceNo = "REF-001"

        await viewModel.submit(client: client)

        XCTAssertEqual(viewModel.successMessage, "收款已提交")
        XCTAssertEqual(viewModel.referenceNo, "")
        XCTAssertEqual(viewModel.payments.count, 2)
        XCTAssertEqual(viewModel.selectedOrder?.paidAmount, 100.0)
        XCTAssertNil(viewModel.errorMessage)
        XCTAssertFalse(viewModel.isSubmitting)
    }
}
