import XCTest
@testable import ZhihuijiIOS

final class ModelDecodingTests: XCTestCase {
    func testEntityIDDecodesFromNumber() throws {
        let value = try JSONDecoder().decode(EntityID.self, from: Data("123456".utf8))
        XCTAssertEqual(value.rawValue, "123456")
    }
}
