import XCTest
@testable import ZhihuijiIOS

final class AuthPermissionTests: XCTestCase {
    func testPermissionRawValue() {
        XCTAssertEqual(Permission.usersManage.rawValue, "users:manage")
        XCTAssertEqual(Permission.agentView.rawValue, "agent:view")
        XCTAssertEqual(Permission.financeWrite.rawValue, "finance:write")
    }

    func testStoreRoleLabelsMatchMobileSemantics() {
        XCTAssertEqual(StoreRole.owner.label, "店长（总）")
        XCTAssertEqual(StoreRole.manager.label, "店长助理")
        XCTAssertEqual(StoreRole.assistant.label, "AI/只读助理")
    }

    func testStoreRoleDecodingAcceptsBackendCaseVariantsAndEncodesCanonicalValue() throws {
        XCTAssertEqual(try JSONDecoder().decode(StoreRole.self, from: Data(#""assistant""#.utf8)), .assistant)
        XCTAssertEqual(try JSONDecoder().decode(StoreRole.self, from: Data(#""ASSISTANT""#.utf8)), .assistant)
        XCTAssertEqual(try JSONDecoder().decode(StoreRole.self, from: Data(#"" manager ""#.utf8)), .manager)

        let encoded = try JSONEncoder().encode(StoreRole.assistant)
        XCTAssertEqual(String(data: encoded, encoding: .utf8), #""ASSISTANT""#)
    }

    func testUserProfileDecodesStringBackedID() throws {
        let data = Data(
            """
            {
              "id": "70001",
              "phone": "13800000001",
              "nickname": "测试账号",
              "status": 1
            }
            """.utf8
        )

        let profile = try JSONDecoder().decode(UserProfile.self, from: data)
        XCTAssertEqual(profile.id.rawValue, "70001")
        XCTAssertEqual(profile.nickname, "测试账号")
    }

    func testAccessIssueCarriesMessage() {
        let issue = AccessIssue(title: "权限不足", message: "当前账号没有权限访问该数据。")
        XCTAssertEqual(issue.title, "权限不足")
        XCTAssertEqual(issue.message, "当前账号没有权限访问该数据。")
    }

    func testAuthNotificationNamesAreStable() {
        XCTAssertEqual(Notification.Name.zhihuijiUnauthorized.rawValue, "zhihuiji.api.unauthorized")
        XCTAssertEqual(Notification.Name.zhihuijiForbidden.rawValue, "zhihuiji.api.forbidden")
    }

    func testMediaWritePolicyMatchesBusinessPermissions() {
        XCTAssertTrue(PermissionPolicy.canWriteMedia(targetType: "product", permissions: [.archivesWrite]))
        XCTAssertTrue(PermissionPolicy.canWriteMedia(targetType: " sale_order ", permissions: [.salesWrite]))
        XCTAssertTrue(PermissionPolicy.canWriteMedia(targetType: "PURCHASE_ORDER", permissions: [.purchaseWrite]))
        XCTAssertTrue(PermissionPolicy.canWriteMedia(targetType: "asset", permissions: [.databaseManage]))

        XCTAssertFalse(PermissionPolicy.canWriteMedia(targetType: "product", permissions: [.archivesView]))
        XCTAssertFalse(PermissionPolicy.canWriteMedia(targetType: "sale_order", permissions: [.financeWrite]))
        XCTAssertFalse(PermissionPolicy.canWriteMedia(targetType: "unknown", permissions: [.archivesWrite]))
    }

    func testDatabaseManagePolicyRequiresDatabaseManagePermission() {
        XCTAssertTrue(PermissionPolicy.canManageDatabase([.databaseManage]))
        XCTAssertFalse(PermissionPolicy.canManageDatabase([.settingsManage, .usersManage]))
        XCTAssertFalse(PermissionPolicy.canManageDatabase([]))
    }

    func testMediaPermissionsRespectTargetTypeScope() {
        XCTAssertTrue(PermissionPolicy.canWriteMedia(targetType: "asset", permissions: [.databaseManage]))
        XCTAssertTrue(PermissionPolicy.canWriteMedia(targetType: "asset", permissions: [.databaseManage, .archivesView]))
        XCTAssertFalse(PermissionPolicy.canWriteMedia(targetType: "asset", permissions: [.archivesWrite]))
        XCTAssertFalse(PermissionPolicy.canWriteMedia(targetType: "asset", permissions: [.salesWrite]))
    }
}
