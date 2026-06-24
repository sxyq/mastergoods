import Foundation
import XCTest
@testable import ZhihuijiIOS

final class RegisterViewModelTests: XCTestCase {
    override func setUp() {
        super.setUp()
        RegisterMockURLProtocol.requestHandler = nil
    }

    override func tearDown() {
        RegisterMockURLProtocol.requestHandler = nil
        super.tearDown()
    }

    @MainActor
    func testRegisterViewModelStartsEmpty() {
        let viewModel = RegisterViewModel()

        XCTAssertEqual(viewModel.phone, "")
        XCTAssertEqual(viewModel.password, "")
        XCTAssertEqual(viewModel.verifyCode, "")
        XCTAssertFalse(viewModel.isLoading)
        XCTAssertNil(viewModel.errorMessage)
        XCTAssertNil(viewModel.successMessage)
    }

    @MainActor
    func testRegisterViewModelRegistersAndHydratesStore() async {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [RegisterMockURLProtocol.self]
        let session = URLSession(configuration: config)
        let tokenStore = AuthTokenStore()
        tokenStore.clear()
        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)
        let appSession = AppSession()
        let viewModel = RegisterViewModel()
        viewModel.phone = "13800000002"
        viewModel.password = "123456"
        viewModel.verifyCode = "888888"

        RegisterMockURLProtocol.requestHandler = { request in
            guard let path = request.url?.path else {
                throw URLError(.badURL)
            }

            switch path {
            case "/v2/auth/register":
                XCTAssertEqual(request.httpMethod, "POST")
                let body = try XCTUnwrap(request.httpBody)
                let json = try JSONSerialization.jsonObject(with: body) as? [String: Any]
                XCTAssertEqual(json?["phone"] as? String, "13800000002")
                XCTAssertEqual(json?["password"] as? String, "123456")
                XCTAssertEqual(json?["verify_code"] as? String, "888888")
                return (RegisterMockURLProtocol.response(statusCode: 200), RegisterMockURLProtocol.registerPayload)
            case "/v2/stores/current":
                XCTAssertEqual(request.value(forHTTPHeaderField: "Authorization"), "Bearer access-token")
                return (RegisterMockURLProtocol.response(statusCode: 200), RegisterMockURLProtocol.currentStorePayload)
            default:
                throw URLError(.fileDoesNotExist)
            }
        }

        await viewModel.register(using: client, session: appSession)

        XCTAssertTrue(appSession.isAuthenticated)
        if case let .loggedIn(userSession) = appSession.auth {
            XCTAssertEqual(userSession.token, "access-token")
            XCTAssertEqual(userSession.refreshToken, "refresh-token")
        } else {
            XCTFail("Expected register session to be authenticated")
        }
        XCTAssertEqual(tokenStore.readAccessToken(), "access-token")
        XCTAssertEqual(tokenStore.readRefreshToken(), "refresh-token")
        XCTAssertEqual(appSession.currentStore?.currentUserName, "Test User")
        XCTAssertEqual(viewModel.successMessage, "注册成功，正在同步门店信息")
        XCTAssertNil(viewModel.errorMessage)
        XCTAssertFalse(viewModel.isLoading)
    }
}

final class RegisterMockURLProtocol: URLProtocol {
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

    fileprivate static func response(statusCode: Int) -> HTTPURLResponse {
        HTTPURLResponse(
            url: URL(string: "https://example.com")!,
            statusCode: statusCode,
            httpVersion: "HTTP/1.1",
            headerFields: ["Content-Type": "application/json"]
        )!
    }

    fileprivate static let registerPayload = Data(
        """
        {
          "code": 0,
          "message": "ok",
          "data": {
            "user_id": "70002",
            "token": "access-token",
            "refresh_token": "refresh-token",
            "expires_in": 7200
          }
        }
        """.utf8
    )

    fileprivate static let currentStorePayload = Data(
        """
        {
          "code": 0,
          "message": "ok",
          "data": {
            "store_id": "90001",
            "store_name": "Test Store",
            "owner_user_id": "70002",
            "current_user_id": "70002",
            "current_user_name": "Test User",
            "current_user_phone": "13800000002",
            "role": "OWNER",
            "title": "Store Owner",
            "status": 1,
            "permissions": ["dashboard:view", "database:manage"],
            "member_count": 1,
            "enabled_member_count": 1,
            "disabled_member_count": 0
          }
        }
        """.utf8
    )
}
