import Foundation
import XCTest
@testable import ZhihuijiIOS

final class PurchaseEditViewModelTests: XCTestCase {
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
    func testPurchaseEditViewModelStartsEmpty() {
        let viewModel = PurchaseEditViewModel()

        XCTAssertTrue(viewModel.suppliers.isEmpty)
        XCTAssertTrue(viewModel.products.isEmpty)
        XCTAssertTrue(viewModel.items.isEmpty)
        XCTAssertNil(viewModel.selectedSupplier)
        XCTAssertNil(viewModel.errorMessage)
        XCTAssertFalse(viewModel.isSubmitting)
    }

    @MainActor
    func testPurchaseEditViewModelLoadsSuppliersAndProducts() async {
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
            case "/v2/suppliers":
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
                              "name": "测试供应商",
                              "phone": "13800002222",
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
        let viewModel = PurchaseEditViewModel()
        await viewModel.load(client: client)

        XCTAssertEqual(viewModel.suppliers.count, 1)
        XCTAssertEqual(viewModel.suppliers.first?.name, "测试供应商")
        XCTAssertEqual(viewModel.products.count, 1)
        XCTAssertEqual(viewModel.products.first?.name, "测试商品")
        XCTAssertNil(viewModel.errorMessage)
    }

    @MainActor
    func testPurchaseEditViewModelCreatesPurchaseOrderSuccessfully() async {
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
            case ("POST", "/v2/purchase-orders"):
                let body = try XCTUnwrap(request.httpBody)
                let json = try JSONSerialization.jsonObject(with: body) as? [String: Any]
                let items = try XCTUnwrap(json?["items"] as? [[String: Any]])
                XCTAssertEqual(items.count, 1)
                XCTAssertEqual(items.first?["product_id"] as? String, "90001")
                XCTAssertEqual(items.first?["product_code"] as? String, "P-001")
                XCTAssertEqual(items.first?["product_name"] as? String, "测试商品")
                XCTAssertEqual((items.first?["quantity"] as? NSNumber)?.doubleValue, 3.0)
                XCTAssertEqual((items.first?["unit_cost"] as? NSNumber)?.doubleValue, 8.0)
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
                            "id": "70001",
                            "order_no": "PO-001",
                            "supplier_id": null,
                            "supplier_name": null,
                            "items": [],
                            "total_amount": 24.0,
                            "paid_amount": 0.0,
                            "received_amount": 0.0,
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
        let viewModel = PurchaseEditViewModel()
        viewModel.items = [
            EditablePurchaseItem(
                productId: "90001",
                productCode: "P-001",
                productName: "测试商品",
                quantity: 3,
                unitCostText: "8.00"
            )
        ]
        viewModel.notes = "采购备注"
        viewModel.initialStatus = 0

        await viewModel.submit(client: client)

        XCTAssertTrue(viewModel.items.isEmpty)
        XCTAssertEqual(viewModel.notes, "")
        XCTAssertNil(viewModel.errorMessage)
        XCTAssertFalse(viewModel.isSubmitting)
    }
}
