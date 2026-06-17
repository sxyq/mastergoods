import Foundation
import SwiftUI

@MainActor
final class AppSession: ObservableObject {
    @Published private(set) var phase: SessionPhase = .booting
    @Published private(set) var auth: AuthState = .loggedOut
    @Published private(set) var currentStore: CurrentStoreProfile?

    private let tokenStore = AuthTokenStore()

    enum SessionPhase {
        case booting
        case ready
    }

    enum AuthState: Equatable {
        case loggedOut
        case loggedIn(UserSession)
    }

    var permissions: Set<Permission> {
        Set(currentStore?.permissions ?? [])
    }

    func bootstrap() async {
        let token = tokenStore.readAccessToken()
        if let token, !token.isEmpty {
            auth = .loggedIn(UserSession(token: token, refreshToken: tokenStore.readRefreshToken()))
        } else {
            auth = .loggedOut
        }
        phase = .ready
    }

    func updateAuth(_ payload: AuthPayload) {
        tokenStore.save(accessToken: payload.token, refreshToken: payload.refreshToken)
        auth = .loggedIn(UserSession(token: payload.token, refreshToken: payload.refreshToken))
    }

    func updateStore(_ profile: CurrentStoreProfile) {
        currentStore = profile
    }

    func logout() {
        tokenStore.clear()
        currentStore = nil
        auth = .loggedOut
    }

    func hydrateStore(using client: APIClient) async {
        guard case .loggedIn = auth else { return }
        do {
            let store = try await client.fetchCurrentStore()
            currentStore = store
        } catch let error as APIError {
            if error == .unauthorized {
                logout()
            }
        } catch {
            return
        }
    }

    func hasPermission(_ permission: Permission) -> Bool {
        permissions.contains(permission)
    }

    func hasAnyPermission(_ values: [Permission]) -> Bool {
        values.contains(where: permissions.contains)
    }
}

struct UserSession: Equatable {
    let token: String
    let refreshToken: String?
}
