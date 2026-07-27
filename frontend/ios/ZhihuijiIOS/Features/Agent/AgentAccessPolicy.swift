import Foundation

struct AgentAccessPolicy: Equatable {
    let canViewAgent: Bool
    let canWriteAgent: Bool

    static func resolve(for permissions: Set<Permission>) -> AgentAccessPolicy {
        AgentAccessPolicy(
            canViewAgent: permissions.contains(.agentView),
            canWriteAgent: permissions.contains(.agentWrite)
        )
    }
}
