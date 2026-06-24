import Foundation

struct SettingsSecurityPolicy: Equatable {
    let canManageDatabase: Bool

    static func resolve(for permissions: Set<Permission>) -> SettingsSecurityPolicy {
        SettingsSecurityPolicy(canManageDatabase: permissions.contains(.databaseManage))
    }
}
