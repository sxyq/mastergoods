import XCTest
@testable import ZhihuijiIOS

final class AppSessionTests: XCTestCase {
    override func tearDown() {
        AppSessionMockURLProtocol.requestHandler = nil
        super.tearDown()
    }

    @MainActor
    func testLogoutClearsAuthenticationAndStoreState() async {
        let session = AppSession()
        await session.bootstrap()
        session.updateAuth(
            AuthPayload(
                userId: EntityID(rawValue: "70001"),
                token: "access-token",
                refreshToken: "refresh-token",
                expiresIn: 7200
            )
        )
        session.updateStore(
            CurrentStoreProfile(
                storeId: EntityID(rawValue: "90001"),
                storeName: "Test Store",
                ownerUserId: EntityID(rawValue: "70001"),
                currentUserId: EntityID(rawValue: "70001"),
                currentUserName: "Test User",
                currentUserPhone: "13800000001",
                role: .owner,
                title: "Store Owner",
                status: 1,
                permissions: [.dashboardView],
                memberCount: 3,
                enabledMemberCount: 3,
                disabledMemberCount: 0
            )
        )

        session.logout()

        XCTAssertFalse(session.isAuthenticated)
        XCTAssertNil(session.currentStore)

        let restoredSession = AppSession()
        await restoredSession.bootstrap()
        XCTAssertFalse(restoredSession.isAuthenticated)
    }

    @MainActor
    func testBootstrapRestoresStoredTokenState() async {
        let seedSession = AppSession()
        seedSession.updateAuth(
            AuthPayload(
                userId: EntityID(rawValue: "70001"),
                token: "access-token",
                refreshToken: "refresh-token",
                expiresIn: 7200
            )
        )

        let restoredSession = AppSession()
        await restoredSession.bootstrap()

        guard case let .loggedIn(userSession) = restoredSession.auth else {
            XCTFail("Expected bootstrapped session to restore login state")
            seedSession.logout()
            return
        }

        XCTAssertEqual(restoredSession.phase, .ready)
        XCTAssertEqual(userSession.token, "access-token")
        XCTAssertEqual(userSession.refreshToken, "refresh-token")

        restoredSession.logout()
    }

    @MainActor
    func testUnauthorizedNotificationLogsOutSession() async {
        let session = AppSession()
        await session.bootstrap()
        session.updateAuth(
            AuthPayload(
                userId: EntityID(rawValue: "70001"),
                token: "access-token",
                refreshToken: "refresh-token",
                expiresIn: 7200
            )
        )

        XCTAssertTrue(session.isAuthenticated)
        NotificationCenter.default.post(name: .zhihuijiUnauthorized, object: nil)
        try? await Task.sleep(nanoseconds: 100_000_000)

        XCTAssertFalse(session.isAuthenticated)
        XCTAssertEqual(session.phase, .ready)
        XCTAssertNil(session.currentStore)
    }

    @MainActor
    func testForbiddenNotificationShowsAccessIssueButKeepsSession() async {
        let session = AppSession()
        await session.bootstrap()
        session.updateAuth(
            AuthPayload(
                userId: EntityID(rawValue: "70001"),
                token: "access-token",
                refreshToken: "refresh-token",
                expiresIn: 7200
            )
        )

        NotificationCenter.default.post(
            name: .zhihuijiForbidden,
            object: nil,
            userInfo: ["message": "No permission for inventory"]
        )
        try? await Task.sleep(nanoseconds: 100_000_000)

        XCTAssertTrue(session.isAuthenticated)
        XCTAssertNotNil(session.accessIssue)
        XCTAssertEqual(session.accessIssue?.title, "权限不足")
        XCTAssertEqual(session.accessIssue?.message, "No permission for inventory")
    }

    @MainActor
    func testHydrateStoreFailureKeepsBusinessRoutesBlockedWithRetryableError() async {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [AppSessionMockURLProtocol.self]
        let urlSession = URLSession(configuration: config)
        let tokenStore = AuthTokenStore()
        tokenStore.clear()
        tokenStore.save(accessToken: "access-token", refreshToken: "refresh-token")
        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: urlSession)
        let session = AppSession()
        await session.bootstrap()
        session.updateAuth(
            AuthPayload(
                userId: EntityID(rawValue: "70001"),
                token: "access-token",
                refreshToken: "refresh-token",
                expiresIn: 7200
            )
        )
        session.updateStore(Self.sampleStoreProfile())

        AppSessionMockURLProtocol.requestHandler = { request in
            guard request.url?.path == "/v2/stores/current" else {
                throw URLError(.fileDoesNotExist)
            }
            return (
                Self.response(statusCode: 500),
                Data(#"{"code":500,"message":"store unavailable","data":null}"#.utf8)
            )
        }

        await session.hydrateStore(using: client)

        XCTAssertTrue(session.isAuthenticated)
        XCTAssertNil(session.currentStore)
        XCTAssertEqual(session.storeLoadError, "store unavailable")

        AppSessionMockURLProtocol.requestHandler = { request in
            guard request.url?.path == "/v2/stores/current" else {
                throw URLError(.fileDoesNotExist)
            }
            return (Self.response(statusCode: 200), Self.currentStorePayload)
        }

        await session.hydrateStore(using: client)

        XCTAssertTrue(session.isAuthenticated)
        XCTAssertEqual(session.currentStore?.storeId.rawValue, "90001")
        XCTAssertNil(session.storeLoadError)
    }

    private static func sampleStoreProfile() -> CurrentStoreProfile {
        CurrentStoreProfile(
            storeId: EntityID(rawValue: "90001"),
            storeName: "Test Store",
            ownerUserId: EntityID(rawValue: "70001"),
            currentUserId: EntityID(rawValue: "70001"),
            currentUserName: "Test User",
            currentUserPhone: "13800000001",
            role: .owner,
            title: "Store Owner",
            status: 1,
            permissions: [.dashboardView],
            memberCount: 3,
            enabledMemberCount: 3,
            disabledMemberCount: 0
        )
    }

    private static func response(statusCode: Int) -> HTTPURLResponse {
        HTTPURLResponse(
            url: URL(string: "https://example.com")!,
            statusCode: statusCode,
            httpVersion: "HTTP/1.1",
            headerFields: ["Content-Type": "application/json"]
        )!
    }

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
            "permissions": ["dashboard:view"],
            "member_count": 3,
            "enabled_member_count": 3,
            "disabled_member_count": 0
          }
        }
        """.utf8
    )
}

final class AppSessionMockURLProtocol: URLProtocol {
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
}
