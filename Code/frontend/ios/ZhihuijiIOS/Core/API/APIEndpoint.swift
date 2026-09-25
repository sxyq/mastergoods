import Foundation

enum APIEndpoint {
    case login
    case refresh
    case logout
    case currentUser
    case currentStore
    case storeMembers
    case agentWorkbench
    case mediaAssets
    case mediaBindings
    case syncHealth
    case importJobs

    var path: String {
        switch self {
        case .login:
            return "/v2/auth/login"
        case .refresh:
            return "/v2/auth/refresh"
        case .logout:
            return "/v2/auth/logout"
        case .currentUser:
            return "/v2/auth/users/me"
        case .currentStore:
            return "/v2/stores/current"
        case .storeMembers:
            return "/v2/stores/current/members"
        case .agentWorkbench:
            return "/v2/agent/workbench"
        case .mediaAssets:
            return "/v2/media/assets"
        case .mediaBindings:
            return "/v2/media/bindings"
        case .syncHealth:
            return "/v2/sync/health"
        case .importJobs:
            return "/v2/import-jobs"
        }
    }
}
