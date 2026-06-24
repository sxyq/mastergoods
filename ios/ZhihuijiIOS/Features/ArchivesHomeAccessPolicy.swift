import Foundation

struct ArchivesHomeAccessPolicy: Equatable {
    let availableTabs: [ArchiveTab]

    static func resolve(for permissions: Set<Permission>) -> ArchivesHomeAccessPolicy {
        var tabs: [ArchiveTab] = []
        if permissions.contains(.archivesView) {
            tabs.append(.products)
        }
        if permissions.contains(.salesView) {
            tabs.append(.customers)
        }
        if permissions.contains(.purchaseView) {
            tabs.append(.suppliers)
        }
        return ArchivesHomeAccessPolicy(availableTabs: tabs)
    }
}
