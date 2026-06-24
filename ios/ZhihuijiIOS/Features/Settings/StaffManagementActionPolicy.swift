import Foundation

struct StaffManagementActionPolicy: Equatable {
    let canManageStaff: Bool

    static func resolve(for permissions: Set<Permission>) -> StaffManagementActionPolicy {
        StaffManagementActionPolicy(canManageStaff: permissions.contains(.usersManage))
    }
}
