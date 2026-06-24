import XCTest
@testable import ZhihuijiIOS

final class InlinePermissionAuditTests: XCTestCase {
    func testNoInlineSessionPermissionChecksRemainInAppSources() throws {
        let sourceRoot = URL(fileURLWithPath: FileManager.default.currentDirectoryPath)
            .appendingPathComponent("ios/ZhihuijiIOS", isDirectory: true)

        let enumerator = FileManager.default.enumerator(
            at: sourceRoot,
            includingPropertiesForKeys: [.isRegularFileKey],
            options: [.skipsHiddenFiles]
        )

        var offenders: [String] = []

        while let fileURL = enumerator?.nextObject() as? URL {
            let values = try fileURL.resourceValues(forKeys: [.isRegularFileKey])
            guard values.isRegularFile == true else { continue }
            guard fileURL.pathExtension == "swift" else { continue }

            let contents = try String(contentsOf: fileURL, encoding: .utf8)
            if contents.contains("session.hasPermission(") {
                offenders.append(fileURL.path)
            }
        }

        XCTAssertTrue(
            offenders.isEmpty,
            "Found inline permission checks that should be routed through policy objects: \(offenders.joined(separator: ", "))"
        )
    }
}
