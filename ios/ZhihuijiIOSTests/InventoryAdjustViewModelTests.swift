import Foundation
import XCTest
@testable import ZhihuijiIOS

final class InventoryAdjustViewModelTests: XCTestCase {
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
    func testLoadPopulatesProductsAndLedgerEntries() async {
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
            case "/v2/products":
                return (Self.response(statusCode: 200), Self.productListEnvelope)
            case "/v2/inventory/ledger":
                return (Self.response(statusCode: 200), Self.ledgerEntryListEnvelope)
            default:
                throw URLError(.fileDoesNotExist)
            }
        }

        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)
        let viewModel = InventoryAdjustViewModel()
        await viewModel.load(client: client)

        XCTAssertEqual(viewModel.products.count, 1)
        XCTAssertEqual(viewModel.products.first?.name, "Test Product")
        XCTAssertNotNil(viewModel.selectedProduct)
        XCTAssertEqual(viewModel.selectedProduct?.name, "Test Product")
        XCTAssertEqual(viewModel.ledgerEntries.count, 1)
        XCTAssertEqual(viewModel.ledgerEntries.first?.productName, "Test Product")
        XCTAssertNil(viewModel.errorMessage)
        XCTAssertFalse(viewModel.isLoading)
    }

    @MainActor
    func testCreateAdjustmentSuccessfullyCreatesLedgerEntry() async {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockURLProtocol.self]
        let session = URLSession(configuration: config)
        let tokenStore = AuthTokenStore()
        tokenStore.clear()
        tokenStore.save(accessToken: "access-token", refreshToken: "refresh-token")

        MockURLProtocol.requestHandler = { request in
            switch (request.httpMethod, request.url?.path) {
            case ("POST", "/v2/inventory/ledger"):
                return (Self.response(statusCode: 200), Self.ledgerEntryEnvelope)
            case ("GET", "/v2/products"):
                return (Self.response(statusCode: 200), Self.productListEnvelope)
            default:
                throw URLError(.fileDoesNotExist)
            }
        }

        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)
        let viewModel = InventoryAdjustViewModel()
        viewModel.selectedProduct = Self.sampleProduct()
        viewModel.quantityChangeText = "5"

        await viewModel.createAdjustment(client: client)

        XCTAssertEqual(viewModel.ledgerEntries.count, 1)
        XCTAssertEqual(viewModel.ledgerEntries.first?.quantityChange, 5.0)
        XCTAssertEqual(viewModel.ledgerEntries.first?.sourceType, "inventory_adjust")
        XCTAssertEqual(viewModel.quantityChangeText, "")
        XCTAssertNotNil(viewModel.statusMessage)
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

    private static func sampleProduct() -> ProductRecord {
        ProductRecord(
            id: "50001",
            code: "P-001",
            name: "Test Product",
            categoryId: nil,
            categoryName: nil,
            unitId: nil,
            unitName: nil,
            salePrice: 12,
            purchasePrice: 8,
            priceLevels: nil,
            defaultSupplier: nil,
            supplierRelations: nil,
            stock: 10,
            safeStock: 5,
            status: 1,
            createdAt: nil,
            updatedAt: nil
        )
    }

    private static let productListEnvelope = Data(
        """
        {
          "code": 0,
          "message": "ok",
          "data": [
            {
              "id": "50001",
              "code": "P-001",
              "name": "Test Product",
              "category_id": null,
              "category_name": null,
              "unit_id": null,
              "unit_name": null,
              "sale_price": 12.0,
              "purchase_price": 8.0,
              "price_levels": null,
              "default_supplier": null,
              "supplier_relations": null,
              "stock": 10.0,
              "safe_stock": 5.0,
              "status": 1,
              "created_at": null,
              "updated_at": null
            }
          ]
        }
        """.utf8
    )

    private static let ledgerEntryListEnvelope = Data(
        """
        {
          "code": 0,
          "message": "ok",
          "data": [
            {
              "id": "60001",
              "product_id": "50001",
              "product_code": "P-001",
              "product_name": "Test Product",
              "warehouse_id": null,
              "quantity_before": 8.0,
              "quantity_change": 2.0,
              "quantity_after": 10.0,
              "unit_cost": 8.0,
              "source_type": "purchase_receipt",
              "source_id": null,
              "source_no": null,
              "notes": null,
              "created_at": 1710000000000
            }
          ]
        }
        """.utf8
    )

    private static let ledgerEntryEnvelope = Data(
        """
        {
          "code": 0,
          "message": "ok",
          "data": {
            "id": "60002",
            "product_id": "50001",
            "product_code": "P-001",
            "product_name": "Test Product",
            "warehouse_id": null,
            "quantity_before": 10.0,
            "quantity_change": 5.0,
            "quantity_after": 15.0,
            "unit_cost": 8.0,
            "source_type": "inventory_adjust",
            "source_id": null,
            "source_no": null,
            "notes": null,
            "created_at": 1710000000000
          }
        }
        """.utf8
    )
}
