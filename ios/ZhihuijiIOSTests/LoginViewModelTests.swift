import Foundation
import XCTest
@testable import ZhihuijiIOS

final class LoginViewModelTests: XCTestCase {
    override func setUp() {
        super.setUp()
        LoginMockURLProtocol.requestHandler = nil
    }

    override func tearDown() {
        LoginMockURLProtocol.requestHandler = nil
        super.tearDown()
    }

    func testLoginViewModelStartsEmpty() {
        let viewModel = LoginViewModel()

        XCTAssertEqual(viewModel.phone, "")
        XCTAssertEqual(viewModel.password, "")
        XCTAssertFalse(viewModel.isLoading)
        XCTAssertNil(viewModel.errorMessage)
    }

    @MainActor
    func testLoginViewModelLogsInAndHydratesStore() async {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [LoginMockURLProtocol.self]
        let session = URLSession(configuration: config)
        let tokenStore = AuthTokenStore()
        tokenStore.clear()
        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)
        let appSession = AppSession()
        let viewModel = LoginViewModel()
        viewModel.phone = "13800000001"
        viewModel.password = "123456"

        LoginMockURLProtocol.requestHandler = { request in
            guard let path = request.url?.path else {
                throw URLError(.badURL)
            }

            switch path {
            case "/v1/auth/login":
                XCTAssertEqual(request.httpMethod, "POST")
                let body = try XCTUnwrap(request.httpBody)
                let json = try JSONSerialization.jsonObject(with: body) as? [String: Any]
                XCTAssertEqual(json?["phone"] as? String, "13800000001")
                XCTAssertEqual(json?["password"] as? String, "123456")
                return (Self.response(statusCode: 200), Self.loginPayload)
            case "/v2/stores/current":
                return (Self.response(statusCode: 200), Self.currentStorePayload)
            default:
                throw URLError(.fileDoesNotExist)
            }
        }

        await viewModel.login(using: client, session: appSession)

        XCTAssertTrue(appSession.isAuthenticated)
        XCTAssertEqual(appSession.phase, .ready)
        if case let .loggedIn(userSession) = appSession.auth {
            XCTAssertEqual(userSession.token, "access-token")
            XCTAssertEqual(userSession.refreshToken, "refresh-token")
        } else {
            XCTFail("Expected login session to be authenticated")
        }
        XCTAssertEqual(tokenStore.readAccessToken(), "access-token")
        XCTAssertEqual(tokenStore.readRefreshToken(), "refresh-token")
        XCTAssertEqual(appSession.currentStore?.currentUserName, "Test User")
        XCTAssertNil(viewModel.errorMessage)
        XCTAssertFalse(viewModel.isLoading)
    }
}

final class LoginMockURLProtocol: URLProtocol {
    static var requestHandler: ((URLRequest) throws -> (HTTPURLResponse, Data))?

    override class func canInit(with request: URLRequest) -> Bool {
        true
    }

    override class func canonicalRequest(for request: URLRequest) -> URLRequest {
        request
    }

    override func startLoading() {
        guard let handler = Self.requestHandler else {
            client?.urlProtocol(self, didFailWithError: URLError(.unsupportedURL))
            return
        }

        do {
            let (response, data) = try handler(request)
            client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
            if !data.isEmpty {
                client?.urlProtocol(self, didLoad: data)
            }
            client?.urlProtocolDidFinishLoading(self)
        } catch {
            client?.urlProtocol(self, didFailWithError: error)
        }
    }

    override func stopLoading() {}

    private static func response(statusCode: Int) -> HTTPURLResponse {
        HTTPURLResponse(
            url: URL(string: "https://example.com")!,
            statusCode: statusCode,
            httpVersion: "HTTP/1.1",
            headerFields: ["Content-Type": "application/json"]
        )!
    }

    private static let loginPayload = Data(
        """
        {
          "code": 0,
          "message": "ok",
          "data": {
            "user_id": "70001",
            "token": "access-token",
            "refresh_token": "refresh-token",
            "expires_in": 7200
          }
        }
        """.utf8
    )

    private static let currentStorePayload = Data(
        """
        {
          "code": 0,
          "message": "ok",
          "data": {
            "store_id": "90001",
            "store_name": "Test Store",
            "owner_user_id": "70001",
            "current_user_id": "70001",
            "current_user_name": "Test User",
            "current_user_phone": "13800000001",
            "role": "OWNER",
            "title": "Store Owner",
            "status": 1,
            "permissions": ["dashboard:view", "database:manage"],
            "member_count": 3,
            "enabled_member_count": 3,
            "disabled_member_count": 0
          }
        }
        """.utf8
    )
}
