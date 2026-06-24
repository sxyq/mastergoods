import Foundation
import XCTest
@testable import ZhihuijiIOS

final class SalesEditViewModelTests: XCTestCase {
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
    func testSalesEditViewModelStartsEmpty() {
        let viewModel = SalesEditViewModel()

        XCTAssertTrue(viewModel.customers.isEmpty)
        XCTAssertTrue(viewModel.products.isEmpty)
        XCTAssertTrue(viewModel.items.isEmpty)
        XCTAssertNil(viewModel.selectedCustomer)
        XCTAssertNil(viewModel.errorMessage)
        XCTAssertFalse(viewModel.isSubmitting)
    }

    @MainActor
    func testSalesEditViewModelLoadsCustomersAndProducts() async {
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
            case "/v2/customers":
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
                              "id": "80001",
                              "name": "测试客户",
                              "phone": "13800001111",
                              "level": 1,
                              "group_id": null,
                              "group_name": null,
                              "primary_contact_name": null,
                              "primary_contact_phone": null,
                              "address": null,
                              "notes": null,
                              "balance": 0,
                              "status": 1,
                              "created_at": 1710000000000,
                              "updated_at": 1710000000000
                            }
                          ]
                        }
                        """.utf8
                    )
                )
            case "/v2/products":
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
                              "id": "90001",
                              "code": "P-001",
                              "name": "测试商品",
                              "category_id": null,
                              "category_name": null,
                              "unit_id": null,
                              "unit_name": null,
                              "sale_price": 12.0,
                              "purchase_price": 8.0,
                              "price_levels": null,
                              "default_supplier": null,
                              "supplier_relations": null,
                              "stock": 100.0,
                              "safe_stock": 5.0,
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
        let viewModel = SalesEditViewModel()
        await viewModel.load(client: client)

        XCTAssertEqual(viewModel.customers.count, 1)
        XCTAssertEqual(viewModel.customers.first?.name, "测试客户")
        XCTAssertEqual(viewModel.products.count, 1)
        XCTAssertEqual(viewModel.products.first?.name, "测试商品")
        XCTAssertNil(viewModel.errorMessage)
    }

    @MainActor
    func testSalesEditViewModelCreatesSaleOrderSuccessfully() async {
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
            case ("POST", "/v2/sale-orders"):
                let body = try XCTUnwrap(request.httpBody)
                let json = try JSONSerialization.jsonObject(with: body) as? [String: Any]
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
                            "id": "50001",
                            "order_no": "SO-001",
                            "customer_id": null,
                            "customer_name": null,
                            "items": [],
                            "subtotal_amount": 24.0,
                            "discount_amount": 0.0,
                            "total_amount": 24.0,
                            "paid_amount": 0.0,
                            "notes": null,
                            "status": 0,
                            "created_at": 1710000000000,
                            "updated_at": 1710000000000
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
        let viewModel = SalesEditViewModel()
        viewModel.items = [
            EditableSaleItem(
                productId: "90001",
                productName: "测试商品",
                quantity: 2,
                unitPriceText: "12.00"
            )
        ]
        viewModel.notes = "测试备注"
        viewModel.discountAmountText = "0.00"

        await viewModel.submit(client: client)

        XCTAssertTrue(viewModel.items.isEmpty)
        XCTAssertEqual(viewModel.notes, "")
        XCTAssertEqual(viewModel.discountAmountText, "0.00")
        XCTAssertNil(viewModel.errorMessage)
        XCTAssertFalse(viewModel.isSubmitting)
    }
}
