import XCTest
@testable import ZhihuijiIOS

final class APIClientTests: XCTestCase {
    func testEndpointPathsAreStable() {
        XCTAssertEqual(APIEndpoint.currentStore.path, "/v2/stores/current")
        XCTAssertEqual(APIEndpoint.storeMembers.path, "/v2/stores/current/members")
        XCTAssertEqual(APIEndpoint.agentWorkbench.path, "/v2/agent/workbench")
        XCTAssertEqual(APIEndpoint.refresh.path, "/v1/auth/refresh")
    }

    func testAuthPayloadDecodesLargeUserIDAsEntityID() throws {
        let data = Data(
            """
            {
              "user_id": 9223372036854775000,
              "token": "access-token",
              "refresh_token": "refresh-token",
              "expires_in": 7200
            }
            """.utf8
        )

        let payload = try JSONDecoder().decode(AuthPayload.self, from: data)
        XCTAssertEqual(payload.userId.rawValue, "9223372036854775000")
        XCTAssertEqual(payload.token, "access-token")
        XCTAssertEqual(payload.refreshToken, "refresh-token")
    }

    func testAppEnvironmentResolvesValidBaseURL() {
        let environment = AppEnvironment.environment(from: "https://example.com:8443")
        XCTAssertEqual(environment?.apiBaseURL.absoluteString, "https://example.com:8443")
        XCTAssertNil(AppEnvironment.environment(from: "not-a-url"))
    }

    func testArchiveRelatedEndpointsAreStable() {
        XCTAssertEqual(APIEndpoint.products.path, "/v2/products")
        XCTAssertEqual(APIEndpoint.currentStore.path, "/v2/stores/current")
    }

    func testInventoryRelatedEndpointsAreStable() {
        XCTAssertEqual(APIEndpoint.inventorySnapshots.path, "/v2/inventory/snapshots")
        XCTAssertEqual(APIEndpoint.inventoryMonthlyStats.path, "/v2/inventory/monthly-stats")
        XCTAssertEqual(APIEndpoint.inventoryLedger.path, "/v2/inventory/ledger")
    }
}
