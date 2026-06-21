import Foundation
import XCTest
@testable import ZhihuijiIOS

final class DashboardViewModelTests: XCTestCase {
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

    func testDashboardViewModelStartsWithoutPlaceholderKpis() {
        let viewModel = DashboardViewModel()

        XCTAssertTrue(viewModel.kpis.isEmpty)
        XCTAssertFalse(viewModel.hasKpis)
        XCTAssertNil(viewModel.errorMessage)
    }

    func testDashboardViewModelKeepsEmptyStateWhenBackendFails() async {
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
            case "/v1/reports/sales-summary", "/v1/reports/low-stock-products":
                return (
                    HTTPURLResponse(
                        url: request.url!,
                        statusCode: 500,
                        httpVersion: "HTTP/1.1",
                        headerFields: ["Content-Type": "application/json"]
                    )!,
                    Data(#"{"code":500,"message":"error","data":null}"#.utf8)
                )
            default:
                throw URLError(.fileDoesNotExist)
            }
        }

        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)
        let viewModel = DashboardViewModel()
        await viewModel.load(using: client)

        XCTAssertTrue(viewModel.kpis.isEmpty)
        XCTAssertTrue(viewModel.lowStockProducts.isEmpty)
        XCTAssertNotNil(viewModel.errorMessage)
    }

    func testDashboardViewModelLoadsRealKpisFromBackend() async {
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
            case "/v1/reports/sales-summary":
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
                            "start_at": 1710000000000,
                            "end_at": 1710003600000,
                            "total_sales_amount": 1280.5,
                            "total_paid_amount": 980.0,
                            "total_refund_amount": 20.0,
                            "total_unpaid_amount": 300.5,
                            "total_order_count": 8
                          }
                        }
                        """.utf8
                    )
                )
            case "/v1/reports/low-stock-products":
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
                              "product_id": "30001",
                              "product_code": "P-001",
                              "product_name": "测试商品",
                              "stock": 2.0,
                              "safe_stock": 5.0
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
        let viewModel = DashboardViewModel()
        await viewModel.load(using: client)

        XCTAssertEqual(viewModel.kpis.count, 4)
        XCTAssertEqual(viewModel.kpis.first?.title, "销售额")
        XCTAssertEqual(viewModel.kpis.first?.value, "¥1280.50")
        XCTAssertEqual(viewModel.lowStockProducts.count, 1)
        XCTAssertEqual(viewModel.lowStockProducts.first?.productName, "测试商品")
        XCTAssertNil(viewModel.errorMessage)
    }
}
