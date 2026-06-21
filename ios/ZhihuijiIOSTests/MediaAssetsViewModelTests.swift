import XCTest
@testable import ZhihuijiIOS

@MainActor
final class MediaAssetsViewModelTests: XCTestCase {
    func testCreateAssetPayloadRequiresRealObjectKeyAndFileMetadata() {
        let viewModel = MediaAssetsViewModel()

        XCTAssertNil(viewModel.makeCreateAssetPayload())
        XCTAssertEqual(viewModel.errorMessage, "请先填写真实的 object key；iOS 当前不会生成默认对象或上传假文件。")

        viewModel.objectKey = "products/50001/cover.png"
        XCTAssertNil(viewModel.makeCreateAssetPayload())
        XCTAssertEqual(viewModel.errorMessage, "请填写原始文件名。")

        viewModel.originalFileName = "cover.png"
        viewModel.mimeType = ""
        XCTAssertNil(viewModel.makeCreateAssetPayload())
        XCTAssertEqual(viewModel.errorMessage, "请填写 MIME type。")
    }

    func testCreateAssetPayloadRejectsNonPositiveSizeAndInvalidMetadata() {
        let viewModel = MediaAssetsViewModel()
        viewModel.objectKey = "products/50001/cover.png"
        viewModel.originalFileName = "cover.png"
        viewModel.mimeType = "image/png"
        viewModel.sizeBytesText = "0"

        XCTAssertNil(viewModel.makeCreateAssetPayload())
        XCTAssertEqual(viewModel.errorMessage, "文件大小必须是大于 0 的 bytes 数值。")

        viewModel.sizeBytesText = "2048"
        viewModel.metadataJson = "not-json"

        XCTAssertNil(viewModel.makeCreateAssetPayload())
        XCTAssertEqual(viewModel.errorMessage, "Metadata JSON 必须是合法 JSON 对象。")
    }

    func testCreateAssetPayloadUsesExplicitUploadedObjectFields() throws {
        let viewModel = MediaAssetsViewModel()
        viewModel.assetType = "product_cover"
        viewModel.storageProvider = "object_storage"
        viewModel.bucketName = "master-goods"
        viewModel.objectKey = "products/50001/cover.png"
        viewModel.originalFileName = "cover.png"
        viewModel.mimeType = "image/png"
        viewModel.sizeBytesText = "2048"
        viewModel.checksum = "sha256:cover"
        viewModel.widthText = "800"
        viewModel.heightText = "600"
        viewModel.metadataJson = #"{"source":"ios"}"#

        let payload = try XCTUnwrap(viewModel.makeCreateAssetPayload())

        XCTAssertEqual(payload.assetType, "product_cover")
        XCTAssertEqual(payload.storageProvider, "object_storage")
        XCTAssertEqual(payload.bucketName, "master-goods")
        XCTAssertEqual(payload.objectKey, "products/50001/cover.png")
        XCTAssertEqual(payload.originalFileName, "cover.png")
        XCTAssertEqual(payload.mimeType, "image/png")
        XCTAssertEqual(payload.sizeBytes, 2048)
        XCTAssertEqual(payload.checksum, "sha256:cover")
        XCTAssertEqual(payload.width, 800)
        XCTAssertEqual(payload.height, 600)
        XCTAssertEqual(payload.metadataJson, #"{"source":"ios"}"#)
        XCTAssertNil(viewModel.errorMessage)
    }

    func testInitialTargetPrefillsProductBindingFields() {
        let viewModel = MediaAssetsViewModel()

        viewModel.applyInitialTarget(type: "product", id: "50001")

        XCTAssertEqual(viewModel.targetType, "product")
        XCTAssertEqual(viewModel.targetId, "50001")
        XCTAssertEqual(viewModel.bindingTargetType, "product")
        XCTAssertEqual(viewModel.bindingTargetId, "50001")
    }
}
