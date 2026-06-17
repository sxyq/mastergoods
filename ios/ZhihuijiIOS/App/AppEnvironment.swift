import Foundation
import SwiftUI

struct AppEnvironment {
    let apiBaseURL: URL
    let apiClient: APIClient
    let tokenStore: AuthTokenStore

    static let live: AppEnvironment = {
        let tokenStore = AuthTokenStore()
        let baseURL = URL(string: UserDefaults.standard.string(forKey: "zhihuiji.ios.base_url") ?? "http://127.0.0.1:8080")!
        return AppEnvironment(
            apiBaseURL: baseURL,
            apiClient: APIClient(baseURL: baseURL, tokenStore: tokenStore),
            tokenStore: tokenStore
        )
    }()
}

private struct AppEnvironmentKey: EnvironmentKey {
    static let defaultValue = AppEnvironment.live
}

extension EnvironmentValues {
    var appEnvironment: AppEnvironment {
        get { self[AppEnvironmentKey.self] }
        set { self[AppEnvironmentKey.self] = newValue }
    }
}
