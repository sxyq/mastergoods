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

    private static func normalizedTargetType(_ targetType: String?) -> String {
        targetType?
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .lowercased() ?? ""
    }
}
