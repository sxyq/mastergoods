import Foundation
import XCTest
@testable import ZhihuijiIOS

final class FinanceRecordViewModelTests: XCTestCase {
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
    func testLoadPopulatesFinanceRecords() async {
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
            case "/v2/finance-records":
                return (Self.response(statusCode: 200), Self.financeRecordListEnvelope)
            default:
                throw URLError(.fileDoesNotExist)
            }
        }

        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)
        let viewModel = FinanceRecordViewModel()
        await viewModel.load(client: client)

        XCTAssertEqual(viewModel.records.count, 1)
        XCTAssertEqual(viewModel.records.first?.recordNo, "FR-001")
        XCTAssertEqual(viewModel.records.first?.category, "rent")
        XCTAssertEqual(viewModel.expenseTotal, 1200.0)
        XCTAssertNil(viewModel.errorMessage)
        XCTAssertFalse(viewModel.isLoading)
    }

    @MainActor
    func testCreateRecordSuccessfullyCreatesFinanceRecord() async {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockURLProtocol.self]
        let session = URLSession(configuration: config)
        let tokenStore = AuthTokenStore()
        tokenStore.clear()
        tokenStore.save(accessToken: "access-token", refreshToken: "refresh-token")

        MockURLProtocol.requestHandler = { request in
            switch (request.httpMethod, request.url?.path) {
            case ("POST", "/v2/finance-records"):
                return (Self.response(statusCode: 200), Self.financeRecordEnvelope)
            default:
                throw URLError(.fileDoesNotExist)
            }
        }

        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)
        let viewModel = FinanceRecordViewModel()
        viewModel.createType = .expense
        viewModel.category = "rent"
        viewModel.partnerName = "mall"
        viewModel.amountText = "1200"
        viewModel.method = .cash
        viewModel.notes = "monthly rent"

        await viewModel.createRecord(client: client)

        XCTAssertEqual(viewModel.records.count, 1)
        XCTAssertEqual(viewModel.records.first?.recordNo, "FR-001")
        XCTAssertEqual(viewModel.records.first?.category, "rent")
        XCTAssertEqual(viewModel.amountText, "")
        XCTAssertEqual(viewModel.category, "")
        XCTAssertEqual(viewModel.partnerName, "")
        XCTAssertEqual(viewModel.notes, "")
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

    private static let financeRecordListEnvelope = Data(
        """
        {
          "code": 0,
          "message": "ok",
          "data": [
            {
              "id": "66001",
              "record_no": "FR-001",
              "type": 2,
              "category": "rent",
              "partner_name": "mall",
              "amount": 1200.0,
              "method": 1,
              "notes": "monthly rent",
              "created_at": 1710000000000,
              "updated_at": 1710000000000
            }
          ]
        }
        """.utf8
    )

    private static let financeRecordEnvelope = Data(
        """
        {
          "code": 0,
          "message": "ok",
          "data": {
            "id": "66001",
            "record_no": "FR-001",
            "type": 2,
            "category": "rent",
            "partner_name": "mall",
            "amount": 1200.0,
            "method": 1,
            "notes": "monthly rent",
            "created_at": 1710000000000,
            "updated_at": 1710000000000
          }
        }
        """.utf8
    )
}
