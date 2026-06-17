import XCTest
@testable import ZhihuijiIOS

final class APIClientTests: XCTestCase {
    func testEndpointPathsAreStable() {
        XCTAssertEqual(APIEndpoint.currentStore.path, "/v2/stores/current")
        XCTAssertEqual(APIEndpoint.storeMembers.path, "/v2/stores/current/members")
    }
}
