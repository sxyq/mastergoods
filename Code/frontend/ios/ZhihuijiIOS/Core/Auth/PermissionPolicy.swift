import Foundation

enum PermissionPolicy {
    static func canManageDatabase(_ permissions: Set<Permission>) -> Bool {
        permissions.contains(.databaseManage)
    }

    static func canWriteMedia(targetType: String?, permissions: Set<Permission>) -> Bool {
        if canManageDatabase(permissions) {
            return true
        }

        switch normalizedTargetType(targetType) {
        case "product":
            return permissions.contains(.archivesWrite)
        case "sale_order":
            return permissions.contains(.salesWrite)
        case "purchase_order":
            return permissions.contains(.purchaseWrite)
        default:
            return false
        }
    }

    /// 角色到权限的静态映射，与后端 StoreAccessPolicy 保持一致。
    static func permissions(for role: StoreRole) -> Set<Permission> {
        rolePermissions[role] ?? []
    }

    /// 所有角色及其权限映射，按 StoreRole 枚举顺序排列。
    static let rolePermissions: [StoreRole: Set<Permission>] = [
        .owner: Set(Permission.allCases),
        .manager: [
            .dashboardView,
            .salesView,
            .salesWrite,
            .purchaseView,
            .purchaseWrite,
            .archivesView,
            .archivesWrite,
            .inventoryView,
            .inventoryWrite,
            .financeView,
            .reportsView,
            .agentView,
            .agentWrite,
            .usersManage,
        ],
        .sales: [
            .dashboardView,
            .salesView,
            .salesWrite,
            .archivesView,
            .financeView,
            .agentView,
        ],
        .purchasing: [
            .dashboardView,
            .purchaseView,
            .purchaseWrite,
            .archivesView,
            .financeView,
            .agentView,
        ],
        .warehouse: [
            .dashboardView,
            .archivesView,
            .inventoryView,
            .inventoryWrite,
            .agentView,
        ],
        .finance: [
            .dashboardView,
            .financeView,
            .financeWrite,
            .reportsView,
            .salesView,
            .purchaseView,
            .agentView,
        ],
        .assistant: [
            .dashboardView,
            .reportsView,
            .agentView,
        ],
    ]

    private static func normalizedTargetType(_ targetType: String?) -> String {
        targetType?
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .lowercased() ?? ""
    }
}
