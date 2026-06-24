import XCTest
@testable import ZhihuijiIOS

final class RolePermissionMatrixTests: XCTestCase {
    func testRolePermissionMatrixMatchesExpectedTopLevelAccess() {
        XCTAssertEqual(PermissionPolicy.permissions(for: .owner), Set(Permission.allCases))

        let managerPermissions = PermissionPolicy.permissions(for: .manager)
        XCTAssertTrue(managerPermissions.contains(.dashboardView))
        XCTAssertTrue(managerPermissions.contains(.salesWrite))
        XCTAssertTrue(managerPermissions.contains(.inventoryWrite))
        XCTAssertTrue(managerPermissions.contains(.usersManage))
        XCTAssertFalse(managerPermissions.contains(.databaseManage))

        let warehousePermissions = PermissionPolicy.permissions(for: .warehouse)
        XCTAssertTrue(warehousePermissions.contains(.dashboardView))
        XCTAssertTrue(warehousePermissions.contains(.inventoryView))
        XCTAssertTrue(warehousePermissions.contains(.inventoryWrite))
        XCTAssertFalse(warehousePermissions.contains(.salesWrite))
        XCTAssertFalse(warehousePermissions.contains(.reportsView))

        let assistantPermissions = PermissionPolicy.permissions(for: .assistant)
        XCTAssertTrue(assistantPermissions.contains(.dashboardView))
        XCTAssertTrue(assistantPermissions.contains(.reportsView))
        XCTAssertTrue(assistantPermissions.contains(.agentView))
        XCTAssertFalse(assistantPermissions.contains(.agentWrite))
    }

    func testAppSessionPermissionQueriesFollowCurrentStoreProfile() {
        let session = AppSession()
        let profile = CurrentStoreProfile(
            storeId: "90001",
            storeName: "智慧记示例门店",
            ownerUserId: "80001",
            currentUserId: "80002",
            currentUserName: "仓库员工",
            currentUserPhone: "13800000001",
            role: .warehouse,
            title: "仓库员工",
            status: 1,
            permissions: Array(PermissionPolicy.permissions(for: .warehouse)),
            memberCount: 5,
            enabledMemberCount: 4,
            disabledMemberCount: 1
        )

        session.updateStore(profile)

        XCTAssertTrue(session.hasPermission(.inventoryView))
        XCTAssertTrue(session.hasAnyPermission([.salesView, .inventoryView]))
        XCTAssertFalse(session.hasPermission(.salesWrite))
        XCTAssertFalse(session.hasAnyPermission([.reportsView, .financeWrite]))
    }

    func testTopLevelTabsFollowMobileInformationArchitecture() {
        XCTAssertEqual(TopLevelTabKey.visibleTabs(for: [.dashboardView]), [.dashboard])
        XCTAssertEqual(
            TopLevelTabKey.visibleTabs(for: [.dashboardView, .salesView]),
            [.dashboard, .documents]
        )
        XCTAssertEqual(
            TopLevelTabKey.visibleTabs(for: [.dashboardView, .salesView, .archivesView, .reportsView, .agentView]),
            [.dashboard, .documents, .archives, .reports, .agent]
        )
        XCTAssertEqual(
            TopLevelTabKey.visibleTabs(for: [.inventoryView]),
            [.documents]
        )
        XCTAssertTrue(TopLevelTabKey.documents.isVisible(for: [.purchaseView]))
        XCTAssertFalse(TopLevelTabKey.reports.isVisible(for: [.financeView]))
        XCTAssertEqual(TopLevelTabKey.dashboard.title, "首页")
        XCTAssertEqual(TopLevelTabKey.agent.systemImage, "sparkles")
    }
}
