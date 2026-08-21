import Foundation
import XCTest
@testable import ZhihuijiIOS

final class CustomerDetailViewModelTests: XCTestCase {
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
    func testLoadPopulatesCustomerAndTransactions() async {
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
            case "/v2/customers/80001":
                return (Self.response(statusCode: 200), Self.customerEnvelope)
            case "/v2/finance-records":
                return (Self.response(statusCode: 200), Self.financeRecordListEnvelope)
            default:
                throw URLError(.fileDoesNotExist)
            }
        }

        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)
        let viewModel = CustomerDetailViewModel()
        await viewModel.load(customerId: "80001", client: client)

        XCTAssertNotNil(viewModel.customer)
        XCTAssertEqual(viewModel.customer?.name, "Test Customer")
        XCTAssertEqual(viewModel.customer?.phone, "13800001111")
        XCTAssertEqual(viewModel.transactions.count, 1)
        XCTAssertEqual(viewModel.transactions.first?.recordNo, "FR-001")
        XCTAssertEqual(viewModel.transactions.first?.partnerName, "Test Customer")
        XCTAssertNil(viewModel.errorMessage)
    }

    @MainActor
    func testLoadClearsCustomerWhenFetchFails() async {
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
            case "/v2/customers/80001":
                return (
                    Self.response(statusCode: 500),
                    Data(#"{"code":500,"message":"customer unavailable","data":null}"#.utf8)
                )
            default:
                throw URLError(.fileDoesNotExist)
            }
        }

        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)
        let viewModel = CustomerDetailViewModel()
        await viewModel.load(customerId: "80001", client: client)

        XCTAssertNil(viewModel.customer)
        XCTAssertTrue(viewModel.transactions.isEmpty)
        XCTAssertNotNil(viewModel.errorMessage)
    }

    private static func response(statusCode: Int) -> HTTPURLResponse {
        HTTPURLResponse(
            url: URL(string: "https://example.com")!,
            statusCode: statusCode,
            httpVersion: "HTTP/1.1",
            headerFields: ["Content-Type": "application/json"]
        )!
    }

    private static let customerEnvelope = Data(
        """
        {
          "code": 0,
          "message": "ok",
          "data": {
            "id": "80001",
            "name": "Test Customer",
            "phone": "13800001111",
            "level": 1,
            "group_id": null,
            "group_name": null,
            "primary_contact_name": null,
            "primary_contact_phone": null,
            "address": null,
            "notes": null,
            "balance": 100.0,
            "status": 1,
            "created_at": 1710000000000,
            "updated_at": 1710000000000
          }
        }
        """.utf8
    )

    private static let financeRecordListEnvelope = Data(
        """
        {
          "code": 0,
          "message": "ok",
          "data": [
            {
              "id": "66001",
              "record_no": "FR-001",
              "type": 1,
              "category": "sales",
              "partner_name": "Test Customer",
              "amount": 300.0,
              "method": 1,
              "notes": "settlement",
              "created_at": 1710000000000,
              "updated_at": 1710000000000
            }
          ]
        }
        """.utf8
    )
}
