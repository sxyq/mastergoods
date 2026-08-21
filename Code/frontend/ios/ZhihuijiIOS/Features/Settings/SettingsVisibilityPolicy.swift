import Foundation

struct SettingsVisibilityPolicy: Equatable {
    let canManageStaff: Bool
    let canAccessSyncImport: Bool
    let canAccessMediaAssets: Bool
    let canAccessPlanning: Bool

    static func resolve(for permissions: Set<Permission>) -> SettingsVisibilityPolicy {
        SettingsVisibilityPolicy(
            canManageStaff: permissions.contains(.usersManage),
            canAccessSyncImport: permissions.contains(.databaseManage),
            canAccessMediaAssets: permissions.contains(.databaseManage),
            canAccessPlanning: true
        )
    }
}
