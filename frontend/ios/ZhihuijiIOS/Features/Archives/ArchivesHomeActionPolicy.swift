import Foundation

struct ArchivesHomeActionPolicy: Equatable {
    let canCreateProduct: Bool
    let canCreateCustomer: Bool
    let canCreateSupplier: Bool
    let canEditPartner: Bool

    static func resolve(for permissions: Set<Permission>) -> ArchivesHomeActionPolicy {
        let canWriteArchives = permissions.contains(.archivesWrite)
        return ArchivesHomeActionPolicy(
            canCreateProduct: canWriteArchives,
            canCreateCustomer: canWriteArchives,
            canCreateSupplier: canWriteArchives,
            canEditPartner: canWriteArchives
        )
    }
}
