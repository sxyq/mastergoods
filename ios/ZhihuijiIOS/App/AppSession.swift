import Foundation
import SwiftUI

@MainActor
final class AppSession: ObservableObject {
    @Published private(set) var phase: SessionPhase = .booting
    @Published private(set) var auth: AuthState = .loggedOut
    @Published private(set) var currentStore: CurrentStoreProfile?
    @Published private(set) var storeLoadError: String?
    @Published var accessIssue: AccessIssue?

    private let tokenStore = AuthTokenStore()
    private var unauthorizedObserverTask: Task<Void, Never>?
    private var forbiddenObserverTask: Task<Void, Never>?

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

    var isAuthenticated: Bool {
        if case .loggedIn = auth {
            return true
        }
        return false
    }

    init() {
        bindAuthNotifications()
    }

    deinit {
        unauthorizedObserverTask?.cancel()
        forbiddenObserverTask?.cancel()
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
        storeLoadError = nil
        accessIssue = nil
    }

    func updateStore(_ profile: CurrentStoreProfile) {
        currentStore = profile
        storeLoadError = nil
        accessIssue = nil
    }

    func logout() {
        tokenStore.clear()
        currentStore = nil
        storeLoadError = nil
        accessIssue = nil
        auth = .loggedOut
    }

    func hydrateStore(using client: APIClient) async {
        guard case .loggedIn = auth else { return }
        do {
            let store = try await client.fetchCurrentStore()
            currentStore = store
            storeLoadError = nil
        } catch let error as APIError {
            if error == .unauthorized {
                logout()
            } else {
                currentStore = nil
                storeLoadError = error.errorDescription ?? error.localizedDescription
            }
        } catch {
            currentStore = nil
            storeLoadError = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    func hasPermission(_ permission: Permission) -> Bool {
        permissions.contains(permission)
    }

    func hasAnyPermission(_ values: [Permission]) -> Bool {
        values.contains(where: permissions.contains)
    }

    func clearAccessIssue() {
        accessIssue = nil
    }

    private func bindAuthNotifications() {
        unauthorizedObserverTask = Task { [weak self] in
            guard let self else { return }
            for await _ in NotificationCenter.default.notifications(named: .zhihuijiUnauthorized) {
                await MainActor.run {
                    self.logout()
                }
            }
        }

        forbiddenObserverTask = Task { [weak self] in
            guard let self else { return }
            for await note in NotificationCenter.default.notifications(named: .zhihuijiForbidden) {
                let message = note.userInfo?["message"] as? String
                await MainActor.run {
                    self.accessIssue = AccessIssue(
                        title: "权限不足",
                        message: message?.nilIfBlank ?? "当前账号没有权限访问该数据或执行该操作。"
                    )
                }
            }
        }
    }
}

struct UserSession: Equatable {
    let token: String
    let refreshToken: String?
}

struct AccessIssue: Identifiable, Equatable {
    let id = UUID()
    let title: String
    let message: String
}
