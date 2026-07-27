import Foundation
import XCTest
@testable import ZhihuijiIOS

final class SalesReturnViewModelTests: XCTestCase {
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
    func testSalesReturnViewModelStartsEmpty() {
        let viewModel = SalesReturnViewModel()

        XCTAssertTrue(viewModel.returns.isEmpty)
        XCTAssertTrue(viewModel.saleOrders.isEmpty)
        XCTAssertNil(viewModel.selectedReturn)
        XCTAssertNil(viewModel.sourceOrder)
        XCTAssertTrue(viewModel.draftItems.isEmpty)
        XCTAssertNil(viewModel.errorMessage)
        XCTAssertFalse(viewModel.isLoading)
        XCTAssertFalse(viewModel.isSubmitting)
    }

    @MainActor
    func testSalesReturnViewModelLoadsReturnsAndSaleOrders() async {
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
            case "/v2/sales-returns":
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
                              "id": "40001",
                              "return_no": "SR-001",
                              "original_order_id": "50001",
                              "customer_id": "80001",
                              "customer_name": "测试客户",
                              "items": [
                                {
                                  "id": "41001",
                                  "return_id": "40001",
                                  "product_id": "90001",
                                  "product_code": "P-001",
                                  "product_name": "测试商品",
                                  "quantity": 1.0,
                                  "unit_price": 12.0,
                                  "amount": 12.0,
                                  "created_at": 1710000000000
                                }
                              ],
                              "total_amount": 12.0,
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
                )
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
            default:
                throw URLError(.fileDoesNotExist)
            }
        }

        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)
        let viewModel = SalesReturnViewModel()
        await viewModel.load(client: client)

        XCTAssertEqual(viewModel.returns.count, 1)
        XCTAssertEqual(viewModel.returns.first?.returnNo, "SR-001")
        XCTAssertEqual(viewModel.selectedReturn?.returnNo, "SR-001")
        XCTAssertEqual(viewModel.saleOrders.count, 1)
        XCTAssertEqual(viewModel.saleOrders.first?.orderNo, "SO-001")
        XCTAssertNil(viewModel.errorMessage)
        XCTAssertFalse(viewModel.isLoading)
    }

    @MainActor
    func testSalesReturnViewModelCreatesReturnSuccessfully() async {
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
            case ("POST", "/v2/sales-returns"):
                let body = try XCTUnwrap(request.httpBody)
                let json = try JSONSerialization.jsonObject(with: body) as? [String: Any]
                XCTAssertEqual(json?["original_order_id"] as? String, "50001")
                XCTAssertEqual(json?["customer_id"] as? String, "80001")
                XCTAssertEqual(json?["customer_name"] as? String, "测试客户")
                let items = try XCTUnwrap(json?["items"] as? [[String: Any]])
                XCTAssertEqual(items.count, 1)
                XCTAssertEqual(items.first?["product_id"] as? String, "90001")
                XCTAssertEqual((items.first?["quantity"] as? NSNumber)?.doubleValue, 2.0)
                XCTAssertEqual((items.first?["unit_price"] as? NSNumber)?.doubleValue, 12.0)
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
                            "id": "40002",
                            "return_no": "SR-002",
                            "original_order_id": "50001",
                            "customer_id": "80001",
                            "customer_name": "测试客户",
                            "items": [
                              {
                                "id": "41002",
                                "return_id": "40002",
                                "product_id": "90001",
                                "product_code": "P-001",
                                "product_name": "测试商品",
                                "quantity": 2.0,
                                "unit_price": 12.0,
                                "amount": 24.0,
                                "created_at": 1710000001000
                              }
                            ],
                            "total_amount": 24.0,
                            "refund_amount": 0.0,
                            "status": 0,
                            "notes": "测试退货",
                            "created_at": 1710000001000,
                            "updated_at": 1710000001000
                          }
                        }
                        """.utf8
                    )
                )
            default:
                throw URLError(.fileDoesNotExist)
            }
        }

        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)
        let viewModel = SalesReturnViewModel()
        viewModel.mode = .create
        viewModel.sourceOrder = SalesOrderSummary(
            id: "50001",
            orderNo: "SO-001",
            customerId: 80001,
            customerName: "测试客户",
            items: [
                SalesOrderItem(
                    id: "51001",
                    orderId: "50001",
                    productId: 90001,
                    productCode: "P-001",
                    productName: "测试商品",
                    customerId: 80001,
                    customerName: "测试客户",
                    quantity: 5,
                    unitPrice: 12,
                    amount: 60,
                    createdAt: 1710000000000
                )
            ],
            subtotalAmount: 100,
            discountAmount: 0,
            totalAmount: 100,
            paidAmount: 100,
            notes: nil,
            status: 1,
            createdAt: 1710000000000,
            updatedAt: 1710000000000
        )
        viewModel.draftItems = [
            SalesReturnDraftItem(
                productId: "90001",
                productCode: "P-001",
                productName: "测试商品",
                maxQuantity: 5,
                quantity: 2,
                unitPriceText: "12.00"
            )
        ]
        viewModel.createNotes = "测试退货"

        await viewModel.createReturn(client: client)

        XCTAssertEqual(viewModel.mode, .manage)
        XCTAssertEqual(viewModel.createNotes, "")
        XCTAssertNil(viewModel.sourceOrder)
        XCTAssertTrue(viewModel.draftItems.isEmpty)
        XCTAssertEqual(viewModel.returns.count, 1)
        XCTAssertEqual(viewModel.returns.first?.returnNo, "SR-002")
        XCTAssertEqual(viewModel.selectedReturn?.returnNo, "SR-002")
        XCTAssertEqual(viewModel.selectedReturn?.totalAmount, 24.0)
        XCTAssertNil(viewModel.errorMessage)
        XCTAssertFalse(viewModel.isSubmitting)
    }
}
